package se.vti.roundtrips.priors;

import java.util.Arrays;

import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.scenario.Scenario;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class IndividualHistogramPrior<N extends Node> implements MHWeight<RoundTrip<N>> {

	private final IndividualUniformPrior<N> uniformPrior;

	private final double[] targetLogWeightsOverSize;

	// -------------------- CONSTRUCTION --------------------

	/**
	 * @param targetProbabilityOverSize array of size (numberOfTimeSteps + 1),
	 *                                  allocating probabilities to sizes
	 *                                  0,1,...,numberOfTimeSteps.
	 */
	public IndividualHistogramPrior(int numberOfNodes, double[] targetProbabilityOverSize) {
		this.uniformPrior = new IndividualUniformPrior<>(numberOfNodes, targetProbabilityOverSize.length - 1);
		this.targetLogWeightsOverSize = Arrays.stream(targetProbabilityOverSize).map(p -> Math.log(p)).toArray();
	}

	/**
	 * @param targetProbabilityOverSize array of size (numberOfTimeSteps + 1),
	 *                                  allocating a probabilities to sizes
	 *                                  0,1,...,numberOfTimeSteps.
	 */
	public IndividualHistogramPrior(Scenario<N> scenario, double[] targetProbabilityOverSize) {
		this(scenario.getNumberOfNodes(), targetProbabilityOverSize);
	}

	// -------------------- IMPLEMENTATION OF MHWeight --------------------

	@Override
	public boolean allowsForWeightsOtherThanOneInMHWeightContainer() {
		return false;
	}

	@Override
	public double logWeight(RoundTrip<N> roundTrip) {
		return (this.uniformPrior.logWeight(roundTrip) + this.targetLogWeightsOverSize[roundTrip.size()]);
	}

}
