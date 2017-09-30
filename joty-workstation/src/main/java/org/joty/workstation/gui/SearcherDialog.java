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

import javax.swing.event.ListSelectionEvent;

import org.joty.workstation.app.Application;
import org.joty.workstation.gui.SearcherPanel.SearcherPanelContainer;

/**
 * Embeds a {@code SearcherPanel} object in its layout.
 * <p>
 * Its descendant must instantiate a CriteriaPanel object and add it to the
 * SearcherPanel.m_criteriaContainer member layout.
 * <p>
 * In order the resulting data to have further processing a key must be define
 * in the constructor on the descendant by invoking addLongKeyElem.
 * <p>
 * The class may equips the data result grid with the ability to open a details
 * dialog. It is fulfilled by overriding the
 * {@code JotyDialog.openDetailsDialog()} method in the descendant class,
 * invoking, there, the {@code openDetailsDialog(JotyDialog dlg)} method with an
 * instance of the details dialog.
 * <p>
 * The implementation of the descendant of this class is a good candidate to be
 * instantiated by the {@code TermContainerPanel.acquireSelectedValueFrom}
 * method.
 * 
 * @see SearcherPanel
 * @see JotyDialog
 * @see TermContainerPanel#acquireSelectedValueFrom
 *
 */
public class SearcherDialog extends JotyDialog implements SearcherPanelContainer {

	public SearcherPanel m_searcherPanel;

	public SearcherDialog(Object callContext, Object openingMode) {
		super(callContext, openingMode);
		m_btnCancel.setVisible(false);
		m_btnOk.setVisible(false);
		m_btnClose.setBounds(431, 11, 63, 23);

		m_contentPanel.setBounds(2, 325, 505, 10);
		m_buttonPane.setBounds(2, 336, 505, 40);
		m_searcherPanel = Factory.createSearcherPanel(this);
		m_searcherPanel.m_btnSelect.setLocation(307, 93);
		m_searcherPanel.m_btnPrevious.setLocation(307, 2);
		m_searcherPanel.m_lblSearchResult.setLocation(10, 162);
		m_searcherPanel.m_criteriaContainer.setBounds(10, 28, 287, 88);
		m_searcherPanel.m_table.setBounds(10, 187, 439, 97);
		m_searcherPanel.m_btnFind.setLocation(307, 28);
		m_searcherPanel.setBounds(10, 11, 496, 310);
		getContentPane().add(m_searcherPanel);
		if (Application.m_app.m_accessor != null)
			m_searcherPanel.m_panelDataDef = Application.m_app.m_accessor.getPanelDataDef(getClass().getName(), 0);

	}

	public void addLongKeyElem(String name) {
		m_searcherPanel.addLongKeyElem(name);
	}

	public void addStrKeyElem(String name) {
		m_searcherPanel.addStrKeyElem(name);
	}

	@Override
	protected void clearAppReferences() {
		m_searcherPanel.m_criteriaPanel.clearAppReferences();
	}

	@Override
	public JotyButton getSelectorButton() {
		return m_searcherPanel.m_btnSelect;
	}

	@Override
	public boolean initChildren() {
		m_searcherPanel.m_criteriaPanel.m_gridManager = m_searcherPanel.m_gridManager;
		m_searcherPanel.injectIntoBuffer();
		m_searcherPanel.m_criteriaPanel.init();
		return true;
	}

	@Override
	protected boolean initDialog() {
		boolean retVal = super.initDialog();
		m_searcherPanel.initContainer(this, null);
		return retVal;
	}

	public SearcherPanel m_searcherPanel() {
		return m_searcherPanel;
	}

	@Override
	void onGridSelChange(ListSelectionEvent e, Panel panel) {
		m_searcherPanel.statusChangeProc();
	}

	public void openDetailsDialog(JotyDialog dlg) {
		if (dlg instanceof DataAccessDialog)
			((DataAccessDialog) dlg).filterInit(m_searcherPanel.m_detailsKeyWField);
		dlg.perform();
	}

	@Override
	public void searchCallback() {
	}

}
