/**
 * se.vti.seas
 * 
 * Copyright (C) 2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.seas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author GunnarF
 *
 */
public class ChannelAssignmentSolver {

	private Map<VesselClass, Double> vesselClass2Rate_veh_h = new LinkedHashMap<>();

	public ChannelAssignmentSolver() {
	}

	public ChannelAssignmentSolver addVesselClass(VesselClass vesselClass, double rate_veh_h) {
		this.vesselClass2Rate_veh_h.put(vesselClass, rate_veh_h);
		return this;
	}

	public ChannelAssignment computeAssignment() {

		List<VesselClass> priorityOrderedVesselClasses = new ArrayList<>(
				this.vesselClass2Rate_veh_h.keySet().stream().toList());
		Collections.sort(priorityOrderedVesselClasses);

		ChannelAssignment assignment = new ChannelAssignment();

		for (int consideredClassIndex = 0; consideredClassIndex < priorityOrderedVesselClasses
				.size(); consideredClassIndex++) {
			VesselClass consideredClass = priorityOrderedVesselClasses.get(consideredClassIndex);

			double leftToAssign_ves_h = this.vesselClass2Rate_veh_h.get(consideredClass);
			int lane = -1;
			while (leftToAssign_ves_h > 1e-8) {
				lane++;
				double assignedShare = 1.0;

				for (int otherClassIndex = 0; otherClassIndex < consideredClassIndex; otherClassIndex++) {
					VesselClass otherClass = priorityOrderedVesselClasses.get(otherClassIndex);
					double otherClassAssignment_veh_h = assignment.getRate_veh_h(otherClass, lane);
					if (otherClassAssignment_veh_h > 0.0) {
						if (otherClass.speed_km_h < consideredClass.speed_km_h) {
							// move right for overtaking
							assignedShare -= consideredClass.computeOvertakingLength_km(otherClass)
									* otherClassAssignment_veh_h / otherClass.speed_km_h;
						} else if (otherClass.speed_km_h > consideredClass.speed_km_h) {
							// evade right for yielding
							assignedShare -= otherClass.computeOvertakingLength_km(consideredClass)
									* otherClassAssignment_veh_h / otherClass.speed_km_h;
						}
					}
				}

				assignedShare = Math.max(0.0, assignedShare);
				double assignedRate_veh_h = Math.min(assignedShare * leftToAssign_ves_h,
						(1.0 - assignment.getRelativeLaneUsage(lane)) * consideredClass.speed_km_h
								/ consideredClass.computeLength_km());
				assignment.setRate_veh_h(consideredClass, lane, assignedRate_veh_h);
				leftToAssign_ves_h -= assignedRate_veh_h;
			}
		}

		return assignment;
	}
}
