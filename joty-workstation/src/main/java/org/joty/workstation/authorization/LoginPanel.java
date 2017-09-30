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

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.joty.workstation.gui.*;
import org.joty.workstation.gui.Factory;
import org.joty.workstation.gui.JotyLabel;
import org.joty.workstation.gui.JotyTextField;
import org.joty.workstation.gui.PasswordLabeledField;
import org.joty.workstation.gui.TermContainerPanel;

/**
 * Instantiated by the {@code LoginDialog} class allows the acquisition of the
 * user's credentials.
 * <p>
 * If the shared mode is active it presents the user, in addition to 'user' and
 * 'password' boxes, a further box for the sharing key.
 * 
 * @see LoginDialog
 */
public class LoginPanel extends TermContainerPanel {

	public JotyTextField m_userName;
	public PasswordLabeledField m_password;
	public JotyTextField m_sharingKey;
	public boolean m_sharedApp;


	public LoginPanel(boolean sharedApp) {
		super();
		m_sharedApp = sharedApp;
		JLabel lblUserName = new JotyLabel();
		lblUserName.setHorizontalAlignment(SwingConstants.RIGHT);

		lblUserName.setBounds(4, sharedApp ? 50 : 14, 68, 14);
		add(lblUserName);
		lblUserName.setText(jotyLang("UserName"));

		m_userName = Factory.createText(this, "uname", null, 30);
		m_userName.setBounds(76, sharedApp ? 47 : 11, 102, 20);
		add(m_userName);

		m_password = new PasswordLabeledField(this, jotyLang("Password"));
		m_password.setBounds(-62, sharedApp ? 75 : 39, 253, 40);
		add(m_password);

		if (sharedApp) {
			JLabel lblSharingKey = new JotyLabel();
			lblSharingKey.setText(jotyLang("SharingKeyLBL"));
			lblSharingKey.setHorizontalAlignment(SwingConstants.RIGHT);
			lblSharingKey.setBounds(4, 13, 68, 14);
			add(lblSharingKey);

			m_sharingKey = Factory.createText(this, "sharingKey", (String) null, 32);
			m_sharingKey.setBounds(76, 10, 208, 20);
			add(m_sharingKey);
		}
	}

	@Override
	public boolean init() {
		boolean retVal = super.init();
		if (m_sharedApp)
			m_sharingKey.setText(m_app.m_common.m_sharingKey);
		return retVal;
	}
	
	
}
