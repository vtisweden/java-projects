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

import se.vti.roundtrips.examples.activityExpandedGridNetwork.Activity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.GridNodeWithActivity;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.simulator.Episode;
import se.vti.roundtrips.simulator.StayEpisode;
import se.vti.utils.misc.metropolishastings.MHAbstractStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
class PlotAgeByActivityHistogram extends MHAbstractStateProcessor<MultiRoundTrip<GridNodeWithActivity>> {

	private final List<Person> syntheticPopulation;

	private long[] min1HourEduAges;
	private long[] min1HourWorkAges;
	private long[] min1HourOtherAges;

	PlotAgeByActivityHistogram(long burnInIterations, long samplingInterval, List<Person> syntheticPopulation) {
		super(burnInIterations, samplingInterval);
		this.syntheticPopulation = syntheticPopulation;
		this.min1HourEduAges = new long[10];
		this.min1HourWorkAges = new long[10];
		this.min1HourOtherAges = new long[10];
	}

	@Override
	public void processStateHook(MultiRoundTrip<GridNodeWithActivity> multiRoundTrip) {
		for (var roundTrip : multiRoundTrip) {

			List<Episode> episodes = roundTrip.getEpisodes();
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

			int ageBin = this.syntheticPopulation.get(roundTrip.getIndex()).age / 10;
			if (simWork_h >= 1.0) {
				this.min1HourWorkAges[ageBin]++;
			}
			if (simEdu_h >= 1.0) {
				this.min1HourEduAges[ageBin]++;
			}
			if (simOther_h >= 1.0) {
				this.min1HourOtherAges[ageBin]++;
			}
		}
	}

	@Override
	public void end() {
		super.end();
		double workSum = Arrays.stream(this.min1HourWorkAges).sum();
		double eduSum = Arrays.stream(this.min1HourEduAges).sum();
		double otherSum = Arrays.stream(this.min1HourOtherAges).sum();
		System.out.println("----------------------------------------");
		System.out.println("age\tfreq(work)\tfreq(education)\tfreq(other)");
		for (int ageBin = 1; ageBin < 10; ageBin++) {
			System.out.println(10 * ageBin + " to " + 10 * (ageBin + 1) + "\t" + this.min1HourWorkAges[ageBin] / workSum
					+ "\t" + this.min1HourEduAges[ageBin] / eduSum + "\t" + min1HourOtherAges[ageBin] / otherSum);
		}
		System.out.println("----------------------------------------");
	}
}
