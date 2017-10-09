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

package org.joty.app;

import java.util.HashMap;
import java.util.Vector;

import org.joty.common.CaselessStringKeyMap;
import org.joty.common.JotyMessenger;
import org.joty.data.JotyResultSet;


/**
 * The data structure that hosts a rarely changing record set having structure = {id,
 * description}
 */

public class LiteralsCollection {

	public class DescrStruct {
		public long id;
		public String descr;
		public String strKey;

		@Override
		public String toString() {
			return descr;
		}
	}
	
	/**
	 * Helper class for the invocation of {@link LiteralsCollection} methods
	 */
	
	public class LiteralStructParams {
		public boolean sortedByID = true;
		public String strKeyFldName = null;
		public String selectStmnt = null;
		public boolean withBlank = false;
		public boolean modifiable = false;
	}
	
	public Vector<DescrStruct> m_descrArray;
	public HashMap<Long, Integer> m_descrReverseMap;
	public CaselessStringKeyMap<Integer> m_strKeyRevMap;
	public String m_name;
	public boolean m_dynamic;
	JotyMessenger m_jotyAppInstance;
	
	public LiteralsCollection(JotyMessenger jotyAppInstance) {
		m_jotyAppInstance = jotyAppInstance;
	}

	public void addLiteral(long ID, String descr, String strKey) {
		m_jotyAppInstance.ASSERT(m_descrArray != null);
		int posIdx = m_descrArray.size();
		DescrStruct descrRec = new DescrStruct();
		descrRec.id = ID;
		descrRec.descr = descr;
		if (strKey != null) {
			m_strKeyRevMap.put(strKey, posIdx);
			descrRec.strKey = strKey;
		}
		m_descrArray.add(descrRec);
		m_descrReverseMap.put(ID, posIdx);
	}

	public void addLiteral(JotyResultSet rs, LiteralStructParams lsParams, String literalField, long IDval, int posIdx) {
		DescrStruct descrRec = new DescrStruct();
		descrRec.id = IDval;
		descrRec.descr = rs.stringValue(literalField);
		if (lsParams.strKeyFldName != null) {
			String strKey = rs.stringValue(lsParams.strKeyFldName);
			m_strKeyRevMap.put(strKey, posIdx);
			descrRec.strKey = strKey;
		}
		m_descrArray.add(descrRec);
		m_descrReverseMap.put(IDval, posIdx);
	}

	public void clear() {
		if (m_descrArray != null)
			m_descrArray.removeAllElements();
		if (m_descrReverseMap != null)
			m_descrReverseMap.clear();
		if (m_strKeyRevMap != null)
			m_strKeyRevMap.clear();
	}

	public void init(String name, boolean withStrKey) {
		m_name = name;
		m_descrArray = new Vector<DescrStruct>();
		m_descrReverseMap = new HashMap<Long, Integer>();
		if (withStrKey)
			m_strKeyRevMap = new CaselessStringKeyMap<Integer>(m_jotyAppInstance);
	}

	public String literal(long key) {
		return m_descrArray.get(m_descrReverseMap.get(key)).descr;
	}

}
