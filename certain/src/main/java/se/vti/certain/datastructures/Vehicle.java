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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author GunnarF
 *
 */
@JsonPropertyOrder({ "id", "typeId", "stationId" })
public class Vehicle extends HasId {

	// -------------------- MEMBERS --------------------

	private final VehicleType type;

	private final Station station;

	// -------------------- JSON --------------------

	static class Raw {
		@JsonProperty("id")
		String id;
		@JsonProperty("typeId")
		String typeId;
		@JsonProperty("stationId")
		String stationId;
	}

	public static class JsonReader {

		private final File file;

		private Map<String, VehicleType> id2VehicleType;

		private Map<String, Station> id2Station;

		private Map<String, Vehicle> id2LoadedVehicle;

		private Set<String> vehicleTypeIdsWithoutInstances;

		private Set<String> stationIdsWithoutInstances;

		private Set<String> vehicleIdsWithoutInstances;

		public JsonReader(File file) {
			this.file = file;
		}

		public JsonReader setId2VehicleType(Map<String, VehicleType> id2VehicleType) {
			this.id2VehicleType = id2VehicleType;
			return this;
		}

		public JsonReader setId2Station(Map<String, Station> id2Station) {
			this.id2Station = id2Station;
			return this;
		}

		public Map<String, Vehicle> read() throws IOException {
			Raw[] rawVehicles = new ObjectMapper().readValue(file, Raw[].class);
			this.id2LoadedVehicle = new LinkedHashMap<>(rawVehicles.length);
			this.vehicleTypeIdsWithoutInstances = new LinkedHashSet<>();
			this.stationIdsWithoutInstances = new LinkedHashSet<>();
			this.vehicleIdsWithoutInstances = new LinkedHashSet<>();

			for (Raw rawVehicle : rawVehicles) {
				VehicleType vehicleType = this.id2VehicleType.get(rawVehicle.typeId);
				Station station = this.id2Station.get(rawVehicle.stationId);
				if ((vehicleType != null) && (station != null)) {
					this.id2LoadedVehicle.put(rawVehicle.id, new Vehicle(rawVehicle.id, vehicleType, station));
				} else {
					this.vehicleIdsWithoutInstances.add(rawVehicle.id);
					if (vehicleType == null) {
						this.vehicleTypeIdsWithoutInstances.add(rawVehicle.typeId);
					}
					if (station == null) {
						this.stationIdsWithoutInstances.add(rawVehicle.stationId);
					}
				}
			}
			return this.id2LoadedVehicle;
		}
	}

	public static Map<String, Vehicle> readFromJsonFile(File file, Map<String, VehicleType> id2VehicleType,
			Map<String, Station> id2Station) throws IOException {
		JsonReader reader = new JsonReader(file).setId2VehicleType(id2VehicleType).setId2Station(id2Station);
		reader.read();
		System.out.println("Vehicle type IDs without instances: " + reader.vehicleTypeIdsWithoutInstances);
		System.out.println("Station IDs without instances: " + reader.stationIdsWithoutInstances);
		System.out.println("Uninstantiated  vehicle IDs: " + reader.vehicleIdsWithoutInstances);
		return reader.id2LoadedVehicle;
	}

	// -------------------- CONSTRUCTION --------------------

	public Vehicle(String id, VehicleType type, Station station) {
		super(id);
		this.type = type;
		this.station = station;
	}

	// -------------------- IMPLEMENTATION --------------------

	@JsonProperty("typeId")
	private String getVehicleTypeId() {
		return this.type.getId();
	}

	@JsonProperty("stationId")
	private String getStationId() {
		return this.station.getId();
	}

	@JsonIgnore
	public VehicleType getVehicleType() {
		return type;
	}

	@JsonIgnore
	public Station getStation() {
		return station;
	}

	@Override
	public String toString() {
		return String.format("%s[id=%s, type=%s, station=%s]", this.getClass().getSimpleName(), this.getId(),
				this.type.getId(), this.station.getId());
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

//	public static void main(String[] args) throws StreamWriteException, DatabindException, IOException {
//		SmallExample.writeVehicles();
//		var id2Vehicle = SmallExample.readVehicles();
//		for (var entry : id2Vehicle.entrySet()) {
//			System.out.println(entry);
//		}
//	}
}
