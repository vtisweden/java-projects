/**
 * se.vti.roundtrips.examples.truckServiceCoverage
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
package se.vti.roundtrips.examples.truckServiceCoverage;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.simulator.StayEpisode;
import se.vti.utils.misc.metropolishastings.MHToFileLogger;

/**
 * 
 * @author GunnarF
 *
 */
class EarliestArrivalLogger extends MHToFileLogger<MultiRoundTrip<GridNode>> {

	private final GridNode depot;
	private final int gridSize;

	EarliestArrivalLogger(GridNode depot, int gridSize, long samplingInterval, String logFileName) {
		super(samplingInterval, logFileName);
		this.depot = depot;
		this.gridSize = gridSize;
	}

	@Override
	public String createHeaderLine() {
		return "Iteration\t" + IntStream.rangeClosed(1, 2 * (this.gridSize - 1)).boxed().map(d -> "Dist=" + d)
				.collect(Collectors.joining("\t"));
	}

	@Override
	public String createDataLine(MultiRoundTrip<GridNode> multiRoundTrip) {
		double[] earliestArrival_h = new double[2 * (this.gridSize - 1)];
		Arrays.fill(earliestArrival_h, Double.POSITIVE_INFINITY);
		for (var roundTrip : multiRoundTrip) {
			for (int i = 0; i < roundTrip.getEpisodes().size(); i += 2) {
				var stay = (StayEpisode<GridNode>) roundTrip.getEpisodes().get(i);
				if (!this.depot.equals(stay.getLocation())) {
					double end_h = stay.getEndTime_h();
					int dist = this.depot.computeGridDistance(stay.getLocation());
					earliestArrival_h[dist - 1] = Math.min(earliestArrival_h[dist - 1], end_h);
				}
			}
		}
		return this.iteration() + "\t" + Arrays.stream(earliestArrival_h).boxed()
				.map(v -> v < Double.POSITIVE_INFINITY ? "" + v : "").collect(Collectors.joining("\t"));
	}
}
