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
public class Loop {

	private final CommodityMode commodityMode;

	private final List<Id<Node>> matsimNodeIds;

	private Set<VehicleType> feasibleVehicleTypes = null;

	private List<Set<ConsolidationUnit>> consolidationUnitsPerContainerSegment = null;

	private List<Set<ConsolidationUnit>> consolidationUnitsPerNoContainerSegment = null;

	Loop(CommodityMode commodityMode, List<Id<Node>> matsimNodesIds) {
		this.commodityMode = commodityMode;
		this.matsimNodeIds = Collections.unmodifiableList(matsimNodesIds);
	}

	Loop(SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode, List<Id<Node>> matsimNodesIds) {
		this(new CommodityMode(commodity, mode), matsimNodesIds);
	}

	public void setFeasibleVehicleTypes(Set<VehicleType> feasibleVehicleTypes) {
		this.feasibleVehicleTypes = Collections.unmodifiableSet(feasibleVehicleTypes);
	}

	public void setConsolidationUnitsPerContainerSegment(
			List<Set<ConsolidationUnit>> consolidationUnitsPerContainerSegment) {
		this.consolidationUnitsPerContainerSegment = consolidationUnitsPerContainerSegment;
	}

	public void setConsolidationUnitsPerNoContainerSegment(
			List<Set<ConsolidationUnit>> consolidationUnitsPerNoContainerSegment) {
		this.consolidationUnitsPerNoContainerSegment = consolidationUnitsPerNoContainerSegment;
	}

	public Set<ConsolidationUnit> getContainerConsolidationUnits(int i) {
		return this.consolidationUnitsPerContainerSegment.get(i);
	}

	public Set<ConsolidationUnit> getNoContainerConsolidationUnits(int i) {
		return this.consolidationUnitsPerNoContainerSegment.get(i);
	}

	public boolean isContainerVehicleCompatible() {
		return this.isVehicleCompatible(this.consolidationUnitsPerContainerSegment);
	}

	public boolean isNoContainerVehicleCompatible() {
		return this.isVehicleCompatible(this.consolidationUnitsPerNoContainerSegment);
	}
	
	private boolean isVehicleCompatible(List<Set<ConsolidationUnit>> consolidationUnitsPerSegment) {
		return (consolidationUnitsPerSegment != null) && (consolidationUnitsPerSegment.size() == this.size())
				&& consolidationUnitsPerSegment.stream().allMatch(cus -> cus != null && cus.size() > 0);
	}

	public Set<VehicleType> getFeasibleVehicleTypesView() {
		return this.feasibleVehicleTypes;
	}

	public int size() {
		return this.matsimNodeIds.size();
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
}
