package se.vti.roundtrips.samplingweights.priors;

import java.util.Arrays;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class MaximumEntropyPriorFactory<N extends Node> {

	// -------------------- CONSTANTS --------------------

	private final int nodeCnt;
	private final int timeBinCnt;
	private final int maxRoundTripLength;

	private final SingleRoundTripCombinatorics sizeCombinatorics;

	// -------------------- CONSTRUCTION --------------------

	public MaximumEntropyPriorFactory(int nodeCnt, int timeBinCnt, int maxRoundTripLength) {
		this.nodeCnt = nodeCnt;
		this.timeBinCnt = timeBinCnt;
		this.maxRoundTripLength = Math.min(maxRoundTripLength, timeBinCnt);
		this.sizeCombinatorics = new SingleRoundTripCombinatorics(nodeCnt, timeBinCnt);
	}

	public MaximumEntropyPriorFactory(int nodeCnt, int timeBinCnt) {
		this(nodeCnt, timeBinCnt, timeBinCnt);
	}

	public MaximumEntropyPriorFactory(Scenario<N> scenario) {
		this(scenario.getNodesCnt(), scenario.getTimeBinCnt(), scenario.getMaxPossibleStayEpisodes());
	}

	// -------------------- INTERNALS --------------------

	private double[] createSingleLogWeights(double meanRoundTripLength, boolean correctForCombinatorics) {
		assert (meanRoundTripLength >= 0);
		assert (meanRoundTripLength <= this.maxRoundTripLength);
		final BinomialDistribution binDistr = new BinomialDistribution(this.maxRoundTripLength,
				meanRoundTripLength / this.maxRoundTripLength);
		double[] result = new double[this.maxRoundTripLength + 1];
		for (int j = 0; j < result.length; j++) {
			result[j] = correctForCombinatorics
					? binDistr.logProbability(j) - this.sizeCombinatorics.getLogNumberOfRoundTrips(j)
					: binDistr.logProbability(j);
		}
		return result;
	}

	private double[] probasFromLogWeights(double[] logWeights) {
		final double maxLogWeight = Arrays.stream(logWeights).max().getAsDouble();
		final double[] result = new double[logWeights.length];
		double sum = 0.0;
		for (int j = 0; j < result.length; j++) {
			result[j] = Math.exp(logWeights[j] - maxLogWeight);
			sum += result[j];
		}
		assert (sum >= 1e-8);

		for (int j = 0; j < result.length; j++) {
			result[j] /= sum;
		}
		return result;
	}

	// -------------------- SINGLE(S) IMPLEMENTATION --------------------

	public MHWeight<RoundTrip<N>> createSingle(double meanRoundTripLength) {
		return new MHWeight<RoundTrip<N>>() {
			private final double[] logWeights = createSingleLogWeights(meanRoundTripLength, true);

			@Override
			public double logWeight(RoundTrip<N> state) {
				return this.logWeights[state.size()];
			}
		};
	}

	public MHWeight<MultiRoundTrip<N>> createSingles(int _N, double meanRoundTripLength) {
		return new SingleToMultiWeight<>(this.createSingle(meanRoundTripLength));
	}

	// -------------------- MULTIPLE IMPLEMENTATION --------------------

	private class Chi2Prior implements MHWeight<MultiRoundTrip<N>> {

		private final int maxRoundTripLength;

		private final Double targetMeanLength;

		/*
		 * singleProbasGivenMeanLength[meanLength][j] is the probability of sampling a
		 * particular size-j roundtrip given the mean round trip length "meanLength".
		 */
		private final double[][] singleProbasGivenMeanLength;

		// lazy initialization, don't know number of round trips from the beginning
		private ChiSquaredDistribution chi2distr = null;

		// -------------------- CONSTRUCTION --------------------

		Chi2Prior(int nodeCnt, int timeBinCnt, int maxRoundTripLength, Double targetMeanLength) {
			this.maxRoundTripLength = maxRoundTripLength;
			this.targetMeanLength = targetMeanLength;

			this.singleProbasGivenMeanLength = new double[maxRoundTripLength + 1][];
			for (int meanLength = 0; meanLength <= maxRoundTripLength; meanLength++) {
				this.singleProbasGivenMeanLength[meanLength] = probasFromLogWeights(
						createSingleLogWeights(meanLength, false));
			}
		}

		// -------------------- INTERNALS --------------------

		private double getOrComputeTargetMeanLength(MultiRoundTrip<?> roundTrips) {
			if (this.targetMeanLength != null) {
				return this.targetMeanLength;
			} else {
				double count = 0;
				for (RoundTrip<?> roundTrip : roundTrips) {
					count += roundTrip.size();
				}
				return (count / roundTrips.size());
			}
		}

		// -------------------- IMPLEMENTATION --------------------

		@Override
		public double logWeight(MultiRoundTrip<N> roundTrips) {

			int[] realizedLengthFrequencies = new int[maxRoundTripLength + 1];
			for (RoundTrip<?> roundTrip : roundTrips) {
				realizedLengthFrequencies[roundTrip.size()]++;
			}

			final double targetMeanLength = this.getOrComputeTargetMeanLength(roundTrips);
			final int targetMeanLengthFloor = (int) targetMeanLength;
			final double[] lowerInterpolationSingleProbas = this.singleProbasGivenMeanLength[targetMeanLengthFloor];
			final double[] upperInterpolationSingleProbas = this.singleProbasGivenMeanLength[(targetMeanLengthFloor == this.maxRoundTripLength)
					? this.maxRoundTripLength
					: targetMeanLengthFloor + 1];
			final double upperInterpolationWeight = targetMeanLength - targetMeanLengthFloor;

			double chi2 = 0.0;
			for (int j = 0; j < realizedLengthFrequencies.length; j++) {
				final double sizeProba = (1.0 - upperInterpolationWeight) * lowerInterpolationSingleProbas[j]
						+ upperInterpolationWeight * upperInterpolationSingleProbas[j];
				chi2 += Math.pow(realizedLengthFrequencies[j] - sizeProba * roundTrips.size(), 2.0) / sizeProba;
			}

			if (this.chi2distr == null) {
				this.chi2distr = new ChiSquaredDistribution(roundTrips.size());
			}
			return this.chi2distr.logDensity(chi2);
		}

		@Override
		public String name() {
			return "Chi2PriorExogMean";
		}

	}

	public MHWeight<MultiRoundTrip<N>> createMultiple(double targetMeanLength) {
		return new Chi2Prior(this.nodeCnt, this.timeBinCnt, this.maxRoundTripLength, targetMeanLength);
	}

	public MHWeight<MultiRoundTrip<N>> createMultiple() {
		return new Chi2Prior(this.nodeCnt, this.timeBinCnt, this.maxRoundTripLength, null);
	}
}
