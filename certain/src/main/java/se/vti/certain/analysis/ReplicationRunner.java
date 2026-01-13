/**
 * se.vti.certain.analysis
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
package se.vti.certain.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import se.vti.certain.analysis.observables.AverageMissingSOCWhenOutOfCharge;
import se.vti.certain.analysis.observables.AverageTimeFromVehicleRequestToArrival;
import se.vti.certain.analysis.observables.NumberOfMissions;
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
import se.vti.certain.simulation.missionimplementation.SystemState;
import se.vti.certain.spatial.DistanceType;
import se.vti.certain.spatial.Distances;
import se.vti.certain.spatial.Zone;
import se.vti.certain.temporal.Season;
import se.vti.certain.temporal.TypeOfDay;

/**
 * @author GunnarF
 */
public class ReplicationRunner {

	private final Random rnd;
	private boolean verbose = true;
	private Integer numberOfReplications = null;

	private SimulationTimeLine timeLine = null;
	private Double minRelSOC = null;

	private Map<String, Zone> id2Zone = null;
	private Distances distances = null;
	private Map<String, Vehicle> id2Vehicle = null;

	private List<Mission> prototypeMissions = null;
	private final List<Mission> specialMissions = new ArrayList<>();

	private List<SystemState> simulatedSystemStates = null;

	public ReplicationRunner(Random rnd) {
		this.rnd = rnd;
	}

	public ReplicationRunner() {
		this(new Random());
	}

	public ReplicationRunner setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	public ReplicationRunner setNumberOfReplications(int numberOfReplications) {
		this.numberOfReplications = numberOfReplications;
		this.simulatedSystemStates = new ArrayList<>(numberOfReplications);
		return this;
	}

	public ReplicationRunner setTimeLine(SimulationTimeLine timeLine) {
		this.timeLine = timeLine;
		return this;
	}

	public ReplicationRunner setMinRelSOC(double minRelSOC) {
		this.minRelSOC = minRelSOC;
		return this;
	}

	public ReplicationRunner setId2Zone(Map<String, Zone> id2Zone) {
		this.id2Zone = id2Zone;
		return this;
	}

	public ReplicationRunner setDistances(Distances distances) {
		this.distances = distances;
		return this;
	}

	public ReplicationRunner setId2Vehicle(Map<String, Vehicle> id2Vehicle) {
		this.id2Vehicle = id2Vehicle;
		return this;
	}

	public ReplicationRunner setPrototypeMissions(List<Mission> prototypeMissions) {
		this.prototypeMissions = prototypeMissions;
		return this;
	}

	public ReplicationRunner addSpecialMission(Mission mission) {
		this.specialMissions.add(mission);
		return this;
	}

	public List<SystemState> getSimulatedSystemStates() {
		return this.simulatedSystemStates;
	}

	public void run() {

		for (int replication = 0; replication < this.numberOfReplications; replication++) {

			if (this.verbose) {
				System.out.println((1 + replication) + " / " + this.numberOfReplications);
			}

			var incidentSimulator = new IncidentSimulator(this.timeLine, this.rnd.nextLong());
			List<Mission> simulatedMissions = incidentSimulator.simulateMissions(this.id2Zone);

			var timingSimulator = new TimingSimulator(this.timeLine, this.rnd);
			timingSimulator.simulateTimings(simulatedMissions);

			var startTimeSimulator = new StartTimeSimulator(this.timeLine, this.rnd);
			startTimeSimulator.simulateStarTimes(simulatedMissions);
			simulatedMissions = StartTimeSimulator.getStartTimeSortedMissions(simulatedMissions);

			var missionFleetSimulator = new MissionVehicleDeploymentSimulator(this.prototypeMissions, this.rnd);
			missionFleetSimulator.simulateFleets(simulatedMissions);
			simulatedMissions = missionFleetSimulator.getMissionsWithDeployedVehicles(simulatedMissions);

			simulatedMissions.addAll(this.specialMissions);
			simulatedMissions = StartTimeSimulator.getStartTimeSortedMissions(simulatedMissions);

			var missionImplementationSimulator = new MissionImplementationSimulator(id2Vehicle, distances)
					.setVerbose(false).setRelSOCWhenAvailable(this.minRelSOC);
			var systemState = missionImplementationSimulator.simulateMissionImplementation(simulatedMissions);
			this.simulatedSystemStates.add(systemState);

		}
	}

	static String path = "./rtog/input/";
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
		int numberOfReplications = 10;

		var daylightStart_h = 9.0;
		var daylightEnd_h = 16.0;
		var numberOfSimulatedDays = 90;
		var timeLine = new SimulationTimeLine(Season.WINTER, TypeOfDay.WORKDAY, daylightStart_h, daylightEnd_h,
				numberOfSimulatedDays);

		var eonMission = Mission
				.readFromJson(new File(path + "eon-mission.json"), id2IncidentType, id2Zone, id2VehicleType, true)
				.get(0);
		eonMission.setStartTime_h(10 * 24.0); // TODO set real time

		var replicationRunner = new ReplicationRunner(rnd).setNumberOfReplications(numberOfReplications)
				.setTimeLine(timeLine).setMinRelSOC(0.8).setId2Zone(id2Zone).setDistances(distances)
				.setId2Vehicle(id2Vehicle).setPrototypeMissions(prototypeMissions).addSpecialMission(eonMission);
		replicationRunner.run();

		var analyzer = new ReplicationAnalyzer();
		analyzer.addObservable(NumberOfMissions.NAME, new NumberOfMissions());
		analyzer.addObservable(AverageTimeFromVehicleRequestToArrival.NAME,
				new AverageTimeFromVehicleRequestToArrival());
		analyzer.addObservable(ShareOfVehiclesRunningOutOfCharge.NAME, new ShareOfVehiclesRunningOutOfCharge());
		analyzer.addObservable(AverageMissingSOCWhenOutOfCharge.NAME, new AverageMissingSOCWhenOutOfCharge());

		analyzer.add(replicationRunner.getSimulatedSystemStates());
		System.out.println(analyzer);

	}

}
