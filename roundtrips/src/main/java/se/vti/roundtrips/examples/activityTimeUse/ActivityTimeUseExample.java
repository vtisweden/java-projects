/**
 * se.vti.roundtrips.examples.truckServiceCoverage
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
package se.vti.roundtrips.examples.activityTimeUse;

import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.Activity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.GridNodeWithActivity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.StrictlyEnforceUniqueHomeLocation;
import se.vti.roundtrips.samplingweights.misc.StrictlyPeriodicSchedule;
import se.vti.roundtrips.samplingweights.misc.timeUse.LogarithmicTimeUseSinglePersonSingleDay;
import se.vti.roundtrips.samplingweights.priors.SingleRoundTripUniformPrior;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripProposal;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHWeightContainer;
import se.vti.utils.misc.metropolishastings.MHWeightsToFileLogger;

/**
 * 
 * @author GunnarF
 *
 */
class ActivityTimeUseExample {

	private final Long seed;

	ActivityTimeUseExample(long seed) {
		this.seed = seed;
	}

	void run(long totalIterations) {

		/*
		 * Sample round trips including activities according to time use assumptions.
		 * Possible use case: sampling of alternatives for choice model estimation.
		 * 
		 * The considered system is a grid network that consists of gridSize x gridSize
		 * nodes. All roads between nodes have the same length and traversal time.
		 * Different activities are possible at different nodes; this is encoded by
		 * activity-specific node duplication (network expansion).
		 * 
		 * The traveler moves in daily round trips. Within-day time is discretized into
		 * 15-minue time bins.
		 */

		int gridSize = 5;
		double edgeLength_km = 1;
		double edgeTime_h = 0.1;

		var scenario = new Scenario<GridNodeWithActivity>();
		scenario.getRandom().setSeed(this.seed);
		scenario.setTimeBinSize_h(1.0 / 4);
		scenario.setTimeBinCnt(4 * 24);

		// A single corner node allows for "home" activities (could be a suburb).
		GridNodeWithActivity home = new GridNodeWithActivity(0, 0, Activity.HOME);
		scenario.addNode(home);

		// Only the center nodes allow for "work" activities (could be CBD).
		for (int row = 1; row < gridSize - 1; row++) {
			for (int col = 1; col < gridSize - 1; col++) {
				scenario.addNode(new GridNodeWithActivity(row, col, Activity.WORK));
			}
		}

		// All nodes allow for "other" activities:
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				scenario.addNode(new GridNodeWithActivity(row, col, Activity.OTHER));
			}
		}

		// Compute all node distances and travel times.
		for (var node1 : scenario.getNodesView()) {
			for (var node2 : scenario.getNodesView()) {
				int gridDistance = node1.computeGridDistance(node2);
				scenario.setDistance_km(node1, node2, edgeLength_km * gridDistance);
				scenario.setTime_h(node1, node2, edgeTime_h * gridDistance);
			}
		}

		/*
		 * Define the sampling weights. For this, create a SamplingWeights container and
		 * populate it with SamplingWeight instances.
		 */

		var weights = new MHWeightContainer<RoundTrip<GridNodeWithActivity>>();

		// An uniformed prior spreading out sampling where information is missing.
		weights.add(new SingleRoundTripUniformPrior<>(scenario));

		// Enforce that every single round trip is completed within the day.
		weights.add(new StrictlyPeriodicSchedule<GridNodeWithActivity>(scenario.getPeriodLength_h()));

		// Enforce that all round trips start and end their unique home location.
		weights.add(new StrictlyEnforceUniqueHomeLocation());

		// Sample round trips according to time use assumptions. See LogarithmicTimeUse
		// implementation for details.
		var timeUse = new LogarithmicTimeUseSinglePersonSingleDay<GridNodeWithActivity>(24.0);

		var homeComponent = timeUse.createComponent(12.0).setMinEnBlockDurationAtLeastOnce_h(8.0);
		homeComponent.addObservedNodes(scenario, node -> Activity.HOME.equals(node.getActivity()));
		timeUse.addConfiguredComponent(homeComponent);

		var workComponent = timeUse.createComponent(9.0).setOpeningTimes_h(6.0, 18.0)
				.setMinEnBlockDurationAtLeastOnce_h(8.0);
		workComponent.addObservedNodes(scenario, node -> Activity.WORK.equals(node.getActivity()));
		timeUse.addConfiguredComponent(workComponent);

		var otherComponent = timeUse.createComponent(3.0).setOpeningTimes_h(10, 20.0);
		otherComponent.addObservedNodes(scenario, node -> Activity.OTHER.equals(node.getActivity()));
		timeUse.addConfiguredComponent(otherComponent);

		weights.add(timeUse);

		/*
		 * Ready to set up the sampling machinery.
		 */

		var algo = new MHAlgorithm<>(new RoundTripProposal<>(scenario), weights, scenario.getRandom());

//		var initialRoundTrip = new RoundTrip<>(new ArrayList<>(Arrays.asList(home)), new ArrayList<>(Arrays.asList(0)));
//		initialRoundTrip.setEpisodes(scenario.getOrCreateSimulator().simulate(initialRoundTrip));
		var initialRoundTrip = scenario.createInitialRoundTrip(home, 0);
		algo.setInitialState(initialRoundTrip);

		// Log summary statistics over sampling iterations. See code for interpretation
		algo.addStateProcessor(new MHWeightsToFileLogger<>(totalIterations / 100, weights,
				"./output/activityExpansion/logWeights.log"));
		algo.addStateProcessor(new PlotTimeUseHistogram(totalIterations / 2, totalIterations / 100));

		algo.setMsgInterval(totalIterations / 100);
		algo.run(totalIterations);
	}

	public static void main(String[] args) {
		ActivityTimeUseExample example = new ActivityTimeUseExample(4711);
		example.run(1000 * 1000);
	}
}
