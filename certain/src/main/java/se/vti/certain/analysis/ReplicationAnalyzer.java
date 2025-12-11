/**
 * se.vti.certain.analysis
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
package se.vti.certain.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_FixedWidth;
import se.vti.certain.simulation.missionimplementation.SystemState;

/**
 * @author GunnarF
 */
public class ReplicationAnalyzer {

	private List<Function<SystemState, Double>> observablesList = new ArrayList<>();
	private List<DescriptiveStatistics> statisticsList = new ArrayList<>();
	private Map<String, DescriptiveStatistics> name2Statistics = new LinkedHashMap<>();

	public ReplicationAnalyzer() {
	}

	public ReplicationAnalyzer addObservable(String name, Function<SystemState, Double> observable) {
		this.observablesList.add(observable);
		var statistics = new DescriptiveStatistics();
		this.statisticsList.add(statistics);
		this.name2Statistics.put(name, statistics);
		return this;
	}

	public void add(List<SystemState> systemStates) {
		for (var systemState : systemStates) {
			for (int i = 0; i < this.observablesList.size(); i++) {
				this.statisticsList.get(i).addValue(this.observablesList.get(i).apply(systemState));
			}
		}
	}

	public Map<String, DescriptiveStatistics> getStatistics() {
		return this.name2Statistics;
	}

	String format(double number) {
		return String.format(Locale.US, "%.3f", number);
	}

	public String toString() {
		
		final AsciiTable table = new AsciiTable();
		table.getRenderer().setCWC(new CWC_FixedWidth().add(30).add(10).add(10).add(10).add(10));
		table.addRule();
		table.addRow("Statistic", "Mean", "StdDev", "Min", "Max");
		table.addRule();
		for (var statisticEntry : this.name2Statistics.entrySet()) {
			table.addRow(Arrays.asList(statisticEntry.getKey(), format(statisticEntry.getValue().getMean()),
					format(statisticEntry.getValue().getStandardDeviation()),
					format(statisticEntry.getValue().getMin()), format(statisticEntry.getValue().getMax())));
		}
		table.addRule();

		return table.render();
	}

}
