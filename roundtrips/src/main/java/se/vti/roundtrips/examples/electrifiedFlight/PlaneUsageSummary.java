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
	private final Map<Tuple<NodeWithCoords, NodeWithCoords>, Double> od2demand;
	
	private List<Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer>> od2CoveragePerPlane;
	private Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer> od2CoverageSum;

	// TODO encapsulate
	Double servedDemand_paxKm;
	Double emptySeats_npaxKm;
	double totalDemand_paxKm;
	Map<NodeWithCoords, Double> node2servedDemand_pax;

	public PlaneUsageSummary(Scenario<NodeWithCoords> scenario, int numberOfPlanes, int planeCapacity_pax) {
		this.scenario = scenario;
		this.numberOfPlanes = numberOfPlanes;
		this.planeCapacity_pax = planeCapacity_pax;
		this.od2demand = new LinkedHashMap<>();
		this.clear();
	}

	@Override
	public void clear() {
		this.od2CoveragePerPlane = new ArrayList<>(this.numberOfPlanes);
		for (int i = 0; i < this.numberOfPlanes; i++) {
			this.od2CoveragePerPlane.add(new LinkedHashMap<>());
		}
		this.od2CoverageSum = new LinkedHashMap<>();
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
			this.add(this.od2CoverageSum, this.od2CoveragePerPlane.get(planeIndex), -1);
			this.od2CoveragePerPlane.set(planeIndex, new LinkedHashMap<>());
		}

		if (newRoundTrip != null) {
			var od2Coverage = this.od2CoveragePerPlane.get(planeIndex);
			var newEpisodes = newRoundTrip.getEpisodes();
			for (int i = 1; i < newEpisodes.size(); i += 2) {
				var move = (MoveEpisode<NodeWithCoords>) newEpisodes.get(i);
				if (move.getOrigin() != move.getDestination()) {
					this.inc(od2Coverage, new Tuple<>(move.getOrigin(), move.getDestination()));
				}
			}
			this.add(this.od2CoverageSum, od2Coverage, +1);
		}

		this.servedDemand_paxKm = 0.0;
		this.emptySeats_npaxKm = 0.0;
		this.node2servedDemand_pax = new LinkedHashMap<>();
		for (var odAndDemand : this.od2demand.entrySet()) {
			var od = odAndDemand.getKey();
			double demand_pax = odAndDemand.getValue();
			double dist_km = this.scenario.getDistance_km(od.getA(), od.getB());
			int validCoverage = this.od2CoverageSum.getOrDefault(odAndDemand.getKey(), 0);
			double pax = Math.min(this.planeCapacity_pax * validCoverage, demand_pax);
			this.servedDemand_paxKm += pax * dist_km;
			this.emptySeats_npaxKm += (validCoverage * this.planeCapacity_pax - pax) * dist_km;
			this.node2servedDemand_pax.compute(od.getA(), (n, d) -> (d == null) ? pax * dist_km : (d + pax * dist_km));
			this.node2servedDemand_pax.compute(od.getB(), (n, d) -> (d == null) ? pax * dist_km : (d + pax * dist_km));
		}
	}

	// for cloning
	private PlaneUsageSummary(Scenario<NodeWithCoords> scenario, int numberOfPlanes, int planeCapacity_pax,
			Map<Tuple<NodeWithCoords, NodeWithCoords>, Double> od2demand,
			List<Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer>> listOfOd2ValidCoveragePerPlane,
			Map<Tuple<NodeWithCoords, NodeWithCoords>, Integer> od2ValidCoverageSum) {
		this.scenario = scenario;
		this.numberOfPlanes = numberOfPlanes;
		this.planeCapacity_pax = planeCapacity_pax;

		this.od2demand = od2demand;
		this.od2CoveragePerPlane = new ArrayList<>(numberOfPlanes);
		for (var od2ValidCoverage : listOfOd2ValidCoveragePerPlane) {
			this.od2CoveragePerPlane.add(new LinkedHashMap<>(od2ValidCoverage));
		}
		this.od2CoverageSum = new LinkedHashMap<>(od2ValidCoverageSum);
	}

	@Override
	public PlaneUsageSummary clone() {
		var child = new PlaneUsageSummary(this.scenario, this.numberOfPlanes, this.planeCapacity_pax, this.od2demand,
				this.od2CoveragePerPlane, this.od2CoverageSum);
		child.servedDemand_paxKm = this.servedDemand_paxKm;
		child.emptySeats_npaxKm = this.servedDemand_paxKm;
		child.totalDemand_paxKm = this.totalDemand_paxKm;
		child.node2servedDemand_pax = new LinkedHashMap<>(this.node2servedDemand_pax);
		return child;
	}
}
