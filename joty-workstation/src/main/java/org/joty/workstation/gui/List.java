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

import java.awt.Font;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import org.joty.access.Logger;
import org.joty.app.LiteralsCollection.DescrStruct;

/**
 * It is a {@code ScrollGridPane} that instantiates a
 * {@code javax.swing.JList<Object>} object as scroll-able object.
 * <p>
 * It uses the default model for its embedded object and lets it be populated in
 * each element with DescrStruct objects or with simple text.
 * <p>
 * In the first case the model is mapped on the content of the whole associated
 * LiteralStruct object and will render the {@code literal} member of each
 * object; and, furthermore, lets the list row have the {@code id} member as
 * datum associated. The list object, in this case, is a "read only" object used
 * typically as medium of choices to drive other data objects.
 * <p>
 * In the second case the model uses, as source of data, the associated
 * JotyDataBuffer, so it as as many rows as its record quantity: however the
 * rendering, by default, is again based on the LiteralStruct object which is
 * looked up by its {@code id} member value that matches the value of the buffer
 * key field. (see {@code ListTerm} class for alternative way of rendering).
 * 
 * @see ScrollGridPane
 * @see ListTerm
 * @see org.joty.app.LiteralsCollection.DescrStruct
 * @see #initVerboseLayout
 * 
 */
public class List extends ScrollGridPane {

	public JList<Object> m_list;
	public List m_targetList;
	protected DefaultListModel<Object> m_listModel;
	protected ListSelectionModel m_listSelectionModel;

	public List(Panel panel, GridTerm term) {
		super(panel, term);
		m_list = new JList<Object>();
		m_listModel = new DefaultListModel<Object>();
		m_list.setModel(m_listModel);
		m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_list.addMouseListener(new ClickHandler());
		m_listSelectionModel = m_list.getSelectionModel();
		m_listSelectionModel.addListSelectionListener(new ListSelectionHandler());
		setViewportView(m_list);
	}

	public int addString(String term) {
		return insertString(m_listModel.getSize(), term);
	}

	@Override
	public void addTerm(DescrStruct term) {
		m_listModel.addElement(term);
	}

	public int deleteString(int index) {
		int retVal = -1;
		try {
			m_listModel.removeElementAt(index);
			retVal = m_listModel.getSize();
		} catch (Exception e) {}
		return retVal;
	}

	@Override
	public void ensureIndexIsVisible(int row) {
		m_list.ensureIndexIsVisible(row);
	}

	public long getItemData(int getCurSel) {
		return getSelectedValue().id;
	}

	@Override
	public JComponent getPaneComponent() {
		return m_list;
	}

	@Override
	public int getRowQty() {
		return m_listModel.getSize();
	}

	public DescrStruct getSelectedValue() {
		return (DescrStruct) m_listModel.getElementAt(getSelection());
	}

	@Override
	public int getSelection() {
		return m_list.getSelectedIndex();
	}

	public String getString() {
		return (String) m_listModel.getElementAt(getSelection());
	}

	/**
	 * Used to populate the embedded component with the content of the
	 * associated {@code LiteralStruct} object
	 */ 
	@Override
	public void initVerboseLayout() {
		Vector<DescrStruct> descrArray = m_gridTerm.m_literalStruct.m_descrArray;
		for (int i = 0; i < descrArray.size(); i++)
			addTerm(descrArray.get(i));
	}

	public int insertString(int index, String text) {
		int retVal = -1;
		try {
			m_listModel.add(index, text);
			retVal = index;
		} catch (Exception e) {
			Logger.exceptionToHostLog(e);
		}
		return retVal;
	}

	public boolean isSelectedIndex(int index) {
		return m_list.isSelectedIndex(index);
	}

	@Override
	public void managedDeleteRow(int rowPos) {
		deleteString(rowPos);
		super.managedDeleteRow(rowPos);
	}

	@Override
	public void nextRow() {
		m_list.setSelectedIndex(m_list.getSelectedIndex() + 1);
	}

	@Override
	public void previousRow() {
		m_list.setSelectedIndex(m_list.getSelectedIndex() - 1);
	}

	@Override
	public void removeAll() {
		m_listModel.removeAllElements();
	}

	@Override
	public void setFont(Font font) {
		if (m_list != null)
			m_list.setFont(font);
		super.setFont(font);
	}

	@Override
	public void setFormat() {
		Font mono = new Font("Monospaced", Font.PLAIN, 12);
		setFont(mono);
		super.setFormat();
	}

	@Override
	public void setSelection(long val) {
		m_list.setSelectedIndex((int) val);
	}

	public void setString(int index, String term) {
		m_listModel.setElementAt(term, index);
	}
}
