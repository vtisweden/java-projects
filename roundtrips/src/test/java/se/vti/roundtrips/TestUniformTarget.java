/**
 * se.vti.roundtrips
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
package se.vti.roundtrips;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.RandomRoundTripGenerator;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.roundtrips.samplingweights.priors.SingleRoundTripUniformPrior;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;
import se.vti.utils.misc.metropolishastings.MHWeightContainer;

/**
 * @author GunnarF
 */
class TestUniformTarget {

	@Test
	void test() {

		String[] locations = { "A", "B", "C" };
		int timeBinCnt = 3;
		int populationSize = 3;

		var scenario = new Scenario<Node>(new Random(4711));
		scenario.setTimeBinCnt(timeBinCnt);
		scenario.setTimeBinSize_h(1.0);

		for (String location : locations) {
			scenario.addNode(new Node(location));
		}

		for (Node from : scenario.getNodesView()) {
			for (Node to : scenario.getNodesView()) {
				scenario.setTime_h(from, to, from != to ? 1.0 : 0.0);
			}
		}

		var initialRoundTrips = new MultiRoundTrip<Node>(populationSize);
		var generator = new RandomRoundTripGenerator<Node>(scenario);
		generator.populateRandomly(initialRoundTrips);

		var weights = new MHWeightContainer<MultiRoundTrip<Node>>();
		weights.add(new SingleToMultiWeight<Node>(new SingleRoundTripUniformPrior<Node>(scenario)));

		var algo = new MHAlgorithm<MultiRoundTrip<Node>>(new MultiRoundTripProposal<Node>(scenario), weights, scenario.getRandom());
		algo.setInitialState(initialRoundTrips);

		algo.addStateProcessor(new MHStateProcessor<MultiRoundTrip<Node>>() {

			long it = 0;
			long[] sizeCounts = new long[timeBinCnt + 1];

			@Override
			public void start() {
			}

			@Override
			public void processState(MultiRoundTrip<Node> roundTrips) {
				if (this.it++ % 10 == 0) {
					for (var roundTrip : roundTrips) {
						this.sizeCounts[roundTrip.size()]++;
					}
				}
			}

			@Override
			public void end() {
				System.out.println(Arrays.toString(this.sizeCounts));
				assertArrayEquals(new long[]{0, 99881, 100264, 99858}, this.sizeCounts);
			}

		});

		algo.run(1_000_000);

		
	}

}
