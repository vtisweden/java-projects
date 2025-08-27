/**
 * se.vti.roundtrips.samplingweights.misc.accessibility
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
package se.vti.roundtrips.samplingweights.misc.accessibility;

import java.util.List;
import java.util.Map;

import se.vti.roundtrips.common.Node;

/**
 * 
 * @author GunnarF
 *
 */
public interface AccessibilityUpdater<N extends Node> {

	String[] getLabels();

	void initialize(N node, Map<N, double[]> node2measures);

	Integer computeMostAccessibleNodeIndex(List<N> nodeSequence, List<Integer> departureSequence,
			Map<N, double[]> node2accessibilities);

	boolean updateDestinationNodeAccessibility(List<N> nodeSequence, List<Integer> departureSequence,
			Map<N, double[]> node2accessibilities);
}
