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
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;

import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Factory;
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
	private File file;
	private long startTime = -1l;
	static final List<Copy> runningCopies = new ArrayList<Copy>();
	boolean inProgress = false;

	// Specific to the operation at hand.
	long tick = 0; // Used to keep track of how many ticks the copy operation has run.
	long blockIndex = 0, lastIndex = 0;// Used to store the index of the block, for statistical
										// reasons.
	private LRUCache<BlockData> cache = new LRUCache<BlockData>(1, 255); // Stores IDs for the BlockData
	private int count = 0; // Stores the number of blocks in a row
	private LRUCache<BlockData>.Node lastData = null; // Stores the string representation of the
														// previous block.

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
		super(validateLocations(min, max), false);
		this.maxX = max.getBlockX();
		this.maxY = max.getBlockY();
		this.maxZ = max.getBlockZ();

		if (!fileName.contains(".")) {
			fileName += ".dat";
			if (!fileName.contains("/") || !fileName.contains("\\")) {
				fileName = Main.savesPath.toString() + "/" +fileName;
			}
		}
		this.fileName = fileName;
		this.sender = sender;
		this.prefix = prefix;

		initChunkUnloading(maxZ);
	}

	/**
	 * Properly validates the input min max locations.
	 * 
	 * @return The min location
	 */
	private static Location validateLocations(Location min, Location max) {
		// Input validation and correction.
		if (!min.getWorld().equals(max.getWorld())) {
			max.setWorld(min.getWorld());
		}
		if (min.getX() > max.getX()) {
			double oldMaxX = max.getX();
			max.setX(min.getX());
			min.setX(oldMaxX);
		}
		if (min.getY() > max.getY()) {
			double oldMaxY = max.getY();
			max.setY(min.getY());
			min.setY(oldMaxY);
		}
		if (min.getZ() > max.getZ()) {
			double oldMaxZ = max.getZ();
			max.setZ(min.getZ());
			min.setZ(oldMaxZ);
		}
		return min;
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

		try {
			out = startFile(out, maxX - minX, maxY - minY, maxZ - minZ, CURRENT_MC_VERSION);
		} catch (IllegalArgumentException | IOException e) {
			end(EndStatus.FAIL_IO_ERROR);
			e.printStackTrace();
		}

		runningCopies.add(this);
		task = Bukkit.getScheduler().runTaskTimer(Main.plugin, this, 1, 1);
		inProgress = true;
		return true;
	}

	// Prepares the file and stream.
	private final boolean initializeStream() {
		// Initializes the file
		file = new File(fileName);
		if (file.exists()) {
			// Deletes it if it exists so it starts over.
			file.delete();
		}

		// Creates the file.
		try {
			File parent = file.getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}
			file.createNewFile();
		} catch (IOException e) {
			System.out.print("Path: " + file.getAbsolutePath());
			e.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}

		// Initializes the FileOutputStream.
		try {
			out = new FileOutputStream(file);
		} catch (IOException e) {
			e.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}
		return true;
	}

	// Writes the initial data- Version, blocks, and size.
	static final OutputStream startFile(OutputStream out, int diffX, int diffY, int diffZ, String mcVersion)
			throws IllegalArgumentException, IOException {
		// Writes the version so that the plugin can convert/reject incompatible
		// versions.
		out.write(VERSION);
		// Writes the min-max differences to the file using writeShort
		// because it can be larger than 255.
		FileUtilities.writeShort(out, diffX);
		FileUtilities.writeShort(out, diffY);
		FileUtilities.writeShort(out, diffZ);

		// After here, put as many non-empty strings as wanted, followed by a 0/null.
		// They should all be key, followed by value.

		// Key
		FileUtilities.writeShortString(out, "minecraft_version");
		// Value
		FileUtilities.writeShortString(out, mcVersion);

		// Key
		FileUtilities.writeShortString(out, "compression");
		// Value
		FileUtilities.writeShortString(out, Config.compressionType.name());

		out.write(0); // 0 - nothing left in the header. Start the actual data.

		switch (Config.compressionType) {
		default:
		case LZ4:
			out = new BufferedOutputStream(
					new LZ4BlockOutputStream(out, 1 << 16, LZ4Factory.safeInstance().highCompressor()));
			break;
		case NONE:
			out = new BufferedOutputStream(out);
			break;
		// out = new DeflaterOutputStream(out); // Corrupts the data- Appears to be a
		// java issue, or it does not like the fact that it is appended.
		// out = new DeflaterOutputStream(out, new
		// Deflater(Deflater.DEFAULT_COMPRESSION, true));
		// break;
		}

		return out;
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
		inProgress = false;

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
			task.cancel();
			task = null;
		}

		new CopyEndEvent(this, System.nanoTime() - startTime, endStatus);
	}

	// ------------- Task ------------

	private final void copyTask() {
		long startTime = System.currentTimeMillis(); // Used to keep track of time.
		tick++; // Increments the tick variable.

		boolean skip = false; // To know when to quit the loop for that tick.

		while (!skip && inProgress) {
			for (int i = 0; inProgress && i < 500; i++) {
				nextBlock();
				if (tempX > maxX) {
					try {
						writeCount();
					} catch (IOException e) {
						end(EndStatus.FAIL_IO_ERROR);
						e.printStackTrace();
					}
					if (inProgress)
						end(EndStatus.SUCCESS);
				}
			}
			// Checks if it has run out of time.
			if (System.currentTimeMillis() - startTime > TaskManager.getMaxTime()) {
				skip = true;
			}
		}

		statusMessage(maxX, maxY, maxZ, blockIndex, tick, "copy");
	}

	private final void nextBlock() {
		if (tempY == minY) { // For efficiency.
			nextLocation(tempX, tempZ);
		}

		// Gets the block at the current location.
		Block block = world.getBlockAt(tempX, tempY, tempZ);
		// boolean container = containers.contains(block.getType());

		// Gets the value and ID of the block at the location.
		BlockData data = block.getBlockData();

		// BEFORE: Took about 5 seconds for outset 200 region.
		// 6.4 seconds with material from the data.

		// Even with the null check, it appears that
		if (count > 0 && count < 65535 && lastData != null && data.equals(lastData.data)) {
			count++; // Increments the counter
		} else {
			// Searches for existing representation of block.
			LRUCache<BlockData>.Node id = cache.get(data);
			try {
				writeCount();
				if (id == null) {
					Material material = data.getMaterial();
					id = cache.add(data, specialBlockStates.contains(material));
					// New block.

					String dataAsString = data.getAsString();

					// It was absent, so writes it to the file,
					// then increments the index.

					// START WITH 0 - Notes new data.
					out.write(0);
					// Next write whether or not there was extra data.
					out.write(id.hasExtraData ? 1 : 0);
					// Write the string
					FileUtilities.writeShortString(out, dataAsString);
					// Write the id value of the data.
					out.write(id.value);
				} else {
					out.write(id.value); // Now writes the index.
				}
				if (id.hasExtraData) {
					count = 0;
					writeBlockState(out, block.getState());
				} else {
					count = 1; // 0 means no count will be written.
				}
			} catch (IOException e) {
				e.printStackTrace();
				end(EndStatus.FAIL_IO_ERROR);
			}
			lastData = id;
		}

		updateVariables();

	}

	static final void writeBlockState(OutputStream out, BlockState blockState) throws IOException {

		if (isSign(blockState.getType())) {
			Sign sign = (Sign) blockState;
			try {
				String allLines = sign.getLine(0);
				for (int i = 1; i < 4; i++) {
					allLines += '\n' + sign.getLine(i);
				}
				byte[] bytes = allLines.getBytes(StandardCharsets.UTF_8);
				FileUtilities.writeShort(out, bytes.length + 1);
				out.write(bytes.length); // assumed less than 256
				out.write(bytes);
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
				FileUtilities.writeShort(out, 0); // To prevent corruption of the output.
			}
		} else {
			switch (blockState.getType()) {
			case PLAYER_HEAD:
			case PLAYER_WALL_HEAD:
				Skull head = (Skull) blockState;
				if (head.hasOwner()) {
					byte[] bytes = head.getOwningPlayer().getUniqueId().toString().getBytes(StandardCharsets.UTF_8);
					// Plus two to account for the owner byte and the length byte
					FileUtilities.writeShort(out, bytes.length + 2);
					out.write(1); // 1 == has owner
					out.write(bytes.length); // assumed less than 256 because UUIDs are of fixed length.
					out.write(bytes);
				} else {
					FileUtilities.writeShort(out, 1);
					out.write(0); // 0 == has no owner
				}
				break;
			case COMMAND_BLOCK:
			case CHAIN_COMMAND_BLOCK:
			case REPEATING_COMMAND_BLOCK:
				CommandBlock cBlock = (CommandBlock) blockState;
				byte[] nameBytes = cBlock.getName().getBytes(StandardCharsets.UTF_8);
	
				byte[] commandBytes = cBlock.getCommand().getBytes(StandardCharsets.UTF_8);
	
				FileUtilities.writeShort(out, nameBytes.length + commandBytes.length + 4);
				FileUtilities.writeShort(out, nameBytes.length);
				out.write(nameBytes);
				FileUtilities.writeShort(out, commandBytes.length);
				out.write(commandBytes);
				break;
			default:
				Main.plugin.getLogger()
						.warning("Code requested writing of unknown blockstate: " + blockState.getType().toString());
				FileUtilities.writeShort(out, 0); // To prevent corruption of the output.
			}
		}
	}

	private final void writeCount() throws IOException {
		if (count != 0) {
			if (count <= 255) {
				out.write(count); // Writes the last one's count
			} else {
				out.write(0);
				FileUtilities.writeShort(out, count);
			}
		}
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