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
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.joty.workstation.gui.Term.TermEnclosable;

/**
 * Simply inherits from javax.swing.JScrollPane to set scroll bars availability
 * and to prepare the enabling features of the entire object.
 * 
 */
public class ScrollPane extends JScrollPane implements TermEnclosable {

	public Panel m_panel;
	public Term m_term;

	public ScrollPane() { // for use with WBE only
	}

	public ScrollPane(Panel panel, Term term) {
		m_panel = panel;
		m_term = term;
		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	public JComponent getPaneComponent() {
		return null;
	}

	@Override
	public boolean getRelatedEnable() {
		return false;
	}

	@Override
	public Term getTerm() {
		return m_term;
	}

	public void init() {}

	protected boolean manageBarEnable() {
		return false;
	}

	@Override
	public void setEnabled(boolean truth) {
		super.setEnabled(truth);
		setInnerComponentEnabled(truth);
		if (manageBarEnable())
			getVerticalScrollBar().setEnabled(truth);
	}

	public void setFormat() {}

	protected void setInnerComponentEnabled(boolean truth) {
		getPaneComponent().setEnabled(truth);
	}

}
