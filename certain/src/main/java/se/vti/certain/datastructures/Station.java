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
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.vti.certain.spatial.Distances;
import se.vti.certain.spatial.Zone;

/**
 * 
 * @author GunnarF
 *
 */
@JsonPropertyOrder({ "id", "zoneId" })
public class Station extends HasId {

	// -------------------- MEMBERS --------------------

	private final Zone zone;

	// -------------------- JSON --------------------

	static class Raw {
		@JsonProperty
		String id;
		@JsonProperty
		String zoneId;
	}

	public static class JsonReader {

		private final File file;

		private Map<String, Zone> id2Zone;

		private Distances distances;

		private Map<String, Station> id2LoadedStation;

		private Set<String> zoneIdsWithoutInstance;

		private Set<String> unloadedStationIds;

		private Set<String> stationsIdsThatCannotReachAllZones;

		private Set<String> stationsIdsThatCannotBeReachedFromAllZones;

		public JsonReader(File file) {
			this.file = file;
		}

		public JsonReader setId2Zone(Map<String, Zone> id2Zone) {
			this.id2Zone = id2Zone;
			return this;
		}

		public JsonReader setDistances(Distances distances) {
			this.distances = distances;
			return this;
		}

		private boolean canReachAllOtherZones(Zone zone) {
			for (Zone otherZone : this.id2Zone.values()) {
				if (this.distances.getDistances(zone, otherZone) == null) {
					return false;
				}
			}
			return true;
		}

		private boolean canBeReachedFromAllOtherZones(Zone zone) {
			for (Zone otherZone : this.id2Zone.values()) {
				if (this.distances.getDistances(otherZone, zone) == null) {
					return false;
				}
			}
			return true;
		}

		public Map<String, Station> read() throws StreamReadException, DatabindException, IOException {
			Raw[] rawStations = new ObjectMapper().readValue(file, Raw[].class);
			this.id2LoadedStation = new LinkedHashMap<>(rawStations.length);
			this.zoneIdsWithoutInstance = new LinkedHashSet<>();
			this.unloadedStationIds = new LinkedHashSet<>();
			this.stationsIdsThatCannotReachAllZones = new LinkedHashSet<>();
			this.stationsIdsThatCannotBeReachedFromAllZones = new LinkedHashSet<>();

			for (Raw rawStation : rawStations) {
				Zone zone = this.id2Zone.get(rawStation.zoneId);
				if (zone != null) {
					this.id2LoadedStation.put(rawStation.id, new Station(rawStation.id, zone));
					boolean canReachAllOtherZones = this.canReachAllOtherZones(zone);
					boolean canBeReachedFromAllOtherZones = this.canBeReachedFromAllOtherZones(zone);
					if (!canReachAllOtherZones) {
						this.stationsIdsThatCannotReachAllZones.add(rawStation.id);
					}
					if (!canBeReachedFromAllOtherZones) {
						this.stationsIdsThatCannotBeReachedFromAllZones.add(rawStation.id);
					}
				} else {
					this.unloadedStationIds.add(rawStation.id);
					this.zoneIdsWithoutInstance.add(rawStation.zoneId);
				}
			}
			return this.id2LoadedStation;
		}
	}

	public static Map<String, Station> readFromJsonFile(File file, Map<String, Zone> id2Zone, Distances distances)
			throws IOException {
		JsonReader reader = new JsonReader(file).setId2Zone(id2Zone).setDistances(distances);
		reader.read();
		System.out.println("STATIONS");
		System.out.println("Zone IDs without instance: " + reader.zoneIdsWithoutInstance);
		System.out.println("Unloaded station IDs: " + reader.unloadedStationIds);
		System.out.println("Station IDs that cannot reach all zones: " + reader.stationsIdsThatCannotReachAllZones);
		System.out.println("Station IDs that cannot be reached from all zones: "
				+ reader.stationsIdsThatCannotBeReachedFromAllZones);
		System.out.println("Loaded station IDs: " + reader.id2LoadedStation.keySet());
		System.out.println();

		return reader.id2LoadedStation;

//		Raw[] rawStations = new ObjectMapper().readValue(file, Raw[].class);
//		return new LinkedHashMap<>(Arrays.stream(rawStations)
//				.collect(Collectors.toMap(rs -> rs.id, rs -> new Station(rs.id, id2Zone.get(rs.zoneId)))));
	}

	// -------------------- CONSTRUCTION --------------------

	public Station(String id, Zone zone) {
		super(id);
		this.zone = zone;
	}

	// -------------------- IMPLEMENTATION --------------------

	@JsonProperty
	private String getZoneId() {
		return this.zone.getId();
	}

	@JsonIgnore
	public Zone getZone() {
		return zone;
	}

	@Override
	public String toString() {
		return String.format("%s[id=%s, zone=%s]", this.getClass().getSimpleName(), this.getId(), this.zone.getId());
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

//	public static void main(String[] args) throws StreamWriteException, DatabindException, IOException {
//		SmallExample.writeStations();
//		var id2Station = SmallExample.readStations();
//		for (var entry : id2Station.entrySet()) {
//			System.out.println(entry);
//		}
//	}
}
