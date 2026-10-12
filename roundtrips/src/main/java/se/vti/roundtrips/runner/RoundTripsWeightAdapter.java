/**
 * se.vti.utils
 * 
 * Copyright (C) 2023-2026 by Gunnar Flötteröd (VTI, LiU).
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
 */
class RoundTripsWeightAdapter<N extends Node> implements MHWeight<RoundTrips<N>> {

	private final MHWeight<MultiRoundTrip<N>> delegate;

	RoundTripsWeightAdapter(MHWeight<MultiRoundTrip<N>> delegate) {
		this.delegate = delegate;
	}

	@Override
	public String name() {
		return this.delegate.name();
	}

	@Override
	public double logWeight(RoundTrips<N> state) {
		return this.delegate.logWeight(state.getMultiRoundTrip());
	}
}
