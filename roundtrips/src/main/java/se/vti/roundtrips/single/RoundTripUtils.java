/**
 * se.vti.roundtrips.single
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
package se.vti.roundtrips.single;

import java.util.List;

import se.vti.roundtrips.simulator.Episode;
import se.vti.roundtrips.simulator.MoveEpisode;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripUtils {

	private static final RoundTripUtils singleton = new RoundTripUtils();

	public static RoundTripUtils singleton() {
		return singleton;
	}

	public RoundTripUtils() {
	}

	// -------------------- TOTAL TRAVEL TIME --------------------

	public double computeTotalMoveDuration_h(Iterable<RoundTrip<?>> roundTrips) {
		double totalDuration_h = 0;
		for (var roundTrip : roundTrips) {
			List<Episode> episodes = roundTrip.getEpisodes();
			for (int i = 1; i < episodes.size(); i += 2) {
				totalDuration_h += ((MoveEpisode<?>) episodes.get(i)).getDuration_h();
			}
		}
		return totalDuration_h;
	}

	// -------------------- ... --------------------

	public int countInterzonalTrips(RoundTrip<?> roundTrip) {
		if (roundTrip.size() < 1) {
			return 0;
		}
		int cnt = 0;
		List<Episode> episodes = roundTrip.getEpisodes();
		for (int i = 1; i < episodes.size(); i += 2) {
			MoveEpisode<?> move = (MoveEpisode<?>) episodes.get(i);
			if (!move.getOrigin().equals(move.getDestination())) {
				cnt++;
			}
		}
		return cnt;
	}

}
