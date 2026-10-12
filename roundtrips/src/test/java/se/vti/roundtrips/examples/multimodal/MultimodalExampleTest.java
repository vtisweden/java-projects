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
				new long[] { 1, 1403, 3596, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, cnts1);
	}

	@Test
	void testColdGlobal() {
		long[] cnts1 = MultimodalExample.run(8.0, 8.0, 0.0, 5_000);
		System.out.println(Arrays.toString(cnts1));
		Assertions.assertArrayEquals(
				new long[] { 1, 5, 5, 0, 65, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 4923, 0, 0, 0, 0, 0, 0, 0, 0 }, cnts1);
	}

	@Test
	void testColdHot() {
		long[] cnts1 = MultimodalExample.run(1.0, 8.0, 0.1, 5_000);
		System.out.println(Arrays.toString(cnts1));
		Assertions.assertArrayEquals(
				new long[] { 2, 323, 1008, 0, 924, 0, 0, 0, 948, 0, 0, 0, 0, 0, 0, 0, 1795, 0, 0, 0, 0, 0, 0, 0, 0 },
				cnts1);
	}

}
