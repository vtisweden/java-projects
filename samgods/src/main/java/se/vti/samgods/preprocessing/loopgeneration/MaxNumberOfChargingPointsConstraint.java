/**
 * se.vti.samgods
 * 
 * Copyright (C) 2025,2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.preprocessing.loopgeneration;

import java.util.LinkedHashMap;
import java.util.Map;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.simulator.electrified.Charging;
import se.vti.roundtrips.simulator.electrified.ChargingUtils;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
class MaxNumberOfChargingPointsConstraint<N extends Node> implements MHWeight<MultiRoundTrip<N>> {

	private final int maxNumberOfChargingPoints;
	private final ChargingUtils utils = new ChargingUtils();

	MaxNumberOfChargingPointsConstraint(int maxNumberOfChargingPoints) {
		this.maxNumberOfChargingPoints = maxNumberOfChargingPoints;
	}

	static <M extends Node> Map<M, Integer> computeChargingNodeUsages(MultiRoundTrip<M> roundTrips,
			ChargingUtils utils) {
		Map<M, Integer> chargingNodeUsages = new LinkedHashMap<>();
		for (var roundTrip : roundTrips) {
			for (int i = 0; i < roundTrip.size(); i++) {
				M node = roundTrip.getNode(i);
				if (Charging.YES == utils.extractCharging(node)) {
					chargingNodeUsages.compute(node, (n, c) -> (c == null ? 0 : c) + 1);
				}
			}
		}
		return chargingNodeUsages;
	}

	@Override
	public double logWeight(MultiRoundTrip<N> roundTrips) {
		if (computeChargingNodeUsages(roundTrips, this.utils).size() <= this.maxNumberOfChargingPoints) {
			return 0.0;
		} else {
			return Double.NEGATIVE_INFINITY;
		}
	}

}
