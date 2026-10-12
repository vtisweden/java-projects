package se.vti.roundtrips.common;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.RoundTrip;

public interface RoundTrips<N extends Node> {

	int size();
	
	RoundTrip<N> getRoundTrip(int index);
	
	int computeSumOfRoundTripSizes();
	
	MultiRoundTrip<N> getAsMultiRoundTrip();
	
	RoundTrips<N> clone();
	
}
