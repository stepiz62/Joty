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

import java.beans.Beans;

import org.joty.app.JotyException;
import org.joty.common.JotyTypes;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.JotyDataBuffer;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.data.JotyDataBuffer.Record;

/**
 * It adds to the hierarchy the ability to use a referenced data grid, as a
 * medium to access data. The data are loaded in the relative associated
 * {@code NavigatorBuffer} object, and the DataScrollingPanel instance can
 * navigate along the grid records by means of the selection performed on the
 * grid or by the use of the navigation buttons located in the containing
 * JotyDialog instance. After the selection has been made the record can be
 * inspected or edited by means of the ancestor functionalities.
 * <p>
 * The data grid and the {@code GridManager associated} may be contained in the
 * panel or outside of it. In the latter case it can assume the role of
 * coordinator of the data access made by other {@code DataAccessPanel} objects
 * located in the same {@code TabbedPane} instance typically within a
 * {@code SearcherMultiPanelDialog} descendant. The class keeps track of the
 * selection state and performs the notification of the selection event to the
 * other panels.
 * <p>
 * After a record has been modified, the class performs the
 * updates of the NavigatorBuffer making the 'controller' (like the data grid is
 * called for the driving role described above) to refresh.
 * 
 * @see DataAccessPanel
 * @see GridManager
 * @see NavigatorBuffer
 * @see TabbedPane
 * @see SearcherMultiPanelDialog
 * 
 */
public class DataScrollingPanel extends DataAccessPanel  {
	boolean m_firstColAsPositioner = false;
	boolean m_builtInGridManager = false;
	protected long m_gridKey;
	public String m_listKeyField;
	public long m_rowKeyID;
	public boolean m_isGridMaster = false;
	private boolean m_ancestorMode = false;

	public DataScrollingPanel() {
		this(false);
	}

	protected DataScrollingPanel(boolean builtInGridManager) {
		this(builtInGridManager, false);
	}

	protected DataScrollingPanel(boolean builtInGridManager, boolean likeTheAncestor) {
		super();
		m_ancestorMode = likeTheAncestor;
		m_rowKeyID = -1;
		m_builtInGridManager = builtInGridManager;
		if (builtInGridManager) {
			m_gridManager = Beans.isDesignTime() ? new GridManager() : new GridManager(this);
		}
	}

	public void addIntegerKeyElem(String fieldName, boolean isTheIdField, boolean isTheListKey) {
		if (m_ancestorMode)
			addKeyElem(fieldName, JotyTypes._dbDrivenInteger);
		else
			addKeyElem(fieldName, JotyTypes._dbDrivenInteger, isTheIdField, null, isTheListKey);
	}

	public WrappedField addKeyElem(String fieldName, int dataType, boolean contextIdentifying, WrappedField defaultVal, boolean isTheListKey) {
		WrappedField retVal = super.addKeyElem(fieldName, dataType, contextIdentifying, defaultVal);
		if (!m_ancestorMode && isTheListKey) {
			if (m_listKeyField == null) {
				m_listKeyField = fieldName;
				if (!Beans.isDesignTime())
					getGridManager().m_gridBuffer.setKeyFieldName(fieldName);
			} else
				m_app.JotyMsg(this, "List id key term already specified ");
		}
		return retVal;
	}

	protected void addStrKeyElem(String fieldName, String defaultVal, boolean isTheIdField, boolean isTheListKey) {
		addKeyElem(fieldName, JotyTypes._text, isTheIdField, defaultVal != null ? defaultStrValWField(defaultVal) : null, isTheListKey);
	}

	@Override
	protected void checkControllerInit(WResultSet rs) {
		if (m_ancestorMode)
			super.checkControllerInit(rs);
		else {
			NavigatorBuffer gridBuffer = getGridManager().m_gridBuffer;
			if (!gridBuffer.m_descriptorBuilt)
				gridBuffer.buildRecordDescriptor(rs);
		}
	}

	protected boolean checkDataLoading() {
		return dataToBeLoaded() && loadData();
	}

	@Override
	protected void checkForControllerInitialization() {
		if (m_gridManager != null)
			checkForInitSelection();
	}

	protected void checkForInitSelection() {
		if (m_rowKeyID > -1)
			m_gridManager.setSelectionOnKeyVal(m_rowKeyID);
	}

	@Override
	protected boolean clientValidation() {
		boolean success = super.clientValidation();
		if (m_gridManager != null && m_gridManager.m_gridBuffer.m_hasKeys && !(m_dialog instanceof SearcherMultiPanelDialog)) {
			if (m_gridManager.m_gridBuffer.m_keyName.compareToIgnoreCase(m_IdFieldName) != 0) {
				WrappedField keyElem = idFieldIsHostedByTerm() ? m_terms.get(m_IdFieldElemIdx) : keyElem(m_gridManager.m_gridBuffer.m_keyName);
				if (m_gridManager.m_gridBuffer.inSetKeyCheck(keyElem)) {
					success = false;
					Application.langWarningMsg("TermAlreadyPresent");
				}
			}
		}
		return success;
	}

	@Override
	protected boolean dialogGridManagerExists() {
		return true;
	}

	@Override
	protected boolean documentIdentified() {
		if (m_ancestorMode)
			return super.documentIdentified();
		else
			return m_gridManager != null && m_gridManager.getCurSel() > -1;
	}

	@Override
	protected void doNew() {
		if (!m_ancestorMode) {
			m_gridManager.setCurSel(-1);
			setMainData(null, 0);
			m_identifyingID = 0;
		}
		super.doNew();
	}

	@Override
	protected boolean doneWithData() {
		if (m_ancestorMode)
			return super.doneWithData();
		else
			return m_builtInGridManager ? checkDataLoading() : true;
	}

	@Override
	protected void doReloadBecauseOfPublishing() {
		renderGrid();
	}

	protected void effectsOnForm() {
		injectedDialog().updateCommandButtons(true);
		checkAndSetLook();
		manageAnalogicalBehavior();
	}

	public void effectsOnTerms(Record row) {
		effectsOnTerms(row, false);
	}

	public void effectsOnTerms(Record row, boolean switchingFromOtherPanel) {
		for (int i = 0; i <= m_maxEffectsIndex; i++) {
			resetUpdateActorsState();
			effectsOnTerms(row, i, switchingFromOtherPanel);
		}
	}

	protected void effectsOnTerms(Record row, int effectsIndex, boolean switchingFromOtherPanel) {
		Term term = null;
		for (int i = 0; i < m_terms.size(); i++) {
			term = m_terms.get(i);
			if (isVisible() && (term.m_container == null || 
					!switchingFromOtherPanel) || 
					!isVisible() && term.m_container != null && term.m_container.m_targetPanel == this)
				if (term.m_effectsIndex == effectsIndex)
					if (!term.isAControlTerm() && (!term.isEnabledAsDetail() || !m_dialog.m_canceling) || term.m_dbFieldName != null) {
						term.preRender();
						term.updateState(row.m_data.get(i));
						term.termRender();
					}
		}
	}

	@Override
	public boolean existController() {
		return m_ancestorMode ? super.existController() : true;
	}

	@Override
	protected void gridManagerGuide() {
		if (m_ancestorMode)
			super.gridManagerGuide();
		else
			m_app.JotyMsg(this, "GridManager not instantiated : check the parameter in super constructor invocation !");
	}

	public int gridRowPos() {
		return dialogGridManagerExists() ? getGridManager().m_gridBuffer.m_cursorPos : -1;
	}

	@Override
	protected long idFromGridRow() {
		return idFromGridRow(-1);
	}

	long idFromGridRow(int pos) {
		long retVal = -1;
		GridManager listMngr = getGridManager();
		if (listMngr != null && !m_IdFieldName.isEmpty() && !m_initializing)
			retVal = listMngr.m_gridBuffer.getKeyLongVal(pos >= 0 ? pos : gridRowPos());
		return retVal;
	}

	protected void inferCompletion() {}

	@Override
	public boolean init() {
		Table table = masterGridTable();
		if (table != null)
			setGridFormat(table);
		return super.init();
	}

	@Override
	protected boolean isControllerMaster() {
		return m_ancestorMode ? super.isControllerMaster() : m_isGridMaster;
	}

	@Override
	public boolean loadData() {
		boolean retVal = super.loadData();
		if (!m_ancestorMode && m_firstColAsPositioner) {
			JotyDataBuffer buffer = ((Table) getGridManager().getListComponent()).getBuffer();
			int index = 0;
			for (Record record : buffer.m_records) {
				index++;
				record.m_data.firstElement().setVal(buffer.m_queueManager == null ? index : (buffer.m_queueManager.getReverseMappedRow(index - 1) + 1));
			}
		}
		return retVal;
	}

	@Override
	protected String mainFilter() {
		if (m_ancestorMode)
			return super.mainFilter();
		else
			return m_isGridMaster ? m_dialog.getGridMaster().getWhereClause() : super.mainFilter();
	}

	protected void manageAnalogicalBehavior() {
		if (m_gridManager.m_selectorMap != null)
			m_gridManager.manageAnalogSelection(m_gridKey, true);
	}

	@Override
	protected void manageController() {
		if (m_ancestorMode)
			super.manageController();
		else {
			loadGrid();
			checkForInitSelection();
		}
	}

	protected Table masterGridTable() {
		GridManager gridManager = getGridManager();
		return gridManager == null ? null : ((Table) gridManager.getListComponent());
	}

	@Override
	boolean newIdManagement() throws JotyException {
		return super.newIdManagement() || !idFieldIsHostedByTerm();
	}

	@Override
	public void nextRecord() {
		if (m_ancestorMode)
			super.nextRecord();
		else {
			m_gridManager.m_listComponent.nextRow();
			m_gridManager.ensureSelectionIsVisible();
		}
	}

	@Override
	public void previousRecord() {
		if (m_ancestorMode)
			super.previousRecord();
		else {
			m_gridManager.m_listComponent.previousRow();
			m_gridManager.ensureSelectionIsVisible();
		}
	}

	public void renderGrid() {
		int oldselection = getGridManager().getCurSel();
		m_inhibitChangeNotification = true;
		loadData();
		getGridManager().setCurSel(oldselection);
		m_inhibitChangeNotification = false;
	}

	public void setController(Table table) {
		if (m_gridManager == null)
			m_app.JotyMsg(this, "Grid manager not create : check the constructor invocation !");
		else
			m_gridManager.m_listComponent = table;
	}

	@Override
	protected void setControllerOnKey(long keyVal) {
		m_rowKeyID = keyVal;
	}

	public void setFirstColAsPositioner() {
		m_firstColAsPositioner = true;
	}

	protected void setGridFormat(Table table) {}

	@Override
	protected void statusChangeProc() {
		ListeningState listeningState = setPanelActionListeningOff();
		super.statusChangeProc();
		if (m_gridManager != null) {
			if (m_IdFieldName.length() > 0)
				keyElem(m_IdFieldName).clear();
			if (gridRowPos() == -1) {
				for (Term term : m_terms)
					term.clearNonStructuredCtrl();
				m_gridKey = -1;
				setContextParams();
			} else {
				m_gridKey = idFromGridRow();
				Record row = m_gridManager.getRecordBuffer();
				getKeyDataFromRow(row, m_keyElems.vector);
				setContextParams();
				effectsOnTerms(row);
			}
			updateVisibilityBasingOnData();
			effectsOnForm();
		}
		restorePanelActionListening(listeningState);
	}


	@Override
	protected void updateController() {
		if (m_ancestorMode)
			super.updateController();
		else {
			if (m_gridManager == null)
				return;
			if (!m_isNewRec && !injectedDialog().m_new_command && gridRowPos() > -1 && !m_dialog.m_gridSelChanging && m_controllerUpdateRequested) {
				m_controllerUpdateRequested = false;
				if (!m_dialog.m_canceling && !m_dialog.isInitializing())
					updateRecordOnController();
			}
		}
	}

	@Override
	protected void updateRecordOnController() {
		if (m_ancestorMode)
			super.updateRecordOnController();
		else {
			m_gridManager.updateRow();
			m_gridManager.m_listComponent.repaint();
		}
	}
	

}
