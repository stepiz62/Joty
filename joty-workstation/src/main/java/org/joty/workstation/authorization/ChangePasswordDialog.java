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

import org.joty.workstation.app.Application;
import org.joty.workstation.app.Application.PasswordValidator;
import org.joty.workstation.gui.Factory;
import org.joty.workstation.gui.JotyDialog;

/**
 * Acquires the new password from the user and manages the change
 * Supports optionally the old password verification. 
 *
 * @see ChangePasswordPanel
 *
 */
public class ChangePasswordDialog extends JotyDialog {

	private String m_gotPwd;
	public ChangePasswordPanel m_chngPwdPanel;
	private boolean m_getOldPassword;

	public ChangePasswordDialog(boolean getOldPassword) {
		super();
		setModal(true);
		setModalityType(ModalityType.TOOLKIT_MODAL);
		m_getOldPassword = getOldPassword;
		m_btnOk.setBounds(107, 99, 69, 23);
		m_buttonPane.setBounds(3, 135, 288, 47);
		m_contentPanel.setBounds(3, 0, 288, 134);
		m_defaultButton.setBounds(167, 11, 28, 28);
		m_btnCancel.setBounds(90, 11, 28, 28);
		m_gotData = false;
		setTitle(jotyLang("PwdChange"));
		setBounds(100, 100, 294, 207);
		m_contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(m_contentPanel);
		m_contentPanel.setLayout(null);

		m_chngPwdPanel = (ChangePasswordPanel) Factory.addTermContainerPanel(m_contentPanel, new ChangePasswordPanel(m_getOldPassword));
		m_panelsTobeInited.add(m_chngPwdPanel);
		m_chngPwdPanel.setBounds(1, 1, 285, 134);
		m_contentPanel.add(m_chngPwdPanel);
		m_buttonPane.setLayout(null);
		getContentPane().setLayout(null);
	}

	public String getNewPassword() {
		return m_gotPwd;
	}

	@Override
	public void onCancel() {
		close();
	}

	@Override
	public void onOK() {
		boolean valid = true;
		if (m_getOldPassword) {
			if (m_chngPwdPanel.m_oldPassword.getContent().compareTo(m_app.m_common.m_password) != 0) {
				valid = false;
				Application.langWarningMsg("WrongOldPwd");
			}
		}
		if (valid)
			if (m_chngPwdPanel.m_password1.getContent().compareTo(m_chngPwdPanel.m_password2.getContent()) == 0) {
				m_gotPwd = m_chngPwdPanel.m_password1.getContent();
			} else {
				valid = false;
				Application.langWarningMsg("NewPwdNotRepeated");
			}
		if (valid && (m_app.m_passwordValidator == null || m_app.m_passwordValidator.validate(m_gotPwd))) {
			m_gotData = true;
			super.onOK();
		}
	}
}
