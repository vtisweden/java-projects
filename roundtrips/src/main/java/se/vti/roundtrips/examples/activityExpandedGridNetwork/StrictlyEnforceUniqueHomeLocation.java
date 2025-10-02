/**
 * se.vti.roundtrips.examples.travelSurveyExpansion
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
package se.vti.roundtrips.examples.activityExpandedGridNetwork;

import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class StrictlyEnforceUniqueHomeLocation implements MHWeight<RoundTrip<GridNodeWithActivity>> {

	@Override
	public double logWeight(RoundTrip<GridNodeWithActivity> roundTrip) {
		GridNodeWithActivity firstNode = roundTrip.getNode(0);
		if (Activity.HOME.equals(firstNode.getActivity())) {
			for (int i = 1; i < roundTrip.size(); i++) {
				GridNodeWithActivity node = roundTrip.getNode(i);
				if (Activity.HOME.equals(node.getActivity()) && !(firstNode.equals(node))) {
					return Double.NEGATIVE_INFINITY;
				}
			}
			return 0;
		} else {
			return Double.NEGATIVE_INFINITY;
		}
	}
}
