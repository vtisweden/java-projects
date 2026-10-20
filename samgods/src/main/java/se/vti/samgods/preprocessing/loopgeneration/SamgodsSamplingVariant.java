package se.vti.samgods.preprocessing.loopgeneration;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import se.vti.roundtrips.runner.Runner;
import se.vti.roundtrips.scenario.NodeWithCoords;
import se.vti.roundtrips.scenario.RandomRoundTripGenerator;
import se.vti.roundtrips.scenario.Scenario;

interface SamgodsSamplingVariant {

    default void configureOptions(Options options) {
    }

    default void configureScenario(
            CommandLine cmd,
            ScenarioContainer dataContainer,
            Scenario<NodeWithCoords> samplingScenario) {
    }

    default List<List<Enum<?>>> createNodeLabels() {
        return List.of(List.of());
    }

    default void addSamplingWeights(
            Runner<NodeWithCoords> runner,
            ScenarioContainer dataContainer,
            Scenario<NodeWithCoords> samplingScenario) {
    }

    default void configureInitialRoundTripGenerator(
            RandomRoundTripGenerator<NodeWithCoords> generator,
            ScenarioContainer dataContainer,
            Scenario<NodeWithCoords> samplingScenario) {
    }
}