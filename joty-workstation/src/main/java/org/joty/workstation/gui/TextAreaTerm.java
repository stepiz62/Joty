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
import javax.swing.border.BevelBorder;

import org.joty.common.JotyTypes;
import org.joty.workstation.gui.TermContainerPanel.TermParams;
/**
 * It is just a Term implementation containing a {@code TextArea} component.
 * @see Term
 * @see TextArea
 * 
 */
public class TextAreaTerm extends Term {
	TextArea m_textArea;

	public TextAreaTerm(TermContainerPanel panel, TermParams params) {
		super(panel, JotyTypes._text, params);
		m_textArea.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
	}

	@Override
	protected void clearComponent() {
		m_textArea.setText("");
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		m_textArea = new TextArea(panel, this);
	}

	@Override
	protected void enableComponent(boolean truth, boolean editability, boolean documentIdentified) {
		truth &= !m_textArea.m_readOnly;
		m_textArea.m_locked = !truth;
		m_textArea.setReadOnly(!(editability || m_mustRemainEnabled) && !isAControlTerm() || !(truth || m_mustRemainEnabled));
	}

	@Override
	public JComponent getComponent() {
		return m_textArea;
	}

	@Override
	public void guiDataExch(boolean in) {
		super.guiDataExch(in);
		if (in) {
			m_strVal = m_textArea.getText();
			setToNull(m_strVal.length() == 0);
		} else
			termRender();
	}

	@Override
	public boolean isWindowVisible() {
		return m_textArea.isVisible();
	}

	@Override
	public void termRender(boolean checkUnselection) {
		m_textArea.render();
	}

}
