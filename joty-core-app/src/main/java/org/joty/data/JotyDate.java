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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.joty.app.Common;
import org.joty.app.JotyApplication;
import org.joty.common.ApplMessenger;
import org.joty.common.IJotyDate;
import org.joty.common.JotyTypes;


/**
 * It extends the java.util.Date class to provide by itself either presentation
 * options and or a validation process, according to the configuration choices
 * made in the application.
 * <p>
 * The class hosts the Joty datum of {@code _date} and {@code _dateTime} type.
 * if provides also the rendering of the value for use in sql expressions
 * according to the underlying dbms requirements.
 *
 * @see org.joty.common.JotyTypes
 */
public  class JotyDate extends Date implements IJotyDate{

	JotyApplication m_app;
	Common m_common;

    public boolean m_isNull;

    private void init(JotyApplication app) {
     	m_app = app;
    	m_common = (Common) ((ApplMessenger) app).getCommon();
        m_isNull = false;
    }
    
    public JotyDate(JotyApplication app) {
        init(app);
    }

    public JotyDate(JotyApplication app, long secs) {
        super(secs);
        init(app);
     }

    public JotyDate(JotyApplication app, String date) {
        this(app);
        if (date.compareToIgnoreCase("now") == 0)
            setDate(getSqlDate());
        else
            setDate(date, false);
    }


    public java.sql.Date getSqlDate() {
        return new java.sql.Date(getTime());
    }

    public java.sql.Timestamp getSqlDateTime() {
        return new java.sql.Timestamp(getTime());
    }

    public boolean IsEmpty() {
        return false;
    }

    public String render() {
        return render(false, false);
    }


    public void setDate(java.sql.Date date) {
        if (date == null)
            m_isNull = true;
        else {
            setTime(date.getTime());
            m_isNull = false;
        }
    }

    public void setDate(java.sql.Timestamp dateTime) {
        if (dateTime == null)
            m_isNull = true;
        else {
            setTime(dateTime.getTime());
            m_isNull = false;
        }
    }

    public void setDate(JotyDate date) {
        if (date == null)
            m_isNull = true;
        else {
            if (date.m_isNull)
                m_isNull = true;
            else {
                setTime(date.getTime());
                m_isNull = false;
            }
        }
    }

    public boolean setDate(String date, boolean withTime) {
        return setDate(date, false, withTime);
    }

    public boolean setDate(String date, boolean webFormat, boolean withTime) {
        return setDate(date, webFormat, withTime, false);
    }

    public boolean setDate(String date, boolean webFormat, boolean withTime, boolean dbmsFormat) {
        boolean retVal = false;
        if (date.compareToIgnoreCase(emptyDateRendering(withTime)) == 0) {
            m_isNull = true;
            retVal = true;
        } else {
            SimpleDateFormat formatter = new SimpleDateFormat(webFormat ?
                    (dbmsFormat ?
                            (withTime ? dbmsDateTimeFormat() : dbmsDateFormat()) :
                            xmlDateFormat()) :
                    (withTime ? defDateTimeFormat() : defDateFormat()));
            Date utildate = null;
            try {
                utildate = formatter.parse(date);
                if (formatter.format(utildate).toString().compareTo(date) == 0)
                    retVal = true;
            } catch (ParseException e) {}
            m_isNull = utildate == null;
            if (!m_isNull)
                setTime(utildate.getTime());
        }
        return retVal;
    }

    @Override
    public void setTime(long time) {
        super.setTime(time);
        m_isNull = false;
    }


    public String toString(boolean withTime) {
        if (m_isNull)
            return emptyDateRendering(false);
        else {
            SimpleDateFormat df = new SimpleDateFormat();
            df.applyPattern(withTime ? defDateTimeFormat() : defDateFormat());
            return df.format(this);
        }
    }

    public boolean validate() {
        Calendar calendarByThis = Calendar.getInstance();
        calendarByThis.setTime(this);
        return validate(calendarByThis.get(Calendar.DAY_OF_MONTH),
                calendarByThis.get(Calendar.MONTH) + 1,
                calendarByThis.get(Calendar.YEAR),
                calendarByThis.get(Calendar.HOUR_OF_DAY),
                calendarByThis.get(Calendar.MINUTE),
                calendarByThis.get(Calendar.SECOND));
    }

    public boolean validate(int day, int month, int year, int hour, int minute, int second) {
        boolean bRetVal = true;
        int LastDayOfMonth;
        if (month == 2) {
            LastDayOfMonth = 28;
            if (year % 400 == 0)
                LastDayOfMonth = 29;
            else if (year % 4 == 0 && year % 100 != 0)
                LastDayOfMonth = 29;
        } else if (month == 4 || month == 6 || month == 9 || month == 11)
            LastDayOfMonth = 30;
        else
            LastDayOfMonth = 31;
        if (day < 1 || day > LastDayOfMonth)
            bRetVal = false;
        if (month < 1 || month > 12)
            bRetVal = false;
        if (hour < 0 || hour > 23)
            bRetVal = false;
        if (minute < 0 || minute > 59)
            bRetVal = false;
        if (second < 0 || second > 59)
            bRetVal = false;
        return bRetVal;
    }

    /**
     * Returns a JotyDate instance  representing a date that is far by {@code nDays} days from the date received as second parameter.
     * @param nDays the distance in days (may less than zero)
     * @param refDate the base date
     * @return the computed date
     */

    JotyDate getDelayedDate(int nDays, JotyDate refDate) {
        return new JotyDate(m_app, refDate.getTime() + (nDays * (long) 24 * 60 * 60));
    }

    @Override
	public String render(boolean forSqlExpr, boolean withTime) {
		return m_app.isDesignTime() ? null : (forSqlExpr ? sqlValueExpr(withTime) : toString(withTime));
	}

	public String defDateFormat() {
		return m_common.m_dateFormat;
	}

	public  String defDateTimeFormat() {
		return m_common.m_dateTimeFormat;
	}

	public String emptyDateRendering(boolean withTime) {
		return withTime ? m_common.m_emptyDateTimeRendering : m_common.m_emptyDateRendering;
	}

	public String dbmsDateTimeFormat() {
		return m_common.m_dbmsDateTimeFormat;
	}

	public String dbmsDateFormat() {
		return m_common.m_dbmsDateFormat;
	}

	public String sqlValueExpr(boolean withTime) {
		return m_isNull ? "NULL" : String.format(m_common.m_sqlDateExpr, (withTime ? getSqlDateTime() : getSqlDate()).toString());
	}

	public String xmlDateFormat() {
		return m_common.m_xmlDateFormat;
	}

    public String dow() {
        Calendar calendarByThis = Calendar.getInstance();
        calendarByThis.setTime(this);
        int index = calendarByThis.get(Calendar.DAY_OF_WEEK) - 1 - (m_common.m_sundayIsFDOW ? 0 : 1);
        return m_common.m_dows.get(index < 0 ? index + 7 : index);
    }

    
}
