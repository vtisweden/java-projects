package se.vti.roundtrips.examples;

import java.util.List;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.ScenarioBuilder;
import se.vti.roundtrips.single.RoundTrip;

public class MichaelRVU {

	public static void main(String[] args) {

		var home = new Node("home");
		var office = new Node("office");
		
		var sb = new ScenarioBuilder<Node>().setNumberOfTimeBins(24).setTimeBinSize_h(1.0);
		sb.addNode(home);
		sb.addNode(office);	
		sb.setMoveDistanceFunction((a,b) -> a.equals(b) ? 0.0 : 1.0);
		sb.setMoveTimeFunction((a,b) -> a.equals(b) ? 0.0 : 0.1);		
		var scenario = sb.build();		
		var simulator = scenario.getOrCreateSimulator();
		
		var roundtrip = new RoundTrip<>(0, List.of(home, office), List.of(7, 17));
		var episodes = simulator.simulate(roundtrip);
		System.out.println(roundtrip);
		System.out.println(episodes);
		
		System.out.println("DONE");
		
	}
	
	
}
