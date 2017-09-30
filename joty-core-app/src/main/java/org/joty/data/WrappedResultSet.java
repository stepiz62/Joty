/*
	Copyright (c) 2013-2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

	This file is part of Joty 2.0 Core.

	Joty 2.0 Core is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Joty 2.0 Core is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with Joty 2.0 Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.joty.data;

import java.io.UnsupportedEncodingException;

import org.joty.app.Common;
import org.joty.app.JotyApplication;
import org.joty.app.JotyException;
import org.joty.app.JotyException.reason;
import org.joty.common.ApplMessenger;
import org.joty.common.BasicPostStatement;
import org.joty.common.JotyTypes;
import org.joty.common.Utilities;
import org.joty.common.Utilities.Stocker;
import org.joty.web.AbstractWebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * It is an abstract class that wraps the object containing the image of data
 * got from inquiries or the base for launching command to modify data on the
 * dbms, no matter which the way of accessing the dbms could be but, in any
 * case, with the representation of data based on the {@code WrappedField}
 * object as atom;
 * <p>
 * It uses a {@code BasicJotyCursor} object as a cursor for holding the single
 * current record and the meta-data relative to the underlying represented set
 * of data. All the getter and setter methods present are based on this object.
 * <p>
 * The class is opened to the web, as a medium to reach the Joty Server and the
 * dbms indeed, by interacting with an instance of the {@code AbstractWebClient}
 * class and participates in the realization of the protocol that manages the
 * Delayed Desktop Transaction. Towards the web client the following type of
 * statements are directed: either inquiring sql statements coming from the
 * outside of the class or the statements internally formed to operate data
 * modifications on the database: the 'new record' and the 'modify' actions are
 * normally translated in sql statement; for this purpose the class uses an
 * instance of the {@code StatementBuilder} class. The 'delete' command is
 * always translated in sql. The result of an inquiry to the database, coming
 * from the Joty Server, is hold in a {@code org.w3c.dom.NodeList} object.
 * <p>
 * As other main classes of the data area of the framework, this one too
 * supports the Accessor mode, by which the completion of the synthesis of the
 * statements or the entire building of them are demanded to an implementation
 * of the {@code Accessor} class.
 * <p>
 * The class is located just below the graphic user interface layer, with which
 * it exchanges data and the returned codes, and stands on a sort of virtual
 * data layer.
 * <p>
 * To communicate with the data layer, in the case of Accessor mode or in
 * WebMode, it uses an instance of {@code BasicPostStatement} class (or an
 * implementation of it).
 *
 *
 * @see BasicJotyCursor
 * @see JotyTypes
 * @see StatementBuilder
 * @see WrappedField
 * @see AbstractWebClient
 * @see BasicPostStatement
 *
 */

public abstract class WrappedResultSet  implements JotyResultSet {
    public BasicJotyCursor m_cursor;
    public Stocker m_actionFields;
    public String m_tableName;
    protected String m_autoId;
    protected StatementBuilder m_statementBuilder;
    public boolean m_metadataReuse;
    public JotyApplication m_app;
    private boolean m_actionByStatement;

    public NodeList m_recNodeList;
    public int m_currNodeIndex;
    public Node m_currNode;
    public boolean m_nodeListEOF;
    public boolean m_nodeListBOF;
    public Stocker m_smallBlobs;
    private Document m_xml;
    public int m_colCount;
    public String m_sql;

    public void addNew() {
        setToNull();
    }

    void setToNull() {
        m_cursor.setToNull();
    }

    void setToNull(String fieldName) {
        fieldDescriptor(fieldName).m_isNull = true;
    }

    protected abstract FieldDescriptor fieldDescriptor(String fieldName);

    public abstract String getSql();

    public abstract int colCount();

    public static String selectStmnt(String tabName) {
        return selectStmnt(tabName, null);
    }

    public static String selectStmnt(String tabName, Stocker openForUpdateFields) {
        String what;
        if (openForUpdateFields == null)
            what = tabName + ".*";
        else
            what = openForUpdateFields.asCommaSeparatedList();
        return "Select " + what + " from " + tabName;
    }

    public void setDescriptor(BasicJotyCursor descriptor) {
        m_cursor = descriptor;
        setMetadataReuse();
    }

    public void setMetadataReuse() {
        m_metadataReuse = true;
    }

    public void setSmallBlobsList(Stocker list) {
        m_smallBlobs = list;
    }

    public abstract void setSql(String sql);

    public abstract boolean webMode();

    public boolean isBOF() {
        return m_nodeListBOF;
    }

    public boolean isEOF() {
        return m_nodeListEOF;
    }

    public boolean actionByStatement() {
        return m_actionByStatement;
    }

    public abstract boolean getValue(WrappedField wfield);

    protected class BuildStatementValues {
        public String sql;
        public BasicPostStatement postStatement;
    }

    public BuildStatementValues buildStatement(boolean newRec, boolean withAutoIncrId, BasicPostStatement contextPostStatement) {
        BuildStatementValues retVal = new BuildStatementValues();
        retVal.sql = null;
        m_autoId = null;
        BasicPostStatement postStatement = null;
        m_statementBuilder.helper.setContextPostStatement(contextPostStatement);
        if (newRec) {
	      	Common common = (Common) ((ApplMessenger) m_app).getCommon();
            if (newRec && withAutoIncrId && common.m_autoIncrementByAddNew) {
                if (webMode())
                    postStatement = m_app.getWebClient().prepareAddNewItems(this);
                else
                    ; // not implemented
            } else
                retVal.sql = m_statementBuilder.genInsertStmnt();
        }
        else
            retVal.sql = m_statementBuilder.genUpdateStmnt();
        if (retVal.sql != null)
            retVal.postStatement = contextPostStatement == null ? postStatement : contextPostStatement;
        return retVal;
    }

    protected void updateCatalog(String fieldName) {
        if (!m_actionFields.contains(fieldName))
            m_actionFields.add(fieldName);
    }

    @Override
    public String getTableName() {
        return m_tableName;
    }

    public boolean typeIsText(String fieldName) {
        return isFieldType(fieldName, JotyTypes._text);
    }

    boolean isFieldType(String fieldName, int type) {
        return fieldDescriptor(fieldName).m_nType == type;
    }

    @Override
    public String getValueStr(String fieldName, boolean forSqlExpr) {
        String retStr = "", tempStr;
        try {
            FieldDescriptor col = fieldDescriptor(fieldName);
            if (col != null) {
                if (col.m_isNull)
                    retStr = "null";
                else if (col.m_delayedVal)
                    retStr = m_app.getWebClient().getGenToken(col.m_genIdIndex);
                else {
                    tempStr = col.valueRender(forSqlExpr);
                    switch (col.m_nType) {
                        case JotyTypes._text:
                            tempStr = tempStr.replace("'", "''");
                            if (forSqlExpr)
                                retStr = "'" + tempStr + "'";
                            else
                                retStr = tempStr;
                            break;
                        case JotyTypes._long:
                        case JotyTypes._int:
                        case JotyTypes._double:
                        case JotyTypes._single:
                            retStr = tempStr.isEmpty() ? "null" : tempStr;
                            break;
                        case JotyTypes._date:
                        case JotyTypes._dateTime:
                            retStr = tempStr;
                            break;
                    }
                }
            }
        } catch (Exception e) {
            m_app.jotyMessage(e);
        }
        return retStr;
    }

    public void setActionByStatement(boolean truth) {
        m_actionByStatement = truth;
    }

    protected void initialize() {
        setActionByStatement(webMode());
        m_actionFields = Utilities.m_me.new Stocker();
        m_statementBuilder = new StatementBuilder(m_app);
        m_statementBuilder.helper = new StatementBuilder.StatementBuilderHelper() {

            BasicPostStatement m_contextPostStatement;

            private String availableField(short index, boolean insert) {
                FieldDescriptor col = m_cursor.m_fields[index];
                String fieldName = col.m_strName;
                if (!col.m_toUpdate && insert)
                    m_autoId = fieldName;
                return m_actionFields.contains(fieldName) ? fieldName : null;
            }

            @Override
            public int colCount() {
                return WrappedResultSet.this.colCount();
            }

            @Override
            public String fieldForInsert(short index) {
                return availableField(index, true);
            }

            @Override
            public String fieldForUpdate(short index) {
                return availableField(index, false);
            }

            @Override
            public String getValueForInsert(String fieldName) {
                return getValueStr(fieldName, true);
            }

            @Override
            public String getValueForUpdate(String fieldName) {
                return getValueStr(fieldName, true);
            }

            @Override
            public void setContextPostStatement(BasicPostStatement contextPostStatement) {
                m_contextPostStatement = contextPostStatement;

            }

            @Override
            public String tableName() {
                return m_contextPostStatement == null ? m_tableName : "<JOTY_CTX>";
            }

            @Override
            public String updateWhereClause() {
                String overallStmnt = getSql();
                int whereClausePos = overallStmnt.toUpperCase().indexOf(" WHERE ");
                return whereClausePos > 0 ? overallStmnt.substring(whereClausePos) : "";
            }
        };
    }

    protected void checkSetName(String setName) {
        if (Utilities.isMoreThanOneWord(setName))
            m_app.jotyWarning("A single name is required as table name. However the following statement was got :\n\n" + setName);
    }

    protected String initSql(String setName, String sql, Stocker openForUpdateFields, BasicPostStatement postStatement) {
        m_tableName = setName;
        String retVal = sql;
        if (retVal != null && !Utilities.isMoreThanOneWord(retVal))
            retVal = selectStmnt(retVal);
        else if (retVal == null && m_tableName != null)
            retVal = selectStmnt(m_tableName, openForUpdateFields);
        return retVal;
    }

    protected void typeCheck(FieldDescriptor col, int expectedType) {
        if (col.m_nType != expectedType && (expectedType != JotyTypes._dbDrivenInteger || col.m_nType != JotyTypes._int && col.m_nType != JotyTypes._long))
            m_app.jotyWarning("Joty " + JotyTypes.getVerbose(expectedType) + " type is searched but Joty " + JotyTypes.getVerbose(col.m_nType) + " type was detected in database FIELD : " + col.m_strName);
    }

    protected void innerSetValue(FieldDescriptor col, boolean delayedVal) {
        innerSetValue(col, delayedVal, true);
    }

    protected void innerSetValue(FieldDescriptor col, boolean delayedVal, boolean updateCat) {
        col.m_isNull = false;
        col.m_delayedVal = delayedVal;
        if (updateCat)
            updateCatalog(col.m_strName);
    }

    public JotyDate dateValue(String dbFieldName) {
        return getDescriptor(dbFieldName, JotyTypes._date).m_dateVal;
    }

    public double doubleValue(String dbFieldName) {
        return getDescriptor(dbFieldName, JotyTypes._double).m_dblVal;
    }

    public void edit() {}


    public float floatValue(String dbFieldName) {
        return getDescriptor(dbFieldName, JotyTypes._single).m_fltVal;
    }

    protected FieldDescriptor getDescriptor(String fieldName, int type) {
        FieldDescriptor retVal = fieldDescriptor(fieldName);
        if (m_app.debug()) {
            if (retVal != null)
                typeCheck(retVal, type);
        }
        return retVal;
    }

    /** to deal with unpredictable configuration / dbms design mismatch */
    public long integerValue(String dbFieldName) {
        return getDescriptor(dbFieldName, JotyTypes._dbDrivenInteger).m_nType == JotyTypes._int ? intValue(dbFieldName) : longValue(dbFieldName);
    }

    public int intValue(String dbFieldName) {
        return getDescriptor(dbFieldName, JotyTypes._int).m_iVal;
    }

    public boolean isFieldNull(String fieldName) {
        return fieldDescriptor(fieldName).m_isNull;
    }

    public long longValue(String dbFieldName) {
        return getDescriptor(dbFieldName, JotyTypes._long).m_lVal;
    }

    public byte[] previewValue(String dbFieldName) {
        return getDescriptor(dbFieldName, JotyTypes._smallBlob).m_previewBytes;
    }

    public void setFieldNotToUpdate(String dbFieldName) {
        if (dbFieldName != null && dbFieldName.length() > 0) {
            FieldDescriptor col = fieldDescriptor(dbFieldName);
            col.m_toUpdate = false;
        }
    }

    public void setIntegerValue(String dbFieldName, long val) {
        setIntegerValue(dbFieldName, val, false);
    }

    public void setIntegerValue(String dbFieldName, long val, boolean delayedVal) {
        setIntegerValue(dbFieldName, val, delayedVal, 0);
    }

    public void setIntegerValue(String dbFieldName, long val, boolean delayedVal, boolean updateCat) {
        setIntegerValue(dbFieldName, val, delayedVal, updateCat, 0);
    }

    public void setIntegerValue(String dbFieldName, long val, boolean delayedVal, boolean updateCat, int genIdIndex) {
        FieldDescriptor col = fieldDescriptor(dbFieldName);
        if (m_app.debug())
            typeCheck(col, JotyTypes._dbDrivenInteger);
        if (col.m_nType == JotyTypes._long)
            col.m_lVal = val;
        else
            col.m_iVal = (int) val;
        col.m_genIdIndex = genIdIndex;
        innerSetValue(col, delayedVal, updateCat);
    }

    public void setIntegerValue(String dbFieldName, long val, boolean delayedVal, int genIdIndex) {
        setIntegerValue(dbFieldName, val, delayedVal, true, genIdIndex);
    }

    @Override
    public void setMemberToNull(String fieldName) {
        FieldDescriptor targetFieldDescriptor;
        String strFldName = fieldName.toUpperCase();
        targetFieldDescriptor = m_cursor.m_fieldsMap.get(strFldName);
        if (targetFieldDescriptor != null)
            targetFieldDescriptor.clear();
        if (actionByStatement())
            updateCatalog(fieldName);
    }

    @Override
    public void setValue(String dbFieldName, byte[] bytes) {
        FieldDescriptor col = fieldDescriptor(dbFieldName);
        col.m_previewBytes = bytes;
        innerSetValue(col, false);
    }

    @Override
    public void setValue(String dbFieldName, double val, boolean delayedVal, int genIdIndex) {
        FieldDescriptor col = fieldDescriptor(dbFieldName);
        col.m_dblVal = val;
        col.m_genIdIndex = genIdIndex;
        innerSetValue(col, delayedVal);
    }

    @Override
    public void setValue(String dbFieldName, float val, boolean delayedVal, int genIdIndex) {
        FieldDescriptor col = fieldDescriptor(dbFieldName);
        col.m_fltVal = val;
        col.m_genIdIndex = genIdIndex;
        innerSetValue(col, delayedVal);
    }

    public void setValue(String dbFieldName, int val, boolean delayedVal, int genIdIndex) {
        FieldDescriptor col = fieldDescriptor(dbFieldName);
        if (m_app.debug())
            typeCheck(col, JotyTypes._int);
        col.m_iVal = val;
        col.m_genIdIndex = genIdIndex;
        innerSetValue(col, delayedVal);
    }

    @Override
    public void setValue(String dbFieldName, JotyDate val, boolean delayedVal, int genIdIndex) {
        FieldDescriptor col = fieldDescriptor(dbFieldName);
        col.m_dateVal.setDate(val);
        col.m_genIdIndex = genIdIndex;
        innerSetValue(col, delayedVal);
    }

    public void setValue(String dbFieldName, long val) {
        setValue(dbFieldName, val, false);
    }

    public void setValue(String dbFieldName, long val, boolean delayedVal) {
        setValue(dbFieldName, val, delayedVal, 0);
    }

    public void setValue(String dbFieldName, long val, boolean delayedVal, int genIdIndex) {
        FieldDescriptor col = fieldDescriptor(dbFieldName);
        if (m_app.debug())
            typeCheck(col, JotyTypes._long);
        col.m_lVal = val;
        col.m_genIdIndex = genIdIndex;
        innerSetValue(col, delayedVal);
    }

    @Override
    public void setValue(String dbFieldName, String val, boolean delayedVal, int genIdIndex) {
        FieldDescriptor col = fieldDescriptor(dbFieldName);
        col.m_strVal = val;
        col.m_genIdIndex = genIdIndex;
        innerSetValue(col, delayedVal);
    }

    public void setValue(String fieldName, WrappedField wfield) {
        setValue(fieldName, wfield, false);
    }

    public void setValue(String fieldName, WrappedField wfield, boolean delayedVal) {
        setValue(fieldName, wfield, delayedVal, delayedVal ? wfield.m_posIndexAsReturningValue : 0);
    }

    public void setValue(String fieldName, WrappedField wfield, boolean delayedVal, int genIdIndex) {
        switch (wfield.dataType()) {
            case JotyTypes._text:
                setValue(fieldName, wfield.m_strVal, delayedVal, genIdIndex);
                break;
            case JotyTypes._long:
                setValue(fieldName, wfield.m_lVal, delayedVal, genIdIndex);
                break;
            case JotyTypes._int:
                setValue(fieldName, wfield.m_iVal, delayedVal, genIdIndex);
                break;
            case JotyTypes._single:
                setValue(fieldName, wfield.m_fltVal, delayedVal, genIdIndex);
                break;
            case JotyTypes._double:
                setValue(fieldName, wfield.m_dblVal, delayedVal, genIdIndex);
                break;
            case JotyTypes._date:
            case JotyTypes._dateTime:
                setValue(fieldName, wfield.m_dateVal, delayedVal, genIdIndex);
                break;
            case JotyTypes._dbDrivenInteger:
                setIntegerValue(fieldName, wfield.getInteger(), delayedVal, genIdIndex);
                break;
        }
        if (actionByStatement())
            updateCatalog(fieldName);
    }

    public void setValue(WrappedField wfield) {
        setValue(wfield.dbFieldName(), wfield, wfield.m_delayed, wfield.m_posIndexAsReturningValue);
    }

    public void setValue(WrappedField wfield, boolean delayedVal) {
        setValue(wfield.dbFieldName(), wfield, delayedVal);
    }

    @Override
    public String stringValue(String dbFieldName) {
        return getDescriptor(dbFieldName, JotyTypes._text).m_strVal;
    }

    protected abstract boolean webOpen(boolean forOnlyMetadata, BasicPostStatement postStatement, Object manager);

    public void instantiate(int fieldQty) {
        m_colCount = fieldQty;
        m_cursor = createCursor(fieldQty);
    }

    protected abstract BasicJotyCursor createCursor(int fieldQty);

    public void setMetaData(int conIndex, FieldDescriptor colDescr) {
        m_cursor.m_fieldsMap.put(colDescr.m_strName, colDescr);
        m_cursor.m_fields[conIndex] = colDescr;
    }

    public boolean onOpened(boolean result) {
        AbstractWebClient wClient = m_app.getWebClient();
        AbstractWebClient.DocumentDescriptor docDescriptor = wClient.getDocumentFromRespContent(result);
        m_xml = docDescriptor.xml;
        boolean success = m_xml != null && docDescriptor.success;
        if (success) {
            wClient.loadResultSetStructureFromXml(this, m_xml);
            Element node = (Element) m_xml.getElementsByTagName("Data").item(0);
            if (node != null) {
                m_recNodeList = node.getElementsByTagName("Record");
                m_currNodeIndex = -1;
                if (!getRecordFromNodeList() && ! m_nodeListBOF)
                    success = false;;
            }
        }
        if (m_cursor != null)
            m_cursor.m_closed = !success;
        return success;
    }

    public boolean getRecordFromNodeList() {
        long setDim = m_recNodeList.getLength();
        if (setDim > m_currNodeIndex)
            m_currNodeIndex++;
        m_nodeListEOF = setDim == 0 || setDim == m_currNodeIndex;
        boolean success = true;
        boolean recordGot = false;
        if (!m_nodeListEOF) {
            recordGot = true;
            m_currNode = m_recNodeList.item(m_currNodeIndex);
            if (m_currNode != null) {
                NodeList valueNodes = m_currNode.getChildNodes();
                String fieldVal;
                FieldDescriptor fieldDescriptor;
                Node theNode, valueNode;
                for (int i = 0; i < m_colCount; i++) {
                    fieldDescriptor = m_cursor.m_fields[i];
                    valueNode = valueNodes.item(i);
					/*
					 * fieldOrdinality=true would allow referencing the node
					 * (<c[N]>) by name
					 * (xml.getElementsByTagName(<name>).item(0)) --> not
					 * implemented for performance requirement : it is assumed
					 * that values arrives in the correct order - their nodes
					 * are instances of <c> !
					 */
                    theNode = valueNode.getFirstChild();
                    if (theNode == null)
                        fieldDescriptor.clear();
                    else {
                        fieldVal = theNode.getNodeValue();
                        fieldDescriptor.m_isNull = false;
            	      	Common common = (Common) ((ApplMessenger) m_app).getCommon();
                        switch (fieldDescriptor.m_nType) {
                            case JotyTypes._text:
                                fieldDescriptor.m_strVal = common.m_xmlEncoder.decode(fieldVal, false);
                                break;
                            case JotyTypes._long:
                                fieldDescriptor.m_lVal = Long.parseLong(fieldVal);
                                break;
                            case JotyTypes._int:
                                fieldDescriptor.m_iVal = Integer.parseInt(fieldVal);
                                break;
                            case JotyTypes._double:
                                fieldDescriptor.m_dblVal = Double.parseDouble(fieldVal);
                                break;
                            case JotyTypes._single:
                                fieldDescriptor.m_fltVal = Float.parseFloat(fieldVal);
                                break;
                            case JotyTypes._date:
                            case JotyTypes._dateTime:
                                fieldDescriptor.m_dateVal.setDate(fieldVal, true, fieldDescriptor.m_nType == JotyTypes._dateTime);
                                break;
                            case JotyTypes._blob:
                                break; // embedded management for blobs
                            case JotyTypes._smallBlob:
                                try {
                                    fieldDescriptor.m_previewBytes = common.m_xmlEncoder.decode(fieldVal, true).
                                            getBytes(Utilities.m_singleByteEncoding);
                                } catch (UnsupportedEncodingException e) {
                                    m_app.jotyMessage(e);
                                }
                                break;
                            case JotyTypes._none:
                                break;
                            default:
                                success = false;
                                manageGettingRecordException();
                        }
                    }
                }
            }
        }
        m_nodeListBOF = !recordGot;
        return recordGot && success;
    }

    protected void manageGettingRecordException() {
        try {
            throw new JotyException(reason.WEB_LAB, "Invalid xml record", m_app);
        } catch (JotyException e) {
            m_app.jotyMessage(e);
        }
    }

    protected void next() {
        getRecordFromNodeList();
    }

    public boolean setValueToWField(String dbFieldName, WrappedField wfield) {
        return setValueToWField(dbFieldName, wfield, false);
    }

    public boolean setValueToWField(String dbFieldName, WrappedField wfield, boolean setMetaData) {
        FieldDescriptor col = fieldDescriptor(dbFieldName);
        if (col != null) {
            wfield.setToNull(col.m_isNull);
            wfield.m_jotyTypeFromDb = col.m_nType;
            switch (col.m_nType) {
                case JotyTypes._text:
                    wfield.m_strVal = col.m_strVal;
                    break;
                case JotyTypes._int:
                    wfield.m_iVal = col.m_iVal;
                    break;
                case JotyTypes._long:
                    wfield.m_lVal = col.m_lVal;
                    break;
                case JotyTypes._single:
                    wfield.m_fltVal = col.m_fltVal;
                    break;
                case JotyTypes._double:
                    wfield.m_dblVal = col.m_dblVal;
                    break;
                case JotyTypes._date:
                case JotyTypes._dateTime:
                    wfield.m_dateVal.setDate(col.m_dateVal);
                    break;
                case JotyTypes._smallBlob:
                    wfield.m_previewBytes = col.m_previewBytes;
                    break;
            }
            if (setMetaData) {
                wfield.m_dataType = col.m_nType;
                wfield.m_dbFieldName = dbFieldName;
            }
        }
        return col != null;
    }

}