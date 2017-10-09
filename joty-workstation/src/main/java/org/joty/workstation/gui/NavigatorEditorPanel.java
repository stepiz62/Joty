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

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.table.TableCellEditor;

import org.joty.access.PostStatement;
import org.joty.common.JotyTypes;
import org.joty.common.BasicPostStatement;
import org.joty.common.Utilities;
import org.joty.common.Utilities.Stocker;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.data.JotyDataBuffer.Record;
import org.joty.workstation.gui.GridRowDescriptor.CellDescriptor;

/**
 * Allows the data editing to occur directly in its grid. It does not use the
 * editing buttons of the container {@code JotyDialog} instance: the editing
 * operations of the javax.swing.table.TableCellEditor implementation are used
 * instead and they are mapped on actions accessing the database: the
 * {@code processValueIfNeeded} method is let to be invoked by the
 * TableCellEditor instance of the table cell under editing, more specifically
 * by its {@code stopCellEditing} method.
 * <p>
 * Actually the stopCellEditing method of the JotyCellEditor class is
 * 'triggered' also by the NavigatorEditorPanel methods, those invoked when the
 * containing JotyDialog closes and when the selection of an
 * AnalogicalRowSelector object, by changing the row selection state, infers the
 * completion of any editing operation.
 * <p>
 * This class, overwrite some main ancestor methods, even for cutting away
 * features not used, but must be noted that the support for the the Accessor
 * mode option and for the duality of the running mode of the application is
 * maintained. .
 * 
 * 
 * @see NavigatorPanel
 * @see JotyDialog
 * @see Table
 * @see org.joty.workstation.gui.JotyTextField.JotyCellEditor
 * @see AnalogicalRowSelector
 * 
 */
public class NavigatorEditorPanel extends NavigatorPanel {

	public class FieldActionDescriptor {
		public boolean actionIsNeeded = true;
		public boolean success;
	}

	public interface InsertFieldEvaluator {
		boolean setValue(String fieldName);
	}

	String m_existenceMonitorKeyField;
	String m_delRecordOnNullField;
	protected NavigatorBuffer m_gridBuffer;
	private InsertFieldEvaluator m_insertFieldEvaluator;

	boolean m_insertAction;

	public NavigatorEditorPanel() {}

	@Override
	public void checkAndSetLook() {}

	@Override
	public boolean checkHasDone() {
		return checkRowEditingEnd() && super.checkHasDone();
	}

	public boolean checkRowEditingEnd() {
		TableCellEditor tableCellEditor = ((Table) getGridManager().getListComponent()).m_jtable.getCellEditor();
		boolean retVal = true;
		if (tableCellEditor != null)
			retVal = tableCellEditor.stopCellEditing();
		return retVal;
	}

	@Override
	protected void doGuiDataExch(boolean store) {}

	@Override
	protected void effectsOnForm() {
		manageAnalogicalBehavior();
	}

	@Override
	public void effectsOnTerms(Record row) {}

	@Override
	protected void inferCompletion() {
		checkRowEditingEnd();
	}

	@Override
	public boolean init() {
		if (m_existenceMonitorKeyField == null) {
			Application.m_app.JotyMsg(this, "existenceMonitorKeyField must be set !");
			return false;
		}
		setQuery();
		WResultSet rs = new WResultSet(null, m_finalQuery);
		if (rs.open(m_queryDefPostStatement)) {
			String fieldName;
			for (int i = 0; i < rs.m_cursor.m_fields.length; i++) {
				fieldName = rs.m_cursor.m_fields[i].m_strName;
				if (!dbFieldHosted(fieldName))
					addField(fieldName, rs.m_cursor.m_fields[i].m_nType);
			}
			rs.close();
		}

		boolean retVal = super.init();
		((Table) getGridManager().getListComponent()).setEnabled(true);
		m_gridBuffer = getGridManager().m_gridBuffer;
		return retVal;
	}

	private boolean isEditable(String fieldName) {
		CellDescriptor cellDescriptor = m_gridRowDescriptor.get(fieldName);
		return cellDescriptor != null && cellDescriptor.m_editable;
	}

	public FieldActionDescriptor processValueIfNeeded(String fieldName) {
		FieldActionDescriptor actionDescriptor = new FieldActionDescriptor();
		actionDescriptor.success = true;
		WrappedField existenceMonitorWField = m_gridBuffer.getWField(m_existenceMonitorKeyField);
		String whereClause = null;
		m_insertAction = existenceMonitorWField.isNull();
		boolean deleteRecord = false;
		boolean delRecordOnNullField_IsNull = m_delRecordOnNullField != null && m_gridBuffer.getWField(m_delRecordOnNullField).isNull();
		boolean actionFieldIsDeletor = m_delRecordOnNullField != null && m_delRecordOnNullField.compareToIgnoreCase(fieldName) == 0;
		if (m_insertAction) {
			if (delRecordOnNullField_IsNull && actionFieldIsDeletor || m_gridBuffer.getWField(fieldName).isNull()) {
				actionDescriptor.actionIsNeeded = false;
				return actionDescriptor;
			}
		} else {
			whereClause = " WHERE " + m_gridBuffer.m_keyName + " = " + m_gridBuffer.integerValue(m_existenceMonitorKeyField);
			deleteRecord = delRecordOnNullField_IsNull && actionFieldIsDeletor;
		}
		if (deleteRecord) {
			if (buildAndExecDeletion(whereClause))
				reLoadData();
			else
				Application.m_app.JotyMsg(this, "Record not deleted !");
		} else {
			PostStatement postStatement = null;
			if (getDialog().m_accessorMode)
				postStatement = createContextPostStatement();
			WResultSet rs = new WResultSet(m_mainDataTable, null, true, null, postStatement);
			if (! m_app.m_webMode && whereClause != null)
				rs.m_sql += whereClause;
			if (rs.open(true, postStatement)) {
				rs.setFieldNotToUpdate(m_IdFieldName);
				if (m_app.m_webMode && whereClause != null)
					rs.m_sql += whereClause;
				m_isNewRec = m_insertAction;
				if (m_insertAction)
					setId(rs);
				if (doUpdate(m_insertAction, rs, false, true)) {
					if (m_insertAction) {
						boolean idGot = true;
						if (m_app.m_webMode)
							idGot = checkForIdentifyingId();
						if (idGot)
							m_gridBuffer.setInteger(m_existenceMonitorKeyField, m_identifyingID);
						reloadGrid();
					}
				} else {
					actionDescriptor.success = false;
					Application.m_app.JotyMsg(this, "Value not set !");
				}
			}
		}
		return actionDescriptor;
	}

	@Override
	public void refresh() {}

	protected void setDelRecordOnNullField(String delRecordOnNullField) {
		m_delRecordOnNullField = delRecordOnNullField;
	}

	protected void setExistenceMonitorKeyField(String fieldName) {
		m_existenceMonitorKeyField = fieldName;
		addField(fieldName, JotyTypes._long);
	}

	protected void setInsertFieldEvaluator(InsertFieldEvaluator insertFieldEvaluator) {
		m_insertFieldEvaluator = insertFieldEvaluator;
	}

	@Override
	protected boolean storeWFieldsData(WResultSet rs) {
		String fieldName;
		boolean addTheField;
		for (int i = 0; i < rs.m_colCount; i++) {
			fieldName = rs.m_cursor.m_fields[i].m_strName;
			if (rs.m_cursor.m_fields[i].m_toUpdate) {
				if (m_insertAction || isEditable(fieldName)) {
					addTheField = true;
					if (m_insertAction && m_insertFieldEvaluator != null)
						addTheField = m_insertFieldEvaluator.setValue(fieldName);
					if (!addTheField)
						addTheField = isEditable(fieldName);
					if (addTheField)
						rs.m_actionFields.add(fieldName);
				}
			}
		}
		boolean retVal;
		Stocker actionFields = Utilities.m_me.new Stocker();
		if (m_app.m_webMode)
			for (String string : rs.m_actionFields)
				actionFields.add(string);
		retVal = m_gridBuffer.writeRecord(rs);
		if (m_app.m_webMode) {
			rs.m_actionFields.clear();
			for (String string : actionFields)
				rs.m_actionFields.add(string);
		}
		return retVal; 
	}

	@Override
	public void updateCommandButtons(boolean idle) {
		for (Component component : m_dialog.m_buttonPane.getComponents())
			if (component instanceof JButton)
				component.setVisible(false);
		m_dialog.m_btnClose.setVisible(true);
	}

}
