/**
 * se.vti.roundtrips.multiple
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
package se.vti.roundtrips.multiple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.single.RoundTrip;

/**
 * @author GunnarF
 */
class TestMultiRoundTripJsonIO {

	@TempDir
	static Path tempDir;

	@Test
	void test() throws IOException {
		Path tempFile = Files.createTempFile(tempDir, null, null);

		Scenario<Node> scenario = new Scenario<>();
		Node home = scenario.addNode(new Node("home"));
		Node work = scenario.addNode(new Node("work"));
		Node school = scenario.addNode(new Node("school"));
		MultiRoundTrip<Node> multiRoundTrip = new MultiRoundTrip<>(2);
		multiRoundTrip.setRoundTripAndUpdateSummaries(0,
				new RoundTrip<>(0, Arrays.asList(home, work), Arrays.asList(7, 18)));
		multiRoundTrip.setRoundTripAndUpdateSummaries(1,
				new RoundTrip<>(1, Arrays.asList(home, school), Arrays.asList(9, 13)));
		MultiRoundTripJsonIO.writeToFile(multiRoundTrip, tempFile.toString());
		multiRoundTrip = MultiRoundTripJsonIO.readFromFile(scenario, tempFile.toString());
		System.out.println(multiRoundTrip);

		Assertions.assertEquals(2, multiRoundTrip.size());
		Assertions.assertEquals(0, multiRoundTrip.getRoundTrip(0).getIndex());
		Assertions.assertEquals(1, multiRoundTrip.getRoundTrip(1).getIndex());

		Assertions.assertEquals(List.of(home, work), multiRoundTrip.getRoundTrip(0).getNodesView());
		Assertions.assertEquals(List.of(7, 18), multiRoundTrip.getRoundTrip(0).getDeparturesView());

		Assertions.assertEquals(List.of(home, school), multiRoundTrip.getRoundTrip(1).getNodesView());
		Assertions.assertEquals(List.of(9, 13), multiRoundTrip.getRoundTrip(1).getDeparturesView());
	}

}
