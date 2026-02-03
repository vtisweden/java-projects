/**
 * georgia.weekly
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author GunnarF
 *
 */
public class ExpandedNodeUtils {

	private static final ExpandedNodeUtils singleton = new ExpandedNodeUtils();

	public static ExpandedNodeUtils singleton() {
		return singleton;
	}

	public ExpandedNodeUtils() {
	}

	private List<List<Enum<?>>> createWithNextDimension(List<List<Enum<?>>> labelsList,
			List<Class<? extends Enum<?>>> dimensions) {
		if (labelsList.get(0).size() == dimensions.size()) {
			return labelsList;
		} else {
			var nextDimension = dimensions.get(labelsList.get(0).size());
			var updatedLabelsList = new ArrayList<List<Enum<?>>>();
			for (var labels : labelsList) {
				for (var addedLabel : nextDimension.getEnumConstants()) {
					var updatedLabels = new ArrayList<>(labels);
					updatedLabels.add(addedLabel);
					updatedLabelsList.add(updatedLabels);
				}
			}
			return createWithNextDimension(updatedLabelsList, dimensions);
		}
	}

	public List<List<Enum<?>>> createAllLabels(List<Class<? extends Enum<?>>> dimensions) {
		return this.createWithNextDimension(Arrays.asList(Arrays.asList()), dimensions);
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		enum Activities {
			home, work, shop, leisure
		}

		enum DepartureModes {
			car, pt, bike, walk
		}

		enum Mood {
			unhappy, neutral, happy
		}

		for (var labels : ExpandedNodeUtils.singleton()
				.createAllLabels(Arrays.asList(Activities.class, DepartureModes.class, Mood.class))) {
			System.out.println(labels);
		}

	}
}
