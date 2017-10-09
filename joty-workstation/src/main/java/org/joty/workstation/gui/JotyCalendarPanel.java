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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.beans.Beans;
import java.util.Calendar;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.joty.workstation.app.Application;
import org.joty.data.JotyDate;

/**
 * 
 * The Joty implementation of a matrix calendar window.
 * <p>
 * It shows a matrix of days of the currently showed month, grouped in columns
 * as days of week. Then, two 'up' and 'down' buttons are available for the
 * selection of the year and a pull-down menu is for the selection of the month.
 * <p>
 * Its gates for the interaction with the hosting visual environment are the
 * {@code setDate} method and an implementor of the {@code DateActor} interface
 * specified by means of the {@code setActor} method: the {@code action} method
 * will be called upon user action on the calendar.
 * 
 */
public class JotyCalendarPanel extends Panel {

	public interface DateActor {
		public void action();
	}

	/**
	 * 
	 * An AnalogicalSelector capable of hosting a date and exposing a label for
	 * the relative day of the month.
	 * <p>
	 * Its mouse main actuator handler is overriden in order to invoke the
	 * {@code update} method of the container class.
	 * <p>
	 * Its date is set by the dedicated method invoked by {@code relableDaySlot}
	 * method of the container class.
	 * 	 *
	 */
	class DaySlot extends AnalogicalSelector {

		int m_col;
		int m_row;
		static final int m_witdth = 21;
		static final int m_height = 18;
		JLabel m_label;
		Color m_normalColor = null;
		Color m_lightColor = new Color(128, 128, 128);
		int m_year, m_month, m_day;

		DaySlot(int col, int row) {
			super((long) (col + row * 7));
			m_col = col;
			m_row = row;
			setBounds(col * m_witdth, row * m_height + m_deltaY2, m_witdth, m_height);
			m_label = new JLabel();
			add(m_label);
			m_label.setFont(m_font);
			m_normalColor = m_label.getForeground();
			setBorder(m_defaultBorder);
		}

		@Override
		public void doMouseReleased() {
			if (isInside()) {
				m_calendarDate.set(Calendar.DATE, m_day);
				m_calendarDate.set(Calendar.MONTH, m_month);
				m_calendarDate.set(Calendar.YEAR, m_year);
				JotyCalendarPanel.this.update();
			}
			super.doMouseReleased();
		}

		public void set(Calendar calendar, boolean current) {
			m_year = calendar.get(Calendar.YEAR);
			m_month = calendar.get(Calendar.MONTH);
			m_day = calendar.get(Calendar.DATE);
			m_label.setText(String.valueOf(m_day));
			m_label.setForeground(current ? m_normalColor : m_lightColor);
		}

		@Override
		protected void setDefaultBorder() {
			m_defaultBorder = new LineBorder(new Color(128, 128, 128));
		}
	}

	int m_selectedDaySlot;
	public Calendar m_calendarDate = Calendar.getInstance();
	int m_deltaY1 = 17;
	int m_deltaY2 = 34;
	JLabel m_yearLabel;
	JComboBox<String> m_monthComboBox;
	int m_oldMonth, m_oldYear;
	DateActor m_dateActor = null;

	boolean m_sundayIsFDOW;
	Vector<DaySlot> m_daySlots;
	private boolean m_monthComboBoxUpdating;

	private Font m_font;

	public JotyCalendarPanel() {
		if (Beans.isDesignTime())
			return;
		m_font = new Font("Tahoma", Font.PLAIN, 9);
		setLayout(null);
		JLabel dowLabel = null;
		Application app = Application.m_app;
		m_sundayIsFDOW = app.m_common.m_sundayIsFDOW;
		m_calendarDate.setFirstDayOfWeek(m_sundayIsFDOW ? Calendar.SUNDAY : Calendar.MONDAY);
		for (int i = 0; i < 7; i++) {
			dowLabel = new JotyLabel(app.m_common.m_dows.get(i).substring(0, 2));
			add(dowLabel);
			dowLabel.setBounds(DaySlot.m_witdth * i + 3, m_deltaY1, DaySlot.m_witdth, DaySlot.m_height);
		}
		m_daySlots = new Vector<DaySlot>();
		DaySlot daySlot = null;
		for (int j = 0; j < 6; j++)
			for (int i = 0; i < 7; i++) {
				daySlot = new DaySlot(i, j);
				m_daySlots.add(daySlot);
				add(daySlot);
			}
		setBounds(0, 0, DaySlot.m_witdth * 7, DaySlot.m_height * 8);

		boolean macOs = app.m_macOs;
		m_monthComboBox = new JComboBox<String>();
		m_monthComboBox.setBounds(macOs ? -4 : 0, 0, 105, 17);
		add(m_monthComboBox);
		for (int i = 0; i < app.m_common.m_months.size(); i++)
			m_monthComboBox.addItem(app.m_common.m_months.get(i));

		m_monthComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_monthComboBoxUpdating)
					return;
				m_calendarDate.set(Calendar.MONTH, ((JComboBox<String>) e.getSource()).getSelectedIndex());
				update();
			}

		});

		m_yearLabel = new JotyLabel("");
		m_yearLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		m_yearLabel.setBounds(93, 0, 34, 14);
		m_yearLabel.setFont(m_font);

		add(m_yearLabel);

		JotyButton yearUp = new JotyButton("");
		yearUp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				incrementYear(1);
			}
		});
		yearUp.setBounds(128, 0, 20, 7);
		add(yearUp);

		JotyButton yearDown = new JotyButton("");
		yearDown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				incrementYear(-1);
			}
		});
		yearDown.setBounds(128, 8, 20, 7);
		add(yearDown);

		m_selectedDaySlot = -1;
		m_oldMonth = -1;
		m_oldYear = -1;
	}

	public String dow() {
		int index = dowIndex();
		return m_app.m_common.m_dows.get(index < 0 ? index + 7 : index);
	}

	private int dowIndex() {
		return m_calendarDate.get(Calendar.DAY_OF_WEEK) - 1 - (m_sundayIsFDOW ? 0 : 1);
	}

	private void incrementYear(int increment) {
		m_calendarDate.set(Calendar.YEAR, m_calendarDate.get(Calendar.YEAR) + increment);
		update();
	}

	private void relabelDaySlots(int dayIndex, int dayOfWeek) {
		Calendar calendarPen = Calendar.getInstance();
		calendarPen = (Calendar) m_calendarDate.clone();
		int currentMonth = m_calendarDate.get(Calendar.MONTH);
		calendarPen.add(Calendar.DATE, -dayIndex);
		for (int i = 0; i < 42; i++) {
			m_daySlots.get(i).set(calendarPen, calendarPen.get(Calendar.MONTH) == currentMonth);
			calendarPen.add(Calendar.DATE, 1);
		}
	}

	private void selectSlot(int index) {
		if (m_selectedDaySlot >= 0)
			m_daySlots.get(m_selectedDaySlot).setSelectionStatus(false);
		m_daySlots.get(index).setSelectionStatus(true);
		m_selectedDaySlot = index;
	}

	public void setActor(DateActor dateActor) {
		m_dateActor = dateActor;
	}

	public void setDate(JotyDate date) {
		m_calendarDate.setTime(date);
		update();
	}

	public void update() {
		int year = m_calendarDate.get(Calendar.YEAR);
		int month = m_calendarDate.get(Calendar.MONTH);
		m_yearLabel.setText(String.valueOf(year));
		updateMonthComboBox(month);
		int dayOfMonth = m_calendarDate.get(Calendar.DATE);
		int dayOfWeek = dowIndex();

		int rowIndex = 0;
		int remainder = dayOfMonth;
		remainder -= dayOfWeek + 1;
		while (remainder > 0) {
			rowIndex++;
			remainder -= 7;
		}
		int dayIndex = rowIndex * 7 + dayOfWeek;

		selectSlot(dayIndex);

		if (year != m_oldYear || month != m_oldMonth)
			relabelDaySlots(dayIndex, dayOfWeek);
		m_oldYear = year;
		m_oldMonth = month;
		if (m_dateActor != null)
			m_dateActor.action();
	}

	private void updateMonthComboBox(int month) {
		m_monthComboBoxUpdating = true;
		m_monthComboBox.setSelectedIndex(month);
		m_monthComboBoxUpdating = false;
	}

}
