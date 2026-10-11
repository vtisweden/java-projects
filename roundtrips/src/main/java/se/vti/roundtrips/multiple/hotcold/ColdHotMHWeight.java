/**
 * se.vti.roundtrips.multiple
 * 
 * Copyright (C) 2024-26 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.multiple.hotcold;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @param <N>
 */
public class ColdHotMHWeight<N extends Node> implements MHWeight<ColdHotMultiRoundTrip<N>> {

	private final MHWeight<MultiRoundTrip<N>> prior;
	private final MHWeight<MultiRoundTrip<N>> coldWeight;
	private final double hotScale;

	public ColdHotMHWeight(MHWeight<MultiRoundTrip<N>> prior, MHWeight<MultiRoundTrip<N>> coldWeight, double hotScale) {
		if (hotScale < 0 || hotScale > 1.0) {
			throw new IllegalArgumentException("hotScale is " + hotScale + ", which is not between zero and one");
		}
		this.prior = prior;
		this.coldWeight = coldWeight;
		this.hotScale = hotScale;
	}

	@Override
	public double logWeight(ColdHotMultiRoundTrip<N> state) {
		return this.prior.logWeight(state.getCold()) + this.prior.logWeight(state.getHot())
				+ this.coldWeight.logWeight(state.getCold())
				+ this.hotScale * this.coldWeight.logWeight(state.getHot());
	}

}
