/**
 * se.vti.atap.examples.minimalframework.parallel_links.ods
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
package se.vti.atap.minimalframework.examples.parallel_links;

import java.util.Set;

import se.vti.atap.minimalframework.defaults.replannerselection.proposed.AbstractApproximateNetworkConditions;

/**
 * 
 * @author GunnarF
 *
 */
public class ApproximateNetworkConditionsImpl
		extends AbstractApproximateNetworkConditions<PathFlows, AgentImpl, ApproximateNetworkConditionsImpl> {

	private double[] linkFlows_veh;

	private PathFlows lastSwitchedPlan = null;
	private AgentImpl lastSwitchedAgent = null;
	private double[] lastLinkFlowsBeforeSwitch_veh = null;

	public ApproximateNetworkConditionsImpl(Set<AgentImpl> agentsUsingCurrentPlan,
			Set<AgentImpl> agentsUsingCandidatePlan, Network network) {
		super(agentsUsingCurrentPlan, agentsUsingCandidatePlan, network);
	}

	@Override
	protected void initializeInternalState(Network network) {
		this.linkFlows_veh = new double[network.getNumberOfLinks()];
	}

	@Override
	public double computeDistance(ApproximateNetworkConditionsImpl other) {
		double sumOfSquares = 0.0;
		for (int link = 0; link < this.linkFlows_veh.length; link++) {
			double diff = this.linkFlows_veh[link] - other.linkFlows_veh[link];
			sumOfSquares += diff * diff;
		}
		return Math.sqrt(sumOfSquares);
	}

	@Override
	public void switchToPlan(PathFlows plan, AgentImpl agent) {
		this.lastSwitchedPlan = this.agent2plan.get(agent);
		this.lastSwitchedAgent = agent;
		this.lastLinkFlowsBeforeSwitch_veh = new double[agent.getNumberOfPaths()];
		for (int path = 0; path < agent.getNumberOfPaths(); path++) {
			this.lastLinkFlowsBeforeSwitch_veh[path] = this.linkFlows_veh[agent.availableLinks[path]];
		}

		if (plan != null) {
			double[] pathFlows_veh = plan.computePathFlows_veh();
			this.agent2plan.put(agent, plan);
			for (int path = 0; path < agent.getNumberOfPaths(); path++) {
				this.linkFlows_veh[agent.availableLinks[path]] += pathFlows_veh[path];
			}
		} else {
			this.agent2plan.remove(agent);
		}
		if (this.lastSwitchedPlan != null) {
			double[] pathFlows_veh = this.lastSwitchedPlan.computePathFlows_veh();
			for (int path = 0; path < agent.getNumberOfPaths(); path++) {
				this.linkFlows_veh[agent.availableLinks[path]] -= pathFlows_veh[path];
			}
		}
	}

	@Override
	public void undoLastSwitch() {
		for (int path = 0; path < this.lastSwitchedAgent.getNumberOfPaths(); path++) {
			this.linkFlows_veh[this.lastSwitchedAgent.availableLinks[path]] = this.lastLinkFlowsBeforeSwitch_veh[path];
		}
		this.agent2plan.put(this.lastSwitchedAgent, this.lastSwitchedPlan);

		this.lastSwitchedPlan = null;
		this.lastSwitchedAgent = null;
		this.lastLinkFlowsBeforeSwitch_veh = null;
	}
}
