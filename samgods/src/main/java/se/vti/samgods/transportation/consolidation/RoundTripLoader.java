/**
 * se.vti.samgods.transportation.consolidation
 * 
 * Copyright (C) 2024-2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.transportation.consolidation;

import se.vti.roundtrips.single.RoundTrip;
import se.vti.samgods.common.SamgodsConstants;

/**
 * @author GunnarF
 */
class RoundTripLoader {

	private final VehicleLoopManager loopManager;
	
	RoundTripLoader(VehicleLoopManager loopManager) {
		this.loopManager = loopManager;
	}
	
	void load(String fileName, SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode) {
		// TODO to use the default loader, this requires to first build a sampling scenario.
		throw new UnsupportedOperationException();
	}
	
	VehicleLoop createVehicleLoop(RoundTrip<?> roundTrip) {
		// TODO See comment in load function -- need to decide on sampling node type.
		// Extract boilerplate code from SamgodsLoopSamplingRunner.
		throw new UnsupportedOperationException();
	}
	
	public static void main(String[] args) {
		
	}
	
}
