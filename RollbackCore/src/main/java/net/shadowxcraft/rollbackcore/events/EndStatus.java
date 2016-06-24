/**
* Copyright (C) 2016 ShadowXCraft Server - All rights reserved.
*/

package net.shadowxcraft.rollbackcore.events;

public enum EndStatus {

	SUCCESS("succeeded!"),
	FILE_END_EARLY("worked but termonated due to the file ending early."),
	FAIL_IO_ERROR("failed due to a file I/O error."),
	FILE_NO_SUCH_FILE("failed because that file does not exist!"),
	FAIL_INCOMPATIBLE_VERSION("failed because the version of the file is incompatible!"),
	FAIL_DUPLICATE("failed because it is a duplicate operation."),
	FAIL_EXERNAL_TERMONATION("was termonated early!"),
	FAIL_UNKNOWN_WORLD("failed because the world cannot be found!");

	private final String description;

	EndStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}
