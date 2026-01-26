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

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.simulator.DefaultMoveSimulator;
import se.vti.roundtrips.simulator.MoveEpisode;
import se.vti.roundtrips.simulator.SimulatorState;
import se.vti.roundtrips.single.RoundTrip;

/**
 * @author GunnarF
 */
public class ElectrifiedMoveSimulator<N extends Node> extends DefaultMoveSimulator<N> {

	public ElectrifiedMoveSimulator(Scenario<N> scenario) {
		super(scenario);
	}

	@Override
	public SimulatorState computeFinalState(RoundTrip<N> roundTrip, MoveEpisode<N> driving) {
		BatteryState initialState = (BatteryState) driving.getInitialState();		
		BatteryState finalState = initialState.clone();
		
		finalState.consume(this.scenario.getDistance_km(driving.getOrigin(), driving.getDestination()));
		return finalState;
	}

}
