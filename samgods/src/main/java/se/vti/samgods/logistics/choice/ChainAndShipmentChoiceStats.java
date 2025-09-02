/**
 * se.vti.samgods.logistics.choicemodel
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
package se.vti.samgods.logistics.choice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.rank.Median;

import de.vandermeer.asciitable.AsciiTable;
import se.vti.samgods.common.NetworkAndFleetData;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.utils.MiscUtils;

/**
 * JUST FOR TESTING
 * 
 * @author GunnarF
 *
 */
public class ChainAndShipmentChoiceStats {

	private final NetworkAndFleetData networkAndFleetData;

	private Map<SamgodsConstants.Commodity, Map<SamgodsConstants.ShipmentSize, Long>> commodity2size2cnt;

	private Map<SamgodsConstants.Commodity, List<Double>> commodity2lengths;

	public ChainAndShipmentChoiceStats(NetworkAndFleetData networkAndFleetData) {
		this.networkAndFleetData = networkAndFleetData;
		this.commodity2size2cnt = new LinkedHashMap<>(SamgodsConstants.commodityCnt());
		this.commodity2lengths = new LinkedHashMap<>(SamgodsConstants.commodityCnt());
		for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
			this.commodity2size2cnt.put(commodity,
					Arrays.stream(SamgodsConstants.ShipmentSize.values()).collect(Collectors.toMap(s -> s, s -> 0l)));
			this.commodity2lengths.put(commodity, new ArrayList<>());
		}
	}

	public void add(ChainAndShipmentSize choice) {
		this.commodity2size2cnt.get(choice.annualShipment.getCommodity()).compute(choice.sizeClass, (s, c) -> c + 1);
		this.commodity2lengths.get(choice.annualShipment.getCommodity())
				.add(choice.transportChain.getEpisodes().stream().flatMap(e -> e.getConsolidationUnits().stream())
						.mapToDouble(cu -> cu.computeLengthStats_km(this.networkAndFleetData).getMean()).sum());
	}

	public String createChoiceStatsTable() {
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Commodity", "Average length [km]", "Median length [km]", "Mean class size [ton]",
				"Size class counts");

		for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {

			final long totalCnt = this.commodity2size2cnt.get(commodity).values().stream().mapToLong(c -> c).sum();
			if (totalCnt > 0) {

				final double averageLength_km = this.commodity2lengths.get(commodity).stream().mapToDouble(l -> l)
						.average().getAsDouble();
				final double medianLength_km = new Median()
						.evaluate(this.commodity2lengths.get(commodity).stream().mapToDouble(l -> l).toArray());

				final List<Map.Entry<SamgodsConstants.ShipmentSize, Long>> sortedSizeEntries = MiscUtils
						.getSortedEntryListSmallestFirst(this.commodity2size2cnt.get(commodity));
				final double avgSize = sortedSizeEntries.stream()
						.mapToDouble(e -> e.getKey().getRepresentativeValue_ton() * e.getValue()).sum() / totalCnt;

				table.addRule();
				table.addRow(commodity, averageLength_km, medianLength_km, avgSize,
						sortedSizeEntries.stream().map(e -> e.getValue().toString()).collect(Collectors.joining(",")));
			}
		}
		table.addRule();
		return table.render();
	}
}
