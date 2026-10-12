/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2025-2026 by Gunnar Flötteröd (VTI, LiU).
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
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * @author GunnarF
 * 
 * @param <N> node type
 */
class RoundTripsStateProcessorAdapter<N extends Node> implements MHStateProcessor<RoundTrips<N>> {

	private final MHStateProcessor<MultiRoundTrip<N>> delegate;
	
	RoundTripsStateProcessorAdapter(MHStateProcessor<MultiRoundTrip<N>> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void start() {
		this.delegate.start();
	}

	@Override
	public void processState(RoundTrips<N> roundTrips, double logWeight) {
		this.delegate.processState(roundTrips.getMultiRoundTrip(), logWeight);
	}

	@Override
	public void end() {
		this.delegate.end();
	}
	
}
