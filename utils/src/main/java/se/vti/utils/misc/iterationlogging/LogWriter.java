/**
 * se.vti.utils
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.utils.misc.iterationlogging;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author GunnarF
 * 
 * @param S the data source for logging
 */
public class LogWriter<S extends Object> {

	// -------------------- CONSTANTS --------------------

	private final String fileName;

	private final List<LogEntry<S>> entries = new ArrayList<LogEntry<S>>();

	private boolean wroteHeader = false;

	// -------------------- CONSTRUCTION AND INITIALIZATION --------------------

	public LogWriter(final String fileName, final boolean append) {
		this.fileName = fileName;
		this.wroteHeader = append;
	}

	// -------------------- SETTERS & GETTERS --------------------

	public void addEntry(final LogEntry<S> statistic) {
		this.entries.add(statistic);
	}

	public String getFileName() {
		return this.fileName;
	}

	// -------------------- FILE WRITING --------------------

	public void writeToFile(final S data, final String... labelOverrideValueSequence) {
		final Map<String, String> label2overrideValue = new LinkedHashMap<>();
		if (labelOverrideValueSequence != null) {
			for (int i = 0; i < labelOverrideValueSequence.length; i += 2) {
				label2overrideValue.put(labelOverrideValueSequence[i], labelOverrideValueSequence[i + 1]);
			}
		}
		this.writeToFile(data, label2overrideValue);
	}

	public void writeToFile(final S data, final Map<String, String> label2overrideValue) {
		try {
			final BufferedWriter writer;
			if (!this.wroteHeader) {
				writer = new BufferedWriter(new FileWriter(this.fileName, false));
				for (LogEntry<S> stat : this.entries) {
					writer.write(stat.label() + "\t");
				}
				writer.newLine();
				this.wroteHeader = true;
			} else {
				writer = new BufferedWriter(new FileWriter(this.fileName, true));
			}
			for (LogEntry<S> stat : this.entries) {
				if (label2overrideValue.containsKey(stat.label())) {
					writer.write(label2overrideValue.get(stat.label()));
				} else {
					String value;
					try {
						value = stat.value(data);
					} catch (Exception e) {
						value = "";
					}
					writer.write(value);
				}
				writer.write("\t");
			}
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
