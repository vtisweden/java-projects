/**
 * se.vti.samgods.external.gis
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
package se.vti.samgods.external.gis;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor.FleetAssignment;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkFlows {

	private final Map<Id<Link>, Map<Commodity, Double>> id2commodity2flow_ton = new LinkedHashMap<>();

	public NetworkFlows() {
	}

	public NetworkFlows add(Map<ConsolidationUnit, FleetAssignment> consolidationUnit2assignment) {
		for (Map.Entry<ConsolidationUnit, FleetAssignment> entry : consolidationUnit2assignment.entrySet()) {
			final ConsolidationUnit consolidationUnit = entry.getKey();
			final FleetAssignment fleetAssignment = entry.getValue();
			for (Id<Link> linkId : consolidationUnit.getRoute(fleetAssignment.vehicleType)) {
				this.id2commodity2flow_ton.computeIfAbsent(linkId, id -> new LinkedHashMap<>(Commodity.values().length))
						.compute(consolidationUnit.commodity, (c, q) -> q == null ? fleetAssignment.annualDemand_ton
								: q + fleetAssignment.annualDemand_ton);
			}
		}
		return this;
	}

	public void writeToFile(String fileName) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		try {
			mapper.writeValue(new File(fileName), this.id2commodity2flow_ton);
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
}
