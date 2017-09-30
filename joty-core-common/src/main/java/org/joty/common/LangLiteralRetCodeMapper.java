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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;



/**
 * Holds a numeric indexing of the language items in the jotyLang "dictionary"
 * to be used in communication between the Joty Server and the Application
 * instance, essentially for carrying back to the client messages that arises in
 * class objects (above all the {@code Accessor} object) of the org.joty.common
 * package, instantiated on the server side.
 * <p>
 * For the most part, an indexed value is chosen as a non successful returned
 * value of a server method invocation; from this ....the class name.
 * 
 */
public class LangLiteralRetCodeMapper {
	CaselessStringKeyMap<Long> m_literalsToRetCodes;
	HashMap<Long, String> m_retCodesToliterals;
	JotyMessenger m_jotyMessanger;
	
/** ____SUCCESS____ is simply a place holder for the successful return code */
	public LangLiteralRetCodeMapper(JotyMessenger jotyMessanger) {
		m_jotyMessanger = jotyMessanger;
		m_literalsToRetCodes = new CaselessStringKeyMap<Long>(m_jotyMessanger);
		m_retCodesToliterals = new HashMap<Long, String>();
		addLiteral("____SUCCESS____"); 
	}

	public void addLiteral(String literal) {
		int retCode = m_literalsToRetCodes.size();
		m_literalsToRetCodes.put(literal, (long) retCode);
		m_retCodesToliterals.put((long) retCode, literal);
	}

	public String literal(long retCode) {
		return m_retCodesToliterals.get(retCode);
	}

	public void load(ConfigFile configFile) {
		for (Iterator it = configFile.m_map.entrySet().iterator(); it.hasNext();)
			addLiteral(((Entry<String, String>) it.next()).getKey());
	}

	public long retCode(String literal) {
		return m_literalsToRetCodes.get(literal);
	}
}
