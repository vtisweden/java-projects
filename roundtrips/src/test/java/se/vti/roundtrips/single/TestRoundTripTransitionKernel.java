/**
 * se.vti.roundtrips.single
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
package se.vti.roundtrips.single;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;

/**
 * @author GunnarF
 */
class TestRoundTripTransitionKernel {

	enum Action {
		INS, REM, FLIP_LOC, FLIP_DEP
	};

	Action identifyAction(RoundTrip<?> from, RoundTrip<?> to) {
		if (from.size() + 1 == to.size()) {
			return Action.INS;
		} else if (from.size() - 1 == to.size()) {
			return Action.REM;
		} else if (!from.getNodesView().equals(to.getNodesView())) {
			return Action.FLIP_LOC;
		} else if (!from.getDeparturesView().equals(to.getDeparturesView())) {
			return Action.FLIP_DEP;
		} else {
			return null;
		}
	}

	boolean locationInsertWasPossible(List<?> shorter, List<?> longer) {
		assert (shorter.size() + 1 == longer.size());
		boolean usedInsert = false;
		for (int indexInShorter = 0; indexInShorter < shorter.size(); indexInShorter++) {
			int indexInLonger = (usedInsert ? indexInShorter + 1 : indexInShorter);
			if (!shorter.get(indexInShorter).equals(longer.get(indexInLonger))) {
				if (usedInsert) {
					return false;
				} else {
					usedInsert = true;
					if (!shorter.get(indexInShorter).equals(longer.get(indexInShorter + 1))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	boolean locationFlipWasPossible(List<?> a, List<?> b) {
		assert (a.size() == b.size());
		int differenceCnt = 0;
		for (int i = 0; i < a.size(); i++) {
			if (!a.get(i).equals(b.get(i))) {
				differenceCnt++;
				if (differenceCnt > 1) {
					return false;
				}
			}
		}
		return (differenceCnt == 1);
	}

	boolean departuresInsertWasPossible(List<?> shorter, List<?> longer) {
		assert (shorter.size() + 1 == longer.size());
		return longer.containsAll(shorter);
	}

	boolean departureFlipWasPossible(List<?> a, List<?> b) {
		assert (a.size() == b.size());

		List<?> diff = new ArrayList<>(a);
		diff.removeAll(b);
		if (diff.size() != 1) {
			return false;
		}

		diff = new ArrayList<>(b);
		diff.removeAll(a);
		return (diff.size() == 1);
	}

	double transitionProbaChecked(RoundTripTransitionKernel<Node> kernel, RoundTrip<?> to) {

		if (kernel.from.size() + 1 == to.size()) {

			if (this.locationInsertWasPossible(kernel.from.getNodesView(), to.getNodesView())
					&& this.departuresInsertWasPossible(kernel.from.getDeparturesView(), to.getDeparturesView())) {
				return kernel.insertProba * kernel.transitionProbaGivenInsert(to);
			}

		} else if (kernel.from.size() - 1 == to.size()) {

			if (this.locationInsertWasPossible(to.getNodesView(), kernel.from.getNodesView())
					&& this.departuresInsertWasPossible(to.getDeparturesView(), kernel.from.getDeparturesView())) {
				return kernel.removeProba * kernel.transitionProbaGivenRemove(to);
			}

		} else if (kernel.from.size() == to.size()) {

			if (kernel.from.getDeparturesView().equals(to.getDeparturesView())) {

				if (this.locationFlipWasPossible(kernel.from.getNodesView(), to.getNodesView())) {
					return kernel.flipLocationProba * kernel.transitionProbaGivenFlipLocation;
				}

			} else if (kernel.from.getNodesView().equals(to.getNodesView())) {

				if (this.departureFlipWasPossible(kernel.from.getDeparturesView(), to.getDeparturesView())) {
					return kernel.flipDepTimeProba * kernel.transitionProbaGivenFlipDepTime;
				}

			}
		}

		return 0.0;
	}

	boolean correctAction(RoundTripTransitionKernel<?> fwdTransitionKernel,
			RoundTripTransitionKernel<?> bwdTransitionKernel, Action realizedFwdAction, RoundTrip<?> from,
			RoundTrip<?> to) {
		final Action identifiedFwdAction = identifyAction(from, to);
		final Action identifiedBwdAction = identifyAction(to, from);
		if (!identifiedFwdAction.equals(realizedFwdAction)) {
			return false;
		}
		if (Action.INS.equals(identifiedFwdAction)) {
			return (Action.REM.equals(identifiedBwdAction));
		} else if (Action.REM.equals(identifiedFwdAction)) {
			return (Action.INS.equals(identifiedBwdAction));
		} else if (Action.FLIP_LOC.equals(identifiedFwdAction)) {
			return (Action.FLIP_LOC.equals(identifiedBwdAction));
		} else if (Action.FLIP_DEP.equals(identifiedFwdAction)) {
			return (Action.FLIP_DEP.equals(identifiedBwdAction));
		} else {
			return false;
		}
	}

	Random rnd = new Random(4711);

	Scenario<Node> createScenario() {
		var scenario = new Scenario<Node>();
		scenario.setTimeBinCnt(24);
		scenario.setTimeBinSize_h(1.0);
		scenario.setUpperBoundOnStayEpisodes(scenario.getTimeBinCnt());
		for (int i = 0; i < 9; i++) {
			scenario.addNode(new Node(Integer.toString(i)));
		}
		for (var from : scenario.getNodesView()) {
			for (var to : scenario.getNodesView()) {
				scenario.setDistance_km(from, to, from == to ? 0.0 : 1.0);
				scenario.setTime_h(from, to, from == to ? 0.0 : 1.0);
			}
		}
		return scenario;
	}

	<T> T readRandomElement(List<T> list) {
		return list.get(this.rnd.nextInt(0, list.size()));
	}

	<T> T removeRandomElement(List<T> list) {
		return list.remove(this.rnd.nextInt(0, list.size()));
	}

	RoundTrip<Node> createRoundTrip(Scenario<Node> scenario, int size) {
		var allNodes = scenario.getNodesView();
		var allDepartures = new ArrayList<>(IntStream.range(0, scenario.getTimeBinCnt()).boxed().toList());

		var nodes = new ArrayList<Node>(size);
		var departures = new ArrayList<Integer>(size);
		for (int i = 0; i < size; i++) {
			nodes.add(this.readRandomElement(allNodes));
			departures.add(removeRandomElement(allDepartures));
		}
		Collections.sort(departures);

		return new RoundTrip<Node>(0, nodes, departures);
	}

	@Test
	void testTransition() {
		var scenario = createScenario();
		var params = new RoundTripProposalParameters();
		var proposal = new RoundTripProposal<Node>(params, scenario);

		for (int size = 0; size <= scenario.getMaxPossibleStayEpisodes(); size++) {
			for (int replication = 0; replication < 10 + 10 * size; replication++) {
				var from = createRoundTrip(scenario, size);
				var transition = proposal.newTransition(from);
				var to = transition.getNewState();

				var fwdKernel = new RoundTripTransitionKernel<>(from, scenario, params);
				Assertions.assertEquals(transition.getFwdLogProb(),
						Math.log(this.transitionProbaChecked(fwdKernel, to)), 1e-8);

				var bwdKernel = new RoundTripTransitionKernel<>(to, scenario, params);
				Assertions.assertEquals(transition.getBwdLogProb(),
						Math.log(this.transitionProbaChecked(bwdKernel, from)), 1e-8);
			}
		}
	}

	double explicitNumberOfRemovalPoints(List<?> longer, List<?> shorter) {
		assert (shorter.size() + 1 == longer.size());
		int result = 0;
		LinkedList<Object> tmp = new LinkedList<>(longer);
		for (int i = 0; i < longer.size(); i++) {
			Object removed = tmp.remove(i);
			assert (tmp.size() == shorter.size());
			if (tmp.equals(shorter)) {
				result++;
			}
			tmp.add(i, removed);
			assert (tmp.equals(longer));
		}
		assert (result > 0);
		return result;
	}

	@Test
	void testNumberOfInsertionRemovalPoints() {
		var scenario = createScenario();
		var params = new RoundTripProposalParameters(0.5, 0.5, 0.0, 0.0); // only insert
		var proposal = new RoundTripProposal<Node>(params, scenario);

		for (int size = 0; size <= scenario.getMaxPossibleStayEpisodes() - 1; size++) {
			for (int replication = 0; replication < 10 + 10 * size; replication++) {
				var from = createRoundTrip(scenario, size);
				var transition = proposal.newTransition(from);
				var to = transition.getNewState();

				if (from.size() - 1 == to.size()) {

					var bwdKernel = new RoundTripTransitionKernel<>(to, scenario, params);
					int numberOfFwdRemovalPoints = (int) this.explicitNumberOfRemovalPoints(from.getNodesView(), to.getNodesView());
					int numberOfBwdInsertionPoints = (int) bwdKernel.numberOfInsertionPoints(to.getNodesView(), from.getNodesView());
					Assertions.assertEquals(numberOfFwdRemovalPoints, numberOfBwdInsertionPoints, "from=" + from.getNodesView() + ", to=" + to.getNodesView());
					
				} else if (from.size() + 1 == to.size()) {

					var fwdKernel = new RoundTripTransitionKernel<>(from, scenario, params);
					int numberOfFwdInsertionPoints = (int) fwdKernel.numberOfInsertionPoints(from.getNodesView(), to.getNodesView());
					int numberOfBwdRemovalPoints = (int) this.explicitNumberOfRemovalPoints(to.getNodesView(), from.getNodesView());
					Assertions.assertEquals(numberOfFwdInsertionPoints, numberOfBwdRemovalPoints, "from=" + from.getNodesView() + ", to=" + to.getNodesView());
					
				} else {
					Assertions.fail("Round trip size did not change by +/- one.");
				}
			}
		}
	}
}
