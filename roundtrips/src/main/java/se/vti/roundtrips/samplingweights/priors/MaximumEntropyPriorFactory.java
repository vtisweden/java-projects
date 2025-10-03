package se.vti.roundtrips.samplingweights.priors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * EXPERIMENTAL!
 * 
 * TODO Chi2Prior needs refinement.
 * 
 * @author GunnarF
 *
 */
public class MaximumEntropyPriorFactory<N extends Node> {

	// -------------------- CONSTANTS --------------------

	private final int nodeCnt;
	private final int timeBinCnt;
	private final int maxRoundTripSize;

	private final SingleRoundTripCombinatorics sizeCombinatorics;

	// -------------------- CONSTRUCTION --------------------

	public MaximumEntropyPriorFactory(int nodeCnt, int timeBinCnt, int maxRoundTripSize) {
		this.nodeCnt = nodeCnt;
		this.timeBinCnt = timeBinCnt;
		this.maxRoundTripSize = Math.min(maxRoundTripSize, timeBinCnt);
		this.sizeCombinatorics = new SingleRoundTripCombinatorics(nodeCnt, timeBinCnt);
	}

	public MaximumEntropyPriorFactory(int nodeCnt, int timeBinCnt) {
		this(nodeCnt, timeBinCnt, timeBinCnt);
	}

	public MaximumEntropyPriorFactory(Scenario<N> scenario) {
		this(scenario.getNodesCnt(), scenario.getTimeBinCnt(), scenario.getMaxPossibleStayEpisodes());
	}

	// -------------------- INTERNALS --------------------

	private double[] createSingleLogWeights(double meanRoundTripSize, boolean correctForCombinatorics) {
		assert (meanRoundTripSize >= 0);
		assert (meanRoundTripSize <= this.maxRoundTripSize);
		final BinomialDistribution binDistr = new BinomialDistribution(this.maxRoundTripSize,
				meanRoundTripSize / this.maxRoundTripSize);
		double[] logWeights = new double[this.maxRoundTripSize + 1];
		for (int j = 0; j < logWeights.length; j++) {
			logWeights[j] = correctForCombinatorics
					? binDistr.logProbability(j) - this.sizeCombinatorics.getLogNumberOfRoundTrips(j)
					: binDistr.logProbability(j);
		}
		return logWeights;
	}

	private double[] probasFromLogWeights(double[] logWeights) {
		final double maxLogWeight = Arrays.stream(logWeights).max().getAsDouble();
		final double[] probas = new double[logWeights.length];
		double probaSum = 0.0;
		for (int j = 0; j < logWeights.length; j++) {
			probas[j] = Math.exp(logWeights[j] - maxLogWeight);
			probaSum += probas[j];
		}
		// No sum check because there was at least one addend equal to exp(0).
		for (int j = 0; j < probas.length; j++) {
			probas[j] /= probaSum;
		}
		return probas;
	}

	// -------------------- SINGLE(S) IMPLEMENTATION --------------------

	public MHWeight<RoundTrip<N>> createSingle(double meanRoundTripSize) {
		return new MHWeight<RoundTrip<N>>() {
			double[] logWeights = createSingleLogWeights(meanRoundTripSize, true);

			@Override
			public double logWeight(RoundTrip<N> roundTrips) {
				return this.logWeights[roundTrips.size()];
			}
		};
	}

	public MHWeight<MultiRoundTrip<N>> createSingles(double meanRoundTripSize) {
		return new SingleToMultiWeight<>(this.createSingle(meanRoundTripSize));
	}

	// -------------------- MULTIPLE IMPLEMENTATION --------------------

	private class Chi2Prior implements MHWeight<MultiRoundTrip<N>> {

		final Double targetMeanSize;

		/*
		 * singleProbasGivenMeanLength[meanSize][j] is the probability of sampling a
		 * single size-j roundtrip given a particular mean round trip size.
		 */
		final double[][] singleProbasGivenMeanSize;

		// Lazy initialization, don't know number of round trips from the beginning.
		ChiSquaredDistribution chi2distr = null;

		// -------------------- CONSTRUCTION --------------------

		Chi2Prior(int nodeCnt, int timeBinCnt, Double targetMeanSize, boolean correctForSampling) {
			this.targetMeanSize = targetMeanSize;
			this.singleProbasGivenMeanSize = new double[maxRoundTripSize + 1][];
			for (int meanSize = 0; meanSize <= maxRoundTripSize; meanSize++) {
				this.singleProbasGivenMeanSize[meanSize] = probasFromLogWeights(
						createSingleLogWeights(meanSize, correctForSampling));
			}
		}

		// -------------------- INTERNALS --------------------

		double getOrComputeTargetSize(MultiRoundTrip<?> roundTrips) {
			if (this.targetMeanSize != null) {
				return this.targetMeanSize;
			} else {
				return StreamSupport.stream(roundTrips.spliterator(), false).mapToDouble(r -> r.size()).average()
						.getAsDouble();
			}
		}

		// -------------------- IMPLEMENTATION --------------------

		int[] computeSizeFrequencies(MultiRoundTrip<N> roundTrips) {
			int[] sizeFrequencies = new int[maxRoundTripSize + 1];
			for (RoundTrip<?> roundTrip : roundTrips) {
				sizeFrequencies[roundTrip.size()]++;
			}
			return sizeFrequencies;
		}

		double computeChi2LogDensity(int[] realizedSizeFrequencies, double targetSize, int numberOfRoundTrips) {
			final int targetSizeFloor = (int) targetSize;
			final double[] lowerSingleProbas = this.singleProbasGivenMeanSize[targetSizeFloor];
			final double[] upperSingleProbas = this.singleProbasGivenMeanSize[Math.min(targetSizeFloor + 1,
					maxRoundTripSize)];
			final double upperInterpolationWeight = targetSize - targetSizeFloor;

			double chi2 = 0.0;
			for (int j = 0; j < realizedSizeFrequencies.length; j++) {
				final double targetSizeProba = (1.0 - upperInterpolationWeight) * lowerSingleProbas[j]
						+ upperInterpolationWeight * upperSingleProbas[j];
				final double targetSizeFrequency = targetSizeProba * numberOfRoundTrips;
				chi2 += Math.pow(realizedSizeFrequencies[j] - targetSizeFrequency, 2.0) / targetSizeFrequency;
			}

			if (this.chi2distr == null) {
				this.chi2distr = new ChiSquaredDistribution(maxRoundTripSize - 1);
			}
			return this.chi2distr.logDensity(chi2) + Math.log(1e-8); // TODO
		}

		@Override
		public double logWeight(MultiRoundTrip<N> roundTrips) {
			// Funktionality split up for testing.
			int[] realizedSizeFrequencies = this.computeSizeFrequencies(roundTrips);
			return this.computeChi2LogDensity(realizedSizeFrequencies, this.getOrComputeTargetSize(roundTrips),
					roundTrips.size());
		}

		@Override
		public String name() {
			return (this.targetMeanSize == null) ? "Chi2Prior(endog.mean)"
					: "Chi2Prior(mean=" + this.targetMeanSize + ")";
		}

	}

	public Chi2Prior createMultiple(Double targetMeanSize) {
		return new Chi2Prior(this.nodeCnt, this.timeBinCnt, targetMeanSize, true);
	}

	public MHWeight<MultiRoundTrip<N>> createMultiple() {
		return this.createMultiple(null);
	}

	Chi2Prior createMultiple(double targetMeanSize, boolean correctForSampling) {
		return new Chi2Prior(this.nodeCnt, this.timeBinCnt, targetMeanSize, correctForSampling);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	static void testSingle() {
		var factory = new MaximumEntropyPriorFactory<>(10, 24);
		for (int targetSize = 0; targetSize <= 24; targetSize++) {
			double adjustedTargetSize = Math.max(1e-3, Math.min(24 - 1e-3, targetSize));
			double[] logWeights = factory.createSingleLogWeights(adjustedTargetSize, false); // No size correction!
			double maxLogWeight = Arrays.stream(logWeights).max().getAsDouble();
			List<Double> weights = Arrays.stream(logWeights).map(lw -> Math.exp(lw - maxLogWeight)).boxed().toList();
			double weightSum = weights.stream().mapToDouble(w -> w).sum();
			System.out.println(weights.stream().mapToDouble(w -> w / weightSum).boxed().map(w -> "" + w)
					.collect(Collectors.joining("\t")));
		}
	}

	static void testMultiple() {
		int numberOfRoundTrips = 5;
		var factory = new MaximumEntropyPriorFactory<>(10, 24);
		for (int targetSize = 0; targetSize <= 24; targetSize++) {
			double adjustedTargetSize = Math.max(1e-3, Math.min(24 - 1e-3, targetSize));
			var multiple = factory.createMultiple(adjustedTargetSize, false); // No size correction!
			List<Double> chi2logDensities = new ArrayList<>();
			for (int realizedSizes = 0; realizedSizes <= 24; realizedSizes++) {
				int[] realizedSizeFrequencies = new int[25];
				realizedSizeFrequencies[realizedSizes] = numberOfRoundTrips;
				chi2logDensities.add(multiple.computeChi2LogDensity(realizedSizeFrequencies, adjustedTargetSize,
						numberOfRoundTrips));
			}
			// size correction?
			double maxLogWeight = chi2logDensities.stream().mapToDouble(d -> d).max().getAsDouble();
			List<Double> weights = chi2logDensities.stream().mapToDouble(lw -> Math.exp(lw - maxLogWeight)).boxed()
					.toList();
			double weightSum = weights.stream().mapToDouble(w -> w).sum();
			System.out.println(weights.stream().mapToDouble(w -> w / weightSum).boxed().map(w -> "" + w)
					.collect(Collectors.joining("\t")));
		}
	}

	public static void main(String[] args) {
		// testSingle();
		testMultiple();
	}
}
