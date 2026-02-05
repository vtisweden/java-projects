/**
 * se.vti.roundtrips.samplingweights.misc.calibration
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
package se.vti.roundtrips.samplingweights.misc.calibration;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.grouping.PopulationGroupFilter;

/**
 * @author GunnarF
 */
class TestTargetDeviationWeight {

	class TestInstance extends TargetDeviationWeight<Node> {

		public TestInstance(double realPopulationSize) {
			super(realPopulationSize);
		}

		@Override
		public String[] createLabels() {
			return null;
		}

		@Override
		public double[] computeTarget() {
			return null;
		}

		@Override
		public double[] computeSample(MultiRoundTrip<Node> multiRoundTrip, PopulationGroupFilter<Node> filter) {
			return null;
		}

	}
	
	double realPopulationSize = 25_000;
	int syntheticPopulationSize = 100;
	double targetValue = 0.5 * realPopulationSize;


//	@Test
	void testTwoSidedExponential() {
		
		var testInstance = new TestInstance(realPopulationSize);
		testInstance.setToTwoSidedExponential();
		testInstance.computeExpansionFactor(syntheticPopulationSize);

		for (int sampleValue = 0; sampleValue <= syntheticPopulationSize; sampleValue++) {
			System.out.println(sampleValue + "\t" + testInstance.computeLogWeight(sampleValue, targetValue));
		}

//		fail("Not yet implemented");
	}

	@Test
	void testGaussian() {

		var testInstance = new TestInstance(realPopulationSize);
		testInstance.setToGaussian();
		testInstance.computeExpansionFactor(syntheticPopulationSize);

		for (int sampleValue = 0; sampleValue <= syntheticPopulationSize; sampleValue++) {
			System.out.println(sampleValue + "\t" + testInstance.computeLogWeight(sampleValue, targetValue));
		}

//		fail("Not yet implemented");
	}

}
