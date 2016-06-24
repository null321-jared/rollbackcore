/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
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
