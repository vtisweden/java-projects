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
package se.vti.roundtrips.runner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.multi.MultiRoundTripJsonIO;
import se.vti.roundtrips.priors.IndividualBinomialPrior;
import se.vti.roundtrips.priors.IndividualUniformPrior;
import se.vti.roundtrips.priors.PopulationBinomialPrior;
import se.vti.roundtrips.priors.PopulationUniformPrior;
import se.vti.roundtrips.runner.basic.BasicStateAdapter;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.scenario.Scenario;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.SpecificationChecker;
import se.vti.utils.misc.metropolishastings.MHSampleLogger;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;
import se.vti.utils.misc.metropolishastings.MHWeight;
import se.vti.utils.misc.metropolishastings.MHWeightsToFileLogger;
import se.vti.utils.misc.metropolishastings.terminationcriteria.FixedNumberOfIterationsTerminationCriterion;
import se.vti.utils.misc.metropolishastings.terminationcriteria.TerminationCriterion;

/**
 * @author GunnarF
 * 
 * @param <N> node type
 */
public class Runner<N extends Node> {

	// -------------------- MEMBERS --------------------

	private final AbstractStateAdapter<N, ? extends MultiRoundTripWrapper<N>> stateAdapter;

	private final Scenario<N> scenario;

	private Map<String, Function<MultiRoundTrip<N>, Double>> sampleExtractors = new LinkedHashMap<>();

	private final MHWeightsToFileLogger<MultiRoundTrip<N>> weightsToFileLogger = new MHWeightsToFileLogger<MultiRoundTrip<N>>(
			10_000l, "./logWeights.log");

	private long sampleLogInterval = 1000l;
	private String samplesLogFile = "./samples.log";

	private long stateDumpInterval = 0;
	private String stateDumpFilePrefix = null;

	private long messageInterval = 1000l;

	private boolean priorAlreadyAdded = false;
	
	private boolean runWasAlreadyCalled = false;

	// -------------------- CONSTRUCTION --------------------

	public Runner(Scenario<N> scenario, AbstractStateAdapter<N, ? extends MultiRoundTripWrapper<N>> stateAdapter) {
		this.scenario = scenario;
		this.stateAdapter = stateAdapter;
	}

	public Runner(Scenario<N> scenario) {
		this(scenario, BasicStateAdapter.createDefaultInstance(scenario, 1.0));
	}

	// PASS-THROUGH TO STATE ADAPTER

	public Runner<N> setInitialState(MultiRoundTrip<N> initialState) {
		initialState.simulateAll(this.scenario.getOrCreateSimulator());
		this.stateAdapter.setInitialState(initialState);
		return this;
	}

	// PRIORS

	public Runner<N> setIndividualPrior(MHWeight<RoundTrip<N>> prior) {
		return this.setPopulationPrior(new SingleToMultiWeight<N>(prior));
	}

	public Runner<N> setIndividualUniformPrior() {
		return this.setIndividualPrior(new IndividualUniformPrior<>(this.scenario));
	}

	public Runner<N> setIndividualBinomialPrior(double meanRoundTripSize) {
		return this.setIndividualPrior(new IndividualBinomialPrior<>(this.scenario, meanRoundTripSize));
	}

	public Runner<N> setPopulationPrior(MHWeight<MultiRoundTrip<N>> prior) {
		
		if (this.priorAlreadyAdded) {
			throw new RuntimeException("Cannot add more than one prior.");
		} 
		this.priorAlreadyAdded = true;
		
		this.stateAdapter.addPrior(prior);
		this.weightsToFileLogger.addWeight(prior);
		return this;
	}

	public Runner<N> setPopulationUniformPrior() {
		return this.setPopulationPrior(new PopulationUniformPrior<>(this.scenario));
	}

	public Runner<N> setPopulationBinomialPrior(double meanRoundTripSize) {
		return this.setPopulationPrior(new PopulationBinomialPrior<>(this.scenario, meanRoundTripSize));
	}

	// SAMPLING WEIGHTS

	public Runner<N> addPopulationWeight(MHWeight<MultiRoundTrip<N>> weight, double scale) {
		this.stateAdapter.addWeight(weight, scale);
		this.weightsToFileLogger.addWeight(weight);
		return this;
	}

	public Runner<N> addPopulationWeight(MHWeight<MultiRoundTrip<N>> weight) {
		return this.addPopulationWeight(weight, 1.0);
	}

	public Runner<N> addIndividualWeight(MHWeight<RoundTrip<N>> weight, double factor) {
		this.addPopulationWeight(new SingleToMultiWeight<>(weight), factor);
		return this;
	}

	public Runner<N> addIndividualWeight(MHWeight<RoundTrip<N>> weight) {
		return this.addIndividualWeight(weight, 1.0);
	}

	// STATISTICS

	public Runner<N> addSampleExtractor(String name, Function<MultiRoundTrip<N>, Double> function) {
		this.sampleExtractors.put(name, function);
		return this;
	}

	// OTHER

	public Runner<N> addStateProcessor(MHStateProcessor<MultiRoundTrip<N>> processor) {
		this.stateAdapter.addStateProcessor(processor);
		return this;
	}

	public Runner<N> setTerminationCriterion(TerminationCriterion<MultiRoundTrip<N>> criterion) {
		this.stateAdapter.setTerminationCriterion(criterion);
		return this;
	}

	public Runner<N> setNumberOfIterations(long numberOfIterations) {
		return this.setTerminationCriterion(
				new FixedNumberOfIterationsTerminationCriterion<MultiRoundTrip<N>>(numberOfIterations));
	}

	public Runner<N> setMessageInterval(long messageInterval) {
		this.messageInterval = messageInterval;
		return this;
	}

	public Runner<N> configureWeightLogging(String fileName, long logInterval) {
		this.weightsToFileLogger.setLogFilename(fileName).setSamplingInterval(logInterval);
		return this;
	}

	public Runner<N> configureStateDumping(String fileNamePrefix, long dumpInterval) {
		this.stateDumpInterval = dumpInterval;
		this.stateDumpFilePrefix = fileNamePrefix;
		return this;
	}

	// -------------------- INTERNALS --------------------

	private void completeStateProcessors() {
		
		if ((this.samplesLogFile != null) && (this.sampleExtractors.size() > 0)) {
			var samplesLogger = new MHSampleLogger<MultiRoundTrip<N>>(this.sampleLogInterval, this.samplesLogFile);
			for (var entry : this.sampleExtractors.entrySet()) {
				samplesLogger.add(entry.getKey(), rts -> entry.getValue().apply(rts));
			}
			this.stateAdapter.addStateProcessor(samplesLogger);
		}

		this.stateAdapter.addStateProcessor(this.weightsToFileLogger);

		if ((this.stateDumpFilePrefix != null) && (this.stateDumpInterval > 0)) {
			this.stateAdapter.addStateProcessor(new MHStateProcessor<MultiRoundTrip<N>>() {
				int iteration;

				@Override
				public void start() {
					this.iteration = 0;
				}

				@Override
				public void processState(MultiRoundTrip<N> state, double logWeight) {
					if (this.iteration % stateDumpInterval == 0) {
						try {
							MultiRoundTripJsonIO.singleton().writeToFile(state,
									stateDumpFilePrefix + "." + this.iteration + ".json");
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
					this.iteration++;
				}

				@Override
				public void end() {
				}
			});
		}
	}

	// -------------------- RUNNING --------------------

	private MultiRoundTrip<N> finalState = null;

	public MultiRoundTrip<N> getFinalState() {
		return this.finalState;
	}

	public void run() {

		if (this.runWasAlreadyCalled) {
			throw new RuntimeException("Run was already called.");
		}
		this.runWasAlreadyCalled = true;

		this.completeStateProcessors();

		var checker = new SpecificationChecker();
		this.stateAdapter.check(checker);

		if (checker.check()) {

			var algo = this.stateAdapter.createAlgorithm();
			algo.setMsgInterval(this.messageInterval);
			algo.run();
			this.finalState = algo.getFinalState().getMultiRoundTrip();

		} else {
			throw new RuntimeException(checker.getRecentErrors());
		}
	}
}
