/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
*/

package net.shadowxcraft.rollbackcore;

public class TaskManager {
	private TaskManager() {
	}

	private static int numberOfTasks = 0;

	/**
	 * Calculates the maximum target time that each running operation should take per tick based on
	 * the config value and the number of runnning operations.
	 * 
	 * @return the number of milliseconds each operation should take.
	 */
	public static double getMaxTime() {
		double maxTime;
		if (numberOfTasks == 0 || numberOfTasks == 1)
			maxTime = Config.targetTime;
		else
			maxTime = Config.targetTime / (numberOfTasks * 1.2);
		return maxTime;
	}

	public static void addTask() {
		numberOfTasks++;
	}

	public static void removeTask() {
		numberOfTasks--;
	}

	public static int cancelAllTasks() {
		int totalCanceledTasks = 0;
		totalCanceledTasks += WatchDogRegion.cancelAll();
		totalCanceledTasks += Copy.cancelAll();
		totalCanceledTasks += Paste.cancelAll();
		return totalCanceledTasks;
	}
}
