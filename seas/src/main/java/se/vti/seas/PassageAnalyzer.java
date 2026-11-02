/**
 * se.vti.seas
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
package se.vti.seas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import se.vti.utils.misc.Units;

/**
 * @author GunnarF
 */
class PassageAnalyzer {

	// -------------------- DATA AND PARAMETERS --------------------

	private final List<Passage> passagesA = new ArrayList<>();

	private final List<Passage> passagesB = new ArrayList<>();

	private Integer draughtInterval_m;

	private Integer speedInterval_km_h;

	private Double effectiveLengthFactor;

	private Double effectiveWidthFactor;

	private Double timeWindow_h;

	private Double lateralBinSize_m;

	private Double overtakingWidthPercentile;

	private int speedLimit_km_h = Integer.MAX_VALUE;

	// -------------------- RESULTS --------------------

	static class OneSidedResult {
		public List<Passage> consideredPassages;
		public Double rate_veh_h;
		public List<VesselClass> vesselClasses;
		public ChannelAssignment assignment;
		public DiscretizedChannelGeometry realChannelUsage;
		public DiscretizedChannelGeometry modeledChannelUsage;
	}

	private OneSidedResult resultA;

	private OneSidedResult resultB;

	// -------------------- CONSTRUCTION & CONFIGURATION --------------------

	PassageAnalyzer() {
	}

	PassageAnalyzer addPassages(List<Passage> passages, Set<String> passageDirectionsA,
			Set<String> passageDirectionsB) {
		this.passagesA.addAll(
				passages.stream().filter(p -> passageDirectionsA.contains(p.crossingCompassDirection)).toList());
		this.passagesB.addAll(
				passages.stream().filter(p -> passageDirectionsB.contains(p.crossingCompassDirection)).toList());
		return this;
	}

	PassageAnalyzer setDraughtInterval_m(int draughtInterval_m) {
		this.draughtInterval_m = draughtInterval_m;
		return this;
	}

	PassageAnalyzer setSpeedInterval_km_h(int speedInterval_km_h) {
		this.speedInterval_km_h = speedInterval_km_h;
		return this;
	}

	PassageAnalyzer setEffectiveLengthFactor(double effectiveLengthFactor) {
		this.effectiveLengthFactor = effectiveLengthFactor;
		return this;
	}

	PassageAnalyzer setEffectiveWidthFactor(double effectiveWidthFactor) {
		this.effectiveWidthFactor = effectiveWidthFactor;
		return this;
	}

	PassageAnalyzer setTimeWindow_h(double timeWindow_h) {
		this.timeWindow_h = timeWindow_h;
		return this;
	}

	PassageAnalyzer setLateralBinSize_m(double lateralBinSize_m) {
		this.lateralBinSize_m = lateralBinSize_m;
		return this;
	}

	PassageAnalyzer setOvertakingWidthPercentile(double overtakingWidthPercentile) {
		this.overtakingWidthPercentile = overtakingWidthPercentile;
		return this;
	}

	PassageAnalyzer setSpeedLimit_km_h(int speedLimit_km_h) {
		this.speedLimit_km_h = speedLimit_km_h;
		return this;
	}

	// -------------------- INTERNALS --------------------

	private List<VesselClass> computeVesselClasses(List<Passage> assignmentCompletePassages) {

		Map<Integer, Map<Integer, List<Passage>>> draughtIndex2SpeedIndex2Passages = new LinkedHashMap<>();
		for (var passage : assignmentCompletePassages) {
			int draughtIndex = (int) Math.ceil(passage.draught_m / this.draughtInterval_m);
			int speedIndex = (int) Math
					.ceil(Math.min(this.speedLimit_km_h, passage.speed_km_h) / this.speedInterval_km_h);
			draughtIndex2SpeedIndex2Passages.computeIfAbsent(draughtIndex, i -> new LinkedHashMap<>())
					.computeIfAbsent(speedIndex, s -> new ArrayList<>()).add(passage);
		}

		List<VesselClass> vesselClasses = new ArrayList<>();
		for (var draughtEntry : draughtIndex2SpeedIndex2Passages.entrySet()) {
			int draughtIndex = draughtEntry.getKey();
			int draught_m = draughtIndex * this.draughtInterval_m;
			for (var speedEntry : draughtEntry.getValue().entrySet()) {
				int speedIndex_km_h = speedEntry.getKey();
				int speed_km_h = speedIndex_km_h * this.speedInterval_km_h - this.speedInterval_km_h / 2;
				var passages = speedEntry.getValue();

				Set<String> shipTypes = new LinkedHashSet<>();
				var widthStats_m = new DescriptiveStatistics();
				var lengthStats_m = new DescriptiveStatistics();
				for (var passage : passages) {
					shipTypes.add(passage.shipType);
					widthStats_m.addValue(passage.width_m);
					lengthStats_m.addValue(passage.length_m);
				}

				String vesselClassName = draught_m + "m_" + speed_km_h + "km/h_"
						+ shipTypes.stream().sorted().collect(Collectors.joining("_"));
				double marketShare = ((double) lengthStats_m.getN()) / assignmentCompletePassages.size();
				vesselClasses.add(new VesselClass(vesselClassName, (int) Math.round(lengthStats_m.getMean()),
						(int) Math.round(widthStats_m.getMean()), draught_m, speed_km_h, marketShare,
						this.effectiveLengthFactor, this.effectiveWidthFactor));
			}
		}

		Collections.sort(vesselClasses);
		return vesselClasses;
	}

	private OneSidedResult computeOneSidedAssignmentResult(List<Passage> consideredPassages,
			int totalNumberOfPassages) {
		OneSidedResult result = new OneSidedResult();
		result.rate_veh_h = totalNumberOfPassages / this.timeWindow_h;
		result.consideredPassages = consideredPassages;
		result.vesselClasses = this.computeVesselClasses(result.consideredPassages);
		var assignmentSolver = new ChannelAssignmentSolver();
		for (VesselClass vesselClass : result.vesselClasses) {
			assignmentSolver.addVesselClass(vesselClass, vesselClass.marketShare * result.rate_veh_h);
		}
		result.assignment = assignmentSolver.computeAssignment();
		result.modeledChannelUsage = result.assignment.computeModeledUsage(this.lateralBinSize_m,
				this.overtakingWidthPercentile);
		return result;
	}

	private DiscretizedChannelGeometry computeRealChannelUsage(List<Passage> passages, double centerLatCoord_m,
			double outerLatCoord_m) {
		boolean invertLateralDirection = (centerLatCoord_m > outerLatCoord_m);
		double minLatCoord_m = Math.min(centerLatCoord_m, outerLatCoord_m);
		double maxLatCoord_m = Math.max(centerLatCoord_m, outerLatCoord_m);
		int numberOfBins = 1 + (int) Math.ceil((maxLatCoord_m - minLatCoord_m) / this.lateralBinSize_m);

		var draughtStats_m = IntStream.range(0, numberOfBins).boxed().map(i -> new DescriptiveStatistics()).toList();
		for (var passage : passages) {
			final double latDistFromCenter_m;
			if (invertLateralDirection) {
				latDistFromCenter_m = centerLatCoord_m - passage.signCorrectedLatCoord_m;
			} else {
				latDistFromCenter_m = passage.signCorrectedLatCoord_m - centerLatCoord_m;
			}
			int minBin = (int) Math.floor((latDistFromCenter_m - 0.5 * passage.width_m) / this.lateralBinSize_m);
			int maxBin = (int) Math.ceil((latDistFromCenter_m + 0.5 * passage.width_m) / this.lateralBinSize_m);
			for (int bin = Math.max(0, minBin); bin <= Math.min(maxBin, numberOfBins - 1); bin++) {
				draughtStats_m.get(bin).addValue(passage.draught_m);
			}
		}

		var outerCoordinates_m = new ArrayList<Double>(numberOfBins);
		var draughts_m = new ArrayList<Integer>(numberOfBins);
		for (int bin = 0; bin < numberOfBins; bin++) {
			outerCoordinates_m.add((bin + 1) * this.lateralBinSize_m);
			draughts_m.add((int) Math.ceil(draughtStats_m.get(bin).getMax()));
		}
		return new DiscretizedChannelGeometry(outerCoordinates_m, draughts_m);
	}

	// -------------------- IMPLEMENTATION --------------------

	public void compute() {

		// Consider only sufficiently complete passages.
		var consideredPassagesA = new ArrayList<>(
				this.passagesA.stream().filter(p -> p.isAssignmentComplete() && p.lateralCoord_m != null).toList());
		var consideredPassagesB = new ArrayList<>(
				this.passagesB.stream().filter(p -> p.isAssignmentComplete() && p.lateralCoord_m != null).toList());

		// Make sure that sign corrected lateral coordinates increase from A to B.
		double avgLatCoordA_m = consideredPassagesA.stream().mapToDouble(p -> p.lateralCoord_m).average().getAsDouble();
		double avgLatCoordB_m = consideredPassagesB.stream().mapToDouble(p -> p.lateralCoord_m).average().getAsDouble();
		double coordFact = (avgLatCoordA_m < avgLatCoordB_m) ? +1.0 : -1.0;
		this.passagesA.stream().forEach(p -> p.signCorrectedLatCoord_m = coordFact * p.lateralCoord_m);
		this.passagesB.stream().forEach(p -> p.signCorrectedLatCoord_m = coordFact * p.lateralCoord_m);

		// Estimate used lateral size of channel.
		double minLatCoordA_m = consideredPassagesA.stream()
				.mapToDouble(p -> p.signCorrectedLatCoord_m - 0.5 * p.width_m).min().getAsDouble();
		double maxLatCoordA_m = consideredPassagesA.stream().mapToDouble(p -> p.signCorrectedLatCoord_m).max()
				.getAsDouble();
		double minLatCoordB_m = consideredPassagesB.stream().mapToDouble(p -> p.signCorrectedLatCoord_m).min()
				.getAsDouble();
		double maxLatCoordB_m = consideredPassagesB.stream()
				.mapToDouble(p -> p.signCorrectedLatCoord_m + 0.5 * p.width_m).max().getAsDouble();
		Double centerLatCoord_m;
		if (maxLatCoordA_m <= minLatCoordB_m) {
			centerLatCoord_m = 0.5 * (maxLatCoordA_m + minLatCoordB_m);
		} else {
			centerLatCoord_m = null;
			Long centerError = null;
			for (double cand_m = minLatCoordB_m; cand_m <= maxLatCoordA_m; cand_m += 1.0) {
				final double finalCand_m = cand_m;
				long candError = consideredPassagesA.stream().filter(p -> p.signCorrectedLatCoord_m > finalCand_m)
						.count()
						+ consideredPassagesB.stream().filter(p -> p.signCorrectedLatCoord_m < finalCand_m).count();
				if ((centerLatCoord_m == null) || (candError < centerError)) {
					centerLatCoord_m = cand_m;
					centerError = candError;
				}
			}
		}

		// Consider only passages that are entirely in their own half.
		final double finalCenterLatCoord_m = centerLatCoord_m;
		consideredPassagesA = new ArrayList<>(consideredPassagesA.stream()
				.filter(p -> p.signCorrectedLatCoord_m + 0.5 * p.width_m <= finalCenterLatCoord_m).toList());
		consideredPassagesB = new ArrayList<>(consideredPassagesB.stream()
				.filter(p -> p.signCorrectedLatCoord_m - 0.5 * p.width_m >= finalCenterLatCoord_m).toList());

		// Given the considered passages, compute results.
		this.resultA = this.computeOneSidedAssignmentResult(consideredPassagesA, this.passagesA.size());
		this.resultB = this.computeOneSidedAssignmentResult(consideredPassagesB, this.passagesB.size());
		this.resultA.realChannelUsage = this.computeRealChannelUsage(this.resultA.consideredPassages, centerLatCoord_m,
				minLatCoordA_m);
		this.resultB.realChannelUsage = this.computeRealChannelUsage(this.resultB.consideredPassages, centerLatCoord_m,
				maxLatCoordB_m);
	}

	// ==================== CONCRETE SEAS SCENARIOS ====================

	private static String createVesselStatistics(List<VesselClass> vesselClasses) {
		StringBuffer result = new StringBuffer("Class\tDraught[m]\tSpeed[km/h]\tMarketShare[%]\n");
		for (var vesselClass : vesselClasses) {
			result.append(vesselClass.name);
			result.append("\t");
			result.append(vesselClass.draught_m);
			result.append("\t");
			result.append(vesselClass.speed_km_h);
			result.append("\t");
			result.append(String.format("%.2f", 100.0 * vesselClass.marketShare).replace(",", "."));
			result.append("\n");
		}
		return result.toString();
	}

	static void processPassages(String fileName, Set<String> compassDirectionsA, Set<String> compassDirectionsB,
			int speedLimit_km_h) {
		processPassages(Arrays.asList(fileName), Arrays.asList(compassDirectionsA), Arrays.asList(compassDirectionsB),
				speedLimit_km_h);
	}

	static void processPassages(List<String> fileNames, List<Set<String>> inboundCompassDirections,
			List<Set<String>> outboundCompassDirections, int speedLimit_km_h) {

		var passageAnalyzer = new PassageAnalyzer().setDraughtInterval_m(2).setSpeedInterval_km_h(4)
				.setEffectiveLengthFactor(5.0).setEffectiveWidthFactor(2.0).setTimeWindow_h(Units.H_PER_D * 31)
				.setLateralBinSize_m(10.0).setOvertakingWidthPercentile(75.0).setSpeedLimit_km_h(speedLimit_km_h);
		for (int i = 0; i < fileNames.size(); i++) {
			passageAnalyzer.addPassages(new PassageReader(fileNames.get(i)).passages, inboundCompassDirections.get(i),
					outboundCompassDirections.get(i));
		}
		passageAnalyzer.compute();

		System.out.println("OVERVIEW");
		System.out.println("Passage files: " + fileNames);
		System.out.println("Directions A: " + inboundCompassDirections);
		System.out.println("Directions B: " + outboundCompassDirections);
		System.out.println("Complete/total passages A: " + passageAnalyzer.resultA.consideredPassages.size() + "/"
				+ passageAnalyzer.passagesA.size());
		System.out.println("Complete/total passages B: " + passageAnalyzer.resultB.consideredPassages.size() + "/"
				+ passageAnalyzer.passagesB.size());
		System.out.println();

		System.out.println("INBOUND VESSEL STATISTICS");
		System.out.println(createVesselStatistics(passageAnalyzer.resultA.vesselClasses));

		System.out.println("OUTBOUND VESSEL STATISTICS");
		System.out.println(createVesselStatistics(passageAnalyzer.resultB.vesselClasses));

		System.out.println("INBOUND LANE ASSIGNMENT");
		System.out.println(passageAnalyzer.resultA.assignment);

		System.out.println("OUTBOUND LANE ASSIGNMENT");
		System.out.println(passageAnalyzer.resultB.assignment);

		System.out.println();
		System.out.println("CHANNEL USAGE");
		printLeftToRight(passageAnalyzer.resultA, passageAnalyzer.resultB, passageAnalyzer.lateralBinSize_m);
	}

	private static void printLeftToRight(OneSidedResult resultA, OneSidedResult resultB, double binSize_m) {

		System.out.println("OuterCoord[m]\tModeledDraught[m]\tRealDraught[m]");
		for (int bin = Math.max(resultA.modeledChannelUsage.binCnt(),
				resultA.realChannelUsage.binCnt()); bin >= 0; bin--) {
			System.out.print(-binSize_m * bin);
			System.out.print("\t");
			System.out.print(
					bin < resultA.modeledChannelUsage.binCnt() ? -resultA.modeledChannelUsage.draughts_m.get(bin) : 0);
			System.out.print("\t");
			System.out.println(
					bin < resultA.realChannelUsage.binCnt() ? -resultA.realChannelUsage.draughts_m.get(bin) : 0);
		}

		for (int bin = 0; bin <= Math.max(resultB.modeledChannelUsage.binCnt(),
				resultB.realChannelUsage.binCnt()); bin++) {
			System.out.print(binSize_m * bin);
			System.out.print("\t");
			System.out.print(
					bin < resultB.modeledChannelUsage.binCnt() ? -resultB.modeledChannelUsage.draughts_m.get(bin) : 0);
			System.out.print("\t");
			System.out.println(
					bin < resultB.realChannelUsage.binCnt() ? -resultB.realChannelUsage.draughts_m.get(bin) : 0);
		}
	}

	public static void main(String[] args) {

		processPassages("./seas/passage_line_3_ship_crossings_20251122_203126.txt", Set.of("E", "NE", "SE"),
				Set.of("W", "NW", "SW"), Integer.MAX_VALUE);
//		processPassages("./seas/passage_line_6_ship_crossings_20251122_203126.txt", Set.of("E", "NE"), Set.of("W", "SW"),
//				Integer.MAX_VALUE);
//		processPassages("./seas/passage_line_7_ship_crossings_20251122_203126.txt", Set.of("E", "NE", "SE"),
//				Set.of("W", "NW", "SW"), Integer.MAX_VALUE);

//		processPassages(
//				Arrays.asList("./seas/passage_line_6_ship_crossings_20251122_203126.txt",
//						"./seas/passage_line_7_ship_crossings_20251122_203126.txt"),
//				Arrays.asList(Set.of("E", "NE"), Set.of("E", "NE", "SE")),
//				Arrays.asList(Set.of("W", "SW"), Set.of("W", "NW", "SW")), outboundLeft, 10);
	}
}
