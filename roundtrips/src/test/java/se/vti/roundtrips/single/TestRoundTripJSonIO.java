/**
 * se.vti.roundtrips.single
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
package se.vti.roundtrips.single;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.common.Scenario;

/**
 * @author GunnarF
 */
class TestRoundTripJSonIO {

	@TempDir
	Path tempDir;

	private File tempFile(String name) {
		return tempDir.resolve(name).toFile();
	}

	@Nested
	class NodeTests {

		@Test
		void serializeProducesExpectedResult() throws IOException {

			Scenario<Node> scenario = new Scenario<>();
			Node home = scenario.addNode(new Node("home"));
			Node work = scenario.addNode(new Node("work"));
			RoundTrip<Node> rt = new RoundTrip<>(0, Arrays.asList(home, work), Arrays.asList(7, 18));
			File out = tempFile("rt-node.json");

			RoundTripJsonIO.writeToFile(rt, out.getAbsolutePath());

			ObjectMapper om = new ObjectMapper();
			JsonNode root = om.readTree(out);
			assertTrue(root.has("index"));
			assertTrue(root.has("nodes"));
			assertTrue(root.has("dptBins"));

			assertEquals(0, root.get("index").asInt(), "index must match");

			JsonNode nodes = root.get("nodes");
			assertEquals(2, nodes.size(), "nodes length must match");
			assertEquals("home", nodes.get(0).asText());
			assertEquals("work", nodes.get(1).asText());

			JsonNode dptBins = root.get("dptBins");
			assertEquals(2, dptBins.size(), "departures length must match");
			assertEquals(7, dptBins.get(0).asInt());
			assertEquals(18, dptBins.get(1).asInt());
		}

		@Test
		void deserializeProducesExpectedResult() throws IOException {

			Scenario<Node> scenario = new Scenario<>();
			Node home = scenario.addNode(new Node("home"));
			Node work = scenario.addNode(new Node("work"));
			List<Integer> dpts = Arrays.asList(7, 18);
			RoundTrip<Node> original = new RoundTrip<>(42, Arrays.asList(home, work), dpts);
			File out = tempFile("rt-node-roundtrip.json");

			RoundTripJsonIO.writeToFile(original, out.getAbsolutePath());
			RoundTrip<Node> restored = RoundTripJsonIO.readFromFile(scenario, out.getAbsolutePath());

			assertNotNull(restored);
			assertEquals(42, restored.getIndex());
			assertEquals(Arrays.asList(7, 18), restored.getDeparturesView());

			assertEquals(2, restored.getNodesView().size());
			assertEquals("home", restored.getNodesView().get(0).getName());
			assertEquals("work", restored.getNodesView().get(1).getName());

			assertSame(home, restored.getNodesView().get(0),
					"Deserialized node 'home' should be the same instance as in scenario");
			assertSame(work, restored.getNodesView().get(1),
					"Deserialized node 'work' should be the same instance as in scenario");
		}
	}

	@Nested
	class NodeWithCoordsTests {

		@Test
		void serializeProducesExpectedResult() throws IOException {

			Scenario<NodeWithCoords> scenario = new Scenario<>();
			NodeWithCoords home = scenario.addNode(new NodeWithCoords("home", 1, 2, 0));
			NodeWithCoords work = scenario.addNode(new NodeWithCoords("work", 3, 4, 0));
			RoundTrip<NodeWithCoords> rt = new RoundTrip<>(0, Arrays.asList(home, work), Arrays.asList(7, 18));
			File out = tempFile("rt-nodecoords.json");

			RoundTripJsonIO.writeToFile(rt, out.getAbsolutePath());

			ObjectMapper om = new ObjectMapper();
			JsonNode root = om.readTree(out);
			assertEquals(0, root.get("index").asInt());

			JsonNode nodes = root.get("nodes");
			assertEquals(2, nodes.size());
			assertEquals("home", nodes.get(0).asText());
			assertEquals("work", nodes.get(1).asText());

			JsonNode dptBins = root.get("dptBins");
			assertEquals(7, dptBins.get(0).asInt());
			assertEquals(18, dptBins.get(1).asInt());
		}

		@Test
		void deserializeProducesExpectedResult() throws IOException {

			Scenario<NodeWithCoords> scenario = new Scenario<>();
			NodeWithCoords home = scenario.addNode(new NodeWithCoords("home", 1, 2, 0));
			NodeWithCoords work = scenario.addNode(new NodeWithCoords("work", 3, 4, 0));
			RoundTrip<NodeWithCoords> original = new RoundTrip<>(7, Arrays.asList(home, work), Arrays.asList(8, 19));
			File out = tempFile("rt-nodecoords-roundtrip.json");

			RoundTripJsonIO.writeToFile(original, out.getAbsolutePath());
			RoundTrip<NodeWithCoords> restored = RoundTripJsonIO.readFromFile(scenario, out.getAbsolutePath());

			assertNotNull(restored);
			assertEquals(7, restored.getIndex());
			assertEquals(Arrays.asList(8, 19), restored.getDeparturesView());

			assertEquals(2, restored.getNodesView().size());
			NodeWithCoords restoredHome = restored.getNodesView().get(0);
			NodeWithCoords restoredWork = restored.getNodesView().get(1);

			assertEquals("home", restoredHome.getName());
			assertEquals("work", restoredWork.getName());

			assertSame(home, restoredHome, "Deserialized NodeWithCoords 'home' should be same instance as in scenario");
			assertSame(work, restoredWork, "Deserialized NodeWithCoords 'work' should be same instance as in scenario");
		}
	}
}
