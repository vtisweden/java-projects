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

import java.util.Random;

import org.checkerframework.checker.units.qual.N;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.runner.AbstractStateAdapter;
import se.vti.roundtrips.runner.MultiRoundTripWrapper;
import se.vti.roundtrips.runner.Runner;
import se.vti.roundtrips.runner.basic.BasicStateAdapter;
import se.vti.roundtrips.runner.coldhot.ColdHotMultiRoundTrip;
import se.vti.roundtrips.runner.coldhot.ColdHotStateAdapter;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.scenario.ScenarioBuilder;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripUtils;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * Example to demonstrate how multi-step proposals and cold/hot tempering can
 * explore a multi-modal probability distribution over the state space.
 * 
 * @author GunnarF
 */
public class MultimodalExample {

	static long[] run(double expectedColdProposalLength, double expectedHotProposalLength, double coldHotRecombineProba,
			int iterations) {

		/*
		 * Build a scenario with 10 nodes.
		 */

		var scenarioBuilder = new ScenarioBuilder<Node>().setRandom(new Random(4711)).setNumberOfTimeBins(24)
				.setTimeBinSize_h(1.0);
		for (int i = 1; i <= 10; i++) {
			scenarioBuilder.addNode(new Node("" + i));
		}
		scenarioBuilder.setMoveDistanceFunction((a, b) -> (a == b ? 0.0 : 1.0));
		scenarioBuilder.setMoveTimeFunction((a, b) -> (a == b ? 0.0 : 1.0));
		var scenario = scenarioBuilder.build();

		/*
		 * Build the state adapter.
		 */
		AbstractStateAdapter<Node, ? extends MultiRoundTripWrapper<Node>> stateAdapter;
		if (coldHotRecombineProba == 0.0) {
			stateAdapter = BasicStateAdapter.createDefaultInstance(scenario, expectedColdProposalLength);
		} else {
			var coldHotStateAdapter = ColdHotStateAdapter.createDefaultInstance(scenario, 0.1, expectedColdProposalLength,
					expectedHotProposalLength, coldHotRecombineProba);
//			coldHotStateAdapter.addNativeStateProcessor(new MHStateProcessor<ColdHotMultiRoundTrip<Node>>() {
//				@Override
//				public void processState(ColdHotMultiRoundTrip<Node> state, double logWeight) {
//					int size = state.getHot().getRoundTrip(0).size();
//					if (true || size > 2) {
//						System.out.println(size);
//					}
//				}
//			});
			stateAdapter = coldHotStateAdapter;
		}

		/*
		 * Build the runner with configurable multi-step proposals and cold/hot
		 * tempering.
		 */

		var runner = new Runner<Node>(scenario, stateAdapter);
//				.setExpectedColdProposalLength(expectedColdProposalLength)
//				.setColdHotParameters(expectedHotProposalLength, coldHotRecombineProba, 0.1);

		// This weight forbids round trips with a number of stages that is not a power
		// of two. Single-step proposals can hence not explore the entire state space.
		runner.setIndividualUniformPrior().addIndividualWeight(new MHWeight<RoundTrip<Node>>() {
			@Override
			public String name() {
				return "PowerOf2Sizes";
			}
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
		runner.addStateProcessor(new MHStateProcessor<MultiRoundTrip<Node>>() {
			@Override
			public void processState(MultiRoundTrip<Node> state, double logWeight) {
				cnts[state.getRoundTrip(0).size()]++;
			}
		});

		runner.setInitialState(RoundTripUtils.singleton().createEmptyMultiRoundTrip(1)).setMessageInterval(1000)
				.setNumberOfIterations(iterations);

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
//		run(1.0, 8.0, 0.1, 1_000_000);
//		run(1.0, 10.0, 0.0, 1_000_000);
		run(8.0, 10.0, 0.0, 1_000_000);
	}

}
