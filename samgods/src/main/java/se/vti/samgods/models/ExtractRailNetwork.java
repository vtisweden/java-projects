/**
 * se.vti.samgods.models
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
package se.vti.samgods.models;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import se.vti.samgods.common.SamgodsConfigGroup;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsRunner;
import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.samgods.network.SamgodsLinkAttributes;
import se.vti.utils.misc.Units;

/**
 * 
 * @author GunnarF
 *
 */
public class ExtractRailNetwork {

	static final Logger log = LogManager.getLogger(ExtractRailNetwork.class);

	static final String consolidationUnitsFileName = "consolidationUnits.json";

	public static void main(String[] args) throws IOException {

		log.info("STARTED ...");

		List<Commodity> allWithoutAir = new ArrayList<>(Arrays.asList(Commodity.values()));
		allWithoutAir.remove(Commodity.AIR);
		allWithoutAir.toArray();

		Config config = ConfigUtils.loadConfig("config.xml");
		SamgodsConfigGroup samgodsConfig = ConfigUtils.addOrGetModule(config, SamgodsConfigGroup.class);
		
		final double scaleFactor = 1.0; 
//		final SamgodsRunner runner = new SamgodsRunner(samgodsConfig).setServiceInterval_days(7)
//				.setConsideredCommodities(Commodity.AGRICULTURE, Commodity.COAL).setSamplingRate(0.001)
//				.setMaxThreads(1).setScale(Commodity.AGRICULTURE, scaleFactor * 0.0004)
//				.setScale(Commodity.COAL, scaleFactor * 0.0000001).setScale(Commodity.METAL, scaleFactor * 0.0000001
//				/* METAL: using coal parameter because, estimated has wrong sign */)
//				.setScale(Commodity.FOOD, scaleFactor * 0.00006).setScale(Commodity.TEXTILES, scaleFactor * 0.0003)
//				.setScale(Commodity.WOOD, scaleFactor * 0.000003).setScale(Commodity.COKE, scaleFactor * 0.00002)
//				.setScale(Commodity.CHEMICALS, scaleFactor * 0.00002)
//				.setScale(Commodity.OTHERMINERAL, scaleFactor * 0.00003)
//				.setScale(Commodity.BASICMETALS, scaleFactor * 0.00002)
//				.setScale(Commodity.MACHINERY, scaleFactor * 0.00006)
//				.setScale(Commodity.TRANSPORT, scaleFactor * 0.00002)
//				.setScale(Commodity.FURNITURE, scaleFactor * 0.0002)
//				.setScale(Commodity.SECONDARYRAW, scaleFactor * 0.00001)
//				.setScale(Commodity.TIMBER, scaleFactor * 0.00009).setScale(Commodity.AIR, scaleFactor * 0.00005)
//				.setMaxIterations(2).setEnforceReroute(false);
		SamgodsRunner runner = new SamgodsRunner(samgodsConfig).setServiceInterval_days(7)
				.setConsideredCommodities(allWithoutAir.toArray(new Commodity[0])).setSamplingRate(1.0)
				.setMaxThreads(Integer.MAX_VALUE).setScale(Commodity.AGRICULTURE, scaleFactor * 0.0004)
				.setScale(Commodity.COAL, scaleFactor * 0.0000001).setScale(Commodity.METAL, scaleFactor * 0.0000001
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

		runner.loadVehiclesOtherThan("WG950", "KOMXL", "SYSXL", "WGEXL", "HGV74", "ROF7", "RAF5", "INW", "ROF2", "ROF5");
//		runner.checkAvailableVehicles();

		runner.loadEntireNetwork();

		Network network = runner.getNetwork();
		
		Set<Node> railNodes = new LinkedHashSet<>();
		PrintWriter linksWriter = new PrintWriter("railLinks.csv");
		linksWriter.println("id,from,to,maxSpeed[km/h],length[km]");
		for (Link link : network.getLinks().values()) {			
			SamgodsLinkAttributes linkAttrs = (SamgodsLinkAttributes) link.getAttributes().getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME);
			if (SamgodsConstants.TransportMode.Rail.equals(linkAttrs.samgodsMode)) {
				railNodes.add(link.getFromNode());
				railNodes.add(link.getToNode());
			}
			linksWriter.print(link.getId());
			linksWriter.print(",");
			linksWriter.print(link.getFromNode().getId());
			linksWriter.print(",");
			linksWriter.print(link.getToNode().getId());
			linksWriter.print(",");
			linksWriter.print(Units.KM_H_PER_M_S * link.getFreespeed());
			linksWriter.print(",");
			linksWriter.print(Units.KM_PER_M * link.getLength());
			linksWriter.print(",");
			linksWriter.println(link.getNumberOfLanes());
		}
		linksWriter.flush();
		linksWriter.close();

		
		PrintWriter nodesWriter = new PrintWriter("railNodes.csv");
		nodesWriter.println("id,x,y");
		for (Node node : railNodes) {
			nodesWriter.print(node.getId());
			nodesWriter.print(",");
			nodesWriter.print(node.getCoord().getX());
			nodesWriter.print(",");
			nodesWriter.print(node.getCoord().getY());
			nodesWriter.println();
		}
		nodesWriter.flush();
		nodesWriter.close();
		
		
		
		
		
//		runner.setNetworkFlowsFileName("linkId2commodity2annualAmount_ton.json");		
//		runner.loadLinkRegionalWeights("./input_2024/link_regions_domestic.csv");
//		runner.loadTransportDemand("./input_2024/ChainChoi", "XTD.out");
//		runner.createOrLoadConsolidationUnits();
//		runner.run();

		log.info("DONE");
	}
}
