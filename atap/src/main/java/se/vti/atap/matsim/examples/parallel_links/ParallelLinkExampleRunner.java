/**
 * se.vti.atap.examples.parallel_links
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
package se.vti.atap.matsim.examples.parallel_links;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.zip.CRC32;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.emulation.EmulationConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

import se.vti.atap.matsim.ATAP;
import se.vti.atap.matsim.ATAPConfigGroup;
import se.vti.atap.matsim.ATAPConfigGroup.ReplannerIdentifierType;

/**
 * 
 * @author GunnarF
 *
 */
public class ParallelLinkExampleRunner {

	public ParallelLinkExampleRunner() {
	}

	private void writeConfig(Config config, String scenarioFolder) {
		File folder = new File(scenarioFolder);
		if (folder.exists()) {
			try {
				FileUtils.cleanDirectory(folder);
			} catch (IOException e) {
				throw new RuntimeException();
			}
		} else {
			folder.mkdirs();
		}
		config.network().setInputFile("network.xml");
		config.plans().setInputFile("population.xml");
		ConfigUtils.writeMinimalConfig(config, Paths.get(scenarioFolder, "config.xml").toString());
	}

	private void writeScenario(Scenario scenario, String scenarioFolder) {
		NetworkUtils.writeNetwork(scenario.getNetwork(),
				Paths.get(scenarioFolder, scenario.getConfig().network().getInputFile()).toString());
		PopulationUtils.writePopulation(scenario.getPopulation(),
				Paths.get(scenarioFolder, scenario.getConfig().plans().getInputFile()).toString());
	}

	public long computeChecksumForTinyTestCase() {
		double sizeFactor = 1.0;
		double inflowDuration_s = 60.0;

		ParallelLinkScenarioFactory factory = new ParallelLinkScenarioFactory(inflowDuration_s, sizeFactor);
		factory.setBottleneck(0, 500.0);
		factory.setBottleneck(1, 500.0);
		factory.setOD(2000, 0, 1);

		Config config = factory.buildConfig();
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(3);
		config.travelTimeCalculator().setTraveltimeBinSize(60);
		config.qsim().setStuckTime(Double.POSITIVE_INFINITY);
		config.addModule(new EmulationConfigGroup());
		
		ATAPConfigGroup atapConfig = new ATAPConfigGroup();
		atapConfig.setReplannerIdentifier(ReplannerIdentifierType.ATAP_EXACT_DISTANCE);
		atapConfig.setReplanningRateIterationExponent(-0.5);
		atapConfig.setMaxMemory(4);
		atapConfig.setKernelHalftime_s(60);
		atapConfig.setShuffleBeforeReplannerSelection(true);
		atapConfig.setUseQuadraticDistance(true);
		config.addModule(atapConfig);

		Scenario scenario = factory.build(config);

		var atap = new ATAP();
		atap.configure(scenario.getConfig());
		var controler = new Controler(scenario);
		var flowListener = new CumulativeFlowListener();
		controler.addControlerListener(flowListener);
		controler.getEvents().addHandler(flowListener);
		atap.configure(controler);

		long[] checkSum = new long[1];
		controler.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent event) {

				CRC32 crc = new CRC32();

				event.getServices().getScenario().getPopulation().getPersons().values().stream()
						.sorted(Comparator.comparing(p -> p.getId().toString())).map(p -> p.getSelectedPlan())
						.forEach(plan -> {
							plan.getPlanElements().stream().filter(pe -> pe instanceof Leg).map(pe -> (Leg) pe)
									.forEach(leg -> {
										StringBuilder sb = new StringBuilder();
										sb.append(leg.getDepartureTime()).append(",");
										sb.append(leg.getTravelTime()).append(",");
										sb.append(leg.getRoute().getRouteDescription());
										byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
										crc.update(bytes);
									});
						});

				checkSum[0] = crc.getValue();
			}
		});

		controler.run();

		return checkSum[0];
	}

	public Scenario createSmallExample(String scenarioFolder, ATAPConfigGroup atapConfig) {
		double sizeFactor = 3.0;
		double inflowDuration_s = 900.0;

		ParallelLinkScenarioFactory factory = new ParallelLinkScenarioFactory(inflowDuration_s, sizeFactor);
		factory.setBottleneck(0, 500.0);
		factory.setBottleneck(1, 500.0);
		factory.setOD(2000, 0, 1);

		Config config = factory.buildConfig();
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(100);
		config.travelTimeCalculator().setTraveltimeBinSize(60);
		config.qsim().setStuckTime(Double.POSITIVE_INFINITY);
		config.addModule(new EmulationConfigGroup());
		config.addModule(atapConfig);
		if (scenarioFolder != null) {
			this.writeConfig(config, scenarioFolder);
		}

		Scenario scenario = factory.build(config);
		if (scenarioFolder != null) {
			this.writeScenario(scenario, scenarioFolder);
		}

		return scenario;
	}

	public Scenario createSmallExampleWithUniform(String scenarioFolder) {
		ATAPConfigGroup atapConfig = new ATAPConfigGroup();
		atapConfig.setReplannerIdentifier(ReplannerIdentifierType.UNIFORM);
		atapConfig.setReplanningRateIterationExponent(-1.0);
		return this.createSmallExample(scenarioFolder, atapConfig);
	}

	public Scenario createSmallExampleWithSorting(String scenarioFolder) {
		ATAPConfigGroup atapConfig = new ATAPConfigGroup();
		atapConfig.setReplannerIdentifier(ReplannerIdentifierType.SORTING);
		atapConfig.setReplanningRateIterationExponent(-0.5);
		atapConfig.setMaxMemory(4);
		atapConfig.setKernelHalftime_s(300);
		atapConfig.setShuffleBeforeReplannerSelection(true);
		return this.createSmallExample(scenarioFolder, atapConfig);
	}

//	public Scenario createTinyExampleWithProposed() {
//		ATAPConfigGroup atapConfig = new ATAPConfigGroup();
//		atapConfig.setReplannerIdentifier(ReplannerIdentifierType.UPPERBOUND_ATOMIC);
//		atapConfig.setReplanningRateIterationExponent(-0.5);
//		atapConfig.setMaxMemory(4);
//		atapConfig.setKernelHalftime_s(60);
//		atapConfig.setShuffleBeforeReplannerSelection(true);
//		atapConfig.setUseQuadraticDistance(true);
//		return this.createTinyExample(atapConfig);
//	}

	public Scenario createSmallExampleWithProposed(String scenarioFolder) {
		ATAPConfigGroup atapConfig = new ATAPConfigGroup();
		atapConfig.setReplannerIdentifier(ReplannerIdentifierType.ATAP_EXACT_DISTANCE);
		atapConfig.setReplanningRateIterationExponent(-0.5);
		atapConfig.setMaxMemory(4);
		atapConfig.setKernelHalftime_s(60);
		atapConfig.setShuffleBeforeReplannerSelection(true);
		atapConfig.setUseQuadraticDistance(true);
		return this.createSmallExample(scenarioFolder, atapConfig);
	}

	public void runScenario(Scenario scenario) {
		var atap = new ATAP();
		atap.configure(scenario.getConfig());
		var controler = new Controler(scenario);
		var flowListener = new CumulativeFlowListener();
		controler.addControlerListener(flowListener);
		controler.getEvents().addHandler(flowListener);
		atap.configure(controler);
		controler.run();
	}

	public static void main(String[] args) {
		var example = new ParallelLinkExampleRunner();
		System.out.println("CheckSum = " + example.computeChecksumForTinyTestCase());		
		// 314257185 ... make this a unit test
		
//		Scenario scenario = example.createSmallExampleWithProposed("./small");
//		example.runScenario(scenario);
	}

}
