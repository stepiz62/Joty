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

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.joty.workstation.app.Application;

/**
 * Acquires locale language and country from the administrator user in order to
 * store them in the database, so that these info last as application
 * attributes.
 * <p>
 * Another task of this class is to read these info from the database, at the
 * start-up of the application, when the containing {@code DataAccessDialog} object is opened
 * as 'fetcher' and to locate them in the {@code m_loc_lang} and
 * {@code m_loc_country} members making them available to the application.
 * 
 * @see AppOptionsDialog
 * @see org.joty.app.Common#acquireLocaleInfo()
 * @see DataAccessPanel#init()
 * @see Application#init(String, String, String, String)
 * 
 */
public class AppOptionsPanel extends DataAccessPanel {
	boolean m_asFetcher;

	public AppOptionsPanel() {
		ComboBox loc_literal = Factory.createBufferedComboBox(this, "loc_literal", "locale_id");
		loc_literal.setBounds(44, 47, 274, 22);
		add(loc_literal);
		term("loc_literal").setMandatory();
		bufferedComboTerm("loc_literal").config("id", "literal", null);

		JLabel lblLocLiteral = new JotyLabel(jotyLang("LocaleLiteral"));
		lblLocLiteral.setHorizontalAlignment(SwingConstants.LEFT);
		lblLocLiteral.setBounds(44, 32, 274, 14);
		add(lblLocLiteral);
		addIntegerKeyElem("ID", true);
		m_asFetcher = getDialog().getMode() == AppOptionsDialog.mode.asFetcher;
		enableRole("Administrators", Permission.readWrite);
	}

	@Override
	protected boolean accessIsAllowed() {
		return m_asFetcher ? true : super.accessIsAllowed();
	}

	@Override
	protected boolean doneWithData() {
		return super.doneWithData() && !m_asFetcher;
	}

	@Override
	public void endEditing(boolean justSaved) {
		if (justSaved) {
			updateEnvVars();
			Application.informationMsg(jotyLang("LocaleUpdateEvent1") + "\n" + jotyLang("LocaleUpdateEvent2") + "\n" + jotyLang("LocaleUpdateEvent3"));
		}
		super.endEditing(justSaved);
	}

	@Override
	protected void postInit() {
		super.postInit();
		if (m_asFetcher)
			updateEnvVars();
	}

	private void updateEnvVars() {
		m_app.m_common.m_loc_lang = m_isNewRec ? "" : bufferedComboTerm("loc_literal").buffer().strValue("language");
		m_app.m_common.m_loc_country = m_isNewRec ? "" : bufferedComboTerm("loc_literal").buffer().strValue("country");
		m_app.m_common.acquireLocaleInfo();
	}
}
