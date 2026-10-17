package se.vti.roundtrips.examples.samplingweights.timeuse;

import java.util.function.Function;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.scenario.Node;

public class UniqueHomeLocationModifier<N extends Node> implements Function<MultiRoundTrip<N>, MultiRoundTrip<N>> {

	@Override
	public MultiRoundTrip<N> apply(MultiRoundTrip<N> original) {

		var modified = original.clone();

		N home = null;
		for (int i = 0; i < (modified.size()) && (home == null); i++) {
			var roundTrip = modified.getRoundTrip(i);
			if (roundTrip.size() > 0) {
				home = roundTrip.getNode(0);
			}
		}

		if (home != null) {
			for (int i = 1; i < modified.size(); i++) {
				var roundTrip = modified.getRoundTrip(i);
				if ((roundTrip.size() > 0) && !home.equals(roundTrip.getNode(0))) {
					var newRoundTrip = roundTrip.clone();
					newRoundTrip.setNode(0, home);
					modified.setRoundTripAndUpdateSummaries(i, newRoundTrip);
				}
			}
		}

		return modified;
	}

}
