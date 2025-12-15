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
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.random.Well19937c;

import se.vti.certain.datastructures.IncidentType;
import se.vti.certain.datastructures.Mission;
import se.vti.certain.spatial.Zone;

public class IncidentSimulator {

	private final SimulationTimeLine timeLine;

	private final RandomDataGenerator rdg;

	public IncidentSimulator(SimulationTimeLine timeLine, long seed) {
		this.timeLine = timeLine;
		this.rdg = new RandomDataGenerator(new Well19937c(seed));
	}

	/**
	 * Creates a list of missions containing only IncidentType and Location.
	 */
	public List<Mission> simulateMissions(Map<String, Zone> id2Zone) {
		ArrayList<Mission> missions = new ArrayList<>();
		for (Zone zone : id2Zone.values()) {
			for (Map.Entry<IncidentType, Double> entry : zone.getIncidentType2Intensity_1_yr().entrySet()) {
				IncidentType incidentType = entry.getKey();
				double intensity_1_yr = incidentType.getRelativeWeight(this.timeLine.season)
						* incidentType.getRelativeWeight(this.timeLine.dayType) * entry.getValue();
				if (intensity_1_yr >= 1e-12) {
					double expectedNumber = intensity_1_yr * this.timeLine.numberOfDays / 365.0;
					long numberOfIncidents = this.rdg.nextPoisson(expectedNumber);
					for (long i = 0; i < numberOfIncidents; i++) {
						missions.add(new Mission(incidentType, zone));
					}
				}
			}
		}
		return missions;
	}
}
