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

	private final MultiRoundTrip<N> cold;

	private final MultiRoundTrip<N> hot;

	// -------------------- CONSTRUCTION --------------------

	ColdHotMultiRoundTrip(MultiRoundTrip<N> cold, MultiRoundTrip<N> hot) {
		assert (cold.size() == hot.size());
		this.cold = cold;
		this.hot = hot;
	}

	// -------------------- IMPLEMENTATION --------------------

	void switchRoundTrip(int i) {
		var previouslyColdRoundTrip = this.cold.getRoundTrip(i);
		this.cold.setRoundTripAndUpdateSummaries(i, this.hot.getRoundTrip(i));
		this.hot.setRoundTripAndUpdateSummaries(i, previouslyColdRoundTrip);
	}

	public MultiRoundTrip<N> getCold() {
		return this.cold;
	}

	public MultiRoundTrip<N> getHot() {
		return this.hot;
	}

	// --------------- IMPLEMENTATION OF MultiRoundTripWrapper ---------------

	@Override
	public MultiRoundTrip<N> getMultiRoundTrip() {
		return this.cold;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public ColdHotMultiRoundTrip<N> clone() {
		return new ColdHotMultiRoundTrip<N>(this.cold.clone(), this.hot.clone());
	}

	@Override
	public String toString() {
		return this.cold.toString();
	}
}
