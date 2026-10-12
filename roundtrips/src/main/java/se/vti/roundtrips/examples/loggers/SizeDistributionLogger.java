/**
 * se.vti.roundtrips.examples.TruckServiceCoverage
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
package se.vti.roundtrips.examples.loggers;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.scenario.Node;
import se.vti.utils.misc.metropolishastings.MHToFileLogger;

/**
 * 
 * @author GunnarF
 *
 */
public class SizeDistributionLogger<N extends Node> extends MHToFileLogger<MultiRoundTrip<N>> {

	private final int maxLength;

	private final boolean includeIntrazonal;

	private int[] lastSizeCnts = null;

	public SizeDistributionLogger(long samplingInterval, int maxSize, boolean includeIntrazonal, String logFileName) {
		super(samplingInterval, logFileName);
		this.maxLength = maxSize;
		this.includeIntrazonal = includeIntrazonal;
	}

	@Override
	public String createHeaderLine() {
		return "\t" + IntStream.rangeClosed(0, this.maxLength).boxed().map(s -> "Size=" + s)
				.collect(Collectors.joining("\t"));
	}

	@Override
	public String createDataLine(MultiRoundTrip<N> roundTrips, double logWeight) {
		this.lastSizeCnts = new int[this.maxLength + 1];
		for (var roundTrip : roundTrips) {
			if (this.includeIntrazonal) {
				this.lastSizeCnts[roundTrip.size()]++;
			} else {
				int size = 0;
				for (int i = 0; i < roundTrip.size(); i++) {
					if (!roundTrip.getNode(i).equals(roundTrip.getSuccessorNode(i))) {
						size++;
					}
				}
				this.lastSizeCnts[size]++;
			}
		}
		return "Iteration=" + this.iteration() + "\t" + Arrays.stream(this.lastSizeCnts).boxed()
				.map(c -> Integer.toString(c)).collect(Collectors.joining("\t"));
	}

	// for testing
	public int[] getLastSizeCounts() {
		return this.lastSizeCnts;
	}
}
