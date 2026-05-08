/**
 * se.vti.utils.misc.metropolishastings
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
package se.vti.utils.misc.metropolishastings;

import java.util.Random;

/**
 * 
 * @author GunnarF
 *
 */
public class MHSequentialOneStepLogic<X> implements MHOneStepLogic<X> {

	private class SequentialState implements MHState<X> {

		private final X state;
		
		private final double logWeight;

		private SequentialState(X state, double logWeight) {
			this.state = state;
			this.logWeight = logWeight;
		}

		@Override
		public Double getLogWeight() {
			return this.logWeight;
		}
		
		@Override
		public X getState() {
			return this.state;
		}
	}

	private final MHProposal<X> proposal;

	private final MHWeight<X> weight;

	private final Random rnd;

	public MHSequentialOneStepLogic(final MHProposal<X> proposal, final MHWeight<X> weight, final Random rnd) {
		this.proposal = proposal;
		this.weight = weight;
		this.rnd = rnd;
	}

	@Override
	public MHState<X> createInitial(X initial) {
		return new SequentialState(initial, this.weight.logWeight(initial));
	}

	@Override
	public MHState<X> drawNext(MHState<X> currentState) {

		// TODO cache
		final double currentLogWeight = currentState.getLogWeight();

		final MHTransition<X> proposalTransition = this.proposal.newTransition(currentState.getState());
		final X proposalState = proposalTransition.getNewState();
		double proposalLogWeight = this.weight.logWeight(proposalState);
		final double logAlpha = (proposalLogWeight - currentLogWeight)
				+ (proposalTransition.getBwdLogProb() - proposalTransition.getFwdLogProb());

		if (Math.log(this.rnd.nextDouble()) < logAlpha) {
			return new SequentialState(proposalState, proposalLogWeight);
		} else {
			return currentState;
		}
	}
}
