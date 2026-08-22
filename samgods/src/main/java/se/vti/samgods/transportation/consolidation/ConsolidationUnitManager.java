package se.vti.samgods.transportation.consolidation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants.CommodityMode;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportEpisode;

/**
 * Several episodes may have consolidation units with the same routes. To reduce
 * routing effort, we collect here, for each possible routing configuration, one
 * representative consolidation unit to be routed.
 * 
 * @author GunnarF
 */
public class ConsolidationUnitManager {

	private record CommodityModeOD(CommodityMode commodityMode, OD od) {
		CommodityModeOD(ConsolidationUnit consolidationUnit) {
			this(new CommodityMode(consolidationUnit), consolidationUnit.od);
		}
	}

    private final Map<ConsolidationUnit, ConsolidationUnit> pattern2Representative =
            new LinkedHashMap<>();
    
	private final Map<CommodityModeOD, Set<ConsolidationUnit>> commodityModeOD2ConsolidationUnits = new LinkedHashMap<>();

    public ConsolidationUnitManager() {
    }
    
    public Set<ConsolidationUnit> getConsolidationUnits(CommodityMode commodityMode, OD od) {
    	return this.commodityModeOD2ConsolidationUnits.get(new CommodityModeOD(commodityMode, od));
    }
    
    public void registerAll(Set<ConsolidationUnit> consolidationUnits) {
    	consolidationUnits.stream().forEach(cu -> this.registerAndReturnRepresentative(cu));
    }

    public ConsolidationUnit registerAndReturnRepresentative(ConsolidationUnit unit) {
		var representative = this.pattern2Representative.computeIfAbsent(unit,  u -> unit);
    	var key = new CommodityModeOD(representative);
    	this.commodityModeOD2ConsolidationUnits.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(representative);
		return representative;
    }

    public void populateWithTemplateConsolidationUnits(TransportChain chain) {
    	for (TransportEpisode episode : chain.getEpisodes()) {
    		if (episode.getConsolidationUnits() != null) {
    			throw new RuntimeException("Episode already contains consolidation units!");
    		}
    		var units = ConsolidationUnit.createUnrouted(episode);
			List<ConsolidationUnit> templates = new ArrayList<>(units.size());
    		for (ConsolidationUnit unit : units) {
		        templates.add(this.registerAndReturnRepresentative(unit));
			}    		
    		episode.setConsolidationUnits(templates);
		}
    }

    public Collection<ConsolidationUnit> getAllRepresentativeConsolidationUnits() {
        return this.pattern2Representative.values();
    }
}