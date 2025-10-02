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
package se.vti.roundtrips.samplingweights.misc;

import java.util.Arrays;
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
public abstract class MultiRoundTripTargetDeviationWeight<N extends Node> implements MHWeight<MultiRoundTrip<N>> {

	// -------------------- MEMBERS --------------------

	private PopulationGroupFilter<N> filter = null;

	private double[] target;

	private double targetSize;

	private Function<Double, Double> singleAbsoluteResidualToLogWeight = null;

	private Function<Double, Double> totalDiscretizationErrorToLogWeight = null;

	// -------------------- CONSTRUCTION --------------------

	public MultiRoundTripTargetDeviationWeight() {
		this.setToTwoSidedExponential();
	}

	// -------------------- SETTERS & GETTERS --------------------

	public void setToTwoSidedExponential() {
		this.setSingleAbsoluteResidualToLogWeight(a -> (-1.0) * a);
		this.setTotalDiscretizationErrorToLogWeight(e -> (-1.0) * e);
	}

	public void setToGaussian() {
		this.setSingleAbsoluteResidualToLogWeight(r -> (-0.5) * r * r);
		this.setTotalDiscretizationErrorToLogWeight(e -> (-0.5) * e * e);
	}

	public void setSingleAbsoluteResidualToLogWeight(Function<Double, Double> singleAbsoluteResidualToLogWeight) {
		this.singleAbsoluteResidualToLogWeight = singleAbsoluteResidualToLogWeight;
	}

	public void setTotalDiscretizationErrorToLogWeight(Function<Double, Double> totalDiscretizationErrorToLogWeight) {
		this.totalDiscretizationErrorToLogWeight = totalDiscretizationErrorToLogWeight;
	}

	public void setFilter(PopulationGroupFilter<N> filter) {
		this.filter = filter;
	}

	public PopulationGroupFilter<N> getFilter() {
		return this.filter;
	}

	public double[] computeTargetIfAbsent() {
		if (this.target == null) {
			this.target = this.computeTarget();
			this.targetSize = Arrays.stream(this.target).sum();
		}
		return this.target;
	}

	// --------------- IMPLEMENTATION OF MHPreferenceComponent ---------------

	@Override
	public double logWeight(MultiRoundTrip<N> multiRoundTrip) {

		final double[] sample = this.computeSample(multiRoundTrip, this.filter);
		final double sampleSize = Math.max(Arrays.stream(sample).sum(), 1e-8);

		this.computeTargetIfAbsent();
		final double slack = 0.5 * this.targetSize / sampleSize;

		double logWeight = 0.0;

		for (int i = 0; i < this.target.length; i++) {
			final double absoluteResidual = Math.max(0.0,
					Math.abs(sample[i] * this.targetSize / sampleSize - this.target[i]) - slack);
			logWeight += this.singleAbsoluteResidualToLogWeight.apply(absoluteResidual);
			// Each addend contributes on average 0.5 * slack to the discretization error.
		}

		final double totalDiscretizationError = 0.5 * slack * this.target.length;
		logWeight += this.totalDiscretizationErrorToLogWeight.apply(totalDiscretizationError);

		return logWeight;
	}

	// --------------- ABSTRACT FUNCTIONS ---------------

	public abstract String[] createLabels();

	public abstract double[] computeTarget();

	public abstract double[] computeSample(MultiRoundTrip<N> multiRoundTrip, PopulationGroupFilter<N> filter);

}
