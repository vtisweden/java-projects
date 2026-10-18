/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.examples.samplingweights.timeuse;

import java.util.function.Function;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.scenario.Node;

/**
 * @author GunnarF
 * 
 * @param <N> the node type
 */
public class UniqueHomeLocationModifier<N extends Node> implements Function<MultiRoundTrip<N>, MultiRoundTrip<N>> {

	@Override
	public MultiRoundTrip<N> apply(MultiRoundTrip<N> original) {

		var modified = original.clone();

		N home = null;
		for (int i = 0; i < (modified.size()) && (home == null); i++) {
			var roundTrip = modified.getRoundTrip(i);
			if (roundTrip.size() > 0) {
				home = roundTrip.getNode(0);
			}
		}

		if (home != null) {
			for (int i = 1; i < modified.size(); i++) {
				var roundTrip = modified.getRoundTrip(i);
				if ((roundTrip.size() > 0) && !home.equals(roundTrip.getNode(0))) {
					var newRoundTrip = roundTrip.clone();
					newRoundTrip.setNode(0, home);
					modified.setRoundTripAndUpdateSummaries(i, newRoundTrip);
				}
			}
		}

		return modified;
	}

}
