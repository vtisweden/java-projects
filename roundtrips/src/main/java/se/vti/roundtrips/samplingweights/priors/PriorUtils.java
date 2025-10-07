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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * 
 * @author GunnarF
 *
 */
class PriorUtils {

	private PriorUtils() {
	}

	static double[] computeUniformLogWeights(int nodeCnt, int timeBinCnt, int maxRoundTripSize) {
		double[] logWeights = new double[Math.min(maxRoundTripSize, timeBinCnt) + 1];
		for (int j = 0; j < logWeights.length; j++) {
			logWeights[j] = -CombinatoricsUtils.binomialCoefficientLog(timeBinCnt, j) - j * Math.log(nodeCnt);
		}
		return logWeights;
	}

	static double[] computeBinomialLogWeights(double numberOfSuccesses, int numberOfTrials) {
		assert (numberOfSuccesses >= 0);
		assert (numberOfSuccesses <= numberOfTrials);
		assert (numberOfTrials > 0);
		BinomialDistribution binDistr = new BinomialDistribution(numberOfTrials, numberOfSuccesses / numberOfTrials);
		double[] logWeights = new double[numberOfTrials + 1];
		for (int j = 0; j < logWeights.length; j++) {
			logWeights[j] = binDistr.logProbability(j);
		}
		return logWeights;
	}

	public static void main(String[] args) {
		int nodeCnt = 100;
		int timeBinCnt = 24;
		int maxRoundTripSize = timeBinCnt;

		double[] uniform = computeUniformLogWeights(nodeCnt, timeBinCnt, maxRoundTripSize);
		double[] binomial = computeBinomialLogWeights(12, 24);

		System.out.println(
				"uniform\t" + Arrays.stream(uniform).boxed().map(p -> "" + p).collect(Collectors.joining("\t")));
		System.out.println(
				"binomial\t" + Arrays.stream(binomial).boxed().map(p -> "" + p).collect(Collectors.joining("\t")));
	}
}
