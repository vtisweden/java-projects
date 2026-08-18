/**
 * se.vti.samgods.transportation.trajectories
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
package se.vti.samgods.external.ntmcalc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.VehicleType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import se.vti.samgods.common.NetworkAndFleetData;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.network.SamgodsLinkAttributes;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor.FleetAssignment;
import se.vti.utils.misc.Units;

/**
 * 
 * @author GunnarF
 *
 */

public class HalfLoopAssignment2NTMCalcWriter
		extends JsonSerializer<HalfLoopAssignment2NTMCalcWriter.RoadHalfLoopAssignment> {

	class RoadHalfLoopAssignment {

		final ConsolidationUnit consolidationUnit;
		final FleetAssignment fleetAssignment;

		RoadHalfLoopAssignment(ConsolidationUnit consolidationUnit, FleetAssignment fleetAssignment) {
			this.consolidationUnit = consolidationUnit;
			this.fleetAssignment = fleetAssignment;
		}

		List<Id<Link>> getAssignedRoute() {
			for (Map.Entry<Set<VehicleType>, ? extends List<Id<Link>>> e : this.consolidationUnit.vehicleType2route
					.entrySet()) {
				if (e.getKey().contains(fleetAssignment.vehicleType)) {
					return e.getValue();
				}
			}
			return null;
		}

	}

	private final NetworkAndFleetData networkAndFleetData;

	public HalfLoopAssignment2NTMCalcWriter(NetworkAndFleetData networkAndFleetData) {
		this.networkAndFleetData = networkAndFleetData;
	}

	@Override
	public void serialize(RoadHalfLoopAssignment halfLoopAssignment, JsonGenerator gen, SerializerProvider serializers)
			throws IOException {

		gen.writeStartObject();

		gen.writeStringField("vehicleType", halfLoopAssignment.fleetAssignment.vehicleType.getId().toString());
		gen.writeNumberField("number_veh_day", halfLoopAssignment.fleetAssignment.averageVehiclePassages_1_day);
		gen.writeNumberField("load_ton_veh", halfLoopAssignment.fleetAssignment.payload_ton);

		gen.writeFieldName("links");
		gen.writeStartArray();

		for (Id<Link> linkId : halfLoopAssignment.getAssignedRoute()) {
			final Link link = this.networkAndFleetData.getLinks().get(linkId);
			final SamgodsLinkAttributes linkAttrs = (SamgodsLinkAttributes) link.getAttributes()
					.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME);
			gen.writeStartObject();
			gen.writeStringField("linkId", linkId.toString());
			gen.writeStringField("domestic", linkAttrs.isDomestic ? "yes" : "no");
			gen.writeNumberField("length_m", link.getLength());
			gen.writeNumberField("maxSpeed_km_h", Math.round(link.getFreespeed() * Units.KM_H_PER_M_S));
			if (linkAttrs.isFerryLink()) {
				assert (SamgodsConstants.TransportMode.Road.equals(halfLoopAssignment.consolidationUnit.samgodsMode)
						|| SamgodsConstants.TransportMode.Rail
								.equals(halfLoopAssignment.consolidationUnit.samgodsMode));
				LogManager.getLogger(this.getClass()).info("Found ferry link: " + linkId); // TODO for testing
				gen.writeStringField("mode", SamgodsConstants.TransportMode.Ferry.toString());
				gen.writeNumberField("vesselDWT", 5000.0); // TODO uniform assumption for both road and rail
			} else {
				gen.writeStringField("mode", halfLoopAssignment.consolidationUnit.samgodsMode.toString());
			}

			gen.writeEndObject();
		}
		gen.writeEndArray();

		gen.writeEndObject();
	}

	public void writeToFile(String fileNamePrefix,
			Map<ConsolidationUnit, FleetAssignment> consolidationUnits2fleetAssignments) {

		for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
			final List<RoadHalfLoopAssignment> dataForCommodity = consolidationUnits2fleetAssignments.entrySet()
					.stream().filter(e -> commodity.equals(e.getKey().commodity))
					.map(e -> new RoadHalfLoopAssignment(e.getKey(), e.getValue())).toList();

			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				final List<RoadHalfLoopAssignment> dataToWrite = dataForCommodity.stream()
						.filter(e -> mode.equals(e.consolidationUnit.samgodsMode)).toList();

				if (dataToWrite.size() > 0) {
					final ObjectMapper mapper = new ObjectMapper();
					mapper.enable(SerializationFeature.INDENT_OUTPUT);
					mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
					final SimpleModule module = new SimpleModule();
					module.addSerializer(RoadHalfLoopAssignment.class, this);
					mapper.registerModule(module);
					try {
						mapper.writeValue(new File(fileNamePrefix + "_" + commodity + "_" + mode + ".json"),
								dataToWrite);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}
}
