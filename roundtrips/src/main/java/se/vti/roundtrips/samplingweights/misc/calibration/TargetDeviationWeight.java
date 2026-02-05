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
package se.vti.roundtrips.samplingweights.misc.calibration;

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

	private double standardDeviation = Double.NaN;
	private double variance = Double.NaN;

	private Function<Double, Double> singleAbsoluteResidualToLogWeight = null;

	// -------------------- CONSTRUCTION --------------------

	public TargetDeviationWeight(double realPopulationSize) {
		this.realPopulationSize = realPopulationSize;
		this.setToTwoSidedExponential();
	}

	// -------------------- SETTERS & GETTERS --------------------

	public void setToTwoSidedExponential() {
		this.singleAbsoluteResidualToLogWeight = (r -> (-1.0) * r * Math.sqrt(2.0) / this.standardDeviation);
//		this.setSingleAbsoluteResidualToLogWeight(a -> (-1.0) * a);
	}

	public void setToGaussian() {
		this.singleAbsoluteResidualToLogWeight = (r -> (-0.5) * r * r / this.variance);
//		this.setSingleAbsoluteResidualToLogWeight(r -> (-0.5) * r * r);
	}

//	public void setSingleAbsoluteResidualToLogWeight(Function<Double, Double> singleAbsoluteResidualToLogWeight) {
//		this.singleAbsoluteResidualToLogWeight = singleAbsoluteResidualToLogWeight;
//	}

	public void setFilter(PopulationGroupFilter<N> filter) {
		this.filter = filter;
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

	// -------------------- IMPLEMENTATION OF MHWeight --------------------

	@Override
	public double logWeight(MultiRoundTrip<N> multiRoundTrip) {

		final double expansionFactor = this.realPopulationSize / multiRoundTrip.size();
		this.variance = expansionFactor * expansionFactor / 12.0;
		this.standardDeviation = Math.sqrt(this.variance);

		final double[] sample = this.computeSample(multiRoundTrip, this.filter);
		this.computeTargetIfAbsent();

		double logWeightSum = 0.0;
		for (int i = 0; i < this.target.length; i++) {
			double e = sample[i] * expansionFactor - this.target[i];
			double logWeight1 = this.singleAbsoluteResidualToLogWeight.apply(Math.abs(e - 0.5 * expansionFactor));
			double logWeight2 = this.singleAbsoluteResidualToLogWeight.apply(Math.abs(e + 0.5 * expansionFactor));
			double maxLogWeight = Math.max(logWeight1, logWeight2);
			logWeightSum += Math.log(Math.exp(logWeight1 - maxLogWeight) + Math.exp(logWeight2 - maxLogWeight))
					+ maxLogWeight;
		}
		return logWeightSum;
	}

	// --------------- ABSTRACT FUNCTIONS ---------------

	public abstract String[] createLabels();

	public abstract double[] computeTarget();

	public abstract double[] computeSample(MultiRoundTrip<N> multiRoundTrip, PopulationGroupFilter<N> filter);

}
