/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
*/

package net.shadowxcraft.rollbackcore.events;

import org.bukkit.Bukkit;

import net.shadowxcraft.rollbackcore.WatchDogRegion;

/**
 * Called when a watchdog region rollback operation completes.
 */
public class WDRollbackEndEvent extends RollbackEvent {
	private final WatchDogRegion wd;
	private final int blocksChanged;

	public WDRollbackEndEvent(WatchDogRegion wd, long nanoSecondsTaken, int blocksChanged, EndStatus endStatus) {
		this.wd = wd;
		this.nanoSecondsTaken = nanoSecondsTaken;
		this.blocksChanged = blocksChanged;
		this.endStatus = endStatus;

		Bukkit.getPluginManager().callEvent(this);
	}

	/**
	 * @return the region that the rollback ended in.
	 */
	public WatchDogRegion getWatchDog() {
		return wd;
	}

	/**
	 * @return The number of blocks changed in the rollback.
	 */
	public int getBlocksChanged() {
		return blocksChanged;
	}

}
