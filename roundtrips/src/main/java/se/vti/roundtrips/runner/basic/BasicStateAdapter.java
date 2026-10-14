/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2025-2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.runner.basic;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.runner.AbstractStateAdapter;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.scenario.Scenario;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripMultiStepProposal;
import se.vti.roundtrips.single.RoundTripSingleStepProposal;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * @author GunnarF
 * 
 */
public class BasicStateAdapter<N extends Node> extends AbstractStateAdapter<N, MultiRoundTrip<N>> {

	public BasicStateAdapter(Scenario<N> scenario) {
		super(scenario);
	}

	public BasicStateAdapter<N> setProposal(BasicProposal<N> proposal) {
		this.proposal = proposal;
		return this;
	}

	@Override
	public BasicStateAdapter<N> setInitialState(MultiRoundTrip<N> initialState) {
		this.initialState = initialState;
		return this;
	}

	@Override
	public BasicStateAdapter<N> addPrior(MHWeight<MultiRoundTrip<N>> weight) {
		this.weights.add(weight, 1.0);
		return this;
	}

	@Override
	public BasicStateAdapter<N> addWeight(MHWeight<MultiRoundTrip<N>> weight, double scale) {
		this.weights.add(weight, scale);
		return this;
	}

	// HELPERS
	
	public static <N extends Node> BasicProposal<N> createPlainProposal(Scenario<N> scenario, double proposalLength) {
		MHProposal<RoundTrip<N>> singleProposal;
		if (proposalLength == 1.0) {
			singleProposal = new RoundTripSingleStepProposal<N>(scenario);
		} else {
			singleProposal = new RoundTripMultiStepProposal<N>(scenario, proposalLength);
		}
		return new BasicProposal<N>(scenario, singleProposal);
	}

	public static <N extends Node> BasicStateAdapter<N> createDefaultInstance(Scenario<N> scenario, double proposalLength) {
		return new BasicStateAdapter<>(scenario).setProposal(createPlainProposal(scenario, proposalLength));
	}

	
}
