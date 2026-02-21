/**
 * se.vti.roundtrips.samplingweights.misc
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
package se.vti.roundtrips.samplingweights.timeUse;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class LogarithmicTimeMultiplePersonsSingleDay<N extends Node> extends LogarithmicTimeUse<N>
		implements MHWeight<MultiRoundTrip<N>> {

	public LogarithmicTimeMultiplePersonsSingleDay(double period_h, int numberOfPersons) {
		super(period_h, numberOfPersons);
	}

	public LogarithmicTimeUseComponent<N> createComponent(double targetDuration_h, int personIndex) {
		return super.createComponent(targetDuration_h, personIndex);
	}

	@Override
	public double logWeight(MultiRoundTrip<N> roundTrips) {
		return super.computeLogWeight(roundTrips);
	}
}
