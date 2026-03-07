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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

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

	private final int fleetSize;
	private final double rangeFactor;

	private Map<Tuple<Node, Node>, Double> od2demand = new LinkedHashMap<>();
	private Map<Tuple<Node, Node>, Integer> od2coverage = new LinkedHashMap<>();
	private Map<Tuple<Node, Node>, Set<Integer>> od2coveringPlaneIndices = new LinkedHashMap<>();

	PlaneUsageSummary(int fleetSize, double rangeFactor) {
		this.fleetSize = fleetSize;
		this.rangeFactor = rangeFactor;
		this.clear();
	}

	void setDemand(Node from, Node to, double demand_pax) {
		this.od2demand.put(new Tuple<>(from, to), demand_pax);
	}

	double getDemand(Node from, Node to) {
		return this.od2demand.getOrDefault(new Tuple<>(from, to), 0.0);
	}

	private int getCoverage(Node from, Node to) {
		return this.od2coverage.getOrDefault(new Tuple<>(from, to), 0);
	}

	private void setCoverage(Node from, Node to, int coverage) {
		this.od2coverage.put(new Tuple<>(from, to), coverage);
	}

	int computeNumberOfMissingPlaneTrips(int planeCapacity_pax) {
		int missingPlaneTrips = 0;
		for (Map.Entry<Tuple<Node, Node>, Double> fromDemand : this.od2demand.entrySet()) {
			var od = fromDemand.getKey();
			double minDemand_pax = fromDemand.getValue() * (1.0 - this.rangeFactor);
			int coverage = this.od2coverage.getOrDefault(od, 0);
			double unservedDemand_pax = Math.max(0, minDemand_pax - coverage * planeCapacity_pax);
			missingPlaneTrips += (int) Math.ceil(unservedDemand_pax / planeCapacity_pax);
		}
		return missingPlaneTrips;
	}

	int computeNumberOfUnnecessaryPlanes(int planeCapacity_pax) {
		Set<Integer> unnecessaryPlanes = new LinkedHashSet<>(IntStream.range(0, this.fleetSize).boxed().toList());
		for (Map.Entry<Tuple<Node, Node>, Integer> coverageEntry : this.od2coverage.entrySet()) {
			var od = coverageEntry.getKey();
			int coverage = coverageEntry.getValue();
			double maxDemand_pax = this.od2demand.getOrDefault(od, 0.0) * (1.0 + this.rangeFactor);
			int neededCoverage = (int) Math.ceil(maxDemand_pax / planeCapacity_pax);
			if (coverage <= neededCoverage) {
				unnecessaryPlanes.removeAll(this.od2coveringPlaneIndices.getOrDefault(od, Collections.emptySet()));
			}
		}
		return unnecessaryPlanes.size();
	}

//	int computeNumberOfUnncessearyPlaneTrips(int planeCapacity) {
//		int unncessaryPlaneTrips = 0;
//		for (Map.Entry<Node, Map<Node, Integer>> fromCoverage : this.coverage.entrySet()) {
//			Node from = fromCoverage.getKey();
//			Map<Node, Double> fromDemand = this.demand.get(from);
//			for (Map.Entry<Node, Integer> toCoverage : fromCoverage.getValue().entrySet()) {
//				Node to = toCoverage.getKey();
//				int coverage = toCoverage.getValue();
//				if (fromDemand == null) {
//					unncessaryPlaneTrips += coverage;
//				} else {
//					double demand_pax = fromDemand.getOrDefault(to, 0.0);
//					int neededCoverage = (int) Math.ceil(demand_pax / planeCapacity);
//					unncessaryPlaneTrips += Math.max(0, coverage - neededCoverage);
//				}
//			}
//		}
//		return unncessaryPlaneTrips;
//	}

	@Override
	public void clear() {
		this.od2coverage = new LinkedHashMap<>();
		this.od2coveringPlaneIndices = new LinkedHashMap<>();
	}

	@Override
	public void update(int roundTripIndex, RoundTrip<NodeWithCoords> oldRoundTrip,
			RoundTrip<NodeWithCoords> newRoundTrip) {
		/*
		 * Compute effect of round trip change on coverage.
		 */
		{
			List<Episode> oldEpisodes = oldRoundTrip.getEpisodes();
			for (int i = 1; i < oldEpisodes.size(); i += 2) {
				MoveEpisode<Node> stay = (MoveEpisode<Node>) oldEpisodes.get(i);
				Node from = stay.getOrigin();
				Node to = stay.getDestination();
				this.setCoverage(from, to, this.getCoverage(from, to) - 1);
				var od = new Tuple<>(from, to);
				this.od2coveringPlaneIndices.computeIfAbsent(od, od2 -> new LinkedHashSet<>()).remove(roundTripIndex);
			}
		}
		{
			List<Episode> newEpisodes = newRoundTrip.getEpisodes();
			for (int i = 1; i < newEpisodes.size(); i += 2) {
				MoveEpisode<Node> stay = (MoveEpisode<Node>) newEpisodes.get(i);
				Node from = stay.getOrigin();
				Node to = stay.getDestination();
				this.setCoverage(from, to, this.getCoverage(from, to) + 1);
				var od = new Tuple<>(from, to);
				this.od2coveringPlaneIndices.computeIfAbsent(od, od2 -> new LinkedHashSet<>()).add(roundTripIndex);
			}
		}

		if (Math.random() < 0.01) {
			System.out.println("Missing plane trips          = " + this.computeNumberOfMissingPlaneTrips(18));
			System.out.println("Number of unnecessary planes = " + this.computeNumberOfUnnecessaryPlanes(18));
//			System.out.println("Unncessary plane trips = " + this.computeNumberOfUnncessearyPlaneTrips(18));
			System.out.println();
		}
	}

	@Override
	public PlaneUsageSummary clone() {
		PlaneUsageSummary child = new PlaneUsageSummary(this.fleetSize, this.rangeFactor);
		child.od2demand = this.od2demand;
		child.od2coverage = new LinkedHashMap<>(this.od2coverage);
		for (var entry : this.od2coveringPlaneIndices.entrySet()) {
			child.od2coveringPlaneIndices.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
		}

//		for (Map.Entry<Node, Map<Node, Double>> fromDemand : this.demand.entrySet()) {
//			child.demand.put(fromDemand.getKey(), new LinkedHashMap<>(fromDemand.getValue()));
//		}
//		for (Map.Entry<Node, Map<Node, Integer>> fromCoverage : this.coverage.entrySet()) {
//			child.coverage.put(fromCoverage.getKey(), new LinkedHashMap<>(fromCoverage.getValue()));
//		}
		return child;
	}
}
