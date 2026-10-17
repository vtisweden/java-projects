/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.runner.modified;

import java.util.function.Function;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.runner.MultiRoundTripWrapper;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.simulator.Simulator;

/**
 * @author GunnarF
 */
public class ModifiedMultiRoundTrip<N extends Node> implements MultiRoundTripWrapper<N> {

	private final Function<MultiRoundTrip<N>, MultiRoundTrip<N>> modifier;

	private final MultiRoundTrip<N> original;

	private final MultiRoundTrip<N> modified;

	ModifiedMultiRoundTrip(Function<MultiRoundTrip<N>, MultiRoundTrip<N>> modifier, MultiRoundTrip<N> original, Simulator<N> simulator) {
		this.modifier = modifier;
		this.original = original;
		this.modified = modifier.apply(original);
		this.modified.simulateAll(simulator);
	}

	Function<MultiRoundTrip<N>, MultiRoundTrip<N>> getModifier() {
		return this.modifier;
	}

	MultiRoundTrip<N> getOriginal() {
		return this.original;
	}

	@Override
	public MultiRoundTrip<N> getMultiRoundTrip() {
		return this.modified;
	}

}
