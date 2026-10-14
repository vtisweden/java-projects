package se.vti.roundtrips.runner;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.scenario.Node;

public interface MultiRoundTripWrapper<N extends Node> {

	public MultiRoundTrip<N> getMultiRoundTrip();
	
}
