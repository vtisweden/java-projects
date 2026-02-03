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
public class SingleRoundTripBinomialPrior<N extends Node> implements MHWeight<RoundTrip<N>> {

	private final SingleRoundTripUniformPrior<N> uniformPrior;

	private final double[] binomialLogWeightsOverSize;

	public SingleRoundTripBinomialPrior(int nodeCnt, int timeBinCnt, double expectedRoundTripSize,
			int maximumRoundTripSize) {
		this.uniformPrior = new SingleRoundTripUniformPrior<>(nodeCnt, timeBinCnt, maximumRoundTripSize);
		this.binomialLogWeightsOverSize = new PriorUtils().computeBinomialLogWeights(expectedRoundTripSize,
				maximumRoundTripSize);
	}

	public SingleRoundTripBinomialPrior(Scenario<N> scenario, double expectedRoundTripSize) {
		this(scenario.getNodesCnt(), scenario.getTimeBinCnt(), expectedRoundTripSize,
				scenario.getMaxPossibleStayEpisodes());
	}

	@Override
	public double logWeight(RoundTrip<N> roundTrip) {
		return (this.uniformPrior.logWeight(roundTrip) + this.binomialLogWeightsOverSize[roundTrip.size()]);
	}

}
