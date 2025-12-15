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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.vti.certain.datastructures.HasId;
import se.vti.certain.datastructures.IncidentType;

/**
 * 
 * @author GunnarF
 *
 */
public class Zone extends HasId {

	// -------------------- MEMBERS --------------------

	@JsonIgnore
	private final Map<IncidentType, Double> incidentType2Intensity_1_yr = new LinkedHashMap<>();

	// -------------------- JSON --------------------

	static class Raw extends HasId {

		Map<String, Double> incidentTypeId2Intensity_1_yr;

		@JsonCreator
		Raw(@JsonProperty("id") String id,
				@JsonProperty("incidentTypeId2Intensity_1_yr") Map<String, Double> incidentTypeId2Intensity_1_yr) {
			super(id);
			this.incidentTypeId2Intensity_1_yr = incidentTypeId2Intensity_1_yr;
		}
	}

	public static class JsonReader {

		private final File file;

		private Map<String, IncidentType> id2IncidentType;

		private Map<String, Zone> loadedId2Zone;

		private Set<String> incidentTypeIdsWithoutInstances;

		private Set<Zone> zonesWithMissingIncidents;

		public JsonReader(File file) {
			this.file = file;
		}

		public JsonReader setId2IncidentType(Map<String, IncidentType> id2IncidentType) {
			this.id2IncidentType = id2IncidentType;
			return this;
		}

		public Map<String, Zone> read() throws IOException {
			Raw[] zonesRaw = new ObjectMapper().readValue(file, Raw[].class);
			this.loadedId2Zone = new LinkedHashMap<>(zonesRaw.length);
			this.incidentTypeIdsWithoutInstances = new LinkedHashSet<>();
			this.zonesWithMissingIncidents = new LinkedHashSet<>();

			for (Raw zoneRaw : zonesRaw) {
				Zone zone = new Zone(zoneRaw.getId());
				for (Map.Entry<String, Double> entry : zoneRaw.incidentTypeId2Intensity_1_yr.entrySet()) {
					String incidentTypeId = entry.getKey();
					IncidentType incidentType = id2IncidentType.get(incidentTypeId);
					if (incidentType != null) {
						double intensity_1_yr = entry.getValue();
						zone.setIncidentIntensity(incidentType, intensity_1_yr);
					} else {
						this.incidentTypeIdsWithoutInstances.add(incidentTypeId);
						this.zonesWithMissingIncidents.add(zone);
					}
				}
				this.loadedId2Zone.put(zone.getId(), zone);
			}
			return this.loadedId2Zone;
		}
	}

	public static Map<String, Zone> readFromJsonFile(File file, Map<String, IncidentType> id2IncidentType)
			throws StreamReadException, DatabindException, IOException {
		JsonReader reader = new JsonReader(file).setId2IncidentType(id2IncidentType);
		reader.read();
		System.out.println("Incident type IDs without instances: " + reader.incidentTypeIdsWithoutInstances);
		System.out.println("Zones with missing incidents: "
				+ reader.zonesWithMissingIncidents.stream().map(z -> z.getId()).toList());
		return reader.loadedId2Zone;
	}

	// -------------------- CONSTRUCTION --------------------

	public Zone(String id) {
		super(id);
	}

	// -------------------- IMPLEMENTATION --------------------

	@JsonProperty("incidentTypeId2Intensity_1_yr")
	private Map<String, Double> getIncidentTypeId2Intensity_1_yr() {
		return this.incidentType2Intensity_1_yr.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().getId(), e -> e.getValue()));
	}

	@JsonIgnore
	public Zone setIncidentIntensity(IncidentType incidentType, double intensity_1_yr) {
		this.incidentType2Intensity_1_yr.put(incidentType, intensity_1_yr);
		return this;
	}

	@JsonIgnore
	public Map<IncidentType, Double> getIncidentType2Intensity_1_yr() {
		return this.incidentType2Intensity_1_yr;
	}

	@Override
	public String toString() {
		return String.format("%s[id=%s, incidentIntensities=%s]", this.getClass().getSimpleName(), this.getId(),
				this.incidentType2Intensity_1_yr.toString());
	}
}
