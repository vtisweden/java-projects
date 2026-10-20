/**
 * se.vti.samgods
 * 
 * Copyright (C) 2026 by Gunnar Flötteröd (VTI, LiU).
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.roundtrips.runner.Runner;
import se.vti.roundtrips.scenario.NodeWithCoords;
import se.vti.roundtrips.scenario.RandomRoundTripGenerator;
import se.vti.roundtrips.scenario.Scenario;
import se.vti.roundtrips.simulator.DefaultSimulator;
import se.vti.roundtrips.simulator.electrified.BatteryWrapAroundSimulator;
import se.vti.roundtrips.simulator.electrified.Charging;
import se.vti.roundtrips.simulator.electrified.ChargingUtils;
import se.vti.roundtrips.simulator.electrified.ElectrifiedMoveSimulator;
import se.vti.roundtrips.simulator.electrified.ElectrifiedStaySimulator;
import se.vti.roundtrips.simulator.electrified.StrictlyNonNegativeBatteryCharge;
import se.vti.utils.misc.Tuple;

/**
 * @author GunnarF
 */
class ElectrifiedSamplingVariant implements SamgodsSamplingVariant {

	static final String stationCntLabel = "stationCnt";
	static final String stationPlacementLabel = "stationPlacement";

	static final String maxThroughput = "maxThroughput";
	static final String maxDegree = "maxDegree";
	static final String endogenous = "endogenous";

	static final double defaultCapacity_kWh = 728.0;
	static final double defaultChargingRate_kW = 400.0;
	static final double defaultConsumptionRate_kWh_km = 0.01 * 130.0;
	static final double chargeWraparoundTolerance_kWh = 0.001;

	private Integer chargingStationCount;
	private String stationPlacement;

	private Set<NodeWithCoords> allowedChargingSamplingNodes;

	ElectrifiedSamplingVariant() {
	}

	@Override
	public void configureOptions(Options options) {

		var stationCntOption = new Option(stationCntLabel, true, stationCntLabel);
		stationCntOption.setRequired(true);
		options.addOption(stationCntOption);

		var stationPlacementOption = new Option(stationPlacementLabel, true, stationPlacementLabel);
		stationPlacementOption.setRequired(true);
		options.addOption(stationPlacementOption);
	}

	@Override
	public void configureScenario(CommandLine cmd, ScenarioDataContainer dataContainer,
			Scenario<NodeWithCoords> samplingScenario) {

		this.chargingStationCount = Integer.parseInt(cmd.getOptionValue(stationCntLabel));
		this.stationPlacement = cmd.getOptionValue(stationPlacementLabel);

		var simulator = (DefaultSimulator<NodeWithCoords>) samplingScenario.getOrCreateSimulator();
		simulator.setStaySimulator(new ElectrifiedStaySimulator<>(samplingScenario));
		simulator.setMoveSimulator(new ElectrifiedMoveSimulator<>(samplingScenario));
		simulator.setWrapAroundSimulator(new BatteryWrapAroundSimulator(defaultCapacity_kWh, defaultChargingRate_kW,
				defaultConsumptionRate_kWh_km, chargeWraparoundTolerance_kWh));
	}

	@Override
	public List<List<Enum<?>>> createNodeLabels() {
		return List.of(List.of(Charging.YES), List.of(Charging.NO));
	}

	@Override
	public void addSamplingWeights(Runner<NodeWithCoords> runner, ScenarioDataContainer dataContainer,
			Scenario<NodeWithCoords> samplingScenario) {

		runner.addIndividualWeight(new StrictlyNonNegativeBatteryCharge<>());

		if (stationPlacement.equals(maxThroughput) || stationPlacement.equals(maxDegree)) {

			List<Tuple<Id<Node>, Double>> nodesAndSize = new ArrayList<>();
			for (var samgodsNodeId : dataContainer.getSamgodsNodeId2SamplingNodesView().keySet()) {
				if (stationPlacement.equals(maxThroughput)) {
					nodesAndSize.add(new Tuple<>(samgodsNodeId, dataContainer.getTotalSent_kTon(samgodsNodeId)
							+ dataContainer.getTotalReceived_kTon(samgodsNodeId)));
				} else {
					nodesAndSize.add(new Tuple<>(samgodsNodeId,
							(double) dataContainer.getOD2Demand_kTon_View().keySet().stream().filter(
									od -> od.origin.equals(samgodsNodeId) || od.destination.equals(samgodsNodeId))
									.count()));
				}
			}

			Collections.sort(nodesAndSize, (p, q) -> -Double.compare(p.getB(), q.getB()));

			this.allowedChargingSamplingNodes = new LinkedHashSet<>(chargingStationCount);
			for (Id<Node> samgodsNodeId : nodesAndSize.subList(0, chargingStationCount).stream().map(Tuple::getA)
					.collect(Collectors.toSet())) {
				for (var samplingNode : dataContainer.getSamgodsNodeId2SamplingNodesView().get(samgodsNodeId)) {
					if (Charging.YES == ChargingUtils.singleton().extractCharging(samplingNode)) {
						allowedChargingSamplingNodes.add(samplingNode);
					}
				}
			}

			if (allowedChargingSamplingNodes.size() != chargingStationCount) {
				throw new RuntimeException("Extracted " + allowedChargingSamplingNodes.size()
						+ " feasible charging nodes but charging station count is " + chargingStationCount);
			}
			runner.addPopulationWeight(new ChargingNodesConstraint<NodeWithCoords>(allowedChargingSamplingNodes));

		} else if (stationPlacement.equals(endogenous)) {
			runner.addPopulationWeight(new MaxNumberOfChargingPointsConstraint<>(chargingStationCount));
		} else {
			throw new UnsupportedOperationException("Unknown station placement: " + stationPlacement);
		}
	}

	@Override
	public void configureInitialRoundTripGenerator(RandomRoundTripGenerator<NodeWithCoords> generator,
			ScenarioDataContainer dataContainer, Scenario<NodeWithCoords> samplingScenario) {

		List<NodeWithCoords> initialNodes;
		if (stationPlacement.equals(maxThroughput) || stationPlacement.equals(maxDegree)) {
			initialNodes = new ArrayList<>(allowedChargingSamplingNodes);
		} else if (stationPlacement.equals(endogenous)) {
			initialNodes = new ArrayList<>(samplingScenario.getNodesView().stream()
					.filter(n -> Charging.YES == ChargingUtils.singleton().extractCharging(n)).toList());
			Collections.shuffle(initialNodes);
			initialNodes = initialNodes.subList(0, chargingStationCount);
		} else {
			throw new RuntimeException("Unknown station placement: " + stationPlacement);
		}

		generator.setFeasibleNodes(initialNodes).setNumberOfStayEpisodesInterval(1, 1);
	}
}