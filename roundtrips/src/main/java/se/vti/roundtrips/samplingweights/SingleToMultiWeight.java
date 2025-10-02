/**
 * instances.testing
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.samplingweights;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class SingleToMultiWeight<N extends Node> implements MHWeight<MultiRoundTrip<N>> {

	private final MHWeight<RoundTrip<N>> singleRoundTripLogWeight;

	private List<RoundTrip<N>> previousRoundTrips = null;

	private List<Double> previousLogWeights = null;

	public SingleToMultiWeight(MHWeight<RoundTrip<N>> singleRoundTripLogWeight) {
		this.singleRoundTripLogWeight = singleRoundTripLogWeight;
	}

	@Override
	public double logWeight(MultiRoundTrip<N> multiRoundTrip) {

		double logWeightSum = 0.0;

		if (this.previousRoundTrips == null) {
			this.previousRoundTrips = new ArrayList<>(Collections.nCopies(multiRoundTrip.size(), null));
			this.previousLogWeights = new ArrayList<>(Collections.nCopies(multiRoundTrip.size(), null));
		}

		for (int i = 0; i < multiRoundTrip.size(); i++) {
			final RoundTrip<N> roundTrip = multiRoundTrip.getRoundTrip(i);
			if (this.previousRoundTrips.get(i) == roundTrip) {
				logWeightSum += this.previousLogWeights.get(i);
			} else {
				final double logWeight = this.singleRoundTripLogWeight.logWeight(roundTrip);
				this.previousRoundTrips.set(i, roundTrip);
				this.previousLogWeights.set(i, logWeight);
				logWeightSum += logWeight;
			}
		}

		return logWeightSum;
	}

	@Override
	public String name() {
		return "1toN(" + this.singleRoundTripLogWeight.name() + ")";
	}

}
