# ATAP - Agent-based Traffic Assignment Problem

The `se.vti.atap` package implements a solution heuristic for the agent-based traffic assigment problem. It contains a MATSim extension (subpackage `matsim`) and a stand-alone implementation (subpackage `minimalframework`). Both are described further below. A detailed explanation of the method can be found in the following working paper (until publication only available from the author, Email below): *G. Flötteröd (2025). A simulation heuristic for traveler- and vehicle-discrete dynamic traffic assignment. Linköping University and Swedish National Road and Transport Research Institute.*

Contact: gunnar.flotterod@{vti,liu}.se

## Exploring ATAP functionality without MATSim

The `se.vti.atap.minimalframework` package is meant for lightweight standalone experimentation. It depends neither on MATSim nor on any other code in this repository, meaning that it can be minimally used by copy&paste into any other java project. There are two ways of using this package.

At the packages top-level, there are only interfaces and a single `Runner.java` class. The interfaces correspond to the terminology introduced in Flötteröd (2025). The `Runner.java` combines these interfaces in an ATAP assignment logic. This functions as a blueprint; to evaluate a concrete model, the corresponding interfaces need to be instantiated. The subpackage `defaults` provides limited default implementations. 

The `defaults.replannerselection` subpackage contains default implementations of all assignment methods exlored in Flötteröd (2025). The subpackage `defaults.planselection.proposed` implements the "proposed method" of that article. `ProposedMethodWithLocalSearchPlanSelection.java` implements the proposed plan method relying on (implementations of) the other interfaces and abstract classes in that package. The class  `AbstractApproximateNetworkConditions.java` is slightly more involved than a naive implementation to avoid that it becomes a major computational bottleneck.

The package `se.vti.atap.minimalframework.examples.parallel_links` offers ready-to-run examples, based on the parallel link nework described in Section 4 of Flötteröd (2025). `ExampleRunner.java` class contains an "agent-based" example (`ExampleRunner.runSmallTripMakerExample()`), an OD flow-based example(`ExampleRunner.runSmallODExample()`), and the original example from the article (`ExampleRunner.runArticleExample()`).

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

The package `se.vti.atap.matsim.examples.parallel_links` contains ready-to-run examples. No input files needed, all required data is created in-code. 

`ParallelLinkScenarioFactory.java` builds a network of parallel links and a corresponding population. The number of parallel links is configurable, so are their parameters. The population is built such that travel occurrs from upstream origin links to downstream destination links that are connected to individually configurable parallel network links. The links connecting origins and destination to the parallel links network are automatically configured such that all origins reach the parallel links at the same time. If there is a chance that congestion spills back into upstream diverges, an exception is thrown and recommendations for redimensioning the system are given.

`ParallelLinkExampleRunner.java` instantiates concrete examples.

