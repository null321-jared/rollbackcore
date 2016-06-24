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
