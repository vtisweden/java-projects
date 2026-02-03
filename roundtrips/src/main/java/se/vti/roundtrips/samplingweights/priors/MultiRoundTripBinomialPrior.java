package se.vti.roundtrips.samplingweights.priors;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * Experimental!
 * 
 * @author GunnarF
 *
 */
public class MultiRoundTripBinomialPrior<N extends Node> implements MHWeight<MultiRoundTrip<N>> {

	// -------------------- CONSTANTS --------------------

	private final double expectedRoundTripSize;

	private final int maximumRoundTripSize;

	private final MHWeight<MultiRoundTrip<N>> uniformPrior;

	private double[] binomialLogWeightsOverTotalSize = null; // lazy initialization

	// -------------------- CONSTRUCTION --------------------

	public MultiRoundTripBinomialPrior(int nodeCnt, int timeBinCnt, double expectedRoundTripSize,
			int maximumRoundTripSize) {
		this.expectedRoundTripSize = expectedRoundTripSize;
		this.maximumRoundTripSize = maximumRoundTripSize;
		this.uniformPrior = new SingleToMultiWeight<>(
				new SingleRoundTripUniformPrior<>(nodeCnt, timeBinCnt, maximumRoundTripSize));
	}

	public MultiRoundTripBinomialPrior(Scenario<N> scenario, double expectedRoundTripSize) {
		this(scenario.getNodesCnt(), scenario.getTimeBinCnt(), expectedRoundTripSize,
				scenario.getMaxPossibleStayEpisodes());
	}

	// -------------------- SINGLE(S) IMPLEMENTATION --------------------

	@Override
	public double logWeight(MultiRoundTrip<N> roundTrips) {
		if (this.binomialLogWeightsOverTotalSize == null) {
			// Upon the first call to this function, we know the number of round trips.
			double expectation = this.expectedRoundTripSize * roundTrips.size();
			int numberOfTrials = this.maximumRoundTripSize * roundTrips.size();
			this.binomialLogWeightsOverTotalSize = new PriorUtils().computeBinomialLogWeights(expectation,
					numberOfTrials);
		}
		return (this.uniformPrior.logWeight(roundTrips)
				+ this.binomialLogWeightsOverTotalSize[roundTrips.computeSumOfRoundTripSizes()]);
	}

}
