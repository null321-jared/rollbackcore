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

import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * @author lizardfreak321
 */
public class Commands implements CommandExecutor {
	private Main plugin;
	private String prefix;

	public Commands(Main plugin, String prefix) {
		this.plugin = plugin;
		this.prefix = prefix;
	}

	@Override
	public final boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		// Checks to see if the player has permission to use the commands.
		if (sender.hasPermission("Rollback.admin")) {
			// Checks if they issued the rollback command.
			if (cmd.getName().equalsIgnoreCase("rollback")) {
				if (args.length < 1) {
					// There is nothing this plugin does with the command
					// "/rollback" by itself.
					sender.sendMessage(prefix + "Not enough arguments!!");
					helpCommand(sender);

				} else if (args.length > 0) {

					if (args[0].equalsIgnoreCase("reload")) {
						reloadCommand(sender);
					} else if (args[0].equalsIgnoreCase("addarena") || args[0].equalsIgnoreCase("addregion")) {
						addArenaCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("watchdog")) {
						watchDogCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("copy")) {
						copyCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("paste")) {
						pasteCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("arena") || args[0].equalsIgnoreCase("rollbackregion")
							|| args[0].equalsIgnoreCase("region")) {
						arenaRollackCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("help")) {
						helpCommand(sender);
					} else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("cancelall")) {
						sender.sendMessage(prefix + "Canceled " + TaskManager.cancelAllTasks() + " tasks.");
					} else {
						sender.sendMessage(ChatColor.RED + "Unknown comand!");
						helpCommand(sender);
					}
				}
			} else {
				helpCommand(sender);
			}
		} else {
			sender.sendMessage(prefix + ChatColor.RED + "No permission!");
		}
		return true;
	}

	private final void reloadCommand(CommandSender sender) {
		Config.loadConfigs(plugin);
		sender.sendMessage(prefix + "Reloaded!");
	}

	private final void addArenaCommand(CommandSender sender, String[] args) {
		// addarena command.
		if (!(sender instanceof ConsoleCommandSender)) {
			if (args.length == 2 && args[1].matches("^[a-zA-Z0-9]+$")) {
				Rollback.copy((Player) sender, args[1], true);
			} else {
				sender.sendMessage(prefix + "Usage: /rollback addarena <name>");
			}
		} else {
			sender.sendMessage(prefix + "Only players can issue this command!");
		}
	}

	private final void watchDogCommand(CommandSender sender, String args[]) {
		if (args.length == 2 && args[1].equalsIgnoreCase("rollback")) {
			WatchDogRegion.playerRollback(sender);
		} else if (args.length == 2 && args[1].equalsIgnoreCase("create")) {
			WatchDogRegion.playerCreateWatchDog((Player) sender);
		} else if (args.length == 3 && args[1].equalsIgnoreCase("export")) {
			WatchDogRegion.playerExport(sender, args[2]);
		} else if (args.length == 3 && args[1].equalsIgnoreCase("import")) {
			sender.sendMessage(prefix + "Importing...");
			WatchDogRegion.importWatchDog(args[2], sender, Main.prefix);
		} else {
			sender.sendMessage(prefix + "Usage: /rollback watchdog <rollback|create|import|export>");
		}
	}

	private final void pasteCommand(CommandSender sender, String args[]) {
		if (args.length == 2) {
			// For the command /rollback paste <fileName>
			if (sender instanceof Player)
				Rollback.paste((Player) sender, args[1]);
			else
				sender.sendMessage(prefix + "Only players can use this command!");
		} else if (args.length >= 6 && args.length <= 8) {
			// For the command:
			// "/rollback paste <x> <y> <z> <world> <file> -clearEntites -ignoreAir"
			// Requires no worldedit.
			Location min;
			Set<String> otherArgs = new HashSet<String>();
			for (int i = 2; i < args.length; i++) {
				otherArgs.add(args[i].toLowerCase());
			}

			try {
				World world = plugin.getServer().getWorld(args[4]);
				if (world == null) {
					sender.sendMessage(prefix + "Unknown world!");
				} else {
					min = new Location(world, Integer.parseInt(args[1]), Integer.parseInt(args[2]),
							Integer.parseInt(args[3]));
					new Paste(min, args[5], sender, otherArgs.remove("-clearentities"), otherArgs.remove("-ignoreair"),
							prefix).run();
					if (otherArgs.size() > 0)
						sender.sendMessage(
								Main.prefix + "Unknown args " + otherArgs.toString() + ". Continuing with operation.");
				}
			} catch (NumberFormatException e) {
				sender.sendMessage(prefix + ChatColor.RED + " You must specify an integer for the time!");
				sender.sendMessage(
						prefix + "Usage: /rollback paste [<x> <y> <z> <world>] <file> [-clearEntities -ignoreAir]");
			}
		} else {
			sender.sendMessage(
					prefix + "Usage: /rollback paste [<x> <y> <z> <world>] <file> [-clearEntities -ignoreAir]");
		}
	}

	private final void copyCommand(CommandSender sender, String args[]) {
		if (args.length == 2) {
			// For /rollback copy <file>
			// Requires worldedit.
			if (sender instanceof Player)
				Rollback.copy((Player) sender, args[1], false);
			else
				sender.sendMessage(prefix + "Only players can use this command!");
		} else {
			sender.sendMessage(prefix + "Usage: /rollback copy <output-file>");
		}
	}

	@SuppressWarnings("deprecation")
	private final void arenaRollackCommand(CommandSender sender, String[] args) {
		if (args.length >= 2) {

			Location temp = Config.getArenaLocation(args[1]);
			if (temp.getWorld() != null) {
				String name = Paths.get(Main.regionsPath.toString(), args[1]).toString();

				Set<String> otherArgs = new HashSet<String>();
				for (int i = 2; i < args.length; i++) {
					otherArgs.add(args[i].toLowerCase());
				}

				File dat = new File(name + ".dat");
				File dir = new File(name);

				// Checks if it is a single file, if not, it uses the distributed
				// system.
				if (dat.exists()) {
					Paste paste = new Paste(temp, name + ".dat", sender, otherArgs.remove("-clearentities"),
							otherArgs.remove("-ignoreair"), Main.prefix);
					Bukkit.getScheduler().runTaskLater(Main.plugin, paste, 1);
					if (otherArgs.size() > 0)
						sender.sendMessage(
								Main.prefix + "Unknown args " + otherArgs.toString() + ". Continuing with operation.");
				} else if (dir.isDirectory()) {
					if (otherArgs.size() > 0) {
						sender.sendMessage(Main.prefix
								+ "Args will be ignored due to old save format. Re-save the region to use them.");
					}
					Rollback.pasteDistributed(temp.getBlockX(), temp.getBlockY(), temp.getBlockZ(), temp.getWorld(),
							name, sender);
				} else if (sender != null) {
					sender.sendMessage(Main.prefix + "Not a file!");
				}

			} else {
				sender.sendMessage(prefix
						+ "Could not find world! Please re-create the arena save or edit the config to contain a valid world.");
			}
		} else {
			sender.sendMessage(prefix + "Usage: /rollback rollbackregion <arenaname> [-clearEntities -ignoreAir]");
		}
	}

	private final void helpCommand(CommandSender sender) {
		sender.sendMessage(ChatColor.GRAY + "----------------------- " + ChatColor.GREEN + "[" + ChatColor.DARK_GREEN
				+ "Help" + ChatColor.GREEN + "]" + ChatColor.GRAY + " -----------------------");
		sender.sendMessage(ChatColor.GRAY + "/rollback reload | Reloads the plugin's config.");
		sender.sendMessage(ChatColor.GRAY + "/rollback copy | The copy commands.");
		sender.sendMessage(ChatColor.GRAY + "/rollback paste | The paste commands.");
		sender.sendMessage(ChatColor.GRAY + "/rollback watchdog <create|rollback> | The watchdog region commands.");
		sender.sendMessage(ChatColor.GRAY
				+ "/rollback <rollbackregion|addregion> <name> | Used for integration with minigame plugins.");
		sender.sendMessage(ChatColor.GRAY + "----------------------------------------------------");
	}

}
