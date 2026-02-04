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
package se.vti.utils.misc.metropolishastings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * @author GunnarF
 */
public class MHBatchBasedVarianceEstimator<X> implements MHStateProcessor<X> {

	private final Function<X, Double> stateToStatistic;

	private double shareOfDiscardedTransients = 0.1;

	private int minBatchSize = 100;

	private List<Double> allDataPoints = new ArrayList<>();

	private boolean statisticsUpToDate = false;
	private Double meanValue = null;
	private Double effectiveVariance = null;
	private Double varianceOfMean = null;

	public MHBatchBasedVarianceEstimator(Function<X, Double> stateToStatistic) {
		this.stateToStatistic = stateToStatistic;
	}

	public MHBatchBasedVarianceEstimator<X> setShareOfDiscartedTransients(double share) {
		this.shareOfDiscardedTransients = share;
		return this;
	}

	public MHBatchBasedVarianceEstimator<X> setMinBatchSize(int minBatchSize) {
		this.minBatchSize = minBatchSize;
		return this;
	}

	public Double getMeanValue() {
		this.ensureStatisticsUpToDate();
		return this.meanValue;
	}

	public Double getEffectiveVariance() {
		this.ensureStatisticsUpToDate();
		return this.effectiveVariance;
	}

	public Double getVarianceOfMean() {
		this.ensureStatisticsUpToDate();
		return this.varianceOfMean;
	}

	private void ensureStatisticsUpToDate() {
		if (!this.statisticsUpToDate) {

			int batchSize = (int) Math.sqrt((1.0 - this.shareOfDiscardedTransients) * this.allDataPoints.size());
			if (batchSize >= this.minBatchSize) {

				int numberOfBatches = batchSize; // both are ~sqrt(allDataPoints.size())
				double[] batchMeans = new double[numberOfBatches];
				for (int batchIndex = 0; batchIndex < numberOfBatches; batchIndex++) {
					int startDataIndex = this.allDataPoints.size() - (1 + batchIndex) * batchSize;
					double sum = 0.0;
					for (int i = startDataIndex; i < startDataIndex + batchSize; i++) {
						sum += this.allDataPoints.get(i);
					}
					batchMeans[batchIndex] = sum / batchSize;
				}

				double overallMean = Arrays.stream(batchMeans).sum() / numberOfBatches;
				double sumOfSquares = Arrays.stream(batchMeans).map(b -> {
					double e = b - overallMean;
					return e * e;
				}).sum();

				this.meanValue = overallMean;
				this.effectiveVariance = batchSize / (numberOfBatches - 1.0) * sumOfSquares;
				this.varianceOfMean = this.effectiveVariance / batchSize / numberOfBatches;
			}

			this.statisticsUpToDate = true;
		}
	}

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
