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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.vti.certain.datastructures.Mission;
import se.vti.certain.datastructures.Vehicle;
import se.vti.certain.datastructures.VehicleType;
import se.vti.certain.spatial.Distances;

/**
 * 
 * @author GunnarF
 *
 */
public class SystemState {

	private final double relSOCWhenAvailable;

	private final Map<VehicleType, List<Vehicle>> vehicleType2Vehicles = new LinkedHashMap<>();

	// contains exactly those times when vehicles are a available at SOC level
	// this.relSOCWhenAvailable
	private final Map<Vehicle, Double> vehicle2Availability_h = new LinkedHashMap<>();

	private final Distances distances;

	private final Map<Mission, List<VehicleMissionLog>> mission2VehicleDispatchmentLog = new LinkedHashMap<>();

	private final List<VehicleRequestedEvent> failedRequests = new ArrayList<>();

	public SystemState(Map<String, Vehicle> id2Vehicle, Distances distances, double relSOCWhenAvailable) {
		this.relSOCWhenAvailable = relSOCWhenAvailable;
		for (Vehicle vehicle : id2Vehicle.values()) {
			VehicleType vehicleType = vehicle.getVehicleType();
			this.vehicleType2Vehicles.computeIfAbsent(vehicleType, t -> new ArrayList<>()).add(vehicle);
			this.vehicle2Availability_h.put(vehicle, -(1.0 - relSOCWhenAvailable) * vehicleType.getBatteryCapacity_kWh()
					/ vehicleType.getChargingRate_kW());
		}
		this.distances = distances;
	}

	public VehicleAvailability reserveNextAvailableVehicle(VehicleRequestedEvent vehicleRequestedEvent) {
		VehicleAvailability bestAvailability = null;
		for (Vehicle candidateVehicle : this.vehicleType2Vehicles
				.get(vehicleRequestedEvent.getVehicleMission().getVehicleType())) {

			if (this.distances.connects(candidateVehicle.getStation().getZone(),
					vehicleRequestedEvent.getMission().getZone())) {
				double candidateDepartureFromStation_h = Math.max(vehicleRequestedEvent.getRequestTime_h(),
						this.vehicle2Availability_h.get(candidateVehicle));
				double candidateArrivalAtDestination_h = candidateDepartureFromStation_h + this.distances
						.computeTravelTimeFromStation_h(candidateVehicle, vehicleRequestedEvent.getMission().getZone());
				if ((bestAvailability == null)
						|| (candidateArrivalAtDestination_h < bestAvailability.getArrivalTimeAtDestination_h())) {
					bestAvailability = new VehicleAvailability(candidateVehicle,
							vehicleRequestedEvent.getMission().getZone(), candidateDepartureFromStation_h,
							candidateArrivalAtDestination_h);
				}
			}
		}

		if (bestAvailability != null) {
			Vehicle vehicle = bestAvailability.getVehicle();

			// Possibly incomplete charging level when first ready for mission.
			double initialSOC_kWh = this.relSOCWhenAvailable * vehicle.getVehicleType().getBatteryCapacity_kWh();
			// Possibly larger (and capacity-unconstrained) charging level when requested.
			initialSOC_kWh += Math.max(0.0,
					bestAvailability.getDepartureTimeFromStation_h() - this.vehicle2Availability_h.get(vehicle))
					* vehicle.getVehicleType().getChargingRate_kW();
			// Capacity-constrained charging level when requested for mission.
			initialSOC_kWh = Math.min(initialSOC_kWh, vehicle.getVehicleType().getBatteryCapacity_kWh());

			VehicleMissionLog anticipatedLog = new VehicleMissionLog(vehicleRequestedEvent, bestAvailability,
					this.distances, initialSOC_kWh, this.relSOCWhenAvailable);
			bestAvailability.setAnticipatedDispatchmentLog(anticipatedLog);
			this.vehicle2Availability_h.put(bestAvailability.getVehicle(), anticipatedLog.againAvailable_h);
			this.getMission2VehicleDispachmentLog()
					.computeIfAbsent(vehicleRequestedEvent.getMission(), m -> new ArrayList<>()).add(anticipatedLog);
		} else {
			this.failedRequests.add(vehicleRequestedEvent);
		}

		return bestAvailability;
	}

	public Map<Mission, List<VehicleMissionLog>> getMission2VehicleDispachmentLog() {
		return this.mission2VehicleDispatchmentLog;
	}

	public List<VehicleRequestedEvent> getFailedRequests() {
		return failedRequests;
	}
}
