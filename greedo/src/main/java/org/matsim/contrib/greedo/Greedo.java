/**
 * org.matsim.contrib.emulation
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
package org.matsim.contrib.greedo;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.emulation.EmulationConfigGroup;
import org.matsim.contrib.emulation.EmulationModule;
import org.matsim.contrib.emulation.EmulationParameters;
import org.matsim.contrib.emulation.emulators.ActivityEmulator;
import org.matsim.contrib.emulation.emulators.LegEmulator;
import org.matsim.contrib.emulation.handlers.EmulationHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Greedo {

	// -------------------- CONSTANTS --------------------

	public static final String nullSubpopulationString = "nullSubpopulation";

	private static final Logger logger = Logger.getLogger(Greedo.class);

	public static final String DEFAULT = "default";

	// -------------------- MEMBERS --------------------

	private Config config = null;

//	private final Map<String, Class<? extends ActivityEmulator>> actType2emulator = new LinkedHashMap<>();
//
//	private final Map<String, Class<? extends LegEmulator>> mode2emulator = new LinkedHashMap<>();
//
//	private final Map<String, Class<? extends LegDecomposer>> mode2decomposer = new LinkedHashMap<>();

	private final EmulationParameters emulationParameters;

	// -------------------- CONSTRUCTION --------------------

	public Greedo() {
//		// (atomic) activity emulation
//		this.setActivityEmulator(DEFAULT, BasicActivityEmulator.class);
//		// (atomic) leg emulation
//		this.setEmulator(TransportMode.car, CarLegEmulator.class);
//		this.setEmulator(DEFAULT, OnlyDepartureArrivalLegEmulator.class);
//		// leg decomposition
//		this.setDecomposer(DEFAULT, BasicLegDecomposer.class);
		this.emulationParameters = new EmulationParameters();
	}

	// -------------------- WIRE GREEDO INTO MATSim --------------------

	public void setActivityEmulator(String type, Class<? extends ActivityEmulator> clazz) {
//		logger.info("Emulator for activity type " + type + " is of type " + clazz.getSimpleName());
//		this.actType2emulator.put(type, clazz);
		this.emulationParameters.setActivityEmulator(type, clazz);
	}

	public void setEmulator(String mode, Class<? extends LegEmulator> clazz) {
//		logger.info("Emulator for mode " + mode + " is of type " + clazz.getSimpleName());
//		this.mode2emulator.put(mode, clazz);
		this.emulationParameters.setEmulator(mode, clazz);
	}

	public void addHandler(Class<? extends EmulationHandler> clazz) {
//		logger.info("Decomposer for mode " + mode + " is of type " + clazz.getSimpleName());
//		this.mode2decomposer.put(mode, clazz);
		this.emulationParameters.addHandler(clazz);
	}

	public void meet(final Config config) {

		if (this.config != null) {
			throw new RuntimeException("Have already met a config.");
		}
		if (config.controler().getFirstIteration() != 0) {
			Logger.getLogger(this.getClass()).warn("The simulation does not start at iteration zero.");
		}
		this.config = config;

		if (!config.getModules().containsKey(EmulationConfigGroup.GROUP_NAME)) {
			logger.warn("Config module " + EmulationConfigGroup.GROUP_NAME
					+ " is missing, falling back to default values.");
		}
		final EmulationConfigGroup ierConfig = ConfigUtils.addOrGetModule(config, EmulationConfigGroup.class);

		if (!config.getModules().containsKey(GreedoConfigGroup.GROUP_NAME)) {
			logger.warn(
					"Config module " + GreedoConfigGroup.GROUP_NAME + " is missing, falling back to default values.");
		}
		final GreedoConfigGroup greedoConfig = ConfigUtils.addOrGetModule(config, GreedoConfigGroup.class);

		boolean thereAreExpensiveStrategies = false;
		boolean thereAreCheapStrategies = false;
		final Set<String> allSubpops = new LinkedHashSet<>();
		final Map<String, Double> subpop2expensiveStrategyWeightSum = new LinkedHashMap<>();
		final Map<String, Double> subpop2cheapStrategyWeightSum = new LinkedHashMap<>();
		for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
			final String strategyName = strategySettings.getStrategyName();
			final String subpop;
			if (strategySettings.getSubpopulation() == null) {
				subpop = nullSubpopulationString;
			} else {
				subpop = strategySettings.getSubpopulation();
				logger.warn("Strategy reweighting not tested for other than null/default subpopulation.");
			}
			allSubpops.add(subpop);
			if (strategySettings.getWeight() > 0.0) {
				if (greedoConfig.getExpensiveStrategySet().contains(strategyName)) {
					subpop2expensiveStrategyWeightSum.put(subpop,
							strategySettings.getWeight() + subpop2expensiveStrategyWeightSum.getOrDefault(subpop, 0.0));
					thereAreExpensiveStrategies = true;
				} else if (greedoConfig.getCheapStrategySet().contains(strategyName)) {
					subpop2cheapStrategyWeightSum.put(subpop,
							strategySettings.getWeight() + subpop2cheapStrategyWeightSum.getOrDefault(subpop, 0.0));
					thereAreCheapStrategies = true;
				}
			}
		}

		if (thereAreCheapStrategies) {
			if (thereAreExpensiveStrategies) {
				ierConfig.setIterationsPerCycle(Math.max(ierConfig.getIterationsPerCycle(), 2));
				logger.info("There are cheap and expensive strategies. Number of emulated iterations per cycle is "
						+ ierConfig.getIterationsPerCycle() + ".");
			} else {
				logger.info("There are no expensive strategies. Keeping number of emulated iterations at "
						+ ierConfig.getIterationsPerCycle() + ".");
			}
		} else {
			if (thereAreExpensiveStrategies) {
				ierConfig.setIterationsPerCycle(1);
				logger.info("There are no cheap strategies- Setting number of emulated iterations to 1.");
			} else {
				throw new RuntimeException("There are no neither cheap nor expensive strategies.");
			}
		}

		for (String subpop : allSubpops) {
			logger.info("Adjusting strategies for subpopulation: " + subpop);

			final double expensiveStrategyWeightFactor;
			if (subpop2expensiveStrategyWeightSum.getOrDefault(subpop, 0.0) > 0.0) {
				expensiveStrategyWeightFactor = 1.0 / ierConfig.getIterationsPerCycle()
						/ subpop2expensiveStrategyWeightSum.getOrDefault(subpop, 0.0);
			} else {
				expensiveStrategyWeightFactor = 0.0;
			}

			final double cheapStrategyWeightFactor;
			if (subpop2cheapStrategyWeightSum.getOrDefault(subpop, 0.0) > 0.0) {
				cheapStrategyWeightFactor = (expensiveStrategyWeightFactor > 0.0
						? (1.0 - 1.0 / ierConfig.getIterationsPerCycle())
						: 1.0) / subpop2cheapStrategyWeightSum.getOrDefault(subpop, 0.0);
			} else {
				cheapStrategyWeightFactor = 0.0;
			}

			double probaSum = 0;

			for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
				if (subpop.equals(strategySettings.getSubpopulation() == null ? nullSubpopulationString
						: strategySettings.getSubpopulation())) {
					final String strategyName = strategySettings.getStrategyName();
					if (greedoConfig.getExpensiveStrategySet().contains(strategyName)) {
						strategySettings.setWeight(strategySettings.getWeight() * expensiveStrategyWeightFactor);
					} else if (greedoConfig.getCheapStrategySet().contains(strategyName)) {
						strategySettings.setWeight(strategySettings.getWeight() * cheapStrategyWeightFactor);
					} else {
						strategySettings.setWeight(0.0);
					}
					logger.info("* Setting weight of strategy " + strategyName + " to " + strategySettings.getWeight()
							+ ".");
					probaSum += strategySettings.getWeight();
				}
			}

			if (probaSum < 1.0 - 1e-8) { // This can happen if a sub-population has no cheap strategies.
				final StrategySettings keepSelected = new StrategySettings();
				keepSelected.setStrategyName(DefaultSelector.KeepLastSelected);
				keepSelected.setSubpopulation(subpop);
				keepSelected.setWeight(1.0 - probaSum);
				config.strategy().addStrategySettings(keepSelected);
				logger.info("* Padding with " + DefaultSelector.KeepLastSelected + " and weight="
						+ keepSelected.getWeight() + ".");
				probaSum += keepSelected.getWeight();
			}
		}

		config.strategy().setMaxAgentPlanMemorySize(1);
		config.strategy().setPlanSelectorForRemoval("WorstPlanSelector");
		logger.info("Approximating a best-response simulation through the following settings:");
		logger.info(" * maxAgentPlanMemorySize = 1");
		logger.info(" * planSelectorForRemoval = worstPlanSelector");

		config.strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);
		logger.info("Setting fractionOfIterationsToDisableInnovation to infinity.");
	}

	public void meet(final Controler controler) {
		if (this.config == null) {
			throw new RuntimeException("First meet the config.");
		}
		for (AbstractModule module : this.getModules()) {
			controler.addOverridingModule(module);
		}
		
		final int checkEmulatedAgentsCnt = ConfigUtils.addOrGetModule(this.config, GreedoConfigGroup.class).getCheckEmulatedAgentsCnt();
		if (checkEmulatedAgentsCnt > 0) {
			EventsChecker.generateObservedPersonIds(controler.getScenario().getPopulation(), checkEmulatedAgentsCnt, "observedPersons.txt");
			EventsChecker simulatedEventsChecker = new EventsChecker("observedPersons.txt", true);
			controler.addControlerListener(simulatedEventsChecker);
			controler.getEvents().addHandler(simulatedEventsChecker);
		}		
	}

	public AbstractModule[] getModules() {

		final AbstractModule greedoModule = new AbstractModule() {

			@Override
			public void install() {
				bind(PlansReplanning.class).to(GreedoReplanning.class);
			}
		};
		return new AbstractModule[] { greedoModule,
				// new EmulationModule(this.actType2emulator, this.mode2emulator,
				// this.mode2decomposer) };
				new EmulationModule(this.emulationParameters) };
//						this.emulationParameters.getActType2emulatorView(),
//						this.emulationParameters.getMode2emulatorView(),
//						this.emulationParameters.getMode2decomposerView()) };
		// this.actType2emulator, this.mode2emulator, this.mode2decomposer) };
	}
}
