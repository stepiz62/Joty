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

import javax.swing.JComponent;

import org.joty.app.LiteralsCollection.DescrStruct;
import org.joty.app.LiteralsCollection.DescrStruct;
import org.joty.app.LiteralsCollection.LiteralStructParams;
import org.joty.common.JotyTypes;
import org.joty.common.Utilities;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.app.Application.LiteralStruct;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.gui.TermContainerPanel.ListeningState;
import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * This class is a container for a {@code ComboBox} object and extends
 * {@code DescrTerm} to have available a {@code LiteralStruct} object to use for
 * populating the ComboBox.
 * 
 * @see org.joty.workstation.app.Application.LiteralStruct
 * @see ComboBox
 *
 */
public class ComboBoxTerm extends DescrTerm {
	

	ComboBox m_cmb;

	public ComboBoxTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
		checkLiteralStructIsDynamic(params);
	}

	/**
	 * Builds an already instantiated and cataloged LiteralStruct object.
	 * 	
	 * @see Application#loadDataIntoLiteralStruct(String, String, String,
	 *      LiteralStruct, LiteralStructParams)
	 * @see org.joty.app.Common#m_literalStructFilter
	 */
	public void buildLiteralStruct(String table, String idField, String descrField, String filter) {
		Application app = m_app;
		m_literalStruct.clear();
		app.m_common.m_literalStructFilter = filter;
		LiteralStructParams descrParms = m_literalsCollectionInstance.new LiteralStructParams();
		descrParms.withBlank = true;
		app.loadDataIntoLiteralStruct(table, idField, descrField, m_literalStruct, descrParms);
	}

	@Override
	public void checkRendering() {
		if (m_dynamicLiteralStructParams != null)
			feedComboByDynamicDescrVector();
	}

	@Override
	public void clear() {
		if (m_dataType == JotyTypes._text)
			m_strVal = "";
		else
			setInteger(-1);
		setToNull(true);
		ListeningState listeningState = m_panel.setPanelActionListeningOff();
		m_cmb.setCurSel(-1);
		m_panel.restorePanelActionListening(listeningState);
	}

	@Override
	protected void clearComponent() {
		switch (m_dataType) {
			case JotyTypes._text:
			case JotyTypes._long:
			case JotyTypes._int:
			case JotyTypes._dbDrivenInteger:
				m_cmb.setCurSel(-1);
				break;
		}
	}

	@Override
	void clearNonStructuredCtrl() {
		clearComponent();
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		m_cmb = new ComboBox(panel, this, m_dataType == JotyTypes._text);
	}

	@Override
	protected String doRender(WrappedField wfield) {
		if (m_dataType != JotyTypes._text) {
			return descrByItemData(wfield.getInteger());
		} else
			return super.doRender(wfield);
	}

	@Override
	protected boolean doValidate() {
		boolean success = true;
		switch (m_dataType) {
			case JotyTypes._text:
				if (m_required && m_strVal.isEmpty()) {
					alert("MustNotBeEmpty");
					success = false;
				}
				break;
			case JotyTypes._long:
			case JotyTypes._int:
			case JotyTypes._dbDrivenInteger:
				if (m_required) {
					if (m_cmb.getCurSel() == -1) {
						alert("MustBeSelected");
						success = false;
					}
				}
				break;
		}
		return success;
	}

	@Override
	protected void enable(boolean predicate) {
		m_cmb.setEnabled(predicate);
	}

	
	
	/**
	 * Invoked when the ComboBox is editable, it switches the LiteralStruct
	 * instance to be the dynamic one (defined by
	 * {@code setDynamicLiteralStructParams} if the character '%' is found in the edited text.
	 */
	void feedComboByDynamicDescrVector() {
		ListeningState listeningState = m_panel.setPanelActionListeningOff();
		ComboBox.SelectionState selectionState = m_cmb.getEditSel();
		String text = m_cmb.getText();
		if (text.indexOf("%") >= 0) {
			if (!m_defaultLiteralStructLost)
				m_defaultLiteralStructLost = true;
			LiteralStruct literalStruct = (LiteralStruct) m_app.m_common.literalStruct(m_dynamicLiteralStructParams.m_structName);
			m_panel.cleanDescr(literalStruct);
			m_app.m_common.m_literalStructFilter = String.format("upper(%1$s) LIKE upper('%2$s')", 
														m_dynamicLiteralStructParams.m_descrFldName, Utilities.sqlEncoded(text));
			LiteralStructParams descrParams = createDescrArrayParams();
			descrParams.sortedByID = false;
			descrParams.strKeyFldName = m_dynamicLiteralStructParams.m_descrFldName;
			m_app.buildLiteralStruct(m_dynamicLiteralStructParams.m_dbTableName, m_dynamicLiteralStructParams.m_keyFldName, 
										m_dynamicLiteralStructParams.m_descrFldName, literalStruct, descrParams);
			m_panel.setDescr(m_name, literalStruct);
			loadComboBoxList();
		} else {
			if (m_defaultLiteralStructLost) {
				m_panel.setDescr(m_name, m_defaultLiteralStruct);
				loadComboBoxList();
				m_defaultLiteralStructLost = false;
			}
			int indexTobeSet = -1;
			if (text.length() > 0)
				for (int i = 0; i < m_literalStruct.m_descrArray.size(); i++)
					if (m_literalStruct.m_descrArray.get(i).descr.toLowerCase().indexOf(text.toLowerCase()) == 0) {
						indexTobeSet = i;
						break;
					}
			m_cmb.setCurSel(indexTobeSet);
		}
		m_cmb.setText(text);
		m_cmb.setEditSel(selectionState);
		m_panel.restorePanelActionListening(listeningState);
	}

	@Override
	public JComponent getComponent() {
		return m_cmb;
	}

	@Override
	public long getCurSelData(boolean updateData) {
		if (updateData) {
			String cmbText = m_cmb.getText();
			setCurSel(cmbText);
		}
		return m_cmb.getItemData(m_cmb.getCurSel());
	}

	@Override
	public String getCurSelStrKey() {
		if (m_cmb.getCurSel() >= 0)
			return m_literalStruct.m_descrArray.get(m_cmb.getCurSel()).strKey;
		else
			return "";
	}

	protected Integer getPosIndexFromData(long value) {
		Integer retVal = null;
		if (m_literalStruct == null) {
			int j;
			for (j = 0; j < m_cmb.getItemCount(); j++)
				if (m_cmb.getItemData(j) == value)
					break;
			if (j == m_cmb.getItemCount())
				j = -1;
			retVal = j;
		} else
			retVal = m_literalStruct.m_descrReverseMap.get(value);
		return retVal;
	}

	@Override
	public int getSelection() {
		return m_cmb.getCurSel();
	}

	@Override
	public String getWindowText() {
		return ((DescrStruct) m_cmb.getSelectedItem()).descr;
	}

	@Override
	public void guiDataExch(boolean in) {
		super.guiDataExch(in);
		if (in) {
			if (m_dataType == JotyTypes._text)
				m_strVal = m_cmb.getText();
			else {
				setInteger(selectionData());
				if (getSelection() == -1)
					setToNull(true);
			}
			if (m_literalStruct != null) {
				String temp = getCurSelStrKey();
				if (temp != null && temp.length() > 0 || m_dataType != JotyTypes._text)
					m_strVal = temp;
			}
		} else {
			if (isNull())
				setCurSel(-1);
			else if (m_dataType == JotyTypes._text) {
				if (m_cmb != null)
					setCurSel(m_strVal);
			} else
				setSelection((int) getInteger(), true);
		}
	}

	@Override
	void init() {
		super.init();
		loadComboBoxList();
	}

	@Override
	public boolean isWindowEnabled() {
		return m_cmb.isEnabled();
	}

	@Override
	public boolean isWindowVisible() {
		return m_cmb.isVisible();
	}

	protected void loadComboBoxList() {
		boolean oldPopUpVisibleState = m_cmb.isPopupVisible();
		m_cmb.hidePopup();
		m_cmb.removeAllItems();
		ListeningState listeningState = m_panel.setPanelActionListeningOff();
		pickUpAndPumpData();
		m_panel.restorePanelActionListening(listeningState);
		if (oldPopUpVisibleState)
			m_cmb.showPopup();
	}

	protected void pickUpAndPumpData() {
		if (m_literalStruct != null) {
			m_cmb.m_loadingData = true;
			for (int i = 0; i < m_literalStruct.m_descrArray.size(); i++)
				m_cmb.addItem(m_literalStruct.m_descrArray.get(i));
			m_cmb.m_loadingData = false;
		}
	}

	@Override
	public void loadDescrList() {
		m_cmb.removeAllItems();
		loadComboBoxList();
	}

	/**
	 * Invoked by the constructor, if it is not provided the name of a cataloged
	 * LiteralStruct such an object is instantiated here and is set dynamic,
	 * that is it can be nameless and not cataloged in the
	 * {@code Application.m_literalStructMap} map.
	 * 
	 * @see org.joty.workstation.app.Application.LiteralStruct
	 */
	protected void checkLiteralStructIsDynamic(TermParams params) {
		if (params.m_descrSetName == null) {
			LiteralStruct literalStruct = m_app.new LiteralStruct(m_app);
			literalStruct.m_dynamic = true;
			literalStruct.init(null, false);
			m_literalStruct = literalStruct;
		}
		if (Beans.isDesignTime())
			params.m_descrSetName = null;
	}

	protected void selectionEffects(int index) {}

	public void selectItem(long itemValue) {
		setSelection((int) itemValue, true);
	}

	public void selectItem(String itemValue) {
		if (m_literalStruct != null) {
			Integer posIdx = itemValue == null ? null : (Integer) m_literalStruct.m_strKeyRevMap.get(itemValue);
			if (posIdx != null)
				m_cmb.setCurSel(posIdx);
			else
				m_cmb.setText(itemValue);
		} else {
			String str;
			int j;
			for (j = 0; j < m_cmb.getItemCount(); j++) {
				str = ((DescrStruct) m_cmb.getItemAt(j)).descr;
				if (str == itemValue)
					break;
			}
			if (j == m_cmb.getItemCount())
				j = -1;
			if (j == -1 && m_cmb.m_term.m_dataType == JotyTypes._text)
				m_cmb.setText(itemValue);
			else
				m_cmb.setCurSel(j);
		}
	}

	@Override
	protected void set(Term srcTerm) {
		if (m_dataType == JotyTypes._text) {
			if (srcTerm.m_dataType == JotyTypes._text)
				setCurSel(((ComboBoxTerm) srcTerm).getCurSelStrKey());
		} else
			super.set(srcTerm);
	}

	@Override
	int setCurSel(String val) {
		int retval = -1;
		boolean valueFound = false;
		m_app.ASSERT(m_dataType == JotyTypes._text);
		if (val != null && val.length() > 0) {
			Integer posIdx = m_literalStruct.m_strKeyRevMap.get(val);
			if (m_dataType == JotyTypes._text && posIdx != null) {
				retval = m_cmb.setCurSel(posIdx);
				valueFound = true;
			}
			if (m_dataType != JotyTypes._text) {
				selectItem(val);
				valueFound = true;
			}
		}
		if (!valueFound) {
			m_cmb.setCurSel(-1);
			m_cmb.setText(val);
		}
		return retval;
	}

	@Override
	public int setSelection(long val, boolean basedOnData) {
		int retval = -1;
		if (basedOnData && !Beans.isDesignTime()) {
			Integer posIdx = getPosIndexFromData(val);
			if (posIdx != null)
				retval = m_cmb.setCurSel(posIdx);
			else
				m_cmb.setCurSel(-1);
			setInteger(val);
		} else
			retval = m_cmb.setCurSel((int) val);
		selectionEffects(retval);
		return retval;
	}

	@Override
	public void show(boolean truth) {
		m_cmb.setVisible(truth);
	}

	@Override
	public String sqlValueExpr() {
		if (m_dataType == JotyTypes._text)
			return m_cmb.getText();
		else {
			long val = getInteger();
			return val < 0 ? "" : String.format("%1$d", val);
		}
	}

	@Override
	public void storeState(WResultSet rs) {
		String temp;
		if (m_dataType == JotyTypes._dbDrivenInteger || m_literalStruct != null) {
			if (m_literalStruct.m_strKeyRevMap != null) {
				temp = getCurSelStrKey();
				rs.setValue(m_dbFieldName, temp != null && temp.length() > 0 ? temp : m_strVal, false, 0);
			} else {
				if (isNull())
					rs.setMemberToNull(m_dbFieldName);
				else
					rs.setIntegerValue(m_dbFieldName, selectionData());
			}
		} else
			super.storeState(rs);
	}

	@Override
	public String toString() {
		if (m_dataType == JotyTypes._text)
			return descrByItemData(m_strVal);
		else if (getInteger() < 0)
			return m_cmb.getText();
		else
			return descrByItemData(getInteger());
	}

	@Override
	public void updateState(WrappedField rowCell) {
		if (m_dataType == JotyTypes._text)
			selectItem(rowCell.m_strVal);
		else
			selectItem(rowCell.getInteger());
		super.updateState(rowCell);
	}

	@Override
	public void updateState(WResultSet rs) {
		if (isNull())
			setCurSel(-1);
		else if (m_dataType == JotyTypes._text)
			selectItem(rs.stringValue(m_dbFieldName));
		else
			selectItem(rs.integerValue(m_dbFieldName));
	}

}
