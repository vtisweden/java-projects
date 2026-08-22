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
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripJsonIO;
import se.vti.samgods.common.SamgodsConfigGroup;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.samgods.common.SamgodsRunner;

/**
 * @author GunnarF
 */
public class LoopLoader {

	public LoopLoader() {
	}

	public Set<Loop> load(String fileName, SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode) {

		// load round trips

		final MultiRoundTrip<se.vti.roundtrips.common.Node> roundTrips;
		try {
			roundTrips = MultiRoundTripJsonIO.singleton().readFromFile(name -> new se.vti.roundtrips.common.Node(name),
					fileName);
			System.out.println("Loaded " + roundTrips.size() + " roundtrips.");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// turn round trips into vehicle loops

		Set<Loop> result = new LinkedHashSet<>();
		for (var roundTrip : roundTrips) {
			var matsimNodeIds = new ArrayList<>(
					roundTrip.getNodesView().stream().map(n -> Id.createNodeId(n.getBasicName())).toList());
			var loop = new Loop(commodity, mode, matsimNodeIds);
			boolean isNew = true;
			for (Loop existingLoop : result) {
				if (LoopUtils.instance.equalUpToShift(existingLoop.getMATSimNodeIdsView(),
						loop.getMATSimNodeIdsView())) {
					isNew = false;
					break;
				}
			}
			if (isNew) {
				result.add(loop);
			}
		}
		return result;
	}

	public static void main(String[] args) throws IOException {

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
				.setEnforceReroute(true);

		runner.loadVehiclesOtherThan("WG950", "KOMXL", "SYSXL", "WGEXL", "HGV74", "ROF7", "RAF5", "INW", "ROF2",
				"ROF5");
		runner.loadDomesticNetwork();
		runner.setNetworkFlowsFileName("linkId2commodity2annualAmount_ton.json");
		runner.loadTransportDemand("./input_2024/ChainChoi", "XTD.out");

//		runner.loadLoops("./input_2024/roundtrips.", ".json");

		runner.createOrLoadConsolidationUnits();

		runner.run();
		
	}
}
