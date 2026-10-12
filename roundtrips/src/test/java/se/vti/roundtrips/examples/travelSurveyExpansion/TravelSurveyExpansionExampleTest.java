/**
 * se.vti.roundtrips
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
package se.vti.roundtrips.examples.travelSurveyExpansion;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author GunnarF
 */
class TravelSurveyExpansionExampleTest {

	@Test
	void test() {
		long[] stats = new TravelSurveyExpansionExample(4711).run(10_000);
		System.out.println(Arrays.toString(stats));
		Assertions.assertArrayEquals(new long[] { 123, 1335 }, stats);
	}
}
