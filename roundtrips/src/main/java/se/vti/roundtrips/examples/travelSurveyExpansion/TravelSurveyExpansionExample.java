/**
 * se.vti.roundtrips.examples.travelSurveyExpansion
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
package se.vti.roundtrips.examples.travelSurveyExpansion;

import java.util.ArrayList;
import java.util.Arrays;

import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.Activity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.GridNodeWithActivity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.StrictlyEnforceUniqueHomeLocation;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.roundtrips.samplingweights.misc.StrictlyPeriodicSchedule;
import se.vti.roundtrips.samplingweights.priors.SingleRoundTripUniformPrior;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHWeightContainer;
import se.vti.utils.misc.metropolishastings.MHWeightsToFileLogger;

/**
 * 
 * @author GunnarF
 *
 */
public class TravelSurveyExpansionExample {

	private final long seed;

	TravelSurveyExpansionExample(long seed) {
		this.seed = seed;
	}

	void run(long totalIterations) {

		int syntheticPopulationSize = 50;

		int gridSize = 5;
		double edgeLength_km = 1;
		double edgeTime_h = 0.1;

		var scenario = new Scenario<GridNodeWithActivity>();
		scenario.getRandom().setSeed(this.seed);
		scenario.setTimeBinSize_h(1.0);
		scenario.setTimeBinCnt(24);
		scenario.setUpperBoundOnStayEpisodes(6);

		// Only the corner nodes allows for "home" activities (could be suburbs).
		var homes = Arrays.asList(new GridNodeWithActivity(0, 0, Activity.HOME),
				new GridNodeWithActivity(0, gridSize, Activity.HOME),
				new GridNodeWithActivity(gridSize, 0, Activity.HOME),
				new GridNodeWithActivity(gridSize, gridSize, Activity.HOME));
		for (var home : homes) {
			scenario.addNode(home);
		}

		// Only the center nodes allow for "work" activities (could be CBD).
		for (int row = 1; row < gridSize - 1; row++) {
			for (int col = 1; col < gridSize - 1; col++) {
				scenario.addNode(new GridNodeWithActivity(row, col, Activity.WORK));
			}
		}

		// Education is possible at the corner nodes of the CBD.
		scenario.addNode(new GridNodeWithActivity(1, 1, Activity.EDUCATION));
		scenario.addNode(new GridNodeWithActivity(1, 3, Activity.EDUCATION));
		scenario.addNode(new GridNodeWithActivity(3, 1, Activity.EDUCATION));
		scenario.addNode(new GridNodeWithActivity(3, 3, Activity.EDUCATION));

		// All nodes allow for "other" activities.
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
		 * Construct the survey
		 */
		var teenager = new SurveyResponse(new Person(15), 0, 6, 6);
		var worker = new SurveyResponse(new Person(35), 9, 0, 2);
		var retiree = new SurveyResponse(new Person(72), 0, 0, 4);
		var responses = Arrays.asList(teenager, worker, retiree);

		/*
		 * Construct the synthetic population
		 */

		var syntheticPopulation = new ArrayList<Person>(syntheticPopulationSize);
		for (int i = 0; i < syntheticPopulationSize; i++) {
			syntheticPopulation.add(new Person(scenario.getRandom().nextInt(15, 100)));
		}

		/*
		 * Define the sampling weights. For this, create a SamplingWeights container and
		 * populate it with SamplingWeight instances.
		 */

		var weights = new MHWeightContainer<MultiRoundTrip<GridNodeWithActivity>>();

		// Uniformed prior
		weights.add(new SingleToMultiWeight<>(new SingleRoundTripUniformPrior<>(scenario)));

		// Enforce that all round trips are completed within the day.
		weights.add(new SingleToMultiWeight<>(
				new StrictlyPeriodicSchedule<GridNodeWithActivity>(scenario.getPeriodLength_h())));

		// Enforce that all round trips start and end their unique home location.
		weights.add(new SingleToMultiWeight<>(new StrictlyEnforceUniqueHomeLocation()));

		// Prefer round trips that are compatible with the survey
//		weights.add(new ExplainResponsesByRoundTrips(responses, syntheticPopulation));
		weights.add(new ExplainRoundTripsByResponses(responses, syntheticPopulation));

		/*
		 * Ready to set up the sampling machinery.
		 */

		var algo = new MHAlgorithm<>(new MultiRoundTripProposal<>(scenario), weights, scenario.getRandom());

		var initialRoundTrip = scenario.createInitialMultiRoundTrip(homes, Arrays.asList(0), syntheticPopulationSize);
		algo.setInitialState(initialRoundTrip);

		// Log summary statistics over sampling iterations. See code for interpretation.
		algo.addStateProcessor(new MHWeightsToFileLogger<>(totalIterations / 100, weights,
				"./output/travelSurveyExpansion/logWeights.log"));
		algo.addStateProcessor(
				new PlotAgeByActivityHistogram(totalIterations / 2, totalIterations / 100, syntheticPopulation));

		algo.setMsgInterval(totalIterations / 100);
		algo.run(totalIterations);
	}

	public static void main(String[] args) {
		var example = new TravelSurveyExpansionExample(4711);
		example.run(1000 * 1000);
	}

}
