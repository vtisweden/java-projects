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
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultPlansRemover;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author GunnarF
 */
@Singleton
public class SimpleAssignmentCooling implements IterationStartsListener, StartupListener {

	private static final Logger log = LogManager.getLogger(SimpleAssignmentCooling.class);

	private final AssignmentCoolingConfigGroup coolingConfig;

	private Set<String> allSubpopulations = null;

	@Inject
	public SimpleAssignmentCooling(Config config) {
		if (!config.replanning().getPlanSelectorForRemoval().equals(DefaultPlansRemover.WorstPlanSelector.toString())) {
			throw new RuntimeException(
					"Must use " + DefaultPlansRemover.WorstPlanSelector.toString() + " plan selector for removal.");
		}
		this.coolingConfig = ConfigUtils.addOrGetModule(config, AssignmentCoolingConfigGroup.class);
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

				int numberOfExpBetaPlanSelectorStrategies = 0;
				int numberOfInnovationStrategies = 0;
				for (var strategy : strategyManager.getStrategies(subpopulation)) {
					String strategyName = strategy.toString();
					if (ExpBetaPlanSelector.class.getSimpleName().toString().equals(strategyName)) {
						numberOfExpBetaPlanSelectorStrategies++;
					} else {
						numberOfInnovationStrategies++;
					}
				}
				assert (numberOfExpBetaPlanSelectorStrategies > 0);
				assert (numberOfInnovationStrategies > 0);

				final double innovationRate;
				if (iteration < this.coolingConfig.getBurnInIterations()) {
					innovationRate = 1.0;
				} else {
					innovationRate = Math.min(1.0 / numberOfInnovationStrategies,
							Math.pow(iteration - this.coolingConfig.getBurnInIterations() + 1,
									(-1.0) * this.coolingConfig.getInnovationIterationExponent()));
//							Math.sqrt(iteration - this.coolingConfig.getBurnInIterations() + 1));
				}
				final double selectionRate = (1.0 - innovationRate) / numberOfExpBetaPlanSelectorStrategies;
				log.info("Subpoulation: " + subpopulation);
				log.info("  Setting innovation rate rate to " + innovationRate);
				log.info("  Setting selection rate to " + selectionRate);

				for (var strategy : strategyManager.getStrategies(subpopulation)) {
					String strategyName = strategy.toString();
					double weight;
					if (ExpBetaPlanSelector.class.getSimpleName().toString().equals(strategyName)) {
						weight = selectionRate;
					} else {
						weight = innovationRate;
					}
					strategyManager.changeWeightOfStrategy(strategy, subpopulation, weight);
				}
			}
		}
	}
}
