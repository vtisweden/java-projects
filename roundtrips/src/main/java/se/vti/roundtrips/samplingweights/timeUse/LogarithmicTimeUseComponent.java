/**
 * se.vti.roundtrips.samplingweights.misc.timeUse
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
package se.vti.roundtrips.samplingweights.timeUse;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.simulator.StayEpisode;
import se.vti.utils.misc.Tuple;

/**
 * Represents one <em>targetDuration * ln(realizedDuration)</em> term.
 * 
 * @author GunnarF
 *
 */
public class LogarithmicTimeUseComponent<N extends Node> {

	// -------------------- CONFIGURATION PARAMETERS --------------------

	final double targetDuration_h;
	final double period_h;
	final int[] roundTripIndices; // keeps track of roundtrips with these indices
	final Set<N> nodes = new LinkedHashSet<>(); // keeps track of stay episodes at these nodes

	private List<Tuple<Double, Double>> openInterval_h;
	private double minEnBlockDurationAtLeastOnce_h;
	private double minEnBlockDurationEachTime_h;

	// -------------------- COLLECTED TIME USE STATISTICS --------------------

	private boolean locked = false;

	private boolean reachedMinEnBlockDurationAtLeastOnce = false;
	private double effectiveDurationSum_h = 0;

	// -------------------- CONSTRUCTOIN --------------------

	// From outside this package, use factory in LogarithmicTimeUse.
	LogarithmicTimeUseComponent(double targetDuration_h, double period_h, int... roundTripIndices) {
		this.targetDuration_h = targetDuration_h;
		this.period_h = period_h;
		this.roundTripIndices = roundTripIndices;
		this.openInterval_h = Arrays.asList(new Tuple<>(0.0, period_h));
		this.minEnBlockDurationAtLeastOnce_h = 0.0;
		this.minEnBlockDurationEachTime_h = 0.0;
	}

	// -------------------- (PACKAGE) PRIVATE IMPLEMENTATION --------------------

	void reset() {
		this.reachedMinEnBlockDurationAtLeastOnce = false;
		this.effectiveDurationSum_h = 0;
	}

	void update(StayEpisode<N> stay) {
		double effectiveDuration_h = stay.overlap_h(this.openInterval_h, this.period_h);
		if (effectiveDuration_h >= this.minEnBlockDurationEachTime_h) {
			this.effectiveDurationSum_h += effectiveDuration_h;
			this.reachedMinEnBlockDurationAtLeastOnce = this.reachedMinEnBlockDurationAtLeastOnce
					|| (effectiveDuration_h >= this.minEnBlockDurationAtLeastOnce_h);
		}
	}

	double getEffectiveDuration_h() {
		return (this.reachedMinEnBlockDurationAtLeastOnce ? this.effectiveDurationSum_h : 0.0);
	}

	boolean isLocked() {
		return this.locked;
	}

	void lock() {
		this.locked = true;
	}

	private void checkLocked() {
		if (this.locked) {
			throw new RuntimeException("Component is locked.");
		}
	}

	// -------------------- PUBLIC IMPLEMENTATION --------------------

	public LogarithmicTimeUseComponent<N> setOpeningTimes_h(double start_h, double end_h) {
		assert (start_h >= 0);
		assert (end_h >= 0);
		assert (start_h <= this.period_h);
		assert (end_h <= this.period_h);
		this.checkLocked();
		if (start_h < end_h) {
			this.openInterval_h = Arrays.asList(new Tuple<>(start_h, end_h));
		} else {
			// wraparound
			this.openInterval_h = Arrays.asList(new Tuple<>(0.0, end_h), new Tuple<>(start_h, this.period_h));
		}
		return this;
	}

	public LogarithmicTimeUseComponent<N> setMinEnBlockDurationAtLeastOnce_h(double dur_h) {
		assert (dur_h >= 0);
		assert (dur_h <= this.period_h);
		this.checkLocked();
		this.minEnBlockDurationAtLeastOnce_h = dur_h;
		return this;
	}

	public LogarithmicTimeUseComponent<N> setMinEnBlockDurationEachTime_h(double dur_h) {
		assert (dur_h >= 0);
		assert (dur_h <= this.period_h);
		this.checkLocked();
		this.minEnBlockDurationEachTime_h = dur_h;
		return this;
	}

	public LogarithmicTimeUseComponent<N> addObservedNode(N node) {
		this.checkLocked();
		this.nodes.add(node);
		return this;
	}

	public LogarithmicTimeUseComponent<N> addObservedNodes(Scenario<N> scenario, Function<N, Boolean> test) {
		for (N node : scenario.getNodesView()) {
			if (test.apply(node)) {
				this.addObservedNode(node);
			}
		}
		return this;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": targetDuration_h=" + this.targetDuration_h + ", period_h="
				+ this.period_h + ", roundTripIndices=" + Arrays.toString(this.roundTripIndices) + ", nodes="
				+ this.nodes + ", openInterval_h=" + this.openInterval_h + ", minEnBlockDurationAtLeastOnce_h="
				+ this.minEnBlockDurationAtLeastOnce_h + ", minEnBlockDurationEachTime_h="
				+ this.minEnBlockDurationEachTime_h;
	}
}
