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
import se.vti.roundtrips.multi.MultiRoundTripJsonLogger;
import se.vti.roundtrips.multi.MultiRoundTripSummary;
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

	private boolean bundleSingleWeights = true;
	private final BundledSingleToMultiWeight<N> bundledSingleWeights = new BundledSingleToMultiWeight<>();

	private MultiRoundTrip<N> initialState = null;
	private final Map<Class<MultiRoundTripSummary<N>>, MultiRoundTripSummary<N>> summaries = new LinkedHashMap<>();

	private MHSampleLogger<MultiRoundTrip<N>> samplesLogger = new MHSampleLogger<MultiRoundTrip<N>>(10_000,
			"./samples.log");

	private MHWeightsToFileLogger<MultiRoundTrip<N>> weightsToFileLogger = new MHWeightsToFileLogger<MultiRoundTrip<N>>(
			10_000l, "./logWeights.log");

	private MultiRoundTripJsonLogger<N> stateLogger = null;

	private long messageInterval = 10_000l;

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

	// -------------------- CONFIGURATION --------------------

	public Runner<N> setBundleSingleWeights(boolean bundle) {
		this.bundleSingleWeights = bundle;
		return this;
	}

	public Runner<N> setInitialState(MultiRoundTrip<N> initialState) {
		this.initialState = initialState;
		return this;
	}

	public Runner<N> addSummary(MultiRoundTripSummary<N> summary) {
		this.summaries.put((Class<MultiRoundTripSummary<N>>) summary.getClass(), summary);
		return this;
	}

	public Runner<N> setTerminationCriterion(TerminationCriterion<MultiRoundTrip<N>> criterion) {
		this.stateAdapter.setTerminationCriterion(criterion);
		return this;
	}

	public Runner<N> addStateProcessor(MHStateProcessor<MultiRoundTrip<N>> processor) {
		this.stateAdapter.addStateProcessor(processor);
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

	// POULATION PRIORS

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

	// INDIVIDUAL PRIORS

	public Runner<N> setIndividualPrior(MHWeight<RoundTrip<N>> prior) {
		if (this.bundleSingleWeights) {
			this.bundledSingleWeights.add(prior, 1.0);
			return this;
		} else {
			return this.setPopulationPrior(new SingleToMultiWeight<N>(prior));
		}
	}

	public Runner<N> setIndividualUniformPrior() {
		return this.setIndividualPrior(new IndividualUniformPrior<>(this.scenario));
	}

	public Runner<N> setIndividualBinomialPrior(double meanRoundTripSize) {
		return this.setIndividualPrior(new IndividualBinomialPrior<>(this.scenario, meanRoundTripSize));
	}

	// POPULATION WEIGHTS

	public Runner<N> addPopulationWeight(MHWeight<MultiRoundTrip<N>> weight, double scale) {
		this.stateAdapter.addWeight(weight, scale);
		this.weightsToFileLogger.addWeight(weight);
		return this;
	}

	public Runner<N> addPopulationWeight(MHWeight<MultiRoundTrip<N>> weight) {
		return this.addPopulationWeight(weight, 1.0);
	}

	// INDIVIDUAL WEIGHTS

	public Runner<N> addIndividualWeight(MHWeight<RoundTrip<N>> weight, double factor) {
		if (this.bundleSingleWeights) {
			this.bundledSingleWeights.add(weight, factor);
			return this;
		} else {
			return this.addPopulationWeight(new SingleToMultiWeight<>(weight), factor);
		}
	}

	public Runner<N> addIndividualWeight(MHWeight<RoundTrip<N>> weight) {
		return this.addIndividualWeight(weight, 1.0);
	}

	// LOGGING

	private boolean doNotLog(String fileName, long logInterval) {
		return ((fileName == null) || (logInterval <= 0));
	}

	public Runner<N> configureWeightLogging(String fileName, long logInterval) {
		if (this.doNotLog(fileName, logInterval)) {
			this.weightsToFileLogger = null;
		} else {
			this.weightsToFileLogger.setLogFilename(fileName).setSamplingInterval(logInterval);
		}
		return this;
	}

	public Runner<N> configureStateLogging(String fileNamePrefix, long logInterval) {
		if (this.doNotLog(fileNamePrefix, logInterval)) {
			this.stateLogger = null;
		} else {
			this.stateLogger = new MultiRoundTripJsonLogger<N>(logInterval, fileNamePrefix);
		}
		return this;
	}

	public Runner<N> configureSampleLogging(String fileName, long logInterval) {
		if (this.doNotLog(fileName, logInterval)) {
			this.samplesLogger = null;
		} else {
			this.samplesLogger.setLogFilename(fileName).setSamplingInterval(logInterval);
		}
		return this;
	}

	public Runner<N> addSampleExtractor(String name, Function<MultiRoundTrip<N>, Double> function) {
		this.samplesLogger.add(name, function);
		return this;
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

		if ((this.samplesLogger) != null && (this.samplesLogger.getName2Extractor().size() > 0)) {
			this.stateAdapter.addStateProcessor(this.samplesLogger);
		}
		if (this.weightsToFileLogger != null) {
			this.stateAdapter.addStateProcessor(this.weightsToFileLogger);
		}
		if (this.stateLogger != null) {
			this.stateAdapter.addStateProcessor(this.stateLogger);
		}

		if (this.bundleSingleWeights) {
			this.stateAdapter.addWeight(this.bundledSingleWeights, 1.0);
		}

		this.initialState.simulateAll(this.scenario.getOrCreateSimulator());
		for (var summary : this.summaries.values()) {
			this.initialState.addSummary(summary);
		}
		this.initialState.recomputeSummaries();
		this.stateAdapter.setInitialState(this.initialState);

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
