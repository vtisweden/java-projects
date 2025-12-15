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

import se.vti.certain.temporal.Season;
import se.vti.certain.temporal.TypeOfDay;
import se.vti.utils.misc.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class SimulationTimeLine {

	public final Season season;
	public final TypeOfDay dayType;
	public final int numberOfDays;

	public final List<Tuple<Double, Double>> nightIntervals_h;
	public final List<Tuple<Double, Double>> dayIntervals_h;

	public final double daylightSingleDay_h;
	public final double totalNight_h;
	public final double totalDay_h;

	public SimulationTimeLine(Season season, TypeOfDay dayType, double sunUpTime_h, double sunDownTime_h,
			int numberOfDays) {
		this.season = season;
		this.dayType = dayType;
		this.numberOfDays = numberOfDays;
		this.daylightSingleDay_h = sunDownTime_h - sunUpTime_h;

		List<Tuple<Double, Double>> tmpNightIntervals_h = new ArrayList<>(numberOfDays + 1);
		List<Tuple<Double, Double>> tmpDayIntervals_h = new ArrayList<>(numberOfDays);

		tmpNightIntervals_h.add(new Tuple<>(0.0, sunUpTime_h));
		double time_h = sunUpTime_h;
		for (int day = 0; day < numberOfDays - 1; day++) {
			tmpDayIntervals_h.add(new Tuple<>(time_h, time_h + daylightSingleDay_h));
			time_h += daylightSingleDay_h;
			tmpNightIntervals_h.add(new Tuple<>(time_h, time_h + (24.0 - daylightSingleDay_h)));
			time_h += 24.0 - daylightSingleDay_h;
		}
		tmpDayIntervals_h.add(new Tuple<>(time_h, time_h + daylightSingleDay_h));
		time_h += daylightSingleDay_h;
		tmpNightIntervals_h.add(new Tuple<>(time_h, time_h + 24.0 - sunDownTime_h));

		this.nightIntervals_h = Collections.unmodifiableList(tmpNightIntervals_h);
		this.dayIntervals_h = Collections.unmodifiableList(tmpDayIntervals_h);

		this.totalNight_h = tmpNightIntervals_h.stream().mapToDouble(n -> n.getB() - n.getA()).sum();
		this.totalDay_h = tmpDayIntervals_h.stream().mapToDouble(d -> d.getB() - d.getA()).sum();
	}
}
