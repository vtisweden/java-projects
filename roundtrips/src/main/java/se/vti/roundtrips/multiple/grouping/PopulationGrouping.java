/**
 * od2roundtrips.model
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.multiple.grouping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import se.vti.roundtrips.common.Node;

/**
 * 
 * @author GunnarF
 *
 */
public class PopulationGrouping {

	// -------------------- MEMBERS --------------------

	private final int populationSize;

	private final Map<String, Double> group2weights = new LinkedHashMap<>();

	private Map<String, int[]> group2indices = null;

	// -------------------- CONSTRUCTION --------------------

	public PopulationGrouping(int populationSize) {
		this.populationSize = populationSize;
	}

	public void addGroup(String name, double weight) {
		this.group2weights.put(name, weight);
	}

	// -------------------- INTERNALS --------------------

	/* package for testing */ void ensureIndexing() {
		if (this.group2indices != null) {
			return;
		}
		final double weightSum = this.group2weights.values().stream().mapToDouble(w -> w).sum();
		final Map<String, List<Integer>> group2indexList = this.group2weights.keySet().stream()
				.collect(Collectors.toMap(g -> g, g -> new ArrayList<>()));
		final Map<String, Double> group2slack = this.group2weights.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue() / weightSum * this.populationSize));
		for (int i = 0; i < this.populationSize; i++) {
			final String worstGroup = group2slack.entrySet().stream()
					.max((e1, e2) -> Double.compare(e1.getValue(), e2.getValue())).get().getKey();
			group2indexList.get(worstGroup).add(i);
			group2slack.compute(worstGroup, (g, s) -> s - 1.0);
		}
		this.group2indices = group2indexList.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().stream().mapToInt(i -> i).toArray()));
	}

	// -------------------- IMPLEMENTATION --------------------

	public Map<String, int[]> getGroup2Indices() {
		return this.group2indices;
	}

	public <L extends Node> PopulationGroupFilter<L> createFilter(String group) {
		this.ensureIndexing();
		return new PopulationGroupFilter<>(group, this.group2indices.get(group));
	}

}
