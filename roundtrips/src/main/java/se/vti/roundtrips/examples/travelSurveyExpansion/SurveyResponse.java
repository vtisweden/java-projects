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

import java.util.List;

import se.vti.roundtrips.examples.activityExpandedGridNetwork.Activity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.GridNodeWithActivity;
import se.vti.roundtrips.simulator.Episode;
import se.vti.roundtrips.simulator.StayEpisode;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
class SurveyResponse {

	private final Person respondent;

	private final double reportedWork_h;
	private final double reportedEdu_h;
	private final double reportedOther_h;

	SurveyResponse(Person respondent, double reportedWork_h, double reportedEdu_h, double reportedOther_h) {
		this.respondent = respondent;
		this.reportedWork_h = reportedWork_h;
		this.reportedEdu_h = reportedEdu_h;
		this.reportedOther_h = reportedOther_h;
	}

	double matchesRespondentWeight(Person syntheticPerson) {
		if (this.respondent.inStudentAge() && syntheticPerson.inStudentAge()
				|| this.respondent.inWorkingAge() && syntheticPerson.inWorkingAge()
				|| this.respondent.inRetirementAge() && syntheticPerson.inRetirementAge()) {
			return 1.0;
		} else {
			return 1e-8;
		}
	}

	private double timeWeight(double reported_h, double simulated_h) {
		if (reported_h >= 1.0) {
			if (simulated_h >= 1.0) {
				return 1.0;
			} else {
				return 1e-8;
			}
		} else {
			if (simulated_h >= 1.0) {
				return 1e-8;
			} else {
				return 1.0;
			}
		}
	}

	double matchesResponseWeight(RoundTrip<GridNodeWithActivity> simulatedRoundTrip) {

		List<Episode> episodes = simulatedRoundTrip.getEpisodes();
		double simWork_h = 0.0;
		double simEdu_h = 0.0;
		double simOther_h = 0.0;
		for (int i = 0; i < episodes.size(); i += 2) {
			var stay = (StayEpisode<GridNodeWithActivity>) episodes.get(i);
			var activity = stay.getLocation().getActivity();
			var duration_h = stay.getDuration_h();
			if (Activity.WORK.equals(activity)) {
				simWork_h += duration_h;
			} else if (Activity.EDUCATION.equals(activity)) {
				simEdu_h += duration_h;
			} else if (Activity.OTHER.equals(activity)) {
				simOther_h += duration_h;
			}
		}

		return this.timeWeight(this.reportedWork_h, simWork_h) * this.timeWeight(this.reportedEdu_h, simEdu_h)
				* this.timeWeight(this.reportedOther_h, simOther_h);
	}
}
