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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FileUtilities {

	// Used to read a value to the file using the current storage way.
	// Big Endian
	public static int readShort(InputStream in) throws IOException {
		int temp = 0;

		temp += in.read() * 256;
		temp += in.read();

		return temp;
	}

	// Big Endian
	public static int readInt(InputStream in) throws IOException {
		int output = 0;
		for (int i = 0; i < 4; i++)
			output = ((in.read() & 0xFF) << i * 8) | output;
		return output;
	}

	public static void writeInt(OutputStream out, int input) throws IOException {
		for (int i = 0; i < 4; i++) {
			out.write((input >> i * 8) & 0xFF);
		}
	}

	// Used to write a value to the file using the current storage way.
	// Stores an unsigned short.
	public static void writeShort(OutputStream out, int input)
			throws IllegalArgumentException, IOException {
		if (input > 65535) {
			throw new IllegalArgumentException(
					"Input " + input + " is out of the legal range (0 to 65,535)");
		}

		out.write(input / 256);
		out.write(input % 256);
	}

	/**
	 * Writes the string to the stream.
	 * 
	 * @param out    The stream that it is writing to.
	 * @param string The string it is writing. Max length of 255 chars.
	 * @throws IllegalArgumentException if length of string is over 255
	 * @throws IOException              if something goes wrong with the
	 *                                  outputstream.
	 */
	public static void writeShortString(OutputStream out, String string) throws IOException {
		if (string.length() > 255)
			throw new IllegalArgumentException("Length of string > 255");

		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		out.write(bytes.length);
		out.write(bytes);
	}

	public static String readString(InputStream in, int length) throws IOException {
		return readString(in, length, new byte[length]);
	}

	public static String readShortString(InputStream in) throws IOException {
		int length = in.read();
		return readShortString(in, new byte[length]);
	}

	public static String readShortString(InputStream in, byte[] buffer) throws IOException {
		int length = in.read();
		return readString(in, length, buffer);
	}

	public static String readString(InputStream in, int length, byte[] buffer) throws IOException {
		in.read(buffer, 0, length);
		return new String(buffer, 0, length, StandardCharsets.UTF_8);
	}

	// public static void readIDAndData(InputStream in) throws IOException {
	// int storedValue = readShort(in);
	// System.out.println("ID: " + (storedValue >> 4));
	// System.out.println("DATA: " + (storedValue & 15));
	// System.out.println("\n");
	// }

	// DEBUG USE ONLY. WILL SPAM CONSOLE!
	public static void displayEntireFile(File file) {
		BufferedInputStream stream;
		try {
			stream = new BufferedInputStream(new FileInputStream(file));
			String total = "";
			while (stream.available() > 0) {
				total += stream.read() + " ";
			}
			Main.plugin.getLogger().info(total);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
