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

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.simulator.MoveEpisode;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
class CoverageWeight implements MHWeight<MultiRoundTrip<GridNode>> {

	private final int gridSize;

	private final double depotOpens_h; // evening hour
	private final double depotCloses_h; // morning hour

	CoverageWeight(int gridSize, double depotOpens_h, double depotCloses_h) {
		this.gridSize = gridSize;
		this.depotOpens_h = depotOpens_h;
		this.depotCloses_h = depotCloses_h;
	}

	@Override
	public double logWeight(MultiRoundTrip<GridNode> multiRoundTrip) {

		/*
		 * Check if all nodes can be reached, directly or indirectly, from the depot.
		 * 
		 * Start out by assuming that only the depot can be reached. The depot is at
		 * grid node (0,0).
		 */

		boolean[][] reached = new boolean[this.gridSize][this.gridSize];
		reached[0][0] = true;

		/*
		 * Check what nodes can be reached, by propagating reachability along all move
		 * episodes in all round trips. We do not considere before/after relationships
		 * when visiting nodes because there is a 24h-wraparound.
		 */

		boolean changed;
		do {
			changed = false;
			for (var roundTrip : multiRoundTrip) {
				var episodes = roundTrip.getEpisodes();
				for (int i = 1; i < episodes.size(); i += 2) {
					// A round trip implies an alternating sequence of stay/move episodes.
					var move = (MoveEpisode<GridNode>) episodes.get(i);
					var from = move.getOrigin();
					var to = move.getDestination();
					if (reached[from.row][from.column] && !reached[to.row][to.column]) {
						if (from.row != 0 || from.column != 0) {
							// not starting from the depot
							reached[to.row][to.column] = true;
							changed = true;
						} else {
							// starting from the depot
							double startTime_h = move.computeStartTime_h(24.0);
							if (startTime_h < this.depotCloses_h || startTime_h >= this.depotOpens_h) {
								reached[to.row][to.column] = true;
								changed = true;
							}
						}
					}
				}
			}
		} while (changed);

		/*
		 * Count how many grid nodes were not reached within 24h. The probability of
		 * sampling a round trip shall be the exponential of minus the number of missed
		 * nodes, so the log weight to be returned here is minus the number of missed
		 * nodes.
		 */

		int missed = 0;
		for (boolean[] row : reached) {
			for (boolean entry : row) {
				if (!entry) {
					missed++;
				}
			}
		}
		return -missed;
	}

}
