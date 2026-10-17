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
package se.vti.roundtrips.runner;

import java.util.ArrayList;
import java.util.List;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.scenario.Scenario;
import se.vti.utils.misc.SpecificationChecker;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;
import se.vti.utils.misc.metropolishastings.MHWeight;
import se.vti.utils.misc.metropolishastings.MHWeightContainer;
import se.vti.utils.misc.metropolishastings.terminationcriteria.TerminationCriterion;

/**
 * @author GunnarF
 * 
 */
public abstract class AbstractStateAdapter<N extends Node, R extends MultiRoundTripWrapper<N>> {

	// -------------------- WRAPPER CLASSES --------------------

	protected class StateProcessorWrapper<P extends MHStateProcessor<MultiRoundTrip<N>>> implements MHStateProcessor<R> {

		protected final P delegate;

		public StateProcessorWrapper(P delegate) {
			this.delegate = delegate;
		}

		@Override
		public void start() {
			this.delegate.start();
		}

		@Override
		public void processState(R roundTrips, double logWeight) {
			this.delegate.processState(roundTrips.getMultiRoundTrip(), logWeight);
		}

		@Override
		public void end() {
			this.delegate.end();
		}

	}

	protected class TerminationCriterionWrapper extends StateProcessorWrapper<TerminationCriterion<MultiRoundTrip<N>>>
			implements TerminationCriterion<R> {

		public TerminationCriterionWrapper(TerminationCriterion<MultiRoundTrip<N>> delegate) {
			super(delegate);
		}

		@Override
		public boolean terminate() {
			return this.delegate.terminate();
		}
	}

	// -------------------- MEMBERS --------------------

	protected final Scenario<N> scenario;

	protected R initialState = null;

	protected MHProposal<R> proposal = null;

	protected MHWeightContainer<R> weights = new MHWeightContainer<>();

	private List<MHStateProcessor<R>> stateProcessors = new ArrayList<>();

	private TerminationCriterion<R> terminationCriterion = null;

	// -------------------- CONSTRUCTION --------------------

	protected AbstractStateAdapter(Scenario<N> scenario) {
		this.scenario = scenario;
	}

	// --------------- MultiRoundTrip-SPECIFIC FUNCTIONALITY ---------------

	protected abstract AbstractStateAdapter<N, R> setInitialState(MultiRoundTrip<N> initialState);

	protected abstract AbstractStateAdapter<N, R> addWeight(MHWeight<MultiRoundTrip<N>> weight, double scale);

	protected AbstractStateAdapter<N, R> addPrior(MHWeight<MultiRoundTrip<N>> weight) {
		this.addWeight(weight, 1.0);
		return this;
	}

	protected AbstractStateAdapter<N, R> addStateProcessor(MHStateProcessor<MultiRoundTrip<N>> stateProcessor) {
		this.stateProcessors.add(new StateProcessorWrapper<>(stateProcessor));
		return this;
	}

	protected AbstractStateAdapter<N, R> setTerminationCriterion(
			TerminationCriterion<MultiRoundTrip<N>> terminationCriterion) {
		this.terminationCriterion = new TerminationCriterionWrapper(terminationCriterion);
		return this;
	}

	// --------------- NATIVE (TYPE R)-SPECIFIC FUNCTIONALITY ---------------

	protected AbstractStateAdapter<N, R> addNativeStateProcessor(MHStateProcessor<R> stateProcessor) {
		this.stateProcessors.add(stateProcessor);
		return this;
	}

	// -------------------- RESULT --------------------

	protected void check(SpecificationChecker checker) {
		checker.defineError(() -> (this.scenario == null), "No scenario defined")
				.defineError(() -> (this.initialState == null), "No initial state defined")
				.defineError(() -> (this.weights.getComponentsView().size() == 0), "No weights defined")
				.defineError(() -> (this.terminationCriterion == null), "Undefined termination criterion");
	}

	protected MHAlgorithm<? extends MultiRoundTripWrapper<N>> createAlgorithm() {
		var algo = new MHAlgorithm<R>(this.proposal, this.weights, this.scenario.getRandom());
		algo.setInitialState(this.initialState);
		algo.setTerminationCriterion(this.terminationCriterion);
		for (MHStateProcessor<R> stateProcessor : this.stateProcessors) {
			algo.addStateProcessor(stateProcessor);
		}
		return algo;
	}
}
