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

import se.vti.roundtrips.simulator.DefaultSimulator.WrapAroundSimulator;
import se.vti.roundtrips.simulator.SimulatorState;
import se.vti.roundtrips.single.RoundTrip;

/**
 * @author GunnarF
 */
public class BatteryWrapAroundSimulator implements WrapAroundSimulator {

	private final double defaultCapacity_kWh;
	private final double defaultChargingRate_kW;
	private final double defaultConsumptionRate_kWh_km;
	private final double chargeWrapAroundTolerance_kWh;

	public BatteryWrapAroundSimulator(double defaultCapacity_kWh, double defaultChargingRate_kW,
			double defaultConsumptionRate_kWh_km, double chargeWraparoundTolerance_kWh) {
		this.defaultCapacity_kWh = defaultCapacity_kWh;
		this.defaultChargingRate_kW = defaultChargingRate_kW;
		this.defaultConsumptionRate_kWh_km = defaultConsumptionRate_kWh_km;
		this.chargeWrapAroundTolerance_kWh = chargeWraparoundTolerance_kWh;
	}

	@Override
	public SimulatorState createInitializeState(RoundTrip<?> roundTrip) {
		return new BatteryState(this.defaultCapacity_kWh, this.defaultChargingRate_kW,
				this.defaultConsumptionRate_kWh_km, this.defaultCapacity_kWh);
	}

	@Override
	public SimulatorState keepOrChangeInitialState(RoundTrip<?> roundTrip, SimulatorState oldInitialState,
			SimulatorState newInitialState) {
		BatteryState oldInitialBatteryState = (BatteryState) oldInitialState;
		BatteryState newInitialBatteryState = (BatteryState) newInitialState;
		if ((newInitialBatteryState.getCharge_kWh() < oldInitialBatteryState.getCharge_kWh()
				- this.chargeWrapAroundTolerance_kWh)
				&& (newInitialBatteryState.getCharge_kWh() >= -this.chargeWrapAroundTolerance_kWh)) {
			// If charge is too negative, this cannot be aligned and needs to be handled elsewhere.
			return newInitialBatteryState;
		} else {
			return oldInitialBatteryState;
		}
	}

}
