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

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class RollbackEvent extends Event {

	protected long nanoSecondsTaken;
	protected EndStatus endStatus;
    private static final HandlerList HANDLERS = new HandlerList();
	
	/**
	 * @return The number of nanoseconds the operation was running for.
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
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
