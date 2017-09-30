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

import org.joty.app.JotyApplication;
import org.joty.common.JotyTypes;

/**
 * It is used as atom for decoupling request of data from the way they are
 * retrieved. It holds the datum and the inherent meta-data.
 * <p>
 * It includes also support for a datum participating in a Joty delayed
 * transaction, where, even without having an available value at the time of
 * definition of the statements, it participates in this phase and its
 * value, generated on the server side, can be used there in the statements
 * following it in execution, and at last, it can return back to the client in
 * the dedicated vector of the returned values.
 *
 */

public class FieldDescriptor {
	public String m_strName;
	public int m_nType;
	/** used only in jdbc mode */
	public int m_nSqlType;
	/** used only in jdbc mode */
	public long m_lPrecision;
	public long m_lLength;
	public int m_nScale;
	public int m_iVal;
	public long m_lVal;
	public float m_fltVal;
	public double m_dblVal;
	public String m_strVal;
	public JotyDate m_dateVal;
	public boolean m_isNull;
	/**
	 * It takes meaning when the application is running in web mode. If true the
	 * datum receives fictitious assignments by the framework and when this
	 * field is involved in building the sql statement (see
	 * {@code StatementBuilder} a placeholder for the field is inserted there
	 * (see {@code WresultSet.getValueStr} ).
	 * <p>
	 * This member takes its value from WrappedField.m_delayed member when the
	 * {@code WrappedField.setWField} method is invoked.
	 * 
	 * @see org.joty.data.WrappedField#m_delayed
	 * @see org.joty.data.WrappedField#setWField(org.joty.data.JotyResultSet, String)
	 */
	public boolean m_delayedVal;
	/**
	 * An host for small blob object that participates in cursor fetching
	 */
	public byte[] m_previewBytes;
	public boolean m_toUpdate;
	/** Positional index within the cursor */
	public int m_pos;
	/**
	 * Works together with the {@code m_delayedVal} member. It indicates the
	 * position within the vector of the returning value from which picking the
	 * value up.
	 * 
	 * @see #m_delayedVal
	 * @see org.joty.data.WrappedField#m_posIndexAsReturningValue
	 * @see org.joty.data.WrappedField#setWField(org.joty.data.JotyResultSet, String)
	 */
	public int m_genIdIndex;
	public boolean m_foreign;

	public FieldDescriptor(JotyApplication jotyApplication) {
		m_dateVal = jotyApplication.createDate();
		m_toUpdate = true;
	}

	public void clear() {
		m_isNull = true;
		m_strVal = "";
		m_lVal = 0;
		m_iVal = 0;
		m_dblVal = 0;
		m_fltVal = 0;
		m_dateVal.m_isNull = true;
		m_previewBytes = null;
	}

	public String valueRender(boolean forSqlExpr) {
		String retStr = null;
		switch (m_nType) {
			case JotyTypes._text:
				retStr = m_strVal;
				break;
			case JotyTypes._long:
				retStr = String.valueOf(m_lVal);
				break;
			case JotyTypes._int:
				retStr = String.valueOf(m_iVal);
				break;
			case JotyTypes._double:
				retStr = String.valueOf(m_dblVal);
				break;
			case JotyTypes._single:
				retStr = String.valueOf(m_fltVal);
				break;
			case JotyTypes._date:
			case JotyTypes._dateTime:
				retStr = m_dateVal.render(forSqlExpr, m_nType == JotyTypes._dateTime);
				break;
		}
		return retStr;
	}

}
