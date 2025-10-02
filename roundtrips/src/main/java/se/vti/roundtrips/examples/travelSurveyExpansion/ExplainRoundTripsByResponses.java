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

import java.util.Arrays;
import java.util.List;

import se.vti.roundtrips.examples.activityExpandedGridNetwork.GridNodeWithActivity;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * This weight function requires that *each* round trip is explained by a survey
 * response.
 * 
 * For an alternative, not well working approach, see
 * {@link ExplainResponsesByRoundTrips}.
 * 
 * @author GunnarF
 *
 */
class ExplainRoundTripsByResponses implements MHWeight<MultiRoundTrip<GridNodeWithActivity>> {

	private final List<SurveyResponse> responses;

	private final double[][] personResponseWeights;

	ExplainRoundTripsByResponses(List<SurveyResponse> responses, List<Person> syntheticPopulation) {
		this.responses = responses;
		// Precomputing the weights for speed.
		this.personResponseWeights = new double[syntheticPopulation.size()][responses.size()];
		for (int personIndex = 0; personIndex < syntheticPopulation.size(); personIndex++) {
			double[] weights = this.personResponseWeights[personIndex];
			for (int responseIndex = 0; responseIndex < responses.size(); responseIndex++) {
				weights[responseIndex] = this.responses.get(responseIndex)
						.matchesRespondentWeight(syntheticPopulation.get(personIndex));
			}
			double sum = Arrays.stream(weights).sum();
			if (sum < 1e-8) {
				throw new RuntimeException("Synthetic person does not match any response.");
			}
			for (int responseIndex = 0; responseIndex < responses.size(); responseIndex++) {
				weights[responseIndex] /= sum;
			}
		}
	}

	@Override
	public double logWeight(MultiRoundTrip<GridNodeWithActivity> multiRoundTrip) {
		double result = 0.0;
		// This double loop is slow. The inner loop could be parallelized.
		for (int personIndex = 0; personIndex < multiRoundTrip.size(); personIndex++) {
			double[] weights = this.personResponseWeights[personIndex];
			double personWeight = 0.0;
			for (int responseIndex = 0; responseIndex < this.responses.size(); responseIndex++) {
				personWeight += weights[responseIndex] * this.responses.get(responseIndex)
						.matchesResponseWeight(multiRoundTrip.getRoundTrip(personIndex));
			}
			result += Math.log(Math.max(1e-8, personWeight));
		}
		return result;
	}
}
