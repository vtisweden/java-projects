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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;

import se.vti.utils.misc.fileio.FileAppender;

/**
 * @author GunnarF
 * 
 * @param <X>
 */
public class BlockAverageTerminationCriterion<X> implements TerminationCriterion<X> {

	// -------------------- CONFIGURATION PARAMETERS --------------------

	private ToDoubleFunction<X> extractor = null;

	private long minSamples = 10 * 1000;

	private double standardizedMeanTolerance = 0.1;

	private double relativeVarianceTolerance = 0.1;

	private int checkInterval = 1000;

	private FileAppender statsLogger = null;

	// -------------------- INTERNAL STATES --------------------

	private long iterations;

	private final List<Double> samples = new ArrayList<>();

	private boolean stabilized = false;

	private Integer burnInIteration;
	private Double stabilizationMeanRange;
	private Double stabilizationVarianceRange;
	private Double threeWindowMeanRange;
	private Double threeWindowVarianceRange;

	// -------------------- CONSTRUCTION AND CONFIGURATION --------------------

	public BlockAverageTerminationCriterion() {
	}

	public BlockAverageTerminationCriterion<X> setExtractor(ToDoubleFunction<X> extractor) {
		this.extractor = extractor;
		return this;
	}

	public BlockAverageTerminationCriterion<X> setMinSamples(long minSamples) {
		if (minSamples % 10 != 0) {
			throw new IllegalArgumentException("minSamples must be a multiple of ten.");
		}
		this.minSamples = minSamples;
		return this;
	}

	public BlockAverageTerminationCriterion<X> setStandardizedMeanTolerance(double tol) {
		this.standardizedMeanTolerance = tol;
		return this;
	}

	public BlockAverageTerminationCriterion<X> setRelativeVarianceTolerance(double tol) {
		this.relativeVarianceTolerance = tol;
		return this;
	}

	public BlockAverageTerminationCriterion<X> setCheckInterval(int interval) {
		if (interval % 10 != 0) {
			throw new IllegalArgumentException("checkInterval must be a multiple of ten.");
		}
		this.checkInterval = interval;
		return this;
	}

	public BlockAverageTerminationCriterion<X> setConvergenceStatsFileName(String file) {
		if (file == null) {
			this.statsLogger = null;
		} else {
			try {
				this.statsLogger = new FileAppender(file);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return this;
	}

	// --------------- IMPLEMENTATION OF TerminationCriterion ---------------

	@Override
	public void start() {
		this.iterations = 0;
		this.samples.clear();
		this.stabilized = false;
		if (this.statsLogger != null) {
			this.statsLogger.appendLine(
					"iteration\tburnInIteration\tstabilizationMeanRange\tstabilizationVarianceRange\tthreeWindowMeanRange"
							+ "\tthreeWindowVarianceRange\tmeanTolerance\tvarianceTolerance\tstabilized");
		}
	}

	@Override
	public void processState(X state, double logWeight) {
		this.iterations++;
		this.samples.add((this.extractor == null) ? logWeight : this.extractor.applyAsDouble(state));
		if ((this.samples.size() >= this.minSamples) && (this.samples.size() % this.checkInterval == 0)) {
			checkStabilization();
			if (this.statsLogger != null) {
				this.statsLogger.appendLine(this.iterations + "\t" + this.nullToNothing(this.burnInIteration) + "\t"
						+ this.stabilizationMeanRange + "\t" + this.stabilizationVarianceRange + "\t"
						+ this.nullToNothing(this.threeWindowMeanRange) + "\t"
						+ this.nullToNothing(this.threeWindowVarianceRange) + "\t" + this.standardizedMeanTolerance
						+ "\t" + this.relativeVarianceTolerance + "\t" + this.stabilized);
			}
		}
	}

	@Override
	public void processState(X state) {
		if (this.extractor == null) {
			throw new UnsupportedOperationException();
		} else {
			this.processState(state, Double.NaN /* not used for anything */);
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

		for (int initialPercent = 0; initialPercent <= 60; initialPercent += 10) {
			int start = (int) (0.01 * initialPercent * _N);
			Stats stats = computeStats(start, _N);
			means.add(stats.mean);
			variances.add(stats.variance);
			cutIndices.add(start);
		}
		
		this.stabilizationMeanRange = Double.POSITIVE_INFINITY;
		this.stabilizationVarianceRange = Double.POSITIVE_INFINITY;

		for (int i = 0; i < means.size() - 2; i++) {

			double dMean1 = Math.abs(means.get(i) - means.get(i + 1))
					/ (Math.sqrt(0.5 * (variances.get(i) + variances.get(i + 1))) + 1e-8);
			double dVar1 = Math.abs(variances.get(i) - variances.get(i + 1))
					/ (0.5 * (variances.get(i) + variances.get(i + 1)) + 1e-8);

			double dMean2 = Math.abs(means.get(i + 1) - means.get(i + 2))
					/ (Math.sqrt(0.5 * (variances.get(i + 1) + variances.get(i + 2))) + 1e-8);
			double dVar2 = Math.abs(variances.get(i + 1) - variances.get(i + 2))
					/ (0.5 * (variances.get(i + 1) + variances.get(i + 2)) + 1e-8);

			this.stabilizationMeanRange = Math.min(this.stabilizationMeanRange, Math.max(dMean1, dMean2));
			this.stabilizationVarianceRange = Math.min(this.stabilizationVarianceRange, Math.max(dVar1, dVar2));

			boolean stableMeans = (dMean1 < this.standardizedMeanTolerance)
					&& (dMean2 < this.standardizedMeanTolerance);
			boolean stableVars = (dVar1 < this.relativeVarianceTolerance) && (dVar2 < this.relativeVarianceTolerance);

			int burnInIndex = cutIndices.get(i + 1);
			this.burnInIteration = burnInIndex;
			
			boolean threeWindowConsistent = this.checkThreeWindowConsistency(burnInIndex);

			this.stabilized = stableMeans && stableVars && threeWindowConsistent;
		}
	}

	private boolean checkThreeWindowConsistency(int start) {

		int _N = this.samples.size();
		int remaining = _N - start;
		int _L = remaining / 3;

		Stats w1 = this.computeStats(start, start + _L);
		Stats w2 = this.computeStats(start + _L, start + 2 * _L);
		Stats w3 = this.computeStats(start + 2 * _L, _N);

		double meanVariance = (w1.variance + w2.variance + w3.variance) / 3.0;

		double meanRange = (this.max(w1.mean, w2.mean, w3.mean) - this.min(w1.mean, w2.mean, w3.mean))
				/ (Math.sqrt(meanVariance) + 1e-8);
		double varRange = (this.max(w1.variance, w2.variance, w3.variance)
				- this.min(w1.variance, w2.variance, w3.variance)) / (meanVariance + 1e-8);

		this.threeWindowMeanRange = meanRange;
		this.threeWindowVarianceRange = varRange;

		return ((meanRange < this.standardizedMeanTolerance) && (varRange < this.relativeVarianceTolerance));
	}

	// -------------------- HELPERS --------------------

	private String nullToNothing(Object obj) {
		return (obj == null) ? "" : obj.toString();
	}

	private static class Stats {
		final double mean;
		final double variance;

		Stats(double mean, double variance) {
			this.mean = mean;
			this.variance = variance;
		}
	}

	private Stats computeStats(int from, int to) {
		int n = to - from;

		double sum = 0.0;
		for (int i = from; i < to; i++) {
			sum += this.samples.get(i);
		}
		double mean = sum / n;

		double varSum = 0.0;
		for (int i = from; i < to; i++) {
			double d = this.samples.get(i) - mean;
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

	// -------------------- TESTING/EXPLORATION --------------------

	private static class Process {

		final double scale;
		final double offset;
		final double rate;
		final Random rnd;

		int k = 0;

		Process(double scale, double offset, double rate, Random rnd) {
			this.scale = scale;
			this.offset = offset;
			this.rate = rate;
			this.rnd = rnd;
		}

		double next() {
			return this.scale * Math.exp(-this.rate * this.k++) + this.offset + this.rnd.nextGaussian();
		}
	}

	public static void main(String[] args) throws IOException {
		var rnd = new Random();

		var allXLists = new ArrayList<List<Double>>();
		System.out.println("stabilizationMean\tstabilizationVar\tthreeWindowMean\tthreeWindowVar");
		for (int r = 0; r < 20; r++) {
			double scale = rnd.nextDouble(-10, 10);
			double offset = rnd.nextDouble(-10, 10);
			double rate = 1.0 / rnd.nextDouble(10, 10_000);
			var process = new Process(scale, offset, rate, rnd);
			var criterion = new BlockAverageTerminationCriterion<Double>().setCheckInterval(1000)
					.setExtractor(x -> x.doubleValue()).setMinSamples(10_000).setRelativeVarianceTolerance(0.1)
					.setStandardizedMeanTolerance(0.1)
					.setConvergenceStatsFileName("./convergenceStats/" + r + ".convergenceStats.tsv");
			int k = 0;
			var xList = new ArrayList<Double>();
			criterion.start();
			while (!criterion.terminate() && k < 1_000_000) {
				double x = process.next();
				xList.add(x);
				criterion.processState(x);
				if (criterion.terminate()) {
					System.out.println(criterion.stabilizationMeanRange + "\t" + criterion.stabilizationVarianceRange
							+ "\t" + criterion.threeWindowMeanRange + "\t" + criterion.threeWindowVarianceRange);
				}
				k++;
			}
			allXLists.add(xList);
		}

		var writer = new PrintWriter("data.tsv");
		int maxK = allXLists.stream().mapToInt(l -> l.size()).max().getAsInt();
		for (int k = 0; k < maxK; k++) {
			writer.print(k + "\t");
			for (var xList : allXLists) {
				if (xList.size() > k) {
					writer.print(xList.get(k));
				}
				writer.print("\t");
			}
			writer.println();
		}
		writer.flush();
		writer.close();
	}
}