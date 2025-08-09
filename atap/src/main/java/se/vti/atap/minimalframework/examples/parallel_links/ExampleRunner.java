/**
 * se.vti.atap.examples.minimalframework.parallel_links
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.atap.minimalframework.examples.parallel_links;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import se.vti.atap.minimalframework.Runner;
import se.vti.atap.minimalframework.defaults.StatisticsComparisonPrinter;
import se.vti.atap.minimalframework.planselection.OneAtATimePlanSelection;
import se.vti.atap.minimalframework.planselection.SortingPlanSelection;
import se.vti.atap.minimalframework.planselection.UniformPlanSelection;
import se.vti.atap.minimalframework.planselection.proposed.LocalSearchPlanSelection;

public class ExampleRunner {

	public enum Mode {
		TRIPMAKERS, ODPAIRS
	};

	private final Random rnd;

	private Mode mode = null;

	private Integer numberOfLinks = null;
	private Integer numberOfODPairs = null;
	private Integer numberOfPaths = null;
	private Double volumeCapacityRatio = null;
	private Integer iterations = null;
	private Integer replications = null;
	private String fileName = null;

	private Double minT0_s = null;
	private Double maxT0_s = null;
	private Double minCap_veh = null;
	private Double maxCap_veh = null;

	ExampleRunner(Random rnd) {
		this.rnd = rnd;
	}

	ExampleRunner seMinT0_s(double minT0_s) {
		this.minT0_s = minT0_s;
		return this;
	}

	ExampleRunner setMaxT0_s(double maxT0_s) {
		this.maxT0_s = maxT0_s;
		return this;
	}

	ExampleRunner setMinCap_veh(double minCap_veh) {
		this.minCap_veh = minCap_veh;
		return this;
	}

	ExampleRunner setMaxCap_veh(double maxCap_veh) {
		this.maxCap_veh = maxCap_veh;
		return this;
	}

	ExampleRunner setMode(Mode mode) {
		this.mode = mode;
		return this;
	}

	ExampleRunner setNumberOfLinks(int numberOfLinks) {
		this.numberOfLinks = numberOfLinks;
		return this;
	}

	ExampleRunner setNumberOfODPairs(int numberOfODPairs) {
		this.numberOfODPairs = numberOfODPairs;
		return this;
	}

	ExampleRunner setNumberOfPaths(int numberOfPaths) {
		this.numberOfPaths = numberOfPaths;
		return this;
	}

	ExampleRunner setVolumeCapacityRatio(double volumeCapacityRatio) {
		this.volumeCapacityRatio = volumeCapacityRatio;
		return this;
	}

	ExampleRunner setIterations(int iterations) {
		this.iterations = iterations;
		return this;
	}

	ExampleRunner setReplications(int replications) {
		this.replications = replications;
		return this;
	}

	ExampleRunner setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	//

	Network createRandomNetwork() {
		Network network = new Network(this.numberOfLinks);
		for (int link = 0; link < this.numberOfLinks; link++) {
			network.setBPRParameters(link, this.rnd.nextDouble(this.minT0_s, this.maxT0_s),
					this.rnd.nextDouble(this.minCap_veh, this.maxCap_veh));
		}
		return network;
	}

	Set<AgentImpl> createRandomDemand(Network network, int numberOfAgents, double agentSize_veh) {
		List<Integer> links = new ArrayList<>(IntStream.range(0, network.getNumberOfLinks()).boxed().toList());
		Set<AgentImpl> agents = new LinkedHashSet<>(numberOfAgents);
		for (int n = 0; n < numberOfAgents; n++) {
			Collections.shuffle(links, this.rnd);
			int[] availableLinks = links.subList(0, this.numberOfPaths).stream().mapToInt(l -> l).toArray();
			agents.add(new AgentImpl("Agent" + Integer.toString(n), agentSize_veh, availableLinks));
		}
		return agents;
	}

	Set<AgentImpl> createRandomDemand(Network network) {
		if (Mode.TRIPMAKERS.equals(this.mode)) {
			int numberOfTripMakers = (int) (this.volumeCapacityRatio * Arrays.stream(network.cap_veh).sum());
			return this.createRandomDemand(network, numberOfTripMakers, 1.0);
		} else if (Mode.ODPAIRS.equals(this.mode)) {
			double networkCapacityPerODPair_veh = Arrays.stream(network.cap_veh).sum() / this.numberOfODPairs;
			double demandPerODPair_veh = this.volumeCapacityRatio * networkCapacityPerODPair_veh;
			return this.createRandomDemand(network, this.numberOfODPairs, demandPerODPair_veh);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	Runner<PathFlows, AgentImpl, NetworkConditionsImpl> createRunner(Network network, Set<AgentImpl> agents) {
		var runner = new Runner<PathFlows, AgentImpl, NetworkConditionsImpl>().setAgents(agents)
				.setNetworkLoading(new NetworkLoadingImpl(network)).setUtilityFunction(new UtilityFunctionImpl())
				.setVerbose(false).setIterations(this.iterations);
		if (Mode.TRIPMAKERS.equals(this.mode)) {
			runner.setPlanInnovation(new ShortestPathsForTripmakers(network));
		} else if (Mode.ODPAIRS.equals(this.mode)) {
			runner.setPlanInnovation(new GreedyPathAssignmentForODFlows(network));
		} else {
			throw new UnsupportedOperationException();
		}
		return runner;
	}

	//

	public void run() {

		LoggerImpl oneAtATimeLogger = new LoggerImpl();
		LoggerImpl uniformLogger = new LoggerImpl();
		LoggerImpl sortingLogger = new LoggerImpl();
		LoggerImpl proposedLogger = new LoggerImpl();

		var comparison = new StatisticsComparisonPrinter();
		comparison.addLogger("OneAtATime", oneAtATimeLogger).addLogger("Uniform", uniformLogger)
				.addLogger("Sorting", sortingLogger).addLogger("Proposed", proposedLogger);
		comparison.addStatistic("10Percentile", ds -> ds.getPercentile(10)).addStatistic("90Percentile",
				ds -> ds.getPercentile(90));

		for (int replication = 0; replication < this.replications; replication++) {
			System.out.println((replication + 1) + "/" + this.replications);

			Network network = this.createRandomNetwork();
			Set<AgentImpl> demand = this.createRandomDemand(network);

			this.createRunner(network, demand).setPlanSelection(new OneAtATimePlanSelection<>(null))
					.setLogger(oneAtATimeLogger).run();
			this.createRunner(network, demand).setPlanSelection(new UniformPlanSelection<>(-1.0, this.rnd))
					.setLogger(uniformLogger).run();
			this.createRunner(network, demand).setPlanSelection(new SortingPlanSelection<>(-1.0))
					.setLogger(sortingLogger).run();

			var proposedPlanSelection = new LocalSearchPlanSelection<PathFlows, AgentImpl, NetworkConditionsImpl, ApproximateNetworkConditionsImpl>(
					-1.0, this.rnd, new ApproximateNetworkLoadingImpl(network)).setApproximateDistance(true)
					.setMinimalRelativeImprovement(1e-8);
			createRunner(network, demand).setPlanSelection(proposedPlanSelection).setLogger(proposedLogger).run();

			comparison.printToFile(this.fileName);
		}
	}

	static void runSmallTripMakerExample() {
		new ExampleRunner(new Random(4711)).setMode(Mode.TRIPMAKERS).seMinT0_s(60.0).setMaxT0_s(600.0)
				.setMinCap_veh(1.0).setMaxCap_veh(3.0).setNumberOfLinks(100)
				.setNumberOfPaths(10).setVolumeCapacityRatio(1.0).setIterations(1000).setReplications(10)
				.setFileName("SmallTripMakerExample.tsv").run();
	}

	static void runSmallODExample() {
		new ExampleRunner(new Random(4711)).setMode(Mode.ODPAIRS).seMinT0_s(60.0).setMaxT0_s(600.0)
				.setMinCap_veh(1000.0).setMaxCap_veh(3000.0).setNumberOfLinks(100).setNumberOfODPairs(100)
				.setNumberOfPaths(10).setVolumeCapacityRatio(1.0).setIterations(1000).setReplications(10)
				.setFileName("SmallODFlowExample.tsv").run();
	}

	public static void main(String[] args) {
		runSmallTripMakerExample();
		runSmallODExample();
	}

}
