/**
 * se.vti.samgods.logistics
 * 
 * Copyright (C) 2024, 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.logistics.costs;

/**
 * 
 * @author GunnarF
 *
 */
public class NonTransportCost {

	public final double amount_ton_yr;

	public final double frequency_1_yr;

	public final double orderCost_1_yr;

	public final double enRouteMonetaryLoss_1_yr;

	public final double inventoryCost_1_yr;

	public NonTransportCost(double amount_ton_yr, double frequency_1_yr, double orderCost_1_yr,
			double enRouteMonetaryLoss_1_yr, double inventoryCost_1_yr) {
		this.amount_ton_yr = amount_ton_yr;
		this.frequency_1_yr = frequency_1_yr;
		this.orderCost_1_yr = orderCost_1_yr;
		this.enRouteMonetaryLoss_1_yr = enRouteMonetaryLoss_1_yr;
		this.inventoryCost_1_yr = inventoryCost_1_yr;
	}
}
