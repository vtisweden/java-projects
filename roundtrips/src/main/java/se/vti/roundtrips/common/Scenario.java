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
package se.vti.roundtrips.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.simulator.DefaultSimulator;
import se.vti.roundtrips.simulator.Simulator;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class Scenario<N extends Node> {

	// -------------------- CONSTANTS --------------------

	private final Random rnd = new Random();

	// -------------------- PARAMETER MEMBERS --------------------

	private Integer timeBinCnt = null;

	private Double binSize_h = null;

	private int upperBoundOnStayEpisodes = Integer.MAX_VALUE;

	// -------------------- NETWORK MEMBERS --------------------

	private Simulator<N> simulator = null;

	private final Map<String, N> name2node = new LinkedHashMap<>();

	private final Map<Tuple<N, N>, Double> od2distance_km = new LinkedHashMap<>();

	private final Map<Tuple<N, N>, Double> od2time_h = new LinkedHashMap<>();

	private List<N> locationsView = Collections.unmodifiableList(new ArrayList<>(0));

	// -------------------- CONSTRUCTION --------------------

	public Scenario() {
	}

	// -------------------- PARAMETER SETTER/GETTER --------------------

	public Double getBinSize_h() {
		return this.binSize_h;
	}

	public void setTimeBinSize_h(double binSize_h) {
		this.binSize_h = binSize_h;
	}

	public Integer getTimeBinCnt() {
		return this.timeBinCnt;
	}

	public void setTimeBinCnt(int timeBinCnt) {
		this.timeBinCnt = timeBinCnt;
	}

	public Double getPeriodLength_h() {
		if (this.binSize_h != null && this.timeBinCnt != null) {
			return this.binSize_h * this.timeBinCnt;
		} else {
			return null;
		}
	}

	public void setUpperBoundOnStayEpisodes(int upperBoundOnStayEpisodes) {
		this.upperBoundOnStayEpisodes = upperBoundOnStayEpisodes;
	}

	public int getUpperBoundOnStayEpisodes() {
		return this.upperBoundOnStayEpisodes;
	}

	public int getMaxPossibleStayEpisodes() {
		return Math.min(this.timeBinCnt, this.upperBoundOnStayEpisodes);
	}

	// -------------------- NETWORK SETTER/GETTER --------------------

	public void setSimulator(Simulator<N> simulator) {
		this.simulator = simulator;
	}

	public Simulator<N> getOrCreateSimulator() {
		if (this.simulator == null) {
			this.simulator = new DefaultSimulator<>(this);
		}
		return this.simulator;
	}

	public N addNode(N node) {
		this.name2node.put(node.getName(), node);
		this.locationsView = Collections.unmodifiableList(new ArrayList<>(this.name2node.values()));
		return node;
	}

	public N getNode(String name) {
		return this.name2node.get(name);
	}

	public Random getRandom() {
		return this.rnd;
	}

	public List<N> getNodesView() {
		return this.locationsView;
	}

	public int getNodesCnt() {
		return this.name2node.size();
	}

	public void setDistance_km(N from, N to, double dist_km) {
		Tuple<N, N> od = new Tuple<>(from, to);
		this.od2distance_km.put(od, dist_km);
	}

	public void setSymmetricDistance_km(N loc1, N loc2, double dist_km) {
		this.setDistance_km(loc1, loc2, dist_km);
		this.setDistance_km(loc2, loc1, dist_km);
	}

	public Double getDistance_km(N from, N to) {
		return this.od2distance_km.get(new Tuple<>(from, to));
	}

	public void setTime_h(N from, N to, double time_h) {
		this.od2time_h.put(new Tuple<>(from, to), time_h);
	}

	// TODO Once tested, allow string args in all time/distance setters.
	public void setTime_h(String fromName, String toName, double time_h) {
		N from = this.getNode(fromName);
		N to = this.getNode(toName);
		this.od2time_h.put(new Tuple<>(from, to), time_h);
	}

	public void setSymmetricTime_h(N loc1, N loc2, double time_h) {
		this.setTime_h(loc1, loc2, time_h);
		this.setTime_h(loc2, loc1, time_h);
	}

	public Double getTime_h(N from, N to) {
		return this.od2time_h.get(new Tuple<>(from, to));
	}

	public Map<Tuple<N, N>, Double> getOD2Time_h() {
		return this.od2time_h;
	}

	// -------------------- UTILITIES --------------------

	private RoundTrip<N> createInitialRoundTrip(int index, N node, Integer departure) {
		if (node == null) {
			node = this.locationsView.get(this.getRandom().nextInt(this.locationsView.size()));
		}
		if (departure == null) {
			departure = this.getRandom().nextInt(this.timeBinCnt);
		}
		var result = new RoundTrip<N>(index, new ArrayList<>(Arrays.asList(node)),
				new ArrayList<>(Arrays.asList(departure)));
		result.setEpisodes(this.getOrCreateSimulator().simulate(result));
		return result;
	}

	public RoundTrip<N> createInitialRoundTrip(N node, Integer departure) {
		return this.createInitialRoundTrip(0, node, departure);
	}

	public void populateInitialMultiRoundTrip(MultiRoundTrip<N> result, List<N> nodes, List<Integer> departures) {
		for (int i = 0; i < result.size(); i++) {
			N node = nodes.get(this.rnd.nextInt(nodes.size()));
			Integer departure = departures.get(this.rnd.nextInt(departures.size()));
			result.setRoundTripAndUpdateSummaries(i, this.createInitialRoundTrip(i, node, departure));
		}
	}

	public void populateInitialMultiRoundTrip(MultiRoundTrip<N> result, N node, Integer departure) {
		this.populateInitialMultiRoundTrip(result, Arrays.asList(node), Arrays.asList(departure));
	}

	public MultiRoundTrip<N> createInitialMultiRoundTrip(N node, Integer departure, int numberOfRoundTrips) {
		MultiRoundTrip<N> result = new MultiRoundTrip<>(numberOfRoundTrips);
		this.populateInitialMultiRoundTrip(result, node, departure);
		return result;
	}

	public MultiRoundTrip<N> createInitialMultiRoundTrip(List<N> nodes, List<Integer> departures,
			int numberOfRoundTrips) {
		MultiRoundTrip<N> result = new MultiRoundTrip<>(numberOfRoundTrips);
		this.populateInitialMultiRoundTrip(result, nodes, departures);
		return result;
	}
}
