/**
 * se.vti.roundtrips.simulator
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
package se.vti.roundtrips.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import se.vti.utils.misc.Tuple;
import se.vti.utils.misc.math.MathHelpers;

/**
 * @author GunnarF
 */
public class IntervalUtils {

	private final double periodLength_h;

	public IntervalUtils(double periodLength_h) {
		this.periodLength_h = periodLength_h;
	}

	/**
	 * If episodes comes out of a physically correct simulation, then the result of
	 * this function is a list of non-overlapping intervals. Not necessarily
	 * time-ordered, though, because of time wrap-around.
	 */
	public List<Tuple<Double, Double>> extractIntervals(List<Episode> episodes,
			Function<Episode, Boolean> testIfEpisodeIsConsidered) {
		ArrayList<Tuple<Double, Double>> intervals = new ArrayList<>(episodes.size());
		for (Episode episode : episodes) {
			if (testIfEpisodeIsConsidered.apply(episode)) {
				intervals.addAll(episode.effectiveIntervals(this.periodLength_h));
			}
		}
		return intervals;
	}

	/**
	 * Requires the entries *within* each interval list argument to be non-overlapping.
	 * This is guaranteed if the lists have been computed by extractIntervals(...)
	 */
	public double computeOverlap_h(List<Tuple<Double, Double>> intervals1, List<Tuple<Double, Double>> intervals2) {
		double result_h = 0.0;
		for (Tuple<Double, Double> int1 : intervals1) {
			for (Tuple<Double, Double> int2 : intervals2) {
				result_h += MathHelpers.overlap(int1.getA(), int1.getB(), int2.getA(), int2.getB());
			}
		}
		return result_h;
	}
}
