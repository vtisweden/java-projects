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
package se.vti.roundtrips.runner;

import java.util.Random;

import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.scenario.Scenario;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @param <N>
 */
class ColdHotProposalAdapter<N extends Node> implements MHProposal<RoundTrips<N>> {

	// -------------------- MEMBERS --------------------

	private final Random rnd;

	private final RoundTripsProposalAdapter<N> coldProposal;
	private final RoundTripsProposalAdapter<N> hotProposal;

	private final double coldHotRecombineProba;

	// -------------------- CONSTRUCTION --------------------

	ColdHotProposalAdapter(Scenario<N> scenario, MHProposal<RoundTrip<N>> coldProposal,
			MHProposal<RoundTrip<N>> hotProposal, double coldHotRecombineProba) {		
		this.rnd = scenario.getRandom();
		this.coldProposal = new RoundTripsProposalAdapter<N>(scenario, coldProposal);
		this.hotProposal = new RoundTripsProposalAdapter<N>(scenario, hotProposal);
		this.coldHotRecombineProba = coldHotRecombineProba;
	}

	// -------------------- IMPLEMENTATION OF MHProposal --------------------

	@Override
	public ColdHotMultiRoundTrip<N> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrips<N>> newTransition(RoundTrips<N> from) {

		final ColdHotMultiRoundTrip<N> fromColdHotMultiRoundTrip = (ColdHotMultiRoundTrip<N>) from;
		final ColdHotMultiRoundTrip<N> to;
		final double fwdLogProba;
		final double bwdLogProba;

		if (this.rnd.nextDouble() < this.coldHotRecombineProba) {

			to = fromColdHotMultiRoundTrip.clone();

			int numberOfDifferentRoundTrips = 0;
			for (int i = 0; i < to.size(); i++) {
				if (!to.getCold().getRoundTrip(i).equalsBehaviorally(to.getHot().getRoundTrip(i))) {
					numberOfDifferentRoundTrips++;
					if (this.rnd.nextBoolean()) {
						to.switchRoundTrip(i);
					}
				}
			}

			fwdLogProba = Math.log(this.coldHotRecombineProba) + numberOfDifferentRoundTrips * Math.log(0.5)
					- Math.log(1.0 - Math.pow(0.5, numberOfDifferentRoundTrips));
			bwdLogProba = fwdLogProba;

		} else {

			MHTransition<RoundTrips<N>> coldTransition = this.coldProposal
					.newTransition(fromColdHotMultiRoundTrip.getCold());
			MHTransition<RoundTrips<N>> hotTransition = this.hotProposal
					.newTransition(fromColdHotMultiRoundTrip.getHot());
			to = new ColdHotMultiRoundTrip<>(coldTransition.getNewState().getMultiRoundTrip(),
					hotTransition.getNewState().getMultiRoundTrip());
			fwdLogProba = Math.log(1.0 - this.coldHotRecombineProba) + coldTransition.getFwdLogProb()
					+ hotTransition.getFwdLogProb();
			bwdLogProba = Math.log(1.0 - this.coldHotRecombineProba) + coldTransition.getBwdLogProb()
					+ hotTransition.getBwdLogProb();

		}

		return new MHTransition<>(from, to, fwdLogProba, bwdLogProba);
	}
}
