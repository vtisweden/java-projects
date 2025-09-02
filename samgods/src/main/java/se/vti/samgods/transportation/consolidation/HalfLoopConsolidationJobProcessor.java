/**
 * se.vti.samgods.transportation.consolidation
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
package se.vti.samgods.transportation.consolidation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.calibration.ascs.ASCDataProvider;
import se.vti.samgods.common.NetworkAndFleetData;
import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.samgods.logistics.choice.ChainAndShipmentSize;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.costs.TransportCostCalculator;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;
import se.vti.utils.misc.Units;
import se.vti.utils.misc.math.LogitChoiceModel;

/**
 * 
 * @author GunnarF
 *
 */
public class HalfLoopConsolidationJobProcessor implements Runnable {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = LogManager.getLogger(HalfLoopConsolidationJobProcessor.class);

	private final BlockingQueue<ConsolidationJob> jobQueue;
	private final ConcurrentHashMap<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment;

	private final TransportCostCalculator transportCostCalculator;
	private final NetworkAndFleetData networkAndFleetData;
	private final Map<Commodity, Double> commodity2scale;
	private final ASCDataProvider ascDataProvider;

	private int couldNotComputeFleetAssignmentWarnings = 0;

	// -------------------- CONSTRUCTION --------------------

	public HalfLoopConsolidationJobProcessor(BlockingQueue<ConsolidationJob> jobQueue,
			ConcurrentHashMap<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment,
			NetworkAndFleetData networkAndFleetData, final Map<Commodity, Double> commodity2scale,
			ASCDataProvider ascDataProvider) {
		this.jobQueue = jobQueue;
		this.consolidationUnit2fleetAssignment = consolidationUnit2fleetAssignment;
		this.transportCostCalculator = new TransportCostCalculator();
		this.networkAndFleetData = networkAndFleetData;
		this.commodity2scale = commodity2scale;
		this.ascDataProvider = ascDataProvider;
	}

	// -------------------- IMPLEMENTATION OF Runnable --------------------

	@Override
	public void run() {
		try {
			while (true) {
				ConsolidationJob job = this.jobQueue.take();
				if (job == ConsolidationJob.TERMINATE) {
					break;
				}
				FleetAssignment fleetAssignment = this.computeFleetAssignment(job);
				if (fleetAssignment != null) {
					this.consolidationUnit2fleetAssignment.put(job.consolidationUnit, fleetAssignment);
				} else {
					if (this.couldNotComputeFleetAssignmentWarnings < 10) {
						log.warn("Could not compute fleet assignment: " + job);
					}
					if (++this.couldNotComputeFleetAssignmentWarnings == 10) {
						log.warn("Suppressing further warnings of this type.");
					}
				}
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------- RESULT CONTAINER CLASS --------------------

	public static class FleetAssignment {

		public final double annualDemand_ton;

		public final VehicleType vehicleType;
		public final double loopLength_km;
		public final double minLoopDuration_h;
		public final double domesticLoopLength_km;

		public final double averageVehiclePassages_1_day;
		public final double payload_ton;
		public final double unitCost_1_tonKm;
		public final double expectedSnapshotVehicleCnt;

		private FleetAssignment(double annualDemand_ton, VehicleType vehicleType, double vehicleCapacity_ton,
				DetailedTransportCost halfLoopCost, double serviceIntervalActiveProba, ConsolidationJob job,
				NetworkAndFleetData networkAndFleetData) {

			this.annualDemand_ton = annualDemand_ton;

			this.vehicleType = vehicleType;
			this.loopLength_km = 2.0 * halfLoopCost.length_km;
			this.minLoopDuration_h = 2.0 * halfLoopCost.duration_h;

			this.domesticLoopLength_km = 2.0 * Units.KM_PER_M
					* job.consolidationUnit.getRoute(vehicleType).stream()
							.filter(lid -> networkAndFleetData.getDomesticLinkIds().contains(lid))
							.mapToDouble(lid -> networkAndFleetData.getLinks().get(lid).getLength()).sum();

			final double serviceInterval_h = Units.H_PER_D * job.serviceInterval_days;
			final double demandPerActiveServiceInterval_ton = (1.0 / serviceIntervalActiveProba)
					* (job.serviceInterval_days / 365.0) * annualDemand_ton;

			// n is fleet size. f is circulation frequency, per service interval
			final double nf = Math.max(1.0, demandPerActiveServiceInterval_ton / vehicleCapacity_ton);

			this.averageVehiclePassages_1_day = serviceIntervalActiveProba * nf / job.serviceInterval_days;

			final double fMin = 1.0; // desirable: complete at least one loop
			final double fMax = serviceInterval_h / this.minLoopDuration_h; // hard physical constraint

			final int n;
			final double f;
			if (fMin <= fMax) {
				// Possible to complete at least fMin=1 loops and up to fMax loops.
				n = Math.max(1, (int) Math.ceil(nf / fMax)); // implies n >= nf / fMax <=> nf / n <= fMax
				f = Math.max(fMin, nf / n); // by the above, this yield fMin <= f <= fMax
			} else {
				// Impossible to complete fMin=1 loops. Choose max. feasible f = fMax;
				n = Math.max(1, (int) Math.ceil(nf / fMax));
				f = fMax;
			}
			assert (f == Math.min(fMax, Math.max(fMin, nf / n)));

			this.payload_ton = demandPerActiveServiceInterval_ton / n / f;
			this.unitCost_1_tonKm = halfLoopCost.monetaryCost / this.payload_ton / (0.5 * this.loopLength_km);

			final double supplyPerActiveServiceInterval_ton = n * f * vehicleCapacity_ton;
			assert (demandPerActiveServiceInterval_ton <= 1e-8 + supplyPerActiveServiceInterval_ton);

			this.expectedSnapshotVehicleCnt = serviceIntervalActiveProba * n;
		}

		@Override
		public String toString() {
			return "Loop of dimensions " + loopLength_km + "km, " + minLoopDuration_h + "h uses vehicle type "
					+ this.vehicleType.getId() + ". Random snapshot sees " + this.expectedSnapshotVehicleCnt
					+ " vehicles carrying an average payload of " + this.payload_ton + "ton at a unit cost of "
					+ this.unitCost_1_tonKm + " 1/tonKm";
		}
	}

	// -------------------- INTERNALS --------------------

	private FleetAssignment computeFleetAssignment(double annualDemand_ton, VehicleType vehicleType,
			ConsolidationJob job, double serviceIntervalActiveProba) {
		SamgodsVehicleAttributes vehicleAttrs = this.networkAndFleetData.getVehicleType2attributes().get(vehicleType);
		FleetAssignment result = new FleetAssignment(annualDemand_ton, vehicleType, vehicleAttrs.capacity_ton,
				this.transportCostCalculator.computeInVehicleCost(vehicleType, 0.5 * vehicleAttrs.capacity_ton,
						job.consolidationUnit, networkAndFleetData),
				serviceIntervalActiveProba, job, this.networkAndFleetData);
		boolean done = false;
		final int maxIts = 100;
		int its = 0;
		while (!done) {
			final FleetAssignment newResult = new FleetAssignment(annualDemand_ton, vehicleType,
					vehicleAttrs.capacity_ton,
					this.transportCostCalculator.computeInVehicleCost(vehicleType, result.payload_ton,
							job.consolidationUnit, networkAndFleetData),
					serviceIntervalActiveProba, job, this.networkAndFleetData);
			final double dev = Math.abs(newResult.unitCost_1_tonKm - result.unitCost_1_tonKm) / result.unitCost_1_tonKm;
			if (++its == maxIts) {
				log.warn("Too many iterations, terminating with relative unit cost deviation " + dev + ".");
				done = true;
			} else {
				done = (dev < 1e-8);
			}
			result = newResult;
		}
		return result;
	}

	private FleetAssignment computeFleetAssignment(ConsolidationJob job) {

		final double annualDemand_ton = job.choices.stream().mapToDouble(c -> c.annualShipment.getTotalAmount_ton())
				.sum();
		final double serviceIntervalActiveProba;
		{
			double probaSingleServiceIntervalInactive = 1.0;
			for (ChainAndShipmentSize choice : job.choices) {
				final double meanShipmentsPerYear = Math.max(1.0,
						choice.annualShipment.getTotalAmount_ton() / choice.sizeClass.upperValue_ton);
				final double meanShipmentsPerServiceInterval = meanShipmentsPerYear * job.serviceInterval_days / 365.0;
				probaSingleServiceIntervalInactive *= Math.exp(-meanShipmentsPerServiceInterval);
			}
			serviceIntervalActiveProba = 1.0 - probaSingleServiceIntervalInactive;
		}

		final double scale = this.commodity2scale.get(job.consolidationUnit.commodity);
		final var assignment2utility = new LinkedHashMap<FleetAssignment, Double>(
				job.consolidationUnit.vehicleType2route.size());
		for (var vehicleTypes : job.consolidationUnit.vehicleType2route.keySet()) {
			for (var vehicleType : vehicleTypes) {
				var assignment = this.computeFleetAssignment(annualDemand_ton, vehicleType, job,
						serviceIntervalActiveProba);
				var utility = (-1.0) * scale * assignment.unitCost_1_tonKm * 0.5 * assignment.loopLength_km
						* annualDemand_ton
						+ this.ascDataProvider.getConcurrentVehicleType2ASC().getOrDefault(vehicleType, 0.0);
				assignment2utility.put(assignment, utility);
			}
		}
		return new LogitChoiceModel().choose(assignment2utility.keySet().stream().toList(),
				a -> assignment2utility.get(a));
	}
}
