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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import se.vti.samgods.common.SamgodsConstants;

/**
 * 
 * @author GunnarF
 *
 */
public class NonTransportCostModel_v1_22 implements NonTransportCostModel {

	private final double annualInterestRate = 0.1;

	private final ConcurrentMap<SamgodsConstants.Commodity, Double> commodity2value_1_ton = new ConcurrentHashMap<>(
			SamgodsConstants.commodityCnt());

	private final ConcurrentMap<SamgodsConstants.Commodity, Double> commodity2inventoryCost_1_yrTon = new ConcurrentHashMap<>(
			SamgodsConstants.commodityCnt());

	private final ConcurrentMap<SamgodsConstants.Commodity, Double> commodity2orderCost = new ConcurrentHashMap<>(
			SamgodsConstants.commodityCnt());

	public NonTransportCostModel_v1_22() {

		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.AGRICULTURE, 3764.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.AIR, 1339679.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.BASICMETALS, 21465.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.CHEMICALS, 23428.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.COAL, 4142.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.COKE, 5736.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.FOOD, 21342.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.FURNITURE, 41191.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.MACHINERY, 202922.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.METAL, 1075.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.OTHERMINERAL, 5055.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.SECONDARYRAW, 3461.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.TEXTILES, 172287.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.TIMBER, 850.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.TRANSPORT, 103911.0);
		this.commodity2value_1_ton.put(SamgodsConstants.Commodity.WOOD, 6641.0);

		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.AGRICULTURE, 1129.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.AIR, 401904.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.BASICMETALS, 6440.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.CHEMICALS, 7028.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.COAL, 1243.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.COKE, 1721.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.FOOD, 6402.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.FURNITURE, 12357.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.MACHINERY, 60877.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.METAL, 323.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.OTHERMINERAL, 1517.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.SECONDARYRAW, 1039.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.TEXTILES, 51686.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.TIMBER, 255.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.TRANSPORT, 31173.0);
		this.commodity2inventoryCost_1_yrTon.put(SamgodsConstants.Commodity.WOOD, 1992.0);

		this.commodity2orderCost.put(SamgodsConstants.Commodity.AGRICULTURE, 855.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.AIR, 936.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.BASICMETALS, 727.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.CHEMICALS, 660.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.COAL, 1059.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.COKE, 957.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.FOOD, 725.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.FURNITURE, 1082.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.MACHINERY, 677.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.METAL, 861.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.OTHERMINERAL, 708.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.SECONDARYRAW, 1181.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.TEXTILES, 701.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.TIMBER, 1518.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.TRANSPORT, 716.0);
		this.commodity2orderCost.put(SamgodsConstants.Commodity.WOOD, 713.0);
	}

	@Override
	public NonTransportCost computeNonTransportCost(SamgodsConstants.Commodity commodity,
			SamgodsConstants.ShipmentSize shipmentSize, double annualAmount_ton, double transportChainDuration_h) {
		var frequency_1_yr = annualAmount_ton / shipmentSize.getRepresentativeValue_ton();
		var orderCost_1_yr = this.commodity2orderCost.get(commodity) * frequency_1_yr;
		var enRouteLoss_1_yr = this.annualInterestRate * (transportChainDuration_h / 365.0 / 24.0)
				* this.commodity2value_1_ton.get(commodity) * annualAmount_ton;
		var inventoryCost_1_yr = this.commodity2inventoryCost_1_yrTon.get(commodity)
				* (shipmentSize.getRepresentativeValue_ton() / 2.0);
		return new NonTransportCost(annualAmount_ton, frequency_1_yr, orderCost_1_yr, enRouteLoss_1_yr,
				inventoryCost_1_yr);
	}
}
