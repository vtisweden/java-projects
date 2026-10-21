/**
 * se.vti.samgods
 * 
 * Copyright (C) 2025, 2026 by Gunnar Flötteröd (VTI, LiU).
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

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.scenario.NodeWithCoords;
import se.vti.utils.misc.math.LogitChoiceModel;
import se.vti.utils.misc.metropolishastings.MHAbstractStateProcessor;

/**
 * @GunnarF
 */
class TransportEfficiencyAnalysis extends MHAbstractStateProcessor<MultiRoundTrip<NodeWithCoords>> {

	private final LogitChoiceModel choiceModel = new LogitChoiceModel(null);

	private final ScenarioContainer scenarioContainer;
	private final double transportCost_1_kTonKm;
	private final double scale;

	TransportEfficiencyAnalysis(ScenarioContainer scenarioContainer, double transportCost_1_kTonKm, double scale,
			int samplingInterval) {
		super(0, samplingInterval);
		this.scenarioContainer = scenarioContainer;
		this.transportCost_1_kTonKm = transportCost_1_kTonKm;
		this.scale = scale;
	}

	@Override
	public void processStateHook(MultiRoundTrip<NodeWithCoords> roundTrips, double logWeight) {

		var odCoverage = (ODCoverage<NodeWithCoords>) roundTrips.getSummary(ODCoverage.class);

		assert (roundTrips.size() == odCoverage.odIndex2ConnectingRoundTripIndices.size());
		final int numberOfODs = odCoverage.odIndex2ConnectingRoundTripIndices.size();

		final double[] transportCostPerRT_1_km = new double[roundTrips.size()];
		final double[] unservedDemandPerOD_kTon = new double[numberOfODs];
		final double[][] transportPerODPerRT_kTon = new double[numberOfODs][roundTrips.size()];

		/*
		 * Evaluate demand model and resulting flows.
		 */

		for (int odIndex = 0; odIndex < numberOfODs; odIndex++) {

			final double demand_kTon = this.scenarioContainer.getDemand_kTon(odIndex);
			final int[] roundTripIndices = odCoverage.odIndex2ConnectingRoundTripIndices.get(odIndex).stream()
					.mapToInt(i -> i).toArray();

			if (roundTripIndices.length == 0) {

				unservedDemandPerOD_kTon[odIndex] = demand_kTon;

			} else {

				unservedDemandPerOD_kTon[odIndex] = 0.0;

				double distance_km = this.scenarioContainer.getDistance_km(odIndex);
				double[] utilities = new double[roundTripIndices.length];
				for (int i = 0; i < roundTripIndices.length; i++) {
					utilities[i] = -this.scale * distance_km * transportCostPerRT_1_km[roundTripIndices[i]];
				}

				double[] transportPerRT_kTon = transportPerODPerRT_kTon[odIndex];
				double[] choiceProbabilities = this.choiceModel.computeLogitProbabilities(utilities);
				for (int i = 0; i < roundTripIndices.length; i++) {
					transportPerRT_kTon[roundTripIndices[i]] = choiceProbabilities[i] * demand_kTon;
				}
			}
		}

		/*
		 * Evaluate supply model and resulting costs.
		 */

		for (int roundTripIndex = 0; roundTripIndex < roundTrips.size(); roundTripIndex++) {
			double maxDemand_kTon = 0.0;
			for (int odIndex : odCoverage.roundTripIndex2ConnectedODIndices.get(roundTripIndex)) {
				maxDemand_kTon = Math.max(maxDemand_kTon, transportPerODPerRT_kTon[odIndex][roundTripIndex]);
			}
			transportCostPerRT_1_km[roundTripIndex] = this.transportCost_1_kTonKm * maxDemand_kTon;
		}
		
		System.out.println("Unserved demand = " + Arrays.stream(unservedDemandPerOD_kTon).sum());
	}
}
