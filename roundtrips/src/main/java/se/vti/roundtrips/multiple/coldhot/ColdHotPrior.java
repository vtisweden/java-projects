/**
 * se.vti.roundtrips.multiple
 * 
 * Copyright (C) 2024-2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.multiple.coldhot;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.RoundTrips;
import se.vti.roundtrips.samplingweights.priors.Prior;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @param <N>
 */
public class ColdHotPrior<N extends Node> implements Prior, MHWeight<RoundTrips<N>> {

	private final MHWeight<RoundTrips<N>> prior;

	public ColdHotPrior(MHWeight<RoundTrips<N>> prior) {
		this.prior = prior;
	}

	@Override
	public double logWeight(RoundTrips<N> roundTrips) {
		var coldHot = (ColdHotMultiRoundTrip<N>) roundTrips;
		return this.prior.logWeight(coldHot.getCold()) + this.prior.logWeight(coldHot.getHot());
	}

}
