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

import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.logging.multiple.SizeDistributionLogger;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.roundtrips.samplingweights.misc.StrictlyForbidShortStays;
import se.vti.roundtrips.samplingweights.misc.StrictlyPeriodicSchedule;
import se.vti.roundtrips.samplingweights.priors.UniformPriorFactory;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHWeightContainer;
import se.vti.utils.misc.metropolishastings.SamplingWeightLogger;

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

		var scenario = new Scenario<GridNode>();
		scenario.getRandom().setSeed(this.seed);
		scenario.setTimeBinSize_h(1.0);
		scenario.setTimeBinCnt(24);
		double minStayDuration_h = 1.0;

		int fleetSize = 5;

		double depotOpening_h = 18.0; // opens at 6pm
		double depotClosing_h = 6.0; // closes at 6am

		/*
		 * Populate the grid world with nodes.
		 * 
		 * Define distances and travel times between grid nodes.
		 */

		GridNode[][] nodes = new GridNode[gridSize][gridSize];
		for (int row = 0; row < gridSize; row++) {
			for (int col = 0; col < gridSize; col++) {
				nodes[row][col] = new GridNode(row, col);
				scenario.addNode(nodes[row][col]);
			}
		}
		GridNode depot = nodes[0][0];

		for (int row1 = 0; row1 < gridSize; row1++) {
			for (int column1 = 0; column1 < gridSize; column1++) {
				GridNode node1 = nodes[row1][column1];
				for (int row2 = 0; row2 < gridSize; row2++) {
					for (int column2 = 0; column2 < gridSize; column2++) {
						GridNode node2 = nodes[row2][column2];
						int gridDistance = node1.computeGridDistance(node2);
						scenario.setDistance_km(node1, node2, edgeLength_km * gridDistance);
						scenario.setTime_h(node1, node2, edgeTime_h * gridDistance);
					}
				}
			}
		}

		/*
		 * Define the sampling weights. For this, create a SamplingWeights container and
		 * populate it with SamplingWeight instances.
		 */

		MHWeightContainer<MultiRoundTrip<GridNode>> weights = new MHWeightContainer<>();

		// A uniform prior spreading out sampling where information is missing.
		weights.add(new UniformPriorFactory<>(scenario).createSingles());

		// Ensure that every single round trip is completed within the day.
		weights.add(new SingleToMultiWeight<>(new StrictlyPeriodicSchedule<GridNode>(scenario.getPeriodLength_h())),
				1.0);

		// Ensure that a vehicle stays a minimum duration at every visited location.
		weights.add(new SingleToMultiWeight<>(new StrictlyForbidShortStays<>(minStayDuration_h)), 1.0);

		// Penalize not reaching all nodes. See comments in CoverageWeight class.
		weights.add(new CoverageWeight(gridSize, depotOpening_h, depotClosing_h), 8.0);

		/*
		 * Ready to set up the sampling machinery.
		 */

		var algo = new MHAlgorithm<>(new MultiRoundTripProposal<>(scenario), weights, scenario.getRandom());

		// Initialize all trucks to just stay at the depot.
		algo.setInitialState(scenario.createInitialMultiRoundTrip(nodes[0][0], 0, fleetSize));

		// Log summary statistics over sampling iterations. See code for interpretation
		algo.addStateProcessor(new SamplingWeightLogger<>(totalIterations / 100, weights,
				"./output/truckServiceCoverage/logWeights.log"));
		var sizeLogger = new SizeDistributionLogger<GridNode>(totalIterations / 10,
				scenario.getMaxPossibleStayEpisodes(), false, "./output/truckServiceCoverage/sizes.log");
		algo.addStateProcessor(sizeLogger);
		algo.addStateProcessor(new MissionLogger(depot, totalIterations / 100));
		algo.addStateProcessor(new EarliestArrivalLogger(depot, gridSize, totalIterations / 100,
				"./output/truckServiceCoverage/earliestArrivals.log"));

		algo.setMsgInterval(totalIterations / 100);
		algo.run(totalIterations);

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
