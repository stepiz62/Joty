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

import org.joty.app.JotyApplication;
import org.joty.common.JotyMessenger;
import org.joty.common.JotyTypes;
import org.joty.data.JotyDate;
import org.joty.data.WrappedField;

/**
	 * An instance of this class hosts the default value or the context derived
	 * value for the Term instance. It keeps a delegate WrappedField member the
	 * existence checking and the initialization of which are contextually made
	 * in the suited setter method.
	 */
	public class InitialValue {
		WrappedField m_value;
		WrappedField m_hostTerm;
		JotyApplication m_jotyApplication;

		public InitialValue(JotyApplication jotyApplication, WrappedField term) {
			m_jotyApplication = jotyApplication;
			m_hostTerm = term;
		}

		private void checkDefaultValInstance(int dataType) {
			if (m_value == null) {
				m_value = new WrappedField(m_jotyApplication);
				m_value.copyWField(m_hostTerm, false);
				m_value.setToNull(false);
			}
			if (m_jotyApplication.debug())
				if (!m_hostTerm.checkType(m_hostTerm.m_dataType, dataType))
					m_jotyApplication.jotyMessage("Data type mismatch in setting default value of term mapped on database field " + m_hostTerm.m_dbFieldName + " !");
		}

		public void copyValue(WrappedField value) {
			if (value == null)
				m_value = null;
			else {
				if (m_value == null)
					m_value = new WrappedField(m_jotyApplication);
				m_value.copyWField(value, false);
			}
		}

		public WrappedField getValue() {
			return m_value;
		}

		public void setValue(int value) {
			checkDefaultValInstance(JotyTypes._int);
			m_value.setInteger(value);
		}

		public void setValue(java.sql.Date value) {
			checkDefaultValInstance(JotyTypes._date);
			m_value.m_dateVal.setDate(value);
		}

		public void setValue(JotyDate value) {
			checkDefaultValInstance(JotyTypes._date);
			m_value.m_dateVal.setDate(value);
		}

		public void setValue(long value) {
			checkDefaultValInstance(JotyTypes._long);
			m_value.setInteger(value);
		}

		public void setValue(String value) {
			checkDefaultValInstance(JotyTypes._text);
			m_value.m_strVal = value;
		}

		public void setValue(double value) {
			checkDefaultValInstance(JotyTypes._double);
			m_value.m_dblVal = value;
		}

		public void setValue(float value) {
			checkDefaultValInstance(JotyTypes._single);
			m_value.m_fltVal = value;
		}
	}
