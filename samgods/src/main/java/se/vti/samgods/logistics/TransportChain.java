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
package se.vti.samgods.logistics;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants.Commodity;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportChain {

	// -------------------- CONSTANTS --------------------

	private final Commodity commodity;

	private final boolean isContainer;

	private final LinkedList<TransportEpisode> episodes = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	public TransportChain(Commodity commodity, boolean isContainer) {
		this.commodity = commodity;
		this.isContainer = isContainer;
	}

	public void addEpisode(final TransportEpisode episode) {
		episode.setParent(this);
		this.episodes.add(episode);
	}

	// -------------------- IMPLEMENTATION --------------------

	public Commodity getCommodity() {
		return this.commodity;
	}

	public boolean isContainer() {
		return this.isContainer;
	}

	public LinkedList<TransportEpisode> getEpisodes() {
		return this.episodes;
	}

	public OD getOD() {
		if (this.episodes.size() == 0) {
			return null;
		} else {
			return new OD(this.episodes.getFirst().getLoadingNodeId(), this.episodes.getLast().getUnloadingNodeId());
		}
	}

	public boolean isRouted() {
		for (TransportEpisode episode : this.episodes) {
			if (!episode.isRouted()) {
				return false;
			}
		}
		return true;
	}

	// -------------------- OVERRIDING OF OBJECT --------------------

	@Override
	public String toString() {
		final List<String> content = new LinkedList<>();
		content.add("commodity=" + this.commodity);
		content.add("isContainer=" + this.isContainer);
		content.add("numberOfEpisodes=" + (this.getEpisodes() != null ? this.getEpisodes().size() : null));
		content.add("od=" + this.getOD());
		content.add("isRouted=" + this.isRouted());
		return this.getClass().getSimpleName() + "[" + content.stream().collect(Collectors.joining(",")) + "]";
	}

}
