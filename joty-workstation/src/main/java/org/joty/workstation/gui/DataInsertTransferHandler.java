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

import javax.swing.DropMode;

import org.joty.workstation.app.Application;

/**
 * Sets the javax.swing.DropMode to 'INSERT' and composes the proper Sql
 * statement depending on the side which the instance is located on.
 * <p>
 * The class implements the validation basing on the exclusive presence of the
 * id value in the (target) Table object but the case in which the Table object
 * is also the source.
 * 
 */
public class DataInsertTransferHandler extends JotyTableTransferHandler {

	public DataInsertTransferHandler(int actionAsSource, int actionAsTarget, Table table, boolean moveToDrain) {
		super(actionAsSource, actionAsTarget, table, moveToDrain);
		m_actor.setDropMode(DropMode.INSERT);
	}

	@Override
	protected boolean exportAction(String carried_val, String identifying_id_val) {
		return m_app.executeSQL("Delete from " + managedDbTable() + " Where " + m_id_dbField + " = " + carried_val, null, createContextPostStatement());
	}

	@Override
	protected int getRowToBeSelected(long carriedID, Long identifyingID) {
		Integer rowToBeSelected = m_buffer.getKeyPos(carriedID);
		return rowToBeSelected == null ? -1 : rowToBeSelected;
	}

	@Override
	protected boolean importAction(String carried_val, String identifying_id_val, Transferable transferable, boolean carriedValDelayed) {
		return m_app.executeSQL(String.format("Insert into " + managedDbTable() + 
													" (" + m_id_dbField + "%s) values(" + carriedValExpr(carried_val, carriedValDelayed) + "%s)", 
												m_app.m_common.m_shared ? ", sharingKey" : "", 
												m_app.m_common.m_shared ? (", '" + m_app.m_common.m_sharingKey + "'") : ""), 
												null, 
												createContextPostStatement());
	}

	@Override
	protected boolean validate(long carriedID, TransferSupport support) {
		boolean retVal = true;
		if (m_buffer.getKeyPos(carriedID) != null && !m_internalMove) {
			Application.langWarningMsg("TermAlreadyPresent");
			retVal = false;
		}
		return retVal;
	}
}
