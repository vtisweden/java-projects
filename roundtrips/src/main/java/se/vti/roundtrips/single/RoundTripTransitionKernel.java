/**
 * se.vti.roundtrips.parallel
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

import java.util.LinkedList;
import java.util.List;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;

/**
 * 
 * @author GunnarF
 *
 * @param <N>
 */
class RoundTripTransitionKernel<N extends Node> {

	// -------------------- CONSTANTS --------------------

	/* package for testing */ final RoundTrip<?> from;
	/* package for testing */ final Scenario<?> scenario;

	public final double insertProba;
	public final double removeProba;
	public final double flipLocationProba;
	public final double flipDepTimeProba;

	/* package for testing */ final double transitionProbaGivenFlipLocation;
	/* package for testing */ final double transitionProbaGivenFlipDepTime;

	// -------------------- CONSTRUCTION --------------------

	RoundTripTransitionKernel(RoundTrip<N> from, Scenario<N> scenario, RoundTripProposalParameters params) {
		this.from = from;
		this.scenario = scenario;

		double effectiveInsertWeight = (from.size() < scenario.getMaxPossibleStayEpisodes() ? params.insertWeight
				: 0.0);
		double effectiveRemoveWeight = (from.size() > 0 ? params.removeWeight : 0.0);
		double effectiveFlipLocationWeight = (from.size() > 0 ? params.flipLocationWeight : 0.0);
		double effectiveFlipDepTimeWeight = (from.size() > 0 && from.size() < scenario.getTimeBinCnt()
				? params.flipDepTimeWeight
				: 0.0);
		final double effectiveWeightSum = effectiveInsertWeight + effectiveRemoveWeight + effectiveFlipLocationWeight
				+ effectiveFlipDepTimeWeight;

		this.insertProba = effectiveInsertWeight / effectiveWeightSum;
		this.removeProba = effectiveRemoveWeight / effectiveWeightSum;
		this.flipLocationProba = effectiveFlipLocationWeight / effectiveWeightSum;
		this.flipDepTimeProba = effectiveFlipDepTimeWeight / effectiveWeightSum;

		assert (Math.abs(
				1.0 - this.insertProba - this.removeProba - this.flipLocationProba - this.flipDepTimeProba) < 1e-8);

		this.transitionProbaGivenFlipLocation = 1.0 / from.size() / (scenario.getNodesCnt() - 1);
		this.transitionProbaGivenFlipDepTime = 1.0 / from.size() / (scenario.getTimeBinCnt() - from.size());
	}

	RoundTripTransitionKernel(RoundTrip<N> from, Scenario<N> scenario) {
		this(from, scenario, new RoundTripProposalParameters());
	}

	// -------------------- INTERNALS --------------------

	/* package for testing */ double numberOfInsertionPoints(List<?> shorter, List<?> longer) {
		assert (shorter.size() + 1 == longer.size());
		int result = 0;
		LinkedList<Object> tmp = new LinkedList<>(shorter);
		for (int i = 0; i < longer.size(); i++) {
			tmp.add(i, longer.get(i));
			if (tmp.equals(longer)) {
				result++;
			}
			tmp.remove(i);
			assert (tmp.equals(shorter));
		}
		return result;
	}

	/* package for testing */ double transitionProbaGivenInsert(RoundTrip<?> to) {
		return this.numberOfInsertionPoints(this.from.getNodesView(), to.getNodesView()) / (this.from.size() + 1.0)
				/ this.scenario.getNodesCnt() / (this.scenario.getTimeBinCnt() - this.from.size());
	}

	private double numberOfRemovalPoints(List<?> longer, List<?> shorter) {
		// see unit test for an explicit computation
		return this.numberOfInsertionPoints(shorter, longer);
	}

	/* package for testing */ double transitionProbaGivenRemove(RoundTrip<?> to) {
		double result = this.numberOfRemovalPoints(this.from.getNodesView(), to.getNodesView()) / this.from.size()
				/ this.from.size();
		assert (result > 0);
		return result;
	}

	// This assumes that the transition from -> to followed the same kernel.
	double transitionProbaUnchecked(RoundTrip<?> to) {
		double result;
		if (this.from.size() + 1 == to.size()) {
			assert (this.insertProba * this.transitionProbaGivenInsert(to) > 0);
			result = this.insertProba * this.transitionProbaGivenInsert(to);
		} else if (this.from.size() - 1 == to.size()) {
			assert (this.removeProba * this.transitionProbaGivenRemove(to) > 0);
			result = this.removeProba * this.transitionProbaGivenRemove(to);
		} else if (!this.from.getNodesView().equals(to.getNodesView())) {
			assert (this.flipLocationProba * this.transitionProbaGivenFlipLocation > 0);
			result = this.flipLocationProba * this.transitionProbaGivenFlipLocation;
		} else if (!this.from.getDeparturesView().equals(to.getDeparturesView())) {
			assert (this.flipDepTimeProba * this.transitionProbaGivenFlipDepTime > 0);
			result = this.flipDepTimeProba * this.transitionProbaGivenFlipDepTime;
		} else {
			throw new UnsupportedOperationException();
		}
		assert (result > 0);
		return result;
	}

	public double transitionProba(RoundTrip<N> to) {
		return this.transitionProbaUnchecked(to);
	}
}
