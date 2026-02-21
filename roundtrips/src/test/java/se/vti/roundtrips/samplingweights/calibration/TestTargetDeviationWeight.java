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
package se.vti.roundtrips.samplingweights.calibration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.grouping.PopulationGroupFilter;
import se.vti.roundtrips.samplingweights.calibration.TargetDeviationWeight;

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
	
	final static double realPopulationSize = 10_000;
	final static int syntheticPopulationSize = 100;
	final static double targetValue = 0.5 * realPopulationSize;


	@Test
	void testTwoSidedExponentialWithoutDiscretization() {		
		var testInstance = new TestInstance(realPopulationSize).setToTwoSidedExponential(1.0).setAccountForDiscretizationNoise(false);
		testInstance.computeExpansionFactor(syntheticPopulationSize);
		Assertions.assertEquals(0, Math.abs(testInstance.computeLogWeight(syntheticPopulationSize / 2.0, realPopulationSize / 2.0)));		
		Assertions.assertEquals(-141.4213562373095, testInstance.computeLogWeight(syntheticPopulationSize / 2.0 - 1, realPopulationSize / 2.0));		
		Assertions.assertEquals(-141.4213562373095, testInstance.computeLogWeight(syntheticPopulationSize / 2.0 + 1, realPopulationSize / 2.0));		
	}

	@Test
	void testTwoSidedExponentialWithDiscretization() {		
		var testInstance = new TestInstance(realPopulationSize).setToTwoSidedExponential(1.0).setAccountForDiscretizationNoise(true);
		testInstance.computeExpansionFactor(syntheticPopulationSize);
		Assertions.assertEquals(0, Math.abs(testInstance.computeLogWeight(syntheticPopulationSize / 2.0, realPopulationSize / 2.0)));		
		Assertions.assertEquals(-4.734955805047642, testInstance.computeLogWeight(syntheticPopulationSize / 2.0 - 1, realPopulationSize / 2.0));		
		Assertions.assertEquals(-4.734955805047642, testInstance.computeLogWeight(syntheticPopulationSize / 2.0 + 1, realPopulationSize / 2.0));		
	}

	@Test
	void testGaussianWithoutDiscretization() {
		var testInstance = new TestInstance(realPopulationSize).setToGaussian(1.0).setAccountForDiscretizationNoise(false);
		testInstance.computeExpansionFactor(syntheticPopulationSize);
		Assertions.assertEquals(0, Math.abs(testInstance.computeLogWeight(syntheticPopulationSize / 2.0, realPopulationSize / 2.0)));		
		Assertions.assertEquals(-5000.0, testInstance.computeLogWeight(syntheticPopulationSize / 2.0 - 1, realPopulationSize / 2.0));		
		Assertions.assertEquals(-5000.0, testInstance.computeLogWeight(syntheticPopulationSize / 2.0 + 1, realPopulationSize / 2.0));		
	}

	@Test
	void testGaussianWithDiscretization() {
		var testInstance = new TestInstance(realPopulationSize).setToGaussian(1.0).setAccountForDiscretizationNoise(true);
		testInstance.computeExpansionFactor(syntheticPopulationSize);
		Assertions.assertEquals(0, Math.abs(testInstance.computeLogWeight(syntheticPopulationSize / 2.0, realPopulationSize / 2.0)));		
		Assertions.assertEquals(-5.9928086296444265, testInstance.computeLogWeight(syntheticPopulationSize / 2.0 - 1, realPopulationSize / 2.0));		
		Assertions.assertEquals(-5.9928086296444265, testInstance.computeLogWeight(syntheticPopulationSize / 2.0 + 1, realPopulationSize / 2.0));		
	}

}
