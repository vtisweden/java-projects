/**
 * se.vti.atap.examples.minimalframework.parallel_links.ods
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
package se.vti.atap.examples.minimalframework.parallel_links.ods;

import java.util.Collections;
import java.util.Set;

import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.examples.minimalframework.parallel_links.NetworkConditionsImpl;
import se.vti.atap.minimalframework.NetworkLoading;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkLoadingImpl implements NetworkLoading<ODPair, NetworkConditionsImpl> {

	private final Network network;

	public NetworkLoadingImpl(Network network) {
		this.network = network;
	}

	public double[] computeLinkFlows_veh(Set<ODPair> agentsUsingCurrentPlan, Set<ODPair> agentsUsingCandidatePlan) {
		double[] linkFlows_veh = new double[this.network.getNumberOfLinks()];
		for (ODPair odPair : agentsUsingCurrentPlan) {
			for (int path = 0; path < odPair.getNumberOfPaths(); path++) {
				linkFlows_veh[odPair.availableLinks[path]] += odPair.getCurrentPlan().pathFlows_veh[path];
			}
		}
		for (ODPair odPair : agentsUsingCandidatePlan) {
			for (int path = 0; path < odPair.getNumberOfPaths(); path++) {
				linkFlows_veh[odPair.availableLinks[path]] += odPair.getCandidatePlan().pathFlows_veh[path];
			}
		}
		return linkFlows_veh;
	}

	public double[] computeLinkTravelTimes_s(double[] linkFlows_veh) {
		double[] travelTimes_s = new double[linkFlows_veh.length];
		for (int link = 0; link < linkFlows_veh.length; link++) {
			travelTimes_s[link] = this.network.computeLinkTravelTime_s(link, linkFlows_veh[link]);
		}
		return travelTimes_s;
	}

	public double[] compute_dLinkTravelTimes_dLinkFlows_s_veh(double[] linkFlows_veh) {
		double[] dLinkTravelTimes_dLinkFlows_s_veh = new double[linkFlows_veh.length];
		for (int link = 0; link < linkFlows_veh.length; link++) {
			dLinkTravelTimes_dLinkFlows_s_veh[link] = this.network.compute_dLinkTravelTime_dLinkFlow_s_veh(link,
					linkFlows_veh[link]);
		}
		return dLinkTravelTimes_dLinkFlows_s_veh;
	}

	@Override
	public NetworkConditionsImpl compute(Set<ODPair> odPairs) {
		double[] linkFlows_veh = this.computeLinkFlows_veh(odPairs, Collections.emptySet());
		var networkConditions = new NetworkConditionsImpl(linkFlows_veh, this.computeLinkTravelTimes_s(linkFlows_veh),
				this.compute_dLinkTravelTimes_dLinkFlows_s_veh(linkFlows_veh));
		return networkConditions;
	}
}
