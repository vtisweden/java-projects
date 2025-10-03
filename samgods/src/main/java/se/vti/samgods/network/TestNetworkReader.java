/**
 * se.vti.samgods.network
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
package se.vti.samgods.network;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.matsim.api.core.v01.network.Network;

/**
 * 
 * @author GunnarF
 *
 */
public class TestNetworkReader {

	private String pathName = null;

	private String nodesFileName = null;
	private String linksFileName = null;

	public TestNetworkReader() {
	}

	public TestNetworkReader setPathName(String pathName) {
		this.pathName = pathName;
		return this;
	}

	public TestNetworkReader setNodesFileName(String nodesFileName) {
		this.nodesFileName = nodesFileName;
		return this;
	}

	public TestNetworkReader setLinksFileName(String linksFileName) {
		this.linksFileName = linksFileName;
		return this;
	}

	private Path getFile(String fileName) {
		if (fileName == null) {
			return null;
		} else {
			return fileName != null ? Paths.get(this.pathName, fileName) : Paths.get(fileName);
		}
	}

	public void run() throws IOException {
		Network network = new NetworkReader().setMinSpeed_km_h(1.0).load(this.getFile(this.nodesFileName).toString(),
				this.getFile(this.linksFileName).toString());
		System.out.println();
		System.out.println(NetworkStatsTable.create(network));
	}

	public static void main(String[] args) throws IOException {
		new TestNetworkReader().setPathName("./input_2024/").setNodesFileName("node_parameters.csv")
				.setLinksFileName("link_parameters.csv").run();
	}
}
