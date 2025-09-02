/**
 * se.vti.samgods.transportation.consolidation
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
package se.vti.samgods.transportation.consolidation;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * 
 * @author GunnarF
 *
 */
class TestConsolidationUnit {

	final VehicleType truck1 = VehicleUtils.createVehicleType(Id.create("truck1", VehicleType.class));
	final VehicleType truck2 = VehicleUtils.createVehicleType(Id.create("truck2", VehicleType.class));
	final VehicleType truck3 = VehicleUtils.createVehicleType(Id.create("truck3", VehicleType.class));
	final VehicleType truck4 = VehicleUtils.createVehicleType(Id.create("truck4", VehicleType.class));

	final Id<Link> link1 = Id.createLinkId("link1");
	final Id<Link> link2 = Id.createLinkId("link2");
	final Id<Link> link3 = Id.createLinkId("link3");

	final List<Id<Link>> route0 = Arrays.asList();
	final List<Id<Link>> route12 = Arrays.asList(link1, link2);
	final List<Id<Link>> route23 = Arrays.asList(link2, link3);
	final List<Id<Link>> route123 = Arrays.asList(link1, link2, link3);

	ConsolidationUnit cu = null;

	@BeforeEach
	void init() {
		this.cu = new ConsolidationUnit(null, null, null, null);
	}

	@Test
	void testRouteManagement1() {
		cu.setRouteFromLinkIds(truck1, route0);
		cu.setRouteFromLinkIds(truck2, route12);
		cu.setRouteFromLinkIds(truck3, route23);
		cu.setRouteFromLinkIds(truck4, route123);
		Assert.equals(route0, cu.getRoute(truck1));
		Assert.equals(route12, cu.getRoute(truck2));
		Assert.equals(route23, cu.getRoute(truck3));
		Assert.equals(route123, cu.getRoute(truck4));
	}

	@Test
	void testRouteManagement2() {
		cu.setRouteFromLinkIds(truck1, route0);
		cu.setRouteFromLinkIds(truck1, route12);
		Assert.equals(1, cu.distinctRoutes());
		
		cu.setRouteFromLinkIds(truck2, route23);
		cu.setRouteFromLinkIds(truck2, route123);
		Assert.equals(2, cu.distinctRoutes());
		
		Assert.equals(route12, cu.getRoute(truck1));
		Assert.equals(route123, cu.getRoute(truck2));
		Assert.isTrue(cu.getRoute(truck3) == null);
		Assert.isTrue(cu.getRoute(truck4) == null);
	}

	@Test
	void testRouteManagement3() {
		cu.setRouteFromLinkIds(truck1, route12);
		cu.setRouteFromLinkIds(truck2, route12);
		Assert.equals(1, cu.distinctRoutes());
		
		cu.setRouteFromLinkIds(truck3, route23);
		cu.setRouteFromLinkIds(truck4, route23);
		Assert.equals(2, cu.distinctRoutes());
		
		Assert.equals(route12, cu.getRoute(truck1));
		Assert.equals(route12, cu.getRoute(truck2));
		Assert.equals(route23, cu.getRoute(truck3));
		Assert.equals(route23, cu.getRoute(truck4));
	}
}
