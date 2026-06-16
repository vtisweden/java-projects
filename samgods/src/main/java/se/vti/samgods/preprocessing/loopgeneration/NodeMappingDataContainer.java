/**
 * se.vti.samgods.transportation.loops
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.samgods.common.OD;
import se.vti.utils.misc.Units;

/**
 * @author GunnarF
 */
class NodeMappingDataContainer {

	private final SamgodsScenarioData loopSamplingData;
	private final TransportDurations transportDurations;
	private final double totalDemand_kTon;
	private final double demandVectorLength_kTon;

	private Map<Id<Node>, Set<NodeWithCoords>> nodeId2SamplingNodes = new LinkedHashMap<>();
	private Map<NodeWithCoords, Id<Node>> samplingNode2NodeId = new LinkedHashMap<>();

	NodeMappingDataContainer(SamgodsScenarioData loopSamplingData, TransportDurations transportDurations) {
		this(loopSamplingData, transportDurations, List.of(List.of()));
	}

	NodeMappingDataContainer(SamgodsScenarioData loopSamplingData, TransportDurations transportDurations,
			List<List<Enum<?>>> allNodeLabels) {
		this.loopSamplingData = loopSamplingData;
		this.transportDurations = transportDurations;
		this.totalDemand_kTon = this.loopSamplingData.computeTotalDemand_kTon();
		this.demandVectorLength_kTon = this.loopSamplingData.computeDemandVectorLength_kTon();

		for (Id<Node> nodeId : loopSamplingData.getAllTerminalNodeIds()) {
			Coord coord = loopSamplingData.getNetwork().getNodes().get(nodeId).getCoord();
			for (List<Enum<?>> nodeLabels : allNodeLabels) {
				this.addNode(nodeId, new NodeWithCoords(nodeId.toString(), coord.getX(), coord.getY(), nodeLabels));
			}
		}
	}

	private void addNode(Id<Node> nodeId, NodeWithCoords samplingNode) {
		this.nodeId2SamplingNodes.computeIfAbsent(nodeId, n -> new LinkedHashSet<NodeWithCoords>()).add(samplingNode);
		this.samplingNode2NodeId.put(samplingNode, nodeId);
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Id<Node>> getAllNodeIdsView() {
		return Collections.unmodifiableSet(this.nodeId2SamplingNodes.keySet());
	}

	Set<NodeWithCoords> getAllSamplingNodesView() {
		return Collections.unmodifiableSet(this.samplingNode2NodeId.keySet());
	}

	Map<OD, Double> getOD2Demand_kTon_View() {
		return Collections.unmodifiableMap(this.loopSamplingData.getOD2Demand_kTon());
	}

	double getDemand_kTon(OD od) {
		return this.loopSamplingData.getOD2Demand_kTon().getOrDefault(od, 0.0);
	}

	Set<NodeWithCoords> getSendingSamplingNodes(OD od) {
		return this.nodeId2SamplingNodes.get(od.origin);
	}

	Set<NodeWithCoords> getReceivingSamplingNodes(OD od) {
		return this.nodeId2SamplingNodes.get(od.destination);
	}

	double getTotalSent_kTon(NodeWithCoords samplingNode) {
		return this.loopSamplingData.getNodeId2Sent_Mton().getOrDefault(this.samplingNode2NodeId.get(samplingNode),
				0.0);
	}

	double getTotalReceived_kTon(NodeWithCoords samplingNode) {
		return this.loopSamplingData.getNodeId2Received_Mton().getOrDefault(this.samplingNode2NodeId.get(samplingNode),
				0.0);
	}

	double getTransportDuration_h(OD od) {
		return Units.H_PER_S * this.transportDurations.getDuration_s(od.origin, od.destination);
	}

	double getTransportDuration_h(NodeWithCoords from, NodeWithCoords to) {
		return this
				.getTransportDuration_h(new OD(this.samplingNode2NodeId.get(from), this.samplingNode2NodeId.get(to)));
	}

	double getTotalDemand_kTon() {
		return this.totalDemand_kTon;
	}

	double getDemandVectorLength_kTon() {
		return this.demandVectorLength_kTon;
	}

	OD getOD(NodeWithCoords fromNode, NodeWithCoords toNode) {
		Id<Node> fromNodeId = this.samplingNode2NodeId.get(fromNode);
		Id<Node> toNodeId = this.samplingNode2NodeId.get(toNode);
		if ((fromNodeId != null) && (toNodeId != null)) {
			return new OD(fromNodeId, toNodeId);
		} else {
			return null;
		}
	}
}
