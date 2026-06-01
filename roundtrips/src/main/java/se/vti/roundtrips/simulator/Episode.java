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
package se.vti.roundtrips.simulator;

import java.util.Arrays;
import java.util.List;

import se.vti.utils.misc.Tuple;
import se.vti.utils.misc.math.MathHelpers;

/**
 * 
 * @author GunnarF
 *
 */
public class Episode {

	private final MathHelpers mathHelpers = new MathHelpers();

	private Double duration_h = null;
	private Double end_h = null;

	private SimulatorState initialState = null;
	private SimulatorState finalState = null;

	// TODO NEW
	public void deepCopyInto(Episode target) {
		target.duration_h = this.duration_h;
		target.end_h = this.end_h;
		target.initialState = (this.initialState == null ? null : this.initialState.clone());
		target.finalState = (this.finalState == null ? null : this.finalState.clone());
	}

	// TODO NEW
	@Override
	public Episode clone() {
		Episode result = new Episode();
		this.deepCopyInto(result);
		return result;
	}

	public Episode() {
	}

	public Double getEndTime_h() {
		return end_h;
	}

	public void setEndTime_h(Double end_h) {
		this.end_h = end_h;
	}

	public Double getDuration_h() {
		return duration_h;
	}

	public void setDuration_h(Double duration_h) {
		this.duration_h = duration_h;
	}

	public Double computeStartTime_h(double period_h) {
		if (this.duration_h == null || this.end_h == null) {
			return null;
		}
		double start_h = this.end_h - this.duration_h;
		while (start_h < 0.0) {
			start_h += period_h;
		}
		while (start_h >= period_h) {
			start_h -= period_h;
		}
		return start_h;
	}

	public Object getInitialState() {
		return initialState;
	}

	public void setInitialState(SimulatorState initialState) {
		this.initialState = initialState;
	}

	public SimulatorState getFinalState() {
		return finalState;
	}

	public void setFinalState(SimulatorState finalState) {
		this.finalState = finalState;
	}

//	public List<Tuple<Double, Double>> effectiveIntervals() {
//		assert (this.duration_h >= 0.0);
//		if (this.duration_h > 24.0) {
//			return Arrays.asList(new Tuple<>(0.0, 24.0));
//		} else {
//			double withinDayEnd_h = this.end_h;
//			while (withinDayEnd_h < 0.0) {
//				withinDayEnd_h += 24.0;
//			}
//			while (withinDayEnd_h > 24.0) {
//				withinDayEnd_h -= 24.0;
//			}
//			double start_h = withinDayEnd_h - this.duration_h;
//			if (start_h < 0.0) {
//				return Arrays.asList(new Tuple<>(start_h + 24.0, 24.0), new Tuple<>(0.0, withinDayEnd_h));
//			} else {
//				return Arrays.asList(new Tuple<>(start_h, withinDayEnd_h));
//			}
//		}
//	}

	public List<Tuple<Double, Double>> effectiveIntervals(double periodLength_h) {
		assert (this.duration_h >= 0.0);
		if (this.duration_h > periodLength_h) {
			return Arrays.asList(new Tuple<>(0.0, periodLength_h));
		} else {
			double withinPeriodEnd_h = this.end_h;
			while (withinPeriodEnd_h < 0.0) {
				withinPeriodEnd_h += periodLength_h;
			}
			while (withinPeriodEnd_h > periodLength_h) {
				withinPeriodEnd_h -= periodLength_h;
			}
			double start_h = withinPeriodEnd_h - this.duration_h;
			if (start_h < 0.0) {
				return Arrays.asList(new Tuple<>(start_h + periodLength_h, periodLength_h),
						new Tuple<>(0.0, withinPeriodEnd_h));
			} else {
				return Arrays.asList(new Tuple<>(start_h, withinPeriodEnd_h));
			}
		}
	}

	public double overlap_h(List<Tuple<Double, Double>> intervals, double periodLength_h) {
		double overlap_h = 0.0;
		for (Tuple<Double, Double> int1 : this.effectiveIntervals(periodLength_h)) {
			for (Tuple<Double, Double> int2 : intervals) {
				overlap_h += this.mathHelpers.overlap(int1.getA(), int1.getB(), int2.getA(), int2.getB());
			}
		}
		return overlap_h;
	}

	public double overlap_h(Tuple<Double, Double> interval, double periodLength_h) {
		double overlap_h = 0.0;
		for (Tuple<Double, Double> myInterval : this.effectiveIntervals(periodLength_h)) {
			overlap_h += this.mathHelpers.overlap(myInterval.getA(), myInterval.getB(), interval.getA(),
					interval.getB());
		}
		return overlap_h;
	}

	public synchronized static List<Tuple<Double, Double>> effectiveIntervals(double duration_h, double end_h,
			double periodLength_h) {
		Episode e = new Episode();
		e.setDuration_h(duration_h);
		e.setEndTime_h(end_h);
		return e.effectiveIntervals(periodLength_h);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ":time(" + this.mathHelpers.round(this.end_h - this.duration_h, 2)
				+ "," + this.mathHelpers.round(this.end_h, 2) + ")";
	}
}
