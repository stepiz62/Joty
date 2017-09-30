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

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.beans.Beans;

import javax.swing.TransferHandler;

import org.joty.data.WrappedField;
import org.joty.workstation.data.JotyDataBuffer;

/**
 * It is a {@code DataScrollingPanel} class that either instantiates the {@code Table}
 * object (the data grid) and the associated GridManager that is going to use,
 * or adds it to its layout.
 * <p>
 * It is equipped with the chance to be a target in D&D operations and
 * collaborates with the {@code Application} class and with the
 * {@code JotyTableTransferHandler} class and its children to make it possible.
 * <p>
 * Currently only this descendant of {@code Panel} has been equipped this way
 * because it is currently the only reasonable candidate to have its content area
 * behave like a collector of D&D operations starting from the built-in
 * {@code Table} object.
 * 
 * @see org.joty.workstation.app.Application
 * @see JotyTableTransferHandler
 * @see Table
 * @see DataScrollingPanel
 * 
 */
public class NavigatorPanel extends DataScrollingPanel implements DropTargetListener {

	public Table m_table;
	private boolean m_hiddenGrid;

	public NavigatorPanel() {
		super();
		Table table = Factory.createTable(this);
		m_table = table;
		table.setBounds(10, 11, 430, 71);
		add(table);
		table.repaint();
		m_hiddenGrid = false;
		new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this, true, null);
	}

	@Override
	protected void checkForInitSelection() {
		if (m_table.getRowQty() > 0)
			if (m_hiddenGrid)
				m_gridManager.setSelectionOnKeyVal(m_table.getBuffer().getKeyLongVal(0));
			else
				super.checkForInitSelection();
	}

	@Override
	protected WrappedField contextKeyElem(String dbFieldName) {
		return keyElem(dbFieldName);
	}

	@Override
	protected boolean doneWithData() {
		return checkDataLoading();
	}

	@Override
	public boolean init() {
		boolean retVal = super.init();
		if (m_hiddenGrid)
			m_table.setVisible(false);
		return retVal;
	}

	@Override
	protected void relatedEnable(boolean generalEnabling) {
		m_gridManager.enable(!generalEnabling);
		if (dialogGridManagerExists() && injectedDialog().m_gridManager != null)
			injectedDialog().m_gridManager.enable(!generalEnabling);
	}

	/**
	 * Particular use of this class can be done by hiding the data grid as using
	 * it as navigator only.
	 */
	public void setNavigationGridHidden() {
		m_hiddenGrid = true;
	}

	/**
	 * Prepares the support for queuing of the group of entities corresponding
	 * to the data records contained in the rows of the grid.
	 * <p>
	 * It initializes the NavigatorBuffer in order to get support of it and
	 * instantiates a DataTransferHandler flavor, designed just for the purpose
	 * to interact for the buffer in such a way prepared .
	 * 
	 * @see JotyDataBuffer#m_queueManager
	 * @see org.joty.workstation.data.JotyDataBuffer.QueueManager
	 * @see QueuedDataTransferHandler
	 * 
	 */
	protected void setRowsQueuing(int actionAsSource, String idField, String prevField, String nextField, JotyTableTransferHandler handler) {
		if (!Beans.isDesignTime())
			m_gridManager.m_gridBuffer.setRowsQueuing(idField, prevField, nextField);
		if (handler == null)
			new QueuedDataTransferHandler(actionAsSource, TransferHandler.COPY_OR_MOVE, (Table) getGridManager().getListComponent(), true);
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		m_app.m_dragDrainTriggerOn = m_app.m_currDnDjtable.getTable().m_panel == this && m_app.m_currDnDsourceIsDrainEnabled;
		m_app.m_DnDdrainIn = true;
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		m_app.m_dragDrainTriggerOn = false;
		m_app.m_DnDdrainIn = false;
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		m_app.m_dragDrainDropped = m_app.m_dragDrainTriggerOn;
		m_app.m_dragDrainTriggerOn = false;
		m_app.m_DnDdrainIn = false;
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {}
}
