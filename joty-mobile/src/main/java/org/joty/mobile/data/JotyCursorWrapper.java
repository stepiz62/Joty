/*
	Copyright (c) 2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

	This file is part of Joty 2.0 Mobile.

	Joty 2.0 Mobile is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Joty 2.0 Mobile is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with Joty 2.0 Mobile.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.joty.mobile.data;

import android.database.Cursor;
import android.database.CursorWrapper;

import org.joty.data.JotyDate;
import org.joty.data.WrappedField;

/**
 * Makes expectations of Android about "_id" translated in the actual name of fisical database id field
 * used.
 * Furthermore provides some accessor methods accepting directly the field name as argument.
 */
public class JotyCursorWrapper extends android.database.CursorWrapper {
    String m_columnIdField = "_id";
    JotyCursor m_jotyCursor;

    public JotyCursorWrapper(Cursor cursor, String columnIdField) {
        super(cursor);
        m_jotyCursor = (JotyCursor) cursor;
        m_columnIdField = columnIdField;
    }

    public JotyCursorWrapper(Cursor cursor) {
        super(cursor);
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        return super.getColumnIndexOrThrow(columnName.compareTo("_id") == 0 ? m_columnIdField : columnName);
    }

    @Override
    public int getColumnIndex(String columnName) {
        return super.getColumnIndex(columnName.compareTo("_id") == 0 ? m_columnIdField : columnName);
    }

    public boolean setValueToWField(String dbFieldName, WrappedField wfield) {
        return m_jotyCursor.m_container.setValueToWField(dbFieldName, wfield);
    }

    public boolean setValueToWField(String dbFieldName, WrappedField wfield, boolean setMetaData) {
        return m_jotyCursor.m_container.setValueToWField(dbFieldName, wfield, setMetaData);
    }

    public int getJotyType(int columnIndex) {
        return m_jotyCursor.getJotyType(columnIndex);
    }

    public JotyDate getDate(String fieldName) {
        return m_jotyCursor.getDate(m_jotyCursor.getColumnIndex(fieldName));
    }

    public String getString(String fieldName) {
        return m_jotyCursor.getString(m_jotyCursor.getColumnIndex(fieldName));
    }

    public Long getLong(String fieldName) {
        return m_jotyCursor.getLong(m_jotyCursor.getColumnIndex(fieldName));
    }

    public byte[] getBlob(String fieldName) {
        return m_jotyCursor.getBlob(m_jotyCursor.getColumnIndex(fieldName));
    }

}
