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
import org.bukkit.World;
import org.bukkit.command.CommandSender;

/**
 * Called when a watchdog region rollback operation completes.
 */
public class WorldRollbackEndEvent extends RollbackEvent {
	private final World world;
	private final String folder;

	public WorldRollbackEndEvent(World world, String folder, long nanosecondsTaken, EndStatus endStatus,
			CommandSender sender, String prefix) {
		this.nanoSecondsTaken = nanosecondsTaken;
		this.folder = folder;
		this.endStatus = endStatus;
		this.world = world;

		if (sender != null)
			sender.sendMessage(prefix + "The world rollback operation " + endStatus.getDescription() + " Took "
					+ nanoSecondsTaken / 1000000000.0 + " seconds.");

		Bukkit.getPluginManager().callEvent(this);
	}

	/**
	 * @return The region that the rollback ended in.
	 */
	public World getWorld() {
		return world;
	}

	public String getFolder() {
		return folder;
	}

}
