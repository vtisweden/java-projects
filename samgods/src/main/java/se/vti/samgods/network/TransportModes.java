/**
 * se.vti.samgods.network
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportModes {

	private TransportModes() {
	}
	
	private static final char samgodsRoadFerryMode = 'P';
	private static final char samgodsRailFerryMode = 'Q';

	public static final List<Character> SAMGODS_FERRYMODES = Collections
			.unmodifiableList(Arrays.asList(samgodsRoadFerryMode, samgodsRailFerryMode));

	public static final Map<Character, TransportMode> CODE_2_SAMGODSMODE;
	static {
		LinkedHashMap<Character, TransportMode> tmpFileCode2samgodsMode = new LinkedHashMap<>();
		tmpFileCode2samgodsMode.put('A', SamgodsConstants.TransportMode.Road);
		tmpFileCode2samgodsMode.put('X', SamgodsConstants.TransportMode.Road);
		tmpFileCode2samgodsMode.put('D', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('d', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('E', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('F', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('f', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('J', SamgodsConstants.TransportMode.Sea);
		tmpFileCode2samgodsMode.put('K', SamgodsConstants.TransportMode.Sea);
		tmpFileCode2samgodsMode.put('L', SamgodsConstants.TransportMode.Sea);
		tmpFileCode2samgodsMode.put('V', SamgodsConstants.TransportMode.Sea);
		tmpFileCode2samgodsMode.put('B', SamgodsConstants.TransportMode.Road);
		tmpFileCode2samgodsMode.put('C', SamgodsConstants.TransportMode.Road);
		tmpFileCode2samgodsMode.put('S', SamgodsConstants.TransportMode.Road);
		tmpFileCode2samgodsMode.put('c', SamgodsConstants.TransportMode.Road);
		tmpFileCode2samgodsMode.put('G', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('H', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('h', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('I', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('T', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('U', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('i', SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('M', SamgodsConstants.TransportMode.Sea);
		tmpFileCode2samgodsMode.put('N', SamgodsConstants.TransportMode.Sea);
		tmpFileCode2samgodsMode.put('O', SamgodsConstants.TransportMode.Sea);
		tmpFileCode2samgodsMode.put('W', SamgodsConstants.TransportMode.Sea);
		tmpFileCode2samgodsMode.put(samgodsRoadFerryMode, SamgodsConstants.TransportMode.Road);
		tmpFileCode2samgodsMode.put(samgodsRailFerryMode, SamgodsConstants.TransportMode.Rail);
		tmpFileCode2samgodsMode.put('R', SamgodsConstants.TransportMode.Air);
		CODE_2_SAMGODSMODE = Collections.unmodifiableMap(tmpFileCode2samgodsMode);
	}


	
	/////

	public static String getMatsimModeIgnoreFerry(TransportMode samgodsMode) {
		if (TransportMode.Road.equals(samgodsMode)) {
			return org.matsim.api.core.v01.TransportMode.car;
		} else if (TransportMode.Rail.equals(samgodsMode)) {
			return org.matsim.api.core.v01.TransportMode.train;
		} else if (TransportMode.Sea.equals(samgodsMode)) {
			return org.matsim.api.core.v01.TransportMode.ship;
		} else if (TransportMode.Air.equals(samgodsMode)) {
			return org.matsim.api.core.v01.TransportMode.airplane;
		} else {
			throw new RuntimeException("No MATSim mode available for " + samgodsMode);
		}
	}

	public static Set<String> computeMatsimModesMapFerryToCarriedModes(SamgodsLinkAttributes linkAttributes) {
		final Set<String> result = new LinkedHashSet<>(2);
		if (linkAttributes.isFerryLink()) {
			// Could be both road and rail ferry:
			if (linkAttributes.isRoadFerryLink()) {
				result.add(org.matsim.api.core.v01.TransportMode.car);
			}
			if (linkAttributes.isRailFerryLink()) {
				result.add(org.matsim.api.core.v01.TransportMode.train);
			}
		} else {
			result.add(getMatsimModeIgnoreFerry(linkAttributes.samgodsMode));
		}
		return result;
	}
}
