package se.vti.utils.matsim.assignmentcooling;

import java.util.LinkedHashSet;
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
public class SimpleAssignmentCooling2 implements IterationStartsListener, StartupListener {

	private static final Logger log = LogManager.getLogger(SimpleAssignmentCooling2.class);

	private final AssignmentCoolingConfigGroup coolingConfig;
	private final int maxNumberOfPlans;

	private Set<String> allSubpopulations = null;

	@Inject
	public SimpleAssignmentCooling2(Config config) {
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
		if (iteration >= 0) {

			var strategyManager = event.getServices().getStrategyManager();
			for (String subpopulation : this.allSubpopulations) {

				int numberOfSelectRandomStrategies = 0;
				int numberOfKeepLastSelectedStrategies = 0;
				int numberOfInnovationStrategies = 0;
				for (var strategy : strategyManager.getStrategies(subpopulation)) {
					String strategyName = strategy.toString();
					log.info("Processing strategy " + strategyName);
					if ("RandomPlanSelector".equals(strategyName)) {
						numberOfSelectRandomStrategies++;
					} else if ("KeepSelected".equals(strategyName)) {
						numberOfKeepLastSelectedStrategies++;
					} else {
						numberOfInnovationStrategies++;
					}
				}
				if (numberOfSelectRandomStrategies != 1) {
					throw new RuntimeException(
							"Subpopulation " + subpopulation + ": Number of SelectRandom strategies must be one.");
				}
				if (numberOfKeepLastSelectedStrategies != 1) {
					throw new RuntimeException(
							"Subpopulation " + subpopulation + ": Number of KeepLastSelected strategies must be one.");
				}
				if (numberOfInnovationStrategies == 0) {
					throw new RuntimeException(
							"Subpopulation " + subpopulation + ": There must be at leats one innovation strategy.");
				}

				final double totalInnovationRate;
				if (iteration < this.coolingConfig.getBurnInIterations()) {
					totalInnovationRate = 1.0 / (1.0 + this.maxNumberOfPlans);
				} else {
					totalInnovationRate = 1.0 / (1.0 + this.maxNumberOfPlans)
							* Math.pow(iteration - this.coolingConfig.getBurnInIterations() + 1,
									(-1.0) * this.coolingConfig.getInnovationIterationExponent());
				}
				final double individualInnovationRate = totalInnovationRate / numberOfInnovationStrategies;
				final double selectRandomRate = totalInnovationRate * this.maxNumberOfPlans;
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
}
