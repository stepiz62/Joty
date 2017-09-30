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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.Beans;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

import org.joty.access.Logger;
import org.joty.app.JotyException;
import org.joty.common.BasicPostStatement;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.JotyDataBuffer;
import org.joty.workstation.gui.Table.JotyJTable;

/**
 * Extends the {@code javax.swing.TransferHandler} class to work for a
 * {@code Table} object.
 * <p>
 * The class receives in the constructor the reference to the {@code Table}
 * object which this instance have to work for, so the setting (actually made to
 * the inner {@code JotyJTable} object) is made in the initialization and the
 * reference to the Table is hold for internal use .
 * <p>
 * In the constructor also the types of action along with the handler behaves
 * when the {@code JotyJTable} object is either the target or the source of the
 * D&D action, must be specified.
 * <p>
 * It can support the move of the dragged item toward a special target, a
 * {@code NavigatorPanel} instance content area, that behaves like a drain,
 * since used ideally to loose the item. This feature also must be declared in
 * the constructor invocation.
 * <p>
 * The data type transferred are a simple text string so that the
 * java.awt.datatransfer.StringSelection class is used to instantiate a
 * java.awt.datatransfer.Transferable object; however the
 * javax.swing.TransferHandler.createTransferable method is overridden in order
 * to deny the drop-ability over regions of the screen that don't own to the
 * Joty application.
 * <p>
 * Even if the data type chosen as vehicle is the text string, actually the
 * class manages transfer of an integer number and it is exchanged as text. The
 * class collaborates with the {@code Application} instance to dialogue with the
 * other side of the D&D operations for the transfer to occur.
 * <p>
 * The integer number exchanged is typically a id value of the entity hosted by
 * the {@code GridBuffer} of the source {@code Table} object.
 * <p>
 * In the code the concept of 'actor' takes place, for the {@code JotyJtable}
 * (and {@code Table} indeed) managed in the class. If the actor is the source,
 * the index of the selected row is used to pick the data id up from the data
 * buffer (the {@code valueToTransfer} method does it) and this is provided to
 * the {@code createTransferable} implementation. If the actor is the target,
 * the {@code getTargetIndex} method detects the index of the row on which or
 * next to which the drop will occur, and informs the calling context whether to
 * continue or not to.
 * <p>
 * It is manifest that the class instance understands which side it is on
 * (source or target) depending on which methods are called among those
 * overridden from the Swing ancestor: within each of those overrides it builds
 * its environmental state that is clearly dedicated to the respective case.
 * <p>
 * The {@code importData} and the {@code exportDone} overrides implement the
 * actual actions in the Joty scenario: they performs changes in the state of
 * the database; their implementation is such that the whole D&D operation is
 * self contained in a database transaction, no matter what the Joty running
 * mode is, another way to say could be that this class assures the D&D user
 * action to happen within a Joty transaction.
 * <p>
 * The transaction management performed by the {@code exportDone} implementation
 * can occur only when neither a transaction nor an atomic sql statement have
 * been performed by the 'target' side of the D&D action (see
 * {@link JotyTableTransferHandler#checkExportTransaction}).
 * <p>
 * The class identifies the abstract contribute that the handler must provide in
 * both side of the D&D action: far from any responsibility coming from the
 * ancestor and deriving from the needs of transaction management the contribute
 * is formalized by the
 * {@code exportAction, importAction, getRowToBeSelected, validate} methods.
 * <p>
 * The class supports the use of the application {@code Accessor} object, client
 * or server side located.
 * <p>
 * Furthermore the class participates in the publishers-subscribers model
 * implementation for the updating propagation of the managed information.
 * 
 * 
 * @see NavigatorPanel
 * @see Table
 * @see GridBuffer
 * @see org.joty.access.Accessor
 * 
 */
public abstract class JotyTableTransferHandler extends TransferHandler {
	class JotyStringSelection extends StringSelection {
		public JotyStringSelection(String data) {
			super(data);
		}

		@Override
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			return m_app.m_insideDataDialogs ? super.getTransferData(flavor) : null;
		}
	}

	int m_actionAsSource;
	/**
	 * Hosts the index of the selected row of the {@code JotyJTable} object when
	 * the 'actor' is the source
	 */
	protected int m_index;
	protected String m_id_dbField;
	protected boolean m_showDropLocation;
	protected String m_managedDbTable;
	/**
	 * Hosts the index of the selected row (or of the row adjacent to the
	 * insertion line determined by the hovering in the dropping region) of the
	 * {@code JotyJTable} object when the 'actor' is the target.
	 */
	int m_targetIndex;
	boolean m_internalMove;
	boolean m_success;
	Application m_app = Application.m_app;
	protected boolean m_moveToDrain;
	protected int m_actionAsTarget;

	protected Table m_table;
	/**
	 * The {@code JotyJTable} object in the {@code Table} object to which this
	 * instance is assigned
	 */
	protected JotyJTable m_actor;
	JotyDataBuffer m_buffer;

	public JotyTableTransferHandler(int actionAsSource, int actionAsTarget, Table table, boolean moveToDrain) {
		m_actionAsSource = actionAsSource;
		m_actionAsTarget = actionAsTarget;
		m_table = table;
		m_moveToDrain = moveToDrain;
		DataAccessPanel dataPanel = null;
		Panel panel = table.m_panel;
		if (panel != null && panel instanceof DataAccessPanel)
			dataPanel = (DataAccessPanel) panel;
		m_managedDbTable = dataPanel != null ? dataPanel.m_mainDataTable : null;
		if (!Beans.isDesignTime())
			m_id_dbField = m_table.m_gridTerm == null ? (dataPanel != null ? dataPanel.m_IdFieldName : null) : m_table.m_gridTerm.m_dataBuffer.m_keyName;
		m_showDropLocation = false;
		init();
	}

	private void init() {
		m_actor = m_table.m_jtable;
		m_buffer = m_table.getBuffer();
		postInit();
	}

	protected void postInit() {
		m_actor.setTransferHandler(this);
		if (m_actionAsSource != TransferHandler.NONE)
			m_actor.setDragEnabled(true);
	}

	@Override
	public boolean canImport(TransferHandler.TransferSupport support) {
		m_app.m_dragDrainDropped = false;
		boolean canImport = true;
		if (!support.isDrop())
			canImport = false;
		if (canImport) {
			support.setShowDropLocation(m_showDropLocation);
			if (!support.isDataFlavorSupported(DataFlavor.stringFlavor))
				canImport = false;
		}
		if (canImport) {
			if (m_actionAsTarget == TransferHandler.NONE)
				canImport = false;
			else {
				if (support.getComponent() instanceof AnalogicalRowSelector && !support.getComponent().isEnabled())
					canImport = false;
				else {
					m_targetIndex = -1;
					canImport = getTargetIndex(support);
					if (canImport) {
						canImport = checkNeighbourhood();
						if (canImport) {
							int sourceActions = support.getSourceDropActions();
							int actionSupported = sourceActions & m_actionAsTarget;
							if (actionSupported != TransferHandler.NONE) {
								if (actionSupported == TransferHandler.COPY_OR_MOVE)
									actionSupported = TransferHandler.COPY;
								support.setDropAction(actionSupported);
							} else
								canImport = false;
						}
					}
				}
			}
		}
		m_app.m_currDnDcanDrop = canImport;
		return canImport;
	}

	protected String carriedValExpr(String carried_val, boolean carriedValDelayed) {
		return (carriedValDelayed ? m_app.m_webClient.getGenToken(0) : carried_val);
	}

	/**
	 * Can return true only in the case no action has taken on the database by
	 * the target side.
	 */
	protected boolean checkExportTransaction() {
		return false;
	}

	void checkForPublishing() {
		if (m_table.m_gridTerm == null) {
			if (m_table.m_panel instanceof DataAccessPanel)
				((DataAccessPanel) m_table.m_panel).checkForPublishing();
		} else
			m_table.m_gridTerm.checkForPublishing();
	}

	protected boolean checkImportTransaction() {
		return false;
	}

	protected boolean checkInternalMove() {
		return m_actor == m_app.m_currDnDjtable;
	}

	protected boolean checkNeighbourhood() {
		return !checkInternalMove();
	}

	protected boolean checkToDelete() {
		return true;
	}

	protected void completeImportTransaction(long dndID, boolean internalMove, boolean delayedDndId) {
		endTransaction();
	}

	protected BasicPostStatement createContextPostStatement() {
		return m_table.m_panel.createContextPostStatement();
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		m_internalMove = false;
		setCurrContext();
		m_app.m_currDnDsourceIsDrainEnabled = m_moveToDrain;
		if (!getAndCheckSourceIndex(c))
			return null;
		prepareTransfer();
		return new JotyStringSelection(String.valueOf(valueToTransfer()));
	}

	protected void endTransaction() {
		try {
			if (m_success)
				m_app.commitTrans();
			else
				m_app.rollbackTrans();
		} catch (JotyException e) {}
	}

	abstract protected boolean exportAction(String carried_val, String identifying_id_val);

	abstract protected boolean importAction(String carried_val, String identifying_id_val, Transferable transferable, boolean carriedValDelayed);

	abstract protected boolean validate(long carriedID, TransferSupport support);

	abstract protected int getRowToBeSelected(long carriedID, Long identifyingID);

	@Override
	protected void exportDone(JComponent source, Transferable transferable, int action) {
		if (source instanceof AnalogicalRowSelector)
			((AnalogicalRowSelector) source).doMouseReleased();
		if (action == MOVE || m_app.m_dragDrainDropped) {
			boolean dataToBeLoaded = true;
			if (m_internalMove)
				dataToBeLoaded = false;
			else {
				boolean transaction = checkExportTransaction();
				if (transaction)
					m_app.beginTrans();
				try {
					m_success = true;
					manageExportActions(transferable, transaction, false);
				} catch (Exception e) {
					Logger.exceptionToHostLog(e);
				} finally {
					if (transaction)
						endTransaction();
				}
			}
			if (dataToBeLoaded) {
				loadData();
				m_table.m_panel.injectedDialog().updateCommandButtons(true);
				if (m_success)
					checkForPublishing();
			}
		}
	}

	protected boolean getAndCheckSourceIndex(JComponent c) {
		m_index = m_actor.getSelectedRow();
		return m_index >= 0 && m_index < m_actor.getRowCount();
	}

	@Override
	public int getSourceActions(JComponent comp) {
		return m_actionAsSource;
	}

	public boolean getSuccess() {
		return m_success;
	}

	protected boolean getTargetIndex(TransferSupport support) {
		boolean retVal = true;
		JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
		if (dl.isInsertColumn())
			retVal = false;
		if (retVal) {
			m_targetIndex = dl.getRow();
			if (m_targetIndex < 0)
				retVal = false;
		}
		return retVal;
	}

	@Override
	public boolean importData(TransferHandler.TransferSupport support) {
		if (!canImport(support))
			return false;
		m_app.m_dragDrainTriggerOn = false;
		m_app.m_DnDdrainIn = false;
		String carried_id = null;
		boolean goOn = false;
		try {
			carried_id = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
			goOn = true;
		} catch (UnsupportedFlavorException e) {} catch (java.io.IOException e) {}
		if (goOn) {
			long dndID = Long.parseLong(carried_id);
			m_internalMove = checkInternalMove();
			goOn = validate(dndID, support);
			if (goOn) {
				boolean transaction = checkImportTransaction();
				try {
					m_success = true;
					if (transaction)
						m_app.beginTrans();
					long identifyingID = targetIdentifyingID(support);
					if (!m_internalMove)
						m_success = importAction(carried_id, String.valueOf(identifyingID), support.getTransferable(), false);
					if (transaction)
						completeImportTransaction(dndID, m_internalMove, false);
					loadData();
					selectContext(dndID, identifyingID);
				} catch (Exception e) {
					Logger.exceptionToHostLog(e);
					m_success = false;
					if (transaction)
						endTransaction();
				}
			}
		}
		boolean retVal = goOn && m_success;
		if (retVal) {
			postImport();
			checkForPublishing();
		} else
			Application.m_common.resetRemoteTransactionBuilding();
		return retVal;
	}

	protected void loadData() {
		if (m_table.m_gridTerm == null)
			((DataAccessPanel) m_table.m_panel).loadData();
		else {
			m_table.m_gridTerm.loadData();
			m_table.m_gridTerm.checkLinkedAspectUpdater();
		}
	}

	protected String managedDbTable() {
		return m_app.codedTabName(m_managedDbTable);
	}

	protected void manageExportActions(Transferable transferable, boolean transaction, boolean foreignCall) throws Exception {
		if (checkToDelete()) {
			String carried_id = (String) transferable.getTransferData(DataFlavor.stringFlavor);
			m_success = exportAction(carried_id, String.valueOf(sourceIdentifyingID()));
		}
	}

	protected void postImport() {}

	protected void prepareTransfer() {}

	protected void selectContext(long dndID, long identifyingID) {
		m_table.setSelection(getRowToBeSelected(dndID, identifyingID));
	}

	protected void setCurrContext() {
		m_app.m_currDnDjtable = m_actor;
	}

	public void setManagedDbTable(String name) {
		m_managedDbTable = name;
	}

	public void setSuccess() {
		m_success = true;
	}

	protected long sourceIdentifyingID() {
		return m_buffer.getKeyLongVal(m_index);
	}

	protected long targetIdentifyingID(TransferSupport support) {
		return m_targetIndex < m_buffer.m_records.size() ? m_buffer.getKeyLongVal(m_targetIndex) : 0;
	}

	protected long valueToTransfer() {
		return sourceIdentifyingID();
	}

}
