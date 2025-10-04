/**
 * se.vti.roundtrips.samplingweights.misc.timeUse
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
package se.vti.roundtrips.samplingweights.misc.timeUse;

import java.util.Arrays;
import java.util.List;

import se.vti.roundtrips.simulator.StayEpisode;
import se.vti.utils.misc.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class LogarithmicTimeUseComponent {

	final double targetDuration_h;
	final double period_h;

	private List<Tuple<Double, Double>> openInterval_h;
	private double minEnBlockDurationAtLeastOnce_h;
	private double minEnBlockDurationEachTime_h;

	private boolean valid = false;
	private double effectiveDurationSum_h = 0;

	// Use factory in LogarithmicTimeUse.
	LogarithmicTimeUseComponent(double targetDuration_h, double period_h) {
		this.targetDuration_h = targetDuration_h;
		this.period_h = period_h;
		this.openInterval_h = Arrays.asList(new Tuple<>(0.0, period_h));
		this.minEnBlockDurationAtLeastOnce_h = 0.0;
		this.minEnBlockDurationEachTime_h = 0.0;
	}

	public LogarithmicTimeUseComponent setOpeningTimes_h(double start_h, double end_h) {
		if (start_h < end_h) {
			this.openInterval_h = Arrays.asList(new Tuple<>(start_h, end_h));
		} else {
			// wraparound
			this.openInterval_h = Arrays.asList(new Tuple<>(0.0, end_h), new Tuple<>(start_h, this.period_h));				
		}
		return this;
	}

	public LogarithmicTimeUseComponent setMinEnBlockDurationAtLeastOnce_h(double dur_h) {
		this.minEnBlockDurationAtLeastOnce_h = dur_h;
		return this;
	}

	public LogarithmicTimeUseComponent setMinEnBlockDurationEachTime_h(double dur_h) {
		this.minEnBlockDurationEachTime_h = dur_h;
		return this;
	}

	void resetEffectiveDuration_h() {
		this.valid = false;
		this.effectiveDurationSum_h = 0;
	}

	void update(StayEpisode<?> stay) {
		double effectiveDuration_h = stay.overlap_h(this.openInterval_h, this.period_h);
		if (effectiveDuration_h >= this.minEnBlockDurationEachTime_h) {
			this.effectiveDurationSum_h += effectiveDuration_h;
			this.valid = this.valid || (effectiveDuration_h >= this.minEnBlockDurationAtLeastOnce_h);
		}
	}

	public double getEffectiveDuration_h() {
		return this.valid ? this.effectiveDurationSum_h : 0.0;
	}

}
