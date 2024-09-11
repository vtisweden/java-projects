/**
 * se.vti.samgods
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
package se.vti.samgods.transportation.fleet;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.utils.ParseNumberUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsFleetReader {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(SamgodsFleetReader.class);

	private static final String VEH_NR = "VEH_NR";

	private static final String VEH_LABEL = "LABEL";

	private static final String VEH_DESCRIPTION = "DESCRIPTIO";

	private static final String COST_1_KM = "KM_COST";

	private static final String COST_1_H = "HOURS_COST";

	private static final String CAPACITY_TON = "CAPACITY";

	private static final String ON_FERRY_COST_1_KM = "ONFER_KM_C";

	private static final String ON_FERRY_COST_1_H = "ONFER_H_C";

	private static final String SPEED_KM_H = "SPEED";

	private static final String COMMODITY_ID = "ID_COM";

	private static final String NO_CONTAINER_LOAD_COST_1_TON = "NC_LCO";

	private static final String NO_CONTAINER_LOAD_TIME_H = "NC_LTI";

	private static final String NO_CONTAINER_TRANSFER_COST_1_TON = "NC_LCOT";

	private static final String NO_CONTAINER_TRANSFER_TIME_H = "NC_LTIT";

	private static final String CONTAINER_LOAD_COST_1_TON = "CONT_LCO";

	private static final String CONTAINER_LOAD_TIME_H = "CONT_LTI";

	private static final String CONTAINER_TRANSFER_COST_1_TON = "CONT_LCO_T";

	private static final String CONTAINER_TRANSFER_TIME_H = "CONT_LTI_T";

	private static final String SUFFIX_INDICATING_CONTAINER = "_CONTAINER";

	// -------------------- MEMBERS --------------------

	private final SamgodsVehicles fleet;

	// -------------------- CONSTRUCTION --------------------

	public SamgodsFleetReader(SamgodsVehicles fleet) {
		this.fleet = fleet;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void load_v12(String vehicleTypeFile, String costFile, SamgodsConstants.TransportMode transportMode)
			throws IOException {
		final Map<String, SamgodsVehicleAttributes.Builder> vehicleNr2builder = new LinkedHashMap<>();

		for (CSVRecord record : CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(vehicleTypeFile))) {
			for (boolean container : new boolean[] { false, true }) {
				final SamgodsVehicleAttributes.Builder builder = new SamgodsVehicleAttributes.Builder(
						record.get(VEH_LABEL) + (container ? SUFFIX_INDICATING_CONTAINER : ""));
				vehicleNr2builder.put(record.get(VEH_NR) + (container ? SUFFIX_INDICATING_CONTAINER : ""), builder);
				builder.setDescription(record.get(VEH_DESCRIPTION)).setMode(transportMode)
						.setCost_1_km(ParseNumberUtils.parseDoubleOrNull(record.get(COST_1_KM)))
						.setCost_1_h(ParseNumberUtils.parseDoubleOrNull(record.get(COST_1_H)))
						.setCapacity_ton(ParseNumberUtils.parseDoubleOrNull(record.get(CAPACITY_TON)))
						.setOnFerryCost_1_km(ParseNumberUtils.parseDoubleOrNull(record.get(ON_FERRY_COST_1_KM)))
						.setOnFerryCost_1_h(ParseNumberUtils.parseDoubleOrNull(record.get(ON_FERRY_COST_1_H)))
						.setSpeed_km_h(ParseNumberUtils.parseDoubleOrNull(record.get(SPEED_KM_H)));
			}
		}

		for (CSVRecord record : CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(costFile))) {
			final SamgodsConstants.Commodity commodity = SamgodsConstants.Commodity
					.values()[ParseNumberUtils.parseIntOrNull(record.get(COMMODITY_ID)) - 1];
			vehicleNr2builder.get(record.get(VEH_NR)).setContainer(false)
					.setLoadCost_1_ton(commodity,
							ParseNumberUtils.parseDoubleOrNull(record.get(NO_CONTAINER_LOAD_COST_1_TON)))
					.setLoadTime_h(commodity, ParseNumberUtils.parseDoubleOrNull(record.get(NO_CONTAINER_LOAD_TIME_H)))
					.setTransferCost_1_ton(commodity,
							ParseNumberUtils.parseDoubleOrNull(record.get(NO_CONTAINER_TRANSFER_COST_1_TON)))
					.setTransferTime_h(commodity,
							ParseNumberUtils.parseDoubleOrNull(record.get(NO_CONTAINER_TRANSFER_TIME_H)));
			vehicleNr2builder.get(record.get(VEH_NR) + SUFFIX_INDICATING_CONTAINER).setContainer(true)
					.setLoadCost_1_ton(commodity,
							ParseNumberUtils.parseDoubleOrNull(record.get(CONTAINER_LOAD_COST_1_TON)))
					.setLoadTime_h(commodity, ParseNumberUtils.parseDoubleOrNull(record.get(CONTAINER_LOAD_TIME_H)))
					.setTransferCost_1_ton(commodity,
							ParseNumberUtils.parseDoubleOrNull(record.get(CONTAINER_TRANSFER_COST_1_TON)))
					.setTransferTime_h(commodity,
							ParseNumberUtils.parseDoubleOrNull(record.get(CONTAINER_TRANSFER_TIME_H)));
		}

		for (SamgodsVehicleAttributes.Builder builder : vehicleNr2builder.values()) {
			try {
				VehicleType vehicleType = builder.buildVehicleType();

				SamgodsVehicleAttributes attrs = (SamgodsVehicleAttributes) vehicleType.getAttributes()
						.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME);
				if (attrs.onFerryCost_1_h == null) {
					log.warn("Vehicle type " + vehicleType.getId() + " has null onFerryCost_1_h.");
				}
				if (attrs.onFerryCost_1_km == null) {
					log.warn("Vehicle type " + vehicleType.getId() + " has null onFerryCost_1_km.");
				}
				if (attrs.speed_km_h == null) {
					log.warn("Vehicle type " + vehicleType.getId() + " has null speed_km_h.");
				}

				Set<SamgodsConstants.Commodity> commodities = new LinkedHashSet<>(
						Arrays.asList(SamgodsConstants.Commodity.values()));
				commodities.removeAll(attrs.loadCost_1_ton.keySet());
				if (commodities.size() > 0) {
					log.warn("Vehicle type " + vehicleType.getId() + " has no loadCost_1_ton for commodities: "
							+ commodities);
				}

				commodities = new LinkedHashSet<>(Arrays.asList(SamgodsConstants.Commodity.values()));
				commodities.removeAll(attrs.loadTime_h.keySet());
				if (commodities.size() > 0) {
					log.warn("Vehicle type " + vehicleType.getId() + " has no loadTime_h for commodities: "
							+ commodities);
				}

				commodities = new LinkedHashSet<>(Arrays.asList(SamgodsConstants.Commodity.values()));
				commodities.removeAll(attrs.transferCost_1_ton.keySet());
				if (commodities.size() > 0) {
					log.warn("Vehicle type " + vehicleType.getId() + " has no transferCost_1_ton for commodities: "
							+ commodities);
				}

				commodities = new LinkedHashSet<>(Arrays.asList(SamgodsConstants.Commodity.values()));
				commodities.removeAll(attrs.transferTime_h.keySet());
				if (commodities.size() > 0) {
					log.warn("Vehicle type " + vehicleType.getId() + " has no transferTime_h for commodities: "
							+ commodities);
				}

				this.fleet.getVehicles().addVehicleType(vehicleType);

			} catch (InsufficientDataException e) {
				e.log(this.getClass(), "Failed to build vehicle type " + builder.id + ".");
			}
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws Exception {
		SamgodsVehicles fleet = new SamgodsVehicles();
		SamgodsFleetReader reader = new SamgodsFleetReader(fleet);
		reader.load_v12("./input_2024/vehicleparameters_air.csv", "./input_2024/transferparameters_air.csv",
				SamgodsConstants.TransportMode.Air);
		reader.load_v12("./input_2024/vehicleparameters_rail.csv", "./input_2024/transferparameters_rail.csv",
				SamgodsConstants.TransportMode.Rail);
		reader.load_v12("./input_2024/vehicleparameters_road.csv", "./input_2024/transferparameters_road.csv",
				SamgodsConstants.TransportMode.Road);
		reader.load_v12("./input_2024/vehicleparameters_sea.csv", "./input_2024/transferparameters_sea.csv",
				SamgodsConstants.TransportMode.Sea);

		FleetStatsTable table = new FleetStatsTable(reader.fleet);

		System.out.println(table.createVehicleTypeTable(SamgodsConstants.TransportMode.Air));
		System.out.println(table.createVehicleTransferCostTable(SamgodsConstants.TransportMode.Air));

		System.out.println(table.createVehicleTypeTable(SamgodsConstants.TransportMode.Rail));
		System.out.println(table.createVehicleTransferCostTable(SamgodsConstants.TransportMode.Rail));

		System.out.println(table.createVehicleTypeTable(SamgodsConstants.TransportMode.Road));
		System.out.println(table.createVehicleTransferCostTable(SamgodsConstants.TransportMode.Road));

		System.out.println(table.createVehicleTypeTable(SamgodsConstants.TransportMode.Sea));
		System.out.println(table.createVehicleTransferCostTable(SamgodsConstants.TransportMode.Sea));

	}

}
