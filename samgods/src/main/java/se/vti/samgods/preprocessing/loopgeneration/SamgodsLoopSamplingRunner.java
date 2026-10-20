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

import se.vti.roundtrips.examples.samplingweights.StrictlyPeriodicSchedule;
import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.multi.MultiRoundTripJsonIO;
import se.vti.roundtrips.runner.Runner;
import se.vti.roundtrips.scenario.NodeWithCoords;
import se.vti.roundtrips.scenario.RandomRoundTripGenerator;
import se.vti.roundtrips.scenario.Scenario;
import se.vti.roundtrips.scenario.ScenarioBuilder;
import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConfigGroup;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.preprocessing.loopgeneration.ODCoverage.Connectivity;
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
	static final String nodesFileLabel = "nodesFile";
	static final String linksFileLabel = "linksFile";
	static final String connectivityLabel = "connectivity";

	static final int periodLength_h = 7 * 24;
	static final int timeBinSize_h = 4;
	static final double minODCoverage = 0.001;

	ScenarioContainer dataContainer = null;
	Scenario<NodeWithCoords> samplingScenario = null;

	final SamgodsConfigGroup samgodsConfig;
	final double maxCoverageError;
	final String initialStateFile;
	final int loopCount;
	final SamgodsConstants.Commodity commodity;
	final SamgodsConstants.TransportMode transportMode;
	final ODCoverage.Connectivity connectivity;
	
	final SamgodsSamplingVariant samplingVariant;

	SamgodsLoopSamplingRunner(String[] args, SamgodsSamplingVariant samplingVariant) {
		this.samplingVariant = samplingVariant;

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

		var nodesFileOption = new Option(nodesFileLabel, true, nodesFileLabel);
		nodesFileOption.setRequired(false);
		options.addOption(nodesFileOption);

		var linksFileOption = new Option(linksFileLabel, true, linksFileLabel);
		linksFileOption.setRequired(false);
		options.addOption(linksFileOption);

		var connectivityOption = new Option(connectivityLabel, true, connectivityLabel);
		connectivityOption.setRequired(true);
		options.addOption(connectivityOption);
		
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
			this.maxCoverageError = Double.parseDouble(cmd.getOptionValue(maxCoverageErrorOption));
			this.connectivity = ODCoverage.Connectivity.valueOf(cmd.getOptionValue(connectivityOption));
			
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		log.info("Loading Samgods scenario.");
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

		log.info("Creating sampling scenario.");
		var scenarioBuilder = new ScenarioBuilder<NodeWithCoords>().setTimeBinSize_h(timeBinSize_h)
				.setNumberOfTimeBins(periodLength_h / timeBinSize_h);
		scenarioBuilder.addNodes(this.dataContainer.getAllSamplingNodesView());
		scenarioBuilder.setMoveDistanceFunction(
				(a, b) -> Units.KM_PER_M * Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)));
		scenarioBuilder.setMoveTimeFunction((a, b) -> this.dataContainer.getTransportDuration_h(a, b));
		this.samplingScenario = scenarioBuilder.build();
		
		this.samplingVariant.configureScenario(cmd, this.dataContainer, this.samplingScenario);
	}

	void runSimulation() {

		log.info("Starting simulation ...");

		var runner = new Runner<NodeWithCoords>(this.samplingScenario);

		final double mu = Math.PI / Math.sqrt(2.0) / this.maxCoverageError / dataContainer.getDemandVectorLength_kTon();
		log.info("Computing uncertainty scale");
		log.info("  max coverage error = " + this.maxCoverageError);
		log.info("  total demand = " + this.dataContainer.getTotalDemand_kTon() + " kTon");
		log.info("  demand vector length = " + this.dataContainer.getDemandVectorLength_kTon() + " kTon");
		log.info("  =>  mu = " + mu);

//		double numberOfODPairs = this.dataContainer.getOD2Demand_kTon_View().size();
//		double avgODPairsPerLoop = numberOfODPairs / this.loopCount;
//		log.info("Parametrizing prior");
//		log.info("  number of OD pairs = " + numberOfODPairs);
//		log.info("  avg. OD pairs per loop = " + avgODPairsPerLoop);
//		log.info("  => setting this as mean per loop in binomial prior");		
//		runner.setIndividualUniformPrior(); 
//		runner.setPopulationUniformPrior(); 
		runner.setIndividualBinomialPrior(0.5 * this.samplingScenario.getNumberOfTimeBins());
//		runner.setPopulationBinomialPrior(0.5 * this.samplingScenario.getNumberOfTimeBins()); 

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
//		this.addToSamplingWeights(runner);
		this.samplingVariant.addSamplingWeights(runner, this.dataContainer, this.samplingScenario);

		final MultiRoundTrip<NodeWithCoords> initialRoundTrips;
		if (initialStateFile == null) {
			initialRoundTrips = new MultiRoundTrip<>(loopCount);
			final List<NodeWithCoords> initialNodes = new ArrayList<>(this.samplingScenario.getNodesView());
			var generator = new RandomRoundTripGenerator<>(samplingScenario).setFeasibleNodes(initialNodes)
					.setFeasibilityCheck(r -> Double.isFinite(strictlyPeriodicWeight.logWeight(r)))
					.setNumberOfStayEpisodesInterval(1, this.samplingScenario.getNumberOfTimeBins());
//			parametrizeInitialRoundTripGenerator(generator);
			this.samplingVariant.configureInitialRoundTripGenerator(generator, this.dataContainer,
					this.samplingScenario);
			generator.populateRandomly(initialRoundTrips);
		} else {
			try {
				initialRoundTrips = MultiRoundTripJsonIO.singleton().readFromFile(samplingScenario, initialStateFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		initialRoundTrips.simulateAll(this.samplingScenario.getOrCreateSimulator());
		initialRoundTrips.addSummary(new ODCoverage<NodeWithCoords>(initialRoundTrips.size(), dataContainer,
				minODCoverage, Connectivity.FULL));
		initialRoundTrips.recomputeSummaries();
		runner.setInitialState(initialRoundTrips);

		runner.setMessageInterval(1000);
		runner.configureWeightLogging("samplingWeights.tsv", 1000);
		runner.configureStateLogging("./roundTrips", 10_000);
		runner.setTerminationCriterion(new BlockAverageTerminationCriterion<MultiRoundTrip<NodeWithCoords>>()
				.setCheckInterval(10_000).setConvergenceStatsFileName("convergenceStats.tsv").setMinSamples(100_000));
		runner.run();

		log.info("... DONE");
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

	public static void main(String[] args) {

		new SamgodsLoopSamplingRunner(new String[] { "-configFileName", "config.xml", "-loopCnt", "1000",
				"-maxCoverageError", "0.1", "-commodity", "AGRICULTURE", "-transportMode", "Road", "-connectivity", "DIRECT" },
				new SamgodsSamplingVariant() {
				}).runSimulation();

//		new SamgodsLoopSamplingRunner(new String[] { "-configFileName", "config.xml", "-loopCnt", "1000",
//				"-stationCnt", "200", "-maxCoverageError", "0.1", "-commodity", "AGRICULTURE", "-transportMode", "Road",
//				"-stationPlacement", "maxThroughput" }, new ElectrifiedSamplingVariant()).runSimulation();

//		new SamgodsLoopSamplingRunner(args).createGIS(args, false);
	}
}
