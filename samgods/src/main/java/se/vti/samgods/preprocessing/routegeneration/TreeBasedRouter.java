/**
 * se.vti.samgods
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.preprocessing.routegeneration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.common.NetworkAndFleetData;
import se.vti.samgods.common.NetworkAndFleetDataProvider;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsConstants.Commodity;

/**
 * 
 * Based on tramodby SimMatrixCalculator.
 * 
 * @author GunnarF
 *
 */
public class TreeBasedRouter {

	// -------------------- LOGGING, ONLY FOR TESTING --------------------

	private static final Logger log = LogManager.getLogger(TreeBasedRouter.class);

	static class Job {
		static Job TERMINATE = new Job();

		VehicleType getVehicleType() {
			throw new UnsupportedOperationException("TODO!");
		}

		Id<Node> getOriginNodeId() {
			throw new UnsupportedOperationException("TODO!");
		}

		Set<Id<Node>> getDestinationNodeIds() {
			throw new UnsupportedOperationException("TODO!");
		}

		Commodity getCommodity() {
			throw new UnsupportedOperationException("TODO!");
		}

		SamgodsConstants.TransportMode getSamgodsMode() {
			throw new UnsupportedOperationException("TODO!");
		}

		boolean isContainer() {
			throw new UnsupportedOperationException("TODO!");
		}
	}

	private class LeastCostPathTreeRunner implements Runnable {

		private final String name;
		private final NetworkAndFleetData networkAndFleetData;
		private final BlockingQueue<Job> jobQueue;

		private final Map<VehicleType, LeastCostPathTree> vehicleType2leastCostPathTree = new LinkedHashMap<>();

		LeastCostPathTreeRunner(String name, NetworkAndFleetData networkAndFleetData, BlockingQueue<Job> jobQueue) {
			this.name = name;
			this.networkAndFleetData = networkAndFleetData;
			this.jobQueue = jobQueue;
		}

		/* TODO */ void computeTree(Job job) {

			final Network unimodalNetwork = this.networkAndFleetData.getUnimodalNetwork(job.getVehicleType());
			if (unimodalNetwork == null) {
				log.warn("No network available. Skipping job: " + job);
//				return null;
			}

			final LeastCostPathTree lcpt = this.vehicleType2leastCostPathTree.computeIfAbsent(job.getVehicleType(),
					vt -> {
						final TravelDisutility travelDisutility = this.networkAndFleetData
								.getTravelDisutility(job.getVehicleType());
						final TravelTime travelTime = this.networkAndFleetData.getTravelTime(job.getVehicleType());
						if (travelTime == null) {
							log.warn("No TravelTime available. Skipping job: " + job);
							return null;
						}
						if (travelDisutility == null) {
							log.warn("No TravelDisutility available. Skipping job: " + job);
							return null;
						}
						return new LeastCostPathTree(travelTime, travelDisutility);
					});
			if (lcpt == null) {
				log.warn("No LeastCostPathTree available. Skipping job: " + job);
//				return null;
			}

			Network network = this.networkAndFleetData.getUnimodalNetwork(job.getVehicleType());
			lcpt.calculate(network, network.getNodes().get(job.getOriginNodeId()), 0.0);

			final Map<Node, Set<Link>> node2outLinks = new LinkedHashMap<>();
			for (Map.Entry<Id<Node>, LeastCostPathTree.NodeData> entry : lcpt.getTree().entrySet()) {
				if (entry.getValue().getPrevNodeId() != null) {
					final Node fromNode = network.getNodes().get(entry.getValue().getPrevNodeId());
					final Node toNode = network.getNodes().get(entry.getKey());
					final Link link = NetworkUtils.getConnectingLink(fromNode, toNode);
					node2outLinks.computeIfAbsent(fromNode, n -> new LinkedHashSet<>()).add(link);
				}
			}

			Map<Id<Node>, Double> destination2TravelTime_s = new LinkedHashMap<>();
			destination2TravelTime_s.put(job.getOriginNodeId(), 0.0);
			for (var node : job.getDestinationNodeIds()) {
				destination2TravelTime_s.put(node, lcpt.getTree().get(node).getTime());
			}
//			origin2Destination2TravelTime_s.put(job.getOriginNodeID(), destination2TravelTime_s);
		}

		void process(Job job) {
			final Set<VehicleType> compatibleVehicleTypes = this.networkAndFleetData
					.getCompatibleVehicleTypes(job.getCommodity(), job.getSamgodsMode(), job.isContainer());
			for (VehicleType vehicleType : compatibleVehicleTypes) {
				this.computeTree(job);
//				consolidationUnit.setRouteFromLinks(vehicleType, links);
			}
		}

		@Override
		public void run() {
			log.info("THREAD STARTED: " + this.name);
			try {
				while (true) {
					Job job = this.jobQueue.take();
					if (job == Job.TERMINATE) {
						break;
					}
					this.process(job);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			log.info("ROUTING THREAD ENDED: " + this.name);
		}
	}

	// -------------------- CONSTANTS AND MEMBERS --------------------

	private final NetworkAndFleetDataProvider networkAndFleetDataProvider;

	private boolean logProgress = false;

	private int maxThreads = 64;

	// -------------------- CONSTRUCTION --------------------

	public TreeBasedRouter(NetworkAndFleetDataProvider networkAndFleetDataProvider) {
		this.networkAndFleetDataProvider = networkAndFleetDataProvider;
	}

	public TreeBasedRouter setLogProgress(boolean logProgress) {
		this.logProgress = logProgress;
		return this;
	}

	public TreeBasedRouter setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
		return this;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void route(Iterable<Job> allJobs) {
		try {
			final int threadCnt = Math.min(this.maxThreads, Runtime.getRuntime().availableProcessors());
			final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<>(10 * threadCnt);
			final List<Thread> routingThreads = new ArrayList<>();

			Level level = LogManager.getLogger(Dijkstra.class).getLevel();
			Configurator.setLevel(Dijkstra.class, Level.OFF);

			log.info("Starting " + threadCnt + " routing threads.");
			for (int i = 0; i < threadCnt; i++) {
				final LeastCostPathTreeRunner routeProcessor = new LeastCostPathTreeRunner(
						LeastCostPathTreeRunner.class.getSimpleName() + i,
						this.networkAndFleetDataProvider.createDataInstance(), jobQueue);
				final Thread routingThread = new Thread(routeProcessor);
				routingThreads.add(routingThread);
				routingThread.start();
			}

			log.info("Starting to populate routing job queue, continuing as threads progress.");
			for (Job job : allJobs) {
				jobQueue.put(job);
			}

			log.info("Waiting for routing jobs to complete.");
			for (int i = 0; i < routingThreads.size(); i++) {
				jobQueue.put(Job.TERMINATE);
			}
			for (Thread routingThread : routingThreads) {
				routingThread.join();
			}

			Configurator.setLevel(Dijkstra.class, level);

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}