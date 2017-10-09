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

import javax.swing.JRadioButton;

import org.joty.workstation.app.Application;
import org.joty.workstation.gui.Term.TermEnclosable;

/**
 * It extends the javax.swing.JRadioButton class with the Joty abilities, to
 * behave with and to communicate to the framework.
 * 
 * @see RadioTerm
 * @see MasterRadioTerm
 *
 */
public class RadioButton extends JRadioButton implements ActionListener, TermEnclosable {

	public TermContainerPanel m_panel;
	public RadioTerm m_term;
	public boolean m_value;

	RadioButton() {}

	public RadioButton(TermContainerPanel panel, Term term) {
		this();
		m_panel = panel;
		m_term = (RadioTerm) term;
		addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent actionevent) {
		m_value = isSelected();
		m_term.notifyEditingAction(actionevent);
		m_term.guiDataExch(true);
		m_panel.notifyEditingAction(actionevent);
		m_panel.onRadiosChange(this);
	}

	public boolean getCheck() {
		return isSelected();
	}

	@Override
	public boolean getRelatedEnable() {
		return true;
	}

	@Override
	public Term getTerm() {
		return m_term;
	}

	public void setCheck(boolean truth) {
		setSelected(truth);
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
