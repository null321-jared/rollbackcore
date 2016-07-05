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

import net.shadowxcraft.rollbackcore.Paste;

/**
 * Called when a Paste operation completes.
 */
public class PasteEndEvent extends RollbackEvent {
	private final Paste paste;
	private final int blocksChanged;

	public PasteEndEvent(Paste paste, long nanoSecondsTaken, int blocksChanged, EndStatus endStatus) {
		this.paste = paste;
		this.nanoSecondsTaken = nanoSecondsTaken;
		this.blocksChanged = blocksChanged;
		this.endStatus = endStatus;

		if (paste.sender != null)
			paste.sender.sendMessage(paste.prefix + "The paste operation " + endStatus.getDescription() + " Took "
					+ nanoSecondsTaken / 1000000000.0 + " seconds to rollback " + blocksChanged + " blocks.");
		Bukkit.getPluginManager().callEvent(this);
	}

	/**
	 * @return The instance of the paste operation that completed.
	 */
	public Paste getPaste() {
		return paste;
	}

	/**
	 * @return The number of blocks that were changed when pasting.
	 */
	public int getBlocksChanged() {
		return blocksChanged;
	}

}
