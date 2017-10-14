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

import android.content.DialogInterface;

import org.joty.app.JotyApplication;
import org.joty.app.Common;
import org.joty.web.AbstractJotyTrustManager;
import org.joty.common.ApplMessenger;
import org.joty.mobile.app.JotyApp;


import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;


/**
 * Concrete Joty 2.0 Mobile implementation of {@code org.joty.web.AbstractJotyTrustManager}.
 * <p/>
 * Together with WebConn.Connector, possibly handles the user interaction for admittance and storage of the server certificate:
 * through the JotyApp.m_currentRespHandlersManager.m_exceptionalResponse 'flag', both sides of this management stay
 * in accord with the timing constraint imposed by the O.S. and do not interfere with the approach used in org.joty.mobile about the building
 * of the execution flow derived by the use of the JotyApp.ResponseHandlersManager class.
 *
 * @see AbstractJotyTrustManager
 */

public class JotyTrustManager extends AbstractJotyTrustManager implements X509TrustManager {

    JotyApp m_jotyApp;
    JotyTrustManager(JotyApplication app) throws Exception {
        super(app);
        m_jotyApp = (JotyApp) app;
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        m_unhandledException = false;
        m_authType = authType;
        m_chain = chain;
        try {
            m_JSSEX509TrustManager.checkServerTrusted(chain, m_authType);
            if (getCommon().m_certTesting != null && getCommon().m_certTesting.compareToIgnoreCase("true") == 0 && !m_jotyApp.m_alreadyCertDeletionOffered) {
                m_jotyApp.m_alreadyCertDeletionOffered = true;
                for (Certificate cert : m_chain)
                    if (cert instanceof X509Certificate) {
                        m_cert = cert;
                        m_jotyApp.yesNoQuestion(JotyApp.m_app.jotyLang("WantDelCertFromTrusted") + "\n\n" + certProperties((X509Certificate) m_cert),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                                String alias = getCommon().m_ks.getCertificateAlias(m_cert);
                                                getCommon().m_ks.deleteEntry(alias);
                                                m_JSSEX509TrustManager.checkServerTrusted(m_chain, m_authType);
                                            }
                                        } catch (CertificateException excep) {
                                            manageCertificateException();
                                        } catch (KeyStoreException e) {
                                            m_unhandledException = true;
                                        }
                                        if (m_unhandledException)
                                            m_jotyApp.warningMsg("Problems with server certificate !");
                                    }
                                });
                    }
            }
        } catch (CertificateException excep) {
            m_jotyApp.m_currentRespHandlersManager.m_exceptionalResponse = true;
            manageCertificateException();
        }
    }

    void manageCertificateException() {
        m_jotyApp.m_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Certificate cert : m_chain) {
                    if (cert instanceof X509Certificate) {
                        m_cert = cert;
                        m_jotyApp.yesNoQuestion(m_jotyApp.jotyLang("SelfCertAllowance") + "\n\n" + certProperties((X509Certificate) m_cert),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            KeyStore.Entry newEntry = new KeyStore.TrustedCertificateEntry(m_cert);
                                            try {
                                                getCommon().m_ks.setEntry(getCommon().m_certAlias, newEntry, null);
                                            } catch (KeyStoreException e) {
                                                m_unhandledException = true;
                                            }
                                            if (!m_unhandledException) {
                                                m_jotyApp.langYesNoQuestion("AddCertToTrustedList",
                                                        new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                if (which == DialogInterface.BUTTON_POSITIVE)
                                                                    m_unhandledException = !getCommon().storeKeyStore();
                                                                certificateExceptionManaged();
                                                            }
                                                        }

                                                );
                                            }
                                        } else {
                                            m_unhandledException = true;
                                            m_jotyApp.m_common.m_commitExit = true;
                                            m_jotyApp.langWarningMsg("AppExiting");
                                        }
                                    }
                                }

                        );
                    }
                }
                }
        });
    }

    void certificateExceptionManaged() {
        JotyApp.ResponseHandlersManager respManager =  m_jotyApp.m_currentRespHandlersManager;
        WebConn webConn = respManager.m_webConn;
        webConn.doConnection(respManager, webConn.m_post, true);
    }

    protected Common getCommon() {
        return (Common) ((ApplMessenger) m_app).getCommon();
    }
}
