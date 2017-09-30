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
import java.util.HashMap;
import java.util.Vector;

import org.joty.common.BasicPostStatement;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.data.JotyDataBuffer.Record;

/**
 * It is directed by the framework to manage, in the containing {@code Panel}
 * instance, the life of the main {@code Table} object, by having it working on
 * a built-in instance of a {@code NavigationBuffer} class.
 * <p>
 * The loading of data, the updating of the Table upon deletion or adding of
 * records are responsibility of this class. Also the exchange of data between
 * the components on the panel (a {@code DataAccessPanel} in this case) and the
 * currently selected row, when the record changes or when the selection
 * changes, is another task of the class.
 * <p>
 * The class manages also the association between the rows of the Table and the
 * possibly existing set of {@code AnalogicalRowSelector} objects coordinating
 * the selection status of them.
 * <p>
 * At last the class makes easy the access to the work buffer. 
 * 
 * @see Table
 * @see NavigatorBuffer
 * @see Panel
 * @see DataAccessPanel
 * @see AnalogicalRowSelector
 *
 */
public class GridManager {
	public interface IRenderAnalogicalSelector {
		void render(GridManager gridManager, int row);
	}

	public TermContainerPanel m_termContainerPanel;
	public NavigatorBuffer m_gridBuffer;
	public JotyDialog m_dialog;
	public ScrollGridPane m_listComponent;
	public HashMap<Long, AnalogicalRowSelector> m_selectorMap;
	public IRenderAnalogicalSelector m_renderAnalogicalSelector;
	public Vector<Boolean> m_selectorHeavyStates;

	private long m_previousSelectionKey;
	private boolean m_actorIsAnalogical;

	public GridManager() { // for use with WBE only
	}

	public GridManager(TermContainerPanel termContainerPanel) {
		m_termContainerPanel = termContainerPanel;
		m_dialog = Application.m_app.m_definingDialog;
		m_gridBuffer = new NavigatorBuffer(m_termContainerPanel);
		m_listComponent = null;
		m_previousSelectionKey = -1;
		m_renderAnalogicalSelector = null;
		m_selectorHeavyStates = null;
	}

	public void clearPreviousAnalogSelection() {
		if (m_selectorMap != null && m_previousSelectionKey >= 0)
			m_selectorMap.get(m_previousSelectionKey).setSelectionStatus(false);
	}

	public void enable() {
		enable(true);
	}

	void enable(boolean state) {
		m_listComponent.setEnabled(state);
	}

	public void ensureSelectionIsVisible() {
		m_listComponent.ensureIndexIsVisible(m_gridBuffer.m_cursorPos);
	}

	public long getCurrId() {
		return idFromGridRow(getCurSel());
	}

	int getCurSel() {
		return m_listComponent.getSelection();
	}

	public int getFieldIndex(String fieldName) {
		return m_gridBuffer.getFieldIndex(fieldName);
	}

	public ScrollGridPane getListComponent() {
		return m_listComponent;
	}

	int getMainSetSize() {
		return m_gridBuffer.m_records.size();
	}

	Record getRecordBuffer() {
		return m_gridBuffer.getRecord();
	}

	public int getRowQty() {
		return m_listComponent.getRowQty();
	}

	long idFromGridRow(int pos) {
		return m_gridBuffer.getKeyLongVal(pos);
	}

	void init() {
		m_listComponent.setFormat();
		if (m_termContainerPanel != null && m_termContainerPanel instanceof DataScrollingPanel)
			m_gridBuffer.m_hasKeys = ((DataScrollingPanel) m_termContainerPanel).m_keyElems.vector.size() > 0;
		m_listComponent.init();
	}

	boolean loadData(WResultSet rs, BasicPostStatement postStatement) {
		boolean retval;
		m_gridBuffer.checkKey();
		m_gridBuffer.preLoadData();
		retval = m_gridBuffer.loadData(rs, postStatement);
		m_gridBuffer.m_cursorPos = -1;
		clearPreviousAnalogSelection();
		return retval;
	}

	void loadGrid() {
		m_listComponent.managedListLoad(m_termContainerPanel);
		if (m_renderAnalogicalSelector != null)
			for (int i = 0; i < m_gridBuffer.m_records.size(); i++)
				m_renderAnalogicalSelector.render(this, i);
	}

	public void manageAnalogSelection(long rowKeyValue, boolean master) {
		if (!master)
			((DataScrollingPanel) m_termContainerPanel).inferCompletion();
		if (!master) {
			m_actorIsAnalogical = true;
			setSelectionOnKeyVal(rowKeyValue);
			m_actorIsAnalogical = false;
		}
		if (rowKeyValue >= 0)
			m_selectorMap.get(rowKeyValue).setSelectionStatus(true);
		if (m_previousSelectionKey >= 0 && rowKeyValue != m_previousSelectionKey)
			clearPreviousAnalogSelection();
		m_previousSelectionKey = rowKeyValue;
		if (!master)
			ensureSelectionIsVisible();
	}

	public void removeAll() {
		m_gridBuffer.empty();
		m_listComponent.removeAll();
	}

	void removeRow() {
		int rowPos = m_gridBuffer.m_cursorPos;
		m_gridBuffer.deleteRecord();
		m_listComponent.managedDeleteRow(rowPos);
		m_listComponent.setSelection(-1);
	}

	public void renderHeavyStatus(int row) {
		m_selectorMap.get(m_gridBuffer.getKeyLongVal(row)).setHeavyStatus(m_selectorHeavyStates.get(row));
	}

	public void renderOnAppend(int iDim) {
		m_listComponent.managedAppend(this, iDim);
		m_listComponent.setSelection(iDim - 1);
	}

	public void setCurSel(int nSelect) {
		if (nSelect == -1)
			clearPreviousAnalogSelection();
		boolean oldChangeEventsEnabled = m_listComponent.m_changeEventsEnabled;
		if (m_dialog.m_currSheet != null && m_dialog.m_currSheet.m_inhibitChangeNotification)
			m_listComponent.m_changeEventsEnabled = false;
		m_listComponent.setSelection(nSelect);
		m_listComponent.m_changeEventsEnabled = oldChangeEventsEnabled;
		storeSelection();
		if (m_termContainerPanel != null && ! m_actorIsAnalogical)
			m_termContainerPanel.statusChangeProc();
	}

	public void setHeavyStatus(int row, boolean heavy) {
		m_selectorHeavyStates.set(row, heavy);
	}

	public void setSelectionOnKeyVal(long keyVal) {
		if (m_gridBuffer.m_keyName == null)
			Application.m_app.JotyMsg(this, "gridBuffer has no keyName specified !");
		else {
			if (m_listComponent.getRowQty() > 0) {
				Integer keyPos = m_gridBuffer.getKeyPos(keyVal);
				if (keyPos != null) {
					if (m_gridBuffer.m_queueManager != null)
						keyPos = m_gridBuffer.m_queueManager.getReverseMappedRow(keyPos);
					setCurSel(keyPos);
				}
			}
		}
	}

	void setSourcePanel(TermContainerPanel panel) {
		m_termContainerPanel = panel;
		if (!Beans.isDesignTime())
			m_gridBuffer.m_panel = panel;
	}

	public void storeSelectedValues(String keyFieldName) {
		Application app = Application.m_app;
		if (keyFieldName == null)
			app.m_justSelectedValue = m_gridBuffer.getKeyLongVal();
		else {
			WrappedField integerKey = m_gridBuffer.getWField(keyFieldName);
			app.m_justSelectedValue = integerKey == null ? -1 : integerKey.getInteger();
		}
		for (String fieldName : app.m_valuesContainer.fieldNames())
			app.m_valuesContainer.putValue(fieldName, m_gridBuffer.getWField(fieldName));
	}

	void storeSelection() {
		m_gridBuffer.m_cursorPos = m_gridBuffer.m_queueManager == null ? getCurSel() : m_gridBuffer.m_queueManager.getMappedRow(getCurSel());
	}

	void updateRow() {
		if (m_gridBuffer.m_records.size() > 0 && m_gridBuffer.m_cursorPos >= 0) {
			updateRowBuffer();
			m_listComponent.managedUpdateRow(this);
		}
	}

	void updateRowBuffer() {
		Record pRow = m_gridBuffer.getRecord();
		for (int i = 0; i < m_termContainerPanel.m_terms.size(); i++)
			pRow.m_data.get(i).copyWField(m_termContainerPanel.m_terms.get(i), false);
		if (m_gridBuffer.m_hasKeys && m_termContainerPanel.idFieldIsHostedByTerm())
			pRow.m_data.get(m_gridBuffer.keyIndex()).copyWField(m_termContainerPanel.m_terms.get(m_termContainerPanel.m_IdFieldElemIdx), false);
		if (m_renderAnalogicalSelector != null)
			m_renderAnalogicalSelector.render(this, m_gridBuffer.m_cursorPos);
	}

}
