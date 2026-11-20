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

import java.util.function.Function;

import se.vti.certain.datastructures.Mission;
import se.vti.certain.simulation.missionimplementation.SystemState;
import se.vti.certain.simulation.missionimplementation.VehicleMissionLog;

/**
 * @author GunnarF
 */
public class AverageOverVehiclesObservable implements Function<SystemState, Double> {

	private final Function<VehicleMissionLog, Double> vehLog2Observable;

	private Function<Mission, Boolean> includeMission;

	private Function<VehicleMissionLog, Boolean> includeVehicleLog;

	public AverageOverVehiclesObservable(Function<VehicleMissionLog, Double> vehLog2Observable) {
		this.vehLog2Observable = vehLog2Observable;
		this.setIncludeMissions(m -> true).setIncludeVehicleLog(l -> true);
	}

	public AverageOverVehiclesObservable setIncludeMissions(Function<Mission, Boolean> includeMission) {
		this.includeMission = includeMission;
		return this;
	}

	public AverageOverVehiclesObservable setIncludeVehicleLog(Function<VehicleMissionLog, Boolean> includeVehicleLog) {
		this.includeVehicleLog = includeVehicleLog;
		return this;
	}

	@Override
	public Double apply(SystemState state) {
		double observableSum = 0;
		int observableCnt = 0;
		for (var missionAndLog : state.getMission2VehicleMissionLogs().entrySet()) {
			if (this.includeMission.apply(missionAndLog.getKey())) {
				for (VehicleMissionLog log : missionAndLog.getValue()) {
					if (this.includeVehicleLog.apply(log)) {
						observableSum += this.vehLog2Observable.apply(log);
						observableCnt++;
					}
				}
			}
		}
		return (observableCnt == 0) ? 0 : (observableSum / observableCnt);
	}
}
