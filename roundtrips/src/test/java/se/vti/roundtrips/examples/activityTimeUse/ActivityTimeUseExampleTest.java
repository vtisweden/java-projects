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
package se.vti.roundtrips.examples.activityTimeUse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import se.vti.roundtrips.single.RoundTrip;

/**
 * @author GunnarF
 */
class ActivityTimeUseExampleTest {

	@Test
	void test() {
		RoundTrip<?> result = new ActivityTimeUseExample(4711).run(10_000);
		System.out.println(result);

		Assertions.assertEquals("(0,0)[HOME]", result.getNode(0).toString());
		Assertions.assertEquals(19, result.getDeparture(0));
		
		Assertions.assertEquals("(2,2)[WORK]", result.getNode(1).toString());
		Assertions.assertEquals(59, result.getDeparture(1));

		Assertions.assertEquals("(0,2)[OTHER]", result.getNode(2).toString());
		Assertions.assertEquals(69, result.getDeparture(2));
	}
}
