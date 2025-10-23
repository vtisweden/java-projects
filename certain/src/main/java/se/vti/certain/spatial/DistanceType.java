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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.vti.certain.datastructures.HasId;
import se.vti.certain.examples.small.SmallExample;

/**
 * 
 * @author GunnarF
 *
 */
public class DistanceType extends HasId {

	// -------------------- MEMBERS --------------------

	private final double maxSpeed_km_h;

	// -------------------- JSON --------------------

	public static Map<String, DistanceType> readFromJsonFile(File file) throws IOException {
		DistanceType[] loadedDistanceTypes = new ObjectMapper().readValue(file, DistanceType[].class);
		return new LinkedHashMap<>(
				Arrays.stream(loadedDistanceTypes).collect(Collectors.toMap(dt -> dt.getId(), dt -> dt)));
	}

	// -------------------- CONSTRUCTION --------------------

	@JsonCreator
	public DistanceType(@JsonProperty("id") String id, @JsonProperty("maxSpeed_km_h") double maxSpeed_km_h) {
		super(id);
		this.maxSpeed_km_h = maxSpeed_km_h;
	}

	// -------------------- IMPLEMENTATION --------------------

	@JsonProperty
	public double getMaxSpeed_km_h() {
		return maxSpeed_km_h;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": id=" + this.getId() + ", maxSpeed_km_h=" + this.getMaxSpeed_km_h();
	}
	
	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws StreamWriteException, DatabindException, IOException {
		SmallExample.writeDistanceTypes();
		var id2DistanceType = SmallExample.readDistanceTypes();
		for (var entry : id2DistanceType.entrySet()) {
			System.out.println(entry);
		}
	}
	
}
