/**
 * se.vti.certain.examples.rtog
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
import java.util.Locale;
import java.util.Map;

import se.vti.certain.analysis.modifiers.VehicleTypeModifier;
import se.vti.certain.analysis.modifiers.ZoneModifier;
import se.vti.certain.analysis.observables.AverageTimeFromVehicleRequestToArrival;
import se.vti.certain.analysis.observables.NumberOfMissions;
import se.vti.certain.analysis.observables.ShareOfIncompleteMissions;
import se.vti.certain.datastructures.IncidentType;
import se.vti.certain.datastructures.VehicleType;
import se.vti.certain.spatial.Zone;
import se.vti.certain.temporal.Season;
import se.vti.certain.temporal.TypeOfDay;

/**
 * @author GunnarF
 */
public class Transportforum2026 {

	static RTOG_Runner createRunner(double batteryCapacityFactor, double chargingRateFactor,
			double incidentIntensityFactor) throws IOException {

		var runner = new RTOG_Runner();

		new VehicleTypeModifier(VehicleType.readFromJsonFile(runner.vehicleTypeFile)) {
			@Override
			public double computeBatteryCapacity_kWh(VehicleType vehicleType) {
				return batteryCapacityFactor * vehicleType.getBatteryCapacity_kWh();
			}

			@Override
			public double computeChargingRate_kW(VehicleType vehicleType) {
				return chargingRateFactor * vehicleType.getChargingRate_kW();
			}
		}.createVehicleTypeFile("modifiedVehicleTypes.json");

		new ZoneModifier(
				Zone.readFromJsonFile(runner.zonesFile, IncidentType.readFromJsonFile(runner.incidentTypeFile))) {
			@Override
			public void updateIncidentType2Intensity_1_yr(String zoneId,
					Map<IncidentType, Double> scenarioIncidentType2Intensity_1_yr) {
				scenarioIncidentType2Intensity_1_yr.entrySet().stream()
						.forEach(e -> e.setValue(incidentIntensityFactor * e.getValue()));
			}
		}.createZoneFile("modifiedZones.json");

		runner.vehicleTypeFile = new File("modifiedVehicleTypes.json");
		runner.zonesFile = new File("modifiedZones.json");
		runner.loadBaseScenario();
		return runner;
	}

	static void testSeasons() throws IOException {
		var runner = createRunner(1.0, 1.0, 1.0);
		for (var season : Season.values()) {
			for (var typeOfDay : TypeOfDay.values()) {
				System.out.println();
				System.out.println(season + ", " + typeOfDay);
				runner.run(30, season, typeOfDay, 100, 0.8);
			}
		}
	}

	// EXAMPLE 1
	static void testIntensities(Season season, TypeOfDay typeOfDay) throws IOException {
		var intensityFactor = new ArrayList<Double>();
		var numberOfMissions = new ArrayList<Double>();
		var timeToArrival = new ArrayList<Double>();
		var percentageIncomplete = new ArrayList<Double>();

		for (double incidentIntensityFactor : new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }) {
			System.out.println();
			System.out.println("INCIDENT INTENSITY FACTOR = " + incidentIntensityFactor);
			var runner = createRunner(1.0, 1.0, incidentIntensityFactor);
			runner.run(90, season, typeOfDay, 100, 1.0);
			intensityFactor.add(incidentIntensityFactor);
			numberOfMissions.add(runner.analyzer.getStatistics().get(NumberOfMissions.NAME).getMean());
			timeToArrival
					.add(runner.analyzer.getStatistics().get(AverageTimeFromVehicleRequestToArrival.NAME).getMean());
			percentageIncomplete.add(runner.analyzer.getStatistics().get(ShareOfIncompleteMissions.NAME).getMean());
		}

		System.out.println();
		System.out.println("Factor\tNumberOfMissions\tTimeToArrival[min]\tIncompleteMissions[%]");
		for (int i = 0; i < intensityFactor.size(); i++) {
			System.out.printf(Locale.US, "%.0f\t%.0f\t%.6f\t%.6f\n", intensityFactor.get(i), numberOfMissions.get(i),
					timeToArrival.get(i), percentageIncomplete.get(i));
		}
		System.out.println();
	}

	static void testChargingRates(Season season, TypeOfDay typeOfDay) throws IOException {
		var chargingRateFactors = new ArrayList<Double>();
		var numberOfMissions = new ArrayList<Double>();
		var timeToArrival = new ArrayList<Double>();
		var percentageIncomplete = new ArrayList<Double>();

		for (double chargingRateFactor : new double[] { 1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1 }) {
			System.out.println();
			System.out.println("CHARGING RATE FACTOR = " + chargingRateFactor);
			var runner = createRunner(0.6, chargingRateFactor, 5.0);
			runner.run(90, season, typeOfDay, 100, 1.0);
			chargingRateFactors.add(chargingRateFactor);
			numberOfMissions.add(runner.analyzer.getStatistics().get(NumberOfMissions.NAME).getMean());
			timeToArrival
					.add(runner.analyzer.getStatistics().get(AverageTimeFromVehicleRequestToArrival.NAME).getMean());
			percentageIncomplete.add(runner.analyzer.getStatistics().get(ShareOfIncompleteMissions.NAME).getMean());
		}

		System.out.println();
		System.out.println("Factor\tNumberOfMissions\tTimeToArrival[min]\tIncompleteMissions[%]");
		for (int i = 0; i < chargingRateFactors.size(); i++) {
			System.out.printf(Locale.US, "%.2f\t%.0f\t%.6f\t%.6f\n", chargingRateFactors.get(i),
					numberOfMissions.get(i), timeToArrival.get(i), percentageIncomplete.get(i));
		}
		System.out.println();
	}

	static void testBatteryCapacities(Season season, TypeOfDay typeOfDay) throws IOException {
		var batteryCapacityFactors = new ArrayList<Double>();
		var numberOfMissions = new ArrayList<Double>();
		var timeToArrival = new ArrayList<Double>();
		var percentageIncomplete = new ArrayList<Double>();

		for (double batteryCapacityFactor : new double[] { 1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1 }) {
			System.out.println();
			System.out.println("BATTERY CAPACITY FACTOR = " + batteryCapacityFactor);
			var runner = createRunner(batteryCapacityFactor, 1.0, 5.0);
			runner.run(90, season, typeOfDay, 100, 1.0);

			batteryCapacityFactors.add(batteryCapacityFactor);
			numberOfMissions.add(runner.analyzer.getStatistics().get(NumberOfMissions.NAME).getMean());
			timeToArrival
					.add(runner.analyzer.getStatistics().get(AverageTimeFromVehicleRequestToArrival.NAME).getMean());
			percentageIncomplete.add(runner.analyzer.getStatistics().get(ShareOfIncompleteMissions.NAME).getMean());
		}

		System.out.println();
		System.out.println("Factor\tNumberOfMissions\tTimeToArrival[min]\tIncompleteMissions[%]");
		for (int i = 0; i < batteryCapacityFactors.size(); i++) {
			System.out.printf(Locale.US, "%.2f\t%.0f\t%.6f\t%.6f\n", batteryCapacityFactors.get(i),
					numberOfMissions.get(i), timeToArrival.get(i), percentageIncomplete.get(i));
		}
		System.out.println();
	}

	static void testMinimalChargingLevel(Season season, TypeOfDay typeOfDay) throws IOException {
		var minRelSOCs = new ArrayList<Double>();
		var numberOfMissions = new ArrayList<Double>();
		var timeToArrival = new ArrayList<Double>();
		var percentageIncomplete = new ArrayList<Double>();

		for (double minRelSOC : new double[] { 1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1 }) {
			var runner = createRunner(0.6, 0.6, 5.0);
			System.out.println();
			System.out.println("MIN CHARGING LEVEL = " + minRelSOC);
			runner.run(90, season, typeOfDay, 100, minRelSOC);

			minRelSOCs.add(minRelSOC);
			numberOfMissions.add(runner.analyzer.getStatistics().get(NumberOfMissions.NAME).getMean());
			timeToArrival
					.add(runner.analyzer.getStatistics().get(AverageTimeFromVehicleRequestToArrival.NAME).getMean());
			percentageIncomplete.add(runner.analyzer.getStatistics().get(ShareOfIncompleteMissions.NAME).getMean());
		}
		
		System.out.println();
		System.out.println("Factor\tNumberOfMissions\tTimeToArrival[min]\tIncompleteMissions[%]");
		for (int i = 0; i < minRelSOCs.size(); i++) {
			System.out.printf(Locale.US, "%.2f\t%.0f\t%.6f\t%.6f\n", minRelSOCs.get(i),
					numberOfMissions.get(i), timeToArrival.get(i), percentageIncomplete.get(i));
		}
		System.out.println();
	}

	public static void main(String[] args) throws IOException {
		System.out.println("STARTED ...");

//		testIntensities(Season.AUTUMN, TypeOfDay.WORKDAY);
//		testChargingRates(Season.AUTUMN, TypeOfDay.WORKDAY);
//		testBatteryCapacities(Season.AUTUMN, TypeOfDay.WORKDAY);
		testMinimalChargingLevel(Season.AUTUMN, TypeOfDay.WORKDAY);

		System.out.println("... DONE");
	}

}
