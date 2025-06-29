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
package se.vti.roundtrips.model;

import se.vti.roundtrips.single.Node;

/**
 * 
 * @author GunnarF
 *
 */
public class StayEpisode<L extends Node> extends Episode {
	
	private final L location;
	
	// TODO NEW
	@Override
	public void deepCopyInto(Episode target) {
		throw new RuntimeException("Use clone().");
	}
	
	// TODO NEW
	@Override
	public StayEpisode<L> clone() {
		StayEpisode<L> result = new StayEpisode<>(this.location);
		super.deepCopyInto(result);
		return result;
	}

	public StayEpisode(L location) {
		this.location = location;
	}

	public L getLocation() {
		return location;
	}
	
	@Override
	public String toString() {
		return super.toString() + ",loc(" + this.location + ")";
	}

}
