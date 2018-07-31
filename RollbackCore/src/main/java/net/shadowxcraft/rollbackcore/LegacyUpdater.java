package net.shadowxcraft.rollbackcore;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.shadowxcraft.rollbackcore.events.EndStatus;

public class LegacyUpdater {
	static BlockData[][] idMappings;

	public static void loadMappings(final Plugin plugin) {
		new BukkitRunnable() {
			public void run() {
				InputStream in = plugin.getResource("legacy_mappings.yml");
				Reader reader = new InputStreamReader(in);

				YamlConfiguration config = new YamlConfiguration();
				idMappings = new BlockData[256][];
				try {
					config.load(reader);
					for (String idKey : config.getKeys(false)) {
						ConfigurationSection section = config.getConfigurationSection(idKey);
						Set<String> dataKeys = section.getKeys(false);

						BlockData[] data = new BlockData[dataKeys.size()];

						for (String dataKey : dataKeys) {
							data[Integer.parseInt(dataKey)] = Bukkit
									.createBlockData(section.getString(dataKey));
						}
						idMappings[Integer.parseInt(idKey)] = data;
					}

				} catch (IOException | InvalidConfigurationException e) {
					plugin.getLogger().warning("Unable to load mappings! Corrupt jar?");
					e.printStackTrace();
				}
			}
		}.runTaskAsynchronously(plugin);
	}

	/**
	 * The list of blocks that the plugin skipped saving the data for in file format
	 * 1 for backwards compatibility. It is updated to Minecraft 1.9 with some
	 * missing blocks. Must be sorted.
	 */
	final static int[] version1Blocks = { 0, 2, 4, 7, 13, 14, 15, 16, 20, 21, 22, 23, 30, 37, 39,
			40, 41, 42, 45, 46, 47, 48, 49, 51, 52, 56, 57, 58, 70, 72, 73, 74, 79, 80, 81, 82, 83,
			84, 87, 88, 89, 101, 102, 103, 112, 113, 116, 117, 118, 121, 122, 123, 124, 129, 133,
			137, 138, 147, 148, 152, 153, 165, 166, 169, 172, 173, 174, 188, 189, 190, 191, 192,
			201, 202, 206 };

	/**
	 * List of blocks that the plugin will skip in the newest version of the plugin.
	 * Currently updated to Minecaft 1.10 Must be sorted.
	 * 
	 * @since 2.0
	 */
	final static int[] latestSimpleBlocks = { 0, 2, 4, 7, 13, 14, 15, 16, 20, 21, 22, 23, 30, 37,
			39, 40, 41, 42, 45, 47, 48, 49, 51, 52, 56, 57, 58, 70, 72, 73, 74, 79, 80, 81, 82, 83,
			84, 87, 88, 89, 101, 102, 103, 112, 113, 116, 117, 118, 121, 122, 123, 124, 129, 133,
			137, 138, 147, 148, 152, 153, 165, 166, 169, 172, 173, 174, 188, 189, 190, 191, 192,
			201, 202, 206, 208, 209, 213, 214, 215 };

	/**
	 * This method returns if the block is "simple", meaning its data value doesn't
	 * need to be stored. For internal use only.
	 * 
	 * @param id           The ID of the block being tested for the simple property.
	 * @param simpleBlocks The list of characters (Used instead of bytes since chars
	 *                     are unsigned) that are considered "Simple"
	 * @return If the block is simple
	 */
	protected static final boolean isSimple(int id, int[] simpleBlocks) {
		// Binary search is very efficient, but it must be sorted.
		return Arrays.binarySearch(simpleBlocks, id) >= 0;
	}

	public static final String[] legacyMaterialNames = new String[] { "AIR", "STONE", "GRASS",
			"DIRT", "COBBLESTONE", "WOOD", "SAPLING", "BEDROCK", "WATER", "STATIONARY_WATER",
			"LAVA", "STATIONARY_LAVA", "SAND", "GRAVEL", "GOLD_ORE", "IRON_ORE", "COAL_ORE", "LOG",
			"LEAVES", "SPONGE", "GLASS", "LAPIS_ORE", "LAPIS_BLOCK", "DISPENSER", "SANDSTONE",
			"NOTE_BLOCK", "BED_BLOCK", "POWERED_RAIL", "DETECTOR_RAIL", "PISTON_STICKY_BASE", "WEB",
			"LONG_GRASS", "DEAD_BUSH", "PISTON_BASE", "PISTON_EXTENSION", "WOOL",
			"PISTON_MOVING_PIECE", "YELLOW_FLOWER", "RED_ROSE", "BROWN_MUSHROOM", "RED_MUSHROOM",
			"GOLD_BLOCK", "IRON_BLOCK", "DOUBLE_STEP", "STEP", "BRICK", "TNT", "BOOKSHELF",
			"MOSSY_COBBLESTONE", "OBSIDIAN", "TORCH", "FIRE", "MOB_SPAWNER", "WOOD_STAIRS", "CHEST",
			"REDSTONE_WIRE", "DIAMOND_ORE", "DIAMOND_BLOCK", "WORKBENCH", "CROPS", "SOIL",
			"FURNACE", "BURNING_FURNACE", "SIGN_POST", "WOODEN_DOOR", "LADDER", "RAILS",
			"COBBLESTONE_STAIRS", "WALL_SIGN", "LEVER", "STONE_PLATE", "IRON_DOOR_BLOCK",
			"WOOD_PLATE", "REDSTONE_ORE", "GLOWING_REDSTONE_ORE", "REDSTONE_TORCH_OFF",
			"REDSTONE_TORCH_ON", "STONE_BUTTON", "SNOW", "ICE", "SNOW_BLOCK", "CACTUS", "CLAY",
			"SUGAR_CANE_BLOCK", "JUKEBOX", "FENCE", "PUMPKIN", "NETHERRACK", "SOUL_SAND",
			"GLOWSTONE", "PORTAL", "JACK_O_LANTERN", "CAKE_BLOCK", "DIODE_BLOCK_OFF",
			"DIODE_BLOCK_ON", "STAINED_GLASS", "TRAP_DOOR", "MONSTER_EGGS", "SMOOTH_BRICK",
			"HUGE_MUSHROOM_1", "HUGE_MUSHROOM_2", "IRON_FENCE", "THIN_GLASS", "MELON_BLOCK",
			"PUMPKIN_STEM", "MELON_STEM", "VINE", "FENCE_GATE", "BRICK_STAIRS", "SMOOTH_STAIRS",
			"MYCEL", "WATER_LILY", "NETHER_BRICK", "NETHER_FENCE", "NETHER_BRICK_STAIRS",
			"NETHER_WARTS", "ENCHANTMENT_TABLE", "BREWING_STAND", "CAULDRON", "ENDER_PORTAL",
			"ENDER_PORTAL_FRAME", "ENDER_STONE", "DRAGON_EGG", "REDSTONE_LAMP_OFF",
			"REDSTONE_LAMP_ON", "WOOD_DOUBLE_STEP", "WOOD_STEP", "COCOA", "SANDSTONE_STAIRS",
			"EMERALD_ORE", "ENDER_CHEST", "TRIPWIRE_HOOK", "TRIPWIRE", "EMERALD_BLOCK",
			"SPRUCE_WOOD_STAIRS", "BIRCH_WOOD_STAIRS", "JUNGLE_WOOD_STAIRS", "COMMAND", "BEACON",
			"COBBLE_WALL", "FLOWER_POT", "CARROT", "POTATO", "WOOD_BUTTON", "SKULL", "ANVIL",
			"TRAPPED_CHEST", "GOLD_PLATE", "IRON_PLATE", "REDSTONE_COMPARATOR_OFF",
			"REDSTONE_COMPARATOR_ON", "DAYLIGHT_DETECTOR", "REDSTONE_BLOCK", "QUARTZ_ORE", "HOPPER",
			"QUARTZ_BLOCK", "QUARTZ_STAIRS", "ACTIVATOR_RAIL", "DROPPER", "STAINED_CLAY",
			"STAINED_GLASS_PANE", "LEAVES_2", "LOG_2", "ACACIA_STAIRS", "DARK_OAK_STAIRS",
			"SLIME_BLOCK", "BARRIER", "IRON_TRAPDOOR", "PRISMARINE", "SEA_LANTERN", "HAY_BLOCK",
			"CARPET", "HARD_CLAY", "COAL_BLOCK", "PACKED_ICE", "DOUBLE_PLANT", "STANDING_BANNER",
			"WALL_BANNER", "DAYLIGHT_DETECTOR_INVERTED", "RED_SANDSTONE", "RED_SANDSTONE_STAIRS",
			"DOUBLE_STONE_SLAB2", "STONE_SLAB2", "SPRUCE_FENCE_GATE", "BIRCH_FENCE_GATE",
			"JUNGLE_FENCE_GATE", "DARK_OAK_FENCE_GATE", "ACACIA_FENCE_GATE", "SPRUCE_FENCE",
			"BIRCH_FENCE", "JUNGLE_FENCE", "DARK_OAK_FENCE", "ACACIA_FENCE", "SPRUCE_DOOR",
			"BIRCH_DOOR", "JUNGLE_DOOR", "ACACIA_DOOR", "DARK_OAK_DOOR", "END_ROD", "CHORUS_PLANT",
			"CHORUS_FLOWER", "PURPUR_BLOCK", "PURPUR_PILLAR", "PURPUR_STAIRS", "PURPUR_DOUBLE_SLAB",
			"PURPUR_SLAB", "END_BRICKS", "BEETROOT_BLOCK", "GRASS_PATH", "END_GATEWAY",
			"COMMAND_REPEATING", "COMMAND_CHAIN", "FROSTED_ICE", "MAGMA", "NETHER_WART_BLOCK",
			"RED_NETHER_BRICK", "BONE_BLOCK", "STRUCTURE_VOID", "OBSERVER", "WHITE_SHULKER_BOX",
			"ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX", "LIGHT_BLUE_SHULKER_BOX",
			"YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX", "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX",
			"SILVER_SHULKER_BOX", "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX", "BLUE_SHULKER_BOX",
			"BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX", "RED_SHULKER_BOX", "BLACK_SHULKER_BOX",
			"WHITE_GLAZED_TERRACOTTA", "ORANGE_GLAZED_TERRACOTTA", "MAGENTA_GLAZED_TERRACOTTA",
			"LIGHT_BLUE_GLAZED_TERRACOTTA", "YELLOW_GLAZED_TERRACOTTA", "LIME_GLAZED_TERRACOTTA",
			"PINK_GLAZED_TERRACOTTA", "GRAY_GLAZED_TERRACOTTA", "SILVER_GLAZED_TERRACOTTA",
			"CYAN_GLAZED_TERRACOTTA", "PURPLE_GLAZED_TERRACOTTA", "BLUE_GLAZED_TERRACOTTA",
			"BROWN_GLAZED_TERRACOTTA", "GREEN_GLAZED_TERRACOTTA", "RED_GLAZED_TERRACOTTA",
			"BLACK_GLAZED_TERRACOTTA", "CONCRETE", "CONCRETE_POWDER", "STRUCTURE_BLOCK" };

	/**
	 * Asynchronously converts the old file to 1.13.
	 * 
	 * @param file         The file that is to be updated.
	 * @param pasteToStart The region that will be run once it is done. Null to
	 *                     disable.
	 */
	public static void convert(final String fileName, final Paste pasteToStart) {
		new BukkitRunnable() {

			File inFile, outFile;
			BufferedInputStream in;
			OutputStream out;
			LRUBlockDataCache cache;
			int[] simpleBlocks;
			int version, diffX, diffY, diffZ;

			public void run() {
				try {
					initFileRead();
					initFileWrite();
					convertMainData();
					in.close();
					renameFiles();
					if (pasteToStart != null)
						Main.plugin.getServer().getScheduler().runTask(Main.plugin, pasteToStart);
				} catch (FileNotFoundException e) {
					if (pasteToStart != null)
						pasteToStart.end(EndStatus.FAIL_NO_SUCH_FILE);
					e.printStackTrace();
					Main.plugin.getLogger()
							.warning("Failed when converting the copied region " + fileName);
				} catch (IOException e) {
					if (pasteToStart != null)
						pasteToStart.end(EndStatus.FAIL_IO_ERROR);
					e.printStackTrace();
					Main.plugin.getLogger()
							.warning("Failed when converting the copied region " + fileName);
				}
			}

			void initFileRead() throws IOException {
				inFile = new File(fileName);
				in = new BufferedInputStream(new FileInputStream(inFile));
				cache = new LRUBlockDataCache(1, 255); // Stores IDs for the
				version = in.read();

				// BlockData
				if (version != 0 && version != 1) {
					if (pasteToStart != null)
						pasteToStart.end(EndStatus.FAIL_INCOMPATIBLE_VERSION);
					else
						Main.plugin.getLogger().warning("Unable to convert paste file " + fileName);
					in.close();
					return;
				}
				if (version == 1) {
					int length = in.read();
					simpleBlocks = new int[length];
					for (int i = 0; i < length; i++)
						simpleBlocks[i] = (char) in.read();
				} else {
					simpleBlocks = version1Blocks;
				}

				// Reads the sizes using readShort because it can be larger than 255
				diffX = FileUtilities.readShort(in);
				diffY = FileUtilities.readShort(in);
				diffZ = FileUtilities.readShort(in);
			}

			void initFileWrite() throws IOException {
				// Initializes the file
				outFile = new File(fileName + "_temp");
				if (outFile.exists()) {
					// Deletes it if it exists so it starts over.
					outFile.delete();
				}

				// Creates the file.
				outFile.createNewFile();

				// Initializes the FileOutputStream.
				out = new FileOutputStream(outFile);

				out = Copy.startFile(out, diffX, diffY, diffZ);
			}

			boolean convertMainData() throws IOException {
				final int maxPosition = (1 + diffX) * (1 + diffY) * (1 + diffZ);
				BlockData lastData = null;
				int lastCount = 0;
				String[] lastLines = null;

				for (int position = 0; position < maxPosition;) {
					BlockData data = null;
					String[] lines = null;
					int compressCount = 0;
					if (position + lastCount < maxPosition) {
						// Gets the ID of the block.
						int currentId = in.read();
						int currentData;

						// For compression, it checks if this
						// block doesn't need data saved.
						if (isSimple(currentId, simpleBlocks)) {
							currentData = 0;
						} else {
							currentData = in.read();
						}

						// In some cases it reaches the end of
						// the file early. That normally happens
						// when there was a copy error or the
						// file got corrupted.
						if (currentId == -1 || currentData == -1) {
							Main.plugin.getLogger()
									.warning("File ended early in conversion to new version.");
							return false;
						}

						data = idMappings[currentId][currentData];

						// For compression, it reads how many
						// times this block is repeated.
						compressCount = in.read();

						// The following code is to read sign
						// text.
						if (compressCount == 0) {
							lines = getLines();
						}
					}

					if (data == null || !data.equals(lastData) || lastCount > 65280
							|| lastCount == 0) {
						position += lastCount;

						// Write the old IDs to file.
						LRUBlockDataCache.Node id = cache.get(lastData);
						if (id == null) {
							Material material = data.getMaterial();
							id = cache.add(data,
									material == Material.SIGN || material == Material.WALL_SIGN);
							// New block.

							String dataAsString = data.getAsString();

							// It was absent, so writes it to the file,
							// then increments the index.

							// START WITH 0 - Notes new data.
							out.write(0);
							// Next write whether or not there was extra data.
							out.write(id.hasExtraData ? 1 : 0);
							// Write the string
							FileUtilities.writeShortString(out, dataAsString);
							// Write the id value of the data.
							out.write(id.value);
						} else {
							out.write(id.value); // Now writes the index.
						}
						if (id.hasExtraData) {
							try {
								// Just signs
								String allLines = lastLines[0];
								for (int i = 1; i < 4; i++) {
									allLines += '\n' + lastLines[i];
								}
								FileUtilities.writeShort(out, allLines.length() + 1);
								FileUtilities.writeShortString(out, allLines);
							} catch (IndexOutOfBoundsException e) {
								e.printStackTrace();
								FileUtilities.writeShort(out, 0); // To prevent corruption of
																	// the output.
							}
						} else {
							// Write the count.
							if (lastCount <= 255) {
								out.write(lastCount); // Writes the last one's count
							} else {
								out.write(0);
								FileUtilities.writeShort(out, lastCount);
							}
						}

						lastData = data;
						lastLines = lines;
						lastCount = compressCount;
					} else {
						lastLines = lines;
						lastCount += compressCount;

					}
				}
				return true;
			}

			// Used in the reading of the stored files.
			private final String[] getLines() throws IOException {
				// Signs have 4 lines.
				String lines[] = new String[4];
				String line;
				char tempChar;

				// Reads the lines.
				for (int lineNumber = 0; lineNumber < 4; lineNumber++) {
					boolean notDone = true;
					line = "";
					while (notDone) {
						tempChar = (char) in.read();
						if (tempChar != 0) {
							line = line + tempChar;
						} else {
							notDone = false;
						}
					}
					lines[lineNumber] = line;
				}
				return lines;
			}
			
			void renameFiles() {
				File newNameForOld = new File(fileName + "_old");
				inFile.renameTo(newNameForOld);
				
				File newNameForNew = new File(fileName);
				outFile.renameTo(newNameForNew);
				
			}
		}.runTaskAsynchronously(Main.plugin);
	}
}
