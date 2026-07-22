package se.vti.roundtrips.samplingweights.priors;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class PopulationBinomialPrior<N extends Node> implements MHWeight<MultiRoundTrip<N>>, Prior {

	// -------------------- CONSTANTS --------------------

	private final int numberOfNodes;

	private final int numberOfTimeBins;

	private final double meanRoundTripSize;

	private PopulationUniformPrior<N> uniformPrior = null; // lazy initialization

	private double[] binomialLogWeightsOverTotalSize = null; // lazy initialization

	// -------------------- CONSTRUCTION --------------------

	public PopulationBinomialPrior(int numberOfNodes, int numberOfTimeBins, double meanRoundTripSize) {
		this.numberOfNodes = numberOfNodes;
		this.numberOfTimeBins = numberOfTimeBins;
		this.meanRoundTripSize = meanRoundTripSize;
	}

	public PopulationBinomialPrior(Scenario<N> scenario, double meanRoundTripSize) {
		this(scenario.getNumberOfNodes(), scenario.getNumberOfTimeBins(), meanRoundTripSize);
	}

	// -------------------- IMPLEMENTATION OF MHWeight --------------------

	@Override
	public boolean allowsForWeightsOtherThanOneInMHWeightContainer() {
		return false;
	}

	@Override
	public double logWeight(MultiRoundTrip<N> roundTrips) {
		if (this.uniformPrior == null) {
			this.uniformPrior = new PopulationUniformPrior<N>(this.numberOfNodes, this.numberOfTimeBins);
			double expectation = this.meanRoundTripSize * roundTrips.size();
			int numberOfTrials = this.numberOfTimeBins * roundTrips.size();
			this.binomialLogWeightsOverTotalSize = new PriorUtils().computeBinomialLogWeights(expectation,
					numberOfTrials);
		}
		return (this.uniformPrior.logWeight(roundTrips)
				+ this.binomialLogWeightsOverTotalSize[roundTrips.computeSumOfRoundTripSizes()]);
	}

}
