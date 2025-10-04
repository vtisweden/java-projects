/**
 * se.vti.roundtrips.samplingweights.misc
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
package se.vti.roundtrips.samplingweights.misc.timeUse;

import java.util.ArrayList;
import java.util.List;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class LogarithmicTimeMultiplePersonsSingleDay<N extends Node> implements MHWeight<MultiRoundTrip<N>> {

	private final List<LogarithmicTimeUse<N>> logarithmicTimeUsePerTraveler;

	public LogarithmicTimeMultiplePersonsSingleDay(int numberOfTravelers, double period_h) {
		this.logarithmicTimeUsePerTraveler = new ArrayList<>(numberOfTravelers);
		for (int n = 0; n < numberOfTravelers; n++) {
			this.logarithmicTimeUsePerTraveler.add(new LogarithmicTimeUse<>(period_h));
		}
	}

	public void assignComponent(LogarithmicTimeUseComponent component, N node, int personIndex) {
		this.logarithmicTimeUsePerTraveler.get(personIndex).assignComponent(component, node, 0);
	}

	@Override
	public double logWeight(MultiRoundTrip<N> roundTrips) {
		double logWeight = 0.0;
		for (int roundTripIndex = 0; roundTripIndex < roundTrips.size(); roundTripIndex++) {
			logWeight += this.logarithmicTimeUsePerTraveler.get(roundTripIndex)
					.computeLogWeight(roundTrips.getRoundTrip(roundTripIndex));
		}
		return logWeight;
	}

}
