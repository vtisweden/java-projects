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
package se.vti.roundtrips.examples.truckServiceCoverage;

import java.util.Random;

import se.vti.roundtrips.common.Runner;
import se.vti.roundtrips.common.ScenarioBuilder;
import se.vti.roundtrips.logging.multiple.SizeDistributionLogger;
import se.vti.roundtrips.samplingweights.StrictlyForbidShortStays;
import se.vti.roundtrips.samplingweights.StrictlyPeriodicSchedule;

/**
 * 
 * @author GunnarF
 *
 */
class TruckServiceCoverageExample {

	private final long seed;

	TruckServiceCoverageExample(long seed) {
		this.seed = seed;
	}

	void run(long totalIterations) {

		/*
		 * What feasible truck round tours may arise in a given system, subject to fleet
		 * and delivery constraints?
		 * 
		 * The considered system is a grid network that consists of gridSize x gridSize
		 * nodes. All roads between nodes have the same length and traversal time.
		 * 
		 * Trucks move in daily loops. Within-day time is discretized into one hour time
		 * bins. There is a minimum (loading/unloading) time the vehicles have to spend
		 * at each stop.
		 * 
		 * The truck fleet size is given. The single depot (source of all shipments) has
		 * limited (over-night) opening times.
		 */

		int gridSize = 5;
		double edgeLength_km = 120;
		double edgeTime_h = 2.0;

		double minStayDuration_h = 1.0;
		int fleetSize = 5;
		double depotOpening_h = 18.0; // opens at 6pm
		double depotClosing_h = 6.0; // closes at 6am

		var scenarioBuilder = new ScenarioBuilder<GridNode>().setRandom(new Random(this.seed)).setTimeBinSize_h(1.0)
				.setNumberOfTimeBins(24);

		/*
		 * Populate the grid world with nodes.
		 * 
		 * Define distances and travel times between grid nodes.
		 */
		GridNode[][] nodes = new GridNode[gridSize][gridSize];
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				nodes[row][col] = new GridNode(row, col);
				scenarioBuilder.addNode(nodes[row][col]);
			}
		}
		GridNode depot = nodes[0][0];
		scenarioBuilder.setMoveDistanceFunction((a, b) -> edgeLength_km * a.computeGridDistance(b));
		scenarioBuilder.setMoveTimeFunction((a, b) -> edgeTime_h * a.computeGridDistance(b));

		var scenario = scenarioBuilder.build();

		var runner = new Runner<>(scenario);

		/*
		 * Define the sampling weights.
		 */

		// A uniform prior spreading out sampling where information is missing.
		runner.setUniformPrior();

		// Ensure that every single round trip is completed within the day.
		runner.addIndividualWeight(new StrictlyPeriodicSchedule<GridNode>(scenario.getPeriodLength_h()));

		// Ensure that a vehicle stays a minimum duration at every visited location.
		runner.addIndividualWeight(new StrictlyForbidShortStays<>(minStayDuration_h));

		// Penalize not reaching all nodes. See comments in CoverageWeight class.
		runner.addPopulationWeight(new CoverageWeight(gridSize, depotOpening_h, depotClosing_h), 8.0);

		/*
		 * Ready to set up the sampling machinery.
		 */

		// Initialize all trucks to just stay at the depot.
		runner.setInitialState(scenario.createInitialMultiRoundTrip(nodes[0][0], 0, fleetSize));

		// Log summary statistics over sampling iterations. See code for interpretation
		runner.configureWeightLogging("./output/truckServiceCoverage/logWeights.log", totalIterations / 100);

		var sizeLogger = new SizeDistributionLogger<GridNode>(totalIterations / 10,
				scenario.getNumberOfTimeBins(), false, "./output/truckServiceCoverage/sizes.log");
		runner.addStateProcessor(sizeLogger).addStateProcessor(new MissionLogger(depot, totalIterations / 100))
				.addStateProcessor(new EarliestArrivalLogger(depot, gridSize, totalIterations / 100,
						"./output/truckServiceCoverage/earliestArrivals.log"));

		runner.setMessageInterval(totalIterations / 100).setNumberOfIterations(totalIterations).run();

		// The resulting files in the output folder can directly be pasted into Excel.

		// testing
		this.lastSizes = sizeLogger.getLastSizeCounts();
	}

	// testing
	private int[] lastSizes = null;

	// testing
	int[] getLastSizes() {
		return this.lastSizes;
	}

	// testing
	int[] test1() {
		this.run(1000);
		return this.lastSizes;
	}

	public static void main(String[] args) {
		TruckServiceCoverageExample example = new TruckServiceCoverageExample(4711);
		example.run(1000 * 1000);
	}
}
