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

import org.joty.common.BasicPostStatement;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.WField;
import org.joty.workstation.data.WResultSet;

/**
 * A GridBuffer implementation used by the {@code GridManager} class.
 * <p>
 * This class changes the way each WrappedField instance references its meta-data: the
 * building of the record happens with the storing of reference to the
 * associated {@code Term} object in the Panel; the WrappedField instance gets
 * meta-data by means of this reference.
 * <p>
 *
 * @see GridManager
 * @see Table
 * @see WrappedField
 * 
 */
public class NavigatorBuffer extends GridBuffer {


	public boolean m_hasKeys;
	public TermContainerPanel m_panel;
	public SearcherPanel m_searcher;

	NavigatorBuffer(TermContainerPanel panel) {
		m_panel = panel;
		m_keyIndex = -1;
		m_hasKeys = false;
		m_maxRecord = Beans.isDesignTime() ? 0 : Integer.parseInt(Application.m_common.m_paginationPageSize);
	}

	@Override
	protected void acquireRecordDescriptor(WResultSet rs) {
		acquireRecordDescriptorPart(rs, m_panel.m_terms, 0, true);
		acquireRecordDescriptorPart(rs, m_panel.m_keyElems.vector, m_panel.m_terms.size(), false);
		acquireRecordDescriptorPart(rs, m_panel.m_wfields.vector, m_panel.m_terms.size() + m_panel.m_keyElems.vector.size(), false);
	}

	void addTermsAsRecord() {
		Record record = new Record();
		buildRecord(record);
		m_records.add(record);
	}

	@Override
	protected void buildRecord(Record record) {
		int i;
		for (i = 0; i < m_panel.m_terms.size(); i++)
			record.m_data.add(new WField(Application.m_app));
		for (i = 0; i < m_panel.m_terms.size(); i++)
			record.m_data.get(i).copyWField(m_panel.m_terms.get(i), false, true);
		addWFieldElems(record, m_panel.m_keyElems.vector, true);
		addWFieldElems(record, m_panel.m_wfields.vector, false);
	}

	public void checkKey() {
		m_hasKeys = m_panel.m_IdFieldName != null && m_panel.m_IdFieldName.length() > 0 || m_keyName != null;
	}


	@Override
	protected BasicPostStatement createContextPostStatement() {
		return m_panel.createContextPostStatement();
	}

	@Override
	protected void getFromDataLayer(Record record, WResultSet rs) {
		for (int i = 0; i < m_panel.m_terms.size(); i++)
			record.m_data.get(i).getWField(rs);
		getWFieldFromDataLayer(record, rs, m_panel.m_keyElems.vector, m_panel.m_terms.size(), true);
		getWFieldFromDataLayer(record, rs, m_panel.m_wfields.vector, m_panel.m_terms.size() + m_panel.m_keyElems.vector.size(), false);
	}

	void getRecordFromPanel() {
		Record record = m_panel.m_gridManager.getRecordBuffer();
		for (int i = 0; i < m_panel.m_terms.size(); i++)
			record.m_data.get(i).copyWField(m_panel.m_terms.get(i), false);
	}

	@Override
	protected boolean loadDataBreak(int count) {
		boolean retVal;
		if (m_searcher == null)
			retVal = super.loadDataBreak(count);
		else {
			retVal = m_maxRecord > 0 && count > m_maxRecord;
			if (retVal)
				m_searcher.m_furtherRecords = true;
		}
		return retVal;
	}

	@Override
	protected void loadDataProlog(WResultSet rs) {
		super.loadDataProlog(rs);
		if (m_searcher != null) {
			if (rs.isEOF())
				m_searcher.resetIteration();
			m_searcher.m_furtherRecords = false;
		}
	}

}
