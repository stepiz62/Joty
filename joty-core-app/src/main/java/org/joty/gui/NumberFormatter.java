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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import org.joty.app.Common;
import org.joty.app.JotyApplication;
import org.joty.common.ApplMessenger;
import org.joty.common.JotyMessenger;
import org.joty.common.JotyTypes;
import org.joty.data.WrappedField;

/**
 * It is a wrapper for a java.text.NumberFormat class that uses an internal
 * {@code WrappedField} object to store some of the Joty datum components and to have
 * them processed by its methods. The class uses the configuration data to set
 * the java.text.NumberFormat and this is exposed my means of the
 * {@code render} method.
 */
public class NumberFormatter {
	public NumberFormat m_format;
	WrappedField m_wfield;
	JotyApplication m_jotyApplication;

    private  NumberFormatter(JotyApplication jotyApplication) {
        m_jotyApplication = jotyApplication;
        m_wfield = m_jotyApplication.createWrappedField();
    }

	public NumberFormatter(JotyApplication jotyApplication, double value, boolean currency) {
		this(jotyApplication);
		m_wfield.m_dataType = JotyTypes._double;
		m_wfield.m_dblVal = value;
		m_wfield.m_isCurrency = currency;
		setDoubleFormat(false);
	}

	public NumberFormatter(JotyApplication jotyApplication, float value) {
        this(jotyApplication);
		m_wfield.m_dataType = JotyTypes._single;
		m_wfield.m_fltVal = value;
		setSingleFormat(false);
	}

	public NumberFormatter(JotyApplication jotyApplication, int value) {
        this(jotyApplication);
		m_wfield.m_dataType = JotyTypes._int;
		m_wfield.m_iVal = value;
		setIntFormat();
	}

	public NumberFormatter(JotyApplication jotyApplication, long value) {
        this(jotyApplication);
		m_wfield.m_dataType = JotyTypes._long;
		m_wfield.m_lVal = value;
		setLongFormat();
	}

	public NumberFormatter(JotyApplication jotyApplication, WrappedField wfield) {
		this(jotyApplication, wfield, false);
	}

	public NumberFormatter(JotyApplication jotyApplication, WrappedField wfield, boolean forcedNoDecimals) {
        m_jotyApplication = jotyApplication;
		m_wfield = wfield;
      	Common common = getCommon();
		if (m_wfield.m_isCurrency) {
			m_format = common.currencyFormat();
			((DecimalFormat) m_format).setNegativePrefix((common.currencySymbolAnte() ?
															(common.currencySymbol() + (common.currencySymbolSpace() ? " " : "")) :
															"") + "-");
			((DecimalFormat) m_format).setNegativeSuffix(common.currencySymbolAnte() ?
														"" : 
														(common.currencySymbolSpace() ? " " : "") + common.currencySymbol());
			}
		else
			m_format = common.numberFormat();
		switch (m_wfield.dataType()) {
			case JotyTypes._double:
				setDoubleFormat(forcedNoDecimals);
				break;
			case JotyTypes._single:
				setSingleFormat(forcedNoDecimals);
				break;
			case JotyTypes._int:
				setIntFormat();
				break;
			case JotyTypes._long:
				setLongFormat();
				break;
			case JotyTypes._dbDrivenInteger:
				if (wfield.m_jotyTypeFromDb == JotyTypes._int)
					setIntFormat();
				else
					setLongFormat();
				break;
		}
	}

	public Number getNumber(String text) {
		Number retVal = null;
		try {
			retVal = m_format.parse(m_wfield.formatWrap(text));
		} catch (ParseException e) {
			 m_jotyApplication.jotyMessage(e);
		}
		return retVal;
	}

	public String render() {
		switch (m_wfield.dataType()) {
			case JotyTypes._double:
				return m_format.format(m_wfield.m_dblVal);
			case JotyTypes._single:
				return m_format.format(m_wfield.m_fltVal);
			case JotyTypes._long:
				return m_format.format(m_wfield.m_lVal);
			case JotyTypes._int:
				return m_format.format(m_wfield.m_iVal);
			case JotyTypes._dbDrivenInteger:
				return m_wfield.m_jotyTypeFromDb == JotyTypes._long ? m_format.format(m_wfield.m_lVal) : m_format.format(m_wfield.m_iVal);
		}
		return null;
	}

	public void setDoubleFormat(boolean forcedNoDecimals) {
		if (m_wfield.m_isCurrency)
			return;
		int fractionDigits = forcedNoDecimals ? 0 : 2;
		m_format.setMaximumIntegerDigits(getCommon().moneyDigitDim());
		m_format.setMaximumFractionDigits(fractionDigits);
	}

	public void setIntFormat() {
		m_format.setMaximumIntegerDigits(getCommon().intDigitDim());
	}

	public void setLongFormat() {
		m_format.setMaximumIntegerDigits(getCommon().longDigitDim());
	}

	public void setSingleFormat(boolean forcedNoDecimals) {
		m_format.setMaximumIntegerDigits(getCommon().fisicalDigitDim());
		m_format.setMaximumFractionDigits(forcedNoDecimals ? 0 : 2);
	}

	private Common getCommon() {
		return (Common) ((ApplMessenger) m_jotyApplication).getCommon();
	}
	
}
