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
package se.vti.samgods.logistics;

import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants;

/**
 * Represents the annual shipments of one or more independent shippers. Meaning
 * that the singleInstanceAmount_ton is not the shipment size but the amount
 * shipped in a single shipper relation (an "instance").
 * 
 * @author GunnarF
 *
 */
public class AnnualShipment {

	// -------------------- CONSTANTS --------------------

	private final SamgodsConstants.Commodity commodity;

	private final OD od;

	private final double singleInstanceAnnualAmount_ton;

	private final int numberOfInstances;

	// -------------------- CONSTRUCTION --------------------

	public AnnualShipment(SamgodsConstants.Commodity commodity, OD od, double singleInstanceAnnualAmount_ton,
			int numberOfInstances) {
		this.commodity = commodity;
		this.od = od;
		this.singleInstanceAnnualAmount_ton = singleInstanceAnnualAmount_ton;
		this.numberOfInstances = numberOfInstances;
	}

	public AnnualShipment createSingleInstance() {
		return new AnnualShipment(this.commodity, this.od, this.singleInstanceAnnualAmount_ton, 1);
	}

	// -------------------- GETTERS --------------------

	public SamgodsConstants.Commodity getCommodity() {
		return this.commodity;
	}

	public OD getOD() {
		return this.od;
	}

	public double getSingleInstanceAnnualAmount_ton() {
		return this.singleInstanceAnnualAmount_ton;
	}

	public int getNumberOfInstances() {
		return this.numberOfInstances;
	}

	public double getTotalAmount_ton() {
		return this.singleInstanceAnnualAmount_ton * this.numberOfInstances;
	}

}