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

import java.awt.Component;
import java.awt.dnd.*;
import java.beans.Beans;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.TransferHandler;

import org.joty.access.PostStatement;
import org.joty.app.*;
import org.joty.app.LiteralsCollection.DescrStruct;
import org.joty.common.CaselessStringKeyMap;
import org.joty.common.JotyTypes;
import org.joty.common.ApplMessenger;
import org.joty.common.BasicPostStatement;
import org.joty.common.Utilities;
import org.joty.common.Utilities.Stocker;
import org.joty.data.BasicJotyCursor;
import org.joty.data.WrappedField;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.app.Application.LiteralStruct;
import org.joty.workstation.data.JotyDataBuffer;
import org.joty.workstation.data.WField;
import org.joty.workstation.data.WResultSet;

/**
 * Extends its ancestor with the ability to access the database directly,
 * through the jdbc layer, or in web mode, via the {@code WebClient} instance.
 * <p>
 * In both way, the layer that is faced by the class for accessing data is made,
 * mainly, by the JotyDb and the WResultSet classes of the org.joty.data
 * package.
 * <p>
 * The definition of the updateable set and of the query set can be defined in
 * the containing dialog class or the Accessor class can be the holder of these
 * definitions. Similar availability of implementation exists for
 * {@code GridTerm} instances, instantiated in this class, that themselves
 * need to have updateable set and of the query set defined: this class offer
 * methods for this purpose or again these definitions can be implemented in the
 * Accessor class.
 * <p>
 * Some methods of the class perform the interaction with a WResultSet
 * object either, in web mode, to prepare statement to forward to the WebClient
 * and to collect from data from it, or to exchange data of the instances of the
 * {@code Term} class with the corresponding fields in the WResultSet object
 * that will participate in a jdbc access.
 * <p>
 * Other methods offer the definition of the keys of the underlying updateable
 * set and other methods the definition of the relation between fields in the
 * panel and fields elsewhere located.
 * <p>
 * Then, application level permissions can be specified, for the various roles
 * in respect of the object accessibility for them.
 * <p>
 * Two main accessor method for writing and for reading the managed data to and
 * from the database are coordinated by the container {@code DataAccessDialog}
 * instance.
 * <p>
 * The class, furthermore, provides methods for managing updating needs among
 * the various {@code DataAccessDialog} objects currently opened in the
 * application, when these dialog shared the same data typically in referencing
 * activity: these methods participates in the publishers-subscribers scenario
 * that the Joty Framework implement for this purpose.
 *
 * @see org.joty.workstation.data.JotyDB
 * @see WResultSet
 * @see org.joty.access.Accessor
 * 
 */
public class DataAccessPanel extends TermContainerPanel {

	public interface ActionOnRowInterface {
		void doAction(Term srcTerm, int column);
	}

	public enum ButtonBehavior {
		free, editing, notEditing, notEditingIdentified
	}

	public class ButtonDescriptor extends JButton {
		JotyButton m_button;
		ButtonBehavior m_behavior;

		public ButtonDescriptor(JotyButton button, ButtonBehavior behavior) {
			super();
			m_button = button;
			m_behavior = behavior;
		}
	}

	public interface DefinedInsertMethodInterface {
		boolean method(WResultSet rs, DataAccessPanel panel, int i);
	}

	public interface DefinedSetMethodInterface {
		void method(WResultSet rs, DataAccessPanel panel);
	}

	public interface GetDelStmntInterface {
		boolean method(DataAccessPanel panel);
	}

	public interface GetWhereClauseInterface {
		String method(DataAccessPanel panel, boolean usedInTransaction);
	}

	protected enum Permission {
		no_access, read, readWrite, readWriteAdd, all
	}

	/**
	 * Used by the {@code ListTerm} class to allow custom rendering, basing
	 * either on the WResultSet or on the JotyDataBuffer objects.
	 */
	public interface RenderRowMethodInterface {
		String method(WResultSet rs, JotyDataBuffer buffer);
	}

	public boolean m_operative;
	public boolean m_growing;
	public boolean m_isNewRec;
	public boolean m_isNewRecOnIdle;
	public boolean m_cascadeUpdate;
	public boolean m_isInTabbedPanel;
	public String m_contextFilter;
	public Vector<String> m_relationElems;

	public String m_mainDataTable;
	public String m_definedQuery;
	public String m_finalQuery;

	public String m_sortExpr;
	public boolean m_dataMaster;

	public boolean m_isUnDeletable;
	public Vector<WrappedField> m_keyElemDefaults;
	public boolean m_askedToLoad;
	public boolean m_preIdGeneration;
	public boolean m_blobManaging;
	public String m_seq_name;
	public Permission m_permission = Permission.no_access;
	protected boolean m_controllerUpdateRequested;

	protected boolean m_preFiltered;
	public long m_identifyingID;
	protected BuildDetailsDialogAdapter m_buildDetailsHandler;
	public Vector<JotyDialog> m_dependentDialogs;
	public Stocker m_smallBlobs;
	public boolean m_clearing;
	protected Vector<ButtonDescriptor> m_buttons;
	protected ButtonBehavior m_currentButtonBehavior;
	protected boolean m_mainlyDbFree;
	private boolean m_readOnly;
	private boolean m_modifyOnly;
	private boolean m_loadDataSession;
	private boolean m_idFieldAutoIncrement;

	private boolean m_autoIncrementByAddNew;
	private CaselessStringKeyMap<String> m_relationElemsMap,
			m_relationElemsReverseMap;
	private String m_validationExpr;

	private CaselessStringKeyMap<Permission> m_rolesPerm;;

	private CaselessStringKeyMap<WrappedField> m_cachedFromDialogRelated;

	private JotyDialog m_detailsDialog;;

	private String m_extractedWhereClause;

	private boolean m_reloadBecauseOfPublishing;

	private boolean m_accessorMode;

	protected boolean m_localAccessor;

	protected boolean m_loadingData;

	protected BasicJotyCursor m_descriptorOnLoad;

	protected Common m_common;

	private Stocker m_keyFieldsForInsert = Utilities.m_me.new Stocker();

	private boolean m_defaultsInitialized;

	private boolean m_doingNew;

	HashSet<String> m_publishersSet;

	private boolean m_isPublisher;
	private Vector<WrappedField> m_delayedWfields;

	public DataAccessPanel() {
		super();
		m_common = (Common) ((ApplMessenger) m_app).getCommon();

		m_relationElems = new Vector<String>();
		m_gridManager = null;

		m_IdFieldName = "";
		m_identifyingID = 0;
		m_initializing = true;
		m_isNewRec = false;
		m_growing = true;
		m_isUnDeletable = false;
		m_recSensingCtrlIdx = -1;
		m_IdFieldElemIdx = -1;
		m_listRowHeader = "";
		m_sortExpr = "";
		m_cascadeUpdate = false;
		m_operative = true;
		m_keyElemDefaults = new Vector<WrappedField>();
		m_validationExpr = null;
		m_clearing = false;
		m_currentButtonBehavior = ButtonBehavior.free;

		m_relationElemsMap = new CaselessStringKeyMap<String>(m_app);
		m_relationElemsReverseMap = new CaselessStringKeyMap<String>(m_app);
		m_rolesPerm = new CaselessStringKeyMap<Permission>(m_app);
		m_cachedFromDialogRelated = new CaselessStringKeyMap<WrappedField>(m_app);
		m_dependentDialogs = new Vector<JotyDialog>();
		m_smallBlobs = Utilities.m_me.new Stocker();
		m_buttons = new Vector<ButtonDescriptor>();
		m_publishersSet = new HashSet<String>();
		m_delayedWfields = new Vector<WrappedField>();
		if (!Beans.isDesignTime()) {
			m_isInTabbedPanel = m_app.m_definingDialog != null && m_app.m_definingDialog instanceof MultiPanelDialog;
			if (m_isInTabbedPanel)
				m_dataMaster = ((MultiPanelDialog) m_app.m_definingDialog).m_dataPanels.size() == 0;
		}
		m_askedToLoad = false;
		m_preFiltered = false;
		m_blobManaging = false;
		if (!Beans.isDesignTime()) {
			m_idFieldAutoIncrement = m_common.m_idFieldAutoIncrement;
			m_autoIncrementByAddNew = m_common.m_autoIncrementByAddNew;
			m_seq_name = m_common.m_seq_name;
		}
		m_accessorMode = getDialog().m_accessorMode;
		m_localAccessor = m_app.m_accessor != null;
	}

	protected boolean accessIsAllowed() {
		if (!getMaxPermission())
			return false;
		if (m_permission.compareTo(Permission.no_access) == 0 && !m_isInTabbedPanel) {
			Application.langWarningMsg("AccessDenied");
			return false;
		}
		return true;
	}

	public BasicPostStatement accessMethodPostStatement(String method, Integer returnedValuePos, Integer returnedValuesQty) {
		return m_app.accessorMethodPostStatement(method, returnedValuePos, returnedValuesQty);
	}

	private boolean accessOutOfContextData() {
		return accessOutOfContextData(true);
	}

	boolean accessOutOfContextData(boolean Store) {
		return true;
	}

	@Override
	public Component add(Component comp) {
		if (comp instanceof JotyButton)
			m_buttons.add(new ButtonDescriptor((JotyButton) comp, m_currentButtonBehavior));
		return super.add(comp);
	}

	@Override
	public void addIntegerKeyElem(String fieldName) {
		addIntegerKeyElem(fieldName, false);
	}

	public void addIntegerKeyElem(String fieldName, boolean isTheIdField) {
		addKeyElem(fieldName, JotyTypes._dbDrivenInteger, isTheIdField, null);
	}

	public WrappedField addKeyElem(String fieldName, int dataType, boolean contextIdentifying, WrappedField defaultVal) {
		if (Beans.isDesignTime())
			return new WField(m_app);
		WrappedField wfield = addKeyElem(fieldName, dataType);
		if (contextIdentifying)
			if (m_IdFieldName.length() > 0)
				m_app.JotyMsg(this, "ID key term already specified ");
			else
				m_IdFieldName = fieldName;
		if (defaultVal != null) {
			defaultVal.m_dbFieldName = fieldName;
			m_keyElemDefaults.add(defaultVal);
		}
		return wfield;
	}

	public void addOutParam(BasicPostStatement postStatement, String name, int type) {
		postStatement.addOutParam(name, type, m_app.returnedValuesAvailablePos());
		m_app.incrementRetValIndex();
	}

	protected void addStrKeyElem(String fieldName, String defaultVal, boolean isTheIdField) {
		addKeyElem(fieldName, JotyTypes._text, isTheIdField, defaultVal != null ? defaultStrValWField(defaultVal) : null);
	}

	protected void onDataLoaded() {}

	public void askToLoad() {
		m_askedToLoad = true;
	}

	void aspectCascadeUpdate() {
		if (!m_cascadeUpdate) {
			m_cascadeUpdate = true;
			for (Term term : m_terms)
				term.updateAspect();
			m_cascadeUpdate = false;
			resetUpdateActorsState();
		}
	}

	@Override
	protected boolean basicallyEditable() {
		return injectedDialog().m_new_command || m_isNewRec;
	}

	public void beginEditing() {
		m_isNewRecOnIdle = m_isNewRec;
	}

	protected String buildTabsRelatingStrKey() {
		return "";
	}

	void calculate() {

	}

	public void checkAndSetLook() {
		checkComponentsRendering();
		enableComponents(injectedDialog().m_editOrNew_command);
		if (!m_isInTabbedPanel)
			m_dialog.updateCommandButtons(!injectedDialog().m_new_command && !injectedDialog().m_editOrNew_command);
	}

	protected void checkControllerInit(WResultSet rs) {}

	@Override
	protected boolean checkEditing() {
		boolean retVal = false;
		if (m_permission.compareTo(Permission.readWrite) >= 0) {
			if (isEditing())
				retVal = true;
			else
				Application.langWarningMsg("EnterEdit");
		} else
			Application.langWarningMsg("AccessDenied");
		return retVal;
	}

	protected void checkForControllerInitialization() {}

	protected boolean checkForIdentifyingId() {
		boolean retVal = true;
		if (m_idFieldAutoIncrement || m_app.m_webMode && m_preIdGeneration) {
			if (m_app.m_webMode) {
				String returnedValue = m_app.m_webClient.getReturnedValue(keyElem(m_IdFieldName).m_posIndexAsReturningValue - 1);
				m_identifyingID = returnedValue == null ? 0 : Long.parseLong(returnedValue);
			} else
				m_identifyingID = m_app.m_db.getAutoIdVal();
			if (m_identifyingID > 0) {
				setMainData(null, m_identifyingID);
				keyElem(m_IdFieldName).m_delayed = false;
			} else {
				m_app.JotyMsg(this, String.format("dbms ID generation was expected but it didn't occur: check definitions on db or joty declarations in class %1$s " +
															"if Auto-incrementing is the case otherwise look at the code in %2$s !", 
														getClass().getName(), m_app.m_dbManager.getClass().getName()));
				retVal = false;
			}
		}
		return retVal;
	}

	protected void checkForPublishing() {
		if (m_isPublisher || m_targetPanel != null && m_targetPanel.m_isPublisher)
			publishThisDialog();
	}

	protected void checkForTermsNewlyGeneratedValues() {
		int retValIndex;
		for (Term term : m_terms) {
			retValIndex = term.m_posIndexAsReturningValue - 1;
			if (retValIndex >= 0) 
				term.setValFromDbSubmittedExpr(m_app.m_webMode ? 
													m_app.m_webClient.getReturnedValue(retValIndex) : 
													m_app.m_returnedValues.get(retValIndex));
		}
	}

	public boolean checkHasDone() {
		return true;
	}

	public void checkPublishers() {
		if (m_reloadBecauseOfPublishing) {
			doReloadBecauseOfPublishing();
			m_reloadBecauseOfPublishing = false;
		}
		for (Term term : m_terms)
			term.checkPublishers();
	}

	void clearComponents() {
		m_defaultsInitialized = false;
		for (Term term : m_terms)
			if (!term.isAControlTerm())
				if (term.isDataComplement()) {
					if (!m_doingNew)
						term.reset();
				} else
					term.clearComponent();
	}

	void clearMainData() {
		for (int i = 0; i < keyElemsSize(); i++)
			keyElem(i).clear(false);
	}

	@Override
	boolean clearNeeded() {
		return m_initializing;
	}

	protected boolean clientValidation() {
		return true;
	}

	void closeDependentDialogs() {
		Vector<JotyDialog> auxVector = new Vector<JotyDialog>();
		for (JotyDialog dialog : m_dependentDialogs)
			auxVector.add(dialog);
		for (JotyDialog dialog : auxVector) {
			if (dialog.m_btnCancel.isVisible())
				dialog.onCancel();
			dialog.close();
		}
		auxVector.removeAllElements();
	}

	boolean compoundDocument() {
		return injectedDialog().compoundDocument();
	}

	public void costraintViolationMsgOnDelete() {
		Application.langWarningMsg("DbmsConstrViolationDelete");
	}

	public void costraintViolationMsgOnUpdate() {
		Application.langWarningMsg("DbmsConstrViolationUpdate");
	}

	protected boolean creationTrigger() { return true;}

	boolean dataToBeLoaded() {
		return m_targetPanel == null;
	}

	protected WrappedField defaultStrValWField(String defaultVal) {
		WrappedField defaultValWField = new WField(m_app);
		defaultValWField.clear();
		defaultValWField.m_strVal = defaultVal;
		return defaultValWField;
	}

	/**
	 * To be overridden in oder to add contribution to the composition of the
	 * grid.
	 * <p>
	 * The override must include a call to the parent implementation.
	 * 
	 * @see CriteriaPanel#defineGrid
	 */ 
	public void defineGrid() {
		if (m_dialog instanceof SearcherMultiPanelDialog && isControllerMaster()) {
			SearcherMultiPanelDialog dlg = (SearcherMultiPanelDialog) m_dialog;
			dlg.m_searcherPanel.m_criteriaPanel.defineGrid(this);
		}
	}

	public void defRelationElement(String fieldName, String relatedFieldName) {
		defRelationElement(fieldName, relatedFieldName, false, null);
	}

	public void defRelationElement(String fieldName, String relatedFieldName, boolean fromCallContext) {
		defRelationElement(fieldName, relatedFieldName, fromCallContext, null);
	}

	public void defRelationElement(String fieldName, String relatedFieldName, boolean fromCallContext, String relatedDialogClass) {
		if (Beans.isDesignTime())
			return;
		m_relationElems.add(fieldName);
		if (relatedDialogClass == null && !fromCallContext) {
			m_relationElemsMap.put(fieldName, relatedFieldName);
			m_relationElemsReverseMap.put(relatedFieldName, fieldName);
		} else {
			WrappedField sourceWField = (fromCallContext ? getDialog().m_callContext.m_caller : m_app.getOpenedDialog(relatedDialogClass)).keyElem(relatedFieldName);
			if (sourceWField == null)
				m_app.JotyMsg(this, "No keyElem on field '" + relatedFieldName + "' found in dialog '" + relatedDialogClass + "' !");
			else
				m_cachedFromDialogRelated.put(fieldName, sourceWField);
		}
	}

	String deleteExtension() {
		return "";
	}

	boolean deletionCheck(boolean preCondition, String msg) {
		if (!preCondition)
			Application.warningMsg(msg);
		return preCondition;
	}

	public void deletionEffects() {
		if (m_targetPanel != null)
			m_targetPanel.deletionEffects();
		else {
			m_initializing = true;
			ensureClearTerms();
			m_initializing = false;
			GridManager gridManager = getGridManager();
			if (gridManager != null && gridManager.getRowQty() > 0)
				gridManager.removeRow();
			guiDataExch(false);
			WResultSet rs = null;
			if (!dialogGridManagerExists()) {
				rs = new WResultSet(null, m_finalQuery);
				if (rs.open(m_queryDefPostStatement)) {
					m_isNewRec = rs.isEOF();
					rs.close();
				}
			}
			deletionEpilogue();
			checkAndSetLook();
		}
	}

	void deletionEpilogue() {}

	protected boolean dialogGridManagerExists() {
		return injectedDialog().gridManagerExists();
	}

	private void doAddNew(WResultSet rs) {
		rs.addNew();
		for (WrappedField wfield : m_keyElemDefaults)
			rs.setValue(wfield, false);
		for (Term term : m_terms)
			if (term.m_delayed)
				rs.setValue(term);
		if (m_preIdGeneration || m_idFieldAutoIncrement)
			setId(rs);
		calculate();
		setMainData(rs, m_identifyingID);
	}

	protected boolean documentIdentified() {
		return m_targetPanel == null ? !m_isNewRec : m_targetPanel.documentIdentified();
	}

	public boolean doDeletion() {
		return doDeletion(false);
	}

	public boolean doDeletion(boolean wideAction) {
		setReloadNeeded();
		boolean retVal = true;
		for (Term term : m_terms) {
			if (!term.isAControlTerm() && !term.isDataComplement()) {
				if (!term.innerClearData()) {
					retVal = false;
					break;
				}
			}
		} 
		if (retVal) {
			if (m_definedQuery != null || m_accessorMode) {
				long formKey = idFromGridRow();
				String whereClause = getWhereClause(formKey, wideAction);
				if (whereClause.isEmpty()) {
					m_app.JotyMsg(this, "Key not define for deletion !");
					retVal = false;
				} else 
					retVal = buildAndExecDeletion("Where " +  whereClause);
			}
		}
		return retVal;
	}

	protected boolean buildAndExecDeletion(String whereSection) {
		String delSqlStmt;
		delSqlStmt = String.format("Delete from %1$s %2$s", m_app.codedTabName(m_mainDataTable), whereSection);
		String extension = deleteExtension();
		if (extension.length() > 0)
			delSqlStmt += " AND " + extension;
		return m_app.m_db.executeSQL(delSqlStmt, null, createContextPostStatement());
	}
	
	void doEdit() {}

	@Override
	boolean doEndDialog() {
		return !m_isInTabbedPanel;
	}

	protected void doNew() {
		m_isNewRec = true;
		setReloadNeeded();
		m_doingNew = true;
		ensureClearTerms();
		m_doingNew = false;
		setTermsDefaults();
	}

	protected boolean doneWithData() {
		boolean retVal = false;
		m_loadDataSession = true;
		retVal = dataToBeLoaded() && loadData();
		m_loadDataSession = false;
		return retVal;
	}

	protected void doReloadBecauseOfPublishing() {
		reLoadData();
	}

	protected boolean doUpdate(boolean isNewRecord, WResultSet rs, boolean localValidation, boolean andCloseRS) {
		boolean success;
		setNotBoundFields(rs);
		if (!m_app.m_webMode)
			checkForTermsNewlyGeneratedValues();
		success = storeWFieldsData(rs);
		if (success && localValidation)
			success = clientValidation();
		if (success)
			success = validation();
		if (success) {
			success = rs.update(isNewRecord, m_idFieldAutoIncrement, createContextPostStatement());
			if (success && !m_app.m_webMode && isNewRecord)
				success = checkForIdentifyingId();
		}
		if (andCloseRS)
			rs.close();
		if (success && m_isNewRec)
			success = creationTrigger();
		return success;
	}

	/**
	 *  Must contain statements rolling back the effects of those statements not manageable by the transaction system
	 */
	protected void nonManagedRollback() {
	}

	protected boolean edit() {
		if (m_isNewRec)
			setTermsDefaults();
		boolean retVal = false;
		if (dialogGridManagerExists()) {
			if (getGridManager() == null) {
				m_app.JotyMsg(this, Application.MSG_ListNotDef);
				return retVal;
			}
			getGridManager().storeSelection();
		}
		retVal = isEditable();
		if (retVal) {
			injectedDialog().m_listenForPanelActions = true;
			doEdit();
		} else
			m_dialog.onCancel();
		return retVal;
	}

	@Override
	boolean editability() {
		return isEditing();
	}

	boolean editingCheck(boolean preCondition, String msg) {
		if (!preCondition)
			Application.warningMsg(msg);
		return preCondition;
	}

	@Override
	public void enableComponents(boolean editing) {
		super.enableComponents(editing);
		boolean enable = false;
		for (ButtonDescriptor buttonDescr : m_buttons) {
			switch (buttonDescr.m_behavior) {
				case editing:
					enable = (documentIdentified() || injectedDialog().m_new_command) ? editing : false;
					break;
				case notEditing:
					enable = !editing;
					break;
				case notEditingIdentified:
					enable = !editing && documentIdentified();
					break;
				case free:
					enable = true;
					break;
			}
			buttonDescr.m_button.setEnabled(enable);
		}
		for (Term term : m_terms)
			term.enableRelatedButtons();
	}

	protected void enableRole(String roleName, Permission permission) {
		m_rolesPerm.put(roleName, permission);
	}

	public void endEditing(boolean justSaved) {
		if (justSaved)
			checkForPublishing();
	}

	public String enrollThisDialog(String publisherDialogName) {
		String publisherFullName = m_app.getDialogClassFullName(publisherDialogName);
		HashSet<String> subscribersSet = m_app.m_refreshMap.get(publisherFullName);
		if (subscribersSet == null) {
			subscribersSet = new HashSet<String>();
			m_app.m_refreshMap.put(publisherFullName, subscribersSet);
		}
		if (!subscribersSet.contains(dialogClassName()))
			subscribersSet.add(dialogClassName());
		return publisherFullName;
	}

	void ensureClearTerms() {
		clearTerms(true);
		m_clearing = true;
		guiDataExch(false);
		m_clearing = false;
		clearComponents();
	}

	String entityDescription(int pos) {
		return "";
	}

	public long entityID() {
		return keyElem(m_IdFieldName).getInteger();
	}

	public String entityIdExpr(boolean usedInTransaction) {
		WrappedField idWField = keyElem(m_IdFieldName);
		return idWField.m_delayed && usedInTransaction ? m_app.m_webClient.getGenToken(idWField.m_posIndexAsReturningValue) : String.valueOf(idWField.getInteger());
	}

	protected boolean entityIdValuePending() {
		return keyElem(m_IdFieldName).m_delayed;
	}

	public boolean existController() {
		return m_targetPanel == null ? false : m_targetPanel.existController();
	}

	public void filterInit(WrappedField keyWField) {}

	protected boolean getAutoIncrementByAddNew() {
		return m_autoIncrementByAddNew;
	}

	@Override
	public GridManager getGridManager() {
		GridManager retVal = null;
		if (m_gridManager != null)
			retVal = m_gridManager;
		else if (m_isInTabbedPanel)
			retVal = injectedDialog().getGridManager(true);
		if (retVal == null && !Beans.isDesignTime())
			gridManagerGuide();
		return retVal;
	}

	public long getID(WResultSet rs) throws JotyException {
		long retID;
		m_preIdGeneration |= newIdManagement();
		if (m_preIdGeneration)
			if (m_isNewRec)
				retID = m_app.getNextId(this);
			else
				retID = rs.integerValue(m_IdFieldName);
		else if (idFieldIsHostedByTerm())
			retID = m_terms.get(m_IdFieldElemIdx).getInteger();
		else
			retID = -1;
		return retID;
	}

	protected boolean getIdFieldAutoIncrement() {
		return m_idFieldAutoIncrement;
	}

	long getMainSetSize() {
		return m_gridManager.getMainSetSize();
	}

	public boolean getMaxPermission() {
		boolean retVal = true;
		if (m_app.m_userRoles.size() > 0) {
			Permission gotPermission = null;
			for (String role : m_app.m_userRoles) {
				gotPermission = m_rolesPerm.get(role);
				if (gotPermission != null && gotPermission.compareTo(m_permission) > 0) {
					m_permission = gotPermission;
					if (m_permission == Permission.all)
						break;
				}
			}
		} else
			retVal = false;
		return retVal;
	}


	private Stocker getOpenForActionFields() {
		Stocker retObj = Utilities.m_me.new Stocker();
		for (Term term : m_terms)
			if (term.isDirty() && term.resultSetFieldName() != null)
				retObj.add(term.resultSetFieldName());
		m_keyFieldsForInsert.clear();
		if (m_isNewRec) {
			putActionFieldsNeededOnNew(m_keyFieldsForInsert);
			retObj.add(m_keyFieldsForInsert);
		}
		return retObj;
	}

	public String getWhereClause() {
		return getWhereClause(0);
	}

	String getWhereClause(long idKey) {
		return getWhereClause(idKey, false);
	}

	String getWhereClause(long idKey, boolean wideAction) {
		String retStr = "", addingClause;
		boolean preExisting = false;
		boolean addAnd;
		if (m_isInTabbedPanel || dialogGridManagerExists() || m_preFiltered || injectedDialog().m_editOrNew_command && !m_isNewRec || injectedDialog().m_is_deleting) {
			boolean keyElemIsKeyForPanel = false;
			for (WrappedField keyElem : m_keyElems.vector) {
				addingClause = "";
				addAnd = false;
				keyElemIsKeyForPanel = m_IdFieldName != null && m_IdFieldName.length() > 0 && keyElem.m_dbFieldName.compareToIgnoreCase(m_IdFieldName) == 0;
				if ((m_isInTabbedPanel || dialogGridManagerExists()) && keyElemIsKeyForPanel && !wideAction) {
					if (dialogGridManagerExists() && (m_blobManaging || 
							isAListDrivenSlave() || 
							injectedDialog().m_editOrNew_command && !injectedDialog().m_new_command || 
							injectedDialog().m_is_deleting)) {
						if (idKey == 0)
							idKey = idFromGridRow();
						if (listKeyIsText())
							addingClause = String.format("%1$s = '%2$s'", 
															m_IdFieldName, 
															Utilities.sqlEncoded(getGridManager().m_gridBuffer.getKeyVal(m_IdFieldName).m_strVal));
						else
							addingClause = String.format("%1$s = %2$d", m_IdFieldName, idKey);
						if (preExisting)
							addAnd = true;
						preExisting = true;
					}
				} else {
					if (!keyElemIsKeyForPanel || (!m_loadDataSession && !wideAction)) {
						if (keyElem.dataType() == JotyTypes._text) {
							if (keyElem.m_strVal == null)
								m_app.JotyMsg(this, "Key value for " + keyElem.m_dbFieldName + " is null !");
							else
								addingClause = String.format("%1$s = '%2$s'", keyElem.m_dbFieldName, Utilities.sqlEncoded(keyElem.m_strVal));
						} else
							addingClause = String.format("%1$s = %2$d ", keyElem.m_dbFieldName, keyElem.integerVal());
						if (preExisting)
							addAnd = true;
						preExisting = true;
					}
				}
				if (addAnd)
					retStr += " AND ";
				retStr += addingClause;
			}
		}
		int i = 0;
		for (String relElem : m_relationElems) {
			if (preExisting)
				retStr += " AND ";
			if (i == 0)
				preExisting = true;
			i++;
			WrappedField relWField = relatedWField(relElem);
			if (relWField == null)
				Application.warningMsg("Unexisting related field : " + relElem);
			else {
				if (relWField.dataType() == JotyTypes._text)
					addingClause = String.format("%1$s = '%2$s'", relElem, Utilities.sqlEncoded(relWField.m_strVal));
				else
					addingClause = String.format("%1$s = %2$d ", relElem, relWField.integerVal());
				retStr += addingClause;
			}
		}
		return retStr;
	}

	protected void gridManagerGuide() {}

	@Override
	public void guiDataExch(boolean store) {
		if (m_targetPanel != null) {
			m_targetPanel.guiDataExch(store);
			return;
		}
		if (!m_operative)
			return;
		if (store)
			m_identifyingID = 0;
		doGuiDataExch(store);
		if (!store && (!m_growing || !m_loadDataSession)) {
			aspectCascadeUpdate();
			updateController();
		}
	}

	long idFromGridRow() {
		return -1;
	}

	public String idGenenerationFilter() {
		return "";
	}

	@Override
	public boolean init() {
		if (!super.init())
			return false;
		if (!accessIsAllowed())
			return false;
		boolean lResult = true;
		for (Term term : m_terms) {
			if (term.m_isToBeLockedAnyWay)
				setLockedAnyWay(term);
			if (term instanceof ImageTerm) {
				ImageTerm imageTerm = (ImageTerm) term;
				if (imageTerm.m_previewBuffered)
					m_smallBlobs.add(imageTerm.m_previewDbField);
			}
		}
		GridManager gridManager = getGridManager();
		if (gridManager != null) {
			TransferHandler transferHandler = ((Table) gridManager.getListComponent()).m_jtable.getTransferHandler();
			if (transferHandler instanceof JotyTableTransferHandler) {
				JotyTableTransferHandler dataTransferHandler = (JotyTableTransferHandler) transferHandler;
				if (dataTransferHandler.m_managedDbTable == null)
					dataTransferHandler.setManagedDbTable(m_mainDataTable);
			}
		}
		if (m_selectorsTransferHandler != null)
			m_selectorsTransferHandler.setManagedDbTable(m_mainDataTable);
		lResult = doneWithData();
		if (lResult) {
			setTermsDefaults();
			setTermsOnContext();
		}
		guiDataExch(false);
		checkAndSetLook();
		m_growing = false;
		m_initializing = false;
		postInit();
		return lResult;
	}

	public long integerKeyElemVal(String fieldName) {
		long retVal = -1;
		WrappedField wfield = keyElemVal(fieldName);
		if (wfield != null)
			retVal = wfield.integerVal();
		return retVal;
	}

	public boolean invokeAccessMethod(BasicPostStatement postStatement) {
		return m_app.invokeAccessMethod(postStatement);
	}

	boolean isAListDrivenSlave() {
		return getGridManager() != null && m_gridManager == null;
	}

	protected boolean isControllerMaster() {
		return false;
	}

	boolean isDeletable() {
		return !m_isUnDeletable;
	}

	boolean isDirty() {
		for (Term term : m_terms)
			if (term.isDirty())
				return true;
		return false;
	}

	boolean isEditable() {
		return true;
	}

	private boolean isEditing() {
		return injectedDialog().m_editOrNew_command || injectedDialog().m_new_command;
	}

	public boolean isModifyOnly() {
		return m_modifyOnly;
	}

	public boolean isReadOnly() {
		return m_readOnly;
	}

	int keyElemsSize() {
		return m_keyElems.vector.size();
	}

	private WrappedField keyElemVal(String fieldName) {
		WrappedField retVal = null;
		if (fieldName.compareToIgnoreCase(m_IdFieldName) != 0 || !injectedDialog().m_new_command || m_storing || !m_isNewRec)
			retVal = relatedField(fieldName) == null ? keyElem(fieldName) : relatedWField(fieldName);
		return retVal;
	}

	boolean listKeyIsText() {
		GridManager gridManager = getGridManager();
		if (gridManager != null && gridManager.m_gridBuffer != null)
			return gridManager.m_gridBuffer.keyIsText();
		else
			return false;
	}

	@Override
	protected boolean keysRefsOnDialogAreTobeSet() {
		return m_dataMaster || !m_isInTabbedPanel;
	}

	void loadComponentsFromVect(ComboBox cmp, Vector<DescrStruct> vet) {
		for (int i = 0; i < vet.size(); i++)
			cmp.addItem(vet.get(i));
	}

	public boolean loadData() {
		m_app.beginWaitCursor();
		boolean bRetVal = true;
		m_loadingData = true;
		WResultSet rs = null;
		if (wresultSetAssociated()) {
			setQuery();
			rs = new WResultSet(null, m_finalQuery);
			rs.setSmallBlobsList(m_smallBlobs);
		}
		if (bRetVal) {
			ensureClearTerms();
			if (existController() || isControllerMaster()) {
				clearMainData();
				if (rs != null) {
					bRetVal = m_gridManager.loadData(rs, m_queryDefPostStatement);
					if (bRetVal)
						manageController();
					if (!m_inhibitChangeNotification)
						refresh();
				} else {
					m_app.JotyMsg(this, "Data set not yet defined !");
					bRetVal = false;
				}
			} else {
				loadOutOfContextData();
				if (wresultSetAssociated()) {
					if (rs.open(m_queryDefPostStatement)) {
						m_isNewRec = rs.isEOF();
						if (!m_isNewRec) {
							for (int i = 0; i < keyElemsSize(); i++)
								rs.getValue(keyElem(i));
							for (Term term : m_terms) {
								if (!term.isAControlTerm() || term.m_ctrlTermInitedByParam) {
									if (term.m_dbFieldName != null && !term.m_dbFieldName.isEmpty() && term.m_drivenBufferTerm == null) {
										term.getWField(rs);
										term.updateState(rs);
									}
								}
							}
						}
						rs.close();
					}
				}
				updateVisibilityBasingOnData();
				setContextParams();
				loadGridTermData();
			}
			onDataLoaded();
			resetDirtyStatus();
		}
		lookForDataStructure(rs);
		m_askedToLoad = false;
		m_loadingData = false;
		m_app.endWaitCursor();
		return bRetVal;
	}

	protected void loadGrid() {
		m_gridManager.loadGrid();
	}

	void loadOutOfContextData() {
		accessOutOfContextData(false);
	}

	void loadGridTermData() {
		for (Term term : m_terms)
			if (!term.isAControlTerm())
				term.innerLoad();
	}

	public void lookForDataStructure(WResultSet rs) {
		if (m_app.m_webMode && m_descriptorOnLoad == null && rs != null)
			m_descriptorOnLoad = rs.m_cursor;
	}

	public void lookForIdFieldTermIndex() {
		int i = 0;
		for (Term term : m_terms) {
			String filedName = term.m_dbFieldName;
			if (filedName != null && m_IdFieldName.compareToIgnoreCase(term.m_dbFieldName) == 0) {
				m_IdFieldElemIdx = i;
				break;
			}
			i++;
		}
	}

	protected boolean lookForModificationNeeded() {
		return m_isInTabbedPanel && m_dataMaster;
	}

	protected String mainFilter() {
		return getWhereClause(0, true);
	}

	protected void manageController() {}

	void manageNewId(long id) {
		if (m_formKeyHiddenTerm != null)
			term(m_formKeyHiddenTerm).setInteger(id);
	}

	 /**
     * This method embraces indirectly much of the Joty integrated features about Delayed Desktop
     * Transaction management : the flow from {@code doAddNew} --> {@code setId} --> the setting of
     * the {@code m_delayed} attribute of the ID field as its value is to be generated, and contextually the use of
     * {@code entityIdExpr} method.
     */

	private boolean manageRecordCreation(WResultSet rs, boolean andCloseRS) throws JotyException {
		checkControllerInit(rs);
		m_isNewRec = true;
		prepareStore(rs);
		m_preIdGeneration = !m_idFieldAutoIncrement && !m_IdFieldName.isEmpty() && (!idFieldIsHostedByTerm() || m_formKeyHiddenTerm != null);
		if (m_preIdGeneration)
			m_identifyingID = getID(rs);
		else if (!idFieldIsHostedByTerm())
			rs.setFieldNotToUpdate(m_IdFieldName);
		doAddNew(rs);
		setRelatedFields(rs);
		return doUpdate(m_isNewRec, rs, true, andCloseRS);
	}

	void manageTermsVisibleState() {
		for (Term term : m_terms)
			term.checkVisibility();
	}

	public boolean needSaving() {
		if (m_targetPanel != null)
			return m_targetPanel.needSaving();
		else
			return m_dirty || isDirty();
	}

	boolean newIdManagement() throws JotyException {
		return m_dataMaster;
	}

	public void nextRecord() {
		if (isAListDrivenSlave())
			m_dialog.getGridMaster().nextRecord();
	}

	public void notifyPublishing(String publisherDialogClassName) {
		if (m_publishersSet.contains(publisherDialogClassName))
			m_reloadBecauseOfPublishing = true;
		for (Term term : m_terms)
			term.notifyPublishing(publisherDialogClassName);
	}

	/**
	 * Invokes the {@code BuildDetailsDialogAdapter.createDialog} method of the
	 * available implementation.
	 * <p>
	 * If a TableTerm is passed it is given the chance to drive the effect of
	 * the BuildDetailsDialogAdapter implementation to be completed with the
	 * navigation, by the target dialog, to the data related to its current
	 * selection.
	 * 
	 * @param term
	 *            the TableTerm object
	 * @param buildDetailsHandler
	 *            if null and if {@code term} is null too, an already internally
	 *            defined implementation will be used
	 * 
	 * @see TableTerm
	 * @see TermContainerPanel.BuildDetailsDialogAdapter
	 */
	public void openDetail(TableTerm term, BuildDetailsDialogAdapter buildDetailsHandler) {
		if (term == null) {
			BuildDetailsDialogAdapter detailsHandler = buildDetailsHandler == null ? m_buildDetailsHandler : buildDetailsHandler;
			if (detailsHandler == null)
				m_app.JotyMsg(this, "buildDetailsHandler not specified / not assigned !");
			else {
				m_detailsDialog = detailsHandler.createDialog(null);
				if (m_detailsDialog != null) {
					if (detailsHandler.identifierFromCaller() == null)
						m_dependentDialogs.add(m_detailsDialog);
					m_detailsDialog.addIdentifierFromCallerToTitle(detailsHandler.identifierFromCaller());
					m_detailsDialog.m_parentDataPanel = this;
					m_detailsDialog.perform();
				}
			}
		} else
			term.rowAction(buildDetailsHandler);
	}

	public Permission permission() {
		return getMaxPermission() ? m_permission : Permission.no_access;
	}

	protected void postInit() {}

	private void prepareStore(WResultSet rs) {
		rs.setActionByStatement(m_app.m_webMode || m_isNewRec && m_idFieldAutoIncrement);
	}

	public void previousRecord() {
		if (isAListDrivenSlave())
			m_dialog.getGridMaster().previousRecord();
	}

	public void publishThisDialog() {
		HashSet<String> subscribersSet = m_app.m_refreshMap.get(dialogClassName());
		if (subscribersSet != null) {
			JotyDialog subscriberDialog;
			for (String dialogName : (HashSet<String>) subscribersSet) {
				subscriberDialog = m_app.getOpenedDialog(dialogName, true);
				if (subscriberDialog != null)
					subscriberDialog.m_currSheet.notifyPublishing(dialogClassName());
			}
		}
	}

	void putActionFieldsNeededOnNew(Stocker list) {
		for (WrappedField wfield : m_keyElems.vector)
			list.add(wfield.m_dbFieldName);
		for (String elem : m_relationElems)
			list.add(elem);
		for (Term term : m_terms)
			if (term.defaultValue().getValue() != null || term.contextValue().getValue() != null)
				list.add(term.resultSetFieldName());
	}


	public void refresh() {
		if (m_operative) {
			for (Term term : m_terms)
				term.structuredInit();
			enableComponents(injectedDialog().m_editOrNew_command);
		}
	}

	@Override
	protected void relatedEnable(boolean generalEnabling) {
		if (getGridManager() != null)
			getGridManager().enable(!generalEnabling);
	}

	public String relatedField(String relatingField) {
		return m_relationElemsMap.get(relatingField);
	}

	WrappedField relatedWField(String fieldName) {
		WrappedField cachedFromRelatedDialog = m_cachedFromDialogRelated.get(fieldName);
		if (cachedFromRelatedDialog != null)
			return cachedFromRelatedDialog;
		else {
			String relatedField = relatedField(fieldName);
			if (relatedField.length() > 0)
				return injectedDialog().keyElem(relatedField);
			else
				return wrongWField();
		}
	}

	public String relatingField(String relatedField) {
		return m_relationElemsReverseMap.get(relatedField);
	}

	protected void reLoadData() {
		boolean oldEditCommandStatus = injectedDialog().m_editOrNew_command;
		injectedDialog().m_editOrNew_command = false;
		if (doneWithData()) {
			guiDataExch(false);
			checkAndSetLook();
		}
		injectedDialog().m_editOrNew_command = oldEditCommandStatus;
	}

	public void reloadGrid() {
		reloadGrid(false);
	}

	void reloadGrid(boolean addingRec) {
		if (m_gridManager == null)
			return;
		int pos = m_gridManager.getCurSel();
		loadGrid();
		if (addingRec)
			pos = m_gridManager.getRowQty() - 1;
		m_gridManager.setCurSel(pos);
	}

	public void resetUpdateActorsState() {
		for (Term term : m_terms)
			term.m_updatingActor = false;
	}

	boolean save() {
		if (m_targetPanel != null)
			return m_targetPanel.save();
		m_app.beginWaitCursor();
		boolean bRet = true;
		bRet = validateComponents();
		if (bRet) {
			updateDrivenBuffers();
			m_app.beginTrans();
			m_delayedWfields.clear();
			try {
				if (storeData()) {
					m_app.commitTrans();
					if (m_app.m_webMode && m_isNewRec) {
						checkForIdentifyingId();
						checkForTermsNewlyGeneratedValues();
					}
					m_isNewRec = false;
					m_controllerUpdateRequested = true;
					guiDataExch(false);
					resetDirtyStatus();
				} else {
					if ( ! m_app.m_webMode)
						nonManagedRollback();
					m_app.rollbackTrans();
					bRet = false;
				}
			} catch (JotyException e) {
				try {
					m_app.rollbackTrans();
				} catch (JotyException e1) {}
				bRet = false;
			} finally {
				for (WrappedField wfield : m_delayedWfields)
					wfield.m_delayed = false;
			}
		}
		m_app.endWaitCursor();
		return bRet;
	}

	public void saveEffects(boolean leaveEditingOn) {
		m_isNewRecOnIdle = false;
	}


	public void setAsPublisher() {
		m_isPublisher = true;
	}

	protected void setAutoIncrementByAddNew(boolean truth) {
		m_autoIncrementByAddNew = truth;
	}

	protected void setContextParams() {}

	protected void setControllerOnKey(long keyVal) {}

	public void setDefinitions(String querySet, String updatableSet, DataAccessDialog paramDialog) {
		if (Beans.isDesignTime())
			return;
		DataAccessDialog dialog = (DataAccessDialog) getDialog();
		if (dialog.m_accessorMode) {
			if (updatableSet != null || querySet != null || dialog.getUpdatableSet() != null || dialog.getQuerySet() != null)
				m_app.JotyMsg(this, m_app.m_msgDataDefExpectedAsEntry);
			return;
		} else {
			if (paramDialog != null) {
				if (updatableSet == null)
					updatableSet = paramDialog.getUpdatableSet();
				if (querySet == null)
					querySet = paramDialog.getQuerySet();
			}
			if (Utilities.isMoreThanOneWord(updatableSet)) {
				m_mainDataTable = Utilities.getMainTableNameFromSql(updatableSet);
				m_extractedWhereClause = Utilities.getWhereClauseNameFromSql(updatableSet);
			} else
				m_mainDataTable = updatableSet;
			m_definedQuery = querySet;
			if ((m_definedQuery == null || !Utilities.isMoreThanOneWord(m_definedQuery)) && updatableSet != null && !Utilities.isMoreThanOneWord(updatableSet))
				m_definedQuery = WResultSet.selectStmnt(updatableSet);
		}
	}

	@Override
	void setDescr(String termName, LiteralStruct literalStruct, String secondaryTermName, boolean loadSetIfAvailable) {
		m_growing = true;
		super.setDescr(termName, literalStruct, secondaryTermName, loadSetIfAvailable);
		m_growing = false;
	}

	public void setEditable(String gui_name) {
		term(gui_name).setModifiable(true);
	}

	protected void setId(WResultSet rs) {
		if (m_IdFieldName != null && m_IdFieldName.length() > 0) {
			rs.setIntegerValue(m_IdFieldName, m_identifyingID, m_app.m_webMode, !m_idFieldAutoIncrement);
			if (m_app.m_webMode)
				setWFieldAsReturnedValue(keyElem(m_IdFieldName));
			else
				keyElem(m_IdFieldName).m_delayed = false;
		}
	}

	public void setIdFieldAutoIncrement(boolean truth) {
		m_idFieldAutoIncrement = truth;
	}

	public void setIntegerKeyElemVal(String fieldName, long Val) {
		WrappedField term = keyElem(fieldName);
		term.setToNull(false);
		term.setInteger(Val);
	}

	void setMainData(WResultSet rs, long id) {
		String keyName;
		for (WrappedField keyElem : m_keyElems.vector) {
			keyName = keyElem.m_dbFieldName;
			if (keyName == m_IdFieldName) {
				if (keyElem.dataType() == JotyTypes._dbDrivenInteger)
					setIntegerKeyElemVal(m_IdFieldName, id);
			}
			if (rs != null)
				if (keyName != m_IdFieldName)
					rs.setValue(keyElem, false);
		}
		if (rs != null)
			for (String relElemName : m_relationElems)
				rs.setValue(relElemName, relatedWField(relElemName));
		manageNewId(id);
	}

	public void setMandatory(String gui_name) {
		term(gui_name).m_mandatory = true;
	}

	public void setModifyOnly(boolean modifyOnly) {
		m_modifyOnly = modifyOnly;
	}

	protected void setNotBoundFields(WResultSet rs) {
		if (m_common.m_shared && m_isNewRec)
			rs.setValue(m_common.m_sharingKeyField, m_common.m_sharingKey, false, 0);
	}

	public void setNotEditable(String gui_name) {
		term(gui_name).setModifiable(false);
	}

	void setQuery() {
		String mainFilter = mainFilter();
		if (m_accessorMode) {
			m_queryDefPostStatement = createQueryDefPostStatement(null, mainFilter, m_sortExpr, m_panelIdxInDialog);
			if (m_localAccessor) {
				m_app.m_accessor.setPostStatement(m_queryDefPostStatement);
				m_finalQuery = m_app.m_accessor.getQueryFromPostStatement();
			}
		} else {
			m_finalQuery = m_definedQuery;
			if (mainFilter.length() > 0)
				m_finalQuery += " WHERE " + mainFilter;
			if (m_sortExpr.length() > 0)
				m_finalQuery += " ORDER BY " + m_sortExpr;
		}
	}

	public void setReadOnly(boolean readOnly) {
		m_readOnly = readOnly;
	}

	public void setReadOnly(String gui_name) {
		term(gui_name).setReadOnly(true);
	}

	protected void setRelatedFields(WResultSet rs) {
		for (String relElem : m_relationElems)
			relatedWField(relElem).setWField(rs, relElem);
	}

	protected void setReloadNeeded() {
		for (Term term : m_terms)
			term.m_reloadNeeded = true;
	}

	public void setStrKeyElemVal(String fieldName, String Val) {
		WrappedField term = keyElem(fieldName);
		term.setToNull(false);
		term.m_strVal = Val;
	}

	/*
	 * Settles the delayed generation and valuation of the term or for its value being
	 * remotely used in the defining insert statement (similar to setId() but
	 * different from it in the nature of remote generation)
	 */
	protected void setTermAsReturnedValue(Term term) {
		setWFieldAsReturnedValue(term);
		term.setDirty();
		term.clear();
		term.setToNull(false);
		m_app.incrementRetValIndex();
	}

	protected void setTermNotAsReturnedValue(WrappedField wfield) {
		wfield.m_posIndexAsReturningValue = 0;
		wfield.m_delayed = false;
	}

	protected void setTermsDefaults() {
		if (m_isNewRec) {
			if (!m_defaultsInitialized) {
				for (Term term : m_terms)
					if (term.defaultValue().getValue() != null && term.isNull()) {
						term.copyWField(term.defaultValue().getValue(), false);
						if (term.m_dbFieldName != null && isEditing())
							term.setDirty();
						term.guiDataExch(false);
					}
				m_defaultsInitialized = true;
			}
		}
	}

	protected void setTermsOnContext() {
		if (m_isNewRec || existController() && !documentIdentified()) {
			for (Term term : m_terms)
				if (term.contextValue().getValue() != null && term.isNull())
					term.copyWField(term.contextValue().getValue(), false);
		}
	}

	protected void setValidationExpr(String expr) {
		m_validationExpr = expr;
	}

	protected void setWFieldAsReturnedValue(WrappedField wfield) {
		wfield.m_posIndexAsReturningValue = m_app.returnedValuesAvailablePos();
		setDelayed(wfield, m_app.m_webMode);
	}

	protected void setDelayed(WrappedField wfield, boolean predicate) {
		wfield.m_delayed = predicate;
		if (predicate)
			m_delayedWfields.add(wfield);
	}
	
	@Override
	protected void statusChangeProc() {
		if (m_targetPanel != null)
			m_targetPanel.statusChangeProc();
		else {
			resetUpdateActorsState();
			if (!m_inhibitChangeNotification)
				closeDependentDialogs();
			super.statusChangeProc();
		}
	}

	protected boolean storeData() throws JotyException {
		boolean tableAssociated = m_mainDataTable != null || m_accessorMode && !m_mainlyDbFree;
		boolean success = true;
		if (!tableAssociated) {
			if (isControllerMaster()) {
				success = false;
				m_app.JotyMsg(this, "Grid master panel must have the main table defined !");
			}
			if (m_definedQuery != null) {
				success = false;
				m_app.JotyMsg(this, "The panel has a complete set specified : in order the storing to take place it must have also the updatable set specified !");
			} else if (!m_accessorMode) {
				success = false;
				m_app.JotyMsg(this, "No updatable set specified !");
			}
		}
		if (success) {
			PostStatement postStatement = null;
			if (m_accessorMode)
				postStatement = createContextPostStatement();
			WResultSet rs = new WResultSet(m_mainDataTable, null, true, m_app.m_webMode ? getOpenForActionFields() : null, postStatement);
			if (m_app.m_webMode && m_common.m_reuseMetadataOnLoadForStore && m_descriptorOnLoad != null && updatableFieldsHaveDescriptorsAvailable())
				rs.setDescriptor(m_descriptorOnLoad);

			if (injectedDialog().m_new_command) {
				if (tableAssociated) {
					success = rs.open(true, postStatement);
					if (success)
						success = manageRecordCreation(rs, true);
				}
				if (success)
					success = storeGridTermData();
			} else if (injectedDialog().m_editOrNew_command) {
				if (tableAssociated) {
					if (m_app.m_accessor != null && postStatement != null)
						
						m_extractedWhereClause = rs.m_extractedWhereClause;
					String filterExpr = m_extractedWhereClause == null ? getWhereClause() : m_extractedWhereClause;
					if (m_common.m_shared) {
						if (filterExpr.length() > 0)
							filterExpr += " and ";
						filterExpr += m_common.sharingClause();
					}
					if (filterExpr.length() == 0 && !m_isNewRec) {
						m_app.JotyMsg(this, "May be the selection context for the updatable set was not specified : no update will be performed !");
						success = false;
					}
					if (success) {
						if (filterExpr != null && filterExpr.length() > 0)
							rs.m_sql += " Where " + filterExpr;
						success = rs.open(true, postStatement);
						if (success) {
							if (m_IdFieldName.length() > 0 && !idFieldIsHostedByTerm())
								rs.setFieldNotToUpdate(m_IdFieldName);
							if (m_app.m_debug && !m_app.m_webMode && m_isNewRec && !(rs.isBOF() && rs.isEOF())) {
								m_app.JotyMsg(this, "Check the dialog query and/or the 'where' clause : \n\nthe record appairs new on loading; " + 
															"however, on saving, an existing record has been identified !");
								success = false;
							} else {
								prepareStore(rs);
								if (m_isNewRec) {
									if (m_IdFieldName.length() > 0) {
										m_preIdGeneration = !m_idFieldAutoIncrement && m_identifyingID == 0;
										if (m_identifyingID == 0) {
											if (!m_preIdGeneration && m_IdFieldName.length() > 0)
												rs.setFieldNotToUpdate(m_IdFieldName);
											m_identifyingID = getID(rs);
										}
									}
									setRelatedFields(rs);
									doAddNew(rs);
								} else
									rs.edit();
								success = doUpdate(m_isNewRec, rs, false, true);
							}
						}
					}
				}
				if (success)
					success = storeGridTermData();
			}
			if (rs.isOpen())
				rs.close();
			if (success && m_app.m_webMode)
				nonManagedRollback();
		}
		return success;
	}

	boolean storeOutOfContextData() {
		return accessOutOfContextData();
	}

	protected boolean storeGridTermData() {
		boolean success = true;
		for (Term term : m_terms)
			if (!term.innerStore()) {
				success = false;
				break;
			}
		return success;
	}

	protected boolean storeWFieldsData(WResultSet rs) {
		boolean success = storeOutOfContextData();
		if (success) {
			for (Term term : m_terms)
				if (!term.isLockedAnyway()) {
					if (term.dbFieldSpecified()) {
						if (!term.isOnlyLoadingData()) {
							if (!m_app.m_webMode || term.isDirty() || m_isNewRec && m_keyFieldsForInsert.contains(term.m_dbFieldName))
								term.storeState(rs);
							WrappedField keyElem = m_keyElems.get(term.m_dbFieldName);
							if (keyElem != null)
								keyElem.copyWField(term, false);
						}
						
					} else if (term.m_GetWFieldImplementor != null)
						term.m_GetWFieldImplementor.method(rs, term);
				}
		}
		return success;
	}

	public String strKeyElemVal(String fieldName) {
		String retVal = "";
		WrappedField wfield = keyElemVal(fieldName);
		if (wfield != null)
			retVal = wfield.m_strVal;
		return retVal;
	}

	public void subscribe(String publisherDialogName) {
		if (!Beans.isDesignTime())
			m_publishersSet.add(enrollThisDialog(publisherDialogName));
	}

	@Override
	protected boolean termExchangable(Term term) {
		return !m_storing || term.isDirty();
	}

	protected boolean updatableFieldsHaveDescriptorsAvailable() {
		boolean success = true;
		for (Term term : m_terms)
			if (!term.isLockedAnyway() && term.dbFieldSpecified() && !term.isOnlyLoadingData())
				if (m_descriptorOnLoad.m_fieldsMap.get(term.m_dbFieldName) == null) {
					success = false;
					break;
				}
		return success;
	}

	public void updateCommandButtons(boolean idle) {}

	protected void updateController() {}

	protected void updateRecordOnController() {}

	protected void updateVisibilityBasingOnData() {}

	boolean validateStructuredComponent(int ctrl_GuiID, int termIdx, String msg) {
		return true;
	}

	protected boolean validation() {
		boolean retVal = true;
		if (m_validationExpr != null)
			retVal = m_app.m_webMode ? m_app.m_webClient.addVerifyExpr(m_validationExpr) : m_app.m_dbManager.validate(m_validationExpr);
		return retVal;
	}

	boolean wresultSetAssociated() {
		boolean retVal = m_definedQuery != null && m_definedQuery.length() > 0 || m_accessorMode;
		if (retVal && m_targetPanel != null)
			m_app.JotyMsg(this, "A different panel is specified as data buffer host, defining a result set for this panel makes no sense, indeed !");
		return retVal;
	}

	public void doActivation() {
		checkPublishers();
	}
	
	
}
