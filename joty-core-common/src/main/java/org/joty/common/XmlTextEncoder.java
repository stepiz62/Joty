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

import java.io.UnsupportedEncodingException;

/**
 * Provides Base64 encoding and decoding of data hosted by the java.lang.String
 * instances. It helps to create a robust channel of communication though http
 * and xml and offer a simple handle of management: the java.lang.String type.
 * If the hosted data is actual text (semantically speaking), the class assumed
 * that the text is UTF-8 encoded.
 * <p>
 * Since Base64 conversion process manages bytes and the
 * javax.xml.bind.DatatypeConverter is used for it to take place, the class has
 * to transfer data between the java.lang.String object and the array of bytes,
 * so that an encoding method is necessary. In the case hosted data is actual
 * text the UTF-8 encoding method is used. In the case the string hosts binary
 * data a single-byte encoding method is used because the class assumes that
 * such a method (in the reverse direction) was used in the original step that
 * converted bits of binary data to the hosting string: a fictitious 'decode'
 * process. In this original process of 'decoding' binary data into a string
 * single-byte encoding method was used because it assures that any single byte
 * value (from 0x00 to 0xff) has a legal corresponding form in the encoded text so
 * that to be correctly transferred back during decoding.
 * <p>
 * 
 */
public abstract class XmlTextEncoder {
	
	JotyMessenger m_jotyMessanger;
	
	public XmlTextEncoder(JotyMessenger jotyMessanger) {
		m_jotyMessanger = jotyMessanger;
	}

	abstract protected byte[] base64decode(String src);
	abstract protected String base64encode(byte[] src);

	public String decode(String srcTxt, Boolean forBinary) {
		String retVal = null;
		try {
			retVal = new String(base64decode(srcTxt), forBinary ? Utilities.m_singleByteEncoding : "UTF-8");
		} catch (UnsupportedEncodingException e) {
			m_jotyMessanger.jotyMessage(e);
		}
		return retVal;
	}

	public String encode(String srcTxt, Boolean forBinary) {
		if (srcTxt == null)
			return null;
		String retVal = null;
		try {
			retVal = base64encode(srcTxt.getBytes(forBinary ? Utilities.m_singleByteEncoding : "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			m_jotyMessanger.jotyMessage(e);
		}
		return retVal;
	}
	
}
