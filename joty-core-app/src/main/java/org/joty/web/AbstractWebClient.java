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

package org.joty.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.joty.app.Common;
import org.joty.app.JotyApplication;
import org.joty.app.JotyException;
import org.joty.common.AbstractDbManager;
import org.joty.common.ApplMessenger;
import org.joty.common.BasicPostStatement;
import org.joty.common.ParamContext;
import org.joty.common.ReportManager;
import org.joty.common.Utilities;
import org.joty.common.Utilities.Stocker;
import org.joty.data.FieldDescriptor;
import org.joty.data.JotyResultSet;
import org.joty.data.WrappedResultSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * It is the talker to the Joty Server (see {@code org.joty.server.JotyServer})
 * via the http connection built on an implementation of {@code AbstractWebConn}
 * object (the {@code createWebConn} abstract method drives the descendant to
 * make the inherent choice). Strictly joined with the {@code JotyApplication} instance
 * and with the {@code WrappedResultSet} it enables the client side of the Joty
 * technology to access the dbms via the web.
 * <p>
 * The http POST command is packaged through the use of the
 * {@code BasicPostStatement} class that serves to build the unit of
 * information either the Accessor mode is active or not. This atom of
 * information arrives to the instance of this class already partially compiled
 * by the calling context and the class take cares of transforming and
 * definitely encoding the information, realizing in this way the Joty 2.0
 * communication protocol understandable by the Joty Server (see
 * {@code #doRequest}).
 * <p>
 * The class is then equipped with the data structures useful to receive the
 * response from the server and provides a set of methods useful to process it,
 * one of the more important of them is {@code #getDocumentFromRespContent} that
 * creates the base (a {@code org.w3c.dom.Document} object) of further analysis:
 * the extraction of the data result set, of the returned values , of the reason
 * of failure, of a large binary content.
 *
 * 
 * @see AbstractWebConn
 * @see BasicPostStatement
 * @see WrappedResultSet
 */

public abstract class AbstractWebClient {
    public class DocumentDescriptor {
        public boolean gotSessionExpired;
        public Document xml = null;
        public boolean success = false;
    }

    protected DocumentBuilder m_xDocBuilder;
    protected String m_myHost;
    String m_myPort;
    String m_servletPath;
    public String m_sessionID;
    public String m_command;
    protected boolean m_buildingRemoteTransaction;
    protected Vector<String> m_moreReqParms;
    protected Vector<String> m_moreReqParmsValues;
    protected String m_queryStmnt;
    public Vector<BasicPostStatement> m_postStatements;
    public int m_currentReturnedValueIndex;
    public boolean m_urlValid;
    public String m_responseText;
    public Vector<String> m_returnedValues;
    protected byte[] m_bytes;
    protected byte[] m_auxiliaryBytes;
    protected JotyApplication m_app;
    public String m_autoId;
    protected String m_authServerPath;
    protected Vector<ReportManager.Parameter> m_reportParams;
    public boolean m_responseSuccess;
    protected Stocker m_smallBlobs;
    protected boolean m_genTableJustSpecified;
    protected AbstractDbManager m_dbManager;
    protected ParamContext m_refTransParamContext;

    public AbstractWebClient(JotyApplication app) {
        m_app =  app;
        m_moreReqParms = new Vector<String>();
        m_moreReqParmsValues = new Vector<String>();
        m_postStatements = new Vector<BasicPostStatement>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            m_xDocBuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            m_app.jotyMessage(e);
        }
        String myDomain;
        int domainStartingPos = getCommon().m_appUrl.indexOf("//") + 2;
        int domainEndingPos = getCommon().m_appUrl.indexOf("/", domainStartingPos);
        m_urlValid = domainEndingPos > domainStartingPos;
        if (m_urlValid) {
            myDomain = getCommon().m_appUrl.substring(domainStartingPos, domainEndingPos);
            domainEndingPos = myDomain.indexOf(":");
            if (domainEndingPos >= 0) {
                m_myHost = myDomain.substring(0, domainEndingPos);
                m_myPort = myDomain.substring(domainEndingPos + 1);
            } else
                m_myHost = myDomain;
            m_servletPath = getCommon().m_appUrl + getCommon().m_servlet;
            m_buildingRemoteTransaction = false;
            m_sessionID = "";
            m_returnedValues = new Vector<String>();
        } else
            m_app.jotyWarning("Url is not valid !");
    }

    private void addBinaryNode(Document xml, Element rootElem, boolean auxiliary) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(auxiliary ? m_auxiliaryBytes : m_bytes);
        } catch (IOException e) {
            m_app.jotyMessage(e);
        }
        try {
            addXmlNode(xml, "Binary", rootElem, getCommon().m_xmlEncoder.encode(baos.toString(Utilities.m_singleByteEncoding), true));
        } catch (UnsupportedEncodingException e) {
            m_app.jotyMessage(e);
        }
    }

    void addPostStatement(Document xml, BasicPostStatement postStatement, String elemName, Element parent, boolean inTransaction) {
        boolean lookAtItemsBuildFromDirty = false;
        if (postStatement.m_paramContext != null) {
            if (m_refTransParamContext == null)
                m_refTransParamContext = postStatement.m_paramContext;
            else if (postStatement.m_paramContext == m_refTransParamContext)
                lookAtItemsBuildFromDirty = true;
        }
        Element stmntElem = addXmlNode(xml, elemName, parent);
        addXmlNode(xml, "SqlStmnt", stmntElem, postStatement.m_sql);
        addXmlNode(xml, "AutoId", stmntElem, postStatement.m_autoId);
        addXmlNode(xml, "GenTable", stmntElem, postStatement.m_genTable);
        addXmlNode(xml, "VerifyExpr", stmntElem, postStatement.m_verifyExpr);
        addXmlNode(xml, "Method", stmntElem, postStatement.m_method);
        addXmlNode(xml, "FOPP", stmntElem, postStatement.m_firstOutParamPos);
        addXmlNode(xml, "OPQ", stmntElem, postStatement.m_outParamsQty);

        addXmlNode(xml, "AccessContext", stmntElem, postStatement.m_AccessorContext);
        addXmlNode(xml, "PanelIdx", stmntElem, postStatement.m_dataPanelIdx);
        addXmlNode(xml, "TermName", stmntElem, postStatement.m_termName);
        addXmlNode(xml, "MainFilter", stmntElem, postStatement.m_mainFilter);
        addXmlNode(xml, "SortExpr", stmntElem, postStatement.m_sortExpr);
        addXmlNode(xml, "Iteration", stmntElem, postStatement.m_iteration);

        if (postStatement.m_nonManagedRollbackActionIden != 0)
            addXmlNode(xml, "NMRA", stmntElem, String.valueOf(postStatement.m_nonManagedRollbackActionIden));
        Element childElem = addXmlNode(xml, "Items", stmntElem);
        Element itemElem = null;
        for (BasicPostStatement.Item item : postStatement.m_items) {
            if (!lookAtItemsBuildFromDirty || item.m_buildFromDirtyPrm) {
                itemElem = addXmlNode(xml, "Item", childElem);
                addXmlNode(xml, "Name", itemElem, item.name);
                addXmlNode(xml, "Val", itemElem, item.valueLiteral);
                addXmlNode(xml, "Type", itemElem, String.valueOf(item.type));
            }
        }
    }

    protected void addReqParm(String param, String value) {
        m_moreReqParms.add(param);
        m_moreReqParmsValues.add(value);
    }

    public void addSqlToPostStmnt(String sql, String genTable) {
        addSqlToPostStmnt(sql, genTable, null, 0);
    }

    public void addSqlToPostStmnt(String sql, String genTable, BasicPostStatement postStmntPrm, int nonManagedRollbackIdx) {
        addSqlToPostStmnt(sql, genTable, postStmntPrm, null, nonManagedRollbackIdx);
    }

    public void addSqlToPostStmnt(String sql, String genTable, BasicPostStatement postStmntPrm, String genIdExpr, int nonManagedRollbackIdx) {
        if (genTable != null && genTable.length() > 0) {
            m_currentReturnedValueIndex++;
            m_genTableJustSpecified = true;
        }
        BasicPostStatement postStmnt = postStmntPrm == null ? new BasicPostStatement(m_app) : postStmntPrm;
        postStmnt.m_sql = sql;
        postStmnt.m_nonManagedRollbackActionIden = nonManagedRollbackIdx;
        if (postStmntPrm == null || m_app.remoteAccessorMode())
            postStmnt.m_autoId = m_autoId;
        if (genTable != null)
            postStmnt.m_genTable = genTable;
        m_postStatements.add(postStmnt);
    }

    public boolean addVerifyExpr(String m_validationExpr) {
        BasicPostStatement postStmnt = new BasicPostStatement(m_app);
        postStmnt.m_verifyExpr = m_validationExpr;
        m_postStatements.add(postStmnt);
        return true;
    }

    Element addXmlNode(Document xml, String tagName) {
        return addXmlNode(xml, tagName, null);
    }

    Element addXmlNode(Document xml, String tagName, Element parent) {
        return addXmlNode(xml, tagName, parent, null);
    }

    Element addXmlNode(Document xml, String tagName, Element parent, String value) {
        Element retVal = xml.createElement(tagName);
        if (value != null)
            retVal.appendChild(xml.createTextNode(value));
        if (parent != null)
            parent.appendChild(retVal);
        else
            xml.appendChild(retVal);
        return retVal;
    }

    public void beginTransaction() {
        resetReqBodyTerms();
        m_buildingRemoteTransaction = true;
    }

    /** Informs whether the definition of a "delayed transaction" is being built */
    public boolean buildingRemoteTransaction() {
        return m_buildingRemoteTransaction;
    }

    /** Clears definition process of a "delayed transaction" */
    public void buildingRemoteTransaction_reset() {
        m_buildingRemoteTransaction = false;
        resetReqBodyTerms();
    }

    public int returnedValuesAvailablePos() {
        int retVal = m_currentReturnedValueIndex + (m_genTableJustSpecified ? 0 : 1);
        m_genTableJustSpecified = false;
        return retVal;
    }

    protected boolean  doRequest(BasicPostStatement queryDefPostStatement, Object manager) {
        m_refTransParamContext = null;
        boolean bodyToDeliver =
                (! usesManager() || manager != null) &&
                        (m_postStatements.size() > 0 ||
                                m_queryStmnt != null ||
                                queryDefPostStatement != null ||
                                m_reportParams != null && m_reportParams.size() > 0);
        String urlStr = getCommon().m_secure ? m_authServerPath : m_servletPath;
        m_responseText = null;
        if (m_sessionID.length() > 0) {
            String sessionIDextension;
            sessionIDextension = String.format(";jsessionid=%1$s", m_sessionID);
            urlStr += sessionIDextension;
        }
        urlStr += "?command=" + m_command;
        for (int i = 0; i < m_moreReqParms.size(); i++) {
            urlStr += "&";
            urlStr += m_moreReqParms.get(i) + "=" + m_moreReqParmsValues.get(i);
        }
        log("URL requested : \n" + urlStr);
        AbstractWebConn webConn = createWebConn();
        webConn.m_url = urlStr;
        if (bodyToDeliver) {
            Document xml = m_xDocBuilder.newDocument();
            Element rootElem = addXmlNode(xml, "JotyReq");

            if (m_app.debug()) {
                rootElem.setAttribute("xmlns", "http://www.joty.org");
                rootElem.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                rootElem.setAttribute("xsi:schemaLocation", "http://www.joty.org JotyRequest.xsd");
            }
            if (m_command.compareToIgnoreCase("report") == 0) {
                Element repParmsElem = addXmlNode(xml, "ReportParms", rootElem);
                Element paramElem = null;
                for (int i = 0; i < m_reportParams.size(); i++) {
                    paramElem = addXmlNode(xml, "Item", repParmsElem);
                    addXmlNode(xml, "Name", paramElem, m_reportParams.get(i).name);
                    addXmlNode(xml, "Val", paramElem, m_reportParams.get(i).render());
                    addXmlNode(xml, "Type", paramElem, String.valueOf(m_reportParams.get(i).type));
                }
            } else {
                if (m_command.compareToIgnoreCase("trans") == 0 || m_command.compareToIgnoreCase("exec") == 0) {
                    Element stmntsElem = addXmlNode(xml, "Stmnts", rootElem);
                    for (BasicPostStatement postStatement : m_postStatements)
                        addPostStatement(xml, postStatement, "Stmnt", stmntsElem, true);
                } else {
                    if (m_queryStmnt != null)
                        addXmlNode(xml, "QueryStmnt", rootElem, m_queryStmnt);
                    if (queryDefPostStatement != null)
                        addPostStatement(xml, queryDefPostStatement, "QueryDef", rootElem, false);
                    if (m_smallBlobs != null && m_smallBlobs.size() > 0) {
                        Element smallBlobsElem = addXmlNode(xml, "SmallBlobs", rootElem);
                        for (String fieldName : m_smallBlobs)
                            addXmlNode(xml, "Field", smallBlobsElem, fieldName);
                    }
                }
            }
            if (m_bytes != null) {
                addBinaryNode(xml, rootElem, false);
                if (m_auxiliaryBytes != null)
                    addBinaryNode(xml, rootElem, true);
            }
            resetReqBodyTerms();
            webConn.m_postContent = "<?xml version='1.0' encoding='UTF-8' ?>" + getXmlContent(xml);
            log("Post content : \n" + webConn.m_postContent);
            return doPost(webConn, manager);
        } else
            return doGet(webConn, manager);
    }


    protected abstract boolean usesManager();

    protected abstract  AbstractWebConn createWebConn();


    protected abstract boolean doPost(AbstractWebConn webConn, Object manager);
    protected abstract boolean doGet(AbstractWebConn webConn, Object manager);

    abstract protected void log(String text);


    public DocumentDescriptor getDocumentFromRespContent(boolean success) {
        return getDocumentFromRespContent(success, false);
    }

    public DocumentDescriptor getDocumentFromRespContent(boolean success, boolean checkGeneratedIds) {
        DocumentDescriptor docDescriptor = new DocumentDescriptor();
        if (success &&  m_responseText != null &&  m_responseText.length() > 0) {
            try {
                docDescriptor.xml = m_xDocBuilder.parse(new ByteArrayInputStream(m_responseText.getBytes("UTF-8")));
            } catch (Exception e) {
                m_app.jotyMessage(e);
            }
            if (docDescriptor.xml != null) {
                getSuccess(docDescriptor);
                if (docDescriptor.success && checkGeneratedIds)
                    getGeneratedIDs(docDescriptor.xml);
                if (!docDescriptor.gotSessionExpired)
                    getSessionID(docDescriptor.xml);
            }
        }
        return docDescriptor;
    }

    private void getGeneratedIDs(Document xml) {
        m_returnedValues.removeAllElements();
        String gotValue;
        Node node = xml.getElementsByTagName("GenIDs").item(0);
        if (node != null)
            node = node.getFirstChild();
        while (node != null) {
            gotValue = node.getFirstChild().getNodeValue();
            if (gotValue == null || gotValue.length() == 0)
                break;
            else
                m_returnedValues.add(gotValue);
            node = node.getNextSibling();
        }
    }

    public String getGenToken(int idIndex) {
        if (idIndex == 0 && m_currentReturnedValueIndex == 0) {
            m_app.jotyWarning("Rendering a delayed term is not possible due to incorrect setting of the genIDs counter !");
            return null;
        } else {
            int theIndex = idIndex == 0 ? m_currentReturnedValueIndex : idIndex;
            String retVal;
            retVal = String.format("'<GenID%1$d>'", theIndex);
            return retVal;
        }
    }

    public String getNodeContent(Element node) {
        return node.getFirstChild().getNodeValue();
    }

    public String getReturnedValue(int index) {
        if (m_returnedValues.size() > index)
            return m_returnedValues.get(index);
        else
            return null;
    }

    void getSessionID(Document xml) {
        Element node = (Element) xml.getElementsByTagName("S_ID").item(0);
        m_sessionID = node == null ? "" : node.getFirstChild().getNodeValue();
    }

    public void getSuccess(DocumentDescriptor descriptor) {
        boolean success = false;
        Element node = (Element) descriptor.xml.getElementsByTagName("Value").item(0);
        if (node != null)
            success = node.getFirstChild().getNodeValue().compareToIgnoreCase("Ok") == 0;
        descriptor.gotSessionExpired = false;
        if (!success && ! getCommon().m_commitExit)
            getWarning(descriptor);
        descriptor.success = success;
        m_responseSuccess = success;
    }

    String getValue(Document xml, int i) {
        return getValue(xml, String.format("c%1$d", i));
    }

    public String getValue(Document xml, String nodeExpr) {
        String nodeValue = null;
        Element node = (Element) xml.getElementsByTagName(nodeExpr).item(0);
        if (node != null)
            nodeValue = node.getFirstChild().getNodeValue();
        return nodeValue;
    }

    public void getWarning(DocumentDescriptor descriptor)  {
        Document xml = descriptor.xml;
        Element reasonNode = (Element) xml.getElementsByTagName("Reason").item(0);
        Element codeNode = null;
        NodeList nodeList = xml.getElementsByTagName("Code");
        if (nodeList.getLength() > 0)
            codeNode = (Element) xml.getElementsByTagName("Code").item(0);
        if (reasonNode != null) {
            Node reasonContent = reasonNode.getFirstChild();
            String reasonValue = reasonContent == null ? "Unknown" : getCommon().m_xmlEncoder.decode(reasonContent.getNodeValue(), false);

            Node codeContent = null;
            if (codeNode != null)
                codeContent = codeNode.getFirstChild();
            String codeValue = codeContent == null ? "" : codeContent.getNodeValue();
            decodeWarningValue(reasonValue, codeValue, descriptor);
        }
    }

    public abstract AbstractDbManager getDbManager();

    protected void decodeWarningValue(String reasonValue, String codeValue, DocumentDescriptor descriptor) {
        try {
            if (reasonValue.indexOf("SESSION_EXP") >= 0
                    && codeValue.length() == 0)
                throw new JotyException(JotyException.reason.SESSION_EXPIRED, null, m_app);
            else if (reasonValue.indexOf("NO_BIRT") >= 0
                    && codeValue.length() == 0)
                throw new JotyException(JotyException.reason.NO_BIRT_ENGINE, null, m_app);
            else {
                if (getDbManager() != null)
                    getCommon().checkAndThrow(getDbManager(), reasonValue, codeValue);
                throw new JotyException(JotyException.reason.WEB_LAB, reasonValue, m_app);
            }
        } catch (JotyException e) {
            m_app.jotyMessage(e);
            descriptor.gotSessionExpired = e.m_webReason == JotyException.reason.SESSION_EXPIRED;
        }
    }

    String getXmlAttrVal(Node node, String attrName) {
        return node.getAttributes().getNamedItem(attrName).getNodeValue();
    }

    int getXmlAttrValInt(Node node, String attrName) {
        String attrVal = getXmlAttrVal(node, attrName);
        return attrVal.length() > 0 ? Integer.parseInt(attrVal) : 0;
    }

    String getXmlBinaryContent(Document xml) {
        return getValue(xml, "Binary");
    }

    public String getXmlContent(Document xml) {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = tf.newTransformer();
        } catch (TransformerConfigurationException e) {
            m_app.jotyMessage(e);
        }
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        try {
            transformer.transform(new DOMSource(xml), new StreamResult(writer));
        } catch (TransformerException e) {
            m_app.jotyMessage(e);
        }
        return writer.getBuffer().toString();

    }

    protected boolean is(String strVal) {
        return strVal != null && strVal.length() > 0;
    }

    public void loadResultSetStructureFromXml(WrappedResultSet resultSet, Document xml) {
        FieldDescriptor colDescr;
        int fieldListLenght;
        Element node = (Element) xml.getElementsByTagName("Structure").item(0);
        if (node != null) {
            NodeList nodeList = node.getElementsByTagName("Field");
            fieldListLenght = nodeList.getLength();
            Node fieldNode;
            resultSet.instantiate(fieldListLenght);
            for (int i = 0; i < fieldListLenght; i++) {
                fieldNode = nodeList.item(i);
                if (fieldNode != null) {
                    colDescr = new FieldDescriptor(m_app);
                    colDescr.m_strName = getXmlAttrVal(fieldNode, "name");
                    colDescr.m_nType = getXmlAttrValInt(fieldNode, "type");
                    colDescr.m_lLength = getXmlAttrValInt(fieldNode, "len");
                    colDescr.m_nScale = getXmlAttrValInt(fieldNode, "dec");
                    if (getCommon().m_fieldOrdinality)
                        getXmlAttrValInt(fieldNode, "dec");
                    colDescr.m_pos = (getCommon().m_fieldOrdinality ? getXmlAttrValInt(fieldNode, "pos") : i);
                    colDescr.m_lPrecision = 0; // unused in webmode
                    colDescr.m_nSqlType = -1; // "
                    colDescr.m_isNull = false;
                    resultSet.setMetaData(i, colDescr);
                }
            }
        }
    }

    public abstract boolean login(Object manager);

    public BasicPostStatement prepareAddNewItems(JotyResultSet rs) {
        String fieldName;
        FieldDescriptor col = null;
        BasicPostStatement retVal = new BasicPostStatement(m_app);
        retVal.m_genTable = rs.getTableName();
        for (short fldIdx = 0; fldIdx < rs.getColCount(); fldIdx++) {
            col = rs.getFieldDescriptor(fldIdx);
            fieldName = col.m_strName;
            if (!col.m_toUpdate)
                retVal.m_autoId = fieldName;
            if (rs.actionFieldsContains(fieldName))
                retVal.addItem(fieldName, rs.getValueStr(fieldName, false));
        }
        if (retVal.m_autoId == null)
            m_app.jotyWarning("Unable to identify auto-increment FIELD !");
        return retVal;
    }

    protected void prepareReqCommand(String command) {
        prepareReqCommand(command, null);
    }

    void prepareReqCommand(String command, String moreParms) {
        resetReqBodyTerms();
        m_command = command;
    }

    public boolean  report(String name, final String renderType, boolean twoProcess, Vector<ReportManager.Parameter> m_params) {
        m_app.volatileMessage("InitializingRepEngineMsg", false);
        m_app.setWaitCursor(true);
        prepareReqCommand("report");
        addReqParm("name", name);
        addReqParm("type", renderType);
        addReqParm("lang", getCommon().m_language);
        addReqParm("twoProc", twoProcess ? "y" : "n");
        m_reportParams = m_params;
        return doReport(renderType);
    }

    protected abstract boolean doReport(String renderType);

    protected void resetReqBodyTerms() {
        m_command = null;
        m_queryStmnt = null;
        m_bytes = null;
        m_auxiliaryBytes = null;
        m_autoId = null;
        if (!m_buildingRemoteTransaction) {
            m_postStatements.removeAllElements();
            m_currentReturnedValueIndex = 0;
        }
        m_moreReqParms.removeAllElements();
        m_moreReqParmsValues.removeAllElements();
    }

    public void setAuthServerUrl() {
        if (getCommon().m_authServer == null)
            m_authServerPath = getCommon().m_appUrl.replace("http://", "https://").
                    replace(m_myPort == null ? m_myHost : m_myPort,
                            m_myPort == null ?
                                    m_myHost + ":" + getCommon().m_sslPort :
                                    getCommon().m_sslPort)
                    + getCommon().m_servlet;
        else
            m_authServerPath = getCommon().m_authServer;
    }

    public void setSmallBlobsList(Stocker list) {
        m_smallBlobs = list;
    }


    public boolean sqlQuery(String sql, boolean onlyStructure, boolean withBinaries, BasicPostStatement dataDefPostStatement, Object respManager) {
        if (getCommon().m_webSessionOn) {
            prepareReqCommand("query");
            if (withBinaries)
                addReqParm("bin", "y");
            addReqParm("data", onlyStructure ? "n" : "y");
        } else {
            prepareReqCommand("login");
            addReqParm("user", getCommon().m_userName);
            addReqParm("pwd", getCommon().m_password);
            if (getCommon().m_shared)
                addReqParm("shK", getCommon().m_sharingKey);
        }
        m_queryStmnt = sql;
        return doSqlQuery(dataDefPostStatement, respManager);
    }

    protected abstract boolean doSqlQuery(BasicPostStatement dataDefPostStatement, Object respManager);


    public boolean updateBinary(String sql, byte[] bytes, boolean auxiliary, BasicPostStatement postStatement) {
        if (auxiliary && !m_buildingRemoteTransaction) {
            m_app.jotyWarning("As auxiliary call must be embedded in a transaction with the not auxiliary call !");
            return true;
        } else {
            if (!auxiliary)
                prepareReqCommand("exec");
            if (auxiliary)
                m_auxiliaryBytes = bytes;
            else
                m_bytes = bytes;
            addSqlToPostStmnt(sql, null, postStatement, 0);
            return m_buildingRemoteTransaction ? true : doBinaryUpdate();
        }
    }

    protected abstract boolean doBinaryUpdate();

    protected boolean doRequest() {
        return doRequest(null, null);
    }


    public boolean endSession() {
        prepareReqCommand("end");
        return doRequest();
    }

    public byte[] getBytesFromRespDocument(DocumentDescriptor docDescriptor, String elementTag) {
		byte[] retVal = null;
		if (docDescriptor.success) {
			Element node = (Element) docDescriptor.xml.getElementsByTagName(elementTag).item(0);
			if (node != null) {
				Node contentNode = node.getFirstChild();
				if (contentNode != null) {
					String nodeVal = contentNode.getNodeValue();
					if (nodeVal != null)
						try {
							retVal = getCommon().m_xmlEncoder.decode(nodeVal, true).getBytes(Utilities.m_singleByteEncoding);
						} catch (UnsupportedEncodingException e) {
							m_app.jotyMessage(e);
						}
				}
			}
		}
		return retVal;
	}

	protected Common getCommon() {
		return (Common) ((ApplMessenger) m_app).getCommon();
	}
}
