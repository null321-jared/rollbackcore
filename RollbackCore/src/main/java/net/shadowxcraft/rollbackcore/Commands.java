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
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.math.BlockVector3;

import net.shadowxcraft.rollbackcore.LegacyUpdater.Mapping;

/**
 * @author lizardfreak321
 */
public class Commands implements CommandExecutor {
	private final Main plugin;
	private final String prefix;

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
						addRegionCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("updatearena") || args[0].equalsIgnoreCase("updateregion")) {
						updateRegionCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("watchdog")) {
						watchDogCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("copy")) {
						copyCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("paste")) {
						pasteCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("arena") || args[0].equalsIgnoreCase("rollbackregion")
							|| args[0].equalsIgnoreCase("region")) {
						regionRollackCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("help")) {
						helpCommand(sender);
					} else if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("cancelall")) {
						sender.sendMessage(prefix + "Canceled " + TaskManager.cancelAllTasks() + " tasks.");
					} else if (args[0].equalsIgnoreCase("mappings")) {
						mappingsCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("chunkLoaded")) {
						chunkCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("worldbackup")) {
						worldBackupCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("worldrollback")) {
						worldRollbackCommand(sender, args);
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

	private void mappingsCommand(CommandSender sender, String[] args) {
		if (args.length == 1) {
			printMappings(sender);
		} else if (args.length == 2) {
			Mapping mappings = LegacyUpdater.loadLatestConversion(args[1]);
			if (mappings == null)
				sender.sendMessage(prefix + "No mapping was found.");
			else
				sender.sendMessage(prefix + "Mapping found to " + mappings.getTo());
		} else if (args.length == 3) {
			LinkedList<Map<Pattern, String>> mappings = LegacyUpdater.getMapping(args[1], args[2]);
			if (mappings == null)
				sender.sendMessage(prefix + "No mapping was found.");
			else
				sender.sendMessage(prefix + "Mapping found: " + mappings.toString());
		} else {
			sender.sendMessage(prefix + "Usage: /rollback mapppings [from] [to]");
		}
	}

	private void printMappings(CommandSender sender) {
		TreeMap<String, TreeMap<String, Map<Pattern, String>>> mappings = LegacyUpdater.getMappings();
		for (Map.Entry<String, TreeMap<String, Map<Pattern, String>>> toEntry : mappings.entrySet()) {
			sender.sendMessage("Mappings to " + toEntry.getKey() + ":");
			for (Map.Entry<String, Map<Pattern, String>> fromEntry : toEntry.getValue().entrySet()) {
				sender.sendMessage("\tMappings from " + fromEntry.getKey() + ":");
				for (Map.Entry<Pattern, String> replacementEntry : fromEntry.getValue().entrySet()) {
					sender.sendMessage(
							"\t\t" + replacementEntry.getKey().pattern() + " -> " + replacementEntry.getValue());
				}
			}
		}
	}

	private void reloadCommand(CommandSender sender) {
		Config.loadConfigs(plugin);
		sender.sendMessage(prefix + "Reloaded!");
	}

	private void addRegionCommand(CommandSender sender, String[] args) {
		// addarena command.
		if (!(sender instanceof ConsoleCommandSender)) {
			if (args.length == 2) {
				Rollback.copy((Player) sender, args[1], true);
			} else {
				sender.sendMessage(prefix + "Usage: /rollback addregion <name>");
			}
		} else {
			sender.sendMessage(prefix + "Only players can issue this command!");
		}
	}
	

	/**
	 * This method is for copying a region that is pre-defined.
	 * @param sender
	 * @param args
	 */
	private void updateRegionCommand(CommandSender sender, String[] args) {
		if (args.length >= 2) {
			
			String name = args[1];
			
			String filePath = Paths.get(Main.regionsPath.toString(), name + ".dat").toString();

			Location min = Config.getRegionMinLocation(name);
			Location max = Config.getRegionMaxLocation(name);
			
			if (max == null) {
				sender.sendMessage(Main.prefix +
						"Region does not exist or was saved on an older version." +
						" Create the region again with \"/rollback addregion\" or ." +
						" add the sizeX, sizeY, and sizeZ values to the config.");
						
				return;
			}
			
			new Copy(min.getBlockX(), min.getBlockY(), min.getBlockZ(), max.getBlockX(), max.getBlockY(),
					max.getBlockZ(), max.getWorld(), filePath, sender).run();
			
			
		} else {
			sender.sendMessage(prefix + "Usage: /rollback rollbackregion <arenaname> [-clearEntities -ignoreAir]");
		}
	}

	private void watchDogCommand(CommandSender sender, String args[]) {
		if (args.length == 2 && args[1].equalsIgnoreCase("rollback")) {
			WatchDogRegion.playerRollback(sender);
		} else if (args.length == 2 && args[1].equalsIgnoreCase("create")) {
			WatchDogRegion.playerCreateWatchDog((Player) sender);
		} else if (args.length == 2 && args[1].equalsIgnoreCase("remove")) {
			WatchDogRegion.playerRemove(sender);
		} else if (args.length == 3 && args[1].equalsIgnoreCase("export")) {
			WatchDogRegion.playerExport(sender, args[2]);
		} else if (args.length == 3 && args[1].equalsIgnoreCase("import")) {
			sender.sendMessage(prefix + "Importing...");
			WatchDogRegion.importWatchDog(args[2], sender, Main.prefix);
		} else {
			sender.sendMessage(prefix + "Usage: /rollback watchdog <rollback|create|remove|import|export>");
		}
	}

	private void pasteCommand(CommandSender sender, String[] args) {
		Set<String> otherArgs = new HashSet<>();
		int lastValidArg = 0;
		for (int i = args.length - 1; i > 0; i--) {
			if (args[i].startsWith("-")) {
				otherArgs.add(args[i].toLowerCase());
			} else {
				lastValidArg = i;
				break;
			}
		}
		boolean ignoreAir = otherArgs.remove("-ignoreair");
		boolean clearEntities = otherArgs.remove("-clearentities");
		if (!otherArgs.isEmpty()) {
			sender.sendMessage(prefix + "Unknown args: " + otherArgs.toString() + ". Continuing.");
		}

		String fileName = args[lastValidArg];
		Location min;
		if (lastValidArg == 1 || lastValidArg == 4 || lastValidArg == 5) {
			// Only filename specified if 1. everything but world specified if 4.
			// all location details specified if 5.
			World world;
			if (lastValidArg == 4 || lastValidArg == 1) {
				if (sender instanceof Player) {
					world = ((Player) sender).getWorld();
				} else {
					sender.sendMessage(prefix + "Non-player command executors must specify all location details!");
					return;
				}
			} else {
				world = Bukkit.getWorld(args[4]);
				if (world == null) {
					sender.sendMessage(prefix + "Unknown world \"" + args[4] + "\". Worlds: " + Bukkit.getWorlds());
					return;
				}
			}

			if (lastValidArg == 4 || lastValidArg == 5) {
				try {
					min = new Location(world, Integer.parseInt(args[1]), Integer.parseInt(args[2]),
							Integer.parseInt(args[3]));
				} catch (NumberFormatException e) {
					sender.sendMessage(prefix + ChatColor.RED + " You must specify an integer for the coordinates!");
					sender.sendMessage(prefix
							+ "Usage: /rollback paste [<x> <y> <z> [<world>]] <file> [-clearEntities -ignoreAir]");
					return;
				}
			} else { // lastValidArg == 1
				if (sender instanceof Player) {
					BlockVector3 minVector = Rollback.getSelectionMin((Player) sender);
					if (minVector != null) {
						min = new Location(((Player) sender).getWorld(), minVector.getX(), minVector.getY(),
								minVector.getZ());
					} else {
						sender.sendMessage(prefix
								+ "You don't have a worldedit selection. Either create one, or specify the coordinates in the command.");
						return;
					}
				} else {
					sender.sendMessage(prefix + "Non-player command executors must specify all location details!");
					return;
				}
			}
		} else {
			sender.sendMessage(prefix
					+ "Unknown amount of args. Usage: Usage: /rollback paste [<x> <y> <z> [<world>]] <file> [-clearEntities -ignoreAir]");
			return;
		}

		sender.sendMessage(prefix + "Starting paste operation!");
		new Paste(min, fileName, sender, clearEntities, ignoreAir, prefix).run();
	}

	private void copyCommand(CommandSender sender, String[] args) {
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

	private void chunkCommand(CommandSender sender, String args[]) {
		if (args.length == 4) {
			// For /rollback chunkloaded <world> <x> <z>
			World world = Bukkit.getServer().getWorld(args[1]);
			if (world == null) {
				sender.sendMessage(prefix + "World '" + args[1] + "' not found.");
				return;
			}
			try {
				sender.sendMessage(prefix + "Chunk is loaded: "
						+ world.isChunkLoaded(Integer.parseInt(args[2]), Integer.parseInt(args[3])));
			} catch (NumberFormatException e) {
				sender.sendMessage(prefix + "Failed to parse arguements.");
			}
		} else {
			sender.sendMessage(prefix + "Usage: /rollback chunkloaded <world> <x> <z>");
		}
	}

	@SuppressWarnings("deprecation")
	private void regionRollackCommand(CommandSender sender, String[] args) {
		if (args.length >= 2) {

			Location temp = Config.getRegionMinLocation(args[1]);
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
					if (!otherArgs.isEmpty())
						sender.sendMessage(
								Main.prefix + "Unknown args " + otherArgs.toString() + ". Continuing with operation.");
				} else if (dir.isDirectory()) {
					if (!otherArgs.isEmpty()) {
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

	public final void worldBackupCommand(CommandSender sender, String[] args) {
		if (args.length <= 3 && args.length >= 2) {
			World world = Bukkit.getWorld(args[1]);
			String directory;

			if (world == null) {
				sender.sendMessage(
						prefix + "World \"" + args[1] + "\" not found! List of worlds: " + Bukkit.getWorlds());
				return;
			}

			if (args.length == 3) {
				directory = args[2];
			} else {
				directory = null;
			}
			File result = WholeWorldRollback.backupWorld(world, directory);
			if (result == null) {
				sender.sendMessage(prefix + "Backup failed. Check console for errors.");
			} else {
				sender.sendMessage(prefix + "Done, but please also make a manual backup in case something fails!");
			}
		}
	}

	public final void worldRollbackCommand(CommandSender sender, String[] args) {
		if (args.length == 3 || args.length == 4 || args.length == 6 || args.length == 7) {
			World worldToRollback = Bukkit.getWorld(args[1]);
			String directory;
			Location locToSendPlayers;

			if (worldToRollback == null) {
				sender.sendMessage(prefix + "World to rollback \"" + args[1] + "\" not found! List of worlds: "
						+ Bukkit.getWorlds());
				return;
			}

			World worldToSendPlayers = Bukkit.getWorld(args[2]);
			if (worldToSendPlayers == null) {
				sender.sendMessage(prefix + "World to rollback \"" + args[2] + "\" not found! List of worlds: "
						+ Bukkit.getWorlds());
				return;
			}
			if (args.length <= 4) {
				locToSendPlayers = worldToSendPlayers.getSpawnLocation();
			} else {
				try {
					locToSendPlayers = new Location(worldToSendPlayers, Double.parseDouble(args[3]),
							Double.parseDouble(args[4]), Double.parseDouble(args[5]));
				} catch (NumberFormatException e) {
					sender.sendMessage(prefix + "Invalid number!");
					return;
				}
			}

			if (args.length == 7 || args.length == 4) {
				directory = args[args.length - 1];
			} else {
				directory = null;
			}
			try {
				WholeWorldRollback.startWholeWorldRollback(worldToRollback, locToSendPlayers, sender, prefix,
						directory);
				sender.sendMessage(prefix + "World rollback running.");
			} catch (Exception e) {
				sender.sendMessage(prefix + "World rollback failed.");
			}
		} else {
			sender.sendMessage(prefix
					+ "Usage: /rollback worldrollback <worldname> <to-worldname> [<to-x> <to-y> <to-z>] [folder name]|\"\r\n"
					+ " Rollbacks the world. The \"to-\" world is the world players should be teleported to.");
		}
	}

	private final void helpCommand(CommandSender sender) {
		sender.sendMessage(ChatColor.GRAY + "----------------------- " + ChatColor.GREEN + "[" + ChatColor.DARK_GREEN
				+ "Help" + ChatColor.GREEN + "]" + ChatColor.GRAY + " -----------------------");
		sender.sendMessage(ChatColor.GRAY + "/rollback reload | Reloads the plugin's config.");
		sender.sendMessage(ChatColor.GRAY + "/rollback copy | The copy commands.");
		sender.sendMessage(ChatColor.GRAY + "/rollback paste | The paste commands.");
		sender.sendMessage(ChatColor.GRAY + "/rollback watchdog <create|rollback|export|import|remove> | "
				+ "The watchdog region commands.");
		sender.sendMessage(ChatColor.GRAY
				+ "/rollback <rollbackregion|addregion> <name> | Used for integration with minigame plugins.");
		sender.sendMessage(ChatColor.GRAY + "/rollback worldbackup <worldname> [folder to put it] |"
				+ " If the folder is left blank, it will use a folder in RollbackCore's folder.");
		sender.sendMessage(ChatColor.GRAY
				+ "/rollback worldrollback <worldname> <to-worldname> [<to-x> <to-y> <to-z>] [folder name]|"
				+ " Rollbacks the world. The \"to-\" world is the world players should be teleported to."
				+ " May not work on the main world.");
		sender.sendMessage(ChatColor.GRAY + "----------------------------------------------------");
	}

}
