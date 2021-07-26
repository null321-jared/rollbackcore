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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.ByteStreams;

enum CompressionType {
	NONE, LZ4;
}

/**
 * @author lizardfreak321
 */
public class Config {
	private static JavaPlugin plugin = null;

	// Used to store the amount of time all running tasks added up should target
	// in one tick.
	public static int targetTime = 25;
	
	// Used to store whether the chunks should be unloaded forcefully
	public static boolean unloadChunks = false;

	// Used to store which compression algorithm (if any) is being used for copies.
	public static CompressionType compressionType = CompressionType.LZ4;

	private Config() {
	}

	public final boolean Loaded() {
		return plugin != null;
	}

	public static final void loadConfigs(JavaPlugin newPlugin) {
		plugin = newPlugin;
		// Loads config
		loadResource(plugin, "config.yml");
		// Checks the config for any needed changes.
		checkConfig();
		// Loads the settings
		Config.targetTime = getTargetTime();
		Config.compressionType = getCompression();
		Config.unloadChunks = getUnload();
		// Alerts user though console.
		plugin.getLogger().info("Configs loaded!");
	}

	// Loads the resource if it isn't there.
	public final static File loadResource(Plugin plugin, String resource) {
		File folder = plugin.getDataFolder();
		// Creates the plugin folder if it doesn't exist.
		if (!folder.exists())
			folder.mkdir();
		File resourceFile = new File(folder, resource);
		// Copes the files that are saved in the jar to the plugin folder if
		// they do not exist in the plugin folder.
		try {
			if (!resourceFile.exists()) {
				resourceFile.createNewFile();
				try (InputStream in = plugin.getResource(resource);
						OutputStream out = new FileOutputStream(resourceFile)) {
					ByteStreams.copy(in, out);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resourceFile;
	}

	private static final void checkConfig() {
		File file = new File(plugin.getDataFolder() + "/config.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		boolean changed = false;

		if (yaml.contains("Config.rollback.rate")) {
			yaml.set("Config.rollback.rate", null);
			changed = true;
		}

		if (!yaml.contains("Config.rollback.targettime")) {
			yaml.set("Config.rollback.targettime", 25);
			changed = true;
		}
		
		if (!yaml.contains("Config.rollback.unload_chunks")) {
			yaml.set("Config.rollback.unload_chunks", false);
			changed = true;
		}

		if (!yaml.contains("Config.rollback.compression")) {
			yaml.set("Config.rollback.compression", "NONE");
			changed = true;
		}

		if (!yaml.contains("configversion")) {
			yaml.set("configversion", 1.0);
			changed = true;
		}

		boolean moved = yaml.getBoolean("movedfiles", false);
		if (!moved) {
			changed = true;
			Path oldArenasFolder = Paths.get("arenas");
			try {
				if (Files.exists(oldArenasFolder)) {
					Files.move(oldArenasFolder, Main.regionsPath, StandardCopyOption.REPLACE_EXISTING);
					Main.plugin.getLogger().info("Moving the arena saves");
				}
				yaml.set("movedfiles", true);
			} catch (IOException e) {
				Main.plugin.getLogger().warning("Failed moving the arena saves");
				e.printStackTrace();
				yaml.set("movedfiles", false);
			}
		}

		if (changed)
			try {
				yaml.save(file);
			} catch (IOException e) {
				Main.plugin.getLogger().info(
						"Unable to save config! This may cause an issue if there is an incompatible config value.");
				e.printStackTrace();
			}
	}

	// Gets Block Rate from the config.
	private static final int getTargetTime() {
		File file = new File(plugin.getDataFolder() + "/config.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

		// Validates input.
		int finalizedTargetTime = yaml.getInt("Config.rollback.targettime", 25);
		if (finalizedTargetTime > 50) {
			finalizedTargetTime = 50;
			Main.plugin.getLogger().info(
					"Your set value for targettime was too high! Setting to 50ms (I highly recommend that you lower this, an operation that takes an entire tick WILL lag the server)");
		} else if (finalizedTargetTime < 1) {
			finalizedTargetTime = 1;
			Main.plugin.getLogger().info(
					"Your set value for targettime was too low! Setting to 1ms (Pastes copy, paste, and other rollback operations will be slow)");
		}

		return finalizedTargetTime;
	}
	
	// Gets Block Rate from the config.
	private static final CompressionType getCompression() {
		File file = new File(plugin.getDataFolder() + "/config.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		
		// Validates input.
		String algoritmName = yaml.getString("Config.rollback.compression", null);
		CompressionType compression;
		if (algoritmName == null) {
			compression = CompressionType.LZ4;
			plugin.getLogger().warning("No compression algoritm specified. Using LZ4");
		} else if (algoritmName.isEmpty()) {
			compression = CompressionType.NONE;
			plugin.getLogger().info("Using no compression.");
		} else {
			try {
				compression = CompressionType.valueOf(algoritmName.toUpperCase());
				plugin.getLogger().info("Using compression \"" + compression.toString() + "\"");
			} catch (IllegalArgumentException e) {
				plugin.getLogger().warning("Compression algoritm not found. Using LZ4. Options: "
						+ Arrays.toString(CompressionType.values()));
				compression = CompressionType.LZ4;
			}
		}
		
		return compression;
	}

	// Gets Block Rate from the config.
	private static final boolean getUnload() {
		File file = new File(plugin.getDataFolder() + "/config.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

		return yaml.getBoolean("Config.rollback.unload_chunks", false);
	}

	// WARNING: Case sensitive!
	public static final Location getRegionMinLocation(String regionName) {
		// For debug reasons.
		if (plugin == null) {
			System.out.println("Plugin is null!");
		}

		// Loads the config.yml
		File file = new File(plugin.getDataFolder() + "/config.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

		// Gets the values.
		double x = yaml.getInt("Config.arenas." + regionName + ".x");
		double y = yaml.getInt("Config.arenas." + regionName + ".y");
		double z = yaml.getInt("Config.arenas." + regionName + ".z");
		String worldName = yaml.getString("Config.arenas." + regionName + ".world", regionName);
		World world = Bukkit.getWorld(worldName);

		return new Location(world, x, y, z);
	}
	
	/**
	 * WARNING: Case sensitive!
	 * 
	 * Returns the max location of the 
	 * @param regionName
	 * @return
	 */
	public static final Location getRegionMaxLocation(String regionName) {
		// For debug reasons.
		if (plugin == null) {
			System.out.println("Plugin is null!");
		}

		// Loads the config.yml
		File file = new File(plugin.getDataFolder() + "/config.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

		// Gets the values.
		double x = yaml.getInt("Config.arenas." + regionName + ".x");
		double y = yaml.getInt("Config.arenas." + regionName + ".y");
		double z = yaml.getInt("Config.arenas." + regionName + ".z");
		int sizeX = yaml.getInt("Config.arenas." + regionName + ".sizeX", -1);
		int sizeY = yaml.getInt("Config.arenas." + regionName + ".sizeY", -1);
		int sizeZ = yaml.getInt("Config.arenas." + regionName + ".sizeZ", -1);
		
		if (sizeX == -1 || sizeY == -1 || sizeZ == -1) {
			return null;
		}
		
		String worldName = yaml.getString("Config.arenas." + regionName + ".world", regionName);
		World world = Bukkit.getWorld(worldName);

		return new Location(world, x + sizeX - 1, y + sizeY - 1, z + sizeZ - 1);
	}

	// Returns false if it fails.
	public static final boolean setArenaLocation(String arena, int x, int y, int z,
			int sizeX, int sizeY, int sizeZ, World world)
	{
		// For debug reasons.
		if (plugin == null) {
			System.out.println("Plugin is null!");
		}

		// Loads the config.yml
		File file = new File(plugin.getDataFolder() + "/config.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

		// Sets the values in the YAML file.
		yaml.set("Config.arenas." + arena + ".x", x);
		yaml.set("Config.arenas." + arena + ".y", y);
		yaml.set("Config.arenas." + arena + ".z", z);
		yaml.set("Config.arenas." + arena + ".sizeX", sizeX);
		yaml.set("Config.arenas." + arena + ".sizeY", sizeY);
		yaml.set("Config.arenas." + arena + ".sizeZ", sizeZ);
		yaml.set("Config.arenas." + arena + ".world", world.getName());

		// Tries to save the file.
		try {
			yaml.save(file);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
