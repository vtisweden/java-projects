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
import java.util.List;

/**
 * @author GunnarF
 */
public class OneSidedChannelGeometry {

	final List<Double> widths_m;
	final List<Integer> draughts_m;

	OneSidedChannelGeometry(List<Double> widths_m, List<Integer> draughts_m) {
		this.widths_m = widths_m;
		this.draughts_m = draughts_m;
	}

	int lanes() {
		return this.widths_m.size();
	}

	double maxWidth_m() {
		return this.widths_m.stream().mapToDouble(w -> w).sum();
	}

	public DiscretizedChannelGeometry createDiscretizedGeometry(double stepSize_m) {

		List<Double> outerSliceCoord_m = new ArrayList<>();
		final double maxWidth_m = this.maxWidth_m();
		double maxOuterSliceCoord_m = 0.0;
		while (maxOuterSliceCoord_m < maxWidth_m) {
			maxOuterSliceCoord_m += stepSize_m;
			outerSliceCoord_m.add(maxOuterSliceCoord_m);
		}

		final double[] outerLaneCoord_m = new double[this.lanes()];
		double sum_m = 0.0;
		for (int i = 0; i < this.lanes(); i++) {
			sum_m += this.widths_m.get(i);
			outerLaneCoord_m[i] = sum_m;
		}

		List<Integer> draughtList_m = new ArrayList<>(outerSliceCoord_m.size());
		int laneIndex = 0;
		for (int sliceIndex = 0; sliceIndex < outerSliceCoord_m.size(); sliceIndex++) {
			double outeSliceCoord_m = outerSliceCoord_m.get(sliceIndex);
			while ((laneIndex < outerLaneCoord_m.length - 1) && (outeSliceCoord_m > outerLaneCoord_m[laneIndex])) {
				laneIndex++;
			}
			draughtList_m.add(this.draughts_m.get(laneIndex));
		}

		return new DiscretizedChannelGeometry(outerSliceCoord_m, draughtList_m);
	}
}
