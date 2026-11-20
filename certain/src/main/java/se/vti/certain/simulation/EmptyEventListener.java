/**
 * se.vti.certain
 * 
 * Copyright (C) 2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.certain.simulation;

import java.util.function.Consumer;

import se.vti.certain.simulation.missionimplementation.Event;
import se.vti.certain.simulation.missionimplementation.IncidentHappensEvent;
import se.vti.certain.simulation.missionimplementation.VehicleRequestedEvent;

/**
 * @author GunnarF
 */
public class EmptyEventListener implements Consumer<Event> {

	public EmptyEventListener() {

	}

	@Override
	public void accept(Event event) {

		if (event instanceof IncidentHappensEvent ihe) {
			this.handleIncidentHappensEvent(ihe);
		} else if (event instanceof VehicleRequestedEvent vre) {
			this.handleVehicleRequestedEvent(vre);
		} else {
			this.handleUnknownEvent(event);
		}
	}

	public void handleIncidentHappensEvent(IncidentHappensEvent event) {
	}

	public void handleVehicleRequestedEvent(VehicleRequestedEvent event) {
	}

	public void handleUnknownEvent(Event event) {
	}

}
