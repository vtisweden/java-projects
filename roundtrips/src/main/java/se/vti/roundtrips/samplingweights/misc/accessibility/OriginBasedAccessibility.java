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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class OriginBasedAccessibility<N extends Node> {

	// -------------------- CONSTANTS --------------------

	private final N origin;

	private final AccessibilityUpdater<N> accessibilityUpdater;

	// -------------------- MEMBERS --------------------

	private Set<N> consideredNodes = null;

	private Map<N, double[]> node2Accessibilities = null;

	// -------------------- CONSTRUCTION --------------------

	public OriginBasedAccessibility(N origin, AccessibilityUpdater<N> accessibilityUpdater) {
		this.origin = origin;
		this.accessibilityUpdater = accessibilityUpdater;
	}

	public OriginBasedAccessibility<N> setConsideredNodes(Set<N> consideredNodes) {
		this.consideredNodes = consideredNodes;
		return this;
	}

	// -------------------- INTERNALS --------------------

	private boolean isConsidered(N node) {
		return ((this.consideredNodes == null) || this.consideredNodes.contains(node));
	}

	// -------------------- IMPLEMENTATION --------------------

	public void compute(MultiRoundTrip<N> roundTrips) {

		this.node2Accessibilities = new LinkedHashMap<>();
		this.accessibilityUpdater.initialize(this.origin, this.node2Accessibilities);

		boolean changed;
		do {
			changed = false;
			for (RoundTrip<N> roundTrip : roundTrips) {

				Integer fromNodeIndex = this.accessibilityUpdater.computeMostAccessibleNodeIndex(
						roundTrip.getNodesView(), roundTrip.getDeparturesView(), this.node2Accessibilities);
				if (fromNodeIndex != null) {
					N fromNode = roundTrip.getNode(fromNodeIndex);

					List<N> nodeSequence = new ArrayList<>(roundTrip.size());
					List<Integer> departureSequence = new ArrayList<>(roundTrip.size());
					nodeSequence.add(fromNode);
					departureSequence.add(roundTrip.getDeparture(fromNodeIndex));

					if (this.isConsidered(fromNode)) {

						for (int toNodeIndex = fromNodeIndex + 1; toNodeIndex < fromNodeIndex
								+ roundTrip.size(); toNodeIndex++) {
							int effectiveToNodeIndex = toNodeIndex % roundTrip.size();
							N toNode = roundTrip.getNode(effectiveToNodeIndex);
							nodeSequence.add(toNode);
							departureSequence.add(roundTrip.getDeparture(effectiveToNodeIndex));
							if (this.isConsidered(toNode)) {
								changed |= this.accessibilityUpdater.updateDestinationNodeAccessibility(nodeSequence,
										departureSequence, this.node2Accessibilities);
								fromNode = toNode;
								nodeSequence = new ArrayList<>(roundTrip.size());
								departureSequence = new ArrayList<>(roundTrip.size());
								nodeSequence.add(fromNode);
								departureSequence.add(roundTrip.getDeparture(fromNodeIndex));
							}
						}
					}
				}
			}
		} while (changed);
	}

	public Map<N, double[]> getNode2Accessibilities() {
		return this.node2Accessibilities;
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

		AccessibilityUpdater<Node> updater = new AccessibilityUpdater<Node>() {

			@Override
			public String[] getLabels() {
				return new String[] { "number of hops" };
			}

			@Override
			public void initialize(Node node, Map<Node, double[]> node2accessibilities) {
				node2accessibilities.put(node, new double[] { 0.0 });
				this.printMeasures(node2accessibilities);
			}

			@Override
			public boolean updateDestinationNodeAccessibility(List<Node> nodeSequence, List<Integer> departureSequence,
					Map<Node, double[]> node2accessibilities) {
				System.out.println("updating");
				System.out.println("  nodes = " + nodeSequence);
				System.out.println("  departures = " + departureSequence);

				Node updatedNode = nodeSequence.get(nodeSequence.size() - 1);
				boolean changed = false;
				double[] oldValues = node2accessibilities.get(updatedNode);
				if (oldValues == null) {
					oldValues = new double[] { Double.POSITIVE_INFINITY };
					node2accessibilities.put(updatedNode, oldValues);
					changed = true;
				}
				double newValue = node2accessibilities.get(nodeSequence.get(0))[0] + (nodeSequence.size() - 1);
				if (newValue < oldValues[0]) {
					oldValues[0] = newValue;
					changed = true;
				}
				this.printMeasures(node2accessibilities);
				return changed;
			}

			@Override
			public Integer computeMostAccessibleNodeIndex(List<Node> nodeSequence, List<Integer> departureSequence,
					Map<Node, double[]> node2measures) {
				Integer minIndex = null;
				double minHops = Double.POSITIVE_INFINITY;

				for (int i = 0; i < nodeSequence.size(); i++) {
					double[] measures = node2measures.get(nodeSequence.get(i));
					if ((measures != null) && (measures[0] < minHops)) {
						minIndex = i;
						minHops = measures[0];
					}
				}
				return minIndex;
			}

			private void printMeasures(Map<Node, double[]> node2measures) {
				System.out.println(node2measures.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()[0])
						.collect(Collectors.joining(", ")));
			}
		};

		var oba = new OriginBasedAccessibility<>(a, updater);
		oba.setConsideredNodes(new LinkedHashSet<>(Arrays.asList(a, d, f)));
		oba.compute(multiRT);

		System.out.println("... DONE");
	}
}
