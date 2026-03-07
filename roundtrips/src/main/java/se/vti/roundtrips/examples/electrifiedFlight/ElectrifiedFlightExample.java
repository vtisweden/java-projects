/**
 * se.vti.roundtrips.examples.travelSurveyExpansion
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
package se.vti.roundtrips.examples.electrifiedFlight;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.common.RandomRoundTripGenerator;
import se.vti.roundtrips.common.Runner;
import se.vti.roundtrips.common.ScenarioBuilder;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.samplingweights.StrictlyPeriodicSchedule;
import se.vti.roundtrips.simulator.DefaultSimulator;
import se.vti.roundtrips.simulator.Episode;
import se.vti.roundtrips.simulator.MoveEpisode;
import se.vti.roundtrips.simulator.electrified.BatteryWrapAroundSimulator;
import se.vti.roundtrips.simulator.electrified.ElectrifiedMoveSimulator;
import se.vti.roundtrips.simulator.electrified.ElectrifiedStaySimulator;
import se.vti.roundtrips.simulator.electrified.StrictlyNonNegativeBatteryCharge;
import se.vti.utils.misc.metropolishastings.MHBatchBasedStatisticEstimator;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class ElectrifiedFlightExample {

	static final Logger log = LogManager.getLogger(ElectrifiedFlightExample.class);

	public static void main(String[] args) {

		double timeBinSize_h = 1.0;
		int numberOfTimeBins = 24;
		var scenarioBuilder = new ScenarioBuilder<NodeWithCoords>().setTimeBinSize_h(timeBinSize_h)
				.setNumberOfTimeBins(numberOfTimeBins);

		/*
		 * Load airports table.
		 */
		CSVParser airportParser;
		try {
			Path airportsPath = Path
					.of("./src/main/resources/se/vti/roundtrips/examples/electrifiedFlight/airports.csv");
			airportParser = CSVParser.parse(airportsPath, StandardCharsets.UTF_8,
					CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		/*
		 * Add airports to the scenario.
		 */
		for (CSVRecord row : airportParser) {
			double x_km = Double.parseDouble(row.get("X"));
			double y_km = Double.parseDouble(row.get("Y"));
			String code = row.get("AIRPORT_ai");
			log.info("Loading airport: " + code + " (" + x_km + ", " + y_km + ")");
			scenarioBuilder.addNode(new NodeWithCoords(code, x_km, y_km));
		}

		/*
		 * Compute inter-airport distances and flight times.
		 */
		double speed_km_h = 300.0;
		scenarioBuilder.setMoveDistanceFunction((from, to) -> from.computeEuclideanDistance(to));
		scenarioBuilder.setMoveTimeFunction((from, to) -> from.computeEuclideanDistance(to) / speed_km_h);

		/*
		 * Scenario is complete. Now create runner for computing the Monte Carlo
		 * analysis.
		 */
		int numberOfIterations = 1_000_000;
		var scenario = scenarioBuilder.build();
		var runner = new Runner<>(scenario).setUniformPrior()
				.addIndividualWeight(new StrictlyPeriodicSchedule<NodeWithCoords>(timeBinSize_h * numberOfTimeBins))
				.setNumberOfIterations(numberOfIterations);

		/*
		 * Configure electrification technology.
		 */
		double batteryCapacity_kWh = 600;
		double chargingRate_kW = 600;
		double cruiseConsumption_kWh_km = 1.5;
		double chargeWraparoundTolerance_kWh = 0.01 * batteryCapacity_kWh;
		var simulator = (DefaultSimulator<NodeWithCoords>) scenario.getOrCreateSimulator();
		simulator.setStaySimulator(new ElectrifiedStaySimulator<>(scenario).setChargeAnywhere(true));
		simulator.setMoveSimulator(new ElectrifiedMoveSimulator<>(scenario));
		simulator.setWrapAroundSimulator(new BatteryWrapAroundSimulator(batteryCapacity_kWh, chargingRate_kW,
				cruiseConsumption_kWh_km, chargeWraparoundTolerance_kWh));
		runner.addIndividualWeight(new StrictlyNonNegativeBatteryCharge<>());

		/*
		 * Load demand table.
		 */
		CSVParser demandParser;
		try {
			Path demandPath = Path
					.of("./src/main/resources/se/vti/roundtrips/examples/electrifiedFlight/annual_demand.csv");
			demandParser = CSVParser.parse(demandPath, StandardCharsets.UTF_8,
					CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		int planeSize_pax = 18;
		int fleetSize = 100;
		double demandSum = 0;
		PlaneUsageSummary planeUsage = new PlaneUsageSummary(fleetSize, planeSize_pax);
		for (CSVRecord r : demandParser) {
			var from = scenario.getNode(r.get("from_iata"));
			var to = scenario.getNode(r.get("to_iata"));
			int demand_pax_day = (int) Math.round(Double.parseDouble(r.get("flow")) / 365.0);
			if (from.computeEuclideanDistance(to) * cruiseConsumption_kWh_km <= (1.0 - 1e-8) * batteryCapacity_kWh
					&& demand_pax_day >= 0.5 * planeSize_pax) {
				log.info(from + " -> " + to + ": " + demand_pax_day + " passengers / day");
				planeUsage.setDemand(from, to, demand_pax_day);
				demandSum += demand_pax_day;
			}
		}
		log.info("total demand = " + demandSum + " pax per day");

		/*
		 * Create a random initial state.
		 */
		var initialFlightPattern = new MultiRoundTrip<NodeWithCoords>(fleetSize);
		new RandomRoundTripGenerator<>(scenario).setNumberOfStayEpisodesInterval(0, 0)
				.populateRandomly(initialFlightPattern);
		initialFlightPattern.simulateAll(scenario.getOrCreateSimulator());
		initialFlightPattern.addSummary(planeUsage);
		initialFlightPattern.recomputeSummaries();
		runner.setInitialState(initialFlightPattern);

		/*
		 * Ground inefficient planes.
		 */
		double distanceCoveredWhenFlyingNonStop_km = timeBinSize_h * numberOfTimeBins * speed_km_h;
		double minIndividual_paxKm = 0.1 * planeSize_pax * distanceCoveredWhenFlyingNonStop_km;
		double minAverage_paxKm = 0.2 * planeSize_pax * distanceCoveredWhenFlyingNonStop_km;

		runner.addPopulationWeight(new MHWeight<MultiRoundTrip<NodeWithCoords>>() {
			@Override
			public String name() {
				return "StrictForbidInefficientPlanes";
			}

			@Override
			public double logWeight(MultiRoundTrip<NodeWithCoords> fleet) {
				var summary = fleet.getSummary(PlaneUsageSummary.class);
				double total_paxKm = 0.0;
				int numberOfActivePlanes = 0;
				for (var plane : fleet) {
					if (plane.size() > 1) {
						numberOfActivePlanes++;
						double individual_paxKm = 0.0;
						List<Episode> episodes = plane.getEpisodes();
						for (int i = 1; i < episodes.size(); i += 2) {
							var move = (MoveEpisode<NodeWithCoords>) episodes.get(i);
							var from = move.getOrigin();
							var to = move.getDestination();
							double demand_pax = summary.getDemand(from, to);
							int coverage = summary.getCoverage(from, to);
							double paxKm = scenario.getDistance_km(from, to) * demand_pax / coverage;
							individual_paxKm += paxKm;
							total_paxKm += paxKm;
						}
						if (individual_paxKm < minIndividual_paxKm) {
							return Double.NEGATIVE_INFINITY;
						}
					}
				}
				if (total_paxKm < minAverage_paxKm * numberOfActivePlanes) {
					return Double.NEGATIVE_INFINITY;
				}
				return 0.0;
			}
		});

		// Define logging.
		runner.configureStatisticsLogging("./statistics.log", 1000);
		runner.addStatisticEstimator(
				new MHBatchBasedStatisticEstimator<MultiRoundTrip<NodeWithCoords>>("NumberOfActivePlanes", fleet -> {
					double active = 0;
					for (var plane : fleet) {
						if (plane.size() > 1) {
							active++;
						}
					}
					return active;
				}));
		runner.addStatisticEstimator(
				new MHBatchBasedStatisticEstimator<MultiRoundTrip<NodeWithCoords>>("NumberOfServedPassengers",
						fleet -> fleet.getSummary(PlaneUsageSummary.class).getNumberOfServedPassengers()));
		runner.addStatisticEstimator(
				new MHBatchBasedStatisticEstimator<MultiRoundTrip<NodeWithCoords>>("NumberOfMissedPassengers",
						fleet -> fleet.getSummary(PlaneUsageSummary.class).getNumberOfUnservedPassengers()));

//		runner.configureWeightLogging("./output/travelSurveyExpansion/logWeights.log", totalIterations / 100);
		// runner.addStateProcessor(
//				new PlotAgeByActivityHistogram(totalIterations / 2, totalIterations / 100, syntheticPopulation));
//		runner.configureStatisticsLogging("./output/travelSurveyExpansion/statisticsLogs.log", 1000)
//				.addStatisticEstimator(new TotalTravelTime<GridNodeWithActivity>());
//
//		// Configure sampling and run.
//		var initialRoundTrip = scenario.createInitialMultiRoundTrip(homes, Arrays.asList(0), maxFleetSize);
//		runner.setInitialState(initialRoundTrip).setMessageInterval(totalIterations / 100)
//				.setNumberOfIterations(totalIterations);

		runner.run();

		System.out.println("DONE");
	}

}
