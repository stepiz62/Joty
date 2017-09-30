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

import org.joty.workstation.gui.PasswordLabeledField;
import org.joty.workstation.gui.TermContainerPanel;

/**
 * Uses instances of the {@code PasswordLabeledField} class for serving the
 * containing {@code ChangePasswordDialog} instance.
 *
 * @see PasswordLabeledField
 * @see ChangePasswordDialog
 *
 */
public class ChangePasswordPanel extends TermContainerPanel {

	public PasswordLabeledField m_oldPassword;
	public PasswordLabeledField m_password1;
	public PasswordLabeledField m_password2;

	public ChangePasswordPanel(boolean getOldPassword) {
		super();

		if (getOldPassword) {
			m_oldPassword = new PasswordLabeledField(this, jotyLang("OldPwd"));
			m_oldPassword.setBounds(0, 0, 253, 40);
			add(m_oldPassword);
		}
		m_password1 = new PasswordLabeledField(this, jotyLang("NewPwd"));
		m_password1.setBounds(0, getOldPassword ? 40 : 20, 253, 40);
		add(m_password1);

		m_password2 = new PasswordLabeledField(this, jotyLang("RepeatNewPwd"));
		m_password2.setBounds(0, getOldPassword ? 80 : 60, 253, 40);
		add(m_password2);
	}

}
