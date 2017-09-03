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

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
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
		if (!event.isCancelled() && WatchDogRegion.hasActiveRegion()) {
			WatchDogRegion.logBlock(event.getBlock());

			for (Block block : event.getBlocks()) {
				WatchDogRegion.logBlock(block);
				WatchDogRegion.logBlock(block.getRelative(event.getDirection()));
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
		WatchDogRegion.logBlock(event.getBlock());
		if (!event.isCancelled() && WatchDogRegion.hasActiveRegion()) {
			try {
				for (Block block : event.getBlocks()) {
					WatchDogRegion.logBlock(block);
					WatchDogRegion.logBlock(block.getRelative(event.getDirection()));
				}
			} catch (NoSuchMethodError error) {
				Block block = event.getBlock().getRelative(event.getDirection());
				WatchDogRegion.logBlock(block);
				block = block.getRelative(event.getDirection());
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
