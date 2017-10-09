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

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.TransferHandler;

import org.joty.workstation.app.Application;

/**
 * It gives AnalogicalSelector the responsibility to be an alternative way for
 * the selection of a row (to which it is associated) of the main grid located
 * in the containing {@code Panel} instance. Further more it gives the visual
 * feedback for the selection of the grid row.
 * <p>
 * Its constructor notifies the {@code GridManager} instance with its own
 * existence and its identifier value enriching the data structures there
 * existing and dedicated to collect the entire set of AnalogicalRowSelector
 * objects: there must be one and only one instance of this class for each row
 * in the main grid.
 * <p>
 * Optionally it can behave as a Drag and Drop source and target, by delegating
 * for this, an instance of the {@code DataTransferHandler} in advance created
 * and made available as member in the containing {@code Panel} instance. for
 * serving all the the instances of this class.
 * 
 * @see AnalogicalSelector
 * @see JotyTableTransferHandler
 * @see Panel#setSelectorsTransferHandler(JotyTableTransferHandler)
 * @see Panel#m_selectorsTransferHandler
 * 
 */
public class AnalogicalRowSelector extends AnalogicalSelector {

	public GridManager m_gridManager;
	private JotyTableTransferHandler m_transferHandler;

	public AnalogicalRowSelector(GridManager gridManager, Long keyValue) {
		this(gridManager, keyValue, false);
	}

	public AnalogicalRowSelector(GridManager gridManager, Long keyValue, boolean dndFeatures) {
		super(keyValue);
		m_gridManager = gridManager;
		if (gridManager.m_selectorHeavyStates == null)
			gridManager.m_selectorHeavyStates = new Vector<Boolean>();
		if (gridManager.m_selectorMap == null)
			gridManager.m_selectorMap = new HashMap<Long, AnalogicalRowSelector>();
		if (gridManager.m_selectorMap.put(keyValue, this) == null)
			gridManager.m_selectorHeavyStates.add(new Boolean(false));
		else
			Application.m_app.JotyMsg(this, "the keyValue " + String.valueOf(keyValue) + " was already specified by another selector !");
		if (dndFeatures) {
			m_transferHandler = m_gridManager.m_listComponent.m_panel.getSelectorsTransferHandler();
			if (m_transferHandler == null)
				Application.m_app.JotyMsg(this, "The container setSelectorsTransferHandler method must be called before the instantiation of any AnalogicalRowSelector," +
											" with DnD features enabled, could take place!");
			else
				setTransferHandler(m_transferHandler);
		}
	}

	@Override
	public void doMouseReleased() {
		if (isInside())
			m_gridManager.manageAnalogSelection(m_keyValue, false);
		super.doMouseReleased();
	}

	@Override
	public boolean isEnabled() {
		return !m_gridManager.m_termContainerPanel.injectedDialog().isEditing();
	}

	@Override
	protected void manageMousePressed(MouseEvent e) {
		if (m_transferHandler != null) {
			AnalogicalRowSelector selector = (AnalogicalRowSelector) e.getSource();
			TransferHandler handler = selector.getTransferHandler();
			handler.exportAsDrag(selector, e, TransferHandler.COPY);
		}
	}

}
