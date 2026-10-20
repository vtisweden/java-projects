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

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.multi.MultiRoundTripSummary;
import se.vti.roundtrips.scenario.NodeWithCoords;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.samgods.common.OD;

/**
 * 
 * @author GunnarF
 *
 */
class ODCoverage<N extends NodeWithCoords> implements MultiRoundTripSummary<N> {

	enum Connectivity {
		DIRECT, FULL
	};

	// -------------------- CONSTANTS --------------------

	private final int numberOfRoundTrips;

	private final ScenarioContainer dataContainer;

	private final double minODCoverage;

	private final Connectivity connectivity;

	// -------------------- MEMBERS --------------------

	private List<LinkedHashSet<OD>> roundTripIndex2ConnectedODs = null;

	private Map<OD, Set<Integer>> od2ConnectingRoundTripIndices = null;

	private Double logWeight = null;

	private Long numberOfUpdates = null;

	// -------------------- CONSTRUCTION --------------------

	ODCoverage(int numberOfRoundTrips, ScenarioContainer dataContainer, double minODCoverage,
			Connectivity connectivity) {
		this.numberOfRoundTrips = numberOfRoundTrips;
		this.dataContainer = dataContainer;
		this.minODCoverage = minODCoverage;
		this.connectivity = connectivity;
		this.clear();
	}

	// -------------------- GETTERS --------------------

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

	// -------------------- INTERNALS --------------------

	private void recomputeInternally(double tol) {
		final Double previousLogWeight = this.logWeight;
		this.logWeight = 0.0;
		for (var odAndRoundTripIndices : this.od2ConnectingRoundTripIndices.entrySet()) {
			var od = odAndRoundTripIndices.getKey();
			int numberOfRoundTrips = odAndRoundTripIndices.getValue().size();
			double demand_kTon = this.dataContainer.getDemand_kTon(od);
			this.logWeight += demand_kTon * Math.log(this.minODCoverage + numberOfRoundTrips);
		}

		if (previousLogWeight != null) {
			final double logWeightError = this.logWeight - previousLogWeight;
			if (Math.abs(logWeightError) > tol) {
				throw new RuntimeException("Unacceptable logWeight error upon recomputation: " + logWeightError);
			}
		}
	}

	// TODO A roundtrip may visit the same node multiple times.
	private int[] destinationIndices(int fromNodeIndex, RoundTrip<N> roundTrip) {
		if (Connectivity.DIRECT.equals(this.connectivity)) {
			if (roundTrip.size() > 1) {
				return new int[] { (fromNodeIndex + 1) % roundTrip.size() };
			} else {
				return new int[] {};
			}
		} else if (Connectivity.FULL.equals(this.connectivity)) {
			return IntStream.range(0, roundTrip.size()).filter(i -> i != fromNodeIndex).toArray();
		} else {
			throw new RuntimeException("Unknown connectivity : " + this.connectivity);
		}
	}

	// --------------- IMPLEMENTATION OF MultiRoundTripSummary ---------------

	@Override
	public void clear() {
		this.roundTripIndex2ConnectedODs = IntStream.range(0, this.numberOfRoundTrips).boxed()
				.map(i -> new LinkedHashSet<OD>()).toList();
		this.od2ConnectingRoundTripIndices = this.dataContainer.getOD2Demand_kTon_View().keySet().stream()
				.collect(Collectors.toMap(od -> od, od -> new LinkedHashSet<>()));
		this.recomputeInternally(Double.POSITIVE_INFINITY);
		this.numberOfUpdates = 0l;
	}

	@Override
	public void update(RoundTrip<N> newRoundTrip, MultiRoundTrip<N> oldMultiRoundTrip) {

		int changedRoundTripIndex = newRoundTrip.getIndex();

		/*
		 * Remove old round trip data.
		 */

		for (var affectedOD : this.roundTripIndex2ConnectedODs.get(changedRoundTripIndex)) {
			double demand_kTon = this.dataContainer.getDemand_kTon(affectedOD);
			var connectingRoundTripIndices = this.od2ConnectingRoundTripIndices.get(affectedOD);

			// Remove log-weight term for affected OD.
			this.logWeight -= demand_kTon * Math.log(this.minODCoverage + connectingRoundTripIndices.size());

			// Remove old round trip from list of round trips connecting affected OD.
			connectingRoundTripIndices.remove(changedRoundTripIndex);

			// Add recomputed log-weight term for affected OD.
			this.logWeight += demand_kTon * Math.log(this.minODCoverage + connectingRoundTripIndices.size());
		}

		this.roundTripIndex2ConnectedODs.get(changedRoundTripIndex).clear();

		/*
		 * Add new round trip data.
		 */

		for (int fromNodeIndex = 0; fromNodeIndex < newRoundTrip.size(); fromNodeIndex++) {
			for (int toNodeIndex : this.destinationIndices(fromNodeIndex, newRoundTrip)) {

				OD affectedOD = this.dataContainer.createOD(newRoundTrip.getNode(fromNodeIndex),
						newRoundTrip.getNode(toNodeIndex));
				if ((affectedOD != null) && this.dataContainer.getOD2Demand_kTon_View().containsKey(affectedOD)) {
					double demand_kTon = this.dataContainer.getDemand_kTon(affectedOD);
					var connectingRoundTripIndices = this.od2ConnectingRoundTripIndices.get(affectedOD);

					// Remove log-weight term for affected OD.
					this.logWeight -= demand_kTon * Math.log(this.minODCoverage + connectingRoundTripIndices.size());

					// Register the new roundtrip as connecting the affected OD pair.
					this.roundTripIndex2ConnectedODs.get(changedRoundTripIndex).add(affectedOD);
					// Add new roundtrip to list of roundtrips connecting the affected OD.
					connectingRoundTripIndices.add(changedRoundTripIndex);

					// Add recomputed log-weight term for affected OD.
					this.logWeight += demand_kTon * Math.log(this.minODCoverage + connectingRoundTripIndices.size());
				}
			}
		}

		this.numberOfUpdates++;
		if (this.numberOfUpdates % (10 * this.numberOfRoundTrips) == 0) {
			this.recomputeInternally(1e-8);
		}
	}

	// -------------------- OVERRIDING OF OBJECT.CLONE --------------------

	private ODCoverage(int numberOfRoundTrips, double minODCoverage, ScenarioContainer dataContainer,
			List<? extends Set<OD>> roundTripIndex2ConnectedODs, Map<OD, Set<Integer>> od2ConnectingRoundTripIndices,
			double logWeight, long numberOfUpdates, Connectivity connectivity) {

		this.numberOfRoundTrips = numberOfRoundTrips;
		this.minODCoverage = minODCoverage;
		this.dataContainer = dataContainer;

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

		this.connectivity = connectivity;
	}

	@Override
	public MultiRoundTripSummary<N> clone() {
		return new ODCoverage<>(this.numberOfRoundTrips, this.minODCoverage, this.dataContainer,
				this.roundTripIndex2ConnectedODs, this.od2ConnectingRoundTripIndices, this.logWeight,
				this.numberOfUpdates, this.connectivity);
	}
}
