/**
 * se.vti.roundtrips.samplingweights.priors
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

import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class SingleRoundTripCombinatorics {

	private double[] logNumberOfRoundTripsOverSize;

	public SingleRoundTripCombinatorics(int locationCnt, int timeBinCnt) {
		this.logNumberOfRoundTripsOverSize = new double[timeBinCnt + 1]; // max timeBinCnt stops in a round trip
		for (int roundTripSize = 0; roundTripSize <= timeBinCnt; roundTripSize++) {
			this.logNumberOfRoundTripsOverSize[roundTripSize] = CombinatoricsUtils.binomialCoefficientLog(timeBinCnt,
					roundTripSize) + roundTripSize * Math.log(locationCnt);
		}
	}

	public double getLogNumberOfRoundTrips(int roundTripSize) {
		return this.logNumberOfRoundTripsOverSize[roundTripSize];
	}

}
