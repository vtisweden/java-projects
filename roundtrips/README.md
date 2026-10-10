# ROUNDTRIPS

The `se.vti.roundtrips` package samples spatio-temporal (vehicle, person) round trips from un-normalized distributions and provides support functionality for extendability and analysis. 

The underlying method is described and illustrated in this working paper: Flötteröd, Gunnar; Rupprecht, Franz-Xaver; and Sederlin, Michael. *Generic probabilistic spatiotemporal transport model*. Available at SSRN: [SSRN Abstract](https://ssrn.com/abstract=7468019) or [DOI](https://dx.doi.org/10.2139/ssrn.7468019). The individual mobility examples of that paper are in [this standalone repository](https://github.com/michaelSederlin/roundtrips-paper-problem-instances) (should run out of the box); the freight example can be found [here](https://github.com/vtisweden/java-projects/blob/master/samgods/src/main/java/se/vti/samgods/preprocessing/loopgeneration/ElectrifiedSamgodsLoopSamplingRunner.java) (this code is more in flux; scenario data can be requested from the authors).

Contact: [gunnar.flotterod@vti.se](mailto:gunnar.flotterod@vti.se)

The overall workflow when analyzing a scenario looks as follows.
1. Decide how you want to represent the locations (nodes) of your scenario. Basic `Node.java` and `NodeWithCoords.java` implementations are available. 
2. Instantiate and parameterize `Scenario.java`. This comprises setting the temporal resolution and defining nodes with distances and move times between them.
3. Instantiate and parameterize `Runner.java`, which requires a complete `Scenario.java` upon construction. Use `set...Prior(...)` to set baseline distributions and add scenario-specific weights with `add...Weight(...)`.
4. Execute the experiment by calling `Runner`'s `run()` function.
5. Collect statistics of interest. `Runner` produces some default statistics files. Simple additional statistics can be generated with its `addSampleExtractor` function; custom state monitoring can be achieved through `addStateProcessor`.

The package `se.vti.roundtrips.examples` contains the following examples, all for illustration only.
* `activityTimeUse` creates individual mobility all-day activity/travel patterns.
* `elektrifiedFlight` studies different configurations of an electrified domestic air transportation system.
* `travelSurveyExpansion` extrapolates a limited travel survey into population-wide travel/activity patterns.
* `truckServiceCoverage` studies possible truck round tours, subject to fleet and delivery constraints.
