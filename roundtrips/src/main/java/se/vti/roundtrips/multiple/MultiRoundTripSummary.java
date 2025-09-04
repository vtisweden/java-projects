/**
 * se.vti.roundtrips.multiple
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
package se.vti.roundtrips.multiple;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 * 
 */
public interface MultiRoundTripSummary<L extends Node> {

	void clear();

	void update(int roundTripIndex, RoundTrip<L> oldRoundTrip, RoundTrip<L> newRoundTrip);

	default void update(RoundTrip<L> newRoundTrip, MultiRoundTrip<L> allRoundTrips) {
		int roundTripIndex = newRoundTrip.getIndex();
		update(roundTripIndex, allRoundTrips.getRoundTrip(roundTripIndex), newRoundTrip);
	}


	MultiRoundTripSummary<L> clone();

}
