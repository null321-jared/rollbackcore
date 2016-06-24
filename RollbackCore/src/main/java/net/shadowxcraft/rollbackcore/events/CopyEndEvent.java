/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
*/

package net.shadowxcraft.rollbackcore.events;

import org.bukkit.Bukkit;

import net.shadowxcraft.rollbackcore.Copy;

/**
 * Called when a Copy operation completes.
 */
public class CopyEndEvent extends RollbackEvent {
	private final Copy copy;

	public CopyEndEvent(Copy copy, long nanoSecondsTaken, EndStatus endStatus) {
		this.copy = copy;
		this.nanoSecondsTaken = nanoSecondsTaken;
		this.endStatus = endStatus;
		if (copy.sender != null)
			copy.sender.sendMessage(copy.prefix + "The copy operation " + endStatus.getDescription() + " Took "
					+ nanoSecondsTaken / 1000000000.0 + " seconds.");
		Bukkit.getPluginManager().callEvent(this);
	}

	/**
	 * @return the instance of the copy operation that completed
	 */
	public Copy getCopy() {
		return copy;
	}

}
