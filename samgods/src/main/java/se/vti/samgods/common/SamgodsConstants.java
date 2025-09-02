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
package se.vti.samgods.common;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsConstants {

	// -------------------- TRANSPORT MODES --------------------

	public static enum TransportMode {

		Road, Rail, Sea, Air, Ferry;

		public boolean isFerry() {
			return Ferry.equals(this);
		}

		public boolean isRail() {
			return Rail.equals(this);
		}
	}

	// -------------------- COMMODITIES --------------------

	public static enum Commodity {

		AGRICULTURE(1, "Products of agriculture, forestry and fishing"),
		COAL(2, "Coal and lignite; crude petroleum and natural gas"),
		METAL(3, "Metal ores and other mining and quarrying products"), FOOD(4, "Food products, beverages and tobacco"),
		TEXTILES(5, "Textiles and textile products; leather and leather products"),
		WOOD(6, "Wood and products of wood; pulp, paper and paper products; printed matter and recorded media"),
		COKE(7, "Coke and refined petroleum products"),
		CHEMICALS(8, "Chemicals, chemical products, and man-made fibers; rubber and plastic products; nuclear fuel"),
		OTHERMINERAL(9, "Other non-metallic mineral products"),
		BASICMETALS(10, "Basic metals; fabricated metal products, except machinery and equipment"),
		MACHINERY(11, "Machinery and equipment"), TRANSPORT(12, "Transport equipment"),
		FURNITURE(13, "Furniture; other manufactured goods n.e.c."),
		SECONDARYRAW(14, "Secondary raw materials; municipal wastes and other wastes"), TIMBER(15, "Timber"),
		AIR(16, "Air freight (fractions of other commodities)");

		public final int code;
		public final String description;

		private Commodity(int code, String name) {
			this.code = code;
			this.description = name;
		}

		public String twoDigitCode() {
			return (this.code < 10) ? ("0" + this.code) : ("" + this.code);
		}
	};

	public static int commodityCnt() {
		return Commodity.values().length;
	}

	public static enum ShipmentSize {

		SIZE01(1e-3, 51), SIZE02(51, 201), SIZE03(201, 801), SIZE04(801, 3001), SIZE05(3001, 7501), SIZE06(7501, 12501),
		SIZE07(12501, 20001), SIZE08(20001, 30001), SIZE09(30001, 35001), SIZE10(35001, 40001), SIZE11(40001, 45001),
		SIZE12(45001, 100001), SIZE13(100001, 200001), SIZE14(200001, 400001), SIZE15(400001, 800001),
		SIZE16(800001, 2500000);

		public static final double MIN_SHIPMENT_SIZE = 1e-3;

		public final double lowerValue_ton;
		public final double upperValue_ton;

		private ShipmentSize(double lowerValue_ton, double upperValue_ton) {
			if (lowerValue_ton < MIN_SHIPMENT_SIZE) {
				throw new IllegalArgumentException("Shipment size below " + MIN_SHIPMENT_SIZE + " ton.");
			}
			if (upperValue_ton < lowerValue_ton) {
				throw new IllegalArgumentException();
			}
			this.lowerValue_ton = lowerValue_ton;
			this.upperValue_ton = upperValue_ton;
		}

		public double getRepresentativeValue_ton() {
			return 0.5 * (this.lowerValue_ton + this.upperValue_ton);
		}

		public String toString() {
			return "Size[" + this.lowerValue_ton + "," + this.upperValue_ton + ")tons";
		}

		public static ShipmentSize getSmallestSize_ton() {
			return ShipmentSize.values()[0];
		}
	}

	public static int shipmentSizeCnt() {
		return ShipmentSize.values().length;
	}

}
