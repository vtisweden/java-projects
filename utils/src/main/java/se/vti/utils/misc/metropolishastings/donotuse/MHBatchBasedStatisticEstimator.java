/**
 * se.vti.utils.misc.metropolishastings
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
package se.vti.utils.misc.metropolishastings.donotuse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * @deprecated
 * @author GunnarF
 */
public class MHBatchBasedStatisticEstimator<X> implements MHStateProcessor<X> {

	// -------------------- CONSTANTS --------------------

	private final String name;

	// -------------------- CONFIGURATION --------------------

	private Function<X, Double> stateToStatistic;

	private double shareOfDiscardedTransients = 0.1;

	private int numberOfBatches = 30;

	private final List<Double> allDataPoints = new ArrayList<>();

	// -------------------- RESULTS --------------------

	private boolean statisticsUpToDate = false;
	private Double meanValue = null;
	private Double effectiveVariance = null;
	private Double varianceOfMean = null;
	private Double batchMeanAutoCorrelation = null;

	// -------------------- CONSTRUCTION AND CONFIGURATION --------------------

	public MHBatchBasedStatisticEstimator(String name, Function<X, Double> stateToStatistic) {
		this.name = name;
		this.stateToStatistic = stateToStatistic;
	}

	public MHBatchBasedStatisticEstimator(String name) {
		this(name, null);
	}

	public MHBatchBasedStatisticEstimator<X> setStateToStatistic(Function<X, Double> f) {
		this.stateToStatistic = f;
		return this;
	}

	public MHBatchBasedStatisticEstimator<X> setShareOfDiscardedTransients(double share) {
		this.shareOfDiscardedTransients = share;
		return this;
	}

	public MHBatchBasedStatisticEstimator<X> setNumberOfBatches(int numberOfBatches) {
		this.numberOfBatches = numberOfBatches;
		return this;
	}

	// -------------------- GETTERS --------------------

	public String getName() {
		return this.name;
	}

	public Double getMeanValue() {
		this.ensureStatisticsUpToDate();
		return this.meanValue;
	}

	public Double getEffectiveVariance() {
		this.ensureStatisticsUpToDate();
		return this.effectiveVariance;
	}

	public Double getBatchMeanAutoCorrelation() {
		this.ensureStatisticsUpToDate();
		return this.batchMeanAutoCorrelation;
	}

	public Double getVarianceOfMean() {
		this.ensureStatisticsUpToDate();
		return this.varianceOfMean;
	}

	public Double getEffectiveStandardDeviation() {
		Double v = this.getEffectiveVariance();
		return (v == null ? null : Math.sqrt(v));
	}

	public Double getStandardDeviationOfMean() {
		Double v = this.getVarianceOfMean();
		return (v == null ? null : Math.sqrt(v));
	}

	// -------------------- INTERNALS --------------------

	private void ensureStatisticsUpToDate() {

		if (this.statisticsUpToDate) {
			return;
		}
		this.statisticsUpToDate = true;

		int burnIn = (int) (this.shareOfDiscardedTransients * this.allDataPoints.size());
		int usable = this.allDataPoints.size() - burnIn;
		if (usable < this.numberOfBatches * 2) {
			return;
		}

		int batchSize = usable / this.numberOfBatches;
		double[] batchMeans = new double[this.numberOfBatches];
		int i = burnIn;
		for (int batch = 0; batch < this.numberOfBatches; batch++) {
			double sum = 0;
			for (int j = 0; j < batchSize; j++) {
				sum += this.allDataPoints.get(i++);
			}
			batchMeans[batch] = sum / batchSize;
		}

		this.meanValue = Arrays.stream(batchMeans).sum() / this.numberOfBatches;

		double varBatchMeans = 0;
		for (double bm : batchMeans) {
			double e = bm - this.meanValue;
			varBatchMeans += e * e;
		}
		varBatchMeans /= (this.numberOfBatches - 1.0);
		this.effectiveVariance = batchSize * varBatchMeans;
		this.varianceOfMean = this.effectiveVariance / (this.numberOfBatches * batchSize);

		double num = 0;
		for (int b = 0; b < this.numberOfBatches - 1; b++) {
			num += (batchMeans[b] - this.meanValue) * (batchMeans[b + 1] - this.meanValue);
		}
		double den = 0;
		for (int b = 0; b < this.numberOfBatches; b++) {
			double e = batchMeans[b] - this.meanValue;
			den += e * e;
		}
		this.batchMeanAutoCorrelation = (den > 0 ? num / den : 0.0);
	}

	// -------------------- IMPLEMENTATION OF MHStateProcessor --------------------

	@Override
	public void start() {
	}

	@Override
	public void processState(X state) {
		this.allDataPoints.add(this.stateToStatistic.apply(state));
		this.statisticsUpToDate = false;
	}

	@Override
	public void end() {
	}

}