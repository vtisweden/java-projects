/**
 * se.vti.roundtrips.examples.electrifiedFlight
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
package se.vti.roundtrips.examples.electrifiedFlight;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.vti.roundtrips.common.NodeWithCoords;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.simulator.MoveEpisode;
import se.vti.utils.misc.metropolishastings.MHAbstractStateProcessor;

/**
 * @author GunnarF
 */
public class FleetPerformanceLogger extends MHAbstractStateProcessor<MultiRoundTrip<NodeWithCoords>> {

	private final Map<NodeWithCoords, Double> node2servedDemandSum_pax = new LinkedHashMap<>();

	double servedDemand_paxKm = 0.0;
	double emptySeatsSum_npaxKm = 0.0;
	double totalDemandSum_paxKm = 0.0;
	double numberOfUsedPlanesSum = 0.0;

	public FleetPerformanceLogger(long burnInIterations, long samplingInterval) {
		super(burnInIterations, samplingInterval);
	}

	public List<Double> getAvgServedDemand_pax(List<NodeWithCoords> nodes) {
		return nodes.stream().mapToDouble(n -> this.node2servedDemandSum_pax.getOrDefault(n, 0.0) / this.samples())
				.boxed().toList();
	}

	@Override
	public void processStateHook(MultiRoundTrip<NodeWithCoords> fleet, double logWeight) {
		var summary = fleet.getSummary(PlaneUsageSummary.class);
		for (var nodeAndServedDemand : summary.node2servedDemand_pax.entrySet()) {
			this.node2servedDemandSum_pax.compute(nodeAndServedDemand.getKey(),
					(n, d) -> (d == null) ? nodeAndServedDemand.getValue() : (d + nodeAndServedDemand.getValue()));
		}
		this.servedDemand_paxKm += summary.servedDemand_paxKm;
		this.emptySeatsSum_npaxKm += summary.emptySeats_npaxKm;
		this.totalDemandSum_paxKm += summary.totalDemand_paxKm;
		for (var plane : fleet) {
			var episodes = plane.getEpisodes();
			for (int i = 1; i < episodes.size(); i += 2) {
				var move = (MoveEpisode<?>) episodes.get(i);
				if (move.getOrigin() != move.getDestination()) {
					this.numberOfUsedPlanesSum++;
					break;
				}
			}
		}
	}
}
