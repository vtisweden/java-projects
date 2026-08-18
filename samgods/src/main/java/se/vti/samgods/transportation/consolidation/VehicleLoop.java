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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants.CommodityModeContainer;

/**
 * @author GunnarF
 */
public class VehicleLoop {

	private final CommodityModeContainer commodityModeContainer;

	private final List<Id<Node>> matsimNodeIds;

	private final Set<ConsolidationUnit> consolidationUnits = new LinkedHashSet<>();

	private final Set<VehicleType> feasibleVehicleTypes = new LinkedHashSet<>();

	VehicleLoop(CommodityModeContainer commodityModeContainer, List<Id<Node>> matsimNodesIds,
			Set<VehicleType> allVehicleTypes) {
		this.commodityModeContainer = commodityModeContainer;
		this.matsimNodeIds = Collections.unmodifiableList(matsimNodesIds);
		this.feasibleVehicleTypes.addAll(allVehicleTypes);
	}

	public CommodityModeContainer getCommodityModeContainer() {
		return this.commodityModeContainer;
	}

	public List<Id<Node>> getMATSimNodeIdsView() {
		return this.matsimNodeIds;
	}

	public boolean containsOD(OD od) {
		return (this.matsimNodeIds.contains(od.origin) && this.matsimNodeIds.contains(od.destination));
	}

	public void addConsolidationUnit(ConsolidationUnit consolidationUnit) {
		assert (this.commodityModeContainer.equals(new CommodityModeContainer(consolidationUnit)));
		this.consolidationUnits.add(consolidationUnit);
		this.feasibleVehicleTypes.retainAll(extractRoutedVehicleTypes(consolidationUnit));
	}

	// TODO wrong place
	private Set<VehicleType> extractRoutedVehicleTypes(ConsolidationUnit consolidationUnit) {
		Set<VehicleType> result = new LinkedHashSet<>();
		for (var vehicleTypes : consolidationUnit.vehicleType2route.keySet()) {
			result.addAll(vehicleTypes);
		}
		return result;
	}
}
