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

import java.util.Set;

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
class ChargingPointsConstraint<N extends Node> implements MHWeight<MultiRoundTrip<N>> {

	private final Set<N> chargingNodes;

	private final ChargingUtils utils = new ChargingUtils();

	ChargingPointsConstraint(Set<N> chargingNodes) {
		this.chargingNodes = chargingNodes;
	}

	@Override
	public double logWeight(MultiRoundTrip<N> roundTrips) {
		for (var roundTrip : roundTrips) {
			for (int i = 0; i < roundTrip.size(); i++) {
				N node = roundTrip.getNode(i);
				if ((this.utils.extractCharging(node) == Charging.YES) && !this.chargingNodes.contains(node)) {
					return Double.NEGATIVE_INFINITY;
				}
			}
		}
		return 0.0;
	}
}
