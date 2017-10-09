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

import org.joty.app.JotyApplication;
import org.joty.common.JotyMessenger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * This class is responsible to manage the http connection with server. When it
 * detects the protocol in the url, it switches in ssl mode delegating a {@code  AbstractJotyTrustManager} instance for the
 * certificate checking or for triggering the management of it.
 *
 * @see  AbstractJotyTrustManager
 */

public abstract class AbstractWebConn {
    public String m_url;
    public String m_postContent;
    protected int responseCode;
    public boolean m_post;
    protected HttpURLConnection m_conn;
    protected boolean m_ssl;
    protected JotyApplication m_app;

    public AbstractWebConn(JotyApplication app) {
        super();
        m_app = app;
    }

    protected static String getResp(InputStream is) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        char[] charBuffer = new char[1];
        StringBuffer responseBuffer = new StringBuffer();
        while ((rd.read(charBuffer)) > 0)
            responseBuffer.append(charBuffer);
        rd.close();
        return responseBuffer.toString();
    }

    public String doConnection(Object manager, boolean post, boolean waitingGet) {
        setManager(manager);
        m_post = post;
        m_ssl = m_url.indexOf("https://") >= 0;
        URL url;

        SSLContext ctx = null;
        SSLSocketFactory sslFactory = null;
        if (m_ssl) {
            try {
                ctx = SSLContext.getInstance("TLS");
            } catch (NoSuchAlgorithmException e) {
                m_app.jotyMessage(e);
            }
            try {
                ctx.init(null, new TrustManager[]{createJotyTrustManager()}, null);
            } catch (Exception e) {
                m_app.jotyMessage(e);
            }
            sslFactory = ctx.getSocketFactory();
        }
        m_conn = null;
        String retVal = null;
        try {
            url = new URL(m_url);
            m_conn = m_ssl ? (HttpsURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection();
            if (m_ssl) {
                final HttpsURLConnection sslConn = (HttpsURLConnection) m_conn;
                sslConn.setSSLSocketFactory(sslFactory);
                sslConn.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession sslSession) {
                        if (hostname.equals(m_app.getWebClient().m_myHost))
                            return true;
                        return HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, sslSession);
                    }
                });
            }

            m_conn.setRequestMethod(m_post ? "POST" : "GET");
            m_conn.setDoInput(true);


            if (m_post) {
                m_conn.setUseCaches(false);
                m_conn.setDoOutput(true);
            } else {
                m_conn.setReadTimeout(waitingGet ? 100000 : 0  /* milliseconds */);
                m_conn.setConnectTimeout(waitingGet ? 150000 : 0 /* milliseconds */);
            }
            retVal = connect();
        } catch (IOException e1) {
            manageException(e1);
        }
        return retVal;
    }

    protected abstract TrustManager createJotyTrustManager() throws Exception;


    abstract protected String connect();

    abstract protected void manageException(IOException e);

    abstract protected void setManager(Object manager);
}
