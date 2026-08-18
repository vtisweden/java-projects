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

import se.vti.samgods.common.SamgodsConstants.CommodityModeContainer;

/**
 * Exploratory -- consider wiring this directly into the affected classes /
 * interfaces.
 * 
 * @author GunnarF
 */
class VehicleLoopManager {

	// -------------------- MEMBERS --------------------

	private final Map<CommodityModeContainer, Set<ConsolidationUnit>> allConsolidationUnits = new LinkedHashMap<>();

	private final Map<CommodityModeContainer, Set<VehicleLoop>> allLoops = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	VehicleLoopManager(Set<ConsolidationUnit> allConsolidationUnits) {
		for (ConsolidationUnit consolidationUnit : allConsolidationUnits) {
			this.allConsolidationUnits
					.computeIfAbsent(new CommodityModeContainer(consolidationUnit), cmc -> new LinkedHashSet<>())
					.add(consolidationUnit);
		}
	}

	// -------------------- IMPLEMENTATION --------------------
	
	void addLoop(VehicleLoop loop) {
		var allLoops = this.allLoops.computeIfAbsent(loop.getCommodityModeContainer(), c -> new LinkedHashSet<>());
		for (VehicleLoop existingLoop : allLoops) {
			if (this.equalUpToShift(existingLoop.getMATSimNodeIdsView(), loop.getMATSimNodeIdsView())) {
				return;
			}
		}
		allLoops.add(loop);

		for (ConsolidationUnit consolidationUnit : allConsolidationUnits.getOrDefault(loop.getCommodityModeContainer(),
				Collections.emptySet())) {
			if (loop.containsOD(consolidationUnit.od)) {
				loop.addConsolidationUnit(consolidationUnit);
			}
		}
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
