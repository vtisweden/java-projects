/**
 * se.vti.samgods
 * 
 * Copyright (C) 2025, 2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.preprocessing.loopgeneration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.common.RandomRoundTripGenerator;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripJsonIO;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.roundtrips.samplingweights.StrictlyPeriodicSchedule;
import se.vti.roundtrips.samplingweights.priors.SingleRoundTripUniformPrior;
import se.vti.roundtrips.simulator.DefaultSimulator;
import se.vti.roundtrips.simulator.electrified.BatteryWrapAroundSimulator;
import se.vti.roundtrips.simulator.electrified.Charging;
import se.vti.roundtrips.simulator.electrified.ChargingUtils;
import se.vti.roundtrips.simulator.electrified.ElectrifiedMoveSimulator;
import se.vti.roundtrips.simulator.electrified.ElectrifiedStaySimulator;
import se.vti.roundtrips.simulator.electrified.StrictlyNonNegativeBatteryCharge;
import se.vti.samgods.common.SamgodsConfigGroup;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;
import se.vti.utils.misc.metropolishastings.MHWeight;
import se.vti.utils.misc.metropolishastings.MHWeightContainer;
import se.vti.utils.misc.metropolishastings.MHWeightsToFileLogger;
import se.vti.utils.misc.metropolishastings.terminationcriteria.BlockAverageTerminationCriterion;

/**
 * 
 * @author GunnarF
 *
 */
public class ElectrifiedSamgodsLoopSamplingRunner extends SamgodsLoopSamplingRunner {

	static final Logger log = LogManager.getLogger(ElectrifiedSamgodsLoopSamplingRunner.class);

//	static final String configFileNameLabel = "configFileName";
//	static final String initialStateLabel = "initialState";
//	static final String analysisFileNameLabel = "analysisFile";
//	static final String loopCntLabel = "loopCnt";
	static final String stationCntLabel = "stationCnt";
//	static final String maxCoverageErrorLabel = "maxCoverageError";

//	static final int periodLength_h = 7 * 24;
//	static final int timeBinSize_h = 4;
//	static final double minODCoverage = 0.001;

	static final double defaultCapacity_kWh = 728.0;
	static final double defaultChargingRate_kW = 400.0;
	static final double defaultConsumptionRate_kWh_km = 0.01 * 130.0;
	static final double chargeWraparoundTolerance_kWh = 0.001;

//	static ScenarioDataContainer dataContainer = null;
//	static Scenario<NodeWithCoords> samplingScenario = null;

//	static void loadSamgodsScenarioIntoDataContainer(SamgodsConfigGroup samgodsConfig,
//			List<List<Enum<?>>> allNodeLabels) {
//		log.info("Loading Samgods scenario.");
//
//		var loopSamplingData = new SamgodsScenarioData(SamgodsConstants.TransportMode.Road, true, samgodsConfig,
//				SamgodsConstants.Commodity.AGRICULTURE);
//		try {
//			GeoJsonWriters.writeTerminals(loopSamplingData.getNetwork(), loopSamplingData.computeTerminalNodeIds(),
//					"GeoJsonNodes.json");
//			GeoJsonWriters.writeLinks(loopSamplingData.getNetwork(), "GeoJsonLinks.json");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		var transportDurations = new TransportDurations(loopSamplingData.getNetwork(),
//				loopSamplingData.computeTerminalNodeIds(), 80.0);
//		log.info("Number of terminals is " + loopSamplingData.computeTerminalNodeIds().size());
//		log.info("Number of OD pairs is " + loopSamplingData.getOD2Demand_kTon().size());
//
//		var durationStats = new DescriptiveStatistics();
//		for (OD activeOD : loopSamplingData.getOD2Demand_kTon().keySet()) {
//			durationStats.addValue(transportDurations.getDuration_h(activeOD));
//		}
//		log.info("  Average transport duration is " + durationStats.getMean() + " hours.");
//		log.info("  Transport duration standard deviation is " + durationStats.getStandardDeviation() + " hours.");
//		log.info("  Maximum transport duration is " + durationStats.getMax() + " hours.");
//
//		dataContainer = new ScenarioDataContainer(loopSamplingData, transportDurations, allNodeLabels);
//		log.info("Total number of OD pairs: " + dataContainer.getOD2Demand_kTon_View().size());
//		log.info("Total freight demand [kTon]: " + dataContainer.getTotalDemand_kTon());
//		log.info("Demand vector length [kTon]: " + dataContainer.getDemandVectorLength_kTon());
//	}

//	static void createSamplingScenario() {
//		log.info("Creating sampling scenario.");
//
//		samplingScenario = new Scenario<NodeWithCoords>();
//		samplingScenario.setTimeBinSize_h(timeBinSize_h);
//		samplingScenario.setNumberOfTimeBins(periodLength_h / timeBinSize_h);
//
//		for (NodeWithCoords node : dataContainer.getAllSamplingNodesView()) {
//			samplingScenario.addNode(node);
//		}
//		for (NodeWithCoords from : samplingScenario.getNodesView()) {
//			for (NodeWithCoords to : samplingScenario.getNodesView()) {
//				double dur_h = dataContainer.getTransportDuration_h(from, to);
//				samplingScenario.setTime_h(from, to, dur_h);
//				double dist_km = Units.KM_PER_M
//						* Math.sqrt((to.x - from.x) * (to.x - from.x) + (to.y - from.y) * (to.y - from.y));
//				samplingScenario.setDistance_km(from, to, dist_km);
//			}
//		}
//	}

	static Integer chargingStationCount;

	static void configureSamplingScenario(String[] args) {

		var options = new Options();

		var stationCntOption = new Option(stationCntLabel, true, stationCntLabel);
		stationCntOption.setRequired(false);
		options.addOption(stationCntOption);

		try {
			CommandLine cmd = new DefaultParser().parse(options, args);
			chargingStationCount = (cmd.hasOption(stationCntOption)
					? Integer.parseInt(cmd.getOptionValue(stationCntOption))
					: null);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		var simulator = (DefaultSimulator<NodeWithCoords>) samplingScenario.getOrCreateSimulator();
		simulator.setStaySimulator(new ElectrifiedStaySimulator<>(samplingScenario));
		simulator.setMoveSimulator(new ElectrifiedMoveSimulator<>(samplingScenario));
		simulator.setWrapAroundSimulator(new BatteryWrapAroundSimulator(defaultCapacity_kWh, defaultChargingRate_kW,
				defaultConsumptionRate_kWh_km, chargeWraparoundTolerance_kWh));
	}

	static List<List<Enum<?>>> createAllNodeLabels() {
		return List.of(List.of(), List.of(Charging.YES), List.of(Charging.NO));
	}

	static void addSamplingWeights(MHWeightContainer<MultiRoundTrip<NodeWithCoords>> samplingWeights) {
		samplingWeights.add(new SingleToMultiWeight<>(new StrictlyNonNegativeBatteryCharge<>()));
		samplingWeights.add(new MaxNumberOfChargingPointsConstraint<>(chargingStationCount));
	}

	static List<NodeWithCoords> createInitialNodes() {
		var tmpNodes = new ArrayList<>(samplingScenario.getNodesView().stream()
				.filter(n -> Charging.YES == ChargingUtils.singleton().extractCharging(n)).toList());
		Collections.shuffle(tmpNodes);
		return tmpNodes.subList(0, chargingStationCount);
	}

	static int getMaxNumberOfInitialStayEpisodes() {
		return 1;
	}
	
	static void runSimulation(String[] args) {
		log.info("Started simulation ...");

		var options = new Options();

		var configFileNameOption = new Option(configFileNameLabel, true, configFileNameLabel);
		configFileNameOption.setRequired(true);
		options.addOption(configFileNameOption);

		var initialStateOption = new Option(initialStateLabel, true, initialStateLabel);
		initialStateOption.setRequired(false);
		options.addOption(initialStateOption);

		var loopCntOption = new Option(loopCntLabel, true, loopCntLabel);
		loopCntOption.setRequired(true);
		options.addOption(loopCntOption);

		var stationCntOption = new Option(stationCntLabel, true, stationCntLabel);
		stationCntOption.setRequired(false);
		options.addOption(stationCntOption);

		var maxCoverageErrorOption = new Option(maxCoverageErrorLabel, true, maxCoverageErrorLabel);
		maxCoverageErrorOption.setRequired(true);
		options.addOption(maxCoverageErrorOption);

		final SamgodsConfigGroup samgodsConfig;
		final boolean electrified;
		final String initialStateFile;
		final int loopCount;
		final Integer chargingStationCount;
		final double maxCoverageError;
		try {
			CommandLine cmd = new DefaultParser().parse(options, args);
			Config config = ConfigUtils.loadConfig(cmd.getOptionValue(configFileNameOption));
			samgodsConfig = ConfigUtils.addOrGetModule(config, SamgodsConfigGroup.class);
			initialStateFile = cmd.getOptionValue(initialStateOption);
			loopCount = Integer.parseInt(cmd.getOptionValue(loopCntOption));
			chargingStationCount = (cmd.hasOption(stationCntOption)
					? Integer.parseInt(cmd.getOptionValue(stationCntOption))
					: null);
			electrified = (chargingStationCount != null);
			maxCoverageError = Double.parseDouble(cmd.getOptionValue(maxCoverageErrorOption));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		loadSamgodsScenarioIntoDataContainer(samgodsConfig,
				electrified ? List.of(List.of(), List.of(Charging.YES), List.of(Charging.NO)) : List.of(List.of()));
		createSamplingScenario();
		if (electrified) {
			var simulator = (DefaultSimulator<NodeWithCoords>) samplingScenario.getOrCreateSimulator();
			simulator.setStaySimulator(new ElectrifiedStaySimulator<>(samplingScenario));
			simulator.setMoveSimulator(new ElectrifiedMoveSimulator<>(samplingScenario));
			simulator.setWrapAroundSimulator(new BatteryWrapAroundSimulator(defaultCapacity_kWh, defaultChargingRate_kW,
					defaultConsumptionRate_kWh_km, chargeWraparoundTolerance_kWh));
		}

		final double mu = Math.PI / Math.sqrt(2.0) / maxCoverageError / dataContainer.getDemandVectorLength_kTon();
		log.info("max coverage error = " + maxCoverageError);
		log.info("total demand = " + dataContainer.getTotalDemand_kTon() + " kTon");
		log.info("demand vector length = " + dataContainer.getDemandVectorLength_kTon() + " kTon");
		log.info("=>  mu = " + mu);

		final MHWeightContainer<MultiRoundTrip<NodeWithCoords>> samplingWeights = new MHWeightContainer<>();
		samplingWeights
				.add(new SingleToMultiWeight<>(new SingleRoundTripUniformPrior<NodeWithCoords>(samplingScenario)));
		var strictlyPeriodicWeight = new StrictlyPeriodicSchedule<NodeWithCoords>(periodLength_h);
		samplingWeights.add(new SingleToMultiWeight<>(strictlyPeriodicWeight));
		samplingWeights.add(new MHWeight<MultiRoundTrip<NodeWithCoords>>() {
			@Override
			public String name() {
				return "ODCoverageWeight";
			}

			@Override
			public double logWeight(MultiRoundTrip<NodeWithCoords> roundTrips) {
				return roundTrips.getSummary(ODCoverage.class).getLogWeight();
			}
		}, mu);
		if (electrified) {
			samplingWeights.add(new SingleToMultiWeight<>(new StrictlyNonNegativeBatteryCharge<>()));
			samplingWeights.add(new MaxNumberOfChargingPointsConstraint<>(chargingStationCount));
		}

		final MultiRoundTrip<NodeWithCoords> initialRoundTrips;
		if (initialStateFile == null) {
			initialRoundTrips = new MultiRoundTrip<>(loopCount);
			final List<NodeWithCoords> initialNodes;
			if (electrified) {
				var tmpNodes = new ArrayList<>(samplingScenario.getNodesView().stream()
						.filter(n -> Charging.YES == ChargingUtils.singleton().extractCharging(n)).toList());
				Collections.shuffle(tmpNodes);
				initialNodes = tmpNodes.subList(0, chargingStationCount);
			} else {
				initialNodes = new ArrayList<>(samplingScenario.getNodesView());
			}
			new RandomRoundTripGenerator<>(samplingScenario).setFeasibleNodes(initialNodes)
					.setFeasibilityCheck(r -> Double.isFinite(strictlyPeriodicWeight.logWeight(r)))
					.setNumberOfStayEpisodesInterval(1, electrified ? 1 : samplingScenario.getNumberOfTimeBins())
					.populateRandomly(initialRoundTrips);
		} else {
			try {
				initialRoundTrips = MultiRoundTripJsonIO.singleton().readFromFile(samplingScenario, initialStateFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		initialRoundTrips.simulateAll(samplingScenario.getOrCreateSimulator());
		initialRoundTrips
				.addSummary(new ODCoverage<NodeWithCoords>(initialRoundTrips.size(), dataContainer, minODCoverage));
		initialRoundTrips.recomputeSummaries();

//		var disconnectedFlowStat = new MHOverlappingBatchBasedStatisticEstimator<MultiRoundTrip<NodeWithCoords>>(
//				"DisconnectedFlow[kTon]",
//				roundTrips -> roundTrips.getSummary(ODCoverage.class).getDisconnectedFlow_kTon())
//				.setBatchSize(10 * initialRoundTrips.size()).setShareOfDiscardedTransients(0.4);
//		var numberOfLoopsStat = new MHOverlappingBatchBasedStatisticEstimator<MultiRoundTrip<NodeWithCoords>>(
//				"#(ConnectingLoops)",
//				roundTrips -> (double) roundTrips.getSummary(ODCoverage.class).getNumberOfConnectingRoundTripSegments())
//				.setBatchSize(10 * initialRoundTrips.size()).setShareOfDiscardedTransients(0.4);
//		var flowPerLoopStat = new MHOverlappingBatchBasedStatisticEstimator<MultiRoundTrip<NodeWithCoords>>(
//				"ConnectedFlow[kTon]/Loop", roundTrips -> {
//					var summary = roundTrips.getSummary(ODCoverage.class);
//					return summary.getConnectedFlow_kTon() / summary.getNumberOfConnectingRoundTripSegments();
//				}).setBatchSize(10 * initialRoundTrips.size()).setShareOfDiscardedTransients(0.4);

//		var statisticsLogger = new MHStatisticsToFileLogger<MultiRoundTrip<NodeWithCoords>>(1000, "./statistics.tsv");
//		statisticsLogger.add(disconnectedFlowStat);
//		statisticsLogger.add(numberOfLoopsStat);
//		statisticsLogger.add(flowPerLoopStat);

		var algo = new MHAlgorithm<>(new MultiRoundTripProposal<>(samplingScenario), samplingWeights,
				samplingScenario.getRandom());
		algo.setInitialState(initialRoundTrips);
		algo.setMsgInterval(1000);
//		algo.addStateProcessor(disconnectedFlowStat);
//		algo.addStateProcessor(numberOfLoopsStat);
//		algo.addStateProcessor(flowPerLoopStat);
//		algo.addStateProcessor(statisticsLogger);
		algo.addStateProcessor(new MHWeightsToFileLogger<>(1000, samplingWeights, "samplingWeights.tsv"));
		algo.addStateProcessor(new MHStateProcessor<MultiRoundTrip<NodeWithCoords>>() {
			int iteration;

			@Override
			public void start() {
				this.iteration = 0;
			}

			@Override
			public void processState(MultiRoundTrip<NodeWithCoords> state) {
				if (this.iteration % 100_000 == 0) {
					try {
						MultiRoundTripJsonIO.singleton().writeToFile(state, "roundTrips." + this.iteration + ".json");
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				this.iteration++;
			}

			@Override
			public void end() {
			}
		});

//		algo.run(numberOfIterations);
		algo.setTerminationCriterion(new BlockAverageTerminationCriterion<MultiRoundTrip<NodeWithCoords>>()
				.setCheckInterval(10_000).setConvergenceStatsFileName("convergenceStats.tsv").setMinSamples(100_000));
		algo.run();

		log.info("... DONE");
	}

	static void createGIS(String[] args, boolean electrified) {

		System.out.println("STARTED createGIS ...");

		Options options = new Options();

		Option configFileNameOption = new Option(configFileNameLabel, true, configFileNameLabel);
		configFileNameOption.setRequired(true);
		options.addOption(configFileNameOption);

		Option analyzedFilesFolderOption = new Option("analyzedFilesFolder", true, "analyzedFilesFolder");
		analyzedFilesFolderOption.setRequired(true);
		options.addOption(analyzedFilesFolderOption);

		final SamgodsConfigGroup samgodsConfig;
		final Path analyzedFilesFolder;
		try {
			CommandLine cmd = new DefaultParser().parse(options, args);
			Config config = ConfigUtils.loadConfig(cmd.getOptionValue(configFileNameLabel));
			samgodsConfig = ConfigUtils.addOrGetModule(config, SamgodsConfigGroup.class);
			analyzedFilesFolder = Path.of(cmd.getOptionValue("analyzedFilesFolder"));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		loadSamgodsScenarioIntoDataContainer(samgodsConfig,
				electrified ? List.of(List.of(), List.of(Charging.YES), List.of(Charging.NO)) : List.of(List.of()));
		createSamplingScenario();
		if (electrified) {
			var simulator = (DefaultSimulator<NodeWithCoords>) samplingScenario.getOrCreateSimulator();
			simulator.setStaySimulator(new ElectrifiedStaySimulator<>(samplingScenario));
			simulator.setMoveSimulator(new ElectrifiedMoveSimulator<>(samplingScenario));
			simulator.setWrapAroundSimulator(new BatteryWrapAroundSimulator(defaultCapacity_kWh, defaultChargingRate_kW,
					defaultConsumptionRate_kWh_km, chargeWraparoundTolerance_kWh));
		}

		List<MultiRoundTrip<NodeWithCoords>> allRoundTrips = new ArrayList<>();
		try {
			for (java.nio.file.Path path : java.nio.file.Files.list(analyzedFilesFolder).collect(Collectors.toList())) {
				System.out.println("Loading sample: " + path.toString());
				MultiRoundTrip<NodeWithCoords> roundTrips = MultiRoundTripJsonIO.singleton()
						.readFromFile(samplingScenario, path.toAbsolutePath().toString());
				roundTrips.addSummary(new ODCoverage<NodeWithCoords>(roundTrips.size(), dataContainer, minODCoverage));
				roundTrips.recomputeSummaries();
				allRoundTrips.add(roundTrips);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		GeoJsonWriters.writeCoverage(dataContainer, allRoundTrips);

		System.out.println("... DONE");
	}

	public static void main(String[] args) {

		createGIS(args, true);

//		runSimulation(args);

//		runSimulation(new String[] { "-configFileName", "./input/config.xml", "-loopCnt", "1000", "-stationCnt", "200",
//				"-maxCoverageError", "0.1"});

//		runSimulation(new String[] { "-configFileName", "./input/config.xml", "-coverageWeight", "100.0", "-loopCnt",
//				"1000", "-initialState", "roundTrips.7700000.json" });

//		createGIS(new String[] { "-" + configFileNameLabel, "./input/config.xml", "-" + analysisFileNameLabel,
//				"./tmp/noStations.json" }, "./noStations.", false);
//		createGIS(new String[] { "-" + configFileNameLabel, "./input/config.xml", "-" + analysisFileNameLabel,
//		"./tmp/100stations.json" }, "./100stations.", true);
//		createGIS(new String[] { "-" + configFileNameLabel, "./input/config.xml", "-" + analysisFileNameLabel,
//		"./tmp/50stations.json" }, "./50stations.", true);
//		createGIS(new String[] { "-" + configFileNameLabel, "./input/config.xml", "-" + analysisFileNameLabel,
//		"./tmp/25stations.json" }, "./25stations.", true);
//		createGIS(new String[] { "-" + configFileNameLabel, "./input/config.xml", "-" + analysisFileNameLabel,
//		"./tmp/12stations.json" }, "./12stations.", true);

//		evaluateCharging(new String[] { "-" + configFileNameLabel, "./input/config.xml", "-" + analysisFileNameLabel,
//				"roundTrips.1000000.json" });
	}
}
