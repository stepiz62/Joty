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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.*;

import org.joty.access.Logger;
import org.joty.app.JotyApplication;
import org.joty.web.AbstractWebConn;
import org.joty.workstation.app.Application;

public class WebConn extends AbstractWebConn {

	public WebConn(JotyApplication app) {
		super(app);
	}

	@Override
	protected TrustManager createJotyTrustManager() throws Exception {
		return new JotyTrustManager(Application.m_app);
	}

	@Override
	protected String connect() {
		String retVal = null;
		try {
			if (m_post) {
				OutputStreamWriter out = new OutputStreamWriter(m_conn.getOutputStream());
				out.write(m_postContent);
				out.close();
			} else {
				m_conn.connect();
			}
			responseCode = m_conn.getResponseCode();
			InputStream is = m_conn.getInputStream();
			retVal = getResp(is);
		} catch (IOException e1) {
			manageException(e1);
		}
		return retVal;
	}

	@Override
	protected void manageException(IOException e) {
		if (e instanceof SSLHandshakeException) {
			Application.m_common.resetRemoteTransactionBuilding();
			Application.m_common.m_commitExit = true;
			Application.langWarningMsg("CertAuthNotPossible");
		} else
			m_app.JotyMsg(this, String.format("The response code was %1$d\n\n" + e.getMessage() + 
														(responseCode == 0 ?  ("\n\n" + (m_ssl ? "Check whether ssl is enabled on the server !" : "(JotyServer did not respond !)")) : ""), 
													responseCode));
		if (m_ssl)
			Application.m_common.m_commitExit = true;
	}

	@Override
	protected void setManager(Object manager) {
	}

}
