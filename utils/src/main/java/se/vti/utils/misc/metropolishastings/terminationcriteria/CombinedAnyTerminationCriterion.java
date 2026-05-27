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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author GunnarF
 * @author MichaelS
 */
public class CombinedAnyTerminationCriterion<X> extends AbstractCombinedTerminationCriterion<X> {

	private final Logger logger = LogManager.getLogger(CombinedAnyTerminationCriterion.class);

	public CombinedAnyTerminationCriterion(TerminationCriterion<X> tc) {
		super(tc);
	}

	@Override
	public boolean terminate() {
		for (TerminationCriterion<X> tc : this.terminationCriteria) {
			if (tc.terminate()) {
				logger.info("Termination triggered by criterion: ", tc.getClass());
				return true;
			}
		}
		return false;
	}
}
