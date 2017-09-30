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
import java.beans.Beans;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.TransferHandler.TransferSupport;

import org.joty.workstation.app.Application;
import org.joty.workstation.data.JotyDataBuffer.IdsStock;

/**
 * Sets the javax.swing.DropMode to 'ON' and adds a constructor to let
 * specifying the database field that, in the record of the target Table will
 * receive the value transferred. Then the uniqueness of the value of this field
 * can be imposed by means of a further parameter in the constructor.
 * <p>
 * Defines an abstract method that promises to pick up the value of the target
 * id field of the row upon which the drop action occurred.
 * <p>
 * Implements the
 * {@code exportAction, importAction, getRowToBeSelected, validate} methods
 * using the {@code m_delegatedTransferHandler} attribute as switcher for the
 * redirection of the responsibility: the delegation chance appears in this
 * class because, only considering a specific target row it seems reasonable to
 * have another component with its own TransferHandler that works as the one
 * associated to the target {@code Table} object.
 * 
 * @see #delegatorTransferHandlerIsActing(javax.swing.TransferHandler.TransferSupport)
 * 
 */

public abstract class DataOnTransferHandler extends JotyTableTransferHandler {

	protected String m_target_id_field;
	protected boolean m_exclusiveTransfer;
	protected IdsStock m_targetIdStock;
	protected DataOnTransferHandler m_delegatedTransferHandler, m_delegatorTransferHandler;

	public DataOnTransferHandler(int actionAsSource, int actionAsTarget, Table table, boolean moveToDrain) {
		super(actionAsSource, actionAsTarget, table, moveToDrain);
		m_actor.setDropMode(DropMode.ON);
		m_showDropLocation = true;
	}

	public DataOnTransferHandler(int actionAsSource, int actionAsTarget, Table table, boolean moveToDrain, String targetIdField, boolean exclusiveTransfer) {
		this(actionAsSource, actionAsTarget, table, moveToDrain);
		m_target_id_field = targetIdField;
		m_exclusiveTransfer = exclusiveTransfer;
		if (exclusiveTransfer && !Beans.isDesignTime()) {
			m_targetIdStock = m_buffer.new IdsStock();
			m_buffer.m_idsStocksMap.put(targetIdField, m_targetIdStock);
		}
	}

	abstract public long getTargetIdVal(TransferSupport support);

	protected boolean exportOnAction(String carried_val, String identifying_id_val) {
		return true;
	} 
	
	protected boolean importOnAction(String carried_val, String identifying_id_val, Transferable transferable, boolean carriedValDelayed) {
		return true;
	} 

	@Override
	protected int getRowToBeSelected(long carriedID, Long identifyingID) {
		if (m_delegatedTransferHandler == null)
			return 0;
		else
			return m_delegatedTransferHandler.getRowToBeSelected(carriedID, identifyingID);
	}

	@Override
	protected boolean exportAction(String carried_val, String identifying_id_val) {
		if (m_delegatedTransferHandler == null) {
			boolean retVal = exportOnAction(carried_val, identifying_id_val);
			if (retVal && m_exclusiveTransfer)
				m_targetIdStock.remove(Long.parseLong(carried_val));
			return retVal;
			}
		else
			return m_delegatedTransferHandler.exportAction(carried_val, identifying_id_val);
	}

	@Override
	protected boolean importAction(String carried_val, String identifying_id_val, Transferable transferable, boolean carriedValDelayed) {
		if (m_delegatedTransferHandler == null) {
			boolean retVal = importOnAction(carried_val, identifying_id_val, transferable, carriedValDelayed);
			if (retVal && m_exclusiveTransfer)
				m_targetIdStock.put(Long.parseLong(carried_val));
			return retVal;
			}
		else
			return m_delegatedTransferHandler.importAction(carried_val, identifying_id_val, transferable, carriedValDelayed);
	}

	@Override
	protected boolean validate(long dndID, TransferSupport support) {
		if (m_delegatedTransferHandler == null) {
			boolean retVal = true;
			long targetVal = getTargetIdVal(support);
			if (targetVal != 0 && !m_internalMove) {
				Application.langWarningMsg("TermAlreadyDefined");
				retVal = false;
			}
			if (retVal && m_exclusiveTransfer) {
				if (m_targetIdStock.isPresent(dndID)) {
					Application.langWarningMsg("TermAlreadyAssigned");
					retVal = false;
				}
			}
			return retVal;
		} else
			return m_delegatedTransferHandler.validate(dndID, support);
	}

	@Override
	protected long valueToTransfer() {
		if (m_delegatedTransferHandler == null)
			return super.valueToTransfer();
		else
			return m_delegatedTransferHandler.valueToTransfer();
	}

	/**
	 * Returns true if the TransferHandler object associated to the target
	 * component is a delegator JotyTableTransferHandler instance in respect of
	 * the current instance. Is is used by the children classes that are
	 * interested by delegation scenarios.
	 * 
	 * @param support
	 *            the TransferSupport instance provided, useful to identify the
	 *            target context of the "Drag and Drop" action;
	 */
	protected boolean delegatorTransferHandlerIsActing(TransferSupport support) {
		return m_delegatorTransferHandler != null && ((JComponent) support.getComponent()).getTransferHandler() == m_delegatorTransferHandler;
	}

}