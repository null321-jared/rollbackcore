package net.shadowxcraft.rollbackcore;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.shadowxcraft.rollbackcore.events.EndStatus;
import net.shadowxcraft.rollbackcore.events.WorldRollbackEndEvent;

public class WholeWorldRollback {

	/**
	 * Backs up a world for future rolling back. It makes a copy of the world in the
	 * default RollbackCore world folder.
	 * 
	 * @param worldToRollback The world that will be backed up.
	 * @return The file of the world rolled back, or null if it failed.
	 */
	public static File backupWorld(World worldToRollback) {
		return backupWorld(worldToRollback, null);
	}

	/**
	 * Backs up a world for future rolling back. It makes a copy of the world in a
	 * sub-folder of the specified folder.
	 * 
	 * @param worldToRollback  The world that will be backed up.
	 * @param parentDirToPutIt The location where it will put the folder of the
	 *                         world backup. Or null for the default location in the
	 *                         RollbackCore folder.
	 * @return The file of the world rolled back, or null if it failed.
	 */
	public static File backupWorld(World worldToRollback, String parentDirToPutIt) {
		File parentDirToPutItFile = parentDirToPutIt == null ? Main.worldsPath.toFile() : new File(parentDirToPutIt);
		if (!parentDirToPutItFile.exists()) {
			parentDirToPutItFile.mkdirs();
		}

		File worldFile = worldToRollback.getWorldFolder();

		File dirToPutIt = new File(parentDirToPutItFile + File.separator + worldToRollback.getName());
		try {
			if(dirToPutIt.exists())
				delete(dirToPutIt);
			copy(worldFile, dirToPutIt);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return dirToPutIt;
	}

	/**
	 * Starts a world rollback.
	 * 
	 * If it fails synchronously before the method returns, it will throw an
	 * exception instead of using the event handler.
	 * 
	 * There is no guarantee on the order at which the event will return compared to
	 * the
	 * 
	 * @param worldToRollback
	 * @param locToSendPlayers
	 * @param sender
	 * @param prefix
	 * @param folderWithBackup
	 * 
	 * @throws IllegalArgumentException if the send location is in the world being
	 *                                  rolled back
	 * @throws IllegalArgumentException if the world backup folder does not exist or
	 *                                  is not a folder
	 * @throws IllegalStateException    if after teleporting all players, there are
	 *                                  still players in the world.
	 */
	public static void startWholeWorldRollback(World worldToRollback, Location locToSendPlayers, CommandSender sender,
			String prefix, String folderWithBackup) {
		long startTime = System.nanoTime();
		if (locToSendPlayers.getWorld() == worldToRollback) {
			if (sender != null)
				sender.sendMessage(prefix == null ? ""
						: prefix + "Send location's world must be different than the world beign rolled back!");
			throw new IllegalArgumentException(
					"Send location's world must be different than the world beign rolled back!");
		}
		for (Player player : worldToRollback.getPlayers()) {
			player.teleport(locToSendPlayers);
		}
		if (!worldToRollback.getPlayers().isEmpty()) {
			if (sender != null)
				sender.sendMessage(
						prefix == null ? "" : prefix + "Players are still in the world that needs to be rolled back");
			throw new IllegalStateException("Players are still in the world that needs to be rolled back");
		}

		final String folderWithBackupProcessed;
		if (folderWithBackup == null)
			folderWithBackupProcessed = Main.worldsPath.toString();
		else
			folderWithBackupProcessed = folderWithBackup;

		File filesToCopy = new File(folderWithBackupProcessed + File.separator + worldToRollback.getName());
		if (!filesToCopy.exists() || !filesToCopy.isDirectory()) {
			if (sender != null)
				sender.sendMessage(
						prefix == null ? "" : prefix + "World backup folder does not exist or is not a folder");
			throw new IllegalArgumentException("World backup folder does not exist or is not a folder");
		}

		WorldCreator worldCreator = new WorldCreator(worldToRollback.getName());

		Bukkit.unloadWorld(worldToRollback.getName(), false);
		new BukkitRunnable() {
			public void run() {
				File worldFolder = worldToRollback.getWorldFolder();
				try {
					delete(worldFolder);
					copy(filesToCopy, worldFolder);

					new BukkitRunnable() {
						public void run() {
							World world = worldCreator.createWorld();
							new WorldRollbackEndEvent(world, folderWithBackupProcessed, System.nanoTime() - startTime,
									EndStatus.SUCCESS, sender, prefix);
						}
					}.runTaskLater(Main.plugin, 10);
				} catch (IOException e) {
					e.printStackTrace();
					new WorldRollbackEndEvent(worldToRollback, folderWithBackupProcessed, System.nanoTime() - startTime,
							EndStatus.FAIL_IO_ERROR, sender, prefix);
				}
			}
		}.runTaskAsynchronously(Main.plugin);
	}

	// ------------------------------------------------ //
	// File utilities
	// ------------------------------------------------ //
	private static void delete(File file) throws IOException {
		if (file.isDirectory()) {
			// directory is empty, then delete it
			if (file.list().length == 0) {

				file.delete();
			} else {

				// list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(file, temp);

					// recursive delete
					delete(fileDelete);
				}

				// check the directory again, if empty then delete it
				if (file.list().length == 0) {
					file.delete();
				}
			}

		} else {
			// It's a file, so it deletes it.
			file.delete();
		}
	}

	private static void copy(File sourcePath, File targetPath) throws IOException {
		Files.walkFileTree(sourcePath.toPath(), new CopyFileVisitor(targetPath.toPath()));
	}

	public static class CopyFileVisitor extends SimpleFileVisitor<Path> {
		private final Path targetPath;
		private Path sourcePath = null;

		public CopyFileVisitor(Path targetPath) {
			this.targetPath = targetPath;
		}

		@Override
		public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
			if (sourcePath == null) {
				sourcePath = dir;
			} else {
				Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
			Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Copying " + file.toString());
			return FileVisitResult.CONTINUE;
		}
	}
}
