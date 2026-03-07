/**
 * se.vti.roundtrips.examples.electrifiedFlight
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
package se.vti.roundtrips.examples.electrifiedFlight;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.multiple.MultiRoundTripSummary;
import se.vti.roundtrips.simulator.Episode;
import se.vti.roundtrips.simulator.MoveEpisode;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.Tuple;

/**
 * @author GunnarF
 */
class PlaneUsageSummary implements MultiRoundTripSummary<NodeWithCoords> {

	private final int numberOfPlanes;
	private final int planeCapacity_pax;

	private Map<Tuple<Node, Node>, Double> od2demand = new LinkedHashMap<>();
	private Map<Tuple<Node, Node>, Map<Integer, Integer>> od2plane2coverage = new LinkedHashMap<>();
	private Map<Tuple<Node, Node>, Integer> od2coverage = new LinkedHashMap<>();

	private Integer numberOfMissingPlaneTrips = null;
	private Double numberOfUnservedPassengers = null;

	PlaneUsageSummary(int numberOfPlanes, int planeCapacity_pax) {
		this.numberOfPlanes = numberOfPlanes;
		this.planeCapacity_pax = planeCapacity_pax;
		this.clear();
	}

	void setDemand(Node from, Node to, double demand_pax) {
		this.od2demand.put(new Tuple<>(from, to), demand_pax);
	}

	double getDemand(Node from, Node to) {
		return this.od2demand.getOrDefault(new Tuple<>(from, to), 0.0);
	}

	int getCoverage(Node from, Node to) {
		return this.od2coverage.getOrDefault(new Tuple<>(from, to), 0);
	}

	private void setCoverage(Node from, Node to, int coverage) {
		this.od2coverage.put(new Tuple<>(from, to), coverage);
	}

	private int getIndividualCoverage(int planeIndex, Node from, Node to) {
		var od = new Tuple<>(from, to);
		if (this.od2plane2coverage.containsKey(od)) {
			return this.od2plane2coverage.get(od).getOrDefault(planeIndex, 0);
		} else {
			return 0;
		}
	}

	Integer getNumberOfMissingPlaneTrips() {
		return this.numberOfMissingPlaneTrips;
	}

	Double getNumberOfServedPassengers() {
		return this.od2demand.values().stream().mapToDouble(d -> d).sum() - this.numberOfUnservedPassengers;
	}

	Double getNumberOfUnservedPassengers() {
		return this.numberOfUnservedPassengers;
	}

	@Override
	public void clear() {
		this.od2coverage = new LinkedHashMap<>();
		this.od2plane2coverage = new LinkedHashMap<>();
	}

	@Override
	public void update(int roundTripIndex, RoundTrip<NodeWithCoords> oldRoundTrip,
			RoundTrip<NodeWithCoords> newRoundTrip) {
		/*
		 * Compute effect of round trip change on coverage.
		 */
		if (oldRoundTrip != null) {
			List<Episode> oldEpisodes = oldRoundTrip.getEpisodes();
			for (int i = 1; i < oldEpisodes.size(); i += 2) {
				MoveEpisode<Node> stay = (MoveEpisode<Node>) oldEpisodes.get(i);
				Node from = stay.getOrigin();
				Node to = stay.getDestination();
				this.setCoverage(from, to, this.getCoverage(from, to) - 1);
				var od = new Tuple<>(from, to);
				this.od2coverage.compute(od, (od2, c) -> c - this.getIndividualCoverage(roundTripIndex, from, to));
				this.od2plane2coverage.computeIfAbsent(od, od2 -> new LinkedHashMap<>()).remove(roundTripIndex);
			}
		}
		if (newRoundTrip != null) {
			List<Episode> newEpisodes = newRoundTrip.getEpisodes();
			for (int i = 1; i < newEpisodes.size(); i += 2) {
				MoveEpisode<Node> stay = (MoveEpisode<Node>) newEpisodes.get(i);
				Node from = stay.getOrigin();
				Node to = stay.getDestination();
				this.setCoverage(from, to, this.getCoverage(from, to) + 1);
				var od = new Tuple<>(from, to);
				this.od2plane2coverage.computeIfAbsent(od, od2 -> new LinkedHashMap<>()).compute(roundTripIndex,
						(r, c) -> (c == null) ? 1 : (c + 1));
				this.od2coverage.compute(od,
						(od2, c) -> (c == null ? 0 : c) - this.getIndividualCoverage(roundTripIndex, from, to));
			}
		}

		/*
		 * Compute summary statistics.
		 */
		this.numberOfMissingPlaneTrips = 0;
		this.numberOfUnservedPassengers = 0.0;
		for (Map.Entry<Tuple<Node, Node>, Double> fromDemand : this.od2demand.entrySet()) {
			var od = fromDemand.getKey();
			double minDemand_pax = fromDemand.getValue();
			int coverage = this.od2coverage.getOrDefault(od, 0);
			double unservedDemand_pax = Math.max(0, minDemand_pax - coverage * planeCapacity_pax);
			this.numberOfUnservedPassengers += unservedDemand_pax;
			this.numberOfMissingPlaneTrips += (int) Math.ceil(unservedDemand_pax / planeCapacity_pax);
		}
	}

	@Override
	public PlaneUsageSummary clone() {
		PlaneUsageSummary child = new PlaneUsageSummary(this.numberOfPlanes, this.planeCapacity_pax);
		child.od2demand = this.od2demand;
		child.od2coverage = new LinkedHashMap<>(this.od2coverage);
		for (var entry : this.od2plane2coverage.entrySet()) {
			child.od2plane2coverage.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
		}
		child.numberOfMissingPlaneTrips = this.numberOfMissingPlaneTrips;
		child.numberOfUnservedPassengers = this.numberOfUnservedPassengers;
		return child;
	}
}
