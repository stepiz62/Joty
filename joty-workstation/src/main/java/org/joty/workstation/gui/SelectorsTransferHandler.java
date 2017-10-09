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
import javax.swing.TransferHandler;

/**
 * It is associated to an {@code AnalogicalRowSelector} object and implements
 * the delegation of effects of all the D&D actions involving it (either as
 * source or as target) to the {@code DataUpdateTransferHandler} object passed
 * to its constructor.
 * <p>
 * It makes use of the redirection capability of its ancestor class and
 * furthermore provides, by the {@code targetIdentifyingID} override, the
 * identity id hosted by the component which is associated to, so that the
 * delegated TransferHandler can identify the row of the {@code Table} to which
 * is associated.
 * 
 * see AnalogicalRowSelector
 * 
 */

public class SelectorsTransferHandler extends DataOnTransferHandler {

	GridManager m_gridManager;

	public SelectorsTransferHandler(DataUpdateTransferHandler delegatedTransferHandler) {
		super(TransferHandler.COPY_OR_MOVE, 
				delegatedTransferHandler.m_actionAsTarget, 
				delegatedTransferHandler.m_table, 
				delegatedTransferHandler.m_moveToDrain, 
				delegatedTransferHandler.m_target_id_field, 
				false);
		m_gridManager = delegatedTransferHandler.m_table.m_panel.m_gridManager;
		m_targetIdStock = delegatedTransferHandler.m_targetIdStock;
		m_exclusiveTransfer = delegatedTransferHandler.m_exclusiveTransfer;
		m_delegatedTransferHandler = delegatedTransferHandler;
		delegatedTransferHandler.m_delegatorTransferHandler = this;
	}

	@Override
	protected boolean getAndCheckSourceIndex(JComponent c) {
		m_index = m_buffer.getKeyPos(((AnalogicalRowSelector) c).m_keyValue);
		return true;
	}

	@Override
	protected boolean getTargetIndex(TransferSupport support) {
		m_targetIndex = m_buffer.getKeyPos(((AnalogicalRowSelector) support.getComponent()).m_keyValue);
		return true;
	}

	@Override
	public long getTargetIdVal(TransferSupport support) {
		return m_buffer.integerValue(m_target_id_field, m_buffer.getKeyPos(targetIdentifyingID(support)));
	}

	@Override
	protected void postInit() {}

	@Override
	protected void selectContext(long dndID, long identifyingID) {
		m_gridManager.setCurSel(getRowToBeSelected(dndID, identifyingID));
	}

	@Override
	protected long targetIdentifyingID(TransferSupport support) {
		return ((AnalogicalRowSelector) support.getComponent()).m_keyValue;
	}

}
