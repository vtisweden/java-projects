/**
 * se.vti.roundtrips.multiple
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.multiple;

import java.util.Random;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripSingleStepProposal;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 */
public class MultiRoundTripProposal<N extends Node> implements MHProposal<MultiRoundTrip<N>> {

	// -------------------- MEMBERS --------------------

	private final Random rnd;

	private final MHProposal<RoundTrip<N>> singleRoundTripProposal;

	private Double flipProba = null;

	// -------------------- CONSTRUCTION --------------------

	public MultiRoundTripProposal(Random rnd, MHProposal<RoundTrip<N>> singleRoundTripProposal) {
		this.rnd = rnd;
		this.singleRoundTripProposal = singleRoundTripProposal;
	}

	public MultiRoundTripProposal(Scenario<N> scenario, MHProposal<RoundTrip<N>> singleRoundTripProposal) {
		this(scenario.getRandom(), singleRoundTripProposal);
	}

	public MultiRoundTripProposal<N> setFlipProbability(double flipProbability) {
		this.flipProba = flipProbability;
		return this;
	}

	// --------------------IMPLEMENTATION OF MHProposal --------------------

	@Override
	public MultiRoundTrip<N> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<MultiRoundTrip<N>> newTransition(MultiRoundTrip<N> from) {

		final double minFlipProba = 1.0 / Math.max(1.0, from.size());
		final double flipProba = (this.flipProba != null ? Math.max(this.flipProba, minFlipProba) : minFlipProba);
		final double atLeastOneFlipProba = 1.0 - Math.pow(1.0 - flipProba, from.size());

		final MultiRoundTrip<N> to = from.clone();

		boolean flipped = false;
		double fwdLogProba;
		double bwdLogProba;
		do {
			fwdLogProba = 0.0;
			bwdLogProba = 0.0;
			for (int i = 0; i < from.size(); i++) {
				if (this.rnd.nextDouble() < flipProba) {
					MHTransition<RoundTrip<N>> transition = this.singleRoundTripProposal.newTransition(from.getRoundTrip(i));
					to.setRoundTripAndUpdateSummaries(i, transition.getNewState());
					fwdLogProba += Math.log(flipProba) + transition.getFwdLogProb();
					bwdLogProba += Math.log(flipProba) + transition.getBwdLogProb();
					flipped = true;
				} else {
					fwdLogProba += Math.log(1.0 - flipProba);
					bwdLogProba += Math.log(1.0 - flipProba);
				}
			}
		} while (!flipped);
		fwdLogProba -= Math.log(atLeastOneFlipProba);
		bwdLogProba -= Math.log(atLeastOneFlipProba);

		return new MHTransition<>(from, to, fwdLogProba, bwdLogProba);
	}

}
