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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

/**
 * The rollback Utility class. Used to start some operations.
 * 
 * @author lizardfreak321
 */
public class Rollback {

	/**
	 * The max size of sub-regions in distributed mode.
	 */
	public static final short SIZE = 100;

	/*------------------------------------| Copy |------------------------------------*/

	/**
	 * Used to allow players to copy without specifying coordinates using worldedit.
	 * 
	 * @param player
	 *            The player who is copying
	 * @param name
	 *            The name and directory of the folder that will contain the saved data.
	 *            Recommended: Make a sub-folder in your Main.plugin and put them in there.
	 * @param addToConf
	 *            Used to specify if the copy should be added to the config (A world-copy). If true,
	 *            the name parameter gets overwritten.
	 * @see Copy
	 */
	public static final void copy(Player player, String name, boolean addToConf) {
		// Uses worldedit to get the player's region.
		WorldEditPlugin worldEditPlugin = null;
		worldEditPlugin = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		if (worldEditPlugin == null) {
			player.sendMessage(Main.prefix + "Error with region command! Error: WorldEdit is null.");
			return;
		}

		Selection sel = worldEditPlugin.getSelection(player);

		// Checks if they have a selection
		if (sel instanceof CuboidSelection) {
			Vector min = sel.getNativeMinimumPoint();
			Vector max = sel.getNativeMaximumPoint();

			// Used to add an arena to the config for integration with SG.
			if (addToConf) {
				new File("arenas").mkdir();
				name = "arenas/" + player.getWorld().getName();
				if (!Config.setArenaXYZ(player.getWorld().getName(), min.getBlockX(), min.getBlockY(),
						min.getBlockZ())) {
					// If this code gets executed, it was unable to save the
					// YAML.
					player.sendMessage(Main.prefix + ChatColor.DARK_RED + "Unable to add arena to config! Aborting.");
					return;
				}
			}

			// Copies the arena (distributed).
			// copyDistributed(min.getBlockX(), min.getBlockY(),
			// min.getBlockZ(), max.getBlockX(), max.getBlockY(),
			// max.getBlockZ(), player.getWorld(), name, player);
			new Copy(min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(),
					max.getBlockZ(), player.getWorld(), name, player).run();

			// Notify the player it's starting at those coordinates.
			player.sendMessage(
					Main.prefix + "Starting! " + min.getBlockX() + " " + min.getBlockY() + " " + min.getBlockZ());
		} else {
			// This means there was no selection, so it skips copying and tells
			// the player.
			player.sendMessage(ChatColor.DARK_RED + "Invalid Selection!");
		}
	}

	/**
	 * Used to copy the region specified in the parameters in a distributed way. Not recommended any
	 * more because the new paste function progressively rolls back arenas.
	 * 
	 * @param minX
	 *            The X value of the minimum location of the region.
	 * @param minY
	 *            The Y value of the minimum location of the region.
	 * @param minZ
	 *            The Z value of the minimum location of the region.
	 * @param maxX
	 *            The X value of the maximum location of the region.
	 * @param maxY
	 *            The X value of the maximum location of the region.
	 * @param maxZ
	 *            The Y value of the maximum location of the region.
	 * @param world
	 *            The world that the region is in.
	 * @param name
	 *            The name and directory of the folder that will contain the saved data.
	 *            Recommended: Make a sub-folder in your Main.plugin and put them in there.
	 * @param sender
	 *            Where status messages will be sent. Null for no messages, consoleSender for
	 *            console, and a player for a player.
	 * @see Copy
	 * 
	 * @deprecated Ever since version 2.0, this method does not increase performance, instead it
	 *             just makes it unnecceceraly complicated.
	 */
	public static final void copyDistributed(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, World world,
			String name, CommandSender sender) {
		File file = null;
		File index = null;
		short files = 0;

		try {
			file = new File("./" + name);
			// create
			System.out.print("Directory created: " + file.mkdir());

			index = new File("./" + name + "/index.dat");
			// If the index exists, delete it
			if (index.exists()) {
				index.delete();
			}

			// Create the index
			index.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			if (sender != null) {
				sender.sendMessage(
						Main.prefix + ChatColor.RED + "Aborted due to file IO exception. Check console for details.");
			}

			return;
		}

		BufferedOutputStream out;

		try {
			out = new BufferedOutputStream(new FileOutputStream(index));
		} catch (IOException e) {
			e.printStackTrace();
			if (sender != null) {
				sender.sendMessage(Main.prefix + ChatColor.RED
						+ "Aborted due to error creating FileOutputStream. Check console for details.");
			}

			return;
		}

		// Keeps track of the min/max locations.
		int tempMaxX = 0;
		int tempMaxY = 0;
		int tempMaxZ = 0;
		int tempMinX = 0;
		int tempMinY = 0;
		int tempMinZ = 0;

		// Keeps track of the file ID
		short tempFile = 0;

		for (int x = 0; x < ((maxX - minX - 1) / SIZE) + 1; x++) {
			for (int y = 0; y < ((maxY - minY - 1) / SIZE) + 1; y++) {
				for (int z = 0; z < ((maxZ - minZ - 1) / SIZE) + 1; z++) {

					// Multiplies by the constant SIZE because the X Y and Z are
					// stored in intervals of SIZE
					tempMinX = minX + (SIZE * x);
					tempMinY = minY + (SIZE * y);
					tempMinZ = minZ + (SIZE * z);
					tempMaxX = tempMinX + SIZE - 1;
					tempMaxY = tempMinY + SIZE - 1;
					tempMaxZ = tempMinZ + SIZE - 1;

					// This code is here to allow it to keep it in-bounds on the
					// outside.
					if (tempMaxX >= maxX - 1) {
						tempMaxX = maxX;
					}
					if (tempMaxY >= maxY - 1) {
						tempMaxY = maxY;
					}
					if (tempMaxZ >= maxZ - 1) {
						tempMaxZ = maxZ;
					}

					// One copy per tick
					Copy copy = new Copy(tempMinX, tempMinY, tempMinZ, tempMaxX, tempMaxY, tempMaxZ, world,
							"./" + name + "/" + files, sender);
					Bukkit.getScheduler().runTaskLater(Main.plugin, copy, files);

					// Writes to the index

					tempFile = files;
					tempMinX = x;
					tempMinY = y;
					tempMinZ = z;

					// Uses writeShort method due to the values potentially
					// being greater than 255.
					try {
						FileUtilities.writeShort(out, tempFile);
						FileUtilities.writeShort(out, tempMinX);
						FileUtilities.writeShort(out, tempMinY);
						FileUtilities.writeShort(out, tempMinZ);
					} catch (IOException ex) {
						sender.sendMessage(Main.prefix + "Read/write exception.");
						return;
					}

					// Adds 1 to the file ID because it just completed the
					files++;
				}
			}
		}

		// Alerts the user that the copy process is finished.
		if (sender != null) {
			DelayedMessage message = new DelayedMessage(Main.prefix + "Done!", sender);

			Bukkit.getScheduler().runTaskLater(Main.plugin, message, files + 1);
		}

		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/*-----------------------------------| Paste |------------------------------------*/

	/**
	 * Used to allow players to paste a saved copy where their worldedit region is. Normally used
	 * for testing.
	 * 
	 * @param player
	 *            The player who will get status messages and who's region will be used.
	 * @param name
	 *            The file/folder directory that will get pasted.
	 */
	public static final void paste(Player player, String name) {
		// Uses worldedit to get the player's region.
		WorldEditPlugin worldEditPlugin = null;
		worldEditPlugin = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		if (worldEditPlugin == null) {
			player.sendMessage(Main.prefix + "Error with region command! Error: WorldEdit is null.");
		}

		Selection sel = worldEditPlugin.getSelection(player);

		// Checks if the player has a selection.
		if (sel instanceof CuboidSelection) {

			// Gets the min point because that is all that is needed for pastes.
			Vector min = sel.getNativeMinimumPoint();

			// Pastes it at those coordinates.
			// Shows on console for debug and notification purposes.

			Paste paste = new Paste(min.getBlockX(), min.getBlockY(), min.getBlockZ(), player.getWorld(), name, null,
					player);
			Bukkit.getScheduler().runTaskLater(Main.plugin, paste, 1);

		} else {
			// Lets the player know there's no selection.
			player.sendMessage(Main.prefix + ChatColor.DARK_RED + "Invalid Selection!");
		}

	}

	/**
	 * Use if you want to maintain compatibility with distributed pastes. It checks if you are
	 * trying to paste a simple file, or a distributed paste, and uses the appropriate methods.
	 * 
	 * @param x
	 *            Where the min-x of the paste will be pasted.
	 * @param y
	 *            Where the min-y of the paste will be pasted.
	 * @param z
	 *            Where the min-z of the paste will be pasted.
	 * @param world
	 *            What world the paste will be pasted in.
	 * @param name
	 *            The directory of the files.
	 * @param sender
	 *            The person who will get status messages. Use null for no messsages, and
	 *            consoleSender for console.
	 * @deprecated Ever since version 2.0, due to the performance improvements this method is no
	 *             longer needed. Only use it for backwards compatibility if your plugin used
	 *             distributed pastes.
	 */
	public static final void prePaste(int x, int y, int z, World world, String name, CommandSender sender) {
		// Shows on console for debug and notification purposes.
		System.out.println("New paste at " + x + " " + y + " " + z + " " + world + " " + name);

		File dat = new File(name + ".dat");
		File dir = new File(name);

		// Checks if it is a single file, if not, it uses the distributed
		// system.
		if (dat.exists()) {
			Paste paste = new Paste(x, y, z, world, name, null, sender);
			Bukkit.getScheduler().runTaskLater(Main.plugin, paste, 1);
		} else if (dir.isDirectory()) {
			pasteDistributed(x, y, z, world, name, sender);
		} else if (sender != null) {
			sender.sendMessage(Main.prefix + "Not a file!");
		}
	}

	/**
	 * A non-recommended way to paste. It checks if you are trying to paste a simple file, or a
	 * distributed paste, and uses the appropriate methods.
	 * 
	 * @param x
	 *            Where the min-x of the paste will be pasted.
	 * @param y
	 *            Where the min-y of the paste will be pasted.
	 * @param z
	 *            Where the min-z of the paste will be pasted.
	 * @param world
	 *            What world the paste will be pasted in.
	 * @param name
	 *            The directory of the files.
	 * @param sender
	 *            The person who will get status messages. Use null for no messsages, and
	 *            consoleSender for console.
	 * @deprecated Ever since version 2.0, due to the performance improvements this method is no
	 *             longer needed. Only use it for backwards compatibility if your plugin used
	 *             distributed pastes.
	 */
	public static final void pasteDistributed(int x, int y, int z, World world, String name, CommandSender sender) {
		// How far in each direction to paste the paste section. Divided by 80
		int differenceX = 0;
		int differenceY = 0;
		int differenceZ = 0;
		// Used to keep track of the files.
		int file = 0;
		// Using to read the index. The index is used to store where it should
		// paste the parts.
		File index = null;
		// Used to read the file.
		BufferedInputStream in;
		// Stores the paste objects.
		ArrayList<Paste> pastes = new ArrayList<Paste>(30);

		// Opens the index file
		try {
			index = new File("./" + name + "/index.dat");
		} catch (Exception e) {
			// if any error occurs
			e.printStackTrace();
			if (sender != null) {
				sender.sendMessage("Paste operation canceled due to read error.");
			}
			return;
		}

		try {
			// Starts the fileReader used to edit the file
			in = new BufferedInputStream(new FileInputStream(index));

			// It needs to check 6 values, so it only checks when there are 6+
			while (in.available() >= 6) {
				file = FileUtilities.readShort(in);

				// Read the width, height, and depth.
				differenceX = FileUtilities.readShort(in);
				differenceY = FileUtilities.readShort(in);
				differenceZ = FileUtilities.readShort(in);

				// Adds the paste to the ArrayList so it can be pasted later.
				pastes.add(new Paste(x + (differenceX * SIZE), y + (differenceY * SIZE), z + (differenceZ * SIZE),
						world, "./" + name + "/" + file, pastes, sender));

			}

		} catch (IOException e) {
			e.printStackTrace();
			if (sender != null) {
				sender.sendMessage(Main.prefix + "Error reading or accessing file!");
			}
			return;
		}

		// Schedules the first paste task to run.
		Bukkit.getScheduler().runTaskLater(Main.plugin, pastes.get(0), 1);
	}
}
