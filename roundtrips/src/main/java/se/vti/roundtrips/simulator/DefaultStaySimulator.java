/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023,2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.simulator;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.simulator.DefaultSimulator.StaySimulator;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class DefaultStaySimulator<N extends Node> implements StaySimulator<N> {

	protected final Scenario<N> scenario;

	public DefaultStaySimulator(Scenario<N> scenario) {
		this.scenario = scenario;
	}

	public SimulatorState computeFinalState(RoundTrip<N> roundTrip, StayEpisode<N> parking) {
		return null;
	}

	@Override
	public StayEpisode<N> newStayEpisode(RoundTrip<N> roundTrip, int indexWithinRoundTrip, double time_h,
			SimulatorState initialState) {
		final StayEpisode<N> stay = new StayEpisode<>(roundTrip.getNode(indexWithinRoundTrip));
		stay.setInitialState(initialState);

		stay.setEndTime_h(Math.max(time_h, this.scenario.getBinSize_h() * roundTrip.getDeparture(indexWithinRoundTrip)));
		stay.setDuration_h(stay.getEndTime_h() - time_h);

		stay.setFinalState(this.computeFinalState(roundTrip, stay));

		return stay;
	}

}
