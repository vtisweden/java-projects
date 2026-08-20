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
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsConstants.CommodityMode;

/**
 * @author GunnarF
 */
public class VehicleLoop {

	private final CommodityMode commodityMode;

	private final List<Id<Node>> matsimNodeIds;

	private final Set<ConsolidationUnit> consolidationUnits = new LinkedHashSet<>();

	private Set<VehicleType> feasibleVehicleTypes = null;

	VehicleLoop(CommodityMode commodityMode, List<Id<Node>> matsimNodesIds) {
		this.commodityMode= commodityMode;
		this.matsimNodeIds = Collections.unmodifiableList(matsimNodesIds);
	}

	VehicleLoop(SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode, List<Id<Node>> matsimNodesIds) {
		this(new CommodityMode(commodity, mode), matsimNodesIds);
	}

	public SamgodsConstants.Commodity getCommodity() {
		return this.commodityMode.commodity();
	}
	
	public SamgodsConstants.TransportMode getMode() {
		return this.commodityMode.samgodsMode();
	}
	
	public CommodityMode getCommodityMode() {
		return this.commodityMode;
	}

	public List<Id<Node>> getMATSimNodeIdsView() {
		return this.matsimNodeIds;
	}

	public boolean containsOD(OD od) {
		return (this.matsimNodeIds.contains(od.origin) && this.matsimNodeIds.contains(od.destination));
	}

	public void addConsolidationUnit(ConsolidationUnit consolidationUnit) {
		assert (this.commodityMode.equals(new CommodityMode(consolidationUnit)));
		this.consolidationUnits.add(consolidationUnit);
		if (this.feasibleVehicleTypes == null) {
			this.feasibleVehicleTypes = new LinkedHashSet<>(this.extractRoutedVehicleTypes(consolidationUnit));
		} else {
			this.feasibleVehicleTypes.retainAll(this.extractRoutedVehicleTypes(consolidationUnit));	
		}		
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
