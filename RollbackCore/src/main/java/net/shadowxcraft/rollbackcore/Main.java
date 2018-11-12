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
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import net.shadowxcraft.rollbackcore.events.ClearEntitiesEndEvent;
import net.shadowxcraft.rollbackcore.events.CopyEndEvent;
import net.shadowxcraft.rollbackcore.events.PasteEndEvent;
import net.shadowxcraft.rollbackcore.events.WDImportEndEvent;
import net.shadowxcraft.rollbackcore.events.WDRollbackEndEvent;

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

		initSkriptSupport();
	}

	private final void initSkriptSupport() {
		if (Bukkit.getPluginManager().getPlugin("Skript") != null) {
			// Register events
			Skript.registerEvent("RollbackCore Copy End", SimpleEvent.class, CopyEndEvent.class,
					"[rollbackcore] copy end");
			EventValues.registerEventValue(CopyEndEvent.class, String.class, new Getter<String, CopyEndEvent>() {
				@Override
				public String get(CopyEndEvent e) {
					return e.endStatus().name();
				}
			}, 0);

			EventValues.registerEventValue(CopyEndEvent.class, Location.class, new Getter<Location, CopyEndEvent>() {
				@Override
				public Location get(CopyEndEvent e) {
					return e.getCopy().getMin();
				}
			}, 0);

			Skript.registerEvent("RollbackCore Paste End", SimpleEvent.class, PasteEndEvent.class,
					"[rollbackcore] paste end");
			EventValues.registerEventValue(PasteEndEvent.class, Location.class, new Getter<Location, PasteEndEvent>() {
				@Override
				public Location get(PasteEndEvent e) {
					return e.getPaste().getMin();
				}
			}, 0);
			EventValues.registerEventValue(PasteEndEvent.class, String.class, new Getter<String, PasteEndEvent>() {
				@Override
				public String get(PasteEndEvent e) {
					return e.endStatus().name();
				}
			}, 0);

			Skript.registerEvent("RollbackCore Clear Entities End", SimpleEvent.class, ClearEntitiesEndEvent.class,
					"[rollbackcore] entityclear end");
			EventValues.registerEventValue(ClearEntitiesEndEvent.class, String.class,
					new Getter<String, ClearEntitiesEndEvent>() {
						@Override
						public String get(ClearEntitiesEndEvent e) {
							return e.endStatus().name();
						}
					}, 0);
			EventValues.registerEventValue(ClearEntitiesEndEvent.class, Location.class,
					new Getter<Location, ClearEntitiesEndEvent>() {
						@Override
						public Location get(ClearEntitiesEndEvent e) {
							return e.getClearEntitiesObject().min;
						}
					}, 0);

			Skript.registerEvent("RollbackCore WatchDog Import End", SimpleEvent.class, WDImportEndEvent.class,
					"[rollbackcore] wdimport end");
			EventValues.registerEventValue(WDImportEndEvent.class, String.class,
					new Getter<String, WDImportEndEvent>() {
						@Override
						public String get(WDImportEndEvent e) {
							return e.endStatus().name();
						}
					}, 0);
			EventValues.registerEventValue(WDImportEndEvent.class, Location.class,
					new Getter<Location, WDImportEndEvent>() {
						@Override
						public Location get(WDImportEndEvent e) {
							return e.getWatchDog().getMin();
						}
					}, 0);

			Skript.registerEvent("RollbackCore WatchDog Rollback End", SimpleEvent.class, WDRollbackEndEvent.class,
					"[rollbackcore] wdrollback end");
			EventValues.registerEventValue(WDRollbackEndEvent.class, String.class,
					new Getter<String, WDRollbackEndEvent>() {
						@Override
						public String get(WDRollbackEndEvent e) {
							return e.endStatus().name();
						}
					}, 0);
			EventValues.registerEventValue(WDRollbackEndEvent.class, Location.class,
					new Getter<Location, WDRollbackEndEvent>() {
						@Override
						public Location get(WDRollbackEndEvent e) {
							return e.getWatchDog().getMin();
						}
					}, 0);
		}
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