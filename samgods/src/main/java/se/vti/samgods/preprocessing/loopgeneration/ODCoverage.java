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
class ODCoverage<N extends NodeWithCoords> implements MultiRoundTripSummary<N> {

	// -------------------- CONSTANTS --------------------

	private final int numberOfRoundTrips;

	private final ScenarioDataContainer dataContainer;

	private final double minODCoverage;

	// -------------------- MEMBERS --------------------

	private boolean checkConsistency = false;

	private List<LinkedHashSet<OD>> roundTripIndex2ConnectedODs = null;

	private Map<OD, Set<Integer>> od2ConnectingRoundTripIndices = null;

	private Double logWeight = null;

	private Long numberOfUpdates = null;

	// -------------------- CONSTRUCTION --------------------

	ODCoverage(int numberOfRoundTrips, ScenarioDataContainer dataContainer, double minODCoverage) {
		this.numberOfRoundTrips = numberOfRoundTrips;
		this.dataContainer = dataContainer;
		this.minODCoverage = minODCoverage;
		this.clear();
	}

	// -------------------- SETTERS AND GETTERS --------------------

	ODCoverage<N> setCheckConsistency(boolean checkConsistency) {
		this.checkConsistency = checkConsistency;
		return this;
	}

	int getNumberOfConnectingRoundTrips(OD od) {
		Set<Integer> connectingRoundTripIndices = this.od2ConnectingRoundTripIndices.get(od);
		if (connectingRoundTripIndices == null) {
			return 0;
		} else {
			return connectingRoundTripIndices.size();
		}
	}

	double getLogWeight() {
		return this.logWeight;
	}

	// --------------- IMPLEMENTATION OF MultiRoundTripSummary ---------------

	private void recomputeStatistics() {
		this.logWeight = 0.0;
		for (var odAndRoundTripIndices : this.od2ConnectingRoundTripIndices.entrySet()) {
			var od = odAndRoundTripIndices.getKey();
			int numberOfRoundTrips = odAndRoundTripIndices.getValue().size();
			double demand_kTon = this.dataContainer.getDemand_kTon(od);
			this.logWeight += demand_kTon * Math.log(this.minODCoverage + numberOfRoundTrips);
		}
	}

	@Override
	public void clear() {
		this.roundTripIndex2ConnectedODs = IntStream.range(0, this.numberOfRoundTrips).boxed()
				.map(i -> new LinkedHashSet<OD>()).toList();
		this.od2ConnectingRoundTripIndices = this.dataContainer.getOD2Demand_kTon_View().keySet().stream()
				.collect(Collectors.toMap(od -> od, od -> new LinkedHashSet<>()));
		this.recomputeStatistics();
		this.numberOfUpdates = 0l;
	}

	@Override
	public void update(int roundTripIndex, RoundTrip<N> oldRoundTrip, RoundTrip<N> newRoundTrip) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(RoundTrip<N> newRoundTrip, MultiRoundTrip<N> multiRoundTrip) {

		int changedRoundTripIndex = newRoundTrip.getIndex();

		// Remove old round trip data.

		for (var affectedOD : this.roundTripIndex2ConnectedODs.get(changedRoundTripIndex)) {
			double demand_kTon = this.dataContainer.getDemand_kTon(affectedOD);
			var connectingRoundTripIndices = this.od2ConnectingRoundTripIndices.get(affectedOD);

			// Remove log-weight term for affected (part of replaced round trip) OD.
			this.logWeight -= demand_kTon * Math.log(this.minODCoverage + connectingRoundTripIndices.size());

			// Remove replaced round trip from list of round trips connecting affected OD.
			connectingRoundTripIndices.remove(changedRoundTripIndex);

			// (Re-)add remaining log-weight term for affected OD.
			this.logWeight += demand_kTon * Math.log(this.minODCoverage + connectingRoundTripIndices.size());
		}

		this.roundTripIndex2ConnectedODs.get(changedRoundTripIndex).clear();

		// Add new round trip data.

		for (int fromNodeIndex = 0; fromNodeIndex < newRoundTrip.size(); fromNodeIndex++) {
			for (int toNodeIndex = 0; toNodeIndex < newRoundTrip.size(); toNodeIndex++) {
				if (fromNodeIndex != toNodeIndex) {

					OD affectedOD = this.dataContainer.getOD(newRoundTrip.getNode(fromNodeIndex),
							newRoundTrip.getNode(toNodeIndex));
					if ((affectedOD != null) && this.dataContainer.getOD2Demand_kTon_View().containsKey(affectedOD)) {

						double demand_kTon = this.dataContainer.getDemand_kTon(affectedOD);
						Set<Integer> connectingRoundTripIndices = this.od2ConnectingRoundTripIndices.get(affectedOD);

						// Remove all logweight terms from affected (part of newly added roundtrip) OD.
						this.logWeight -= demand_kTon
								* Math.log(this.minODCoverage + connectingRoundTripIndices.size());

						// Register the new roundtrip as connecting the affected OD pair.
						this.roundTripIndex2ConnectedODs.get(changedRoundTripIndex).add(affectedOD);
						// Add new roundtrip to list of roundtrips connecting the affected OD.
						connectingRoundTripIndices.add(changedRoundTripIndex);

						// (Re-)add remaining logweight terms to affected (part of new roundtrip) OD.
						this.logWeight += demand_kTon
								* Math.log(this.minODCoverage + connectingRoundTripIndices.size());
					}
				}
			}
		}

		// Optional: Check consistency.

		if (this.checkConsistency) {

			double currentLogWeight = this.logWeight;
			this.recomputeStatistics();
			double logWeightError = currentLogWeight - this.logWeight;
			if (Math.abs(logWeightError) > 1e-8) {
				throw new RuntimeException("logWeightError = " + logWeightError);
			}
			this.logWeight = currentLogWeight;

			for (int roundTripIndex = 0; roundTripIndex < this.numberOfRoundTrips; roundTripIndex++) {
				for (var od : this.roundTripIndex2ConnectedODs.get(roundTripIndex)) {
					if (!this.od2ConnectingRoundTripIndices.get(od).contains(roundTripIndex)) {
						throw new RuntimeException("Round trip " + roundTripIndex + " connects OD pair " + od
								+ ", but that OD pair does list the round trip as connecting.");
					}
				}
			}
			for (var entry : this.od2ConnectingRoundTripIndices.entrySet()) {
				var od = entry.getKey();
				for (int roundTripIndex : entry.getValue()) {
					if (!this.roundTripIndex2ConnectedODs.get(roundTripIndex).contains(od)) {
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

	// -------------------- OVERRIDING OF OBJECT.CLONE --------------------

	private ODCoverage(int numberOfRoundTrips, double minODCoverage, ScenarioDataContainer dataContainer,
			boolean checkConsistency, List<? extends Set<OD>> roundTripIndex2ConnectedODs,
			Map<OD, Set<Integer>> od2ConnectingRoundTripIndices, double logWeight, long numberOfUpdates) {

		this.numberOfRoundTrips = numberOfRoundTrips;
		this.minODCoverage = minODCoverage;
		this.dataContainer = dataContainer;
		this.checkConsistency = checkConsistency;

		this.roundTripIndex2ConnectedODs = new ArrayList<>(numberOfRoundTrips);
		for (Set<OD> ods : roundTripIndex2ConnectedODs) {
			this.roundTripIndex2ConnectedODs.add(new LinkedHashSet<OD>(ods));
		}

		this.od2ConnectingRoundTripIndices = new LinkedHashMap<>(od2ConnectingRoundTripIndices.size());
		for (var odAndRoundTrips : od2ConnectingRoundTripIndices.entrySet()) {
			this.od2ConnectingRoundTripIndices.put(odAndRoundTrips.getKey(),
					new LinkedHashSet<>(odAndRoundTrips.getValue()));
		}

		this.logWeight = logWeight;
		this.numberOfUpdates = numberOfUpdates;
	}

	@Override
	public MultiRoundTripSummary<N> clone() {
		return new ODCoverage<>(this.numberOfRoundTrips, this.minODCoverage, this.dataContainer, this.checkConsistency,
				this.roundTripIndex2ConnectedODs, this.od2ConnectingRoundTripIndices, this.logWeight,
				this.numberOfUpdates);
	}
}
