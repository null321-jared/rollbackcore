package net.shadowxcraft.rollbackcore;

import org.bukkit.block.data.BlockData;

class BlockCache {
	boolean hasExtraData;
	BlockData data;
	int id;

	public BlockCache(int id, boolean hasExtraData, BlockData data) {
		this.id = id;
		this.hasExtraData = hasExtraData;
		this.data = data;
	}
}