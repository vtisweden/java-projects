/**
 * se.vti.roundtrips.simulator.electrified
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
package se.vti.roundtrips.simulator.electrified;

import se.vti.roundtrips.simulator.SimulatorState;

/**
 * @author GunnarF
 */
public class BatteryState implements SimulatorState {

	private final double capacity_kWh;
	private final double chargingRate_kW;
	private final double consumptionRate_kWh_km;

	private double charge_kWh;

	public BatteryState(double capacity_kWh, double chargingRate_kW, double consumptionRate_kWh_km, double charge_kWh) {
		this.capacity_kWh = capacity_kWh;
		this.chargingRate_kW = chargingRate_kW;
		this.consumptionRate_kWh_km = consumptionRate_kWh_km;
		this.charge_kWh = charge_kWh;
	}

	public double getCapacity_kWh() {
		return capacity_kWh;
	}

	public double getChargingRate_kW() {
		return chargingRate_kW;
	}

	public double getConsumptionRate_kWh_km() {
		return consumptionRate_kWh_km;
	}

	public double getCharge_kWh() {
		return charge_kWh;
	}

	public void setCharge_kWh(double charge_kWh) {
		this.charge_kWh = charge_kWh;
	}

	public void charge(double duration_h) {
		this.charge_kWh = Math.min(this.capacity_kWh, this.charge_kWh + duration_h * this.chargingRate_kW);
	}

	public void consume(double distance_km) {
		this.charge_kWh -= distance_km * this.consumptionRate_kWh_km;
	}

	public BatteryState clone() {
		BatteryState clone = new BatteryState(this.capacity_kWh, this.chargingRate_kW, this.getConsumptionRate_kWh_km(),
				this.charge_kWh);
		return clone;
	}
}
