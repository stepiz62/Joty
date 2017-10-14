package org.joty.mobile.web;

/*
	Copyright (c) 2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

	This file is part of Joty 2.0 mobile.

	Joty 2.0 mobile is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Joty 2.0 mobile is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with Joty 2.0 mobile.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.joty.app.Common;
import org.joty.app.JotyApplication;
import org.joty.app.JotyException;
import org.joty.common.AbstractDbManager;
import org.joty.common.BasicPostStatement;
import org.joty.common.ConfigFile;
import org.joty.common.Utilities;
import org.joty.mobile.app.JotyApp;
import org.joty.mobile.app.JotyApp.ResponseHandlersManager;
import org.joty.web.AbstractWebClient;
import org.joty.web.AbstractWebConn;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.UnsupportedEncodingException;

/**
 * Instantiates a {@code org.joty.mobile.web.WebConn} and makes a concrete
 * implementation of its ancestor for the Joty mobile app: prepares the invocation of the
 * {org.joty.web.AbstractWebClient.doRequest} method to which a
 * {@code ResponseHandlersManager} object, conveniently initialized, is passed as argument; the the
 * class defines the methods the represent the response handling side.
 *
 * @see org.joty.web.AbstractWebClient
 */

public class WebClient extends AbstractWebClient {
    JotyApp m_jotyApp;

    public WebClient(JotyApplication app) {
        super(app);
        m_jotyApp = (JotyApp) app;
    }

    @Override
    protected AbstractWebConn createWebConn() {
        return new WebConn(m_app);
    }

    @Override
    protected boolean doPost(AbstractWebConn webConn, Object manager) {
        webConn.doConnection(manager, true, false);
        return true;
    }

    @Override
    protected boolean doGet(AbstractWebConn webConn, Object manager) {
        webConn.doConnection(manager, false, true);
        return true;
    }

    @Override
    protected void log(String text) {
    }

    /**
     * @see org.joty.workstation.web.WebClient#login
     * @see org.joty.mobile.app.JotyApp#openAccessorWResultSet
     */

    @Override
    public boolean login(Object manager) {
        m_jotyApp.m_common.setApplicationScopeAccessorMode(true);
        m_jotyApp.openAccessorWResultSet(null, (ResponseHandlersManager) manager);
        m_jotyApp.m_common.setApplicationScopeAccessorMode(false);
        return true;
    }

    @Override
    protected boolean doReport(final String renderType) {
        ResponseHandlersManager respManager = m_jotyApp.new ResponseHandlersManager();
        respManager.push(m_jotyApp.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                m_jotyApp.onReport(renderType);
            }
        });
        doRequest(null, respManager);
        return true;
    }

    public void endTransaction(ResponseHandlersManager prmRespManager) {
        m_command = "trans";
        m_buildingRemoteTransaction = false;
        ResponseHandlersManager respManager = prmRespManager == null ? m_jotyApp.new ResponseHandlersManager() : prmRespManager;
        respManager.push(m_jotyApp.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                boolean success = m_responseText != null;
                if (success)
                    success = getDocumentFromRespContent(true, true).success;
                if (success)
                    respManager.popAndcheckToExecute(true);
                else {
                    m_jotyApp.m_currentRespHandlersManager = respManager;
                    new JotyException(JotyException.reason.GENERIC, getCommon().jotyLang("NoTransaction"), m_app);
                    m_jotyApp.m_currentRespHandlersManager.popAndcheckToExecute(false);
                }
            }
        });
        doRequest(null, respManager);
    }

    public void executeCommand(String sql, boolean autoIncrEvent, String autoID, boolean byAccessMethod, BasicPostStatement postStatement, int nonManagedRollbackIdx, ResponseHandlersManager respManager) {
        if (!m_buildingRemoteTransaction)
            prepareReqCommand("exec");
        if (byAccessMethod)
            m_postStatements.add(postStatement);
        else {
            m_autoId = autoID;
            addSqlToPostStmnt(sql, null, (autoIncrEvent && m_jotyApp.m_common.m_autoIncrementByAddNew) || m_jotyApp.remoteAccessorMode() ? postStatement : null, nonManagedRollbackIdx);
        }
        if (!m_buildingRemoteTransaction)
            successfulRequest(is(autoID) || byAccessMethod, respManager);
    }

    public void getConfig(String type) {
        getConfig(type, null);
    }

    /**
     * @see org.joty.workstation.web.WebClient#getConfig
     * @see org.joty.mobile.app.JotyApp#onGonfigurationGot
     */

    public void getConfig(String type, String lang) {
        prepareReqCommand("config");
        addReqParm("type", type);
        if (lang != null)
            addReqParm("lang", lang);
        m_jotyApp.m_configFile = null;
        ResponseHandlersManager respManager = m_jotyApp.new ResponseHandlersManager();
        respManager.setParam("configFileType", type);
        respManager.push(m_jotyApp.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                    DocumentDescriptor docDescriptor = getDocumentFromRespContent(true);
                if (docDescriptor.success) {
                    String configContent = getCommon().m_xmlEncoder.decode(getValue(docDescriptor.xml, "ConfigData"), false);
                    m_jotyApp.m_configFile = new ConfigFile(m_jotyApp);
                    m_jotyApp.m_configFile.buildDoc(configContent);
                    m_jotyApp.onGonfigurationGot((String) respManager.getParam("configFileType"));
                }
            }
        });
        doRequest(null, respManager);
    }

    public AbstractDbManager getDbManager() {
        if (m_dbManager == null)
            m_dbManager = m_jotyApp.m_dbManager;
        return m_dbManager;
    }

    public void  manageCommand(String lpszSQL, boolean autoIncrEvent, String autoID, boolean byAccessMethod, BasicPostStatement postStatement,
                               int nonManagedRollbackIdx, ResponseHandlersManager respManager) {
        if (m_buildingRemoteTransaction) {
            if (autoID != null && autoID.length() > 0)
                m_currentReturnedValueIndex++;
        }
        if (respManager != null) {
            respManager.setParam("isAutoId", is(autoID));
            respManager.push(m_jotyApp.new ResponseHandler() {
                @Override
                public void handle(boolean result, ResponseHandlersManager respManager) {
                    respManager.popAndcheckToExecute(onCommandManaged(result, respManager));
                }
            });
        }
        executeCommand(lpszSQL, autoIncrEvent, autoID, byAccessMethod, postStatement, m_buildingRemoteTransaction ? nonManagedRollbackIdx : 0, respManager);
    }

    public boolean onCommandManaged(boolean result, ResponseHandlersManager respManager) {
        return m_buildingRemoteTransaction ? result : successDocumentFromRespContent(result, (boolean) respManager.getParam("isAutoId"));
    }

    public void  manageCommand(String lpszSQL, boolean autoIncrEvent, String autoID, BasicPostStatement contextPostStatement, int nonManagedRollbackIdx, ResponseHandlersManager respManager) {
        manageCommand(lpszSQL, autoIncrEvent, autoID, false, contextPostStatement, nonManagedRollbackIdx, respManager);
    }

    @Override
    protected boolean doSqlQuery(BasicPostStatement dataDefPostStatement, Object respManager) {
        doRequest(dataDefPostStatement, respManager);
        return true;
    }

    public boolean successDocumentFromRespContent(boolean requestResult, boolean checkGeneratedIDs) {
        return getDocumentFromRespContent(requestResult, checkGeneratedIDs).success;
    }

    public void successfulRequest(boolean checkGeneratedIDs, ResponseHandlersManager respManager) {
        respManager.setParam("checkGeneratedIDs", checkGeneratedIDs);
        respManager.push(m_jotyApp.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                respManager.popAndcheckToExecute(onSuccessfulRequest(result, respManager));
            }
        });
        doRequest(null, respManager);
    }

    public boolean onSuccessfulRequest(boolean result, ResponseHandlersManager respManager) {
        return successDocumentFromRespContent(m_responseText != null, (Boolean) respManager.getParam("checkGeneratedIDs"));
    }

    @Override
    protected  boolean doBinaryUpdate() {
        ResponseHandlersManager respManager = JotyApp.m_app.new ResponseHandlersManager();
        respManager.push(m_jotyApp.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                onBinaryUpdated();
            }
        });
        successfulRequest(false, respManager);
        return true;
    }

    public void onBinaryUpdated() {
       successDocumentFromRespContent(m_responseText != null, false);
    }
	
	@Override
	protected boolean usesManager() {
		return true;
	}

}
