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
import org.joty.workstation.gui.NavigatorPanel;
import org.joty.workstation.gui.Table;
import org.joty.workstation.gui.TextArea;

/**
 * Instantiated by the {@code RolesDialog} class allows the access only to the
 * users having the 'Administrators' role.
 * 
 * @see RolesDialog
 */
public class RolesPanel extends NavigatorPanel {

	public RolesPanel() {
		m_table.setBounds(10, 11, 552, 114);
		String nameText = jotyLang("Name");
		String descriptionText = jotyLang("Description");
		JLabel lblName = new JotyLabel(nameText);
		lblName.setHorizontalAlignment(SwingConstants.RIGHT);
		lblName.setBounds(20, 136, 46, 14);
		add(lblName);

		JotyTextField textFieldP_1 = Factory.createText(this, "Name", "Name", 50);
		textFieldP_1.setBounds(68, 136, 494, 20);
		add(textFieldP_1);

		JLabel lblRole = new JotyLabel(descriptionText);
		lblRole.setHorizontalAlignment(SwingConstants.RIGHT);
		lblRole.setBounds(5, 167, 60, 14);
		add(lblRole);

		TextArea textArea = Factory.createTextArea(this, "description", "description", 512);
		textArea.setBounds(68, 168, 494, 98);
		add(textArea);

		addTermToGrid("Name", nameText);
		addTermToGrid("description", descriptionText);
		addIntegerKeyElem("ID", true, true);

		enableRole("Administrators", Permission.read);
	}

	@Override
	protected void setGridFormat(Table table) {
		table.m_colWidths = new int[] { 100, 300 };
	}

}
