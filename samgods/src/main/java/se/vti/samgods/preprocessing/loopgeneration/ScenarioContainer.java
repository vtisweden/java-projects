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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.roundtrips.scenario.NodeWithCoords;
import se.vti.samgods.common.OD;

/**
 * @author GunnarF
 */
class ScenarioContainer {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = LogManager.getLogger(ScenarioContainer.class);

	// -------------------- MEMBERS --------------------

	private final SamgodsScenario samgodsScenario;

	private final TransportDurations transportDurations;

	// Necessary because sampling may happen on an expanded network.
	private final Map<Id<Node>, Set<NodeWithCoords>> samgodsNodeId2SamplingNodes = new LinkedHashMap<>();
	private final Map<NodeWithCoords, Id<Node>> samplingNode2SamgodsNodeId = new LinkedHashMap<>();

	final Map<OD, Integer> od2index;
	final List<OD> index2OD;

	private final double[] odIndex2Demand_kTon;
	private final double[] odIndex2Distance_km;

	// -------------------- CONSTRUCTION --------------------

	ScenarioContainer(SamgodsScenario samgodsScenario, List<List<Enum<?>>> allNodeLabels) {
		this.samgodsScenario = samgodsScenario;

		this.transportDurations = new TransportDurations(samgodsScenario.getNetwork(),
				samgodsScenario.computeTerminalNodeIds(), 80.0);

		Map<OD, Integer> od2index = new LinkedHashMap<>();
		List<OD> odList = new ArrayList<>();
		int odIndex = 0;
		var durationStats = new DescriptiveStatistics();
		for (OD activeOD : samgodsScenario.getOD2Demand_kTon().keySet()) {
			durationStats.addValue(transportDurations.getDuration_h(activeOD));
			odList.add(activeOD);
			od2index.put(activeOD, odIndex++);
		}
		this.od2index = Collections.unmodifiableMap(od2index);
		this.index2OD = Collections.unmodifiableList(odList);

		log.info("  Average transport duration is " + durationStats.getMean() + " hours.");
		log.info("  Transport duration standard deviation is " + durationStats.getStandardDeviation() + " hours.");
		log.info("  Maximum transport duration is " + durationStats.getMax() + " hours.");

		var speedStats = new DescriptiveStatistics();
		this.odIndex2Demand_kTon = new double[this.index2OD.size()];
		this.odIndex2Distance_km = new double[this.index2OD.size()];
		for (odIndex = 0; odIndex < this.index2OD.size(); odIndex++) {
			OD od = this.index2OD.get(odIndex);
			this.odIndex2Demand_kTon[odIndex] = this.samgodsScenario.getOD2Demand_kTon().get(od);
			this.odIndex2Distance_km[odIndex] = this.transportDurations.getDistance_km(od);
			if (this.odIndex2Distance_km[odIndex] >= 0.001 && this.transportDurations.getDuration_h(od) > 0.001) {
				speedStats.addValue(this.odIndex2Distance_km[odIndex] / this.transportDurations.getDuration_h(od));
			} else {
				System.out.println("  invalid speed from\t" + this.odIndex2Distance_km[odIndex] + "\t"
						+ this.transportDurations.getDuration_h(od));
			}
		}
		log.info("  Average speed is " + speedStats.getMean() + " km/h.");
		log.info("  Speed standard deviation is " + speedStats.getStandardDeviation() + " km/h.");

		for (Id<Node> samgodsNodeId : samgodsScenario.computeTerminalNodeIds()) {
			Coord coord = samgodsScenario.getNetwork().getNodes().get(samgodsNodeId).getCoord();
			for (List<Enum<?>> nodeLabels : allNodeLabels) {
				NodeWithCoords samplingNode = new NodeWithCoords(samgodsNodeId.toString(), coord.getX(), coord.getY(),
						nodeLabels);
				this.samgodsNodeId2SamplingNodes
						.computeIfAbsent(samgodsNodeId, n -> new LinkedHashSet<NodeWithCoords>()).add(samplingNode);
				this.samplingNode2SamgodsNodeId.put(samplingNode, samgodsNodeId);
			}
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	double getXCoord(Id<Node> samgodsNodeId) {
		// All labeled duplicates have the same coordinates.
		return this.samgodsNodeId2SamplingNodes.get(samgodsNodeId).iterator().next().x;
	}

	double getYCoord(Id<Node> samgodsNodeId) {
		// All labeled duplicates have the same coordinates.
		return this.samgodsNodeId2SamplingNodes.get(samgodsNodeId).iterator().next().y;
	}

	Map<Id<Node>, Set<NodeWithCoords>> getSamgodsNodeId2SamplingNodesView() {
		return Collections.unmodifiableMap(this.samgodsNodeId2SamplingNodes);
	}

	Id<Node> getSamgodsNodeId(NodeWithCoords samplingNode) {
		return this.samplingNode2SamgodsNodeId.get(samplingNode);
	}

//	Set<Id<Node>> getAllNodeIdsView() {
//		return Collections.unmodifiableSet(this.nodeId2SamplingNodes.keySet());
//	}

	Set<NodeWithCoords> getAllSamplingNodesView() {
		return Collections.unmodifiableSet(this.samplingNode2SamgodsNodeId.keySet());
	}

	Map<OD, Double> getOD2Demand_kTon_View() {
		return Collections.unmodifiableMap(this.samgodsScenario.getOD2Demand_kTon());
	}

	double getDemand_kTon(int odIndex) {
		return this.odIndex2Demand_kTon[odIndex];
	}

	double getDistance_km(int odIndex) {
		return this.odIndex2Distance_km[odIndex];
	}

	double getTotalSent_kTon(Id<Node> samgodsNodeId) {
		return this.samgodsScenario.getNodeId2Sent_Mton().getOrDefault(samgodsNodeId, 0.0);
	}

	double getTotalReceived_kTon(Id<Node> samgodsNodeId) {
		return this.samgodsScenario.getNodeId2Received_Mton().getOrDefault(samgodsNodeId, 0.0);
	}

	double getTotalSent_kTon(NodeWithCoords samplingNode) {
		return this.getTotalSent_kTon(this.samplingNode2SamgodsNodeId.get(samplingNode));
	}

	double getTotalReceived_kTon(NodeWithCoords samplingNode) {
		return this.getTotalReceived_kTon(this.samplingNode2SamgodsNodeId.get(samplingNode));
	}

	double getTransportDuration_h(OD od) {
		return this.transportDurations.getDuration_h(od);
	}

	double getTransportDuration_h(NodeWithCoords from, NodeWithCoords to) {
		return this.transportDurations.getDuration_h(this.samplingNode2SamgodsNodeId.get(from),
				this.samplingNode2SamgodsNodeId.get(to));
	}

	double getTotalDemand_kTon() {
		return this.samgodsScenario.computeTotalDemand_kTon();
	}

	double getDemandVectorLength_kTon() {
		return this.samgodsScenario.computeDemandVectorLength_kTon();
	}

	OD createOD(NodeWithCoords fromSamplingNode, NodeWithCoords toSamplingNode) {
		Id<Node> fromNodeId = this.samplingNode2SamgodsNodeId.get(fromSamplingNode);
		Id<Node> toNodeId = this.samplingNode2SamgodsNodeId.get(toSamplingNode);
		if ((fromNodeId != null) && (toNodeId != null)) {
			return new OD(fromNodeId, toNodeId);
		} else {
			return null;
		}
	}
}
