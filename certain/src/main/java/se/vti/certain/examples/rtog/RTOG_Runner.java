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
package se.vti.certain.examples.rtog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import se.vti.certain.analysis.ReplicationAnalyzer;
import se.vti.certain.analysis.ReplicationRunner;
import se.vti.certain.analysis.observables.AverageMissingSOCWhenOutOfCharge;
import se.vti.certain.analysis.observables.AverageTimeFromVehicleRequestToArrival;
import se.vti.certain.analysis.observables.NumberOfMissions;
import se.vti.certain.analysis.observables.ShareOfIncompleteMissions;
import se.vti.certain.analysis.observables.ShareOfVehiclesRunningOutOfCharge;
import se.vti.certain.datastructures.IncidentType;
import se.vti.certain.datastructures.Mission;
import se.vti.certain.datastructures.Station;
import se.vti.certain.datastructures.Vehicle;
import se.vti.certain.datastructures.VehicleType;
import se.vti.certain.simulation.SimulationTimeLine;
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
class RTOG_Runner {

	String path = "./rtog/input/";
	File vehicleTypeFile = new File(path + "vehicleTypes.json");
	File incidentTypeFile = new File(path + "incidentTypes.json");
	File zonesFile = new File(path + "zones.json");
	File stationsFile = new File(path + "stations.json");
	File vehiclesFile = new File(path + "vehicles.json");
	File missionsFile = new File(path + "missions.json");
	File distanceTypesFile = new File(path + "distanceTypes.json");
	File distancesFile = new File(path + "distances.json");

	boolean verbose = false;

	Map<String, VehicleType> id2VehicleType;
	Map<String, IncidentType> id2IncidentType;
	Map<String, Zone> id2Zone;
	Map<String, DistanceType> id2DistanceType;
	Distances distances;
	Map<String, Station> id2Station;
	Map<String, Vehicle> id2Vehicle;
	List<Mission> prototypeMissions;

	List<Mission> extraMissions = new ArrayList<>();

	RTOG_Runner() {
	}

	void loadBaseScenario() throws IOException {
		this.id2VehicleType = VehicleType.readFromJsonFile(this.vehicleTypeFile);
		System.out.println("Loaded " + this.id2VehicleType.size() + " vehicle types.");

		this.id2IncidentType = IncidentType.readFromJsonFile(this.incidentTypeFile);
		System.out.println("Loaded " + this.id2IncidentType.size() + " incident types.");

		this.id2Zone = Zone.readFromJsonFile(this.zonesFile, this.id2IncidentType);
		System.out.println("Loaded " + this.id2Zone.size() + " zones.");

		this.id2DistanceType = DistanceType.readFromJsonFile(this.distanceTypesFile);
		System.out.println("Loaded " + this.id2DistanceType.size() + " distance types.");

		this.distances = Distances.readFromJsonFile(this.distancesFile, this.id2Zone, this.id2DistanceType);
		this.distances.symmetrize();
		System.out.println("Loaded " + this.distances.size() + " distances.");

		this.id2Station = Station.readFromJsonFile(this.stationsFile, this.id2Zone, this.distances);
		System.out.println("Loaded " + this.id2Station.size() + " stations.");

		this.id2Vehicle = Vehicle.readFromJsonFile(this.vehiclesFile, this.id2VehicleType, this.id2Station);
		System.out.println("Loaded " + this.id2Vehicle.size() + " vehicles.");

		this.prototypeMissions = Mission.readFromJson(this.missionsFile, this.id2IncidentType, this.id2Zone,
				this.id2VehicleType, false);
		System.out.println("Loaded " + this.prototypeMissions.size() + " prototype missions.");
	}

	void addMission(Mission mission) {
		this.extraMissions.add(mission);
	}

	ReplicationAnalyzer analyzer = null;

	void run(int numberOfDays, Season season, TypeOfDay typeOfDay, int numberOfReplications, double minRelSOC) {

		var rnd = new Random();

		final double daylightStart_h;
		final double daylightEnd_h;
		if (Season.SPRING == season || Season.AUTUMN == season) {
			daylightStart_h = 7.0;
			daylightEnd_h = 18.5;
		} else if (Season.SUMMER == season) {
			daylightStart_h = 5.0;
			daylightEnd_h = 21.0;
		} else if (Season.WINTER == season) {
			daylightStart_h = 9.0;
			daylightEnd_h = 16.0;
		} else {
			throw new RuntimeException("Unknown season: " + season);
		}
		var timeLine = new SimulationTimeLine(season, typeOfDay, daylightStart_h, daylightEnd_h, numberOfDays);

		var replicationRunner = new ReplicationRunner(rnd).setVerbose(false)
				.setNumberOfReplications(numberOfReplications).setTimeLine(timeLine).setMinRelSOC(minRelSOC)
				.setId2Zone(this.id2Zone).setDistances(this.distances).setId2Vehicle(this.id2Vehicle)
				.setPrototypeMissions(this.prototypeMissions);
		replicationRunner.run();

		this.analyzer = new ReplicationAnalyzer();
		this.analyzer.addObservable(NumberOfMissions.NAME, new NumberOfMissions());
		this.analyzer.addObservable(AverageTimeFromVehicleRequestToArrival.NAME,
				new AverageTimeFromVehicleRequestToArrival());
		this.analyzer.addObservable(ShareOfVehiclesRunningOutOfCharge.NAME, new ShareOfVehiclesRunningOutOfCharge());
		this.analyzer.addObservable(ShareOfIncompleteMissions.NAME, new ShareOfIncompleteMissions());
		this.analyzer.addObservable(AverageMissingSOCWhenOutOfCharge.NAME, new AverageMissingSOCWhenOutOfCharge());

		this.analyzer.add(replicationRunner.getSimulatedSystemStates());
		System.out.println(this.analyzer);
	}
}
