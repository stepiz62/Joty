/*
	Copyright (c) 2013-2017, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

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

import java.awt.Font;
import java.beans.Beans;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.joty.common.Utilities;
import org.joty.workstation.app.Application;

/**
 * It is a parameterized About dialog class the opening of which is driven by
 * the menu item offered by the framework or driven by the Os platform in the
 * case it is MacOs X.
 * 
 */
public class AboutDialog extends JotyDialog {
	public AboutDialog() {
		Font largeFont = new Font("Tahoma", Font.PLAIN, 18);
		Font mediumFont = new Font("Tahoma", Font.PLAIN, 13);
		m_contentPanel.setBorder(null);
		Application app = Application.m_app;
		setTitle(String.format(app.m_common.jotyLang("About"), "..."));
		m_contentPanel.setBounds(2, 2, 344, 208);

		JLabel lblApplication = new JLabel(app.m_name);
		lblApplication.setBounds(11, 73, 124, 24);
		m_contentPanel.add(lblApplication);
		lblApplication.setFont(new Font("Tahoma", Font.PLAIN, 15));

		JLabel logo = new JLabel();
		logo.setBounds(9, 5, 321, 63);
		m_contentPanel.add(logo);
		logo.setIcon(Beans.isDesignTime() ? null : Application.m_app.m_appLogo);

		JLabel lblVersion = new JLabel(String.format(app.m_common.jotyLang("Version"), app.m_versionString));
		lblVersion.setBounds(139, 73, 127, 24);
		m_contentPanel.add(lblVersion);
		lblVersion.setFont(new Font("Tahoma", Font.PLAIN, 13));

		JLabel lblMoreInfo = new JLabel(app.m_common.jotyLang("BasedOn"));
		lblMoreInfo.setHorizontalAlignment(SwingConstants.RIGHT);
		lblMoreInfo.setBounds(5, 137, 110, 18);
		m_contentPanel.add(lblMoreInfo);

		JLabel jotyLogo = new JLabel();
		jotyLogo.setBounds(120, 130, 32, 32);
		m_contentPanel.add(jotyLogo);
		jotyLogo.setIcon(Beans.isDesignTime() ? null : Application.m_app.m_jotyLogo);

		JLabel lblJoty = new JLabel("Joty 2.0");
		lblJoty.setBounds(157, 137, 64, 18);
		m_contentPanel.add(lblJoty);
		setBounds(0, 0, 347, 233);
		lblJoty.setFont(new Font("SansSerif", Font.PLAIN, 15));

		JotyLabel webJoty = new JotyLabel();
		webJoty.setHorizontalAlignment(SwingConstants.LEFT);
		webJoty.setBounds(224, 137, 110, 18);
		webJoty.setAsHyperLink("http://www.joty.org");
		m_contentPanel.add(webJoty);

		JotyLabel webSite = new JotyLabel();
		webSite.setHorizontalAlignment(SwingConstants.LEFT);
		webSite.setAsHyperLink(app.m_applicationLink);
		webSite.setBounds(11, 106, 317, 18);
		m_contentPanel.add(webSite);

		JLabel copyrightNote = new JLabel("copyright");
		copyrightNote.setHorizontalAlignment(SwingConstants.CENTER);
		copyrightNote.setBounds(8, 180, 320, 18);
		m_contentPanel.add(copyrightNote);
		copyrightNote.setText(app.m_copyrightYears != null ? ("@ " + app.m_copyrightYears + " " + app.m_author) : null);
	}
}
