/**
 * se.vti.utils
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import se.vti.utils.misc.metropolishastings.terminationcriteria.FixedNumberOfIterationsTerminationCriterion;
import se.vti.utils.misc.metropolishastings.terminationcriteria.TerminationCriterion;

/**
 * 
 * Based on floetteroed.utilities.math.metropolishastings package.
 * 
 * @author GunnarF
 * 
 * @param <X>
 */
public class MHAlgorithm<X extends Object> {

	// -------------------- CONSTANTS --------------------

	private final MHOneStepLogic<X> oneStepLogic;

	private final MHProposal<X> proposal;

	private TerminationCriterion<X> terminationCriterion;
	
	// -------------------- MEMBERS --------------------

	private MHStateProcessor<X> stateLogger;

	private X initialState = null;

	private List<MHStateProcessor<X>> stateProcessors = new ArrayList<MHStateProcessor<X>>();

	private long lastCompTime_ms = 0;

	private X finalState = null;

	// -------------------- CONSTRUCTION --------------------

	public MHAlgorithm(final MHOneStepLogic<X> oneStepLogic, final MHProposal<X> proposal) {
		this.oneStepLogic = oneStepLogic;
		this.proposal = proposal;
	}

	public MHAlgorithm(final MHProposal<X> proposal, final MHWeight<X> weight, final Random rnd) {
		this(new MHSequentialOneStepLogic<X>(proposal, weight, rnd), proposal);
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setInitialState(final X initialState) {
		this.initialState = initialState;
	}

	public X getInitialState() {
		return this.initialState;
	}

	public X getFinalState() {
		return this.finalState;
	}

	public void setMsgInterval(final long msgInterval) {
		if (msgInterval <= 0) {
			this.stateLogger = null;
		} else {
			this.stateLogger = new MHStateLogger<>(msgInterval);
		}
	}

	public void setTerminationCriterion(TerminationCriterion<X> terminationCriterion) {
		if (this.terminationCriterion != null) {
			throw new RuntimeException("Termination criterion already added.");
		}
		this.terminationCriterion = terminationCriterion;
		this.stateProcessors.add(terminationCriterion);
	}
	
	public void addStateProcessor(final MHStateProcessor<X> stateProcessor) {
		if (stateProcessor == null) {
			throw new IllegalArgumentException("state processor is null");
		}
		if (stateProcessor instanceof TerminationCriterion) {
			throw new IllegalArgumentException("add termination criterion through separate setter");
		}
		this.stateProcessors.add(stateProcessor);
	}

	public long getLastCompTime_ms() {
		return this.lastCompTime_ms;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run(long iterations) {
		
		if (this.terminationCriterion != null) {
			throw new RuntimeException("Cannot set simultaneously set number of iterations and termination criterion.");
		}
		this.setTerminationCriterion(new FixedNumberOfIterationsTerminationCriterion<X>(iterations));
		this.run();
	}
	
	public void run() {

		if (this.terminationCriterion == null) {
			throw new RuntimeException("No termination criterion defined");
		}
		
		this.lastCompTime_ms = 0;

		if (this.stateLogger != null) {
			this.stateProcessors.add(0, this.stateLogger);
		}

		/*
		 * initialize (iteration 0)
		 */
		for (MHStateProcessor<X> processor : this.stateProcessors) {
			processor.start();
		}

		long tick_ms = System.currentTimeMillis();
		MHState<X> currentState = this.oneStepLogic
				.createInitial(this.initialState != null ? this.initialState : this.proposal.newInitialState());

		this.lastCompTime_ms += System.currentTimeMillis() - tick_ms;

		for (MHStateProcessor<X> processor : this.stateProcessors) {
			processor.processState(currentState.getState(), currentState.getLogWeight());
		}

		/*
		 * iterate (iterations 1, 2, ...)
		 */
		int i = 0;
		while (!this.terminationCriterion.terminate()) {
			i++;
			tick_ms = System.currentTimeMillis();
			currentState = this.oneStepLogic.drawNext(currentState);
			this.lastCompTime_ms += System.currentTimeMillis() - tick_ms;
			for (MHStateProcessor<X> processor : this.stateProcessors) {
				processor.processState(currentState.getState(), currentState.getLogWeight());
			}
		}

		/*
		 * wrap up
		 */
		this.finalState = currentState.getState();

		for (MHStateProcessor<X> processor : this.stateProcessors) {
			processor.end();
		}
	}
}
