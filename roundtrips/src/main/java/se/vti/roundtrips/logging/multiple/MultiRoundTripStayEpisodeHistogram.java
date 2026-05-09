/**
 * se.vti.roundtrips.multiple
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.logging.multiple;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
public class MultiRoundTripStayEpisodeHistogram<L extends Node> implements MHStateProcessor<MultiRoundTrip<L>> {

	private final String fileName;

	private final int logInterval;

	private final int maxStayCnt;

	private long iteration;

	private PrintWriter writer;

	public MultiRoundTripStayEpisodeHistogram(String fileName, int logInterval, int maxStayCnt) {
		this.fileName = fileName;
		this.logInterval = logInterval;
		this.maxStayCnt = maxStayCnt;
	}

	@Override
	public void start() {
		this.iteration = 0;
		try {
			this.writer = new PrintWriter(this.fileName);
			this.writer.print("Iteration");
			for (int f = 0; f <= this.maxStayCnt; f++) {
				this.writer.print("\t");
				this.writer.print("freq(" + f + ")");
			}
			this.writer.println();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void processState(MultiRoundTrip<L> roundTrips) {
		if (this.iteration % this.logInterval == 0) {
			int[] hist = new int[this.maxStayCnt + 1];
			for (RoundTrip<L> roundTrip : roundTrips) {
				hist[roundTrip.size()]++;
			}
			this.writer.print(this.iteration + "\t");
			this.writer.println(Arrays.stream(hist).boxed().map(f -> Integer.toString(f))
					.collect(Collectors.joining("\t")).toString());
			this.writer.flush();
		}
		this.iteration++;
	}

	@Override
	public void end() {
		this.writer.close();
	}

}
