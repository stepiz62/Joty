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

package org.joty.mobile.gui;

import android.content.Context;
import android.widget.ResourceCursorAdapter;

import org.joty.mobile.data.JotyCursorWrapper;

/**
 * A ResourceCursorAdapter implementation that provides some comfortable accessor method to the fields
 * of {@code JotyCursorWrapper} object.
 * <p>
 * Furthermore it hosts, in the {@code m_maxRecordQty} member, the  page size of the pagination prescribed
 * by the Joty application, that is initialized in the JotyResultFragment. In order to make the adapter to
 * conform with the pagination, the class overrides the {@code getCount()} method.
 *
 * @see JotyCursorWrapper
 * @see JotyResultFragment
 */

public abstract class JotyResourceCursorAdapter extends android.widget.ResourceCursorAdapter {
    public int m_maxRecordQty = 0;
    public JotyCursorWrapper m_cursor;

    public JotyResourceCursorAdapter(Context context, int layout, JotyCursorWrapper cursorWrapper, int flags) {
        super(context, layout, cursorWrapper, flags);
        m_cursor = cursorWrapper;
    }

    @Override
    public int getCount() {
        return m_maxRecordQty == 0 || super.getCount() <= m_maxRecordQty ? super.getCount() : m_maxRecordQty;
    }

    public long getLong(String fieldName) {
        return m_cursor.getLong(m_cursor.getColumnIndex(fieldName));
    }

    public int getInt(String fieldName) {
        return m_cursor.getInt(m_cursor.getColumnIndex(fieldName));
    }

    public boolean isNull(String fieldName) {
        return m_cursor.isNull(m_cursor.getColumnIndex(fieldName));
    }

    public String getString(String fieldName) {
        return m_cursor.getString(m_cursor.getColumnIndex(fieldName));
    }

}
