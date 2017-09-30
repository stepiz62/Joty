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

package org.joty.workstation.authorization;

import javax.swing.border.EmptyBorder;

import org.joty.workstation.gui.Factory;
import org.joty.workstation.gui.JotyDialog;

/**
 * Provides the built-in login dialog for the Joty application and, for this,
 * it embeds in its layout a {@code JotyPanel} instance.
 * 
 * @see LoginPanel
 * 
 */
public class LoginDialog extends JotyDialog {

	public boolean m_valid;
	public boolean m_abandoned;
	private String m_gotPwd;
	public LoginPanel m_loginPanel;

	public LoginDialog(boolean sharedApp) {
		super();
		m_btnOk.setBounds(107, 99, 69, 23);
		m_buttonPane.setBounds(0, sharedApp ? 124 : 79, sharedApp ? 300 : 186, 49);
		m_contentPanel.setBounds(0, 0, sharedApp ? 300 : 186, sharedApp ? 123 : 81);
		m_defaultButton.setBounds(sharedApp ? 220 : 117, 10, 28, 28);
		m_btnCancel.setBounds(40, 10, 28, 28);
		m_gotData = false;
		m_valid = false;
		setTitle(jotyLang("Login"));
		setBounds(100, 100, sharedApp ? 306 : 191, sharedApp ? 198 : 153);
		m_contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(m_contentPanel);
		m_contentPanel.setLayout(null);

		m_loginPanel = (LoginPanel) Factory.addTermContainerPanel(m_contentPanel, new LoginPanel(sharedApp));
		m_panelsTobeInited.add(m_loginPanel);
		m_loginPanel.setBounds(1, 1, sharedApp ? 273 : 184, sharedApp ? 123 : 79);
		m_contentPanel.add(m_loginPanel);
		m_buttonPane.setLayout(null);
		getContentPane().setLayout(null);
	}

	public String getPassword() {
		return m_gotPwd != null ? m_gotPwd.trim() : null;
	}

	public String getSharingKey() {
		return m_loginPanel.m_sharingKey.getText().trim();
	}

	public String getUserName() {
		return m_loginPanel.m_userName.getText().trim();
	}

	@Override
	public void onCancel() {
		m_valid = false;
		m_abandoned = true;
		m_loginPanel.m_userName.setText(null);
		m_loginPanel.m_password.m_password.setText(null);
		m_gotPwd = null;
		close();
	}

	@Override
	public void onOK() {
		m_gotPwd = m_loginPanel.m_password.getContent();
		m_gotData = true;
		super.onOK();
	}

}

