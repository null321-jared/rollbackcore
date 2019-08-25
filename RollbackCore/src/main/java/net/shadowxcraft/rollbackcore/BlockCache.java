package net.shadowxcraft.rollbackcore;

class BlockCache<E> {
	boolean hasExtraData;
	E data;
	int id;

	public BlockCache(int id, boolean hasExtraData, E data) {
		this.id = id;
		this.hasExtraData = hasExtraData;
		this.data = data;
	}
}