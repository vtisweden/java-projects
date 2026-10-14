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
package se.vti.roundtrips.runner.coldhot;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.runner.MultiRoundTripWrapper;
import se.vti.roundtrips.scenario.Node;

/**
 * @author GunnarF
 */
public class ColdHotMultiRoundTrip<N extends Node> implements MultiRoundTripWrapper<N> {

	// -------------------- MEMBERS --------------------

	private final MultiRoundTrip<N> coldMultiRoundTrip;

	private final MultiRoundTrip<N> hotMultiRoundTrip;

	// -------------------- CONSTRUCTION --------------------

	ColdHotMultiRoundTrip(MultiRoundTrip<N> coldMultiRoundTrip, MultiRoundTrip<N> hotMultiRoundTrip) {
		assert (coldMultiRoundTrip.size() == hotMultiRoundTrip.size());
		this.coldMultiRoundTrip = coldMultiRoundTrip;
		this.hotMultiRoundTrip = hotMultiRoundTrip;
	}

	// -------------------- IMPLEMENTATION --------------------

	void switchRoundTrip(int i) {
		var previouslyColdRoundTrip = this.coldMultiRoundTrip.getRoundTrip(i);
		this.coldMultiRoundTrip.setRoundTripAndUpdateSummaries(i, this.hotMultiRoundTrip.getRoundTrip(i));
		this.hotMultiRoundTrip.setRoundTripAndUpdateSummaries(i, previouslyColdRoundTrip);
	}

	public MultiRoundTrip<N> getCold() {
		return this.coldMultiRoundTrip;
	}

	public MultiRoundTrip<N> getHot() {
		return this.hotMultiRoundTrip;
	}

	// -------------------- IMPLEMENTATION OF RoundTrips --------------------

	@Override
	public MultiRoundTrip<N> getMultiRoundTrip() {
		return this.coldMultiRoundTrip;
	}

//	@Override
//	public void simulateAll(Simulator<N> simulator) {
//		this.coldMultiRoundTrip.simulateAll(simulator);
//		this.hotMultiRoundTrip.simulateAll(simulator);
//	}

	public int size() {
		return this.coldMultiRoundTrip.size();
	}

	@Override
	public ColdHotMultiRoundTrip<N> clone() {
		return new ColdHotMultiRoundTrip<N>(this.coldMultiRoundTrip.clone(), this.hotMultiRoundTrip.clone());
	}

	@Override
	public String toString() {
		return this.coldMultiRoundTrip.toString();
	}
}
