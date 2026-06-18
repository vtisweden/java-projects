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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.common.RandomRoundTripGenerator;
import se.vti.roundtrips.common.Runner;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.roundtrips.simulator.DefaultSimulator;
import se.vti.roundtrips.simulator.electrified.BatteryWrapAroundSimulator;
import se.vti.roundtrips.simulator.electrified.Charging;
import se.vti.roundtrips.simulator.electrified.ChargingUtils;
import se.vti.roundtrips.simulator.electrified.ElectrifiedMoveSimulator;
import se.vti.roundtrips.simulator.electrified.ElectrifiedStaySimulator;
import se.vti.roundtrips.simulator.electrified.StrictlyNonNegativeBatteryCharge;

/**
 * 
 * @author GunnarF
 *
 */
class ElectrifiedSamgodsLoopSamplingRunner extends SamgodsLoopSamplingRunner {

	static final Logger log = LogManager.getLogger(ElectrifiedSamgodsLoopSamplingRunner.class);

	static final String stationCntLabel = "stationCnt";

	static final double defaultCapacity_kWh = 728.0;
	static final double defaultChargingRate_kW = 400.0;
	static final double defaultConsumptionRate_kWh_km = 0.01 * 130.0;
	static final double chargeWraparoundTolerance_kWh = 0.001;

	Integer chargingStationCount;

	ElectrifiedSamgodsLoopSamplingRunner(String[] args) {
		super(args);
	}

	@Override
	void configureSamplingScenario(String[] args) {

		var options = new Options();

		var stationCntOption = new Option(stationCntLabel, true, stationCntLabel);
		stationCntOption.setRequired(false);
		options.addOption(stationCntOption);
		try {
			CommandLine cmd = new DefaultParser().parse(options, args);
			this.chargingStationCount = (cmd.hasOption(stationCntOption)
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

	@Override
	void addToNodeLabels(List<List<Enum<?>>> allNodeLabels) {
		allNodeLabels.add(List.of(Charging.YES));
		allNodeLabels.add(List.of(Charging.NO));
	}

	@Override
	void addToSamplingWeights(Runner<NodeWithCoords> runner) {
		runner.addIndividualWeight(new StrictlyNonNegativeBatteryCharge<>());
		runner.addPopulationWeight(new MaxNumberOfChargingPointsConstraint<>(this.chargingStationCount));
	}

	@Override
	void parametrizeInitialRoundTripGenerator(RandomRoundTripGenerator<NodeWithCoords> generator) {
		List<NodeWithCoords> initialNodes = new ArrayList<>(samplingScenario.getNodesView().stream()
				.filter(n -> Charging.YES == ChargingUtils.singleton().extractCharging(n)).toList());
		Collections.shuffle(initialNodes);
		initialNodes = initialNodes.subList(0, this.chargingStationCount);
		generator.setFeasibleNodes(initialNodes).setNumberOfStayEpisodesInterval(1, 1);
	}

	public static void main(String[] args) {

//		createGIS(args, true);

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
