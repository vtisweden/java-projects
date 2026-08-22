package se.vti.samgods.transportation.consolidation;

import java.util.List;

public class LoopUtils {

	// For use without instantiation.
	public static final LoopUtils instance = new LoopUtils();

	// For parallel use.
	public LoopUtils() {
	}
	
	public <N> boolean equalUpToShift(List<N> a, List<N> b) {
		int n = a.size();
		if (n != b.size()) {
			return false;
		}
		if (n == 0 || a.equals(b)) {
			return true;
		}
		for (int start = 1; start < n; start++) {
			int split = n - start;
			if (a.subList(0, split).equals(b.subList(start, n)) && a.subList(split, n).equals(b.subList(0, start))) {
				return true;
			}
		}
		return false;
	}

}
