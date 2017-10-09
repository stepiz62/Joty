/*
	Copyright (c) 2013-2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

	This file is part of Joty 2.0 Core.

	Joty 2.0 Core is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Joty 2.0 Core is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with Joty 2.0 Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.joty.common;

/**
 * Holds the data types associated with the database table fields as encoded and
 * managed by the Joty framework.
 * 
 */
public class JotyTypes {
	public static String getVerbose(int type) {
		return verbose[type];
	}

	public static final int _none = 0;
	public static final int _text = 1;
	public static final int _int = 2;
	public static final int _long = 3;
	public static final int _single = 4;
	public static final int _double = 5;
	public static final int _date = 6;
	public static final int _dateTime = 7;
	/**
	 * it represents an ordinary large binary object that the framework manages
	 * in a dedicated way.
	 */
	public static final int _blob = 8;
	/**
	 * it represents a small binary object treated like other data types in
	 * fetch operations but typically associated, in writing, to a large blob
	 * datum, and with it treated.
	 */
	public static final int _smallBlob = 9;
	/**
	 * it is an integer data type the final dimension of which is demanded to
	 * the detection on the database field jdbc type.
	 */
	public static final int _dbDrivenInteger = 10;

	private static String[] verbose = { "no type", "Text", "Int", "Long", "Single", "Double", "Date", "DateTime", "Blob", "Small blob", "Db driven integer" };
}
