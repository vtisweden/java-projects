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
package se.vti.certain.datastructures;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 
 * @author GunnarF
 *
 */
@JsonPropertyOrder({ "vehicleTypeId", "offset_h", "duration_h" })
public class VehicleMission {

	// -------------------- MEMBERS --------------------

	private final VehicleType vehicleType;

	private final double offset_h;

	private final double duration_h;

	// -------------------- JSON --------------------

	static class Raw {
		@JsonProperty
		String vehicleTypeId;
		@JsonProperty
		double offset_h;
		@JsonProperty
		double duration_h;
	}

	// -------------------- CONSTRUCTION --------------------

	public VehicleMission(VehicleType vehicleType, double offset_h, double duration_h) {
		this.vehicleType = vehicleType;
		this.offset_h = offset_h;
		this.duration_h = duration_h;
	}

	// -------------------- IMPLEMENTATION --------------------

	@JsonProperty("vehicleTypeId")
	private String getVehicleTypeId() {
		return this.vehicleType.getId();
	}

	@JsonIgnore
	public VehicleType getVehicleType() {
		return this.vehicleType;
	}

	@JsonProperty
	public double getOffset_h() {
		return this.offset_h;
	}

	@JsonProperty
	public double getDuration_h() {
		return this.duration_h;
	}

	@Override
	public String toString() {
		return String.format("%s[vehicleType=%s, offset=%.2fh, duration=%.2fh]", this.getClass().getSimpleName(),
				this.vehicleType.getId(), this.offset_h, this.duration_h);
	}
}
