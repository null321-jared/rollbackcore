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
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;

import com.google.common.base.Charsets;

import net.shadowxcraft.rollbackcore.events.CopyEndEvent;
import net.shadowxcraft.rollbackcore.events.EndStatus;

/**
 * This class is used to save a region to a file that can be pasted later to rollback the region.
 * 
 * @see CopyEndEvent
 * @author lizardfreak321
 */
public class Copy extends RollbackOperation {

	protected Map<String, Integer> idMapping = new HashMap<String, Integer>();
	private OutputStream out, rawOut;
	private File tempFile;
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
	 * @param min
	 *            The location that contains data for the min location of the region.
	 * @param max
	 *            The location that contains data for the max location of the region.
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
		if (!fileName.contains(".")) {
			fileName += ".dat";
			if (!fileName.contains("/") || !fileName.contains("\\")) {
				fileName = Main.plugin.getDataFolder().getAbsolutePath() + "/saves/" + fileName;
			}
		}
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

		if (!startFile(out))
			return false;

		CopyTask task = new CopyTask(min, max, out, this, sender, prefix);

		runningCopies.add(this);
		TaskManager.addTask();
		taskID = Main.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(Main.plugin, task, 1, 1);
		return true;
	}

	// Prepares the file and stream.
	private final boolean initializeStream() {
		// Initializes the file
		tempFile = new File(fileName);
		if (tempFile.exists()) {
			// Deletes it if it exists so it starts over.
			tempFile.delete();
		}

		// Creates the file.
		try {
			tempFile.createNewFile();
		} catch (IOException e) {
			System.out.print("Path: " + tempFile.getAbsolutePath());
			e.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}

		// Initializes the FileOutputStream.
		try {
			rawOut = new FileOutputStream(tempFile);
			out = new BufferedOutputStream(rawOut);
		} catch (IOException e) {
			e.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}
		return true;
	}

	// Writes the initial data- Version, blocks, and size.
	private final boolean startFile(OutputStream out) {
		// Writes the version so that the plugin can convert/reject incompatible
		// versions.
		try {
			out.write(VERSION);

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
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
	final OutputStream out;		// Used to read from the file.
	long tick = 0;		// Used to keep track of how many ticks the copy operation has run.
	long blockIndex = 0;// Used to store the index of the block, for statistical reasons.
	protected int index = 1; // Stores the next index for the item IDs in the palate.
	protected int count = 0; // Stores the number of blocks in a row
	protected String lastString = null; // Stores the string representation of the previous block.

	public CopyTask(Location min, Location max, OutputStream out, Copy copy, CommandSender sender, String prefix) {
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

	private final void nextBlock() {
		// Gets the location from the XYZ of the for loops.

		// Gets the block at the current location.
		Block block = tempLoc.getBlock();

		// Gets the value and ID of the block at the location.
		String data = block.getBlockData().getAsString();
		if (count < 255 && data.equals(lastString)) {
			count++; // Increments the counter
		} else {
			// Searches for existing representation of block.
			Integer id = copy.idMapping.putIfAbsent(data, index);
			try {
				if(count != 0)
					out.write(count); // Writes the last one's count
				count = 1;
				if (id == null) {
					// New block.
					// TODO: Support more than 255 IDs.
					// It was absent, so writes it to the file,
					// then increments the index.
					out.write(0); // Notes new data.
					out.write(data.length()); // Length of the data.
					out.write(data.getBytes(StandardCharsets.ISO_8859_1));

					out.write(index); // Now writes the index.
					index++; // Indexes it for the next one.
				} else {
					out.write(id.intValue()); // ID
				}
			} catch (IOException e) {
				e.printStackTrace();
				copy.end(EndStatus.FAIL_IO_ERROR);
			}
			lastString = data;
		}

		updateVariables();

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
		System.out.println("Number of unique blocks: " + index);
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