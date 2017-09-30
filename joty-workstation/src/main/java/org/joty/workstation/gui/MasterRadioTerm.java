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

import java.util.Vector;

import javax.swing.ButtonGroup;

import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * This class extends its ancestor with the ability to hold the catalog of
 * collaborating RadioButton objects (the 'group') in defining the current selection, that is
 * the integer value associated to the database field.
 * 
 * @see RadioButton
 * 
 */
public class MasterRadioTerm extends RadioTerm {

	Vector<RadioButton> m_buttonArray;
	ButtonGroup m_group;
	public int m_maxSiblingsValue = 0;
	   
	public MasterRadioTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
		m_buttonArray = new Vector<RadioButton>();
		addRadioToGrp();
	}

	void addRadioToGrp() {
		addRadioToGrp(null);
	}

	void addRadioToGrp(String termName) {
		RadioTerm radioTerm = (RadioTerm) (termName == null ? this : m_panel.term(termName));
		if (termName != null)
			radioTerm.m_storedValue = ++m_maxSiblingsValue;
		RadioButton ctrl = radioTerm.m_btn;
		m_group.add(ctrl);
		ctrl.addActionListener(m_panel);
		m_buttonArray.add(ctrl);
		radioTerm.m_master = radioTerm == this ? null : this;
	}

	@Override
	public void clear() {
		setInteger(-1);
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		super.createComponent(panel);
		m_group = new ButtonGroup();
	}

	@Override
	protected boolean doValidate() {
		boolean success = true;
		if (m_required && m_iVal == -1) {
			alert("MustBeSelectedOne");
			success = false;
		}
		return success;
	}

	@Override
	protected void enable(boolean predicate) {
		m_buttonArray.get(0).setEnabled(predicate);
	}

	@Override
	public void enableComponent(boolean enablCtx, boolean bState, boolean docIdentified, boolean basicallyEditable, boolean editability) {
		for (RadioButton btn : m_buttonArray)
			btn.setEnabled(enablCtx && bState);
	}

	@Override
	public void guiDataExch(boolean in) {
		super.guiDataExch(in);
		if (! in) {
			m_group.clearSelection();
			if (!isNull()) {
				int value = (int) getInteger();
				if (value >= 0)
					m_buttonArray.get(value).setCheck(true);
			}
		}
	}

	@Override
	public boolean isWindowEnabled() {
		return m_buttonArray.get(0).isEnabled();
	}

	@Override
	public boolean isWindowVisible() {
		return m_buttonArray.get(0).isVisible();
	}

	public void onRadiosChange() {
		RadioTerm radioTerm;
		for (RadioButton btn : m_buttonArray) {
			radioTerm = btn.m_term;
			if (radioTerm.m_isActor) {
				radioTerm.enablingDependentComponents();
				if (!radioTerm.m_btn.getCheck() && radioTerm.m_dependenceDirectness || radioTerm.m_btn.getCheck() && !radioTerm.m_dependenceDirectness)
					radioTerm.clearDependentComponents();
			}
		}
	}
 
	@Override
	public void show(boolean truth) {
		for (int i = 0; i < m_buttonArray.size(); i++)
			m_buttonArray.get(i).setVisible(truth);
	}

}
