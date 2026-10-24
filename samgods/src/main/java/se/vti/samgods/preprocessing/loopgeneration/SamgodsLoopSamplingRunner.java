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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

import se.vti.roundtrips.examples.samplingweights.StrictlyPeriodicSchedule;
import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.multi.MultiRoundTripJsonIO;
import se.vti.roundtrips.runner.Runner;
import se.vti.roundtrips.scenario.NodeWithCoords;
import se.vti.roundtrips.scenario.RandomRoundTripGenerator;
import se.vti.roundtrips.scenario.Scenario;
import se.vti.roundtrips.scenario.ScenarioBuilder;
import se.vti.samgods.common.SamgodsConfigGroup;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.preprocessing.loopgeneration.ODCoverage.Connectivity;
import se.vti.utils.misc.metropolishastings.MHWeight;
import se.vti.utils.misc.metropolishastings.terminationcriteria.BlockAverageTerminationCriterion;

/**
 * 
 * @author GunnarF
 *
 */
class SamgodsLoopSamplingRunner {

	// -------------------- CONSTANTS --------------------

	static final Logger log = LogManager.getLogger(SamgodsLoopSamplingRunner.class);

	static final String configFileNameLabel = "configFileName";
	static final String initialStateLabel = "initialState";
	static final String analysisFileNameLabel = "analysisFile";
	static final String loopCntLabel = "loopCnt";
	static final String maxCoverageErrorLabel = "maxCoverageError";
	static final String commodityLabel = "commodity";
	static final String transportModeLabel = "transportMode";
	static final String nodesFileLabel = "nodesFile";
	static final String linksFileLabel = "linksFile";
	static final String connectivityLabel = "connectivity";
	static final String precisionLabel = "precision";

	static final int periodLength_h = 7 * 24;
	static final int timeBinSize_h = 4;
	static final double minODCoverage = 0.001;

	// -------------------- MEMBERS --------------------

	final SamgodsSamplingVariant samplingVariant;

	final SamgodsConfigGroup samgodsConfig;

	final String initialStateFile;

	final int loopCount;

	final SamgodsConstants.Commodity commodity;

	final SamgodsConstants.TransportMode transportMode;

	final ODCoverage.Connectivity connectivity;

	// may be null below

	final Double maxCoverageError;

	final Double precision;

	// -------------------- ADDITIONAL MEMBERS --------------------

	ScenarioContainer dataContainer = null;

	Scenario<NodeWithCoords> samplingScenario = null;

	// -------------------- CONSTRUCTION --------------------

	SamgodsLoopSamplingRunner(String[] args, SamgodsSamplingVariant samplingVariant) {
		this.samplingVariant = samplingVariant;

		var options = new Options();

		var configFileNameOption = new Option(configFileNameLabel, true, configFileNameLabel);
		configFileNameOption.setRequired(true);
		options.addOption(configFileNameOption);

		var loopCntOption = new Option(loopCntLabel, true, loopCntLabel);
		loopCntOption.setRequired(true);
		options.addOption(loopCntOption);

		var commodityOption = new Option(commodityLabel, true, commodityLabel);
		commodityOption.setRequired(true);
		options.addOption(commodityOption);

		var transportModeOption = new Option(transportModeLabel, true, transportModeLabel);
		transportModeOption.setRequired(true);
		options.addOption(transportModeOption);

//		var nodesFileOption = new Option(nodesFileLabel, true, nodesFileLabel);
//		nodesFileOption.setRequired(false);
//		options.addOption(nodesFileOption);

//		var linksFileOption = new Option(linksFileLabel, true, linksFileLabel);
//		linksFileOption.setRequired(false);
//		options.addOption(linksFileOption);

		var connectivityOption = new Option(connectivityLabel, true, connectivityLabel);
		connectivityOption.setRequired(true);
		options.addOption(connectivityOption);

		// SIMULATION OPTIONS (could make this a sampling variant)

		var initialStateOption = new Option(initialStateLabel, true, initialStateLabel);
		initialStateOption.setRequired(false);
		options.addOption(initialStateOption);

		var maxCoverageErrorOption = new Option(maxCoverageErrorLabel, true, maxCoverageErrorLabel);
		maxCoverageErrorOption.setRequired(false);
		options.addOption(maxCoverageErrorOption);

		var precisionOption = new Option(precisionLabel, true, precisionLabel);
		precisionOption.setRequired(false);
		options.addOption(precisionOption);

		// DERIVED OPTIONS

		this.samplingVariant.configureOptions(options);

		final CommandLine cmd;
		try {
			cmd = new DefaultParser().parse(options, args);
			Config config = ConfigUtils.loadConfig(cmd.getOptionValue(configFileNameOption));

			this.samgodsConfig = ConfigUtils.addOrGetModule(config, SamgodsConfigGroup.class);
			this.initialStateFile = cmd.getOptionValue(initialStateOption);
			this.loopCount = Integer.parseInt(cmd.getOptionValue(loopCntOption));
			this.commodity = SamgodsConstants.Commodity.valueOf(cmd.getOptionValue(commodityOption));
			this.transportMode = SamgodsConstants.TransportMode.valueOf(cmd.getOptionValue(transportModeOption));
			this.connectivity = ODCoverage.Connectivity.valueOf(cmd.getOptionValue(connectivityOption));

			this.maxCoverageError = cmd.getOptionValue(maxCoverageErrorOption) == null ? null
					: Double.parseDouble(cmd.getOptionValue(maxCoverageErrorOption));
			this.precision = cmd.getOptionValue(precisionOption) == null ? null
					: Double.parseDouble(cmd.getOptionValue(precisionOption));

		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		var samgodsScenario = new SamgodsScenario(this.transportMode, true, samgodsConfig, this.commodity);
		try {
			GeoJsonWriters.writeTerminals(samgodsScenario.getNetwork(), samgodsScenario.computeTerminalNodeIds(),
					"GeoJsonNodes.json");
			GeoJsonWriters.writeLinks(samgodsScenario.getNetwork(), "GeoJsonLinks.json");
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Number of terminals is " + samgodsScenario.computeTerminalNodeIds().size());
		log.info("Number of OD pairs is " + samgodsScenario.getOD2Demand_kTon().size());

		this.dataContainer = new ScenarioContainer(samgodsScenario, this.samplingVariant.createNodeLabels());
		log.info("Total number of OD pairs: " + dataContainer.getOD2Demand_kTon_View().size());
		log.info("Total freight demand [kTon]: " + dataContainer.getTotalDemand_kTon());
		log.info("Demand vector length [kTon]: " + dataContainer.getDemandVectorLength_kTon());

		var scenarioBuilder = new ScenarioBuilder<NodeWithCoords>().setTimeBinSize_h(timeBinSize_h)
				.setNumberOfTimeBins(periodLength_h / timeBinSize_h);
		scenarioBuilder.addNodes(this.dataContainer.getAllSamplingNodesView());
		scenarioBuilder.setMoveDistanceFunction((a, b) -> this.dataContainer.getTransportDuration_h(a, b));
		scenarioBuilder.setMoveTimeFunction((a, b) -> this.dataContainer.getTransportDuration_h(a, b));
		this.samplingScenario = scenarioBuilder.build();

		this.samplingVariant.configureScenario(cmd, this.dataContainer, this.samplingScenario);
	}

	void runSimulation() {

		var runner = new Runner<NodeWithCoords>(this.samplingScenario);

		final double mu = Math.PI / Math.sqrt(2.0) / this.maxCoverageError / dataContainer.getDemandVectorLength_kTon();
		log.info("Computing uncertainty scale");
		log.info("  max coverage error = " + this.maxCoverageError);
		log.info("  total demand = " + this.dataContainer.getTotalDemand_kTon() + " kTon");
		log.info("  demand vector length = " + this.dataContainer.getDemandVectorLength_kTon() + " kTon");
		log.info("  =>  mu = " + mu);

//		runner.setIndividualBinomialPrior(0.5 * this.samplingScenario.getNumberOfTimeBins());
		runner.setIndividualUniformPrior();

		runner.addSummary(
				new ODCoverage<NodeWithCoords>(this.loopCount, this.dataContainer, minODCoverage, this.connectivity));

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

		this.samplingVariant.addSamplingWeights(runner, this.dataContainer, this.samplingScenario);

		final MultiRoundTrip<NodeWithCoords> initialRoundTrips;
		if (initialStateFile == null) {
			initialRoundTrips = new MultiRoundTrip<>(loopCount);
			var generator = new RandomRoundTripGenerator<>(samplingScenario)
					.setFeasibleNodes(this.samplingScenario.getNodesView())
					.setFeasibilityCheck(r -> Double.isFinite(strictlyPeriodicWeight.logWeight(r)))
					.setNumberOfStayEpisodesInterval(1, this.samplingScenario.getNumberOfTimeBins());
			this.samplingVariant.configureInitialRoundTripGenerator(generator, this.dataContainer,
					this.samplingScenario);
			generator.populateRandomly(initialRoundTrips);
		} else {
			try {
				initialRoundTrips = MultiRoundTripJsonIO.singleton().readFromFile(this.samplingScenario,
						this.initialStateFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		runner.setInitialState(initialRoundTrips);

		runner.setMessageInterval(10_000);
		runner.configureWeightLogging("samplingWeights.tsv", 10_000);
		runner.configureStateLogging(Paths.get("./states"), "./roundTrips", 100_000);
		runner.setTerminationCriterion(new BlockAverageTerminationCriterion<MultiRoundTrip<NodeWithCoords>>()
				.setCheckInterval(10 * this.loopCount).setConvergenceStatsFileName("convergenceStats.tsv")
				.setMinSamples(100_000).setRelativeVarianceTolerance(this.precision)
				.setStandardizedMeanTolerance(this.precision));
		runner.run();

		log.info("... DONE");
	}

	static int readBurnInIteration(Path file) throws IOException {
		var lines = Files.readAllLines(file);
		if (lines.size() < 2) {
			throw new IllegalArgumentException("File contains no data rows.");
		}
		var header = lines.get(0).split("\t");
		int burnInIndex = -1;
		int stabilizedIndex = -1;
		for (int i = 0; i < header.length; i++) {
			if (header[i].equals("burnInIteration")) {
				burnInIndex = i;
			} else if (header[i].equals("stabilized")) {
				stabilizedIndex = i;
			}
		}
		if (burnInIndex < 0 || stabilizedIndex < 0) {
			throw new IllegalArgumentException("Missing burnInIteration or stabilized column.");
		}
		for (int i = 1; i < lines.size(); i++) {
			var fields = lines.get(i).split("\t");
			if (Boolean.parseBoolean(fields[stabilizedIndex])) {
				if (i != lines.size() - 1) {
					throw new IllegalArgumentException("First stabilized=true is not in the last row.");
				}
				return Integer.parseInt(fields[burnInIndex]);
			}
		}
		throw new IllegalArgumentException("No stabilized=true row found.");
	}

	void analyzeStates() throws IOException {

		int burnInIteration = readBurnInIteration(Paths.get("./convergenceStats.tsv"));

		var odCoverage = new ODCoverage<NodeWithCoords>(this.loopCount, this.dataContainer, minODCoverage,
				this.connectivity);
		var efficiencyAnalyzer = new TransportEfficiencyAnalyzer(this.dataContainer, 0.0, 0.0, 1);
		var stateAnalyzer = new SamgodsLoopsAnalyzer(this.samplingScenario, odCoverage, efficiencyAnalyzer);

		stateAnalyzer.run(Paths.get("./states"), "roundTrips.", ".json", burnInIteration, Integer.MAX_VALUE);
	}

	void createGIS(String[] args) {

		System.out.println("STARTED createGIS ...");

		List<MultiRoundTrip<NodeWithCoords>> allRoundTrips = new ArrayList<>();
		try {
			for (java.nio.file.Path path : java.nio.file.Files.list(Path.of("./results"))
					.collect(Collectors.toList())) {
				System.out.println("Loading sample: " + path.toString());
				MultiRoundTrip<NodeWithCoords> roundTrips = MultiRoundTripJsonIO.singleton()
						.readFromFile(samplingScenario, path.toAbsolutePath().toString());
				roundTrips.addSummary(new ODCoverage<NodeWithCoords>(roundTrips.size(), dataContainer, minODCoverage,
						Connectivity.FULL));
				roundTrips.recomputeSummaries();
				allRoundTrips.add(roundTrips);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		GeoJsonWriters.writeCoverage(this.dataContainer, allRoundTrips);

		System.out.println("... DONE");
	}

	public static void main(String[] args) throws IOException {

		new SamgodsLoopSamplingRunner(args, new SamgodsSamplingVariant() {
		}).analyzeStates();

//		new SamgodsLoopSamplingRunner(
//				new String[] { "-configFileName", "config.xml", "-loopCnt", "1000", 
//						"-commodity", "AGRICULTURE", "-transportMode", "Road", "-connectivity", "DIRECT" },
//				new SamgodsSamplingVariant() {
//				}).analyzeStates();

//		new SamgodsLoopSamplingRunner(
//				new String[] { "-configFileName", "config.xml", "-loopCnt", "1000", "-maxCoverageError", "0.1",
//						"-commodity", "AGRICULTURE", "-transportMode", "Road", "-connectivity", "DIRECT" },
//				new SamgodsSamplingVariant() {
//				}).runSimulation();

//		new SamgodsLoopSamplingRunner(
//				new String[] { "-configFileName", "config.xml", "-loopCnt", "1000", "-stationCnt", "200",
//						"-maxCoverageError", "0.1", "-commodity", "AGRICULTURE", "-transportMode", "Road",
//						"-stationPlacement", "maxThroughput", "-connectivity", "DIRECT" },
//				new ElectrifiedSamplingVariant()).runSimulation();

//		new SamgodsLoopSamplingRunner(args, new ElectrifiedSamplingVariant()).runSimulation();

//		new SamgodsLoopSamplingRunner(args).createGIS(args, false);
	}
}
