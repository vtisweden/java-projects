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

import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.scenario.NodeWithCoords;
import se.vti.utils.misc.math.LogitChoiceModel;
import se.vti.utils.misc.metropolishastings.MHAbstractStateProcessor;

/**
 * @GunnarF
 */
class TransportEfficiencyAnalysis extends MHAbstractStateProcessor<MultiRoundTrip<NodeWithCoords>> {

	// -------------------- MEMBERS --------------------

	private final ScenarioContainer scenarioContainer;

	private final double transportCost_1_kTonKm;

	private final double choiceModelScale;

	private final LogitChoiceModel choiceModel = new LogitChoiceModel(null);

	private double relativeChangeThreshold = 1e-2;

	private long maxIterations = 1_000_000;

	// -------------------- CONSTRUCTION --------------------

	TransportEfficiencyAnalysis(ScenarioContainer scenarioContainer, double transportCost_1_kTonKm,
			double choiceModelScale, int samplingInterval) {
		super(0, samplingInterval);
		this.scenarioContainer = scenarioContainer;
		this.transportCost_1_kTonKm = transportCost_1_kTonKm;
		this.choiceModelScale = choiceModelScale;
	}

	// -------------------- CONSTRUCTION --------------------

	TransportEfficiencyAnalysis setRelativeChangeThreshold(double relativeChangeThreshold) {
		this.relativeChangeThreshold = relativeChangeThreshold;
		return this;
	}

	TransportEfficiencyAnalysis setMaxIterations(long maxIterations) {
		this.maxIterations = maxIterations;
		return this;
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public void processStateHook(MultiRoundTrip<NodeWithCoords> roundTrips, double logWeight) {

		final var odCoverage = (ODCoverage<NodeWithCoords>) roundTrips.getSummary(ODCoverage.class);

		assert (roundTrips.size() == odCoverage.getNumberOfRoundTrips());
		final int numberOfODs = odCoverage.getNumberOfODs();

		final double[] odIndex2UnservedDemand_kTon = new double[numberOfODs];
		final double[] roundTripIndex2MaxTransport_kTon = new double[roundTrips.size()];
		final double[] roundTripIndex2TransportCost_1_km = new double[roundTrips.size()];
		final double[][] roundTripAndODIndices2Transport_kTon = new double[roundTrips.size()][numberOfODs];

		long it = 0;
		double relativeChange;
		do {

			/*
			 * Evaluate demand model and resulting flows.
			 */

			for (int odIndex = 0; odIndex < numberOfODs; odIndex++) {

				double demand_kTon = this.scenarioContainer.getDemand_kTon(odIndex);
				int[] roundTripIndices = odCoverage.odIndex2ConnectingRoundTripIndices.get(odIndex).stream()
						.mapToInt(i -> i).toArray();

				if (roundTripIndices.length == 0) {

					odIndex2UnservedDemand_kTon[odIndex] = demand_kTon;

				} else {

					odIndex2UnservedDemand_kTon[odIndex] = 0.0;

					double distance_km = this.scenarioContainer.getDistance_km(odIndex);
					double[] utilities = new double[roundTripIndices.length];
					for (int i = 0; i < roundTripIndices.length; i++) {
						utilities[i] = (-1.0) * this.choiceModelScale * distance_km
								* roundTripIndex2TransportCost_1_km[roundTripIndices[i]];
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

				roundTripIndex2MaxTransport_kTon[roundTripIndex] = Arrays
						.stream(roundTripAndODIndices2Transport_kTon[roundTripIndex]).max().orElseThrow();
				double transportCost_1_km = this.transportCost_1_kTonKm
						* roundTripIndex2MaxTransport_kTon[roundTripIndex];

				relativeChange = Math.max(relativeChange, Math
						.abs(transportCost_1_km - roundTripIndex2TransportCost_1_km[roundTripIndex])
						/ (1e-8 + Math.max(transportCost_1_km, roundTripIndex2TransportCost_1_km[roundTripIndex])));

				double innoWeight = 1.0 / (it + 1);
				roundTripIndex2TransportCost_1_km[roundTripIndex] = (1.0 - innoWeight)
						* roundTripIndex2TransportCost_1_km[roundTripIndex] + innoWeight * transportCost_1_km;
			}

			/*
			 * Termination check.
			 */

//			if (it % 1000 == 0) {
//			System.out.println("it " + it + ", relativeChange = " + relativeChange);
//			}

			it++;
			if ((it >= this.maxIterations) && (relativeChange > this.relativeChangeThreshold)) {
				throw new RuntimeException("Reached maximum number of " + this.maxIterations
						+ " iterations with relative error of " + relativeChange + " still above threshold value "
						+ this.relativeChangeThreshold + ".");
			}

		} while (relativeChange > this.relativeChangeThreshold);

		/*
		 * Extract statistics.
		 */

		var efficiencyStatsOverRoundTrips = new DescriptiveStatistics();

		double totalLoadedTransportWork_kTonKm = 0;
		double totalUnloadedTransportWork_kTonKm = 0;

		for (int roundTripIndex = 0; roundTripIndex < roundTrips.size(); roundTripIndex++) {
			double maxTransport_kTon = roundTripIndex2MaxTransport_kTon[roundTripIndex];

			double loadedTransportWork_kTonKm = 0;
			double unloadedTransportWork_kTonKm = 0;
			double[] odIndex2Transport_kTon = roundTripAndODIndices2Transport_kTon[roundTripIndex];
			for (int odIndex : odCoverage.roundTripIndex2ConnectedODIndices.get(roundTripIndex)) {
				double distance_km = this.scenarioContainer.getDistance_km(odIndex);
				double transport_kTon = odIndex2Transport_kTon[odIndex];
				loadedTransportWork_kTonKm += distance_km * transport_kTon;
				unloadedTransportWork_kTonKm += distance_km * (maxTransport_kTon - transport_kTon);
			}

			totalLoadedTransportWork_kTonKm += loadedTransportWork_kTonKm;
			totalUnloadedTransportWork_kTonKm += unloadedTransportWork_kTonKm;

			efficiencyStatsOverRoundTrips.addValue(
					loadedTransportWork_kTonKm / (1e-8 + loadedTransportWork_kTonKm + unloadedTransportWork_kTonKm));
		}

		System.out.println("Total loaded transport work: " + totalLoadedTransportWork_kTonKm + " kTon");
		System.out.println("Total unloaded transport work: " + totalUnloadedTransportWork_kTonKm + " kTon");
		System.out.println("Total transport efficiency: " + totalLoadedTransportWork_kTonKm
				/ (totalLoadedTransportWork_kTonKm + totalUnloadedTransportWork_kTonKm));
		System.out.println("Transport efficiency per round trip " + efficiencyStatsOverRoundTrips);
	}
}
