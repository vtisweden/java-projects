/**
 * se.vti.samgods.transportation.consolidation
 * 
 * Copyright (C) 2024-2026 by Gunnar Flötteröd (VTI, LiU).
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants.CommodityMode;
import se.vti.samgods.transportation.costs.BasicTransportCost;

/**
 * Exploratory.
 * 
 * @author GunnarF
 */
class VehicleLoopManager {

	// -------------------- MEMBERS --------------------

	record CommodityModeOD(CommodityMode commodityMode, OD od) {
		CommodityModeOD(ConsolidationUnit consolidationUnit) {
			this(new CommodityMode(consolidationUnit), consolidationUnit.od);
		}
	}

	// -------------------- MEMBERS --------------------

	private final Map<CommodityMode, Set<VehicleLoop>> commodityMode2VehicleLoops = new LinkedHashMap<>();

	private final Map<CommodityModeOD, Set<ConsolidationUnit>> commodityModeOD2ConsolidationUnits = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	VehicleLoopManager(Set<ConsolidationUnit> allConsolidationUnits) {
		for (ConsolidationUnit consolidationUnit : allConsolidationUnits) {
			this.commodityModeOD2ConsolidationUnits
					.computeIfAbsent(new CommodityModeOD(consolidationUnit), cmod -> new LinkedHashSet<>())
					.add(consolidationUnit);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	void addLoop(VehicleLoop loop) {

		// Find out if this loop should be added at all, and do so if adequate.

		Set<VehicleLoop> allLoops = this.commodityMode2VehicleLoops.computeIfAbsent(loop.getCommodityMode(),
				c -> new LinkedHashSet<>());
		for (VehicleLoop existingLoop : allLoops) {
			if (this.equalUpToShift(existingLoop.getMATSimNodeIdsView(), loop.getMATSimNodeIdsView())) {
				return;
			}
		}

		// Find all feasible vehicle types.

		Set<VehicleType> feasibleVehicleTypes = null;
		Id<Node> predecessorNodeId = loop.getMATSimNodeIdsView().getLast();
		for (int i = 0; i < loop.size(); i++) {
			Id<Node> currentNodeId = loop.getMATSimNodeIdsView().get(i);
			OD od = new OD(predecessorNodeId, currentNodeId);
			Set<ConsolidationUnit> consolidationUnits = this.commodityModeOD2ConsolidationUnits
					.get(new CommodityModeOD(loop.getCommodityMode(), od));
			if ((consolidationUnits == null) || (consolidationUnits.size() == 0)) {
				feasibleVehicleTypes = new LinkedHashSet<>(0);
				System.err.println("Rejecting loop because of lacking consolidation units.");
				return;
			} else {
				for (ConsolidationUnit cu : consolidationUnits) {
					for (var vehicleTypes : cu.vehicleType2route.keySet()) {
						if (feasibleVehicleTypes == null) {
							feasibleVehicleTypes = new LinkedHashSet<>(vehicleTypes);
						} else {
							feasibleVehicleTypes.retainAll(vehicleTypes);
						}
					}
				}
			}
			predecessorNodeId = currentNodeId;
		}
		if ((feasibleVehicleTypes == null) || (feasibleVehicleTypes.size() == 0)) {
			System.err.println("Rejecting loop because of lacking vehicle types.");
			return;
		}

		// Loop is topologically new and has at least one vehicle type that applies on
		// all single-step ODs.

		loop.setFeasibleVehicleTypes(feasibleVehicleTypes);
		allLoops.add(loop);
	}

	void printStats() {
		int numberOfLoops = 0;
		int numberOfFeasibleVehicleTypes = 0;
		for (var loops : this.commodityMode2VehicleLoops.values()) {
			numberOfLoops += loops.size();
			for (var loop : loops) {
				numberOfFeasibleVehicleTypes+= loop.getFeasibleVehicleTypesView().size();
			}
		}
		System.out.println("Total number of topological loops: " + numberOfLoops);
		System.out.println("Total number vehicle-type/loop pairs:" + numberOfFeasibleVehicleTypes);
	}

	// -------------------- INTERNALS --------------------

	<N> boolean equalUpToShift(List<N> a, List<N> b) {
		int n = a.size();
		if (n != b.size()) {
			return false;
		}
		if (n == 0 || a.equals(b)) {
			return true;
		}
		for (int start = 1; start < n; start++) {
			int split = n - start;
			if (a.subList(0, split).equals(b.subList(start, n)) && a.subList(split, n).equals(b.subList(0, start))) {
				return true;
			}
		}
		return false;
	}

}
