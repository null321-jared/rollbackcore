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
	 * @return The region that the rollback ended in.
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
