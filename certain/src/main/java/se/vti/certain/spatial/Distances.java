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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.vti.certain.datastructures.Vehicle;
import se.vti.certain.datastructures.VehicleType;
import se.vti.utils.misc.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class Distances {

	// -------------------- MEMBERS --------------------

	private final Map<Zone, Map<Zone, List<DistanceValue>>> zone2Zone2Distances;

	// -------------------- JSON --------------------

	static class Raw {
		@JsonProperty("zone2Zone2Distances")
		Map<String, Map<String, List<DistanceValue.Raw>>> zoneId2ZoneId2DistancesRaw;
	}

	public static Distances readFromJsonFile(File file, Map<String, Zone> id2Zone,
			Map<String, DistanceType> id2DistanceType) throws StreamReadException, DatabindException, IOException {
		Raw distancesRaw = new ObjectMapper().readValue(file, Raw.class);
		Distances distances = new Distances();
		for (Map.Entry<String, Map<String, List<DistanceValue.Raw>>> entry1 : distancesRaw.zoneId2ZoneId2DistancesRaw
				.entrySet()) {
			Zone from = id2Zone.get(entry1.getKey());
			for (Map.Entry<String, List<DistanceValue.Raw>> entry2 : entry1.getValue().entrySet()) {
				Zone to = id2Zone.get(entry2.getKey());
				List<DistanceValue.Raw> distanceValuesRaw = entry2.getValue();
				for (DistanceValue.Raw distanceValueRaw : distanceValuesRaw) {
					distances.add(from, to, new DistanceValue(id2DistanceType.get(distanceValueRaw.distanceTypeId),
							distanceValueRaw.distance_km));
				}
			}
		}
		return distances;
	}

	// -------------------- CONSTRUCTION --------------------

	public Distances() {
		this.zone2Zone2Distances = new LinkedHashMap<>();
	}

	// -------------------- IMPLEMENTATION --------------------

	@JsonProperty("zone2Zone2Distances")
	private Map<String, Map<String, List<DistanceValue.Raw>>> getZone2Zone2DistancesRaw() {
		Map<String, Map<String, List<DistanceValue.Raw>>> from2To2DistancesRaw = new LinkedHashMap<>();
		for (Map.Entry<Zone, Map<Zone, List<DistanceValue>>> entry1 : this.zone2Zone2Distances.entrySet()) {
			String fromZoneId = entry1.getKey().getId();
			Map<String, List<DistanceValue.Raw>> to2DistancesRaw = from2To2DistancesRaw.computeIfAbsent(fromZoneId,
					zid -> new LinkedHashMap<>());
			for (Map.Entry<Zone, List<DistanceValue>> entry2 : entry1.getValue().entrySet()) {
				String toZoneId = entry2.getKey().getId();
				List<DistanceValue.Raw> distancesRaw = entry2.getValue().stream().map(dv -> new DistanceValue.Raw(dv))
						.toList();
				to2DistancesRaw.put(toZoneId, distancesRaw);
			}
		}
		return from2To2DistancesRaw;
	}

	@JsonIgnore
	public Map<Zone, Map<Zone, List<DistanceValue>>> getZone2Zone2Distances() {
		return zone2Zone2Distances;
	}

	@JsonIgnore
	public List<DistanceValue> getDistances(Zone origin, Zone destination) {
		var destination2Distances = this.zone2Zone2Distances.get(origin);
		if (destination2Distances == null) {
			return null;
		}
		return destination2Distances.get(destination);
	}

	public boolean connects(Zone one, Zone two) {
		return ((this.getDistances(one, two) != null) && (this.getDistances(two, one) != null));
	}

	public void symmetrize() {
		Set<Tuple<Zone, Zone>> wellDefinedODs = new LinkedHashSet<>();
		for (var entry1 : this.zone2Zone2Distances.entrySet()) {
			var from = entry1.getKey();
			for (var entry2 : entry1.getValue().entrySet()) {
				var to = entry2.getKey();
				wellDefinedODs.add(new Tuple<>(from, to));
			}
		}

		for (var od : wellDefinedODs) {
			Zone inverseOrigin = od.getB();
			Zone inverseDestination = od.getA();
			if (this.getDistances(inverseOrigin, inverseDestination) == null) {
				this.set(inverseOrigin, inverseDestination, this.getDistances(od.getA(), od.getB()));
			}
		}
	}

	public Distances add(Zone from, Zone to, DistanceValue distanceValue) {
		this.zone2Zone2Distances.computeIfAbsent(from, z -> new LinkedHashMap<>())
				.computeIfAbsent(to, z -> new ArrayList<>()).add(distanceValue);
		return this;
	}

	public Distances set(Zone from, Zone to, List<DistanceValue> distanceValues) {
		this.zone2Zone2Distances.computeIfAbsent(from, z -> new LinkedHashMap<>()).put(to, distanceValues);
		return this;
	}

	public double computeDistance_km(Zone from, Zone to) {
		return this.zone2Zone2Distances.get(from).get(to).stream().mapToDouble(dv -> dv.getDistance_km()).sum();
	}

	public double computeTravelTime_h(Zone from, Zone to, VehicleType vehicleType) {
		return this.zone2Zone2Distances.get(from).get(to).stream().mapToDouble(dv -> dv.getMinTravelTime_h(vehicleType))
				.sum();
	}

	public double computeTravelTimeFromStation_h(Vehicle vehicle, Zone destination) {
		return this.computeTravelTime_h(vehicle.getStation().getZone(), destination, vehicle.getVehicleType());
	}

	public int size() {
		return this.zone2Zone2Distances.entrySet().stream().mapToInt(e -> e.getValue().size()).sum();
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

//	public static void main(String[] args) throws StreamWriteException, DatabindException, IOException {
//		SmallExample.writeDistances();
//		Distances distances = SmallExample.readDistances();
//		for (var entry1 : distances.zone2Zone2Distances.entrySet()) {
//			var from = entry1.getKey();
//			for (var entry2 : entry1.getValue().entrySet()) {
//				var to = entry2.getKey();
//				var distanceList = entry2.getValue();
//				System.out
//						.println(String.format("from=%s, to=%s, distances=%s", from.getId(), to.getId(), distanceList));
//			}
//		}
//	}
}
