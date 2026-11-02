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

import se.vti.utils.misc.Units;

/**
 * 
 * @author GunnarF
 *
 */
class VesselClass implements Comparable<VesselClass> {

	final String name;
	final int length_m;
	final int width_m;
	final int draught_m;
	final int speed_km_h;
	final double marketShare;
	final double effectiveLengthFactor;
	final double effectiveWidthFactor;

	VesselClass(String name, int length_m, int width_m, int draught_m, int speed_km_h, double marketShare,
			double effectiveLengthFactor, double effectiveWidthFactor) {
		this.name = name;
		this.length_m = length_m;
		this.width_m = width_m;
		this.draught_m = draught_m;
		this.speed_km_h = speed_km_h;
		this.marketShare = marketShare;
		this.effectiveLengthFactor = effectiveLengthFactor;
		this.effectiveWidthFactor = effectiveWidthFactor;
	}

	double computeEffectiveWidth_m() {
		return this.effectiveWidthFactor * this.width_m;
	}

	int computeVolume_m3() {
		return this.length_m * this.width_m * this.draught_m;
	}

	double computeLength_km() {
		return Units.KM_PER_M * this.length_m;
	}

	double computeEffectiveLength_km() {
		return this.effectiveLengthFactor * this.computeLength_km();
	}

	double computeOvertakingLength_km(VesselClass other) {
		return this.computeEffectiveLength_km() + other.computeEffectiveLength_km();
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof VesselClass vc) {
			return this.name.equals(vc.name);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return String.format(
				"VesselClass{name='%s', length=%dm, width=%dm, draught=%dm, speed=%dkm/h, marketShare=%.4f, effectiveLengthFactor=%.2f, volume=%dm³}",
				name, length_m, width_m, draught_m, speed_km_h, marketShare, effectiveLengthFactor, computeVolume_m3());
	}

	@Override
	public int compareTo(VesselClass other) {
		// deeper first
		int draughtCompare = -Integer.compare(this.draught_m, other.draught_m);
		if (draughtCompare != 0) {
			return draughtCompare;
		}
		// larger first
		int volumeCompare = -Integer.compare(this.computeVolume_m3(), other.computeVolume_m3());
		if (volumeCompare != 0) {
			return volumeCompare;
		}
		// slower first
		int speedCompare = +Integer.compare(this.speed_km_h, other.speed_km_h);
		if (speedCompare != 0) {
			return speedCompare;
		}
		// fallback
		return this.name.compareTo(other.name);
	}
}
