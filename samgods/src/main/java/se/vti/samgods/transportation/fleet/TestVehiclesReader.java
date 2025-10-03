/**
 * se.vti.samgods.transportation.fleet
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
package se.vti.samgods.transportation.fleet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.common.SamgodsConstants;

/**
 * This is not a junit test. It can be used to test if concrete vehicle files
 * are correctly read into the model system.
 * 
 * @author GunnarF
 *
 */
public class TestVehiclesReader {

	private String pathName = null;

	private final Map<SamgodsConstants.TransportMode, String> mode2vehicleParametersFileName = new LinkedHashMap<>();
	private final Map<SamgodsConstants.TransportMode, String> mode2transferParametersFileName = new LinkedHashMap<>();

	public TestVehiclesReader() {
	}

	public TestVehiclesReader setPathName(String pathName) {
		this.pathName = pathName;
		return this;
	}

	public TestVehiclesReader setVehicleFileNames(SamgodsConstants.TransportMode mode, String vehicleParameterFileName,
			String transferParameterFileName) {
		this.mode2vehicleParametersFileName.put(mode, vehicleParameterFileName);
		this.mode2transferParametersFileName.put(mode, transferParameterFileName);
		return this;
	}

	private Path getFile(String fileName) {
		if (fileName == null) {
			return null;
		} else {
			return fileName != null ? Paths.get(this.pathName, fileName) : Paths.get(fileName);
		}
	}

	public void run() throws IOException {
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		VehiclesReader reader = new VehiclesReader(vehicles);
		for (var mode : SamgodsConstants.TransportMode.values()) {
			if (this.mode2vehicleParametersFileName.containsKey(mode)
					&& this.mode2transferParametersFileName.containsKey(mode)) {
				reader.load_v12(this.getFile(this.mode2vehicleParametersFileName.get(mode)).toString(),
						this.getFile(this.mode2transferParametersFileName.get(mode)).toString(), mode);
			}
		}
		FleetStatsTable table = new FleetStatsTable(vehicles);

		for (var mode : SamgodsConstants.TransportMode.values()) {
			System.out.println();
			System.out.println("VEHICLE PARAMETERS " + mode + " FROM "
					+ this.getFile(this.mode2vehicleParametersFileName.get(mode)));
			System.out.println(table.createVehicleTypeTable(mode));
			System.out.println();
			System.out.println("TRANSFER PARAMETERS " + mode + " FROM "
					+ this.getFile(this.mode2transferParametersFileName.get(mode)));
			System.out.println(table.createVehicleTransferCostTable(mode));
		}
	}

	public static void main(String[] args) throws Exception {
		new TestVehiclesReader().setPathName("./input_2024/")
				.setVehicleFileNames(SamgodsConstants.TransportMode.Air, "vehicleparameters_air.csv",
						"transferparameters_air.csv")
				.setVehicleFileNames(SamgodsConstants.TransportMode.Rail, "vehicleparameters_rail.csv",
						"transferparameters_rail.csv")
				.setVehicleFileNames(SamgodsConstants.TransportMode.Road, "vehicleparameters_road.csv",
						"transferparameters_road.csv")
				.setVehicleFileNames(SamgodsConstants.TransportMode.Sea, "vehicleparameters_sea.csv",
						"transferparameters_sea.csv")
				.run();
	}
}
