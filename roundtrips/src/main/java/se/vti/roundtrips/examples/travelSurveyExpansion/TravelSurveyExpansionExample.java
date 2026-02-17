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
import java.util.Random;

import se.vti.roundtrips.common.Runner;
import se.vti.roundtrips.common.ScenarioBuilder;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.Activity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.GridNodeWithActivity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.StrictlyEnforceUniqueHomeLocation;
import se.vti.roundtrips.samplingweights.misc.StrictlyPeriodicSchedule;
import se.vti.roundtrips.statistics.TotalTravelTime;

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

		var rnd = new Random(this.seed);
		var scenarioBuilder = new ScenarioBuilder<GridNodeWithActivity>().setRandom(rnd).setTimeBinSize_h(1.0)
				.setNumberOfTimeBins(24).setUpperBoundOnStayEpisodes(6);

		// Only the corner nodes allows for "home" activities (could be suburbs).
		var homes = Arrays.asList(new GridNodeWithActivity(0, 0, Activity.HOME),
				new GridNodeWithActivity(0, gridSize, Activity.HOME),
				new GridNodeWithActivity(gridSize, 0, Activity.HOME),
				new GridNodeWithActivity(gridSize, gridSize, Activity.HOME));
		for (var home : homes) {
			scenarioBuilder.addNode(home);
		}

		// Only the center nodes allow for "work" activities (could be CBD).
		for (int row = 1; row < gridSize - 1; row++) {
			for (int col = 1; col < gridSize - 1; col++) {
				scenarioBuilder.addNode(new GridNodeWithActivity(row, col, Activity.WORK));
			}
		}

		// Education is possible at the corner nodes of the CBD.
		scenarioBuilder.addNode(new GridNodeWithActivity(1, 1, Activity.EDUCATION))
				.addNode(new GridNodeWithActivity(1, 3, Activity.EDUCATION))
				.addNode(new GridNodeWithActivity(3, 1, Activity.EDUCATION))
				.addNode(new GridNodeWithActivity(3, 3, Activity.EDUCATION));

		// All nodes allow for "other" activities.
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				scenarioBuilder.addNode(new GridNodeWithActivity(row, col, Activity.OTHER));
			}
		}

		// Define distances and travel times.
		scenarioBuilder.setMoveDistanceFunction((a, b) -> edgeLength_km * a.computeGridDistance(b));
		scenarioBuilder.setMoveTimeFunction((a, b) -> edgeTime_h * a.computeGridDistance(b));

		// Construct the survey.
		var teenager = new SurveyResponse(new Person(15), 0, 6, 6);
		var worker = new SurveyResponse(new Person(35), 9, 0, 2);
		var retiree = new SurveyResponse(new Person(72), 0, 0, 4);
		var responses = Arrays.asList(teenager, worker, retiree);

		// Construct the synthetic population.
		var syntheticPopulation = new ArrayList<Person>(syntheticPopulationSize);
		for (int i = 0; i < syntheticPopulationSize; i++) {
			syntheticPopulation.add(new Person(rnd.nextInt(15, 100)));
		}

		// Scenario is ready.
		var scenario = scenarioBuilder.build();

		var runner = new Runner<GridNodeWithActivity>(scenario);

		// Definee the sampling weights.
		runner.setUniformPrior()
				.addSingleWeight(
						new StrictlyPeriodicSchedule<GridNodeWithActivity>(scenario.getPeriodLength_h()))
				.addSingleWeight(new StrictlyEnforceUniqueHomeLocation())
				.addWeight(new ExplainRoundTripsByResponses2(responses, syntheticPopulation));

		// Define the logging.
		runner.configureWeightLogging("./output/travelSurveyExpansion/logWeights.log", totalIterations / 100);
		runner.addStateProcessor(
				new PlotAgeByActivityHistogram(totalIterations / 2, totalIterations / 100, syntheticPopulation));
		runner.configureStatisticsLogging("./output/travelSurveyExpansion/statisticsLogs.log", 1000)
				.addStatisticEstimator(new TotalTravelTime<GridNodeWithActivity>());

		// Configure sampling and run.
		var initialRoundTrip = scenario.createInitialMultiRoundTrip(homes, Arrays.asList(0), syntheticPopulationSize);
		runner.setInitialState(initialRoundTrip).setMessageInterval(totalIterations / 100)
				.setNumberOfIterations(totalIterations);
		runner.run();
	}

	public static void main(String[] args) {
		var example = new TravelSurveyExpansionExample(4711);
		example.run(1000 * 1000);
	}

}
