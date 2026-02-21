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
package se.vti.roundtrips.samplingweights;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class StrictlyPeriodicSchedule<N extends Node> implements MHWeight<RoundTrip<N>> {

	private final double periodLength_h;

	public StrictlyPeriodicSchedule(double periodLength_h) {
		this.periodLength_h = periodLength_h;
	}

	@Override
	public double logWeight(RoundTrip<N> roundTrip) {
		if (roundTrip.size() <= 1) {
			return 0.0;
		} else {
			double realizedDuration_h = roundTrip.getEpisodes().stream().mapToDouble(e -> e.getDuration_h()).sum();
			return realizedDuration_h > this.periodLength_h ? Double.NEGATIVE_INFINITY : 0.0;
		}
	}
}
