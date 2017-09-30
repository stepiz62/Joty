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

import org.joty.workstation.gui.TermContainerPanel.TermParams;
 
/**
 * It is a container for a {@code CheckBox} instance.
 * 
 * @see CheckBox
 * 
 */
public class CheckTerm extends Term {

	public CheckBox m_chk;

	public CheckTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
		m_len = 1;
	}

	@Override
	protected void clearComponent() {
		m_chk.setSelected(false);
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		m_chk = new CheckBox(panel, this);
	}

	@Override
	protected void enable(boolean predicate) {
		m_chk.setEnabled(predicate);
	}

	@Override
	public JComponent getComponent() {
		return m_chk;
	}

	@Override
	protected boolean getSetStatus() {
		return m_chk.getCheck() == 1;
	}

	@Override
	protected long getTermData() {
		return m_chk.getCheck();
	}

	@Override
	protected String getWindowText() {
		return m_chk.getText();
	}

	@Override
	public void guiDataExch(boolean in) {
		super.guiDataExch(in);
		if (in)
			setInteger(m_chk.getCheck());
		else
			termRender();
	}
	
	@Override
	public void termRender(boolean checkUnselection) {
		m_chk.setCheck(isNull() ? 0 : (int) getInteger());
		super.termRender(checkUnselection);
	}

	@Override
	public boolean isWindowEnabled() {
		return m_chk.isEnabled();
	}

	@Override
	public boolean isWindowVisible() {
		return m_chk.isVisible();
	}

	@Override
	public void show(boolean truth) {
		m_chk.setVisible(truth);
	}

	@Override
	public String sqlValueExpr() {
		return String.valueOf(m_chk.getCheck());
	}

}
