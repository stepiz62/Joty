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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;

import org.joty.access.Logger;
import org.joty.workstation.app.Application;
import org.joty.workstation.gui.SearcherPanel.SearcherPanelContainer;

/**
 * Embeds a {@code SearcherPanel} object in its layout.
 * <p>
 * Its descendant must instantiate a CriteriaPanel object and add it to the
 * SearcherPanel.m_criteriaContainer member layout.
 * <p>
 * It is a MultiPanelDialog that uses the result data grid of the SearcherPanel
 * instance as controller for the data navigation: this grid is made the object
 * addressed by the DataScrollingPanel instance that has the role of master
 * panel.
 * 
 * @see SearcherPanel
 * @see MultiPanelDialog
 * 
 */
public class SearcherMultiPanelDialog extends MultiPanelDialog implements SearcherPanelContainer {
	public SearcherPanel m_searcherPanel;
	protected JLabel m_lblManage;
	protected JLabel m_lblSearch;
	public JotyButton m_btnSearcherExpand;
	protected Application m_app = Application.m_app;

	public SearcherMultiPanelDialog(Object callContext, Object openingMode, boolean initAction) {
		super(callContext, openingMode, initAction);
		m_btnSelect.setSize(63, 23);
		m_btnSelect.setLocation(585, 560);

		m_btnOk.setBounds(-632, -249, 28, 28);
		m_buttonPane.setBounds(6, 560, 568, 40);
		m_contentPanel.setBounds(6, 246, 644, 303);
		m_searcherPanel = Factory.createSearcherPanel(this);
		m_searcherPanel.m_lblCriteria.setSize(285, 14);
		m_searcherPanel.m_btnSelect.setLocation(564, 83);
		m_searcherPanel.setBounds(10, 14, 637, 211);
		m_searcherPanel.m_table.setSize(616, 78);
		m_searcherPanel.m_table.setLocation(10, 122);
		m_searcherPanel.m_table.setToolTipText(jotyLang("SelectRowToManage"));
		m_searcherPanel.m_btnPrevious.setLocation(564, 5);
		if (m_app.m_accessor != null)
			m_searcherPanel.m_panelDataDef = m_app.m_accessor.getPanelDataDef(getClass().getName(), 0);
		setBounds(10, 14, 662, 626);
		m_reloadTabOnActivate = true;

		m_contentPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		m_tabbedPane.setSize(631, 282);
		m_tabbedPane.setLocation(6, 6);
		m_searcherPanel.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		m_searcherPanel.m_lblSearchResult.setSize(258, 14);
		m_searcherPanel.m_lblSearchResult.setLocation(10, 108);
		m_searcherPanel.m_gridManager.m_listComponent.setBounds(10, 122, 617, 82);
		m_searcherPanel.injectIntoBuffer();
		m_searcherPanel.m_criteriaContainer.setSize(544, 82);
		m_searcherPanel.m_criteriaContainer.setLocation(10, 24);
		m_searcherPanel.m_lblCriteria.setLocation(10, 7);
		m_searcherPanel.m_btnFind.setLocation(564, 32);
		getContentPane().add(m_searcherPanel);
		m_lblSearch = new JotyLabel(jotyLang("Research"));
		m_lblSearch.setBounds(3, 1, 202, 14);
		getContentPane().add(m_lblSearch);
		m_lblManage = new JotyLabel(jotyLang("Manage"));
		m_lblManage.setBounds(6, 231, 57, 14);
		getContentPane().add(m_lblManage);
		m_btnSearcherExpand = new JotyButton();
		m_btnSearcherExpand.setLocation(215, 3);
		m_btnSearcherExpand.setSize(28, 12);
		m_btnSearcherExpand.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_searcherPanel.setSearcherLayout(SearcherMultiPanelDialog.this, m_contentPanel.getBounds());
			}
		});
		getContentPane().add(m_btnSearcherExpand);
	}

	public void addLongKeyElem(String name) {
		m_searcherPanel.addLongKeyElem(name);
	}

	public void addStrKeyElem(String name) {
		m_searcherPanel.addStrKeyElem(name);
	}

	@Override
	protected void checkAndSetLook() {
		enableSearch(!isEditing());
		super.checkAndSetLook();
	}

	@Override
	protected void doActivationChange(Boolean activating) {
		super.doActivationChange(activating);
		m_lblManage.setVisible(!m_app.m_dialogOpeningAsValueSelector && !m_isViewer);
		m_lblManage.setEnabled(isEditing());
		m_btnSearcherExpand.setVisible(activating && m_app.m_dialogOpeningAsValueSelector);
	}

	public void enableSearch(boolean truth) {
		m_searcherPanel.m_criteriaPanel.enableComponents(truth);
		m_searcherPanel.m_btnFind.setEnabled(truth);
		m_lblManage.setEnabled(!truth);
		m_lblSearch.setEnabled(truth);
	}

	@Override
	public JotyButton getSearcherExpandButton() {
		return m_btnSearcherExpand;
	}

	@Override
	public JotyButton getSelectorButton() {
		return m_searcherPanel.m_btnSelect;
	}

	@Override
	boolean gridManagerExists() {
		return true;
	}

	@Override
	protected GridManager identifyGridManager(DataAccessPanel masterPanel) {
		return m_searcherPanel.m_gridManager;
	}

	@Override
	public boolean initChildren() {
		m_searcherPanel.m_criteriaPanel.init();
		return super.initChildren() && m_dataPanels.get(0).m_panelIdxInDialog == 0;
	}

	@Override
	protected void processFault() {
		if (m_dataPanels.size() > 0 && m_dataPanels.get(0).m_panelIdxInDialog > 0)
			Application.langWarningMsg("FirstSheetMustBeAccessible");
		super.processFault();
	}

	@Override
	protected boolean initDialog() {
		boolean retVal = super.initDialog();
		if (retVal) {
			m_dataPanels.get(m_gridMasterClassIndex).defineGrid();
			m_searcherPanel.initContainer(this, m_contentPanel.getBounds());
			m_dataPanels.get(m_gridMasterClassIndex).m_queryDefPostStatement = m_searcherPanel.m_queryDefPostStatement;
		}
		return retVal;
	}

	@Override
	public GridManager masterGridManager() {
		return m_searcherPanel.m_gridManager;
	}

	@Override
	public void onNew() {
		super.onNew();
		if (m_gridManager == null)
			Logger.appendToHostLog("GridManager not built: possible list driver definition missing !");
	}

	public String strKeyElemVal(String name) {
		return keyElem(name).m_strVal;
	}

	@Override
	public void searchCallback() {
	}

}
