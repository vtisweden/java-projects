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
		return IntStream.range(0, roundTrip.size()).filter(i -> roundTrip.getNode(i).equals(node)).toArray();
	}

	public static <N extends Node> List<N> shortestPath(N from, N to, RoundTrip<N> roundTrip) {

		var fromIndices = findIndices(from, roundTrip);
		var toIndices = findIndices(to, roundTrip);
		if ((fromIndices.length == 0) || (toIndices.length == 0)) {
			return null;
		}

		if (from.equals(to)) {
			return Arrays.asList(from);
		}

		int shortestDist = Integer.MAX_VALUE;
		int bestFromIndex = -1;
		int bestToIndex = -1;
		for (int fromIndex : fromIndices) {
			for (int toIndex : toIndices) {
				int dist;
				if (fromIndex <= toIndex) {
					dist = toIndex - fromIndex;
				} else {
					dist = (toIndex + roundTrip.size()) - fromIndex;
				}
				if (dist < shortestDist) {
					shortestDist = dist;
					bestFromIndex = fromIndex;
					bestToIndex = toIndex;
				}
			}
		}

		if (bestToIndex < bestFromIndex) {
			bestToIndex += roundTrip.size();
		}
		ArrayList<N> result = new ArrayList<>(shortestDist + 1);
		for (int index = bestFromIndex; index <= bestToIndex; index++) {
			result.add(roundTrip.getNode(index % roundTrip.size()));
		}
		return result;
	}
}
