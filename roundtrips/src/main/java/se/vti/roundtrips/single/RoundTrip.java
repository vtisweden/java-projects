/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023,2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.single;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.simulator.Episode;

/**
 * 
 * @author GunnarF
 *
 * @param <N> the location type
 * 
 */
public class RoundTrip<N extends Node> implements Iterable<RoundTrip<N>> {

	// -------------------- CONSTANTS --------------------

	private final int index;

//	private final Object attributes;

	// -------------------- MEMBERS --------------------

	private List<N> nodes;

	private List<Integer> departures;

	private List<Episode> episodes = null;

	// -------------------- CONSTRUCTION --------------------

	public RoundTrip(int index, List<N> nodes, List<Integer> departures) {
		this.index = index;
		this.nodes = nodes;
		this.departures = departures;
//		this.attributes = attributes;
	}

//	public RoundTrip(List<N> nodes, List<Integer> departures) {
//		this(0, nodes, departures);
//	}

	// -------------------- INTERNALS --------------------

	public int predecessorIndex(int i) {
		if (i > 0) {
			return i - 1;
		} else {
			return this.size() - 1;
		}
	}

	public int successorIndex(int i) {
		if (i < this.size() - 1) {
			return i + 1;
		} else {
			return 0;
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public int getIndex() {
		return this.index;
	}

//	public Object getAttributes() {
//		return this.attributes;
//	}

	public int size() {
		return this.nodes.size();
	}

	public N getPredecessorNode(int i) {
		return this.nodes.get(this.predecessorIndex(i));
	}

	public N getNode(int i) {
		return this.nodes.get(i);
	}

	public N getSuccessorNode(int i) {
		return this.nodes.get(this.successorIndex(i));
	}

	public List<N> getNodesView() {
		return Collections.unmodifiableList(this.nodes);
	}

	public void setNode(int i, N node) {
		this.nodes.set(i, node);
	}

	public void setDepartureAndEnsureOrdering(int i, Integer departureBin) {
		this.departures.set(i, departureBin);
		Collections.sort(this.departures);
	}

	public Integer getDeparture(int i) {
		return this.departures.get(i);
	}

	public Integer getNextDeparture(int i) {
		return this.departures.get(this.successorIndex(i));
	}

	public boolean containsDeparture(int bin) {
		return this.departures.contains(bin);
	}

	public List<Integer> getDeparturesView() {
		return Collections.unmodifiableList(this.departures);
	}

	public void addAndEnsureSortedDepartures(int i, N node, Integer departureBin) {
		this.nodes.add(i, node);
		this.departures.add(i, departureBin);
		Collections.sort(this.departures);
	}

	public void remove(int nodeIndex, int departureIndex) {
		this.nodes.remove(nodeIndex);
		this.departures.remove(departureIndex);
	}

	public void remove(int i) {
		this.remove(i, i);
	}

	public ArrayList<N> cloneNodes() {
		return new ArrayList<>(this.nodes);
	}

	public ArrayList<Integer> cloneDepartures() {
		return new ArrayList<>(this.departures);
	}

	public List<Episode> getEpisodes() {
		return episodes;
	}

	public void setEpisodes(List<Episode> episodes) {
		this.episodes = episodes;
	}

	public void cloneEpisodes(RoundTrip<N> other) {
		// Deliberately not checking for this.episodes==null, should fail clearly.
		this.episodes = new ArrayList<>(other.episodes.size());
		for (Episode episode : other.episodes) {
			this.episodes.add(episode.clone());
		}
	}

	// -------------------- IMPLEMENTATION OF Iterable --------------------
	
	@Override
	public Iterator<RoundTrip<N>> iterator() {
		return Collections.singleton(this).iterator();
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public RoundTrip<N> clone() {
		// TODO for this to work, attributes have to be immutable, as they are shared by
		// round trips
		final RoundTrip<N> result = new RoundTrip<>(this.index, this.cloneNodes(), this.cloneDepartures());
		if (this.episodes != null) {
			result.cloneEpisodes(this);
		}
		return result;
	}

//	@Override
//	public boolean equals(Object other) {
//		throw new UnsupportedOperationException("Don't want to use this method, talk to Gunnar.");
////		if (other instanceof RoundTrip) {
////			RoundTrip<?> otherRoundTrip = (RoundTrip<?>) other;
////			return (this.index == otherRoundTrip.index) && this.nodes.equals(otherRoundTrip.nodes)
////					&& this.departures.equals(otherRoundTrip.departures);
////		} else {
////			return false;
////		}
//	}
//
//	@Override
//	public int hashCode() {
//		throw new UnsupportedOperationException("Don't want to use this method, talk to Gunnar.");
////		return this.nodes.hashCode() + 31 * this.departures.hashCode();
//	}

	@Override
	public String toString() {
		return "index=" + this.index + ",nodes["
				+ this.nodes.stream().map(l -> l.toString()).collect(Collectors.joining(",")) + "],bins["
				+ this.departures.stream().map(l -> l.toString()).collect(Collectors.joining(",")) + "]";
	}
}
