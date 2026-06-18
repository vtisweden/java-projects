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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConfigGroup;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.logistics.ChainChoiReader;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportDemandAndChains;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.NetworkReader;
import se.vti.samgods.network.SamgodsLinkAttributes;
import se.vti.samgods.network.SamgodsNetworkUtils;
import se.vti.utils.misc.Units;

/**
 * 
 * @author GunnarF
 *
 */
class SamgodsScenarioData {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = LogManager.getLogger(SamgodsScenarioData.class);

	private final SamgodsConstants.TransportMode transportMode;

	private final boolean onlyDomestic;

	private final SamgodsConfigGroup samgodsConfig;

	// -------------------- MEMBERS --------------------

	private final Network network;

	private final Map<OD, Double> od2Demand_kTon;

	private final LinkedHashMap<Id<Node>, Double> nodeId2Sent_Mton;

	private final LinkedHashMap<Id<Node>, Double> nodeId2Received_Mton;

	// -------------------- CONSTRUCTION --------------------

	SamgodsScenarioData(SamgodsConstants.TransportMode transportMode, boolean onlyDomestic,
			SamgodsConfigGroup samgodsConfig, SamgodsConstants.Commodity... commodities) {

		this.transportMode = transportMode;
		this.onlyDomestic = onlyDomestic;
		this.samgodsConfig = samgodsConfig;

		log.info("Loading network.");
		try {
			this.network = new NetworkReader().load(this.samgodsConfig.getNetworkNodesFileName(),
					this.samgodsConfig.getNetworkLinksFileName());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		log.info("Reducing/cleaning network.");
		{
			final Set<Id<Link>> excludedLinkIds = new LinkedHashSet<>();
			for (Link link : this.network.getLinks().values()) {
				SamgodsLinkAttributes linkAttrs = SamgodsNetworkUtils.instance.getLinkAttrs(link);
				if ((this.onlyDomestic && !linkAttrs.isDomestic) || !this.transportMode.equals(linkAttrs.samgodsMode)) {
					excludedLinkIds.add(link.getId());
				}
			}
			for (Id<Link> excludedLinkId : excludedLinkIds) {
				this.network.removeLink(excludedLinkId);
			}
			NetworkUtils.runNetworkCleaner(this.network);
		}

		log.info("Checking for slow links.");
		for (Link link : this.network.getLinks().values()) {
			double speed_km_h = Units.KM_H_PER_M_S * link.getFreespeed();
			if (speed_km_h < 30) {
				String msg = "Link " + link.getId() + " has speed " + speed_km_h + " km/h.";
				log.warn(msg);
				if (speed_km_h <= 0.0) {
					throw new RuntimeException(msg);
				}
			}
		}

		log.info("Extracting OD pairs and transfer nodes.");
		this.od2Demand_kTon = new LinkedHashMap<>();
		var transportDemand = new TransportDemandAndChains();
		for (var commodity : commodities) {
			log.info("Processing commodity: " + commodity);
			new ChainChoiReader(commodity, transportDemand, true, true)
					.parse(this.samgodsConfig.getChainChoiFileName(commodity));
			for (var odAndChains : transportDemand.getCommodity2od2transportChains().get(commodity).entrySet()) {
				for (TransportChain chain : odAndChains.getValue()) {
					for (TransportEpisode episode : chain.getEpisodes()) {
						if (this.transportMode.equals(episode.getMode())) {
							Node loadingNode = this.network.getNodes().get(episode.getLoadingNodeId());
							Node unloadingNode = this.network.getNodes().get(episode.getUnloadingNodeId());
							if ((loadingNode != null) && (unloadingNode != null)) {
								this.od2Demand_kTon.put(episode.getLoadingUnloadingOD(), 0.0);
							}
						}
					}
				}
			}
		}

		log.info("Computing OD freight flows.");
		for (var commodity : commodities) {
			for (var odAndShipments : transportDemand.getCommodity2od2annualShipments().get(commodity).entrySet()) {
				OD senderReceiverPair = odAndShipments.getKey();
				double totalDemand_kTon = 1e-3
						* odAndShipments.getValue().stream().mapToDouble(c -> c.getTotalAmount_ton()).sum();
				if (totalDemand_kTon > 0) {
					List<TransportChain> chains = transportDemand.getCommodity2od2transportChains().get(commodity)
							.get(senderReceiverPair);
					if ((chains != null) && (chains.size() > 0)) {
						double demandPerChain_kton = totalDemand_kTon / chains.size();
						for (TransportChain chain : chains) {
							for (TransportEpisode episode : chain.getEpisodes()) {
								if (this.transportMode.equals(episode.getMode())
										&& this.od2Demand_kTon.containsKey(episode.getLoadingUnloadingOD())) {
									this.od2Demand_kTon.compute(episode.getLoadingUnloadingOD(),
											(od, d) -> (d == null) ? demandPerChain_kton : d + demandPerChain_kton);
								}
							}
						}
					}
				}
			}
		}

		log.info("Removing ODs without freight flow.");
		Set<OD> emptyODs = this.od2Demand_kTon.entrySet().stream()
				.filter(e -> (e.getValue() == null) || (e.getValue() == 0)).map(e -> e.getKey())
				.collect(Collectors.toSet());
		emptyODs.stream().forEach(od -> this.od2Demand_kTon.remove(od));

		log.info("Computing node demand totals.");
		this.nodeId2Sent_Mton = new LinkedHashMap<>();
		this.nodeId2Received_Mton = new LinkedHashMap<>();
		for (var odAndDemand_kTon : this.od2Demand_kTon.entrySet()) {
			OD od = odAndDemand_kTon.getKey();
			double amount_Mton = 1e-3 * odAndDemand_kTon.getValue();
			this.nodeId2Sent_Mton.compute(od.origin, (n, s) -> (s == null) ? amount_Mton : s + amount_Mton);
			this.nodeId2Received_Mton.compute(od.destination, (n, r) -> (r == null) ? amount_Mton : r + amount_Mton);
		}
	}

	// -------------------- GETTERS --------------------

	Network getNetwork() {
		return this.network;
	}

	Map<OD, Double> getOD2Demand_kTon() {
		return this.od2Demand_kTon;
	}

	Map<Id<Node>, Double> getNodeId2Sent_Mton() {
		return this.nodeId2Sent_Mton;
	}

	Map<Id<Node>, Double> getNodeId2Received_Mton() {
		return this.nodeId2Received_Mton;
	}

	Set<Id<Node>> computeTerminalNodeIds() {
		Set<Id<Node>> allTerminalNodeIds = new LinkedHashSet<>();
		allTerminalNodeIds.addAll(this.nodeId2Sent_Mton.keySet());
		allTerminalNodeIds.addAll(this.nodeId2Received_Mton.keySet());
		return allTerminalNodeIds;
	}

	double computeTotalDemand_kTon() {
		return this.od2Demand_kTon.values().stream().mapToDouble(d -> d).sum();
	}

	double computeDemandVectorLength_kTon() {
		return Math.sqrt(this.od2Demand_kTon.values().stream().mapToDouble(d -> d * d).sum());
	}
}
