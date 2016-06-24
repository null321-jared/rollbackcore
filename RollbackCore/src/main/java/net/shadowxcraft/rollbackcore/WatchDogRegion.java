/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
*/

package net.shadowxcraft.rollbackcore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

import net.shadowxcraft.rollbackcore.events.EndStatus;
import net.shadowxcraft.rollbackcore.events.WDImportEndEvent;
import net.shadowxcraft.rollbackcore.events.WDRollbackEndEvent;

/**
 * A class used to setup regions that are supposed to be rollbacked later. It keeps track of block
 * changes, and puts things back to the way they were when rollback is called.
 * 
 * @since 2.0
 * @author lizardfreak321
 */
public class WatchDogRegion {

	// Stores all active watchdog regions.
	private static List<WatchDogRegion> watchDogs = new LinkedList<WatchDogRegion>();
	private Location min, max;						// The min and max of the region.
	private int rollbackTask = -1;					// The task ID of the running operation.
	// Where it stores the blocks' data for later repair.
	private List<BlockState> originalStates = new ArrayList<BlockState>();
	private final boolean originalWorldSaveSetting; // The original world save setting of the world.
	private final String prefix; 					// Used as the prefix in messages.

	/**
	 * This constructor creates a new temporary watchdog region for the variables specified.
	 * 
	 * @param min
	 *            The corner of the region with the lowest x y and z.
	 * @param max
	 *            the corner of the region with the greatest x y and z.
	 */
	public WatchDogRegion(Location min, Location max, String prefix) {
		this.min = min;
		this.max = max;
		this.originalWorldSaveSetting = min.getWorld().isAutoSave();
		this.prefix = prefix;
		watchDogs.add(this);
	}

	/**
	 * Cancels all running rollback tasks.
	 * 
	 * @return the number of canceled tasks.
	 */
	public static final int cancelAll() {
		int numberOfTasks = watchDogs.size();
		for (WatchDogRegion wd : watchDogs)
			if (wd.rollbackTask != -1) {
				Bukkit.getScheduler().cancelTask(wd.rollbackTask);
				new WDRollbackEndEvent(wd, 0, 0, EndStatus.FAIL_EXERNAL_TERMONATION);
				wd.min.getWorld().setAutoSave(wd.originalWorldSaveSetting);
				TaskManager.removeTask();
			}
		watchDogs.clear();
		return numberOfTasks;
	}

	/**
	 * First, finds the WatchDog(s) that the block is a part of. Adds the block to the RAM storage
	 * if the block's location is not already there.
	 * 
	 * @param state
	 *            The blockState that should be saved.
	 */
	public final static void logBlock(BlockState state) {
		for (WatchDogRegion watchDog : watchDogs) {
			if (watchDog.isInRegion(state.getLocation())) {
				watchDog.addState(state);
			}
		}
	}

	/**
	 * First, finds the WatchDog(s) that the block is a part of. Adds the block to the RAM storage
	 * if the block's location is not already there.
	 * 
	 * @param block
	 *            The block that should be saved.
	 */
	public final static void logBlock(Block block) {
		logBlock(block.getState());
	}

	/**
	 * First, finds the WatchDog(s) that the block is a part of. Adds the block to the RAM storage
	 * if the block's location is not already there.
	 * 
	 * @param state
	 *            The blockState that should be saved.
	 */
	protected final void addState(BlockState state) {
		int index = 0;				// The index of the position in originalStates.
		boolean originalLoc = true; // Keeps track of if the block's location is already saved.
		// Loops through all of the existing states to make sure it isn't already there.
		while (index < originalStates.size() && originalLoc) {
			Location loc1 = state.getLocation();
			Location loc2 = originalStates.get(index).getLocation();
			if (loc1.getBlockX() == loc2.getBlockX() && loc1.getBlockY() == loc2.getBlockY()
					&& loc1.getBlockZ() == loc2.getBlockZ() && loc1.getWorld().equals(loc2.getWorld())) {
				originalLoc = false;
			}
			index++;
		}
		// Adds it if it is original/unique.
		if (originalLoc)
			originalStates.add(state);
	}

	/**
	 * Used to check if the location is in the WatchDog's region.
	 * 
	 * @param loc
	 *            The location being tested.
	 * @return If the Location is in the region.
	 */
	public final boolean isInRegion(Location loc) {
		return Utilities.isInRegion(loc, min, max);
	}

	/**
	 * Used to allow a player to rollback the watchdog(s) the player is in.
	 * 
	 * @param sender
	 *            The CommandSender that is issuing the command. Should be a player.
	 */
	public final static void playerRollback(CommandSender sender) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			int count = 0;

			// Checks every watchdog region to see if the player is in it.
			for (WatchDogRegion watchDog : watchDogs) {
				if (watchDog.isInRegion(player.getLocation())) {
					count++;
					sender.sendMessage(Main.prefix + "Detected you are in a region! Rolling back all "
							+ watchDog.originalStates.size() + " blocks..");
					watchDog.rollBack(sender, true, true);
				}
			}
			if (count == 0) {
				sender.sendMessage(Main.prefix + "You are not in a WatchDog region!");
			}
		} else {
			sender.sendMessage(Main.prefix + "Only players can use this command!");
		}
	}

	/**
	 * Used to allow a player to export the watchdog(s) the player is in.
	 * 
	 * @param sender
	 *            The CommandSender that is issuing the command. Should be a player.
	 */
	public final static void playerExport(CommandSender sender, String fileName) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			int count = 0;

			// Checks every watchdog region to see if the player is in it.
			for (WatchDogRegion watchDog : watchDogs) {
				if (watchDog.isInRegion(player.getLocation())) {
					count++;
					sender.sendMessage(watchDog.prefix + "Detected you are in a region! Exporting all "
							+ watchDog.originalStates.size() + " blocks..");
					try {
						if (count == 1)
							watchDog.export(fileName);
						else
							// In case they are in several WD regions.
							watchDog.export(fileName + count);
					} catch (IOException e) {
						sender.sendMessage(watchDog.prefix + "Failed.");
						e.printStackTrace();
					}
				}
			}
			if (count == 0) {
				sender.sendMessage(Main.prefix + "You are not in a WatchDog region!");
			}
		} else {
			sender.sendMessage(Main.prefix + "Only players can use this command!");
		}
	}

	/**
	 * Allows a player to create a watchdog region using their worldedit region.
	 * 
	 * @param player
	 *            The player that is creating the watchdog.
	 * @return The region that is created. Null if they have no valid region.
	 */
	public final static WatchDogRegion playerCreateWatchDog(Player player) {
		// Uses worldedit to get the player's region.
		WorldEditPlugin worldEditPlugin = null;
		WatchDogRegion createdWatchdog = null;
		worldEditPlugin = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		if (worldEditPlugin == null) {
			player.sendMessage(Main.prefix + "Error with region command! Error: WorldEdit is null.");
			return null;
		}

		Selection sel = worldEditPlugin.getSelection(player);

		// Checks if they have a selection
		if (sel instanceof CuboidSelection) {
			Vector min = sel.getNativeMinimumPoint();
			Vector max = sel.getNativeMaximumPoint();
			Location minLoc = new Location(sel.getWorld(), min.getBlockX(), min.getBlockY(), min.getBlockZ());
			Location maxLoc = new Location(sel.getWorld(), max.getBlockX(), max.getBlockY(), max.getBlockZ());
			createdWatchdog = new WatchDogRegion(minLoc, maxLoc, Main.prefix);
			player.sendMessage(Main.prefix + "WatchDog region created!");
		} else {
			// This means there was no selection, so it skips copying and
			// tells the player.
			player.sendMessage(ChatColor.DARK_RED + "Invalid Selection!");
		}

		return createdWatchdog;
	}

	// Used to sort the blockstates by height, since it prevents a lot of issues.
	protected final static Comparator<BlockState> byHeight = new Comparator<BlockState>() {
		@Override
		public int compare(BlockState o1, BlockState o2) {
			return o1.getY() - o2.getY();
		}
	};

	/**
	 * Used to rollback the watchdog region.
	 * 
	 * @param sender
	 *            The person who receives the messages. Null if no one.
	 * @param prefix
	 *            The prefixes of the sent messages.
	 * @param clearEntities
	 *            If entities should be cleared
	 * @param quickClearEntities
	 *            If the entity clear process should only check loaded chunks. Significantly faster
	 *            but less thorough. Irrelevant if clearEntities is false..
	 */
	public final void rollBack(final CommandSender sender, final boolean clearEntities,
			final boolean quickClearEntities) {
		final int size = originalStates.size();
		final long beginTime = System.nanoTime();
		final WatchDogRegion wd = this;
		final ClearEntities clearing;
		// Sorts by height to prevent issues with blocks that are effected by gravity.
		Collections.sort(originalStates, byHeight);
		if (clearEntities) {
			// In case there are entities that can cause problems, like lit TNT.
			clearing = new ClearEntities(min, max, null, quickClearEntities).progressiveClearEntities();
		} else {
			clearing = null;
		}

		// Disables auto saving to increase performance.
		min.getWorld().setAutoSave(false);
		if (size > 0) {
			TaskManager.addTask();

			rollbackTask = Main.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(Main.plugin,
					new Runnable() {
						int index = 0;
						long tick = 0;

						@Override
						public void run() {
							tick++;
							long startTime = System.nanoTime();

							while ((clearing == null || clearing.isDone()) && index < size
									&& System.nanoTime() - startTime < TaskManager.getMaxTime() * 1000000) {
								// The update puts it back to the way it was.
								try {
									originalStates.get(index).update(true, false);
								} catch (NoSuchMethodError e) {
									originalStates.get(index).update(true);
								}
								index++;
							}

							// Lets the user know how it is going.
							if (sender != null && tick % 100 == 0) {
								if (clearing == null || clearing.isDone())
									sender.sendMessage(prefix + new DecimalFormat("#.00").format(index / (double) size)
											+ "% done with Rollback operation.");
								else
									sender.sendMessage(prefix
											+ "The rollback operation will complete once the entities are cleared.");
							}
							// finishes things up once it completes.
							if (index >= size) {
								Bukkit.getScheduler().cancelTask(rollbackTask);
								rollbackTask = -1;
								originalStates.clear();
								min.getWorld().setAutoSave(originalWorldSaveSetting);
								if (sender != null) {
									sender.sendMessage(prefix + "Done with rollback!");
								}
								TaskManager.removeTask();
								new WDRollbackEndEvent(wd, System.nanoTime() - beginTime, size, EndStatus.SUCCESS);
							}
						}
					}, 1, 1);
		}
	}

	/**
	 * Exports all temporarily stored blocks to the file directory specified.
	 * 
	 * @param fileName
	 *            The file name (without extension) and directory that the file will be stored in.
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	public final void export(String fileName) throws IOException {
		final short VERSION = 1;
		BufferedOutputStream out;
		File file = new File(fileName + ".wdbackup");
		file.createNewFile();
		file.mkdirs();
		out = new BufferedOutputStream(new FileOutputStream(file));

		// Writes everything about the region.
		FileUtilities.writeShort(out, VERSION);
		out.write(this.min.getWorld().getName().length());
		out.write(this.min.getWorld().getName().getBytes());
		FileUtilities.writeInt(out, min.getBlockX());
		FileUtilities.writeInt(out, min.getBlockY());
		FileUtilities.writeInt(out, min.getBlockZ());
		FileUtilities.writeInt(out, max.getBlockX());
		FileUtilities.writeInt(out, max.getBlockY());
		FileUtilities.writeInt(out, max.getBlockZ());

		// Writes every single block that needs reverting.
		for (BlockState state : originalStates) {
			FileUtilities.writeShort(out, state.getX() - min.getBlockX());
			FileUtilities.writeShort(out, state.getY() - min.getBlockY());
			FileUtilities.writeShort(out, state.getZ() - min.getBlockZ());
			FileUtilities.writeIDAndData(out, state.getTypeId(), state.getRawData());
		}

		out.close();
	}

	/**
	 * Used to import the backup into a watchdog. It will create one if there is not one that
	 * matches the coordinates, and if there is one that matches it will import it into that. After
	 * a restart, import AFTER re-creating the regions in case there is one that has the same region
	 * so there are no duplicates.
	 * 
	 * @param fileName
	 *            The file name/directory of the backup
	 * @param sender
	 *            The person who will receive the messages. Optional. Use null for none.
	 * @param prefix
	 *            The prefix used for all messages. Do not use null.
	 * @return The region that will be importing into.
	 */
	public final static WatchDogRegion importWatchDog(String fileName, CommandSender sender, String prefix) {
		File file = new File(fileName + ".wdbackup");
		BufferedInputStream in = null;
		int worldNameLength;
		String worldName = "";
		World world;
		Location min, max;
		WatchDogRegion exportedTo = null;
		WatchDogRegion tempRegion;

		if (!file.exists()) {
			new WDImportEndEvent(null, 0, 0, EndStatus.FILE_NO_SUCH_FILE, sender);
			return null;
		}

		try {
			in = new BufferedInputStream(new FileInputStream(file));
			final short VERSION = (short) FileUtilities.readShort(in);
			if (VERSION != 1) {
				in.close();
				new WDImportEndEvent(null, 0, 0, EndStatus.FAIL_INCOMPATIBLE_VERSION, sender);

				return null;
			}
			// Gets and checks the world.
			worldNameLength = in.read();
			for (int i = 0; i < worldNameLength; i++)
				worldName += (char) in.read();
			world = Bukkit.getWorld(worldName);
			if (world == null) {
				in.close();
				new WDImportEndEvent(null, 0, 0, EndStatus.FAIL_UNKNOWN_WORLD, sender);
				return null;
			}
			// Gets the min and max locations of the region.
			min = new Location(world, FileUtilities.readInt(in), FileUtilities.readInt(in), FileUtilities.readInt(in));
			max = new Location(world, FileUtilities.readInt(in), FileUtilities.readInt(in), FileUtilities.readInt(in));

			// Looks for an existing watchdog that has the same region
			for (int index = 0; index < watchDogs.size() && exportedTo == null; index++) {
				tempRegion = watchDogs.get(index);
				if (tempRegion.min.equals(min) && tempRegion.max.equals(max))
					exportedTo = tempRegion;
			}
			if (exportedTo == null) {
				exportedTo = new WatchDogRegion(min, max, prefix);
			}
			// Starts the operation.
			new importOperation(in, min, world, exportedTo, sender);

		} catch (IOException e) {
			e.printStackTrace();
			try {
				in.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			new WDImportEndEvent(null, 0, 0, EndStatus.FAIL_IO_ERROR, sender);

			return null;
		}

		return exportedTo;
	}

	/**
	 * Used to clear all of the block data in the region.
	 */
	public final void reset() {
		originalStates.clear();
	}

	/**
	 * Call this method when you no longer want to use it. It clears it and removes it from the list
	 * that it keeps up to date.
	 */
	public final void remove() {
		originalStates = null;
		watchDogs.remove(this);
	}

	public String getPrefix() {
		return prefix;
	}
}

/**
 * Used by the importWatchDog to import the blocks. Import is a lot more intensive than export, so
 * it needs to be done progressively.
 * 
 * @author lizardfreak321
 */
class importOperation extends BukkitRunnable {

	private InputStream in; 		// The inputstream the backup is being read from.
	private Location min;			// The max location of the region being imported
	private Location blockLocation; // A location used in the process.
	BlockState state;				// A state variable used in the process.
	WatchDogRegion exportedTo;	// The watchdog region it is being saved to.
	CommandSender sender;
	long startTime = System.nanoTime();
	int blocksImported = 0;

	importOperation(InputStream in, Location min, World world, WatchDogRegion exportedTo, CommandSender sender) {
		this.in = in;
		blockLocation = new Location(world, 0, 0, 0);
		this.min = min;
		this.exportedTo = exportedTo;
		this.runTaskTimer(Main.plugin, 1, 1);
		TaskManager.addTask();
		this.sender = sender;
	}

	@SuppressWarnings("deprecation")
	@Override
	public final void run() {
		int tempDataID;
		long time = System.nanoTime();
		try {
			// Loops until it runs out of stuff to import, or time.
			while ((in.available() > 7) && ((System.nanoTime() - time) < TaskManager.getMaxTime() * 1000000)) {
				// Gets the X Y and Z from the file.
				blockLocation.setX(min.getX() + FileUtilities.readShort(in));
				blockLocation.setY(min.getY() + FileUtilities.readShort(in));
				blockLocation.setZ(min.getZ() + FileUtilities.readShort(in));
				// Gets the data, stored in two bytes using bitwise operators.
				tempDataID = FileUtilities.readShort(in);
				// Gets the state at the location
				state = blockLocation.getBlock().getState();
				// Updates its type to what it is in the backup.
				// The bitwise operators are used to properly read the compressed data.
				state.setTypeId(tempDataID >> 4);
				state.setRawData((byte) ((tempDataID) & 15));
				// Adds it to the watchdog region.
				exportedTo.addState(state);
				blocksImported++;
			}
			if (in.available() < 8) {
				// Ends it once it finishes.
				end(EndStatus.SUCCESS);
			}
		} catch (IOException e) {
			end(EndStatus.FAIL_IO_ERROR);
		}

	}

	public void end(EndStatus endStatus) {
		// Closes in to save resources.
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		TaskManager.removeTask();
		new WDImportEndEvent(exportedTo, System.nanoTime() - startTime, blocksImported, endStatus, sender);
		// Cancels the task because it is done.
		this.cancel();

	}

}
