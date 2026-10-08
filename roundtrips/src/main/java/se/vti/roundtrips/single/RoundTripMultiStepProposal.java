/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023-2026 by Gunnar Flötteröd (VTI, LiU).
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

import java.util.Random;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 * @param <N> the location type
 */
public class RoundTripMultiStepProposal<N extends Node> implements MHProposal<RoundTrip<N>> {

	// -------------------- MEMBERS --------------------

	private final RoundTripSingleStepProposal<N> singleStepProposal;

	private final double stopProbaBeyondOne;

	private final Random rnd;

	// -------------------- CONSTRUCTION --------------------

	public RoundTripMultiStepProposal(RoundTripProposalParameters proposalParams, Scenario<N> scenario,
			double expectedSequenceLength) {
		this.singleStepProposal = new RoundTripSingleStepProposal<>(proposalParams, scenario);
		this.stopProbaBeyondOne = 1.0 / expectedSequenceLength;
		this.rnd = scenario.getRandom();
	}

	public RoundTripMultiStepProposal(Scenario<N> scenario, double expectedSequenceLength) {
		this(new RoundTripProposalParameters(), scenario, expectedSequenceLength);
	}

	// -------------------- INTERNALS --------------------

	/* package for testing */ int drawSequenceLength() {
		int length = 1;
		while (!(this.rnd.nextDouble() < this.stopProbaBeyondOne) && (length < 1_000_000)) {
			length++;
		}
		return length;
	}

	/* package for testing */ double computeSequenceLogProba(int length) {
		return (length - 1.0) * Math.log(1.0 - this.stopProbaBeyondOne) + Math.log(this.stopProbaBeyondOne);
	}

	// -------------------- IMPLEMENTATION OF INTERFACE --------------------

	@Override
	public RoundTrip<N> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrip<N>> newTransition(RoundTrip<N> from) {

		final int length = this.drawSequenceLength();
		final double lengthLogProba = this.computeSequenceLogProba(length);

		double fwdLogProba = lengthLogProba;
		double bwdLogProba = lengthLogProba;

		RoundTrip<N> current = from;
		for (int i = 0; i < length; i++) {
			MHTransition<RoundTrip<N>> step = this.singleStepProposal.newTransition(current);
			fwdLogProba += step.getFwdLogProb();
			bwdLogProba += step.getBwdLogProb();
			current = step.getNewState();
		}

		return new MHTransition<>(from, current, fwdLogProba, bwdLogProba);
	}
}
