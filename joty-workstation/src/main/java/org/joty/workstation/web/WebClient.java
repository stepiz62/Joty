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

package org.joty.workstation.web;

import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.joty.access.Accessor;
import org.joty.access.Logger;
import org.joty.app.Common;
import org.joty.app.JotyApplication;
import org.joty.app.JotyException;
import org.joty.common.AbstractDbManager;
import org.joty.common.BasicPostStatement;
import org.joty.common.ConfigFile;
import org.joty.common.Utilities;
import org.joty.web.AbstractWebClient;
import org.joty.web.AbstractWebConn;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.WResultSet;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Instantiates a {@code org.joty.workstation.web.WebConn} and makes a concrete
 * implementation of its ancestor for the Joty workstation application.
 * 
 * @see org.joty.web.AbstractWebClient
 */
public class WebClient extends AbstractWebClient {

	Application m_application;
	public static Common m_common;
	
	public WebClient(JotyApplication app) {
		super(app);
		m_application = (Application) app;
		m_common = getCommon();
	}

	@Override
	protected AbstractWebConn createWebConn() {
		return new WebConn(m_app);
	}

   @Override
    protected boolean doPost(AbstractWebConn webConn, Object manager) {
		m_responseText = webConn.doConnection(null, true, false);
        return doEpilog();
    }

    @Override
    protected boolean doGet(AbstractWebConn webConn, Object manager) {
		m_responseText = webConn.doConnection(null, false, m_command.compareToIgnoreCase("end") != 0);
        return doEpilog();
    }
    
    private boolean doEpilog(){
		if (Application.m_debug && Application.m_common.m_configuration != null) {
			Logger.appendToHostLog("Response : \n" + m_responseText + "\n\n", false, true);
			if (m_responseText != null) {
				String xsdName = "JotyResponse.xsd";
				URL xsdURL = getClass().getClassLoader().getResource("res/" + xsdName);
				if (xsdURL != null) {
					if (!Utilities.xsdValidate(Utilities.getXmlDocument(m_responseText), xsdURL, "response"))
						m_responseText = null;
				}
			}
		}
		if (m_responseText == null)
			m_application.m_common.resetRemoteTransactionBuilding();
		return m_responseText != null;
    }

	@Override
    protected void log(String text) {
        if (m_app.debug())
            Logger.appendToHostLog(text, false, true);
    }

	public boolean endTransaction() throws JotyException {
		m_command = "trans";
		m_buildingRemoteTransaction = false;
		boolean success = doRequest();
		if (success)
			success = getDocumentFromRespContent(true, true).success;
		return success;
	}

	public boolean executeCommand(String sql) {
		return executeCommand(sql, false);
	}

	public boolean executeCommand(String sql, boolean autoIncrEvent) {
		return executeCommand(sql, autoIncrEvent, "");
	}

	public boolean executeCommand(String sql, boolean autoIncrEvent, String autoID) {
		return executeCommand(sql, autoIncrEvent, autoID, false, null, 0);
	}

	public boolean executeCommand(String sql, boolean autoIncrEvent, String autoID, boolean byAccessMethod, BasicPostStatement postStatement, int nonManagedRollbackIdx) {
		if (!m_buildingRemoteTransaction)
			prepareReqCommand("exec");
		if (byAccessMethod)
			m_postStatements.add(postStatement);
		else {
			m_autoId = autoID;
			addSqlToPostStmnt(sql, null, (autoIncrEvent && m_common.m_autoIncrementByAddNew) || m_app.remoteAccessorMode() ? postStatement : null, nonManagedRollbackIdx);
		}
		return m_buildingRemoteTransaction ? true : successfulRequest(is(autoID) || byAccessMethod);
	}

	public ConfigFile getConfig(String type) {
		return getConfig(type, null);
	}

	/**
	 * Gets a {@code ConfigFile} from the server.
	 * @param type possible values are {conf, confX, jotyLang, appLang}
	 * @param lang the language identifier in the case {@code type} gets the values {jotyLang, appLang}
	 * @return {@code ConfigFile} object
	 */
	public ConfigFile getConfig(String type, String lang) {
		prepareReqCommand("config");
		addReqParm("type", type);
		if (lang != null)
			addReqParm("lang", lang);
		ConfigFile configuration = null;
		if (doRequest()) {
			DocumentDescriptor docDescriptor = getDocumentFromRespContent(true);
			if (docDescriptor.success) {
				String configContent = m_common.m_xmlEncoder.decode(getValue(docDescriptor.xml, "ConfigData"), false);
				configuration = new ConfigFile(m_app);
				configuration.buildDoc(configContent);
			}
		}
		if (configuration != null)
			configuration.m_type = type;
		return configuration;
	}

	public AbstractDbManager getDbManager() {
		if (m_dbManager == null)
			m_dbManager = Application.m_app.m_dbManager;
		return m_dbManager;
	}

	/**
	 * Drives the framework to operate through the {@code Accessor} object for
	 * querying the database about authentication data. The method, at the latest stage, 
	 * invokes the {@code sqlQuery} method when the static member
	 * {@code Common.m_webSessionOn} is false: this produces the Joty 'login'
	 * command to the server.
	 * 
	 * @return true on success
	 * 
	 * @see Accessor
	 * @see Application#openAccessorWResultSet(String)
	 * @see WResultSet
	 * @see #sqlQuery(String, boolean)
	 * @see Common#m_webSessionOn
	 */

	@Override
	public boolean login(Object manager) {
		boolean retVal = false;
		m_common.setApplicationScopeAccessorMode();
		WResultSet rs = m_application.openAccessorWResultSet(null);
		m_common.setApplicationScopeAccessorMode(false);
		if (rs != null) {
			retVal = true;
			rs.close();
		}
		return retVal;
	}

	public boolean manageCommand(String lpszSQL, boolean autoIncrEvent) {
		return manageCommand(lpszSQL, autoIncrEvent, "", null);
	}

	public boolean manageCommand(String lpszSQL, boolean autoIncrEvent, String autoID, boolean byAccessMethod, BasicPostStatement postStatement, int nonManagedRollbackIdx) {
		boolean success = true;
		if (m_buildingRemoteTransaction) {
			if (autoID != null && autoID.length() > 0)
				m_currentReturnedValueIndex++;
			success = executeCommand(lpszSQL, autoIncrEvent, autoID, byAccessMethod, postStatement, nonManagedRollbackIdx);
		} else
			success = successDocumentFromRespContent(executeCommand(lpszSQL, autoIncrEvent, autoID, byAccessMethod, postStatement, 0), is(autoID));
		m_autoId = "";
		return success;
	}

	public boolean manageCommand(String lpszSQL, boolean autoIncrEvent, String autoID, BasicPostStatement contextPostStatement) {
		return manageCommand(lpszSQL, autoIncrEvent, autoID, false, contextPostStatement, 0);
	}

	public boolean manageCommand(String lpszSQL, boolean autoIncrEvent, String autoID, BasicPostStatement contextPostStatement, int nonManagedRollbackIdx) {
		return manageCommand(lpszSQL, autoIncrEvent, autoID, false, contextPostStatement, nonManagedRollbackIdx);
	}

	@Override
    protected boolean doReport(final String renderType) {
		boolean retVal = doRequest();
		m_application.closeInfoDialog();
		return retVal;
	}

	public boolean sqlQuery(String sql, boolean onlyStructure) {
		return sqlQuery(sql, onlyStructure, false);
	}

	public boolean sqlQuery(String sql, boolean onlyStructure, boolean withBinaries) {
		return sqlQuery(sql, onlyStructure, withBinaries, null, null);
	}


	@Override
	protected boolean doSqlQuery(BasicPostStatement dataDefPostStatement, Object respManager) {
		return doRequest(dataDefPostStatement, respManager);
	}
	
	@Override
    protected  boolean doBinaryUpdate() {
        return successfulRequest(false);
    }

	public boolean successDocumentFromRespContent(boolean requestResult, boolean checkGeneratedIDs) {
		return getDocumentFromRespContent(requestResult, checkGeneratedIDs).success;
	}

	public boolean successfulRequest(boolean checkGeneratedIDs) {
		return successDocumentFromRespContent(doRequest(), checkGeneratedIDs);
	}

	@Override
	protected boolean usesManager() {
		return false;
	}

	
}
