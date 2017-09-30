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

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.joty.app.Common;
import org.joty.app.JotyApplication;
import org.joty.common.ApplMessenger;



/**
 * Prepares the implementation of the trust manager class specific to the
 * framework target context and leaves the {@code checkServerTrusted} method
 * unimplemented. Its implementation manages the user acceptance and the
 * acquisition by the client of the server x509-type certificate. A
 * configuration item ('certTesting') allows the developer/tester to erase the
 * certificate from the client store.
 */

public abstract class AbstractJotyTrustManager implements X509TrustManager {
    protected X509TrustManager m_JSSEX509TrustManager;
    protected JotyApplication m_app;
    protected Certificate m_cert;
    protected X509Certificate[] m_chain;
    protected boolean m_unhandledException = false;
    protected String m_authType;

    public AbstractJotyTrustManager(JotyApplication app) throws Exception {
        m_app = app;
        TrustManagerFactory tmFactory = null;
        tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmFactory.init(((Common) ((ApplMessenger) m_app).getCommon()).m_ks);
        TrustManager tms[] = tmFactory.getTrustManagers();
        for (TrustManager tm : tms) {
            if (tm instanceof X509TrustManager) {
                m_JSSEX509TrustManager = (X509TrustManager) tm;
                return;
            }
        }
        throw new Exception("Failure on creating JotyTrustManager instance");
    }

    protected String certProperties(X509Certificate x509Cert) {
        String retVal = "Type: " + x509Cert.getType() + "\n" +
                "IssuerDN: " + x509Cert.getIssuerDN() + "\n" +
                "SubjectDN: " + x509Cert.getSubjectDN() + "\n";
        return retVal;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        m_JSSEX509TrustManager.checkClientTrusted(chain, authType);
    }


    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return m_JSSEX509TrustManager.getAcceptedIssuers();
    }

}
