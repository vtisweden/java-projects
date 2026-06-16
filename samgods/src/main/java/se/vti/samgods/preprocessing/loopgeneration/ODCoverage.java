/**
 * hermes
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripSummary;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.samgods.common.OD;

/**
 * 
 * @author GunnarF
 *
 */
public class ODCoverage<N extends NodeWithCoords> implements MultiRoundTripSummary<N> {

	// -------------------- CONSTANTS --------------------

	private final int numberOfRoundTrips;

	private final NodeMappingDataContainer dataContainer;

	private final double minODCoverage;

	// -------------------- MEMBERS --------------------

	private boolean checkConsistency = false;

	private List<LinkedHashSet<OD>> connectedODsPerRoundTripIndex;

	private Map<OD, Set<Integer>> od2ConnectingRoundTripIndices;

	private double logWeight;

	private double disconnectedFlow_kTon;

	private int numberOfConnectingRoundTripSegments;

	private long numberOfUpdates;

	// -------------------- CONSTRUCTION --------------------

	public ODCoverage(int numberOfRoundTrips, NodeMappingDataContainer dataContainer, double minODCoverage) {
		this.numberOfRoundTrips = numberOfRoundTrips;
		this.dataContainer = dataContainer;
		this.minODCoverage = minODCoverage;
		this.initialize();
	}

	// -------------------- INTERNALS --------------------

	private void initialize() {
		this.connectedODsPerRoundTripIndex = IntStream.range(0, this.numberOfRoundTrips).boxed()
				.map(i -> new LinkedHashSet<OD>()).toList();
		this.od2ConnectingRoundTripIndices = this.dataContainer.getOD2Demand_kTon_View().keySet().stream()
				.collect(Collectors.toMap(od -> od, od -> new LinkedHashSet<>()));
		this.recomputeStatistics();
	}

	private void recomputeStatistics() {
		this.logWeight = 0.0;
		this.disconnectedFlow_kTon = 0.0;
		this.numberOfConnectingRoundTripSegments = 0;
		for (var odAndRoundTripIndices : this.od2ConnectingRoundTripIndices.entrySet()) {
			var od = odAndRoundTripIndices.getKey();
			int numberOfRoundTrips = odAndRoundTripIndices.getValue().size();
			double demand_kTon = this.dataContainer.getDemand_kTon(od);
			this.logWeight += demand_kTon * Math.log(this.minODCoverage + numberOfRoundTrips);
			if (numberOfRoundTrips == 0) {
				this.disconnectedFlow_kTon += demand_kTon;
			} else {
				this.numberOfConnectingRoundTripSegments += numberOfRoundTrips;
			}
		}
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public ODCoverage<N> setCheckConsistency(boolean checkConsistency) {
		this.checkConsistency = checkConsistency;
		return this;
	}

	public int getNumberOfConnectingRoundTrips(OD od) {
		Set<Integer> connectingRoundTripIndices = this.od2ConnectingRoundTripIndices.get(od);
		if (connectingRoundTripIndices == null) {
			return 0;
		} else {
			return connectingRoundTripIndices.size();
		}
	}

	public double getLogWeight() {
		return this.logWeight;
	}

	public double getConnectedFlow_kTon() {
		return this.dataContainer.getTotalDemand_kTon() - this.disconnectedFlow_kTon;
	}

	public double getDisconnectedFlow_kTon() {
		return this.disconnectedFlow_kTon;
	}

	public int getNumberOfConnectingRoundTripSegments() {
		return this.numberOfConnectingRoundTripSegments;
	}

	// --------------- IMPLEMENTATION OF MultiRoundTripSummary ---------------

	@Override
	public void clear() {
		this.initialize();
	}

	@Override
	public void update(int roundTripIndex, RoundTrip<N> oldRoundTrip, RoundTrip<N> newRoundTrip) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(RoundTrip<N> newRoundTrip, MultiRoundTrip<N> multiRoundTrip) {

		int changedRoundTripIndex = newRoundTrip.getIndex();

		// Remove old roundtrip data.

		for (var affectedOD : this.connectedODsPerRoundTripIndex.get(changedRoundTripIndex)) {
			
			// Remove all logweight terms from affected (part of replaced roundtrip) OD.
			double demand_kTon = this.dataContainer.getDemand_kTon(affectedOD);
			int numberOfRoundTrips = this.od2ConnectingRoundTripIndices.get(affectedOD).size();
			this.logWeight -= demand_kTon * Math.log(this.minODCoverage + numberOfRoundTrips);

			// Remove replaced roundtrip from list of roundtrips connecting the affected OD.
			var connectingRoundTripIndices = this.od2ConnectingRoundTripIndices.get(affectedOD);
			connectingRoundTripIndices.remove(changedRoundTripIndex);
			if (connectingRoundTripIndices.size() == 0) {
				this.disconnectedFlow_kTon += this.dataContainer.getDemand_kTon(affectedOD);
			}
			
			// (Re-)add remaining logweight terms to affected (part of replaced roundtrip) OD.
			numberOfRoundTrips = this.od2ConnectingRoundTripIndices.get(affectedOD).size();
			this.logWeight += demand_kTon * Math.log(this.minODCoverage + numberOfRoundTrips);
		}

		this.numberOfConnectingRoundTripSegments -= this.connectedODsPerRoundTripIndex.get(changedRoundTripIndex)
				.size();
		this.connectedODsPerRoundTripIndex.get(changedRoundTripIndex).clear();

		// Add new roundtrip data.

		for (int fromNodeIndex = 0; fromNodeIndex < newRoundTrip.size(); fromNodeIndex++) {
			for (int toNodeIndex = 0; toNodeIndex < newRoundTrip.size(); toNodeIndex++) {
				if (fromNodeIndex != toNodeIndex) {
					
					N fromNode = newRoundTrip.getNode(fromNodeIndex);
//					N toNode = newRoundTrip.getSuccessorNode(toNodeIndex);
					N toNode = newRoundTrip.getNode(toNodeIndex);
					OD affectedOD = this.dataContainer.getOD(fromNode, toNode);
					
					if ((affectedOD != null) && this.dataContainer.getOD2Demand_kTon_View().containsKey(affectedOD)) {

						// Remove all logweight terms from affected (part of newly added roundtrip) OD.
						double demand_kTon = this.dataContainer.getDemand_kTon(affectedOD);
						int numberOfRoundTrips = this.od2ConnectingRoundTripIndices.get(affectedOD).size();
						this.logWeight -= demand_kTon * Math.log(this.minODCoverage + numberOfRoundTrips);

						// Register the new roundtrip as connecting the affected OD pair.
						this.connectedODsPerRoundTripIndex.get(changedRoundTripIndex).add(affectedOD);

						// Add new roundtrip to list of roundtrips connecting the affected OD.
						var connectingRoundTripIndices = this.od2ConnectingRoundTripIndices.get(affectedOD);
						if (!connectingRoundTripIndices.contains(changedRoundTripIndex)) {
							if (connectingRoundTripIndices.size() == 0) {
								this.disconnectedFlow_kTon -= this.dataContainer.getDemand_kTon(affectedOD);
							}
							connectingRoundTripIndices.add(changedRoundTripIndex);
							this.numberOfConnectingRoundTripSegments++;
						}

						// (Re-)add remaining logweight terms to affected (part of new roundtrip) OD.
//						numberOfRoundTrips = this.od2ConnectingRoundTripIndices.get(affectedOD).size();
						numberOfRoundTrips = connectingRoundTripIndices.size();
						this.logWeight += demand_kTon * Math.log(this.minODCoverage + numberOfRoundTrips);
					}
				}
			}
		}

		// Optional: Check consistency.

		if (this.checkConsistency) {

			double currentLogWeight = this.logWeight;
			double currentDisconnectedFlow_kTon = this.disconnectedFlow_kTon;
			int currentNumberOfConnectingRoundTripSegments = this.numberOfConnectingRoundTripSegments;
			this.recomputeStatistics();
			double logWeightError = currentLogWeight - this.logWeight;
			double disconnectedFlowError = currentDisconnectedFlow_kTon - this.disconnectedFlow_kTon;
			int numberOfConnectingLoopsError = currentNumberOfConnectingRoundTripSegments
					- this.numberOfConnectingRoundTripSegments;
			double absErr = Math.max(Math.max(Math.abs(logWeightError), Math.abs(disconnectedFlowError)),
					Math.abs(numberOfConnectingLoopsError));
			if (absErr > 1e-8) {
				throw new RuntimeException("logWeightError = " + logWeightError + "\ndisconnectedFlowError_kTon = "
						+ disconnectedFlowError + "\nnumberOfConnectingLoopsError = " + numberOfConnectingLoopsError);
			}
			this.logWeight = currentLogWeight;
			this.disconnectedFlow_kTon = currentDisconnectedFlow_kTon;
			this.numberOfConnectingRoundTripSegments = currentNumberOfConnectingRoundTripSegments;

			for (int roundTripIndex = 0; roundTripIndex < this.numberOfRoundTrips; roundTripIndex++) {
				for (var od : this.connectedODsPerRoundTripIndex.get(roundTripIndex)) {
					if (!this.od2ConnectingRoundTripIndices.get(od).contains(roundTripIndex)) {
						throw new RuntimeException("Round trip " + roundTripIndex + " connects OD pair " + od
								+ ", but that OD pair does list the round trip as connecting.");
					}
				}
			}
			for (var entry : this.od2ConnectingRoundTripIndices.entrySet()) {
				var od = entry.getKey();
				for (int roundTripIndex : entry.getValue()) {
					if (!this.connectedODsPerRoundTripIndex.get(roundTripIndex).contains(od)) {
						throw new RuntimeException("OD pair " + od + " is connected by round trip " + roundTripIndex
								+ ", but that round trip does not list the OD pair as connected.");
					}
				}
			}
		}

		this.numberOfUpdates++;
		if (this.numberOfUpdates % (10 * this.numberOfRoundTrips) == 0) {
			this.recomputeStatistics();
		}
	}

	/* for cloning */ private ODCoverage(int numberOfRoundTrips, double minODCoverage,
			NodeMappingDataContainer dataContainer, List<? extends Set<OD>> connectedODsPerRoundTripIndex,
			Map<OD, Set<Integer>> od2ConnectingRoundTripIndices, double logWeight, double disconnectedFlow_kTon,
			int numberOfConnectingLoopSegments, long numberOfUpdates) {

		this.numberOfRoundTrips = numberOfRoundTrips;

		this.minODCoverage = minODCoverage;

		this.dataContainer = dataContainer;

		this.connectedODsPerRoundTripIndex = new ArrayList<>(numberOfRoundTrips);
		for (Set<OD> ods : connectedODsPerRoundTripIndex) {
			this.connectedODsPerRoundTripIndex.add(new LinkedHashSet<OD>(ods));
		}

		this.od2ConnectingRoundTripIndices = new LinkedHashMap<>(od2ConnectingRoundTripIndices.size());
		for (var odAndRoundTrips : od2ConnectingRoundTripIndices.entrySet()) {
			this.od2ConnectingRoundTripIndices.put(odAndRoundTrips.getKey(),
					new LinkedHashSet<>(odAndRoundTrips.getValue()));
		}

		this.logWeight = logWeight;
		this.disconnectedFlow_kTon = disconnectedFlow_kTon;
		this.numberOfConnectingRoundTripSegments = numberOfConnectingLoopSegments;
		this.numberOfUpdates = numberOfUpdates;
	}

	@Override
	public MultiRoundTripSummary<N> clone() {
		ODCoverage<N> child = new ODCoverage<>(this.numberOfRoundTrips, this.minODCoverage, this.dataContainer,
				this.connectedODsPerRoundTripIndex, this.od2ConnectingRoundTripIndices, this.logWeight,
				this.disconnectedFlow_kTon, this.numberOfConnectingRoundTripSegments, this.numberOfUpdates);
		return child;
	}
}
