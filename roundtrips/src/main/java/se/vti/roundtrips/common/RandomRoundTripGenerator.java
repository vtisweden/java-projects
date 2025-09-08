/**
 * se.vti.roundtrips.common
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
package se.vti.roundtrips.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class RandomRoundTripGenerator<N extends Node> {

	private final Logger log = LogManager.getLogger(RandomRoundTripGenerator.class);
	
	private final Scenario<N> scenario;
	private final Random rnd;

	private int minSize;

	private int maxSize;

	private long maxTrialsForGivenLength = 1_000;

	private long maxTrialsForAnyLength = 1_000;

	private boolean exceptionOnFailure = true;

	private Function<RoundTrip<N>, Boolean> feasibilityCheck = new Function<>() {
		@Override
		public Boolean apply(RoundTrip<N> t) {
			return true;
		}
	};

	public RandomRoundTripGenerator(Scenario<N> scenario) {
		this.scenario = scenario;
		this.rnd = scenario.getRandom();
		this.minSize = 1;
		this.maxSize = scenario.getMaxPossibleStayEpisodes();
	}

	public RandomRoundTripGenerator<N> setNumberOfStayEpisodesInterval(int minSize, int maxSize) {
		this.minSize = minSize;
		this.maxSize = maxSize;
		return this;
	}

	public RandomRoundTripGenerator<N> setMaxTrialsForGivenLength(long maxTrialsForGivenLength) {
		this.maxTrialsForGivenLength = maxTrialsForGivenLength;
		return this;
	}

	public RandomRoundTripGenerator<N> setMaxTrialsForAnyLength(long maxTrialsForAnyLength) {
		this.maxTrialsForAnyLength = maxTrialsForAnyLength;
		return this;
	}

	public RandomRoundTripGenerator<N> setFeasibilityCheck(Function<RoundTrip<N>, Boolean> feasibilityCheck) {
		this.feasibilityCheck = feasibilityCheck;
		return this;
	}

	public RandomRoundTripGenerator<N> setExceptionOnFailure(boolean exceptionOnFailure) {
		this.exceptionOnFailure = exceptionOnFailure;
		return this;
	}

	private RoundTrip<N> createRandomRoundTrip(int index, int size) {
		long trial = 0;

		do {
			List<N> allNodes = this.scenario.getNodesView();
			List<N> nodes = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				nodes.add(allNodes.get(this.rnd.nextInt(allNodes.size())));
			}

			List<Integer> allDepartures = new ArrayList<>(
					IntStream.range(0, this.scenario.getTimeBinCnt()).boxed().toList());
			Collections.shuffle(allDepartures, this.rnd);
			List<Integer> departures = allDepartures.subList(0, size);
			Collections.sort(departures);
			
			RoundTrip<N> candidate = new RoundTrip<>(index, nodes, departures);
			candidate.setEpisodes(this.scenario.getOrCreateSimulator().simulate(candidate));
			if (this.feasibilityCheck.apply(candidate)) {
				return candidate;
			}

		} while (++trial < this.maxTrialsForGivenLength);

		return null;
	}

	public RoundTrip<N> createRandomRoundTrip(int index) {
		log.info("Creating round trip with index " + index + ".");
		RoundTrip<N> candidate = null;
		long trial = 0;
		do {
			candidate = this.createRandomRoundTrip(index, this.rnd.nextInt(this.minSize, this.maxSize + 1));
		} while (((candidate == null) || (!this.feasibilityCheck.apply(candidate)))
				&& (++trial < this.maxTrialsForAnyLength));
		if (this.exceptionOnFailure && (candidate == null)) {
			throw new RuntimeException("Failure, too many trials.");
		}
		return candidate;
	}

	public void populateRandomly(MultiRoundTrip<N> multiRoundTrip) {
		for (int index = 0; index < multiRoundTrip.size(); index++) {
			if (index == 999) {
				System.out.print("");
			}
			multiRoundTrip.setRoundTripAndUpdateSummaries(index, this.createRandomRoundTrip(index));
		}
	}
}
