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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import se.vti.utils.misc.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import se.vti.utils.misc.tabularfileparser.TabularFileParser;

/**
 * @author GunnarF
 */
public class PassageReader extends AbstractTabularFileHandlerWithHeaderLine {

	List<Passage> passages = new ArrayList<>();

	public PassageReader(String fileName) {
		TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\t");
		try {
			parser.parse(fileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// READING / PARSING

	private String stringOrNull(String val) {
		if ("NA".equals(val)) {
			return null;
		} else {
			return val;
		}
	}

	private Double doubleOrNull(String val) {
		if ("NA".equals(val)) {
			return null;
		} else {
			return Double.parseDouble(val);
		}
	}

	@Override
	public void startCurrentDataRow() {
		Double secondsSinceFirstCrossing = this.doubleOrNull(this.getStringValue("seconds_since_first_crossing"));
		String mmsi = this.stringOrNull(this.getStringValue("mmsi"));
		String imo = this.stringOrNull(this.getStringValue("imo"));
		String cargoType = this.stringOrNull(this.getStringValue("cargo_type")); // may be null
		Double draught_m = this.doubleOrNull(this.getStringValue("draught_avg")); // may be null
		Double width_m = this.doubleOrNull(this.getStringValue("breadthextreme")); // may be null
		Double length_m = this.doubleOrNull(this.getStringValue("lengthoverallloa")); // may be null
		Double speed_km_h = this.doubleOrNull(this.getStringValue("sog_kmh_avg"));
		String crossingCompassDirection = this.stringOrNull(this.getStringValue("crossing_compass_direction"));
		String shipType = this.stringOrNull(this.getStringValue("ship_type")); // may be null
		Double lateralCoord_m = this.doubleOrNull(this.getStringValue("distance_from_passage_line_start_to_crossing_m"));
		this.passages.add(new Passage(secondsSinceFirstCrossing, mmsi, imo, cargoType, draught_m, width_m, length_m,
				speed_km_h, crossingCompassDirection, shipType, lateralCoord_m));
	}
}
