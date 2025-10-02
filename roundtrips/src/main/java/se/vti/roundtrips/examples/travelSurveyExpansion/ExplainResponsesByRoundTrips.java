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
 * This weight function does not work well: For any response, it is enough to
 * have one round trip that explains this response, but this does not keep
 * possibly many other round trips to behave completely incompatible with the
 * survey.
 * 
 * For a better approach, see {@link ExplainRoundTripsByResponses}.
 * 
 * @deprecated
 * @author GunnarF
 *
 */
class ExplainResponsesByRoundTrips implements MHWeight<MultiRoundTrip<GridNodeWithActivity>> {

	private final List<SurveyResponse> responses;

	private final double[][] responsePersonWeights;

	ExplainResponsesByRoundTrips(List<SurveyResponse> responses, List<Person> syntheticPopulation) {
		this.responses = responses;
		// Precomputing the person weights for speed.
		this.responsePersonWeights = new double[responses.size()][syntheticPopulation.size()];
		for (int responseIndex = 0; responseIndex < responses.size(); responseIndex++) {
			SurveyResponse response = this.responses.get(responseIndex);
			double[] weights = this.responsePersonWeights[responseIndex];
			for (int personIndex = 0; personIndex < syntheticPopulation.size(); personIndex++) {
				weights[personIndex] = response.matchesRespondentWeight(syntheticPopulation.get(personIndex));
			}
			double sum = Arrays.stream(weights).sum();
			if (sum < 1e-8) {
				throw new RuntimeException("Response does not match the synthetic population.");
			}
			for (int personIndex = 0; personIndex < syntheticPopulation.size(); personIndex++) {
				weights[personIndex] /= sum;
			}
		}
	}

	@Override
	public double logWeight(MultiRoundTrip<GridNodeWithActivity> multiRoundTrip) {
		double result = 0.0;
		// This double loop is slow. The inner loop could be parallelized.
		for (int responseIndex = 0; responseIndex < this.responses.size(); responseIndex++) {
			SurveyResponse response = this.responses.get(responseIndex);
			double[] weights = this.responsePersonWeights[responseIndex];
			double responseWeight = 0.0;
			for (int personIndex = 0; personIndex < weights.length; personIndex++) {
				responseWeight += weights[personIndex]
						* response.matchesResponseWeight(multiRoundTrip.getRoundTrip(personIndex));
			}
			result += Math.log(Math.max(1e-8, responseWeight));
		}
		return result;
	}
}
