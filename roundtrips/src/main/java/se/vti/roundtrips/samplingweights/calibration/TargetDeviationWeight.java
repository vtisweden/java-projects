/**
 * od2roundtrips.model
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.samplingweights.calibration;

import java.util.function.Function;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.grouping.PopulationGroupFilter;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public abstract class TargetDeviationWeight<N extends Node> implements MHWeight<MultiRoundTrip<N>> {

	// -------------------- MEMBERS --------------------

	private final double realPopulationSize;

	private PopulationGroupFilter<N> filter = null;

	private double[] target;

	private boolean accountForDiscretizationNoise = true;
	private boolean expansionFactorIsComputed = false;
	private double expansionFactor = Double.NaN;
	private double discretizationStandardDeviation = Double.NaN;
	private double discretizationVariance = Double.NaN;

	private Function<Double, Double> absoluteResidualToLogWeight = null;

	// -------------------- CONSTRUCTION --------------------

	public TargetDeviationWeight(double realPopulationSize) {
		this.realPopulationSize = realPopulationSize;
	}

	// -------------------- SETTERS & GETTERS --------------------

	/*
	 * f(r) ~ exp(-|r| / b)
	 * 
	 * sigma^2 = 2 * b^2
	 * 
	 * b = sigma / sqrt(2)
	 * 
	 */
	public TargetDeviationWeight<N> setToTwoSidedExponential(double standardDeviationWithoutDiscretization) { 
		this.absoluteResidualToLogWeight = (r -> (-1.0) * r * Math.sqrt(2.0)
				/ (standardDeviationWithoutDiscretization + this.discretizationStandardDeviation));
		return this;
	}

	/*
	 * f(r) ~exp(-1/2 * r^2 / sigma^2)
	 */
	public TargetDeviationWeight<N> setToGaussian(double standardDeviationWithoutDiscretization) {
		this.absoluteResidualToLogWeight = (r -> (-0.5) * r * r
				/ (standardDeviationWithoutDiscretization * standardDeviationWithoutDiscretization
						+ this.discretizationVariance));
		return this;
	}

	public TargetDeviationWeight<N> setAccountForDiscretizationNoise(boolean accountForDiscretizationNoise) {
		this.accountForDiscretizationNoise = accountForDiscretizationNoise;
		return this;
	}

	public TargetDeviationWeight<N> setFilter(PopulationGroupFilter<N> filter) {
		this.filter = filter;
		return this;
	}

	public PopulationGroupFilter<N> getFilter() {
		return this.filter;
	}

	public double[] computeTargetIfAbsent() {
		if (this.target == null) {
			this.target = this.computeTarget();
		}
		return this.target;
	}

	// -------------------- INTERNALS --------------------

	/* package for testing */ void computeExpansionFactor(int syntheticPopulationSize) {
		if (!this.expansionFactorIsComputed) {
			// ExpansionFactor equals width of uniform discretization noise distribution.
			this.expansionFactor = this.realPopulationSize / syntheticPopulationSize;
			if (this.accountForDiscretizationNoise) {
				this.discretizationVariance = this.expansionFactor * this.expansionFactor / 12.0;
				this.discretizationStandardDeviation = Math.sqrt(this.discretizationVariance);
			} else {
				this.discretizationVariance = 0.0;
				this.discretizationStandardDeviation = 0.0;
			}
			this.expansionFactorIsComputed = true;
		}
	}

	/* package for testing */ double computeLogWeight(double sampleValue, double targetValue) {
//		double e = sampleValue * this.expansionFactor - targetValue;
//		double logWeight1 = this.singleAbsoluteResidualToLogWeight.apply(Math.abs(e - 0.5 * this.expansionFactor));
//		double logWeight2 = this.singleAbsoluteResidualToLogWeight.apply(Math.abs(e + 0.5 * this.expansionFactor));
//		double maxLogWeight = Math.max(logWeight1, logWeight2);
//		return Math.log(Math.exp(logWeight1 - maxLogWeight) + Math.exp(logWeight2 - maxLogWeight)) + maxLogWeight;
		return this.absoluteResidualToLogWeight.apply(Math.abs(sampleValue * this.expansionFactor - targetValue));
	}

	// -------------------- IMPLEMENTATION OF MHWeight --------------------

	@Override
	public boolean allowsForWeightsOtherThanOneInMHWeightContainer() {
		return false;
	}

	@Override
	public double logWeight(MultiRoundTrip<N> multiRoundTrip) {

		this.computeExpansionFactor(multiRoundTrip.size());

		final double[] sample = this.computeSample(multiRoundTrip, this.filter);
		this.computeTargetIfAbsent();

		double logWeightSum = 0.0;
		for (int i = 0; i < this.target.length; i++) {
			logWeightSum += this.computeLogWeight(sample[i], this.target[i]);
		}
		return logWeightSum;
	}

	// --------------- ABSTRACT FUNCTIONS ---------------

	public abstract String[] createLabels();

	public abstract double[] computeTarget();

	public abstract double[] computeSample(MultiRoundTrip<N> multiRoundTrip, PopulationGroupFilter<N> filter);

}
