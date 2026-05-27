/**
  * se.vti.utils.misc.metropolishastings.terminationcriteria
  *
  * Copyright (C) 2026 by Michael Sederlin (VTI, LiU).
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
package se.vti.utils.misc.metropolishastings.terminationcriteria;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author MichaelS
 * @author GunnarF
 */
public abstract class AbstractCombinedTerminationCriterion<X> implements TerminationCriterion<X> {

	// -------------------- MEMBERS --------------------

	protected final Set<TerminationCriterion<X>> terminationCriteria = new LinkedHashSet<>();

	// -------------------- CONSTRUCTION --------------------

	public AbstractCombinedTerminationCriterion(TerminationCriterion<X> tc) {
		this.add(tc);
	}

	public final AbstractCombinedTerminationCriterion<X> add(TerminationCriterion<X> tc) {
		this.terminationCriteria.add(tc);
		return this;
	}

	// ---------- PARTIAL IMPLEMENTATION OF TerminationCriterion ----------

	@Override
	public final void start() {
		for (TerminationCriterion<X> tc : this.terminationCriteria) {
			tc.start();
		}
	}

	@Override
	public final void processState(X state, double logWeight) {
		for (TerminationCriterion<X> tc : this.terminationCriteria) {
			tc.processState(state, logWeight);
		}
	}

	@Override
	public final void end() {
		for (TerminationCriterion<X> tc : this.terminationCriteria) {
			tc.end();
		}
	}

	@Override
	public final void processState(X state) {
		for (TerminationCriterion<X> tc : this.terminationCriteria) {
			tc.processState(state);
		}
	}

	@Override
	public abstract boolean terminate();
}
