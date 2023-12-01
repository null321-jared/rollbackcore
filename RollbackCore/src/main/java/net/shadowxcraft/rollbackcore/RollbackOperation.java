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

import java.util.EnumSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.type.HangingSign;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallHangingSign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

/**
 * The RollbackOperation class is a parent class for all of the copy/paste
 * rollback operations. It contains all of the things they share.
 * 
 * @author lizardfreak321
 * @since 2.0
 */
abstract class RollbackOperation implements Runnable {
	public static final String CURRENT_MC_VERSION;
	static final EnumSet<Material> specialBlockStates = EnumSet.of(Material.PLAYER_HEAD, Material.PLAYER_WALL_HEAD,
			Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK);

	protected World world;
	protected int minX, minY, minZ;
	protected int maxX, maxY, maxZ;
	long blockIndex = 0;
	
	protected int tempX, tempY, tempZ;
	public CommandSender sender; // The optional sender of messages.
	public String prefix; // The prefixes used by messages if sender is not null
	protected String fileName; // The name and directory of the file.
	protected BukkitTask task = null; // The ID of the task running for the operation.
	public static final int VERSION = 2;// The current version of the plugin storage format.
	private int lastChunkX, minChunkZ, lastChunkZ;// To keep track of when to unload the row of
													// chunks.
	private int zChunks;
	private boolean[] loadedChunks;
	private boolean writing;

	protected RollbackOperation(Location min, boolean writing) {
		this.world = min.getWorld();
		this.minX = min.getBlockX();
		this.minY = min.getBlockY();
		this.minZ = min.getBlockZ();
		this.tempX = minX;
		this.tempY = minY;
		this.tempZ = minZ;
		this.writing = writing;
	}

	protected void initChunkUnloading(int maxZ) {

		this.lastChunkX = minX >> 4; // Not one less so that it doesn't
										// think it must unload it all.
		this.minChunkZ = minZ >> 4; // One less so it knows to save it.
		this.lastChunkZ = this.minChunkZ - 1; // One less so it knows to save it.

		this.zChunks = (maxZ >> 4) - (minZ >> 4) + 1;

		loadedChunks = new boolean[this.zChunks];
	}

	/**
	 * Call this every time the rollback operation with iteration order z y x moves
	 * to the next location.
	 * 
	 * IMPORTANT: Call this BEFORE the chunk is accessed to ensure that it knows
	 * whether it was loaded beforehand.
	 * 
	 * This will store which ones were loaded beforehand, and it will unload the
	 * ones that are no longer being used by the operation.
	 * 
	 * @param tempX the X location of the block
	 * @param tempZ the Z location of the block
	 */
	protected void nextLocation(int tempX, int tempZ) {
		if (Config.unloadChunks) {
			int locChunkX = tempX >> 4;
			int locChunkZ = tempZ >> 4;
	
			if (locChunkX != this.lastChunkX) {
				for (int i = 0; i < this.zChunks; i++) {
					if (loadedChunks[i]) {
						// Was loaded.
						world.unloadChunkRequest(this.lastChunkX, this.minChunkZ + i);
					} else {
						// Was not loaded. Assume it is safe to do so, because
						// unless a player was moving really fast, the chances
						// of them getting closer to the chunk is very slim.
	
						// We can't use 'request' because in large tests, they
						// weren't actually unloaded, leading towards a crash.
						world.unloadChunk(this.lastChunkX, this.minChunkZ + i, writing);
					}
				}
			}
			if (locChunkZ > this.lastChunkZ || locChunkX > this.lastChunkX) {
				boolean chunkLoaded = world.isChunkLoaded(locChunkX, locChunkZ);
				loadedChunks[locChunkZ - this.minChunkZ] = chunkLoaded;
				this.lastChunkX = locChunkX;
				this.lastChunkZ = locChunkZ;
			}
		}
	}

	long lastIndex, lastTime;

	// Used to send status messages to the "sender" if the sender is not null.
	protected final void statusMessage(int maxX, int maxY, int maxZ, long currentIndex, long tick,
			String operationName) {
		if (sender != null && tick % 100 == 0) {
			long currentTime = System.nanoTime();
			int sizeX = maxX - minX + 1;
			int sizeY = maxY - minY + 1;
			int sizeZ = maxZ - minZ + 1;
			long maxBlocks = (long) sizeX * sizeY;
			maxBlocks *= sizeZ;

			// Calculate percentage remaining
			double percent = Math.round((double) 10000 * (currentIndex / (double) maxBlocks)) / 100;

			// Calculate the rate of progress
			double blocksPerSecond = (1000000000 * (currentIndex - lastIndex)) / (currentTime - lastTime);

			// Format it for the user
			String detailedProgress = "(" + currentIndex + "/" + maxBlocks + ", " + (int) Math.round(blocksPerSecond)
					+ " blocks/second).";
			int estimatedTimeLeft = (int) Math.round(100 * ((maxBlocks - currentIndex) / blocksPerSecond)) / 100;

			String estimatedTimeLeftString = "";
			if (estimatedTimeLeft > 0) {
				estimatedTimeLeftString = "Estimated time left: ";
				if (estimatedTimeLeft >= 60) {
					estimatedTimeLeftString += (int) (estimatedTimeLeft / 60) + " minutes ";
				} else {
					estimatedTimeLeftString += (estimatedTimeLeft % 60) + " seconds.";
				}
			}

			// Send to player
			sender.sendMessage(prefix + "Working on " + operationName + " operation: " + percent + "% done "
					+ detailedProgress + " " + estimatedTimeLeftString);

			// Update variables to keep track.
			lastIndex = currentIndex;
			lastTime = currentTime;
		}
	}

	public Location getMin() {
		return new Location(world, minX, minY, minZ);
	}

	public String getFileName() {
		return fileName;
	}

	protected static boolean isSign(Material material) {
		Class<?> materialClass = material.data;
		return materialClass == Sign.class || materialClass == WallSign.class ||
				materialClass == HangingSign.class || materialClass == WallHangingSign.class;
	}

	static {
		if (isJUnitTest()) {
			CURRENT_MC_VERSION = "no-version-unittest";
		} else {
			String bukkitVersion = Bukkit.getVersion();
			CURRENT_MC_VERSION = bukkitVersion.substring(bukkitVersion.lastIndexOf(' ') + 1, bukkitVersion.indexOf(')'));
		}
		// Loop through all materials, and add ones that are signs.
		for (Material type : Material.values()) {
			if (isSign(type)) {
				specialBlockStates.add(type);
			}
		}
	}

	public static boolean isJUnitTest() {
		for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
			if (element.getClassName().startsWith("org.junit.")) {
				return true;
			}
		}
		return false;
	}

	protected void updateVariables() {
		// Updates variables.
		blockIndex++;
		updateRaster();
	}

	private void updateRaster() {
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
