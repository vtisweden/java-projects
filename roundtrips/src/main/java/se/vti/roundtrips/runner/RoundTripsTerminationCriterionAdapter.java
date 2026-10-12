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
import se.vti.utils.misc.metropolishastings.terminationcriteria.TerminationCriterion;

/**
 * @author GunnarF
 * 
 * @param <N> node type
 */
class RoundTripsTerminationCriterionAdapter<N extends Node> implements TerminationCriterion<RoundTrips<N>> {

	private final TerminationCriterion<MultiRoundTrip<N>> delegate;
	
	RoundTripsTerminationCriterionAdapter(TerminationCriterion<MultiRoundTrip<N>> delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public void start() {
		this.delegate.start();
	}

	@Override
	public void processState(RoundTrips<N> state, double logWeight) {
		this.delegate.processState(state.getMultiRoundTrip(), logWeight);
	}

	@Override
	public void end() {
		this.delegate.end();
	}

	@Override
	public boolean terminate() {
		return this.delegate.terminate();
	}

}
