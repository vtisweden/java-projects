/**
 * se.vti.samgods
 *
 * Copyright (C) 2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.preprocessing.loopgeneration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.scenario.NodeWithCoords;

/**
 * @author Gunnar Flötteröd
 */
class UniformTransportEfficiencyAnalyzer {

	private static final int BIN_COUNT = 10;

	// -------------------- SCENARIO DATA --------------------

	private final ScenarioContainer scenarioContainer;

	// -------------------- RESULTS --------------------

	final int maxSize;

	final List<double[]> sizeHistogramList;

	final List<double[]> kTonKmEfficiencyHistogramList;

	final List<double[]> kTonEfficiencyHistogramList;

	final List<double[]> kTonKmEfficiencyOverSizeList;

	final List<double[]> kTonEfficiencyOverSizeList;

	final List<double[]> coveredDemandShareOverRoundtripsByKTonKmEfficiencyList;
	final List<double[]> coveredDemandShareOverRoundtripsByKTonEfficiencyList;
	final List<double[]> coveredDemandShareOverRoundtripsByVolumeList;
	final List<double[]> coveredDemandShareOverRoundtripsBySizeList;

	final List<double[]> coveredODsOverRoundtripsByKTonKmEfficiencyList;
	final List<double[]> coveredODsOverRoundtripsByKTonEfficiencyList;
	final List<double[]> coveredODsOverRoundtripsByVolumeList;
	final List<double[]> coveredODsOverRoundtripsBySizeList;

	final List<double[]> odIndex2NumberOfVisistsList;

	// -------------------- CONSTRUCTION --------------------

	UniformTransportEfficiencyAnalyzer(ScenarioContainer scenarioContainer, int maxSize,
			List<double[]> sizeHistogramList, List<double[]> tonKmEfficiencyHistogramList,
			List<double[]> kTonEfficiencyHistogramList, List<double[]> kTonKmEfficiencyOverSizeList,
			List<double[]> kTonEfficiencyOverSizeList,
			List<double[]> coveredDemandShareOverRoundtripsByKTonKmEfficiencyList,
			List<double[]> coveredDemandShareOverRoundtripsByKTonEfficiencyList,
			List<double[]> coveredDemandShareOverRoundtripsByVolumeList,
			List<double[]> coveredDemandShareOverRoundtripsBySizeList,
			List<double[]> coveredODsOverRoundtripsByKTonKmEfficiencyList,
			List<double[]> coveredODsOverRoundtripsByKTonEfficiencyList,
			List<double[]> coveredODsOverRoundtripsByVolumeList, List<double[]> coveredODsOverRoundtripsBySizeList,
			List<double[]> odIndex2NumberOfVisistsList) {
		this.scenarioContainer = scenarioContainer;
		this.maxSize = maxSize;
		this.sizeHistogramList = sizeHistogramList;
		this.kTonKmEfficiencyHistogramList = tonKmEfficiencyHistogramList;
		this.kTonEfficiencyHistogramList = kTonEfficiencyHistogramList;
		this.kTonKmEfficiencyOverSizeList = kTonKmEfficiencyOverSizeList;
		this.kTonEfficiencyOverSizeList = kTonEfficiencyOverSizeList;

		this.coveredDemandShareOverRoundtripsByKTonKmEfficiencyList = coveredDemandShareOverRoundtripsByKTonKmEfficiencyList;
		this.coveredDemandShareOverRoundtripsByKTonEfficiencyList = coveredDemandShareOverRoundtripsByKTonEfficiencyList;
		this.coveredDemandShareOverRoundtripsByVolumeList = coveredDemandShareOverRoundtripsByVolumeList;
		this.coveredDemandShareOverRoundtripsBySizeList = coveredDemandShareOverRoundtripsBySizeList;

		this.coveredODsOverRoundtripsByKTonKmEfficiencyList = coveredODsOverRoundtripsByKTonKmEfficiencyList;
		this.coveredODsOverRoundtripsByKTonEfficiencyList = coveredODsOverRoundtripsByKTonEfficiencyList;
		this.coveredODsOverRoundtripsByVolumeList = coveredODsOverRoundtripsByVolumeList;
		this.coveredODsOverRoundtripsBySizeList = coveredODsOverRoundtripsBySizeList;

		this.odIndex2NumberOfVisistsList = odIndex2NumberOfVisistsList;
	}

	// -------------------- IMPLEMENTATION --------------------

	record RoundTripEfficiency(int roundTripIndex, double loaded_kTonKm, double potential_kTonKm, double loaded_kTon,
			double potential_kTon, int size) {
		double kTonKmEfficiency() {
			return this.potential_kTonKm > 0.0 ? this.loaded_kTonKm / this.potential_kTonKm : 0.0;
		}

		double kTonEfficiency() {
			return this.potential_kTon > 0.0 ? this.loaded_kTon / this.potential_kTon : 0.0;
		}
	}

	void process(MultiRoundTrip<NodeWithCoords> roundTrips) {
		final var odCoverage = (ODCoverage<NodeWithCoords>) roundTrips.getSummary(ODCoverage.class);
		if (roundTrips.size() != odCoverage.getNumberOfRoundTrips()) {
			throw new IllegalStateException("Inconsistent numbers of round trips: " + roundTrips.size() + " versus "
					+ odCoverage.getNumberOfRoundTrips() + ".");
		}

		final int numberOfRTs = roundTrips.size();
		final int numberOfODs = odCoverage.getNumberOfODs();

		/*
		 * DISTRIBUTE EACH COVERED OD DEMAND UNIFORMLY OVER ITS CONNECTING ROUND TRIPS.
		 */
		final double[][] roundTripAndODIndices2Transport_kTon = new double[numberOfRTs][numberOfODs];
		for (int odIndex = 0; odIndex < numberOfODs; odIndex++) {
			final double demand_kTon = this.scenarioContainer.getDemand_kTon(odIndex);
			final var roundTripIndices = odCoverage.getConnectingRoundTripIndices(odIndex);
			if (!roundTripIndices.isEmpty()) {
				final double singleRoundTripShare = 1.0 / roundTripIndices.size();
				for (int roundTripIndex : roundTripIndices) {
					roundTripAndODIndices2Transport_kTon[roundTripIndex][odIndex] = singleRoundTripShare * demand_kTon;
				}
			}
		}

		/*
		 * EXTRACT ROUND-TRIP EFFICIENCIES.
		 */
		final var roundTripEfficiencies = new ArrayList<RoundTripEfficiency>(numberOfRTs);
		for (int roundTripIndex = 0; roundTripIndex < numberOfRTs; roundTripIndex++) {
			double maxTransport_kTon = 0.0;
			for (int odIndex : odCoverage.getConnectedODIndices(roundTripIndex)) {
				maxTransport_kTon = Math.max(maxTransport_kTon,
						roundTripAndODIndices2Transport_kTon[roundTripIndex][odIndex]);
			}

			double loaded_kTonKm = 0.0;
			double potential_kTonKm = 0.0;
			double loaded_kTon = 0.0;
			double potential_kTon = 0.0;
			for (int odIndex : odCoverage.getConnectedODIndices(roundTripIndex)) {
				final double distance_km = this.scenarioContainer.getDistance_km(odIndex);
				final double transport_kTon = roundTripAndODIndices2Transport_kTon[roundTripIndex][odIndex];
				loaded_kTonKm += distance_km * transport_kTon;
				potential_kTonKm += distance_km * maxTransport_kTon;
				loaded_kTon += transport_kTon;
				potential_kTon += maxTransport_kTon;
			}

			roundTripEfficiencies.add(new RoundTripEfficiency(roundTripIndex, loaded_kTonKm, potential_kTonKm,
					loaded_kTon, potential_kTon, roundTrips.getRoundTrip(roundTripIndex).size()));
		}

		/*
		 * HISTOGRAM OF ROUND-TRIP SIZES.
		 *
		 * Sizes from 0 through maxSize are distributed over BIN_COUNT bins. The
		 * histogram contains shares of round trips, not absolute counts.
		 */
		var sizeHistogram = new double[BIN_COUNT];
		for (var roundTripEfficiency : roundTripEfficiencies) {
			final int size = roundTripEfficiency.size();
			if (size < 0 || size > this.maxSize) {
				throw new IllegalStateException(
						"Round-trip size " + size + " is outside the expected interval [0, " + this.maxSize + "].");
			}
			final int bin = Math.min(BIN_COUNT - 1, size * BIN_COUNT / (this.maxSize + 1));
			sizeHistogram[bin]++;
		}
		sizeHistogram = Arrays.stream(sizeHistogram).map(count -> count / numberOfRTs).toArray();
		this.sizeHistogramList.add(sizeHistogram);

		/*
		 * MEAN ROUND-TRIP EFFICIENCY BY SIZE.
		 *
		 * Uses the same size bins as sizeHistogram. Empty bins are represented by
		 * Double.NaN, since there is no meaningful mean efficiency for such a bin.
		 */
		var kTonKmEfficiencyBySize = new double[BIN_COUNT];
		var kTonEfficiencyBySize = new double[BIN_COUNT];
		var roundTripCountBySize = new int[BIN_COUNT];

		for (var roundTripEfficiency : roundTripEfficiencies) {
			final int size = roundTripEfficiency.size();

			if (size < 0 || size > this.maxSize) {
				throw new IllegalStateException(
						"Round-trip size " + size + " is outside the expected interval [0, " + this.maxSize + "].");
			}

			final int bin = Math.min(BIN_COUNT - 1, size * BIN_COUNT / (this.maxSize + 1));

			kTonKmEfficiencyBySize[bin] += roundTripEfficiency.kTonKmEfficiency();
			kTonEfficiencyBySize[bin] += roundTripEfficiency.kTonEfficiency();
			roundTripCountBySize[bin]++;
		}

		for (int bin = 0; bin < BIN_COUNT; bin++) {
			if (roundTripCountBySize[bin] > 0) {
				kTonKmEfficiencyBySize[bin] /= roundTripCountBySize[bin];
				kTonEfficiencyBySize[bin] /= roundTripCountBySize[bin];
			} else {
				kTonKmEfficiencyBySize[bin] = Double.NaN;
				kTonEfficiencyBySize[bin] = Double.NaN;
			}
		}

		this.kTonKmEfficiencyOverSizeList.add(kTonKmEfficiencyBySize);
		this.kTonEfficiencyOverSizeList.add(kTonEfficiencyBySize);

		/*
		 * HISTOGRAM OF ROUND-TRIP kTonKm EFFICIENCIES.
		 */
		var kTonKmEfficiencyHistogram = new double[BIN_COUNT];
		for (var roundTripEfficiency : roundTripEfficiencies) {
			int bin = (int) (roundTripEfficiency.kTonKmEfficiency() * BIN_COUNT);
			bin = Math.max(0, Math.min(BIN_COUNT - 1, bin));
			kTonKmEfficiencyHistogram[bin]++;
		}
		kTonKmEfficiencyHistogram = Arrays.stream(kTonKmEfficiencyHistogram).map(count -> count / numberOfRTs)
				.toArray();
		this.kTonKmEfficiencyHistogramList.add(kTonKmEfficiencyHistogram);

		/*
		 * HISTOGRAM OF ROUND-TRIP kTon EFFICIENCIES.
		 */
		var kTonEfficiencyHistogram = new double[BIN_COUNT];
		for (var roundTripEfficiency : roundTripEfficiencies) {
			int bin = (int) (roundTripEfficiency.kTonEfficiency() * BIN_COUNT);
			bin = Math.max(0, Math.min(BIN_COUNT - 1, bin));
			kTonEfficiencyHistogram[bin]++;
		}
		kTonEfficiencyHistogram = Arrays.stream(kTonEfficiencyHistogram).map(count -> count / numberOfRTs).toArray();
		this.kTonEfficiencyHistogramList.add(kTonEfficiencyHistogram);

		/*
		 * Total OD demand volume, including demand that is not covered by any round
		 * trip.
		 */
		double totalDemand_kTon = this.scenarioContainer.getTotalDemand_kTon();

		/*
		 * CUMULATIVE COVERED-DEMAND SHARES WHEN ROUND TRIPS ARE RANKED BY kTonKm
		 * EFFICIENCY.
		 */
		roundTripEfficiencies.sort(Comparator.comparingDouble(RoundTripEfficiency::kTonKmEfficiency).reversed());
		this.coveredDemandShareOverRoundtripsByKTonKmEfficiencyList.add(
				cumulativeDemandCoverage(roundTripEfficiencies, RoundTripEfficiency::loaded_kTon, totalDemand_kTon));
		this.coveredODsOverRoundtripsByKTonKmEfficiencyList
				.add(cumulativeODCoverage(roundTripEfficiencies, odCoverage, numberOfODs));

		/*
		 * CUMULATIVE COVERED-DEMAND SHARES WHEN ROUND TRIPS ARE RANKED BY kTon
		 * EFFICIENCY.
		 */
		roundTripEfficiencies.sort(Comparator.comparingDouble(RoundTripEfficiency::kTonEfficiency).reversed());
		this.coveredDemandShareOverRoundtripsByKTonEfficiencyList.add(
				cumulativeDemandCoverage(roundTripEfficiencies, RoundTripEfficiency::loaded_kTon, totalDemand_kTon));
		this.coveredODsOverRoundtripsByKTonEfficiencyList
				.add(cumulativeODCoverage(roundTripEfficiencies, odCoverage, numberOfODs));

		/*
		 * CUMULATIVE COVERED-DEMAND SHARES WHEN ROUND TRIPS ARE RANKED BY VOLUME.
		 */
		roundTripEfficiencies.sort(Comparator.comparingDouble(RoundTripEfficiency::loaded_kTon).reversed());
		this.coveredDemandShareOverRoundtripsByVolumeList.add(
				cumulativeDemandCoverage(roundTripEfficiencies, RoundTripEfficiency::loaded_kTon, totalDemand_kTon));
		this.coveredODsOverRoundtripsByVolumeList
				.add(cumulativeODCoverage(roundTripEfficiencies, odCoverage, numberOfODs));

		/*
		 * CUMULATIVE COVERED-DEMAND SHARES WHEN ROUND TRIPS ARE RANKED BY SIZE.
		 */
		roundTripEfficiencies.sort(Comparator.comparingDouble(RoundTripEfficiency::size).reversed());
		this.coveredDemandShareOverRoundtripsBySizeList.add(
				cumulativeDemandCoverage(roundTripEfficiencies, RoundTripEfficiency::loaded_kTon, totalDemand_kTon));
		this.coveredODsOverRoundtripsBySizeList
				.add(cumulativeODCoverage(roundTripEfficiencies, odCoverage, numberOfODs));

		/*
		 * UPDATE TOTAL NUMBER OF VISITS PER OD PAIR
		 */
		double[] odIndex2NumberOfVisits = new double[numberOfODs];
		for (int odIndex = 0; odIndex < numberOfODs; odIndex++) {
			odIndex2NumberOfVisits[odIndex] += odCoverage.getConnectingRoundTripIndices(odIndex).size();
		}
		this.odIndex2NumberOfVisistsList.add(odIndex2NumberOfVisits);
	}

	private static double[] cumulativeDemandCoverage(ArrayList<RoundTripEfficiency> sortedRoundTripEfficiencies,
			ToDoubleFunction<RoundTripEfficiency> loadedDemand, double totalToBeCovered) {

		final double[] result = new double[BIN_COUNT];

		if (sortedRoundTripEfficiencies.isEmpty() || totalToBeCovered <= 0.0) {
			return result;
		}

		double cumulative = 0.0;
		int includedRoundTripCount = 0;

		for (int bin = 0; bin < BIN_COUNT; bin++) {

			final int targetRoundTripCount = (int) Math
					.ceil((bin + 1.0) * sortedRoundTripEfficiencies.size() / BIN_COUNT);

			while (includedRoundTripCount < targetRoundTripCount) {
				cumulative += loadedDemand.applyAsDouble(sortedRoundTripEfficiencies.get(includedRoundTripCount));
				includedRoundTripCount++;
			}

			result[bin] = cumulative / totalToBeCovered;
		}

		return result;
	}

	private static double[] cumulativeODCoverage(ArrayList<RoundTripEfficiency> sortedRoundTripEfficiencies,
			ODCoverage<NodeWithCoords> odCoverage, int numberOfODs) {

		final double[] result = new double[BIN_COUNT];

		if (sortedRoundTripEfficiencies.isEmpty() || numberOfODs == 0) {
			return result;
		}

		final boolean[] covered = new boolean[numberOfODs];
		int coveredODCount = 0;
		int includedRoundTripCount = 0;

		for (int bin = 0; bin < BIN_COUNT; bin++) {

			final int targetRoundTripCount = (int) Math
					.ceil((bin + 1.0) * sortedRoundTripEfficiencies.size() / BIN_COUNT);

			while (includedRoundTripCount < targetRoundTripCount) {

				final int roundTripIndex = sortedRoundTripEfficiencies.get(includedRoundTripCount).roundTripIndex();

				for (int odIndex : odCoverage.getConnectedODIndices(roundTripIndex)) {
					if (!covered[odIndex]) {
						covered[odIndex] = true;
						coveredODCount++;
					}
				}

				includedRoundTripCount++;
			}

			result[bin] = ((double) coveredODCount) / numberOfODs;
		}

		return result;
	}
}
