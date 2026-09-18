# ROUNDTRIPS

The `se.vti.roundtrips` package samples spatio-temporal (vehicle, person) round trips from un-normalized distributions and provides support functionality for extendability and analysis. 

The underlying method is described and illustrated in this working paper: Flötteröd, Gunnar; Rupprecht, Franz-Xaver; and Sederlin, Michael. *Generic probabilistic spatiotemporal transport model*. Available at SSRN: [SSRN Abstract](https://ssrn.com/abstract=7468019) or [DOI](https://dx.doi.org/10.2139/ssrn.7468019). The individual mobility examples of that paper are in [this standalone repository](https://github.com/michaelSederlin/roundtrips-paper-problem-instances) (should be ready to run); the freight example can be found [here](https://github.com/vtisweden/java-projects/blob/master/samgods/src/main/java/se/vti/samgods/preprocessing/loopgeneration/ElectrifiedSamgodsLoopSamplingRunner.java) (this code is more in flux; scenario data can be requested from the authors).

This project is under continuous further development; we are currently relying on small example applications for documentation. The package `se.vti.roundtrips.examples` contains the following subpackages, all for illustrative purposes only:
* `activityTimeUse` creates individual mobility all-day activity/travel patterns.
* `elektrifiedFlight` studies different configurations of an electrified domestic air transportation system.
* `travelSurveyExpansion` extrapolates a limited travel survey into population-wide travel/activity patterns.
* `truckServiceCoverage` studies possible truck round tours, subject to fleet and delivery constraints.

Contact: gunnar.flotterod@vti.se	
