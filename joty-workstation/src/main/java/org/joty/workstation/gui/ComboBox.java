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
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.joty.app.LiteralsCollection.DescrStruct;
import org.joty.workstation.app.Application;
import org.joty.workstation.gui.Term.TermEnclosable;

/**
 * Provides access to the loaded list of {@code DescrStruct} instances.
 * <p>
 * It is instantiated as 'editable' when the hosted data type is {@code _text}.
 * <p>
 * It clears itself on the {DELETE/CANC} key. It implements the 'copy' operation
 * taking the text currently displayed even when not editable.
 * <p>
 * It allow customized effects, on state change, by means of the possible
 * implementation of its internal interface {@code ActionPostExecutor}.
 * 
 * @see ComboBoxTerm
 * @see org.joty.workstation.app.Application.LiteralStruct
 * @see ActionPostExecutor
 * 
 */
public class ComboBox extends JComboBox<Object> implements FocusListener, ActionListener, KeyListener, PopupMenuListener, TermEnclosable {

	/**
	 * The implementation of this interface takes care of doing something
	 * delayed in respect of what the Joty framework asks the component to
	 * execute when it changes status, that is the last statement in the
	 * {@code ActionListener.actionPerformed} implementation. That is its
	 * contribution acts certainly after the component has notified to the
	 * framework its change, and after the framework has 'reacted', indeed.
	 * 
	 * @see #setActionPostExecutor
	 */

	public interface ActionPostExecutor {
		public void doIt(ComboBox comboBox);
	}

	public class SelectionState {
		int selStart;
		int selEnd;
	}

	public TermContainerPanel m_panel;
	public Term m_term;
	public SelectionState m_selectionState;
	final String DEL_ACTION = "del-action";
	private int m_oldSelIndex;
	
	/** @see ActionPostExecutor
	 *  @see #setActionPostExecutor
	 */
	public ActionPostExecutor m_actionPostExecutor;

	public boolean m_loadingData;

	ComboBox() {}

	public ComboBox(TermContainerPanel panel, Term term) {
		this(panel, term, false);
	}

	public ComboBox(TermContainerPanel panel, Term term, boolean editable) {
		this();
		m_panel = panel;
		m_term = term;
		addFocusListener(this);
		addActionListener(this);
		getModel();
		getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DELETE"), DEL_ACTION);
		getActionMap().put(DEL_ACTION, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setCurSel(-1);
			}
		});
		addKeyListener(this);
		addPopupMenuListener(this);
		setEditable(editable);
		if (editable) {
			m_selectionState = new SelectionState();
			getEditor().getEditorComponent().addFocusListener(this);
			getEditor().getEditorComponent().addKeyListener(this);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		m_term.notifyEditingAction(e);
		if (m_panel != null) {
			m_panel.notifyEditingAction(e);
			boolean bPredicate = m_term.getDepEnablingStatus(true);
			if (!bPredicate && m_term.m_dependenceDirectness || bPredicate && !m_term.m_dependenceDirectness)
				m_term.clearDependentComponents();
			m_term.enablingDependentComponents();
		}
		newIndexEffects();
		if (m_actionPostExecutor != null && 
				!m_loadingData && 
				(!(m_panel instanceof DataAccessPanel) || !((DataAccessPanel) m_panel).m_loadingData))
			m_actionPostExecutor.doIt(this);
	}

	@Override
	protected void fireActionEvent() {
		boolean inhibiting = false;
		if (m_panel.m_dialog instanceof DataAccessDialog)
			inhibiting = ((DataAccessDialog) m_panel.m_dialog).m_saving;
		if (m_panel != null && 
				!m_panel.isListeningForActions() && 
				m_actionPostExecutor == null || inhibiting)
			return;
		super.fireActionEvent();
	}

	@Override
	public void focusGained(FocusEvent focusevent) {}

	@Override
	public void focusLost(FocusEvent focusevent) {
		if (isEditable()) {
			m_term.setCurSel(getText());
		}
		if (m_term != null)
			m_term.killFocus();
	}

	public int getCurSel() {
		return getSelectedIndex();
	}

	public SelectionState getEditSel() {
		JTextField txtBox = (JTextField) getEditor().getEditorComponent();
		m_selectionState.selStart = txtBox.getSelectionStart();
		m_selectionState.selEnd = txtBox.getSelectionEnd();
		return m_selectionState;
	}

	public long getItemData(int index) {
		Object obj = getItemAt(index);
		if (obj == null)
			return -1;
		else
			return (int) ((DescrStruct) obj).id;
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
		if (isEditable())
			return getEditor().getItem().toString();
		else {
			DescrStruct descrStruct = (DescrStruct) getSelectedItem();
			return descrStruct == null ? "" : descrStruct.descr;
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if ((e.getModifiersEx() & m_term.commandDownMask()) > 0 && e.getKeyCode() == KeyEvent.VK_C)
			Application.m_app.setClipboardContents(getText());
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (isEditable())
			m_term.checkRendering();
	}

	@Override
	public void keyTyped(KeyEvent e) {}

	protected void newIndexEffects() {}

	@Override
	public void popupMenuCanceled(PopupMenuEvent popupmenuevent) {}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent popupmenuevent) {
		if (isEditable() && getCurSel() != m_oldSelIndex)
			m_term.checkRendering();
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent popupmenuevent) {
		m_oldSelIndex = getCurSel();
	}

	/**
	 * Registers a {@code ActionPostExecutor} implementation instance
	 * 
	 * @see ActionPostExecutor
	 */

	public void setActionPostExecutor(ActionPostExecutor executor) {
		m_actionPostExecutor = executor;
	}

	public int setCurSel(int i) {
		int retVal = -1;
		try {
			setSelectedIndex(i);
			retVal = i;
		} catch (Exception e) {}
		if (Application.m_debug && retVal != i && m_term != null)
			Application.m_app.JotyMsg(m_term, "ComboBox value not set !");
		return retVal;
	}

	public void setEditSel(SelectionState selState) {
		JTextField txtBox = (JTextField) getEditor().getEditorComponent();
		txtBox.setSelectionStart(selState.selStart);
		txtBox.setSelectionEnd(selState.selEnd);
	}

	@Override
	public void setEnabled(boolean flag) {
		super.setEnabled(flag);
		if (isEditable())
			setBackground(flag ? Color.WHITE : getParent().getBackground());
	}

	@Override
	public void setSize(int arg0, int arg1) {
		super.setSize(80, 20);
	}

	public void setText(String val) {
		if (getEditor() != null)
			getEditor().setItem(val);
	}

}
