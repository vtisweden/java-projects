/**
 * se.vti.seas
 * 
 * Copyright (C) 2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.seas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author GunnarF
 */
public class DiscretizedChannelGeometry {

	public final List<Double> outerCoordinates_m;
	public final List<Integer> draughts_m;

	public DiscretizedChannelGeometry(List<Double> outerCoordinates_m, List<Integer> draughts_m) {
		this.outerCoordinates_m = Collections.unmodifiableList(new ArrayList<>(outerCoordinates_m));
		this.draughts_m = Collections.unmodifiableList(new ArrayList<>(draughts_m));
	}

	public int binCnt() {
		return this.outerCoordinates_m.size();
	}
	
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer("maxLat_m\tdraught_m\n");
		for (int i = 0; i < outerCoordinates_m.size(); i++) {
			result.append(this.outerCoordinates_m.get(i));
			result.append("\t");
			result.append(this.draughts_m.get(i));
			result.append("\n");
		}
		return result.toString();
	}
}
