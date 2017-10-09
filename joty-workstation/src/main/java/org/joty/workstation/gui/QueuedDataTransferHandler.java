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

import java.awt.datatransfer.Transferable;

/**
 * Allows the instance to work for a Table in which data queuing is active, so
 * that, the queue modification is properly performed in all circumstances of a
 * D&D action, including the internal movement of a row.
 * <p>
 * Allows the transaction to occur by care of the instance even when it works on
 * the source side and the move is external: by default, it is assumed, in this
 * scenario, that on the target side no action is made on the database.
 * 
 * @see org.joty.workstation.data.JotyDataBuffer.QueueManager
 */
public class QueuedDataTransferHandler extends DataInsertTransferHandler {

	public QueuedDataTransferHandler(int actionAsSource, int actionAsTarget, Table table, boolean moveToDrain) {
		super(actionAsSource, actionAsTarget, table, moveToDrain);
		table.addToolTipRow(m_app.m_common.jotyLang("DragUpAndDown"));
		m_showDropLocation = true;
	}

	/** Must be overridden to return true in all cases some database action is taken on the database by the target side. */
	@Override
	protected boolean checkExportTransaction() {
		return true;
	}

	@Override
	protected boolean checkImportTransaction() {
		return true;
	}

	@Override
	protected boolean checkNeighbourhood() {
		boolean retVal = true;
		boolean internalMove = checkInternalMove();
		if (internalMove && m_buffer.m_queueManager == null)
			retVal = false;
		if (retVal && internalMove && (m_index == m_targetIndex || m_index == m_targetIndex - 1))
			retVal = false;
		return retVal;
	}

	@Override
	protected boolean checkToDelete() {
		return !m_internalMove;
	}

	@Override
	protected void completeImportTransaction(long dndID, boolean internalMove, boolean delayedDndId) {
		if (m_success)
			m_success = m_buffer.m_queueManager.manageQueueOnDbTable(dndID, m_targetIndex, internalMove, delayedDndId);
		super.completeImportTransaction(dndID, internalMove, delayedDndId);;
	}

	@Override
	protected int getRowToBeSelected(long carriedID, Long identifyingID) {
		Integer retVal = m_buffer.getKeyPos(carriedID);
		return retVal == null ? -1 : m_buffer.m_queueManager.getReverseMappedRow(retVal);
	}

	@Override
	public void manageExportActions(Transferable transferable, boolean transaction, boolean foreignCall) throws Exception {
		super.manageExportActions(transferable, transaction, foreignCall);
		if (m_success && transaction)
			m_success = m_buffer.m_queueManager.manageRemoval();
	}

	@Override
	protected void prepareTransfer() {
		m_buffer.m_queueManager.prepareTransaction(m_index);
	}

	@Override
	public void setManagedDbTable(String name) {
		super.setManagedDbTable(name);
		m_buffer.m_queueManager.setMainMetadata(m_managedDbTable, m_id_dbField);
	}

	@Override
	protected long valueToTransfer() {
		return m_buffer.getKeyLongVal(m_buffer.m_queueManager.getMappedRow(m_index));
	}
}
