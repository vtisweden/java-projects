package se.vti.samgods.preprocessing.loopgeneration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.multi.grouping.MultiRoundTripDirectoryReader;
import se.vti.roundtrips.scenario.NodeWithCoords;
import se.vti.roundtrips.scenario.Scenario;

public class SamgodsLoopsAnalyzer {

	private final Scenario<NodeWithCoords> scenario;

	private final ODCoverage<NodeWithCoords> odCoverage;

	private final UniformTransportEfficiencyAnalyzer analyzer;

	public SamgodsLoopsAnalyzer(Scenario<NodeWithCoords> scenario, ODCoverage<NodeWithCoords> odCoverage,
			UniformTransportEfficiencyAnalyzer analyzer) {
		this.scenario = scenario;
		this.odCoverage = odCoverage;
		this.analyzer = analyzer;
	}

	void processState(MultiRoundTrip<NodeWithCoords> state) {
		state.simulateAll(scenario.getOrCreateSimulator());
		state.addSummary(odCoverage);
		state.recomputeSummaries();
		this.analyzer.process(state);
	}

	public void run(Path directory, String prefix, String suffix, int min, int max) {
		var directoryReader = new MultiRoundTripDirectoryReader<NodeWithCoords>(this.scenario,
				new Consumer<MultiRoundTrip<NodeWithCoords>>() {
					@Override
					public void accept(MultiRoundTrip<NodeWithCoords> state) {
						processState(state);
					}
				});
		try {
			directoryReader.run(directory, prefix, suffix, min, max);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
