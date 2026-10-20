/**
 * se.vti.roundtrips.multiple.grouping
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
package se.vti.roundtrips.multi.grouping;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.multi.MultiRoundTripSummary;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.single.RoundTrip;

/**
 * @author GunnarF
 */
class TestPopulationGrouping {

	static int populationSize = 14;
	static int[] expectedGroupIndicesA = new int[] { 8, 11 };
	static int[] expectedGroupIndicesB = new int[] { 4, 6, 9, 12 };
	static int[] expectedGroupIndicesC = new int[] { 0, 1, 2, 3, 5, 7, 10, 13 };

	static PopulationGrouping createGrouping() {
		PopulationGrouping g = new PopulationGrouping(populationSize);
		g.addGroup("a", 1.0);
		g.addGroup("b", 2.0);
		g.addGroup("c", 4.0);
		g.ensureIndexing();
		return g;
	}

	@Test
	void testAllocationOfIndividualsToGroups() {
		PopulationGrouping g = createGrouping();

		assertArrayEquals(expectedGroupIndicesA, g.getGroup2Indices().get("a"));
		assertArrayEquals(expectedGroupIndicesB, g.getGroup2Indices().get("b"));
		assertArrayEquals(expectedGroupIndicesC, g.getGroup2Indices().get("c"));
	}

	@Test
	void testAllocationOfSummariesToIndividuals() {
		PopulationGrouping g = createGrouping();

		Map<MultiRoundTripSummary<Node>, Integer> summary2updateCount = new LinkedHashMap<>();
		Supplier<MultiRoundTripSummary<Node>> supplier = new Supplier<>() {

			@Override
			public MultiRoundTripSummary<Node> get() {
				return new MultiRoundTripSummary<Node>() {
					{
						summary2updateCount.put(this, 0);
					}

					@Override
					public void clear() {
					}

					@Override
					public void update(RoundTrip<Node> newRoundTrip, MultiRoundTrip<Node> oldRoundTrips) {
						summary2updateCount.compute(this, (s, c) -> c + 1);
					}

					@Override
					public MultiRoundTripSummary<Node> clone() {
						return null;
					}
				};
			}

		};

		ByPopulationGroupSummary<Node, MultiRoundTripSummary<Node>> byGroupSummary = new ByPopulationGroupSummary<Node, MultiRoundTripSummary<Node>>(
				g, supplier, Set.of("a", "b", "c")) {
			@Override
			public ByPopulationGroupSummary<Node, MultiRoundTripSummary<Node>> clone() {
				return null;
			}
		};

		for (int i = 0; i < populationSize; i++) {
			byGroupSummary.update(new RoundTrip<>(i, null, null), null);
		}

		Assertions.assertEquals(expectedGroupIndicesA.length,
				summary2updateCount.get(byGroupSummary.getSummaries().get("a")));
		Assertions.assertEquals(expectedGroupIndicesB.length,
				summary2updateCount.get(byGroupSummary.getSummaries().get("b")));
		Assertions.assertEquals(expectedGroupIndicesC.length,
				summary2updateCount.get(byGroupSummary.getSummaries().get("c")));
	}

}
