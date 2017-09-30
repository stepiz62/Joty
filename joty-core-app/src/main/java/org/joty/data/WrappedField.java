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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import org.joty.app.Common;
import org.joty.app.JotyApplication;
import org.joty.common.ApplMessenger;
import org.joty.common.JotyTypes;
import org.joty.common.Utilities;
import org.joty.gui.NumberFormatter;

/**
 * It is a wrapper for a value of a database field. The data types that an
 * instance of this class can assume are specified by the {@code JotyTypes}
 * class.
 * <p>
 * There is no specific reasons for this class has been chosen to be polymorphic
 * itself instead of having a polymorphic dynasty implemented from a root (even
 * abstract) class, a part from chance of keeping under a common insight all the
 * different behaviours in any different context of elaboration: that is, in
 * representing the Joty data type container a feature oriented approach has
 * been preferred to an object oriented one. The waste of memory derived from
 * this approach is contained with an acceptable limit.
 * <p>
 * The class provides various methods for assigning, copying, exchanging the
 * inner values with fields of JotyResultSet instances, methods for rendering in a
 * textual form the value hosted, depending on the locale and on the
 * configuration data (for instance see the {@code JotyDate} class}) and, if the
 * rendering is for use in sql expressions, even on the dbms.
 * <p>
 * Participates in the paradigm of the Joty framework: all the instances of this
 * class that must get their values generated on the server (at the end of Joty
 * transaction) are marked as "delayed" my means of the {@code m_delayed} member
 * variable.
 * <p>
 * The attributes name and type are held internally or are referenced
 * externally, typically in buffering, either located in a descriptor of a set
 * of fields or in another WrappedField instance.
 * 
 * @see org.joty.common.JotyTypes
 * @see org.joty.data.JotyDate
 * @see #m_delayed
 */
public class WrappedField {

	public interface GetWFieldInterface {
		int method(JotyResultSet rs, WrappedField WrappedField);
	}

	public interface IdentityRenderer {
		String render();
	}

	public static boolean checkType(int definedType, int typeID) {
		return definedType == typeID || 
				definedType == JotyTypes._dbDrivenInteger || 
				typeID == JotyTypes._none || 
				typeID == JotyTypes._dbDrivenInteger || 
				typeID == JotyTypes._date && definedType == JotyTypes._dateTime || 
				typeID == JotyTypes._dateTime && definedType == JotyTypes._date;
	}

	public int m_iVal;

	public long m_lVal;
	public float m_fltVal;
	public double m_dblVal;
	public String m_strVal;
	public JotyDate m_dateVal;
	public byte[] m_previewBytes;
	private NumberFormatter m_formatter = null;
	public int m_len;

	public String m_dbFieldName;
	public int m_idx;
	public WrappedField m_metaDataWField;
	public boolean m_isCurrency;
	public int m_dataType;
	public GetWFieldInterface m_GetWFieldImplementor;
	private boolean m_isNull;
	public int m_jotyTypeFromDb = JotyTypes._none;
	/**
	 * It is used typically for integer types (_int, _long, _dbDrivenInteger), if
	 * true the value of the instance is waiting to get its value returned from
	 * the Joty Server. In this case the framework keeps track of the position
	 * within the vector of the returning values, position dedicated to host the value for
	 * 'this' instance, and stores the index in {@code m_posIndexAsReturningValue}
	 * member and passes it to the JotyResultSet instance, responsible to communicate
	 * with the {@code WebClient} instance .
	 * 
	 * @see JotyResultSet
	 * @see FieldDescriptor
	 * @see #m_posIndexAsReturningValue
	 */
	public boolean m_delayed;
	/** Keeps its value from a call to {@link org.joty.app.JotyApplication#returnedValuesAvailablePos()} */
	public int m_posIndexAsReturningValue;
	public JotyApplication m_jotyApplication;
	

	public WrappedField(JotyApplication jotyApplication) {
		m_jotyApplication = jotyApplication;
		m_isCurrency = false;
		m_delayed = false;
		m_posIndexAsReturningValue = 0;
		m_dataType = JotyTypes._none;
		m_dateVal = jotyApplication.createDate();
		clear(true);
		m_idx = -1;
	}

	public void clear() {
		clear(false);
	}

	public void clear(boolean overAll) {
		m_isNull = true;
		if (overAll) {
			m_strVal = "";
			m_lVal = 0;
			m_iVal = 0;
			m_dblVal = 0;
			m_fltVal = 0;
			m_dateVal.m_isNull = true;
			m_previewBytes = null;
		} else
			switch (dataType()) {
				case JotyTypes._text:
					m_strVal = "";
					break;
				case JotyTypes._long:
				case JotyTypes._int:
				case JotyTypes._dbDrivenInteger:
					m_iVal = 0;
					m_lVal = 0;
					break;
				case JotyTypes._double:
					m_dblVal = 0;
					break;
				case JotyTypes._single:
					m_fltVal = 0;
					break;
				case JotyTypes._date:
				case JotyTypes._dateTime:
					m_dateVal.m_isNull = true;
					break;
				case JotyTypes._blob:
				case JotyTypes._smallBlob:
					m_previewBytes = null;
					break;
			}
	}

	public void copyWField(WrappedField source) {
		copyWField(source, true);
	}

	public void copyWField(WrappedField source, boolean withDbIdentity) {
		copyWField(source, withDbIdentity, false);
	}
	
	public void copyWField(WrappedField source, boolean withDbIdentity, boolean storeSource) {
		m_isNull = source.m_isNull;
		m_strVal = source.m_strVal;
		m_lVal = source.m_lVal;
		m_iVal = source.m_iVal;
		m_dblVal = source.m_dblVal;
		m_fltVal = source.m_fltVal;
		m_dateVal.setDate(source.m_dateVal);
		if (withDbIdentity) {
			m_dataType = source.resultSetDataType();
			m_dbFieldName = source.resultSetFieldName();
		}
		m_isCurrency = source.m_isCurrency;
		m_jotyTypeFromDb = source.m_jotyTypeFromDb;
		m_previewBytes = source.m_previewBytes;
		m_idx = source.m_idx;
		m_len = source.m_len;
		m_GetWFieldImplementor = source.m_GetWFieldImplementor;
		if (storeSource)
			m_metaDataWField = source;
	}

	public JotyDate dateVal() {
		return m_dateVal;
	}

	public String dbFieldName() {
		return m_metaDataWField != null ? 
			m_metaDataWField.m_dbFieldName : dbFieldNameFromMetadataSource(); 
	}
				
	public String dbFieldNameFromMetadataSource() {
		return m_dbFieldName;
	}
	
	public int dataType() {
		return  m_metaDataWField != null ? 
			m_metaDataWField.m_dataType : dataTypeFromMetadataSource();
	}

	public int dataTypeFromMetadataSource() {
		return  m_dataType;
	}
				
	public boolean dbFieldSpecified() {
		return m_dbFieldName != null && !m_dbFieldName.isEmpty() || dbFieldSpecifiedFromMetadataSource();
	}

	public boolean dbFieldSpecifiedFromMetadataSource() {
		return false;
	}

	public double doubleVal() {
		return m_isNull ? 0 : m_dblVal;
	}

	public float floatVal() {
		return m_isNull ? 0 : m_fltVal;
	}

	public String formatWrap(String input) {
       	Common common = (Common) ((ApplMessenger) m_jotyApplication).getCommon();
		if (m_isCurrency && input.indexOf(common.currencySymbol()) == -1) {
			StringBuilder sb = new StringBuilder();
			sb.append(common.currencySymbolAnte() ? common.currencySymbol() : input);
			if (common.currencySymbolSpace())
				sb.append(' ');
			sb.append(common.currencySymbolAnte() ? input : common.currencySymbol());
			return sb.toString();
		} else
			return input;
	}

	public long getInteger() {
		return m_delayed ? 0 : (m_jotyTypeFromDb == JotyTypes._long || m_jotyTypeFromDb == JotyTypes._none && dataType() == JotyTypes._long ? m_lVal : m_iVal);
	}

	public void getValFrom(WrappedField source) {
		switch (dataType()) {
			case JotyTypes._text:
				m_strVal = source.m_strVal;
				break;
			case JotyTypes._long:
				m_lVal = source.m_lVal;
				break;
			case JotyTypes._int:
				m_iVal = source.m_iVal;
				break;
			case JotyTypes._dbDrivenInteger:
				if (source.m_jotyTypeFromDb == JotyTypes._long)
					m_lVal = source.m_lVal;
				else
					m_iVal = source.m_iVal;
				break;
			case JotyTypes._double:
				m_dblVal = source.m_dblVal;
				m_isCurrency = source.m_isCurrency;
				break;
			case JotyTypes._single:
				m_fltVal = source.m_fltVal;
				break;
			case JotyTypes._date:
			case JotyTypes._dateTime:
				m_dateVal.setDate(source.m_dateVal);
				break;
			case JotyTypes._smallBlob:
				m_previewBytes = source.m_previewBytes;
				break;
		}
		m_isNull = source.m_isNull;
	}

	public void getWField(JotyResultSet rs) {
		if (resultSetFieldName() == null) {
			if (m_GetWFieldImplementor != null)
				m_GetWFieldImplementor.method(rs, this);
		} else
			rs.setValueToWField(resultSetFieldName(), this);
	}

	public long integerVal() {
		return m_isNull ? 0 : getInteger();
	}

	public int intVal() {
		return m_isNull ? 0 : m_iVal;
	}

	protected void invalidAssignementMsg() {
		m_jotyApplication.jotyMessage("Invalid Joty type '" + JotyTypes.getVerbose(dataType()) + 
				"' for field '" + dbFieldName() + "' !");
	}

	public boolean isEmpty() {
		switch (dataType()) {
			case JotyTypes._text:
				return m_strVal == null || m_strVal.length() == 0;
			case JotyTypes._date:
			case JotyTypes._dateTime:
			case JotyTypes._dbDrivenInteger:
			case JotyTypes._long:
			case JotyTypes._int:
			case JotyTypes._single:
			case JotyTypes._double:
			case JotyTypes._smallBlob:
				return m_isNull;
			default:
				return true;
		}
	}

	public boolean isNull() {
		return m_isNull;
	}

	public String render() {
		return render(false);
	}

	public String render(boolean forcedNoDecimal) {
		return render(forcedNoDecimal, false);
	}

	public String render(boolean forcedNoDecimal, boolean forSqlExpr) {
		String retStr = "";
		if (!m_isNull)
			switch (dataType()) {
				case JotyTypes._double:
				case JotyTypes._single:
				case JotyTypes._long:
				case JotyTypes._int:
				case JotyTypes._dbDrivenInteger:
					if (m_formatter == null)
						m_formatter = new NumberFormatter(m_jotyApplication, this, forcedNoDecimal);
					retStr = m_formatter.render();
					break;
				case JotyTypes._text:
					retStr = m_strVal;
					break;
				case JotyTypes._date:
				case JotyTypes._dateTime:
					retStr = m_dateVal.render(forSqlExpr, dataType() == JotyTypes._dateTime);
					break;
			}
		return retStr;
	}

	public int resultSetDataType() {
		return m_metaDataWField == null ? dataType() : m_metaDataWField.resultSetDataType();
	}

	public String resultSetFieldName() {
		return m_metaDataWField == null ? dbFieldName() : m_metaDataWField.resultSetFieldName();
	}

	public void setData(String gotText, NumberFormatter numberFormatter) {
		String trimmedText = gotText == null ? "" : gotText.trim();
		setToNull(trimmedText.length() == 0);
		switch (dataType()) {
			case JotyTypes._text:
				m_strVal = trimmedText;
				break;
			case JotyTypes._long:
			case JotyTypes._int:
			case JotyTypes._dbDrivenInteger:
			case JotyTypes._single:
			case JotyTypes._double:
				if (!isNull()) {
					Number number = numberFormatter.getNumber(trimmedText);
					if (dataType() == JotyTypes._single)
						m_fltVal = number.floatValue();
					else if (dataType() == JotyTypes._double)
						m_dblVal = number.doubleValue();
					else
						setInteger(number);
				}
				break;
			case JotyTypes._date:
			case JotyTypes._dateTime:
				if (!isNull()) {
			      	Common common = (Common) ((ApplMessenger) m_jotyApplication).getCommon();
					setToNull(gotText.compareToIgnoreCase(common.emptyDateRendering(dataType() == JotyTypes._dateTime)) == 0);
				}
				m_dateVal.m_isNull = isNull();
				if (!isNull())
					m_dateVal.setDate(gotText, dataType() == JotyTypes._dateTime);
				break;
		}
	}

	public void setInteger(long val) {
		if (m_jotyTypeFromDb == JotyTypes._long || m_jotyTypeFromDb == JotyTypes._none)
			setVal(val);
		if (m_jotyTypeFromDb == JotyTypes._int || m_jotyTypeFromDb == JotyTypes._none)
			setVal((int) val);
	}

	void setInteger(Number number) {
		boolean longValue = true;
		switch (dataType()) {
			case JotyTypes._dbDrivenInteger:
				longValue = m_jotyTypeFromDb == JotyTypes._long;
				break;
			case JotyTypes._int:
				longValue = false;
				break;
		}
		if (longValue)
			m_lVal = number.longValue();
		else
			m_iVal = number.intValue();
	}

	public void setToNull(boolean truth) {
		m_isNull = truth;
	}

	public void setVal(double dblVal) {
		m_dblVal = dblVal;
		m_isNull = false;
	}

	public void setVal(float fltVal) {
		m_fltVal = fltVal;
		m_isNull = false;
	}

	public void setVal(int iVal) {
		m_iVal = iVal;
		m_isNull = false;
	}

	public void setVal(JotyDate dtVal) {
		m_dateVal.setDate(dtVal);
		m_isNull = dtVal.m_isNull;
	}

	public void setVal(long lVal) {
		m_lVal = lVal;
		m_isNull = false;
	}

	public void setVal(String strVal) {
		m_strVal = strVal;
		m_isNull = false;
	}

	public void setValFromDbSubmittedExpr(String strVal) {
		try {
			switch (dataType()) {
				case JotyTypes._text:
					m_strVal = Utilities.unquote(strVal.replace("''", "'"));
					break;
				case JotyTypes._int:
				case JotyTypes._long:
				case JotyTypes._dbDrivenInteger:
					setInteger(Long.parseLong(strVal));
					break;
				case JotyTypes._double:
					m_dblVal = Double.parseDouble(strVal);
					break;
				case JotyTypes._single:
					m_fltVal = Float.parseFloat(strVal);
					break;
				case JotyTypes._date:
				case JotyTypes._dateTime:
					m_dateVal.setDate(Utilities.unquote(strVal), true, dataType() == JotyTypes._dateTime, true);
					break;
			}
		} catch (Throwable t) {
			m_jotyApplication.jotyMessage(t);
		}
	}

	public void setWField(JotyResultSet rs) {
		setWField(rs, null);
	}

	public void setWField(JotyResultSet rs, String relatedField) {
		String dbFieldName = relatedField == null ? dbFieldName() : relatedField;
		if (m_isNull)
			rs.setMemberToNull(dbFieldName);
		else {
			int retValIndex = m_delayed ? m_posIndexAsReturningValue : 0;
			switch (dataType()) {
				case JotyTypes._text:
					rs.setValue(dbFieldName, m_strVal, m_delayed, retValIndex);
					break;
				case JotyTypes._int:
				case JotyTypes._long:
				case JotyTypes._dbDrivenInteger:
					rs.setIntegerValue(dbFieldName, getInteger(), m_delayed, retValIndex);
					break;
				case JotyTypes._single:
					rs.setValue(dbFieldName, m_fltVal, m_delayed, retValIndex);
					break;
				case JotyTypes._double:
					rs.setValue(dbFieldName, m_dblVal, m_delayed, retValIndex);
					break;
				case JotyTypes._date:
				case JotyTypes._dateTime:
					rs.setValue(dbFieldName, m_dateVal, m_delayed, retValIndex);
					break;
				case JotyTypes._smallBlob:
					rs.setValue(dbFieldName, m_previewBytes);
					break;
			}
		}
	}

	public String strVal() {
		return m_strVal;
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean ForcedNoDecimal) {
		String retStr = "";
		if (m_isNull) {
	      	Common common = (Common) ((ApplMessenger) m_jotyApplication).getCommon();
			if (dataType() == JotyTypes._date)
				retStr = common.emptyDateRendering(false);
			else if (dataType() == JotyTypes._dateTime)
				retStr = common.emptyDateRendering(true);
		} else
			switch (dataType()) {
				case JotyTypes._text:
					retStr = m_strVal;
					break;
				case JotyTypes._int:
					retStr = Integer.toString(m_iVal);
					break;
				case JotyTypes._long:
					retStr = Long.toString(m_lVal);
					break;
				case JotyTypes._dbDrivenInteger:
					retStr = Long.toString(integerVal());
					break;
				case JotyTypes._double: {
					DecimalFormat format = new DecimalFormat(m_isCurrency ? "#,###.##" : "#.##");
					retStr = format.format(m_dblVal);
				}
					break;
				case JotyTypes._single: {
					DecimalFormat format = new DecimalFormat(ForcedNoDecimal ? "#" : "#.##");
					retStr = format.format(m_fltVal);
				}
					break;
				case JotyTypes._date:
				case JotyTypes._dateTime: {
			      	Common common = (Common) ((ApplMessenger) m_jotyApplication).getCommon();
					SimpleDateFormat format = new SimpleDateFormat(dataType() == JotyTypes._dateTime ? 
							common.defDateTimeFormat() : common.defDateFormat());
					retStr = format.format(m_dateVal);
				}
					break;
			}
		return retStr;
	}

	public void typeCheck() {
		if (dataType() != m_jotyTypeFromDb && dataType() != JotyTypes._dbDrivenInteger && m_jotyTypeFromDb != JotyTypes._none)
			m_jotyApplication.jotyWarning("Joty " + JotyTypes.getVerbose(dataType()) + " type is set but Joty " + 
								JotyTypes.getVerbose(m_jotyTypeFromDb) + " type was detected in database FIELD : " + dbFieldName());
	}
}
