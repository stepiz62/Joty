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
import org.joty.mobile.web.WebClient;
import org.joty.mobile.app.JotyApp;
import org.joty.common.AbstractDbManager;

/**
 * Similar to {@code org.joty.workstation.data.JotyDB}, it is a subset of that class that is customized
 * for facing with the Android technology and for this purpose it adopts a ResponseHandlersManager in all its derived methods.
 *<p>
 * Together with the {@code org.joty.mobile.data.WResultSet} class realizes
 * the interface with the virtual data level provided by the {@code org.joty.mobile.web.WebClient} class.
 *
 * @see org.joty.mobile.app.JotyApp.ResponseHandlersManager
 * @see org.joty.workstation.data.JotyDB
 *
 */
public class JotyDB {

	JotyApp m_app;

	private JotyApp application() {
		if (m_app == null)
			m_app = JotyApp.m_app;
		return m_app;
	}

	public void executeSQL(String sql, JotyApp.ResponseHandlersManager respManager) {
		executeSQL(sql, null, respManager);
	}

	public void executeSQL(String sql, String autoID, JotyApp.ResponseHandlersManager respManager) {
        executeSQL(sql, autoID, null, respManager);
	}

	public void executeSQL(String sql, String autoID, BasicPostStatement contextPostStatement, JotyApp.ResponseHandlersManager respManager) {
		executeSQL(sql, autoID, contextPostStatement, 0, respManager);
	}
	
	public void executeSQL(String sql, String autoID, BasicPostStatement contextPostStatement, int nonManagedRollbackAction, JotyApp.ResponseHandlersManager respManager) {
		application().m_webClient.manageCommand(sql, autoID != null, autoID, contextPostStatement, nonManagedRollbackAction, respManager);
	}

	public void getBytesFromDb(String sql, BasicPostStatement postStatement, JotyApp.ResponseHandlersManager respManager) {
		String querySql = null;
		WebClient wClient = application().m_webClient;
		if (m_app.remoteAccessorMode())
			postStatement.m_sql = sql;
		else
			postStatement = null;
        respManager.push(m_app.new ResponseHandler() {
            @Override
            public void handle(boolean result, JotyApp.ResponseHandlersManager respManager) {
                if (result) {
                    byte[] bytes = getBytesFromResponse();
                    if (bytes == null || bytes.length == 0)
                        m_app.toast(m_app.jotyLang("EmptyBlob"));
                    else {
                        respManager.setParam("bytes", bytes);
                        respManager.popAndcheckToExecute(true);
                    }
                }
            }
        });
        wClient.sqlQuery(querySql, false, true, postStatement, respManager);
	}

    public byte[] getBytesFromResponse() {
        byte[] bytes = null;
        WebClient wClient = application().m_webClient;
        WebClient.DocumentDescriptor docDescriptor = wClient.getDocumentFromRespContent(wClient.m_responseText != null);
        if (docDescriptor.xml != null)
            bytes = wClient.getBytesFromRespDocument(docDescriptor, application().m_common.m_fieldOrdinality ? "c1" : "c");
        return bytes;
    }

    public void beginTrans() {
        application().m_webClient.beginTransaction();
    }

    public void commitTrans(JotyApp.ResponseHandlersManager respManager)  {
        endTrans(respManager);
    }

    void endTrans(JotyApp.ResponseHandlersManager respManager){
         application().m_webClient.endTransaction(respManager);
    }

}
