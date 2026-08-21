/**
 * se.vti.samgods.transportation.consolidation
 * 
 * Copyright (C) 2024-2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.transportation.consolidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import se.vti.roundtrips.common.ScenarioBuilder;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripJsonIO;
import se.vti.samgods.common.SamgodsConfigGroup;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.samgods.common.SamgodsConstants.TransportMode;
import se.vti.samgods.common.SamgodsRunner;
import se.vti.samgods.logistics.TransportChain;

/**
 * @author GunnarF
 */
class RoundTripLoader {

	private final VehicleLoopManager loopManager;

	private final Set<Id<Node>> allNodeIds;

	private final double timeBinSize_h;
	private final int numberOfTimeBins;

	RoundTripLoader(VehicleLoopManager loopManager, Set<Id<Node>> allNodeIds, double timeBinSize_h, int timeBinCnt) {
		this.loopManager = loopManager;
		this.allNodeIds = allNodeIds;
		this.timeBinSize_h = timeBinSize_h;
		this.numberOfTimeBins = timeBinCnt;
	}

	void load(String fileName, SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode) {

		// build sampling scenario (necessary for loading)

		var scenarioBuilder = new ScenarioBuilder<se.vti.roundtrips.common.Node>().setTimeBinSize_h(this.timeBinSize_h)
				.setNumberOfTimeBins(this.numberOfTimeBins);
		scenarioBuilder.addNodes(this.allNodeIds.stream().map(id -> new se.vti.roundtrips.common.Node(id.toString()))
				.collect(Collectors.toSet()));
		scenarioBuilder.setMoveDistanceFunction((a, b) -> 0.0);
		scenarioBuilder.setMoveTimeFunction((a, b) -> 0.0);
		var samplingScenario = scenarioBuilder.build();

		// load round trips

		final MultiRoundTrip<se.vti.roundtrips.common.Node> roundTrips;
		try {
			roundTrips = MultiRoundTripJsonIO.singleton().readFromFile(samplingScenario, fileName);
			System.out.println("Loaded " + roundTrips.size() + " roundtrips.");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// turn round trips into vehicle loops and add to manager

		for (var roundTrip : roundTrips) {
			var matsimNodeIds = new ArrayList<>(
					roundTrip.getNodesView().stream().map(n -> Id.createNodeId(n.getBasicName())).toList());
			var loop = new VehicleLoop(commodity, mode, matsimNodeIds);
			this.loopManager.addLoop(loop);
		}
	}

	public static void main(String[] args) throws IOException {

		// create plain samgods

		List<Commodity> allWithoutAir = new ArrayList<>(Arrays.asList(Commodity.values()));
		allWithoutAir.remove(Commodity.AIR);
		allWithoutAir.toArray();

		Config config = ConfigUtils.loadConfig("config.xml");
		SamgodsConfigGroup samgodsConfig = ConfigUtils.addOrGetModule(config, SamgodsConfigGroup.class);

		final double scaleFactor = 1.0;
		SamgodsRunner runner = new SamgodsRunner(samgodsConfig).setServiceInterval_days(7)
				.setConsideredCommodities(SamgodsConstants.Commodity.AGRICULTURE).setSamplingRate(1.0).setMaxThreads(16)
				.setScale(Commodity.AGRICULTURE, scaleFactor * 0.0004).setScale(Commodity.COAL, scaleFactor * 0.0000001)
				.setScale(Commodity.METAL, scaleFactor * 0.0000001
				/* METAL: using coal parameter because, estimated has wrong sign */)
				.setScale(Commodity.FOOD, scaleFactor * 0.00006).setScale(Commodity.TEXTILES, scaleFactor * 0.0003)
				.setScale(Commodity.WOOD, scaleFactor * 0.000003).setScale(Commodity.COKE, scaleFactor * 0.00002)
				.setScale(Commodity.CHEMICALS, scaleFactor * 0.00002)
				.setScale(Commodity.OTHERMINERAL, scaleFactor * 0.00003)
				.setScale(Commodity.BASICMETALS, scaleFactor * 0.00002)
				.setScale(Commodity.MACHINERY, scaleFactor * 0.00006)
				.setScale(Commodity.TRANSPORT, scaleFactor * 0.00002)
				.setScale(Commodity.FURNITURE, scaleFactor * 0.0002)
				.setScale(Commodity.SECONDARYRAW, scaleFactor * 0.00001)
				.setScale(Commodity.TIMBER, scaleFactor * 0.00009).setScale(Commodity.AIR, scaleFactor * 0.00005)
				.setEnforceReroute(false);

		runner.loadVehiclesOtherThan("WG950", "KOMXL", "SYSXL", "WGEXL", "HGV74", "ROF7", "RAF5", "INW", "ROF2",
				"ROF5");
		runner.loadDomesticNetwork();

		runner.setNetworkFlowsFileName("linkId2commodity2annualAmount_ton.json");
		runner.loadTransportDemand("./input_2024/ChainChoi", "XTD.out");
		runner.createOrLoadConsolidationUnits();

		// load roundtrips

		Set<Id<Node>> terminals = new LinkedHashSet<>();
		var od2transportChains = runner.getTransportDemand().getCommodity2od2transportChains()
				.get(SamgodsConstants.Commodity.AGRICULTURE);
		for (List<TransportChain> chainsPerOD : od2transportChains.values()) {
			for (var transportChain : chainsPerOD) {
				for (var transportEpisode : transportChain.getEpisodes()) {
					Id<Node> fromNodeId = transportEpisode.getLoadingNodeId();
					Id<Node> toNodeId = transportEpisode.getUnloadingNodeId();
					terminals.add(fromNodeId);
					terminals.add(toNodeId);
				}
			}
		}
		System.out.println("Using " + terminals.size() + " active terminals.");

		var loopManager = new VehicleLoopManager(runner.getConsolidationUnits());
		var roundTripLoader = new RoundTripLoader(loopManager, terminals, 4.0, 42);
		roundTripLoader.load("./input_2024/roundtrips.AGRICULTURE.json", Commodity.AGRICULTURE, TransportMode.Road);

		loopManager.printStats();
		
	}

}
