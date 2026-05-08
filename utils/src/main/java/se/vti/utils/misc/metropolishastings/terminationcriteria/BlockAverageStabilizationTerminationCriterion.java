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
package se.vti.utils.misc.metropolishastings.terminationcriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * @author GunnarF
 * 
 * @param <X>
 */
public class BlockAverageStabilizationTerminationCriterion<X> implements TerminationCriterion<X> {

	// -------------------- CONFIGURATION PARAMETERS --------------------

	private ToDoubleFunction<X> extractor = null;

	private long minSamples = 10 * 1000;

	private double standardizedMeanTolerance = 0.1;

	private double relativeVarianceTolerance = 0.01;

	private int checkInterval = 1000;

	// -------------------- INTERNAL STATES --------------------

	private final List<Double> samples = new ArrayList<>();

	private boolean stabilized = false;

	// -------------------- CONSTRUCTION AND CONFIGURATION --------------------

	public BlockAverageStabilizationTerminationCriterion() {
	}

	public BlockAverageStabilizationTerminationCriterion<X> setExtractor(ToDoubleFunction<X> extractor) {
		this.extractor = extractor;
		return this;
	}

	public BlockAverageStabilizationTerminationCriterion<X> setMinSamples(long minSamples) {
		if (minSamples % 10 != 0) {
			throw new RuntimeException("minSamples must be a multiple of ten.");
		}
		this.minSamples = minSamples;
		return this;
	}

	public BlockAverageStabilizationTerminationCriterion<X> setStandardizedMeanTolerance(double tol) {
		this.standardizedMeanTolerance = tol;
		return this;
	}

	public BlockAverageStabilizationTerminationCriterion<X> setNormalizedVarianceTolerance(double tol) {
		this.relativeVarianceTolerance = tol;
		return this;
	}

	public BlockAverageStabilizationTerminationCriterion<X> setCheckInterval(int checkInterval) {
		if (checkInterval % 10 != 0) {
			throw new RuntimeException("checkInterval must be a multiple of ten.");
		}
		this.checkInterval = checkInterval;
		return this;
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public void start() {
		this.samples.clear();
		this.stabilized = false;
	}

	@Override
	public void processState(X state) {
		this.samples.add(this.extractor.applyAsDouble(state));
		if ((this.samples.size() >= this.minSamples) && (this.samples.size() % this.checkInterval == 0)) {
			checkStabilization();
		}
	}

	@Override
	public void end() {
	}

	@Override
	public boolean terminate() {
		return this.stabilized;
	}

	// -------------------- INTERNALS --------------------

	private void checkStabilization() {

		int _N = this.samples.size();

		List<Double> means = new ArrayList<>();
		List<Double> variances = new ArrayList<>();
		List<Integer> cutIndices = new ArrayList<>();

		for (int initialPercent = 0; initialPercent <= 40; initialPercent += 10) {
			int start = (int) (0.01 * initialPercent * _N);
			Stats stats = computeStats(start, _N);
			means.add(stats.mean);
			variances.add(stats.variance);
			cutIndices.add(start);
		}

		for (int i = 0; i < means.size() - 2; i++) {

			double dMean1 = Math.abs(means.get(i) - means.get(i + 1))
					/ Math.max((Math.sqrt(variances.get(i)) + Math.sqrt(variances.get(i + 1))) / 2.0, 1e-8);
			double dMean2 = Math.abs(means.get(i + 1) - means.get(i + 2))
					/ Math.max((Math.sqrt(variances.get(i + 1)) + Math.sqrt(variances.get(i + 2))) / 2.0, 1e-8);

			double dVar1 = Math.abs(variances.get(i) - variances.get(i + 1))
					/ Math.max((variances.get(i) + variances.get(i + 1)) / 2.0, 1e-8);
			double dVar2 = Math.abs(variances.get(i + 1) - variances.get(i + 2))
					/ Math.max((variances.get(i + 1) + variances.get(i + 2)) / 2.0, 1e-8);

			boolean stableMeans = (dMean1 < this.standardizedMeanTolerance)
					&& (dMean2 < this.standardizedMeanTolerance);
			boolean stableVars = (dVar1 < this.relativeVarianceTolerance) && (dVar2 < this.relativeVarianceTolerance);

			if (stableMeans && stableVars) {
				int burnInIndex = cutIndices.get(i + 1);
				if (this.checkThreeWindowConsistency(burnInIndex)) {
					this.stabilized = true;
					return;
				}
			}
		}
	}

	private boolean checkThreeWindowConsistency(int start) {

		int _N = this.samples.size();
		int remaining = _N - start;
		int _L = remaining / 3;

		Stats w1 = computeStats(start, start + _L);
		Stats w2 = computeStats(start + _L, start + 2 * _L);
		Stats w3 = computeStats(start + 2 * _L, _N);

		double meanVariance = (w1.variance + w2.variance + w3.variance) / 3.0;

		double meanRange = (max(w1.mean, w2.mean, w3.mean) - min(w1.mean, w2.mean, w3.mean))
				/ Math.max(Math.sqrt(meanVariance), 1e-8);
		double varRange = (max(w1.variance, w2.variance, w3.variance) - min(w1.variance, w2.variance, w3.variance))
				/ Math.max(meanVariance, 1e-8);

		return (meanRange < this.standardizedMeanTolerance) && (varRange < this.relativeVarianceTolerance);
	}

	private Stats computeStats(int from, int to) {
		int n = to - from;

		double sum = 0.0;
		for (int i = from; i < to; i++) {
			sum += samples.get(i);
		}

		double mean = sum / n;

		double varSum = 0.0;
		for (int i = from; i < to; i++) {
			double d = samples.get(i) - mean;
			varSum += d * d;
		}

		double variance = varSum / Math.max(n - 1, 1);

		return new Stats(mean, variance);
	}

	private double max(double a, double b, double c) {
		return Math.max(a, Math.max(b, c));
	}

	private double min(double a, double b, double c) {
		return Math.min(a, Math.min(b, c));
	}

	private static class Stats {
		final double mean;
		final double variance;

		Stats(double mean, double variance) {
			this.mean = mean;
			this.variance = variance;
		}
	}
}