/**
 * se.vti.certain.analysis.observables
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
package se.vti.certain.analysis.observables;

import java.util.List;
import java.util.function.Function;

import se.vti.certain.simulation.missionimplementation.SystemState;
import se.vti.certain.simulation.missionimplementation.VehicleMissionLog;

/**
 * @author GunnarF
 */
public class AverageTimeFromVehicleRequestToArrival implements Function<SystemState, Double> {

	public static String NAME = "Avg. veh. delay [min]";
	
	@Override
	public Double apply(SystemState state) {
		double timeSum_h = 0.0;
		double cnt = 0.0;		
		for (List<VehicleMissionLog> listOfLogs : state.getMission2VehicleMissionLogs().values()) {
			for (VehicleMissionLog log : listOfLogs) {
				timeSum_h += (log.arrivalAtSite_h() - log.vehicleRequestedEvent.getRequestTime_h());
				cnt++;
			}
		}
		return (60.0 * timeSum_h / cnt);
	}

}
