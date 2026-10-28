/**
 * se.vti.samgods
 * 
 * Copyright (C) 2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.preprocessing.loopgeneration;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.scenario.NodeWithCoords;
import se.vti.utils.misc.math.LogitChoiceModel;
import se.vti.utils.misc.metropolishastings.MHAbstractStateProcessor;

/**
 * @GunnarF
 */
class IterativeTransportEfficiencyAnalyzer extends MHAbstractStateProcessor<MultiRoundTrip<NodeWithCoords>> {

	private static final Logger log = LogManager.getLogger(IterativeTransportEfficiencyAnalyzer.class);

	// -------------------- MEMBERS --------------------

	private final ScenarioContainer scenarioContainer;

	private final double transportCost_1_kTonKm;

	private final double choiceModelScale;

	private final LogitChoiceModel choiceModel = new LogitChoiceModel(null);

	private double relativeChangeThreshold = 1e-2;

	private long maxIterations = 1_000_000;

	private long[] efficiencyHistogram = null;


	private DescriptiveStatistics totalEfficiencyStats = null;
	private DescriptiveStatistics overODsEfficiencyStats = null;
	private DescriptiveStatistics costStats = null;
	private DescriptiveStatistics connectionsPerODStats = null;
	private DescriptiveStatistics connectionsPerKTonStats = null;

	// -------------------- CONSTRUCTION --------------------

	IterativeTransportEfficiencyAnalyzer(ScenarioContainer scenarioContainer, double transportCost_1_kTonKm,
			double choiceModelScale, int samplingInterval) {
		super(0, samplingInterval);
		this.scenarioContainer = scenarioContainer;
		this.transportCost_1_kTonKm = transportCost_1_kTonKm;
		this.choiceModelScale = choiceModelScale;
	}

	// -------------------- CONSTRUCTION --------------------

	IterativeTransportEfficiencyAnalyzer setRelativeChangeThreshold(double relativeChangeThreshold) {
		this.relativeChangeThreshold = relativeChangeThreshold;
		return this;
	}

	IterativeTransportEfficiencyAnalyzer setMaxIterations(long maxIterations) {
		this.maxIterations = maxIterations;
		return this;
	}

	IterativeTransportEfficiencyAnalyzer setTotalEfficiencyStats(DescriptiveStatistics totalEfficiencyStats) {
		this.totalEfficiencyStats = totalEfficiencyStats;
		return this;
	}

	IterativeTransportEfficiencyAnalyzer setCostStats(DescriptiveStatistics costStats) {
		this.costStats = costStats;
		return this;
	}

	IterativeTransportEfficiencyAnalyzer setOverODsEfficiencyStats(DescriptiveStatistics overODsEfficiencyStats) {
		this.overODsEfficiencyStats = overODsEfficiencyStats;
		return this;
	}

	IterativeTransportEfficiencyAnalyzer setConnectionsPerKTonStats(DescriptiveStatistics connectionsPerKTonStats) {
		this.connectionsPerKTonStats = connectionsPerKTonStats;
		return this;
	}

	IterativeTransportEfficiencyAnalyzer setConnectionsPerODStats(DescriptiveStatistics connectionsPerODStats) {
		this.connectionsPerODStats = connectionsPerODStats;
		return this;
	}

	IterativeTransportEfficiencyAnalyzer setEfficiencyHistogram(long[] efficiencyHistogram) {
		this.efficiencyHistogram = efficiencyHistogram;
		return this;
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public void processStateHook(MultiRoundTrip<NodeWithCoords> roundTrips, double logWeight) {
		this.process(roundTrips);
	}

	void process(MultiRoundTrip<NodeWithCoords> roundTrips) {

		final var odCoverage = (ODCoverage<NodeWithCoords>) roundTrips.getSummary(ODCoverage.class);

		assert (roundTrips.size() == odCoverage.getNumberOfRoundTrips());
		final int numberOfODs = odCoverage.getNumberOfODs();

		final int[][] odIndex2RoundTripIndices = new int[numberOfODs][];
		for (int odIndex = 0; odIndex < numberOfODs; odIndex++) {
			odIndex2RoundTripIndices[odIndex] = odCoverage.getConnectingRoundTripIndices(odIndex).stream()
					.mapToInt(i -> i).toArray();
		}

		final double[] roundTripIndex2MaxTransport_kTon = new double[roundTrips.size()];
		final double[] roundTripIndex2TransportCost_1_km = new double[roundTrips.size()];
		final double[][] roundTripAndODIndices2Transport_kTon = new double[roundTrips.size()][numberOfODs];

		/*
		 * SOLVE EQUILIBRIUM MODEL.
		 */

		long it = 0;
		double relativeChange;

		do {

			/*
			 * Evaluate OD demand model and resulting flows.
			 */

			for (int odIndex = 0; odIndex < numberOfODs; odIndex++) {

				double demand_kTon = this.scenarioContainer.getDemand_kTon(odIndex);
				int[] roundTripIndices = odIndex2RoundTripIndices[odIndex];

				if (roundTripIndices.length > 0) {
					double distance_km = this.scenarioContainer.getDistance_km(odIndex);
					double[] utilities = new double[roundTripIndices.length];

					for (int i = 0; i < roundTripIndices.length; i++) {
						int roundTripIndex = roundTripIndices[i];
						double cost_1_kTonKm = roundTripIndex2TransportCost_1_km[roundTripIndex]
								/ (1e-8 + roundTripAndODIndices2Transport_kTon[roundTripIndex][odIndex]);
						double cost = cost_1_kTonKm * demand_kTon * distance_km;
						utilities[i] = (-1.0) * this.choiceModelScale * cost;
					}

					double[] choiceProbabilities = this.choiceModel.computeLogitProbabilities(utilities);
					for (int i = 0; i < roundTripIndices.length; i++) {
						roundTripAndODIndices2Transport_kTon[roundTripIndices[i]][odIndex] = choiceProbabilities[i]
								* demand_kTon;
					}
				}
			}

			/*
			 * Evaluate supply model and resulting costs.
			 */

			relativeChange = 0.0;

			for (int roundTripIndex = 0; roundTripIndex < roundTrips.size(); roundTripIndex++) {
				double[] odIndex2Transport_kTon = roundTripAndODIndices2Transport_kTon[roundTripIndex];

				double maxTransport_kTon = 0.0;
				for (int odIndex : odCoverage.getConnectedODIndices(roundTripIndex)) {
					maxTransport_kTon = Math.max(maxTransport_kTon, odIndex2Transport_kTon[odIndex]);
				}
				roundTripIndex2MaxTransport_kTon[roundTripIndex] = maxTransport_kTon;
				double transportCost_1_km = this.transportCost_1_kTonKm * maxTransport_kTon;

				relativeChange = Math.max(relativeChange, Math
						.abs(transportCost_1_km - roundTripIndex2TransportCost_1_km[roundTripIndex])
						/ (1e-8 + 0.5 * (transportCost_1_km + roundTripIndex2TransportCost_1_km[roundTripIndex])));

				double innoWeight = 1.0 / (it + 1);
				roundTripIndex2TransportCost_1_km[roundTripIndex] = (1.0 - innoWeight)
						* roundTripIndex2TransportCost_1_km[roundTripIndex] + innoWeight * transportCost_1_km;
			}

			/*
			 * Termination check.
			 */

			it++;

			if (it % 1000 == 0) {
				log.info("  iteration = " + it + ", relativeChange = " + relativeChange);
			}

		} while ((relativeChange > this.relativeChangeThreshold) && (it < this.maxIterations));

		/*
		 * EXTRACT SOLUTION STATISTICS.
		 */

		/*
		 * Roundtrip statistics.
		 */
		
		double totalLoadedTransportWork_kTonKm = 0;
		double totalUnloadedTransportWork_kTonKm = 0;

		for (int roundTripIndex = 0; roundTripIndex < roundTrips.size(); roundTripIndex++) {
			double maxTransport_kTon = roundTripIndex2MaxTransport_kTon[roundTripIndex];

			double loadedTransportWork_kTonKm = 0;
			double unloadedTransportWork_kTonKm = 0;
			double[] odIndex2Transport_kTon = roundTripAndODIndices2Transport_kTon[roundTripIndex];
			for (int odIndex : odCoverage.getConnectedODIndices(roundTripIndex)) {
				double distance_km = this.scenarioContainer.getDistance_km(odIndex);
				double transport_kTon = odIndex2Transport_kTon[odIndex];
				loadedTransportWork_kTonKm += distance_km * transport_kTon;
				unloadedTransportWork_kTonKm += distance_km * (maxTransport_kTon - transport_kTon);
			}
			
			if (this.efficiencyHistogram != null) {
				double roundTripEfficiency = loadedTransportWork_kTonKm / (1e-8 + loadedTransportWork_kTonKm + unloadedTransportWork_kTonKm);
				int bin = (int) (roundTripEfficiency * this.efficiencyHistogram.length);
				bin = Math.max(0, Math.min(this.efficiencyHistogram.length - 1, bin));
				this.efficiencyHistogram[bin]++;
			}
			
			totalLoadedTransportWork_kTonKm += loadedTransportWork_kTonKm;
			totalUnloadedTransportWork_kTonKm += unloadedTransportWork_kTonKm;

			if (this.overODsEfficiencyStats != null) {
				double odEfficiency = loadedTransportWork_kTonKm
						/ (1e-8 + loadedTransportWork_kTonKm + unloadedTransportWork_kTonKm);
				this.overODsEfficiencyStats.addValue(odEfficiency);
			}
	
			if (this.costStats != null) {
				this.costStats.addValue(roundTripIndex2TransportCost_1_km[roundTripIndex]);
			}
		}

		if (this.totalEfficiencyStats != null) {
			double totalEfficiency = totalLoadedTransportWork_kTonKm
					/ (1e-8 + totalLoadedTransportWork_kTonKm + totalUnloadedTransportWork_kTonKm);
			this.totalEfficiencyStats.addValue(totalEfficiency);
		}

		/*
		 * OD statistics.
		 */

		for (int odIndex = 0; odIndex < numberOfODs; odIndex++) {
			var roundTripIndices = odIndex2RoundTripIndices[odIndex];
			if (this.connectionsPerODStats != null) {
				this.connectionsPerODStats.addValue(roundTripIndices.length);
			}
			if (this.connectionsPerKTonStats != null) {
				this.connectionsPerKTonStats
						.addValue(roundTripIndices.length / (1e-8 + this.scenarioContainer.getDemand_kTon(odIndex)));
			}
		}
	}
}
