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
package se.vti.roundtrips.runner.coldhot;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.runner.AbstractStateAdapter;
import se.vti.roundtrips.runner.BundledSingleToMultiWeight;
import se.vti.roundtrips.runner.basic.BasicProposal;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.scenario.Scenario;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * @author GunnarF
 * 
 */
public class ColdHotStateAdapter<N extends Node> extends AbstractStateAdapter<N, ColdHotMultiRoundTrip<N>> {

	// -------------------- CONSTANTS --------------------

	private final double relativeHotScale;

	// -------------------- CONSTRUCTION --------------------

	public ColdHotStateAdapter(Scenario<N> scenario, double relativeHotScale) {
		super(scenario);
		this.relativeHotScale = relativeHotScale;
	}

	public ColdHotStateAdapter<N> setProposals(BasicProposal<N> coldProposal, BasicProposal<N> hotProposal,
			double coldHotRecombineProba) {
		this.proposal = new ColdHotProposalWrapper<N>(this.scenario.getRandom(), coldProposal, hotProposal,
				coldHotRecombineProba);
		return this;
	}

	public static <N extends Node> ColdHotStateAdapter<N> createDefaultInstance(Scenario<N> scenario,
			double relativeHotScale, double expectedColdProposalLength, double expectedHotProposalLength,
			double coldHotRecombineProba) {
		var coldProposal = BasicProposal.createDefaultInstance(scenario, expectedColdProposalLength);
		var hotProposal = BasicProposal.createDefaultInstance(scenario, expectedHotProposalLength);
		return new ColdHotStateAdapter<N>(scenario, relativeHotScale).setProposals(coldProposal, hotProposal,
				coldHotRecombineProba);
	}

	// -------------------- EXTENSION OF AbstractStateAdapter --------------------

	@Override
	public ColdHotStateAdapter<N> setInitialState(MultiRoundTrip<N> initialState) {
		var clone = initialState.clone();
		clone.simulateAll(this.scenario.getOrCreateSimulator());
		this.initialState = new ColdHotMultiRoundTrip<N>(initialState, clone);
		return this;
	}

	@Override
	public ColdHotStateAdapter<N> addPrior(MHWeight<MultiRoundTrip<N>> weight) {
		this.weights.add(new ColdHotWeightWrapper<N>(weight, 1.0), 1.0);
		return this;
	}

	@Override
	public ColdHotStateAdapter<N> addWeight(MHWeight<MultiRoundTrip<N>> weight, double scale) {
		if (weight instanceof BundledSingleToMultiWeight) {
			throw new RuntimeException("Cannot handle " + BundledSingleToMultiWeight.class + "; call Runner.setBundleSingleWeights(false).");
		}
		this.weights.add(new ColdHotWeightWrapper<N>(weight, this.relativeHotScale), scale);
		return this;
	}
}
