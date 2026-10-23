/**
 * se.vti.utils
 * 
 * Copyright (C) 2025-2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.runner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.scenario.ScenarioBuilder;
import se.vti.roundtrips.single.RoundTripUtils;

/**
 * @author GunnarF
 */
class RunnerLoggingTest {

	@TempDir
	Path tempDir;

	private Runner<Node> createRunner() {
		var scenarioBuilder = new ScenarioBuilder<Node>().setRandom(new Random(4711)).setNumberOfTimeBins(24)
				.setTimeBinSize_h(1.0);
		for (int i = 1; i <= 10; i++) {
			scenarioBuilder.addNode(new Node("" + i));
		}
		scenarioBuilder.setMoveDistanceFunction((a, b) -> (a == b ? 0.0 : 1.0));
		scenarioBuilder.setMoveTimeFunction((a, b) -> (a == b ? 0.0 : 1.0));
		var scenario = scenarioBuilder.build();
		return new Runner<>(scenario).setNumberOfIterations(2).setIndividualUniformPrior()
				.setInitialState(RoundTripUtils.singleton().createEmptyMultiRoundTrip(2));
	}

	// -------------------- SAMPLE LOGGING --------------------

	@Test
	void sampleLoggingCreatesFile() {
		Path file = tempDir.resolve("samples.log");
		Runner<Node> runner = createRunner().configureSampleLogging(file.toString(), 1).addSampleExtractor("test",
				state -> 0.0);
		runner.run();
		assertTrue(Files.exists(file));
	}

	@Test
	void sampleLoggingCanBeDisabledByNullFilename() {
		Runner<Node> runner = createRunner().configureSampleLogging(null, 1);
		runner.run();
		assertFalse(Files.exists(tempDir.resolve("samples.log")));
	}

	@Test
	void sampleLoggingCanBeDisabledByZeroInterval() {
		Path file = tempDir.resolve("samples.log");
		Runner<Node> runner = createRunner().configureSampleLogging(file.toString(), 0);
		runner.run();
		assertFalse(Files.exists(file));
	}

	// -------------------- WEIGHT LOGGING --------------------

	@Test
	void weightLoggingCreatesFile() {
		Path file = tempDir.resolve("weights.log");
		Runner<Node> runner = createRunner().configureWeightLogging(file.toString(), 1);
		runner.run();
		assertTrue(Files.exists(file));
	}

	@Test
	void weightLoggingCanBeDisabledByNullFilename() {
		Runner<Node> runner = createRunner().configureWeightLogging(null, 1);
		runner.run();
		assertFalse(Files.exists(tempDir.resolve("weights.log")));
	}

	@Test
	void weightLoggingCanBeDisabledByZeroInterval() {
		Path file = tempDir.resolve("weights.log");
		Runner<Node> runner = createRunner().configureWeightLogging(file.toString(), 0);
		runner.run();
		assertFalse(Files.exists(file));
	}

	// -------------------- STATE LOGGING --------------------

	@Test
	void stateLoggingCreatesFile() throws IOException {
		Path prefix = tempDir.resolve("state");
		Runner<Node> runner = createRunner().configureStateLogging(Path.of("."), prefix.toString(), 1);
		runner.run();
		assertTrue(fileWithPrefixExists("state"));
	}

	@Test
	void stateLoggingCanBeDisabledByNullFilename() throws IOException {
		Runner<Node> runner = createRunner().configureStateLogging(Path.of("."), null, 1);
		runner.run();
		assertFalse(fileWithPrefixExists("state"));
	}

	@Test
	void stateLoggingCanBeDisabledByZeroInterval() throws IOException {
		Path prefix = tempDir.resolve("state");
		Runner<Node> runner = createRunner().configureStateLogging(Path.of("."), prefix.toString(), 0);
		runner.run();
		assertFalse(fileWithPrefixExists("state"));
	}

	// -------------------- HELPERS --------------------

	private boolean fileWithPrefixExists(String prefix) throws IOException {
		try (var files = Files.list(tempDir)) {
			return files.anyMatch(file -> file.getFileName().toString().startsWith(prefix));
		}
	}
}
