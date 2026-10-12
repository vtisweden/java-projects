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
package se.vti.roundtrips.multiple.coldhot;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.RoundTrips;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;

/**
 * @author GunnarF
 */
class ColdHotMultiRoundTrip<N extends Node> implements RoundTrips<N> {

	// -------------------- MEMBERS --------------------

	private final MultiRoundTrip<N> coldMultiRoundTrip;

	private final MultiRoundTrip<N> hotMultiRoundTrip;

	// -------------------- CONSTRUCTION --------------------

	ColdHotMultiRoundTrip(MultiRoundTrip<N> coldMultiRoundTrip, MultiRoundTrip<N> hotMultiRoundTrip) {
		assert (coldMultiRoundTrip.size() == hotMultiRoundTrip.size());
		this.coldMultiRoundTrip = coldMultiRoundTrip;
		this.hotMultiRoundTrip = hotMultiRoundTrip;
	}

	ColdHotMultiRoundTrip(int size) {
		this(new MultiRoundTrip<>(size), new MultiRoundTrip<>(size));
	}

	// -------------------- IMPLEMENTATION --------------------

	void switchRoundTrip(int i) {
		var previouslyColdRoundTrip = this.coldMultiRoundTrip.getRoundTrip(i);
		this.coldMultiRoundTrip.setRoundTripAndUpdateSummaries(i, this.hotMultiRoundTrip.getRoundTrip(i));
		this.hotMultiRoundTrip.setRoundTripAndUpdateSummaries(i, previouslyColdRoundTrip);
	}

	MultiRoundTrip<N> getCold() {
		return this.coldMultiRoundTrip;
	}

	MultiRoundTrip<N> getHot() {
		return this.hotMultiRoundTrip;
	}

	@Override
	public int size() {
		return this.coldMultiRoundTrip.size();
	}

	@Override
	public RoundTrip<N> getRoundTrip(int index) {
		return this.coldMultiRoundTrip.getRoundTrip(index);
	}

	@Override
	public int computeSumOfRoundTripSizes() {
		return this.coldMultiRoundTrip.computeSumOfRoundTripSizes();
	}

	@Override
	public MultiRoundTrip<N> getAsMultiRoundTrip() {
		return this.coldMultiRoundTrip;
	}

	@Override
	public ColdHotMultiRoundTrip<N> clone() {
		return new ColdHotMultiRoundTrip<N>(this.coldMultiRoundTrip.clone(), this.hotMultiRoundTrip.clone());
	}
}
