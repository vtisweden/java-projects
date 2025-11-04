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

import java.util.List;
import java.util.Random;

import se.vti.certain.datastructures.Mission;
import se.vti.certain.temporal.TimeOfDay;

/**
 * 
 * @author GunnarF
 *
 */
public class TimingSimulator {

	private final SimulationTimeLine timeLine;

	private final Random rnd;

	public TimingSimulator(SimulationTimeLine timeLine, Random rnd) {
		this.timeLine = timeLine;
		this.rnd = rnd;
	}

	/**
	 * Takes a list of missions containing only IncidentType and Location and adds
	 * timing information, if possible.
	 */
	public void simulateTimings(List<Mission> missions) {
		for (Mission mission : missions) {
			mission.setSeason(this.timeLine.season).setTypeOfDay(this.timeLine.dayType);
			if (this.rnd.nextDouble() < mission.getIncidentType().getShare(TimeOfDay.DAY)) {
				mission.setTimeOfDay(TimeOfDay.DAY);
			} else {
				mission.setTimeOfDay(TimeOfDay.NIGHT);
			}
		}
	}
}
