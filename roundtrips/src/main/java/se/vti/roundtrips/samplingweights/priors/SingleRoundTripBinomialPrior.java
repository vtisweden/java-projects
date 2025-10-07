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

	// -------------------- CONSTANTS --------------------

	private final SingleRoundTripUniformPrior<N> uniformPrior;

	private final double[] binomialLogWeightsOverSize;

	// -------------------- CONSTRUCTION --------------------

	public SingleRoundTripBinomialPrior(int nodeCnt, int timeBinCnt, double expectedRoundTripSize,
			int maxRoundTripSize) {
		this.uniformPrior = new SingleRoundTripUniformPrior<>(nodeCnt, timeBinCnt, maxRoundTripSize);
		this.binomialLogWeightsOverSize = PriorUtils.computeBinomialLogWeights(expectedRoundTripSize, maxRoundTripSize);
	}

	public SingleRoundTripBinomialPrior(Scenario<N> scenario, double expectedRoundTripSize) {
		this(scenario.getNodesCnt(), scenario.getTimeBinCnt(), expectedRoundTripSize,
				scenario.getMaxPossibleStayEpisodes());
	}

	// -------------------- SINGLE(S) IMPLEMENTATION --------------------

	@Override
	public double logWeight(RoundTrip<N> state) {
		return (this.uniformPrior.logWeight(state) + this.binomialLogWeightsOverSize[state.size()]);
	}

}
