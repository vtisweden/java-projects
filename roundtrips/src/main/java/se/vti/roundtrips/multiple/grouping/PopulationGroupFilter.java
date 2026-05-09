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
package se.vti.roundtrips.multiple.grouping;

import java.util.Iterator;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class PopulationGroupFilter<L extends Node> {

	// -------------------- MEMBERS --------------------

	private final String groupName;

	private final int[] indices;

	// -------------------- CONSTRUCTION --------------------

	public PopulationGroupFilter(String groupName, int[] indices) {
		this.groupName = groupName;
		this.indices = indices;
	}

	// -------------------- IMPLEMENTATION --------------------

	public String getGroupName() {
		return this.groupName;
	}

	public int getGroupSize() {
		return this.indices.length;
	}

	public Iterator<RoundTrip<L>> filteredIterator(MultiRoundTrip<L> multiRoundTrip) {
		return new Iterator<>() {
			private int i = 0;

			@Override
			public boolean hasNext() {
				return (this.i < indices.length);
			}

			@Override
			public RoundTrip<L> next() {
				return multiRoundTrip.getRoundTrip(indices[i++]);
			}
		};
	}

	public Iterable<RoundTrip<L>> filteredIterable(MultiRoundTrip<L> multiRoundTrip) {
		return new Iterable<>() {
			@Override
			public Iterator<RoundTrip<L>> iterator() {
				return filteredIterator(multiRoundTrip);
			}
		};
	}
}
