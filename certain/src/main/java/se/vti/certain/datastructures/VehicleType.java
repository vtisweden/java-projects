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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author GunnarF
 *
 */
public class VehicleType extends HasId {

	// -------------------- MEMBERS --------------------

	private final double batteryCapacity_kWh;

	private final double energyNeed_kWh_per_km;

	private final double chargingRate_kW;

	private final double energyNeedDuringMission_kW;

	private final double maxSpeed_km_h;

	// -------------------- CONSTRUCTION --------------------

	@JsonCreator
	public VehicleType(@JsonProperty("id") String id, @JsonProperty("batteryCapacity_kWh") double batteryCapacity_kWh,
			@JsonProperty("energyNeed_kWh_per_km") double energyNeed_kWh_per_km,
			@JsonProperty("chargingRate_kW") double chargingRate_kW,
			@JsonProperty("energyNeedDuringMission_kW") double energyNeedDuringMission_kW,
			@JsonProperty("maxSpeed_km_h") double maxSpeed_km_h) {
		super(id);
		this.batteryCapacity_kWh = batteryCapacity_kWh;
		this.energyNeed_kWh_per_km = energyNeed_kWh_per_km;
		this.chargingRate_kW = chargingRate_kW;
		this.energyNeedDuringMission_kW = energyNeedDuringMission_kW;
		this.maxSpeed_km_h = maxSpeed_km_h;
	}

	// -------------------- JSON --------------------

	public static Map<String, VehicleType> readFromJsonFile(File file) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		VehicleType[] loadedVehicleTypes = mapper.readValue(file, VehicleType[].class);
		return new LinkedHashMap<>(
				Arrays.stream(loadedVehicleTypes).collect(Collectors.toMap(vt -> vt.getId(), vt -> vt)));
	}

	// -------------------- IMPLEMENTATION --------------------

	@JsonProperty("batteryCapacity_kWh")
	public double getBatteryCapacity_kWh() {
		return batteryCapacity_kWh;
	}

	@JsonProperty("energyNeed_kWh_per_km")
	public double getEnergyNeed_kWh_per_km() {
		return energyNeed_kWh_per_km;
	}

	@JsonProperty("chargingRate_kW")
	public double getChargingRate_kW() {
		return chargingRate_kW;
	}

	@JsonProperty("energyNeedDuringMission_kW")
	public double getEnergyNeedDuringMission_kW() {
		return energyNeedDuringMission_kW;
	}

	@JsonProperty("maxSpeed_km_h")
	public double getMaxSpeed_km_h() {
		return maxSpeed_km_h;
	}

	@Override
	public String toString() {
		return String.format(
				"%s[id=%s, batteryCapacity_kWh=%.2f, energyNeed_kWh_per_km=%.3f, chargingRate_kW=%.1f, maxSpeed_km_h=%.1f]",
				this.getClass().getSimpleName(), getId(), batteryCapacity_kWh, energyNeed_kWh_per_km, chargingRate_kW,
				maxSpeed_km_h);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

//	public static void main(String[] args) throws Exception {
//		SmallExample.writeVehicleTypes();
//		var vehicleTypes = SmallExample.readVehicleTypes();
//		for (var entry : vehicleTypes.entrySet()) {
//			System.out.println(entry);
//		}
//	}
}
