/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.runner.modified;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.simulator.Simulator;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * @author GunnarF
 */
public class ModifiedProposalWrapper<N extends Node> implements MHProposal<ModifiedMultiRoundTrip<N>> {

	private final MHProposal<MultiRoundTrip<N>> delegate;

	private final Simulator<N> simulator;

	ModifiedProposalWrapper(MHProposal<MultiRoundTrip<N>> delegate, Simulator<N> simulator) {
		this.delegate = delegate;
		this.simulator = simulator;
	}

	@Override
	public ModifiedMultiRoundTrip<N> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<ModifiedMultiRoundTrip<N>> newTransition(ModifiedMultiRoundTrip<N> oldState) {
		var transition = this.delegate.newTransition(oldState.getOriginal());
		return new MHTransition<ModifiedMultiRoundTrip<N>>(oldState,
				new ModifiedMultiRoundTrip<N>(oldState.getModifier(), transition.getNewState(), this.simulator),
				transition.getFwdLogProb(), transition.getBwdLogProb());
	}

}
