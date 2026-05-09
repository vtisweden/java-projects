/**
 * se.vti.roundtrips.parallel
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
package se.vti.roundtrips.single;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripProposalParameters {

	public final double insertWeight;
	public final double removeWeight;
	public final double flipLocationWeight;
	public final double flipDepTimeWeight;

	public RoundTripProposalParameters(double insertWeight, double removeWeight, double flipLocationWeight,
			double flipDepTimeWeight) {
		this.insertWeight = insertWeight;
		this.removeWeight = removeWeight;
		this.flipLocationWeight = flipLocationWeight;
		this.flipDepTimeWeight = flipDepTimeWeight;
	}

	public RoundTripProposalParameters() {
		this(1.0, 1.0, 1.0, 1.0);
	}
}
