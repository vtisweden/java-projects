/**
 * se.vti.utils
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
package se.vti.utils.misc.metropolishastings;

/**
 * Based on floetteroed.utilities.math.metropolishastings package.
 * 
 * @author GunnarF
 * 
 * @param <S>
 */
public interface MHWeight<S extends Object> {

	double logWeight(final S state);

	default String name() {
		return this.getClass().getSimpleName();
	}
	
	default boolean allowsForWeightsOtherThanOneInMHWeightContainer() {
		return true;
	}
	
}
