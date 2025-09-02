/**
 * se.vti.samgods.transportation.costs
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
package se.vti.samgods.transportation.costs;

import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.common.NetworkAndFleetData;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportCostCalculator {

	public TransportCostCalculator() {
	}

	public DetailedTransportCost computeInVehicleCost(VehicleType vehicleType, SamgodsVehicleAttributes vehicleAttrs,
			double payload_ton, ConsolidationUnit consolidationUnit,
			Map<Id<Link>, BasicTransportCost> link2unitCost, Set<Id<Link>> ferryLinks) {
		final DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().setToAllZeros()
				.addAmount_ton(payload_ton);
		for (Id<Link> linkId : consolidationUnit.getRoute(vehicleType)) {
			BasicTransportCost unitCost = link2unitCost.get(linkId);
			builder.addMoveDuration_h(unitCost.duration_h);
			builder.addDistance_km(unitCost.length_km);
			if (ferryLinks.contains(linkId)) {
				builder.addMoveCost(unitCost.duration_h * vehicleAttrs.onFerryCost_1_h);
				builder.addMoveCost(unitCost.length_km * vehicleAttrs.onFerryCost_1_km);
			} else {
				builder.addMoveCost(unitCost.duration_h * vehicleAttrs.cost_1_h);
				builder.addMoveCost(unitCost.length_km * vehicleAttrs.cost_1_km);
			}
		}
		return builder.build();
	}

	public DetailedTransportCost computeInVehicleCost(VehicleType vehicleType, double payload_ton,
			ConsolidationUnit consolidationUnit, NetworkAndFleetData networkAndFleetData) {
		return this.computeInVehicleCost(vehicleType, networkAndFleetData.getVehicleType2attributes().get(vehicleType),
				payload_ton, consolidationUnit, networkAndFleetData.getLinkId2unitCost(vehicleType),
				networkAndFleetData.getFerryLinkIds());
	}

	public DetailedTransportCost computeLoadUnloadTransferCost() {
		throw new RuntimeException("TODO");
	}

}
