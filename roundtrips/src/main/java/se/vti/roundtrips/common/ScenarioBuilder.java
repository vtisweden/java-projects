/**
 * se.vti.roundtrips.common
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
package se.vti.roundtrips.common;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;

import se.vti.roundtrips.simulator.Simulator;

/**
 * @author GunnarF
 */
public class ScenarioBuilder<N extends Node> {

	// -------------------- MEMBERS --------------------

	private Random random = new Random();
	private Integer numberOfTimeBins = null;
	private Double timeBinSize_h = null;
	private Integer upperBoundOnStayEpisodes = Integer.MAX_VALUE;

	private final Set<N> nodes = new LinkedHashSet<>();
	private BiFunction<N, N, Double> nodesToDistance_km = null;
	private BiFunction<N, N, Double> nodesToTime_h = null;

	private Simulator<N> simulator = null;

	// -------------------- CONSTRUCTION --------------------

	public ScenarioBuilder() {
	}

	public ScenarioBuilder<N> setRandom(Random random) {
		this.random = random;
		return this;
	}

	public ScenarioBuilder<N> setTimeBinSize_h(double timeBinSize_h) {
		this.timeBinSize_h = timeBinSize_h;
		return this;
	}

	public ScenarioBuilder<N> setNumberOfTimeBins(int numberOfTimeBins) {
		this.numberOfTimeBins = numberOfTimeBins;
		return this;
	}

	public ScenarioBuilder<N> setUpperBoundOnStayEpisodes(int upperBoundOnStayEpisodes) {
		this.upperBoundOnStayEpisodes = upperBoundOnStayEpisodes;
		return this;
	}

	public ScenarioBuilder<N> setSimulator(Simulator<N> simulator) {
		this.simulator = simulator;
		return this;
	}

	public ScenarioBuilder<N> addNode(N node) {
		this.nodes.add(node);
		return this;
	}

	public ScenarioBuilder<N> setMoveDistanceFunction(BiFunction<N, N, Double> nodesToDistance_km) {
		this.nodesToDistance_km = nodesToDistance_km;
		return this;
	}

	public ScenarioBuilder<N> setMoveTimeFunction(BiFunction<N, N, Double> nodesToTime_h) {
		this.nodesToTime_h = nodesToTime_h;
		return this;
	}

	// -------------------- BUILDING --------------------

	public Scenario<N> build() {
		var checker = new SpecificationChecker().defineError(() -> (this.random == null), "No Random instance defined")
				.defineError(() -> (this.numberOfTimeBins == null), "Undefined scenario variable: numberOfTimeBins")
				.defineError(() -> ((this.numberOfTimeBins != null) && (this.numberOfTimeBins < 1)),
						"numberOfTimeBins must be at least one")
				.defineError(() -> (this.timeBinSize_h == null), "Undefined scenario variable: timeBinSize_h")
				.defineError(() -> ((this.timeBinSize_h != null) && (this.timeBinSize_h < 1e-8)),
						"timeBinSize_h smaller than 1e-8")
				.defineError(() -> (this.nodes.size() == 0), "Scenario contains no nodes")
				.defineError(() -> (nodesToDistance_km == null), "No move distance function defined")
				.defineError(() -> (nodesToTime_h == null), "No move time function defined");
		if (checker.check()) {
			final Scenario<N> scenario;
			if (this.random != null) {
				scenario = new Scenario<>(this.random);
			} else {
				scenario = new Scenario<>();
			}
			scenario.setTimeBinSize_h(this.timeBinSize_h);
			scenario.setNumberOfTimeBins(this.numberOfTimeBins);
			scenario.setUpperBoundOnStayEpisodes(this.upperBoundOnStayEpisodes);
			if (this.simulator != null) {
				scenario.setSimulator(this.simulator);
			}
			for (N node : this.nodes) {
				scenario.addNode(node);
			}
			for (N from : this.nodes) {
				for (N to : this.nodes) {
					scenario.setDistance_km(from, to, this.nodesToDistance_km.apply(from, to));
					scenario.setTime_h(from, to, this.nodesToTime_h.apply(from, to));
				}
			}
			return scenario;
		} else {
			throw new RuntimeException(checker.getRecentErrors());
		}
	}
}
