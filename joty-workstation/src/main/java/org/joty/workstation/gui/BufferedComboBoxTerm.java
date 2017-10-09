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

import org.joty.access.PostStatement;
import org.joty.app.LiteralsCollection.DescrStruct;
import org.joty.common.JotyTypes;
import org.joty.common.BasicPostStatement;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.JotyDataBuffer;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * It is a container of an instance of an internally defined extension of the
 * ComboBox class.
 * <p>
 * It uses a JotyDataBuffer to host an entire record set (instead of a set of
 * {@code DescrStruct} objects), and it makes possible the choice of the field to be
 * displayed by the ComboBox component and associates the current selection of
 * it to the corresponding record in the buffer.
 * 
 * @see ComboBox
 * @see org.joty.app.LiteralsCollection.DescrStruct
 * @see JotyDataBuffer
 * 
 */
public class BufferedComboBoxTerm extends ComboBoxTerm {

	public class BufferedComboBox extends ComboBox {

		BufferedComboBoxTerm m_bufferedTerm;

		public BufferedComboBox(TermContainerPanel panel, BufferedComboBoxTerm bufferedComboBoxTerm) {
			super(panel, bufferedComboBoxTerm);
			m_bufferedTerm = bufferedComboBoxTerm;
		}

		@Override
		protected void newIndexEffects() {
			super.newIndexEffects();
			m_bufferedTerm.m_dataBuffer.m_cursorPos = getSelectedIndex();
		}

	}

	private JotyDataBuffer m_dataBuffer;
	private String m_bufferKey;
	private String m_visibleField;
	private String m_query;

	public BufferedComboBoxTerm(TermContainerPanel panel, TermParams params) {
		super(panel, JotyTypes._long, params);
		m_dataBuffer = new JotyDataBuffer();
	}

	public JotyDataBuffer buffer() {
		return m_dataBuffer;
	}

	public void config(String bufferKey, String bufferVisibleField, String query) {
		m_bufferKey = bufferKey;
		m_visibleField = bufferVisibleField;
		m_query = query;

	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		m_cmb = new BufferedComboBox(panel, this);
	}

	@Override
	protected String doRender(WrappedField wfield) {
		return m_dataBuffer.strValue(m_visibleField, (int) wfield.getInteger());
	}

	@Override
	protected Integer getPosIndexFromData(long value) {
		return m_dataBuffer.getKeyPos(value);
	}

	@Override
	void init() {
		m_dataBuffer.setKeyFieldName(m_bufferKey);
		PostStatement postStatement = m_panel.createContextPostStatement(m_name);
		if (m_panel instanceof DataAccessPanel && ((DataAccessPanel) m_panel).m_localAccessor) {
			m_app.m_accessor.setPostStatement(postStatement);
			m_query = m_app.m_accessor.getQueryFromPostStatement();
		}
		WResultSet rs = new WResultSet(null, m_query);

		m_dataBuffer.empty(false);
		m_dataBuffer.loadData(rs, postStatement);
		super.init();
	}

	@Override
	protected void pickUpAndPumpData() {
		DescrStruct descrStruct = null;
		for (int i = 0; i < m_dataBuffer.m_records.size(); i++) {
			descrStruct = m_literalsCollectionInstance.new DescrStruct();
			descrStruct.id = m_dataBuffer.getKeyLongVal(i);
			descrStruct.descr = m_dataBuffer.strValue(m_visibleField, i);
			m_cmb.addItem(descrStruct);
		}
	}

	@Override
	protected void checkLiteralStructIsDynamic(TermParams params) {}

	@Override
	protected void selectionEffects(int index) {
		m_dataBuffer.m_cursorPos = index;
	}

}
