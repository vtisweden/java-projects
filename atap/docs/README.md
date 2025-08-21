# ATAP - Agent-based Traffic Assignment Problem

** This document is under construction. Please come back later. **

The `se.vti.atap` package implements a solution heuristic for the agent-based traffic assigment problem. It contains a MATSim extension (subpackage `matsim`) and a stand-alone implementation (subpackage `minimalframework`). Both are described further below. A detailed explanation of the method can be found in the following working paper (until publication only available via Email from gunnar.flotterod@vti.se): *G. Flötteröd (2025). A simulation heuristic for traveler- and vehicle-discrete dynamic traffic assignment. Linköping University and Swedish National Road and Transport Research Institute.*

## Using the ATAP MATSim extension

The ATAP extension replaces MATSim's standard solver (a coevolutionary algorithm). Experience so far indicates that solutions computed with ATAP exhibit very little variability (i.e. high reproducibility) and have much lower equilibrium gaps than the standard solver.

### Accessing the code

Either clone the repository or configure [github packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages). 

Include the following Maven dependency in your pom.xml:

	<dependency>
		<groupId>se.vti.matsim-projects</groupId>
		<artifactId>atap</artifactId>
		<version>TODO</version>
	</dependency>

The MATSim extension is located in the `se.vti.atap.matsim` package.

### Using the code

Minimal usage:

	ATAP atap = new ATAP();
	
	Config config = ConfigUtils.loadConfig(configFileName);		
	atap.configure(config);
		
	Scenario scenario = ScenarioUtils.loadScenario(config);		
	Controler controler = new Controler(scenario);
	atap.configure(controler);
	
	controler.run();
	
This will use a default configuration, which may work for simple MATSim scenarios. Two additional configurations may be necessary.

Add an atap module to your MATSim config file. Example, using default values:

	<module name="atap" >
	
		<!-- ASSIGNMENT METHOD. OPTIONS: DO_NOTHING, UNIFORM, SORTING, ATAP_APPROXIMATE_DISTANCE, ATAP_EXACT_DISTANCE ->
		<param name="replannerIdentifier" value="ATAP_APPROXIMATE_DISTANCE" />
	
		<!-- STEPSIZE = initialStepSizeFactor * (1.0 + iteration)^replanningRateIterationExponent -->
		<param name="initialStepSizeFactor" value="1.0" />
		<param name="replanningRateIterationExponent" value="-0.5" />
	
		<!-- NUMBER OF ITERATIONS USED TO FILTER OUT DNL NOISE -->
		<param name="maxMemory" value="1" />
	
		<!-- NETWORK FLOW SMOOTHING. DEFAULTS WORK FOR "STANDARD MATSIM" -->
		<param name="kernelHalftime_s" value="300.0" />
		<param name="kernelThreshold" value="0.01" />
		
		<!-- SPECIFY COMPUTATIONAL CHEAP AND HEAVY MATSIM STRATEGIES. FOR INTERNAL PERFORMANCE TUNING. -->
		<param name="cheapStrategies" value="TimeAllocationMutator" />
		<param name="expensiveStrategies" value="ReRoute,TimeAllocationMutator_ReRoute,ChangeSingleTripMode,SubtourModeChoice,ChangeTripMode,ChangeLegMode,ChangeSingleLegMode,TripSubtourModeChoice" />
		
		<!-- DISTANCE TRANSFORMATIONS. OTHER THAN DEFAULT MAY NOT WORK. -->
		<param name="useLinearDistance" value="true" />
		<param name="useQuadraticDistance" value="true" />
		<param name="useExponentialDistance" value="false" />
		<param name="useLogarithmicDistance" value="false" />
		<param name="normalizeDistance" value="false" />
		
		<!-- TESTING AND/OR DEBUGGING ONLY. BETTER NOT CHANGE. -->
		<param name="checkDistance" value=false" />
		<param name="shuffleBeforeReplannerSelection" value="true" />
		<param name="useFilteredTravelTimesForEmulation" value="false" />
		<param name="useFilteredTravelTimesForReplanning" value="false" />
		<param name="linkShareInDistance" value="1.0" />
		<param name="checkEmulatedAgentsCnt" value="0" />
		
	</module>

ATAP needs to anticipate the scores of not yet executed plans. This functionality is provided by the emulation package, on which ATAP depends. It is provided in the module `emulation` of this repositor.   

The `se.vti.emulation` package moves agents according to exogeneously specified travel times through the system and generates an event stream as if the agent was moved by the mobsim. Emulation is preconfigured for car as a congested network mode and for teleported modes. Other transport modes require to specify corresponding emulation functionality. For instance, the emulation of "pt submodes" can be configured as follows:

	ATAP atap = new ATAP();
	
	atap.setEmulator(TransportMode.pt, ScheduleBasedTransitLegEmulator.class);
	atap.setEmulator("busPassenger", ScheduleBasedTransitLegEmulator.class);
	atap.setEmulator("subwayPassenger", ScheduleBasedTransitLegEmulator.class);
	atap.setEmulator("railPassenger", ScheduleBasedTransitLegEmulator.class);
	
	Config config = ConfigUtils.loadConfig(configFileName);		
	atap.configure(config);
		
	Scenario scenario = ScenarioUtils.loadScenario(config);		
	Controler controler = new Controler(scenario);
	atap.configure(controler);
	
	controler.run();
	
There is also (likely outdated) functionality for emulating roadpricing.

### Example scenario


The package `se.vti.atap.matsim.examples.parallel_links` offers a ready-to-run example of using the ATAP assignment logic in MATSim. (No input files needed, all required data is created in-code.) 

## Exploring ATAP functionality without MATSim

The package `se.vti.atap.minimalframework` is meant for lightweight standalone experimentation with the algorithm. It depends neither on MATSim nor on any other code in this repository, meaning that it can be used by copy&paste into any other java project. There are two ways of using this package.

At the top-level of `se.vti.atap.minimalframework`, there are only interfaces and a single `Runner.java` class. The interfaces correspond to the terminology introduced in Flötteröd (2025). The `Runner.java` combines these interfaces in an ATAP assignment logic. This functions as a blueprint; to evaluate the model, one needs to specify a concrete agent representation, (dynamic) network loading, etc.

The top-level classes of `se.vti.atap.minimalframework.defaults` provides limited default implementations. The `se.vti.atap.minimalframework.defaults.planselection` package provides default implementations of all plan selection algorithms exlored in Flötteröd (2025). 

The package `se.vti.atap.minimalframework.defaults.planselection.proposed` contains an implementation of the "proposed method" of that article. The class `ProposedMethodWithLocalSearchPlanSelection.java` implements the prpoosed plan selection logic relying on (implementations of) the other interfaces and abstract classes in that package. The class  `AbstractApproximateNetworkConditions.java` is slightly more involved than a naive implementation such that it does not constitute a major computational bottleneck (it is evaluated many times).

Ready-to run examples are proided by the `ExampleRunner.java` class in the package `se.vti.atap.minimalframework.examples.parallel_links`. This package contains a complete instantiation of the "minimal framework", for both an "agent-based" and an "OD-flow-based" assignment problem.


