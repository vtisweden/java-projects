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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.multiple.MultiRoundTripSummary;
import se.vti.roundtrips.simulator.MoveEpisode;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.Tuple;

/**
 * @author GunnarF
 */
public class PlaneUsageSummary implements MultiRoundTripSummary<NodeWithCoords> {

	private final Scenario<NodeWithCoords> scenario;
	private final int numberOfPlanes;
	private final int planeCapacity_pax;
	private final List<Tuple<Double, Double>> noPassengerTimes;
	private final Map<Tuple<NodeWithCoords, NodeWithCoords>, Double> od2demand;

	private List<Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer>> od2ValidCoveragePerPlane;
	private Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer> od2ValidCoverageSum;

	private List<Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer>> od2InvalidCoveragePerPlane;
	private Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer> od2InvalidCoverageSum;

	// TODO encapsulate
	Double servedDemand_paxKm;
	Double emptySeats_npaxKm;
	double totalDemand_paxKm;
	Map<NodeWithCoords, Double> node2servedDemand_pax;

	public PlaneUsageSummary(Scenario<NodeWithCoords> scenario, int numberOfPlanes, int planeCapacity_pax,
			double earliestTravel_h, double latestTravel_h) {
		this.scenario = scenario;
		this.numberOfPlanes = numberOfPlanes;
		this.planeCapacity_pax = planeCapacity_pax;
		this.noPassengerTimes = List.of(new Tuple<>(0.0, earliestTravel_h), new Tuple<>(latestTravel_h, 24.0));
		this.od2demand = new LinkedHashMap<>();
		this.clear();
	}

	@Override
	public void clear() {
		this.od2ValidCoveragePerPlane = new ArrayList<>(this.numberOfPlanes);
		this.od2InvalidCoveragePerPlane = new ArrayList<>(this.numberOfPlanes);
		for (int i = 0; i < this.numberOfPlanes; i++) {
			this.od2ValidCoveragePerPlane.add(new LinkedHashMap<>());
			this.od2InvalidCoveragePerPlane.add(new LinkedHashMap<>());
		}
		this.od2ValidCoverageSum = new LinkedHashMap<>();
		this.od2InvalidCoverageSum = new LinkedHashMap<>();
	}

	public void setDemand(NodeWithCoords from, NodeWithCoords to, double demand_pax) {
		var od = new Tuple<>(from, to);
		double dist_km = this.scenario.getDistance_km(from, to);
		this.totalDemand_paxKm += dist_km * (demand_pax - this.od2demand.getOrDefault(od, 0.0));
		this.od2demand.put(od, demand_pax);
	}

	private <K> void add(Map<K, Integer> addToThis, Map<K, Integer> addend, int factor) {
		for (var entry : addend.entrySet()) {
			addToThis.compute(entry.getKey(), (k, v) -> (v == null ? 0 : v) + factor * entry.getValue());
		}
	}

	private <K> void inc(Map<K, Integer> incThis, K key) {
		incThis.compute(key, (k, c) -> (c == null) ? 1 : (c + 1));
	}

	@Override
	public void update(int planeIndex, RoundTrip<NodeWithCoords> oldRoundTrip, RoundTrip<NodeWithCoords> newRoundTrip) {

		if (oldRoundTrip != null) {
			this.add(this.od2ValidCoverageSum, this.od2ValidCoveragePerPlane.get(planeIndex), -1);
			this.od2ValidCoveragePerPlane.set(planeIndex, new LinkedHashMap<>());

			this.add(this.od2InvalidCoverageSum, this.od2InvalidCoveragePerPlane.get(planeIndex), -1);
			this.od2InvalidCoveragePerPlane.set(planeIndex, new LinkedHashMap<>());
		}

		if (newRoundTrip != null) {
			var od2ValidCoverage = this.od2ValidCoveragePerPlane.get(planeIndex);
			var od2InvalidCoverage = this.od2InvalidCoveragePerPlane.get(planeIndex);
			var newEpisodes = newRoundTrip.getEpisodes();
			for (int i = 1; i < newEpisodes.size(); i += 2) {
				var move = (MoveEpisode<NodeWithCoords>) newEpisodes.get(i);
				if (move.getOrigin() != move.getDestination()) {
					var od = new Tuple<>(move.getOrigin(), move.getDestination());
					if (move.overlap_h(this.noPassengerTimes, 24.0) < 1e-8) {
						this.inc(od2ValidCoverage, od);
					} else {
						this.inc(od2InvalidCoverage, od);
					}
				}
			}
			this.add(this.od2ValidCoverageSum, od2ValidCoverage, +1);
			this.add(this.od2InvalidCoverageSum, od2InvalidCoverage, +1);
		}

		this.servedDemand_paxKm = 0.0;
		this.emptySeats_npaxKm = 0.0;
		this.node2servedDemand_pax = new LinkedHashMap<>();
		for (var odAndDemand : this.od2demand.entrySet()) {
			var od = odAndDemand.getKey();
			double demand_pax = odAndDemand.getValue();
			double dist_km = this.scenario.getDistance_km(od.getA(), od.getB());
			int validCoverage = this.od2ValidCoverageSum.getOrDefault(odAndDemand.getKey(), 0);
			double pax = Math.min(this.planeCapacity_pax * validCoverage, demand_pax);
			this.servedDemand_paxKm += pax * dist_km;
			this.emptySeats_npaxKm += (validCoverage * this.planeCapacity_pax - pax) * dist_km;
			int invalidCoverage = this.od2InvalidCoverageSum.getOrDefault(odAndDemand.getKey(), 0);
			this.emptySeats_npaxKm += invalidCoverage * this.planeCapacity_pax * dist_km;
			this.node2servedDemand_pax.compute(od.getA(), (n, d) -> (d == null) ? pax * dist_km : (d + pax * dist_km));
			this.node2servedDemand_pax.compute(od.getB(), (n, d) -> (d == null) ? pax * dist_km : (d + pax * dist_km));
		}
	}

	// for cloning
	private PlaneUsageSummary(Scenario<NodeWithCoords> scenario, int numberOfPlanes, int planeCapacity_pax,
			List<Tuple<Double, Double>> noPassengerTimes, Map<Tuple<NodeWithCoords, NodeWithCoords>, Double> od2demand,
			List<Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer>> listOfOd2ValidCoveragePerPlane,
			List<Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer>> listOfOd2InvalidCoveragePerPlane,
			Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer> od2ValidCoverageSum,
			Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer> od2InvalidCoverageSum) {
		this.scenario = scenario;
		this.numberOfPlanes = numberOfPlanes;
		this.planeCapacity_pax = planeCapacity_pax;
		this.noPassengerTimes = noPassengerTimes;

		this.od2demand = od2demand;
		this.od2ValidCoveragePerPlane = new ArrayList<>(numberOfPlanes);
		for (var od2ValidCoverage : listOfOd2ValidCoveragePerPlane) {
			this.od2ValidCoveragePerPlane.add(new LinkedHashMap<>(od2ValidCoverage));
		}
		this.od2InvalidCoveragePerPlane = new ArrayList<>(numberOfPlanes);
		for (var od2InvalidCoverage : listOfOd2InvalidCoveragePerPlane) {
			this.od2InvalidCoveragePerPlane.add(new LinkedHashMap<>(od2InvalidCoverage));
		}

		this.od2ValidCoverageSum = new LinkedHashMap<>(od2ValidCoverageSum);
		this.od2InvalidCoverageSum = new LinkedHashMap<>(od2InvalidCoverageSum);
	}

	@Override
	public PlaneUsageSummary clone() {
		var child = new PlaneUsageSummary(this.scenario, this.numberOfPlanes, this.planeCapacity_pax,
				this.noPassengerTimes, this.od2demand, this.od2ValidCoveragePerPlane, this.od2InvalidCoveragePerPlane,
				this.od2ValidCoverageSum, this.od2InvalidCoverageSum);
		child.servedDemand_paxKm = this.servedDemand_paxKm;
		child.emptySeats_npaxKm = this.servedDemand_paxKm;
		child.totalDemand_paxKm = this.totalDemand_paxKm;
		child.node2servedDemand_pax = new LinkedHashMap<>(this.node2servedDemand_pax);
		return child;
	}
}
