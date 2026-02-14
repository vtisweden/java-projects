/**
 * se.vti.roundtrips.common
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
package se.vti.roundtrips.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author GunnarF
 */
public class SpecificationChecker {

	private List<Supplier<Boolean>> errorConditions = new ArrayList<>();
	private List<String> errorMessages = new ArrayList<>();

	private List<String> recentErrors = null;

	public SpecificationChecker() {
	}

	public SpecificationChecker defineError(Supplier<Boolean> errorCondition, String errorMessage) {
		this.errorConditions.add(errorCondition);
		this.errorMessages.add(errorMessage);
		return this;
	}

	public boolean check() {
		this.recentErrors = new ArrayList<>();
		for (int i = 0; i < this.errorConditions.size(); i++) {
			if (this.errorConditions.get(i).get()) {
				this.recentErrors.add(this.errorMessages.get(i));
			}
		}
		return this.recentErrors.isEmpty();
	}

	public String getRecentErrors() {
		if (this.recentErrors.isEmpty()) {
			return null;
		} else {
			return this.recentErrors.stream().collect(Collectors.joining("\n"));
		}
	}
}
