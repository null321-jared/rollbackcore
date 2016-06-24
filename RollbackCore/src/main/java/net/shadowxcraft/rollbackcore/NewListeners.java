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

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;

/**
 * This class contains all of the listeners to things that can change a block that are incompatible
 * with older versions of Bukkit, so if these fail, the others work.
 */
public class NewListeners implements Listener {
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockExplodeEvent(BlockExplodeEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockMultiPlaceEvent(BlockMultiPlaceEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlockPlaced());
			WatchDogRegion.logBlock(event.getBlockAgainst());
		}
	}

}
