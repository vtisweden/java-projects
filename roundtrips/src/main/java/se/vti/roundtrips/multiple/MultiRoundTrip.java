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
package se.vti.roundtrips.multiple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.RoundTrips;
import se.vti.roundtrips.simulator.Simulator;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 * @param <N>
 */
public class MultiRoundTrip<N extends Node> implements Iterable<RoundTrip<N>>, RoundTrips<N> {

	// -------------------- MEMBERS --------------------

	private final List<RoundTrip<N>> roundTrips;

	private final Map<Class<?>, MultiRoundTripSummary<N>> class2summary = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public MultiRoundTrip(int size) {
		this.roundTrips = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			this.roundTrips.add(null);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public final void setRoundTripAndUpdateSummaries(int i, RoundTrip<N> roundTrip) {
		if (roundTrip == this.getRoundTrip(i)) {
			return;
		}
		if ((roundTrip != null) && (i != roundTrip.getIndex())) {
			throw new RuntimeException(
					"Trying to add RoundTrip #" + i + ", but added RoundTrip has index " + roundTrip.getIndex());
		}
		for (MultiRoundTripSummary<N> summaryStats : this.class2summary.values()) {
			summaryStats.update(roundTrip, this);
		}
		this.roundTrips.set(i, roundTrip);
	}

	public RoundTrip<N> getRoundTrip(int i) {
		return this.roundTrips.get(i);
	}

	public int size() {
		return this.roundTrips.size();
	}
	
	public int computeSumOfRoundTripSizes() {
		int sizeSum = 0;
		for (var roundTrip : this) {
			sizeSum += roundTrip.size();
		}
		return sizeSum;		
	}

	public void addSummary(MultiRoundTripSummary<N> summary) {
		this.class2summary.put(summary.getClass(), summary);
	}

	public <S extends MultiRoundTripSummary<N>> S getSummary(Class<S> summaryClass) {
		return (S) this.class2summary.get(summaryClass);
	}

	public void recomputeSummaries() {
		for (MultiRoundTripSummary<N> summary : this.class2summary.values()) {
			summary.clear();
		}
		for (int i = 0; i < this.roundTrips.size(); i++) {
			final RoundTrip<N> roundTrip = this.roundTrips.get(i);
			this.roundTrips.set(i, null);
			this.setRoundTripAndUpdateSummaries(i, roundTrip);
		}
	}

	public void simulateAll(Simulator<N> simulator) {
		for (RoundTrip<N> roundTrip : this.roundTrips) {
			roundTrip.setEpisodes(simulator.simulate(roundTrip));
		}
	}

	// -------------------- IMPLEMENTATION OF Iterable --------------------

	@Override
	public Iterator<RoundTrip<N>> iterator() {
		return this.roundTrips.iterator();
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public MultiRoundTrip<N> clone() {

		// Has initially no summary stats.
		final MultiRoundTrip<N> result = new MultiRoundTrip<N>(this.size());

		// Not yet any summary stats to update.
		for (int i = 0; i < this.roundTrips.size(); i++) {
			result.setRoundTripAndUpdateSummaries(i, this.getRoundTrip(i));
		}

		// Only now, clone summaries.
		for (MultiRoundTripSummary<N> summary : this.class2summary.values()) {
			result.addSummary(summary.clone());
		}

		return result;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("{");
		int i = 0;
		while (result.length() < 80 && i < this.size()) {
			if (i > 0) {
				result.append(",");
			}
			result.append(this.roundTrips.get(i));
			i++;
		}
		if (i < this.size()) {
			result.append(",...");
		}
		result.append("}");
		return result.toString();
	}

	@Override
	public MultiRoundTrip<N> getAsMultiRoundTrip() {
		return this;
	}
}
