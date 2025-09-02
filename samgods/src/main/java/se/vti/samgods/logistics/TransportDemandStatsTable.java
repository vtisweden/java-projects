/**
 * se.vti.samgods.logistics
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
package se.vti.samgods.logistics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.vandermeer.asciitable.AsciiTable;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.utils.MiscUtils;
import se.vti.utils.misc.math.MathHelpers;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportDemandStatsTable {

	private TransportDemandStatsTable() {
	}

	public static String createChainStatsTable(int maxRowCnt, Commodity commodity, TransportDemandAndChains demand) {
		// OD flow -> chain assignment may not be available, hence just counting chains.
		Map<List<SamgodsConstants.TransportMode>, Integer> modeSeq2cnt = new LinkedHashMap<>();
		for (var chains : demand.getCommodity2od2transportChains().get(commodity).values()) {
			for (var chain : chains) {
				final var modes = chain.getEpisodes().stream().map(e -> e.getMode()).toList();
				modeSeq2cnt.compute(modes, (m, c) -> c == null ? 1 : c + 1);
			}
		}
		var sortedEntries = MiscUtils.getSortedEntryListLargestFirst(modeSeq2cnt);

		final long total = modeSeq2cnt.values().stream().mapToLong(c -> c).sum();
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Rank", "episodes", "Count", "Share [%]");
		table.addRule();
		table.addRow("", "Total", total, 100);
		table.addRule();
		for (int i = 0; i < Math.min(maxRowCnt, sortedEntries.size()); i++) {
			Map.Entry<List<SamgodsConstants.TransportMode>, Integer> entry = sortedEntries.get(i);
			table.addRow(i + 1, entry.getKey(), entry.getValue(),
					MathHelpers.round(100.0 * entry.getValue().doubleValue() / total, 2));
			table.addRule();
		}

		final StringBuffer result = new StringBuffer();
		result.append(
				"\nCOMMODITY: " + commodity + ", counting occurrences of logistic chains (NOT freight volumes)\n");
		result.append(table.render());
		return result.toString();
	}
}
