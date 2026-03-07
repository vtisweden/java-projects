/**
 * se.vti.skellefeaV2X
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
import se.vti.roundtrips.simulator.Episode;
import se.vti.roundtrips.simulator.MoveEpisode;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class StrictlyNonNegativeBatteryCharge<N extends Node> implements MHWeight<RoundTrip<N>> {

	public StrictlyNonNegativeBatteryCharge() {
	}

	@Override
	public double logWeight(RoundTrip<N> roundTrip) {
		if (roundTrip.size() >= 2) {
			List<Episode> episodes = roundTrip.getEpisodes();
			for (int i = 1; i < episodes.size(); i += 2) {
				var batteryState = (BatteryState) ((MoveEpisode<?>) episodes.get(i)).getFinalState();
				if (batteryState.getCharge_kWh() < 0.0) {
					return Double.NEGATIVE_INFINITY;
				}
			}
		}
		return 0.0;
	}

}
