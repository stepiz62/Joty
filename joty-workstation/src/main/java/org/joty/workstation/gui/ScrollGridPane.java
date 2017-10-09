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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.joty.app.LiteralsCollection.DescrStruct;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.JotyDataBuffer;

/**
 * A ScrollPane, made abstract and specialized in controlling the scrolling of a
 * 'grid' (a multi-row component).
 * <p>
 * It prepares the association with a {@code JotyDataBuffer} object and connects
 * the selection of a row with the current record of the buffer.
 * <p>
 * In this class the most part of the dispatching of the selection and of the
 * double-click user actions is defined; the built-in listeners that will be
 * registered by the managed component in the concrete class, invokes the
 * methods responsible of this.
 * 
 * @see JotyDataBuffer
 * 
 */

public abstract class ScrollGridPane extends ScrollPane {

	protected class ClickHandler extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (!e.isConsumed()) {
				if (e.getClickCount() == 2)
					manageDoubleClick(e);
				e.consume();
			}
		}
	}

	protected class ListSelectionHandler implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (m_changeEventsEnabled && !e.getValueIsAdjusting())
				onSelchange(e);
		}

	}

	public boolean m_changeEventsEnabled;

	public GridTerm m_gridTerm;

	public ScrollGridPane() { // for use with WBE only
	}

	public ScrollGridPane(Panel panel, GridTerm term) {
		super(panel, term);
		m_gridTerm = term;
		m_changeEventsEnabled = true;
	}

	public void addTerm(DescrStruct descriptionRec) {}

	protected void doActualRemoval() {
		getPaneComponent().removeAll();
	}

	public abstract void ensureIndexIsVisible(int row);

	protected JotyDataBuffer getBuffer() {
		return null;
	}

	@Override
	public boolean getRelatedEnable() {
		return getSelection() != -1;
	}

	public int getSelectedColumn(MouseEvent e) {
		return 0;
	}

	public String getSortInfo() {
		return null;
	}

	public void initVerboseLayout() {}
	public void managedAppend(GridManager gridManager, int iDim) {}
	public void managedDeleteRow(int m_currentRowPos) {}
	public void managedListLoad(TermContainerPanel termContainerPanel) {}
	public void managedUpdateRow(GridManager manager) {}
	public void nextRow() {}
	public void previousRow() {}
	public void setColsProperties() {}
	public void setSelection(long i) {}
	public void setSortInfo(String mainSortIndex) {}

	public abstract int getRowQty();

	protected void manageDoubleClick(MouseEvent e) {
		if (m_term != null)
			((GridTerm) m_term).manageDoubleClick(e);
		else if (m_panel.m_actionOnRowHandler != null)
			m_panel.m_actionOnRowHandler.doAction(null, getSelectedColumn(e));
		else if (m_panel instanceof SearcherPanel) {
			if (!m_panel.m_insidePanel)
				((SearcherPanel) m_panel).rowAction();
		} else if (Application.m_app.m_dialogOpeningAsValueSelector) {
			m_panel.m_gridManager.storeSelectedValues(null);
			m_panel.m_dialog.close();
		}
	}

	protected void onSelchange(ListSelectionEvent e) {
		if (m_term == null) {
			if (m_panel.m_insidePanel)
				getBuffer().m_cursorPos = getSelection();
			else {
				m_panel.m_dialog.onGridSelChange(e, m_panel);
				if (Application.m_app.m_dialogOpeningAsValueSelector) {
					JButton button = m_panel.m_dialog.getSelectorButton();
					button.setEnabled(getSelection() != -1);
				}
			}
		} else {
			GridTerm term = (GridTerm) m_term;
			term.checkSlaveTermBuffer();
			if (term.m_dataBuffer != null)
				term.m_dataBuffer.m_cursorPos = getSelection();
			term.checkSelection();
			term.refreshSlaveTerm();
			term.enableRelatedButtons();
			if (term.m_selectionHandler != null)
				term.m_selectionHandler.selChange(term);
		}
	}

	@Override
	public void removeAll() {
		m_changeEventsEnabled = false;
		doActualRemoval();
		m_changeEventsEnabled = true;
		signalRemoval();
	}

	protected void signalRemoval() {}
	protected abstract int getSelection();

}
