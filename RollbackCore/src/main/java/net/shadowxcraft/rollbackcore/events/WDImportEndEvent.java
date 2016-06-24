/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
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
	 * @return the region that the blocks were imported in to.
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
