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
import se.vti.certain.datastructures.VehicleMission;

/**
 * 
 * @author GunnarF
 *
 */
public class MissionVehicleDeploymentSimulator {

	private final Random rnd;

	private final Map<IncidentType, List<Mission>> incidentType2PrototypeMissions;

	public MissionVehicleDeploymentSimulator(List<Mission> prototypeMissions, Random rnd) {
		this.rnd = rnd;
		this.incidentType2PrototypeMissions = new LinkedHashMap<>();
		for (Mission prototypeMission : prototypeMissions) {
			this.incidentType2PrototypeMissions
					.computeIfAbsent(prototypeMission.getIncidentType(), t -> new ArrayList<>()).add(prototypeMission);
		}
	}

	public void simulateFleets(List<Mission> missions) {
		for (Mission mission : missions) {
			List<Mission> prototypeMissions = this.incidentType2PrototypeMissions.get(mission.getIncidentType());
			if ((prototypeMissions != null) && (prototypeMissions.size() > 0)) {
				Mission prototypeMission = prototypeMissions.get(this.rnd.nextInt(prototypeMissions.size()));
				for (VehicleMission vehicleMission : prototypeMission.getVehicleMissions()) {
					mission.addVehicleMission(vehicleMission);
				}
			}			
		}
	}
	
	public List<Mission> getMissionsWithDeployedVehicles(List<Mission> allMissions) {
		return new ArrayList<>(allMissions.stream().filter(m -> m.getVehicleMissions().size() > 0).toList());
	}

	public List<Mission> getMissionsWithoutStartTimes(List<Mission> allMissions) {
		return new ArrayList<>(allMissions.stream().filter(m -> m.getVehicleMissions().size() == 0).toList());
	}
}
