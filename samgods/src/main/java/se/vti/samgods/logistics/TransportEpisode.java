/**
 * se.vti.samgods.logistics
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
package se.vti.samgods.logistics;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants.Commodity;
import se.vti.samgods.common.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportEpisode {

	// -------------------- MEMBERS --------------------

	private final TransportMode mode;

	private final LinkedList<OD> segmentODs = new LinkedList<>();

	private TransportChain parent = null;

	private List<ConsolidationUnit> consolidationUnits = null;

	// -------------------- CONSTRUCTION/COMPOSITION --------------------

	public TransportEpisode(TransportMode mode) {
		this.mode = mode;
	}

	// Package private -- called by TransportChain when adding an Episode.
	void setParent(TransportChain parent) {
		this.parent = parent;
	}

	public void addSegmentOD(OD od) {
		this.segmentODs.add(od);
	}

	public void setConsolidationUnits(List<ConsolidationUnit> consolidationUnits) {
		this.consolidationUnits = consolidationUnits;
	}

	// -------------------- IMPLEMENTATION --------------------

	public TransportMode getMode() {
		return this.mode;
	}

	public LinkedList<OD> getSegmentODs() {
		return this.segmentODs;
	}

	public Id<Node> getLoadingNodeId() {
		if (this.segmentODs.size() == 0) {
			return null;
		} else {
			return this.segmentODs.getFirst().origin;
		}
	}

	public Id<Node> getUnloadingNodeId() {
		if (this.segmentODs.size() == 0) {
			return null;
		} else {
			return this.segmentODs.getLast().destination;
		}
	}
	
	public OD getLoadingUnloadingOD() {
		return new OD(this.getLoadingNodeId(), this.getUnloadingNodeId());
	}

	public Commodity getCommodity() {
		if (this.parent == null) {
			return null;
		} else {
			return parent.getCommodity();
		}
	}

	public Boolean isContainer() {
		if (this.parent == null) {
			return null;
		} else {
			return this.parent.isContainer();
		}
	}

	public List<ConsolidationUnit> getConsolidationUnits() {
		return this.consolidationUnits;
	}

	public boolean isRouted() {
		return ((this.consolidationUnits != null) && (this.consolidationUnits.stream()
				.allMatch(cu -> (cu != null) && (cu.vehicleType2route.size() > 0))));
	}

	// -------------------- OVERRIDING OF OBJECT --------------------

	@Override
	public String toString() {
		final List<String> content = new LinkedList<>();
		content.add("mode=" + this.mode);
		content.add("isRouted=" + this.isRouted());
		content.add("segmentOds={" + (this.segmentODs != null
				? this.segmentODs.stream().map(od -> od.toString()).collect(Collectors.joining(",")) + "}"
				: null));
		content.add("consolidationUnits=" + (this.consolidationUnits != null
				? "{" + this.consolidationUnits.stream().map(cu -> cu.toString()).collect(Collectors.joining(",")) + "}"
				: null));
		return this.getClass().getSimpleName() + "[" + content.stream().collect(Collectors.joining(",")) + "]"
				+ ", parent=" + (this.parent != null ? this.parent.toString() : null);
	}

}
