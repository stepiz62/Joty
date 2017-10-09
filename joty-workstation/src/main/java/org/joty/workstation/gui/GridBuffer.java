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

import java.util.Vector;

import org.joty.common.JotyTypes;
import org.joty.common.Utilities;
import org.joty.data.FieldDescriptor;
import org.joty.data.WrappedField;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.JotyDataBuffer;
import org.joty.workstation.data.WField;
import org.joty.workstation.data.WResultSet;

/**
 * This is the root for the concrete classes used by the {@code TableTerm} class
 * and by {@code GridManager} class to associate a buffer to the managed
 * {@code Table} instance.
 * <p>
 * It makes available tool methods that either help building the record
 * descriptor or help building the actual record of the buffer, by exposing a
 * re-usability on different part of it, that is ordinary data fields, key
 * fields explicitly embedded in buffer for special purposes.
 * 
 * @see TableTerm
 * @see GridManager
 * @see Table
 * 
 */
public abstract class GridBuffer extends JotyDataBuffer {

	public GridBuffer() {
		super();
	}

	protected void acquireRecordDescriptorPart(WResultSet rs, Object vectorObj, int initPos, boolean sourcesAreTerms) {
		WrappedField wfield;
		Vector<Term> termsVector = null;
		Vector<WrappedField> dataVector = null;
		if (sourcesAreTerms)
			termsVector = (Vector<Term>) vectorObj;
		else
			dataVector = (Vector<WrappedField>) vectorObj;
		FieldDescriptor wfieldFromDb = null;
		for (int i = 0; i < (sourcesAreTerms ? termsVector : dataVector).size(); i++) {
			wfield = (sourcesAreTerms ? termsVector : dataVector).get(i);
			String fieldName = wfield.resultSetFieldName();
			if (rs != null && fieldName != null) {
				wfieldFromDb = rs.m_cursor.m_fieldsMap.get(fieldName);
				if (wfieldFromDb == null) {
					Application.m_app.JotyMsg(this, "The db field '" + fieldName + "' is not present in the defined set for the term : \n\n" + Utilities.formattedSql(rs.m_sql));
				} else {
					if (wfield.m_jotyTypeFromDb == JotyTypes._none)
						wfield.m_jotyTypeFromDb = wfieldFromDb.m_nType;
					m_fieldTypes.add(wfieldFromDb.m_nType);
				}
				if (sourcesAreTerms || m_fieldNamesMap.get(fieldName) == null)
					/*
					 * with m_keyElems or m_wfields the first map occurrence is
					 * preserved with no warning
					 */
					m_fieldNamesMap.put(fieldName, initPos + i);
				m_fieldNames.add(fieldName);
			} else {
				m_fieldNames.add(null);
				if (rs == null && wfield.dataType() == JotyTypes._none)
					wfield.m_dataType = JotyTypes._text;
				m_fieldTypes.add(wfield.dataType());
			}
		}
	}

	protected void addWFieldElems(Record record, Vector<WrappedField> vector, boolean checkKeyPos) {
		int firstFieldPos;
		if (vector != null) {
			firstFieldPos = record.m_data.size();
			if (checkKeyPos)
				m_firstKeyPos = firstFieldPos;
			WrappedField wfield = null;
			for (int i = 0; i < vector.size(); i++) {
				wfield = new WField(Application.m_app);
				wfield.copyWField(vector.get(i), false);
				((WField)wfield).m_metaDataSource = this;
				wfield.m_idx = firstFieldPos + i;
				record.m_data.add(wfield);
			}
		}
	}

	abstract protected void buildRecord(Record record);

	@Override
	protected void buildRecord(Record record, WResultSet sourceRs) {
		checkRecordDescriptor(sourceRs);
		buildRecord(record);
	}

	protected void getWFieldFromDataLayer(Record record, WResultSet rs, Vector<WrappedField> vector, int firstFieldPos, boolean checkKeyPos) {
		if (vector != null) {
			if (checkKeyPos)
				m_firstKeyPos = firstFieldPos;
			WrappedField recordKeyWField;
			for (int i = 0; i < vector.size(); i++) {
				recordKeyWField = record.m_data.get(firstFieldPos + i);
				recordKeyWField.getWField(rs);
			}
		}
	}

	void preLoadData() {
		empty();
	}

}
