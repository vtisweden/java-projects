/**
 * se.vti.samgods.logistics
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
package se.vti.samgods.logistics;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants.Commodity;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportDemandAndChains {

	// -------------------- MEMBERS --------------------

	private final Map<Commodity, Map<OD, List<TransportChain>>> commodity2od2transportChains = new LinkedHashMap<>();

	private final Map<Commodity, Map<OD, List<AnnualShipment>>> commodity2od2annualShipments = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION/COMPOSITION --------------------

	public TransportDemandAndChains() {
	}

	public void addChain(TransportChain transportChain) {
		this.commodity2od2transportChains.computeIfAbsent(transportChain.getCommodity(), c -> new LinkedHashMap<>())
				.computeIfAbsent(transportChain.getOD(), od -> new LinkedList<>()).add(transportChain);
	}

	public void addShipments(Commodity commodity, OD od, double singleInstanceAmount_ton, int numberOfInstances) {
		this.commodity2od2annualShipments.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
				.computeIfAbsent(od, od2 -> new LinkedList<>())
				.add(new AnnualShipment(commodity, od, singleInstanceAmount_ton, numberOfInstances));
	}

	// -------------------- GETTERS --------------------

	public Map<Commodity, Map<OD, List<TransportChain>>> getCommodity2od2transportChains() {
		return commodity2od2transportChains;
	}

	public Map<Commodity, Map<OD, List<AnnualShipment>>> getCommodity2od2annualShipments() {
		return commodity2od2annualShipments;
	}
}
