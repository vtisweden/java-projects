/**
 * se.vti.samgods.network
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
package se.vti.samgods.network;

import org.matsim.api.core.v01.network.Link;

/**
 * Do not use in parallel code!
 * 
 * @author GunnarF
 *
 */
public class SamgodsNetworkUtils {

	// For use without instantiation.
	public static final SamgodsNetworkUtils instance = new SamgodsNetworkUtils();
	
	// For parallel use.
	public SamgodsNetworkUtils() {		
	}
	
	public SamgodsLinkAttributes getLinkAttrs(Link link) {
		return (SamgodsLinkAttributes) link.getAttributes()
				.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME);
	}

}
