/**
 * se.vti.roundtrips.multiple
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
package se.vti.roundtrips.multi.grouping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.multi.MultiRoundTripSummary;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 * @param <N>
 */
public abstract class ByPopulationGroupSummary<N extends Node, S extends MultiRoundTripSummary<N>>
		implements MultiRoundTripSummary<N> {

	// -------------------- MEMBERS --------------------

	private final Map<String, S> group2summary = new LinkedHashMap<>();

	private final Map<Integer, S> index2summary = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ByPopulationGroupSummary(final PopulationGrouping grouping, final Supplier<S> summaryFactory,
			Set<String> consideredGroups) {
		for (String group : consideredGroups) {
			final S summary = summaryFactory.get();
			this.group2summary.put(group, summary);
			for (int index : grouping.getGroup2Indices().get(group)) {
				assert(!this.index2summary.containsKey(index));
				this.index2summary.put(index, summary);
			}
		}
	}

	// for deep cloning in subclasses
	public ByPopulationGroupSummary(ByPopulationGroupSummary<N, S> parent) {
		final Map<S, S> summary2clone = new LinkedHashMap<>(parent.group2summary.size());
		for (Map.Entry<String, S> entry : parent.group2summary.entrySet()) {
			final String group = entry.getKey();
			final S summary = entry.getValue();
			final S clonedSummary = summary2clone.computeIfAbsent(summary, ps -> (S) ps.clone());
			this.group2summary.put(group, clonedSummary);
		}
		for (Map.Entry<Integer, S> entry : parent.index2summary.entrySet()) {
			final Integer index = entry.getKey();
			final S summary = entry.getValue();
			this.index2summary.put(index, summary2clone.get(summary));
		}
	}

	// -------------------- PARTIAL IMPLEMENTATION --------------------

	public Map<String, S> getSummaries() {
		return this.group2summary;
	}

	@Override
	public void clear() {
		for (S summary : this.group2summary.values()) {
			summary.clear();
		}
	}

	@Override
//	public void update(int roundTripIndex, RoundTrip<L> oldRoundTrip, RoundTrip<L> newRoundTrip) {
//		final S summary = this.index2summary.get(roundTripIndex);
//		if (summary != null) {
//			summary.update(roundTripIndex, oldRoundTrip, newRoundTrip);
//		}
//	}
	public void update(RoundTrip<N> newRoundTrip, MultiRoundTrip<N> oldRoundTrips) {
		final S summary = this.index2summary.get(newRoundTrip.getIndex());
		if (summary != null) {
			summary.update(newRoundTrip, oldRoundTrips);
		}
	}

	@Override
	public abstract ByPopulationGroupSummary<N, S> clone();

}
