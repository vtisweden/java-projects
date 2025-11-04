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
package se.vti.certain.spatial;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import se.vti.certain.datastructures.VehicleType;

/**
 * 
 * @author GunnarF
 *
 */
public class DistanceValue {

	// -------------------- MEMBERS --------------------

	private final DistanceType distanceType;

	private final double distance_km;

	// -------------------- JSON --------------------

	static class Raw {
		@JsonProperty("distanceTypeId")
		String distanceTypeId;
		@JsonProperty("distance_km")
		double distance_km;

		Raw() {
		}

		Raw(DistanceValue distanceValue) {
			this.distanceTypeId = distanceValue.getDistanceType().getId();
			this.distance_km = distanceValue.getDistance_km();
		}
	}

	// -------------------- CONSTRUCTION --------------------

	@JsonCreator
	public DistanceValue(@JsonProperty("distanceType") DistanceType distanceType,
			@JsonProperty("distance_km") double distance_km) {
		this.distanceType = distanceType;
		this.distance_km = distance_km;
	}

	// -------------------- IMPLEMENTATION --------------------

	@JsonProperty("distanceTypeId")
	private String getDistanceTypeId() {
		return this.distanceType.getId();
	}

	@JsonIgnore
	public DistanceType getDistanceType() {
		return distanceType;
	}

	@JsonProperty
	public double getDistance_km() {
		return distance_km;
	}

	@JsonIgnore
	public double getMinTravelTime_h() {
		return this.distance_km / this.distanceType.getMaxSpeed_km_h();
	}

	@JsonIgnore
	public double getMinTravelTime_h(VehicleType vehicleType) {
		return this.distance_km
				/ Math.min(vehicleType.getMaxSpeed_km_h(), this.distanceType.getMaxSpeed_km_h());
	}

	@Override
	public String toString() {
		return String.format("%s[type=%s, distance_km=%.2f", this.getClass().getSimpleName(), this.distanceType.getId(),
				this.distance_km);
	}
}
