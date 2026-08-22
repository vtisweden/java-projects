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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants.CommodityMode;

/**
 * Exploratory.
 * 
 * @author GunnarF
 */
public class LoopManager {

	// -------------------- MEMBERS --------------------

	private final Map<CommodityMode, Set<Loop>> commodityMode2Loops = new LinkedHashMap<>();

	private Map<ConsolidationUnit, Set<Loop>> consolidationUnit2Loops = null;

	private ConsolidationUnitManager consolidationUnitManager = null;

	// -------------------- CONSTRUCTION --------------------

	public LoopManager() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setConsolidationUnitManager(ConsolidationUnitManager consolidationUnitManager) {
		this.consolidationUnitManager = consolidationUnitManager;
	}

	public void addLoops(Set<Loop> loops) {
		for (Loop loop : loops) {
			Set<Loop> storedLoops = this.commodityMode2Loops.computeIfAbsent(loop.getCommodityMode(),
					l -> new LinkedHashSet<>());
			boolean isNew = true;
			for (Loop existingLoop : storedLoops) {
				if (LoopUtils.instance.equalUpToShift(existingLoop.getMATSimNodeIdsView(),
						loop.getMATSimNodeIdsView())) {
					isNew = false;
					System.err.println("Rejecting loop because of topological redundancy.");
					break;
				}
			}

			if (isNew) {
				storedLoops.add(loop);
			}
		}
	}

	public void computeConsolidationUnits() {
		for (var entry : this.commodityMode2Loops.entrySet()) {
			var commodity = entry.getKey().commodity();
			var mode = entry.getKey().samgodsMode();
			for (var loop : entry.getValue()) {
				var nodeIds = loop.getMATSimNodeIdsView();
				for (int i = 0; i < loop.size(); i++) {
					OD od = new OD(nodeIds.get(i), nodeIds.get((i + 1) % loop.size()));
					this.consolidationUnitManager
							.registerAndReturnRepresentative(new ConsolidationUnit(od, commodity, mode, true));
					this.consolidationUnitManager
							.registerAndReturnRepresentative(new ConsolidationUnit(od, commodity, mode, false));
				}
			}
		}
	}

	public Collection<ConsolidationUnit> getAllConsolidationUnits() {
		return this.consolidationUnitManager.getAllRepresentativeConsolidationUnits();
	}

	public Set<Loop> getLoops(ConsolidationUnit consolidationUnit) {
		return this.consolidationUnit2Loops.get(consolidationUnit);
	}
	
	public void postprocessLoops() {

		this.consolidationUnit2Loops = new LinkedHashMap<>();

		for (var entry : this.commodityMode2Loops.entrySet()) {
			var commodityMode = entry.getKey();

			for (Loop loop : entry.getValue()) {
				Set<VehicleType> feasibleContainerVehicleTypes = null;
				Set<VehicleType> feasibleNoContainerVehicleTypes = null;

				List<Set<ConsolidationUnit>> consolidationUnitsPerContainerSegment = new ArrayList<>(loop.size());
				List<Set<ConsolidationUnit>> consolidationUnitsPerNoContainerSegment = new ArrayList<>(loop.size());

				for (int i = 0; i < loop.size(); i++) {
					Id<Node> fromNodeId = loop.getMATSimNodeIdsView().get(i);
					Id<Node> toNodeId = loop.getMATSimNodeIdsView().get((i + 1) % loop.size());
					OD od = new OD(fromNodeId, toNodeId);

					var consolidationUnits = this.consolidationUnitManager.getConsolidationUnits(commodityMode, od);
					Set<ConsolidationUnit> containerConsolidationUnits = new LinkedHashSet<>();
					Set<ConsolidationUnit> noContainerConsolidationUnits = new LinkedHashSet<>();
					consolidationUnitsPerContainerSegment.add(containerConsolidationUnits);
					consolidationUnitsPerNoContainerSegment.add(noContainerConsolidationUnits);

					if ((consolidationUnits == null) || (consolidationUnits.size() == 0)) {
						feasibleContainerVehicleTypes = new LinkedHashSet<>(0);
						feasibleNoContainerVehicleTypes = new LinkedHashSet<>(0);
						System.err.println("Rejecting loop because of lacking consolidation units.");
						break;
					}

					for (ConsolidationUnit cu : consolidationUnits) {
						Set<VehicleType> unitVehicleTypes = new LinkedHashSet<>();
						for (var vehicleTypes : cu.vehicleType2route.keySet()) {
							unitVehicleTypes.addAll(vehicleTypes);
						}

						if (cu.isContainer) {
							containerConsolidationUnits.add(cu);
							if (feasibleContainerVehicleTypes == null) {
								feasibleContainerVehicleTypes = new LinkedHashSet<>(unitVehicleTypes);
							} else {
								feasibleContainerVehicleTypes.retainAll(unitVehicleTypes);
							}
						} else {
							noContainerConsolidationUnits.add(cu);
							if (feasibleNoContainerVehicleTypes == null) {
								feasibleNoContainerVehicleTypes = new LinkedHashSet<>(unitVehicleTypes);
							} else {
								feasibleNoContainerVehicleTypes.retainAll(unitVehicleTypes);
							}
						}
					}
				}

				var feasibleVehicleTypes = new LinkedHashSet<VehicleType>();
				if ((feasibleContainerVehicleTypes != null)) {
					feasibleVehicleTypes.addAll(feasibleContainerVehicleTypes);
				}
				if (feasibleNoContainerVehicleTypes != null) {
					feasibleVehicleTypes.addAll(feasibleNoContainerVehicleTypes);
				}
				if (feasibleVehicleTypes.isEmpty()) {
					System.err.println("Rejecting loop because of lacking vehicle types.");
				} else {

					loop.setFeasibleVehicleTypes(feasibleVehicleTypes);

					this.alignConsolidationUnitsPerSegmentWithLoop(consolidationUnitsPerContainerSegment, loop);
					loop.setConsolidationUnitsPerContainerSegment(consolidationUnitsPerContainerSegment);

					this.alignConsolidationUnitsPerSegmentWithLoop(consolidationUnitsPerNoContainerSegment, loop);
					loop.setConsolidationUnitsPerNoContainerSegment(consolidationUnitsPerNoContainerSegment);
				}
			}
		}
	}

	private void alignConsolidationUnitsPerSegmentWithLoop(List<Set<ConsolidationUnit>> consolidationUnitsPerSegment,
			Loop loop) {
		for (var consolidationUnits : consolidationUnitsPerSegment) {
			consolidationUnits.removeIf(cu -> cu.vehicleType2route.keySet().stream().flatMap(Set::stream)
					.noneMatch(loop.getFeasibleVehicleTypesView()::contains));
			consolidationUnits.stream().forEach(
					cu -> this.consolidationUnit2Loops.computeIfAbsent(cu, cu2 -> new LinkedHashSet<>()).add(loop));
		}
	}

	// for testing
	public void printStats() {
		int numberOfLoops = 0;
		int numberOfFeasibleLoops = 0;
		int numberOfFeasibleVehicleTypes = 0;
		Map<Set<VehicleType>, Integer> vehicleTypes2count = new LinkedHashMap<>();
		for (var loops : this.commodityMode2Loops.values()) {
			numberOfLoops += loops.size();
			for (var loop : loops) {
				if (loop.getFeasibleVehicleTypesView() != null && loop.getFeasibleVehicleTypesView().size() > 0
						&& (loop.isContainerVehicleCompatible() || loop.isNoContainerVehicleCompatible())) {
					System.out.println("Loop with " + loop.getFeasibleVehicleTypesView().size()
							+ " feasible vehicle types: " + loop.getFeasibleVehicleTypesView().stream()
									.map(t -> t.getId().toString()).collect(Collectors.joining(",")));
					if (loop.getFeasibleVehicleTypesView().size() > 0) {
						numberOfFeasibleLoops++;
					}
					numberOfFeasibleVehicleTypes += loop.getFeasibleVehicleTypesView().size();
					vehicleTypes2count.compute(loop.getFeasibleVehicleTypesView(),
							(vt, ct) -> ct == null ? ct = 1 : (ct + 1));
				}
			}
		}
		System.out.println("Total number of loops: " + numberOfLoops);
		System.out.println("Number of feasible loops: " + numberOfFeasibleLoops);
		System.out.println("Total number vehicle-type/loop pairs:" + numberOfFeasibleVehicleTypes);
		for (var entry : vehicleTypes2count.entrySet()) {
			System.out.println(entry.getKey().stream().map(vt -> vt.getId().toString()).collect(Collectors.joining(","))
					+ " : " + entry.getValue());
		}
	}
}
