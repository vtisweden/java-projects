/**
 * se.vti.samgods
 * 
 * Copyright (C) 2024, 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.transportation.consolidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

import se.vti.samgods.common.NetworkAndFleetData;
import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.samgods.common.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.utils.misc.Units;

/**
 * 
 * @author GunnarF
 *
 */
@JsonSerialize(using = ConsolidationUnit.Serializer.class)
public class ConsolidationUnit {

	// -------------------- CONSTANTS --------------------

	// "Poison pill" for multithreaded routing.
	public static final ConsolidationUnit TERMINATE = new ConsolidationUnit(null, null, null, null);

	// -------------------- MEMBERS --------------------

	public final OD od;
	public final SamgodsConstants.Commodity commodity;
	public final SamgodsConstants.TransportMode samgodsMode;
	public final Boolean isContainer;

	// --------------------CONSTRUCTION --------------------

	/* package for testing */ ConsolidationUnit(OD od, SamgodsConstants.Commodity commodity,
			SamgodsConstants.TransportMode mode, Boolean isContainer) {
		this.od = od;
		this.commodity = commodity;
		this.samgodsMode = mode;
		this.isContainer = isContainer;
	}

	public static List<ConsolidationUnit> createUnrouted(TransportEpisode episode) {
		if (episode.getSegmentODs() == null) {
			return Collections.emptyList();
		} else {
			return episode.getSegmentODs().stream().map(
					od -> new ConsolidationUnit(od, episode.getCommodity(), episode.getMode(), episode.isContainer()))
					.collect(Collectors.toList());
		}
	}

	public ConsolidationUnit cloneWithoutRoutes() {
		return new ConsolidationUnit(this.od, this.commodity, this.samgodsMode, this.isContainer);
	}

	// -------------------- OVERRIDING Object --------------------

	private List<Object> createAsList() {
		return Arrays.asList(this.od, this.commodity, this.samgodsMode, this.isContainer);
	}

	@Override
	public int hashCode() {
		return this.createAsList().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (other instanceof ConsolidationUnit) {
			return this.createAsList().equals(((ConsolidationUnit) other).createAsList());
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		final List<String> content = new LinkedList<>();
		content.add("commodity=" + this.commodity);
		content.add("isContainer=" + this.isContainer);
		content.add("mode=" + this.samgodsMode);
		content.add("od=" + this.od);
		return this.getClass().getSimpleName() + "[" + content.stream().collect(Collectors.joining(",")) + "]";
	}

	// -------------------- ROUTE MANAGEMENT --------------------

	/*
	 * Even the Set<VehicleType> key must be concurrent. Implementation ensures that
	 * there is at most one key containing any vehicle type.
	 */
	public final ConcurrentMap<Set<VehicleType>, CopyOnWriteArrayList<Id<Link>>> vehicleType2route = new ConcurrentHashMap<>();

	public List<Id<Link>> getRoute(VehicleType vehicleType) {
		return this.vehicleType2route.entrySet().stream().filter(e -> e.getKey().contains(vehicleType)).findFirst()
				.map(e -> e.getValue()).orElseGet(() -> null);
	}

	public void removeRoute(VehicleType vehicleType) {
		var entry = this.vehicleType2route.entrySet().stream().filter(e -> e.getKey().contains(vehicleType)).findFirst()
				.orElseGet(() -> null);
		if (entry != null) {
			this.vehicleType2route.remove(entry.getKey());
			if (entry.getKey().size() > 1) {
				entry.getKey().remove(vehicleType);
				this.vehicleType2route.put(entry.getKey(), entry.getValue());
			}
		}
	}

	public void setRouteFromLinkIds(VehicleType vehicleType, List<Id<Link>> routeIds) {
		if (routeIds == null) {
			throw new IllegalArgumentException("Route must not be null.");
		}
		this.removeRoute(vehicleType);
		var entry = this.vehicleType2route.entrySet().stream().filter(e -> e.getValue().equals(routeIds)).findFirst()
				.orElseGet(() -> null);
		if (entry == null) {
			Set<VehicleType> key = ConcurrentHashMap.newKeySet();
			key.add(vehicleType);
			this.vehicleType2route.put(key, new CopyOnWriteArrayList<>(routeIds));
		} else {
			this.vehicleType2route.remove(entry.getKey());
			entry.getKey().add(vehicleType);
			this.vehicleType2route.put(entry.getKey(), entry.getValue());
		}
	}

	public void setRouteFromLinks(VehicleType vehicleType, List<Link> links) {
		if (links == null) {
			throw new IllegalArgumentException();
		} else {
			this.setRouteFromLinkIds(vehicleType, links.stream().map(l -> l.getId()).toList());
		}
	}

	public SummaryStatistics computeLengthStats_km(NetworkAndFleetData networkData) {
		SummaryStatistics result = new SummaryStatistics();
		for (var linkIds : this.vehicleType2route.values()) {
			result.addValue(Units.KM_PER_M
					* linkIds.stream().mapToDouble(id -> networkData.getLinks().get(id).getLength()).sum());
		}
		return result;
	}

	/* package for testing */
	int distinctRoutes() {
		return this.vehicleType2route.size();
	}
	
	// -------------------- Json Serializer --------------------

	public static class Serializer extends JsonSerializer<ConsolidationUnit> {

		Serializer() {
		}

		@Override
		public void serialize(ConsolidationUnit consolidationUnit, JsonGenerator gen, SerializerProvider serializers)
				throws IOException {
			gen.writeStartObject();
			gen.writeStringField("origin", consolidationUnit.od.origin.toString());
			gen.writeStringField("destination", consolidationUnit.od.destination.toString());
			gen.writeStringField("commodity", consolidationUnit.commodity.toString());
			gen.writeStringField("mode", consolidationUnit.samgodsMode.toString());
			gen.writeStringField("isContainer", consolidationUnit.isContainer.toString());

			gen.writeFieldName("routes");
			gen.writeStartArray();
			for (Map.Entry<Set<VehicleType>, ? extends List<Id<Link>>> entry : consolidationUnit.vehicleType2route
					.entrySet()) {
				gen.writeStartObject();
				gen.writeFieldName("vehicleTypes");
				gen.writeStartArray();
				for (VehicleType vehicleType : entry.getKey()) {
					gen.writeString(vehicleType.getId().toString());
				}
				gen.writeEndArray();
				gen.writeFieldName("links");
				gen.writeStartArray();
				for (Id<Link> linkId : entry.getValue()) {
					gen.writeString(linkId.toString());
				}
				gen.writeEndArray();
				gen.writeEndObject();
			}
			gen.writeEndArray();

			gen.writeEndObject();
		}
	}

	// -------------------- Json Deserializer --------------------

	public static class Deserializer extends JsonDeserializer<ConsolidationUnit> {

		private final Vehicles vehicles;

		public Deserializer(Vehicles vehicles) {
			this.vehicles = vehicles;
		}

		@Override
		public ConsolidationUnit deserialize(JsonParser p, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			ObjectCodec codec = p.getCodec();
			JsonNode node = codec.readTree(p);

			Id<Node> origin = Id.createNodeId(((TextNode) node.get("origin")).asText());
			Id<Node> destination = Id.createNodeId(((TextNode) node.get("destination")).asText());
			OD od = new OD(origin, destination);
			Commodity commodity = Commodity.valueOf(((TextNode) node.get("commodity")).asText());
			TransportMode mode = TransportMode.valueOf(((TextNode) node.get("mode")).asText());
			boolean isContainer = Boolean.parseBoolean(((TextNode) node.get("isContainer")).asText());

			final ConsolidationUnit result = new ConsolidationUnit(od, commodity, mode, isContainer);

			ArrayNode routesNode = (ArrayNode) node.get("routes");
			for (JsonNode routeNode : routesNode) {

				ArrayNode vehiclesNode = (ArrayNode) routeNode.get("vehicleTypes");
				final Set<VehicleType> vehicleTypes = ConcurrentHashMap.newKeySet(vehiclesNode.size());
				for (JsonNode vehicleNode : vehiclesNode) {
					vehicleTypes.add(
							this.vehicles.getVehicleTypes().get(Id.create(vehicleNode.asText(), VehicleType.class)));
				}

				ArrayNode linksNode = (ArrayNode) routeNode.get("links");
				final List<Id<Link>> linkIds = new ArrayList<>(linksNode.size());
				for (JsonNode linkNode : linksNode) {
					linkIds.add(Id.createLinkId(linkNode.asText()));
				}
				result.vehicleType2route.put(vehicleTypes, new CopyOnWriteArrayList<>(linkIds));
			}

			return result;
		}
	}
}