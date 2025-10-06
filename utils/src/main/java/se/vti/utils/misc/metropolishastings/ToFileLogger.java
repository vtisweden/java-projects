/**
 * se.vti.roundtrips
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
package se.vti.utils.misc.metropolishastings;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 
 * @author GunnarF
 *
 */
public abstract class ToFileLogger<R> extends MHAbstractStateProcessor<R> implements MHStateProcessor<R> {

	private final File logFile;

	PrintWriter writer;

	private boolean flushEachLine = true;

	public ToFileLogger(long samplingInterval, String logFileName) {
		super(0, samplingInterval);
		this.logFile = new File(logFileName);
	}
	
	public void setFlushEachLine(boolean flushEachLine) {
		this.flushEachLine = flushEachLine;
	}

	@Override
	public final void start() {
		super.start();
		File directory = this.logFile.getParentFile();
		if (directory != null && !directory.exists()) {
			directory.mkdirs();
		}
		if (this.logFile.exists()) {
			this.logFile.delete();
		}
		try {
			this.logFile.createNewFile();
			this.writer = new PrintWriter(this.logFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.writer.println(this.createHeaderLine());
	}

	@Override
	public final void end() {
		this.writer.flush();
		this.writer.close();
	}

	@Override
	public final void processStateHook(R state) {
		this.writer.println(createDataLine(state));
		if (this.flushEachLine) {
			this.writer.flush();
		}
	}

	public abstract String createHeaderLine();

	public abstract String createDataLine(R state);
}
