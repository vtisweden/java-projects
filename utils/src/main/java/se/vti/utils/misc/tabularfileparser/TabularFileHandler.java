/**
 * se.vti.utils
 * 
 * Copyright (C) 2015-2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.utils.misc.tabularfileparser;

/**
 * An implementation of this interface is expected by the
 * <code>TabularFileParser</code> for row-by-row handling of parsed files.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public interface TabularFileHandler {

	public String preprocess(final String line);
	
	public void startDocument();

	/**
	 * Is called by the <code>TabularFileParser</code> whenever a row has been
	 * parsed
	 * 
	 * @param row
	 *            a <code>String[]</code> representation of the parsed row's
	 *            columns
	 */
	public void startRow(String[] row);
	
	public void endDocument();
	
}
