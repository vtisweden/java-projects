/**
 * se.vti.certain.spatial
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.certain.analysis.modifiers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import se.vti.certain.datastructures.IncidentType;
import se.vti.certain.spatial.Zone;

/**
 * @author GunnarF
 */
public class ZoneModifier {

	private final Map<String, Zone> id2Zone;

	public ZoneModifier(Map<String, Zone> id2Zone) {
		this.id2Zone = id2Zone;
	}

	public void updateIncidentType2Intensity_1_yr(String zoneId,
			Map<IncidentType, Double> scenarioIncidentType2Intensity_1_yr) {
	}

	public void createZoneFile(String fileName) throws StreamWriteException, DatabindException, IOException {
		List<Zone> scenarioZones = new ArrayList<>(this.id2Zone.size());
		for (Zone zone : this.id2Zone.values()) {
			Zone scenarioZone = new Zone(zone.getId());
			var scenarioIncidentType2Intensity_1_yr = new LinkedHashMap<>(zone.getIncidentType2Intensity_1_yr());
			this.updateIncidentType2Intensity_1_yr(zone.getId(), scenarioIncidentType2Intensity_1_yr);
			scenarioZone.getIncidentType2Intensity_1_yr().clear();
			scenarioIncidentType2Intensity_1_yr.entrySet()
					.forEach(e -> scenarioZone.setIncidentIntensity(e.getKey(), e.getValue()));
			scenarioZones.add(scenarioZone);
		}
		new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(fileName), scenarioZones);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws IOException {
		String path = "./minimal/input/";
		File incidentTypeFile = new File(path + "incidentTypes.json");
		File zonesFile = new File(path + "zones.json");

		System.out.println("STARTED ...");

		var id2IncidentType = IncidentType.readFromJsonFile(incidentTypeFile);
		var id2Zone = Zone.readFromJsonFile(zonesFile, id2IncidentType);

		new ZoneModifier(id2Zone) {
			@Override
			public void updateIncidentType2Intensity_1_yr(String zoneId,
					Map<IncidentType, Double> scenarioIncidentType2Intensity_1_yr) {
			}
		}.createZoneFile("original.json");

		new ZoneModifier(id2Zone) {
			@Override
			public void updateIncidentType2Intensity_1_yr(String zoneId,
					Map<IncidentType, Double> scenarioIncidentType2Intensity_1_yr) {
				scenarioIncidentType2Intensity_1_yr.entrySet().stream().forEach(e -> e.setValue(2.0 * e.getValue()));
			}
		}.createZoneFile("doubled.json");

		new ZoneModifier(id2Zone) {
			@Override
			public void updateIncidentType2Intensity_1_yr(String zoneId,
					Map<IncidentType, Double> scenarioIncidentType2Intensity_1_yr) {
				scenarioIncidentType2Intensity_1_yr.entrySet().stream().forEach(e -> e.setValue(4.0 * e.getValue()));
			}
		}.createZoneFile("quadrupled.json");

		System.out.println("... DONE");

	}

}
