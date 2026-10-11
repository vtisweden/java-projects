/**
 * se.vti.roundtrips.multiple
 * 
 * Copyright (C) 2024-26 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.multiple.coldhot;

import java.util.Random;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @param <N>
 */
public class ColdHotMultiRoundTripProposal<N extends Node> implements MHProposal<ColdHotMultiRoundTrip<N>> {

	// -------------------- MEMBERS --------------------

	private final MultiRoundTripProposal<N> coldProposal;
	private final MultiRoundTripProposal<N> hotProposal;

	private final Random rnd;

	private final double switchProba;

	// -------------------- CONSTRUCTION --------------------

	public ColdHotMultiRoundTripProposal(MultiRoundTripProposal<N> coldProposal, MultiRoundTripProposal<N> hotProposal,
			double switchProba) {
		this.coldProposal = coldProposal;
		this.hotProposal = hotProposal;
		this.rnd = coldProposal.getRandom();
		this.switchProba = switchProba;
	}

	// -------------------- INTERNALS --------------------

	// -------------------- IMPLEMENTATION OF MHProposal --------------------

	@Override
	public ColdHotMultiRoundTrip<N> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<ColdHotMultiRoundTrip<N>> newTransition(ColdHotMultiRoundTrip<N> from) {

		final ColdHotMultiRoundTrip<N> to;
		final double fwdLogProba;
		final double bwdLogProba;

		if (this.rnd.nextDouble() < this.switchProba) {

			to = from.clone();
			int numberOfSwitches = 0;
			while (numberOfSwitches == 0) {
				for (int i = 0; i < to.size(); i++) {
					if (this.rnd.nextBoolean()) {
						to.switchRoundTrip(i);
						numberOfSwitches++;
					}
				}
			}
			fwdLogProba = Math.log(this.switchProba) + to.size() * Math.log(0.5)
					- Math.log(1.0 - Math.pow(0.5, to.size()));
			bwdLogProba = fwdLogProba;

		} else {

			var coldTransition = this.coldProposal.newTransition(from.getCold());
			var hotTransition = this.hotProposal.newTransition(from.getHot());
			to = new ColdHotMultiRoundTrip<>(coldTransition.getNewState(), hotTransition.getNewState());
			fwdLogProba = Math.log(1.0 - this.switchProba) + coldTransition.getFwdLogProb()
					+ hotTransition.getFwdLogProb();
			bwdLogProba = Math.log(1.0 - this.switchProba) + coldTransition.getBwdLogProb()
					+ hotTransition.getBwdLogProb();

		}

		return new MHTransition<>(from, to, fwdLogProba, bwdLogProba);
	}
}
