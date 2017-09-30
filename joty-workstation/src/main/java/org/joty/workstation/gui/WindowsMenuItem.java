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

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import org.joty.workstation.app.Application;

public class WindowsMenuItem {
	JMenuItem m_item;
	Window m_window;

	WindowsMenuItem(String text, Window window) {
		m_item = new JMenuItem(text);
		m_window = window;
		Application.m_app.m_windowsMenu.add(m_item);

		m_item.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_window instanceof JotyDialog)
					((JotyDialog) m_window).m_activatedByWindowsMenu = true;
				m_window.setVisible(true);
			}
		});
	}

	void remove() {
		Application.m_app.m_windowsMenu.remove(m_item);
	}

}
