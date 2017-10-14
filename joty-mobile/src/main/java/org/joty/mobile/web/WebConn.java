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

package org.joty.mobile.web;

import android.os.AsyncTask;

import org.joty.app.JotyApplication;
import org.joty.mobile.app.JotyApp;
import org.joty.web.AbstractWebConn;
import org.joty.app.Common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * This implementation works tightly to the {@code ResponseHandlersManager} class and with a built-in
 * implementation of the {@code android.os.AsyncTask} class ( the inner {@code Connector} class ),
 * to allow the running of the implemented response handling code upon a background thread.
 * 
 *
 * @see org.joty.mobile.web.JotyTrustManager
 * @see org.joty.mobile.app.JotyApp.ResponseHandlersManager
 *
 */

public class WebConn extends AbstractWebConn {

     boolean m_commitExit;
    JotyApp.ResponseHandlersManager m_respManager;
    String responseText;
    JotyApp m_app;

    public WebConn(JotyApplication app) {
        super(app);
        m_app = (JotyApp) app;
    }

    public boolean doIt() {
        return  m_respManager == null || ! m_respManager.m_exceptionalResponse;
    }

    @Override
    protected TrustManager createJotyTrustManager() throws Exception {
        return new JotyTrustManager(m_app);
    }

    @Override
    protected String connect() {
        new WebConn.Connector().execute((String) null);
        return null;
    }

    @Override
    protected void manageException(IOException e) {
        m_app.m_messageText = "Trouble in setting the web connection !";
        ((Common) m_app.getCommon()).m_commitExit = true;
    }

    @Override
    protected  void setManager (Object manager) {
        m_respManager = (JotyApp.ResponseHandlersManager) manager;
        if (m_respManager != null)
            m_respManager.m_webConn = this;
    }

    public class Connector extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... urls) {
            try {
                responseText = null;
                m_commitExit = false;
                if (m_respManager != null)
                    m_respManager.m_exceptionalResponse = false;
                m_app.m_currentRespHandlersManager = m_respManager;
                if (m_post) {
                    OutputStreamWriter out = new OutputStreamWriter(m_conn.getOutputStream());
                    if (doIt())
                        out.write(m_postContent);
                    out.close();
                } else
                    m_conn.connect();

                if (doIt()) {
                    responseCode = m_conn.getResponseCode();
                    InputStream is = m_conn.getInputStream();
                    responseText = getResp(is);
                }
            } catch (IOException e1) {
                if (e1 instanceof SSLHandshakeException)
                     m_app.m_messageText = m_app.jotyLang("CertAuthNotPossible");
                else
                    m_app.m_messageText = String.format("The response code was %1$d\n\n" + e1.getMessage() +
                                    (responseCode == 0 ? ("\n\n" + (m_ssl ? "Check whether ssl is enabled on the server !" : "(JotyServer did not respond !)")) : ""),
                            responseCode);
                m_commitExit = true;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (doIt()) {
                m_app.getWebClient().m_responseText = responseText;
                boolean success = responseText != null;
                if ( ! success) {
                    m_app.resetRemoteTransactionBuilding();
                    if (m_commitExit)
                        m_app.m_common.m_commitExit = m_commitExit;
                    m_app.warningMsg(m_app.m_messageText);
                }
                if (m_respManager != null)
                    m_respManager.checkToExecute(success);
            }
        }
    }


}
