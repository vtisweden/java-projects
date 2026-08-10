/**
 * se.vti.utils
 * 
 * Copyright (C) 2023-2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.utils.misc.metropolishastings.terminationcriteria;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * @author GunnarF
 */
class BlockAverageTerminationCriterionTest {

    @Test
    void testRejectInvalidMinSamples() {
        var criterion = new BlockAverageTerminationCriterion<Double>();
        assertThrows(IllegalArgumentException.class,
                () -> criterion.setMinSamples(123));
    }

    @Test
    void testRejectInvalidCheckInterval() {
        var criterion = new BlockAverageTerminationCriterion<Double>();
        assertThrows(IllegalArgumentException.class,
                () -> criterion.setCheckInterval(123));
    }

    @Test
    void testProcessStateWithoutExtractorThrows() {
        var criterion = new BlockAverageTerminationCriterion<Double>();
        criterion.start();
        assertThrows(UnsupportedOperationException.class,
                () -> criterion.processState(1.0));
    }

    @Test
    void testProcessStateWithExtractor() {
        var criterion = new BlockAverageTerminationCriterion<Double>()
                .setExtractor(Double::doubleValue);
        criterion.start();
        assertDoesNotThrow(() -> criterion.processState(1.0));
    }

    @Test
    void testStartResetsEverything() {
        var criterion = new BlockAverageTerminationCriterion<Double>()
                .setExtractor(Double::doubleValue);
        
        criterion.start();
        
        criterion.processState(1.0);
        criterion.processState(2.0);

        criterion.stabilized = true;
        criterion.burnInIteration = 123;
        criterion.stabilizedMean = 42.0;

        criterion.start();

        assertEquals(0, criterion.iterations);
        assertTrue(criterion.samples.isEmpty());
        assertFalse(criterion.stabilized);
    }

    @Test
    void testConstantSeriesIsDetectedAsStable() {
        var criterion = new BlockAverageTerminationCriterion<Double>()
                .setExtractor(Double::doubleValue)
                .setMinSamples(100)
                .setCheckInterval(10);
        criterion.start();

        for (int i = 0; i < 100; i++) {
            criterion.processState(7.0);
        }

        assertTrue(criterion.stabilized);
        assertTrue(criterion.terminate());
        assertEquals(0.0, criterion.stabilizationMeanRange, 1e-10);
        assertEquals(0.0, criterion.stabilizationVarianceRange, 1e-10);
        assertEquals(0.0, criterion.threeWindowMeanRange, 1e-10);
        assertEquals(0.0, criterion.threeWindowVarianceRange, 1e-10);
        assertEquals(7.0, criterion.stabilizedMean, 1e-10);
    }

    @Test
    void testStrongTrendIsNotStable() {
        var criterion = new BlockAverageTerminationCriterion<Double>()
                .setExtractor(Double::doubleValue)
                .setMinSamples(100)
                .setCheckInterval(10);
        criterion.start();

        for (int i = 0; i < 1000; i++) {
            criterion.processState((double) i);
        }

        assertFalse(criterion.stabilized);
        assertFalse(criterion.terminate());
    }

    @Test
    void testRunIndefinitelySuppressesTermination() {
        var criterion = new BlockAverageTerminationCriterion<Double>()
                .setExtractor(Double::doubleValue)
                .setRunIndefinitely(true)
                .setMinSamples(100)
                .setCheckInterval(10);
        criterion.start();

        for (int i = 0; i < 100; i++) {
            criterion.processState(1.0);
        }

        assertTrue(criterion.stabilized);
        assertFalse(criterion.terminate());
    }

    @Test
    void testMinimumStableSamplesRequirement() {
        var criterion = new BlockAverageTerminationCriterion<Double>()
                .setExtractor(Double::doubleValue)
                .setMinSamples(100)
                .setCheckInterval(10)
                .setMinNumberOfStableSamples(200);

        criterion.start();
        for (int i = 0; i < 100; i++) {
            criterion.processState(1.0);
        }

        assertFalse(criterion.stabilized);
        assertFalse(criterion.terminate());

        for (int i = 0; i < 300; i++) {
            criterion.processState(1.0);
        }

        assertTrue(criterion.stabilized);
    }

    @Test
    void testIterationCounterMatchesProcessedStates() {
        var criterion = new BlockAverageTerminationCriterion<Double>()
                .setExtractor(Double::doubleValue);
        criterion.start();

        for (int i = 0; i < 17; i++) {
            criterion.processState((double) i);
        }

        assertEquals(17, criterion.iterations);
        assertEquals(17, criterion.samples.size());
    }

    @Test
    void testBurnInIterationIsReasonableForStableData() {
        var rnd = new Random(12345);
        var criterion = new BlockAverageTerminationCriterion<Double>()
                .setExtractor(Double::doubleValue)
                .setMinSamples(1000)
                .setCheckInterval(100)
                .setStandardizedMeanTolerance(0.5)
                .setRelativeVarianceTolerance(0.5);
        criterion.start();

        for (int i = 0; i < 5000 && !criterion.stabilized; i++) {
            criterion.processState(rnd.nextGaussian());
        }

        assertTrue(criterion.stabilized);

        assertNotNull(criterion.burnInIteration);
        assertTrue(criterion.burnInIteration >= 0);
        assertTrue(criterion.burnInIteration <= criterion.samples.size());
    }

    @Test
    void testDiagnosticRangesBecomeFiniteAfterCheck() {
        var criterion = new BlockAverageTerminationCriterion<Double>()
                .setExtractor(Double::doubleValue)
                .setMinSamples(100)
                .setCheckInterval(10);
        criterion.start();

        for (int i = 0; i < 100; i++) {
            criterion.processState(1.0);
        }

        assertNotNull(criterion.stabilizationMeanRange);
        assertNotNull(criterion.stabilizationVarianceRange);
        assertNotNull(criterion.threeWindowMeanRange);
        assertNotNull(criterion.threeWindowVarianceRange);

        assertTrue(Double.isFinite(criterion.stabilizationMeanRange));
        assertTrue(Double.isFinite(criterion.stabilizationVarianceRange));
        assertTrue(Double.isFinite(criterion.threeWindowMeanRange));
        assertTrue(Double.isFinite(criterion.threeWindowVarianceRange));
    }

    @Test
    void testTerminateRequiresStabilization() {
        var criterion = new BlockAverageTerminationCriterion<Double>()
                .setExtractor(Double::doubleValue);
        criterion.start();

        assertFalse(criterion.terminate());

        criterion.stabilized = true;
        assertTrue(criterion.terminate());
    }
}