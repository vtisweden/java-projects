// /**
//  * se.vti.roundtrips.samplingweights.misc
//  *
//  * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
//  *
//  * VTI = Swedish National Road and Transport Institute
//  * LiU = Linköping University, Sweden
//  *
//  * This program is free software: you can redistribute it and/or modify it under the terms
//  * of the GNU General Public License as published by the Free Software Foundation, either
//  * version 3 of the License, or (at your option) any later version.
//  *
//  * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  * See the GNU General Public License for more details.
//  *
//  * You should have received a copy of the GNU General Public License along with this program.
//  * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
//  */

package se.vti.utils.misc.metropolishastings.terminationcriteria;

/**
* @author MichaelS
*/


import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CombinedTerminationCriterion<X> implements TerminationCriterion<X> {
    private final Logger logger = LogManager.getLogger(CombinedTerminationCriterion.class);
    private final Set<TerminationCriterion<X>> terminationCriteria = new HashSet<>();

    @SafeVarargs
    public CombinedTerminationCriterion(TerminationCriterion<X>... terminationCriteria) {
        for (TerminationCriterion<X> tc : terminationCriteria) {
            this.terminationCriteria.add(tc);
        }
    }

    public void addCriterion(TerminationCriterion<X> tc) {
        this.terminationCriteria.add(tc);
    }

    @Override
    public void start() {
        for (TerminationCriterion<X> tc : this.terminationCriteria) {
            tc.start();
        }
    }

    @Override
    public void processState(X state, double logWeight) {
        for (TerminationCriterion<X> tc : this.terminationCriteria) {
            tc.processState(state, logWeight);
        }
    }

    @Override
    public void end() {
        for (TerminationCriterion<X> tc : this.terminationCriteria) {
            tc.end();
        }
    }

    @Override
    public boolean terminate() {
        for (TerminationCriterion<X> tc : this.terminationCriteria) {
            if (tc.terminate()) {
                logger.info("Termination triggered by criterion {}", tc.getClass());
                return true;
            }
        }
        return false;
    }

    @Override
    public void processState(X state) {
        for (TerminationCriterion<X> tc : this.terminationCriteria) {
            tc.processState(state);
        }
    }
}
