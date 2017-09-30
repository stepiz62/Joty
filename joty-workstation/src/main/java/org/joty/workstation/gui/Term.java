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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.Beans;
import java.util.Vector;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

import org.joty.access.PostStatement;
import org.joty.app.LiteralsCollection;
import org.joty.app.LiteralsCollection.DescrStruct;
import org.joty.app.LiteralsCollection.LiteralStructParams;
import org.joty.common.JotyMessenger;
import org.joty.common.JotyTypes;
import org.joty.common.BasicPostStatement;
import org.joty.data.WrappedField;
import org.joty.gui.InitialValue;
import org.joty.workstation.app.Application;
import org.joty.workstation.app.Application.LiteralStruct;
import org.joty.data.JotyDate;
import org.joty.workstation.data.WField;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.gui.GridRowDescriptor.CellDescriptor;
import org.joty.workstation.gui.TermContainerPanel.TermParams;
import org.joty.workstation.gui.TextArea.JotyJTextArea;

/**
 * Adds to the WrappedField class the responsibility to make the hosted datum to live
 * visually inside a {@code TermContainerPanel} instance.
 * <p>
 * This is an abstract class, a root for various derived classes dedicated to
 * host an embedded Java Swing Component suited to render the type of datum
 * hosted. It has been introduced to re-organize, through this containment, the
 * hierarchy of the various Java Swing components adopted.
 * <p>
 * The Term class provides an inner class and several methods to support the
 * management of a single datum container and to make the datum available either
 * for communication with the database or with other data containers along the
 * application.
 * 
 */
public abstract class Term extends WField {

	public interface AlterFormatMethodInterface {
		String method(String content, Term term);
	}

	public interface AsideLoadInterface {
		int method(TermContainerPanel m_panel, WResultSet rs);
	}

	public enum CcpCommand {
		CCP_NONE, CCP_CUT, CCP_COPY, CCP_PASTE, CCP_SELECTALL
	}

	public interface TermEnclosable {
		boolean getRelatedEnable();

		Term getTerm();
	};

	Application m_app = Application.m_app;

	/**
	 * Its assignments to 'true' comes from the effects of user actions made on
	 * the contained visual component, during the editing session of the
	 * container {@code DataAccessDialog} instance.
	 */
	private boolean m_dirty;
	protected TermContainerPanel m_panel;

	public String m_name;
	public LiteralStruct m_defaultLiteralStruct;
	/** Hosts the default value if defined */
	private InitialValue m_defaultValue;
	/**
	 * Hosts the value derived from the context
	 * 
	 * @see Factory#setTermValuedFromParam
	 * @see DataAccessPanel#setTermsOnContext
	 */
	private InitialValue m_contextValue;

	/** If true the dependence realizes itself positively. */
	boolean m_dependenceDirectness;

	private boolean m_modifiable;
	private boolean m_lockedAnyway;
	boolean m_isForFKeyBuffering;
	public int m_valMirrorTermIdx;
	boolean m_temporarilyHiddenToEnable;
	private boolean m_readOnly;
	private boolean m_onlyLoadingData;
	/**
	 * Identifies a GridTerm object of which the currently selected record is to
	 * be updated in the field specified by the {@code m_fieldToDrive} member
	 * variable.
	 */
	GridTerm m_drivenBufferTerm;
	String m_fieldToDrive;
	public boolean m_updatingActor;

	boolean m_forcedNoDecimal;
	boolean m_reloadNeeded;
	String m_sortExpr;
	boolean m_manageDepCtrlsVisibility;
	public String m_extendedSet;
	public String m_mask;

	private boolean m_controlTerm;
	public AsideLoadInterface m_asideLoadMethod;

	protected boolean m_mandatory;
	public String m_msg;

	public boolean m_mustRemainEnabled;
	private boolean m_enabledAsDetail;
	public boolean m_isToBeLockedAnyWay;
	public AlterFormatMethodInterface m_alterFormatMethod;

	public ViewersManager m_viewersManager;

	/**
	 * A vector of Term instance names that are tight to 'this' instance by a
	 * relation of dependency in term of the enable state and the chance to have
	 * value.
	 */
	Vector<String> m_dependentTermNamesVect;

	protected boolean m_required;
	protected boolean m_clearable;
	public boolean m_ctrlTermInitedByParam;

	/**
	 * To be its state preserved against several clearing actions made by the
	 * framework on all the Term instances. (That is in some way having an
	 * independent life)
	 */
	private boolean m_isDataComplement;

	public JComboBox m_operatorsCombo;
	public int m_effectsIndex;
	public int m_tabIndex;
	public TermContainerPanel m_container;
	protected LiteralsCollection m_literalsCollectionInstance;

	Vector<JButton> m_relatedButtons;
	JotyButton m_browseButton;
	

	private String m_visualTerm;

	public Term() {
		super(Application.m_app);
	}

	public Term(TermContainerPanel panel, int dataType, TermParams params) {
		super(Application.m_app);
		m_literalsCollectionInstance = new LiteralsCollection(Application.m_app);
		m_panel = panel.m_targetPanel == null ? panel : panel.m_targetPanel;
		m_dataType = dataType;
		m_dbFieldName = params.m_dbField;
		m_name = params.m_termName;
		m_mandatory = params.m_mandatory;
		m_msg = params.m_msg;
		m_len = params.m_len;

		m_extendedSet = "";
		m_isCurrency = params.m_currency;
		m_dependenceDirectness = true;
		m_valMirrorTermIdx = -1;
		m_isForFKeyBuffering = false;
		m_modifiable = true;
		m_lockedAnyway = false;
		m_temporarilyHiddenToEnable = false;
		createComponent(m_panel);
		if (params.m_termName == null)
			m_panel.notifyJotyDesignError(getComponent(), "'termName' parameter must be specified !");
		m_dependentTermNamesVect = new Vector<String>();
		m_clearable = true;
		m_dirty = false;
		m_ctrlTermInitedByParam = false;
		m_effectsIndex = 0;
		m_relatedButtons = new Vector<JButton>();
		m_tabIndex = -1;
	}

	void addDependentTerm(String termName) {
		if (m_dependentTermNamesVect == null)
			m_dependentTermNamesVect = new Vector<String>();
		m_dependentTermNamesVect.add(termName);
	}

	/**
	 * Show a message related to the Term instance, next to it or optionally
	 * next to an associate other Term instance. If a target term is in an
	 * hidden pane of the TabbedPane object the method select that pane.
	 * <p>
	 * If {@code m_msg} has value its content will be used as text else the
	 * parameter will be used as literal for picking the text up form the
	 * language "dictionary".
	 * 
	 * @param msgLiteral
	 */
	protected void alert(String msgLiteral) {
		if (m_visualTerm != null)
			m_panel.term(m_visualTerm).alert(msgLiteral);
		else {
			if (m_tabIndex >= 0)
				((MultiPanelDialog) m_panel.getDialog()).m_tabbedPane.setSelectedIndex(m_tabIndex);
			Color oldBgColor = getComponent().getBackground();
			boolean wasDisabled = !getComponent().isEnabled();
			if (wasDisabled)
				getComponent().setEnabled(true);
			getComponent().setBackground(Color.red);
			if (m_msg == null)
				langLocatedWarningMsg(msgLiteral);
			else
				locatedWarningMsg(m_msg);
			getComponent().setBackground(oldBgColor);
			if (wasDisabled)
				getComponent().setEnabled(false);
		}
	}

	void checkAndClear() {
		checkAndClear(false);
	}

	void checkAndClear(boolean strong) {
		if (m_clearable) {
			if (!m_readOnly || isLockedAnyway() || m_panel.clearNeeded() || strong) {
				clear();
			}
		}
	}

	/** Checks if it is time to notify data change */
	protected void checkForPublishing() {}

	/** Checks if some publisher has published its data change */
	public void checkPublishers() {}

	public void checkRendering() {}

	public void checkSelection() {}

	void checkVisibility() {
		if (!isWindowVisible() && !dbFieldSpecified())
			checkAndClear();
	}

	@Override
	public void clear() {
		clearComponent();
		super.clear();
	}

	/**
	 * Clears any reference to this instance made by / located in the
	 * {@code Application} object
	 */
	public void clearAppReferences() {}

	protected void clearComponent() {}

	void clearDependentComponents() {
		JComponent pWnd;
		boolean doIt = true;
		if (doIt) {
			m_panel.guiDataExch(true);
			Term term;
			for (int j = 0; j < m_dependentTermNamesVect.size(); j++) {
				term = m_panel.term(m_dependentTermNamesVect.get(j));
				if (term != null) {
					if (!term.m_temporarilyHiddenToEnable)
						term.checkAndClear();
					term.m_temporarilyHiddenToEnable = false;
				} else {
					pWnd = m_panel.term(m_dependentTermNamesVect.get(j)).getComponent();
					if (pWnd instanceof JToggleButton)
						((JToggleButton) pWnd).setSelected(false);
				}
			}
			m_panel.guiDataExch(false);
		}
	}

	void clearNonStructuredCtrl() {}

	public int commandDownMask() {
		return m_app.m_macOs ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
	}


	abstract protected void createComponent(TermContainerPanel panel);

	public PostStatement createContextPostStatement() {
		return m_panel.createContextPostStatement(m_name);
	}

	protected LiteralStructParams createDescrArrayParams() {
		return m_literalsCollectionInstance.new LiteralStructParams();
	}

	public InitialValue defaultValue() {
		if (m_defaultValue == null)
			m_defaultValue = new InitialValue(m_app, this);
		return m_defaultValue;
	}
	
	public InitialValue contextValue() {
		if (m_contextValue == null)
			m_contextValue = new InitialValue(m_app, this);
		return m_contextValue;
	}

	protected String doRender(WrappedField wfield) {
		return wfield.render(m_forcedNoDecimal);
	}

	protected boolean doValidate() {
		return true;
	}

	public void effectsPostPone(Term term) {
		term.m_effectsIndex = m_effectsIndex + 1;
		if (term.m_effectsIndex > m_panel.m_maxEffectsIndex)
			m_panel.m_maxEffectsIndex = term.m_effectsIndex;
	}

	protected void enable(boolean predicate) {
		getComponent().setEnabled(predicate);
	}

	protected void enableComponent(boolean truth, boolean editability, boolean docIdentified) {
		enable((truth && !m_enabledAsDetail) || (!truth && m_enabledAsDetail && docIdentified) || m_mustRemainEnabled);
	}

	public void enableComponent(boolean enablCtx, boolean stateCmd, boolean docIdentified, boolean basicallyEditable, boolean editability) {
		boolean truth = enablCtx && stateCmd && !m_lockedAnyway && !m_readOnly && (basicallyEditable || m_modifiable);
		enableComponent(truth, editability, docIdentified);
	}

	public void enableRelatedButtons() {
		for (JButton button : m_relatedButtons)
			button.setEnabled(getTermComponent().getRelatedEnable());
	}

	void enablingDependentComponents() {
		enablingDependentComponents(true);
	}

	void enablingDependentComponents(boolean generalEnabling) {
		Term term;
		boolean bPredicate;
		bPredicate = getDepEnablingStatus(generalEnabling);
		for (int j = 0; j < m_dependentTermNamesVect.size(); j++) {
			term = m_panel.term(m_dependentTermNamesVect.get(j));
			if (term != null) {
				if (!term.m_temporarilyHiddenToEnable) {
					if (bPredicate) {
						term.enable(bPredicate);
						if (!bPredicate)
							term.enablingDependentComponents(bPredicate);
					}
				}
				if (bPredicate)
					term.m_temporarilyHiddenToEnable = false;
			} else
				m_panel.term(m_dependentTermNamesVect.get(j)).enable(bPredicate);
			if (m_manageDepCtrlsVisibility) {
				boolean visible = getDepEnablingStatus();
				m_panel.showComponent(m_dependentTermNamesVect.get(j), visible);
				if (term != null)
					m_temporarilyHiddenToEnable = !visible;
			}
		}
	}

	public CellDescriptor fieldDescr(String name) {
		return null;
	}

	public JComponent getComponent() {
		return null;
	}

	int getCount() {
		return 0;
	}

	long getCurSelData(boolean updateData) {
		return -1;
	}

	boolean getDepEnablingStatus() {
		return m_dependenceDirectness ? getSetStatus() : !getSetStatus();
	}

	boolean getDepEnablingStatus(boolean generalEnabling) {
		return getDepEnablingStatus() && generalEnabling;
	}

	public int getSelection() {
		return -1;
	}

	protected boolean getSetStatus() {
		return true;
	}

	private TermEnclosable getTermComponent() {
		return (TermEnclosable) getComponent();
	}

	protected long getTermData() {
		return selectionData();
	}

	protected String getWindowText() {
		return "";
	}

	/**
	 * Conceptually abstract this method has the task t o exchange the datum
	 * between the visual representation made by the visual component and the
	 * datum internally held.
	 * 
	 * @param in
	 *            it is true if the direction of the exchanging is from the
	 *            visual to the internal representation.
	 */
	public void guiDataExch(boolean in) {
		if (m_app.debug())
			typeCheck();
	}

	void init() {
		if (!Beans.isDesignTime())
			m_panel.getDialog().checkForHooveringListener(getComponent());
	}

	void innerClear() {}

	public boolean innerClearData() {
		return true;
	}

	public void innerLoad() {}

	public boolean innerStore() {
		return true;
	}

	public boolean isAControlTerm() {
		return m_controlTerm;
	}

	public boolean isDataComplement() {
		return m_isDataComplement;
	}

	public boolean isDirty() {
		return m_dirty;
	}

	public boolean isEnabledAsDetail() {
		return m_enabledAsDetail;
	}

	public boolean isLockedAnyway() {
		return m_lockedAnyway;
	}

	public boolean isMandatory() {
		return m_mandatory;
	}

	public boolean isModifiable() {
		return m_modifiable;
	}

	public boolean isOnlyLoadingData() {
		return m_onlyLoadingData;
	}

	public boolean isReadOnly() {
		return m_readOnly;
	}

	boolean isSelectedIndex(int index) {
		return false;
	}

	public boolean isWindowEnabled() {
		return false;
	}

	abstract public boolean isWindowVisible();

	public void killFocus() {
		guiDataExch(true);
	}

	public void langLocatedWarningMsg(String literal) {
		locatedWarningMsg(m_app.m_common.jotyLang(literal));
	}

	/**
	 * Show a Warning message and locates it next to the term instance but
	 * avoiding to completely overlap it
	 */

	public void locatedWarningMsg(String text) {
		JOptionPane pane = new JOptionPane(text, JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION);
		final JDialog d = pane.createDialog(m_app.m_name);
		d.setIconImages(m_app.m_iconImages);
		Point paneLocation = d.getLocation();
		Point componentLocation = getComponent().getLocationOnScreen();
		Rectangle componentBounds = new Rectangle();
		componentBounds.x = componentLocation.x;
		componentBounds.y = componentLocation.y;
		componentBounds.width = getComponent().getWidth();
		componentBounds.height = getComponent().getHeight();
		Rectangle intersection = new Rectangle();
		Rectangle.intersect(d.getBounds(), componentBounds, intersection);
		if (componentBounds.width - intersection.width < 20 && componentBounds.height - intersection.height < 20)
			d.setLocation(paneLocation.x, paneLocation.y + d.getBounds().height * (paneLocation.y > componentLocation.y ? 1 : -1));
		d.setVisible(true);
	}

	private void makeEffective() {
		if (m_panel.getDialog().isEditing())
			setDirty();
		guiDataExch(false);
	}

	public void manageAsRelated(LiteralStruct masterLiteralStruct, boolean loadSetIfAvailable) {}

	public void manageTermConsistence() {}

	public CellDescriptor masterGridCellDescriptor() {
		return m_panel.gridCellDescriptor(m_name, true);
	}

	protected void message(String msg) {
		Application.warningMsg(m_msg != null ? m_msg : msg);
	}

	public void notifyEditingAction(ActionEvent e) {
		if (m_panel.getDialog().m_listenForPanelActions)
			m_dirty = true;
	}

	/**
	 * Notifies the framework that {@code publisherDialogName} has its data
	 * changed.
	 */
	public void notifyPublishing(String publisherDialogName) {}

	void preInit(TermContainerPanel panel) {}

	public void preRender() {}

	public void refresh() {}

	protected String render(WrappedField wfield) {
		String output = doRender(wfield);
		return m_alterFormatMethod != null ? m_alterFormatMethod.method(output, this) : output;
	}

	/**
	 * Provides a name for the entity associated with this Term instance and
	 * with the specific value it assumes on the current record
	 * 
	 * @param fieldSpecified
	 *            possible name of another field used the
	 *            {@code IdentityRenderer} implementation of which will be used
	 * @return the rendered text
	 */
	public String renderedIdentity(String fieldSpecified) {
		CellDescriptor cellDescriptorInMasterGrid = masterGridCellDescriptor();
		return cellDescriptorInMasterGrid == null || cellDescriptorInMasterGrid.m_identityRenderer == null ? ((m_panel.getGridManager() == null ? "" : (String.valueOf(m_panel.getGridManager().getCurSel() + 1)))) : cellDescriptorInMasterGrid.m_identityRenderer.render();
	}

	public void reset() {}

	void resetDirtyStatus() {
		m_dirty = false;
	}

	public long selectionData() {
		return getCurSelData(false);

	}

	protected void set(Term source) {
		set((WrappedField) source);
		m_onlyLoadingData = source.m_onlyLoadingData;
		defaultValue().copyValue(source.defaultValue().getValue());
		m_reloadNeeded = source.m_reloadNeeded;
	}

	protected void set(WrappedField source) {
		copyWField(source, false);
	}

	public void setAsControlTerm() {
		m_controlTerm = true;
	}

	public void setAsDataComplement() {
		m_isDataComplement = true;
	}

	void setCheck(int val) {
		setInteger(val);
	}

	public void setCurSel(int index) {
		setSelection(index, false);
	}

	int setCurSel(String val) {
		return -1;
	}

	public void setDirty() {
		m_dirty = true;
		if (m_panel != null)
			m_panel.notifyEditingAction(null);
	}

	public void setEnabledAsDetail() {
		m_enabledAsDetail = true;
	}

	void setFocus() {
		getComponent().grabFocus();
	}

	public void setLockedAnyway(boolean lockedAnyway) {
		m_lockedAnyway = lockedAnyway;
	}

	public void setMandatory() {
		m_mandatory = true;
	}

	public void setMandatory(String visualTerm) {
		setMandatory();
		m_visualTerm = visualTerm;
	}

	public void setModifiable(boolean modifiable) {
		m_modifiable = modifiable;
	}

	void setNoDecimal() {
		m_forcedNoDecimal = true;
	}

	public void setNotClearable() {
		m_clearable = false;
	}

	public void setOnlyLoadingData(boolean onlyLoadingData) {
		m_onlyLoadingData = onlyLoadingData;
	}

	public void setReadOnly(boolean readOnly) {
		m_readOnly = readOnly;
	}

	public void setRowActionButton(JButton button) {
		m_relatedButtons.add(button);
	}

	public int setSelection(long val, boolean basedOnData) {
		return -1;
	}

	public void setTermVal(double value) {
		if (m_dataType == JotyTypes._double)
			setVal(value);
		else
			invalidAssignementMsg();
	}

	public void setTermVal(float value) {
		if (m_dataType == JotyTypes._single)
			setVal(value);
		else
			invalidAssignementMsg();
	}

	public void setTermVal(int value) {
		if (m_dataType == JotyTypes._int || m_dataType == JotyTypes._dbDrivenInteger)
			setVal(value);
		else
			invalidAssignementMsg();
	}

	public void setTermVal(JotyDate value) {
		if (m_dataType == JotyTypes._date || m_dataType == JotyTypes._dateTime)
			setVal(value);
		else
			invalidAssignementMsg();
	}

	public void setTermVal(long value) {
		if (m_dataType == JotyTypes._long || m_dataType == JotyTypes._dbDrivenInteger)
			setVal(value);
		else
			invalidAssignementMsg();
	}

	public void setTermVal(String value) {
		if (m_dataType == JotyTypes._text)
			setVal(value);
		else
			invalidAssignementMsg();
	}

	public void setToVal(double dblVal) {
		super.setVal(dblVal);
		makeEffective();
	}

	public void setToVal(float fltVal) {
		super.setVal(fltVal);
		makeEffective();
	}

	public void setToVal(int iVal) {
		super.setVal(iVal);
		makeEffective();
	}

	public void setToVal(JotyDate dtVal) {
		super.setVal(dtVal);
		makeEffective();
	}

	public void setToVal(long lVal) {
		super.setVal(lVal);
		makeEffective();
	}

	public void setToVal(String strVal) {
		super.setVal(strVal);
		makeEffective();
	}

	public void show(boolean truth) {}

	public String sqlValueExpr() {
		return render(false, true);
	}

	public void storeState(WResultSet rs) {
		setWField(rs);
	}

	public void structuredInit() {}

	void structuredInit(boolean loadSetIfAvailable) {}

	public void termRender() {
		termRender(true);
	}

	public void termRender(boolean checkUnselection) {}

	public void termRender(boolean checkUnselection, boolean preClearComponent) {
		if (preClearComponent)
			clearComponent();
		termRender(checkUnselection);
	}

	@Override
	public String toString() {
		return "";
	}

	public String toString(WrappedField wfield) {
		return render(wfield);
	}

	protected void updateAspect() {}

	protected void updateDrivenBuffer() {
		if (m_drivenBufferTerm != null && m_drivenBufferTerm.m_dataBuffer != null)
			if (m_drivenBufferTerm.m_dataBuffer.m_cursorPos >= 0)
				m_drivenBufferTerm.m_dataBuffer.wfield(m_fieldToDrive).copyWField(this, false);
	}

	public void updateState(WrappedField rowCell) {
		copyWField(rowCell, false);
	}

	public void updateState(WResultSet rs) {}

	/** Performs validation in the end of typical editing sessions */
	boolean validate() {
		boolean success = true;
		m_required = m_mandatory && isWindowVisible();
		success = doValidate();
		if (!success) {
			if (m_browseButton == null) {
				if (isWindowEnabled())
					setFocus();
			} else
				m_browseButton.grabFocus();
		}
		return success;
	}

}
