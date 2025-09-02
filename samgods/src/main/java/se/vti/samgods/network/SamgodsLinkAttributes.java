/**
 * se.vti.samgods.readers
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.network;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsLinkAttributes {

	// -------------------- CONSTANTS --------------------

	public static final String ATTRIBUTE_NAME = "samgods";

	private static final String roadFerryNetworkMode = "p";
	private static final String railFerryNetworkMode = "q";

	public SamgodsConstants.TransportMode samgodsMode;

	public final Double speed1_km_h;

	public final Double speed2_km_h;

	public final boolean isDomestic;

	public final Set<String> networkModes;

	// -------------------- CONSTRUCTION --------------------

	public SamgodsLinkAttributes(SamgodsConstants.TransportMode samgodsMode, Double speed1_km_h, Double speed2_km_h,
			boolean isDomestic, String[] networkModes) {
		this.samgodsMode = samgodsMode;
		this.speed1_km_h = speed1_km_h;
		this.speed2_km_h = speed2_km_h;
		this.isDomestic = isDomestic;
		this.networkModes = Collections.unmodifiableSet(Arrays.stream(networkModes).collect(Collectors.toSet()));
	}

	// -------------------- IMPLEMENTATION --------------------

	public boolean isRoadFerryLink() {
		final boolean result = this.networkModes.contains(roadFerryNetworkMode);
		assert (!result || TransportMode.Ferry.equals(this.samgodsMode));
		return result;
	}

	public boolean isRailFerryLink() {
		final boolean result = this.networkModes.contains(railFerryNetworkMode);
		assert (!result || TransportMode.Ferry.equals(this.samgodsMode));
		return result;
	}

	public boolean isFerryLink() {
		return (this.isRoadFerryLink() || this.isRailFerryLink());
	}
}
