/**
 * se.vti.roundtrips.examples.activityExpansion
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
package se.vti.roundtrips.examples.activityTimeUse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.vti.roundtrips.examples.activityExpandedGridNetwork.Activity;
import se.vti.roundtrips.examples.activityExpandedGridNetwork.GridNodeWithActivity;
import se.vti.roundtrips.simulator.Episode;
import se.vti.roundtrips.simulator.MoveEpisode;
import se.vti.roundtrips.simulator.StayEpisode;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.Tuple;
import se.vti.utils.misc.metropolishastings.MHAbstractStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
class PlotTimeUseHistogram extends MHAbstractStateProcessor<RoundTrip<GridNodeWithActivity>> {

	private Map<Activity, long[]> activity2histogram;

	public PlotTimeUseHistogram(long burnInIterations, long samplingInterval) {
		super(burnInIterations, samplingInterval);
		this.activity2histogram = new LinkedHashMap<>();
		this.activity2histogram.put(Activity.HOME, new long[24]);
		this.activity2histogram.put(Activity.WORK, new long[24]);
		this.activity2histogram.put(Activity.OTHER, new long[24]);
	}

	@Override
	public void processStateHook(RoundTrip<GridNodeWithActivity> roundTrip) {
		List<Episode> episodes = roundTrip.getEpisodes();
		for (int i = 0; i < episodes.size(); i += 2) {
			var stay = (StayEpisode<GridNodeWithActivity>) episodes.get(i);
			long[] hist = this.activity2histogram.get(stay.getLocation().getActivity());
			for (Tuple<Double, Double> interval_h : stay.effectiveIntervals(24.0)) {
				for (int hour = interval_h.getA().intValue(); hour < interval_h.getB(); hour++) {
					hist[hour]++;
				}
			}
		}
	}

	@Override
	public void end() {
		super.end();
		System.out.println("----------------------------------------");
		System.out.println("hour\thome\twork\tother");
		for (int hour = 0; hour < 24; hour++) {
			System.out.println(hour + "\t" + this.activity2histogram.get(Activity.HOME)[hour] + "\t"
					+ this.activity2histogram.get(Activity.WORK)[hour] + "\t"
					+ this.activity2histogram.get(Activity.OTHER)[hour]);
		}
		System.out.println("----------------------------------------");
	}
}
