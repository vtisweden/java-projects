/**
 * se.vti.samgods.calibration
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
package se.vti.samgods.calibration.ascs;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.samgods.common.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor.FleetAssignment;
import se.vti.utils.misc.iterationlogging.LogEntry;
import se.vti.utils.misc.iterationlogging.LogWriter;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportWorkMonitor {

	private final LogWriter<TransportWorkMonitor> vehicleTypeGTonKmWriter;
	private final LogWriter<TransportWorkMonitor> modeGTonKmWriter;
	private final LogWriter<TransportWorkMonitor> railCommodityGTonKmWriter;

	private Map<VehicleType, Double> vehicleType2lastRealizedDomesticGTonKm = null;
	private Map<TransportMode, Double> mode2lastRealizedDomesticGTonKm = null;
	private Map<Commodity, Double> commodity2lastRealizedRailDomesticGTonKm = null;

	public TransportWorkMonitor(Vehicles vehicles) {

		this.vehicleTypeGTonKmWriter = new LogWriter<>("./results/vehicleTypeGTonKm.txt", false);
		for (VehicleType vehicleType : vehicles.getVehicleTypes().values()) {
			this.vehicleTypeGTonKmWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return vehicleType.getId().toString();
				}

				@Override
				public String value(TransportWorkMonitor transportWork) {
					return LogEntry.toString(
							transportWork.vehicleType2lastRealizedDomesticGTonKm.getOrDefault(vehicleType, 0.0));
				}
			});
		}

		this.modeGTonKmWriter = new LogWriter<>("./results/modeGTonKm.txt", false);
		for (TransportMode mode : TransportMode.values()) {
			this.modeGTonKmWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return mode.toString();
				}

				@Override
				public String value(TransportWorkMonitor transportWork) {
					return LogEntry.toString(transportWork.mode2lastRealizedDomesticGTonKm.getOrDefault(mode, 0.0));
				}
			});
		}

		this.railCommodityGTonKmWriter = new LogWriter<>("./results/railCommodityGTonKm.txt", false);
		for (Commodity commodity : Commodity.values()) {
			this.railCommodityGTonKmWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return commodity.toString() + "_realized";
				}

				@Override
				public String value(TransportWorkMonitor fleetCalibrator) {
					return LogEntry.toString(
							fleetCalibrator.commodity2lastRealizedRailDomesticGTonKm.getOrDefault(commodity, 0.0));
				}
			});
		}
	}

	public Map<VehicleType, Double> getVehicleType2lastRealizedDomesticGTonKm() {
		return vehicleType2lastRealizedDomesticGTonKm;
	}

	public Map<TransportMode, Double> getMode2lastRealizedDomesticGTonKm() {
		return mode2lastRealizedDomesticGTonKm;
	}

	public Map<Commodity, Double> getCommodity2lastRealizedRailDomesticGTonKm() {
		return commodity2lastRealizedRailDomesticGTonKm;
	}

	public void update(
			Map<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment) {
		this.vehicleType2lastRealizedDomesticGTonKm = new LinkedHashMap<>();
		this.mode2lastRealizedDomesticGTonKm = new LinkedHashMap<>();
		this.commodity2lastRealizedRailDomesticGTonKm = new LinkedHashMap<>();
		for (Map.Entry<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> entry : consolidationUnit2fleetAssignment
				.entrySet()) {
			final ConsolidationUnit consolidationUnit = entry.getKey();
			final FleetAssignment fleetAssignment = entry.getValue();
			final double realized_GTonKm = 1e-9 * fleetAssignment.annualDemand_ton * 0.5
					* fleetAssignment.domesticLoopLength_km;
			this.vehicleType2lastRealizedDomesticGTonKm.compute(fleetAssignment.vehicleType,
					(v, s) -> s == null ? realized_GTonKm : s + realized_GTonKm);
			this.mode2lastRealizedDomesticGTonKm.compute(consolidationUnit.samgodsMode,
					(m, s) -> s == null ? realized_GTonKm : s + realized_GTonKm);
			if (TransportMode.Rail.equals(consolidationUnit.samgodsMode)) {
				this.commodity2lastRealizedRailDomesticGTonKm.compute(consolidationUnit.commodity,
						(c, s) -> s == null ? realized_GTonKm : s + realized_GTonKm);
			}
		}
		this.vehicleTypeGTonKmWriter.writeToFile(this);
		this.modeGTonKmWriter.writeToFile(this);
		this.railCommodityGTonKmWriter.writeToFile(this);
	}
}
