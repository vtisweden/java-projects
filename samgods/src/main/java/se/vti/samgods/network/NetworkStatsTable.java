/**
 * se.vti.samgods.network
 * 
 * Copyright (C) 2024, 2025 by Gunnar Flötteröd (VTI, LiU).
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import de.vandermeer.asciitable.AsciiTable;
import se.vti.samgods.common.SamgodsConstants.TransportMode;
import se.vti.utils.misc.Units;

/**
 * Just for testing.
 * 
 * @author GunnarF
 *
 */
public class NetworkStatsTable {

	private NetworkStatsTable() {
	}

	private static String divideOrEmpty(Double num, Integer den) {
		if (num == null || den == null || den == 0) {
			return "";
		} else {
			return "" + (num / den);
		}
	}
	
	public static String create(Network network) {
		final Map<TransportMode, Integer> mode2cnt = new LinkedHashMap<>();
		final Map<TransportMode, Integer> mode2speed1cnt = new LinkedHashMap<>();
		final Map<TransportMode, Integer> mode2speed2cnt = new LinkedHashMap<>();

		final Map<TransportMode, Double> mode2speedSum = new LinkedHashMap<>();
		final Map<TransportMode, Double> mode2speed1Sum = new LinkedHashMap<>();
		final Map<TransportMode, Double> mode2speed2Sum = new LinkedHashMap<>();
		final Map<TransportMode, Double> mode2lengthSum = new LinkedHashMap<>();
		final Map<TransportMode, Double> mode2lanesSum = new LinkedHashMap<>();

		final Map<TransportMode, Integer> mode2capCnt = new LinkedHashMap<>();
		final Map<TransportMode, Double> mode2capSum = new LinkedHashMap<>();

		for (Link link : network.getLinks().values()) {
			final SamgodsLinkAttributes attr = (SamgodsLinkAttributes) link.getAttributes()
					.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME);
			final TransportMode mode = attr.samgodsMode;
			mode2cnt.compute(mode, (m, c) -> c == null ? 1 : c + 1);
			mode2lengthSum.compute(mode, (m, s) -> s == null ? link.getLength() : s + link.getLength());
			mode2lanesSum.compute(mode, (m, s) -> s == null ? link.getNumberOfLanes() : s + link.getNumberOfLanes());
			mode2speedSum.compute(mode, (m, s) -> s == null ? link.getFreespeed() : s + link.getFreespeed());
			if (attr.speed1_km_h != null) {
				mode2speed1cnt.compute(mode, (m, c) -> c == null ? 1 : c + 1);
				mode2speed1Sum.compute(mode, (m, s) -> s == null ? attr.speed1_km_h : s + attr.speed1_km_h);
			}
			if (attr.speed2_km_h != null) {
				mode2speed2cnt.compute(mode, (m, c) -> c == null ? 1 : c + 1);
				mode2speed2Sum.compute(mode, (m, s) -> s == null ? attr.speed2_km_h : s + attr.speed2_km_h);
			}
			if (Double.isFinite(link.getCapacity())) {
				mode2capCnt.compute(mode, (m, c) -> c == null ? 1 : c + 1);
				mode2capSum.compute(mode, (m, s) -> s == null ? link.getCapacity() : s + link.getCapacity());
			}
		}

		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Mode", "Links", "Avg. length [km]", "Avg. no. of lanes", "Avg. speed [km/h]",
				"Avg. speed1 [km/h] if >0", "# with speed1>0", "Avg. speed2 [km/h] if >0", "# with speed2>0",
				"Avg. capacity [veh/h] if <oo", "# with cap<00");
		table.addRule();
		for (Map.Entry<TransportMode, Integer> e : mode2cnt.entrySet()) {
			final TransportMode mode = e.getKey();
			final int cnt = e.getValue();
			table.addRow(mode, cnt, divideOrEmpty(Units.KM_PER_M * mode2lengthSum.get(e.getKey()), cnt),
					divideOrEmpty(mode2lanesSum.get(mode), cnt),
					divideOrEmpty(Units.KM_H_PER_M_S * mode2speedSum.get(mode), cnt),
					divideOrEmpty(mode2speed1Sum.get(mode), mode2speed1cnt.get(mode)),
					mode2speed1cnt.getOrDefault(mode, 0),
					divideOrEmpty(mode2speed2Sum.get(mode), mode2speed2cnt.get(mode)),
					mode2speed2cnt.getOrDefault(mode, 0), divideOrEmpty(mode2capSum.get(mode), mode2capCnt.get(mode)),
					mode2capCnt.getOrDefault(mode, 0));

			table.addRule();
		}
		return table.render();
	}
}
