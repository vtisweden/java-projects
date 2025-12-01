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
package se.vti.roundtrips.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import se.vti.roundtrips.single.RoundTrip;

/**
 * @author GunnarF
 */
class RoundTripUtilsTest {

	@Test
	void testShortestPath_SameNode() {
		List<Node> nodes = Arrays.asList(new Node("A"), new Node("B"), new Node("C"));
		RoundTrip<Node> roundTrip = new RoundTrip<>(0, nodes, null);

		List<Node> path = RoundTripUtils.shortestPath(nodes.get(1), nodes.get(1), roundTrip);
		assertEquals(Collections.singletonList(nodes.get(1)), path);
	}

	@Test
	void testShortestPath_NodeNotFound() {
		List<Node> nodes = Arrays.asList(new Node("X"), new Node("Y"));
		RoundTrip<Node> roundTrip = new RoundTrip<>(0, nodes, null);

		Node missing = new Node("Z");
		assertNull(RoundTripUtils.shortestPath(nodes.get(0), missing, roundTrip));
	}

	@Test
	void testShortestPath_EmptyRoundTrip() {
		RoundTrip<Node> roundTrip = new RoundTrip<>(0, Collections.emptyList(), null);
		Node a = new Node("A");
		Node b = new Node("B");
		assertNull(RoundTripUtils.shortestPath(a, b, roundTrip));
	}

	@Test
	void testShortestPath_MultipleOccurrences1() {
		Node a = new Node("A");
		Node b = new Node("B");
		List<Node> nodes = Arrays.asList(a, b, a, b, a);
		RoundTrip<Node> roundTrip = new RoundTrip<>(0, nodes, null);

		List<Node> path = RoundTripUtils.shortestPath(a, b, roundTrip);
		// Shortest forward path should be [A, B]
		assertEquals(Arrays.asList(a, b), path);
	}

	@Test
	void testShortestPath_MultipleOccurrences2() {
		Node a = new Node("A");
		Node b = new Node("B");
		Node c = new Node("C");
		List<Node> nodes = Arrays.asList(a, c, c, b, a, c, b);
		RoundTrip<Node> roundTrip = new RoundTrip<>(0, nodes, null);

		List<Node> path = RoundTripUtils.shortestPath(a, b, roundTrip);
		assertEquals(Arrays.asList(a, c, b), path);
	}

	@Test
	void testShortestPath_MultipleOccurrences3() {
		Node a = new Node("A");
		Node b = new Node("B");
		Node c = new Node("C");
		List<Node> nodes = Arrays.asList(b, a, c, c, b, a, c, b, a);
		RoundTrip<Node> roundTrip = new RoundTrip<>(0, nodes, null);

		List<Node> path = RoundTripUtils.shortestPath(a, b, roundTrip);
		assertEquals(Arrays.asList(a, b), path);
	}

	@Test
	void testShortestPath_WrapAround() {
		List<Node> nodes = Arrays.asList(new Node("0"), new Node("1"), new Node("2"), new Node("3"));
		RoundTrip<Node> roundTrip = new RoundTrip<>(0, nodes, null);

		List<Node> path = RoundTripUtils.shortestPath(nodes.get(3), nodes.get(1), roundTrip);
		assertEquals(Arrays.asList(nodes.get(3), nodes.get(0), nodes.get(1)), path);
	}
}
