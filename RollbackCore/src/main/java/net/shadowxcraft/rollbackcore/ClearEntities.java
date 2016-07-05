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

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import net.shadowxcraft.rollbackcore.events.ClearEntitiesEndEvent;
import net.shadowxcraft.rollbackcore.events.EndStatus;

/**
 * This class is used to clear entities in a region.
 * 
 * @see ClearEntitiesEndEvent
 * @author lizardfreak321
 */
public class ClearEntities {
	public final Location min;
	public final Location max;
	private List<EntityType> allowedEntities;
	private long startclearTime;
	boolean quick;
	ClearEntitiesTask task;

	/**
	 * The constructor for the ClearEntities class, used to clear entities in a region.
	 * 
	 * @param min
	 *            The min X, Y, and Z of the region.
	 * @param max
	 *            The min X, Y, and Z of the region.
	 * @param allowedEntities
	 *            A lof of the entities that won't get cleared. Null for default.
	 * @param quick
	 *            If the clear will be quick. A quick clear only clears loaded chunks in the region.
	 * @throws IllegalArgumentException
	 *             If the worlds are not the same.
	 */
	public ClearEntities(Location min, Location max, List<EntityType> allowedEntities, boolean quick)
			throws IllegalArgumentException {

		if (!min.getWorld().equals(max.getWorld())) {
			throw new IllegalArgumentException("The worlds must be the same!");
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

		this.min = min;
		this.max = max;
		this.allowedEntities = allowedEntities;
		this.quick = quick;

		// Default entities that will not be cleared.
		if (this.allowedEntities == null) {
			this.allowedEntities = new ArrayList<EntityType>(5);
			try {
				this.allowedEntities.add(EntityType.PLAYER);
				this.allowedEntities.add(EntityType.ENDER_CRYSTAL);
				this.allowedEntities.add(EntityType.PAINTING);
				this.allowedEntities.add(EntityType.LEASH_HITCH);
				this.allowedEntities.add(EntityType.ARMOR_STAND);
			} catch (NoSuchFieldError e) {
				Main.plugin.getLogger().warning("Incompatible entity type: " + e.getLocalizedMessage());
			}
		}
	}

	/**
	 * Starts the clear entities operation.
	 * 
	 * @return The ClearEntities object being operated on.
	 */
	public final ClearEntities progressiveClearEntities() {
		TaskManager.addTask(); // To manage timings.
		startclearTime = System.nanoTime();
		this.task = new ClearEntitiesTask(min, max, quick, allowedEntities, this);
		this.task.runTaskTimer(Main.plugin, 1, 1);
		return this;
	}

	/**
	 * @return The nano-time of when the operation started.
	 */
	public long getStartClearTime() {
		return startclearTime;
	}

	public boolean isDone() {
		if (task == null)
			return false;
		else
			return task.isDone();
	}
}

/**
 * @author lizardfreak321
 */
class ClearEntitiesTask extends BukkitRunnable {
	Location min;
	Location max;
	int tempX;
	int tempZ;
	int index = 0;
	Chunk loadedChunks[] = null;
	boolean quick;
	List<EntityType> allowedEntities;
	ClearEntities parentTask;
	final private int CHUNK_SIZE = 16;
	private boolean isDone = false;

	public ClearEntitiesTask(Location min, Location max, boolean quick, List<EntityType> allowedEntities,
			ClearEntities parentTask) {
		this.min = min;
		this.max = max;
		this.quick = quick;
		tempX = min.getBlockX();
		tempZ = min.getBlockZ();
		this.allowedEntities = allowedEntities;
		this.parentTask = parentTask;
	}

	@Override
	public void run() {
		long startTime = System.nanoTime();
		boolean skip = false;

		if (quick && loadedChunks == null)
			loadedChunks = min.getWorld().getLoadedChunks();
		// Quick loop
		while (quick && index < loadedChunks.length
				&& System.nanoTime() - startTime < TaskManager.getMaxTime() * 1000000) {
			clearEntitiesInChunk(loadedChunks[index]);
			index++;
		}

		// Full loop
		while (!quick && tempX <= max.getBlockX() && !skip && allowedEntities != null) {
			fullClear();

			// Checks if it has run out of time
			if (System.nanoTime() - startTime > TaskManager.getMaxTime() * 1000000) {
				skip = true;
			} else {
				skip = false;
			}
		}

		// If this is true, it means it is done.
		if (tempX > max.getBlockX() || (quick && index >= loadedChunks.length)) {
			this.cancel();
			TaskManager.removeTask();
			isDone = true;
			new ClearEntitiesEndEvent(parentTask, System.nanoTime() - parentTask.getStartClearTime(),
					EndStatus.SUCCESS);
		}
	}

	private void fullClear() {
		Location location = new Location(min.getWorld(), tempX, 0, tempZ);
		// Gets the chunk at that location.
		Chunk chunk = location.getChunk();

		// Clears all of its entities.
		clearEntitiesInChunk(chunk);

		// Unloads that chunk to save RAM.
		RollbackOperation.safeUnloadChunk(chunk);

		// Updates X and Z.
		tempZ += CHUNK_SIZE;

		if (tempZ > max.getBlockZ()) {
			tempZ = min.getBlockZ();
			tempX += CHUNK_SIZE;
		}
	}

	// Clears all of the non-whitelisted entities in the chunk and in the region.
	private void clearEntitiesInChunk(Chunk chunk) {
		for (Entity entity : chunk.getEntities()) {
			if (Utilities.isInRegion(entity.getLocation(), min, max)) {
				EntityType type = entity.getType();
				if (!allowedEntities.contains(type)) {
					entity.remove();
				}
			}
		}
	}

	public boolean isDone() {
		return isDone;
	}

}
