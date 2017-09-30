/*
	Copyright (c) 2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

	This file is part of Joty 2.0 Mobile.

	Joty 2.0 Mobile is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Joty 2.0 Mobile is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with Joty 2.0 Mobile.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.joty.mobile.gui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;

import org.joty.common.BasicPostStatement;
import org.joty.common.JotyTypes;
import org.joty.common.Utilities;
import org.joty.common.Utilities.*;
import org.joty.data.WrappedField;
import org.joty.mobile.app.JotyApp;
import org.joty.mobile.R;
import org.joty.mobile.data.JotyCursor;
import org.joty.gui.WFieldSet;
import org.joty.mobile.data.WResultSet;

import java.util.Vector;

/**
 * It is the renderer and optionally the editor of a single data record.
 * <p>
 * It is "controlled" by an instance of the {@code DetailsController} class.
 * <p>
 * Each data field of the record that is involved by the implementation subclass,
 * is associated with one of the View descendants, owning to the android.widget package, that are wrapped by a
 * subclass of the {@code Term} class. The constructor of the subclass of Term, must be used to instantiate
 * each field wrapper in the overriding of the {@code onCreate} method: the constructor receives all the parameters
 * to specify the responsibility of field data and view type.
 * <p>
 * On the onCreate method this class retrieves a reference to its controller from the stack hosted by the "top"
 * {@code DataMainActivity} object, accessible by the {@code m_dataMainActivity} member and  pushes itself
 * into the {@code JotyApp.m_dataDetailsActivityStack} stack.
 * <p>
 * The instance of the class can live in two different states identified by the truth of the {@code m_editing} boolean member.
 * If the state is "not in editing" the various {@code Term} objects let the wrapped view disabled and the action bar
 * shows first the "edit" and the "delete" commands.
 * If the state is "in editing" the various {@code Term} objects let the wrapped view enabled and the action bar
 * shows first the "save" and the "cancel" commands.
 * <p>
 * The class dispatches the four actions above to respective protected methods that, all, implement a concrete behavior.
 * <p>
 * The instance can start to edit a new record (this is detected during the creation using the controller method:
 * in this case the instance starts in editing mode and all the default value stored in the Term objects are
 * made effective in the wrapped view. Then each Term object, if involved in some change by the  user, notifies its dirty state and,
 * at last, on the "save" command, the class performs the expected action. The "cancel" command bring the navigation back to the
 * previous activity (the host of the "new record" command).
 * <p>
 *    The storing of information leverages on an instance of WResultSet that, once received data and metadata from
 *    the visual scenario, builds the statement or the accessor coordinates, packages these information into a BasicPostStatement object;
 *    at last the call of the {@code JotyApp.commitTrans} method perform the actual forwarding to the JotyServer through the WebClient object.
 * <p>
 *     If all goes successfully the Activity reaches the "not in editing" state else a convenient message is presented to the user and the Activity stays in editing.
 * <p>
 *
 * If the instance of this class, instead, start in "not in editing" it does it for rendering an existing record of data: the "edit" command makes the Activity to switch
 * into the "in editing" mode and the "save" command carries the execution along a flow similar to the one of the "new record" circumstance except the final statement generated.
 * The "cancel" command restores the "non in editing" state and this, if some datum was changed into any widget, is performed with a reloading of the record.
 * <p>
 * The Activity "Resume" event is the target for processing what, possibly has been "selected" from a {@code DataResultActivity} instance launched as "selector" of the
 * value for some widget, so that the overriding of {@code onResume} takes care of it.
 * <p>
 * Lastly it is worthwhile to note that the {@code openDetails} method opens another instance of type DataDetailsActivity that can be made to swich to a related record.
 *
 * @see DetailsController
 * @see MenuActivity
 * @see DataResultActivity
 * @see WResultSet
 * @see org.joty.mobile.web.WebClient
 *
 */

public class DataDetailsActivity extends MenuActivity {
    protected boolean m_editing, m_wasEditingOnPause;
    public long m_identifyingID;
    protected Vector<WrappedField> m_delayedWfields;
    public boolean m_selectionIsRunning;
    public boolean m_furtherDetailsInspecting;
    protected boolean m_isDeleting;
    protected boolean m_isSaving;
    protected boolean m_newRecord;
    private Stocker m_keyFieldsForInsert;
    protected Term m_selectionTarget;
    private boolean m_defaultsInitialized;
    protected boolean m_idFieldAutoIncrement;
    protected boolean m_autoIncrementByAddNew;
    protected String m_seq_name;
    protected DetailsController m_detailsController;
    protected JotyCursor m_descriptorOnLoad;
    public WFieldSet m_keyElems;
    public boolean m_preIdGeneration;
    public boolean m_asSelector;

    public String getId() {
        return m_detailsController.m_id;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_app.m_dataDetailsActivityStack.push(this);
        m_permission = m_mainActivity.m_permission;
        m_detailsController = m_mainActivity.m_detailsControllersStack.top();
        m_extras = m_detailsController.m_extras;
        m_keyElems = m_detailsController.m_keyElems;
        m_contextActivity = m_mainActivity;
        m_newRecord = m_detailsController.isNewRecord();
        m_editing = m_newRecord;
        lockRotation(m_editing);
        m_delayedWfields = new Vector<WrappedField>();
        m_keyFieldsForInsert = Utilities.m_me.new Stocker();
        m_idFieldAutoIncrement = m_common.m_idFieldAutoIncrement;
        m_autoIncrementByAddNew = m_common.m_autoIncrementByAddNew;
        m_seq_name = m_common.m_seq_name;
        m_asSelector = m_extras != null && m_extras.getBoolean("asSelector");
    }

    void clearTerms() {
         for (Term term : m_terms)
            term.clear();
    }

    public void processData() {
        clearTerms();
        if (m_newRecord)
            doWithData(null);
        else {
            JotyApp.QueryResponseHandlersManager respManager = getRespManager();
            m_newRecord = respManager.m_rs.isEOF();
            if (!m_newRecord) {
                for (int i = 0; i < m_detailsController.keyElemsSize(); i++)
                    respManager.m_rs.getValue(keyElem(i));
                for (Term term : m_terms) {
                    if (term.m_dbFieldName != null && !term.m_dbFieldName.isEmpty())
                        term.getWField(respManager.m_rs);
                }
            }
            doWithData(respManager.m_rs);
        }
    }

    protected void doWithData(WResultSet rs) {
        setContextParams();
        onDataLoaded();
        if (m_newRecord) {
            setTermsDefaults();
            setTermsOnContext();
        } else
            lookForDataStructure(rs);
        guiDataExchange(false);
        resetDirtyStatus();
        setWaitCursor(false);
    }

    public void lookForDataStructure(WResultSet rs) {
        if (m_descriptorOnLoad == null && rs != null)
            m_descriptorOnLoad = rs.m_jotyCursor;
    }

    protected void onDataLoaded() {
    }

    String idFieldName() {
        return m_detailsController.idFieldName();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (m_editing) {
            addMenuItemToAppMenu(menu, JotyApp.JotyMenus.SaveMenu, jotyLang("LBL_SAVE"), R.drawable.save, false,
                    new Action() {
                        public void doIt() {
                            if (isDirty())
                                save();
                            else {
                                m_app.toast(m_common.appLang("NoChange"));
                                cancel();
                            }
                        }
                    });
            addMenuItemToAppMenu(menu, JotyApp.JotyMenus.CancelMenu, jotyLang("LBL_CNC"), R.drawable.cancel, false,
                    new Action() {
                        public void doIt() {
                            cancel();
                        }
                    });
        } else if (!m_detailsController.m_readOnly) {
            if (editIsAllowed())
                addMenuItemToAppMenu(menu, JotyApp.JotyMenus.EditMenu, jotyLang("LBL_MNG"), R.drawable.edit, false,
                        new Action() {
                            public void doIt() {
                                edit();
                            }
                        });
            if (deleteIsAllowed())
                addMenuItemToAppMenu(menu, JotyApp.JotyMenus.DeleteMenu, jotyLang("LBL_DEL"), R.drawable.delete, false,
                        new Action() {
                            public void doIt() {
                                delete();
                            }
                        });
        }
        addIntermediateOptions(menu);
        return super.onCreateOptionsMenu(menu);
    }

    protected void addIntermediateOptions(Menu menu) {
    }

    protected void edit() {
        setEditing(true);
    }

    protected void setEditing(boolean truth) {
        enableWidgets(truth);
        m_editing = truth;
        if (!truth) {
            m_isSaving = false;
            m_isDeleting = false;
        }
        lockRotation(m_editing);
        invalidateOptionsMenu();
    }

    @Override
    protected boolean navigationNeeded() {
        return !m_editing;
    }

    String deleteExtension() {
        return "";
    }

    protected void delete() {
        if (isDeletable()) {
            m_app.yesNoQuestion(String.format(jotyLang("WantToDelete"), jotyLang("ItemToDelete")), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        m_app.beginTrans();
                        m_isDeleting = true;
                        m_delayedWfields.clear();
                        JotyApp.ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
                        respManager.push(m_app.new ResponseHandler() {
                            @Override
                            public void handle(boolean result, JotyApp.ResponseHandlersManager respManager) {
                                if (result) {
                                    m_app.m_dataModified = true;
                                    finish();
                                } else
                                    m_isDeleting = false;
                            }
                        });
                        doDeletion();
                        m_app.commitTrans(respManager);
                    }
                }
            });
        }
    }

    protected void doDeletion() {
        String delSqlStmt;
        if (m_detailsController.m_definedQuery != null || accessorMode()) {
            String whereClause = m_detailsController.getWhereClause(Long.parseLong(m_detailsController.m_id), false, false, false, true);
            if (whereClause.isEmpty()) {
                m_app.warningMsg("Key not define for deletion !");
            } else {
                delSqlStmt = String.format("Delete from %1$s Where %2$s", m_app.codedTabName(m_detailsController.m_mainDataTable), whereClause);
                String extension = deleteExtension();
                if (extension.length() > 0)
                    delSqlStmt += " AND " + extension;
                m_app.m_db.executeSQL(delSqlStmt, null, createContextPostStatement(m_detailsController.m_accessorCoordinates), null);
            }
        }
    }

    protected boolean isDeletable() {
        return true;
    }

    protected void cancel() {
        if (m_newRecord)
            finish();
        else {
            if (isDirty())
                m_detailsController.openDetailsActivity(this, m_detailsController.m_id);
            else
                setEditing(false);
        }
    }

    protected boolean validateComponents() {
        boolean retVal = true;
        for (Term term : m_terms)
            if (!term.validate()) {
                retVal = false;
                break;
            }
        return retVal;
    }

    /**
     * The method accomplishes its task defining a Desktop Delayed Transaction like the Joty workstation application does.
     * It realizes a two stages usage of two different instances of the
     * {@code ResponseHandlersManager} class. The outer one is needed because, along the invocation of the
     * "forwarding part", that is the invocation of the {@code storeData} method, the configuration of the
     * Joty application may be such that a fresh copy of the fields metadata is desired (this happens when
     * the "reuseMetadataOnLoadForStore" configuration item is 'false'): in this case an
     * inquiry takes place and as such it must (in Joty 2.0 Mobile) be managed by means of a {@code QueryResponseHandlersManager}
     * object that allows its handling part to execute the {@code handle} method of the handler located in the
     * outer manager, method that, finally, executes the commit command, that, at its turn, realizes the
     * transaction forwarding, by relying on another instance of {@code ResponseHandlersManager}.
     * <p>
     * This asset seemed to be visually understandable to the developer, as decoupling two different access
     * to the Joty server, the first one of which, an inquiry, is unusual but, nevertheless, to be supported by the framework.
     * The stack of one only {@code ResponseHandlersManager} object could have been used, instead, but
     * the presence of the possibility of an inquiry access to the server, during the building of the Delayed Desktop Transaction,
     * would have been less apparent.
     */

    protected void save() {
        boolean bRet = true;
        guiDataExchange(true);
        bRet = validateComponents();
        if (bRet) {
            m_app.beginTrans();
            m_isSaving = true;
            m_delayedWfields.clear();
            JotyApp.ResponseHandlersManager prepareStoreRespManager = m_app.new ResponseHandlersManager();
            prepareStoreRespManager.push(m_app.new ResponseHandler() {

                @Override
                public void handle(boolean result, JotyApp.ResponseHandlersManager prmRespManager) {
                    if (result) {
                        JotyApp.ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
                        respManager.push(m_app.new ResponseHandler() {
                            @Override
                            public void handle(boolean result, JotyApp.ResponseHandlersManager respManager) {
                                if (result) {
                                    if (m_newRecord) {
                                        checkForIdentifyingId();
                                        checkForTermsNewlyGeneratedValues();
                                    }
                                    m_newRecord = false;
                                    guiDataExchange(false);
                                    resetDelayedState();
                                    resetDirtyStatus();
                                    setEditing(false);
                                    saveEffects();
                                    m_app.m_dataModified = true;
                                    if (m_extras != null && m_extras.getBoolean("asSelector")) {
                                        m_app.m_valuesOnChoice.m_id = Long.parseLong(m_detailsController.m_id);
                                        finish();
                                    }
                                }
                            }
                        });
                        m_app.commitTrans(respManager);
                    }
                }

            });
            storeData(prepareStoreRespManager);
        }
    }

    protected void saveEffects() {
    }

    protected void checkForTermsNewlyGeneratedValues() {
        int retValIndex;
        for (Term term : m_terms) {
            retValIndex = term.m_posIndexAsReturningValue - 1;
            if (retValIndex >= 0)
                term.setValFromDbSubmittedExpr(m_app.m_webClient.getReturnedValue(retValIndex));
        }
    }

    protected WrappedField keyElem(int index) {
        return m_keyElems.get(index);
    }

    protected WrappedField keyElem(String keyName) {
        WrappedField retVal = m_keyElems.get(keyName);
        if (retVal == null)
            m_app.warningMsg("No key elem found for field '" + keyName + "'");
        return retVal;
    }

    protected boolean checkForIdentifyingId() {
        boolean retVal = true;
        if (m_idFieldAutoIncrement || m_preIdGeneration) {
            String returnedValue = m_app.m_webClient.getReturnedValue(keyElem(idFieldName()).m_posIndexAsReturningValue - 1);
            m_identifyingID = returnedValue == null ? 0 : Long.parseLong(returnedValue);
            if (m_identifyingID > 0) {
                setMainData(null, m_identifyingID);
                keyElem(idFieldName()).m_delayed = false;
                m_detailsController.m_id = String.valueOf(m_identifyingID);
            } else {
                m_app.warningMsg(String.format("dbms ID generation was expected but it didn't occur: check definitions on db or joty declarations in class %1$s " +
                                "if Auto-incrementing is the case otherwise look at the code in %2$s !",
                        getClass().getName(), m_app.m_dbManager.getClass().getName()));
                retVal = false;
            }
        }
        return retVal;
    }

    void setMainData(WResultSet rs, long id) {
        String keyName;
        for (WrappedField keyElem : m_keyElems.vector) {
            keyName = keyElem.m_dbFieldName;
            if (keyName == idFieldName()) {
                if (keyElem.dataType() == JotyTypes._dbDrivenInteger)
                    setIntegerKeyElemVal(idFieldName(), id);
            }
            if (rs != null)
                if (keyName != idFieldName())
                    rs.setValue(keyElem, false);
        }
        manageNewId(id);
    }

    public boolean costraintViolationMsgOnDelete() {
        m_app.toast(jotyLang("DbmsConstrViolationDelete"), false);
        return false;
    }

    public boolean costraintViolationMsgOnUpdate() {
        m_app.toast(jotyLang("DbmsConstrViolationUpdate"), false);
        return false;
    }

    protected void manageNewId(long id) {
    }

    public void setIntegerKeyElemVal(String fieldName, long Val) {
        WrappedField term = keyElem(fieldName);
        term.setToNull(false);
        term.setInteger(Val);
    }

    void resetDelayedState() {
        for (WrappedField wfield : m_delayedWfields)
            wfield.m_delayed = false;
    }

    private Stocker getOpenForActionFields() {
        Stocker retObj = Utilities.m_me.new Stocker();
        for (Term term : m_terms)
            if (term.isDirty() && term.resultSetFieldName() != null)
                retObj.add(term.resultSetFieldName());
        m_keyFieldsForInsert.clear();
        if (m_newRecord) {
            putActionFieldsNeededOnNew(m_keyFieldsForInsert);
            retObj.add(m_keyFieldsForInsert);
        }
        return retObj;
    }

    void putActionFieldsNeededOnNew(Stocker list) {
        for (WrappedField wfield : m_keyElems.vector)
            list.add(wfield.m_dbFieldName);
        for (Term term : m_terms)
            if (term.defaultValue().getValue() != null || term.contextValue().getValue() != null)
                list.add(term.resultSetFieldName());
    }

    protected boolean updatableFieldsHaveDescriptorsAvailable() {
        boolean success = true;
        for (Term term : m_terms)
            if (term.dbFieldSpecified())
                if (m_descriptorOnLoad.m_fieldsMap.get(term.m_dbFieldName) == null) {
                    success = false;
                    break;
                }
        return success;
    }

    private boolean manageRecordCreation(WResultSet rs) {
        rs.setFieldNotToUpdate(idFieldName());
        doAddNew(rs);
        return doUpdate(m_newRecord, rs);
    }

    private void doAddNew(WResultSet rs) {
        rs.addNew();
        for (WrappedField wfield : m_detailsController.m_keyElemDefaults)
            rs.setValue(wfield, false);
        for (Term term : m_terms)
            if (term.m_delayed)
                rs.setValue(term);
        setId(rs);
        setMainData(rs, m_identifyingID);
    }

    protected void setId(WResultSet rs) {
        if (idFieldName() != null && idFieldName().length() > 0) {
            rs.setIntegerValue(idFieldName(), m_identifyingID, true, !m_idFieldAutoIncrement);
            setWFieldAsReturnedValue(keyElem(idFieldName()));
        }
    }

    protected void setWFieldAsReturnedValue(WrappedField wfield) {
        wfield.m_posIndexAsReturningValue = m_app.returnedValuesAvailablePos();
        setDelayed(wfield, true);
    }

    protected void setDelayed(WrappedField wfield, boolean predicate) {
        wfield.m_delayed = predicate;
        if (predicate)
            m_delayedWfields.add(wfield);
    }

    protected boolean doUpdate(boolean isNewRecord, WResultSet rs) {
        setNotBoundFields(rs);
        storeWFieldsData(rs);
        rs.update(isNewRecord, m_idFieldAutoIncrement, createContextPostStatement(m_detailsController.m_accessorCoordinates), null);
        return true;
    }


    protected void setNotBoundFields(WResultSet rs) {
        if (m_common.m_shared && m_newRecord)
            rs.setValue(m_common.m_sharingKeyField, m_common.m_sharingKey, false, 0);
    }

    protected void storeWFieldsData(WResultSet rs) {
        for (Term term : m_terms)
            if (term.dbFieldSpecified()) {
                if (term.isDirty() || m_newRecord && m_keyFieldsForInsert.contains(term.m_dbFieldName))
                    term.storeState(rs);
                WrappedField keyElem = m_keyElems.get(term.m_dbFieldName);
                if (keyElem != null)
                    keyElem.copyWField(term, false);
            } else if (term.m_GetWFieldImplementor != null)
                term.m_GetWFieldImplementor.method(rs, term);
    }

    protected void storeData(final JotyApp.ResponseHandlersManager prmRespManager) {
        BasicPostStatement postStatement = null;
        if (accessorMode())
            postStatement = createContextPostStatement(m_detailsController.m_accessorCoordinates);
        WResultSet rs = new WResultSet(m_detailsController.m_mainDataTable, null, true, getOpenForActionFields(), postStatement);
        if (m_common.m_reuseMetadataOnLoadForStore && m_descriptorOnLoad != null && updatableFieldsHaveDescriptorsAvailable())
            rs.setDescriptor(m_descriptorOnLoad);
        if (rs.m_metadataReuse)
            prmRespManager.checkToExecute(doStoreData(true, rs, postStatement));
        else {
            JotyApp.QueryResponseHandler respHandler = m_app.new QueryResponseHandler() {
                @Override
                public void handleQuery(boolean result, WResultSet rs, BasicPostStatement postStatement) {
                    prmRespManager.checkToExecute(doStoreData(result, rs, postStatement));
                }
            };
            JotyApp.QueryResponseHandlersManager qRespManager = accessorMode() ?
                    m_app.new QueryResponseHandlersManager(postStatement, respHandler, rs) :
                    m_app.new QueryResponseHandlersManager((String) null, respHandler, rs);
            qRespManager.open(true);
        }
    }

    protected boolean doStoreData(boolean result, WResultSet rs, BasicPostStatement postStatement) {
        boolean success = result;
        if (success) {
            if (m_newRecord)
                success = manageRecordCreation(rs);
            else {
                String filterExpr;
                filterExpr = m_detailsController.getWhereClause(0, false, m_newRecord, m_isSaving, m_isDeleting);
                if (m_common.m_shared) {
                    if (filterExpr.length() > 0)
                        filterExpr += " and ";
                    filterExpr += m_common.sharingClause();
                }
                if (filterExpr.length() == 0 && !m_newRecord) {
                    m_app.warningMsg("May be the selection context for the updatable set was not specified : no update will be performed !");
                    success = false;
                }
                if (success) {
                    if (filterExpr != null && filterExpr.length() > 0)
                        rs.m_sql += " Where " + filterExpr;
                    if (idFieldName().length() > 0)
                        rs.setFieldNotToUpdate(idFieldName());
                    if (m_newRecord) {
                        if (idFieldName().length() > 0) {
                            m_preIdGeneration = !m_idFieldAutoIncrement && m_identifyingID == 0;
                            if (m_identifyingID == 0) {
                                if (!m_preIdGeneration && idFieldName().length() > 0)
                                    rs.setFieldNotToUpdate(idFieldName());
                                m_identifyingID = -1;
                            }
                        }
                        doAddNew(rs);
                    } else
                        rs.edit();
                    success = doUpdate(m_newRecord, rs);
                }
            }
        }
        if (success)
            nonManagedRollback();
        return success;
    }

    protected void nonManagedRollback() {
    }

    protected void enableWidgets(boolean truth) {
        for (Term term : m_terms)
            term.enable(truth);
    }

    protected boolean editIsAllowed() {
        return hasPermission(Permission.readWrite);
    }

    protected boolean deleteIsAllowed() {
        return hasPermission(Permission.all);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (m_app.m_home)
            return;
        if (m_selectionIsRunning) {
            if (m_app.m_valuesOnChoice.selected()) {
                m_selectionTarget.setDirty();
                selectionCallBack(m_selectionTarget);
                m_selectionTarget.guiDataExch(false);
             }
        } else {
            if (!m_wasEditingOnPause && !m_furtherDetailsInspecting)
                processData();
        }
        m_selectionIsRunning = false;
        m_furtherDetailsInspecting = false;
        enableWidgets(m_editing);
        m_detailsController.m_processing = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        m_wasEditingOnPause = m_editing;
    }

    protected void selectionCallBack(Term target) {
    }

    protected void setContextValueFromParam(Term term, String paramName) {
        String value = m_detailsController.m_paramContext.contextParameter(paramName);
        if (value != "-1")
            switch (term.dataType()) {
                case JotyTypes._text:
                    term.contextValue().setValue(value);
                    break;
                case JotyTypes._long:
                    term.contextValue().setValue(Long.parseLong(value));
                    break;
                case JotyTypes._date:
                    term.contextValue().setValue(java.sql.Date.valueOf(value.replace("'", "")));
                    break;
            }
    }

    protected void setTermsDefaults() {
        if (!m_defaultsInitialized) {
            for (Term term : m_terms)
                if (term.defaultValue().getValue() != null && term.isNull()) {
                    term.copyWField(term.defaultValue().getValue(), false);
                    if (term.m_dbFieldName != null && m_editing)
                        term.setDirty();
                    term.guiDataExch(false);
                }
            m_defaultsInitialized = true;
        }
    }

    protected void setTermsOnContext() {
        for (Term term : m_terms)
            if (term.contextValue().getValue() != null && term.isNull())
                term.copyWField(term.contextValue().getValue(), false);
    }

    protected void setLangTitle(String literal) {
        setTitle(m_common.appLang(literal) + (m_newRecord ? (" (" + jotyLang("LBL_NEW") + ")") : ""));
    }

    @Override
    protected void beforeFinish(boolean forRenewing) {
        if (!forRenewing)
            m_mainActivity.m_detailsControllersStack.pop();
        m_app.m_dataDetailsActivityStack.pop();
    }

    protected void openResultPrepare(long id) {
        mainActivity().m_resultControllersStack.top().m_paramContext = createParamContext();
    }

    protected void openDetails(String id, Class activityClass, String accessorMainCoordinate, int selectionTargetResId) {
        m_selectionTarget = m_termMap.get(selectionTargetResId);
        DetailsController detailsController = new DetailsController(activityClass);
        if (selectionTargetResId > 0) {
            detailsController.m_extras.putBoolean("asSelector", true);
            m_selectionIsRunning = true;
        }
        detailsController.instantiateOwnData();
        detailsController.m_accessorCoordinates.jotyDialogClassName = accessorMainCoordinate;
        detailsController.addIntegerKeyElem("id");
        detailsController.m_keysFilterOnReading = true;
        m_mainActivity.m_detailsControllersStack.push(detailsController);
        detailsController.openDetailsActivity(this, id, null);
    }


    public void forwardExtraExtras() {
    }
}
