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
import org.bukkit.command.CommandSender;

import net.shadowxcraft.rollbackcore.Main;
import net.shadowxcraft.rollbackcore.WatchDogRegion;

/**
 * Called when a watchdog region import operation completes.
 */
public class WDImportEndEvent extends RollbackEvent {
	private final WatchDogRegion wd;
	private final int blocksImported;

	public WDImportEndEvent(WatchDogRegion wd, long nanoSecondsTaken, int blocksImported, EndStatus endStatus, CommandSender sender) {
		this.wd = wd;
		this.nanoSecondsTaken = nanoSecondsTaken;
		this.blocksImported = blocksImported;
		this.endStatus = endStatus;
		String prefix = Main.prefix;
		if(wd != null)
			prefix = wd.getPrefix();
		
		Bukkit.getPluginManager().callEvent(this);
		
		if(sender != null)
			sender.sendMessage(prefix + "The import operation " + endStatus.getDescription() + " Took "
					+ nanoSecondsTaken / 1000000000.0 + " seconds to import " + blocksImported + " blocks.");

	}

	/**
	 * @return The region that the blocks were imported in to.
	 */
	public WatchDogRegion getWatchDog() {
		return wd;
	}

	/**
	 * @return The number of blocks imported to the region.
	 */
	public int getBlocksImported() {
		return blocksImported;
	}

}
