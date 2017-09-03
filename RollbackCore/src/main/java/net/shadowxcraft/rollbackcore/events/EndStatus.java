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

package net.shadowxcraft.rollbackcore.events;

public enum EndStatus {

	SUCCESS("succeeded!"),
	FILE_END_EARLY("worked but termonated due to the file ending early."),
	FAIL_IO_ERROR("failed due to a file I/O error."),
	FAIL_NO_SUCH_FILE("failed because that file does not exist!"),
	FAIL_INCOMPATIBLE_VERSION("failed because the version of the file is incompatible!"),
	FAIL_DUPLICATE("failed because it is a duplicate operation."),
	FAIL_EXERNAL_TERMONATION("was termonated early!"),
	FAIL_UNKNOWN_WORLD("failed because the world cannot be found!");

	private final String description;

	EndStatus(String description) {
		this.description = description;
	}

	/**
	 * @return The incomplete description of the enum, for messages.
	 */
	public String getDescription() {
		return description;
	}

}
