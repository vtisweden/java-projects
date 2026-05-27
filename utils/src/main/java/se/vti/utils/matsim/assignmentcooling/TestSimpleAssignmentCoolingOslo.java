/**
 * se.vti.atap
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
package se.vti.utils.matsim.assignmentcooling;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Berlin scenario used in the article: <i>"A simulation heuristic for traveler-
 * and vehicle-discrete dynamic traffic assignment"</i>.
 *
 * <b>Scenario data:</b>
 * https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/zz_archive/2014-08-01_car_1pct
 * 
 * @author Gunnar Flötteröd
 */

public class TestSimpleAssignmentCoolingOslo {

	public static void main(String[] args) {

//		boolean useCooling = true;
		String configFileName = "./oslo_config_cooling_example.xml";
//		String configFileName = "./src/test/resources/se/vti/utils/matsim/assignmentcooling/oslo_config_cooling_example.xml";

		Config config = ConfigUtils.loadConfig(configFileName);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		boolean useCooling = config.getModules().containsKey(AssignmentCoolingConfigGroup.GROUP_NAME);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		if (useCooling) {
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					this.addControlerListenerBinding().to(SimpleAssignmentCooling.class);
				}
			});
		}

		controler.run();
	}

}
