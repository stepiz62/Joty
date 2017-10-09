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

import javax.swing.TransferHandler.TransferSupport;

import org.joty.workstation.app.Application;

/**
 * Provides the sql update statement to be executed either on the 'target record
 * of the drop action' or on the 'source record of the start drag action'.
 * <p>
 * If the instance is directed by a delegator the {@code getTargetIdVal}
 * override gets the identifying id from it.
 * 
 * @see DataOnTransferHandler
 * 
 */
public class DataUpdateTransferHandler extends DataOnTransferHandler {

	public DataUpdateTransferHandler(int actionAsSource, int actionAsTarget, Table table, boolean moveToDrain) {
		super(actionAsSource, actionAsTarget, table, moveToDrain);
	}

	public DataUpdateTransferHandler(int actionAsSource, int actionAsTarget, Table table, boolean moveToDrain, String targetIdField) {
		super(actionAsSource, actionAsTarget, table, moveToDrain, targetIdField, true);
	}

	@Override
	protected boolean exportOnAction(String carried_val, String identifying_id_val) {
		return updateAction(carried_val, identifying_id_val);
	}

	@Override
	protected int getRowToBeSelected(long carriedID, Long identifyingID) {
		return m_buffer.getKeyPos(identifyingID);
	}

	@Override
	final public long getTargetIdVal(TransferSupport support) {
		if (delegatorTransferHandlerIsActing(support))
			return m_delegatorTransferHandler.getTargetIdVal(support);
		else
			return m_buffer.integerValue(m_target_id_field, m_targetIndex);
	}

	@Override
	protected boolean importOnAction(String carried_val, String identifying_id_val, Transferable transferable, boolean carriedValDelayed) {
		return m_app.executeSQL(updateStatement(identifying_id_val, carriedValExpr(carried_val, carriedValDelayed)), 
											null, 
											createContextPostStatement());
	}

	protected boolean updateAction(String carried_val, String identifying_id_val) {
		return m_app.executeSQL(updateStatement(identifying_id_val, "NULL"), null, createContextPostStatement());
	}

	private String updateStatement(String identifying_id_val, String Value) {
		return "Update " + managedDbTable() + " set " + m_target_id_field + " = " + Value + 
				" where " + m_id_dbField + " = " + identifying_id_val;
	}


	@Override
	protected long valueToTransfer() {
		return m_target_id_field == null ? 0 : m_buffer.integerValue(m_target_id_field, m_index);
	}


}
