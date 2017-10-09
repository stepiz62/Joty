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
 * A pure layout overriding that lets the searcher to extend vertically.
 *
 */
public class SearcherMultiPanelDialog_V extends SearcherMultiPanelDialog {
	public SearcherMultiPanelDialog_V(Object callContext, Object openingMode, boolean initAction) {
		super(callContext, openingMode, initAction);
		m_lblManage.setSize(101, 14);
		m_searcherPanel.m_lblCriteria.setBounds(12, 57, 46, 14);
		getContentPane().setLocation(-10, -5);
		m_searcherPanel.m_lblSearchResult.setLocation(12, 194);
		m_searcherPanel.m_btnFind.setLocation(165, 45);
		m_searcherPanel.m_btnPrevious.setLocation(165, 11);
		m_searcherPanel.setBounds(10, 14, 637, 280);
		m_searcherPanel.setBounds(10, 23, 239, 577);
		m_searcherPanel.m_criteriaContainer.setBounds(10, 24, 223, 82);
		m_searcherPanel.m_table.setBounds(10, 211, 218, 355);
		m_searcherPanel.m_criteriaContainer.setBounds(10, 75, 218, 108);
		m_buttonPane.setSize(605, 40);
		m_lblSearch.setLocation(10, 5);
		m_lblManage.setLocation(258, 5);
		m_contentPanel.setSize(605, 526);
		m_tabbedPane.setBounds(6, 15, 593, 500);
		m_contentPanel.setLocation(258, 23);
		m_buttonPane.setLocation(258, 560);
		setBounds(12, 57, 873, 628);
	}

}
