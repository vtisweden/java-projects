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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.common.NetworkAndFleetData;
import se.vti.samgods.common.NetworkAndFleetDataProvider;
import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.utils.misc.math.MathHelpers;

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

	private static int done = 0;
	private static Integer total = null;
	private static double nextOutput;

	private static synchronized void resetCounter(int total) {
		done = 0;
		nextOutput = 5; // 5 %.
		TreeBasedRouter.total = total;
	}

	private static synchronized void incDone() {
		done++;
		double progress = MathHelpers.singleton().round(100.0 * done / total, 3);
		if (progress >= nextOutput) {
			log.info(progress + "% done");
			nextOutput += 5; // Add 5 %.
		}
	}

	static class Job {
		final static Job TERMINATE = new Job(null); // poison pill

		static record Key(Commodity commodity, SamgodsConstants.TransportMode samgodsMode, Boolean isContainer) {
		};

		final Key key;

		final Map<OD, List<ConsolidationUnit>> od2ConsolidationUnits = new LinkedHashMap<>();

		Job(Key key) {
			this.key = key;
		}

		void addConsolidationUnit(ConsolidationUnit cu) {
			assert (this.key.commodity.equals(cu.commodity));
			assert (this.key.samgodsMode.equals(cu.samgodsMode));
			assert (this.key.isContainer.equals(cu.isContainer));
			this.od2ConsolidationUnits.computeIfAbsent(cu.od, od -> new ArrayList<>()).add(cu);
		}

		Set<Id<Node>> computeOriginIds() {
			return this.od2ConsolidationUnits.keySet().stream().map(od -> od.origin).collect(Collectors.toSet());
		}

		Set<Id<Node>> computeDestinationIds() {
			return this.od2ConsolidationUnits.keySet().stream().map(od -> od.destination).collect(Collectors.toSet());
		}

		Commodity getCommodity() {
			return this.key.commodity;
		}

		SamgodsConstants.TransportMode getSamgodsMode() {
			return this.key.samgodsMode;
		}

		boolean isContainer() {
			return this.key.isContainer;
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

		/* TODO */ void process(Job job) {

			final Set<VehicleType> compatibleVehicleTypes = this.networkAndFleetData
					.getCompatibleVehicleTypes(job.getCommodity(), job.getSamgodsMode(), job.isContainer());
			for (VehicleType vehicleType : compatibleVehicleTypes) {

				final Network unimodalNetwork = this.networkAndFleetData.getUnimodalNetwork(vehicleType);
				if (unimodalNetwork == null) {
					log.warn("No network available. Skipping job: " + job);
//					return null;
				}

				final LeastCostPathTree lcpt = this.vehicleType2leastCostPathTree.computeIfAbsent(vehicleType, vt -> {
					final TravelDisutility travelDisutility = this.networkAndFleetData.getTravelDisutility(vehicleType);
					final TravelTime travelTime = this.networkAndFleetData.getTravelTime(vehicleType);
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
//					return null;
				}

				Set<Id<Node>> originIds = job.computeOriginIds();
				Set<Id<Node>> destinationIds = job.computeDestinationIds();

				for (Id<Node> originId : originIds) {
					
					Node origin = unimodalNetwork.getNodes().get(originId);
					lcpt.calculate(unimodalNetwork, origin, 0.0);

					final Map<Node, Link> node2inLinks = new LinkedHashMap<>();
					for (Map.Entry<Id<Node>, LeastCostPathTree.NodeData> entry : lcpt.getTree().entrySet()) {
						if (entry.getValue().getPrevNodeId() != null) {
							final Node fromNode = unimodalNetwork.getNodes().get(entry.getValue().getPrevNodeId());
							final Node toNode = unimodalNetwork.getNodes().get(entry.getKey());
							final Link link = NetworkUtils.getConnectingLink(fromNode, toNode);
							node2inLinks.put(toNode, link);
						}
					}

					for (Id<Node> destinationId : destinationIds) {
						LinkedList<Link> route = new LinkedList<>();
						Node currentNode = unimodalNetwork.getNodes().get(destinationId);
						while (currentNode != origin) {							
							Link link = node2inLinks.get(currentNode);
							route.addFirst(link);
							currentNode = link.getFromNode();
						}					
						for (ConsolidationUnit cu : job.od2ConsolidationUnits.get(new OD(originId, destinationId))) {
							cu.setRouteFromLinks(vehicleType, route);
						}
					}
				}
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
					incDone();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			log.info("ROUTING THREAD ENDED: " + this.name);
		}
	}

	// -------------------- CONSTANTS AND MEMBERS --------------------

	private final NetworkAndFleetDataProvider networkAndFleetDataProvider;

	private int maxThreads = 64;

	// -------------------- CONSTRUCTION --------------------

	public TreeBasedRouter(NetworkAndFleetDataProvider networkAndFleetDataProvider) {
		this.networkAndFleetDataProvider = networkAndFleetDataProvider;
	}

	public TreeBasedRouter setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
		return this;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void routeInternally(Collection<Job> allJobs) {
		try {
			
			resetCounter(allJobs.size());
			
			final int threadCnt = Math.min(this.maxThreads, Runtime.getRuntime().availableProcessors());
			final BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<>(10 * threadCnt);
			final List<Thread> routingThreads = new ArrayList<>();

			log.info("Starting " + threadCnt + " tree routing threads.");
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

			log.info("Waiting for tree routing jobs to complete.");
			for (int i = 0; i < routingThreads.size(); i++) {
				jobQueue.put(Job.TERMINATE);
			}
			for (Thread routingThread : routingThreads) {
				routingThread.join();
			}

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void route(Iterable<ConsolidationUnit> consolidationUnits) {
		Map<Job.Key, Job> key2job = new LinkedHashMap<>();
		for (ConsolidationUnit cu : consolidationUnits) {
			Job.Key key = new Job.Key(cu.commodity, cu.samgodsMode, cu.isContainer);
			key2job.computeIfAbsent(key, k -> new Job(key)).addConsolidationUnit(cu);
		}
		this.routeInternally(key2job.values());
	}

}