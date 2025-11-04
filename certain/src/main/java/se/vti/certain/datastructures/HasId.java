/**
 * se.vti.certain
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
package se.vti.certain.datastructures;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * @author GunnarF
 *
 */
public class HasId {

	// -------------------- MEMBERS --------------------

	private final String id;

	// -------------------- CONSTRUCTION --------------------

	@JsonCreator
	public HasId(@JsonProperty("id") String id) {
		this.id = id;
	}

	// -------------------- IMPLEMENTATION --------------------

	@JsonProperty("id")
	public String getId() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof HasId other) {
			return this.id.equals(((HasId) obj).id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
}
