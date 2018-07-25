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
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;

import net.shadowxcraft.rollbackcore.events.CopyEndEvent;
import net.shadowxcraft.rollbackcore.events.EndStatus;

/**
 * This class is used to save a region to a file that can be pasted later to
 * rollback the region.
 * 
 * @see CopyEndEvent
 * @author lizardfreak321
 */
public class Copy extends RollbackOperation {
	private int maxX, maxY, maxZ;
	private OutputStream out;
	private File tempFile;
	private long startTime = -1l, lastTime;
	private static final List<Copy> runningCopies = new ArrayList<Copy>();
	boolean inProgress = false;

	// Specific to the operation at hand.
	long tick = 0; // Used to keep track of how many ticks the copy operation has run.
	long blockIndex = 0, lastIndex = 0;// Used to store the index of the block, for statistical reasons.
	private LRUBlockDataCache cache = new LRUBlockDataCache(1, 255); // Stores IDs for the BlockData
	private int count = 0; // Stores the number of blocks in a row
	private int misses = 0; // Stores the number of times the data is written.
	private BlockData lastData = null; // Stores the string representation of the previous block.

	/**
	 * Used to schedule a copy. This is the legacy constructor. Used by the
	 * copyDistributed method.
	 * 
	 * @param minX     The X value of the minimum location of the region.
	 * @param minY     The Y value of the minimum location of the region.
	 * @param minZ     The Z value of the minimum location of the region.
	 * @param maxX     The X value of the maximum location of the region.
	 * @param maxY     The X value of the maximum location of the region.
	 * @param maxZ     The Y value of the maximum location of the region.
	 * @param world    The world that the region is in.
	 * @param fileName The fileName and directory of the folder that will contain
	 *                 the saved data. Recommended: Make a sub-folder in your
	 *                 Main.plugin and put them in there.
	 * @param sender   Where status messages will be sent. Null for no messages,
	 *                 consoleSender for console, and a player for a player.
	 */
	public Copy(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, World world, String fileName,
			CommandSender sender) {
		this(new Location(world, minX, minY, minZ), new Location(world, maxX, maxY, maxZ), fileName, sender,
				Main.prefix);
	}

	/**
	 * Used to schedule a copy.
	 * 
	 * @param min      The location that contains data for the min location of the
	 *                 region.
	 * @param max      The location that contains data for the max location of the
	 *                 region.
	 * @param fileName The fileName and directory of the folder that will contain
	 *                 the saved data. Recommended: Make a sub-folder in your
	 *                 Main.plugin and put them in there.
	 * @param sender   Where status messages will be sent. Null for no messages,
	 *                 consoleSender for console, and a player for a player.
	 * @param prefix   The prefix that will be used when sending messages to the
	 *                 sender.
	 */
	public Copy(Location min, Location max, String fileName, CommandSender sender, String prefix) {
		super(min);
		this.maxX = max.getBlockX();
		this.maxY = max.getBlockY();
		this.maxZ = max.getBlockZ();

		// Input validation and correction.
		if (!min.getWorld().equals(max.getWorld())) {
			max.setWorld(min.getWorld());
		}
		if (minX > maxX) {
			int oldMaxX = maxX;
			maxX = minX;
			minX = oldMaxX;
		}
		if (minY > maxY) {
			int oldMaxY = maxY;
			maxY = minY;
			minY = oldMaxY;
		}
		if (minZ > maxZ) {
			int oldMaxZ = maxZ;
			maxZ = minZ;
			minZ = oldMaxZ;
		}

		if (!fileName.contains(".")) {
			fileName += ".dat";
			if (!fileName.contains("/") || !fileName.contains("\\")) {
				fileName = Main.plugin.getDataFolder().getAbsolutePath() + "/saves/" + fileName;
			}
		}
		this.fileName = fileName;
		this.sender = sender;
		this.prefix = prefix;

		tempX = min.getBlockX();
		tempY = min.getBlockY();
		tempZ = min.getBlockZ();

		initChunkUnloading(maxZ);
	}

	/**
	 * Cancels all of the running copy operations.
	 * 
	 * @return The number of operations cancelled.
	 */
	public static final int cancelAll() {
		int numberOfTasks = runningCopies.size();
		for (int i = 0; i < numberOfTasks; i++) {
			runningCopies.get(i).end(EndStatus.FAIL_EXERNAL_TERMINATION);
		}
		return numberOfTasks;
	}

	/**
	 * Runs the copy operation.
	 */
	@Override
	public final void run() {
		// Calls the copy method.
		if (!inProgress) {
			initCopy();
		}
		copyTask();
	}

	// The internal method to start the copy operation.
	protected final boolean initCopy() {
		startTime = System.nanoTime();
		lastTime = startTime;
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

		runningCopies.add(this);
		TaskManager.addTask();
		task = runTaskTimer(Main.plugin, 1, 1);
		inProgress = true;
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
			out = new BufferedOutputStream(new FileOutputStream(tempFile));
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
			FileUtilities.writeShort(out, maxX - minX);
			FileUtilities.writeShort(out, maxY - minY);
			FileUtilities.writeShort(out, maxZ - minZ);
		} catch (IOException e1) {
			e1.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}
		return true;
	}

	/**
	 * Forcefully ends the operation. Sets everything back to the way it should be
	 * and closes open resources.
	 */
	public final void kill() {
		end(EndStatus.FAIL_EXERNAL_TERMINATION);
	}

	// Ends it with that end status.
	private final void end(EndStatus endStatus) {
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
		if (task != null && !task.isCancelled()) {
			cancel();
			task = null;
		}

		new CopyEndEvent(this, System.nanoTime() - startTime, endStatus);
	}

	// ------------- Task ------------

	private void copyTask() {
		long startTime = System.currentTimeMillis(); // Used to keep track of time.
		tick++; // Increments the tick variable.

		boolean skip = false; // To know when to quit the loop for that tick.

		while (!skip && inProgress) {
			for (int i = 0; inProgress && i < 500; i++) {
				nextBlock();
			}
			// Checks if it has run out of time.
			if (System.currentTimeMillis() - startTime > TaskManager.getMaxTime()) {
				skip = true;
			} else {
				skip = false;
			}
		}

		statusMessage();
		if (tempX > maxX) {
			Main.plugin.getLogger().info("Number of cache misses blocks: " + misses);
			end(EndStatus.SUCCESS);
		}
	}

	private final void statusMessage() {
		if (sender != null && tick % 100 == 0) {
			long currentTime = System.nanoTime();
			int sizeX = maxX - minX;
			int sizeY = maxY - minY;
			int sizeZ = maxZ - minZ;
			long maxBlocks = sizeX * sizeY;
			maxBlocks *= sizeZ;
			double percent = (blockIndex / (double) maxBlocks) * 100;
			sender.sendMessage(prefix + "Working on copy operation; " + new DecimalFormat("#.0").format(percent)
					+ "% done (" + blockIndex + "/" + maxBlocks + ", "
					+ ((1000000000 * (blockIndex - lastIndex)) / (currentTime - lastTime)) + " blocks/second)");

			lastIndex = blockIndex;
			lastTime = currentTime;
		}
	}

	private final void nextBlock() {
		if (tempY == minY) { // For efficiency.
			nextLocation(tempX, tempZ);
		}

		// Gets the block at the current location.
		Block block = world.getBlockAt(tempX, tempY, tempZ);

		// Gets the value and ID of the block at the location.
		BlockData data = block.getBlockData();

		// Even with the null check, it appears that
		if (count < 65280 && lastData != null && data.hashCode() == lastData.hashCode()) {
			count++; // Increments the counter
		} else {
			// Searches for existing representation of block.
			int id = cache.get(data);
			try {
				if (count <= 255)
					out.write(count); // Writes the last one's count
				else {
					out.write(0);
					FileUtilities.writeShort(out, count);
				}
				count = 1;
				if (id == -1) {
					id = cache.add(data);
					misses++;
					// New block.

					// It was absent, so writes it to the file,
					// then increments the index.
					String dataAsString = data.getAsString();
					out.write(0); // Notes new data.
					out.write(dataAsString.length()); // Length of the data.
					out.write(dataAsString.getBytes(StandardCharsets.ISO_8859_1));

					out.write(id);
				} else {
					out.write(id); // Now writes the index.
				}
			} catch (IOException e) {
				e.printStackTrace();
				end(EndStatus.FAIL_IO_ERROR);
			}
			lastData = data;
		}

		updateVariables();

	}

	private final void updateVariables() {
		// Updates variables.
		blockIndex++;
		// Move Z
		tempZ++;

		// Wrap Z
		if (tempZ > maxZ) {
			tempZ = minZ;
			// Move Y
			tempY++;

			// Wrap Y
			if (tempY > maxY) {
				tempY = minY;

				// Move X - does not need wrapping
				tempX += 1;
			}
		}
	}

}