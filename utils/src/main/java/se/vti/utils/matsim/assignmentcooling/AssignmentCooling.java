package se.vti.utils.matsim.assignmentcooling;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultPlansRemover;
import org.matsim.core.replanning.strategies.KeepLastSelected;
import org.matsim.core.replanning.strategies.SelectRandom;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author GunnarF
 */
@Singleton
public class AssignmentCooling implements IterationStartsListener, StartupListener {

	private static final Logger log = LogManager.getLogger(AssignmentCooling.class);

	private final AssignmentCoolingConfigGroup coolingConfig;
	private final int maxNumberOfPlans;

	private Set<String> allSubpopulations = null;

	@Inject
	public AssignmentCooling(Config config) {
		if (!config.replanning().getPlanSelectorForRemoval().equals(DefaultPlansRemover.WorstPlanSelector.toString())) {
			throw new RuntimeException(
					"Must use " + DefaultPlansRemover.WorstPlanSelector.toString() + " plan selector for removal.");
		}
		this.coolingConfig = ConfigUtils.addOrGetModule(config, AssignmentCoolingConfigGroup.class);
		this.maxNumberOfPlans = config.replanning().getMaxAgentPlanMemorySize();
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.allSubpopulations = new LinkedHashSet<>();
		for (Person person : event.getServices().getScenario().getPopulation().getPersons().values()) {
			this.allSubpopulations.add(PopulationUtils.getSubpopulation(person));
		}
		log.info("Identified subpopulations: " + this.allSubpopulations);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {

		int iteration = event.getIteration() - event.getServices().getConfig().controller().getFirstIteration();
		var strategyManager = event.getServices().getStrategyManager();

		for (String subpopulation : this.allSubpopulations) {

			int numberRandomPlanSelector = 0;
			int numberOfKeepSelected = 0;
			List<String> innovationStrategies = new ArrayList<>();
			for (var strategy : strategyManager.getStrategies(subpopulation)) {
				String strategyName = strategy.toString();
				log.info("Processing strategy " + strategyName);
				if ("RandomPlanSelector".equals(strategyName)) {
					numberRandomPlanSelector++;
				} else if ("KeepSelected".equals(strategyName)) {
					numberOfKeepSelected++;
				} else {
					innovationStrategies.add(strategyName);
				}
			}
			if (numberRandomPlanSelector != 1) {
				throw new RuntimeException(
						"Subpopulation " + subpopulation + ": Number of SelectRandom strategies must be one.");
			}
			if (numberOfKeepSelected != 1) {
				throw new RuntimeException(
						"Subpopulation " + subpopulation + ": Number of KeepLastSelected strategies must be one.");
			}
			if (innovationStrategies.size() == 0) {
				throw new RuntimeException(
						"Subpopulation " + subpopulation + ": There must be at leats one innovation strategy.");
			} else {
				log.info("Treating the following strategies as DISTINCT INNOVATION STRATEGIES (check!): "
						+ innovationStrategies);
			}

			double totalInnovationRate = 1.0 / (1.0 + this.maxNumberOfPlans);
			double selectRandomRate = 1.0 - totalInnovationRate;

			int postBurnInIterations = iteration - this.coolingConfig.getBurnInIterations();
			if (postBurnInIterations >= 0) {
				totalInnovationRate *= Math.pow(postBurnInIterations + 1,
						(-1.0) * this.coolingConfig.getInnovationIterationExponent());
				selectRandomRate = Math.min(selectRandomRate,
						Math.pow(postBurnInIterations + 1,
								(-1.0) * this.coolingConfig.getSelectionIterationExponent()));
			}
			
			final double individualInnovationRate = totalInnovationRate / innovationStrategies.size();
			final double keepLastSelectedRate = Math.max(0.0, 1.0 - totalInnovationRate - selectRandomRate);

			log.info("Subpoulation: " + subpopulation);
			log.info("  Setting total innovation rate rate to " + totalInnovationRate);
			log.info("  Setting random selection rate to " + selectRandomRate);
			log.info("  Setting keep-last-selected rate to " + keepLastSelectedRate);

			for (var strategy : strategyManager.getStrategies(subpopulation)) {
				String strategyName = strategy.toString();
				final double weight;
				if (SelectRandom.class.getSimpleName().toString().equals(strategyName)) {
					weight = selectRandomRate;
				} else if (KeepLastSelected.class.getSimpleName().toString().equals(strategyName)) {
					weight = keepLastSelectedRate;
				} else {
					weight = individualInnovationRate;
				}
				strategyManager.changeWeightOfStrategy(strategy, subpopulation, weight);
			}
		}
	}
}
