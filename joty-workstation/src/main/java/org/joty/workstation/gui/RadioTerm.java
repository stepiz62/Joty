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

import java.awt.event.ActionEvent;

import javax.swing.JComponent;

import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * It is a container for a {@code RadioButton} instance. 
 * <p>
 * The {@code m_master} member holds a reference to the Term instance that manages the RadioButton
 * having the role of data holder.
 * 
 * @see RadioButton
 * @see MasterRadioTerm
 * 
 */
public class RadioTerm extends Term {

	/**
	 * Set by {@code TermContainerPanel.setRadioAsActor} method this member holds the
	 * ability of the term to manage the enabling state or to clear the value of
	 * the set of components managed by Term instances that result to be
	 * dependent on this instance.
	 * 
	 * @see Term#m_dependenceDirectness
	 * @see TermContainerPanel#setDependence
	 * @see RadioButton
	 */
	
	public boolean m_isActor = false;
	public MasterRadioTerm m_master;
	RadioButton m_btn;
	public int m_storedValue = 0;

	public RadioTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		m_btn = new RadioButton(panel, this);
	}

	@Override
	public JComponent getComponent() {
		return m_btn;
	}

	@Override
	protected boolean getSetStatus() {
		return m_btn.getCheck();
	}

	@Override
	protected String getWindowText() {
		return m_btn.getText();
	}


	@Override
	public boolean isWindowVisible() {
		return m_btn.isVisible();
	}

	@Override
	public void notifyEditingAction(ActionEvent e) {
		super.notifyEditingAction(e);
		if (m_master != null)
			m_master.notifyEditingAction(e);
	}

}
