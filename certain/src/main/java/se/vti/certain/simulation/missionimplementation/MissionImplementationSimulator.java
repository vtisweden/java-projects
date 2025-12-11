/**
 * se.vti.certain
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
package se.vti.certain.simulation.missionimplementation;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import se.vti.certain.datastructures.Mission;
import se.vti.certain.datastructures.Vehicle;
import se.vti.certain.spatial.Distances;

/**
 * 
 * @author GunnarF
 *
 */
public class MissionImplementationSimulator {

	private final Map<String, Vehicle> id2Vehicle;

	private final Distances distances;

	private boolean verbose = true;
	
	private double relSOCWhenAvailable = 1.0;
	
	public MissionImplementationSimulator(Map<String, Vehicle> id2Vehicle, Distances distances) {
		this.id2Vehicle = id2Vehicle;
		this.distances = distances;
	}

	public MissionImplementationSimulator setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	public MissionImplementationSimulator setRelSOCWhenAvailable(double relSOCWhenAvailable) {
		this.relSOCWhenAvailable = relSOCWhenAvailable;
		return this;
	}
	
	private boolean canBeSimulated(Mission mission) {
		return true;
	}

	public SystemState simulateMissionImplementation(List<Mission> missions) {

		double time_h = 0.0;
		PriorityQueue<Event> eventQueue = new PriorityQueue<>(
				(e1, e2) -> Double.compare(e1.getStartTime_h(), e2.getStartTime_h()));
		missions.stream().filter(m -> this.canBeSimulated(m)).forEach(m -> eventQueue.add(new IncidentHappensEvent(m)));

		SystemState systemState = new SystemState(this.id2Vehicle, this.distances, this.relSOCWhenAvailable);

		while (!eventQueue.isEmpty()) {
			Event nextEvent = eventQueue.poll();
			time_h = nextEvent.getStartTime_h();
			if (this.verbose) {
				System.out.println(time_h + ": " + nextEvent);
			}
			List<Event> newEvents = nextEvent.process(systemState);
			eventQueue.addAll(newEvents);
		}

		return systemState;
	}
}
