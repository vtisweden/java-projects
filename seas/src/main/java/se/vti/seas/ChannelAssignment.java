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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import se.vti.utils.misc.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class ChannelAssignment {

	private final Map<VesselClass, List<Double>> class2laneAssignments_veh_h;

	private final List<Double> relativeLaneUsages;

	// CONSTRUCTION

	public ChannelAssignment() {
		this.class2laneAssignments_veh_h = new LinkedHashMap<>();
		this.relativeLaneUsages = new ArrayList<>();
	}

	void setRate_veh_h(VesselClass consideredClass, int lane, double newRate_veh_h) {

		List<Double> laneAssignments_veh_h = this.getOrCreateLaneAssignments_veh_h(consideredClass, lane + 1);
		double oldRate_veh_h = laneAssignments_veh_h.set(lane, newRate_veh_h);

		double relativeLaneUsage = this.getOrCreateRelativeLaneUsages(lane + 1).get(lane);
		relativeLaneUsage += (newRate_veh_h - oldRate_veh_h) * consideredClass.computeLength_km()
				/ consideredClass.speed_km_h;
		relativeLaneUsage = Math.max(0, Math.min(1.0, relativeLaneUsage)); // TODO Include check for more than
																			// epsilon-deviations.
		this.getOrCreateRelativeLaneUsages(lane + 1).set(lane, relativeLaneUsage);
	}

	// INTERNAL GETTERS

	private List<Double> getListWithMinimumSize(List<Double> list, int minSize) {
		while (list.size() < minSize) {
			list.add(0.0);
		}
		return list;
	}

	private List<Double> getOrCreateLaneAssignments_veh_h(VesselClass vesselClass, int numberOfLanes) {
		List<Double> laneAssignments_veh_h = this.class2laneAssignments_veh_h.computeIfAbsent(vesselClass,
				c -> new ArrayList<>());
		return this.getListWithMinimumSize(laneAssignments_veh_h, numberOfLanes);
	}

	private List<Double> getOrCreateRelativeLaneUsages(int numberOfLanes) {
		return this.getListWithMinimumSize(this.relativeLaneUsages, numberOfLanes);
	}

	// EXTERNAL GETTERS

	int getNumberOfLanes() {
		return this.relativeLaneUsages.size();
	}

	double getRate_veh_h(VesselClass vesselClass, int lane) {
		return this.getOrCreateLaneAssignments_veh_h(vesselClass, lane + 1).get(lane);
	}

	double getRelativeLaneUsage(int lane) {
		return this.getOrCreateRelativeLaneUsages(lane + 1).get(lane);
	}

	// COMPUTATIONS

	DiscretizedChannelGeometry computeModeledUsage(double lateralBinSize_m, double overtakingWidthPercentile) {
		return this.computeChannelGeometry(overtakingWidthPercentile).createDiscretizedGeometry(lateralBinSize_m);
	}
	
	OneSidedChannelGeometry computeChannelGeometry(double overtakingWidthPercentile) {

		List<Double> widths_m = new ArrayList<>();
		List<Integer> draughts_m = new ArrayList<>();

		double totalWidth_m = 0.0;
		
		for (int lane = 0; lane < this.getNumberOfLanes(); lane++) {

			double requiredWidth_m = 0.0;
			int maxDraught_m = 0;
			List<Tuple<Double, Double>> widthsAndRates = new ArrayList<>();
			double rateSum_veh_h = 0.0;
			for (var vesselClassAndAssignment : this.class2laneAssignments_veh_h.entrySet()) {
				var vesselClass = vesselClassAndAssignment.getKey();
				List<Double> rates_veh_h = vesselClassAndAssignment.getValue();
				if (rates_veh_h.size() > lane) {
					double rate_veh_h = rates_veh_h.get(lane);
					if (rate_veh_h >= 1e-8) {
						requiredWidth_m = Math.max(requiredWidth_m, vesselClass.computeEffectiveWidth_m());
						maxDraught_m = Math.max(maxDraught_m, vesselClass.draught_m);
						double width_m = vesselClass.computeEffectiveWidth_m();
						widthsAndRates.add(new Tuple<>(width_m, rate_veh_h));
						rateSum_veh_h += rate_veh_h;
					}
				}
			}

			if (widthsAndRates.size() > 0 && rateSum_veh_h >= 1e-8) {

				Collections.sort(widthsAndRates, (wr1, wr2) -> Double.compare(wr1.getA(), wr2.getA()));
				double percentileSum = 0.0;
				Double width_m = null;
				for (var widthAndRate : widthsAndRates) {
					percentileSum += 100.0 * (widthAndRate.getB() / rateSum_veh_h);
					if (percentileSum >= overtakingWidthPercentile) {
						width_m = widthAndRate.getA();
						break;
					}
				}
				if (width_m == null) {
					// hedge against numerical imprecision when accumulating probabilities
					width_m = widthsAndRates.stream().mapToDouble(t -> t.getA()).max().getAsDouble();
				}
				
				if (totalWidth_m + width_m < requiredWidth_m) {
					width_m = requiredWidth_m - totalWidth_m;
				}
				
				widths_m.add(width_m);
				draughts_m.add(maxDraught_m);
				totalWidth_m += width_m;
			}
		}

		return new OneSidedChannelGeometry(widths_m, draughts_m);
	}

	double computeProbaOfNoLaneUsed() {
		double noLaneUSedProba = 1.0;
		for (int lane = 0; lane < this.relativeLaneUsages.size(); lane++) {
			noLaneUSedProba *= (1.0 - this.relativeLaneUsages.get(lane));
		}
		return noLaneUSedProba;
	}

	List<Double> computeProbaOfBeingRightmostLane() {
		List<Double> result = new ArrayList<>(this.relativeLaneUsages.size());
		for (int lane = 0; lane < this.relativeLaneUsages.size(); lane++) {
			double higherNotUsedProba = 1.0;
			for (int lane2 = lane + 1; lane2 < this.relativeLaneUsages.size(); lane2++) {
				higherNotUsedProba *= (1.0 - this.relativeLaneUsages.get(lane2));
			}
			result.add(this.relativeLaneUsages.get(lane) * higherNotUsedProba);
		}
		return result;
	}

	int computeNumberOfLanes(double probaThreshold, List<Double> probaOfBeingRightMost) {
		int lanes = 0;
		double probaOfNoLaneUsed = this.computeProbaOfNoLaneUsed();
		double probaSum = 0.0;
		while (probaSum < probaThreshold) {
			probaSum += probaOfBeingRightMost.get(lanes) / (1.0 - probaOfNoLaneUsed);
			lanes++;
		}
		return lanes;
	}

	int computeNumberOfLanes(double probaThreshold) {
		return this.computeNumberOfLanes(probaThreshold, this.computeProbaOfBeingRightmostLane());
	}

	@Override
	public String toString() {
		int numberOfLanes = this.getNumberOfLanes();
		StringBuffer result = new StringBuffer("\t" + IntStream.range(0, numberOfLanes).boxed()
				.map(l -> "Lane" + (l + 1) + "[veh/h]").collect(Collectors.joining("\t")) + "\n");
		for (var classAndAssignment : this.class2laneAssignments_veh_h.entrySet()) {
			VesselClass vesselClass = classAndAssignment.getKey();
			List<Double> assignments_veh_h = classAndAssignment.getValue();
			result.append(vesselClass.name);
			for (int lane = 0; lane < numberOfLanes; lane++) {
				result.append("\t");
				result.append((lane < assignments_veh_h.size()) ? assignments_veh_h.get(lane) : 0.0);
			}
			result.append("\n");
		}
		return result.toString();
	}
}
