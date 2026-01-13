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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.vti.certain.examples.small.SmallExample;
import se.vti.certain.temporal.Season;
import se.vti.certain.temporal.TimeOfDay;
import se.vti.certain.temporal.TypeOfDay;

/**
 * 
 * @author GunnarF
 *
 */
public class IncidentType extends HasId {

	// -------------------- MEMBERS --------------------

	@JsonProperty("season2Weight")
	private final Map<Season, Double> season2Weight;

	@JsonProperty("typeOfDay2Weight")
	private final Map<TypeOfDay, Double> typeOfDay2Weight;

	@JsonProperty("timeOfDay2Weight")
	private final Map<TimeOfDay, Double> timeOfDay2Weight;

	// -------------------- CACHING --------------------

	@JsonIgnore
	private Double averageSeasonWeight = null;

	@JsonIgnore
	private Double averageTypeOfDayWeight = null;

	@JsonIgnore
	private Double averageTimeOfDayWeight = null;

	private void recalculateAverageWeights() {
		this.averageSeasonWeight = this.season2Weight.values().stream().mapToDouble(w -> w).average().stream().boxed()
				.findFirst().orElse(null);
		this.averageTypeOfDayWeight = this.typeOfDay2Weight.values().stream().mapToDouble(w -> w).average().stream()
				.boxed().findFirst().orElse(null);
		this.averageTimeOfDayWeight = this.timeOfDay2Weight.values().stream().mapToDouble(w -> w).average().stream()
				.boxed().findFirst().orElse(null);
	}

	// -------------------- JSON --------------------

	public static Map<String, IncidentType> readFromJsonFile(File file)
			throws StreamReadException, DatabindException, IOException {
		IncidentType[] incidentTypes = new ObjectMapper().readValue(file, IncidentType[].class);
		return Arrays.stream(incidentTypes).collect(Collectors.toMap(t -> t.getId(), t -> t));
	}

	// -------------------- CONSTRUCTION --------------------

	public IncidentType(String id) {
		super(id);
		this.season2Weight = new LinkedHashMap<>(
				Arrays.stream(Season.values()).collect(Collectors.toMap(s -> s, s -> 1.0)));
		this.typeOfDay2Weight = new LinkedHashMap<>(
				Arrays.stream(TypeOfDay.values()).collect(Collectors.toMap(t -> t, t -> 1.0)));
		this.timeOfDay2Weight = new LinkedHashMap<>(
				Arrays.stream(TimeOfDay.values()).collect(Collectors.toMap(t -> t, t -> 1.0)));
		this.recalculateAverageWeights();
	}

	@JsonCreator
	private IncidentType(@JsonProperty("id") String id,
			@JsonProperty("season2Weight") Map<Season, Double> season2Weight,
			@JsonProperty("typeOfDay2Weight") Map<TypeOfDay, Double> typeOfDay2Weight,
			@JsonProperty("timeOfDay2Weight") Map<TimeOfDay, Double> timeOfday2Weight) {
		super(id);
		this.season2Weight = season2Weight;
		this.typeOfDay2Weight = typeOfDay2Weight;
		this.timeOfDay2Weight = timeOfday2Weight;
		this.recalculateAverageWeights();
	}

	// -------------------- IMPLEMENTATION --------------------

	public IncidentType clearTiming() {
		this.season2Weight.clear();
		this.typeOfDay2Weight.clear();
		this.timeOfDay2Weight.clear();
		this.recalculateAverageWeights();
		return this;
	}

	@JsonIgnore // use constructor
	public IncidentType setWeight(Season season, double weight) {
		this.season2Weight.put(season, weight);
		this.averageSeasonWeight = this.season2Weight.values().stream().mapToDouble(w -> w).average().stream().boxed()
				.findFirst().orElse(null);
		return this;
	}

	@JsonIgnore // use constructor
	public IncidentType setWeight(TypeOfDay typeOfDay, double weight) {
		this.typeOfDay2Weight.put(typeOfDay, weight);
		this.averageTypeOfDayWeight = this.typeOfDay2Weight.values().stream().mapToDouble(w -> w).average().stream()
				.boxed().findFirst().orElse(null);
		return this;
	}

	@JsonIgnore // use constructor
	public IncidentType setWeight(TimeOfDay timeOfDay, double weight) {
		this.timeOfDay2Weight.put(timeOfDay, weight);
		this.averageTimeOfDayWeight = this.timeOfDay2Weight.values().stream().mapToDouble(w -> w).average().stream()
				.boxed().findFirst().orElse(null);
		return this;
	}

	@JsonIgnore
	public double getRelativeWeight(Season season) {
		if (this.averageSeasonWeight < 1e-8) {
			return 0.0;
		}
		return this.season2Weight.getOrDefault(season, 0.0) / this.averageSeasonWeight;
	}

	@JsonIgnore
	public double getRelativeWeight(TypeOfDay typeOfDay) {
		if (this.averageTypeOfDayWeight < 1e-8) {
			return 0.0;
		}
		return this.typeOfDay2Weight.getOrDefault(typeOfDay, 0.0) / this.averageTypeOfDayWeight;
	}

	@JsonIgnore
	public double getRelativeWeight(TimeOfDay timeOfDay) {
		if (this.averageTimeOfDayWeight < 1e-8) {
			return 0.0;
		}
		return this.timeOfDay2Weight.getOrDefault(timeOfDay, 0.0) / this.averageTimeOfDayWeight;
	}

	@JsonIgnore
	public double getDayProba(double daytime_h) {
		double dayWeight = daytime_h * this.timeOfDay2Weight.getOrDefault(TimeOfDay.DAY, 0.0);
		double nightWeight = (24.0 - daytime_h) * this.timeOfDay2Weight.getOrDefault(TimeOfDay.NIGHT, 0.0);
//		if (dayWeight < 1e-8 && nightWeight < 1e-8) {
//			return 0.0;
//		} 
		return dayWeight / (dayWeight + nightWeight);
	}

	@Override
	public String toString() {
		return String.format("%s[id=%s, season2Weight=%s, typeOfDay2Weight=%s, timeOfDay2Weight=%s]",
				this.getClass().getSimpleName(), this.getId(), this.season2Weight, this.typeOfDay2Weight,
				this.timeOfDay2Weight);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws Exception {
		SmallExample.writeIncidentTypes();
		var id2IncidentType = SmallExample.readIncidentTypes();
		for (var entry : id2IncidentType.entrySet()) {
			System.out.println(entry);
		}
	}
}
