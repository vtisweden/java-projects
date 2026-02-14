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

import java.util.Random;

import se.vti.roundtrips.common.Runner;
import se.vti.roundtrips.common.ScenarioBuilder;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.Activity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.GridNodeWithActivity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.StrictlyEnforceUniqueHomeLocation;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.samplingweights.misc.StrictlyPeriodicSchedule;
import se.vti.roundtrips.samplingweights.misc.timeUse.LogarithmicTimeUseSinglePersonSingleDay;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
class ActivityTimeUseExampleWithBuilderAndRunner {

	private final Long seed;

	ActivityTimeUseExampleWithBuilderAndRunner(long seed) {
		this.seed = seed;
	}

	void run(long totalIterations) {

		var builder = new ScenarioBuilder<GridNodeWithActivity>();

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

		double timeBinSize_h = 0.25;
		int numberOfTimeBins = 4 * 25;
		builder.setRandom(new Random(this.seed)).setTimeBinSize_h(timeBinSize_h).setNumberOfTimeBins(numberOfTimeBins);

		// A single corner node allows for "home" activities (could be a suburb).
		GridNodeWithActivity home = new GridNodeWithActivity(0, 0, Activity.HOME);
		builder.addNode(home);

		// Only the center nodes allow for "work" activities (could be CBD).
		for (int row = 1; row < gridSize - 1; row++) {
			for (int col = 1; col < gridSize - 1; col++) {
				builder.addNode(new GridNodeWithActivity(row, col, Activity.WORK));
			}
		}

		// All nodes allow for "other" activities:
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				builder.addNode(new GridNodeWithActivity(row, col, Activity.OTHER));
			}
		}

		// Compute all node distances and travel times.
		builder.setMoveDistanceFunction((a, b) -> edgeLength_km * a.computeGridDistance(b));
		builder.setMoveTimeFunction((a, b) -> edgeTime_h * a.computeGridDistance(b));

		var scenario = builder.build();

		/*
		 * Define the sampling weights. For this, create a SamplingWeights container and
		 * populate it with SamplingWeight instances.
		 */
		var runner = new Runner<GridNodeWithActivity>(scenario);

		runner.setUniformPrior();

		// Enforce that every single round trip is completed within the day.
		runner.addSingleWeight(new StrictlyPeriodicSchedule<GridNodeWithActivity>(scenario.getPeriodLength_h()));

		// Enforce that all round trips start and end their unique home location.
		runner.addSingleWeight(new StrictlyEnforceUniqueHomeLocation());

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

		runner.addSingleWeight(timeUse);

		/*
		 * Ready to set up the sampling machinery.
		 */
		var initialState = new MultiRoundTrip<GridNodeWithActivity>(1);
		initialState.setRoundTripAndUpdateSummaries(0, scenario.createInitialRoundTrip(home, 0));
		runner.setInitialState(initialState).setNumberOfIterations(totalIterations)
				.setMessageInterval(totalIterations / 100);

		// Log summary statistics over sampling iterations. See code for interpretation
		runner.configureWeightLogging("./output/activityExpansion/logWeights.log", totalIterations / 100);
		runner.addStateProcessor(new MHStateProcessor<MultiRoundTrip<GridNodeWithActivity>>() {
			private PlotTimeUseHistogram plotHistogram = new PlotTimeUseHistogram(totalIterations / 2,
					totalIterations / 100);

			@Override
			public void start() {
				this.plotHistogram.start();
			}

			@Override
			public void processState(MultiRoundTrip<GridNodeWithActivity> state) {
				this.plotHistogram.processState(state.getRoundTrip(0));
			}

			@Override
			public void end() {
				this.plotHistogram.end();
			}
		});

		runner.run();
	}

	public static void main(String[] args) {
		ActivityTimeUseExampleWithBuilderAndRunner example = new ActivityTimeUseExampleWithBuilderAndRunner(4711);
		example.run(1000 * 1000);
	}
}
