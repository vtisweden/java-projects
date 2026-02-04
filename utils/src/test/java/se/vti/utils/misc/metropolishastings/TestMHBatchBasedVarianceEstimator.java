/**
 * se.vti.utils.misc.metropolishastings
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
package se.vti.utils.misc.metropolishastings;

import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author GunnarF
 */
class TestMHBatchBasedVarianceEstimator {

	@Test
	void test() {
		Random rnd = new Random(4711);
		var estimator = new MHBatchBasedVarianceEstimator<Double>(x -> x).setMinBatchSize(10)
				.setShareOfDiscartedTransients(0.0);
		for (int i = 0; i < 3_000; i++) {
			estimator.processState(rnd.nextGaussian());
		}
		Assertions.assertEquals(0.0279648119240542, estimator.getMeanValue());
		Assertions.assertEquals(1.0718082295108025, estimator.getEffectiveVariance());
		Assertions.assertEquals(3.675611212314137E-4, estimator.getVarianceOfMean());

	}

}
