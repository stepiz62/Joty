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

import java.util.Vector;

import javax.swing.JComponent;

import org.joty.workstation.data.WResultSet;
import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * It wraps a {@code CheckBoxList} component and uses the built-in reference to
 * the LiteralStruct object to populate its verbose part.
 * <p>
 * For the management of the data it uses the accessor methods provided by the
 * embedded component to access the checked status, in the case the Term
 * instance is the 'master', and uses a vector of integers, for each possible
 * selection in the primary component, in the case the instance is the 'slave';
 * the selection state in the i-th row, identifies the {@code id} member of the
 * {@code DescrStruct} associated with the row. This id is the datum that is
 * written to or read from the database. In the case of slave role this class
 * creates in memory all the scenarios of selections that has been taken form on
 * each row selected in the primary component.
 * <p>
 * It loads into the component all the literals of the LiteralStruct object
 * associated, then a row is checked if in the underlying record set, a record
 * with a key corresponding to the id of the literal exists.
 * <p>
 * On the storing of the data, all the record set in the database is deleted and
 * the new ones are inserted, as in the ancestor class, but, here, the record
 * inserted carries nothing but an id referencing a literal in the LiteralStruct
 * object.
 * 
 * @see CheckBoxList
 * @see org.joty.workstation.app.Application.LiteralStruct
 * @see org.joty.app.LiteralsCollection.DescrStruct
 * 
 */
public class CheckListTerm extends GridTerm {

	CheckBoxList m_checkBoxList;
	Vector<Vector<Integer>> m_buffers;

	public CheckListTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
		if (params.m_len < 0)
			m_loadOnly = true;
		m_variableDescrSet = (m_literalStruct == null);
		m_buffers = new Vector<Vector<Integer>>();
	}

	boolean addRecord(WResultSet rs, boolean asSlave, int index) {
		rs.addNew();
		rs.setIntegerValue(m_targetDatumField, m_checkBoxList.getItemData(index));
		if (m_mainEntityKeyField != null)
			rs.setIntegerValue(m_mainEntityKeyField, mainEntityVal(), ((DataAccessPanel) m_panel).entityIdValuePending());
		if (m_definedSetMethod != null)
			m_definedSetMethod.method(rs, (DataAccessPanel) m_panel);
		return rs.update(true, false, createContextPostStatement());
	}

	@Override
	public void clear() {
		if (m_slave) {
			if (m_mainIterator >= 0)
				for (int i = 0; i < m_buffers.get(m_mainIterator).size(); i++)
					m_buffers.get(m_mainIterator).set(i, 0);
		}
		clearCheckBoxes();
	}

	private void clearCheckBoxes() {
		for (int i = 0; i < m_checkBoxList.getRowQty(); i++)
			m_checkBoxList.setCheck(i, 0);
	}

	@Override
	protected void clearComponent() {
		if (m_variableDescrSet)
			m_checkBoxList.removeAll();
	}

	@Override
	void clearSlaveBuffers() {
		super.clearSlaveBuffers();
		m_buffers.removeAllElements();
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		m_checkBoxList = new CheckBoxList(panel, this);
	}

	@Override
	boolean descrVectToBeLoaded() {
		return true;
	}

	@Override
	protected void doLoadData() {
		if (m_slave && m_mainTermLiteralStruct != null) {
			int mainTermSize = m_mainTermLiteralStruct.m_descrArray.size();
			if (m_buffers.size() > 0)
				clearSlaveBuffers();
			for (int i = 0; i < mainTermSize; i++)
				m_buffers.add(new Vector<Integer>());
			loadDataAsSlave();
			if (mainTermSize > 0) {
				updateCheckBoxes(true);
				m_updatingActor = true;
				m_panel.guiDataExch(false);
			}
		} else
			loadRecords(false);
	}

	@Override
	protected boolean doStoreData() {
		boolean success = true;
		if (m_slave) {
			if (m_mainTermLiteralStruct != null) {
				updateCheckBoxes(false);
				success = storeDataAsSlave();
			}
		} else {
			clearData(false);
			WResultSet rs = createAndOpenWRset();
			if (rs != null) {
				int iDim = m_checkBoxList.getRowQty();
				for (int i = 0; i < iDim; i++) {
					if (m_checkBoxList.getCheck(i) > 0) {
						if (m_definedInsertMethod != null)
							success = m_definedInsertMethod.method(rs, (DataAccessPanel) m_panel, i);
						else
							success = addRecord(rs, false, i);
					}
				}
				rs.close();
			}
		}
		return success;
	}

	@Override
	protected boolean doValidate() {
		boolean success = true;
		if (m_required) {
			success = m_checkBoxList.getRowQty() == 0;
			for (int i = 0; i < m_checkBoxList.getRowQty(); i++)
				if (m_checkBoxList.getCheck(i) > 0) {
					success = true;
					break;
				}
			if (!success)
				alert("MustBeSelectedOne");
		}
		return success;
	}

	@Override
	protected void enable(boolean predicate) {
		m_checkBoxList.setEnabled(predicate);
	}

	@Override
	protected void enableComponent(boolean truth, boolean editability, boolean documentIdentified) {
		enable(true);
		m_checkBoxList.enableChkList(truth);
	}

	@Override
	public JComponent getComponent() {
		return m_checkBoxList;
	}

	@Override
	int getCount() {
		return m_checkBoxList.getRowQty();
	}

	@Override
	long getCurSelData(boolean updateData) {
		return (int) m_checkBoxList.getItemData(m_checkBoxList.getSelection());
	}

	@Override
	public ScrollGridPane getScrollPane() {
		return m_checkBoxList;
	}

	@Override
	public int getSelection() {
		return m_checkBoxList.getSelection();
	}

	@Override
	protected String getWindowText() {
		return m_checkBoxList.getSelectedText();
	}

	@Override
	public void innerLoad() {
		loadData();
	}

	@Override
	boolean isSelectedIndex(int index) {
		return m_checkBoxList.getCheck(index) != 0;
	}

	@Override
	public boolean isWindowEnabled() {
		return m_checkBoxList.isEnabled();
	}

	@Override
	public boolean isWindowVisible() {
		return m_checkBoxList.isVisible();
	}

	private void loadDataAsSlave() {
		int oldMainIterator;
		int mainTermSize = m_mainTermLiteralStruct.m_descrArray.size();
		oldMainIterator = m_mainIterator;
		for (int k = 0; k < mainTermSize; k++) {
			m_mainIterator = k;
			setLiteralStruct();
			loadRecords(true);
		}
		m_mainIterator = oldMainIterator;
		setLiteralStruct();
	}

	@Override
	public void loadDescrList() {
		m_checkBoxList.removeAll();
		loadVerboseFromDescrArray();
	}

	private void loadRecords(boolean asSlave) {
		if (m_literalStruct != null) {
			int size = m_literalStruct.m_descrArray.size();
			if (asSlave)
				prepareBuffer(m_mainIterator, size);
			else
				m_checkBoxList.buildTruthVector();
			WResultSet rs = new WResultSet(null, dataQuery());
			if (rs.open(m_queryDefPostStatement)) {
				long termWField;
				while (!rs.isEOF()) {
					termWField = rs.integerValue(m_targetDatumField);
					for (int j = 0; j < size; j++)
						if (asSlave) {
							if (termWField == m_literalStruct.m_descrArray.get(j).id) {
								m_buffers.get(m_mainIterator).set(j, 1);
								break;
							}
						} else {
							if (termWField == (m_checkBoxList.getItemData(j))) {
								m_checkBoxList.setCheck(j, 1);
								break;
							}
						}

					rs.next();
				}
				rs.close();
			}
			if (!asSlave) {
				m_updatingActor = true;
				m_panel.guiDataExch(false);
			}
			lookForDataStructure(rs);
		}
	}

	@Override
	void loadVerboseFromDescrArray() {
		super.loadVerboseFromDescrArray();
		if (!m_panel.injectedDialog().m_editOrNew_command)
			m_checkBoxList.enableChkList(false);
	}

	@Override
	public void manageTermConsistence() {
		reloadData();
	}

	void prepareBuffer(int idx, int size) {
		int j;
		int currentSize = m_buffers.get(idx).size();
		for (j = 0; j < size - currentSize; j++)
			m_buffers.get(idx).addElement(new Integer(0));
		for (j = 0; j < size; j++)
			m_buffers.get(idx).set(j, 0);
	}

	@Override
	public void refresh() {
		if (m_slave) {
			if (m_mainIterator >= 0)
				updateCheckBoxes(false);
			m_mainIterator = mainSelection();
			clearComponent();
			loadVerboseFromDescrArray();
			if (m_buffers.size() == 0)
				loadData();
			updateCheckBoxes(true);
		}
	}

	void reloadData() {
		if (m_loadOnly)
			loadData();
	}

	@Override
	public void show(boolean truth) {
		m_checkBoxList.setVisible(truth);
	}

	private boolean storeDataAsSlave() {
		boolean success = true;
		int mainTermSize = m_mainTermLiteralStruct.m_descrArray.size(), slaveTermSize, oldMainIterator;
		oldMainIterator = m_mainIterator;
		for (int k = 0; k < mainTermSize; k++) {
			m_mainIterator = k;
			setLiteralStruct();
			if (m_literalStruct != null) {
				slaveTermSize = m_literalStruct.m_descrArray.size();
				clearData(false);
				WResultSet rs = createAndOpenWRset();
				if (rs != null) {
					for (int i = 0; i < slaveTermSize; i++) {
						if (m_buffers.get(m_mainIterator).get(i) != 0)
							success = addRecord(rs, true, i);
						if (!success)
							break;
					}
					rs.close();
				}
			}
			if (!success)
				break;
		}
		m_mainIterator = oldMainIterator;
		setLiteralStruct();
		return success;
	}

	@Override
	protected void updateAspect() {
		if (!m_updatingActor) {
			if (m_reloadNeeded) {
				clearComponent();
				loadVerboseFromDescrArray();
				m_reloadNeeded = false;
			}
			loadData();
		}
	}

	void updateCheckBoxes(boolean direct) {
		int iDim = m_checkBoxList.getRowQty();
		if (direct)
			for (int i = 0; i < iDim; i++)
				m_checkBoxList.setCheck(i, m_buffers.get(m_mainIterator).get(i));
		else
			for (int i = 0; i < iDim; i++)
				m_buffers.get(m_mainIterator).set(i, m_checkBoxList.getCheck(i));
	}

}
