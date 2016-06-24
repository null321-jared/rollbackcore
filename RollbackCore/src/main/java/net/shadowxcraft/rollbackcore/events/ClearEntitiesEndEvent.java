/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
*/

package net.shadowxcraft.rollbackcore.events;

import org.bukkit.Bukkit;

import net.shadowxcraft.rollbackcore.ClearEntities;

/**
 * Called when a ClearEntities operation completes.
 */
public class ClearEntitiesEndEvent extends RollbackEvent {
	ClearEntities clearEntities;

	public ClearEntitiesEndEvent(ClearEntities clearEntities, long nanoSecondsTaken, EndStatus endStatus) {
		this.nanoSecondsTaken = nanoSecondsTaken;
		this.endStatus = endStatus;
		this.clearEntities = clearEntities;

		Bukkit.getPluginManager().callEvent(this);
	}

	/**
	 * @return The ClearEntities object that complteed the operation.
	 */
	public ClearEntities getClearEntitiesObject() {
		return clearEntities;
	}
}
