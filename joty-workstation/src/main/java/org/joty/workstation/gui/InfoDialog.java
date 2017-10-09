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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import org.joty.workstation.app.Application;
import java.awt.Window.Type;

/**
 * Is an information dialog to inform the user that some action is taking place.
 * <p>
 * It is typically closed from the context by which it has been opened, indeed.
 * <p>
 * Optionally it receives a (running) {@code Thread} object as parameter. In
 * this case the dialog shows a button by which the user can stop the thread
 * execution.
 * 
 * @see Application#openInfoDialog(String, Thread, boolean)
 * @see Application#closeInfoDialog(boolean)
 * 
 */
public class InfoDialog extends JDialog {

	JPanel m_contentPanel = new Panel();
	JLabel m_lblMessage;

	public InfoDialog(Window owner, String message, final Thread thread) {
		super(owner);
		setUndecorated(true);
		m_contentPanel.setOpaque(true);
		m_contentPanel.setBorder(new LineBorder(new Color(0, 0, 0), 2, true));
		m_contentPanel.setLayout(null);
		m_lblMessage = new JotyLabel();
		m_lblMessage.setOpaque(true);
		m_lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
		m_lblMessage.setBounds(77, 33, 458, 45);
		m_contentPanel.add(m_lblMessage);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(m_contentPanel, BorderLayout.CENTER);

		if (thread != null) {
			setAlwaysOnTop(true);
			JotyButton btnInterrupt = new JotyButton(Application.m_common.jotyLang("Interrupt"));
			btnInterrupt.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					thread.interrupt();
				}
			});
			btnInterrupt.setBounds(536, 82, 91, 23);
			m_contentPanel.add(btnInterrupt);
		} 
		setResizable(false);
		setBounds(100, 100, 632, 111);
		setText(message);
	}
	
	@Override
	public void repaint() {
		paintAll(getGraphics());
		m_contentPanel.paintAll(m_contentPanel.getGraphics());
		m_lblMessage.paintAll(m_lblMessage.getGraphics());
	}

	public void setText(String text) {
		m_lblMessage.grabFocus();
		m_lblMessage.setText("........ " + text + " ........");
	}
}
