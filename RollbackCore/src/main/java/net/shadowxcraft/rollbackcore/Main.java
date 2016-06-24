/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
*/

package net.shadowxcraft.rollbackcore;

import org.bukkit.plugin.java.JavaPlugin;

import net.shadowxcraft.rollbackcore.metrics.Metrics;

import java.io.IOException;

import org.bukkit.ChatColor;

public class Main extends JavaPlugin {

	public static JavaPlugin plugin;

	// Fired when plugin is first enabled
	@Override
	public final void onEnable() {
		plugin = this;
		// Register the rollback command
		getCommand("rollback").setExecutor(new Commands(this, prefix));
		getServer().getPluginManager().registerEvents(new BukkitListener(), plugin);
		getServer().getPluginManager().registerEvents(new NewListeners(), plugin);

		Config.loadConfigs(plugin);

		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (IOException e) {
			// Failed to submit the stats :-(
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