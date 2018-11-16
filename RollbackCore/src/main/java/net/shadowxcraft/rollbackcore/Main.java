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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	public static JavaPlugin plugin;
	public static Path savesPath;
	public static Path regionsPath;
	Metrics metrics;

	// Fired when plugin is first enabled
	@Override
	public final void onEnable() {
		plugin = this;
		// Register the rollback command
		getCommand("rollback").setExecutor(new Commands(this, prefix));
		getServer().getPluginManager().registerEvents(new BukkitListener(), plugin);

		try {
			savesPath = Paths.get(getDataFolder().getAbsolutePath(), "/saves");
			Files.createDirectories(savesPath);

			regionsPath = Paths.get(savesPath.toString(), "/regions");
			Files.createDirectories(regionsPath);
		} catch (IOException e) {
			// Failed to submit the stats :-(
		}
		Config.loadConfigs(plugin);
		LegacyUpdater.loadMappings(this);

		metrics = new Metrics(this);

		metrics.addCustomChart(new Metrics.SimplePie("Compression", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return Config.compressionType.name();
			}
		}));
		metrics.addCustomChart(new Metrics.SimplePie("Target_Time", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return Integer.toString(Config.targetTime);
			}
		}));

		metrics.addCustomChart(new Metrics.SimplePie("Skript", new Callable<String>() {
			@Override
			public String call() throws Exception {
				return Boolean.toString(Bukkit.getPluginManager().getPlugin("Skript") != null);
			}
		}));

		if (Bukkit.getPluginManager().getPlugin("Skript") != null)
			SkriptSupport.initSkriptSupport();
	}

	// The plugin's prefix, used for messages.
	public final static String prefix = ChatColor.GREEN + "[" + ChatColor.DARK_GREEN + "RollbackCore" + ChatColor.GREEN
			+ "] " + ChatColor.GRAY;

	// Fired when plugin is disabled
	@Override
	public void onDisable() {
		plugin = null;
	}

}