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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.vti.certain.spatial.Zone;
import se.vti.certain.temporal.Season;
import se.vti.certain.temporal.TimeOfDay;
import se.vti.certain.temporal.TypeOfDay;

/**
 * 
 * @author GunnarF
 *
 */
@JsonPropertyOrder({ "incidentTypeId", "vehicleMissions", "zoneId", "timing", "startTime_h" })
public class Mission {

	// -------------------- MEMBERS --------------------

	private final IncidentType incidentType;

	private final Zone zone;

//	private Timing timing = null;
	private Season season = null;
	private TypeOfDay typeOfDay = null;
	private TimeOfDay timeOfDay = null;

	private final List<VehicleMission> vehicleMissions = new ArrayList<>();

	private Double startTime_h = null;

	// -------------------- JSON --------------------

	static class Raw {

		@JsonProperty
		String incidentTypeId;

		@JsonProperty
		String zoneId;

		@JsonProperty
		Season season;
		@JsonProperty
		TypeOfDay typeOfDay;
		@JsonProperty
		TimeOfDay timeOfDay;

		@JsonProperty
		Double startTime_h;

		@JsonProperty
		List<VehicleMission.Raw> vehicleMissions = new ArrayList<>();
	}

	public static class JsonReader {

		private final File file;

		private final boolean alsoLoadIncomplete;

		private Map<String, IncidentType> id2IncidentType;

		private Map<String, Zone> id2Zone;

		private Map<String, VehicleType> id2VehicleType;

		private List<Mission> loadedMissions;

		private Set<String> missingIncidentTypeIds;

		private Set<String> missingVehicleTypeIds;

		private int numberOfMissionsWithoutIncidentType;

		private int numberOfMissionsWithIncompleteFleet;

		private int numberOfUnloadedMissions;

		public JsonReader(File file, boolean alsoLoadIncomplete) {
			this.file = file;
			this.alsoLoadIncomplete = alsoLoadIncomplete;
		}

		public JsonReader setId2IncidentType(Map<String, IncidentType> id2IncidentType) {
			this.id2IncidentType = id2IncidentType;
			return this;
		}

		public JsonReader setId2Zone(Map<String, Zone> id2Zone) {
			this.id2Zone = id2Zone;
			return this;
		}

		public JsonReader setId2VehicleType(Map<String, VehicleType> id2VehicleType) {
			this.id2VehicleType = id2VehicleType;
			return this;
		}

		public List<Mission> read() throws IOException {
			Raw[] missionsRaw = new ObjectMapper().readValue(file, Raw[].class);
			this.loadedMissions = new ArrayList<>(missionsRaw.length);
			this.missingIncidentTypeIds = new LinkedHashSet<>();
			this.missingVehicleTypeIds = new LinkedHashSet<>();
			this.numberOfMissionsWithoutIncidentType = 0;
			this.numberOfMissionsWithIncompleteFleet = 0;
			this.numberOfUnloadedMissions = 0;

			for (Raw missionRaw : missionsRaw) {

				IncidentType incidentType = this.id2IncidentType.get(missionRaw.incidentTypeId);

				List<VehicleMission> vehicleMissions = new ArrayList<>(missionRaw.vehicleMissions.size());
				for (VehicleMission.Raw vehicleMissionRaw : missionRaw.vehicleMissions) {
					VehicleType vehicleType = this.id2VehicleType.get(vehicleMissionRaw.vehicleTypeId);
					if (vehicleType != null) {
						vehicleMissions.add(new VehicleMission(vehicleType, vehicleMissionRaw.offset_h,
								vehicleMissionRaw.duration_h));
					} else {
						this.missingVehicleTypeIds.add(vehicleMissionRaw.vehicleTypeId);
					}
				}
				if (vehicleMissions.size() < missionRaw.vehicleMissions.size()) {
					this.numberOfMissionsWithIncompleteFleet++;
				}

//				if (incidentType != null) {
//				Mission mission = new Mission(incidentType, this.id2Zone.get(missionRaw.zoneId))
//						.setSeason(missionRaw.season).setTypeOfDay(missionRaw.typeOfDay)
//						.setTimeOfDay(missionRaw.timeOfDay).setStartTime_h(missionRaw.startTime_h);
//				vehicleMissions.forEach(vm -> mission.addVehicleMission(vm));
//				this.loadedMissions.add(mission);
//			} else {
//				this.numberOfUnloadedMissions++;
//				if (incidentType == null) {
//					this.missingIncidentTypeIds.add(missionRaw.incidentTypeId);
//				}
//			}

				if (incidentType == null) {
					if (this.alsoLoadIncomplete) {
						incidentType = new IncidentType(
								missionRaw.incidentTypeId + "_missing" + this.numberOfMissionsWithoutIncidentType)
								.clearTiming();
					}
					this.numberOfMissionsWithoutIncidentType++;
					this.missingIncidentTypeIds.add(missionRaw.incidentTypeId);
				} // Incident type may have changed!
				if (incidentType != null) {
					Mission mission = new Mission(incidentType, this.id2Zone.get(missionRaw.zoneId))
							.setSeason(missionRaw.season).setTypeOfDay(missionRaw.typeOfDay)
							.setTimeOfDay(missionRaw.timeOfDay).setStartTime_h(missionRaw.startTime_h);
					vehicleMissions.forEach(vm -> mission.addVehicleMission(vm));
					this.loadedMissions.add(mission);
				}

			}
			return this.loadedMissions;
		}

	}

	public static List<Mission> readFromJson(File file, Map<String, IncidentType> id2IncidentType,
			Map<String, Zone> id2Zone, Map<String, VehicleType> id2VehicleType, boolean alsoLoadIncomplete)
			throws IOException {
		JsonReader reader = new JsonReader(file, alsoLoadIncomplete).setId2IncidentType(id2IncidentType)
				.setId2Zone(id2Zone).setId2VehicleType(id2VehicleType);
		reader.read();
		System.out.println("---------- LOADED MISSIONS ----------");
		System.out.println("Incident type IDs without instances: " + reader.missingIncidentTypeIds);
		System.out.println("Vehicle type IDs without instances: " + reader.missingVehicleTypeIds);
		System.out.println("Number of incomplete (not loaded) missions: " + reader.numberOfUnloadedMissions);
		System.out.println("Number of missions without incident type: " + reader.numberOfMissionsWithoutIncidentType);
		System.out.println("Number of missions with incomplete fleet: " + reader.numberOfMissionsWithIncompleteFleet);
		System.out.println("Number of loaded missions:" + reader.loadedMissions.size());
		System.out.println();
		return reader.loadedMissions;

//		Raw[] missionsRaw = new ObjectMapper().readValue(file, Raw[].class);
//		List<Mission> missions = new ArrayList<>(missionsRaw.length);
//		for (Raw missionRaw : missionsRaw) {
//			Mission mission = new Mission(id2IncidentType.get(missionRaw.incidentTypeId),
//					id2Zone.get(missionRaw.zoneId));
//			mission.setTiming(missionRaw.timing);
//			mission.setStartTime_h(missionRaw.startTime_h);
//			for (VehicleMission.Raw vehicleMissionRaw : missionRaw.vehicleMissions) {
//				mission.addVehicleMission(new VehicleMission(id2VehicleType.get(vehicleMissionRaw.vehicleTypeId),
//						vehicleMissionRaw.offset_h, vehicleMissionRaw.duration_h));
//			}
//			missions.add(mission);
//		}
//		return missions;
	}

	// -------------------- CONSTRUCTION --------------------

	public Mission(IncidentType incidentType, Zone zone) {
		this.incidentType = incidentType;
		this.zone = zone;
	}

	// -------------------- IMPLEMENTATION --------------------

	@JsonProperty("incidentTypeId")
	private String getIncidentTypeId() {
		return this.incidentType.getId();
	}

	@JsonProperty("zoneId")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String getZoneId() {
		return this.zone == null ? null : this.zone.getId();
	}

//	@JsonProperty("timing")
//	@JsonInclude(JsonInclude.Include.NON_NULL)
//	public Timing getTiming() {
//		return this.timing == null ? null : this.timing;
//	}

	@JsonProperty("season")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Season getSeason() {
		return this.season;
	}

	@JsonProperty("typeOfDay")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public TypeOfDay getTypeOfDay() {
		return this.typeOfDay;
	}

	@JsonProperty("timeOfDay")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public TimeOfDay getTimeOfDay() {
		return this.timeOfDay;
	}

	@JsonProperty("vehicleMissions")
	public List<VehicleMission> getVehicleMissions() {
		return this.vehicleMissions;
	}

	@JsonProperty("startTime_h")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Double getStartTime_h() {
		return this.startTime_h;
	}

	@JsonIgnore
	public IncidentType getIncidentType() {
		return incidentType;
	}

	@JsonIgnore
	public Zone getZone() {
		return zone;
	}

//	@JsonIgnore
//	public Mission setTiming(Timing timing) {
//		this.timing = timing;
//		return this;
//	}

	@JsonIgnore
	public Mission setSeason(Season season) {
		this.season = season;
		return this;
	}

	@JsonIgnore
	public Mission setTypeOfDay(TypeOfDay typeOfDay) {
		this.typeOfDay = typeOfDay;
		return this;
	}

	@JsonIgnore
	public Mission setTimeOfDay(TimeOfDay timeOfDay) {
		this.timeOfDay = timeOfDay;
		return this;
	}

	@JsonIgnore
	public Mission setStartTime_h(Double startTime_h) {
		this.startTime_h = startTime_h;
		return this;
	}

	public Mission addVehicleMission(VehicleMission vehicleMission) {
		this.vehicleMissions.add(vehicleMission);
		return this;
	}

	public Mission setVehicleMissions(List<VehicleMission> vehicleMissions) {
		this.vehicleMissions.clear();
		for (VehicleMission vehicleMission : vehicleMissions) {
			this.addVehicleMission(vehicleMission);
		}
		return this;
	}

	@Override
	public String toString() {
		return String.format(
				"%s[type=%s, location=%s, season=%s, typeOfDay=%s, timeOfDay=%s, startTime_h=%.2f, vehicleMissions=%s]",
				this.getClass().getSimpleName(), this.incidentType.getId(), this.getZoneId(), this.season,
				this.typeOfDay, this.timeOfDay, this.startTime_h, this.vehicleMissions);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

//	public static void main(String[] args) throws Exception {		
//		SmallExample.writeAnonymousMissions();
//		var anonymousMissions = SmallExample.readAnonymousMissions();
//		for (Mission mission : anonymousMissions) {
//			System.out.println(mission);
//		}
//	}
}
