/*
	Copyright (c) 2013-2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

	This file is part of Joty 2.0 Workstation.

	Joty 2.0 Workstation is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Joty 2.0 Workstation is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with Joty 2.0 Workstation.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.joty.workstation.gui;

import java.beans.Beans;

import org.joty.data.JotyDate;
import org.joty.workstation.app.Application;
import org.joty.workstation.gui.JotyCalendarPanel.DateActor;

/**
 * A JotyDialog implementation dedicated to host a JotyCalendarPanel and to
 * serve the rendering of a date value.
 * 
 */
public class JotyCalendarDialog extends JotyDialog {
	JotyCalendarPanel m_calendar;
	JotyDate m_day;

	public JotyCalendarDialog(boolean sundayIsFDOW, final JotyTextField caller) {
		m_btnOk.setBounds(114, 344, 28, 28);
		m_buttonPane.setBounds(2, 339, 596, 37);
		m_contentPanel.setBounds(2, 2, 149, 143);
		m_calendar = new JotyCalendarPanel();
		m_contentPanel.add(m_calendar);
		getContentPane().setLayout(null);
		setBounds(2, 2, 157, 174);
		m_day = new JotyDate(Application.m_app, "now");
		caller.m_term.guiDataExch(true);
		if (!caller.m_term.isNull())
			m_day.setTime(caller.m_term.m_dateVal.getTime());
		if (!Beans.isDesignTime())
			m_calendar.setActor(new DateActor() {
				@Override
				public void action() {
					m_day.setTime(m_calendar.m_calendarDate.getTime().getTime());
					caller.setTextAndNotify(m_day.render());
				}
			});
		setTitle(jotyLang("Calendar"));
	}

	@Override
	public boolean initChildren() {
		m_calendar.setDate(m_day);
		return super.initChildren();
	}

}
