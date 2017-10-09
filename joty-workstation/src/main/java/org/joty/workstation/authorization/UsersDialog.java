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

import org.joty.access.Instantiator;
import org.joty.common.ConfigFile;
import org.joty.workstation.app.Application;
import org.joty.workstation.gui.DataAccessDialog;
import org.joty.workstation.gui.DataAccessPanel;
import org.joty.workstation.gui.Factory;

/**
 * Provides the built-in dialog for editing the roles of the users of the
 * application and even, if configured, to add and delete application users. It
 * embeds in its layout a {@code UsersPanel} instance and keeps the database
 * information not loaded by the Java application when the application runs in
 * web mode because it makes use of the Accessor mode.
 * <p>
 * The UserPanel class is instantiated by reflection in order to leave the
 * framework extendible, for user administration, at a 'binary' level, that is
 * without the need to modify the framework itself.
 * 
 * @see UsersPanel
 * @see org.joty.basicaccessor.BasicAccessor
 * @see Instantiator
 * 
 */
public class UsersDialog extends DataAccessDialog {

	public UsersDialog() {
		super(null, null);
		m_btnSelect.setBounds(558, 339, 28, 28);

		m_accessorMode = true;

		m_buttonPane.setSize(561, 37);
		m_buttonPane.setLocation(3, 321);
		m_contentPanel.setBounds(2, 2, 561, 316);
		getContentPane().setLayout(null);
		setBounds(2, 2, 574, 387);
		setTitle(jotyLang("Users"));
		ConfigFile configuration = Application.m_common.m_configuration;
		DataAccessPanel panel = Factory.addDataAccessPanel(m_contentPanel, Application.m_app.createUserPanel());
		if (panel != null) {
			panel.setBounds(100, 101, 200, 50);
			m_contentPanel.add(panel);
		}

	}
	
	@Override
	protected void setEntityName() {
		m_entityName = "Users";
	}
}
