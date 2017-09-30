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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.joty.workstation.gui.TextEditManager;
import org.joty.workstation.gui.Term.TermEnclosable;

/**
 * It does not inherit from javax.swing.JTextArea but it is a
 * org.joty.workstation.gui.ScrollPane implementation that support the scrolling of an
 * instance of it.
 * <p>
 * More in details, it instantiates a {@code JotyJTextArea} object, from an
 * internal definition that extends the javax.swing.JTextArea, and directs to it
 * all the commands coming from the container {@code Term} instance.
 * <p>
 * It instantiates a {@code TextEditManager} as helper in decoding the typed
 * keys.
 * 
 * @see TextEditManager
 * @see JotyJTextArea
 * @see TextAreaTerm
 * 
 */
public class TextArea extends ScrollPane implements TermEnclosable {

	public class JotyJTextArea extends JTextArea implements KeyListener {
		private TextArea m_textArea;

		JotyJTextArea(TextArea textArea) {
			m_textArea = textArea;
			addKeyListener(this);
			setLineWrap(true);
			setWrapStyleWord(true);
			setFont(((Font) UIManager.get("JTextArea.font")));
		}

		public TextArea getTextArea() {
			return m_textArea;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			m_textManager.getDotPos();
			if (m_textManager.isAmodifyingKey(e)) {
				m_term.notifyEditingAction(null);
				m_panel.notifyEditingAction(null);
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			m_textManager.checkSize();
		}

		@Override
		public void keyTyped(KeyEvent e) {}

		@Override
		public void setEditable(boolean b) {
			if (m_textManager != null && m_textManager.m_undoManager != null)
				m_textManager.m_undoManager.discardAllEdits();
			setForeground(b ? m_enableForeColor : m_disableForeColor);
			setBackground(b ? m_whiteColor : m_lightGreyColor);
			super.setEditable(b);
		}

	}

	protected Term m_term;
	protected TermContainerPanel m_panel;
	public boolean m_readOnly;
	public boolean m_locked;
	private Color m_disableForeColor;
	private Color m_enableForeColor;
	protected Color m_whiteColor;
	protected Color m_lightGreyColor;
	private TextEditManager m_textManager;

	public JotyJTextArea m_jtextArea;

	public TextArea(TermContainerPanel panel, Term term) {
		m_panel = panel;
		m_term = term;
		m_jtextArea = new JotyJTextArea(this);
		setViewportView(m_jtextArea);
		m_textManager = new TextEditManager(m_jtextArea, term);
		getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), new Object());
		m_disableForeColor = new java.awt.Color(128, 128, 128);
		m_enableForeColor = new java.awt.Color(0, 0, 0);
		m_whiteColor = new Color(255, 255, 255);
		m_lightGreyColor = new Color(212, 208, 196);
		setDropTarget(null);
	}

	@Override
	public boolean getRelatedEnable() {
		return true;
	}

	@Override
	public Term getTerm() {
		return m_term;
	}

	public String getText() {
		return m_jtextArea.getText();
	}

	public void render() {
		m_jtextArea.setText(m_term.m_strVal);
		m_jtextArea.setSelectionStart(0);
		m_jtextArea.setSelectionEnd(0);
	}

	public void setReadOnly(boolean b) {
		m_jtextArea.setEditable(!b);
	}

	@Override
	public void setSize(int arg0, int arg1) {
		super.setSize(80, 40);
	}

	public void setText(String text) {
		m_jtextArea.setText(text);
	}

}
