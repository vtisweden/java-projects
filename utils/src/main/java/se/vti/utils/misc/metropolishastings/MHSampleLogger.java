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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 
 * @author GunnarF
 *
 */
public class MHSampleLogger<X> extends MHToFileLogger<X> {

	private Map<String, Function<X, Double>> name2Extractor = new LinkedHashMap<>();

	public MHSampleLogger(long logInterval, String logFileName) {
		super(logInterval, logFileName);
	}

	public void add(String name, Function<X, Double> sampleExtractor) {
		this.name2Extractor.put(name, sampleExtractor);
	}

	@Override
	public String createHeaderLine() {
		StringBuffer result = new StringBuffer(super.createHeaderLine());
		for (var name : this.name2Extractor.keySet()) {
			result.append("\t");
			result.append(name);
		}
		return result.toString();
	}

	@Override
	public String createDataLine(X state, double logWeight) {
		StringBuffer result = new StringBuffer(super.createDataLine(state, logWeight));
		for (var extractor : this.name2Extractor.values()) {
			result.append(String.format(Locale.US, "\t%f", extractor.apply(state)));
		}
		return result.toString();
	}
}
