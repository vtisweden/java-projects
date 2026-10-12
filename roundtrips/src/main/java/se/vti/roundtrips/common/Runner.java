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

import se.vti.roundtrips.multiple.MultiRoundTripJsonIO;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.roundtrips.multiple.coldhot.ColdHotMultiRoundTripProposal;
import se.vti.roundtrips.multiple.coldhot.ColdHotPrior;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.roundtrips.samplingweights.priors.IndividualBinomialPrior;
import se.vti.roundtrips.samplingweights.priors.IndividualUniformPrior;
import se.vti.roundtrips.samplingweights.priors.PopulationBinomialPrior;
import se.vti.roundtrips.samplingweights.priors.PopulationUniformPrior;
import se.vti.roundtrips.samplingweights.priors.Prior;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripMultiStepProposal;
import se.vti.roundtrips.single.RoundTripSingleStepProposal;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHSampleLogger;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;
import se.vti.utils.misc.metropolishastings.MHWeight;
import se.vti.utils.misc.metropolishastings.MHWeightContainer;
import se.vti.utils.misc.metropolishastings.MHWeightsToFileLogger;
import se.vti.utils.misc.metropolishastings.terminationcriteria.FixedNumberOfIterationsTerminationCriterion;
import se.vti.utils.misc.metropolishastings.terminationcriteria.TerminationCriterion;

/**
 * @author GunnarF
 */
public class Runner<N extends Node> {

	// -------------------- MEMBERS --------------------

	private final Scenario<N> scenario;

	private final MHWeightContainer<RoundTrips<N>> weights = new MHWeightContainer<>();
	private MHWeight<RoundTrips<N>> prior = null;

	private Map<String, Function<RoundTrips<N>, Double>> sampleExtractors = new LinkedHashMap<>();
	private List<MHStateProcessor<RoundTrips<N>>> stateProcessors = new ArrayList<>();

	private long weightsLogInterval = 1000l;
	private String weightsLogFile = "./logWeights.log";

	private long sampleLogInterval = 1000l;
	private String samplesLogFile = "./samples.log";

	private long stateDumpInterval = 0;
	private String stateDumpFilePrefix = null;

	private double expectedColdProposalLength = 1.0;
	private double expectedHotProposalLength = 1.0;
	private double coldHotRecombineProba = 0.0;

	private RoundTrips<N> initialState = null;
	private TerminationCriterion<RoundTrips<N>> terminationCriterion = null;
	private long messageInterval = 1000l;

	private boolean runWasAlreadyCalled = false;

	// -------------------- CONSTRUCTION --------------------

	public Runner(Scenario<N> scenario) {
		this.scenario = scenario;
		this.setIndividualUniformPrior();
	}

	// PRIORS

	public <P extends Prior & MHWeight<RoundTrip<N>>> Runner<N> setIndividualPrior(P prior) {
		this.prior = new SingleToMultiWeight<>(prior);
		return this;
	}

	public Runner<N> setIndividualUniformPrior() {
		return this.setIndividualPrior(new IndividualUniformPrior<>(this.scenario));
	}

	public Runner<N> setIndividualBinomialPrior(double meanRoundTripSize) {
		return this.setIndividualPrior(new IndividualBinomialPrior<>(this.scenario, meanRoundTripSize));
	}

	public <P extends Prior & MHWeight<RoundTrips<N>>> Runner<N> setPopulationPrior(P prior) {
		this.prior = prior;
		return this;
	}

	public Runner<N> setPopulationUniformPrior() {
		return this.setPopulationPrior(new PopulationUniformPrior<>(this.scenario));
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

	public Runner<N> addPopulationWeight(MHWeight<RoundTrips<N>> weight, double factor) {
		this.checkForNotPrior(weight);
		this.weights.add(weight, factor);
		return this;
	}

	public Runner<N> addPopulationWeight(MHWeight<RoundTrips<N>> weight) {
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

	public Runner<N> addSampleExtractor(String name, Function<RoundTrips<N>, Double> function) {
		this.sampleExtractors.put(name, function);
		return this;
	}

	// OTHER

	public Runner<N> addStateProcessor(MHStateProcessor<RoundTrips<N>> processor) {
		this.stateProcessors.add(processor);
		return this;
	}

	public Runner<N> setExpectedColdProposalLength(double expectedColdProposalLength) {
		if (expectedColdProposalLength < 1.0) {
			throw new IllegalArgumentException("expectedColdProposalLength must not be smaller than one.");
		}
		this.expectedColdProposalLength = expectedColdProposalLength;
		return this;
	}

	public Runner<N> setColdHotParameters(double expectedHotProposalLength, double coldHotRecombineProba) {
		if (expectedHotProposalLength < 1.0) {
			throw new IllegalArgumentException(
					"expectedHotProposalLength must not be smaller than one but is " + expectedHotProposalLength + ".");
		}
		if (coldHotRecombineProba < 0.0 || coldHotRecombineProba >= 1.0) {
			throw new IllegalArgumentException(
					"coldHotRecombineProba must be in [0,1) but is " + coldHotRecombineProba + ".");
		}
		this.expectedHotProposalLength = expectedHotProposalLength;
		this.coldHotRecombineProba = coldHotRecombineProba;
		return this;
	}

	public Runner<N> setInitialState(RoundTrips<N> initialState) {
		this.initialState = initialState;
		return this;
	}

	public Runner<N> setTerminationCriterion(TerminationCriterion<RoundTrips<N>> criterion) {
		this.terminationCriterion = criterion;
		return this;
	}

	public Runner<N> setNumberOfIterations(long numberOfIterations) {
		return this.setTerminationCriterion(
				new FixedNumberOfIterationsTerminationCriterion<RoundTrips<N>>(numberOfIterations));
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

	public Runner<N> configureStateDumping(String fileNamePrefix, long dumpInterval) {
		this.stateDumpInterval = dumpInterval;
		this.stateDumpFilePrefix = fileNamePrefix;
		return this;
	}

	// -------------------- RUNNING --------------------

	public List<MHWeight<RoundTrips<N>>> getSamplingWeightsView() {
		return this.weights.getComponentsView();
	}

	public List<Double> getWeightsOfSamplingWeightsView() {
		return this.weights.getWeightsView();
	}

	private RoundTrips<N> finalState = null;

	public RoundTrips<N> getFinalState() {
		return this.finalState;
	}

	public void run() {

		if (this.runWasAlreadyCalled) {
			throw new RuntimeException("Run was already called.");
		}
		this.runWasAlreadyCalled = true;

		var checker = new SpecificationChecker().defineError(() -> (this.prior == null), "No prior defined")
				.defineError(() -> (this.initialState == null), "No initial state defined")
				.defineError(() -> (this.terminationCriterion == null), "Undefined termination criterion");

		if (checker.check()) {

			final MHProposal<RoundTrip<N>> coldProposal;
			if (this.expectedColdProposalLength == 1.0) {
				coldProposal = new RoundTripSingleStepProposal<N>(this.scenario);
			} else {
				coldProposal = new RoundTripMultiStepProposal<N>(this.scenario, this.expectedColdProposalLength);
			}

			final MHAlgorithm<RoundTrips<N>> algo;
			if ((this.expectedHotProposalLength == 1.0) && (this.coldHotRecombineProba == 0.0)) {

				this.weights.add(this.prior);
				algo = new MHAlgorithm<RoundTrips<N>>(new MultiRoundTripProposal<N>(this.scenario, coldProposal),
						this.weights, this.scenario.getRandom());

			} else {

				final MHProposal<RoundTrip<N>> hotProposal;
				if (this.expectedHotProposalLength == 1.0) {
					hotProposal = new RoundTripSingleStepProposal<N>(this.scenario);
				} else {
					hotProposal = new RoundTripMultiStepProposal<N>(this.scenario, this.expectedHotProposalLength);
				}
				this.weights.add(new ColdHotPrior<N>(this.prior));
				algo = new MHAlgorithm<RoundTrips<N>>(new ColdHotMultiRoundTripProposal<N>(this.scenario, coldProposal,
						hotProposal, this.coldHotRecombineProba), this.weights, this.scenario.getRandom());

			}

			if ((this.samplesLogFile != null) && (this.sampleExtractors.size() > 0)) {
				var samplesLogger = new MHSampleLogger<RoundTrips<N>>(this.sampleLogInterval, this.samplesLogFile);
				this.stateProcessors.add(samplesLogger);
				for (var entry : this.sampleExtractors.entrySet()) {
					samplesLogger.add(entry.getKey(), entry.getValue());
				}
			}

			if ((this.weightsLogFile != null) && (this.weightsLogInterval > 0)) {
				this.stateProcessors
						.add(new MHWeightsToFileLogger<>(this.weightsLogInterval, this.weights, this.weightsLogFile));
			}

			if ((this.stateDumpFilePrefix != null) && (this.stateDumpInterval > 0)) {
				this.stateProcessors.add(new MHStateProcessor<RoundTrips<N>>() {
					int iteration;

					@Override
					public void start() {
						this.iteration = 0;
					}

					@Override
					public void processState(RoundTrips<N> state, double logWeight) {
						if (this.iteration % stateDumpInterval == 0) {
							try {
								MultiRoundTripJsonIO.singleton().writeToFile(state.getMultiRoundTrip(),
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

			this.stateProcessors.stream().forEach(sp -> algo.addStateProcessor(sp));
			
			algo.setMsgInterval(this.messageInterval);
			algo.setTerminationCriterion(this.terminationCriterion);
			algo.setInitialState(this.initialState);
			
			algo.run();
			this.finalState = algo.getFinalState();
		} else {
			throw new RuntimeException(checker.getRecentErrors());
		}
	}
}
