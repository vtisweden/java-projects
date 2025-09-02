/**
 * se.vti.samgods.network
 * 
 * Copyright (C) 2024, 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.misc;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import se.vti.samgods.network.NetworkReader;
import se.vti.samgods.network.SamgodsNodeAttributes;
import se.vti.utils.misc.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class LinkRegionsReader {

	private static final Logger log = LogManager.getLogger(LinkRegionsReader.class);

	private static final String From = "From";
	private static final String To = "To";
	private static final String REGION = "REGION";
	private static final String WEIGHT = "WEIGHT";

	private final Network network;

	public LinkRegionsReader(Network network) {
		this.network = network;
	}

	public Map<Id<Link>, Double> read(String fileName) throws IOException {

		final Map<Id<Link>, Double> linkId2domesticWeightSumUpToRegion2584 = new LinkedHashMap<>();
		final Map<Id<Link>, Double> linkId2domesticWeightSumFromRegion2585 = new LinkedHashMap<>();
		final Map<Id<Link>, Double> linkId2domesticWeightSumWithoutRegion = new LinkedHashMap<>();

		List<Tuple<Id<Node>, Id<Node>>> missingLinkNodeIds = new ArrayList<>();

		for (CSVRecord record : CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(fileName))) {

			final Id<Node> fromNodeId = Id.createNodeId(record.get(From));
			final Id<Node> toNodeId = Id.createNodeId(record.get(To));
			final Node fromNode = network.getNodes().get(fromNodeId);
			final Node toNode = network.getNodes().get(toNodeId);
			final Link link;
			if (fromNode != null && toNode != null) {
				link = NetworkUtils.getConnectingLink(fromNode, toNode);
			} else {
				link = null;
			}

			if (link == null) {
				missingLinkNodeIds.add(new Tuple<>(fromNodeId, toNodeId));
			} else {
				final Double weight = Double.parseDouble(record.get(WEIGHT)) / 100.0;
				final String regionStr = record.get(REGION);
				if ("NA".equals(regionStr)) {
					linkId2domesticWeightSumWithoutRegion.put(link.getId(), weight);
				} else {
					final long regionCode = Long.parseLong(regionStr);
					if (regionCode < 2585) {
						linkId2domesticWeightSumUpToRegion2584.compute(link.getId(),
								(l, s) -> s == null ? weight : s + weight);
					} else {
						linkId2domesticWeightSumFromRegion2585.put(link.getId(), weight);
					}
				}
			}
		}

		log.info(linkId2domesticWeightSumUpToRegion2584.values().stream().filter(s -> s > 0.0).count()
				+ " links with positive weights.");
		log.warn(linkId2domesticWeightSumUpToRegion2584.values().stream().filter(s -> s > 1.0).count()
				+ " links with weights larger than one.");
		log.warn(missingLinkNodeIds.size() + " node pairs without links: " + missingLinkNodeIds);

		int i = 0;
		var txt = new StringBuffer("Links with weight > 1:\n");
		for (var entry : linkId2domesticWeightSumUpToRegion2584.entrySet()) {
			if (entry.getValue() > 1.0) {
				txt.append(entry + "\t");
				if (++i % 10 == 0) {
					txt.append("\n");
				}
			}
		}
		log.warn(txt);
		this.writeMATSimNetwork(linkId2domesticWeightSumUpToRegion2584.keySet(),
				"./input_2024/network_regionUpTo2584.xml");
		this.writeMATSimNetwork(linkId2domesticWeightSumFromRegion2585.keySet(),
				"./input_2024/network_regionFrom2585.xml");
		this.writeMATSimNetwork(linkId2domesticWeightSumWithoutRegion.keySet(), "./input_2024/network_regionNone.xml");

		final Set<Id<Link>> linkIdsWithTwoDomesticNodes = new LinkedHashSet<>();
		final Set<Id<Link>> linkIdsWithOneDomesticNode = new LinkedHashSet<>();
		for (Link link : this.network.getLinks().values()) {
			int domesticNodeCnt = 0;
			final SamgodsNodeAttributes fromNodeAttrs = (SamgodsNodeAttributes) link.getFromNode().getAttributes()
					.getAttribute(SamgodsNodeAttributes.ATTRIBUTE_NAME);
			if (fromNodeAttrs != null && fromNodeAttrs.isDomestic) {
				domesticNodeCnt++;
			}
			final SamgodsNodeAttributes toNodeAttrs = (SamgodsNodeAttributes) link.getToNode().getAttributes()
					.getAttribute(SamgodsNodeAttributes.ATTRIBUTE_NAME);
			if (toNodeAttrs != null && toNodeAttrs.isDomestic) {
				domesticNodeCnt++;
			}
			if (domesticNodeCnt == 2) {
				linkIdsWithTwoDomesticNodes.add(link.getId());
			} else if (domesticNodeCnt == 1) {
				linkIdsWithOneDomesticNode.add(link.getId());
			}
		}
		this.writeMATSimNetwork(linkIdsWithTwoDomesticNodes, "./input_2024/network_twoDomesticNodes.xml");
		this.writeMATSimNetwork(linkIdsWithOneDomesticNode, "./input_2024/network_oneDomesticNode.xml");
		NetworkUtils.writeNetwork(this.network, "./input_2024/entire_network.xml");

		return linkIdsWithTwoDomesticNodes.stream().collect(Collectors.toMap(id -> id, id -> 1.0));
	}

	private void writeMATSimNetwork(Set<Id<Link>> linkIds, String fileName) {

		final Network subnet = NetworkUtils.createNetwork();

		final Set<Node> nodes = new LinkedHashSet<>();
		for (Id<Link> linkId : linkIds) {
			Link link = this.network.getLinks().get(linkId);
			nodes.add(link.getFromNode());
			nodes.add(link.getToNode());
		}
		for (Node node : nodes) {
			NetworkUtils.createAndAddNode(subnet, node.getId(), node.getCoord());
		}

		final Set<Link> links = linkIds.stream().map(id -> this.network.getLinks().get(id)).collect(Collectors.toSet());
		for (Link link : links) {
			Link newLink = NetworkUtils.createAndAddLink(subnet, link.getId(),
					subnet.getNodes().get(link.getFromNode().getId()), subnet.getNodes().get(link.getToNode().getId()),
					link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
			newLink.setAllowedModes(link.getAllowedModes());
		}

		NetworkUtils.writeNetwork(subnet, fileName);
	}

	public static void main(String[] args) throws IOException {

		System.out.println("STARTED");

		Network network = new NetworkReader().load("./input_2024/node_parameters.csv",
				"./input_2024/link_parameters.csv");

		new LinkRegionsReader(network).read("./input_2024/link_regions_domestic.csv");

		System.out.println("DONE");

	}

}
