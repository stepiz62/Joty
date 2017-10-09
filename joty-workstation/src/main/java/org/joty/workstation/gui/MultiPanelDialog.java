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

import java.awt.GridLayout;
import java.beans.Beans;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;

import org.joty.app.JotyException;
import org.joty.common.CaselessStringKeyMap;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.gui.DataAccessPanel.Permission;


/**
 * Instead of hosting a single {@code DataAccessPanel} object directly added
 * to its layout, it contains a {@code TabbedPane} object capable to contain
 * several DataAccessPanel instances.
 * <p>
 * References to these instances is maintained in convenient data structures to
 * provide facilities in accessing each of them and, above all, to have the
 * currently selected one to be processed as the ancestor of this class does
 * with the one only there available.
 * <p>
 * The class is able to change the equipment of the {@code m_buttonPane} object
 * depending on the type of the panel currently selected
 * <p>
 * It manages the selection of pane by adding all is needed to guide the user in
 * ending or maintaining active the editing session, depending on the target
 * sheet being bound to the abandoning one from a relation of extension of it or
 * not.
 * <p>
 * The class makes the availability of each pane depending on the resulting
 * permission the user has on the panel associated: if no panel results
 * accessible the dialog does not open at all.
 * <p>
 * The class hosts two different scenarios: one in which all the
 * DataAccessPanel instances are independent and a second in which all
 * instances are related to a main one, typically the first, that is an instance
 * of the {@code DataScrollingPanel} class. In the second scenario the instance
 * of this class is said to host a 'multi-tab document'.
 * <p>
 * In the 'multi-tab document' case the master panel manages a navigation grid
 * that can be either internal to its layout (that is the master is an instance
 * of {@code NavigatorPanel} or external in which case the {@code Table} object
 * must be explicitly instantiated and notified to the panel by means of the
 * {@code setController} method. The 'new' and the 'delete' user command have
 * different effect in each of these different scenarios: in the multi-tab
 * document circumstance and if the current panel is a single record panel, they
 * are referenced to the entity managed by the master panel, having, indeed, the
 * 'new' command acting on its record and the 'delete' command acting on all
 * panel, basing on the binding relations.
 * 
 * 
 * @see TabbedPane#addTab(String, javax.swing.Icon, java.awt.Component, String)
 */
public class MultiPanelDialog extends DataAccessDialog {

	protected TabbedPane m_tabbedPane;
	public Vector<DataAccessPanel> m_dataPanels;
	public int m_iCurrentPage;
	public Vector<Integer> m_tabPanePanelClassIndex;
	private int m_initPage;
	public boolean m_reloadTabOnActivate;
	public CaselessStringKeyMap<Integer> m_panelsTitleMap;
	public int m_gridMasterIdx;
	protected int m_gridMasterClassIndex;
	protected int m_definedPanels;

	public MultiPanelDialog() {
		this(null);
	}

	public MultiPanelDialog(Object callContext) {
		this(callContext, null);
	}

	public MultiPanelDialog(Object callContext, Object openingMode) {
		this(callContext, openingMode, false);
	}

	public MultiPanelDialog(Object callContext, Object openingMode, boolean initAction) {
		super(callContext, openingMode, initAction);
		m_initializing = true;
		m_contentPanel.setBounds(0, 0, 586, 317);
		m_buttonPane.setLocation(0, 325);
		m_tabbedPane = new TabbedPane(JTabbedPane.TOP);
		m_tabbedPane.setLocation(4, 3);
		m_tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		setBounds(100, 100, 594, 393);
		m_tabbedPane.setSize(578, 311);
		m_contentPanel.add(m_tabbedPane);
		m_dataPanels = new Vector<DataAccessPanel>();
		m_panelsTitleMap = new CaselessStringKeyMap<Integer>(m_app);
		m_tabPanePanelClassIndex = new Vector<Integer>();
		m_reloadTabOnActivate = false;
		m_gridMasterIdx = -1;
		m_definedPanels = 0;
	}

	@Override
	protected DataAccessPanel addEnabling_driver() {
		return multiTabDocumentExists() ? m_dataPanels.get(0) : m_currSheet;
	}

	void buildTabs() {}

	@Override
	protected void checkAndSetLook() {
		m_currSheet.guiDataExch(false);
		m_currSheet.checkAndSetLook();
		showingEffects(m_iCurrentPage);
		updateCommandButtons(!m_new_command && !m_editOrNew_command);
	}

	@Override
	protected void checkForHooveringListener() {
		super.checkForHooveringListener();
		m_tabbedPane.addMouseListener(new EditAbandon_HooveringListener(false, true));
	}

	@Override
	protected boolean checkForNormalBehavior() {
		return (m_gridMasterIdx < 0 || m_iCurrentPage != m_gridMasterIdx && masterGridManager().getCurSel() >= 0) && 
				(m_currSheet.m_targetPanel == null || m_currSheet.m_targetPanel != m_dataPanels.get(m_gridMasterClassIndex));
	}

	@Override
	protected void checkForPublishings() {
		for (int i = 0; i < m_tabbedPane.getTabCount(); i++)
			m_dataPanels.get(m_tabPanePanelClassIndex.get(i)).checkForPublishing();
	}

	@Override
	protected void checkPanelForNewRec() {
		if (dialogRecordLevel())
			if (m_gridMasterIdx >= 0)
				if (m_currSheet != m_dataPanels.get(m_gridMasterClassIndex) && m_dataPanels.get(m_gridMasterClassIndex).m_operative)
					m_tabbedPane.setSelectedIndex(m_gridMasterIdx);
	}

	protected void checkPanelsCollaboration() {
		if (m_gridMasterIdx >= 0)
			gridSelectionChangeHandler();
	}

	@Override
	protected void clearAppReferences() {
		for (DataAccessPanel panel : m_dataPanels)
			panel.clearAppReferences();
	}

	@Override
	public void closeDependentDialogs() {
		for (DataAccessPanel panel : m_dataPanels)
			panel.closeDependentDialogs();
	}

	@Override
	public boolean compoundDocument() {
		return multiTabDocumentExists();
	}

	@Override
	protected DataAccessPanel deleteEnabling_driver() {
		return multiTabDocumentExists() ? m_dataPanels.get(0) : m_currSheet;
	}

	public boolean dialogRecordLevel() {
		return !sheetRecordLevel();
	}

	@Override
	boolean documentIdentified() {
		if (multiTabDocumentExists() && !m_currSheet.existController())
			return getGridMaster().documentIdentified();
		else
			return super.documentIdentified();
	}

	@Override
	public void filterInit(WrappedField keyWField) {
		if (m_dataPanels.size() > 0)
			m_dataPanels.get(0).filterInit(keyWField);
	}

	@Override
	boolean getDelete_EnablingState() {
		DataAccessPanel driver = deleteEnabling_driver();
		return !driver.isReadOnly() && !driver.isModifyOnly() && driver.m_permission.compareTo(Permission.all) == 0 && 
					(gridManagerExists() || !multiTabDocumentExists()) && !m_isViewer;
	}

	@Override
	public GridManager getGridManager(boolean DialogLevelImperative) {
		return DialogLevelImperative || dialogRecordLevel() ? masterGridManager() : m_currSheet.m_gridManager;
	}

	@Override
	public DataAccessPanel getGridMaster() {
		return m_gridMasterIdx >= 0 ? m_dataPanels.get(m_gridMasterClassIndex) : null;
	}

	@Override
	boolean gridManagerExists() {
		return m_gridManager != null || m_currSheet.existController();
	}

	protected void gridSelectionChangeHandler() {
		if (m_gridMasterIdx >= 0) {
			for (TermContainerPanel panel : m_dataPanels) {
				panel.m_keyElems.clearWFields();
				panel.m_wfields.clearWFields();
			}
			m_dataPanels.get(m_gridMasterClassIndex).statusChangeProc();
			if (m_currSheet != m_dataPanels.get(m_gridMasterClassIndex)) {
				if (m_currSheet.dataToBeLoaded())
					m_currSheet.loadData();
				m_currSheet.statusChangeProc();
			}
		} else
			m_currSheet.clearTerms(true);
		m_currSheet.guiDataExch(false);
	}

	protected GridManager identifyGridManager(DataAccessPanel masterPanel) {
		return masterPanel instanceof DataScrollingPanel && 
				((DataScrollingPanel) masterPanel).m_builtInGridManager ? masterPanel.m_gridManager : null;
	}

	@Override
	public boolean initChildren() {
		for (int i = 0; i < m_dataPanels.size(); i++)
			m_dataPanels.get(i).init();
		return m_dataPanels.size() > 0;
	}

	@Override
	protected boolean initDialog() {
		boolean lResult = true;
		if (!Beans.isDesignTime()) {
			if (m_tabPanePanelClassIndex.size() == 0) {
				if (m_app.m_debug)
					Application.m_app.JotyMsg(this, "At least one Data Accessor Panel must be added to the tab pane and must be accessible to the current user !");
				lResult = false;
			} else {
				DataAccessPanel masterPanel = m_dataPanels.get(0);
				m_gridManager = identifyGridManager(masterPanel);
				if (m_gridManager != null) {
					m_gridMasterIdx = 0;
					m_gridMasterClassIndex = m_tabPanePanelClassIndex.get(0);
					m_gridManager.setSourcePanel(masterPanel);
					masterPanel.m_gridManager = m_gridManager;
					((DataScrollingPanel) masterPanel).m_isGridMaster = true;
				}
			}
		}
		if (lResult)
			lResult = super.initDialog();
		if (lResult) {
			if (m_tabbedPane.getTabCount() > 0) {
				boolean oldWaitState = m_app.setWaitCursor(true);
				m_iCurrentPage = 0;
				m_tabbedPane.setSelectedIndex(0);
				initSelection();
				lResult = loadDataByContainer();
				m_currSheet.checkForControllerInitialization();
				m_app.setWaitCursor(oldWaitState);
			}
			if (lResult) {
				checkPanelsCollaboration();
				postInitContainer();
			}
			m_initializing = false;
		}
		return lResult;
	}

	void initSelection() {
		m_dataPanels.get(m_initPage).setVisible(true);
		m_dataPanels.get(m_initPage).setEnabled(true);
		m_tabbedPane.setSelectedIndex(m_initPage);
		m_iCurrentPage = m_initPage;
	}

	@Override
	public boolean isDeletable() {
		boolean success = true;
		if (multiTabDocumentExists())
			for (DataAccessPanel dataPanel : m_dataPanels)
				if (!dataPanel.isDeletable() && dataPanel.m_operative) {
					success = false;
					break;
				}
		return success;
	}

	boolean loadDataByContainer() {
		boolean retVal = true;
		if (m_newDocument)
			manageNewDocumentState();
		if (retVal) {
			updateCommandButtons(true);
			if (m_tabbedPane.getTabCount() > 0) {
				for (int i = 0; i < m_tabbedPane.getTabCount(); i++)
					m_dataPanels.get(m_tabPanePanelClassIndex.get(i)).refresh();
				m_currSheet.checkAndSetLook();
				for (int i = 0; i < m_tabbedPane.getTabCount(); i++)
					m_dataPanels.get(m_tabPanePanelClassIndex.get(i)).m_growing = false;
			}
		}
		return retVal;
	}

	@Override
	boolean manageDeletions() {
		boolean success = true;
		if (multiTabDocumentExists() || getGridManager() == null) {
			DataAccessPanel panel;
			for (int i = m_tabbedPane.getTabCount() - 1; i >= 0; i--) {
				panel = m_dataPanels.get(m_tabPanePanelClassIndex.get(i));
				if (panel.m_operative && panel.m_targetPanel == null)
					if (!panel.doDeletion(i > 0)) {
						success = false;
						break;
					}
			}
		} else
			success = super.manageDeletions();
		return success;
	}

	void manageNewDocumentState() {}

	@Override
	public GridManager masterGridManager() {
		return identifyGridManager(m_dataPanels.get(0));
	}

	boolean multiTabDocumentExists() {
		return m_gridMasterIdx >= 0;
	}

	@Override
	public void onDelete() {
		if (m_tabbedPane.getTabCount() == 0)
			return;
		if (multiTabDocumentExists() && !m_currSheet.existController()) {
			if (!Application.langYesNoQuestion("WantDeleteWholeDoc"))
				return;
			m_is_deleting = true;
			m_app.beginTrans();
			try {
				if (isDeletable() && manageDeletions()) {
					m_app.commitTrans();
					m_currSheet.deletionEffects();
				} else
					m_app.rollbackTrans();
			} catch (JotyException e) {}
			checkAndSetLook();
			m_is_deleting = false;
		} else {
			m_is_deleting = true;
			super.onDelete();
			m_is_deleting = false;
		}
		checkAndSetLook();
	}

	@Override
	void onGridSelChange(ListSelectionEvent e, Panel panel) {
		if (panel.m_gridManager == masterGridManager()) {
			m_gridSelChanging = true;
			gridSelectionChangeHandler();
			m_gridSelChanging = false;
		} else
			super.onGridSelChange(e, panel);
	}

	@Override
	public void onNew() {
		super.onNew();
		if (m_gridManager != null)
			m_gridManager.m_gridBuffer.checkKey();
	}

	void onTabSelectionChanged(ChangeEvent e) {
		if (!Beans.isDesignTime()) {
			m_editAbandoning = true;
			if (m_iCurrentPage >= 0)
				m_dataPanels.get(m_tabPanePanelClassIndex.get(m_iCurrentPage)).guiDataExch(true);
			m_iCurrentPage = m_tabbedPane.getSelectedIndex();
			m_currSheet = m_dataPanels.get(m_tabPanePanelClassIndex.get(m_iCurrentPage));
			if (	!m_editOrNew_command && 
					!m_new_command && 
					(m_currSheet.m_askedToLoad || 
							m_reloadTabOnActivate && checkForNormalBehavior() && m_currSheet.dataToBeLoaded())
				)
				m_currSheet.loadData();
			else {
				if (m_currSheet instanceof DataScrollingPanel)
					if (documentIdentified())
						((DataScrollingPanel) m_currSheet).effectsOnTerms(m_gridManager.getRecordBuffer(), true);
					else
						m_currSheet.ensureClearTerms();
				m_currSheet.checkPublishers();
			}
			checkAndSetLook();
		}
	}

	void postInitContainer() {}

	@Override
	protected void processFault() {
		if (m_dataPanels.size() == 0)
			Application.langWarningMsg("NoDataSheetAccessible");
	}

	@Override
	public void resetPanel(String panelTitle) {
		m_dataPanels.get(m_panelsTitleMap.get(panelTitle)).askToLoad();
	}

	public boolean sheetRecordLevel() {
		return checkForNormalBehavior() && m_currSheet.existController();
	}

	public void showingEffects(int iNewPage) {}

}
