/**
 * se.vti.roundtrips.multiple
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
package se.vti.roundtrips.multiple;

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

import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripJsonIO;

/**
 * Loads a MultiRoundTrip without any summaries. Add summary objects and call
 * MultiRoundTrip.recomputeSummaries() if needed. Re-simulate round trips if
 * necessary.
 * 
 * 
 * @author GunnarF
 *
 */
public class MultiRoundTripJsonIO {

	public static class Serializer extends JsonSerializer<MultiRoundTrip<? extends Node>> {
		@Override
		public void serialize(MultiRoundTrip<? extends Node> value, JsonGenerator gen,
				SerializerProvider serializers) throws IOException {
			gen.writeStartArray();
			for (RoundTrip<? extends Node> roundTrip : value) {
				gen.writeObject(roundTrip);
			}
			gen.writeEndArray();
		}
	}

	public static void writeToFile(MultiRoundTrip<? extends Node> multiRoundTrip, String fileName)
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		SimpleModule module = new SimpleModule();
		module.addSerializer((Class) RoundTrip.class, new RoundTripJsonIO.Serializer());
		module.addSerializer((Class) MultiRoundTrip.class, new MultiRoundTripJsonIO.Serializer());
		mapper.registerModule(module);
		mapper.writeValue(new File(fileName), multiRoundTrip);
	}

	public static class Deserializer extends JsonDeserializer<MultiRoundTrip<Node>> {
		@Override
		public MultiRoundTrip<Node> deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			JsonNode root = p.getCodec().readTree(p);
			List<RoundTrip<Node>> roundTrips = new ArrayList<>();
			for (JsonNode roundTripNode : root) {
				RoundTrip<Node> roundTrip = p.getCodec().treeToValue(roundTripNode, RoundTrip.class);
				roundTrips.add(roundTrip);
			}
			MultiRoundTrip<Node> result = new MultiRoundTrip<>(roundTrips.size());
			for (int i = 0; i < roundTrips.size(); i++) {
				result.setRoundTripAndUpdateSummaries(i, roundTrips.get(i));
			}
			return result;
		}
	}

	public static MultiRoundTrip<Node> readFromFile(Scenario<Node> scenario, String fileName)
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(RoundTrip.class, new RoundTripJsonIO.Deserializer(scenario));
		module.addDeserializer(MultiRoundTrip.class, new Deserializer());
		mapper.registerModule(module);
		ObjectReader reader = mapper.readerFor(MultiRoundTrip.class);
		JsonParser parser = mapper.getFactory().createParser(new File(fileName));
		MultiRoundTrip<Node> result = reader.readValue(parser);
		parser.close();
		return result;
	}

	public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
		Scenario<Node> scenario = new Scenario<>();
		Node home = scenario.addNode(new Node("home"));
		Node work = scenario.addNode(new Node("work"));
		Node school = scenario.addNode(new Node("school"));
		MultiRoundTrip<Node> multiRoundTrip = new MultiRoundTrip<>(2);
		multiRoundTrip.setRoundTripAndUpdateSummaries(0,
				new RoundTrip<>(0, Arrays.asList(home, work), Arrays.asList(7, 18)));
		multiRoundTrip.setRoundTripAndUpdateSummaries(1,
				new RoundTrip<>(1, Arrays.asList(home, school), Arrays.asList(9, 13)));
		writeToFile(multiRoundTrip, "test.json");
		multiRoundTrip = null;
		multiRoundTrip = readFromFile(scenario, "test.json");
		System.out.println(multiRoundTrip);
	}

}
