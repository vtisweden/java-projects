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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.roundtrips.samplingweights.priors.IndividualBinomialPrior;
import se.vti.roundtrips.samplingweights.priors.PopulationBinomialPrior;
import se.vti.roundtrips.samplingweights.priors.Prior;
import se.vti.roundtrips.samplingweights.priors.SingleRoundTripUniformPrior;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHBatchBasedStatisticEstimator;
import se.vti.utils.misc.metropolishastings.MHSampleLogger;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;
import se.vti.utils.misc.metropolishastings.MHStatisticsToFileLogger;
import se.vti.utils.misc.metropolishastings.MHWeight;
import se.vti.utils.misc.metropolishastings.MHWeightContainer;
import se.vti.utils.misc.metropolishastings.MHWeightsToFileLogger;

/**
 * @author GunnarF
 */
public class Runner<N extends Node> {

	// -------------------- MEMBERS --------------------

	private final Scenario<N> scenario;

	private final MHWeightContainer<MultiRoundTrip<N>> weights = new MHWeightContainer<>();
	private MHWeight<MultiRoundTrip<N>> prior = null;

	private List<MHBatchBasedStatisticEstimator<MultiRoundTrip<N>>> statisticEstimators = new ArrayList<>();
	private Map<String, Function<MultiRoundTrip<N>, Double>> sampleExtractors = new LinkedHashMap<>();
	private List<MHStateProcessor<MultiRoundTrip<N>>> stateProcessors = new ArrayList<>();
	private List<Hook> postRunHooks = new ArrayList<>();
	private List<String[]> postRunHookArgs = new ArrayList<>();

	private long weightsLogInterval = 1000l;
	private String weightsLogFile = "./logWeights.log";

	private long sampleLogInterval = 1000l;
	private String samplesLogFile = "./samples.log";

	private long statisticsLogInterval = 1000l;
	private String statisticsLogFile = "./statistics.log";

	private MultiRoundTrip<N> initialState = null;
	private Long numberOfIterations = null;
	private long messageInterval = 1000l;

	private boolean runWasAlreadyCalled = false;

	// -------------------- CONSTRUCTION --------------------

	public Runner(Scenario<N> scenario) {
		this.scenario = scenario;
		this.setUniformPrior();
	}

	// PRIORS

	public <P extends Prior & MHWeight<MultiRoundTrip<N>>> Runner<N> setPopulationPrior(P prior) {
		this.prior = prior;
		return this;
	}

	public <P extends Prior & MHWeight<RoundTrip<N>>> Runner<N> setIndividualPrior(P prior) {
		this.prior = new SingleToMultiWeight<>(prior);
		return this;
	}

	public Runner<N> setUniformPrior() {
		return this.setIndividualPrior(new SingleRoundTripUniformPrior<>(this.scenario));
	}

	public Runner<N> setIndividualBinomialPrior(double meanRoundTripSize) {
		return this.setIndividualPrior(new IndividualBinomialPrior<>(this.scenario, meanRoundTripSize));
	}

	public Runner<N> setPopulationBinomialPrior(double meanRoundTripSize) {
		return this.setPopulationPrior(new PopulationBinomialPrior<>(this.scenario, meanRoundTripSize));
	}

	// SAMPLING WEIGHTS

	private void checkForNotPrior(MHWeight<?> weight) {
		if (weight instanceof Prior) {
			throw new RuntimeException(weight.getClass().getSimpleName() + " implements " + Prior.class.getSimpleName()
					+ ". Cannot be added as a sampling weight, must be set as a prior.");
		}
	}

	public Runner<N> addPopulationWeight(MHWeight<MultiRoundTrip<N>> weight, double factor) {
		this.checkForNotPrior(weight);
		this.weights.add(weight, factor);
		return this;
	}

	public Runner<N> addPopulationWeight(MHWeight<MultiRoundTrip<N>> weight) {
		return this.addPopulationWeight(weight, 1.0);
	}

	public Runner<N> addIndividualWeight(MHWeight<RoundTrip<N>> weight, double factor) {
		this.checkForNotPrior(weight);
		this.weights.add(new SingleToMultiWeight<>(weight), factor);
		return this;
	}

	public Runner<N> addIndividualWeight(MHWeight<RoundTrip<N>> weight) {
		return this.addIndividualWeight(weight, 1.0);
	}

	// STATISTICS

	public Runner<N> addStatisticEstimator(MHBatchBasedStatisticEstimator<MultiRoundTrip<N>> statisticEstimator) {
		this.statisticEstimators.add(statisticEstimator);
		return this;
	}

	public Runner<N> addSampleExtractor(String name, Function<MultiRoundTrip<N>, Double> function) {
		this.sampleExtractors.put(name, function);
		return this;
	}

	// OTHER

	public Runner<N> addStateProcessor(MHStateProcessor<MultiRoundTrip<N>> processor) {
		this.stateProcessors.add(processor);
		return this;
	}

	public Runner<N> addPostRunHook(Hook hook, String... args) {
		this.postRunHooks.add(hook); 
		this.postRunHookArgs.add(args == null ? new String[0] : args);
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

	public Runner<N> configureWeightLogging(String fileName, long logInterval) {
		this.weightsLogFile = fileName;
		this.weightsLogInterval = logInterval;
		return this;
	}

	public Runner<N> configureStatisticsLogging(String fileName, long logInterval) {
		this.statisticsLogFile = fileName;
		this.statisticsLogInterval = logInterval;
		return this;
	}

	// -------------------- RUNNING --------------------

	public void run() {

		if (this.runWasAlreadyCalled) {
			throw new RuntimeException("Run was already called.");
		}
		this.runWasAlreadyCalled = true;

		var checker = new SpecificationChecker().defineError(() -> (this.prior == null), "No prior defined")
				.defineError(() -> (this.initialState == null), "No initial state defined")
				.defineError(() -> (this.numberOfIterations == null), "Undefined parameter: numberOfIterations")
				.defineError(() -> (this.numberOfIterations != null && this.numberOfIterations < 1),
						"numberOfIterations smaller than one");

		if (checker.check()) {
			this.weights.add(this.prior);
			var algo = new MHAlgorithm<MultiRoundTrip<N>>(new MultiRoundTripProposal<N>(this.scenario), this.weights,
					this.scenario.getRandom());
			if (this.weightsLogFile != null) {
				this.stateProcessors
						.add(new MHWeightsToFileLogger<>(this.weightsLogInterval, this.weights, this.weightsLogFile));
			}
			if ((this.statisticsLogFile != null) && (this.statisticEstimators.size() > 0)) {
				var statisticsLogger = new MHStatisticsToFileLogger<MultiRoundTrip<N>>(this.statisticsLogInterval,
						this.statisticsLogFile);
				algo.addStateProcessor(statisticsLogger);
				for (var statisticEstimator : this.statisticEstimators) {
					statisticsLogger.add(statisticEstimator);
					algo.addStateProcessor(statisticEstimator);
				}
			}

			if ((this.samplesLogFile != null) && (this.sampleExtractors.size() > 0)) {
				var samplesLogger = new MHSampleLogger<MultiRoundTrip<N>>(this.sampleLogInterval, this.samplesLogFile);
				algo.addStateProcessor(samplesLogger);
				for (var entry : this.sampleExtractors.entrySet()) {
					samplesLogger.add(entry.getKey(), entry.getValue());
				}
			}

			this.stateProcessors.stream().forEach(sp -> algo.addStateProcessor(sp));
			algo.setInitialState(this.initialState);
			algo.setMsgInterval(this.messageInterval);
			algo.run(this.numberOfIterations);

			for (int ii = 0; ii < this.postRunHooks.size(); ii++) {
				try {
					this.postRunHooks.get(ii).run(this.postRunHookArgs.get(ii));
				} catch (Exception e) {
					System.out.println(String.format("Post-run hook %d failed: %s", ii, e.getMessage(), e));
				}
			}
						
		} else {
			throw new RuntimeException(checker.getRecentErrors());
		}
	}
}
