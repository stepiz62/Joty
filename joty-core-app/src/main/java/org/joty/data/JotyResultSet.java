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

package org.joty.data;

public interface JotyResultSet {

    String stringValue(String literalField);

    boolean setValueToWField(String resultSetFieldName, WrappedField wrappedField);

    void setMemberToNull(String dbFieldName);

    void setValue(String dbFieldName, String m_strVal, boolean m_delayed,
                  int retValIndex);

    void setIntegerValue(String dbFieldName, long integer, boolean m_delayed,
                         int retValIndex);

    void setValue(String dbFieldName, float m_fltVal, boolean m_delayed,
                  int retValIndex);

    void setValue(String dbFieldName, double m_dblVal, boolean m_delayed,
                  int retValIndex);

    void setValue(String dbFieldName, JotyDate m_dateVal, boolean m_delayed,
                  int retValIndex);

    void setValue(String dbFieldName, byte[] m_previewBytes);

    String getTableName();

    int getColCount();

    FieldDescriptor getFieldDescriptor(short fldIdx);

    boolean actionFieldsContains(String fieldName);

    String getValueStr(String fieldName, boolean b);

}
