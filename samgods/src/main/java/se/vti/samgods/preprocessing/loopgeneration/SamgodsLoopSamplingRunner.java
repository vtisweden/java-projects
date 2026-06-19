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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.common.RandomRoundTripGenerator;
import se.vti.roundtrips.common.Runner;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.common.ScenarioBuilder;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripJsonIO;
import se.vti.roundtrips.samplingweights.StrictlyPeriodicSchedule;
import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConfigGroup;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.utils.misc.Units;
import se.vti.utils.misc.metropolishastings.MHWeight;
import se.vti.utils.misc.metropolishastings.terminationcriteria.BlockAverageTerminationCriterion;

/**
 * 
 * @author GunnarF
 *
 */
class SamgodsLoopSamplingRunner {

	static final Logger log = LogManager.getLogger(SamgodsLoopSamplingRunner.class);

	static final String configFileNameLabel = "configFileName";
	static final String initialStateLabel = "initialState";
	static final String analysisFileNameLabel = "analysisFile";
	static final String loopCntLabel = "loopCnt";
	static final String maxCoverageErrorLabel = "maxCoverageError";
	static final String commodityLabel = "commodity";
	static final String transportModeLabel = "transportMode";

	static final int periodLength_h = 7 * 24;
	static final int timeBinSize_h = 4;
	static final double minODCoverage = 0.001;

	ScenarioDataContainer dataContainer = null;
	Scenario<NodeWithCoords> samplingScenario = null;

	final SamgodsConfigGroup samgodsConfig;
	final double maxCoverageError;
	final String initialStateFile;
	final int loopCount;
	final SamgodsConstants.Commodity commodity;
	final SamgodsConstants.TransportMode transportMode;

	SamgodsLoopSamplingRunner(String[] args) {

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

		var commodityOption = new Option(commodityLabel, true, commodityLabel);
		commodityOption.setRequired(true);
		options.addOption(commodityOption);

		var transportModeOption = new Option(transportModeLabel, true, transportModeLabel);
		transportModeOption.setRequired(true);
		options.addOption(transportModeOption);

		var maxCoverageErrorOption = new Option(maxCoverageErrorLabel, true, maxCoverageErrorLabel);
		maxCoverageErrorOption.setRequired(true);
		options.addOption(maxCoverageErrorOption);

		try {
			CommandLine cmd = new DefaultParser().parse(options, args);
			Config config = ConfigUtils.loadConfig(cmd.getOptionValue(configFileNameOption));
			this.samgodsConfig = ConfigUtils.addOrGetModule(config, SamgodsConfigGroup.class);
			this.initialStateFile = cmd.getOptionValue(initialStateOption);
			this.loopCount = Integer.parseInt(cmd.getOptionValue(loopCntOption));
			this.commodity = SamgodsConstants.Commodity.valueOf(cmd.getOptionValue(commodityOption));
			this.transportMode = SamgodsConstants.TransportMode.valueOf(cmd.getOptionValue(transportModeOption));
			this.maxCoverageError = Double.parseDouble(cmd.getOptionValue(maxCoverageErrorOption));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		log.info("Loading Samgods scenario.");

		var loopSamplingData = new SamgodsScenarioData(this.transportMode, true, samgodsConfig, this.commodity);
		try {
			GeoJsonWriters.writeTerminals(loopSamplingData.getNetwork(), loopSamplingData.computeTerminalNodeIds(),
					"GeoJsonNodes.json");
			GeoJsonWriters.writeLinks(loopSamplingData.getNetwork(), "GeoJsonLinks.json");
		} catch (IOException e) {
			e.printStackTrace();
		}

		var transportDurations = new TransportDurations(loopSamplingData.getNetwork(),
				loopSamplingData.computeTerminalNodeIds(), 80.0);
		log.info("Number of terminals is " + loopSamplingData.computeTerminalNodeIds().size());
		log.info("Number of OD pairs is " + loopSamplingData.getOD2Demand_kTon().size());

		var durationStats = new DescriptiveStatistics();
		for (OD activeOD : loopSamplingData.getOD2Demand_kTon().keySet()) {
			durationStats.addValue(transportDurations.getDuration_h(activeOD));
		}
		log.info("  Average transport duration is " + durationStats.getMean() + " hours.");
		log.info("  Transport duration standard deviation is " + durationStats.getStandardDeviation() + " hours.");
		log.info("  Maximum transport duration is " + durationStats.getMax() + " hours.");

		List<List<Enum<?>>> allNodeLabels = List.of(List.of());
		this.addToNodeLabels(allNodeLabels);
		this.dataContainer = new ScenarioDataContainer(loopSamplingData, transportDurations, allNodeLabels);
		log.info("Total number of OD pairs: " + dataContainer.getOD2Demand_kTon_View().size());
		log.info("Total freight demand [kTon]: " + dataContainer.getTotalDemand_kTon());
		log.info("Demand vector length [kTon]: " + dataContainer.getDemandVectorLength_kTon());

		log.info("Creating sampling scenario.");

		var scenarioBuilder = new ScenarioBuilder<NodeWithCoords>().setTimeBinSize_h(timeBinSize_h)
				.setNumberOfTimeBins(periodLength_h / timeBinSize_h);
		scenarioBuilder.addNodes(this.dataContainer.getAllSamplingNodesView());
		scenarioBuilder.setMoveDistanceFunction(
				(a, b) -> Units.KM_PER_M * Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)));
		scenarioBuilder.setMoveTimeFunction((a, b) -> this.dataContainer.getTransportDuration_h(a, b));

		this.samplingScenario = scenarioBuilder.build();
		configureSamplingScenario(args);
	}

	// HOOKS FOR SUBCLASSING (quick fix to not lose electrification functionality)

	void configureSamplingScenario(String[] args) {
	}

	void addToNodeLabels(List<List<Enum<?>>> allNodeLabels) {
	}

	void parametrizeInitialRoundTripGenerator(RandomRoundTripGenerator<NodeWithCoords> generator) {
	}

	List<NodeWithCoords> createInitialNodes() {
		return new ArrayList<>(this.samplingScenario.getNodesView());
	}

	int getMaxNumberOfInitialStayEpisodes() {
		return this.samplingScenario.getNumberOfTimeBins();
	}

	void addToSamplingWeights(Runner<NodeWithCoords> runner) {
	}

	void runSimulation() {

		log.info("Started simulation ...");

		final double mu = Math.PI / Math.sqrt(2.0) / this.maxCoverageError / dataContainer.getDemandVectorLength_kTon();
		log.info("max coverage error = " + this.maxCoverageError);
		log.info("total demand = " + this.dataContainer.getTotalDemand_kTon() + " kTon");
		log.info("demand vector length = " + this.dataContainer.getDemandVectorLength_kTon() + " kTon");
		log.info("=>  mu = " + mu);

		var runner = new Runner<NodeWithCoords>(this.samplingScenario);
		runner.setUniformPrior();
		var strictlyPeriodicWeight = new StrictlyPeriodicSchedule<NodeWithCoords>(periodLength_h);
		runner.addIndividualWeight(strictlyPeriodicWeight);
		runner.addPopulationWeight(new MHWeight<MultiRoundTrip<NodeWithCoords>>() {
			@Override
			public String name() {
				return "ODCoverageWeight";
			}

			@Override
			public double logWeight(MultiRoundTrip<NodeWithCoords> roundTrips) {
				return roundTrips.getSummary(ODCoverage.class).getLogWeight();
			}
		}, mu);
		this.addToSamplingWeights(runner);

		final MultiRoundTrip<NodeWithCoords> initialRoundTrips;
		if (initialStateFile == null) {
			initialRoundTrips = new MultiRoundTrip<>(loopCount);
			final List<NodeWithCoords> initialNodes = createInitialNodes();
			var generator = new RandomRoundTripGenerator<>(samplingScenario).setFeasibleNodes(initialNodes)
					.setFeasibilityCheck(r -> Double.isFinite(strictlyPeriodicWeight.logWeight(r)))
					.setNumberOfStayEpisodesInterval(1, getMaxNumberOfInitialStayEpisodes());
			parametrizeInitialRoundTripGenerator(generator);
			generator.populateRandomly(initialRoundTrips);

		} else {
			try {
				initialRoundTrips = MultiRoundTripJsonIO.singleton().readFromFile(samplingScenario, initialStateFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		initialRoundTrips.simulateAll(this.samplingScenario.getOrCreateSimulator());
		initialRoundTrips
				.addSummary(new ODCoverage<NodeWithCoords>(initialRoundTrips.size(), dataContainer, minODCoverage));
		initialRoundTrips.recomputeSummaries();
		runner.setInitialState(initialRoundTrips);

		runner.setMessageInterval(1000);
		runner.configureWeightLogging("samplingWeights.tsv", 1000);
		runner.configureStateDumping("./roundTrips", 100_000);
		runner.setTerminationCriterion(new BlockAverageTerminationCriterion<MultiRoundTrip<NodeWithCoords>>()
				.setCheckInterval(10_000).setConvergenceStatsFileName("convergenceStats.tsv").setMinSamples(100_000));
		runner.run();

		log.info("... DONE");
	}

	void createGIS(String[] args, boolean electrified) {

		System.out.println("STARTED createGIS ...");

//		Options options = new Options();
//
//		Option configFileNameOption = new Option(configFileNameLabel, true, configFileNameLabel);
//		configFileNameOption.setRequired(true);
//		options.addOption(configFileNameOption);
//
//		Option analyzedFilesFolderOption = new Option("analyzedFilesFolder", true, "analyzedFilesFolder");
//		analyzedFilesFolderOption.setRequired(true);
//		options.addOption(analyzedFilesFolderOption);
//
//		final SamgodsConfigGroup samgodsConfig;
//		final Path analyzedFilesFolder;
//		try {
//			CommandLine cmd = new DefaultParser().parse(options, args);
//			Config config = ConfigUtils.loadConfig(cmd.getOptionValue(configFileNameLabel));
//			samgodsConfig = ConfigUtils.addOrGetModule(config, SamgodsConfigGroup.class);
//			analyzedFilesFolder = Path.of(cmd.getOptionValue("analyzedFilesFolder"));
//		} catch (ParseException e) {
//			throw new RuntimeException(e);
//		}
//
//		loadSamgodsScenarioIntoDataContainer(samgodsConfig,
////				electrified ? List.of(List.of(), List.of(Charging.YES), List.of(Charging.NO)) : List.of(List.of()));
//				createAllNodeLabels());
//		createSamplingScenario();
////		if (electrified) {
////			var simulator = (DefaultSimulator<NodeWithCoords>) samplingScenario.getOrCreateSimulator();
////			simulator.setStaySimulator(new ElectrifiedStaySimulator<>(samplingScenario));
////			simulator.setMoveSimulator(new ElectrifiedMoveSimulator<>(samplingScenario));
////			simulator.setWrapAroundSimulator(new BatteryWrapAroundSimulator(defaultCapacity_kWh, defaultChargingRate_kW,
////					defaultConsumptionRate_kWh_km, chargeWraparoundTolerance_kWh));
////		}
//		configureSamplingScenario(args);

		Options options = new Options();
		Option analyzedFilesFolderOption = new Option("analyzedFilesFolder", true, "analyzedFilesFolder");
		analyzedFilesFolderOption.setRequired(true);
		options.addOption(analyzedFilesFolderOption);
		final Path analyzedFilesFolder;
		try {
			CommandLine cmd = new DefaultParser().parse(options, args);
			analyzedFilesFolder = Path.of(cmd.getOptionValue("analyzedFilesFolder"));
		} catch (ParseException e) {
			throw new RuntimeException(e);
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

		new SamgodsLoopSamplingRunner(args).runSimulation();

//		new SamgodsLoopSamplingRunner(new String[] { "-configFileName", "config.xml", "-loopCnt", "1000",
//				"-maxCoverageError", "0.1", "-commodity", "AGRICULTURE", "-transportMode", "Road" }).runSimulation();

//		new SamgodsLoopSamplingRunner(args).createGIS(args, false);

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
