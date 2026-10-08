package se.vti.roundtrips.single;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;

/**
 * 
 */
class TestRoundTripMultiStepProposal {

	@Test
	void testSequenceLengthDistributionConsistency() {

		final double[] expectedLengths = { 1.0, 2.0, 3.0, 5.0, 10.0 };
		final int sampleSize = 1_000_000;

		System.out.println("Theoretical\tRealized");
		for (double expectedLength : expectedLengths) {

			RoundTripMultiStepProposal<Node> proposal = new RoundTripMultiStepProposal<>(
					new Scenario<>(new Random(4711)), expectedLength);

			double p = 1.0 / expectedLength;
			double variance = (1.0 - p) / (p * p);
			double standardError = Math.sqrt(variance / sampleSize);

			double empiricalMean = 0.0;
			for (int i = 0; i < sampleSize; i++) {
				int length = proposal.drawSequenceLength();
				empiricalMean += length;
			}
			empiricalMean /= sampleSize;

			System.out.println(expectedLength + "\t" + empiricalMean);
			assertEquals(expectedLength, empiricalMean, 5.0 * standardError);

		}
	}
}
