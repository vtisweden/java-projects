/**
 * se.vti.certain.datastructures
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
package se.vti.certain.analysis.modifiers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.vti.certain.datastructures.VehicleType;

/**
 * @author GunnarF
 */
public class VehicleTypeModifier {

	private final Map<String, VehicleType> id2VehicleType;

	public VehicleTypeModifier(Map<String, VehicleType> id2VehicleType) {
		this.id2VehicleType = id2VehicleType;
	}

	/**
	 * Override to set scenario value.
	 */
	public double computeBatteryCapacity_kWh(VehicleType vehicleType) {
		return vehicleType.getBatteryCapacity_kWh();
	}

	/**
	 * Override to set scenario value.
	 */
	public double computeChargingRate_kW(VehicleType vehicleType) {
		return vehicleType.getChargingRate_kW();
	}

	/**
	 * Override to set scenario value.
	 */
	public double computeEnergyNeed_kWh_per_km(VehicleType vehicleType) {
		return vehicleType.getEnergyNeed_kWh_per_km();
	}

	/**
	 * Override to set scenario value.
	 */
	public double computeEnergyNeedDuringMission_kW(VehicleType vehicleType) {
		return vehicleType.getEnergyNeedDuringMission_kW();
	}

	/**
	 * Override to set scenario value.
	 */
	public double computeMaxSpeed_km_h(VehicleType vehicleType) {
		return vehicleType.getMaxSpeed_km_h();
	}

	public void createVehicleTypeFile(String fileName) throws StreamWriteException, DatabindException, IOException {
		List<VehicleType> scenarioVehicleTypes = new ArrayList<>(this.id2VehicleType.size());
		for (VehicleType vehicleType : this.id2VehicleType.values()) {
			VehicleType scenarioVehicleType = new VehicleType(vehicleType.getId(),
					this.computeBatteryCapacity_kWh(vehicleType), this.computeEnergyNeed_kWh_per_km(vehicleType),
					this.computeChargingRate_kW(vehicleType), this.computeEnergyNeedDuringMission_kW(vehicleType),
					this.computeMaxSpeed_km_h(vehicleType));
			scenarioVehicleTypes.add(scenarioVehicleType);
		}
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(fileName), scenarioVehicleTypes);
	}

	static String path = "./minimal/input/";
	static File vehicleTypeFile = new File(path + "vehicleTypes.json");

	public static void main(String[] args) throws IOException {
		System.out.println("STARTED ...");

		var id2VehicleType = VehicleType.readFromJsonFile(vehicleTypeFile);

		new VehicleTypeModifier(id2VehicleType) {
			@Override
			public double computeBatteryCapacity_kWh(VehicleType vehicleType) {
				return vehicleType.getBatteryCapacity_kWh();
			}
		}.createVehicleTypeFile("original.json");

		new VehicleTypeModifier(id2VehicleType) {
			@Override
			public double computeBatteryCapacity_kWh(VehicleType vehicleType) {
				return 2.0 * vehicleType.getBatteryCapacity_kWh();
			}
		}.createVehicleTypeFile("doubled.json");

		new VehicleTypeModifier(id2VehicleType) {
			@Override
			public double computeBatteryCapacity_kWh(VehicleType vehicleType) {
				return 4.0 * vehicleType.getBatteryCapacity_kWh();
			}
		}.createVehicleTypeFile("quadrupled.json");

		System.out.println("... DONE");

	}
}
