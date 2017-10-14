/*
	Copyright (c) 2013-2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

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

package org.joty.mobile.data;


import org.joty.common.BasicPostStatement;
import org.joty.common.Utilities.Stocker;
import org.joty.data.BasicJotyCursor;
import org.joty.data.FieldDescriptor;
import org.joty.data.WrappedField;
import org.joty.data.WrappedResultSet;
import org.joty.mobile.app.JotyApp;
import org.joty.mobile.app.JotyApp.ResponseHandlersManager;
import org.joty.web.AbstractWebClient;

/**
 * This implementation of {@code WrappedResultSet} is dedicated to exclusively exchange data with
 * the web client of Joty 2.0 Mobile.
 * 
 * It uses an instance of the {@code JotyCursor} class as cursor object.
 * 
 * As element of the Joty 2.0 Mobile sub-framework, the class is traversed by the management of a
 * {@code ResponseHandlersManager} object that, in all uses, at last, is delivered to the {@code org.joty.mobile.web.WebClient} instance.
 *
 * @see org.joty.mobile.web.WebClient
 * @see org.joty.mobile.app.JotyApp.ResponseHandlersManager
 * @see org.joty.mobile.data.JotyCursor
 */

public class WResultSet extends WrappedResultSet {

    public JotyCursor m_jotyCursor;
    private JotyApp m_jotyApp;

    @Override
    protected FieldDescriptor fieldDescriptor(String fieldName) {
        return m_jotyCursor.fieldDescriptor(fieldName);
    }

    @Override
    public String getSql() {
        return m_sql;
    }

    @Override
    public int colCount() {
        return m_cursor.m_fields.length;
    }

    @Override
    public void setDescriptor(BasicJotyCursor descriptor) {
        String sql = m_sql;
        super.setDescriptor(descriptor);
        m_jotyCursor = (JotyCursor) descriptor;
        m_sql = sql;
    }


    @Override
    public void setSql(String sql) {
        m_sql = sql;
    }

    @Override
    public boolean webMode() {
        return true;
    }

    @Override
    public boolean isBOF() {
        return m_jotyCursor.isBeforeFirst();
    }

    @Override
    public boolean isEOF() {
        return m_jotyCursor.isAfterLast();
    }

    @Override
    public boolean getValue(WrappedField wfield) {
        return setValueToWField(wfield.dbFieldName(), wfield);
    }

    @Override
    protected BasicJotyCursor createCursor(int fieldQty) {
        return new JotyCursor(fieldQty, m_app);
    }

    public WResultSet(Object fictitious, String sql) {
        this(null, sql, false);
    }

    public WResultSet(String tableName, String sql, boolean forUpdate) {
        this(tableName, sql, forUpdate, null);
    }

    public WResultSet(String tableName, String sql, boolean forUpdate, Stocker openForUpdateFields) {
        this(tableName, sql, forUpdate, openForUpdateFields, null);
    }

    public WResultSet(String tableName, String prmSql, boolean forUpdate, Stocker openForUpdateFields, BasicPostStatement postStatement) {
        m_jotyApp = JotyApp.m_app;
        m_app = m_jotyApp;
        checkSetName(tableName);
        String sql = null;
        if (postStatement == null)
            sql = initSql(tableName, prmSql, openForUpdateFields, postStatement);
        init(forUpdate, sql);
    }

    private void init(boolean forUpdate, String sql) {
        m_sql = sql;
        initialize();
    }

    @Override
    public void instantiate(int fieldQty) {
        super.instantiate(fieldQty);
        m_jotyCursor = (JotyCursor) m_cursor;
        m_jotyCursor.m_container = this;
    }

    @Override
    public void next() {
        m_jotyCursor.moveToNext();
    }

    public void open(ResponseHandlersManager respManager) {
        open(null, respManager);
    }

    public void open(boolean forMetadataOnly, ResponseHandlersManager respManager) {
        open(forMetadataOnly, null);
    }

    public void open(boolean forMetadataOnly, BasicPostStatement postStatement, ResponseHandlersManager respManager) {
        m_actionFields.clear();
        webOpen(forMetadataOnly, postStatement, respManager);
    }

    public void open(BasicPostStatement postStatement, ResponseHandlersManager respManager) {
        open(false, postStatement, respManager);
    }

    @Override
    public int getColCount() {
        return colCount();
    }

    @Override
    public FieldDescriptor getFieldDescriptor(short fldIdx) {
        return m_cursor.m_fields[fldIdx];
    }

    @Override
    public boolean actionFieldsContains(String fieldName) {
        return m_actionFields.contains(fieldName);
    }

    public void update(boolean newRec, boolean withAutoIncrId, ResponseHandlersManager respManager) {
        update(newRec, withAutoIncrId, null, respManager);
    }

    public void update(boolean newRec, boolean withAutoIncrId, BasicPostStatement contextPostStatement, ResponseHandlersManager respManager) {
        updateByStatement(newRec, withAutoIncrId, contextPostStatement, respManager);
    }

    public void updateByStatement(boolean newRec, boolean withAutoIncrId, BasicPostStatement contextPostStatement, ResponseHandlersManager respManager) {
        BuildStatementValues values = buildStatement(newRec, withAutoIncrId, contextPostStatement);
        if (values.sql != null)
            m_jotyApp.m_webClient.manageCommand(values.sql, newRec && withAutoIncrId, m_autoId, false, values.postStatement, 0, respManager);
    }

    @Override
    public boolean webOpen(boolean forOnlyMetadata, BasicPostStatement postStatement, Object respManager) {
        AbstractWebClient wClient = m_app.getWebClient();
        wClient.setSmallBlobsList(m_smallBlobs);
        ((ResponseHandlersManager) respManager).push(m_jotyApp.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                respManager.popAndcheckToExecute(onOpened(result));
            }
        });
        wClient.sqlQuery(m_sql, forOnlyMetadata, false, postStatement, respManager);
        return true;
    }

    @Override
    public void setMetaData(int conIndex, FieldDescriptor colDescr) {
        super.setMetaData(conIndex, colDescr);
        m_jotyCursor.m_fieldNames[conIndex] = colDescr.m_strName;
    }

}
