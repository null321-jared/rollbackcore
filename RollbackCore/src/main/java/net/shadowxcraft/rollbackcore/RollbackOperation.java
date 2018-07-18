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

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * The RollbackOperation class is a parent class for all of the copy/paste rollback operations. It
 * contains all of the things they share.
 * 
 * @author lizardfreak321
 * @since 2.0
 */
abstract class RollbackOperation implements Runnable {
	protected Location min; 			// The location of the min X Y and Z of the region.
	protected Location max;				// The location of the max X Y and Z of the region.
	public CommandSender sender;		// The optional sender of messages.
	public String prefix;				// The prefixes used by messages if sender is not null
	protected String fileName;			// The name and directory of the file.
	protected int taskID = -1;			// The ID of the task running for the operation.
	public static final int VERSION = 2;// The current version of the plugin stoage format.
	int lastChunkX;						// To keep track of when to unload the row of chunks.

	// Storing the following IDs provides about a 15% increase in performance
	// than comparing the materials.
	@SuppressWarnings("deprecation")
	static final int wallSignID = Material.WALL_SIGN.getId();
	@SuppressWarnings("deprecation")
	static final int signPostID = Material.SIGN.getId();

	/**
	 * The list of blocks that the plugin skipped saving the data for in file format 1 for backwards
	 * compatibility. It is updated to Minecraft 1.9 with some missing blocks. Must be sorted.
	 */
	final static int[] version1Blocks = { 0, 2, 4, 7, 13, 14, 15, 16, 20, 21, 22, 23, 30, 37, 39, 40, 41, 42, 45, 46,
			47, 48, 49, 51, 52, 56, 57, 58, 70, 72, 73, 74, 79, 80, 81, 82, 83, 84, 87, 88, 89, 101, 102, 103, 112, 113,
			116, 117, 118, 121, 122, 123, 124, 129, 133, 137, 138, 147, 148, 152, 153, 165, 166, 169, 172, 173, 174,
			188, 189, 190, 191, 192, 201, 202, 206 };

	/**
	 * List of blocks that the plugin will skip in the newest version of the plugin. Currently
	 * updated to Minecaft 1.10 Must be sorted.
	 * 
	 * @since 2.0
	 */
	final static int[] simpleBlocks = { 0, 2, 4, 7, 13, 14, 15, 16, 20, 21, 22, 23, 30, 37, 39, 40, 41, 42, 45, 47, 48,
			49, 51, 52, 56, 57, 58, 70, 72, 73, 74, 79, 80, 81, 82, 83, 84, 87, 88, 89, 101, 102, 103, 112, 113, 116,
			117, 118, 121, 122, 123, 124, 129, 133, 137, 138, 147, 148, 152, 153, 165, 166, 169, 172, 173, 174, 188,
			189, 190, 191, 192, 201, 202, 206, 208, 209, 213, 214, 215 };

	/**
	 * This method returns if the block is "simple", meaning its data value doesn't need to be
	 * stored. For internal use only.
	 * 
	 * @param id
	 *            The ID of the block being tested for the simple property.
	 * @param simpleBlocks
	 *            The list of characters (Used instead of bytes since chars are unsigned) that are
	 *            considered "Simple"
	 * @return If the block is simple
	 */
	protected static final boolean isSimple(int id, int[] simpleBlocks) {
		// Binary search is very efficient, but it must be sorted.
		return Arrays.binarySearch(simpleBlocks, id) >= 0;
	}

	// Used to check the chunk at that no longer is being pasted in, and unloads
	// it to save RAM if no players are in it.
	protected static final void checkChunk(World world, int x, int z) {
		Chunk chunk = new Location(world, x - 1, 0, z).getChunk();
		safeUnloadChunk(chunk);
	}

	// Used to check the chunk at that no longer is being pasted in, and unloads
	// it to save RAM if no players are in it.
	protected static final void safeUnloadChunk(Chunk chunk) {
		if (!playersNearBy(chunk)) {
			chunk.unload(true);
		}
	}

	// Checks if the chunks need unloading. Unloads them if they do.
	protected final void checkChunks(Location currentLoc) {
		// Gets the current chunk all of the way to the min Z value.
		int currentChunkX = currentLoc.getChunk().getX();

		// If they aren't the same as last time, unloads the entire row.
		if (lastChunkX != currentChunkX) {
			lastChunkX = currentChunkX;
			for (int zIndex = min.getBlockZ(); zIndex < max.getBlockZ(); zIndex += 16) {
				checkChunk(min.getWorld(), currentLoc.getBlockX(), zIndex);
			}
			checkChunk(min.getWorld(), currentLoc.getBlockX(), max.getBlockZ());
		}
	}

	protected static final boolean playersNearBy(Chunk chunk) {
		int chunkX = chunk.getX();
		int chunkZ = chunk.getZ();
		int distance = Bukkit.getViewDistance() + 1;
		Chunk playerChunk;

		List<Player> onlinePlayers = chunk.getWorld().getPlayers();

		for (Player p : onlinePlayers) {
			playerChunk = p.getLocation().getChunk();
			if (Math.abs(playerChunk.getX() - chunkX) < distance && Math.abs(playerChunk.getZ() - chunkZ) < distance) {
				return true;
			}
		}
		return false;
	}
}
