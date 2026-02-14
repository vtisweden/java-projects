/**
 * se.vti.roundtrips.common
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
package se.vti.roundtrips.common;

import java.util.ArrayList;
import java.util.List;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.roundtrips.samplingweights.priors.MultiRoundTripBinomialPrior;
import se.vti.roundtrips.samplingweights.priors.SingleRoundTripBinomialPrior;
import se.vti.roundtrips.samplingweights.priors.SingleRoundTripUniformPrior;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;
import se.vti.utils.misc.metropolishastings.MHWeight;
import se.vti.utils.misc.metropolishastings.MHWeightContainer;
import se.vti.utils.misc.metropolishastings.MHWeightsToFileLogger;

/**
 * @author GunnarF
 */
public class Runner<N extends Node> {

	private final Scenario<N> scenario;

	private final MHWeightContainer<MultiRoundTrip<N>> weights = new MHWeightContainer<>();

	private MHWeight<MultiRoundTrip<N>> prior;

	private List<MHStateProcessor<MultiRoundTrip<N>>> stateProcessors = new ArrayList<>();

	private String weightsLogFile = "./samplingLogWeights.log";

	private MultiRoundTrip<N> initialState = null;

	private Long numberOfIterations = null;

	private long weightsLogInterval = 1000l;

	private long messageInterval = 1000l;

	private boolean runWasAlreadyCalled = false;

	public Runner(Scenario<N> scenario) {
		this.scenario = scenario;
		this.setUniformPrior();
	}

	public Runner<N> setUniformPrior() {
		this.prior = new SingleToMultiWeight<>(new SingleRoundTripUniformPrior<>(this.scenario));
		return this;
	}

	public Runner<N> setIndividualBinomialPrior(double meanRoundTripSize) {
		this.prior = new SingleToMultiWeight<>(new SingleRoundTripBinomialPrior<>(this.scenario, meanRoundTripSize));
		return this;
	}

	public Runner<N> setPopulationBinomialPrior(double meanRoundTripSize) {
		this.prior = new MultiRoundTripBinomialPrior<>(this.scenario, meanRoundTripSize);
		return this;
	}

	public Runner<N> addWeight(MHWeight<MultiRoundTrip<N>> weight, double factor) {
		this.weights.add(weight, factor);
		return this;
	}

	public Runner<N> addWeight(MHWeight<MultiRoundTrip<N>> weight) {
		return this.addWeight(weight, 1.0);
	}

	public Runner<N> addSingleWeight(MHWeight<RoundTrip<N>> weight, double factor) {
		this.weights.add(new SingleToMultiWeight<>(weight), factor);
		return this;
	}

	public Runner<N> addSingleWeight(MHWeight<RoundTrip<N>> weight) {
		return this.addSingleWeight(weight, 1.0);
	}

	public Runner<N> addStateProcessor(MHStateProcessor<MultiRoundTrip<N>> processor) {
		this.stateProcessors.add(processor);
		return this;
	}

	public Runner<N> setInitialState(MultiRoundTrip<N> initialState) {
		this.initialState = initialState;
		return this;
	}

	public Runner<N> setNumberOfIterations(long numberOfIterations) {
		this.numberOfIterations = numberOfIterations;
		return this;
	}

	public Runner<N> setMessageInterval(long messageInterval) {
		this.messageInterval = messageInterval;
		return this;
	}

	public Runner<N> configureWeightLogging(String fileName, Long logInterval) {
		this.weightsLogFile = fileName;
		this.weightsLogInterval = logInterval;
		return this;
	}

	public void run() {

		if (this.runWasAlreadyCalled) {
			throw new RuntimeException("Run was already called.");
		}
		this.runWasAlreadyCalled = true;

		var checker = new SpecificationChecker().defineError(() -> (this.prior == null), "No prior defined")
				.defineError(() -> (this.initialState == null), "No initial state defined")
				.defineError(() -> (this.numberOfIterations == null), "Undefined parameter: numberOfIterations");

		if (checker.check()) {
			this.weights.add(this.prior);
			var algo = new MHAlgorithm<MultiRoundTrip<N>>(new MultiRoundTripProposal<N>(this.scenario), this.weights,
					this.scenario.getRandom());
			if (this.weightsLogFile != null) {
				this.stateProcessors
						.add(new MHWeightsToFileLogger<>(this.weightsLogInterval, this.weights, this.weightsLogFile));
			}
			this.stateProcessors.stream().forEach(sp -> algo.addStateProcessor(sp));
			algo.setInitialState(this.initialState);
			algo.setMsgInterval(this.messageInterval);
			algo.run(this.numberOfIterations);
		} else {
			throw new RuntimeException(checker.getRecentErrors());
		}

	}

}
