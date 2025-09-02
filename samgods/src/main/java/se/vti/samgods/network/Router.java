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
package se.vti.samgods.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.AStarLandmarksFactory;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.common.NetworkAndFleetData;
import se.vti.samgods.common.NetworkAndFleetDataProvider;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;

/**
 * 
 * @author GunnarF
 *
 */
public class Router {

	// -------------------- LOGGING, ONLY FOR TESTING --------------------

	private static final Logger log = LogManager.getLogger(Router.class);

	private long lastUpdate_ms = System.currentTimeMillis();

	private Set<Runnable> recentlyActiveThreads = new LinkedHashSet<>();

	private long foundRoutes = 0;
	private long failedRoutesNoOD = 0;
	private long failedRoutesNoConnection = 0;
	private long failedRoutesNoRouter = 0;

	private void msgOrNot() {
		if (System.currentTimeMillis() - this.lastUpdate_ms > 500) {
			log.info("found " + this.foundRoutes + ", failed: " + this.failedRoutesNoOD + " (no OD), "
					+ this.failedRoutesNoConnection + " (no connection), " + this.failedRoutesNoRouter + " (no router)"
					+ ", active threads: " + this.recentlyActiveThreads.size());
			this.lastUpdate_ms = System.currentTimeMillis();
			this.recentlyActiveThreads.clear();
		}
	}

	private synchronized void registerFoundRoute(Runnable thread) {
		this.msgOrNot();
		this.foundRoutes++;
		this.recentlyActiveThreads.add(thread);
	}

	private synchronized void registerFailedRouteNoOD(Runnable thread) {
		this.msgOrNot();
		this.failedRoutesNoOD++;
		this.recentlyActiveThreads.add(thread);
	}

	private synchronized void registerFailedRouteNoConnection(Runnable thread) {
		this.msgOrNot();
		this.failedRoutesNoConnection++;
		this.recentlyActiveThreads.add(thread);
	}

	private synchronized void registerFailedRouteNoRouter(Runnable thread) {
		this.msgOrNot();
		this.failedRoutesNoRouter++;
		this.recentlyActiveThreads.add(thread);
	}

	// -------------------- INNER CLASSES --------------------

	private class RouteProcessor implements Runnable {

		private final String name;
		private final NetworkAndFleetData networkAndFleetData;
		private final BlockingQueue<ConsolidationUnit> jobQueue;

		private final AStarLandmarksFactory routerFactory = new AStarLandmarksFactory(4);
		private final Map<VehicleType, LeastCostPathCalculator> vehicleType2router = new LinkedHashMap<>();

		RouteProcessor(String name, NetworkAndFleetData networkAndFleetData,
				BlockingQueue<ConsolidationUnit> jobQueue) {
			this.name = name;
			this.networkAndFleetData = networkAndFleetData;
			this.jobQueue = jobQueue;
		}

		List<Link> computeRoute(ConsolidationUnit job, VehicleType vehicleType) {

			final Network unimodalNetwork = this.networkAndFleetData.getUnimodalNetwork(vehicleType);
			if (unimodalNetwork == null) {
				log.warn("No network available. Skipping consolidationUnit " + job);
				return null;
			}

			final LeastCostPathCalculator router = this.vehicleType2router.computeIfAbsent(vehicleType, vt -> {
				final TravelDisutility travelDisutility = this.networkAndFleetData.getTravelDisutility(vehicleType);
				if (travelDisutility == null) {
					log.warn("No TravelDisutility available. Skipping consolidationUnit " + job);
					return null;
				}
				final TravelTime travelTime = this.networkAndFleetData.getTravelTime(vehicleType);
				if (travelTime == null) {
					log.warn("No TravelTime available. Skipping consolidationUnit: " + job);
					return null;
				}
				return this.routerFactory.createPathCalculator(unimodalNetwork, travelDisutility, travelTime);
			});
			if (router == null) {
				log.warn("No Router available. Skipping consolidationUnit: " + job);
				return null;
			}

			final Id<Node> fromNodeId = job.od.origin;
			final Id<Node> toNodeId = job.od.destination;
			if (fromNodeId.equals(toNodeId)) {
				if (logProgress) {
					registerFoundRoute(this);
				}
				return (new ArrayList<>());
			} else {
				final Node from = unimodalNetwork.getNodes().get(fromNodeId);
				final Node to = unimodalNetwork.getNodes().get(toNodeId);
				if ((from != null) && (to != null) && (router != null)) {
					final Path path = router.calcLeastCostPath(from, to, 0, null, null);
					if (path == null) {
						if (logProgress) {
							registerFailedRouteNoConnection(this);
						}
					} else if (logProgress) {
						registerFoundRoute(this);
					}
					return (path == null ? null : path.links);
				} else {
					if (logProgress) {
						if (from == null || to == null) {
							registerFailedRouteNoOD(this);
						} else if (router == null) {
							registerFailedRouteNoRouter(this);
						} else {
							throw new RuntimeException("impossible");
						}
					}
					return null;
				}
			}
		}

		void process(ConsolidationUnit consolidationUnit) {
			consolidationUnit.vehicleType2route.clear();
			final Set<VehicleType> compatibleVehicleTypes = this.networkAndFleetData.getCompatibleVehicleTypes(
					consolidationUnit.commodity, consolidationUnit.samgodsMode, consolidationUnit.isContainer);
			for (VehicleType vehicleType : compatibleVehicleTypes) {
				final List<Link> links = this.computeRoute(consolidationUnit, vehicleType);
				if (links != null) {
					consolidationUnit.setRouteFromLinks(vehicleType, links);
				}
			}
		}

		@Override
		public void run() {
			log.info("THREAD STARTED: " + this.name);
			try {
				while (true) {
					ConsolidationUnit job = this.jobQueue.take();
					if (job == ConsolidationUnit.TERMINATE) {
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

	public Router(NetworkAndFleetDataProvider networkAndFleetDataProvider) {
		this.networkAndFleetDataProvider = networkAndFleetDataProvider;
	}

	public Router setLogProgress(boolean logProgress) {
		this.logProgress = logProgress;
		return this;
	}

	public Router setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
		return this;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void route(Iterable<ConsolidationUnit> allJobs) {
		try {
			final int threadCnt = Math.min(this.maxThreads, Runtime.getRuntime().availableProcessors());
			final BlockingQueue<ConsolidationUnit> jobQueue = new LinkedBlockingQueue<>(10 * threadCnt);
			final List<Thread> routingThreads = new ArrayList<>();

			Level level = LogManager.getLogger(Dijkstra.class).getLevel();
			Configurator.setLevel(Dijkstra.class, Level.OFF);

			log.info("Starting " + threadCnt + " routing threads.");
			for (int i = 0; i < threadCnt; i++) {
				final RouteProcessor routeProcessor = new RouteProcessor(RouteProcessor.class.getSimpleName() + i,
						this.networkAndFleetDataProvider.createDataInstance(), jobQueue);
				final Thread routingThread = new Thread(routeProcessor);
				routingThreads.add(routingThread);
				routingThread.start();
			}

			log.info("Starting to populate routing job queue, continuing as threads progress.");
			for (ConsolidationUnit job : allJobs) {
				jobQueue.put(job);
			}

			log.info("Waiting for routing jobs to complete.");
			for (int i = 0; i < routingThreads.size(); i++) {
				jobQueue.put(ConsolidationUnit.TERMINATE);
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