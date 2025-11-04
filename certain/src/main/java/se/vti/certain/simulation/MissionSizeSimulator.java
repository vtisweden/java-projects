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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import se.vti.certain.datastructures.IncidentType;
import se.vti.certain.datastructures.Mission;

public class MissionSizeSimulator {

	private final Random rnd;

	private final Map<IncidentType, List<Mission>> incidentType2historicalMissions = new LinkedHashMap<>();

	public MissionSizeSimulator(Random rnd) {
		this.rnd = rnd;
	}

	public MissionSizeSimulator addHistoricalMission(IncidentType incidentType, Mission mission) {
		this.incidentType2historicalMissions.computeIfAbsent(incidentType, it -> new ArrayList<>()).add(mission);
		return this;
	}

	private Mission resampleMission(IncidentType incidentType) {
		List<Mission> historicalMissions = this.incidentType2historicalMissions.get(incidentType);
		return historicalMissions.get(this.rnd.nextInt(historicalMissions.size()));
	}

	public void simulateSizes(List<Mission> missions) {
		for (Mission mission : missions) {
			Mission resampledMission = this.resampleMission(mission.getIncidentType());
			mission.setVehicleMissions(resampledMission.getVehicleMissions());
		}
	}

}
