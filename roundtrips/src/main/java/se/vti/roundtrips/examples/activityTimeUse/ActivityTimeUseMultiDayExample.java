/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2025-2026 by Gunnar Flötteröd (VTI, LiU).
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

import se.vti.roundtrips.examples.activityExpandedGridNetwork.Activity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.GridNodeWithActivity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.StrictlyEnforceUniqueHomeLocation;
import se.vti.roundtrips.examples.samplingweights.StrictlyPeriodicSchedule;
import se.vti.roundtrips.examples.samplingweights.timeuse.LogarithmicTimeUseSinglePersonMultipleDays;
import se.vti.roundtrips.examples.samplingweights.timeuse.UniqueHomeLocationModifier;
import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.runner.Runner;
import se.vti.roundtrips.runner.basic.BasicProposal;
import se.vti.roundtrips.runner.modified.ModifiedStateAdapter;
import se.vti.roundtrips.scenario.ScenarioBuilder;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripUtils;
import se.vti.utils.misc.metropolishastings.MHAbstractStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
class ActivityTimeUseMultiDayExample {

	private final Long seed;

	ActivityTimeUseMultiDayExample(long seed) {
		this.seed = seed;
	}

	RoundTrip<GridNodeWithActivity> run(long totalIterations) {

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
		int numberOfTimeBins = 4 * 24;
		builder.setRandom(new Random(this.seed)).setTimeBinSize_h(timeBinSize_h).setNumberOfTimeBins(numberOfTimeBins);

		// All corner nodes allow for "home" activities
		GridNodeWithActivity initialHome = new GridNodeWithActivity(0, 0, Activity.HOME);
		builder.addNode(initialHome).addNode(new GridNodeWithActivity(0, gridSize - 1, Activity.HOME))
				.addNode(new GridNodeWithActivity(gridSize - 1, 0, Activity.HOME))
				.addNode(new GridNodeWithActivity(gridSize - 1, gridSize - 1, Activity.HOME));

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
		 * Define the modified sampling space. This is needed to modify round trips
		 * under the hood.
		 */
		var stateAdapter = new ModifiedStateAdapter<>(scenario, new UniqueHomeLocationModifier<GridNodeWithActivity>(),
				BasicProposal.createDefaultInstance(scenario, 1.0));

		/*
		 * Define the sampling weights. For this, create a SamplingWeights container and
		 * populate it with SamplingWeight instances.
		 */
		var runner = new Runner<GridNodeWithActivity>(scenario, stateAdapter);

		runner.setIndividualUniformPrior();

		// Enforce that every single round trip is completed within the day.
		runner.addIndividualWeight(new StrictlyPeriodicSchedule<GridNodeWithActivity>(scenario.getPeriodLength_h()));

		// Enforce that all round trips start and end their unique home location.
		runner.addIndividualWeight(new StrictlyEnforceUniqueHomeLocation());

		// TODO ENFORCE ACROSS DIFFERENT DAYS!!! NEEDS POSTPROCESSING FOR WEEK-WIDE
		// SWITCH!!!

		// Sample round trips according to time use assumptions. See LogarithmicTimeUse
		// implementation for details.

		var timeUse = new LogarithmicTimeUseSinglePersonMultipleDays<GridNodeWithActivity>(24.0, 7);

		var homeComponent = timeUse.createComponent(7 * 12.0, 0, 1, 2, 3, 4, 5, 6)
				.setMinEnBlockDurationAtLeastOnce_h(8.0);
		homeComponent.addObservedNodes(scenario, node -> Activity.HOME.equals(node.getActivity()));
		timeUse.addConfiguredComponent(homeComponent);

		var workComponent = timeUse.createComponent(5 * 9.0, 0, 1, 2, 3, 4).setOpeningTimes_h(6.0, 18.0)
				.setMinEnBlockDurationAtLeastOnce_h(8.0);
		workComponent.addObservedNodes(scenario, node -> Activity.WORK.equals(node.getActivity()));
		timeUse.addConfiguredComponent(workComponent);

		var otherComponent = timeUse.createComponent(3 * 3.0, 4, 5, 6).setOpeningTimes_h(10.0, 20.0);
		otherComponent.addObservedNodes(scenario, node -> Activity.OTHER.equals(node.getActivity()));
		timeUse.addConfiguredComponent(otherComponent);

		runner.addPopulationWeight(timeUse, 4.0);

		/*
		 * Ready to set up the sampling machinery.
		 */

		var initialState = RoundTripUtils.singleton().createMultiRoundTrip(initialHome, 0, 7);

		runner.setInitialState(initialState).setNumberOfIterations(totalIterations)
				.setMessageInterval(totalIterations / 100);

		// Log summary statistics over sampling iterations. See code for interpretation
		runner.configureWeightLogging("./output/activityExpansion/logWeights.log", 0);
		runner.addStateProcessor(
				new MHAbstractStateProcessor<MultiRoundTrip<GridNodeWithActivity>>(totalIterations / 2, 1000) {

					private PlotTimeUseHistogram[] plotHistograms = new PlotTimeUseHistogram[7];

					@Override
					public void start() {
						for (int day = 0; day < 7; day++) {
							this.plotHistograms[day] = new PlotTimeUseHistogram();
						}
					}

					@Override
					public void processStateHook(MultiRoundTrip<GridNodeWithActivity> state, double logWeight) {
						for (int day = 0; day < 7; day++) {
							this.plotHistograms[day].processStateHook(state.getRoundTrip(day), logWeight);
						}
					}

					@Override
					public void end() {
						String[] days = new String[] { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
								"Saturday", "Sunday" };
						for (int day = 0; day < 7; day++) {
							System.out.println(days[day]);
							this.plotHistograms[day].end();
						}
					}
				});

		runner.run();

		return runner.getFinalState().getRoundTrip(0);
	}

	public static void main(String[] args) {
		ActivityTimeUseMultiDayExample example = new ActivityTimeUseMultiDayExample(new Random().nextLong());
		example.run(1_000_000);
	}
}
