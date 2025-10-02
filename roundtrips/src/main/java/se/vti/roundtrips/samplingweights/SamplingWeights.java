/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023,2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.samplingweights;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class SamplingWeights<X> implements MHWeight<X> {

	private List<MHWeight<X>> components = new ArrayList<>();

	private List<Double> weights = new ArrayList<>();

	public SamplingWeights() {
	}

	public List<MHWeight<X>> getComponentsView() {
		return Collections.unmodifiableList(this.components);
	}
	
	public void add(MHWeight<X> component, double weight) {
		this.components.add(component);
		this.weights.add(weight);
	}

	public void add(MHWeight<X> component) {
		this.add(component, 1.0);
	}

	@Override
	public double logWeight(X state) {
		double result = 0.0;
		for (int i = 0; i < this.components.size(); i++) {
			result += this.weights.get(i) * this.components.get(i).logWeight(state);
		}
		return result;
	}
}
