/**
 * se.vti.tramodby
 * 
 * Copyright (C) 2023 Gunnar Flötteröd (VTI, LiU), Rasmus Ringdahl (LiU). 
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
package se.vti.tramodby.runner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.emulation.EmulationConfigGroup;
import org.matsim.contrib.roadpricing.RoadPricing;
import org.matsim.contrib.roadpricing.RoadPricingConfigGroup;
import org.matsim.contrib.roadpricing.RoadPricingEmulationHandler;
import org.matsim.contrib.roadpricing.RoadPricingTollCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;

import se.vti.atap.matsim.ATAP;
import se.vti.atap.matsim.ATAPConfigGroup;
import se.vti.tramodby.module.TramodByConfigGroup;
import se.vti.tramodby.od.InterZonalMatrices;
import se.vti.tramodby.od.InterZonalMatrices.Matrix;
import se.vti.tramodby.od.ODMatrixUtils;
import se.vti.tramodby.od.SkimMatrixCalculator;
import se.vti.tramodby.od.ZonalSystem;
import se.vti.tramodby.od.ZonalSystem.Zone;
import se.vti.tramodby.od.ZonalSystemUtils;

public class TramodSim {
	private static final Logger log = LogManager.getLogger(TramodSim.class);
	public static void main(String[] args) throws IOException {
		
		// Extracting run parameters.
		CommandLine cli = parseArgs(args);
		
		Path configFilePath = Paths.get(cli.getOptionValue("work-directory"),
										cli.getOptionValue("config-name", "config.xml"));
		/** Configuration and OD loading */
		// Loading configuration file and adding custom modules.
		Config config = ConfigUtils.loadConfig(configFilePath.toString(), 
											   new TramodByConfigGroup(),
											   new EmulationConfigGroup(),
											   new ATAPConfigGroup(),
											   new RoadPricingConfigGroup()
											   );
		// Initialize logging to controller log file.
		OutputDirectoryLogging.initLogging(new OutputDirectoryHierarchy(config));
		
		// Set the OverwriteFileSetting to overwrite in order to make 
		// the controller to not delete the log prior to creation.
		ControllerConfigGroup controlerConfig = ConfigUtils.addOrGetModule(config, ControllerConfigGroup.class);
		controlerConfig.setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		log.info("Starting Tramod By MATSim simulation.");
		
		TramodByConfigGroup tramodByConfig = ConfigUtils.addOrGetModule(config,
																		TramodByConfigGroup.class);
		config.qsim().setFlowCapFactor(tramodByConfig.getSamplingRate());
		config.qsim().setStorageCapFactor(tramodByConfig.getSamplingRate());

		ATAP atap = new ATAP();
		atap.configure(config);

		atap.addHandler(RoadPricingEmulationHandler.class);
		
		// Loading the scenario and cleaning the network from inconsistencies.
		Scenario scenario = ScenarioUtils.loadScenario(config);
		new NetworkCleaner().run(scenario.getNetwork());
		
		// Loading zonal system 
		ZonalSystem zonalSystem = ZonalSystemUtils.createZonalSystemFromFile(tramodByConfig, 
																			 scenario.getNetwork());
		log.info("number of zones: " + zonalSystem.zoneCnt());
		log.info("number of links: " + zonalSystem.linkCnt());
		log.info("consistent: " + zonalSystem.consistent());

		// Loading OD for each time slice.
		InterZonalMatrices ods = new InterZonalMatrices(tramodByConfig);
		log.info("Loading OD matrices.");
		for( int timebin = 0 ; timebin < tramodByConfig.getBinCount() ; timebin ++) {
			log.info("  " + (1 + timebin) + " of " + tramodByConfig.getBinCount());
			ODMatrixUtils.loadTimeSlice(ods, timebin, tramodByConfig.getOdFileName(timebin), tramodByConfig.getSamplingRate());
		}

		// Creating a link sampler to get a random link within a zone.
		final Random rnd = new Random();
		ZonalSystemUtils.LinkFromZoneSampler linkSampler = ZonalSystemUtils.createLinkFromZoneSampler(zonalSystem,
				scenario.getNetwork(), rnd);
		
		/** Population generation */ 
		long personCnt = 0;
		double expectedTotalFlow = 0.0;
		PopulationFactory popFact = scenario.getPopulation().getFactory();
		log.info("Generating population.");
		for (Id<Zone> fromZoneId : zonalSystem.getAllZones().keySet()) {
			for (Id<Zone> toZoneId : zonalSystem.getAllZones().keySet()) {
				List<Double> dptTimes = ODMatrixUtils.sampleDeparturesFromPiecewiseConstantInterpolation(ods,
						fromZoneId, toZoneId, rnd);
				
				// Creating a plan and generating a person for each departure time.
				for (double dptTime : dptTimes) {
					// TODO what happens if a person has a null origin or destination link?
					Plan plan = popFact.createPlan();
					Activity originAct = popFact.createActivityFromLinkId("origin", linkSampler.drawLinkId(fromZoneId));
					originAct.setEndTime(dptTime);
					plan.addActivity(originAct);
					plan.addLeg(popFact.createLeg("car"));
					plan.addActivity(popFact.createActivityFromLinkId("destination", linkSampler.drawLinkId(toZoneId)));
					
					Person person = popFact.createPerson(Id.createPersonId(personCnt++));
					person.addPlan(plan);
					scenario.getPopulation().addPerson(person);
				}
				
				// Calculating expected total flow for sanity check.
				for (Matrix matrix : ods.getMatrixListView()) {
					final double val = matrix.get(fromZoneId, toZoneId);
					expectedTotalFlow += val;
				}
			}
		}
		
		log.info("Total expected flow: " + expectedTotalFlow);
		log.info("Total sampled flow: " + scenario.getPopulation().getPersons().size());

		/** Simulation and storing of output */
		Controler controler = new Controler(scenario);
		
		// Configuring the RoadPricing module.
		RoadPricing.configure(controler);
		
		// Suppress EventManager logging output.
		controler.addControlerListener((StartupListener) event -> 
			{ 
				Configurator.setLevel(EventsManagerImpl.class, Level.OFF);
				Configurator.setLevel(RoadPricingTollCalculator.class, Level.OFF);
			}
		);

		// Set up listener for printing output data.
		controler.addControlerListener(new SkimMatrixCalculator(zonalSystem));

		atap.configure(controler);

		// Running the simulation.
		log.info("Starting simulation.");
		controler.run();
		
		if(Thread.currentThread().isInterrupted()) {
			log.fatal("Failed to calculate or write Tramod By output data.");
		}
		
		log.info("Tramod By MATsim simulation is done.");
	}
	
	/**
	 * This method handles input parameters from the program arguments.
	 * @param program arguments
	 * @return parameters
	 */
	private static CommandLine parseArgs(String[] args) {
		Options options = new Options();
		
		// Work directory location
		Option wdOption = new Option("w","work-directory",true, "work directory location");
		wdOption.setRequired(true);
		options.addOption(wdOption);
		
		// Configuration file name
		Option configOption = new Option("c","config-name",true, "population file location [default: config.xml]");
		configOption.setRequired(false);
		options.addOption(configOption);
		
		// Parsing parameters
		CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine parameters = null;
        
        try 
        {
        	parameters = parser.parse(options, args);
        } 
        catch (ParseException e) 
        {
            log.fatal(e.getMessage());
            formatter.printHelp("Tramod sim", options);

            System.exit(1);
        }

        return parameters;
		
	}
}
