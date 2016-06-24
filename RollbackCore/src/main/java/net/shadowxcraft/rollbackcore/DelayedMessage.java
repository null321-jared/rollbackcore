/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
*/

package net.shadowxcraft.rollbackcore;

import org.bukkit.command.CommandSender;

public class DelayedMessage implements Runnable {
	public void run() {
		sender.sendMessage(message);
	}

	private String message;
	private CommandSender sender;

	public DelayedMessage(String message, CommandSender sender) {
		this.message = message;
		this.sender = sender;
	}
}
