/**
 * se.vti.samgods
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import se.vti.samgods.calibration.ascs.ASCDataProvider;
import se.vti.samgods.calibration.ascs.TransportWorkAscCalibrator;
import se.vti.samgods.calibration.ascs.TransportWorkMonitor;
import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.samgods.common.SamgodsConstants.TransportMode;
import se.vti.samgods.external.gis.NetworkFlows;
import se.vti.samgods.external.ntmcalc.HalfLoopAssignment2NTMCalcWriter;
import se.vti.samgods.logistics.AnnualShipment;
import se.vti.samgods.logistics.ChainChoiReader;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportDemandAndChains;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.choice.ChainAndShipmentChoiceStats;
import se.vti.samgods.logistics.choice.ChainAndShipmentSize;
import se.vti.samgods.logistics.choice.ChainAndShipmentSizeUtilityFunction;
import se.vti.samgods.logistics.choice.ChoiceJob;
import se.vti.samgods.logistics.choice.ChoiceJobProcessor;
import se.vti.samgods.logistics.choice.LogisticChoiceDataProvider;
import se.vti.samgods.logistics.choice.MonetaryChainAndShipmentSizeUtilityFunction;
import se.vti.samgods.logistics.costs.NonTransportCostModel;
import se.vti.samgods.logistics.costs.NonTransportCostModel_v1_22;
import se.vti.samgods.network.NetworkReader;
import se.vti.samgods.network.Router;
import se.vti.samgods.transportation.consolidation.ConsolidationJob;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor;
import se.vti.samgods.transportation.fleet.VehiclesReader;
import se.vti.utils.MiscUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsRunner {

	// -------------------- CONSTANTS --------------------

	private final static Logger log = LogManager.getLogger(SamgodsRunner.class);

	private final static long defaultSeed = 4711;

	private final static int defaultMaxThreads = Integer.MAX_VALUE;

	private final static int defautServiceInterval_days = 7;

//	private final static int defaultMaxIterations = 5;

	private final static boolean defaultEnforceReroute = false;

	private final static double defaultSamplingRate = 1.0;

	private final static Commodity[] defaultConsideredCommodities = Commodity.values();

	private final static double defaultLogitScale = 1.0;

	// -------------------- MEMBERS --------------------

	private final SamgodsConfigGroup config;

	private Random rnd = new Random();

	private List<Commodity> consideredCommodities;

	private int maxThreads;

	// TODO concurrency?
	private final Map<SamgodsConstants.Commodity, Integer> commodity2serviceInterval_days = new LinkedHashMap<>();

//	private int maxIterations;

	private boolean enforceReroute;

	private double samplingRate;

	//

	private final Map<Commodity, Double> commodity2scale = new LinkedHashMap<>(
			Arrays.stream(Commodity.values()).collect(Collectors.toMap(c -> c, c -> defaultLogitScale)));

	private Vehicles vehicles = null;

	private Network network = null;

	private TransportDemandAndChains transportDemand = null;

	private TransportWorkAscCalibrator fleetCalibrator = null;
	private ASCDataProvider ascDataProvider = null;

	private String networkFlowsFileName = null;

	public SamgodsRunner setNetworkFlowsFileName(String networkFlowsFileName) {
		this.networkFlowsFileName = networkFlowsFileName;
		return this;
	}

	// -------------------- CONSTRUCTION --------------------

	public SamgodsRunner(SamgodsConfigGroup config) {
		this.config = config;
		this.setRandomSeed(defaultSeed);
		this.setConsideredCommodities(defaultConsideredCommodities);
		this.setMaxThreads(defaultMaxThreads);
		this.setServiceInterval_days(defautServiceInterval_days);
//		this.setMaxIterations(defaultMaxIterations);
		this.setEnforceReroute(defaultEnforceReroute);
		this.setSamplingRate(defaultSamplingRate);
	}

	// -------------------- SETTERS --------------------

	public SamgodsRunner setRandomSeed(long seed) {
		this.rnd = new Random(seed);
		return this;
	}

	public SamgodsRunner setConsideredCommodities(Commodity... consideredCommodities) {
		this.consideredCommodities = Arrays.asList(consideredCommodities);
		return this;
	}

	public SamgodsRunner setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
		return this;
	}

	public SamgodsRunner setServiceInterval_days(int serviceInterval_days) {
		Arrays.stream(Commodity.values())
				.forEach(c -> this.commodity2serviceInterval_days.put(c, serviceInterval_days));
		return this;
	}

//	public SamgodsRunner setMaxIterations(int maxIterations) {
//		this.maxIterations = maxIterations;
//		return this;
//	}

	public SamgodsRunner setEnforceReroute(boolean enforceReroute) {
		this.enforceReroute = enforceReroute;
		return this;
	}

	public SamgodsRunner setSamplingRate(double samplingRate) {
		this.samplingRate = samplingRate;
		return this;
	}

	//

	public SamgodsRunner setScale(Commodity commodity, double scale) {
		this.commodity2scale.put(commodity, scale);
		return this;
	}

	// -------------------- GETTERS --------------------

	public Network getNetwork() {
		return this.network;
	}

	// -------------------- LOAD VEHICLE FLEET --------------------

	private SamgodsRunner loadVehicles(String vehicleParametersFileName, String transferParametersFileName,
			TransportMode samgodsMode, String... excludedIds) throws IOException {
		if (this.vehicles == null) {
			this.vehicles = VehicleUtils.createVehiclesContainer();
		}
		final VehiclesReader fleetReader = new VehiclesReader(this.vehicles);
		for (String excludedId : excludedIds) {
			fleetReader.addExcludedId(excludedId);
		}
		fleetReader.load_v12(vehicleParametersFileName, transferParametersFileName, samgodsMode);
		return this;
	}

	public SamgodsRunner loadVehiclesOtherThan(String... excludedIds) throws IOException {
		if (this.config.getRailVehicleParametersFileName() != null
				&& this.config.getRailTransferParametersFileName() != null) {
			this.loadVehicles("./input_2024/vehicleparameters_rail.csv", "./input_2024/transferparameters_rail.csv",
					SamgodsConstants.TransportMode.Rail, excludedIds);
		}
		if (this.config.getRoadVehicleParametersFileName() != null
				&& this.config.getRoadTransferParametersFileName() != null) {
			this.loadVehicles("./input_2024/vehicleparameters_road.csv", "./input_2024/transferparameters_road.csv",
					SamgodsConstants.TransportMode.Road, excludedIds);
		}
		if (this.config.getSeaVehicleParametersFileName() != null
				&& this.config.getSeaTransferParametersFileName() != null) {
			this.loadVehicles("./input_2024/vehicleparameters_sea.csv", "./input_2024/transferparameters_sea.csv",
					SamgodsConstants.TransportMode.Sea, excludedIds);

		}
		return this;
	}

	// -------------------- LOAD NETWORK --------------------

	public SamgodsRunner loadNetwork() throws IOException {
		this.network = new NetworkReader().load(this.config.getNetworkNodesFileName(),
				this.config.getNetworkLinksFileName());
		return this;
	}

	// -------------------- LOAD TRANSPORT DEMAND --------------------

	public SamgodsRunner loadTransportDemand(String demandFilePrefix, String demandFileSuffix) {
		this.transportDemand = new TransportDemandAndChains();
		for (Commodity commodity : this.consideredCommodities) {
			new ChainChoiReader(commodity, this.transportDemand, this.vehicles)
					.setSamplingRate(this.samplingRate, new Random(4711))
					.parse(demandFilePrefix + commodity.twoDigitCode() + demandFileSuffix);
		}
		return this;
	}

	// -------------------- PREPARE CONSOLIDATION UNITS --------------------

	public void createOrLoadConsolidationUnits() throws IOException {

		NetworkAndFleetDataProvider.initialize(this.network, this.vehicles);

		if (this.enforceReroute || !(new File(this.config.getConsolidationUnitsFileName()).exists())) {

			/*
			 * Several episodes may have consolidation units with the same routes. To reduce
			 * routing effort, we collect here, for each possible routing configuration, one
			 * representative consolidation unit to be routed. We store a back-links to the
			 * consolidation units attached to the episodes in order to later replace them
			 * by their equivalent routed instances.
			 */
			final Map<ConsolidationUnit, ConsolidationUnit> consolidationUnitPattern2representativeUnit = new LinkedHashMap<>();
			for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
				for (List<TransportChain> chains : this.transportDemand.getCommodity2od2transportChains().get(commodity)
						.values()) {
					for (TransportChain chain : chains) {
						for (TransportEpisode episode : chain.getEpisodes()) {
							episode.setConsolidationUnits(ConsolidationUnit.createUnrouted(episode));
							for (ConsolidationUnit consolidationUnit : episode.getConsolidationUnits()) {
								consolidationUnitPattern2representativeUnit.put(consolidationUnit.cloneWithoutRoutes(),
										consolidationUnit);
							}
						}
					}
				}
			}

			/*
			 * Route (if possible) the representative consolidation units.
			 * 
			 * Routing changes the behavior of hashcode(..) / equals(..) in
			 * ConsolidationUnit, but this should matter in the *values* of a HashMap.
			 */
			new Router(NetworkAndFleetDataProvider.getProviderInstance()).setLogProgress(true)
					.setMaxThreads(this.maxThreads).route(consolidationUnitPattern2representativeUnit.values());

			/*
			 * Stream routed consolidation units to json file.
			 */
			long routedCnt = 0;
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
			FileOutputStream fos = new FileOutputStream(new File(this.config.getConsolidationUnitsFileName()));
			JsonGenerator gen = mapper.getFactory().createGenerator(fos);
			for (ConsolidationUnit consolidationUnit : consolidationUnitPattern2representativeUnit.values()) {
				if (consolidationUnit.vehicleType2route.size() > 0) {
					mapper.writeValue(gen, consolidationUnit);
					routedCnt++;
				}
			}
			gen.flush();
			gen.close();
			fos.flush();
			fos.close();
			log.info("Wrote " + routedCnt + " (out of in total " + consolidationUnitPattern2representativeUnit.size()
					+ ") routed consolidation units to file " + this.config.getConsolidationUnitsFileName());

			/*
			 * Attach representative consolidation units to the episodes. This means that
			 * episodes from now on share consolidation units. TransortChain.isRouted only
			 * produces meaningful behavior after this operation is complete.
			 * 
			 * There must not be redundancies in the consolidation unit file.
			 */
			for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
				for (List<TransportChain> chains : this.transportDemand.getCommodity2od2transportChains().get(commodity)
						.values()) {
					for (TransportChain chain : chains) {
						for (TransportEpisode episode : chain.getEpisodes()) {
							List<ConsolidationUnit> templates = new ArrayList<>(episode.getConsolidationUnits().size());
							for (ConsolidationUnit tmpUnit : episode.getConsolidationUnits()) {
								ConsolidationUnit routingEquivalent = tmpUnit.cloneWithoutRoutes();
								ConsolidationUnit template = consolidationUnitPattern2representativeUnit
										.get(routingEquivalent);
								assert (template != null);
								templates.add(template);
							}
							episode.setConsolidationUnits(templates);
						}
					}
				}
			}

		} else {

			/*
			 * Load (routed!) consolidation units.
			 */
			log.info("Loading consolidation units from file " + this.config.getConsolidationUnitsFileName());
			ObjectMapper mapper = new ObjectMapper();
			SimpleModule module = (new SimpleModule()).addDeserializer(ConsolidationUnit.class,
					new ConsolidationUnit.Deserializer(this.vehicles));
			mapper.registerModule(module);
			ObjectReader reader = mapper.readerFor(ConsolidationUnit.class);
			JsonParser parser = mapper.getFactory().createParser(new File(this.config.getConsolidationUnitsFileName()));
			Map<ConsolidationUnit, ConsolidationUnit> consolidationUnitPattern2representativeUnit = new LinkedHashMap<>();
			while (parser.nextToken() != null) {
				ConsolidationUnit unit = reader.readValue(parser);
//				unit.compress(); // TODO not necessary when the file is already compressed
				consolidationUnitPattern2representativeUnit.put(unit.cloneWithoutRoutes(), unit);
			}
			parser.close();

			/*
			 * Attach representative consolidation units to episodes.
			 */
			log.info("Attaching consolidation units to episodes.");
			for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
				log.info("... processing commodity: " + commodity);
				for (List<TransportChain> chains : this.transportDemand.getCommodity2od2transportChains().get(commodity)
						.values()) {
					for (TransportChain chain : chains) {
						for (TransportEpisode episode : chain.getEpisodes()) {
							List<ConsolidationUnit> tmpUnits = ConsolidationUnit.createUnrouted(episode);
							List<ConsolidationUnit> representativeUnits = new ArrayList<>(tmpUnits.size());
							for (ConsolidationUnit tmpUnit : tmpUnits) {
								ConsolidationUnit match = consolidationUnitPattern2representativeUnit
										.get(tmpUnit.cloneWithoutRoutes());
								if (match != null) {
									representativeUnits.add(match);
								} else {
									representativeUnits = null;
									break;
								}
							}
							episode.setConsolidationUnits(representativeUnits);
						}
					}
				}
			}
		}

		for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
			long removedChainCnt = 0;
			long totalChainCnt = 0;
			for (Map.Entry<OD, List<TransportChain>> entry : this.transportDemand.getCommodity2od2transportChains()
					.get(commodity).entrySet()) {
				final int chainCnt = entry.getValue().size();
				totalChainCnt += chainCnt;
				entry.setValue(entry.getValue().stream().filter(c -> c.isRouted()).toList());
				removedChainCnt += chainCnt - entry.getValue().size();
			}
			log.warn(commodity + ": Removed " + removedChainCnt + " out of " + totalChainCnt
					+ " chains with incomplete routes.");
		}
	}

	// -------------------- RUN ITERATIONS --------------------

	public void run() {

		MiscUtils.ensureEmptyFolder("./results");

		final TransportWorkMonitor transportWorkMonitor = new TransportWorkMonitor(this.vehicles);

		if (this.config.getAscSourceFileName() != null) {
			try {
				this.ascDataProvider = ASCDataProvider.createFromFile(this.config.getAscSourceFileName(),
						this.vehicles);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			this.ascDataProvider = new ASCDataProvider();
		}
//		NetworkAndFleetDataProvider.updateASCs(this.ascs);

		if (this.config.getAscCalibrationStepSize() != null) {
			this.fleetCalibrator = new TransportWorkAscCalibrator(this.vehicles,
					this.config.getAscCalibrationStepSize());
		} else {
			this.fleetCalibrator = null;
		}

		final Set<ConsolidationUnit> allConsolidationUnits = new LinkedHashSet<>();
		for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
			log.info(commodity + ": Collecting consolidation units.");
			for (List<TransportChain> transportChains : this.transportDemand.getCommodity2od2transportChains()
					.get(commodity).values()) {
				transportChains.stream().flatMap(c -> c.getEpisodes().stream())
						.forEach(e -> allConsolidationUnits.addAll(e.getConsolidationUnits()));
			}
		}

		for (int iteration = 0; iteration < this.config.getMaxIterations(); iteration++) {
			log.info("STARTING ITERATION " + iteration);

			/*
			 * Simulate choices.
			 */

			final LogisticChoiceDataProvider logisticChoiceDataProvider = new LogisticChoiceDataProvider(
					NetworkAndFleetDataProvider.getProviderInstance());
			logisticChoiceDataProvider.update(null);

			BlockingQueue<ChainAndShipmentSize> allChoices = new LinkedBlockingQueue<>();
			{
				final int threadCnt = Math.min(this.maxThreads, Runtime.getRuntime().availableProcessors());
				BlockingQueue<ChoiceJob> jobQueue = new LinkedBlockingQueue<>(10 * threadCnt);
				List<Thread> choiceThreads = new ArrayList<>();

				log.info("Starting " + threadCnt + " choice simulation threads.");
				try {
					for (int i = 0; i < threadCnt; i++) {
//						final NetworkAndFleetData networkAndFleetData = NetworkAndFleetDataProvider
//								.getProviderInstance().createDataInstance();
						final NonTransportCostModel nonTransportCostModel = new NonTransportCostModel_v1_22();
						final ChainAndShipmentSizeUtilityFunction utilityFunction = new MonetaryChainAndShipmentSizeUtilityFunction(
								new LinkedHashMap<>(this.commodity2scale),
//								this.ascDataProvider.getConcurrentMode2ASC(),
								this.ascDataProvider.getConcurrentRailCommodity2ASC());
						final ChoiceJobProcessor choiceSimulator = new ChoiceJobProcessor(
								logisticChoiceDataProvider.createLogisticChoiceData(), nonTransportCostModel,
								utilityFunction, jobQueue, allChoices);
						final Thread choiceThread = new Thread(choiceSimulator);
						choiceThreads.add(choiceThread);
						choiceThread.start();
					}

					log.info("Starting to populate choice job queue, continuing as threads progress.");
					for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
						for (Map.Entry<OD, List<AnnualShipment>> e : this.transportDemand
								.getCommodity2od2annualShipments().get(commodity).entrySet()) {
							final OD od = e.getKey();
							final List<AnnualShipment> annualShipments = e.getValue();
							final List<TransportChain> transportChains = this.transportDemand
									.getCommodity2od2transportChains().get(commodity).get(od);
							if (transportChains.size() > 0) {
								jobQueue.put(new ChoiceJob(commodity, od, transportChains, annualShipments));
							} else {
								log.warn("No transport chains available for commodity=" + commodity + ",od=" + od);
							}
						}
					}

					log.info("Waiting for choice jobs to complete.");
					for (int i = 0; i < choiceThreads.size(); i++) {
						jobQueue.put(ChoiceJob.TERMINATE);
					}
					for (Thread choiceThread : choiceThreads) {
						choiceThread.join();
					}

				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			log.info("Collecting data, calculating choice stats.");
			Map<ConsolidationUnit, List<ChainAndShipmentSize>> consolidationUnit2choices = new LinkedHashMap<>();
			ChainAndShipmentChoiceStats stats = new ChainAndShipmentChoiceStats(
					NetworkAndFleetDataProvider.getProviderInstance().createDataInstance());
			for (ChainAndShipmentSize choice : allChoices) {
				stats.add(choice);
				for (TransportEpisode episode : choice.transportChain.getEpisodes()) {
					for (ConsolidationUnit consolidationUnit : episode.getConsolidationUnits()) {
						consolidationUnit2choices.computeIfAbsent(consolidationUnit, s -> new LinkedList<>())
								.add(choice);
					}
				}
			}
			log.info(allChoices.stream().mapToLong(c -> c.transportChain.getEpisodes().size()).sum() + " episodes.");
			log.info(consolidationUnit2choices.size() + " episode signatures.");
			log.info(consolidationUnit2choices.values().stream().flatMap(l -> l.stream())
					.mapToDouble(c -> c.annualShipment.getTotalAmount_ton() / c.sizeClass.getRepresentativeValue_ton())
					.sum() + " shipments.");
			log.info("\n" + stats.createChoiceStatsTable());

//			ShipmentPopulationCreator populationCreator = new ShipmentPopulationCreator(this.network);
//			for (Map<OD, List<TransportChain>> od2chains : this.transportDemand.getCommodity2od2transportChains().values()) {
//				for (List<TransportChain> chains : od2chains.values()) {
//					for (TransportChain chain : chains) {
//						populationCreator.add(chain);
//					}
//				}
//			}
//			populationCreator.writeToFile("./input_2024/shipmentPlans.xml");

			/*
			 * Consolidate.
			 */
			final ConcurrentHashMap<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2assignment = new ConcurrentHashMap<>();
			{
				final int threadCnt = Math.min(this.maxThreads, Runtime.getRuntime().availableProcessors());
				BlockingQueue<ConsolidationJob> jobQueue = new LinkedBlockingQueue<>(10 * threadCnt);
				List<Thread> consolidationThreads = new ArrayList<>();

				try {

					log.info("Starting " + threadCnt + " consolidation threads.");
					for (int i = 0; i < threadCnt; i++) {
						NetworkAndFleetData networkAndFleetData = NetworkAndFleetDataProvider.getProviderInstance()
								.createDataInstance();
						HalfLoopConsolidationJobProcessor consolidationProcessor = new HalfLoopConsolidationJobProcessor(
								jobQueue, consolidationUnit2assignment, networkAndFleetData,
								new LinkedHashMap<>(this.commodity2scale), this.ascDataProvider);
						Thread choiceThread = new Thread(consolidationProcessor);
						consolidationThreads.add(choiceThread);
						choiceThread.start();
					}

					log.info("Starting to populate consolidation job queue, continuing as threads progress.");
					final NetworkAndFleetData networkAndFleetData = NetworkAndFleetDataProvider.getProviderInstance()
							.createDataInstance();
					for (Map.Entry<ConsolidationUnit, List<ChainAndShipmentSize>> entry : consolidationUnit2choices
							.entrySet()) {
						ConsolidationUnit consolidationUnit = entry.getKey();
						List<ChainAndShipmentSize> choices = entry.getValue();
						if ((choices != null) && (choices.size() > 0)) {
							final double totalDemand_ton = choices.stream()
									.mapToDouble(c -> c.annualShipment.getTotalAmount_ton()).sum();
							if (totalDemand_ton >= 1e-3
									&& consolidationUnit.computeLengthStats_km(networkAndFleetData).getMean() >= 1e-3) {
								ConsolidationJob job = new ConsolidationJob(consolidationUnit, choices,
										this.commodity2serviceInterval_days.get(consolidationUnit.commodity));
								jobQueue.put(job);
							}
						} else {
							log.warn("No transport chains available for consolidation: " + consolidationUnit);
						}
					}

					log.info("Waiting for choice jobs to complete.");
					for (int i = 0; i < consolidationThreads.size(); i++) {
						jobQueue.put(ConsolidationJob.TERMINATE);
					}
					for (Thread consolidationThread : consolidationThreads) {
						consolidationThread.join();
					}

				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			/*
			 * POSTPROCESSING, SUMMARY STATISTICS.
			 */

			logisticChoiceDataProvider.update(consolidationUnit2assignment);

			log.info("Collecting fleet statistics");
			transportWorkMonitor.update(consolidationUnit2assignment);
			if (this.fleetCalibrator != null) {
				this.fleetCalibrator.update(transportWorkMonitor.getVehicleType2lastRealizedDomesticGTonKm(),
						transportWorkMonitor.getMode2lastRealizedDomesticGTonKm(),
						transportWorkMonitor.getCommodity2lastRealizedRailDomesticGTonKm(), iteration);
				this.ascDataProvider = this.fleetCalibrator.createASCDataProvider();
			}

//			NetworkAndFleetDataProvider.updateASCs(this.ascs);

			if ((iteration == this.config.getMaxIterations() - 1)) {

				if (this.networkFlowsFileName != null) {
					new NetworkFlows().add(consolidationUnit2assignment).writeToFile(this.networkFlowsFileName);
				}

				new HalfLoopAssignment2NTMCalcWriter(
						NetworkAndFleetDataProvider.getProviderInstance().createDataInstance())
						.writeToFile("Flows2NTM_", consolidationUnit2assignment);
				;
			}
		}
	}
}
