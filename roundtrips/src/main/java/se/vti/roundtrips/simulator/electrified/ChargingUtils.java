/**
 * se.vti.roundtrips.simulator.electrified
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
package se.vti.roundtrips.simulator.electrified;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.simulator.StayEpisode;

/**
 * @author GunnarF
 */
public class ChargingUtils {

	private static final ChargingUtils singleton = new ChargingUtils();

	public static ChargingUtils singleton() {
		return singleton;
	}

	public ChargingUtils() {
	}

	public Charging extractCharging(Node node) {
		List<? extends Enum<?>> labels = node.getLabels();
		for (int i = 0; i < labels.size(); i++) {
			if (labels.get(i) instanceof Charging chargingLabel) {
				return chargingLabel;
			}
		}
		return null;
	}
	
	public <N extends Node> Map<N, Integer> computeChargingNodeUsages(MultiRoundTrip<N> roundTrips) {
		Map<N, Integer> chargingNodeUsages = new LinkedHashMap<>();
		for (var roundTrip : roundTrips) {
			for (int i = 0; i < roundTrip.size(); i++) {
				N node = roundTrip.getNode(i);
				if (Charging.YES == this.extractCharging(node)) {
					chargingNodeUsages.compute(node, (n, c) -> (c == null ? 0 : c) + 1);
				}
			}
		}
		return chargingNodeUsages;
	}


	public Charging extractCharging(StayEpisode<?> parking) {
		return this.extractCharging(parking.getLocation());
	}

}
