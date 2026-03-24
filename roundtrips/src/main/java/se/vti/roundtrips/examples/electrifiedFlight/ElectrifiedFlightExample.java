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

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.common.RandomRoundTripGenerator;
import se.vti.roundtrips.common.Runner;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.common.ScenarioBuilder;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.samplingweights.StrictlyPeriodicSchedule;
import se.vti.roundtrips.simulator.DefaultSimulator;
import se.vti.roundtrips.simulator.electrified.BatteryWrapAroundSimulator;
import se.vti.roundtrips.simulator.electrified.ElectrifiedMoveSimulator;
import se.vti.roundtrips.simulator.electrified.ElectrifiedStaySimulator;
import se.vti.roundtrips.simulator.electrified.StrictlyNonNegativeBatteryCharge;
import se.vti.utils.misc.Tuple;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class ElectrifiedFlightExample {

	static Logger log = LogManager.getLogger(ElectrifiedFlightExample.class);

	static double timeBinSize_h = 1.0;
	static int numberOfTimeBins = 24;

	static double speed_km_h = 300.0;
	static double batteryCapacity_kWh = 600;
	static double chargingRate_kW = 600;
	static double cruiseConsumption_kWh_km = 1.5;
	static double chargeWraparoundTolerance_kWh = 0.01 * batteryCapacity_kWh;

	static int planeSize_pax = 18;
	static int fleetSize = 200;

	static final Scenario<NodeWithCoords> scenario;
	static final Map<Tuple<NodeWithCoords, NodeWithCoords>, Double> od2Demand_pax;
	static final Map<NodeWithCoords, Double> node2Demand_paxKm;
	static {
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
		scenarioBuilder.setMoveDistanceFunction((from, to) -> from.computeEuclideanDistance(to));
		scenarioBuilder.setMoveTimeFunction((from, to) -> from.computeEuclideanDistance(to) / speed_km_h);
		/*
		 * Create scenario instance.
		 */
		scenario = scenarioBuilder.build();

		/*
		 * Load demand.
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
		od2Demand_pax = new LinkedHashMap<>();
		node2Demand_paxKm = new LinkedHashMap<>();
		for (CSVRecord r : demandParser) {
			var from = scenario.getNode(r.get("from_iata"));
			var to = scenario.getNode(r.get("to_iata"));
			double pax = Double.parseDouble(r.get("flow")) / 365.0;
			if (from.computeEuclideanDistance(to) * cruiseConsumption_kWh_km <= (1.0 - 1e-8) * batteryCapacity_kWh) {
				od2Demand_pax.put(new Tuple<>(from, to), pax);
				double paxKm = pax * from.computeEuclideanDistance(to);
				node2Demand_paxKm.compute(from, (n, s) -> (s == null) ? paxKm : (s + paxKm));
				node2Demand_paxKm.compute(to, (n, s) -> (s == null) ? paxKm : (s + paxKm));
			}
		}
	}

	static FleetPerformanceLogger run(double servedDemandShareWeight, double emptySeatShareWeight) {

		var runner = new Runner<>(scenario).setUniformPrior()
				.addIndividualWeight(new StrictlyPeriodicSchedule<NodeWithCoords>(24.0))
				.setNumberOfIterations(totalIterations);

		/*
		 * Configure electrification technology.
		 */
		var simulator = (DefaultSimulator<NodeWithCoords>) scenario.getOrCreateSimulator();
		simulator.setStaySimulator(new ElectrifiedStaySimulator<>(scenario).setChargeAnywhere(true));
		simulator.setMoveSimulator(new ElectrifiedMoveSimulator<>(scenario));
		simulator.setWrapAroundSimulator(new BatteryWrapAroundSimulator(batteryCapacity_kWh, chargingRate_kW,
				cruiseConsumption_kWh_km, chargeWraparoundTolerance_kWh));
		runner.addIndividualWeight(new StrictlyNonNegativeBatteryCharge<>());

		/*
		 * Configure demand analysis.
		 */
		var planeUsage = new PlaneUsageSummary(scenario, fleetSize, planeSize_pax);
		for (var odEntry : od2Demand_pax.entrySet()) {
			var od = odEntry.getKey();
			var from = od.getA();
			var to = od.getB();
			if (from.computeEuclideanDistance(to) * cruiseConsumption_kWh_km <= (1.0 - 1e-8) * batteryCapacity_kWh) {
				planeUsage.setDemand(from, to, odEntry.getValue());
			}
		}

		/*
		 * Create an "all grounded" initial state.
		 */
		var initialFlightPattern = new MultiRoundTrip<NodeWithCoords>(fleetSize);
		new RandomRoundTripGenerator<>(scenario).setNumberOfStayEpisodesInterval(0, 0)
				.populateRandomly(initialFlightPattern);
		initialFlightPattern.simulateAll(scenario.getOrCreateSimulator());
		initialFlightPattern.addSummary(planeUsage);
		initialFlightPattern.recomputeSummaries();
		runner.setInitialState(initialFlightPattern);

		/*
		 * Objective function: reduce empty seats, increase served demand.
		 */
		runner.addPopulationWeight(new MHWeight<MultiRoundTrip<NodeWithCoords>>() {

			@Override
			public String name() {
				return "ScenarioWeight(servedDemandShareWeight=" + servedDemandShareWeight + ",emptySeatShareWeight="
						+ emptySeatShareWeight + ")";
			}

			@Override
			public double logWeight(MultiRoundTrip<NodeWithCoords> fleet) {
				var summary = fleet.getSummary(PlaneUsageSummary.class);
				return -emptySeatShareWeight * summary.emptySeats_npaxKm / summary.totalDemand_paxKm
						+ servedDemandShareWeight * summary.servedDemand_paxKm / summary.totalDemand_paxKm;
			}
		});

		/*
		 * Configure logging.
		 */
		var fleetPerformanceLogger = new FleetPerformanceLogger(burnInIterations, samplingInterval);
		runner.addStateProcessor(fleetPerformanceLogger);

		/*
		 * Run experiment.
		 */
		runner.run();
		return fleetPerformanceLogger;
	}

	static final long burnInIterations = 100_000;
	static final long samplingInterval = 10_000;
	static final long totalIterations = 1_100_000;

	public static void main(String[] args) throws Exception {

		PrintWriter nodeUsageWriter = new PrintWriter("nodeUsages.log");
		nodeUsageWriter.println(
				"\tservedDemandShareWeight\temptySeatShareWeight\tnumberOfUsedPlanesSum\tservedDemand_paxKm\temptySeatsSum_npaxKm\t"
						+ scenario.getNodesView().stream().map(n -> n.getBasicName())
								.collect(Collectors.joining("\t")));
		nodeUsageWriter.println("TotalDemand(paxKm)\t\t\t\t\t\t" + scenario.getNodesView().stream()
				.map(n -> "" + node2Demand_paxKm.getOrDefault(n, 0.0)).collect(Collectors.joining("\t")));
		nodeUsageWriter.flush();

		// var range = List.of(0, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096);
		var servedDemandShareRange = List.of(0, 400, 800);
		var emptySeatShareRange = List.of(-200, 200, 600, 1000);
		for (double servedDemandShareWeight : servedDemandShareRange) {
			for (double emptySeatShareWeight : emptySeatShareRange) {
				var logger = run(servedDemandShareWeight, emptySeatShareWeight);
				nodeUsageWriter.println("\t" + servedDemandShareWeight + "\t" + emptySeatShareWeight + "\t"
						+ (logger.numberOfUsedPlanesSum / logger.samples()) + "\t"
						+ (logger.servedDemand_paxKm / logger.samples()) + "\t"
						+ (logger.emptySeatsSum_npaxKm / logger.samples()) + "\t"
						+ logger.getAvgServedDemand_pax(scenario.getNodesView()).stream().map(x -> "" + x)
								.collect(Collectors.joining("\t")));
				nodeUsageWriter.flush();
			}
		}
		nodeUsageWriter.close();

		System.out.println("DONE");
	}

}
