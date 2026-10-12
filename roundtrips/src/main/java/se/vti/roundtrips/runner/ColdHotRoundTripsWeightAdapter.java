/**
 * se.vti.roundtrips.multiple
 * 
 * Copyright (C) 2024-26 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.runner;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.scenario.Node;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * @author GunnarF
 * 
 * @param <N>
 */
class ColdHotRoundTripsWeightAdapter<N extends Node> implements MHWeight<RoundTrips<N>> {

	private final MHWeight<MultiRoundTrip<N>> delegate;
	private final double hotScale;

	ColdHotRoundTripsWeightAdapter(MHWeight<MultiRoundTrip<N>> multiRoundTripWeight, double hotScale) {
		if (hotScale < 0.0 || hotScale > 1.0) {
			throw new IllegalArgumentException("hotScale is " + hotScale + ", which is not between zero and one");
		}
		this.delegate = multiRoundTripWeight;
		this.hotScale = hotScale;
	}

	@Override
	public double logWeight(RoundTrips<N> state) {
		var coldHotState = (ColdHotMultiRoundTrip<N>) state;
		return this.delegate.logWeight(coldHotState.getCold())
				+ this.hotScale * this.delegate.logWeight(coldHotState.getHot());
	}

}
