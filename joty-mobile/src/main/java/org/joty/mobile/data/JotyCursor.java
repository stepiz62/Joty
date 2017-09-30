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

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

import org.joty.app.JotyApplication;
import org.joty.common.JotyTypes;
import org.joty.data.BasicJotyCursor;
import org.joty.data.FieldDescriptor;
import org.joty.data.JotyDate;
import org.joty.mobile.app.JotyApp;

/**
 * The class is a derivation of {@code BasicJotyCursor} that implements the {@code  android.database.Cursor} interface
 * to fully integrate with Android os and to support, indeed, the interaction with {@code JotyResourceCursorAdapter}.
 *
 * @see org.joty.data.BasicJotyCursor
 * @see org.joty.mobile.data.WResultSet
 * @see org.joty.mobile.gui.JotyResourceCursorAdapter
 *
 */

public class JotyCursor extends BasicJotyCursor implements android.database.Cursor {
    public JotyApp m_app = JotyApp.m_app;
    public String m_fieldNames[];
    public WResultSet m_container;

    public JotyCursor(int size, JotyApplication app) {
        super(size, app);
        m_fieldNames = new String[size];
    }

    @Override
    public int getCount() {
        return m_container.m_recNodeList.getLength();
    }

    @Override
    public int getPosition() {
        return m_container.m_currNodeIndex;
    }

    @Override
    public boolean move(int offset) {
        return moveToPosition(getPosition() + offset);
    }

    @Override
    public boolean moveToPosition(int position) {
        m_container.m_currNodeIndex = position - 1;
        moveToNext();
        return true;
    }

    @Override
    public boolean moveToFirst() {
        m_container.m_currNodeIndex = - 1;
        moveToNext();
        return true;
    }

    @Override
    public boolean moveToLast() {
        m_container.m_currNodeIndex = getCount() - 2;
        moveToNext();
        return true;
    }

    @Override
    public boolean moveToNext() {
         return m_container.getRecordFromNodeList();
    }

    @Override
    public boolean moveToPrevious() {
        return getPosition() > 0 ? moveToPosition(getPosition() - 1) : false;
    }

    @Override
    public boolean isFirst() {
        return getPosition() == 0;
    }

    @Override
    public boolean isLast() {
        return getPosition() == getCount() -1;
    }

    @Override
    public boolean isBeforeFirst() {
        return m_container.m_nodeListBOF;
    }

    @Override
    public boolean isAfterLast() {
        return m_container.m_nodeListEOF;
    }

    @Override
    public int getColumnIndex(String columnName) {
        int retVal = -1;
        FieldDescriptor fieldDescr = fieldDescriptor(columnName);
        if (fieldDescr != null)
            retVal = fieldDescr.m_pos;
        return retVal;
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        int retVal = getColumnIndex(columnName);
        if (retVal == -1)
            throw new IllegalArgumentException();
        return retVal;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return m_fields[columnIndex].m_strName;
    }

    @Override
    public String[] getColumnNames() {
        return m_fieldNames;
    }

    @Override
    public int getColumnCount() {
        return m_fields.length;
    }

    @Override
    public byte[] getBlob(int columnIndex) {
        return m_fields[columnIndex].m_previewBytes;
    }

    @Override
    public String getString(int columnIndex) {
        return  m_fields[columnIndex].m_strVal;
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        String renderedValue = m_fields[columnIndex].valueRender(false);
        if (buffer.data == null ||  buffer.data.length < renderedValue.length())
            buffer.data = renderedValue.toCharArray();
        else {
            int i;
            for (i = 0; i < renderedValue.length(); i++)
                buffer.data[i] = renderedValue.charAt(i);
            for (int j = i; j < buffer.data.length; j++)
                buffer.data[j] = '\0';
        }
    }

    @Override
    public short getShort(int columnIndex) {
        return 0;
    }

    @Override
    public int getInt(int columnIndex) {
        return m_fields[columnIndex].m_iVal;
    }

    @Override
    public long getLong(int columnIndex) {
        return m_fields[columnIndex].m_lVal;
    }

    @Override
    public float getFloat(int columnIndex) {
        return m_fields[columnIndex].m_fltVal;
    }

    @Override
    public double getDouble(int columnIndex) {
        return m_fields[columnIndex].m_dblVal;
    }

    public JotyDate getDate(int columnIndex) {
        return m_fields[columnIndex].m_dateVal;
    }

    @Override
    public int getType(int columnIndex) {
         switch (getJotyType(columnIndex)) {
            case JotyTypes._text:
            case JotyTypes._date:
            case JotyTypes._dateTime:
                return FIELD_TYPE_STRING;
            case JotyTypes._long:
            case JotyTypes._int:
                return FIELD_TYPE_INTEGER;
            case JotyTypes._double:
            case JotyTypes._single:
                return FIELD_TYPE_FLOAT;
            case JotyTypes._blob:
            case JotyTypes._none:
                return FIELD_TYPE_NULL;
            case JotyTypes._smallBlob:
                return FIELD_TYPE_BLOB;
            default:
                return FIELD_TYPE_NULL;
        }
    }

    public int getJotyType(int columnIndex) {
        return m_fields[columnIndex].m_nType;
    }

    @Override
    public boolean isNull(int columnIndex) {
        return m_fields[columnIndex].m_isNull;
    }

    FieldDescriptor fieldDescriptor(String fieldName) {
        return m_fieldsMap.get(fieldName);
    }

    @Override
    public void deactivate() {

    }

    @Override
    public boolean requery() {
        return false;
    }

    @Override
    public void close() {
        m_closed = true;
    }

    @Override
    public boolean isClosed() {
        return m_closed;
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {}

    @Override
    public void unregisterContentObserver(ContentObserver observer) {}

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {}

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {}

    @Override
    public void setNotificationUri(ContentResolver cr, Uri uri) {}

    @Override
    public Uri getNotificationUri() {
        return null;
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return false;
    }

    @Override
    public void setExtras(Bundle extras) {

    }

    @Override
    public Bundle getExtras() {
        return null;
    }

    @Override
    public Bundle respond(Bundle extras) {
        return null;
    }

  }
