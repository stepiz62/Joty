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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.joty.common.Utilities;
import org.joty.workstation.gui.*;
import org.joty.workstation.app.Application;
import org.joty.app.JotyException;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.gui.CheckBox;
import org.joty.workstation.gui.CheckBoxList;
import org.joty.workstation.gui.Factory;
import org.joty.workstation.gui.JotyButton;
import org.joty.workstation.gui.JotyLabel;
import org.joty.workstation.gui.JotyTextField;
import org.joty.workstation.gui.NavigatorPanel;
import org.joty.workstation.gui.DataAccessPanel.ButtonBehavior;

/**
 * Instantiated by the {@code UsersDialog} class allows the access only to the
 * users having the 'Administrators' role.
 * <p>
 * For the data actually provided by this class see the
 * {@link org.joty.basicaccessor.BasicAccessor} class.
 * <p>
 * In the case the configuration item 'addRemoveUsers' is set to true the adding
 * and the deletion of users is also allowed.
 * 
 * @see UsersDialog
 * @see org.joty.basicaccessor.BasicAccessor
 * 
 */
public class UsersPanel extends NavigatorPanel {

	String m_passwordOnNewRecord;
	JotyTextField username;
	private JotyButton btnSetPassword;
	private boolean m_userAccountAddedInDbms;

	public UsersPanel() {
		m_table.setBounds(10, 11, 536, 124);

		String userNameText = jotyLang("UserName");
		String firstNameText = jotyLang("FirstName");
		String lastNameText = jotyLang("LastName");
		username = Factory.createText(this, "username", "USERNAME", 30);
		username.setBounds(101, 151, 156, 20);
		add(username);
		term("username").setMandatory();

		JLabel lblUsername = new JotyLabel(userNameText);
		lblUsername.setHorizontalAlignment(SwingConstants.TRAILING);
		lblUsername.setBounds(10, 154, 86, 14);
		add(lblUsername);

		JotyTextField textFieldP = Factory.createText(this, "firstName", "FIRSTNAME", 20);
		textFieldP.setBounds(101, 182, 156, 20);
		add(textFieldP);

		JotyTextField textFieldP_1 = Factory.createText(this, "lastName", "LASTNAME", 20);
		textFieldP_1.setBounds(101, 210, 156, 20);
		add(textFieldP_1);

		CheckBox chckbxpForcePasswordChange = Factory.createCheck(this, "forcePwdChange", "FORCEPWDCHANGE");
		chckbxpForcePasswordChange.setVerticalAlignment(SwingConstants.TOP);
		chckbxpForcePasswordChange.setText(jotyLang("ReqPwdChange"));
		chckbxpForcePasswordChange.setBounds(9, 271, 289, 20);
		add(chckbxpForcePasswordChange);

		JLabel lblFirstName = new JotyLabel(firstNameText);
		lblFirstName.setHorizontalAlignment(SwingConstants.TRAILING);
		lblFirstName.setBounds(10, 185, 86, 14);
		add(lblFirstName);

		JLabel lblLastName = new JotyLabel(lastNameText);
		lblLastName.setHorizontalAlignment(SwingConstants.TRAILING);
		lblLastName.setBounds(10, 213, 86, 14);
		add(lblLastName);

		m_currentButtonBehavior = ButtonBehavior.free;

		btnSetPassword = new JotyButton(jotyLang("SetPwd"));
		btnSetPassword.setEnabled(false);
		btnSetPassword.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (injectedDialog().m_new_command) {
					ChangePasswordDialog dlg = new ChangePasswordDialog(false);
					if (dlg.perform())
						m_passwordOnNewRecord = dlg.getNewPassword();
				} else {
					String user = getUser();
					m_app.setPassword(user.compareToIgnoreCase(m_app.m_common.m_userName) == 0, user);
				}
			}
		});
		btnSetPassword.setBounds(13, 241, 177, 23);
		add(btnSetPassword);

		CheckBoxList roles = Factory.createCheckBoxList(this, "Roles", "joty_roles", null, "roleID", "userID", null, false);
		roles.setBounds(325, 167, 221, 110);
		add(roles);

		JLabel lblRoles = new JotyLabel(jotyLang("Roles"));
		lblRoles.setBounds(328, 150, 76, 14);
		add(lblRoles);

		addTermToGrid("username", userNameText);
		addTermToGrid("firstName", firstNameText);
		addTermToGrid("lastName", lastNameText);

		addIntegerKeyElem("ID", true, true);

		enableRole("Administrators", m_app.m_common.m_addRemoveUsers ? Permission.all : Permission.readWrite);
	}
	
	boolean delUserAccountInDbms(boolean asNonTransactionManagedRollbackAction) {
		boolean retVal = m_app.m_db.executeSQL("DROP USER " + getUser(), null, null, asNonTransactionManagedRollbackAction ? -1 : 0);
		if (retVal && m_app.m_common.m_dbmsUserSecondaryStatement != null)
			retVal = m_app.m_db.executeSQL("DROP LOGIN " + getUser(), null, null, asNonTransactionManagedRollbackAction ? -2 : 0);
		return retVal;
	}

	@Override
	public boolean doDeletion() {
		if (integerKeyElemVal("ID") == 1) {
			Application.langWarningMsg("AdminDelImpossible");
			return false;
		} else {
			boolean retVal = delUserAccountInDbms(false);
			if (retVal)
				retVal = super.doDeletion();
			return retVal;
		}
	}

	@Override
	public void enableComponents(boolean bState) {
		super.enableComponents(bState);
		btnSetPassword.setEnabled(bState && injectedDialog().m_new_command || !bState && documentIdentified());
	}

	private String getUser() {
		return username.getText().trim();
	}

	@Override
	protected void setContextParams() {
		setContextParam("userID", integerKeyElemVal("ID"));
	}
	
	@Override
	protected boolean storeWFieldsData(WResultSet rs) {
		boolean retVal = super.storeWFieldsData(rs);
		if (retVal && injectedDialog().m_new_command) {
			m_userAccountAddedInDbms = m_app.setUserPwd(getUser(), m_passwordOnNewRecord, "CREATE", null, 1);;
			retVal = m_userAccountAddedInDbms;
			if (retVal && m_app.m_common.m_dbmsUserSecondaryStatement != null)
				retVal = m_app.m_db.executeSQL(String.format(m_app.m_common.m_dbmsUserSecondaryStatement, getUser()), null, null, 2);
			if (retVal)
				retVal = m_app.m_db.executeSQL(String.format(m_app.m_common.m_dbmsUserGrantedRolesStmnt, getUser()));
		}
		return retVal;
	}

	@Override
	protected boolean creationTrigger() {
		m_app.m_paramContext.setContextParam("forcePwdChange", checkTerm("forcePwdChange").getInteger()); // doSetPassword works with m_app.m_paramContext
		return m_app.doSetPassword(getUser(), m_passwordOnNewRecord);
	}

	@Override
	protected void nonManagedRollback() {
		if (m_userAccountAddedInDbms)
			delUserAccountInDbms(true);
	}


	@Override
	protected boolean validateComponents() {
		boolean retVal = super.validateComponents();
		if (retVal && injectedDialog().m_new_command) {
			retVal = m_passwordOnNewRecord != null && m_passwordOnNewRecord.length() > 0;
			if (!retVal)
				Application.langWarningMsg("PwdNeeded");
		}
		return retVal;
	}

}
