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
package se.vti.certain.examples.small;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.vti.certain.datastructures.IncidentType;
import se.vti.certain.datastructures.Mission;
import se.vti.certain.datastructures.Station;
import se.vti.certain.datastructures.Vehicle;
import se.vti.certain.datastructures.VehicleMission;
import se.vti.certain.datastructures.VehicleType;
import se.vti.certain.simulation.IncidentSimulator;
import se.vti.certain.simulation.MissionVehicleDeploymentSimulator;
import se.vti.certain.simulation.SimulationTimeLine;
import se.vti.certain.simulation.StartTimeSimulator;
import se.vti.certain.simulation.TimingSimulator;
import se.vti.certain.simulation.missionimplementation.MissionImplementationSimulator;
import se.vti.certain.spatial.DistanceType;
import se.vti.certain.spatial.DistanceValue;
import se.vti.certain.spatial.Distances;
import se.vti.certain.spatial.Zone;
import se.vti.certain.temporal.Season;
import se.vti.certain.temporal.TimeOfDay;
import se.vti.certain.temporal.TypeOfDay;

public class SmallExample {

	public static File vehicleTypeFile = new File("vehicleTypes.json");

	public static void writeVehicleTypes(double chargingFactor)
			throws StreamWriteException, DatabindException, IOException {
		VehicleType car = new VehicleType("EV_Car", 75.0, 0.15, chargingFactor * 50.0, 0.5, 130.0);
		VehicleType truck = new VehicleType("EV_Truck", 300.0, 0.8, chargingFactor * 150.0, 1.5, 90.0);
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(vehicleTypeFile, List.of(car, truck));
	}

	public static Map<String, VehicleType> readVehicleTypes()
			throws StreamWriteException, DatabindException, IOException {
		return VehicleType.readFromJsonFile(vehicleTypeFile);
	}

	public static File incidentTypeFile = new File("incidentTypes.json");

	public static void writeIncidentTypes() throws StreamWriteException, DatabindException, IOException {
//		IncidentType buildingFire = new IncidentType("BuildingFire").setTimings(
//				new TimingFactory().setFlatWeights(1.0).setWeight(TimeOfDay.DAY, 2.0).createTimingWeights());
		IncidentType buildingFire = new IncidentType("BuildingFire").setWeight(TimeOfDay.DAY, 2.0);
//		IncidentType carCrash = new IncidentType("CarCrash").setTimings(new TimingFactory().setFlatWeights(1.0)
//				.setWeight(Season.AUTUMN, 2.0).setWeight(Season.SPRING, 2.0).setWeight(Season.WINTER, 3.0)
//				.setWeight(TypeOfDay.WORKDAY, 2.0).setWeight(TimeOfDay.DAY, 2.0).createTimingWeights());
		IncidentType carCrash = new IncidentType("CarCrash").setWeight(Season.AUTUMN, 2.0).setWeight(Season.SPRING, 2.0)
				.setWeight(Season.WINTER, 4.0).setWeight(TimeOfDay.DAY, 4.0).setWeight(TypeOfDay.WORKDAY, 2.0);
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(incidentTypeFile,
				List.of(buildingFire, carCrash));
	}

	public static Map<String, IncidentType> readIncidentTypes()
			throws StreamReadException, DatabindException, IOException {
		return IncidentType.readFromJsonFile(incidentTypeFile);
	}

	public static File zonesFile = new File("zones.json");

	public static void writeZones(double intensityFactor, Map<String, IncidentType> id2IncidentType)
			throws StreamWriteException, DatabindException, IOException {
		IncidentType buildingFire = id2IncidentType.get("BuildingFire");
		IncidentType carCrash = id2IncidentType.get("CarCrash");
		Zone northernAmal = new Zone("NorthernAmal").setIncidentIntensity(buildingFire, intensityFactor * 10.0)
				.setIncidentIntensity(carCrash, intensityFactor * 10.0);
		Zone southernAmal = new Zone("SouthernAmal").setIncidentIntensity(buildingFire, intensityFactor * 10.0)
				.setIncidentIntensity(carCrash, intensityFactor * 1.0);
		Zone westernAmal = new Zone("WesternAmal").setIncidentIntensity(buildingFire, intensityFactor * 1.0)
				.setIncidentIntensity(carCrash, intensityFactor * 10.0);
		Zone easternAmal = new Zone("EasternAmal").setIncidentIntensity(buildingFire, intensityFactor * 1.0)
				.setIncidentIntensity(carCrash, intensityFactor * 1.0);
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(zonesFile,
				List.of(northernAmal, southernAmal, westernAmal, easternAmal));
	}

	public static void writeZones(Map<String, IncidentType> id2IncidentType)
			throws StreamWriteException, DatabindException, IOException {
		writeZones(1.0, id2IncidentType);
	}

	public static Map<String, Zone> readZones() throws StreamReadException, DatabindException, IOException {
		var id2IncidentType = readIncidentTypes();
		return Zone.readFromJsonFile(zonesFile, id2IncidentType);
	}

	public static File stationsFile = new File("stations.json");

	public static void writeStations(Map<String, Zone> id2Zone)
			throws StreamWriteException, DatabindException, IOException {
		Station northernStation = new Station("NorthernStation", id2Zone.get("NorthernAmal"));
		Station southernStation = new Station("SouthernStation", id2Zone.get("SouthernAmal"));
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(stationsFile,
				List.of(northernStation, southernStation));
	}

	public static Map<String, Station> readStations(Map<String, Zone> id2Zone, Distances distances)
			throws StreamReadException, DatabindException, IOException {
		return Station.readFromJsonFile(stationsFile, id2Zone, distances);
	}

	public static File vehiclesFile = new File("vehicles.json");

	public static void writeVehicles(Map<String, VehicleType> id2VehicleType, Map<String, Station> id2Station)
			throws StreamWriteException, DatabindException, IOException {
		Vehicle car001 = new Vehicle("Car_001", id2VehicleType.get("EV_Car"), id2Station.get("NorthernStation"));
		Vehicle truck001 = new Vehicle("Truck_001", id2VehicleType.get("EV_Truck"), id2Station.get("NorthernStation"));
		Vehicle car002 = new Vehicle("Car_002", id2VehicleType.get("EV_Car"), id2Station.get("SouthernStation"));
		List<Vehicle> vehicles = List.of(car001, truck001, car002);
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(vehiclesFile, vehicles);
	}

	public static Map<String, Vehicle> readVehicles(Map<String, VehicleType> id2VehicleType,
			Map<String, Station> id2Station) throws StreamWriteException, DatabindException, IOException {
		return Vehicle.readFromJsonFile(vehiclesFile, id2VehicleType, id2Station);
	}

	public static File distanceTypesFile = new File("distanceTypes.json");

	public static void writeDistanceTypes() throws StreamWriteException, DatabindException, IOException {
		DistanceType urbanRoad = new DistanceType("UrbanRoad", 50.0);
		DistanceType crossCountry = new DistanceType("CrossCountry", 10);
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(distanceTypesFile,
				List.of(urbanRoad, crossCountry));
	}

	public static Map<String, DistanceType> readDistanceTypes() throws IOException {
		return DistanceType.readFromJsonFile(distanceTypesFile);
	}

	public static File distancesFile = new File("distances.json");

	public static void writeDistances(Map<String, DistanceType> id2DistanceType, Map<String, Zone> id2Zone)
			throws StreamWriteException, DatabindException, IOException {

		var urbanRoad = id2DistanceType.get("UrbanRoad");
		var crossCountry = id2DistanceType.get("CrossCountry");

		var withinZone = List.of(new DistanceValue(urbanRoad, 5.0), new DistanceValue(crossCountry, 0.0));
		var betweenZones = List.of(new DistanceValue(urbanRoad, 10.0), new DistanceValue(crossCountry, 0.0));
		Distances distances = new Distances();
		for (Zone from : id2Zone.values()) {
			for (Zone to : id2Zone.values()) {
				if (from == to) {
					distances.set(from, to, withinZone);
				} else {
					distances.set(from, to, betweenZones);
				}
			}
		}
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(distancesFile, distances);
	}

	public static Distances readDistances(Map<String, DistanceType> id2DistanceType, Map<String, Zone> id2Zone)
			throws IOException {
		return Distances.readFromJsonFile(distancesFile, id2Zone, id2DistanceType);
	}

	public static File anonymousMissionsFile = new File("missions.json");

	public static void writeAnonymousMissions(Map<String, IncidentType> id2IncidentType,
			Map<String, VehicleType> id2VehicleType) throws StreamWriteException, DatabindException, IOException {
		Mission mission1 = new Mission(id2IncidentType.get("BuildingFire"), null)
				.addVehicleMission(new VehicleMission(id2VehicleType.get("EV_Car"), 0.0, 2.0))
				.addVehicleMission(new VehicleMission(id2VehicleType.get("EV_Truck"), 0.0, 2.0));
		Mission mission2 = new Mission(id2IncidentType.get("BuildingFire"), null)
				.addVehicleMission(new VehicleMission(id2VehicleType.get("EV_Car"), 0.0, 2.0))
				.addVehicleMission(new VehicleMission(id2VehicleType.get("EV_Truck"), 0.0, 2.0));
		Mission mission3 = new Mission(id2IncidentType.get("BuildingFire"), null)
				.addVehicleMission(new VehicleMission(id2VehicleType.get("EV_Car"), 0.0, 2.0))
				.addVehicleMission(new VehicleMission(id2VehicleType.get("EV_Car"), 0.0, 2.0))
				.addVehicleMission(new VehicleMission(id2VehicleType.get("EV_Truck"), 0.0, 2.0));
		Mission mission4 = new Mission(id2IncidentType.get("CarCrash"), null)
				.addVehicleMission(new VehicleMission(id2VehicleType.get("EV_Car"), 0.0, 1.0));
		Mission mission5 = new Mission(id2IncidentType.get("CarCrash"), null)
				.addVehicleMission(new VehicleMission(id2VehicleType.get("EV_Car"), 0.0, 1.0));
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(anonymousMissionsFile,
				List.of(mission1, mission2, mission3, mission4, mission5));
	}

//	public static List<Mission> readAnonymousMissions() throws StreamReadException, DatabindException, IOException {
//		var id2IncidentType = IncidentType.readFromJsonFile(incidentTypeFile);
//		var id2Zone = Zone.readFromJsonFile(zonesFile, id2IncidentType);
//		var id2VehicleType = VehicleType.readFromJsonFile(vehicleTypeFile);
//		var id2Station = Station.readFromJsonFile(stationsFile, id2Zone);
//		var id2Vehicle = Vehicle.readFromJsonFile(vehiclesFile, id2VehicleType, id2Station);
//		return Mission.readFromJson(anonymousMissionsFile, id2IncidentType, id2Zone, id2Vehicle);
//	}

	public static double simulateAverageDelay_h(double intensityFactor, double chargingFactor, boolean verbose)
			throws StreamWriteException, DatabindException, IOException {

		// CREATE TEST DATA

		writeVehicleTypes(chargingFactor);
		var id2VehicleType = VehicleType.readFromJsonFile(vehicleTypeFile);

		writeIncidentTypes();
		var id2IncidentType = IncidentType.readFromJsonFile(incidentTypeFile);

		writeZones(intensityFactor, id2IncidentType);
		var id2Zone = Zone.readFromJsonFile(zonesFile, id2IncidentType);

		writeAnonymousMissions(id2IncidentType, id2VehicleType);
		var prototypeMissions = Mission.readFromJson(anonymousMissionsFile, id2IncidentType, id2Zone, id2VehicleType,
				false);

		writeDistanceTypes();
		var id2DistanceType = DistanceType.readFromJsonFile(distanceTypesFile);

		writeDistances(id2DistanceType, id2Zone);
		var distances = Distances.readFromJsonFile(distancesFile, id2Zone, id2DistanceType);

		writeStations(id2Zone);
		var id2Station = Station.readFromJsonFile(stationsFile, id2Zone, distances);

		writeVehicles(id2VehicleType, id2Station);
		var id2Vehicle = Vehicle.readFromJsonFile(vehiclesFile, id2VehicleType, id2Station);

		// SIMULATION

		var rnd = new Random();

		var daylightStart_h = 9.0;
		var daylightEnd_h = 16.0;
		var numberOfSimulatedDays = 90;
		var timeLine = new SimulationTimeLine(Season.WINTER, TypeOfDay.WORKDAY, daylightStart_h, daylightEnd_h,
				numberOfSimulatedDays);

		var incidentSimulator = new IncidentSimulator(timeLine, rnd);
		var simulatedMissions = incidentSimulator.simulateMissions(id2Zone);

		var timingSimulator = new TimingSimulator(timeLine, rnd);
		timingSimulator.simulateTimings(simulatedMissions);

		var startTimeSimulator = new StartTimeSimulator(timeLine, rnd);
		startTimeSimulator.simulateStarTimes(simulatedMissions);
		simulatedMissions = StartTimeSimulator.getStartTimeSortedMissions(simulatedMissions);

		var missionFleetSimulator = new MissionVehicleDeploymentSimulator(prototypeMissions, rnd);
		missionFleetSimulator.simulateFleets(simulatedMissions);
		simulatedMissions = missionFleetSimulator.getMissionsWithDeployedVehicles(simulatedMissions);

		var missionImplementationSimulator = new MissionImplementationSimulator(id2Vehicle, distances)
				.setVerbose(verbose).setRelSOCWhenAvailable(0.8);
		var systemState = missionImplementationSimulator.simulateMissionImplementation(simulatedMissions);
		var mission2VehicleLogs = systemState.getMission2VehicleDispachmentLog();

		double missionCnt = mission2VehicleLogs.size();
		double sum_h = 0.0;
		for (var entry : mission2VehicleLogs.entrySet()) {
			for (var log : entry.getValue()) {
				sum_h += log.timeAtSite_h - log.requestTime_h;
			}
		}
		return sum_h / missionCnt;
	}

	static void runStressTest(double chargingFactor) throws StreamWriteException, DatabindException, IOException {
		System.out.println("intensityFactor\tavg.delay[min]");
		for (double intensityFactor = 0; intensityFactor <= 20.0 + 1e-6; intensityFactor += 0.1) {
			double delay_h = simulateAverageDelay_h(intensityFactor, chargingFactor, false);
			System.out.println(intensityFactor + "\t" + (Double.isNaN(delay_h) ? "" : delay_h * 60.0));
		}
	}

	static void runBaseCase(double intensityFactor, double chargingFactor)
			throws StreamWriteException, DatabindException, IOException {
		simulateAverageDelay_h(intensityFactor, chargingFactor, true);
	}

	public static void main(String[] args) throws StreamReadException, DatabindException, IOException {
		runBaseCase(1.0, 1.0);
//		runStressTest(1.0);
//		runStressTest(0.1);
	}
}
