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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4Factory;
import net.shadowxcraft.rollbackcore.events.EndStatus;

public class LegacyUpdater {
	private static Mapping postLegacyMapping;
	private static String[][] legacyIdMappings;
	private static boolean done = false;
	// First key: to version
	// Second key: from version
	// Third key: from blockdata as string
	// Thirs key's value: to blockdata as string
	private static TreeMap<String, TreeMap<String, Map<Pattern, String>>> blockDataMappings;

	public static TreeMap<String, TreeMap<String, Map<Pattern, String>>> getMappings() {
		return blockDataMappings;
	}
	
	public static void loadMappings(final Plugin plugin) {
		done = false;
		new BukkitRunnable() {
			public void run() {
				loadAllModernBlockDataMappings(plugin);
				// run modern before legacy since legacy will map to the current MC version
				// which may be newer than 1.13. It uses the modern mappings to
				loadLegacyMappings(plugin);
				done = true;
			}
		}.runTaskAsynchronously(plugin);
	}

	public static void waitUntilLoaded(Paste pasteToStart) {
		if (!done) {
			pasteToStart.sender.sendMessage(pasteToStart.prefix + "Waiting for the version update data to load.");
			Main.plugin.getLogger().info("Waiting for the version update data to load.");
			while (!done)
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

			Main.plugin.getLogger().info("Done waiting for version data to load.");
		}
	}

	/**
	 * Gets all maps to go from one version to the other.
	 */
	public static LinkedList<Map<Pattern, String>> getMapping(String from, String to) {
		// Search through all paths.
		// Get iterator from the lowest version. Move onto next if from
		// version is less than the the required from version.
		TreeMap<String, Map<Pattern, String>> toCurrentTo = blockDataMappings.get(to);
		if (toCurrentTo != null) {
			for (Map.Entry<String, Map<Pattern, String>> mapping : toCurrentTo.entrySet()) {
				String fromVersion = mapping.getKey();

				int comparison = from.compareTo(fromVersion);
				if (comparison == 0) {
					// found it!
					LinkedList<Map<Pattern, String>> newList = new LinkedList<Map<Pattern, String>>();
					if (!mapping.getValue().isEmpty())
						newList.add(mapping.getValue());
					return newList;
				} else if (comparison < 0) {
					// This means that the entry's from version is greater than
					// the from that is being looked for.
					// In this case, check to see if it is greater than the two version.
					// If it is, ignore it.
					if (to.compareTo(fromVersion) > 0) { // not going backwards, which would likely cause infinite
															// recursion
						// Good. Done with that section, but not done yet.

						LinkedList<Map<Pattern, String>> list = getMapping(from, fromVersion);
						if (list != null) {
							if (!mapping.getValue().isEmpty())
								list.add(mapping.getValue());
							return list;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Loads the legacy mappings, allowing saves from 1.12 or older to be saved in
	 * the new format that uses blockdata.
	 * 
	 * @param plugin The RollbackCore plugin for resource loading.
	 */
	private static void loadLegacyMappings(final Plugin plugin) {
		// go from 1.13 to the newest version that can be supported.
		postLegacyMapping = loadLatestConversion("1.13");

		// load the mapping of 1.12 -> 1.13
		InputStream in = plugin.getResource("legacy_mappings.yml");
		Reader reader = new InputStreamReader(in);

		YamlConfiguration config = new YamlConfiguration();

		// represent the conversion with an array with all possible positions.
		legacyIdMappings = new String[256][16];
		try {
			// last step in loading
			config.load(reader);
			// breaks down the config by id
			for (String idKey : config.getKeys(false)) {
				ConfigurationSection section = config.getConfigurationSection(idKey);
				Set<String> dataKeys = section.getKeys(false);

				// gets the group of 16
				String[] data = legacyIdMappings[Integer.parseInt(idKey)];

				try {
					for (String dataKey : dataKeys) {

						String dataAsString = section.getString(dataKey);
						// update if applicable
						if (postLegacyMapping != null)
							dataAsString = postLegacyMapping.update(dataAsString);
						data[Integer.parseInt(dataKey)] = dataAsString;
					}

					// reassigns it
					legacyIdMappings[Integer.parseInt(idKey)] = data;
				} catch (ArrayIndexOutOfBoundsException e) {
					plugin.getLogger().warning("Index issue loading indexes " + dataKeys + " of ID " + idKey);
				}
			}
			plugin.getLogger().info("Loaded legacy mappings.");
		} catch (IOException | InvalidConfigurationException e) {
			plugin.getLogger().warning("Unable to load mappings! Corrupt jar?");
			e.printStackTrace();
		}
	}

	/**
	 * Checks all to versions and returns the latest converstion that goes from
	 * 'from' to the newest version that is older or equal to the current version.
	 * 
	 * @param from The version that the converstion will map from.
	 * @return A mapping object if one is found, else null.
	 */
	public static Mapping loadLatestConversion(String from) {
		// Null since there is no converstion from and to the same version.
		if (RollbackOperation.CURRENT_MC_VERSION == from)
			return null;

		for (String toVersion : blockDataMappings.descendingKeySet()) {
			// Next, prevent it from updating the files to a version newer than
			// the current server.
			if (toVersion.compareTo(RollbackOperation.CURRENT_MC_VERSION) <= 0) {
				LinkedList<Map<Pattern, String>> mapping = getMapping(from, toVersion);
				if (mapping != null) {
					return new Mapping(from, toVersion, mapping);
				}
			}
		}
		// None found
		return null;
	}

	/**
	 * An object that will update block IDs from one from to another.
	 */
	static class Mapping {
		private final String from, to;
		private final LinkedList<Map<Pattern, String>> mappings;

		/**
		 * Creates a mapping that goes from the "from" version to the "to" version,
		 * using the list of maps to do the actual updating of the strings.
		 * 
		 * @param from     The from version.
		 * @param to       The to version.
		 * @param mappings A map, in order of oldest to newest version, that has a
		 *                 pattern and the replacement that is used to the materials.
		 */
		public Mapping(String from, String to, LinkedList<Map<Pattern, String>> mappings) {
			this.from = from;
			this.to = to;
			this.mappings = mappings;
		}

		public String getFrom() {
			return from;
		}

		public String getTo() {
			return to;
		}

		/**
		 * Processes the string with all conversions to update it to the to version.
		 * Note: Only pass in strings from the getFrom() version. Anything else could
		 * cause unintended updates that break, as opposed to fix, the data strings.
		 * 
		 * @param original The original blockdata string from the MC version getFrom()
		 * @return The processed blockdata
		 */
		public String update(String original) {
			String output = original;
			// goes through all replacements in the mappings
			for (Map<Pattern, String> replacements : mappings) {
				for (Map.Entry<Pattern, String> replacement : replacements.entrySet()) {
					// uses the replaceFirst method to replace.
					output = replacement.getKey().matcher(output).replaceFirst(replacement.getValue());
				}
			}
			return output;
		}
	}

	/**
	 * Loads both the provided mappings, as well as the custom ones. Load this after
	 * updating the config to ensure that the config is valid and that this does not
	 * happen while the config is being saved.
	 * 
	 * @param plugin The plugin instance for RollbackCore
	 */
	private static void loadAllModernBlockDataMappings(final Plugin plugin) {
		// loads the one in the jar
		InputStream in = plugin.getResource("new_mappings.yml");
		Reader reader = new InputStreamReader(in);

		YamlConfiguration config = new YamlConfiguration();
		try {
			config.load(reader);
			loadModernBlockDataMappings(plugin, config);
			plugin.getLogger().info("Loaded default blockdata mappings.");
		} catch (IOException | InvalidConfigurationException e) {
			plugin.getLogger().warning("Unable to load default mappings! Corrupt jar?");
			e.printStackTrace();
		}

		// loads the user mappings in the config
		File file = new File(plugin.getDataFolder() + "/config.yml");
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		if (yaml.isConfigurationSection("custom-mappings")) {
			loadModernBlockDataMappings(plugin, yaml.getConfigurationSection("custom-mappings"));
		}
	}

	/**
	 * Loads the mappings to the maps. If the
	 * 
	 * @param configurationSection
	 */
	private static void loadModernBlockDataMappings(final Plugin plugin, ConfigurationSection configurationSection) {
		// creates the map if it is null
		if (blockDataMappings == null) {
			blockDataMappings = new TreeMap<String, TreeMap<String, Map<Pattern, String>>>();
		}

		// goes through the first level of keys, which are the two versions.
		for (String toVersion : configurationSection.getKeys(false)) {
			// replaces the dashes with dots due to dots being unable to be used
			String toVersionFormatted = toVersion.replace('-', '.');
			ConfigurationSection fromVersions = configurationSection.getConfigurationSection(toVersion);
			TreeMap<String, Map<Pattern, String>> toVersionMappings = blockDataMappings.get(toVersionFormatted);
			if (toVersionMappings == null) {
				toVersionMappings = new TreeMap<String, Map<Pattern, String>>();
				blockDataMappings.put(toVersionFormatted, toVersionMappings);
			}

			// now that it is within the 'to' version's config section, it goes
			// through the 'from' keys
			for (String fromVersion : fromVersions.getKeys(false)) {
				// replaces the dashes with dots due to dots being unable to be used
				String fromVersionFormatted = fromVersion.replace('-', '.');

				// gets the existing map, and creates one if it does not exist
				Map<Pattern, String> fromVersionMappings = toVersionMappings.get(fromVersionFormatted);
				if (fromVersionMappings == null) {
					fromVersionMappings = new HashMap<Pattern, String>();
					toVersionMappings.put(fromVersionFormatted, fromVersionMappings);
				}

				// gets the strings from the config
				List<Map<?, ?>> mappings = fromVersions.getMapList(fromVersion);
				for (Map<?, ?> mapping : mappings) {
					try {
						String fromExpression = (String) mapping.get("from");
						String toExpression = (String) mapping.get("to");

						// compiles the pattern to improve the efficiency.
						Pattern fromPattern = Pattern.compile(fromExpression);

						fromVersionMappings.put(fromPattern, toExpression);
					} catch (java.lang.ClassCastException e) {
						plugin.getLogger().warning("Invalid data type for mappings in from version " + fromVersion
								+ ", to version " + toVersion);
					}
				}
			}
		}
	}

	/**
	 * Updates the blockdata that changes between versions.
	 * 
	 * @param fileName     The filename of the
	 * @param pasteToStart
	 */
	public static void updateModernBlockData(final String fileName, final Paste pasteToStart) {
		pasteToStart.sender.sendMessage(Main.prefix + "Attempting to update paste \"" + fileName + "\".");
		new BukkitRunnable() {
			File inFile, outFile;
			InputStream in;
			OutputStream out;
			int diffX, diffY, diffZ;
			int totCount;
			int index = 0;
			String fromMCVersion = null;
			Mapping versionMapping;
			private HashMap<Integer, BlockCache<String>> dataCache = new HashMap<Integer, BlockCache<String>>();

			public void run() {
				waitUntilLoaded(pasteToStart);
				boolean success = false;
				try {
					if (initFileRead()) {
						if (setupVersionMapping()) {
							initFileWrite();
							convertMainData();
							pasteToStart.sender.sendMessage(Main.prefix + "Successfully updated \"" + fileName
									+ "\" to " + versionMapping.getTo());
							success = true;
						}
					}
					// They must always be closed
					if (in != null)
						in.close();
					if (out != null)
						out.close();
					
					if(success) {
						if (renameFiles(fileName, inFile, outFile)) {
							if (pasteToStart != null) {
								Main.plugin.getServer().getScheduler().runTaskLater(Main.plugin, pasteToStart, 1);
								if (pasteToStart.sender != null)
									pasteToStart.sender
											.sendMessage(pasteToStart.prefix + "Updated sucessfully! Starting paste.");
	
							}
						} else {
							Main.plugin.getLogger().warning("Unable to rename the files after conversion. Aborting.");
							if (pasteToStart != null) {
								pasteToStart.end(EndStatus.FAIL_IO_ERROR);
								if (pasteToStart.sender != null)
									pasteToStart.sender.sendMessage(
											pasteToStart.prefix + "Unable to rename the files after conversion. Aborting.");
							}
						}
					} else {
						// Have the paste run with the outdated data.
						pasteToStart.reportIncompleteUpdate();
						Main.plugin.getLogger().warning("Unable to update the region. Starting the paste.");
						if (pasteToStart != null) {
							Main.plugin.getServer().getScheduler().runTaskLater(Main.plugin, pasteToStart, 1);
						}
					}
				} catch (FileNotFoundException e) {
					if (pasteToStart != null)
						pasteToStart.end(EndStatus.FAIL_NO_SUCH_FILE);
					e.printStackTrace();
					Main.plugin.getLogger().warning("Failed when updating the copied region " + fileName);
				} catch (IOException e) {
					if (pasteToStart != null)
						pasteToStart.end(EndStatus.FAIL_IO_ERROR);
					e.printStackTrace();
					Main.plugin.getLogger().warning("Failed when updating the copied region " + fileName);
				}
			}

			boolean initFileRead() throws IOException {
				try {
					// Initializes the InputStream
					inFile = new File(fileName);
					in = new FileInputStream(inFile);
					// In case the file they are trying to read is in of date or too
					// new.
					int copyVersion = in.read();
					if (copyVersion != 2) {
						pasteToStart.end(EndStatus.FAIL_INCOMPATIBLE_VERSION);
						pasteToStart.sender
								.sendMessage(pasteToStart.prefix + "The blockdata updater cannot update this version.");
						return false;
					}

					CompressionType compression = null;

					// Reads the sizes using readShort because it can be larger than 255
					diffX = FileUtilities.readShort(in);
					diffY = FileUtilities.readShort(in);
					diffZ = FileUtilities.readShort(in);

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
								pasteToStart.end(EndStatus.FAIL_UNKNOWN_COMPRESSION);
							}
							break;
						case "minecraft_version":
							fromMCVersion = FileUtilities.readString(in, valueLength, bytes);

							break;
						default:
							in.skip(valueLength); // Don't assume it's a string.
							Main.plugin.getLogger().warning("File being read has an unknown key \"" + key
									+ "\". Was it written with another program?");
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
						pasteToStart.end(EndStatus.FAIL_UNKNOWN_COMPRESSION);
						return false;
					}

				} catch (IOException e1) {
					e1.printStackTrace();
					pasteToStart.end(EndStatus.FAIL_IO_ERROR);
					return false;
				}
				return true;
			}

			boolean setupVersionMapping() {
				if (fromMCVersion == null) {
					return false;
				}

				versionMapping = loadLatestConversion(fromMCVersion);
				if (versionMapping == null) {
					return false;
				}
				return true;
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

				out = Copy.startFile(out, diffX, diffY, diffZ, versionMapping.getTo());
			}

			boolean convertMainData() throws IOException {
				int id = -1;
				BlockCache<String> data = null;
				final int BUFF_LEN = 2000;
				final byte[] bytes = new byte[BUFF_LEN];
				totCount = (diffX + 1) * (diffY + 1) * (diffZ + 1);

				try {
					while (index < totCount) {
						// read and write id
						id = in.read();
						if (id == -1) {
							pasteToStart.end(EndStatus.FILE_END_EARLY);
							return false;
						}
						out.write(id);
						if (id == 0) {
							// New block

							// Reads and writes value stating whether or not there is extra data.
							int hasExtraDataRaw = in.read();
							out.write(hasExtraDataRaw);
							boolean hasExtraData = hasExtraDataRaw == 1;
							// Reads and writing the string of the blockdata.
							String blockDataString = FileUtilities.readShortString(in, bytes);
							String replacementBlockData = versionMapping.update(blockDataString);
							FileUtilities.writeShortString(out, replacementBlockData);
							// Reads and writes the new ID
							id = in.read();
							out.write(id);

							// saves the id in the cache
							data = new BlockCache<String>(id, hasExtraData, blockDataString);
							dataCache.put(id, data);

						} else {
							// Interprets the ID to properly address the block as having extra data or not
							data = dataCache.get(id);
						}
						// processes blocks in a row so it knows when it will be done
						int blocksInARow;
						if (!data.hasExtraData) {
							blocksInARow = in.read();
							out.write(blocksInARow);

							// too many to fit in 8 bits, so reads the next 16
							if (blocksInARow == 0) {
								blocksInARow = FileUtilities.readShort(in);
								FileUtilities.writeShort(out, blocksInARow);
							}
						} else {
							blocksInARow = 1;

							// copy over the data. No interpretation needed.
							int length = FileUtilities.readShort(in);
							byte[] dataBuffer;
							if (length <= BUFF_LEN) {
								dataBuffer = bytes; // reuse what can be reused
							} else {
								dataBuffer = new byte[length];
							}
							in.read(dataBuffer, 0, length);
							// write the exact length and data
							FileUtilities.writeShort(out, length);
							out.write(dataBuffer, 0, length);
						}
						index += blocksInARow;
					}
				} catch (IOException e) {
					pasteToStart.end(EndStatus.FAIL_IO_ERROR);
					e.printStackTrace();
				}
				return true;
			}
		}.runTaskAsynchronously(Main.plugin);
	}

	/**
	 * The list of blocks that the plugin skipped saving the data for in file format
	 * 1 for backwards compatibility. It is updated to Minecraft 1.9 with some
	 * missing blocks. Must be sorted.
	 */
	final static int[] version1Blocks = { 0, 2, 4, 7, 13, 14, 15, 16, 20, 21, 22, 23, 30, 37, 39, 40, 41, 42, 45, 46,
			47, 48, 49, 51, 52, 56, 57, 58, 70, 72, 73, 74, 79, 80, 81, 82, 83, 84, 87, 88, 89, 101, 102, 103, 112, 113,
			116, 117, 118, 121, 122, 123, 124, 129, 133, 137, 138, 147, 148, 152, 153, 165, 166, 169, 172, 173, 174,
			188, 189, 190, 191, 192, 201, 202, 206 };

	/**
	 * List of blocks that the plugin will skip in the newest version of the plugin.
	 * Currently updated to Minecaft 1.10 Must be sorted.
	 * 
	 * @since 2.0
	 */
	final static int[] latestSimpleBlocks = { 0, 2, 4, 7, 13, 14, 15, 16, 20, 21, 22, 23, 30, 37, 39, 40, 41, 42, 45,
			47, 48, 49, 51, 52, 56, 57, 58, 70, 72, 73, 74, 79, 80, 81, 82, 83, 84, 87, 88, 89, 101, 102, 103, 112, 113,
			116, 117, 118, 121, 122, 123, 124, 129, 133, 137, 138, 147, 148, 152, 153, 165, 166, 169, 172, 173, 174,
			188, 189, 190, 191, 192, 201, 202, 206, 208, 209, 213, 214, 215 };

	static final int[] commandBlockIDs = { 137, 210, 211 };

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
			LRUCache<String> cache;
			int[] simpleBlocks;
			int version, diffX, diffY, diffZ;

			public void run() {
				waitUntilLoaded(pasteToStart);
				try {
					initFileRead();
					initFileWrite();
					convertMainData();
					in.close();
					out.close();
					if (renameFiles(fileName, inFile, outFile)) {
						if (pasteToStart != null) {
							Main.plugin.getServer().getScheduler().runTaskLater(Main.plugin, pasteToStart, 1);
							if (pasteToStart.sender != null)
								pasteToStart.sender
										.sendMessage(pasteToStart.prefix + "Converted sucessfully! Starting paste.");

						}
					} else {
						Main.plugin.getLogger().warning("Unable to rename the files after conversion. Aborting.");
						if (pasteToStart != null) {
							pasteToStart.end(EndStatus.FAIL_IO_ERROR);
							if (pasteToStart.sender != null)
								pasteToStart.sender.sendMessage(
										pasteToStart.prefix + "Unable to rename the files after conversion. Aborting.");
						}
					}
				} catch (FileNotFoundException e) {
					if (pasteToStart != null)
						pasteToStart.end(EndStatus.FAIL_NO_SUCH_FILE);
					e.printStackTrace();
					Main.plugin.getLogger().warning("Failed when converting the copied region " + fileName);
				} catch (IOException e) {
					if (pasteToStart != null)
						pasteToStart.end(EndStatus.FAIL_IO_ERROR);
					e.printStackTrace();
					Main.plugin.getLogger().warning("Failed when converting the copied region " + fileName);
				}
			}

			void initFileRead() throws IOException {
				inFile = new File(fileName);
				in = new BufferedInputStream(new FileInputStream(inFile));
				cache = new LRUCache<String>(1, 255); // Stores IDs for the
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
				diffX = legacyReadShort(in);
				diffY = legacyReadShort(in);
				diffZ = legacyReadShort(in);
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

				out = Copy.startFile(out, diffX, diffY, diffZ,
						postLegacyMapping == null ? "1.13" : postLegacyMapping.getTo());
			}

			boolean convertMainData() throws IOException {
				final int maxPosition = (1 + diffX) * (1 + diffY) * (1 + diffZ);
				String lastData = null;
				int lastCount = 0;
				String[] lastLines = null;
				int[] flowerData = new int[diffZ + 1];

				for (int position = 0; position < maxPosition;) {
					String data = null;
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
							if (lastData == null) {
								// reached end.
								return true;
							}
							data = null;
							compressCount = -1;
						} else {

							// For compression, it reads how many
							// times this block is repeated.
							compressCount = in.read();

							// Hack, because top half data isn't detailed in pre-1.13
							if (currentId == 175) {
								for (int diff = 0; diff < compressCount; diff++) {
									if (currentData < 8) {
										flowerData[(position + lastCount + diff) % (diffZ + 1)] = currentData;
									} else {
										currentData = flowerData[(position + lastCount + diff) % (diffZ + 1)] + 8;
									}
								}
							}

							if (currentData < 16 && currentId < 256)
								data = legacyIdMappings[currentId][currentData];
							else
								data = null;
							if (data == null) {
								System.out.println("Unknown: " + currentId + " " + currentData);
								data = legacyIdMappings[0][0]; // air
							}

							// The following code is to read sign
							// text.
							if (compressCount == 0) {
								lines = getLines();
							} else {
								lines = null;
							}
						}
					}

					if (lastData == null) {
						lastData = data;
						lastLines = lines;
						lastCount += compressCount;
					} else if (data == null || !data.equals(lastData) || lastCount > 65280 || lastCount == 0) {
						position += lastCount == 0 ? 1 : lastCount;

						// Write the old IDs to file.
						LRUCache<String>.Node id = cache.get(lastData);
						if (id == null) {
							// Material material = lastData.getMaterial();
							id = cache.add(lastData, lastCount == 0);
							// New block.

							// It was absent, so writes it to the file,
							// then increments the index.

							// START WITH 0 - Notes new data.
							out.write(0);
							// Next write whether or not there was extra data.
							out.write(id.hasExtraData ? 1 : 0);
							// Write the string
							FileUtilities.writeShortString(out, lastData);
							// Write the id value of the data.
							out.write(id.value);
						} else {
							out.write(id.value); // Now writes the index.
						}
						if (id.hasExtraData) {
							if (id.data.contains("sign")) {
								try {
									// Just signs and command blocks.
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
							} else if (id.data.contains("command")) {
								String command, name;

								if (lastLines == null) {
									name = "";
									command = "";
								} else {
									try {
										name = lastLines[0];
										command = lastLines[1];
									} catch (IndexOutOfBoundsException e) {
										e.printStackTrace();
										name = "";
										command = "";
									}
								}
								byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
								byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);

								FileUtilities.writeShort(out, nameBytes.length + commandBytes.length + 4);
								FileUtilities.writeShort(out, nameBytes.length);
								out.write(nameBytes);
								FileUtilities.writeShort(out, commandBytes.length);
								out.write(commandBytes);
							} else {
								Main.plugin.getLogger().warning("Unknown data state " + id.data
										+ " found during conversion from old to 1.13+.");
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
		}.runTaskAsynchronously(Main.plugin);
	}

	/**
	 * Renames the files.
	 * 
	 * @return True if both succeeded. False if either failed.
	 */
	static boolean renameFiles(String fileName, File inFile, File outFile) {
		File newNameForOld = new File(fileName + "_old");
		if (!inFile.renameTo(newNameForOld))
			return false;

		File newNameForNew = new File(fileName);
		if (!outFile.renameTo(newNameForNew))
			return false;
		return true; // success

	}

	static final int legacyReadShort(InputStream in) throws IOException {
		int temp = 0;

		temp += in.read() * 255;
		temp += in.read();

		return temp;
	}
}
