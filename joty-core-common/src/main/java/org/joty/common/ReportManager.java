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

import java.util.Vector;

/**
 * This class is responsible for hosting the set of parameters of a report
 * launch invocation and properly encoding and decoding their values depending on the their data type.
 * 
 */
public class ReportManager {
	public class Parameter {
		public String name;
		public int type;
		public int intVal;
		public String strVal;

		public Parameter() {}

		public Parameter(String sName, int theType) {
			type = theType;
			name = sName;
		}

		public String render() {
			return m_xmlEncoder.encode(type == JotyTypes._int ? String.valueOf(intVal) : strVal, false);
		}

		public void setValue(String literalValue, int type) {
			String decodedVal = m_xmlEncoder.decode(literalValue, false);
			switch (type) {
				case JotyTypes._long:
				case JotyTypes._text:
				case JotyTypes._double:
				case JotyTypes._single:
				case JotyTypes._date:
				case JotyTypes._dateTime:
				case JotyTypes._dbDrivenInteger:
					strVal = decodedVal;
					break;
				case JotyTypes._int:
					intVal = Integer.parseInt(decodedVal);
					break;
			}
		}

	}

	XmlTextEncoder m_xmlEncoder;

	public Vector<Parameter> m_params;

	public ReportManager() {
		m_params = new Vector<Parameter>();
	}

	public void addParameter(String name, int value) {
		Parameter param = new Parameter(name, JotyTypes._int);
		param.intVal = value;
		m_params.add(param);
	}

	public void addParameter(String name, String value) {
		Parameter param = new Parameter(name, JotyTypes._text);
		param.strVal = value;
		m_params.add(param);
	}

	public void addParameter(String name, String literalValue, int type) {
		switch (type) {
			case JotyTypes._long:
			case JotyTypes._text:
			case JotyTypes._double:
			case JotyTypes._single:
			case JotyTypes._date:
			case JotyTypes._dateTime:
			case JotyTypes._dbDrivenInteger:
				addParameter(name, literalValue);
				break;
			case JotyTypes._int:
				addParameter(name, Integer.parseInt(literalValue));
				break;
		}
	}

	public void resetParams() {
		m_params.removeAllElements();
	}

	public void setXmlEncoder(XmlTextEncoder encoder) {
		m_xmlEncoder = encoder;
	}

}
