/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
*/

package net.shadowxcraft.rollbackcore.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class RollbackEvent extends Event {

	protected long nanoSecondsTaken;
	protected EndStatus endStatus;

	/**
	 * @return the number of nanoseconds the operation was running for.
	 */
	public long getNanoSecondsTaken() {
		return nanoSecondsTaken;
	}

	/**
	 * @return The end status.
	 */
	public EndStatus endStatus() {
		return endStatus;
	}

	@Override
	public HandlerList getHandlers() {
		return new HandlerList();
	}
}
