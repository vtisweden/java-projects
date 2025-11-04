/**
 * se.vti.certain
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
package se.vti.certain.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import se.vti.certain.datastructures.Mission;
import se.vti.certain.temporal.TimeOfDay;
import se.vti.utils.misc.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class StartTimeSimulator {

	private final SimulationTimeLine timeLine;

	private final Random rnd;

	public StartTimeSimulator(SimulationTimeLine timeLine, Random rnd) {
		this.timeLine = timeLine;
		this.rnd = rnd;
	}

	private Double simulateStartTime_h(TimeOfDay timeOfDay) {
		final List<Tuple<Double, Double>> intervals_h;
		final double totalTime_h;
		if (TimeOfDay.DAY == timeOfDay) {
			intervals_h = this.timeLine.dayIntervals_h;
			totalTime_h = this.timeLine.totalDay_h;
		} else if (TimeOfDay.NIGHT == timeOfDay) {
			intervals_h = this.timeLine.nightIntervals_h;
			totalTime_h = this.timeLine.totalNight_h;
		} else {
			throw new UnsupportedOperationException("Unkown: " + timeOfDay);
		}

		double u = this.rnd.nextDouble() * totalTime_h;
		double sum_h = 0.0;
		for (Tuple<Double, Double> interval_h : intervals_h) {
			double intervalSize_h = interval_h.getB() - interval_h.getA();
			if (sum_h <= u && sum_h + intervalSize_h > u) {
				return (interval_h.getA() + this.rnd.nextDouble() * intervalSize_h);
			}
			sum_h += intervalSize_h;
		}
		return null;
	}

	/**
	 * Takes a list of missions containing IncidentType, Location, Timing, and adds
	 * start time information, if possible.
	 */
	public void simulateStarTimes(List<Mission> missions) {
		for (Mission mission : missions) {
			if (mission.getTimeOfDay() != null) {
				mission.setStartTime_h(this.simulateStartTime_h(mission.getTimeOfDay()));
			}
		}
	}

	public List<Mission> getStartTimeSortedMissions(List<Mission> allMissions) {
		List<Mission> result = new ArrayList<>(allMissions.stream().filter(m -> m.getStartTime_h() != null).toList());
		Collections.sort(result, (m1, m2) -> Double.compare(m1.getStartTime_h(), m2.getStartTime_h()));
		return result;
	}

	public List<Mission> getMissionsWithoutStartTimes(List<Mission> allMissions) {
		return new ArrayList<>(allMissions.stream().filter(m -> m.getStartTime_h() == null).toList());
	}
}
