package se.vti.roundtrips.common;

/**
 * se.vti.samgods.transportation.loops
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import se.vti.roundtrips.single.RoundTrip;

/**
 * @author GunnarF
 */
public class RoundTripUtils {

	private RoundTripUtils() {
	}

	public static <N extends Node> int[] findIndices(N node, RoundTrip<N> roundTrip) {
		return IntStream.range(0, roundTrip.size()).filter(i -> roundTrip.getNode(i) == node).toArray();
	}

	public static <N extends Node> List<int[]> shortestPaths(N from, N to, RoundTrip<N> roundTrip) {

		int size = roundTrip.size();
		if (size == 0) {
			return Collections.emptyList();
		}

		var fromIndices = findIndices(from, roundTrip);
		var toIndices = findIndices(to, roundTrip);
		if ((fromIndices.length == 0) || (toIndices.length == 0)) {
			return Collections.emptyList();
		}

		if (from.equals(to)) {
			return Arrays.stream(fromIndices).boxed().map(i -> new int[] { i }).toList();
		}

		int shortestDist = Integer.MAX_VALUE;
		List<Integer> bestFromIndices = new ArrayList<>();
		List<Integer> bestToIndices = new ArrayList<>();
		for (int fromIndex : fromIndices) {
			for (int toIndex : toIndices) {

				int effectiveToIndex;
				if (fromIndex <= toIndex) {
					effectiveToIndex = toIndex;
				} else {
					effectiveToIndex = toIndex + size;
				}

				int dist = effectiveToIndex - fromIndex;
				if (dist <= shortestDist) {
					if (dist < shortestDist) {
						shortestDist = dist;
						bestFromIndices = new ArrayList<>();
						bestToIndices = new ArrayList<>();
					}
					bestFromIndices.add(fromIndex);
					bestToIndices.add(effectiveToIndex);
				}
			}
		}

		List<int[]> result = new ArrayList<>(bestFromIndices.size());
		for (int pathIndex = 0; pathIndex < bestFromIndices.size(); pathIndex++) {
			int fromIndex = bestFromIndices.get(pathIndex);
			int toIndex = bestToIndices.get(pathIndex);
			result.add(IntStream.rangeClosed(fromIndex, toIndex).map(i -> i % size).toArray());
		}

		return result;
	}
}
