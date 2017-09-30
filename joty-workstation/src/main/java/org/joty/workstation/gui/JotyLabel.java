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

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.JLabel;

import org.joty.access.Logger;
import org.joty.workstation.app.Application;

/**
 * Provides the tool-tip management and the hyper-link graphic syntax and
 * behavior.
 * 
 */
public class JotyLabel extends JLabel {
	boolean m_asHyperlink;
	private String m_uriString;

	public JotyLabel() {
		super();
	}

	public JotyLabel(String text) {
		super(text);
		setToolTipText(text);
	}

	public void setAsHyperLink(String uriString) {
		m_uriString = uriString;
		if (uriString != null) {
			m_asHyperlink = true;
			setText("<html><a href='" + m_uriString + "'>" + m_uriString + "</a></html>");
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent event) {
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(new URI(m_uriString));
						} catch (Exception e) {
							Logger.exceptionToHostLog(e);
						}
					}
				}
			});
		}
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		setToolTipText(text);
	}

	@Override
	public void setToolTipText(String text) {
		super.setToolTipText(Application.m_app.m_toolTipsEnabled() && !m_asHyperlink ? text : null);
	}
}
