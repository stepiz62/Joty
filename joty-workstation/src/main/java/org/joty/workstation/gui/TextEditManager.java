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

import java.awt.event.KeyEvent;

import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

import org.joty.workstation.app.Application;
import org.joty.workstation.gui.Term.CcpCommand;
import org.joty.workstation.gui.TextArea.JotyJTextArea;

/**
 * It is an helper class for the {@code JotyTextField} and the {@code TextArea}
 * classes. Manages the the usual copy-cut-paste actions and making them
 * possible inside the specific behavior of the two client classes, and further
 * more it helps in elaborating other typical key events moving the insertion point.
 * <p>
 * It instantiates and install an instance of the Java Swing {@code UndoManager}
 * class in the TextComponent injected into the constructor, to provide
 * it with undo-redo behavior.
 * <p>
 * Then it offers the two client classes its methods, the most important of which
 * is {@code isAmodifyingKey} that stores internally the tasks associated with the
 * character currently analyzed .
 * 
 * @see JotyTextField
 * @see TextArea
 */
public class TextEditManager {
	public int m_selStart;
	public int m_selEnd;
	public UndoManager m_undoManager;
	public boolean m_undoRedoAction;
	public boolean m_concreteChar;
	public CcpCommand m_ccpCommand;
	JTextComponent m_textComponent;
	private boolean m_isTextArea;
	private int m_oldDot;
	private Term m_term;

	TextEditManager(JTextComponent textComponent, Term term) {
		m_textComponent = textComponent;
		m_term = term;
		m_isTextArea = m_textComponent instanceof JotyJTextArea;
		m_undoManager = new UndoManager();
		m_textComponent.getDocument().addUndoableEditListener(m_undoManager);
	}

	public void checkSize() {
		boolean save = (m_isTextArea ? m_textComponent.getText() : m_textComponent.getText().trim()).length() <= m_term.m_len;
		m_term.guiDataExch(save);
		if (!save) {
			m_textComponent.getCaret().setDot(m_oldDot);
			Application.langWarningMsg("AtMostXChars", new Object[] { new Integer(m_term.m_len) });
		}

	}

	public void getDotPos() {
		m_oldDot = m_textComponent.getCaret().getDot();
	}

	public boolean isAmodifyingKey(KeyEvent e) {
		boolean retVal = false;
		m_undoRedoAction = false;
		m_ccpCommand = CcpCommand.CCP_NONE;
		if ((e.getModifiersEx() & m_term.commandDownMask()) > 0) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_C:
					m_ccpCommand = CcpCommand.CCP_COPY;
					break;
				case KeyEvent.VK_X:
					m_ccpCommand = CcpCommand.CCP_CUT;
					break;
				case KeyEvent.VK_V:
					m_ccpCommand = CcpCommand.CCP_PASTE;
					break;
				case KeyEvent.VK_A:
					m_ccpCommand = CcpCommand.CCP_SELECTALL;
					break;
				case KeyEvent.VK_Z:
					for (int i = 0; i < 2; i++)
						if (m_undoManager.canUndo())
							m_undoManager.undo();
					m_undoRedoAction = true;
					break;
				case KeyEvent.VK_Y:
					for (int i = 0; i < 2; i++)
						if (m_undoManager.canRedo())
							m_undoManager.redo();
					m_undoRedoAction = true;
					break;
			}
		}
		retVal = Character.getType(e.getKeyChar()) != 0;
		int code = e.getKeyCode();
		m_selStart = m_textComponent.getSelectionStart();
		m_selEnd = m_textComponent.getSelectionEnd();
		String content = m_textComponent.getText();
		if (!m_isTextArea)
			content = content.trim();
		retVal &= ! ( 	code == KeyEvent.VK_BACK_SPACE && m_selEnd == 0 || 
						code == KeyEvent.VK_DELETE && (m_selEnd == m_selStart && m_selEnd == content.length()) || 
						(!m_isTextArea && code == KeyEvent.VK_ENTER)
					 );
		retVal &= !(m_ccpCommand == CcpCommand.CCP_CUT && m_selEnd == m_selStart);
		retVal &= !(m_ccpCommand == CcpCommand.CCP_PASTE && Application.m_app.getClipboardContents().length() == 0);
		retVal &= m_ccpCommand != CcpCommand.CCP_COPY;
		retVal &= m_ccpCommand != CcpCommand.CCP_SELECTALL;
		m_concreteChar = retVal;
		return retVal;
	}
}
