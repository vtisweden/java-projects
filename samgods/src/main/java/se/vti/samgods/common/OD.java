/**
 * se.vti.samgods
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.common;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

/**
 * 
 * @author GunnarF
 *
 */
public class OD {

	public final Id<Node> origin;
	public final Id<Node> destination;

	public OD(Id<Node> origin, Id<Node> destination) {
		this.origin = origin;
		this.destination = destination;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (other instanceof OD) {
			final OD otherOD = (OD) other;
			return (this.origin.equals(otherOD.origin) && (this.destination.equals(otherOD.destination)));
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.origin.hashCode() + 31 * this.destination.hashCode();
	}

	@Override
	public String toString() {
		return "OD(" + this.origin + "," + this.destination + ")";
	}
}
