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
