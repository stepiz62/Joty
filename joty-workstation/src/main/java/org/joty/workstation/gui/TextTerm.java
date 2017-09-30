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
import javax.swing.text.MaskFormatter;

import org.joty.common.JotyTypes;
import org.joty.workstation.app.Application;
import org.joty.data.JotyDate;
import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * The class manages an instance of the {@code JotyTextField} class, embracing
 * its whole polymorphic, data type dependent, behavior.
 * <p>
 * Currently is the only Term implementation that the enables its component, on loosing focus, to
 * update a field of the current record of a 'buffer equipped' term.
 * 
 * @see Term
 * @see TextArea
 * @see Term#m_drivenBufferTerm
 * @see TermContainerPanel#setTermAsDriverOf
 * 
 */

public class TextTerm extends Term {

	JotyTextField m_edit;

	public TextTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
	}

	@Override
	protected void clearComponent() {
		switch (m_dataType) {
			case JotyTypes._text:
				m_edit.setText("");
				break;
			case JotyTypes._long:
			case JotyTypes._int:
			case JotyTypes._double:
			case JotyTypes._single:
			case JotyTypes._dbDrivenInteger:
				m_edit.setText("");
				break;
			case JotyTypes._date:
				m_edit.setText(m_app.m_common.emptyDateRendering(false));
				break;
			case JotyTypes._dateTime:
				m_edit.setText(m_app.m_common.emptyDateRendering(true));
				break;
		}
	}

	@Override
	void clearNonStructuredCtrl() {
		clearComponent();
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		MaskFormatter maskFormatter = Application.m_app.createFormatter(m_dataType, m_isCurrency, m_len);
		m_edit = new JotyTextField(panel, this, maskFormatter);
	}

	@Override
	protected boolean doValidate() {
		boolean success = m_edit.doValidate();
		if (success) {
			switch (m_dataType) {
				case JotyTypes._date:
				case JotyTypes._dateTime:
				case JotyTypes._text:
				case JotyTypes._long:
				case JotyTypes._int:
				case JotyTypes._double:
				case JotyTypes._single:
				case JotyTypes._dbDrivenInteger:
					if (m_required) {
						if (isNull()) {
							alert("MustNotBeEmpty");
							success = false;
						}
					}
					break;
			}
		}
		return success;
	}

	@Override
	protected void enable(boolean predicate) {
		m_edit.setEnabled(predicate);
	}

	@Override
	public void enableComponent(boolean truth, boolean editability, boolean documentIdentified) {
		if (!truth)
			m_edit.m_dirty = false;
		truth &= !m_edit.m_readOnly;
		boolean BufferConnectionMissing = false;
		if (m_drivenBufferTerm != null)
			BufferConnectionMissing = m_drivenBufferTerm.getSelection() < 0 && m_drivenBufferTerm.m_dataBuffer != null && m_drivenBufferTerm.m_dataBuffer.m_cursorPos < 0;
		m_edit.setReadOnly(!(editability || m_mustRemainEnabled) && !isAControlTerm() || !(truth || m_mustRemainEnabled) || BufferConnectionMissing && editability);
	}

	@Override
	public JComponent getComponent() {
		return m_edit;
	}

	@Override
	protected String getWindowText() {
		return m_edit.getText();
	}

	@Override
	public void guiDataExch(boolean in) {
		super.guiDataExch(in);
		if (in)
			setData(m_edit.getText(), m_edit.m_numberFormat);
		else
			termRender();
	}

	@Override
	public boolean isWindowEnabled() {
		return m_edit.isEnabled();
	}

	@Override
	public boolean isWindowVisible() {
		return m_edit.isVisible();
	}

	public boolean isZero() {
		return m_dataType == JotyTypes._dbDrivenInteger && integerVal() == 0 || 
				m_dataType == JotyTypes._long && m_lVal == 0 || 
				m_dataType == JotyTypes._int && m_iVal == 0 || 
				m_dataType == JotyTypes._double && m_dblVal == 0 || 
				m_dataType == JotyTypes._single && m_fltVal == 0;
	}

	@Override
	public void killFocus() {
		super.killFocus();
		updateDrivenBuffer();
		if (m_panel != null)
			m_panel.componentsKillFocus(this);
	}

	@Override
	public void show(boolean truth) {
		m_edit.setVisible(truth);
	}

	@Override
	public void termRender(boolean checkUnselection) {
		m_edit.render();
	}

	@Override
	public String toString() {
		return m_edit.getText().trim();
	}

	public void setToNow() {
		m_dateVal.setDate(new JotyDate(m_app, "now"));
        setToNull(false);
	}


}
