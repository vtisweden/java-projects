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
package se.vti.certain.simulation.missionimplementation;

import java.util.ArrayList;
import java.util.List;

import se.vti.certain.datastructures.Mission;
import se.vti.certain.datastructures.VehicleMission;

/**
 * 
 * @author GunnarF
 *
 */
public class IncidentHappensEvent extends Event {

	private final Mission mission;

	public IncidentHappensEvent(Mission mission) {
		super(mission.getStartTime_h());
		this.mission = mission;
	}

	@Override
	public List<Event> process(SystemState systemState) {
		List<Event> newEvents = new ArrayList<>();
		for (VehicleMission vehicleMission : this.mission.getVehicleMissions()) {
			newEvents.add(new VehicleRequestedEvent(this.mission, vehicleMission, this.mission.getZone()));
		}
		return newEvents;
	}
	
	@Override
	public String toString() {
		return String.format("%s[mission=%s", this.getClass().getSimpleName(), this.mission);
	}

}
