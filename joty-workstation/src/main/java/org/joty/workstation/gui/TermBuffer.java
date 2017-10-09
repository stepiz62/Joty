/*
	Copyright (c) 2013-2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

	This file is part of Joty 2.0 Workstation.

	Joty 2.0 Workstation is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Joty 2.0 Workstation is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with Joty 2.0 Workstation.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.joty.workstation.gui;

import org.joty.common.JotyTypes;
import org.joty.gui.WFieldSet;
import org.joty.workstation.data.WResultSet;

/**
 * A GridBuffer implementation used by the {@code TableTerm} class.
 * <p>
 * Essentially the structure of the record is determined by the
 * {@code m_wfields} member.
 * 
 * @see GridBuffer
 * @see Table
 * @see WFieldSet
 * @see TableTerm
 * 
 */
public class TermBuffer extends GridBuffer {
	GridTerm m_term;

	public TermBuffer(GridTerm term) {
		super();
		m_term = term;
	};

	public TermBuffer(GridTerm term, String datumField) {
		this(term);
		setKeyFieldName(datumField);
	}

	@Override
	protected void acquireRecordDescriptor(WResultSet rs) {
		m_term.m_wfields.vector.size();
		for (int i = 0; i < rs.m_cursor.m_fields.length; i++)
			if (m_term.m_wfields.map.get(rs.m_cursor.m_fields[i].m_strName) == null)
				m_term.addFieldToBuffer(rs.m_cursor.m_fields[i].m_strName);
		acquireRecordDescriptorPart(rs, m_term.m_wfields.vector, 0, false);
		for (int i = 0; i < m_fieldNames.size(); i++)
			if (m_fieldNames.get(i) == null) {
				if (m_term.m_wfields.vector.get(i).dataType() == JotyTypes._none)
					m_fieldTypes.set(i, JotyTypes._text);
			} else
				m_fieldTypes.set(i, rs.m_cursor.m_fieldsMap.get(m_fieldNames.get(i)).m_nType);
	}

	public void addEmptyRecord() {
		Record record = new Record();
		buildRecord(record);
		m_records.add(record);
	}

	@Override
	protected void buildRecord(Record record) {
		addWFieldElems(record, m_term.m_wfields.vector, false);
	}

	@Override
	protected void getFromDataLayer(Record record, WResultSet rs) {
		getWFieldFromDataLayer(record, rs, m_term.m_wfields.vector, 0, false);
	}

	public void initializeRecordDescriptor() {
		acquireRecordDescriptorPart(null, m_term.m_wfields.vector, 0, false);
	}

	public void initWithEmptyRecords(int recQty) {
		initializeRecordDescriptor();
		for (int i = 0; i < recQty; i++)
			addEmptyRecord();
	}

}
