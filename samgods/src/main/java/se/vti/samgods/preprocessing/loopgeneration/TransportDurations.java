/**
 * se.vti.samgods
 * 
 * Copyright (C) 2023 Gunnar Flötteröd (VTI, LiU), Rasmus Ringdahl (LiU). 
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
package se.vti.samgods.preprocessing.loopgeneration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.vehicles.Vehicle;

import se.vti.samgods.common.OD;
import se.vti.utils.misc.Units;

/**
 * @author GunnarF
 * 
 * Based on tramodby SimMatrixCalculator, @author RasmusR
 */
public class TransportDurations {

	private static final Logger log = LogManager.getLogger(TransportDurations.class);

	private final Map<Id<Node>, Map<Id<Node>, Double>> origin2Destination2TravelTime_s;

	public TransportDurations(Network network, Set<Node> consideredNodes, double maxSpeed_km_h) {
		double maxSpeed_m_s = Units.M_S_PER_KM_H * maxSpeed_km_h;
		
		TravelTime linkTravelTimes = new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				double t0_s = link.getLength() / Math.min(link.getFreespeed(), maxSpeed_m_s);
				if (Double.isInfinite(t0_s)) {
					throw new RuntimeException("Infinite travel time on link " + link.getId());
				}
				return t0_s;
			}
		};
		
		TravelDisutility linkCosts = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return linkTravelTimes.getLinkTravelTime(link, 0.0, null, null);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return linkTravelTimes.getLinkTravelTime(link, 0.0, null, null);
			}
		};

		this.origin2Destination2TravelTime_s = new LinkedHashMap<>(consideredNodes.size());
		int cnt = 0;
		for (var originNode : consideredNodes) {
			log.info("Routing from origin " + (++cnt) + " of " + consideredNodes.size());

			LeastCostPathTree lcpt = new LeastCostPathTree(linkTravelTimes, linkCosts);
			lcpt.calculate(network, originNode, 0.0);

			Map<Id<Node>, Double> destination2TravelTime_s = new LinkedHashMap<>(consideredNodes.size());
			destination2TravelTime_s.put(originNode.getId(), 0.0);
			for (var node : consideredNodes) {
				destination2TravelTime_s.put(node.getId(), lcpt.getTree().get(node.getId()).getTime());
			}
			this.origin2Destination2TravelTime_s.put(originNode.getId(), destination2TravelTime_s);
		}
	}

	Map<Id<Node>, Map<Id<Node>, Double>> getOrigin2Destination2TravelTime_s() {
		return this.origin2Destination2TravelTime_s;
	}

	Double getDuration_s(Id<Node> from, Id<Node> to) {
		if (this.origin2Destination2TravelTime_s.containsKey(from)) {
			return this.origin2Destination2TravelTime_s.get(from).get(to);
		} else {
			return null;
		}
	}

	Double getDuration_s(OD odPair) {
		return this.getDuration_s(odPair.origin, odPair.destination);
	}

	Double getDuration_h(OD odPair) {
		return Units.H_PER_S * this.getDuration_s(odPair);
	}
}
