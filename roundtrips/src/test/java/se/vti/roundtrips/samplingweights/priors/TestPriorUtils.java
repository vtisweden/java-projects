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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author GunnarF
 */
class TestPriorUtils {

	@Test
	void testUniform() {
		int nodeCnt = 2;
		int timeBinCnt = 3;
		int maxSize = 2;
		double[] uniform = PriorUtils.singleton().computeUniformLogWeights(nodeCnt, timeBinCnt, maxSize);
		Assertions.assertArrayEquals(new double[] { -0.0, -1.791759469228055, -2.4849066497880004 }, uniform);
	}

	@Test
	void testBinomial() {
		int timeBinCnt = 3;
		double successProbability = 0.5;
		double[] binomial = PriorUtils.singleton().computeBinomialLogWeights(successProbability, timeBinCnt);
		Assertions.assertArrayEquals(
				new double[] { -0.5469646703818638, -1.0577902941478547, -2.667228206581955, -5.375278407684165 },
				binomial);
	}
}
