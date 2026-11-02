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

/**
 * @author GunnarF
 */
class Passage {

	Double secondsSinceFirstCrossing;
	String mmsi;
	String imo;
	String cargoType;
	Double draught_m;
	Double width_m;
	Double length_m;
	Double speed_km_h;
	String crossingCompassDirection;
	String shipType;
	Double lateralCoord_m;
	
	// Such that coordinates increase when going from direction-A passages to direction-B passages.
	Double signCorrectedLatCoord_m;

	Passage(Double secondsSinceFirstCrossing, String mmsi, String imo, String cargoType, Double draught_m,
			Double width_m, Double length_m, Double speed_km_h, String crossingCompassDirection, String shipType,
			Double lateralCoord_m) {
		this.secondsSinceFirstCrossing = secondsSinceFirstCrossing;
		this.mmsi = mmsi;
		this.imo = imo;
		this.cargoType = cargoType;
		this.draught_m = draught_m;
		this.width_m = width_m;
		this.length_m = length_m;
		this.speed_km_h = speed_km_h;
		this.crossingCompassDirection = crossingCompassDirection;
		this.shipType = shipType;
		this.lateralCoord_m = lateralCoord_m;
		this.signCorrectedLatCoord_m = null;
	}

	double computeVolume_m3() {
		return this.draught_m * this.width_m * this.length_m;
	}

	boolean isAssignmentComplete() {
		return (this.shipType != null) && (this.draught_m != null) && (this.draught_m > 0) && (this.width_m != null)
				&& (this.width_m > 0) && (this.length_m != null) && (this.length_m > 0) && (this.speed_km_h != null)
				&& (this.speed_km_h > 0);
	}
}