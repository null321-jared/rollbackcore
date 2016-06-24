/**
 * Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
 */

package net.shadowxcraft.rollbackcore;

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
					} else if (args[0].equalsIgnoreCase("addarena")) {
						addArenaCommand(sender);
					} else if (args[0].equalsIgnoreCase("watchdog")) {
						watchDogCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("copy")) {
						copyCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("paste")) {
						pasteCommand(sender, args);
					} else if (args[0].equalsIgnoreCase("arena")) {
						worldArenaRollackCommand(sender, args);
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

	private final void addArenaCommand(CommandSender sender) {
		// addarena command.
		if (!(sender instanceof ConsoleCommandSender)) {
			Rollback.copy((Player) sender, null, true);
		} else {
			sender.sendMessage("Only players can issue this command!");
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
			sender.sendMessage(prefix + "Usage: /rollback watchdog <rollback|create>");
		}
	}

	@SuppressWarnings("deprecation")
	private final void pasteCommand(CommandSender sender, String args[]) {
		if (args.length == 2) {
			// For the command /rollback paste <fileName>
			if (sender instanceof Player)
				Rollback.paste((Player) sender, args[1]);
			else
				sender.sendMessage(prefix + "Only players can use this command!");
		} else if (args.length == 6) {
			// For the command "/rollback paste <x> <y> <z> <world> <file>"
			// Requires no worldedit.
			World world = plugin.getServer().getWorld(args[4]);
			if (world == null) {
				sender.sendMessage(prefix + "Unknown world!");
			} else {
				try {
					Rollback.prePaste(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]),
							plugin.getServer().getWorld(args[4]), args[5], sender);
				} catch (NumberFormatException e) {
					sender.sendMessage(prefix + ChatColor.RED + " You must specify an integer for the time!");
					sender.sendMessage(prefix
							+ "Usage: /rollback paste [<x> <y> <z> <world>] <file> [<clearEntities> <ignoreAir>]");
				}
			}
		} else if (args.length == 8) {
			// For the command:
			// "/rollback paste <x> <y> <z> <world> <file> <clearEntites> <ignoreAir>"
			// Requires no worldedit.
			Location min;
			try {
				World world = plugin.getServer().getWorld(args[4]);
				if (world == null) {
					sender.sendMessage(prefix + "Unknown world!");
				} else {
					min = new Location(world, Integer.parseInt(args[1]), Integer.parseInt(args[2]),
							Integer.parseInt(args[3]));
					new Paste(min, args[5], sender, Boolean.parseBoolean(args[6]), Boolean.parseBoolean(args[7]),
							prefix).run();
				}
			} catch (NumberFormatException e) {
				sender.sendMessage(prefix + ChatColor.RED + " You must specify an integer for the time!");
				sender.sendMessage(
						prefix + "Usage: /rollback paste [<x> <y> <z> <world>] <file> [<clearEntities> <ignoreAir>]");
			}
		} else {
			sender.sendMessage(
					prefix + "Usage: /rollback paste [<x> <y> <z> <world>] <file> [<clearEntities> <ignoreAir>]");
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
	private final void worldArenaRollackCommand(CommandSender sender, String args[]) {
		if (args.length == 2) {
			int[] temp = Config.getArenaXYZ(args[1]);
			Rollback.prePaste(temp[0], temp[1], temp[2], plugin.getServer().getWorld(args[1]), "arenas/" + args[1],
					sender);
		} else {
			sender.sendMessage(prefix + "Usage: /rollback arena <world>");
		}
	}

	private final void helpCommand(CommandSender sender) {
		sender.sendMessage(ChatColor.GRAY + "----------------------- " + ChatColor.GREEN + "[" + ChatColor.DARK_GREEN
				+ "Help" + ChatColor.GREEN + "]" + ChatColor.GRAY + " -----------------------");
		sender.sendMessage(ChatColor.GRAY + "/rollback reload | Reloads the plugin's config.");
		sender.sendMessage(ChatColor.GRAY + "/rollback copy | The copy commands.");
		sender.sendMessage(ChatColor.GRAY + "/rollback paste | The paste commands.");
		sender.sendMessage(ChatColor.GRAY + "/rollback watchdog <create|rollback> | The watchdog region commands.");
		sender.sendMessage(ChatColor.GRAY + "/rollback <arena|addarena> | Used for integration with SurvivalGames.");
		sender.sendMessage(ChatColor.GRAY + "----------------------------------------------------");
	}

}
