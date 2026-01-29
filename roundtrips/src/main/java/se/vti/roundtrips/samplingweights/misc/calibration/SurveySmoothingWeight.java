/**
 * se.vti.roundtrips.samplingweights.misc.surveysmoother
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
package se.vti.roundtrips.samplingweights.misc.calibration;

import java.util.Arrays;
import java.util.function.BiFunction;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * @author GunnarF
 */
public class SurveySmoothingWeight<N extends Node> implements MHWeight<MultiRoundTrip<N>> {

	private final double[][] agentRespondentWeights;

	private boolean weightsAreNormalized = false;

	private BiFunction<RoundTrip<N>, Integer, Double> movementSimilarityFunction;

	public SurveySmoothingWeight(int numberOfAgents, int numberOfRespondents) {
		this.agentRespondentWeights = new double[numberOfAgents][numberOfRespondents];
	}

	public SurveySmoothingWeight<N> setAgentSimilarity(int agentIndex, int respondentIndex, double similarity) {
		if (this.weightsAreNormalized) {
			throw new RuntimeException("Too late to set agent similarities.");
		}
		this.agentRespondentWeights[agentIndex][respondentIndex] = similarity;
		return this;
	}

	public SurveySmoothingWeight<N> setMovementSimilarityFunction(
			BiFunction<RoundTrip<N>, Integer, Double> movementSimilarityFunction) {
		this.movementSimilarityFunction = movementSimilarityFunction;
		return this;
	}

	private void normalizeWeights() {
		for (double[] weights : this.agentRespondentWeights) {
			double sum = Arrays.stream(weights).sum();
			if (sum < 1e-8) {
				throw new RuntimeException("Agent does not match any response.");
			}
			for (int respondentIndex = 0; respondentIndex < weights.length; respondentIndex++) {
				weights[respondentIndex] /= sum;
			}
		}
	}

	@Override
	public double logWeight(MultiRoundTrip<N> multiRoundTrip) {
		if (!this.weightsAreNormalized) {
			this.normalizeWeights();
		}
		double logWeight = 0.0;
		// This double loop is slow. The inner loop could be parallelized.
		for (int agentIndex = 0; agentIndex < multiRoundTrip.size(); agentIndex++) {
			double[] weights = this.agentRespondentWeights[agentIndex];
			double agentWeight = 0.0;
			for (int respondentIndex = 0; respondentIndex < weights.length; respondentIndex++) {
				agentWeight += weights[respondentIndex] * this.movementSimilarityFunction
						.apply(multiRoundTrip.getRoundTrip(agentIndex), respondentIndex);
			}
			logWeight += Math.log(Math.max(1e-8, agentWeight));
		}
		return logWeight;
	}
}
