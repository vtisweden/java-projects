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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.NetworkConditions;
import se.vti.atap.minimalframework.Plan;
import se.vti.atap.minimalframework.PlanSelection;
import se.vti.atap.minimalframework.defaults.replannerselection.MSAStepSize;

/**
 * 
 * @author GunnarF
 *
 */
public class ProposedMethodWithLocalSearchPlanSelection<P extends Plan, A extends Agent<P>, T extends NetworkConditions, Q extends ApproximateNetworkConditions<P, A, Q>>
		implements PlanSelection<A, T> {

	private final MSAStepSize stepSize;

	private final Random rnd;

	private final ApproximateNetworkLoading<P, A, T, Q> approximateNetworkLoading;

	private boolean approximateDistance = false;

	private double minimalRelativeImprovement = 1e-8;
	
	public ProposedMethodWithLocalSearchPlanSelection(double stepSizeIterationExponent, Random rnd,
			ApproximateNetworkLoading<P, A, T, Q> approximateNetworkLoading) {
		this.stepSize = new MSAStepSize(stepSizeIterationExponent);
		this.rnd = rnd;
		this.approximateNetworkLoading = approximateNetworkLoading;
	}

	public ProposedMethodWithLocalSearchPlanSelection<P, A, T, Q> setApproximateDistance(boolean approximateDistance) {
		this.approximateDistance = approximateDistance;
		return this;
	}

	public ProposedMethodWithLocalSearchPlanSelection<P, A, T, Q> setMinimalRelativeImprovement(double minimalImprovement) {
		this.minimalRelativeImprovement = minimalImprovement;
		return this;
	}

	private Double objectiveFunctionNumeratorScale = null;
	private Double objectiveFunctionDenominatorScale = null;

	private double computeObjectiveFunctionValue(double expectedImprovement, Q currentApproximateNetworkConditions,
			Q candidateApproximatNetworkConditions, double absoluteAmbitionLevel) {		
		double distance;
		if (this.approximateDistance) {
			distance = currentApproximateNetworkConditions.computeDistance(candidateApproximatNetworkConditions);
		} else {
			distance = currentApproximateNetworkConditions
					.computeLeaveOneOutDistance(candidateApproximatNetworkConditions);
		}
		if (this.objectiveFunctionNumeratorScale == null) {
			this.objectiveFunctionNumeratorScale = 1.0 / expectedImprovement;
			this.objectiveFunctionDenominatorScale = 1.0 / (distance + distance * distance + 1e-8);
		}
		double numerator = this.objectiveFunctionNumeratorScale * (expectedImprovement - absoluteAmbitionLevel);
		double denominator = this.objectiveFunctionDenominatorScale * (distance + distance * distance + 1e-8);
		return numerator / denominator;
	}

	@Override
	public void assignSelectedPlans(Set<A> agents, T networkConditions, int iteration) {

		double absoluteAmbitionLevel = this.stepSize.compute(iteration)
				* agents.stream().mapToDouble(a -> a.computeGap()).sum();

		Q approximateNetworkConditionsWithoutAnySwitch = this.approximateNetworkLoading.compute(agents,
				Collections.emptySet(), networkConditions);

		Set<A> agentsUsingCurrentPlan = new LinkedHashSet<>();
		Set<A> agentsUsingCandidatePlan = new LinkedHashSet<>(agents);

		Q approximateNetworkConditions = this.approximateNetworkLoading.compute(agentsUsingCurrentPlan,
				agentsUsingCandidatePlan, networkConditions);
		double expectedImprovement = agentsUsingCandidatePlan.stream().mapToDouble(a -> a.computeGap()).sum();
		double objectiveFunctionValue = this.computeObjectiveFunctionValue(expectedImprovement,
				approximateNetworkConditionsWithoutAnySwitch, approximateNetworkConditions, absoluteAmbitionLevel);

		List<A> allAgents = new ArrayList<>(agents);
		boolean switched;
		do {
			switched = false;
			Collections.shuffle(allAgents, this.rnd);
//			Collections.sort(allAgents, new Comparator<>() {
//				@Override
//				public int compare(A agent1, A agent2) { // sort in descending order
//					return Double.compare(agent2.computeGap(), agent1.computeGap());
//				}
//			});
			for (A agent : allAgents) {
				boolean agentWasUsingCandidatePlan = agentsUsingCandidatePlan.contains(agent);
				double expectedImprovementAfterSwitch;
				if (agentWasUsingCandidatePlan) {
					approximateNetworkConditions.switchToPlan(agent.getCurrentPlan(), agent);
					expectedImprovementAfterSwitch = expectedImprovement - agent.computeGap();
				} else {
					approximateNetworkConditions.switchToPlan(agent.getCandidatePlan(), agent);
					expectedImprovementAfterSwitch = expectedImprovement + agent.computeGap();
				}
				double objectiveFunctionValueAfterSwitch = this.computeObjectiveFunctionValue(
						expectedImprovementAfterSwitch, approximateNetworkConditionsWithoutAnySwitch,
						approximateNetworkConditions, absoluteAmbitionLevel);
				
				if (objectiveFunctionValueAfterSwitch > objectiveFunctionValue
						* (1.0 + this.minimalRelativeImprovement)) {
					expectedImprovement = expectedImprovementAfterSwitch;
					objectiveFunctionValue = objectiveFunctionValueAfterSwitch;
					if (agentWasUsingCandidatePlan) {
						agentsUsingCandidatePlan.remove(agent);
						agentsUsingCurrentPlan.add(agent);
					} else {
						agentsUsingCurrentPlan.remove(agent);
						agentsUsingCandidatePlan.add(agent);
					}
					switched = true;
				} else {
					approximateNetworkConditions.undoLastSwitch();
				}
			}
		} while (switched);

		agentsUsingCandidatePlan.stream().forEach(a -> a.setCurrentPlanToCandidatePlan());
		agents.stream().forEach(a -> a.setCandidatePlan(null));
	}
}
