/**
 * se.vti.roundtrips.samplingweights.misc.accessibility
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
package se.vti.roundtrips.samplingweights.misc.accessibility;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class OriginBasedAccessibility<N extends Node, L extends Object> {

	// -------------------- CONSTANTS --------------------

	private final N origin;

	private final AccessibilityUpdater<N, L> accessibilityUpdater;

	// -------------------- MEMBERS --------------------

//	private Set<N> consideredNodes = null;

	private boolean verbose = false;

	private Map<N, L> node2Labels = null;

	// -------------------- CONSTRUCTION --------------------

	public OriginBasedAccessibility(N origin, AccessibilityUpdater<N, L> accessibilityUpdater) {
		this.origin = origin;
		this.accessibilityUpdater = accessibilityUpdater;
	}

//	public OriginBasedAccessibility<N, L> setConsideredNodes(Set<N> consideredNodes) {
//		this.consideredNodes = consideredNodes;
//		return this;
//	}

//	// -------------------- INTERNALS --------------------
//
//	private boolean isConsidered(N node) {
//		return ((this.consideredNodes == null) || this.consideredNodes.contains(node));
//	}

	// -------------------- IMPLEMENTATION --------------------

	public Map<N, L> getNode2Labels() {
		return this.node2Labels;
	}

	public void compute(MultiRoundTrip<N> roundTrips) {

		this.node2Labels = new LinkedHashMap<>();
		this.accessibilityUpdater.initialize(this.origin, this.node2Labels);

		boolean changedAnyRoundTrip;
		do {
			changedAnyRoundTrip = false;

			for (RoundTrip<N> roundTrip : roundTrips) {
				if (this.verbose) {
					System.out.println("  Round trip:" + roundTrip);
				}

				Set<Integer> activeNodeIndices = IntStream.range(0, roundTrip.size())
						.filter(i -> this.node2Labels.containsKey(roundTrip.getNode(i))).boxed()
						.collect(Collectors.toSet());

				while (activeNodeIndices.size() > 0) {
					if (this.verbose) {
						System.out.println("    active node indices = " + activeNodeIndices);
					}
					Set<Integer> changedNodeIndices = new LinkedHashSet<>(activeNodeIndices.size());
					for (int activeNodeIndex : activeNodeIndices) {
						int succNodeIndex = (activeNodeIndex + 1) % roundTrip.size();
						if (this.accessibilityUpdater.update(roundTrip, activeNodeIndex,
								(activeNodeIndex + 1) % roundTrip.size(), this.node2Labels)) {
							changedNodeIndices.add(succNodeIndex);
							changedAnyRoundTrip = true;
						}
					}
					activeNodeIndices = changedNodeIndices;
				}
			}

		} while (changedAnyRoundTrip);

	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		var a = new Node("A");
		var b = new Node("B");
		var c = new Node("C");
		var d = new Node("D");
		var e = new Node("E");
		var f = new Node("F");

		MultiRoundTrip<Node> multiRT = new MultiRoundTrip<>(2);
		multiRT.setRoundTripAndUpdateSummaries(0,
				new RoundTrip<>(0, Arrays.asList(a, b, c, d), Arrays.asList(0, 0, 0, 0)));
		multiRT.setRoundTripAndUpdateSummaries(1,
				new RoundTrip<>(1, Arrays.asList(d, e, f, b), Arrays.asList(0, 0, 0, 0)));

		AccessibilityUpdater<Node, Integer> updater = new AccessibilityUpdater<>() {

			@Override
			public void initialize(Node origin, Map<Node, Integer> node2Labels) {
				node2Labels.put(origin, 0);
			}

			@Override
			public boolean update(RoundTrip<Node> roundTrip, int fromIndex, int toIndex, Map<Node, Integer> node2accessibilities) {

				Node predNode = roundTrip.getNode(fromIndex);
				Node updatedNode = roundTrip.getNode(toIndex);
				
				int predValue = node2accessibilities.get(predNode);
				int succValue = node2accessibilities.getOrDefault(updatedNode, Integer.MAX_VALUE);

				int dist;
				if (fromIndex < toIndex) {
					dist = toIndex - fromIndex;
				} else {
					dist = (roundTrip.size() + toIndex) - fromIndex;
				}
				
				boolean changed = false;
				if (predValue + dist < succValue) {
					node2accessibilities.put(updatedNode, predValue + dist);
					changed = true;
				}
				this.printMeasures(node2accessibilities);
				return changed;
			}

			private void printMeasures(Map<Node, Integer> node2measures) {
				System.out.println(node2measures.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue())
						.collect(Collectors.joining(", ")));
			}
		};

		var oba = new OriginBasedAccessibility<>(a, updater);
		oba.verbose = true;
		oba.compute(multiRT);

		System.out.println("... DONE");
	}
}
