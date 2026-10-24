/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2026 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.multi.grouping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import se.vti.roundtrips.multi.MultiRoundTrip;
import se.vti.roundtrips.multi.MultiRoundTripJsonIO;
import se.vti.roundtrips.scenario.Node;
import se.vti.roundtrips.scenario.Scenario;

/**
 * @author GunnarF
 */
public class MultiRoundTripDirectoryReader<N extends Node> {

	private record NumberedFile(int number, Path path) {
	}

	private final Scenario<N> scenario;

	private final Consumer<MultiRoundTrip<N>> consumer;

	public MultiRoundTripDirectoryReader(Scenario<N> scenario, Consumer<MultiRoundTrip<N>> consumer) {
		this.scenario = scenario;
		this.consumer = consumer;
	}

	public void run(Path directory, String prefix, String suffix, int min, int max) throws IOException {
	    directory = directory.toAbsolutePath().normalize();

	    var pattern = Pattern.compile(
	            Pattern.quote(directory.resolve(prefix).toString())
	            + "(\\d+)"
	            + Pattern.quote(suffix));

	    var files = new ArrayList<NumberedFile>();

	    try (var paths = Files.list(directory)) {
	        for (var path : paths.toList()) {
	            if (!Files.isRegularFile(path)) {
	                continue;
	            }

	            var matcher = pattern.matcher(path.toString());
	            if (!matcher.matches()) {
	                continue;
	            }

	            int number = Integer.parseInt(matcher.group(1));
	            if (number < min || number > max) {
	                continue;
	            }

	            files.add(new NumberedFile(number, path));
	        }
	    }

	    files.sort(Comparator.comparingInt(NumberedFile::number));

	    for (var file : files) {
	        var state = MultiRoundTripJsonIO.singleton()
	                .readFromFile(this.scenario, file.path.toString());
	        this.consumer.accept(state);
	    }
	}	
}
