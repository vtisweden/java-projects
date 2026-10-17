/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.runner.modified;

import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import se.vti.roundtrips.runner.Runner;
import se.vti.roundtrips.runner.basic.BasicProposal;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.scenario.Scenario;
import se.vti.roundtrips.scenario.ScenarioBuilder;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripUtils;

/**
 * @author GunnarF
 */
class ModifiedTest {

	Scenario<Node> createScenario() {
		var scenarioBuilder = new ScenarioBuilder<Node>().setRandom(new Random(4711)).setNumberOfTimeBins(24)
				.setTimeBinSize_h(1.0);
		for (int i = 1; i <= 10; i++) {
			scenarioBuilder.addNode(new Node("" + i));
		}
		scenarioBuilder.setMoveDistanceFunction((a, b) -> (a == b ? 0.0 : 1.0));
		scenarioBuilder.setMoveTimeFunction((a, b) -> (a == b ? 0.0 : 1.0));
		return scenarioBuilder.build();
	}

	RoundTrip<Node> computeFinalState(Runner<Node> runner) {
		runner.setInitialState(RoundTripUtils.singleton().createEmptyMultiRoundTrip(1)).setIndividualUniformPrior()
				.setMessageInterval(1000).setNumberOfIterations(10_000);
		runner.run();
		return runner.getFinalState().getRoundTrip(0);
	}

	@Test
	void testIdentityModificationChangesNothing() {
		var scenario = createScenario();
		var basicResult = computeFinalState(new Runner<Node>(scenario));
		scenario = createScenario();
		var modifiedResult = computeFinalState(new Runner<Node>(scenario, new ModifiedStateAdapter<Node>(scenario,
				x -> x.clone(), BasicProposal.createDefaultInstance(scenario, 1.0))));
		System.out.println(basicResult);
		System.out.println(modifiedResult);

		Assertions.assertEquals(basicResult.size(), modifiedResult.size());
		for (int i = 0; i < basicResult.size(); i++) {
			Assertions.assertEquals(basicResult.getNode(i).toString(), modifiedResult.getNode(i).toString());
			Assertions.assertEquals(basicResult.getDeparture(i), modifiedResult.getDeparture(i));
		}
	}
}
