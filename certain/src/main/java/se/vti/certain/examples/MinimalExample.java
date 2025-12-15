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
package se.vti.certain.examples;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import se.vti.certain.analysis.ReplicationAnalyzer;
import se.vti.certain.analysis.ReplicationRunner;
import se.vti.certain.analysis.observables.AverageMissingSOCWhenOutOfCharge;
import se.vti.certain.analysis.observables.AverageTimeFromVehicleRequestToArrival;
import se.vti.certain.analysis.observables.NumberOfMissions;
import se.vti.certain.analysis.observables.ShareOfDaytimeIncidents;
import se.vti.certain.analysis.observables.ShareOfNighttimeIncidents;
import se.vti.certain.analysis.observables.ShareOfVehiclesRunningOutOfCharge;
import se.vti.certain.datastructures.IncidentType;
import se.vti.certain.datastructures.Mission;
import se.vti.certain.datastructures.Station;
import se.vti.certain.datastructures.Vehicle;
import se.vti.certain.datastructures.VehicleType;
import se.vti.certain.simulation.IncidentSimulator;
import se.vti.certain.simulation.MissionVehicleDeploymentSimulator;
import se.vti.certain.simulation.SimulationTimeLine;
import se.vti.certain.simulation.StartTimeSimulator;
import se.vti.certain.simulation.TimingSimulator;
import se.vti.certain.simulation.missionimplementation.MissionImplementationSimulator;
import se.vti.certain.spatial.DistanceType;
import se.vti.certain.spatial.Distances;
import se.vti.certain.spatial.Zone;
import se.vti.certain.temporal.Season;
import se.vti.certain.temporal.TypeOfDay;

/**
 * 
 * @author GunnarF
 *
 */
public class MinimalExample {

	static String path = "./minimal/input/";
	static File vehicleTypeFile = new File(path + "vehicleTypes.json");
	static File incidentTypeFile = new File(path + "incidentTypes.json");
	static File zonesFile = new File(path + "zones.json");
	static File stationsFile = new File(path + "stations.json");
	static File vehiclesFile = new File(path + "vehicles.json");
	static File missionsFile = new File(path + "missions.json");
	static File distanceTypesFile = new File(path + "distanceTypes.json");
	static File distancesFile = new File(path + "distances.json");

	public static void main(String[] args) throws IOException {
		System.out.println("STARTED ...");

		var id2VehicleType = VehicleType.readFromJsonFile(vehicleTypeFile);
		System.out.println("Loaded " + id2VehicleType.size() + " vehicle types.");

		var id2IncidentType = IncidentType.readFromJsonFile(incidentTypeFile);
		System.out.println("Loaded " + id2IncidentType.size() + " incident types.");

		var id2Zone = Zone.readFromJsonFile(zonesFile, id2IncidentType);
		System.out.println("Loaded " + id2Zone.size() + " zones.");

		var id2DistanceType = DistanceType.readFromJsonFile(distanceTypesFile);
		System.out.println("Loaded " + id2DistanceType.size() + " distance types.");

		var distances = Distances.readFromJsonFile(distancesFile, id2Zone, id2DistanceType);
		distances.symmetrize();
		System.out.println("Loaded " + distances.size() + " distances.");

		var id2Station = Station.readFromJsonFile(stationsFile, id2Zone, distances);
		System.out.println("Loaded " + id2Station.size() + " stations.");

		var id2Vehicle = Vehicle.readFromJsonFile(vehiclesFile, id2VehicleType, id2Station);
		System.out.println("Loaded " + id2Vehicle.size() + " vehicles.");

		var prototypeMissions = Mission.readFromJson(missionsFile, id2IncidentType, id2Zone, id2VehicleType, false);
		System.out.println("Loaded " + prototypeMissions.size() + " prototype missions.");

		var rnd = new Random();
		int numberOfReplications = 100;
		
		var daylightStart_h = 9.0;
		var daylightEnd_h = 16.0;
		var numberOfSimulatedDays = 365;
		var timeLine = new SimulationTimeLine(Season.WINTER, TypeOfDay.WORKDAY, daylightStart_h, daylightEnd_h,
				numberOfSimulatedDays);
		
		var replicationRunner = new ReplicationRunner(rnd).setNumberOfReplications(numberOfReplications)
				.setTimeLine(timeLine).setMinRelSOC(0.8).setId2Zone(id2Zone).setDistances(distances)
				.setId2Vehicle(id2Vehicle).setPrototypeMissions(prototypeMissions);
		replicationRunner.run();

		var analyzer = new ReplicationAnalyzer();
		analyzer.addObservable(NumberOfMissions.NAME, new NumberOfMissions());
		analyzer.addObservable(AverageTimeFromVehicleRequestToArrival.NAME,
				new AverageTimeFromVehicleRequestToArrival());
		analyzer.addObservable(ShareOfVehiclesRunningOutOfCharge.NAME, new ShareOfVehiclesRunningOutOfCharge());
		analyzer.addObservable(AverageMissingSOCWhenOutOfCharge.NAME, new AverageMissingSOCWhenOutOfCharge());
		analyzer.addObservable(ShareOfDaytimeIncidents.NAME, new ShareOfDaytimeIncidents());
		
		analyzer.add(replicationRunner.getSimulatedSystemStates());
		System.out.println(analyzer);

		
//		var incidentSimulator = new IncidentSimulator(timeLine, rnd.nextLong());
//		List<Mission> simulatedMissions = incidentSimulator.simulateMissions(id2Zone);
//		System.out.println("Simulated " + simulatedMissions.size() + " missions.");
//
//		var timingSimulator = new TimingSimulator(timeLine, rnd);
//		timingSimulator.simulateTimings(simulatedMissions);
//		System.out.println("Added timings to " + simulatedMissions.size() + " missions.");
//
//		var startTimeSimulator = new StartTimeSimulator(timeLine, rnd);
//		startTimeSimulator.simulateStarTimes(simulatedMissions);
//		simulatedMissions = StartTimeSimulator.getStartTimeSortedMissions(simulatedMissions);
//		System.out.println("Added start times to " + simulatedMissions.size() + " missions.");
//
//		var missionFleetSimulator = new MissionVehicleDeploymentSimulator(prototypeMissions, rnd);
//		missionFleetSimulator.simulateFleets(simulatedMissions);
//		simulatedMissions = missionFleetSimulator.getMissionsWithDeployedVehicles(simulatedMissions);
//		System.out.println("Added fleets to " + simulatedMissions.size() + " missions.");
//
//		var missionImplementationSimulator = new MissionImplementationSimulator(id2Vehicle, distances).setVerbose(true)
//				.setRelSOCWhenAvailable(0.8);
//		var systemState = missionImplementationSimulator.simulateMissionImplementation(simulatedMissions);
//		System.out.println("Simulated implementation of " + simulatedMissions.size() + " missions.");
//		System.out.println("Number of served vehicle requests: "
//				+ systemState.getMission2VehicleMissionLogs().values().stream().mapToInt(l -> l.size()).sum());
//		System.out.println("Number of failed vehicle requests: " + systemState.getFailedRequests().size());
//		System.out.println("Zones with failed vehicle requests: " + new LinkedHashSet<>(
//				systemState.getFailedRequests().stream().map(r -> r.getMission().getZone().getId()).toList()));
//
//		System.out.println("... DONE");
	}

}
