/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
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
	 * @return the instance of the paste operation that completed
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
