/**
 * se.vti.roundtrips.samplingweights.misc
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.simulator.Episode;
import se.vti.roundtrips.simulator.StayEpisode;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
class LogarithmicTimeUse<N extends Node> {

	private final double minDur_h = 0.001;

	private final double period_h;

	// Depending on the subclass, index may refer to a person or a weekday.
	private final List<Map<N, LinkedHashSet<LogarithmicTimeUseComponent<N>>>> node2ComponentsOverIndices;

	private final Set<LogarithmicTimeUseComponent<N>> components = new LinkedHashSet<>();

	// -------------------- PACKAGE PRIVATE IMPLEMENTATION --------------------

	LogarithmicTimeUse(double period_h, int numberOfIndices) {
		this.period_h = period_h;
		this.node2ComponentsOverIndices = new ArrayList<>(numberOfIndices);
		for (int i = 0; i < numberOfIndices; i++) {
			this.node2ComponentsOverIndices.add(new LinkedHashMap<>());
		}
	}

	LogarithmicTimeUseComponent<N> createComponent(double targetDuration_h, int... roundTripIndices) {
		if (roundTripIndices.length == 0) {
			throw new RuntimeException("Pass at least one index.");
		}
		return new LogarithmicTimeUseComponent<>(targetDuration_h, this.period_h, roundTripIndices);
	}

	double computeLogWeight(Iterable<RoundTrip<N>> roundTrips) {
		for (var component : this.components) {
			component.reset();
		}
		for (RoundTrip<N> roundTrip : roundTrips) {
			var node2Components = this.node2ComponentsOverIndices.get(roundTrip.getIndex());
			List<Episode> episodes = roundTrip.getEpisodes();
			for (int i = 0; i < episodes.size(); i += 2) {
				StayEpisode<N> stay = (StayEpisode<N>) episodes.get(i);
				for (var component : node2Components.get(stay.getLocation())) {
					component.update(stay);
				}
			}
		}
		double result = 0.0;
		for (var component : this.components) {
			result += component.targetDuration_h
					* Math.log(Math.max(this.minDur_h, component.getEffectiveDuration_h()));
		}
		return result;
	}

	// -------------------- PUBLIC IMPLEMENTATION --------------------

	public void addConfiguredComponent(LogarithmicTimeUseComponent<N> component) {
		if (component.isLocked()) {
			throw new RuntimeException("Component is locked.");
		}
		for (int roundTripIndex : component.roundTripIndices) {
			var node2Components = this.node2ComponentsOverIndices.get(roundTripIndex);
			for (N node : component.nodes) {
				node2Components.computeIfAbsent(node, n -> new LinkedHashSet<>()).add(component);
			}
		}
		this.components.add(component);
		component.lock();
	}
}
