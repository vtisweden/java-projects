/**
 * se.vti.samgods.logistics.choicemodel
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
package se.vti.samgods.logistics.choice;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.matsim.vehicles.VehicleType;

import se.vti.samgods.common.NetworkAndFleetData;
import se.vti.samgods.common.NetworkAndFleetDataProvider;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor.FleetAssignment;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.costs.TransportCostCalculator;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class LogisticChoiceDataProvider {

	// -------------------- CONSTANTS --------------------

	private final double initialTransportEfficiency = 0.7;

	private final TransportCostCalculator realizedInVehicleCost = new TransportCostCalculator();

	private final NetworkAndFleetData internalNetworkAndFleetData;

	// -------------------- CONSTRUCTION --------------------

	public LogisticChoiceDataProvider(NetworkAndFleetDataProvider networkAndFleetDataProvider) {
		this.internalNetworkAndFleetData = networkAndFleetDataProvider.createDataInstance();
	}

	public LogisticChoiceData createLogisticChoiceData() {
		return new LogisticChoiceData(this);
	}

	// -------------------- THREAD SAFE CONSOLIDATION COSTS --------------------

	private ConcurrentMap<ConsolidationUnit, FleetAssignment> consolidationUnit2fleetAssignment = null;

	public void update(ConcurrentMap<ConsolidationUnit, FleetAssignment> consolidationUnit2fleetAssignment) {
		this.consolidationUnit2fleetAssignment = consolidationUnit2fleetAssignment;
	}

	private final ConcurrentMap<Boolean, ConcurrentMap<Boolean, ConcurrentMap<ConsolidationUnit, DetailedTransportCost>>> load2unload2consolidationUnit2transportUnitCost_1_ton = new ConcurrentHashMap<>();

	private synchronized DetailedTransportCost createTransportUnitCost_1_ton(ConsolidationUnit consolidationUnit,
			boolean load, boolean unload) {

		/*
		 * Identify (possibly randomly if no other data available) the used vehicle
		 * type.
		 */
		final VehicleType vehicleType;
		if (this.consolidationUnit2fleetAssignment != null) {
			vehicleType = this.consolidationUnit2fleetAssignment.get(consolidationUnit).vehicleType;
		} else {
			final List<VehicleType> availableTypes = new ArrayList<>(consolidationUnit.vehicleType2route.keySet()
					.stream().flatMap(types -> types.stream()).collect(Collectors.toSet()));
			vehicleType = availableTypes.get(new Random().nextInt(availableTypes.size()));
		}
		final SamgodsVehicleAttributes vehicleAttributes = (SamgodsVehicleAttributes) vehicleType.getAttributes()
				.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME);

		/*
		 * Identify or estimate payload and initialize cost building.
		 */
		final double payload_ton;
		if (this.consolidationUnit2fleetAssignment != null) {
			payload_ton = this.consolidationUnit2fleetAssignment.get(consolidationUnit).payload_ton;
		} else {
			payload_ton = this.initialTransportEfficiency * vehicleAttributes.capacity_ton;
		}
		final DetailedTransportCost.Builder costBuilder = new DetailedTransportCost.Builder().setToAllZeros()
				.addAmount_ton(payload_ton);

		/*
		 * Compute in-vehicle cost.
		 */
		if (this.consolidationUnit2fleetAssignment != null) {
			costBuilder.add(this.realizedInVehicleCost.computeInVehicleCost(vehicleType, vehicleAttributes, payload_ton,
					consolidationUnit, this.internalNetworkAndFleetData.getLinkId2unitCost(vehicleType),
					this.internalNetworkAndFleetData.getFerryLinkIds()), false);
		} else {
			costBuilder.add(this.realizedInVehicleCost.computeInVehicleCost(vehicleType, vehicleAttributes, payload_ton,
					consolidationUnit, this.internalNetworkAndFleetData.getLinkId2unitCost(vehicleType),
					this.internalNetworkAndFleetData.getFerryLinkIds()), false);
		}

		/*
		 * Add loading/unloading/transfer costs.
		 */
		if (load) {
			costBuilder.addLoadingDuration_h(vehicleAttributes.loadTime_h.get(consolidationUnit.commodity));
			costBuilder.addLoadingCost(vehicleAttributes.loadCost_1_ton.get(consolidationUnit.commodity) * payload_ton);
		}
		if (unload) {
			costBuilder.addUnloadingDuration_h(vehicleAttributes.loadTime_h.get(consolidationUnit.commodity));
			costBuilder
					.addUnloadingCost(vehicleAttributes.loadCost_1_ton.get(consolidationUnit.commodity) * payload_ton);
		}
		final int transfers = (load ? 0 : 1) + (unload ? 0 : 1);
		if (transfers > 0) {
			costBuilder.addTransferDuration_h(
					transfers * vehicleAttributes.transferTime_h.get(consolidationUnit.commodity));
			costBuilder.addTransferCost(
					transfers * vehicleAttributes.transferCost_1_ton.get(consolidationUnit.commodity) * payload_ton);
		}

		return costBuilder.build().createUnitCost_1_ton();
	}

	public DetailedTransportCost getTransportUnitCost_1_ton(ConsolidationUnit consolidationUnit, boolean load,
			boolean unload) {
		return this.load2unload2consolidationUnit2transportUnitCost_1_ton
				.computeIfAbsent(load, l -> new ConcurrentHashMap<>())
				.computeIfAbsent(unload, u -> new ConcurrentHashMap<>())
				.computeIfAbsent(consolidationUnit, cu -> this.createTransportUnitCost_1_ton(cu, load, unload));
	}

	// --------------- THREAD SAFE EPISODE UNIT COST ACCESS ---------------

	private final ConcurrentMap<TransportEpisode, DetailedTransportCost> episode2unitCost_1_ton = new ConcurrentHashMap<>();

	ConcurrentMap<TransportEpisode, DetailedTransportCost> getEpisode2unitCost_1_ton() {
		return this.episode2unitCost_1_ton;
	}
}
