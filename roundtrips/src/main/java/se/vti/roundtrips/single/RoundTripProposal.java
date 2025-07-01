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
package se.vti.roundtrips.single;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;
import se.vti.roundtrips.simulator.DefaultSimulator;
import se.vti.roundtrips.simulator.Simulator;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class RoundTripProposal<L extends Node> implements MHProposal<RoundTrip<L>> {

	// -------------------- MEMBERS --------------------

	private final RoundTripProposalParameters proposalParams;

	private final Scenario<L> scenario;

	private final Simulator<L> simulator;

	private final List<L> allLocations;

	private final Random rnd;

	// -------------------- CONSTRUCTION --------------------

	public RoundTripProposal(RoundTripProposalParameters proposalParams, Scenario<L> scenario,
			Simulator<L> simulator) {
		this.proposalParams = proposalParams;
		this.scenario = scenario;
		this.simulator = simulator;
		this.allLocations = scenario.getLocationsView();
		this.rnd = scenario.getRandom();
	}

	public RoundTripProposal(Scenario<L> scenario, Simulator<L> simulator) {
		this(new RoundTripProposalParameters(), scenario, simulator);
	}

	// -------------------- INTERNALS --------------------

	private L drawLocationDifferentFrom(L notThisOne) {
		if (this.allLocations.size() <= 1) {
			throw new RuntimeException("Not more than one location available.");
		}
		while (true) {
			final L location = this.allLocations.get(this.rnd.nextInt(this.allLocations.size()));
			if (!location.equals(notThisOne)) {
				return location;
			}
		}
	}

	private Integer drawUnusedDepartureTime(RoundTrip<?> state) {
		if (state.size() == this.scenario.getTimeBinCnt()) {
			throw new RuntimeException("No space for inserting a departure time bin.");
		}
		while (true) {
			final Integer depTime = this.rnd.nextInt(this.scenario.getTimeBinCnt());
			if (!state.containsDeparture(depTime)) {
				return depTime;
			}
		}
	}

	// for testing
	private boolean correctAction(RoundTripTransitionKernel<L> fwdTransitionKernel,
			RoundTripTransitionKernel<L> bwdTransitionKernel, RoundTripTransitionKernel.Action realizedFwdAction,
			RoundTrip<L> from, RoundTrip<L> to) {
		final RoundTripTransitionKernel.Action identifiedFwdAction = fwdTransitionKernel.identifyAction(to);
		final RoundTripTransitionKernel.Action identifiedBwdAction = bwdTransitionKernel.identifyAction(from);
		if (!identifiedFwdAction.equals(realizedFwdAction)) {
			return false;
		}
		if (RoundTripTransitionKernel.Action.INS.equals(identifiedFwdAction)) {
			return (RoundTripTransitionKernel.Action.REM.equals(identifiedBwdAction));
		} else if (RoundTripTransitionKernel.Action.REM.equals(identifiedFwdAction)) {
			return (RoundTripTransitionKernel.Action.INS.equals(identifiedBwdAction));
		} else if (RoundTripTransitionKernel.Action.FLIP_LOC.equals(identifiedFwdAction)) {
			return (RoundTripTransitionKernel.Action.FLIP_LOC.equals(identifiedBwdAction));
		} else if (RoundTripTransitionKernel.Action.FLIP_DEP.equals(identifiedFwdAction)) {
			return (RoundTripTransitionKernel.Action.FLIP_DEP.equals(identifiedBwdAction));
		} else {
			return false;
		}
	}

	// -------------------- IMPLEMENTATION OF INTERFACE --------------------

	@Override
	public RoundTrip<L> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> from) {

		assert (new LinkedHashSet<>(from.getDeparturesView()).size() == from.getDeparturesView().size());

		final RoundTripTransitionKernel<L> fwdTransitionKernel = new RoundTripTransitionKernel<>(from, this.scenario,
				this.proposalParams);

		final RoundTrip<L> to = from.clone();

		final RoundTripTransitionKernel.Action realizedFwdAction;
		final double _U = this.rnd.nextDouble();
		if (_U < fwdTransitionKernel.insertProba) { // INSERT

			realizedFwdAction = RoundTripTransitionKernel.Action.INS;
			final int whereToInsert = this.rnd.nextInt(from.size() + 1);
			final L whatLocationToInsert = this.allLocations.get(this.rnd.nextInt(this.allLocations.size()));
			final Integer whatDepTimeToInsert = this.drawUnusedDepartureTime(from);
			to.addAndEnsureSortedDepartures(whereToInsert, whatLocationToInsert, whatDepTimeToInsert);

		} else if (_U < fwdTransitionKernel.insertProba + fwdTransitionKernel.removeProba) { // REMOVE

			/*
			 * We need to draw location and departure index independently because it
			 * otherwise can happen that there is no one-step transition back after an
			 * insert.
			 */
			realizedFwdAction = RoundTripTransitionKernel.Action.REM;
			final int whereToRemoveLoc = this.rnd.nextInt(from.size());
			final int whereToRemoveDep = this.rnd.nextInt(from.size());
			to.remove(whereToRemoveLoc, whereToRemoveDep);

		} else if (_U < fwdTransitionKernel.insertProba + fwdTransitionKernel.removeProba
				+ fwdTransitionKernel.flipLocationProba) { // FLIP LOCATION

			realizedFwdAction = RoundTripTransitionKernel.Action.FLIP_LOC;
			final int whereToFlip = this.rnd.nextInt(from.size());
			final L newLocation = this.drawLocationDifferentFrom(from.getLocation(whereToFlip));
			to.setLocation(whereToFlip, newLocation);

		} else { // FLIP DEPARTURE TIME

			realizedFwdAction = RoundTripTransitionKernel.Action.FLIP_DEP;
			final int whereToFlip = this.rnd.nextInt(from.size());
			final Integer newDptTime = this.drawUnusedDepartureTime(from);
			to.setDepartureAndEnsureOrdering(whereToFlip, newDptTime);
		}

		to.setEpisodes(this.simulator.simulate(to));
		final RoundTripTransitionKernel<L> bwdTransitionKernel = new RoundTripTransitionKernel<>(to, this.scenario,
				this.proposalParams);

		assert (this.correctAction(fwdTransitionKernel, bwdTransitionKernel, realizedFwdAction, from, to));
		assert (fwdTransitionKernel.transitionProba(to) > 0);
		assert (bwdTransitionKernel.transitionProba(from) > 0);

		return new MHTransition<>(from, to, Math.log(fwdTransitionKernel.transitionProba(to)),
				Math.log(bwdTransitionKernel.transitionProba(from)));
	}
}
