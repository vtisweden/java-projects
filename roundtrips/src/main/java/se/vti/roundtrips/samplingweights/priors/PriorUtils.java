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

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * 
 * @author GunnarF
 *
 */
class PriorUtils {

	static double[] computeUniformLogWeights(int nodeCnt, int timeBinCnt, int maxRoundTripSize) {
		double[] logWeights = new double[Math.min(maxRoundTripSize, timeBinCnt) + 1];
		for (int j = 0; j < logWeights.length; j++) {
			logWeights[j] = -CombinatoricsUtils.binomialCoefficientLog(timeBinCnt, j) - j * Math.log(nodeCnt);
		}
		return logWeights;
	}

	static double[] computeUniformLogWeights(int nodeCnt, int timeBinCnt) {
		return computeUniformLogWeights(nodeCnt, timeBinCnt, timeBinCnt); // max timeBinCnt stops in a round trip
	}

	static double[] computeBinomialLogWeights(double expectation, int numberOfTrials) {
		assert (expectation >= 0);
		assert (expectation <= numberOfTrials);
		BinomialDistribution binDistr = new BinomialDistribution(numberOfTrials, expectation / numberOfTrials);
		double[] logWeights = new double[numberOfTrials + 1];
		for (int j = 0; j < logWeights.length; j++) {
			logWeights[j] = binDistr.logProbability(j);
		}
		return logWeights;
	}

//	static double[] computeProbasFromLogWeights(double[] logWeights) {
//		final double maxLogWeight = Arrays.stream(logWeights).max().getAsDouble();
//		final double[] probas = new double[logWeights.length];
//		double probaSum = 0.0;
//		for (int j = 0; j < logWeights.length; j++) {
//			probas[j] = Math.exp(logWeights[j] - maxLogWeight);
//			probaSum += probas[j];
//		}
//		// No sum check because there was at least one addend equal to exp(0).
//		for (int j = 0; j < probas.length; j++) {
//			probas[j] /= probaSum;
//		}
//		return probas;
//	}

}
