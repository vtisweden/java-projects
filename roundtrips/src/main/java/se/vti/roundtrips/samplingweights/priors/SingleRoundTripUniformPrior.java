/**
 * se.vti.roundtrips
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
public class SingleRoundTripUniformPrior<N extends Node> implements MHWeight<RoundTrip<N>>, Prior {

	private final double[] uniformLogWeightsOverSize;

	// -------------------- CONSTRUCTION --------------------
	
	public SingleRoundTripUniformPrior(int numberOfNodes, int numberOfTimeBins, int maximumRoundTripSize) {
		this.uniformLogWeightsOverSize = new PriorUtils().computeUniformLogWeights(numberOfNodes, numberOfTimeBins,
				maximumRoundTripSize);
	}

	public SingleRoundTripUniformPrior(Scenario<N> scenario) {
		this(scenario.getNumberOfNodes(), scenario.getNumberOfTimeBins(), scenario.getMaxPossibleStayEpisodes());
	}

	// -------------------- IMPLEMENTATION OF MHWeight --------------------
	
	@Override
	public boolean allowsForWeightsOtherThanOneInMHWeightContainer() {
		return false;
	}

	@Override
	public double logWeight(RoundTrip<N> roundTrip) {
		return this.uniformLogWeightsOverSize[roundTrip.size()];
	}

}
