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
package se.vti.roundtrips.examples.multimodal;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author GunnarF
 */
class MultimodalExampleTest {

	@Test
	void testColdLocal() {
		long[] cnts1 = MultimodalExample.run(1.0, 10.0, 0.0, 5_000);
		System.out.println(Arrays.toString(cnts1));
		Assertions.assertArrayEquals(
				new long[] { 1, 1354, 3645, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, cnts1);
	}

	@Test
	void testColdGlobal() {
		long[] cnts1 = MultimodalExample.run(8.0, 8.0, 0.0, 5_000);
		System.out.println(Arrays.toString(cnts1));
		Assertions.assertArrayEquals(
				new long[] { 1, 20, 76, 0, 189, 0, 0, 0, 1382, 0, 0, 0, 0, 0, 0, 0, 3332, 0, 0, 0, 0, 0, 0, 0, 0 }, cnts1);
	}

	@Test
	void testColdHot() {
		long[] cnts1 = MultimodalExample.run(1.0, 8.0, 0.1, 5_000);
		System.out.println(Arrays.toString(cnts1));
		Assertions.assertArrayEquals(
				new long[] { 2, 125, 367, 0, 1201, 0, 0, 0, 2759, 0, 0, 0, 0, 0, 0, 0, 546, 0, 0, 0, 0, 0, 0, 0, 0 },
				cnts1);
	}

}
