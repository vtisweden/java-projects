package se.vti.roundtrips.samplingweights.priors;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.samplingweights.SingleToMultiWeight;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class PopulationBinomialPrior<N extends Node> implements MHWeight<MultiRoundTrip<N>>, Prior {

	// -------------------- CONSTANTS --------------------

	private final double expectedRoundTripSize;

	private final int numberOfTimeBins;

	private final MHWeight<MultiRoundTrip<N>> uniformPrior;

	private double[] binomialLogWeightsOverTotalSize = null; // lazy initialization

	// -------------------- CONSTRUCTION --------------------

	public PopulationBinomialPrior(int numberOfNodes, int numberOfTimeBins, double expectedRoundTripSize) {
		this.numberOfTimeBins = numberOfTimeBins;
		this.expectedRoundTripSize = expectedRoundTripSize;
		this.uniformPrior = new SingleToMultiWeight<>(
				new SingleRoundTripUniformPrior<>(numberOfNodes, numberOfTimeBins));
	}

	public PopulationBinomialPrior(Scenario<N> scenario, double expectedRoundTripSize) {
		this(scenario.getNumberOfNodes(), scenario.getNumberOfTimeBins(), expectedRoundTripSize);
	}

	// -------------------- IMPLEMENTATION OF MHWeight --------------------

	@Override
	public boolean allowsForWeightsOtherThanOneInMHWeightContainer() {
		return false;
	}

	@Override
	public double logWeight(MultiRoundTrip<N> roundTrips) {
		if (this.binomialLogWeightsOverTotalSize == null) {
			// Upon the first call to this function, we know the number of round trips.
			double expectation = this.expectedRoundTripSize * roundTrips.size();
			int numberOfTrials = this.numberOfTimeBins * roundTrips.size();
			this.binomialLogWeightsOverTotalSize = new PriorUtils().computeBinomialLogWeights(expectation,
					numberOfTrials);
		}
		return (this.uniformPrior.logWeight(roundTrips)
				+ this.binomialLogWeightsOverTotalSize[roundTrips.computeSumOfRoundTripSizes()]);
	}

}
