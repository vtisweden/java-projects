/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023-2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.examples.multimodal;

import java.util.Arrays;
import java.util.Random;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.RoundTrips;
import se.vti.roundtrips.common.Runner;
import se.vti.roundtrips.common.ScenarioBuilder;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.coldhot.ColdHotMultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * @author GunnarF
 */
public class MultimodalExample {

	static long[] run(double expectedColdProposalLength, double expectedHotProposalLength,
			double coldHotRecombineProba, int iterations) {

		// Build the scenario.

		var scenarioBuilder = new ScenarioBuilder<Node>().setRandom(new Random(4711)).setNumberOfTimeBins(24)
				.setTimeBinSize_h(1.0);

		for (int i = 1; i <= 10; i++) {
			scenarioBuilder.addNode(new Node("" + i));
		}

		scenarioBuilder.setMoveDistanceFunction((a, b) -> (a == b ? 0.0 : 1.0));
		scenarioBuilder.setMoveTimeFunction((a, b) -> (a == b ? 0.0 : 1.0));

		var scenario = scenarioBuilder.build();

		// Build the runner.

		var runner = new Runner<Node>(scenario).setExpectedColdProposalLength(expectedColdProposalLength)
				.setColdHotParameters(expectedHotProposalLength, coldHotRecombineProba);

		runner.setIndividualUniformPrior().addIndividualWeight(new MHWeight<RoundTrip<Node>>() {
			@Override
			public double logWeight(RoundTrip<Node> roundTrip) {
				final double penalty = Double.NEGATIVE_INFINITY;
				
				int size = roundTrip.size();

				if (size <= 0) {
					return penalty;
				}

				int result = 0;
				while (size > 1) {
					if (size % 2 != 0) {
						return penalty;
					}
					size = size / 2;
					result++;
				}
				return result;
			}
		});
		
		long[] cnts = new long[1 + 24];
		runner.addStateProcessor(new MHStateProcessor<RoundTrips<Node>>() {

			@Override
			public void start() {
			}

			@Override
			public void processState(RoundTrips<Node> state, double logWeight) {
				cnts[state.getRoundTrip(0).size()]++;
			}

			@Override
			public void end() {
			}
			
		});
		
		var initialColdMultiRoundTrip = new MultiRoundTrip<Node>(1);
		initialColdMultiRoundTrip.setRoundTripAndUpdateSummaries(0, new RoundTrip<Node>(0, Arrays.asList(), Arrays.asList()));

		var initialHotMultiRoundTrip = new MultiRoundTrip<Node>(1);
		initialHotMultiRoundTrip.setRoundTripAndUpdateSummaries(0, new RoundTrip<Node>(0, Arrays.asList(), Arrays.asList()));

		runner.setInitialState(new ColdHotMultiRoundTrip<Node>(initialColdMultiRoundTrip, initialHotMultiRoundTrip));
		runner.setMessageInterval(1000).setNumberOfIterations(iterations);
		
		runner.run();
		
		System.out.println("size\tln(count[size])-ln(count[1])");
		for (int i = 1; i <= 24; i++) {
			System.out.print(i + "\t");
			if (cnts[i] == 0) {
				System.out.println();
			} else {
				System.out.println(Math.log(cnts[i]) - Math.log(cnts[1]));
			}
		}
		
		return cnts;
	}

	public static void main(String[] args) {

		run(1.0, 10.0, 0.1, 1_000_000);
		run(10.0, 10.0, 0.0, 1_000_000);
	}

}
