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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.simulator.electrified.ChargingUtils;
import se.vti.samgods.common.OD;

/**
 * @author GunnarF
 */
class GeoJsonWriters {

	static void writeTerminals(Network network, Set<Id<Node>> transferNodeIds, String outputFile) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();
		root.put("type", "FeatureCollection");

		ArrayNode features = mapper.createArrayNode();

		for (Id<Node> nodeId : transferNodeIds) {
			Node node = network.getNodes().get(nodeId);
			
			ObjectNode feature = mapper.createObjectNode();
			feature.put("type", "Feature");

			ObjectNode properties = mapper.createObjectNode();
			properties.put("id", node.getId().toString());
			feature.set("properties", properties);

			ObjectNode geometry = mapper.createObjectNode();
			geometry.put("type", "Point");

			ArrayNode coordinates = mapper.createArrayNode();
			coordinates.add(node.getCoord().getX());
			coordinates.add(node.getCoord().getY());

			geometry.set("coordinates", coordinates);
			feature.set("geometry", geometry);

			features.add(feature);
		}

		root.set("features", features);

		mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputFile), root);
	}

	static void writeLinks(Network network, String outputFile) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();
		root.put("type", "FeatureCollection");

		ArrayNode features = mapper.createArrayNode();

		for (Link link : network.getLinks().values()) {
			ObjectNode feature = mapper.createObjectNode();
			feature.put("type", "Feature");

			ObjectNode properties = mapper.createObjectNode();
			properties.put("id", link.getId().toString());
			properties.put("from", link.getFromNode().getId().toString());
			properties.put("to", link.getToNode().getId().toString());
			feature.set("properties", properties);

			ObjectNode geometry = mapper.createObjectNode();
			geometry.put("type", "LineString");

			ArrayNode coordinates = mapper.createArrayNode();

			ArrayNode fromCoord = mapper.createArrayNode();
			fromCoord.add(link.getFromNode().getCoord().getX());
			fromCoord.add(link.getFromNode().getCoord().getY());

			ArrayNode toCoord = mapper.createArrayNode();
			toCoord.add(link.getToNode().getCoord().getX());
			toCoord.add(link.getToNode().getCoord().getY());

			coordinates.add(fromCoord);
			coordinates.add(toCoord);

			geometry.set("coordinates", coordinates);
			feature.set("geometry", geometry);

			features.add(feature);
		}

		root.set("features", features);

		mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputFile), root);
	}
	
	static void writeCoverage(ScenarioDataContainer dataContainer, List<MultiRoundTrip<NodeWithCoords>> allSolutions) {

		// total sent == total received
		double totalTarget_kTon = dataContainer.getTotalDemand_kTon();

		Map<NodeWithCoords, DescriptiveStatistics> sender2FlowPerConnectionStats_kTon = new LinkedHashMap<>();
		Map<NodeWithCoords, DescriptiveStatistics> receiver2FlowPerConnectionStats_kTon = new LinkedHashMap<>();

		Map<NodeWithCoords, DescriptiveStatistics> sender2DisconnectedFlowStats_kTon = new LinkedHashMap<>();
		Map<NodeWithCoords, DescriptiveStatistics> receiver2DisconnectedFlowStats_kTon = new LinkedHashMap<>();

		Map<NodeWithCoords, Integer> node2usagesForCharging = new LinkedHashMap<>();

		int i = 0;
		for (var solution : allSolutions) {
			System.out.println("Processing sample " + (++i) + " / " + allSolutions.size());

			ODCoverage<NodeWithCoords> demandCoverage = solution.getSummary(ODCoverage.class);
			double totalCoverage = dataContainer.getOD2Demand_kTon_View().keySet().stream()
					.mapToDouble(od -> demandCoverage.getNumberOfConnectingRoundTrips(od)).sum();

			for (var nodeAndUsages : MaxNumberOfChargingPointsConstraint
					.computeChargingNodeUsages(solution, ChargingUtils.singleton()).entrySet()) {
				node2usagesForCharging.compute(nodeAndUsages.getKey(),
						(n, c) -> (c == null ? 0 : c) + nodeAndUsages.getValue());
			}

			if ((totalTarget_kTon >= 1e-3) && (totalCoverage >= 1)) {

				Map<NodeWithCoords, Double> sender2DisconnectedFlow_kTon = new LinkedHashMap<>();
				Map<NodeWithCoords, Double> receiver2DisconnectedFlow_kTon = new LinkedHashMap<>();

				for (var odEntry : dataContainer.getOD2Demand_kTon_View().entrySet()) {

					OD od = odEntry.getKey();
					var sender = dataContainer.getSendingSamplingNodes(od).iterator().next();
					var receiver = dataContainer.getReceivingSamplingNodes(od).iterator().next();

					double flow_kTon = odEntry.getValue();
					int connections = demandCoverage.getNumberOfConnectingRoundTrips(od);

					if (connections > 0) {
						/*
						 * We are interested, per node in the average coverage over all OD relations and
						 * over all solutions. We hence average one by one, over OD relations and
						 * solutions.
						 */
						double flowPerConnection_kTon = flow_kTon / (0.001 + connections);
						sender2FlowPerConnectionStats_kTon.computeIfAbsent(sender, s -> new DescriptiveStatistics())
								.addValue(flowPerConnection_kTon);
						receiver2FlowPerConnectionStats_kTon.computeIfAbsent(receiver, r -> new DescriptiveStatistics())
								.addValue(flowPerConnection_kTon);
					} else {
						/*
						 * We are interested, per node in the total disconnected flow, on average over
						 * all solutions. We hence sum up values within one solution and average later
						 * over solutions.
						 */
						sender2DisconnectedFlow_kTon.compute(sender, (s, f) -> (f == null ? 0.0 : f) + flow_kTon);
						receiver2DisconnectedFlow_kTon.compute(receiver, (r, f) -> (f == null ? 0.0 : f) + flow_kTon);
					}
				}

				/*
				 * Now we update the average disconnected flow over solutions.
				 */
				for (NodeWithCoords node : dataContainer.getAllSamplingNodesView()) {
					sender2DisconnectedFlowStats_kTon.computeIfAbsent(node, n -> new DescriptiveStatistics())
							.addValue(sender2DisconnectedFlow_kTon.getOrDefault(node, 0.0));
					receiver2DisconnectedFlowStats_kTon.computeIfAbsent(node, n -> new DescriptiveStatistics())
							.addValue(receiver2DisconnectedFlow_kTon.getOrDefault(node, 0.0));
				}

			}
		}

		List<String> labels = new ArrayList<>();
		List<Function<NodeWithCoords, String>> extractors = new ArrayList<>();

		// MEANS

		labels.add("MeanConnected(sender)");
		extractors.add(sender -> {
			var stat = sender2FlowPerConnectionStats_kTon.get(sender);
			return (stat == null || stat.getN() == 0) ? "null" : "" + stat.getMean();
		});

		labels.add("MeanConnected(receiver)");
		extractors.add(sender -> {
			var stat = receiver2FlowPerConnectionStats_kTon.get(sender);
			return (stat == null || stat.getN() == 0) ? "null" : "" + stat.getMean();
		});

		labels.add("Disconnected(sender)");
		extractors.add(sender -> {
			var stat = sender2DisconnectedFlowStats_kTon.get(sender);
			return (stat == null || stat.getN() == 0) ? "null" : "" + stat.getMean();
		});

		labels.add("Disconnected(receiver)");
		extractors.add(receiver -> {
			var stat = receiver2DisconnectedFlowStats_kTon.get(receiver);
			return (stat == null || stat.getN() == 0) ? "null" : "" + stat.getMean();
		});

		// MEDIANS

		labels.add("MedianMeanConnected(sender)");
		extractors.add(sender -> {
			var stat = sender2FlowPerConnectionStats_kTon.get(sender);
			return (stat == null || stat.getN() == 0) ? "null" : "" + stat.getPercentile(50.0);
		});

		labels.add("MedianMeanConnected(receiver)");
		extractors.add(sender -> {
			var stat = receiver2FlowPerConnectionStats_kTon.get(sender);
			return (stat == null || stat.getN() == 0) ? "null" : "" + stat.getPercentile(50.0);
		});

		labels.add("MedianDisconnected(sender)");
		extractors.add(sender -> {
			var stat = sender2DisconnectedFlowStats_kTon.get(sender);
			return (stat == null || stat.getN() == 0) ? "null" : "" + stat.getPercentile(50.0);
		});

		labels.add("MedianDisconnected(receiver)");
		extractors.add(receiver -> {
			var stat = receiver2DisconnectedFlowStats_kTon.get(receiver);
			return (stat == null || stat.getN() == 0) ? "null" : "" + stat.getPercentile(50.0);
		});

		labels.add("ChargingStationUsage");
		extractors.add(node -> {
			return "" + ((double) node2usagesForCharging.getOrDefault(node, 0)) / allSolutions.size();
		});

		Set<NodeWithCoords> allNodes = new LinkedHashSet<>(sender2FlowPerConnectionStats_kTon.keySet());
		allNodes.addAll(receiver2FlowPerConnectionStats_kTon.keySet());
		allNodes.addAll(sender2DisconnectedFlowStats_kTon.keySet());
		allNodes.addAll(receiver2DisconnectedFlowStats_kTon.keySet());

		writeNodeValues("coverageStats.json", labels, extractors, allNodes);
	}

	private static void writeNodeValues(String fileName, List<String> listOfFieldNames,
			List<Function<NodeWithCoords, String>> listOfExtractors, Set<NodeWithCoords> allNodes) {

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();
		root.put("type", "FeatureCollection");

		ArrayNode features = mapper.createArrayNode();
		root.set("features", features);

		for (NodeWithCoords node : allNodes) {

			ObjectNode feature = mapper.createObjectNode();
			feature.put("type", "Feature");

			// properties
			ObjectNode props = mapper.createObjectNode();
			props.put("Name", node.getBasicName());

			for (int i = 0; i < listOfFieldNames.size(); i++) {
				String fieldName = listOfFieldNames.get(i);
				String value = listOfExtractors.get(i).apply(node);

				if ("null".equals(value)) {
					props.putNull(fieldName);
				} else {
					props.put(fieldName, Double.parseDouble(value));
				}
			}

			feature.set("properties", props);

			// geometry
			ObjectNode geom = mapper.createObjectNode();
			geom.put("type", "Point");

			ArrayNode coords = mapper.createArrayNode();
			coords.add(node.x);
			coords.add(node.y);

			geom.set("coordinates", coords);
			feature.set("geometry", geom);

			features.add(feature);
		}

		// write file
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), root);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


}
