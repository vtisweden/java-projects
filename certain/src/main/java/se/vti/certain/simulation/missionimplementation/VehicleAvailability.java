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
import se.vti.certain.spatial.Zone;

/**
 * 
 * @author GunnarF
 *
 */
public class VehicleAvailability {

	private final Vehicle vehicle;

	private final Zone destination;

	private final double departureTimeFromStation_h;

	private final double arrivalTimeAtDestination_h;
	
	private VehicleMissionLog anticipatedDispatchmentLog = null;
	
	public VehicleAvailability(Vehicle vehicle, Zone destination, double departurTimeFromStation_h,
			double arrivalTimeAtDestination_h) {
		this.vehicle = vehicle;
		this.destination = destination;
		this.departureTimeFromStation_h = departurTimeFromStation_h;
		this.arrivalTimeAtDestination_h = arrivalTimeAtDestination_h;
	}
	
	public Vehicle getVehicle() {
		return this.vehicle;
	}
	
	public Zone getDestination() {
		return this.destination;
	}
	
	public double getDepartureTimeFromStation_h() {
		return this.departureTimeFromStation_h;
	}
	
	public double getArrivalTimeAtDestination_h() {
		return this.arrivalTimeAtDestination_h;
	}

	public VehicleMissionLog getAnticipatedDispatchmentLog() {
		return anticipatedDispatchmentLog;
	}

	public void setAnticipatedDispatchmentLog(VehicleMissionLog anticipatedDispatchmentLog) {
		this.anticipatedDispatchmentLog = anticipatedDispatchmentLog;
	}

}
