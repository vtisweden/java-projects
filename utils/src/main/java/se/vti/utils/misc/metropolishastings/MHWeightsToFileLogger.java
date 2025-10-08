/**
 * se.vti.roundtrips
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
package se.vti.utils.misc.metropolishastings;

/**
 * 
 * @author GunnarF
 *
 */
public class MHWeightsToFileLogger<R> extends MHToFileLogger<R> {

	final MHWeightContainer<R> samplingWeights;

	public MHWeightsToFileLogger(long samplingInterval, MHWeightContainer<R> samplingWeights, String logFileName) {
		super(samplingInterval, logFileName);
		this.samplingWeights = samplingWeights;
	}

	@Override
	public String createHeaderLine() {
		StringBuffer result = new StringBuffer("Iteration");
		for (MHWeight<R> samplingWeight : this.samplingWeights.getComponentsView()) {
			result.append("\t");
			result.append(samplingWeight.name());
		}
		return result.toString();
	}

	@Override
	public String createDataLine(R state) {
		StringBuffer result = new StringBuffer(Long.toString(this.iteration()));
		for (MHWeight<R> samplingWeight : this.samplingWeights.getComponentsView()) {
			result.append("\t");
			result.append(samplingWeight.logWeight(state));
		}
		return result.toString();
	}
}
