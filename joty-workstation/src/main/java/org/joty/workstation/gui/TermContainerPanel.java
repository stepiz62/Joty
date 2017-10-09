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
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.Beans;
import java.util.Map;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.joty.access.Logger;
import org.joty.common.CaselessStringKeyMap;
import org.joty.common.JotyTypes;
import org.joty.common.ReportManager;
import org.joty.common.Utilities;
import org.joty.common.Utilities.Stocker;
import org.joty.data.*;
import org.joty.gui.WFieldSet;
import org.joty.workstation.app.Application;
import org.joty.workstation.app.Application.LiteralStruct;
import org.joty.workstation.data.JotyDataBuffer;
import org.joty.data.JotyDate;
import org.joty.workstation.data.WField;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.gui.GridRowDescriptor.CellDescriptor;
import org.joty.workstation.gui.GridRowDescriptor.RowCellMappingType;
import org.joty.workstation.gui.Term.TermEnclosable;

/**
 * Provides the containment of {@code Term} objects. Each object can be added by
 * means of the creator methods in the {@code Factory} class that, at the late
 * stage, invoke the local {@code addTerm} method, making use of instances of
 * the {@code TermParams} inner class for managing various possible parameters.
 * <p>
 * The class is equipped with methods that exchange data between each visual
 * component and the relative field hosted by the wrapping {@code Term} object.
 * <p>
 * It is possible to have the instance living as 'part' of another one: the
 * {@code m_targetPanel} member references the object that has the identity or
 * the responsibility as you wish to say.
 * 
 * 
 * @see Term
 * @see JotyDialog
 * 
 */
public class TermContainerPanel extends Panel implements ActionListener {

	/**
	 * Has the responsibility to implement the call for the opening of a
	 * JotyDialog object related to the current container dialog, after,
	 * possibly, having conveniently prepared the ParamContext object to be
	 * passed.
	 * <p>
	 * Optionally it allows to provide a contribution to the title of the target
	 * dialog.
	 */
	abstract public class BuildDetailsDialogAdapter {
		public JotyDialog createDialog() {
			return null;
		};

		public abstract JotyDialog createDialog(TableTerm GridTerm);

		/**
		 * override the following method in order to have a stand alone details
		 * dialog (with title extended by the text here returned). If this
		 * method returns null the details dialog is dependent in its life on
		 * this dialog life.
		 */
		public String identifierFromCaller() {
			return null;
		};
	}

	public class ListeningState {
		boolean panelState;
		boolean dialogState;
	}

	/**
	 * An helper class for parameter passing in the adding of a {@code Term}
	 * object.
	 * 
	 * @see Factory
	 */
	public class TermParams {

		String m_termName;
		String m_dbField;
		public int m_len = 0;
		boolean m_mandatory = false;
		String m_dataTable = null;
		String m_targetDatumField = null;
		String mainEntityKeyField = null;
		public String mainTermName = null;
		String m_msg = null;
		boolean m_dataSetLoadOnly = true;
		public String m_descrSetName = null;
		boolean m_currency = false;

		protected TermParams(String termName, String dbField) {
			m_termName = termName;
			m_dbField = dbField;
		}
	}

	public Vector<Term> m_terms;
	public CaselessStringKeyMap<Term> m_termMap;
	public Stocker m_dbFieldsHostedByTerms;
	public String m_currentActorName;
	protected boolean m_currentDependenceDirection;
	private Vector<JotyDataBuffer> m_tableSet;
	public String m_formKeyHiddenTerm;
	public String m_IdFieldName;
	public int m_IdFieldElemIdx;
	public int m_recSensingCtrlIdx;
	public String m_listRowHeader;
	public boolean m_termsInBold;
	public boolean m_initializing;
	public WFieldSet m_keyElems;
	public WFieldSet m_wfields;
	public SearchQueryBuilderFront m_queryBuilder;
	public boolean m_dirty;
	public GridRowDescriptor m_gridRowDescriptor;
	protected BuildDetailsDialogAdapter m_buildDetailsHandler;
	public int m_hiddenTermsCount;
	public DataAccessPanel m_targetPanel;
	public boolean m_inhibitChangeNotification;
	private boolean m_synchroTermActing = false;
	protected boolean m_listenForActions;
	public boolean m_inhibitGridTermsEffect = false;
	public int m_maxEffectsIndex;

	boolean m_validatingComponents;

	public TermContainerPanel() {
		super();
		m_terms = new Vector<Term>();
		m_termMap = new CaselessStringKeyMap<Term>(m_app);
		m_dbFieldsHostedByTerms = Utilities.m_me.new Stocker();
		m_currentActorName = null;
		m_currentDependenceDirection = false;
		if (!Beans.isDesignTime())
			m_dialog = m_app.m_definingDialog;
		m_termsInBold = false;
		m_keyElems = new WFieldSet(m_app);
		m_wfields = new WFieldSet(m_app);
		m_gridColumnWidths = null;
		m_absoluteGridColumnWidths = false;
		m_queryBuilder = new SearchQueryBuilderFront(m_app, m_app.new ClauseContribution());
		m_dirty = false;
		m_formKeyHiddenTerm = null;
		m_targetPanel = null;
		m_listenForActions = true;
		m_inhibitChangeNotification = false;
		m_gridRowDescriptor = new GridRowDescriptor();
		m_maxEffectsIndex = 0;
	}

	/**
	 * Used for presenting a selector dialog to the user and for collecting the
	 * result of the choice in order to give value to one or more target
	 * {@code Term} objects and optionally {@code WrappedField} instances.
	 * 
	 * @param browserDialogClassName
	 *            the dialog for the choice
	 * @param termName
	 *            the target {@code Term} object receiving the result of the
	 *            choice
	 * @param dependentTermList
	 *            list of fields or terms associated to the chosen key value
	 * @param targetFieldList
	 *            other target field receiving value as effect of the choice
	 * @param openingMode
	 *            optional mode of opening for the browsing dialog
	 * @param paramToSet
	 *            name of the optional context parameters to be set
	 * @param withQueryParams
	 *            if true the browsing dialog will have the calling context
	 *            available for looking for context parameters
	 * @return true if the selection was successful
	 */
	protected boolean acquireSelectedValueFrom(String browserDialogClassName, 
												String termName,
												String[] dependentTermList, 
												String[] targetFieldList, 
												Object openingMode, 
												String paramToSet,
												boolean withQueryParams) {
		m_app.m_dialogOpeningAsValueSelector = true;
		m_app.m_justSelectedValue = -1;
		m_app.m_valuesContainer.clear();
		if (dependentTermList != null) {
			if (term(termName).m_dataType != JotyTypes._long && term(termName).m_dataType != JotyTypes._dbDrivenInteger) {
				m_app.JotyMsg(this, "The first argument must refer to a 'long num' term or a combo box term !");
				return false;
			}
			Term term;
			int i = 0;
			if (targetFieldList != null && targetFieldList.length != dependentTermList.length) {
				m_app.JotyMsg(this, "method 'acquireSelectedValueFrom' : wrong dimension for array argument 'targetFieldList' !");
				return false;
			}
			for (String dependentTerm : dependentTermList) {
				term = term(dependentTerm);
				m_app.m_valuesContainer.add(term.m_name, targetFieldList == null ? term.m_dbFieldName : targetFieldList[i]);
				i++;
			}
		}
		if (withQueryParams)
			DataAccessDialog.tryCreate(browserDialogClassName, callContext(), openingMode);
		else
			DataAccessDialog.tryCreate(browserDialogClassName, openingMode);
		if (m_app.m_justSelectedValue != -1) {
			if (termName != null) {
				term(termName).setDirty();
				if (term(termName) instanceof TextTerm) {
					term(termName).setInteger(m_app.m_justSelectedValue);
					term(termName).guiDataExch(false);
				} else if (term(termName) instanceof ComboBoxTerm) {
					if (m_app.m_justSelectedValue != -1 && termName != null)
						comboTerm(termName).selectItem(m_app.m_justSelectedValue);
				}
				if (dependentTermList != null)
					for (String dependenTerm : dependentTermList) {
						term(dependenTerm).copyWField(m_app.m_valuesContainer.getValue(dependenTerm), false);
						term(dependenTerm).guiDataExch(false);
						term(dependenTerm).setDirty();
					}
			}
			if (paramToSet != null)
				setContextParam(paramToSet, m_app.m_justSelectedValue);
		}
		m_app.m_dialogOpeningAsValueSelector = false;
		enableComponents(true);
		return m_app.m_justSelectedValue != -1;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {}

	public WrappedField addField(String fieldName, int dataType) {
		return m_wfields.add(fieldName, dataType);
	}

	public void addFieldToGrid(String fieldName) {
		addWFieldToGrid(fieldName, m_wfields, null, RowCellMappingType.FIELD);
	}

	public void addIntegerKeyElem(String fieldName) {
		addKeyElem(fieldName, JotyTypes._dbDrivenInteger);
	}

	public WrappedField addKeyElem(String fieldName, int dataType) {
		if (Beans.isDesignTime())
			return new WField(m_app);
		if (keysRefsOnDialogAreTobeSet() && m_app.m_definingDialog.m_keyElems == null)
			m_app.m_definingDialog.m_keyElems = m_keyElems;
		return m_keyElems.add(fieldName, dataType);
	}

	protected void addKeyElemToGrid(String keyName) {
		addKeyElemToGrid(keyName, null);
	}

	public void addKeyElemToGrid(String keyName, String label) {
		addWFieldToGrid(keyName, m_keyElems, label, RowCellMappingType.KEY_ELEM);
	}

	public void addStrKeyElem(String fieldName) {
		addKeyElem(fieldName, JotyTypes._text);
	}

	protected void addTerm(Term term, TermParams params) {
		if (params.m_termName == null)
			return;
		if (m_targetPanel != null) {
			term.m_tabIndex = ((MultiPanelDialog) getDialog()).m_tabbedPane.getTabCount();
			m_targetPanel.addTerm(term, params);
			term.m_container = this;
		} else {
			CaselessStringKeyMap<Term> map = m_termMap;
			Vector<Term> vector = m_terms;
			if (map.get(params.m_termName) != null) {
				String msg = String.format("termName: '%1$s' is already used !", params.m_termName);
				notifyJotyDesignError(term.getComponent(), msg);
			} else {
				int nTerms = vector.size();
				vector.add(term);
				map.put(params.m_termName, term);
				if (params.m_dbField != null)
					m_dbFieldsHostedByTerms.add(params.m_dbField);
				term.m_idx = nTerms;
				implementDependency(params.m_termName);
			}
		}
	}

	public void addTermAsReportParameter(Term term, String paramName) {
		ReportManager reportManager = m_app.m_reportManager;
		switch (term.m_dataType) {
			case JotyTypes._dbDrivenInteger:
			case JotyTypes._long:
			case JotyTypes._text:
			case JotyTypes._double:
			case JotyTypes._single:
			case JotyTypes._date:
			case JotyTypes._dateTime: {
				String valueLiteral = term.render().replace("'", "''");
				reportManager.addParameter(paramName, valueLiteral, term.m_dataType);
			}
			break;
			case JotyTypes._int:
				reportManager.addParameter(paramName, term.m_iVal);
				break;
			default:
				Application.warningMsg("Unsupported parameter type !");
		}

	}

	public void addTermToGrid(String termName) {
		addTermToGrid(termName, null);
	}

	public void addTermToGrid(String termName, String label) {
		Term term = m_termMap.get(termName);
		if (term == null)
			m_app.JotyMsg(this, "No term with name '" + termName + "' found !");
		else {
			if (term instanceof ImageTerm && !((ImageTerm) term).m_previewBuffered)
				m_app.JotyMsg(term, "Buffering is intrinsic in having an ImageTerm represented in the navigator grid: \n" + 
											"you can't invoke setPreviewUnbuffered on the term if you whish to have it in the grid !");
			addToGridRowDescriptor(term.m_dbFieldName, RowCellMappingType.PANEL_TERM, term.m_idx, label);
		}
	}

	protected void addToGridRowDescriptor(String fieldName, RowCellMappingType mappingType, int pos, String label) {
		m_gridRowDescriptor.add(fieldName, mappingType, pos, label);
	}

	private void addWFieldToGrid(String fieldName, WFieldSet set, String label, RowCellMappingType mapType) {
		if (!Beans.isDesignTime()) {
			Integer wfieldPos = set.pos(fieldName);
			WrappedField wfield = wfieldPos == null ? null : set.vector.get(wfieldPos);
			if (wfield == null)
				m_app.JotyMsg(this, "Field '" + fieldName + "' not found in the set of the added wfields of the panel !");
			else
				addToGridRowDescriptor(fieldName, mapType, wfieldPos, label);
		}
	}

	protected boolean basicallyEditable() {
		return true;
	}

	public BlobTerm blobTerm(String termName) {
		return (BlobTerm) term(termName);
	}

	public BufferedComboBoxTerm bufferedComboTerm(String termName) {
		return (BufferedComboBoxTerm) term(termName);
	}

	void buildSet(JotyDataBuffer dataBuffer, int posID, String sqlStmnt) {
		WResultSet rs = new WResultSet(null, sqlStmnt);
		dataBuffer.loadData(rs);
		m_tableSet.setElementAt(dataBuffer, posID);
	}

	public Object callContext() {
		return getDialog().m_callContext;
	}

	public void checkComponentsRendering() {
		for (Term term : m_terms)
			term.checkRendering();
	}

	boolean checkEditing() {
		return true;
	}

	public CheckTerm checkTerm(String termName) {
		return (CheckTerm) term(termName);
	}

	public CheckListTerm chkListTerm(String termName) {
		return (CheckListTerm) term(termName);
	}

	protected void cleanDescr(LiteralStruct literalStruct) {
		if (!Beans.isDesignTime() && literalStruct != null)
			literalStruct.clear();
	}

	void cleanDescr(String literal) {
		cleanDescr((LiteralStruct) m_app.m_common.m_literalStructMap.get(literal));
	}

	public void clearAppReferences() {
		for (Term term : m_terms)
			term.clearAppReferences();
	}

	boolean clearNeeded() {
		return true;
	}

	void clearTerms() {
		clearTerms(false);
	}

	void clearTerms(boolean DueToStateChange) {
		ListeningState listeningState = setPanelActionListeningOff();
		for (Term term : m_terms) {
			if (!term.isAControlTerm() && !term.isDataComplement())
				term.checkAndClear(injectedDialog().m_new_command || DueToStateChange);
		}
		restorePanelActionListening(listeningState);
	}

	public ComboBoxTerm comboTerm(String termName) {
		return (ComboBoxTerm) term(termName);
	}

	public void componentsKillFocus(Term term) {}

	protected String contextParameter(String name) {
		return (m_app == null || Beans.isDesignTime()) ? "" : m_dialog.contextParameter(name);
	}

	public long contextParamLong(String name) {
		return Long.parseLong(contextParameter(name));
	}

	public BlobTerm createBlobTerm(int dataType, TermParams params) {
		BlobTerm term = new BlobTerm(this, dataType, params);
		addTerm(term, params);
		return term;
	}

	public ComboBoxTerm createBufferedComboTerm(TermParams params) {
		ComboBoxTerm term = new BufferedComboBoxTerm(this, params);
		addTerm(term, params);
		return term;
	}

	public CheckListTerm createCheckListTerm(int dataType, TermParams params) {
		CheckListTerm term = new CheckListTerm(this, dataType, params);
		addTerm(term, params);
		return term;
	}

	public CheckTerm createCheckTerm(int dataType, TermParams params) {
		CheckTerm term = new CheckTerm(this, dataType, params);
		addTerm(term, params);
		return term;
	}

	public ComboBoxTerm createComboTerm(int dataType, TermParams params) {
		ComboBoxTerm term = new ComboBoxTerm(this, dataType, params);
		addTerm(term, params);
		return term;
	}

	public ImageTerm createImageTerm(int dataType, TermParams params, String previewImageField) {
		ImageTerm term = new ImageTerm(this, dataType, params);
		term.m_previewDbField = previewImageField;
		addTerm(term, params);
		return term;
	}

	public ListTerm createListTerm(int dataType, TermParams params) {
		ListTerm term = new ListTerm(this, dataType, params);
		addTerm(term, params);
		return term;
	}

	public MasterRadioTerm createMasterRadioTerm(int dataType, TermParams params) {
		MasterRadioTerm term = new MasterRadioTerm(this, dataType, params);
		addTerm(term, params);
		return term;
	}

	public RadioTerm createRadioTerm(int dataType, TermParams params) {
		RadioTerm term = new RadioTerm(this, dataType, params);
		addTerm(term, params);
		return term;
	}

	public TableTerm createTableTerm(int dataType, TermParams params) {
		TableTerm term = new TableTerm(this, dataType, params);
		addTerm(term, params);
		return term;

	}

	public TextAreaTerm createTextAreaTerm(TermParams params) {
		TextAreaTerm term = new TextAreaTerm(this, params);
		addTerm(term, params);
		return term;
	}

	public TextTerm createTextTerm(int dataType, TermParams params) {
		TextTerm term = new TextTerm(this, dataType, params);
		addTerm(term, params);
		return term;

	}

	public boolean dbFieldHosted(String fieldName) {
		return m_termMap.get(fieldName) != null || m_dbFieldsHostedByTerms.contains(fieldName) || m_keyElems.get(fieldName) != null || m_wfields.get(fieldName) != null;
	}

	public void deleteDblclkedRowFromList(String gui_name) {
		if (checkEditing()) {
			JotyDataBuffer termBuf = termBuffer(gui_name);
			boolean lookedUpTerm = termBuf.m_textKey ? termBuf.recordLookup(termBuf.getKeyVal().m_strVal) : termBuf.recordLookup(termBuf.getKeyVal().getInteger());
			if (lookedUpTerm)
				((GridTerm) term(gui_name)).deleteTermRow();
		}
	}

	String descrByData(LiteralStruct literalStruct, int itemData) {
		Integer posIdx = literalStruct.m_descrReverseMap.get(itemData);
		if (posIdx != null)
			return literalStruct.m_descrArray.get(posIdx).descr;
		else
			return "";
	}

	public String dialogClassName() {
		return getDialog().getClass().getName();
	}

	boolean dialogContainment() {
		return getParent() instanceof TabbedPane;
	}

	boolean doEndDialog() {
		return true;
	}

	@Override
	protected void doGuiDataExch(boolean store) {
		if (m_targetPanel != null) {
			m_targetPanel.doGuiDataExch(store);
			return;
		}
		super.doGuiDataExch(store);
		injectedDialog();
		ListeningState listeningState = setPanelActionListeningOff();
		for (Term term : m_terms)
			if (termExchangable(term))
				term.guiDataExch(store);
		restorePanelActionListening(listeningState);
	}

	boolean editability() {
		return true;
	}

	public void enableComponents(boolean bState) {
		if (m_targetPanel != null) {
			m_targetPanel.enableComponents(bState);
			return;
		}
		boolean docIdentified = injectedDialog().documentIdentified();
		boolean basicallyEditable = basicallyEditable();
		boolean editability = editability();
		for (Term term : m_terms)
			term.enableComponent(getEnablingContextTruth(term), bState, docIdentified, basicallyEditable, editability);
		for (Term term : m_terms)
			term.enablingDependentComponents(bState);
		relatedEnable(bState);
		for (Term term : m_terms)
			term.checkSelection();
	}

	protected void enabledAsDetail(String termName) {
		term(termName).setEnabledAsDetail();
	}

	protected boolean getEnablingContextTruth(Term term) {
		return true;
	}

	public GridManager getGridManager() {
		return null;
	}

	protected long getSelectedValue() {
		return m_app.m_justSelectedValue;
	}

	protected Object getSource(ActionEvent e) {
		return e == null ? null : e.getSource();
	}

	public DataAccessPanel getTargetPanel() {
		return m_targetPanel;
	}

	public WrappedField getWField(String fieldName) {
		return m_wfields.get(fieldName);
	}

	public CellDescriptor gridCellDescriptor(String termOrFieldName) {
		return gridCellDescriptor(termOrFieldName, false);
	}

	public CellDescriptor gridCellDescriptor(String termOrFieldName, boolean silent) {
		Term term = m_termMap.get(termOrFieldName);
		CellDescriptor retVal = m_gridRowDescriptor.get(term == null ? termOrFieldName : term.m_dbFieldName);
		if (retVal == null)
			if (Beans.isDesignTime())
				return m_gridRowDescriptor.new CellDescriptor();
			else if (!silent)
				m_app.JotyMsg(this, "May be the term or field  '" + termOrFieldName + "' has not yet been added to the master grid !");
		return retVal;
	}

	public GridTerm gridTerm(String termName) {
		return (GridTerm) term(termName);
	}

	public boolean idFieldIsHostedByTerm() {
		return m_IdFieldElemIdx >= 0;
	}

	public ImageTerm imageTerm(String termName) {
		return (ImageTerm) term(termName);
	}

	public void implementDependency(String termName) {
		if (m_currentActorName != null)
			term(m_currentActorName).addDependentTerm(termName);
	}

	@Override
	public boolean init() {
		super.init();
		instanceComponents();
		m_storing = false;
		for (Term term : m_terms) {
			if (m_termsInBold)
				setBold(term);
			term.init();
		}
		clearTerms();
		if (m_gridManager != null)
			m_gridManager.init();
		return true;
	}

	void instanceComponents() {
		for (Term term : m_terms)
			term.preInit(this);
	}

	public boolean isListeningForActions() {
		return m_listenForActions;
	}

	boolean isLockedAnyWay(Term term) {
		return term.isLockedAnyway();
	}

	protected WrappedField keyElem(int index) {
		return m_keyElems.get(index);
	}

	protected WrappedField keyElem(String keyName) {
		WrappedField retVal = m_keyElems.get(keyName);
		if (retVal == null)
			m_app.JotyMsg(this, "No key elem found for field '" + keyName + "'");
		return retVal;
	}

	protected boolean keysRefsOnDialogAreTobeSet() {
		return true;
	}

	public ListTerm listTerm(String termName) {
		return (ListTerm) term(termName);
	}

	void manageTermsConsistence() {
		for (Term term : m_terms)
			term.manageTermConsistence();
	}

	public void notifyEditingAction(ActionEvent e) {
		if (injectedDialog().m_listenForPanelActions) {
			if (!m_dirty) {
				injectedDialog().showDirtyEffect();
				m_dirty = true;
			}
		}
	}

	public void notifyJotyDesignError(JComponent component, String text) {
		notifyJotyDesignError(component, text, false);
	}
	
	
	/**
	 * If it is the design time the method writes the specified text on the
	 * design log file (and optionally it highlights the component ) else
	 * present a verbose dialog to the user.
	 * 
	 * @param component the component to be highlighted
	 * @param text
	 *            the text to append to the file content.
	 * @param operativeOnly
	 */
	public void notifyJotyDesignError(JComponent component, String text, boolean operativeOnly) {
		Term term = null;
		if (component instanceof TermEnclosable)
			term = ((TermEnclosable) component).getTerm();
		String msg = component.getClass().getSimpleName();
		if (term != null && term.m_name != null)
			msg += " " + term.m_name;
		msg += " : " + text;
		if (Beans.isDesignTime()) {
			if (!operativeOnly) {
				component.setBackground(Color.RED);
				Logger.appendToLog(m_app.m_JotyDesignLog, msg, false, null, true);
			}
		} else
			m_app.JotyMsg(this, msg);
	}

	void onChecksChange(int clickedChkboxID) {}

	void onOK() {
		if (doEndDialog())
			m_dialog.onOK();
	}

	void onRadiosChange(RadioButton radioButton) {
		RadioTerm term = radioButton.m_term;
		MasterRadioTerm masterTerm = (MasterRadioTerm) (term.m_master == null ? term : term.m_master);
		masterTerm.m_iVal = term.m_storedValue;
		masterTerm.onRadiosChange();
	}

	public MasterRadioTerm radioMasterTerm(String termName) {
		return (MasterRadioTerm) term(termName);
	}

	public RadioTerm radioTerm(String termName) {
		return (RadioTerm) term(termName);
	}

	protected void relatedEnable(boolean generalEnabling) {}

	public void resetDirtyStatus() {
		m_dirty = false;
		for (Term term : m_terms)
			term.resetDirtyStatus();
		if (m_targetPanel != null)
			m_targetPanel.resetDirtyStatus();
	}

	public void restorePanelActionListening(ListeningState listeningState) {
		injectedDialog().m_listenForPanelActions = listeningState.dialogState;
		m_listenForActions = listeningState.panelState;
	}

	protected void setAsInsidePanel(String gui_name) {
		term(gui_name).setAsControlTerm();
		term(gui_name).m_mustRemainEnabled = true;
	}

	void setBold(Term term) {
		JComponent component = term.getComponent();
		Font font = new Font("Verdana", Font.BOLD, 11);
		component.setFont(font);
	}

	public void setContextParam(String varName, JotyDate valueExpr) {
		if (!Beans.isDesignTime())
			m_dialog.setContextParam(varName, valueExpr);
	}

	public void setContextParam(String varName, long valueExpr) {
		if (!Beans.isDesignTime())
			m_dialog.setContextParam(varName, valueExpr);
	}

	public void setContextParam(String varName, String valueExpr) {
		if (!Beans.isDesignTime())
			m_dialog.setContextParam(varName, valueExpr);
	}

	void setDependence(String actorTermName) {
		setDependence(actorTermName, true);
	}

	/**
	 * Starts or ends a definition session of the state enabling dependency.
	 * 
	 * @param actorTermName
	 *            if null the ongoing session stops, if not null the subsequent
	 *            Term instances will result to be dependent on the term having the
	 *            name here specified.
	 * @see Factory
	 */
	void setDependence(String actorTermName, boolean direct) {
		m_currentDependenceDirection = direct;
		if (actorTermName != null)
			term(actorTermName).m_dependenceDirectness = direct;
		m_currentActorName = actorTermName;
	}

	void setDescr(DescrTerm term, LiteralStruct literalStruct, boolean loadSetIfAvailable) {
		term.m_literalStruct = literalStruct;
		term.m_reloadNeeded = true;
		term.structuredInit(loadSetIfAvailable);
	}

	public void setDescr(String termName, LiteralStruct literalStruct) {
		setDescr(termName, literalStruct, null);
	}

	protected void setDescr(String termName, LiteralStruct literalStruct, String secondaryTerm) {
		setDescr(termName, literalStruct, secondaryTerm, true);
	}

	void setDescr(String termName, LiteralStruct literalStruct, String secondaryTerm, boolean loadSetIfAvailable) {
		Term term = m_termMap.get(termName);
		if (term != null && term instanceof DescrTerm)
			setDescr((DescrTerm) term, literalStruct, loadSetIfAvailable);
		if (secondaryTerm != null) {
			term = m_termMap.get(secondaryTerm);
			if (term != null)
				term.manageAsRelated(literalStruct, loadSetIfAvailable);
		}
	}

	void setLockedAnyWay(String gui_name) {
		setLockedAnyWay(term(gui_name));
	}

	void setLockedAnyWay(Term term) {
		if (term != null)
			term.m_isToBeLockedAnyWay = true;
	}

	public ListeningState setPanelActionListeningOff() {
		ListeningState listeningState = new ListeningState();
		listeningState.panelState = m_listenForActions;
		listeningState.dialogState = injectedDialog().m_listenForPanelActions;
		m_listenForActions = false;
		injectedDialog().m_listenForPanelActions = false;
		return listeningState;
	}

	/**
	 * 
	 * @param termName
	 */
	public void setRadioAsActor(String termName) {
		((RadioTerm) term(termName)).m_isActor = true;
	}

	protected void setRemainEnabled(String termName) {
		setRemainEnabled(term(termName));
	}

	void setRemainEnabled(Term term) {
		term.m_mustRemainEnabled = true;
	}

	public void setTargetPanel(DataAccessPanel panel) {
		m_targetPanel = panel;
	}

	/**
	 * It allows the definition of the ability of a single datum component to
	 * update a cell in the buffer of a {@code GridTerm} object.
	 * 
	 * @see Term#m_drivenBufferTerm
	 * @see GridTerm
	 * @see TextTerm
	 */ 
	protected void setTermAsDriverOf(String drivingTermName, String drivenTermName, String fieldToDrive) {
		Term drivenTerm = term(drivenTermName);
		if (drivenTerm instanceof GridTerm)
			term(drivingTermName).m_drivenBufferTerm = (GridTerm) drivenTerm;
		term(drivingTermName).m_fieldToDrive = fieldToDrive;
	}

	void showAllComponents(boolean truth) {
		for (Component cmp : getComponents())
			if (cmp instanceof JComponent)
				cmp.setVisible(truth);
	}

	void showComponent(String gui_Name) {
		showComponent(gui_Name, true);
	}

	void showComponent(String gui_name, boolean truth) {
		showComponent(gui_name, truth, null);
	}

	void showComponent(String termName, boolean truth, JLabel label) {
		Term term = term(termName);
		term.show(truth);
		term.enable(injectedDialog().m_editOrNew_command);
		if (label != null)
			label.setVisible(truth);
	}

	public GridTerm GridTerm(String termName) {
		return (GridTerm) term(termName);
	}

	protected void synchroCombo(String master, String slave) {
		if (!m_listenForActions || m_synchroTermActing)
			return;
		m_synchroTermActing = true;
		comboTerm(slave).selectItem(comboTerm(master).selectionData());
		m_synchroTermActing = false;
	}

	public TableTerm tableTerm(String termName) {
		return (TableTerm) term(termName);
	}

	public Term term(String termName) {
		if (termName == null)
			return null;
		else {
			Term retVal = m_termMap.get(termName);
			if (retVal == null && !Beans.isDesignTime())
				Application.warningMsg(String.format("Term '%1$s' not found !", termName));
			return retVal;
		}
	}

	public JotyDataBuffer termBuffer(String termName) {
		GridTerm gridTerm = (GridTerm) term(termName);
		JotyDataBuffer dataBuffer = null;
		if (gridTerm != null) {
			if (gridTerm.m_dataBuffer == null) {
				if (gridTerm.m_slave && gridTerm.m_mainIterator >= 0) {
					gridTerm.checkSlaveTermBuffer();
					gridTerm.m_dataBuffer = gridTerm.slaveBuffer();
				}
			}
			dataBuffer = gridTerm.m_dataBuffer;
		}
		m_app.ASSERT(dataBuffer != null);
		return dataBuffer;
	}

	protected boolean termExchangable(Term term) {
		return true;
	}

	public TextTerm textTerm(String termName) {
		return (TextTerm) term(termName);
	}

	protected void updateDrivenBuffers() {
		for (Term term : m_terms)
			term.updateDrivenBuffer();
	}

	protected boolean validateComponents() {
		boolean retVal = true;
		if (m_targetPanel != null)
			return m_targetPanel.validateComponents();
		if (!m_validatingComponents) {
			m_validatingComponents = true;
			for (Term term : m_terms)
				if (!term.validate()) {
					retVal = false;
					break;
				}
			m_validatingComponents = false;
		}
		return retVal;
	}

	WrappedField wrongWField() {
		Application.warningMsg("Joty : failure");
		return new WField(m_app);
	}
	
	

}
