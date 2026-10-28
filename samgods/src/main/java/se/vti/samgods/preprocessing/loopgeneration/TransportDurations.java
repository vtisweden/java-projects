/**
 * se.vti.samgods
 * 
 * Copyright (C) 2025, 2026 by Gunnar Flötteröd (VTI, LiU).
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import se.vti.samgods.common.OD;
import se.vti.utils.misc.Units;

/**
 * Based on tramodby SimMatrixCalculator, @author RasmusR
 * 
 * @author GunnarF
 * 
 */
class TransportDurations {

	private static final Logger log = LogManager.getLogger(TransportDurations.class);

	private final Map<Id<Node>, Map<Id<Node>, Double>> origin2Destination2TravelTime_s;
	private final Map<Id<Node>, Map<Id<Node>, Double>> origin2Destination2Distance_m;

//	TransportDurations(Network network, Set<Id<Node>> consideredNodeIds, double maxSpeed_km_h) {
//		double maxSpeed_m_s = Units.M_S_PER_KM_H * maxSpeed_km_h;
//
//		var linkTravelTimes = new TravelTime() {
//			@Override
//			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
//				double t0_s = link.getLength() / Math.min(link.getFreespeed(), maxSpeed_m_s);
//				if (Double.isInfinite(t0_s)) {
//					throw new RuntimeException("Infinite travel time on link " + link.getId());
//				}
//				return t0_s;
//			}
//		};
//
//		var linkCosts = new TravelDisutility() {
//			@Override
//			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
//				return linkTravelTimes.getLinkTravelTime(link, 0.0, null, null);
//			}
//
//			@Override
//			public double getLinkMinimumTravelDisutility(Link link) {
//				return linkTravelTimes.getLinkTravelTime(link, 0.0, null, null);
//			}
//		};
//
//		Set<Node> consideredNodes = consideredNodeIds.stream().map(id -> network.getNodes().get(id))
//				.collect(Collectors.toSet());
//		this.origin2Destination2TravelTime_s = new LinkedHashMap<>(consideredNodes.size());
//		this.origin2Destination2Distance_m = new LinkedHashMap<>(consideredNodes.size());
//		int cnt = 0;
//		for (var originNode : consideredNodes) {
//			log.info("Routing from origin " + (++cnt) + " of " + consideredNodes.size());
//
//			LeastCostPathTree lcpt = new LeastCostPathTree(linkTravelTimes, linkCosts);
//			lcpt.calculate(network, originNode, 0.0);
//
//			Map<Id<Node>, Double> destination2TravelTime_s = new LinkedHashMap<>(consideredNodes.size());
//			Map<Id<Node>, Double> destination2Distance_m = new LinkedHashMap<>(consideredNodes.size());
//			destination2TravelTime_s.put(originNode.getId(), 0.0);
//			for (var node : consideredNodes) {
//				destination2TravelTime_s.put(node.getId(), lcpt.getTree().get(node.getId()).getTime());
//				destination2Distance_m.put(node.getId(), this.computeDistance_m(lcpt, network, node));
//			}
//			this.origin2Destination2TravelTime_s.put(originNode.getId(), destination2TravelTime_s);
//			this.origin2Destination2Distance_m.put(originNode.getId(), destination2Distance_m);
//		}
//	}

	public TransportDurations(Network network, Set<Id<Node>> consideredNodeIds, double maxSpeed_km_h, String fileName) {

		this.origin2Destination2TravelTime_s = new LinkedHashMap<>(consideredNodeIds.size());
		this.origin2Destination2Distance_m = new LinkedHashMap<>(consideredNodeIds.size());

		if (!this.readDistancesAndTimes(fileName, consideredNodeIds)) {
			this.computeAndWriteDistancesAndTimes(network, consideredNodeIds, maxSpeed_km_h, fileName);
		}
	}

	private boolean readDistancesAndTimes(String fileName, Set<Id<Node>> consideredNodeIds) {

		File file = new File(fileName);
		if (!file.exists()) {
			return false;
		}

		try (CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withSkipLines(1)
				.withCSVParser(new CSVParserBuilder().withSeparator('\t').build()).build()) {

			String[] row;
			while ((row = reader.readNext()) != null) {

				Id<Node> from = Id.createNodeId(row[0]);
				Id<Node> to = Id.createNodeId(row[1]);

				if (consideredNodeIds.contains(from) && consideredNodeIds.contains(to)) {
					this.origin2Destination2TravelTime_s.computeIfAbsent(from, id -> new LinkedHashMap<>()).put(to,
							Double.parseDouble(row[2]));
					this.origin2Destination2Distance_m.computeIfAbsent(from, id -> new LinkedHashMap<>()).put(to,
							Double.parseDouble(row[3]));
				}
			}

			for (Id<Node> from : consideredNodeIds) {
			    for (Id<Node> to : consideredNodeIds) {
			        if (!this.origin2Destination2TravelTime_s
			                .getOrDefault(from, Map.of()).containsKey(to)
			                || !this.origin2Destination2Distance_m
			                .getOrDefault(from, Map.of()).containsKey(to)) {
			            this.origin2Destination2TravelTime_s.clear();
			            this.origin2Destination2Distance_m.clear();
			            return false;
			        }
			    }
			}
			return true;

		} catch (IOException | CsvValidationException e) {
			throw new RuntimeException(e);
		}
	}

	private void computeAndWriteDistancesAndTimes(Network network, Set<Id<Node>> consideredNodeIds,
			double maxSpeed_km_h, String fileName) {

		final double maxSpeed_m_s = Units.M_S_PER_KM_H * maxSpeed_km_h;

		final TravelTime linkTravelTimes = new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength() / Math.min(link.getFreespeed(), maxSpeed_m_s);
			}
		};

		final TravelDisutility linkCosts = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return linkTravelTimes.getLinkTravelTime(link, time, person, vehicle);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return linkTravelTimes.getLinkTravelTime(link, 0.0, null, null);
			}
		};

		final Set<Node> consideredNodes = consideredNodeIds.stream().map(network.getNodes()::get)
				.collect(Collectors.toSet());

		try (BufferedWriter writer = Files.newBufferedWriter(Path.of(fileName))) {

			writer.write("from\tto\ttime[s]\tdistance[m]");
			writer.newLine();

			int cnt = 0;

			for (Node originNode : consideredNodes) {

				log.info("Routing from origin " + (++cnt) + " of " + consideredNodes.size());

				final LeastCostPathTree lcpt = new LeastCostPathTree(linkTravelTimes, linkCosts);
				lcpt.calculate(network, originNode, 0.0);
				
				final Map<Id<Node>, Double> destination2TravelTime_s = new LinkedHashMap<>();
				final Map<Id<Node>, Double> destination2Distance_m = new LinkedHashMap<>();

				for (Node destinationNode : consideredNodes) {
					final double travelTime_s = lcpt.getTree().get(destinationNode.getId()).getTime();
					final double distance_m = this.computeDistance_m(lcpt, network, destinationNode);
					destination2TravelTime_s.put(destinationNode.getId(), travelTime_s);
					destination2Distance_m.put(destinationNode.getId(), distance_m);

					writer.write(originNode.getId().toString());
					writer.write('\t');
					writer.write(destinationNode.getId().toString());
					writer.write('\t');
					writer.write(Double.toString(travelTime_s));
					writer.write('\t');
					writer.write(Double.toString(distance_m));
					writer.newLine();
				}

				this.origin2Destination2TravelTime_s.put(originNode.getId(), destination2TravelTime_s);
				this.origin2Destination2Distance_m.put(originNode.getId(), destination2Distance_m);
			}

		} catch (IOException e) {
			throw new UncheckedIOException("Could not write distances and travel times to " + fileName, e);
		}
	}

	private double computeDistance_m(LeastCostPathTree tree, Network network, Node destination) {
		double distance_m = 0.0;
		Node current = destination;
		while (!current.equals(tree.getOrigin())) {
			LeastCostPathTree.NodeData data = tree.getTree().get(current.getId());
			Id<Node> prevNodeId = data.getPrevNodeId();
			Link link = current.getInLinks().values().stream().filter(l -> l.getFromNode().getId().equals(prevNodeId))
					.findFirst().orElseThrow();
			distance_m += link.getLength();
			current = network.getNodes().get(prevNodeId);
		}
		return distance_m;
	}

	private Double getValue(Id<Node> from, Id<Node> to, Map<Id<Node>, Map<Id<Node>, Double>> origin2destination2value) {
		if (origin2destination2value.containsKey(from)) {
			return origin2destination2value.get(from).get(to);
		} else {
			return null;
		}
	}

	private Double multiplyOrNull(Double value, double factor) {
		if (value == null) {
			return null;
		} else {
			return factor * value;
		}
	}

	Double getDuration_s(Id<Node> from, Id<Node> to) {
		return this.getValue(from, to, this.origin2Destination2TravelTime_s);
	}

	Double getDuration_h(Id<Node> from, Id<Node> to) {
		return this.multiplyOrNull(this.getDuration_s(from, to), Units.H_PER_S);
	}

	Double getDuration_s(OD odPair) {
		return this.getDuration_s(odPair.origin, odPair.destination);
	}

	Double getDuration_h(OD odPair) {
		return this.getDuration_h(odPair.origin, odPair.destination);
	}

	Double getDistance_m(Id<Node> from, Id<Node> to) {
		return this.getValue(from, to, this.origin2Destination2Distance_m);
	}

	Double getDistance_km(Id<Node> from, Id<Node> to) {
		return this.multiplyOrNull(this.getDistance_m(from, to), Units.KM_PER_M);
	}

	Double getDistance_m(OD odPair) {
		return this.getDistance_m(odPair.origin, odPair.destination);
	}

	Double getDistance_km(OD odPair) {
		return this.getDistance_km(odPair.origin, odPair.destination);
	}

}
