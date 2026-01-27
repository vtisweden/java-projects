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

import java.util.List;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.simulator.StayEpisode;

/**
 * @author GunnarF
 */
public enum Charging {

	YES, NO;

	public static Charging extractCharging(Node node) {
		List<? extends Enum<?>> labels = node.getLabels();
		for (int i = 0; i < labels.size(); i++) {
			if (labels.get(i) instanceof Charging chargingLabel) {
				return chargingLabel;
			}
		}
		return null;
	}

	public static Charging extractCharging(StayEpisode<?> parking) {
		return extractCharging(parking.getLocation());
	}

}
