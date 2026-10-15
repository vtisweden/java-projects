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
package se.vti.roundtrips.multi;

import se.vti.roundtrips.scenario.Node;
import se.vti.utils.misc.metropolishastings.MHAbstractStateProcessor;

/**
 * @author GunnarF
 */
public class MultiRoundTripJsonLogger<N extends Node> extends MHAbstractStateProcessor<MultiRoundTrip<N>> {

	private String stateDumpFilePrefix = null;

	public MultiRoundTripJsonLogger(long samplingInterval, String stateDumpFilePrefix) {
		super(0, samplingInterval);
		this.stateDumpFilePrefix = stateDumpFilePrefix;
	}

	public MultiRoundTripJsonLogger<N> setStateDumpPrefix(String prefix) {
		this.stateDumpFilePrefix = prefix;
		return this;
	}

	@Override
	public void processStateHook(MultiRoundTrip<N> state, double logWeight) {
		try {
			MultiRoundTripJsonIO.singleton().writeToFile(state, this.stateDumpFilePrefix + "." + iteration() + ".json");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
