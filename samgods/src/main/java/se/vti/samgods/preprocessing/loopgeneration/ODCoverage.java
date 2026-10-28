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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

	private List<Set<Integer>> roundTripIndex2ConnectedODIndices = null;

	private List<Set<Integer>> odIndex2ConnectingRoundTripIndices = null;

	private double logWeight = 0.0;

	private long numberOfUpdates = 0;

	// -------------------- CONSTRUCTION --------------------

	ODCoverage(int numberOfRoundTrips, ScenarioContainer scenarioContainer, double minODCoverage,
			Connectivity connectivity) {
		this.numberOfRoundTrips = numberOfRoundTrips;
		this.dataContainer = scenarioContainer;
		this.minODCoverage = minODCoverage;
		this.connectivity = connectivity;
		this.clear();
	}

	// -------------------- GETTERS --------------------

	int getNumberOfConnectingRoundTrips(OD od) {
		Integer index = this.dataContainer.od2index.get(od);
		if (index == null) {
			return 0;
		}
		return this.odIndex2ConnectingRoundTripIndices.get(index).size();
	}

	double getLogWeight() {
		return this.logWeight;
	}

	int getNumberOfODs() {
		return this.odIndex2ConnectingRoundTripIndices.size();
	}

	int getNumberOfRoundTrips() {
		return this.roundTripIndex2ConnectedODIndices.size();
	}

	Set<Integer> getConnectedODIndices(int roundTripIndex) {
		return this.roundTripIndex2ConnectedODIndices.get(roundTripIndex);
	}
	
	Set<Integer> getConnectingRoundTripIndices(int odIndex) {
		return this.odIndex2ConnectingRoundTripIndices.get(odIndex);
	}
	
	// -------------------- INTERNALS --------------------

	private void recomputeInternally(double tol) {
		final double previousLogWeight = this.logWeight;
		this.logWeight = 0.0;
		for (int odIndex = 0; odIndex < this.odIndex2ConnectingRoundTripIndices.size(); odIndex++) {
			int numberOfRoundTrips = this.odIndex2ConnectingRoundTripIndices.get(odIndex).size();
			double demand_kTon = this.dataContainer.getDemand_kTon(odIndex);
			this.logWeight += demand_kTon * Math.log(this.minODCoverage + numberOfRoundTrips);
		}

		final double logWeightError = this.logWeight - previousLogWeight;
		if (Math.abs(logWeightError) > tol) {
			throw new RuntimeException("Unacceptable logWeight error upon recomputation: " + logWeightError);
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

	private <V> Set<V> createImmutableInstanceExcludingValue(Set<V> original, V excludeValue) {
		var copy = new LinkedHashSet<>(original);
		copy.remove(excludeValue);
		return Collections.unmodifiableSet(copy);
	}

	private <V> Set<V> createImmutableInstanceIncludingValue(Set<V> original, V includeValue) {
		var copy = new LinkedHashSet<>(original);
		copy.add(includeValue);
		return Collections.unmodifiableSet(copy);
	}

	@Override
	public void clear() {
		this.roundTripIndex2ConnectedODIndices = new ArrayList<>(
				IntStream.range(0, this.numberOfRoundTrips).boxed().map(i -> Collections.<Integer>emptySet()).toList());

		this.odIndex2ConnectingRoundTripIndices = new ArrayList<>(this.dataContainer.index2OD.size());
		for (int odIndex = 0; odIndex < this.dataContainer.index2OD.size(); odIndex++) {
			this.odIndex2ConnectingRoundTripIndices.add(Collections.<Integer>emptySet());
		}

		this.recomputeInternally(Double.POSITIVE_INFINITY);
		this.numberOfUpdates = 0l;
	}

	@Override
	public void update(RoundTrip<N> newRoundTrip, MultiRoundTrip<N> oldMultiRoundTrip) {

		int changedRoundTripIndex = newRoundTrip.getIndex();

		/*
		 * Remove old round trip data.
		 */

		for (int affectedODIndex : this.roundTripIndex2ConnectedODIndices.get(changedRoundTripIndex)) {
			double demand_kTon = this.dataContainer.getDemand_kTon(affectedODIndex);

			var oldConnectingRoundTrips = this.odIndex2ConnectingRoundTripIndices.get(affectedODIndex);

			// Remove log-weight term for affected OD.
			this.logWeight -= demand_kTon * Math.log(this.minODCoverage + oldConnectingRoundTrips.size());

			// Remove old round trip from list of round trips connecting affected OD.
			var newConnectingRoundTrips = this.createImmutableInstanceExcludingValue(oldConnectingRoundTrips,
					changedRoundTripIndex);
			this.odIndex2ConnectingRoundTripIndices.set(affectedODIndex, newConnectingRoundTrips);

			// Add recomputed log-weight term for affected OD.
			this.logWeight += demand_kTon * Math.log(this.minODCoverage + newConnectingRoundTrips.size());
		}
		// No need to clear the roundTripIndex2ConnectedODs entry because it is overwritten below.

		/*
		 * Add new round trip data.
		 */

		Set<Integer> connectedODIndices = new LinkedHashSet<>();

		for (int fromNodeIndex = 0; fromNodeIndex < newRoundTrip.size(); fromNodeIndex++) {
			for (int toNodeIndex : this.destinationIndices(fromNodeIndex, newRoundTrip)) {

				OD affectedOD = this.dataContainer.createOD(newRoundTrip.getNode(fromNodeIndex),
						newRoundTrip.getNode(toNodeIndex));
				Integer affectedODIndex = this.dataContainer.od2index.get(affectedOD);
				if ((affectedODIndex != null) && (connectedODIndices.add(affectedODIndex))) {

//					double demand_kTon = this.dataContainer.getDemand_kTon(affectedOD);
					double demand_kTon = this.dataContainer.getDemand_kTon(affectedODIndex);

					var oldConnectingRoundTrips = this.odIndex2ConnectingRoundTripIndices.get(affectedODIndex);

					// Remove log-weight term for affected OD.
					this.logWeight -= demand_kTon * Math.log(this.minODCoverage + oldConnectingRoundTrips.size());

					// Add new roundtrip to list of roundtrips connecting the affected OD.
					var newConnectingRoundTrips = this.createImmutableInstanceIncludingValue(oldConnectingRoundTrips,
							changedRoundTripIndex);
					this.odIndex2ConnectingRoundTripIndices.set(affectedODIndex, newConnectingRoundTrips);

					// Add recomputed log-weight term for affected OD.
					this.logWeight += demand_kTon * Math.log(this.minODCoverage + newConnectingRoundTrips.size());
				}
			}
		}

		this.roundTripIndex2ConnectedODIndices.set(changedRoundTripIndex,
				Collections.unmodifiableSet(connectedODIndices));

		this.numberOfUpdates++;
		if (this.numberOfUpdates % (10 * this.numberOfRoundTrips) == 0) {
			this.recomputeInternally(1e-8);
		}
	}

	// -------------------- OVERRIDING OF OBJECT.CLONE --------------------

	private ODCoverage(int numberOfRoundTrips, double minODCoverage, ScenarioContainer dataContainer,
			List<? extends Set<Integer>> roundTripIndex2ConnectedODIndices,
			List<Set<Integer>> odIndex2ConnectingRoundTripIndices, double logWeight, long numberOfUpdates,
			Connectivity connectivity) {
		this.numberOfRoundTrips = numberOfRoundTrips;
		this.minODCoverage = minODCoverage;
		this.dataContainer = dataContainer;
		this.roundTripIndex2ConnectedODIndices = new ArrayList<>(roundTripIndex2ConnectedODIndices);
		this.odIndex2ConnectingRoundTripIndices = new ArrayList<>(odIndex2ConnectingRoundTripIndices);
		this.logWeight = logWeight;
		this.numberOfUpdates = numberOfUpdates;
		this.connectivity = connectivity;
	}

	@Override
	public MultiRoundTripSummary<N> clone() {
		return new ODCoverage<>(this.numberOfRoundTrips, this.minODCoverage, this.dataContainer,
				this.roundTripIndex2ConnectedODIndices, this.odIndex2ConnectingRoundTripIndices, this.logWeight,
				this.numberOfUpdates, this.connectivity);
	}
}
