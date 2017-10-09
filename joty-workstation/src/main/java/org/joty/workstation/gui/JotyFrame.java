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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;

import org.joty.access.Logger;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.JotyDB;

/**
 * The main window of the Joty application.
 * <p>
 * On exiting it is responsible, with the {@code Application} class, to ask each
 * application window to close.
 * <p>
 * It can assume two states: the normal one, as the application starts and as
 * background of communications to the user at the application level, and second
 * one consisting in a reduced floating and always on top form that encloses
 * just the application menu.
 * <p>
 * The class provides methods to switch to and from the reduced form and the
 * framework invokes them to reduce the JotyFrame instance at every opening of a
 * {@code DataAccessDialog} object.
 *
 * @see Application
 * @see DataAccessDialog
 */
public class JotyFrame extends JFrame {

	private Rectangle m_defaultRect;
	private Rectangle m_rectAsBar;

	final String m_appFrameName = "____JOTY_APPFRAME";

	private boolean m_initialized;

	public JotyFrame() {
		setBounds(100, 100, 375, 45);
		m_defaultRect = new Rectangle();
		m_rectAsBar = new Rectangle();
		m_rectAsBar = getBounds();
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				Application app = Application.m_app;
				if (app.m_committedClose || (app.m_exitByMenu && app.m_openedDialogs.size() == 0) || Application.langYesNoQuestion("WantExitApp", JotyFrame.this)) {
					Boolean terminating = true;
					try {
						terminating = app.closeAllWindows();
					} catch (Exception ex) {
						app.m_common.resetRemoteTransactionBuilding();
					}
					if (terminating) {
						app.endApp();
						if (app.m_webMode) {							
							if (app.m_common.m_webSessionOn)
								app.m_webClient.endSession();
						} else if (JotyDB.m_conn != null) {
							try {
								JotyDB.m_conn.close();
							} catch (SQLException ex) {
								Logger.exceptionToHostLog(ex);
							}
						}
						dispose();
					}
				}
				app.m_exitByMenu = false;
			}
		});
	}

	public void close() {
		if (Application.m_app != null)
			Application.m_app.closeContainer(this);
	}

	public void setAsFloatingBar(boolean selected) {
		setAsFloatingBar(selected, false);
	}

	public void setAsFloatingBar(boolean selected, boolean flipFlop) {
		Application app = Application.m_app;
		JCheckBoxMenuItem setAsFloatMenu = app.m_mntmSetFrame;
		if (setAsFloatMenu != null) {
			if (setAsFloatMenu.isSelected() == selected && !flipFlop)
				return;
			setAsFloatMenu.setSelected(selected);
		}
		setAlwaysOnTop(selected);
		boolean init = app.m_windowsLocations.get(m_appFrameName) == null;
		if (!init && !selected && m_initialized)
			app.m_windowsLocations.put(m_appFrameName, getLocation());
		setPreferredSize(new Dimension(selected ? m_rectAsBar.width : m_defaultRect.width, selected ? m_rectAsBar.height : m_defaultRect.height));
		setResizable(!selected);
		pack();
		if (selected)
			setLocationAsBar();
		else if (init)
			app.m_windowsLocations.put(m_appFrameName, getLocation());
		if (!m_initialized)
			m_initialized = true;
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		Application.center(this, width, height);
	}

	public void setDefaultRectDim(int width, int height) {
		m_defaultRect.setBounds(100, 100, width, height);
	}

	public void setLocationAsBar() {
		Point location = Application.m_app.m_windowsLocations.get(m_appFrameName);
		setLocationAsBar(location == null ? getLocation() : location);
	}

	public void setLocationAsBar(Point location) {
		Application.setLocation(this, location);
	}

	public void setRectDimAsBar(int width, int height) {
		m_rectAsBar.setBounds(100, 100, width, height);
	}

}
