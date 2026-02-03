/**
 * se.vti.roundtrips.multiple.grouping
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
package se.vti.roundtrips.multiple.grouping;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

/**
 * @author GunnarF
 */
class TestPopulationGrouping {

	@Test
	void test() {
		int size = 14;

		PopulationGrouping g = new PopulationGrouping(size);
		g.addGroup("a", 1.0);
		g.addGroup("b", 2.0);
		g.addGroup("c", 4.0);

		g.ensureIndexing();

		assertArrayEquals(new int[] { 8, 11 }, g.getGroup2Indices().get("a"));
		assertArrayEquals(new int[] { 4, 6, 9, 12 }, g.getGroup2Indices().get("b"));
		assertArrayEquals(new int[] { 0, 1, 2, 3, 5, 7, 10, 13 }, g.getGroup2Indices().get("c"));
	}

}
