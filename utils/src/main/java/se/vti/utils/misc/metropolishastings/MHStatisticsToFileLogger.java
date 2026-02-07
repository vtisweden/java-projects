/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.utils.misc.metropolishastings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 
 * @author GunnarF
 *
 */
public class MHStatisticsToFileLogger<X> extends MHToFileLogger<X> {

	private List<MHBatchBasedStatisticEstimator<X>> statisticsEstimators = new ArrayList<>();

	public MHStatisticsToFileLogger(long logInterval, String logFileName) {
		super(logInterval, logFileName);
	}

	public void add(MHBatchBasedStatisticEstimator<X> statisticsEstimator) {
		this.statisticsEstimators.add(statisticsEstimator);
	}

	@Override
	public String createHeaderLine() {
		StringBuffer result = new StringBuffer("Iteration");
		for (var statisticEstimator : this.statisticsEstimators) {
			String name = statisticEstimator.getName();
			result.append(String.format("\tAVG(%s)\tSTD(%s)\tSTD(AVG(%s))", name, name, name));
		}
		return result.toString();
	}

	@Override
	public String createDataLine(X state) {
		StringBuffer result = new StringBuffer(Long.toString(this.iteration()));
		boolean foundData = false;
		for (var statisticEstimator : this.statisticsEstimators) {
			Double mean = statisticEstimator.getMeanValue();
			Double stddev = statisticEstimator.getEffectiveStandardDeviation();
			Double stddevOfMean = statisticEstimator.getStandardDeviationOfMean();
			if (mean != null && stddev != null && stddevOfMean != null) {
				result.append(String.format(Locale.US, "\t%f\t%f\t%f", mean, stddev, stddevOfMean));
				foundData = true;
			}
		}
		if (foundData) {
			return result.toString();
		} else {
			return null;
		}
	}
}
