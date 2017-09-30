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

/**
 * It is instantiated either for user interaction, being the dialog opened from
 * the application menu, or on the application startup for accessing the
 * database for acquisition of the inherent information.
 * 
 * @see AppOptionsPanel
 *
 */
public class AppOptionsDialog extends DataAccessDialog {

	public enum mode {
		normal, asFetcher
	}

	public AppOptionsDialog(Object context, Object mode) {
		super(context, mode);
		m_accessorMode = true;
		m_btnEditOrNew.setSize(28, 28);
		m_buttonPane.setSize(357, 37);
		m_btnSave.setLocation(59, 5);
		m_btnNew.setLocation(3, 5);
		m_btnEditOrNew.setLocation(59, 5);
		m_btnCancel.setLocation(90, 5);
		m_btnDelete.setLocation(128, 5);
		m_btnPrevious.setLocation(400, 5);
		m_btnNext.setLocation(440, 5);
		m_btnHome.setLocation(288, 5);
		m_btnClose.setLocation(322, 5);
		m_buttonPane.setLocation(3, 139);
		m_contentPanel.setBounds(2, 2, 358, 133);
		setBounds(40, 40, 368, 208);
		DataAccessPanel dataAccessPanel = Factory.addDataAccessPanel(m_contentPanel, new AppOptionsPanel());
		dataAccessPanel.setLocation(1, 1);
		dataAccessPanel.setSize(357, 131);
		m_contentPanel.add(dataAccessPanel);
		getContentPane().setLayout(null);
	}

	@Override
	protected void setEntityName() {
		m_entityName = jotyLang("AppOptions");
	}

}
