/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
*/

package net.shadowxcraft.rollbackcore;

import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * This class contains all of the listeners to things that can change a block.
 * 
 * @author lizardfreak321
 */
public class BukkitListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBreakEvent(BlockBreakEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBurnEvent(BlockBurnEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityExplodeEvent(EntityExplodeEvent event) {
		if (!event.isCancelled()) {
			for (Block block : event.blockList())
				WatchDogRegion.logBlock(block);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockFadeEvent(BlockFadeEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockFormEvent(BlockFormEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockFromToEvent(BlockFromToEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getToBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockGrowEvent(BlockGrowEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockIgniteEvent(BlockIgniteEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
		if (!event.isCancelled()) {
			for (Block block : event.getBlocks()) {
				WatchDogRegion.logBlock(block);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
		if (!event.isCancelled()) {
			for (Block block : event.getBlocks()) {
				WatchDogRegion.logBlock(block);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockSpreadEvent(BlockSpreadEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityBlockFormEvent(EntityBlockFormEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onLeavesDecayEvent(LeavesDecayEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlockReplacedState());
		}
	}

	// Events that should be taken care of elsewhere, but for some reason are
	// not.
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!event.isCancelled()) {
			Material mt = event.getMaterial();
			if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
				if (mt.equals(Material.WATER_BUCKET) || mt.equals(Material.LAVA_BUCKET)
						|| mt.equals(Material.FIREWORK_CHARGE) || mt.equals(Material.FLINT_AND_STEEL)) {
					WatchDogRegion.logBlock(event.getClickedBlock().getRelative(event.getBlockFace()));
				}
				if (event.getClickedBlock().getType().equals(Material.TNT)) {
					WatchDogRegion.logBlock(event.getClickedBlock());
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityChangeBlockEvent(EntityChangeBlockEvent event) {
		if (!event.isCancelled()) {
			WatchDogRegion.logBlock(event.getBlock());
		}
	}

}
