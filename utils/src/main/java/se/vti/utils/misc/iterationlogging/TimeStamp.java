/**
 * se.vti.utils
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
package se.vti.utils.misc.iterationlogging;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * @author GunnarF
 *
 */
public class TimeStamp implements LogEntry<Object> {

	public static final String TIMESTAMP = "Timestamp";

	public TimeStamp() {
	}

	@Override
	public String label() {
		return TIMESTAMP;
	}

	@Override
	public String value(final Object data) {
		return (new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss")).format(new Date(System.currentTimeMillis()));
	}
}
