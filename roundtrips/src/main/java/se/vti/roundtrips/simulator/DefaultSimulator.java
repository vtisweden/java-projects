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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class DefaultSimulator<N extends Node> implements Simulator<N> {

	// -------------------- INTERFACES --------------------

	public interface StaySimulator<N extends Node> {

		StayEpisode<N> newStayEpisode(RoundTrip<N> roundTrip, int indexWithingRoundTrip, double initialTime_h,
				SimulatorState initialState);
	}

	public interface MoveSimulator<N extends Node> {

		MoveEpisode<N> newMoveEpisode(RoundTrip<N> roundTrip, int indexWithingRoundTrip, double initialTime_h,
				SimulatorState initialState);
	}

	public interface WrapAroundSimulator {

		SimulatorState createInitializeState(RoundTrip<?> roundTrip);

		SimulatorState keepOrChangeInitialState(RoundTrip<?> roundTrip, SimulatorState oldInitialState,
				SimulatorState newInitialState);
	}

	// -------------------- MEMBERS --------------------

	protected final Scenario<N> scenario;

	private MoveSimulator<N> moveSimulator = null;
	private StaySimulator<N> staySimulator = null;
	private WrapAroundSimulator wrapAroundSimulator = null;

	// -------------------- CONSTRUCTION --------------------

	public DefaultSimulator(Scenario<N> scenario) {
		this.scenario = scenario;
		this.setMoveSimulator(new DefaultMoveSimulator<>(scenario));
		this.setStaySimulator(new DefaultStaySimulator<>(scenario));
		this.setWrapAroundSimulator(new WrapAroundSimulator() {
			@Override
			public SimulatorState createInitializeState(RoundTrip<?> roundTrip) {
				return null;
			}

			@Override
			public SimulatorState keepOrChangeInitialState(RoundTrip<?> roundTrip, SimulatorState oldInitialState,
					SimulatorState newInitialState) {
				return oldInitialState;
			}
		});
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public Scenario<N> getScenario() {
		return this.scenario;
	}

	public void setMoveSimulator(MoveSimulator<N> drivingSimulator) {
		this.moveSimulator = drivingSimulator;
	}

	public void setStaySimulator(StaySimulator<N> parkingSimulator) {
		this.staySimulator = parkingSimulator;
	}

	public void setWrapAroundSimulator(WrapAroundSimulator wrapAroundSimulator) {
		this.wrapAroundSimulator = wrapAroundSimulator;
	}

	// -------------------- HOOKS FOR SUBCLASSING --------------------

	public StayEpisode<N> createHomeOnlyEpisode(RoundTrip<N> roundTrip) {
		StayEpisode<N> home = new StayEpisode<>(roundTrip.getNode(0));
		home.setDuration_h(this.scenario.getPeriodLength_h());
		home.setEndTime_h(this.scenario.getPeriodLength_h() - 1e-8); // wraparound
		home.setInitialState(this.wrapAroundSimulator.createInitializeState(roundTrip));
		home.setFinalState(this.wrapAroundSimulator.createInitializeState(roundTrip));
		return home;
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public List<Episode> simulate(RoundTrip<N> roundTrip) {

		if (roundTrip.size() == 1) {
			return Collections.singletonList(this.createHomeOnlyEpisode(roundTrip));
		}

		final double initialTime_h = this.scenario.getBinSize_h() * roundTrip.getDeparture(0);
		SimulatorState initialState = this.wrapAroundSimulator.createInitializeState(roundTrip);

		List<Episode> episodes = null;
		do {

			episodes = new ArrayList<>(2 * roundTrip.size() - 1);
			episodes.add(null); // placeholder for home episode

			double time_h = initialTime_h;
			SimulatorState currentState = initialState;

			for (int index = 0; index < roundTrip.size() - 1; index++) {

				final MoveEpisode<N> moving = this.moveSimulator.newMoveEpisode(roundTrip, index, time_h, currentState);
				episodes.add(moving);
				time_h = moving.getEndTime_h();
				currentState = moving.getFinalState();

				final StayEpisode<N> staying = this.staySimulator.newStayEpisode(roundTrip, index + 1, time_h,
						currentState);
				episodes.add(staying);
				time_h = staying.getEndTime_h();
				currentState = staying.getFinalState();
			}

			final MoveEpisode<N> moving = this.moveSimulator.newMoveEpisode(roundTrip, roundTrip.size() - 1, time_h,
					currentState);
			episodes.add(moving);
			time_h = moving.getEndTime_h();
			currentState = moving.getFinalState();

			final StayEpisode<N> home = this.staySimulator.newStayEpisode(roundTrip, 0,
					time_h - this.scenario.getPeriodLength_h(), currentState);
			episodes.set(0, home);

			final SimulatorState newInitialState = this.wrapAroundSimulator.keepOrChangeInitialState(roundTrip,
					initialState, home.getFinalState());
			if (newInitialState == initialState) {
				// accept wrap-around
				home.setFinalState(initialState);
			} else {
				// try again
				episodes = null;
				initialState = newInitialState;
			}
		} while (episodes == null);

		return episodes;
	}
}
