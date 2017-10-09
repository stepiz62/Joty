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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

import org.joty.workstation.app.Application;

/**
 * Provides a visual object containing a {@code JotyPasswordField} object plus a
 * label that describes it.
 * 
 * @see JotyPasswordField
 * 
 */
public class PasswordLabeledField extends JPanel {
	/**
	 * A child of JPasswordField class enriched with the capability to map the
	 * 'Enter' key action on the action mapped on the default button of the
	 * containing {@code Panel} instance.
	 * <p>
	 * Furthermore the {@code getContent} method extracts the text content.
	 * 
	 * @see Panel#doClickOnDefaultButton()
	 *
	 */
	public class JotyPasswordField extends JPasswordField {

		private Panel m_panel;

		JotyPasswordField(Panel panel) {
			m_panel = panel;
			addKeyListener(new KeyListener() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER)
						m_panel.doClickOnDefaultButton();
				}

				@Override
				public void keyReleased(KeyEvent e) {}

				@Override
				public void keyTyped(KeyEvent e) {}
			});
		}

		public String getContent() {
			char[] input = getPassword();
			StringBuilder retVal = new StringBuilder();
			for (int i = 0; i < input.length; i++)
				retVal.append(input[i]);
			return retVal.toString();
		}
	}

	public JLabel lbl;
	public JotyPasswordField m_password;

	public PasswordLabeledField(TermContainerPanel parentPanel, String caption) {
		super();
		Application.checkWBE(this);
		setLayout(null);

		lbl = new JotyLabel();
		lbl.setBounds(10, 14, 124, 14);
		add(lbl);
		lbl.setText(caption);
		lbl.setHorizontalAlignment(SwingConstants.RIGHT);

		m_password = new JotyPasswordField(parentPanel);
		m_password.setBounds(138, 11, 102, 20);
		add(m_password);
		m_password.setColumns(Application.m_common.m_passwordLen);

	}

	public String getContent() {
		return m_password.getContent();
	}

	@Override
	public void hide() {
		m_password.setVisible(false);
		lbl.setVisible(false);
	}
}
