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

import org.joty.workstation.app.Application;
import org.joty.workstation.gui.DataAccessDialog;
import org.joty.workstation.gui.DataAccessPanel;
import org.joty.workstation.gui.Factory;

/**
 * Provides the built-in dialog for listing the roles defined for the
 * application. It embeds in its layout a {@code RolesPanel} instance and keeps
 * the database information not loaded by the Java application when the
 * application runs in web mode because it makes use of the Accessor mode.
 * 
 * @see RolesPanel
 * @see org.joty.basicaccessor.BasicAccessor
 * 
 */
public class RolesDialog extends DataAccessDialog {

	public RolesDialog() {
		super(null, null);
		m_btnHome.setLocation(536, 5);
		m_btnSelect.setBounds(558, 339, 28, 28);

		m_accessorMode = true;

		m_btnSave.setLocation(75, 5);
		Application app = Application.m_app;
		m_buttonPane.setSize(574, 37);
		m_btnClose.setLocation(476, 5);
		m_btnNext.setLocation(242, 5);
		m_btnPrevious.setLocation(213, 5);
		m_btnDelete.setLocation(147, 5);
		m_btnCancel.setLocation(109, 5);
		m_btnEditOrNew.setLocation(75, 5);
		m_btnNew.setLocation(10, 5);
		m_buttonPane.setLocation(2, 282);
		m_contentPanel.setBounds(2, 2, 574, 280);
		setBounds(2, 2, 586, 348);

		setTitle(app.m_common.jotyLang("Roles"));
		m_contentPanel.setLayout(null);
		RolesPanel rolesPanel = new RolesPanel();
		rolesPanel.setBounds(1, 1, 565, 278);
		DataAccessPanel dataAccessPanel = Factory.addDataAccessPanel(m_contentPanel, rolesPanel);
		dataAccessPanel.setBounds(1, 1, 565, 278);
		m_contentPanel.add(dataAccessPanel);
		getContentPane().setLayout(null);
		m_isViewer = true;
	}

	@Override
	protected void setEntityName() {
		m_entityName = "Roles";
	}
}
