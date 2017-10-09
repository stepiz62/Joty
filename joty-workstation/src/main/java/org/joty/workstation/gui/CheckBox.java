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
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import org.joty.workstation.app.Application;
import org.joty.workstation.gui.Term.TermEnclosable;

/**
 * It extends the javax.swing.JCheckBox class with the Joty abilities, to
 * behave with and to communicate to the framework.
 * 
 * @see CheckTerm
 *
 */
public class CheckBox extends JCheckBox implements ActionListener, TermEnclosable {

	public TermContainerPanel m_panel;
	public CheckTerm m_term;

	public CheckBox(TermContainerPanel panel, CheckTerm term) {
		addActionListener(this);
		m_panel = panel;
		m_term = term;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		m_term.notifyEditingAction(e);
		m_panel.notifyEditingAction(e);
		m_term.setToNull(false);
		m_term.guiDataExch(true);
	}

	public int getCheck() {
		return getSelectedObjects() == null ? 0 : 1;
	}

	@Override
	public boolean getRelatedEnable() {
		return true;
	}

	@Override
	public Term getTerm() {
		return m_term;
	}

	public void setCheck(int val) {
		setSelected(val != 0);
	}

	@Override
	public void setSize(int arg0, int arg1) {
		super.setSize(80, 21);
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		setToolTipText(text);
	}

	@Override
	public void setToolTipText(String text) {
		super.setToolTipText(Application.m_app.m_toolTipsEnabled() ? text : null);
	}
}
