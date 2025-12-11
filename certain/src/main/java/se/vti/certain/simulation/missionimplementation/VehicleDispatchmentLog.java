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
public class VehicleDispatchmentLog {

	public final VehicleRequestedEvent vehicleRequestedEvent;
	public final VehicleAvailability vehicleAvailability;

	public final double distanceToSite_km;
	public final double durationAtSite_h;
	public final double distanceBack_km;

	public final double initialSOC_kWh;
	public final double consumedOnWayToSite_kWh;
	public final double consumedAtSite_kWh;
	public final double comsumedOnWayBack_kWh;
	public final double finalStateOfCharge_kWh;

	public final double requestTime_h;
	public final double dispatchTime_h;
	public final double timeAtSite_h;
	public final double departFromSiteTime_h;
	public final double backAtStation_h;
	public final double againAvailable_h;

	public VehicleDispatchmentLog(VehicleRequestedEvent requestEvent, VehicleAvailability vehicleAvailability,
			Distances distances, double initialSOC_kWh, double minRelSOC) {

		this.vehicleRequestedEvent = requestEvent;
		this.vehicleAvailability = vehicleAvailability;
		Vehicle vehicle = vehicleAvailability.getVehicle();
		Zone from = vehicle.getStation().getZone();
		Zone to = vehicleAvailability.getDestination();

		this.distanceToSite_km = distances.computeDistance_km(from, to);
		this.durationAtSite_h = requestEvent.getVehicleMission().getDuration_h();
		this.distanceBack_km = distances.computeDistance_km(to, from);

		this.initialSOC_kWh = initialSOC_kWh;
		this.consumedOnWayToSite_kWh = distanceToSite_km * vehicle.getVehicleType().getEnergyNeed_kWh_per_km();
		this.consumedAtSite_kWh = durationAtSite_h * vehicle.getVehicleType().getEnergyNeedDuringMission_kW();
		this.comsumedOnWayBack_kWh = distanceBack_km * vehicle.getVehicleType().getEnergyNeed_kWh_per_km();
		this.finalStateOfCharge_kWh = this.initialSOC_kWh - consumedOnWayToSite_kWh - consumedAtSite_kWh
				- comsumedOnWayBack_kWh;

		this.requestTime_h = requestEvent.getRequestTime_h();
		this.dispatchTime_h = vehicleAvailability.getDepartureTimeFromStation_h();
		this.timeAtSite_h = vehicleAvailability.getArrivalTimeAtDestimation_h();
		this.departFromSiteTime_h = timeAtSite_h + requestEvent.getVehicleMission().getDuration_h();
		this.backAtStation_h = departFromSiteTime_h + distances.computeTravelTime_h(to, from, vehicle.getVehicleType());
		this.againAvailable_h = backAtStation_h
				+ (minRelSOC * vehicle.getVehicleType().getBatteryCapacity_kWh() - Math.max(0, finalStateOfCharge_kWh))
						/ vehicle.getVehicleType().getChargingRate_kW();
	}

}
