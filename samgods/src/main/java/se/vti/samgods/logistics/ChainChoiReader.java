/**
 * se.vti.samgods
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
package se.vti.samgods.logistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.common.OD;
import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.network.TransportModes;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;
import se.vti.utils.misc.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import se.vti.utils.misc.tabularfileparser.TabularFileParser;

/**
 * 
 * @author GunnarF
 *
 */
public class ChainChoiReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = LogManager.getLogger(ChainChoiReader.class);

	private final static String Key = "Key";
	private final static String NRelations = "NRelations";
	private final static String AnnualVolumeTonnesPerRelation = "AnnualVolume_(Tonnes)";
	private final static String Prob = "Prob";
	private final static String ChainType = "ChainType";

	private final static String Orig = "Orig";
	private final static String Dest = "Dest";
	private final static String Orig2 = "Orig2";
	private final static String Orig3 = "Orig3";
	private final static String Orig4 = "Orig4";
	private final static String Orig5 = "Orig5";
	private final static List<List<String>> originsColumnsByChainLength = Arrays.asList(null, Arrays.asList(Orig),
			Arrays.asList(Orig, Orig2), Arrays.asList(Orig, Orig2, Orig3), Arrays.asList(Orig, Orig2, Orig3, Orig4),
			Arrays.asList(Orig, Orig2, Orig3, Orig4, Orig5));
	private final static List<List<String>> destinationColumnsByChainLength = Arrays.asList(null, Arrays.asList(Dest),
			Arrays.asList(Orig2, Dest), Arrays.asList(Orig2, Orig3, Dest), Arrays.asList(Orig2, Orig3, Orig4, Dest),
			Arrays.asList(Orig2, Orig3, Orig4, Orig5, Dest));

	// -------------------- MEMBERS --------------------

	private final SamgodsConstants.Commodity commodity;

	private final TransportDemandAndChains transportDemand;

	private final boolean[] feasibleIsContainerValues;

	private double samplingRate = 1.0;
	private Random rnd = null;

	// -------------------- CONSTRUCTION/CONFIGURATION --------------------

	public ChainChoiReader(final SamgodsConstants.Commodity commodity, final TransportDemandAndChains transportDemand,
			Vehicles vehicles) {
		this.commodity = commodity;
		this.transportDemand = transportDemand;

		if (this.thereIsAtLeastOneCompatibleVehicle(true, vehicles)) {
			if (this.thereIsAtLeastOneCompatibleVehicle(false, vehicles)) {
				this.feasibleIsContainerValues = new boolean[] { true, false };
			} else {
				this.feasibleIsContainerValues = new boolean[] { true };
			}
		} else {
			if (this.thereIsAtLeastOneCompatibleVehicle(false, vehicles)) {
				this.feasibleIsContainerValues = new boolean[] { false };
			} else {
				throw new RuntimeException(
						"No vehicles (neither container nor bulk) available for commodity: " + commodity);
			}
		}
	}

	public ChainChoiReader setSamplingRate(double rate, Random rnd) {
		this.samplingRate = rate;
		this.rnd = rnd;
		return this;
	}

	// -------------------- INTERNALS --------------------

	private boolean thereIsAtLeastOneCompatibleVehicle(boolean isContainer, Vehicles vehicles) {
		for (var vehicleType : vehicles.getVehicleTypes().values()) {
			var attrs = (SamgodsVehicleAttributes) vehicleType.getAttributes()
					.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME);
			if ((isContainer == attrs.isContainer) && attrs.isCompatible(this.commodity)) {
				return true;
			}
		}
		return false;
	}

	private Long key = null;
	private Double singleInstanceVolume_ton_yr = null;
	private Integer numberOfInstances = null;
	private Map<OD, Double> od2proba = new LinkedHashMap<>();

	private void addTmpDataToDemandAndReset() {
		assert ((this.samplingRate < 1.0)
				|| Math.abs(this.od2proba.values().stream().mapToDouble(p -> p).sum() - 1.0) <= 0.001);
		final Map.Entry<OD, Double> odEntry = this.od2proba.entrySet().stream()
				.max((e, f) -> e.getValue().compareTo(f.getValue())).get();
		this.transportDemand.addShipments(this.commodity, odEntry.getKey(), this.singleInstanceVolume_ton_yr,
				this.numberOfInstances);

		this.key = null;
		this.singleInstanceVolume_ton_yr = null;
		this.numberOfInstances = null;
		this.od2proba = null;
	}

	// --------------- IMPLEMENTATION OF TabularFileHandler ---------------

	@Override
	public void startCurrentDataRow() {

		if ((this.samplingRate < 1.0) && (this.rnd.nextDouble() >= this.samplingRate)) {
			return; // Only for testing.
		}

		// Load chain parameters.

		final long key = Long.parseLong(this.getStringValue(Key));
		final double proba = this.getDoubleValue(Prob);
		final double singleInstanceVolume_ton_yr = this.getDoubleValue(AnnualVolumeTonnesPerRelation);
		final int numberOfInstances = this.getIntValue(NRelations);
		final OD od = new OD(Id.createNodeId(Long.parseLong(this.getStringValue(Orig))),
				Id.createNodeId(Long.parseLong(this.getStringValue(Dest))));

		// Load OD demand.

		if ((this.key != null) && (this.key == key)) {
			assert (this.singleInstanceVolume_ton_yr == singleInstanceVolume_ton_yr);
			assert (this.numberOfInstances == numberOfInstances);
			this.od2proba.compute(od, (od2, p2) -> p2 == null ? proba : p2 + proba);
		} else {
			if (this.key != null) {
				this.addTmpDataToDemandAndReset();
			}
			this.key = key;
			this.singleInstanceVolume_ton_yr = singleInstanceVolume_ton_yr;
			this.numberOfInstances = numberOfInstances;
			this.od2proba = new LinkedHashMap<>();
			this.od2proba.put(od, proba);
		}

		// Load transport legs.

		String chainType = this.getStringValue(ChainType);
		{
			final String oldChainType = chainType;
			for (int i = 1; i < 10; i++) {
				chainType = chainType.replace("" + i, "");
			}
			if (oldChainType.length() > chainType.length()) {
				log.warn("Reduced chain type " + oldChainType + " to " + chainType + ".");
			}
		}

		final List<String> originColumns = originsColumnsByChainLength.get(chainType.length());
		final List<String> destinationColumns = destinationColumnsByChainLength.get(chainType.length());

		final List<OD> segmentODs = new ArrayList<>(chainType.length());
		final List<SamgodsConstants.TransportMode> modes = new ArrayList<>(chainType.length());
		for (int i = 0; i < chainType.length(); i++) {
			final long intermedOrigin = Long.parseLong(this.getStringValue(originColumns.get(i)));
			final long intermedDestination = Long.parseLong(this.getStringValue(destinationColumns.get(i)));
			segmentODs.add(new OD(Id.createNodeId(intermedOrigin), Id.createNodeId(intermedDestination)));
			modes.add(TransportModes.CODE_2_SAMGODSMODE.get(chainType.charAt(i)));
		}

		// filter out ferry episodes

		assert (!TransportModes.SAMGODS_FERRYMODES.contains(chainType.charAt(0)));
		assert (!TransportModes.SAMGODS_FERRYMODES.contains(chainType.charAt(chainType.length() - 1)));

		int i = 1;
		while (i < segmentODs.size() - 1) {
			if (TransportModes.SAMGODS_FERRYMODES.contains(chainType.charAt(i))) {
				assert (!TransportModes.SAMGODS_FERRYMODES.contains(chainType.charAt(i - 1)));
				assert (!TransportModes.SAMGODS_FERRYMODES.contains(chainType.charAt(i + 1)));
				assert (modes.get(i - 1).equals(modes.get(i)));
				assert (modes.get(i).equals(modes.get(i + 1)));

				final OD condensedOD = new OD(segmentODs.get(i - 1).origin, segmentODs.get(i + 1).destination);
				segmentODs.remove(i + 1);
				segmentODs.remove(i);
				segmentODs.set(i - 1, condensedOD);

				modes.remove(i + 1);
				modes.remove(i);
			} else {
				i++;
			}
		}
		assert (segmentODs.size() == modes.size());

		/*
		 * Compose episodes from segments. The only case where an episode contains more than
		 * one segment are rail segments in sequence with marshalling in between.
		 */

		if (segmentODs.size() > 0) {
			for (boolean isContainer : this.feasibleIsContainerValues) {
				var transportChain = new TransportChain(this.commodity, isContainer);
				TransportEpisode currentEpisode = null;
				for (int segmentIndex = 0; segmentIndex < segmentODs.size(); segmentIndex++) {
					var segmentOD = segmentODs.get(segmentIndex);
					var segmentMode = modes.get(segmentIndex);
					if ((currentEpisode == null)
							|| !SamgodsConstants.TransportMode.Rail.equals(currentEpisode.getMode())
							|| !SamgodsConstants.TransportMode.Rail.equals(segmentMode)) {
						currentEpisode = new TransportEpisode(segmentMode);
						transportChain.addEpisode(currentEpisode);
					}
//					assert (currentEpisode.getMode().equals(segmentMode));
					currentEpisode.addSegmentOD(segmentOD);
					if (!currentEpisode.getMode().equals(segmentMode)) {
						log.warn(
								"Episode mode is " + currentEpisode.getMode() + ", but segment mode is " + segmentMode);
						log.warn("  Current chain: " + transportChain);
						log.warn("  Current episode: " + currentEpisode);
					}
				}
				this.transportDemand.addChain(transportChain);
			}
		}
	}

	@Override
	public void endDocument() {
		if (this.key != null) {
			this.addTmpDataToDemandAndReset();
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public ChainChoiReader parse(String fileName) {
		log.info(
				"Parsing file:" + fileName + (this.samplingRate < 1 ? " with sampling rate " + this.samplingRate : ""));
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		parser.setOmitEmptyColumns(false);
		try {
			parser.parse(fileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}
}
