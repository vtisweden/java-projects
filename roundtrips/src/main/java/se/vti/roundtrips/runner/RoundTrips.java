/**
 * se.vti.roundtrips.multiple
 * 
 * Copyright (C) 2024-2026 by Gunnar Flötteröd (VTI, LiU).
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

import java.util.Iterator;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.multi.MultiRoundTripSummary;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.simulator.Simulator;
import se.vti.roundtrips.single.RoundTrip;

/**
 * @author GunnarF
 * 
 * @param <N> the node type
 */
public interface RoundTrips<N extends Node> extends Iterable<RoundTrip<N>> {

	MultiRoundTrip<N> getMultiRoundTrip();

	RoundTrips<N> clone();

	void simulateAll(Simulator<N> simulator);

	default int size() {
		return this.getMultiRoundTrip().size();
	}

	default RoundTrip<N> getRoundTrip(int index) {
		return this.getMultiRoundTrip().getRoundTrip(index);
	}

	default int computeSumOfRoundTripSizes() {
		return this.getMultiRoundTrip().computeSumOfRoundTripSizes();
	}

	default <S extends MultiRoundTripSummary<N>> S getSummary(Class<S> summaryClass) {
		return this.getMultiRoundTrip().getSummary(summaryClass);
	}

	default Iterator<RoundTrip<N>> iterator() {
		return this.getMultiRoundTrip().iterator();
	}

}
