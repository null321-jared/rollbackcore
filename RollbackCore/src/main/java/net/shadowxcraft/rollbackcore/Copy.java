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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;

import net.shadowxcraft.rollbackcore.events.CopyEndEvent;
import net.shadowxcraft.rollbackcore.events.EndStatus;

/**
 * This class is used to save a region to a file that can be pasted later to rollback the region.
 * 
 * @see CopyEndEvent
 * @author lizardfreak321
 */
public class Copy extends RollbackOperation {

	private CopyTask copyTask;				// The copy task that this object is using.
	private BufferedOutputStream out;
	private File file;
	private Long startTime = -1l;
	private static final List<Copy> runningCopies = new ArrayList<Copy>();

	/**
	 * Used to schedule a copy. This is the legacy constructor. Used by the copyDistributed method.
	 * 
	 * @param minX
	 *            The X value of the minimum location of the region.
	 * @param minY
	 *            The Y value of the minimum location of the region.
	 * @param minZ
	 *            The Z value of the minimum location of the region.
	 * @param maxX
	 *            The X value of the maximum location of the region.
	 * @param maxY
	 *            The X value of the maximum location of the region.
	 * @param maxZ
	 *            The Y value of the maximum location of the region.
	 * @param world
	 *            The world that the region is in.
	 * @param fileName
	 *            The fileName and directory of the folder that will contain the saved data.
	 *            Recommended: Make a sub-folder in your Main.plugin and put them in there.
	 * @param sender
	 *            Where status messages will be sent. Null for no messages, consoleSender for
	 *            console, and a player for a player.
	 */
	public Copy(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, World world, String fileName,
			CommandSender sender) {
		this(new Location(world, minX, minY, minZ), new Location(world, maxX, maxY, maxZ), fileName, sender,
				Main.prefix);
	}

	/**
	 * Used to schedule a copy.
	 * 
	 * @param minX
	 *            The X value of the minimum location of the region.
	 * @param minY
	 *            The Y value of the minimum location of the region.
	 * @param minZ
	 *            The Z value of the minimum location of the region.
	 * @param maxX
	 *            The X value of the maximum location of the region.
	 * @param maxY
	 *            The X value of the maximum location of the region.
	 * @param maxZ
	 *            The Y value of the maximum location of the region.
	 * @param world
	 *            The world that the region is in.
	 * @param fileName
	 *            The fileName and directory of the folder that will contain the saved data.
	 *            Recommended: Make a sub-folder in your Main.plugin and put them in there.
	 * @param sender
	 *            Where status messages will be sent. Null for no messages, consoleSender for
	 *            console, and a player for a player.
	 * @param prefix
	 *            The prefix that will be used when sending messages to the sender.
	 */
	public Copy(Location min, Location max, String fileName, CommandSender sender, String prefix) {
		this.min = min;
		this.max = max;
		this.fileName = fileName;
		this.sender = sender;
		this.prefix = prefix;
		// Input validation and correction.
		if (!min.getWorld().equals(max.getWorld())) {
			max.setWorld(min.getWorld());
		}
		if (min.getBlockX() > max.getBlockX()) {
			int maxX = max.getBlockX();
			max.setX(min.getX());
			min.setX(maxX);
		}
		if (min.getBlockY() > max.getBlockY()) {
			int maxY = max.getBlockY();
			max.setY(min.getY());
			min.setY(maxY);
		}
		if (min.getBlockZ() > max.getBlockZ()) {
			int maxZ = max.getBlockZ();
			max.setZ(min.getZ());
			min.setZ(maxZ);
		}

	}

	/**
	 * Cancels all of the running copy operations.
	 * 
	 * @return The number of operations cancelled.
	 */
	public static final int cancelAll() {
		int numberOfTasks = runningCopies.size();
		for (int i = 0; i < numberOfTasks; i++) {
			runningCopies.get(i).end(EndStatus.FAIL_EXERNAL_TERMONATION);
		}
		return numberOfTasks;
	}

	/**
	 * Runs the copy operation.
	 */
	@Override
	public final void run() {
		// Calls the copy method.
		copy();
	}

	// The internal method to start the copy operation.
	protected final boolean copy() {
		startTime = System.nanoTime();
		// Checks if there are any currently running pastes of the exact same thing.
		for (Copy runningCopy : runningCopies) {
			if (runningCopy.fileName.equals(fileName)) {
				new CopyEndEvent(this, 0, EndStatus.FAIL_DUPLICATE);
				return false;
			}
		}

		if (!initializeStream())
			return false;

		if (!startFile())
			return false;

		CopyTask task = new CopyTask(min, max, out, this, sender, prefix);

		this.copyTask = task;
		runningCopies.add(this);
		TaskManager.addTask();
		taskID = Main.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(Main.plugin, task, 1, 1);
		return true;
	}

	// Prepares the file and stream.
	private final boolean initializeStream() {
		// Initializes the file
		file = new File(fileName + ".dat");
		if (file.exists()) {
			// Deletes it if it exists so it starts over.
			file.delete();
		}

		// Creates the file.
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}

		// Initializes the FileOutputStream.
		try {
			out = new BufferedOutputStream(new FileOutputStream(file));
		} catch (IOException e) {
			e.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}
		return true;
	}

	// Writes the initial data- Version, blocks, and size.
	private final boolean startFile() {
		// Writes the version so that the plugin can convert/reject incompatible
		// versions.
		try {
			out.write(VERSION);
			// VERSION 1 SPECIFIC
			out.write(simpleBlocks.length);
			for (int id : simpleBlocks) {
				out.write(id);
			}
			// END VERSION 1 SPECIFIC

			// Writes the sizes to the file using writeShort because it can be
			// larger than 255.
			FileUtilities.writeShort(out, max.getBlockX() - min.getBlockX());
			FileUtilities.writeShort(out, max.getBlockY() - min.getBlockY());
			FileUtilities.writeShort(out, max.getBlockZ() - min.getBlockZ());
		} catch (IOException e1) {
			e1.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}
		return true;
	}

	/**
	 * Forcefully ends the operation. Sets everything back to the way it should be and closes open
	 * resources.
	 */
	public final void kill() {
		end(EndStatus.FAIL_EXERNAL_TERMONATION);
	}

	// Ends it with that end status.
	protected final void end(EndStatus endStatus) {
		TaskManager.removeTask();

		runningCopies.remove(this);
		// Closes the resource to close resources.
		if (out != null)
			try {
				copyTask.out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		if (taskID >= 0) {
			// Ends the repeating task.
			Bukkit.getScheduler().cancelTask(taskID);
			taskID = -1;
		}
		new CopyEndEvent(this, System.nanoTime() - startTime, endStatus);
	}

}

/**
 * CopyTask class, used by the Copy task as a runnable to make copies progressive.
 * 
 * @author LAPTOP
 */
class CopyTask extends RollbackOperation {
	final private Location tempLoc;		// Stores the location that is currently being worked on.
	private final Copy copy;					// Stores the copy object this works with.
	final BufferedOutputStream out;		// Used to read from the file.
	int lastId = -1;	// Used to keep track of which ID was the previous for the count.
	int lastData = -1;	// Used to keep track of which Data was the previous for the count.
	int count = 0;		// Used for compression to keep track of how many times the block repeats.
	long tick = 0;		// Used to keep track of how many ticks the copy operation has run.
	long blockIndex = 0;// Used to store the index of the block, for statistical reasons.

	public CopyTask(Location min, Location max, BufferedOutputStream out, Copy copy, CommandSender sender,
			String prefix) {
		this.min = min;
		this.tempLoc = min.clone();
		this.max = max;
		this.out = out;
		this.copy = copy;
		this.sender = sender;
		this.prefix = prefix;
		this.lastChunkX = min.getChunk().getX();
	}

	@Override
	public void run() {
		long startTime = System.nanoTime(); // Used to keep track of time.
		tick++; // Increments the tick variable.

		boolean skip = false; // To know when to quit the loop for that tick.

		while (tempLoc.getBlockX() <= max.getBlockX() && !skip) {
			nextBlock();
			// Checks if it has run out of time.
			if (System.nanoTime() - startTime > TaskManager.getMaxTime() * 1000000) {
				skip = true;
			} else {
				skip = false;
			}
		}

		statusMessage(min, max, blockIndex, tick);
		if (tempLoc.getBlockX() > max.getBlockX()) {
			finish();
		}
	}

	@SuppressWarnings("deprecation")
	private final void nextBlock() {
		// Gets the location from the XYZ of the for loops.

		// Gets the block at the current location.
		Block block = tempLoc.getBlock();

		// Gets the value and ID of the block at the location.
		int id = block.getTypeId();
		byte data = block.getData();
		// Material type = block.getType();

		// If they are the same, skip writing. If it's -1 it means it is the first block so it
		// should skip writing. If it's 255, write it because that's the max value the byte
		// array can hold.
		// !(type.equals(Material.SIGN_POST) || type.equals(Material.WALL_SIGN))
		if (id == lastId && data == lastData && count != 255 && id != wallSignID && id != signPostID) {

			// This means that the block repeated itself, so it keeps track of it, rather than
			// writing it every time. This is to compress the output file.
			count++;

		} else {
			try {
				writeIDsToFile(id, data, block);
			} catch (IOException e) {
				e.printStackTrace();
				copy.end(EndStatus.FAIL_IO_ERROR);

			}
		}

		updateVariables();

		// Sets the Last ints to last used ones so that it can check
		// if the new block is the same as the last one.
		lastId = id;
		lastData = data;

	}

	private final void writeIDsToFile(int id, int data, Block block) throws IOException {
		// Write the count of the previous block down.
		if (count != 0) {
			out.write(count);
		}

		// Write the ID of the new block.
		out.write(id);

		// Checks if it is a sign.
		if (id == wallSignID || id == signPostID) {
			// If it is a sign, write the data (Direction it is facing in a
			// sign's case)
			out.write(data);
			// Write 0 to signify the start of a line
			out.write(0);
			for (int i = 0; i < 4; i++) {
				// Write the line to the file.
				String signText = ((Sign) block.getState()).getLine(i);
				for (int indx = 0; indx < signText.length(); indx++) {
					out.write(signText.charAt(indx));
				}
				// Write 0 to signify the end of the line and start of a new
				// one.
				out.write(0);
			}
			count = 0;
		} else {
			// Skip writing the ID if the block doesn't need the ID saved to
			// save an average of about 15% of the data.
			if (!isSimple(id, simpleBlocks)) {
				out.write(data);
			}
			// Sets count = to one to signify it is the first block in a
			// row.
			count = 1;
		}

	}

	private final void updateVariables() {
		// Updates variables.
		blockIndex++;
		tempLoc.setZ(tempLoc.getBlockZ() + 1);

		if (tempLoc.getBlockZ() > max.getBlockZ()) {
			tempLoc.setZ(min.getBlockZ());
			tempLoc.setY(tempLoc.getBlockY() + 1);
		}
		if (tempLoc.getBlockY() > max.getBlockY()) {
			tempLoc.setY(min.getBlockY());
			tempLoc.setX(tempLoc.getBlockX() + 1);

			checkChunks(tempLoc);
		}
	}

	private final void finish() {
		// Writes the count to finish the file.
		// Skips signs because signs don't compress.
		if (lastId != 63 && lastId != 68) {
			try {
				out.write(count);
			} catch (IOException e) {
				e.printStackTrace();
				copy.end(EndStatus.FAIL_IO_ERROR);
				return;
			}
		}

		copy.end(EndStatus.SUCCESS);
	}

	protected final void statusMessage(Location min, Location max, long index, long tick) {
		if (sender != null && tick % 100 == 0) {
			int sizeX = max.getBlockX() - min.getBlockX();
			int sizeY = max.getBlockY() - min.getBlockY();
			int sizeZ = max.getBlockZ() - min.getBlockZ();
			long maxBlocks = sizeX * sizeY;
			maxBlocks *= sizeZ;
			double percent = (index / (double) maxBlocks) * 100;
			sender.sendMessage(prefix + "Working on copy operation; " + new DecimalFormat("#.0").format(percent)
					+ "% done (" + index + "/" + maxBlocks + ")");
		}
	}
}