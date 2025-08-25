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
package se.vti.atap.minimalframework.defaults.replannerselection.proposed;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.Plan;
import se.vti.atap.minimalframework.examples.parallel_links.Network;

/**
 * 
 * @author GunnarF
 *
 */
public abstract class AbstractApproximateNetworkConditions<P extends Plan, A extends Agent<P>, Q extends AbstractApproximateNetworkConditions<P, A, Q>>
		implements ApproximateNetworkConditions<P, A, Q> {

	protected final Map<A, P> agent2plan;

	public AbstractApproximateNetworkConditions(Set<A> agentsUsingCurrentPlan, Set<A> agentsUsingCandidatePlan,
			Network network) {
		this.agent2plan = new LinkedHashMap<>(agentsUsingCurrentPlan.size() + agentsUsingCandidatePlan.size());
		this.initializeInternalState(network);
		for (A agent : agentsUsingCurrentPlan) {
			this.switchToPlan(agent.getCurrentPlan(), agent);
		}
		for (A agent : agentsUsingCandidatePlan) {
			this.switchToPlan(agent.getCandidatePlan(), agent);
		}
	}

	@Override
	public double computeLeaveOneOutDistance(Q other) {
		double result = 0.0;
		for (A agent : this.agent2plan.keySet()) {
			this.switchToPlan(null, agent);
			other.switchToPlan(null, agent);
			result += this.computeDistance(other);
			this.undoLastSwitch();
			other.undoLastSwitch();
		}
		return result /= this.agent2plan.size();
	}

	@Override
	abstract public double computeDistance(Q other);

	abstract protected void initializeInternalState(Network network);

	@Override
	public abstract void switchToPlan(P plan, A agent);

	@Override
	abstract public void undoLastSwitch();
}
