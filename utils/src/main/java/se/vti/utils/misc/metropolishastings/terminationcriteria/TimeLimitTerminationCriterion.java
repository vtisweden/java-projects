/**
 * se.vti.utils.misc.metropolishastings.terminationcriteria
 *
 * Copyright (C) 2026 by Michael Sederlin (VTI, LiU).
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
package se.vti.utils.misc.metropolishastings.terminationcriteria;

/**
 * @author MichaelS
 */

public class TimeLimitTerminationCriterion<X> implements TerminationCriterion<X> {

	private final double maxTime_s;
	private double startTime_s;
	private double now_s;

	public TimeLimitTerminationCriterion(double maxTime_s) {
		this.maxTime_s = maxTime_s;
	}

	@Override
	public void start() {
		this.startTime_s = System.currentTimeMillis() / 1000.0;
	}

	@Override
	public void processState(X state) {
		this.now_s = System.currentTimeMillis() / 1000.0;
	}

	@Override
	public void end() {
	}

	@Override
	public boolean terminate() {
		return (this.now_s - this.startTime_s >= this.maxTime_s);
	}
}
