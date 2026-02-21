package se.vti.roundtrips.samplingweights.priors;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class IndividualBinomialPrior<N extends Node> implements MHWeight<RoundTrip<N>>, Prior {

	private final SingleRoundTripUniformPrior<N> uniformPrior;

	private final double[] binomialLogWeightsOverSize;

	// -------------------- CONSTRUCTION --------------------

	public IndividualBinomialPrior(int numberOfNodes, int numberOfTimeBins, double expectedRoundTripSize) {
		this.uniformPrior = new SingleRoundTripUniformPrior<>(numberOfNodes, numberOfTimeBins);
		this.binomialLogWeightsOverSize = new PriorUtils().computeBinomialLogWeights(expectedRoundTripSize,
				numberOfTimeBins);
	}

	public IndividualBinomialPrior(Scenario<N> scenario, double expectedRoundTripSize) {
		this(scenario.getNumberOfNodes(), scenario.getNumberOfTimeBins(), expectedRoundTripSize);
	}

	// -------------------- IMPLEMENTATION OF MHWeight --------------------

	@Override
	public boolean allowsForWeightsOtherThanOneInMHWeightContainer() {
		return false;
	}

	@Override
	public double logWeight(RoundTrip<N> roundTrip) {
		return (this.uniformPrior.logWeight(roundTrip) + this.binomialLogWeightsOverSize[roundTrip.size()]);
	}

}
