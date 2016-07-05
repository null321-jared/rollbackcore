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
	 * @return The instance of the copy operation that completed.
	 */
	public Copy getCopy() {
		return copy;
	}

}
