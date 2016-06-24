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
