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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4Factory;
import net.shadowxcraft.rollbackcore.events.EndStatus;
import net.shadowxcraft.rollbackcore.events.PasteEndEvent;

/**
 * This class is used to paste a saved rollbackcore copy file.
 * 
 * @see PasteEndEvent
 * @author lizardfreak321
 */
public class Paste extends RollbackOperation {
	private static final List<Paste> runningPastes = new ArrayList<Paste>();

	private int maxX, maxY, maxZ;
	private int sizeX, sizeY, sizeZ;
	// Used to store the pastes so that this class can start a new paste in
	// distributed pastes.
	private ArrayList<Paste> pastes = null;
	// Keeps track of the old world save setting because it is disabled during
	// pasting.
	private final boolean originalWorldSaveSetting;
	// Keeps track of if entities should be cleared.
	private final boolean clearEntities;
	private final boolean ignoreAir;
	private boolean updateFailed = false;
	// Stored for efficiency.
	private static BlockData air = Bukkit.createBlockData(Material.AIR),
			caveAir = Bukkit.createBlockData(Material.CAVE_AIR);
	private InputStream in;
	private File file;
	private long startTime = -1l;
	boolean inProgress = false;
	private int copyVersion;

	// Specific to the operation at hand.
	long tick = 0; // Used to keep track of how many ticks the copy operation has run.
	long blockIndex = 0, blocksChanged;// Used to store the index of the block, for
										// statistical reasons.
	// TODO: Optimize the cache and the loading of the data.
	// Stores the blockdata for the IDs.
	private HashMap<Integer, BlockCache<BlockData>> dataCache = new HashMap<Integer, BlockCache<BlockData>>();

	/**
	 * The legacy constructor for backwards compatibility.
	 * 
	 * @param x        Where the min-x of the paste will be pasted.
	 * @param y        Where the min-y of the paste will be pasted.
	 * @param z        Where the min-z of the paste will be pasted.
	 * @param world    What world the paste will be pasted in.
	 * @param fileName The directory of the files.
	 * @param pastes   An optional ArrayList of pastes that will run after this one,
	 *                 allowing them to be distributed. Set to null if you don't
	 *                 want a paste to follow.
	 * @param sender   The person who will get status messages. Use null for no
	 *                 messages, and consoleSender for console.
	 */
	public Paste(int x, int y, int z, World world, String fileName, ArrayList<Paste> pastes, CommandSender sender) {
		this(new Location(world, x, y, z), fileName, sender, false, false, Main.prefix);
		this.pastes = pastes;
	}

	/**
	 * Used to create a new paste operation instance that can be used to paste the
	 * file.
	 * 
	 * @param min           Where the min coordinates and world of the paste that
	 *                      will be pasted.
	 * @param fileName      The path of the file
	 * @param sender        The person who will get status messages. Use null for no
	 *                      messages, and consoleSender for console.
	 * @param clearEntities Used to specify if the paste operation will schedule the
	 *                      removal of the entities.
	 * @param prefix        Used for the prefix shown in the messages.
	 * @param ignoreAir     Not check blocks that are air in the file. May be useful
	 *                      for some plugins.
	 */

	public Paste(Location min, String fileName, CommandSender sender, boolean clearEntities, boolean ignoreAir,
			String prefix) {
		super(min, true);

		this.originalWorldSaveSetting = min.getWorld().isAutoSave();
		min.getWorld().setAutoSave(false);
		if (!fileName.contains(".")) {
			fileName += ".dat";
			if (!fileName.contains("/") || !fileName.contains("\\")) {
				fileName = Main.plugin.getDataFolder().getAbsolutePath() + "/saves/" + fileName;
			}
		}
		this.fileName = fileName;
		this.sender = sender;
		this.prefix = prefix;
		this.clearEntities = clearEntities;
		this.ignoreAir = ignoreAir;
	}

	public static int cancelAll() {
		int numberOfTasks = runningPastes.size();
		for (int i = 0; i < numberOfTasks; i++)
			runningPastes.get(i).end(EndStatus.FAIL_EXERNAL_TERMINATION);
		return numberOfTasks;
	}

	/**
	 * Internal use only. It lets it know that the update was unable to fully update
	 * it to the current version. It will now no longer attempt to update blockdata,
	 * and will instead handle unknown blocks with a default block.
	 */
	void reportIncompleteUpdate() {
		Main.plugin.getLogger().warning("Incomplete update reported for paste.");
		updateFailed = true;
	}

	/**
	 * Runs the paste operation.
	 */
	@Override
	public void run() {
		// Calls the copy method.
		if (!inProgress) {
			initPaste();
		}
		pasteTask();
	}

	// The internal method to start the copy operation.
	protected final boolean initPaste() {
		if (startTime == -1l)
			startTime = System.nanoTime();
		lastTime = startTime;
		// Checks if there are any currently running pastes of the exact same thing.
		for (Paste runningPaste : runningPastes) {
			if (runningPaste.fileName.equals(fileName) && runningPaste.minX == minX && runningPaste.minY == minY
					&& runningPaste.minZ == minZ) {
				end(EndStatus.FAIL_DUPLICATE);
				return false;
			}
		}

		if (!initializeFile()) {
			return false;
		}

		if (!readFile()) {
			return false;
		}

		if (clearEntities) {
			new ClearEntities(new Location(world, minX, minY, minZ), new Location(world, maxX, maxY, maxZ), null, false)
					.progressiveClearEntities();
		}

		runningPastes.add(this);

		// Schedules the repeating task for the pasting.
		TaskManager.addTask();
		task = Bukkit.getScheduler().runTaskTimer(Main.plugin, this, 1, 1);
		inProgress = true;
		return true;
	}

	private final boolean initializeFile() {
		// Initializes the file.
		file = new File(fileName);

		if (!file.exists()) {
			end(EndStatus.FAIL_NO_SUCH_FILE);
			Main.plugin.getLogger().info("Could not find file " + file.getAbsolutePath());
			return false;
		}

		try {
			// Initializes the InputStream
			in = new FileInputStream(file);
		} catch (IOException e) {
			e.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}
		return true;
	}

	private final boolean readFile() {
		try {
			// In case the file they are trying to read is in of date or too
			// new.
			copyVersion = in.read();
			CompressionType compression = null;

			if (copyVersion > VERSION) {
				end(EndStatus.FAIL_INCOMPATIBLE_VERSION);
				return false;
			} else if (copyVersion < VERSION) {
				// Initializes conversion.
				in.close();
				LegacyUpdater.convert(fileName, this);
				if (sender != null) {
					sender.sendMessage(prefix
							+ "Converting region! This may take some time for larger files. The operation will resume if conversion succeeded.");
				}

				return false;
			}

			// Reads the sizes using readShort because it can be larger than 255
			sizeX = FileUtilities.readShort(in);
			sizeY = FileUtilities.readShort(in);
			sizeZ = FileUtilities.readShort(in);
			maxX = minX + sizeX;
			maxY = minY + sizeY;
			maxZ = minZ + sizeZ;

			int keyLength;
			byte[] bytes = new byte[255];
			while ((keyLength = in.read()) != 0) {
				String key = FileUtilities.readString(in, keyLength, bytes);
				int valueLength = in.read();

				switch (key) {
				case "compression":
					String compressionTypeName = FileUtilities.readString(in, valueLength, bytes);
					try {
						compression = CompressionType.valueOf(compressionTypeName);
					} catch (IllegalArgumentException e) {
						end(EndStatus.FAIL_UNKNOWN_COMPRESSION);
					}
					break;
				case "minecraft_version":
					String version = FileUtilities.readString(in, valueLength, bytes);
					if (!version.equalsIgnoreCase(CURRENT_MC_VERSION)) {
						Main.plugin.getLogger().warning("File was written in MC version " + version
								+ ", but you are running " + CURRENT_MC_VERSION);
						if (!updateFailed) {
							in.close();
							LegacyUpdater.updateModernBlockData(fileName, this);
							return false;
						}
					}
					break;
				default:
					in.skip(valueLength); // Don't assume it's a string.
					Main.plugin.getLogger().warning(
							"File being read has an unknown key \"" + key + "\". Was it written with another program?");
				}
			}

			if (compression != null) {
				switch (compression) {
				case LZ4:
					in = new LZ4BlockInputStream(in, LZ4Factory.safeInstance().fastDecompressor());
				case NONE:
					in = new BufferedInputStream(in);
				}
				// in = new InflaterInputStream(in); // Corrupts data
				// in = new GZIPInputStream(in); // Same
			} else {
				end(EndStatus.FAIL_UNKNOWN_COMPRESSION);
				return false;
			}

		} catch (IOException e1) {
			e1.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
			return false;
		}

		initChunkUnloading(maxZ);

		return true;
	}

	/**
	 * Ends the paste task if it is done or not. Sets everything back to the way it
	 * should be and closes open resources.
	 */
	public final void kill() {
		end(EndStatus.FAIL_EXERNAL_TERMINATION);
	}

	protected final void end(EndStatus endStatus) {
		world.setAutoSave(originalWorldSaveSetting);

		inProgress = false;
		TaskManager.removeTask();

		runningPastes.remove(this);
		// Closes the resource to close resources.
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (task != null && !task.isCancelled()) {
			task.cancel();
			task = null;
		}
		if (endStatus.equals(EndStatus.SUCCESS) && pastes != null && pastes.size() > 1) {
			// This is for the legacy distributed pastes.
			// Checks to see if there is another paste task to run.
			// Removes from the ArrayList because it's scheduling it.
			pastes.remove(0);

			Paste nextPaste = pastes.get(0);
			nextPaste.startTime = this.startTime;
			nextPaste.blocksChanged = blocksChanged;
			// Schedules it.
			Bukkit.getScheduler().runTaskLater(Main.plugin, nextPaste, 1);
		} else {
			new BukkitRunnable() {
				public void run() {
					new PasteEndEvent(Paste.this, System.nanoTime() - startTime, blocksChanged, endStatus);
				}
			}.runTask(Main.plugin);
		}
	}

	// ---------- Task ----------

	private final void pasteTask() {
		long startTime = System.currentTimeMillis(); // Used to keep track of time.
		tick++; // Increments the tick variable.

		boolean skip = false; // To know when to quit the loop for that tick.

		while (!skip && inProgress) {
			for (int i = 0; inProgress && i < 250; i++) {
				nextBlock();
				if (tempX > maxX) {
					end(EndStatus.SUCCESS);
				}
			}
			// Checks if it has run out of time.
			if (System.currentTimeMillis() - startTime > TaskManager.getMaxTime()) {
				skip = true;
			}
		}

		statusMessage(maxX, maxY, maxZ, blockIndex, tick, "paste");
	}

	int id = -1;
	private BlockCache<BlockData> lastData = null;
	private int count = 0;
	private final byte[] bytes = new byte[1000];

	private final void nextBlock() {
		if (tempY == minY) { // For efficiency.
			nextLocation(tempX, tempZ);
		}

		if (count < 1) {
			try {
				id = in.read();
				if (id == -1) {
					end(EndStatus.FILE_END_EARLY);
					return;
				}
				if (id == 0) {
					// New block

					// Reads whether or not there is extra data.
					boolean hasExtraData = in.read() == 1;
					// Reads the string.
					String blockDataString = FileUtilities.readShortString(in, bytes);
					// Reads the new ID
					id = in.read();
					BlockData blockData;
					try {
						blockData = Bukkit.createBlockData(blockDataString);
					} catch (Exception e) {
						blockData = Material.STONE.createBlockData();
						Main.plugin.getLogger().warning("Unable to create BlockData for \"" + blockDataString
								+ "\"! Defaulting to stone. This problem is caused by either RollbackCore being "
								+ "unable to update the file to the newest version, or the update data being incorrect.");
					}

					lastData = new BlockCache<BlockData>(id, hasExtraData, blockData);
					dataCache.put(id, lastData);

				} else {
					lastData = dataCache.get(id);
				}
				if (!lastData.hasExtraData) {
					count = in.read();
					if (count == 0) {
						count = FileUtilities.readShort(in);
					}
				} else {
					count = 1;
				}
			} catch (IOException e) {
				end(EndStatus.FAIL_IO_ERROR);
				e.printStackTrace();
			}
		}
		count--;

		if (!ignoreAir || (!lastData.data.equals(air) && !lastData.data.equals(caveAir))) {
			Block currentBlock = world.getBlockAt(tempX, tempY, tempZ);
			if (lastData.hasExtraData || !currentBlock.getBlockData().equals(lastData.data)) {
				currentBlock.setBlockData(lastData.data, false);
				if (lastData.hasExtraData) {
					updateBlockState(currentBlock);
				}
				blocksChanged++;
			}
		}

		updateVariables();
	}

	private final void updateBlockState(Block block) {
		try {
			int length = FileUtilities.readShort(in);
			switch (lastData.data.getMaterial()) {
			case OAK_WALL_SIGN:
			case SPRUCE_WALL_SIGN:
			case BIRCH_WALL_SIGN:
			case ACACIA_WALL_SIGN:
			case JUNGLE_WALL_SIGN:
			case DARK_OAK_WALL_SIGN:
			case CRIMSON_WALL_SIGN:
			case WARPED_WALL_SIGN:
			case OAK_SIGN:
			case SPRUCE_SIGN:
			case BIRCH_SIGN:
			case JUNGLE_SIGN:
			case ACACIA_SIGN:
			case DARK_OAK_SIGN:
			case CRIMSON_SIGN:
			case WARPED_SIGN:
				Sign sign = (Sign) block.getState();
				String allLines = FileUtilities.readShortString(in, bytes);
				String[] lines = allLines.split("\\n");
				int numLines = lines.length;
				for (int i = 0; i < numLines; i++) {
					sign.setLine(i, lines[i]);
				}
				sign.update(true, false);
				break;
			case PLAYER_HEAD:
			case PLAYER_WALL_HEAD:
				Skull skull = (Skull) block.getState();
				if (in.read() > 0) { // Has owner
					UUID uuid = UUID.fromString(FileUtilities.readShortString(in));
					skull.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
					skull.update(true, false);
				} else {
					if (skull.hasOwner()) {
						// Set to steve
						skull.setOwningPlayer(
								Bukkit.getOfflinePlayer(UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7")));
						skull.update(true, false);
					}
				}
				break;
			case COMMAND_BLOCK:
			case CHAIN_COMMAND_BLOCK:
			case REPEATING_COMMAND_BLOCK:
				CommandBlock cBlock = (CommandBlock) block.getState();

				int nameLength = FileUtilities.readShort(in);
				String name = FileUtilities.readString(in, nameLength);
				int commandLength = FileUtilities.readShort(in);
				String command = FileUtilities.readString(in, commandLength);

				if (cBlock.getName() != name || cBlock.getCommand() != command) {
					cBlock.setName(name);
					cBlock.setCommand(command);
					cBlock.update();
				}

				break;
			default:
				Main.plugin.getLogger().warning(
						"File specifies blockstate data, but RollbackCore does not know how to interpret it. Skipping.");
				in.skip(length); // To ensure it moves forward as it should.
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
			end(EndStatus.FAIL_IO_ERROR);
		}

	}

	private final void updateVariables() {
		// Updates variables.
		blockIndex++;
		// Move Z
		tempZ++;

		// Wrap Z
		if (tempZ > maxZ) {
			tempZ = minZ;
			// Move Y
			tempY++;

			// Wrap Y
			if (tempY > maxY) {
				tempY = minY;

				// Move X - does not need wrapping
				tempX += 1;
			}
		}
	}

}