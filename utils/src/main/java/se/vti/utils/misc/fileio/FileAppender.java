/**
 * se.vti.utils
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.utils.misc.fileio;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;

/**
 * @author GunnarF
 */
public class FileAppender {

	private final Path filePath;

	public FileAppender(String fileName) throws IOException {
		this.filePath = Paths.get(fileName);
		Path parent = this.filePath.getParent();
		if (parent != null && Files.notExists(parent)) {
			Files.createDirectories(parent);
		}
		Files.deleteIfExists(this.filePath);
		Files.createFile(this.filePath);
	}

	public void appendLine(String text) {
		try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.APPEND)) {
			writer.write(text);
			writer.newLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void append(String text) {
		try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.APPEND)) {
			writer.write(text);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}