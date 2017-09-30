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

package org.joty.gui;

import java.util.Vector;

import org.joty.app.JotyApplication;
import org.joty.common.CaselessStringKeyMap;
import org.joty.common.JotyMessenger;
import org.joty.data.WrappedField;

/**
 * This class hosts a set of WrappedField objects and a map of their positions for
 * accessing them by name, too.
 *
 * @see org.joty.data.WrappedField
 *
 */
public class WFieldSet {
    public Vector<WrappedField> vector;
    public CaselessStringKeyMap<Integer> map;
    JotyMessenger m_jotyMessanger;

    public WFieldSet(JotyMessenger jotyMessanger) {
        m_jotyMessanger = jotyMessanger;
        vector = new Vector<WrappedField>();
        map = new CaselessStringKeyMap<Integer>(m_jotyMessanger);
    }

    public WrappedField add(String fieldName, int dataType) {
        return add(fieldName, dataType, false);
    }

    public WrappedField add(String fieldName, int dataType, boolean asCurrency) {
        WrappedField WrappedField = ((JotyApplication) m_jotyMessanger).createWrappedField();
        WrappedField.clear();
        WrappedField.m_dataType = dataType;
        WrappedField.m_dbFieldName = fieldName;
        WrappedField.m_isCurrency = asCurrency;
        vector.add(WrappedField);
        if (fieldName != null)
            map.put(fieldName, vector.size() - 1);
        return WrappedField;
    }

    public void clearWFields() {
        for (WrappedField WrappedField : vector)
            WrappedField.clear();
    }

    public WrappedField get(int pos) {
        return vector.get(pos);
    }

    public WrappedField get(String fieldName) {
        Integer position = pos(fieldName);
        return position == null ? null : vector.get(position);
    }

    public Integer pos(String fieldName) {
        return map.get(fieldName);
    }

    public int size() {
        return vector.size();
    }

    public void clear() {
        vector.clear();
        map.clear();
    }

}
