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

import java.util.function.Function;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.runner.AbstractStateAdapter;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.scenario.Scenario;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * @author GunnarF
 * 
 * @param <N> the node type
 */
public class ModifiedStateAdapter<N extends Node> extends AbstractStateAdapter<N, ModifiedMultiRoundTrip<N>> {

	private final Function<MultiRoundTrip<N>, MultiRoundTrip<N>> modifier;

	public ModifiedStateAdapter(Scenario<N> scenario, Function<MultiRoundTrip<N>, MultiRoundTrip<N>> modifier,
			MHProposal<MultiRoundTrip<N>> proposal) {
		super(scenario);
		this.proposal = new ModifiedProposalWrapper<N>(proposal, scenario.getOrCreateSimulator());
		this.modifier = modifier;
	}

	@Override
	protected AbstractStateAdapter<N, ModifiedMultiRoundTrip<N>> setInitialState(MultiRoundTrip<N> initialState) {
		this.initialState = new ModifiedMultiRoundTrip<>(this.modifier, initialState,
				this.scenario.getOrCreateSimulator());
		return this;
	}

	@Override
	protected AbstractStateAdapter<N, ModifiedMultiRoundTrip<N>> addWeight(MHWeight<MultiRoundTrip<N>> weight,
			double scale) {
		this.weights.add(new ModifiedWeightWrapper<>(weight), scale);
		return this;
	}

}
