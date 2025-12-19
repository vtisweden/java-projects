/**
 * se.vti.roundtrips.single
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.common.Scenario;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripJsonIO {

	public static class Serializer extends JsonSerializer<RoundTrip<? extends Node>> {
		@Override
		public void serialize(RoundTrip<? extends Node> value, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {
			gen.writeStartObject();
			gen.writeFieldName("index");
			gen.writeNumber(value.getIndex());
			gen.writeFieldName("nodes");
			gen.writeStartArray();
			for (Node location : value.getNodesView()) {
				gen.writeString(location.getName());
			}
			gen.writeEndArray();
			gen.writeFieldName("dptBins");
			gen.writeStartArray();
			for (Integer departure : value.getDeparturesView()) {
				gen.writeNumber(departure);
			}
			gen.writeEndArray();
			gen.writeEndObject();
		}
	}

	public static class Deserializer<N extends Node> extends JsonDeserializer<RoundTrip<N>> {

		private final Scenario<N> scenario;

		public Deserializer(Scenario<N> scenario) {
			this.scenario = scenario;
		}

		@Override
		public RoundTrip<N> deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {

			JsonNode node = p.getCodec().readTree(p);
			int index = node.get("index").asInt();

			List<N> scenarioNodes = new ArrayList<>();
			JsonNode jsonNodes = node.get("nodes");
			for (JsonNode n : jsonNodes) {
				scenarioNodes.add(this.scenario.getNode(n.asText()));
			}

			List<Integer> departures = new ArrayList<>();
			JsonNode dptBins = node.get("dptBins");
			for (JsonNode dpt : dptBins) {
				departures.add(dpt.asInt());
			}

			return new RoundTrip<>(index, scenarioNodes, departures);
		}

	}

	public static void writeToFile(RoundTrip<? extends Node> roundTrip, String fileName)
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		SimpleModule module = new SimpleModule();
		module.addSerializer((Class) RoundTrip.class, new Serializer());
		mapper.registerModule(module);
		mapper.writeValue(new File(fileName), roundTrip);
	}

	public static <N extends Node> RoundTrip<N> readFromFile(Scenario<N> scenario, String fileName)
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(RoundTrip.class, new Deserializer(scenario));
		mapper.registerModule(module);
		ObjectReader reader = mapper.readerFor(RoundTrip.class);
		JsonParser parser = mapper.getFactory().createParser(new File(fileName));
		RoundTrip<N> result = reader.readValue(parser);
		parser.close();
		return result;
	}

	static void testNode() throws JsonGenerationException, JsonMappingException, IOException {
		Scenario<Node> scenario = new Scenario<>();
		Node home = scenario.addNode(new Node("home"));
		Node work = scenario.addNode(new Node("work"));
		RoundTrip<Node> roundTrip = new RoundTrip<>(0, Arrays.asList(home, work), Arrays.asList(7, 18));
		writeToFile(roundTrip, "test.json");
		roundTrip = null;
		roundTrip = readFromFile(scenario, "test.json");
		System.out.println(roundTrip);
	}

	static void testNodeWithCoords() throws JsonGenerationException, JsonMappingException, IOException {
		Scenario<NodeWithCoords> scenario = new Scenario<>();
		NodeWithCoords home = scenario.addNode(new NodeWithCoords("home", 1, 2, 0));
		NodeWithCoords work = scenario.addNode(new NodeWithCoords("work", 3, 4, 0));
		RoundTrip<NodeWithCoords> roundTrip = new RoundTrip<>(0, Arrays.asList(home, work), Arrays.asList(7, 18));
		writeToFile(roundTrip, "test.json");
		roundTrip = null;
		roundTrip = readFromFile(scenario, "test.json");
		System.out.println(roundTrip);
		for (NodeWithCoords node : roundTrip.getNodesView()) {
			if (node != scenario.getNode(node.getName())) {
				System.out.println("Roundtrip node instance if " + node.getName() + " is not referenced in scenario.");
			}
		}

	}

	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
//		testNode();
		testNodeWithCoords();
	}

}
