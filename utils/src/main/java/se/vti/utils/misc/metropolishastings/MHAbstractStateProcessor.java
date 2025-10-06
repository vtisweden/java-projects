/**
 * se.vti.roundtrips.logging
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
package se.vti.utils.misc.metropolishastings;

/**
 * 
 * @author GunnarF
 *
 * @param <X>
 */
public abstract class MHAbstractStateProcessor<X> implements MHStateProcessor<X> {

	private final long burnInIterations;
	private final long samplingInterval;
	private long iteration;
	private long samples;
	
	public MHAbstractStateProcessor(long burnInIterations, long samplingInterval) {
		super();
		this.burnInIterations = burnInIterations;
		this.samplingInterval = samplingInterval;
	}

	public long iteration() {
		return this.iteration;
	}
	
	public long samples() {
		return this.samples;
	}

	@Override
	public void start() {
		this.iteration = 0;
		this.samples = 0;
	}

	@Override
	public final void processState(X state) {
		if ((this.iteration >= this.burnInIterations) && (this.iteration % this.samplingInterval == 0)) {
			this.processStateHook(state);
			this.samples++;
		}
		this.iteration++;
	}

	@Override
	public void end() {
	}

	public abstract void processStateHook(X state);
}