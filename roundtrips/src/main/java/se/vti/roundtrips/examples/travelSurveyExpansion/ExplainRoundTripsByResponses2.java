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
import se.vti.roundtrips.samplingweights.misc.calibration.SurveySmoothingWeight;
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
class ExplainRoundTripsByResponses2 implements MHWeight<MultiRoundTrip<GridNodeWithActivity>> {

	private final SurveySmoothingWeight<GridNodeWithActivity> surveySmoothingWeight;

	ExplainRoundTripsByResponses2(List<SurveyResponse> responses, List<Person> syntheticPopulation) {
		this.surveySmoothingWeight = new SurveySmoothingWeight<GridNodeWithActivity>(syntheticPopulation.size(),
				responses.size());
		for (int agentIndex = 0; agentIndex < syntheticPopulation.size(); agentIndex++) {
			for (int respondentIndex = 0; respondentIndex < responses.size(); respondentIndex++) {
				this.surveySmoothingWeight.setAgentSimilarity(agentIndex, respondentIndex,
						responses.get(respondentIndex).matchesRespondentWeight(syntheticPopulation.get(agentIndex)));
			}
		}
		this.surveySmoothingWeight.setMovementSimilarityFunction((rndTr, rspInd) -> {
			return responses.get(rspInd).matchesResponseWeight(rndTr);
		});
	}

	@Override
	public double logWeight(MultiRoundTrip<GridNodeWithActivity> multiRoundTrip) {
		return this.surveySmoothingWeight.logWeight(multiRoundTrip);
	}
}
