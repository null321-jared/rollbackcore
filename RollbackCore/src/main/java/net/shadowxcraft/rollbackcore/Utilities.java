/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
*/

package net.shadowxcraft.rollbackcore;

import org.bukkit.Location;

public class Utilities {
	/**
	 * Used to check if the location is in the region.
	 * 
	 * @param loc
	 *            The location being testes.
	 * @param min
	 *            The min location based on X-Y-Z of the region.
	 * @param max
	 *            The max location based on X-Y-Z of the region.
	 * @return If the location is in the region.
	 */
	public final static boolean isInRegion(Location loc, Location min, Location max) {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		return (x >= min.getBlockX() && x <= max.getBlockX() && y >= min.getBlockY() && y <= max.getBlockY()
				&& z >= min.getBlockZ() && z <= max.getBlockZ() && loc.getWorld().equals(min.getWorld()));
	}

}
