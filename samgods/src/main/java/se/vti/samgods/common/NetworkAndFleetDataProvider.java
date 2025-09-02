/**
 * se.vti.samgods
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.samgods.common.SamgodsConstants.TransportMode;
import se.vti.samgods.network.SamgodsLinkAttributes;
import se.vti.samgods.network.SamgodsNodeAttributes;
import se.vti.samgods.network.TransportModes;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkAndFleetDataProvider {

	private static final Logger log = LogManager.getLogger(NetworkAndFleetDataProvider.class);

	// -------------------- MEMBERS --------------------

	private final Network multimodalNetwork;

	private final ConcurrentMap<Id<Link>, Link> links;
	private final Set<Id<Node>> domesticNodeIds = ConcurrentHashMap.newKeySet();
	private final Set<Id<Link>> domesticLinkIds = ConcurrentHashMap.newKeySet();
	private final Set<Id<Link>> ferryLinkIds = ConcurrentHashMap.newKeySet();

	private final ConcurrentMap<VehicleType, SamgodsVehicleAttributes> vehicleType2attributes;
	private final ConcurrentMap<Id<Link>, CopyOnWriteArraySet<VehicleType>> linkId2allowedVehicleTypes;
	private final ConcurrentMap<Commodity, ConcurrentMap<TransportMode, ConcurrentMap<Boolean, CopyOnWriteArraySet<VehicleType>>>> commodity2transportMode2isContainer2compatibleVehicleTypes = new ConcurrentHashMap<>();

	private final ConcurrentMap<VehicleType, ConcurrentMap<Id<Link>, BasicTransportCost>> vehicleType2linkId2unitCost = new ConcurrentHashMap<>();

//	private ConcurrentMap<VehicleType, Double> vehicleType2asc = new ConcurrentHashMap<>();
//	private ConcurrentMap<TransportMode, Double> mode2asc = new ConcurrentHashMap<>();
//	private ConcurrentMap<Commodity, Double> railCommodity2asc = new ConcurrentHashMap<>();

	// -------------------- SINGLETON --------------------

	private static NetworkAndFleetDataProvider instance = null;

	// TODO use also for re-initialization
	public static void initialize(Network network, Vehicles vehicles) {
		instance = new NetworkAndFleetDataProvider(network, vehicles);
	}

//	// TODO unsure if below should stay here, combine with the above?
//	public static void updateASCs(ASCs ascs) {
//		instance.vehicleType2asc = new ConcurrentHashMap<>(ascs.getVehicleTyp2ASC());
//		instance.mode2asc = new ConcurrentHashMap<>(ascs.getMode2ASC());
//		instance.railCommodity2asc = new ConcurrentHashMap<>(ascs.getRailCommodity2ASC());
//	}

	public static NetworkAndFleetDataProvider getProviderInstance() {
		return instance;
	}

	public NetworkAndFleetData createDataInstance() {
		return new NetworkAndFleetData(this);
	}

	// -------------------- PRIVATE CONSTRUCTION --------------------

	private NetworkAndFleetDataProvider(Network multimodalNetwork, Vehicles vehicles) {

		/*
		 * Network parameter extraction.
		 */

		this.multimodalNetwork = multimodalNetwork;
		this.domesticNodeIds.addAll(multimodalNetwork.getNodes().values().stream()
				.filter(l -> ((SamgodsNodeAttributes) l.getAttributes()
						.getAttribute(SamgodsNodeAttributes.ATTRIBUTE_NAME)).isDomestic)
				.map(l -> l.getId()).collect(Collectors.toSet()));
		this.domesticLinkIds.addAll(multimodalNetwork.getLinks().values().stream()
				.filter(l -> this.domesticNodeIds.contains(l.getFromNode().getId())
						&& this.domesticNodeIds.contains(l.getToNode().getId()))
				.map(l -> l.getId()).collect(Collectors.toSet()));
		this.ferryLinkIds.addAll(multimodalNetwork.getLinks().values().stream()
				.filter(l -> ((SamgodsLinkAttributes) l.getAttributes()
						.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME)).samgodsMode.isFerry())
				.map(l -> l.getId()).collect(Collectors.toSet()));
		this.links = new ConcurrentHashMap<>(multimodalNetwork.getLinks());

		/*
		 * Fleet parameter extraction.
		 */

		this.vehicleType2attributes = new ConcurrentHashMap<>(vehicles.getVehicleTypes().values().stream()
				.collect(Collectors.toMap(t -> t, t -> (SamgodsVehicleAttributes) t.getAttributes()
						.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME))));
//		this.vehicleType2asc = new ConcurrentHashMap<>(
//				vehicles.getVehicleTypes().values().stream().collect(Collectors.toMap(t -> t, t -> 0.0)));

		final Map<String, Set<VehicleType>> networkMode2vehicleTypes = new LinkedHashMap<>();
		final Set<VehicleType> ferryCompatibleRoadVehicleTypes = new LinkedHashSet<>();
		final Set<VehicleType> ferryCompatibleRailVehicleTypes = new LinkedHashSet<>();
		for (Map.Entry<VehicleType, SamgodsVehicleAttributes> e : this.vehicleType2attributes.entrySet()) {
			final VehicleType vehicleType = e.getKey();
			final SamgodsVehicleAttributes vehicleAttrs = e.getValue();
			for (String networkMode : vehicleAttrs.networkModes) {
				networkMode2vehicleTypes.computeIfAbsent(networkMode, m -> new LinkedHashSet<>()).add(vehicleType);
			}
			if (vehicleAttrs.isFerryCompatible()) {
				if (TransportMode.Road.equals(vehicleAttrs.samgodsMode)) {
					ferryCompatibleRoadVehicleTypes.add(vehicleType);
				} else if (TransportMode.Rail.equals(vehicleAttrs.samgodsMode)) {
					ferryCompatibleRailVehicleTypes.add(vehicleType);
				} else {
					throw new RuntimeException(
							"Unexpected ferry compatible samgods main mode " + vehicleAttrs.samgodsMode);
				}
			}
		}

		this.linkId2allowedVehicleTypes = new ConcurrentHashMap<>();
		for (Link link : multimodalNetwork.getLinks().values()) {
			final SamgodsLinkAttributes linkAttributes = (SamgodsLinkAttributes) link.getAttributes()
					.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME);
			final Set<VehicleType> allowedTypes = new LinkedHashSet<>();
			if (TransportMode.Ferry.equals(linkAttributes.samgodsMode)) {
				if (linkAttributes.isRoadFerryLink()) {
					allowedTypes.addAll(ferryCompatibleRoadVehicleTypes);
				} else if (linkAttributes.isRailFerryLink()) {
					allowedTypes.addAll(ferryCompatibleRailVehicleTypes);
				} else {
					throw new RuntimeException("Link has transport mode " + TransportMode.Ferry
							+ " but is neither a road ferry link nor a rail ferry link.");
				}
			} else {
				for (String networkMode : linkAttributes.networkModes) {
					if (networkMode2vehicleTypes.containsKey(networkMode)) {
						allowedTypes.addAll(networkMode2vehicleTypes.get(networkMode));
					}
				}
			}
			this.linkId2allowedVehicleTypes.put(link.getId(), new CopyOnWriteArraySet<>(allowedTypes));
		}
	}

	// -------------------- CONTENT ACCESS --------------------

	synchronized Network createNetwork(SamgodsVehicleAttributes vehicleAttrs) {
		assert (!TransportMode.Ferry.equals(vehicleAttrs.samgodsMode));

		final Network unimodalNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(this.multimodalNetwork).filter(unimodalNetwork,
				Collections.singleton(TransportModes.getMatsimModeIgnoreFerry(vehicleAttrs.samgodsMode)));

		final Set<Id<Link>> removeLinkIds = new LinkedHashSet<>();
		for (Link link : this.multimodalNetwork.getLinks().values()) {
			final SamgodsLinkAttributes linkAttrs = (SamgodsLinkAttributes) link.getAttributes()
					.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME);
			if (vehicleAttrs.networkModes.stream().noneMatch(nm -> linkAttrs.networkModes.contains(nm))) {
				removeLinkIds.add(link.getId());
			}
		}
		removeLinkIds.forEach(id -> unimodalNetwork.removeLink(id));

		log.warn("Not cleaning unimodal network."); // new NetworkCleaner().run(unimodalNetwork);
		return unimodalNetwork;
	}

	Set<Id<Node>> getDomesticNodeIds() {
		return this.domesticNodeIds;
	}

	Set<Id<Link>> getDomesticLinkIds() {
		return this.domesticLinkIds;
	}

	Set<Id<Link>> getFerryLinkIds() {
		return this.ferryLinkIds;
	}

	ConcurrentMap<Id<Link>, Link> getLinks() {
		return this.links;
	}

	ConcurrentMap<VehicleType, ConcurrentMap<Id<Link>, BasicTransportCost>> getVehicleType2linkId2unitCost() {
		return this.vehicleType2linkId2unitCost;
	}

	ConcurrentMap<Id<Link>, CopyOnWriteArraySet<VehicleType>> getLinkId2allowedVehicleTypes() {
		return this.linkId2allowedVehicleTypes;
	}

	ConcurrentMap<VehicleType, SamgodsVehicleAttributes> getVehicleType2attributes() {
		return this.vehicleType2attributes;
	}

	ConcurrentMap<Commodity, ConcurrentMap<TransportMode, ConcurrentMap<Boolean, CopyOnWriteArraySet<VehicleType>>>> getCommodity2transportMode2isContainer2compatibleVehicleTypes() {
		return this.commodity2transportMode2isContainer2compatibleVehicleTypes;
	}

//	ConcurrentMap<VehicleType, Double> getVehicleType2asc() {
//		return this.vehicleType2asc;
//	}
//
//	ConcurrentMap<TransportMode, Double> getMode2asc() {
//		return this.mode2asc;
//	}
//
//	ConcurrentMap<Commodity, Double> getRailCommodity2asc() {
//		return this.railCommodity2asc;
//	}
}
