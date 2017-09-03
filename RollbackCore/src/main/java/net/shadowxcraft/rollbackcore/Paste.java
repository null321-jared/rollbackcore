/**
 * Copyright (C) 2016 lizardfreak321 <lizardfreak7@gmail.com>
 * 
 * This file is part of RollbackCore
 * 
 * RollbackCore is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.shadowxcraft.rollbackcore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;

import net.shadowxcraft.rollbackcore.events.EndStatus;
import net.shadowxcraft.rollbackcore.events.PasteEndEvent;

/**
 * This class is used to paste a saved rollbackcore copy file.
 * 
 * @see PasteEndEvent
 * @author lizardfreak321
 */
public class Paste extends RollbackOperation {

	// Used to store the X, Y, and Z of where it's getting pasted.
	private final Location min;
	// Used to store the pastes so that this class can start a new paste in
	// distributed pastes.
	private ArrayList<Paste> pastes = null;
	// Keeps track of the old world save setting because it is disabled during
	// pasting.
	private final boolean originalWorldSaveSetting;
	// Keeps track of if entities should be cleared.
	private final boolean clearEntities;
	private final boolean ignoreAir;
	protected PasteTask pasteTask;		// The paste task of this paste.
	protected long startPasteTime = -1; // The nano-time the paste started at.
	protected int blocksChanged = 0;	// The number of blocks changed, for statistical reasons.
	private BufferedInputStream in;
	private File file;
	// Variables used to store the per-block values.
	private int sizeX;
	private int sizeY;
	private int sizeZ;
	private int version;
	int[] simpleBlocks = version1Blocks;
	private static final List<Paste> runningPastes = new ArrayList<Paste>();

	/**
	 * The legacy constructor for backwards compatibility.
	 * 
	 * @param x
	 *            Where the min-x of the paste will be pasted.
	 * @param y
	 *            Where the min-y of the paste will be pasted.
	 * @param z
	 *            Where the min-z of the paste will be pasted.
	 * @param world
	 *            What world the paste will be pasted in.
	 * @param fileName
	 *            The directory of the files.
	 * @param pastes
	 *            An optional ArrayList of pastes that will run after this one, allowing them to be
	 *            distributed. Set to null if you don't want a paste to follow.
	 * @param sender
	 *            The person who will get status messages. Use null for no messages, and
	 *            consoleSender for console.
	 */
	public Paste(int x, int y, int z, World world, String fileName, ArrayList<Paste> pastes, CommandSender sender) {
		this(new Location(world, x, y, z), fileName, sender, false, false, Main.prefix);
		this.pastes = pastes;
	}

	/**
	 * Used to create a new paste operation instance that can be used to paste the file.
	 * 
	 * @param x
	 *            Where the min-x of the paste will be pasted.
	 * @param y
	 *            Where the min-y of the paste will be pasted.
	 * @param z
	 *            Where the min-z of the paste will be pasted.
	 * @param world
	 *            What world the paste will be pasted in.
	 * @param fileName
	 *            The path of the file
	 * @param sender
	 *            The person who will get status messages. Use null for no messages, and
	 *            consoleSender for console.
	 * @param clearEntities
	 *            Used to specify if the paste operation will schedule the removal of the entities.
	 * @param prefix
	 *            Used for the prefix shown in the messages.
	 * @param ignoreAir
	 *            Not check blocks that are air in the file. May be useful for some plugins.
	 */
	public Paste(Location min, String fileName, CommandSender sender, boolean clearEntities, boolean ignoreAir,
			String prefix) {
		this.min = min;
		this.originalWorldSaveSetting = min.getWorld().isAutoSave();
		min.getWorld().setAutoSave(false);
		if (!fileName.endsWith(".dat")) {
			fileName = Main.plugin.getDataFolder().getAbsolutePath() + "/saves/" + fileName + ".dat";
		}
		this.fileName = fileName;
		this.sender = sender;
		this.prefix = prefix;
		this.clearEntities = clearEntities;
		this.ignoreAir = ignoreAir;
	}

	public static int cancelAll() {
		int numberOfTasks = runningPastes.size();
		for (int i = 0; i < numberOfTasks; i++)
			runningPastes.get(i).end(EndStatus.FAIL_EXERNAL_TERMONATION);
		return numberOfTasks;
	}

	/**
	 * Runs the paste operation.
	 */
	@Override
	public void run() {
		paste();
	}

	/**
	 * Runs the paste operation.
	 */
	protected final void paste() {
		if (startPasteTime == -1)
			startPasteTime = System.nanoTime();
		// Checks if there are any currently running pastes of the exact same thing.
		for (Paste runningPaste : runningPastes) {
			if (runningPaste.fileName.equals(fileName) && runningPaste.min.getBlockX() == min.getBlockX()
					&& runningPaste.min.getBlockY() == min.getBlockY()
					&& runningPaste.min.getBlockZ() == min.getBlockZ()) {
				new PasteEndEvent(this, 0, 0, EndStatus.FAIL_DUPLICATE);
				return;
			}
		}

		TaskManager.addTask();

		if (!initializeFile()) {
			return;
		}

		if (!readFile()) {
			return;
		}

		if (clearEntities)
			new ClearEntities(min, max, null, false).progressiveClearEntities();

		// Creates the new paste task, used for progressive pasting.
		PasteTask task = new PasteTask(min, max, in, this, simpleBlocks, ignoreAir, sender, prefix);

		this.pasteTask = task;
		runningPastes.add(this);

		// Schedules the repeating task for the pasting.
		taskID = Main.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(Main.plugin, task, 1, 1);

	}

	private final boolean initializeFile() {
		// Initializes the file.
		file = new File(fileName);

		if (!file.exists()) {
			end(EndStatus.FAIL_NO_SUCH_FILE);
			return false;
		}

		try {
			// Initializes the InputStream
			in = new BufferedInputStream(new FileInputStream(file));
		} catch (IOException e) {
			e.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}
		return true;
	}

	private final boolean readFile() {
		try {
			// In case the file they are trying to read is in of date or too
			// new.
			version = in.read();

			if (version != 0 && version != 1) {
				end(EndStatus.FAIL_INCOMPATIBLE_VERSION);
				in.close();
				return false;
			}

			if (version == 1) {
				int length = in.read();
				simpleBlocks = new int[length];
				for (int i = 0; i < length; i++)
					simpleBlocks[i] = (char) in.read();
			} else {
				simpleBlocks = version1Blocks;
			}

			// Reads the sizes using readShort because it can be larger than 255
			sizeX = FileUtilities.readShort(in);
			sizeY = FileUtilities.readShort(in);
			sizeZ = FileUtilities.readShort(in);
			max = new Location(min.getWorld(), min.getX() + sizeX, min.getY() + sizeY, min.getZ() + sizeZ);

		} catch (IOException e1) {
			e1.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}
		return true;
	}

	/**
	 * Ends the paste task if it is done or not. Sets everything back to the way it should be and
	 * closes open resources.
	 */
	public final void kill() {
		end(EndStatus.FAIL_EXERNAL_TERMONATION);
	}

	protected final void end(EndStatus endStatus) {
		min.getWorld().setAutoSave(originalWorldSaveSetting);
		try {
			if (pasteTask != null && pasteTask.in != null)
				pasteTask.in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Bukkit.getScheduler().cancelTask(taskID);
		taskID = -1;
		runningPastes.remove(this);
		TaskManager.removeTask();

		if (endStatus.equals(EndStatus.SUCCESS) && pastes != null && pastes.size() > 1) {
			// This is for the legacy distributed pastes.
			// Checks to see if there is another paste task to run. Removes from the ArrayList
			// because it's scheduling it.
			pastes.remove(0);

			Paste nextPaste = pastes.get(0);
			nextPaste.startPasteTime = this.startPasteTime;
			nextPaste.blocksChanged = blocksChanged;
			// Schedules it.
			Bukkit.getScheduler().runTaskLater(Main.plugin, nextPaste, 1);
		} else {
			new PasteEndEvent(this, System.nanoTime() - startPasteTime, blocksChanged, endStatus);
		}
	}
}

/**
 * PasteTask class, used by the Paste task as a runnable to make pastes progressive.
 * 
 * @author lizardfreak321
 */
class PasteTask extends RollbackOperation {
	final private Location tempLoc;		// Stores the location that is currently being worked on.
	private int id;						// The ID of the block being worked on.
	private int data;					// The data of the block being worked on.
	private int compressCount = 0;		// Used in the compression algorithm.
	private String[] lines = null;		// Used when getting the lines of a sign from file.
	private long index = 0;				// The index of the block.
	private long tick = 0;				// The current tick.
	protected final BufferedInputStream in;	// The stream used to read from the file.
	protected final CommandSender sender;		// The sender that all messages are sent to.
	private String prefix;				// The prefix all messages will have.
	private final Paste paste;				// The PasteTask object.
	private final int[] simpleBlocks; 	// Stores what blocks do not need the data saved
	private final boolean ignoreAir;

	public PasteTask(Location min, Location max, BufferedInputStream in, Paste paste, int[] simpleBlocks,
			boolean ignoreAir, CommandSender sender, String prefix) {
		this.min = min;
		this.tempLoc = min.clone();
		this.max = max;
		this.lastChunkX = min.getChunk().getX();
		this.in = in;
		this.sender = sender;
		this.prefix = prefix;
		this.paste = paste;
		this.simpleBlocks = simpleBlocks;
		this.ignoreAir = ignoreAir;
	}

	@Override
	public final void run() {
		long startTime = System.nanoTime(); // Keeps track of the start time.
		boolean skip = false;

		// Increments the tick variable to keep track of how many ticks this operation has been
		// running.
		tick++;

		// Loops until done or skipped. Done is defined as when the x value goes too far.
		while (tempLoc.getBlockX() <= max.getBlockX() && !skip) {

			try {
				if (!getIDsFromFile())
					return;
				checkAndUpdateBlocks();
				index++;

				// Subtracts from compressCount because it
				// finished with a block.
				compressCount--;
			} catch (IOException e) {
				e.printStackTrace();
				paste.end(EndStatus.FAIL_IO_ERROR);

			}

			updateXYZ();

			// Checks if it has run out of time.
			if (System.nanoTime() - startTime > TaskManager.getMaxTime() * 1000000) {
				skip = true;
			} else {
				skip = false;
			}
		}

		// Displays the status update to the user if needed.
		statusMessage();

		// Checks if it is done, ends it if it is.
		if (tempLoc.getBlockX() > max.getBlockX())
			paste.end(EndStatus.SUCCESS);

	}

	private final void updateXYZ() {
		// Updates X, Y, and Z variables.
		tempLoc.setZ(tempLoc.getBlockZ() + 1);

		// Checks if the Z value has gone too far.
		if (tempLoc.getBlockZ() > max.getBlockZ()) {
			tempLoc.setZ(min.getBlockZ());
			tempLoc.setY(tempLoc.getBlockY() + 1);
		}
		// Checks if the Y value has gone too car.
		if (tempLoc.getBlockY() > max.getBlockY()) {
			tempLoc.setY(min.getBlockY());
			tempLoc.setX(tempLoc.getBlockX() + 1);
			// Unloads the finished chunks to save resources.
			checkChunks(tempLoc);
		}
	}

	private boolean getIDsFromFile() throws IOException {
		// less than or equal to 0 means it needs to
		// check for the next set of blocks.
		if (compressCount <= 0) {
			// Gets the ID of the block.
			id = in.read();

			// For compression, it checks if this
			// block doesn't need data saved.
			if (isSimple(id, simpleBlocks)) {
				data = 0;
			} else {
				data = in.read();
			}

			// In some cases it reaches the end of
			// the file early. That normally happens
			// when there was a copy error or the
			// file got corrupted.
			if (id == -1) {
				paste.end(EndStatus.FILE_END_EARLY);
				return false;
			}

			// For compression, it reads how many
			// times this block is repeated.
			compressCount = in.read();

			// The following code is to read sign
			// text.
			if (compressCount == 0) {
				lines = getLines();
			} else {
				lines = null;
			}
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	private final void checkAndUpdateBlocks() {
		// Gets the block from the temporary location.
		Block block = tempLoc.getBlock();

		// Checks if the blocks are the same to save resources. It is much more efficient to only
		// change blocks that are different.
		if ((id != 0 || !ignoreAir) && (id != block.getTypeId() || data != block.getData())) {
			// If they are, schedule a new
			// BlockClass
			block.setTypeIdAndData(id, (byte) data, false);

			paste.blocksChanged++;
		}

		// If it's a sign, set the text to what it was in the database (The array named "text")
		if (lines != null && (id == signPostID || id == wallSignID)) {
			if (!Arrays.equals(((Sign) block.getState()).getLines(), lines)) {

				Sign sign = (Sign) block.getState();
				for (int i = 0; i < 4; i++) {
					sign.setLine(i, lines[i]);
				}

				// Update the sign
				sign.update();
			}
		}
	}

	// Used in the reading of the stored files.
	private final String[] getLines() throws IOException {
		// Signs have 4 lines.
		String lines[] = new String[4];
		String line;
		char tempChar;

		// Reads the lines.
		for (int lineNumber = 0; lineNumber < 4; lineNumber++) {
			boolean notDone = true;
			line = "";
			while (notDone) {
				tempChar = (char) in.read();
				if (tempChar != 0) {
					line = line + tempChar;
				} else {
					notDone = false;
				}
			}
			lines[lineNumber] = line;
		}
		return lines;
	}

	// Used to send status messages to the "sender" if the sender is not null.
	private final void statusMessage() {
		if (sender != null && tick % 100 == 0) {
			long maxBlocks = (max.getBlockX() - min.getBlockX());
			maxBlocks *= (max.getBlockY() - min.getBlockY());
			maxBlocks *= (max.getBlockZ() - min.getBlockZ());
			double percent = (index / (double) maxBlocks) * 100;
			sender.sendMessage(prefix + "Working on paste operation; " + new DecimalFormat("#.0").format(percent)
					+ "% done (" + index + "/" + maxBlocks + ")");
		}
	}

}