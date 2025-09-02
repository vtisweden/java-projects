/**
 * se.vti.samgods
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
package se.vti.samgods.common;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "samgods";

	public SamgodsConfigGroup() {
		super(GROUP_NAME);
	}

	public static SamgodsConfigGroup createFromFile(String fileName) {
		return ConfigUtils.addOrGetModule(ConfigUtils.loadConfig(fileName), SamgodsConfigGroup.class);
	}

	//

	private Integer maxIterations = null;

	@StringGetter("maxIterations")
	public Integer getMaxIterations() {
		return this.maxIterations;
	}

	@StringSetter("maxIterations")
	public void setMaxIterations(Integer maxIterations) {
		this.maxIterations = maxIterations;
	}

	//

	private String ascSourceFileName = null;

	@StringGetter("ascSourceFileName")
	public String getAscSourceFileName() {
		return this.ascSourceFileName;
	}

	@StringSetter("ascSourceFileName")
	public void setAscSourceFileName(String ascSourceFileName) {
		this.ascSourceFileName = ascSourceFileName;
	}

	//

	private Double ascCalibrationStepSize = null;

	@StringGetter("ascCalibrationStepSize")
	public Double getAscCalibrationStepSize() {
		return this.ascCalibrationStepSize;
	}

	@StringSetter("ascCalibrationStepSize")
	public void setAscCalibrationStepSize(Double ascCalibrationStepSize) {
		this.ascCalibrationStepSize = ascCalibrationStepSize;
	}

	//

	private String railVehicleParametersFileName = null;

	@StringGetter("railVehicleParametersFileName")
	public String getRailVehicleParametersFileName() {
		return this.railVehicleParametersFileName;
	}

	@StringSetter("railVehicleParametersFileName")
	public void setRailVehicleParametersFileName(String railVehicleParametersFileName) {
		this.railVehicleParametersFileName = railVehicleParametersFileName;
	}

	//

	private String roadVehicleParametersFileName = null;

	@StringGetter("roadVehicleParametersFileName")
	public String getRoadVehicleParametersFileName() {
		return this.roadVehicleParametersFileName;
	}

	@StringSetter("roadVehicleParametersFileName")
	public void setRoadVehicleParametersFileName(String roadVehicleParametersFileName) {
		this.roadVehicleParametersFileName = roadVehicleParametersFileName;
	}

	//

	private String seaVehicleParametersFileName = null;

	@StringGetter("seaVehicleParametersFileName")
	public String getSeaVehicleParametersFileName() {
		return this.seaVehicleParametersFileName;
	}

	@StringSetter("seaVehicleParametersFileName")
	public void setSeaVehicleParametersFileName(String seaVehicleParametersFileName) {
		this.seaVehicleParametersFileName = seaVehicleParametersFileName;
	}

	//

	private String railTransferParametersFileName = null;

	@StringGetter("railTransferParametersFileName")
	public String getRailTransferParametersFileName() {
		return this.railTransferParametersFileName;
	}

	@StringSetter("railTransferParametersFileName")
	public void setRailTransferParametersFileName(String railTransferParametersFileName) {
		this.railTransferParametersFileName = railTransferParametersFileName;
	}

	//

	private String roadTransferParametersFileName = null;

	@StringGetter("roadTransferParametersFileName")
	public String getRoadTransferParametersFileName() {
		return this.roadTransferParametersFileName;
	}

	@StringSetter("roadTransferParametersFileName")
	public void setRoadTransferParametersFileName(String roadTransferParametersFileName) {
		this.roadTransferParametersFileName = roadTransferParametersFileName;
	}

	//

	private String seaTransferParametersFileName = null;

	@StringGetter("seaTransferParametersFileName")
	public String getSeaTransferParametersFileName() {
		return this.seaTransferParametersFileName;
	}

	@StringSetter("seaTransferParametersFileName")
	public void setSeaTransferParametersFileName(String seaTransferParametersFileName) {
		this.seaTransferParametersFileName = seaTransferParametersFileName;
	}

	//

	private String networkNodesFileName = null;

	@StringGetter("networkNodesFileName")
	public String getNetworkNodesFileName() {
		return this.networkNodesFileName;
	}

	@StringSetter("networkNodesFileName")
	public void setNetworkNodesFileName(String networkNodesFileName) {
		this.networkNodesFileName = networkNodesFileName;
	}

	//

	private String networkLinksFileName = null;

	@StringGetter("networkLinksFileName")
	public String getNetworkLinksFileName() {
		return this.networkLinksFileName;
	}

	@StringSetter("networkLinksFileName")
	public void setNetworkLinksFileName(String networkLinksFileName) {
		this.networkLinksFileName = networkLinksFileName;
	}

	//

	private String consolidationUnitsFileName = null;

	@StringGetter("consolidationUnitsFileName")
	public String getConsolidationUnitsFileName() {
		return this.consolidationUnitsFileName;
	}

	@StringSetter("consolidationUnitsFileName")
	public void setConsolidationUnitsFileName(String consolidationUnitsFileName) {
		this.consolidationUnitsFileName = consolidationUnitsFileName;
	}

	//

	private String chainChoiPrefix = null;

	@StringGetter("chainChoiPrefix")
	public String getChainChoiPrefix() {
		return this.chainChoiPrefix;
	}

	@StringSetter("chainChoiPrefix")
	public void setChainChoiPrefix(String chainChoiPrefix) {
		this.chainChoiPrefix = chainChoiPrefix;
	}

	//

	private String chainChoiSuffix = null;

	@StringGetter("chainChoiSuffix")
	public String getChainChoiSuffix() {
		return this.chainChoiSuffix;
	}

	@StringSetter("chainChoiSuffix")
	public void setChainChoiSuffix(String chainChoiSuffix) {
		this.chainChoiSuffix = chainChoiSuffix;
	}
	
	// 
	
	public String getChainChoiFileName(SamgodsConstants.Commodity commodity)  {
		return this.chainChoiPrefix + commodity.twoDigitCode() + this.chainChoiSuffix;
	}

	//

	public static void main(String[] args) {

//		Config config = ConfigUtils.createConfig();
//		ConfigUtils.addOrGetModule(config, SamgodsConfigGroup.class);
//		ConfigUtils.writeConfig(config, "config.xml");

		SamgodsConfigGroup config = SamgodsConfigGroup.createFromFile("config.xml");
		System.out.println("DONE");
	}

}
