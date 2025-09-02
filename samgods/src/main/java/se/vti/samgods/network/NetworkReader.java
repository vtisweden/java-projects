/**
 * se.vti.samgods
 * 
 * Copyright (C) 2023, 2024, 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.network;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsConstants.TransportMode;
import se.vti.utils.ParseNumberUtils;
import se.vti.utils.misc.Units;

/**
 *
 * @author GunnarF
 *
 */
public class NetworkReader {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = LogManager.getLogger(NetworkReader.class);

	private static final String NODE_COUNTER = "N";
	private static final String NODE_X = "X";
	private static final String NODE_Y = "Y";
	private static final String NODE_ID = "NORIG";
	private static final String NODE_ID_COUNTRY = "ID_COUNTRY";

	private static final String LINK_ID = "OBJECTID";
	private static final String LINK_FROM_NODE_COUNTER = "A";
	private static final String LINK_TO_NODE_COUNTER = "B";
	private static final String LINK_LENGTH_M = "SHAPE_Length";
	private static final String LINK_SPEED_1 = "SPEED_1";
	private static final String LINK_SPEED_2 = "SPEED_2";
	private static final String LINK_LANES = "NLANES";
	private static final String LINK_MODE = "GENERAL_MO";
	private static final String LINK_CAPACITY_TRAINS_DAY = "ORIGCAP";

	private static final String LINK_MODESTR = "MODESTR";

	// -------------------- MEMBERS --------------------

	private double minSpeed_km_h;

	private double minCapacity_veh_h;

	private final Map<TransportMode, Double> mode2fallbackSpeed_km_h = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public NetworkReader() {
		this.setMinSpeed_km_h(1.0);
		this.setMinCapacity_veh_h(1.0 / 24.0);
		this.setFallbackSpeed_km_h(TransportMode.Air, 800.0);
		this.setFallbackSpeed_km_h(TransportMode.Ferry, 5.0);
		this.setFallbackSpeed_km_h(TransportMode.Rail, 100.0);
		this.setFallbackSpeed_km_h(TransportMode.Road, 70.0);
		this.setFallbackSpeed_km_h(TransportMode.Sea, 15.0);
	}

	public NetworkReader setMinSpeed_km_h(double minSpeed_km_h) {
		log.info("Setting minimal link speed to " + minSpeed_km_h + " km/h, links below that are not loaded.");
		this.minSpeed_km_h = minSpeed_km_h;
		return this;
	}

	public NetworkReader setMinCapacity_veh_h(double minCapacity_veh_h) {
		log.info("Setting minimal link capacity to " + minCapacity_veh_h + " veh/h, links below that are not loaded.");
		this.minCapacity_veh_h = minCapacity_veh_h;
		return this;
	}

	public NetworkReader setFallbackSpeed_km_h(TransportMode mode, double speed_km_h) {
		log.info("Setting fallback speed for mode " + mode + " to " + speed_km_h + " km/h");
		this.mode2fallbackSpeed_km_h.put(mode, speed_km_h);
		return this;
	}

	// -------------------- IMPLEMENTATION --------------------

	/**
	 * Link lengths may be zero. Link lengths are adjusted to be at least as large
	 * as the Euclidean distance of their start- and endnode.
	 * 
	 * Link speeds are bounded from below by minimum value and set to fallback
	 * values if not defined in file.
	 * 
	 * Link capacities are bounded from below by minimum value and set to infinity
	 * if not defined in file.
	 */
	public Network load(final String nodesFile, final String linksFile) throws IOException {

		final Network network = NetworkUtils.createNetwork();
		network.setCapacityPeriod(3600.0);

		// Need this because NODE_ID and NODE_COUNTER are different.
		final Map<Long, Node> nodeCounter2node = new LinkedHashMap<>();

		final Set<Node> domesticNodes = new LinkedHashSet<>();

		for (CSVRecord record : CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(nodesFile))) {
			final Id<Node> id = Id.createNodeId(Long.parseLong(record.get(NODE_ID)));
			assert (!network.getNodes().containsKey(id));
			final double x = Double.parseDouble(record.get(NODE_X));
			final double y = Double.parseDouble(record.get(NODE_Y));
			final Node node = NetworkUtils.createAndAddNode(network, id, new Coord(x, y));
			final boolean isDomestic = (1 == Integer.parseInt(record.get(NODE_ID_COUNTRY))); // TODO magic number
			if (isDomestic) {
				domesticNodes.add(node);
			}
			node.getAttributes().putAttribute(SamgodsNodeAttributes.ATTRIBUTE_NAME,
					new SamgodsNodeAttributes(isDomestic));
			nodeCounter2node.put(Long.parseLong(record.get(NODE_COUNTER)), node);
		}

		for (CSVRecord record : CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(linksFile))) {
			final Id<Link> id = Id.createLinkId(Long.parseLong(record.get(LINK_ID)));
			assert (!network.getLinks().containsKey(id));
			final Node fromNode = nodeCounter2node.get(Long.parseLong(record.get(LINK_FROM_NODE_COUNTER)));
			final Node toNode = nodeCounter2node.get(Long.parseLong(record.get(LINK_TO_NODE_COUNTER)));
			assert (fromNode != null);
			assert (toNode != null);
			final double nodeDist_m = NetworkUtils.getEuclideanDistance(fromNode.getCoord(), toNode.getCoord());
			final boolean isDomestic = domesticNodes.contains(fromNode) && domesticNodes.contains(toNode);

			final double lanes = Double.parseDouble(record.get(LINK_LANES));
			final SamgodsConstants.TransportMode samgodsMode = SamgodsConstants.TransportMode
					.valueOf(record.get(LINK_MODE));
			double length_m = Double.parseDouble(record.get(LINK_LENGTH_M));
			if (length_m < nodeDist_m) {
				// do not log mm-deviations
				if (length_m < nodeDist_m - 1e-3) {
					log.warn("Increasing link " + id + "'s length from " + length_m + "m to node distance " + nodeDist_m
							+ "m.");
				}
				length_m = nodeDist_m;
			}

			final String[] networkModes = record.get(LINK_MODESTR).split("");

			final Double speed1_km_h = ParseNumberUtils.parseDoubleOrNull(record.get(LINK_SPEED_1));
			final Double speed2_km_h = ParseNumberUtils.parseDoubleOrNull(record.get(LINK_SPEED_2));
			double speed_km_h;
			if (speed1_km_h != null) {
				if (speed2_km_h != null) {
					speed_km_h = Math.max(speed1_km_h, speed2_km_h);
				} else {
					speed_km_h = speed1_km_h;
				}
			} else {
				if (speed2_km_h != null) {
					speed_km_h = speed2_km_h;
				} else {
					speed_km_h = this.mode2fallbackSpeed_km_h.get(samgodsMode);
				}
			}
			assert (Double.isFinite(speed_km_h));

			if (speed_km_h < this.minSpeed_km_h) {
				LogManager.getLogger(NetworkReader.class)
						.warn("Skipping link " + id + " because of too low speed: " + speed_km_h + " km/h.");
			} else {
				final double capacity_veh_h = ParseNumberUtils
						.parseDoubleOrDefault(record.get(LINK_CAPACITY_TRAINS_DAY), Double.POSITIVE_INFINITY) / 24.0;
				if (capacity_veh_h < this.minCapacity_veh_h) {
					LogManager.getLogger(NetworkReader.class).warn(
							"Skipping link " + id + " because of too low capacity: " + capacity_veh_h + " veh/h.");
				} else {
					final Link link = NetworkUtils.createAndAddLink(network, id, fromNode, toNode, length_m,
							Units.M_S_PER_KM_H * speed_km_h, capacity_veh_h, lanes, null, null);
					final SamgodsLinkAttributes linkAttributes = new SamgodsLinkAttributes(samgodsMode, speed1_km_h,
							speed2_km_h, isDomestic, networkModes);
					link.setAllowedModes(TransportModes.computeMatsimModesMapFerryToCarriedModes(linkAttributes));
					link.getAttributes().putAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME, linkAttributes);
				}
			}
		}

		log.info("Loaded " + network.getNodes().size() + " nodes.");
		log.info("Loaded " + network.getLinks().size() + " links.");

		return network;
	}
}
