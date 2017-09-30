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

import javax.swing.JComponent;

import org.joty.workstation.app.Application;
import org.joty.workstation.data.JotyDataBuffer;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.gui.DataAccessPanel.RenderRowMethodInterface;
import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * The class contains a {@code List} component and instantiates a
 * {@code JotyDataBuffer} to hold data that can be a reference to an id Literal
 * contained in a {@code LiteralStruct} object or something different.
 * <p>
 * By default, it populates the one only column, available in the embedded
 * object, with the text got from the associated LiteralStruct, since the whole
 * LiteralStruct is a static source of data or by looking at it by 'id' using
 * the value of the key column of the buffer. However the class provides a
 * method ({@code setRenderRowMethod}) for rendering text resulting from the
 * content of the buffer fields.
 * <p>
 * in the case the buffer is used as source of data the class provides an
 * override form of the {@code renderRecord} so that its implementation can
 * concretely make the rendering during the execution of the calling context
 * (the {@code JotyDataBuffer.loadData} method). The implementation of the
 * {@code bufferRender}) instead renders upon all other circumstances (see the
 * calling {@code Term.termRender} method).
 * <p>
 * In the case the source of data is the LiteralStruct as a whole, the rendering
 * is performed by the {@code DescrTerm.toString} method by being the DescrTerm
 * instance the object loaded in the list model element.
 * 
 * @see List
 * @see JotyDataBuffer
 * @see org.joty.workstation.app.Application.LiteralStruct
 * @see DataAccessPanel.RenderRowMethodInterface
 * @see #setRenderRowMethod(DataAccessPanel.RenderRowMethodInterface)
 * 
 */
public class ListTerm extends GridTerm {

	public List m_list;
	private RenderRowMethodInterface m_renderRowMethod;

	public ListTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
		if (!m_slave)
			m_dataBuffer = new JotyDataBuffer(m_targetDatumField);
	}

	@Override
	public void bufferRender() {
		int bufferSize = m_dataBuffer.m_records.size();
		if (m_newRowJustCreated) {
			m_newRowJustCreated = false;
			m_list.m_changeEventsEnabled = false;
			bufferRowRender(bufferSize - 1);
			m_list.m_changeEventsEnabled = true;
		} else
			for (int i = 0; i < bufferSize; i++)
				bufferRowRender(i);
	}

	private void bufferRowRender(int rowIndex) {
		m_dataBuffer.m_cursorPos = rowIndex;
		m_list.addString(m_renderRowMethod != null ? 
				m_renderRowMethod.method(null, m_dataBuffer) : 
				m_literalStruct.literal((int) m_dataBuffer.getKeyVal().getInteger()));
	}

	/** @see RenderRowMethodInterface */
	public void setRenderRowMethod(RenderRowMethodInterface renderRowMethod) {
		m_renderRowMethod = renderRowMethod;
	}
	
	@Override
	public void clear() {
		clearComponent();
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		m_list = new List(panel, this);
	}

	@Override
	protected boolean doValidate() {
		boolean success = true;
		if (m_required) {
			success = m_list.getSelection() >= 0;
			if (!success)
				alert("MustBeSelectedOne");
		}
		return success;
	}

	@Override
	protected void enable(boolean predicate) {
		m_list.setEnabled(predicate);
	}

	@Override
	public JComponent getComponent() {
		return m_list;
	}

	@Override
	int getCount() {
		return m_list.getRowQty();
	}

	@Override
	long getCurSelData(boolean updateData) {
		return m_list.getItemData(m_list.getSelection());
	}

	@Override
	public String getCurSelStrKey() {
		if (m_list.getSelection() >= 0)
			if (m_literalStruct != null)
				return m_literalStruct.m_descrArray.get(m_list.getSelection()).strKey;
			else if (m_dataBuffer != null)
				return m_dataBuffer.m_textKey ? m_dataBuffer.strValue(m_targetDatumField) : "";
			else
				return "";
		else
			return "";
	}

	@Override
	protected int getRowQty() {
		return m_list.getRowQty();
	}

	@Override
	public ScrollGridPane getScrollPane() {
		return m_list;
	}

	@Override
	public int getSelection() {
		return m_list.getSelection();
	}

	@Override
	protected String getWindowText() {
		return m_list.getSelectedValue().descr;
	}

	@Override
	public void innerLoad() {
		if (m_dataTable != null && !m_dataTable.isEmpty()) {
			clearComponent();
			m_mainIterator = -1;
			loadData();
		}
	}

	@Override
	boolean isSelectedIndex(int index) {
		return m_list.isSelectedIndex(index);
	}

	@Override
	public boolean isWindowEnabled() {
		return m_list.isEnabled();
	}

	@Override
	public boolean isWindowVisible() {
		return m_list.isVisible();
	}

	@Override
	public void loadDescrList() {
		m_list.removeAll();
		loadVerboseFromDescrArray();
	}

	@Override
	protected void removeAll() {
		m_list.removeAll();
	}

	@Override
	protected void renderAfterDeletion() {
		termRender(true, true);
	}

	@Override
	public void renderRecord(WResultSet rs, String m_keyFieldName) {
		String rowText;
		if (m_renderRowMethod != null)
			rowText = m_renderRowMethod.method(rs, null);
		else
			rowText = m_literalStruct.literal(rs.integerValue(m_keyFieldName));
		m_list.m_changeEventsEnabled = false;
		m_list.addString(rowText);
		m_list.m_changeEventsEnabled = true;
	}

	@Override
	public void selectLastRow() {
		if (m_list.getRowQty() > 0) {
			int targetSel = m_list.getRowQty() - 1;
			m_list.setSelection(targetSel);
			if (m_slaveTermName != null)
				m_panel.gridTerm(m_slaveTermName).m_mainIterator = targetSel;
		}
	}

	@Override
	public int setSelection(long val, boolean basedOnData) {
		// override not implemented yet for basedOnData=true
		m_app.ASSERT(!basedOnData);
		m_list.setSelection(val);
		return super.setSelection(val, basedOnData);
	}

	@Override
	public void show(boolean truth) {
		m_list.setVisible(truth);
	}

}
