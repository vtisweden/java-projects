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
import se.vti.utils.misc.metropolishastings.MHToFileLogger;

/**
 * 
 * @author GunnarF
 *
 */
class MissionLogger extends MHToFileLogger<MultiRoundTrip<GridNode>> {

	private final GridNode depot;

	MissionLogger(GridNode depot, long samplingInterval) {
		super(samplingInterval, "./output/truckServiceCoverage/missions.log");
		this.depot = depot;
	}

	@Override
	public String createHeaderLine() {
		return "Iteration\tGoingThroughDepot\tNeverAtDeplot\tUnused";
	}

	@Override
	public String createDataLine(MultiRoundTrip<GridNode> state) {
		int goingThroughDepot = 0;
		int neverAtDepot = 0;
		int unused = 0;
		for (var roundTrip : state) {
			if (roundTrip.size() < 2) {
				unused++;
			} else if (roundTrip.getNodesView().contains(this.depot)) {
				goingThroughDepot++;
			} else {
				neverAtDepot++;
			}
		}
		return this.iteration() + "\t" + goingThroughDepot + "\t" + neverAtDepot + "\t"
				+ unused;
	}

}
