/**
 * se.vti.atap.minimalframework
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.atap.minimalframework.defaults.planselection;

import java.util.Set;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.NetworkConditions;
import se.vti.atap.minimalframework.PlanSelection;

/**
 * 
 * @author GunnarF
 *
 */
public class OnlyBestPlanSelection<A extends Agent<?>, T extends NetworkConditions> implements PlanSelection<A, T> {

	public OnlyBestPlanSelection() {
	}

	@Override
	public void assignSelectedPlans(Set<A> agents, T networkConditions, int iteration) {
		agents.stream().max((a1, a2) -> Double.compare(a1.computeGap(), a2.computeGap())).get()
				.setCurrentPlanToCandidatePlan();
	}

}
