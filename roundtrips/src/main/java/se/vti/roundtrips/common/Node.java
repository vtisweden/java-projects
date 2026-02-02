/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023,2024 by Gunnar Flötteröd (VTI, LiU).
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 * @author GunnarF
 *
 */
public class Node {

	// -------------------- CONSTANTS --------------------

	private final String name;

	private final String basicName;

	private final List<? extends Enum<?>> labels;

	// -------------------- CONSTRUCTION --------------------

	public Node(String basicName, List<? extends Enum<?>> labels) {
		this.basicName = basicName;
		this.name = createName(basicName, labels);
		if (labels == null || labels.size() == 0) {
			this.labels = null;
		} else {
			this.labels = Collections.unmodifiableList(new ArrayList<>(labels));
		}
	}

	public Node(String basicName, Enum<?>... labels) {
		this(basicName, Arrays.asList(labels));
	}

	// -------------------- IMPLEMENTATION --------------------

	public static String createName(String basicName, List<? extends Enum<?>> labels) {
		if (labels == null || labels.size() == 0) {
			return basicName;
		} else {
			return basicName + "[" + labels.stream().map(l -> l.toString()).collect(Collectors.joining(",")) + "]";
		}
	}

	public static String createName(String basicName, Enum<?>... labels) {
		return createName(basicName, Arrays.asList(labels));
	}

	public String getName() {
		return this.name;
	}

	public String getBasicName() {
		return this.basicName;
	}

	public List<? extends Enum<?>> getLabels() {
		return this.labels;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
