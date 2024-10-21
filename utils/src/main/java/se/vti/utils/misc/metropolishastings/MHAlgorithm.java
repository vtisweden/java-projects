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

/**
 * 
 * Based on floetteroed.utilities.math.metropolishastings package.
 * 
 * @author GunnarF
 * 
 * @param <S>
 */
public class MHAlgorithm<S extends Object> {

	// -------------------- CONSTANTS --------------------

	private final MHProposal<S> proposal;

	private final MHWeight<S> weight;

	private final Random rnd;

	// -------------------- MEMBERS --------------------

	private S initialState = null;

	private List<MHStateProcessor<S>> stateProcessors = new ArrayList<MHStateProcessor<S>>();

	private long msgInterval = 1;

	private long lastCompTime_ms = 0;

	private S finalState = null;
	
	// -------------------- CONSTRUCTION --------------------

	public MHAlgorithm(final MHProposal<S> proposal, final MHWeight<S> weight, final Random rnd) {
		if (proposal == null) {
			throw new IllegalArgumentException("proposal is null");
		}
		if (weight == null) {
			throw new IllegalArgumentException("weight is null");
		}
		if (rnd == null) {
			throw new IllegalArgumentException("rnd is null");
		}
		this.proposal = proposal;
		this.weight = weight;
		this.rnd = rnd;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setInitialState(final S initialState) {
		this.initialState = initialState;
	}

	public S getInitialState() {
		return this.initialState;
	}

	public S getFinalState() {
		return this.finalState;
	}

	public void setMsgInterval(final long msgInterval) {
		if (msgInterval < 1) {
			throw new IllegalArgumentException("message interval < 1");
		}
		this.msgInterval = msgInterval;
	}

	public long getMsgInterval() {
		return this.msgInterval;
	}

	public void addStateProcessor(final MHStateProcessor<S> stateProcessor) {
		if (stateProcessor == null) {
			throw new IllegalArgumentException("state processor is null");
		}
		this.stateProcessors.add(stateProcessor);
	}

	public long getLastCompTime_ms() {
		return this.lastCompTime_ms;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void run(final long iterations) {

		this.lastCompTime_ms = 0;

		/*
		 * initialize (iteration 0)
		 */
		for (MHStateProcessor<S> processor : this.stateProcessors) {
			processor.start();
		}

		long tick_ms = System.currentTimeMillis();
		S currentState;
		if (this.initialState != null) {
			currentState = this.initialState;
		} else {
			currentState = this.proposal.newInitialState();
		}
		double currentLogWeight = this.weight.logWeight(currentState);
		this.lastCompTime_ms += System.currentTimeMillis() - tick_ms;

		for (MHStateProcessor<S> processor : this.stateProcessors) {
			processor.processState(currentState);
		}

		/*
		 * iterate (iterations 1, 2, ...)
		 */
		for (long i = 1; i <= iterations; i++) {

			if ((this.msgInterval >= 1) && (i % this.msgInterval == 0)) {
				System.out.println("MH iteration " + i);
				System.out.println("  state  = " + currentState);
				System.out.println("  weight = " + Math.exp(currentLogWeight));
			}

			tick_ms = System.currentTimeMillis();
			final MHTransition<S> proposalTransition = this.proposal.newTransition(currentState);
			final S proposalState = proposalTransition.getNewState();
			double proposalLogWeight = this.weight.logWeight(proposalState);
			final double logAlpha = (proposalLogWeight - currentLogWeight)
					+ (proposalTransition.getBwdLogProb() - proposalTransition.getFwdLogProb());
			
			if (Math.log(this.rnd.nextDouble()) < logAlpha) {
				currentState = proposalState;
				currentLogWeight = proposalLogWeight;
			}
			this.lastCompTime_ms += System.currentTimeMillis() - tick_ms;

			for (MHStateProcessor<S> processor : this.stateProcessors) {
				processor.processState(currentState);
			}
		}

		/*
		 * wrap up
		 */
		this.finalState = currentState;

		for (MHStateProcessor<S> processor : this.stateProcessors) {
			processor.end();
		}		
	}
}
