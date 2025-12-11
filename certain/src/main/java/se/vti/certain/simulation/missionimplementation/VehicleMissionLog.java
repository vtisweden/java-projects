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

import se.vti.certain.datastructures.Vehicle;
import se.vti.certain.spatial.Distances;
import se.vti.certain.spatial.Zone;

/**
 * 
 * @author GunnarF
 *
 */
public class VehicleMissionLog {

	public final VehicleRequestedEvent vehicleRequestedEvent;
	private final VehicleAvailability vehicleAvailability;

	public final double initialSOC_kWh;

	public final double distanceToSite_km;
	public final double distanceBack_km;

	public final double drivingTimeToSite_h;
	public final double drivingTimeBackToStation_h;
	public final double againAvailable_h;

	public Vehicle vehicle() {
		return this.vehicleAvailability.getVehicle();
	}

	public double durationAtSite_h() {
		return this.vehicleRequestedEvent.getVehicleMission().getDuration_h();
	}

	public double consumedOnWayToSite_kWh() {
		return this.distanceToSite_km * vehicle().getVehicleType().getEnergyNeed_kWh_per_km();
	}

	public double consumedAtSite_kWh() {
		return durationAtSite_h() * vehicle().getVehicleType().getEnergyNeedDuringMission_kW();
	}

	public double consumedOnWayBack_kWh() {
		return this.distanceBack_km * vehicle().getVehicleType().getEnergyNeed_kWh_per_km();
	}

	public double finalStateOfCharge_kWh() {
		return this.initialSOC_kWh - consumedOnWayToSite_kWh() - consumedAtSite_kWh() - consumedOnWayBack_kWh();
	}

	public double arrivalAtSite_h() {
		return this.vehicleAvailability.getArrivalTimeAtDestination_h();
	}

	public double departureFromSiteTime_h() {
		return this.vehicleAvailability.getArrivalTimeAtDestination_h()
				+ vehicleRequestedEvent.getVehicleMission().getDuration_h();
	}

	public double backAtStation_h() {
		return departureFromSiteTime_h() + this.drivingTimeBackToStation_h;
	}

	public Zone stationZone() {
		return vehicle().getStation().getZone();
	}

	public Zone siteZone() {
		return this.vehicleAvailability.getDestination();
	}

	public VehicleMissionLog(VehicleRequestedEvent requestEvent, VehicleAvailability vehicleAvailability,
			Distances distances, double initialSOC_kWh, double minRelSOC) {

		this.vehicleRequestedEvent = requestEvent;
		this.vehicleAvailability = vehicleAvailability;

		this.initialSOC_kWh = initialSOC_kWh;

		this.distanceToSite_km = distances.computeDistance_km(stationZone(), siteZone());
		this.distanceBack_km = distances.computeDistance_km(siteZone(), stationZone());

		this.drivingTimeToSite_h = distances.computeTravelTime_h(stationZone(), siteZone(), vehicle().getVehicleType());
		this.drivingTimeBackToStation_h = distances.computeTravelTime_h(siteZone(), stationZone(),
				vehicle().getVehicleType());
		this.againAvailable_h = backAtStation_h() + (minRelSOC * vehicle().getVehicleType().getBatteryCapacity_kWh()
				- Math.max(0, finalStateOfCharge_kWh())) / vehicle().getVehicleType().getChargingRate_kW();
	}

}
