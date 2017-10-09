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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.joty.app.Common;
import org.joty.app.JotyApplication;
import org.joty.common.ApplMessenger;
import org.joty.web.AbstractJotyTrustManager;
import org.joty.workstation.app.Application;


/**
 * Concrete implementation of the Joty Trust manager for Joty 2.0 workstation.
 * 
 * @see AbstractJotyTrustManager
 * 
 */
public class JotyTrustManager extends AbstractJotyTrustManager implements X509TrustManager {

	Application m_application;
	public JotyTrustManager(JotyApplication app) throws Exception {
		super(app);
		m_application = (Application) app;
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		boolean ksSave = false;
		boolean unhandledException = false;
		Common common = (Common) ((ApplMessenger) m_app).getCommon();
		try {
			m_JSSEX509TrustManager.checkServerTrusted(chain, authType);
			if (common.m_certTesting != null && common.m_certTesting.compareToIgnoreCase("true") == 0 && !m_application.m_alreadyCertDeletionOffered) {
				m_application.m_alreadyCertDeletionOffered = true;
				for (Certificate cert : chain) {
					if (cert instanceof X509Certificate) {
						if (Application.yesNoQuestion(Application.m_common.jotyLang("WantDelCertFromTrusted") + "\n\n" + certProperties((X509Certificate) cert))) {
							String alias = common.m_ks.getCertificateAlias(cert);
							common.m_ks.deleteEntry(alias);
							ksSave = true;
							m_JSSEX509TrustManager.checkServerTrusted(chain, authType);
						}
					}
				}
			}
		} catch (CertificateException excep) {
			for (Certificate cert : chain) {
				if (cert instanceof X509Certificate) {
					boolean trustIt = Application.yesNoQuestion(m_application.m_common.jotyLang("SelfCertAllowance") + "\n\n" + certProperties((X509Certificate) cert));
					if (trustIt) {
						KeyStore.Entry newEntry = new KeyStore.TrustedCertificateEntry(cert);
						try {
							common.m_ks.setEntry(common.m_certAlias, newEntry, null);
						} catch (KeyStoreException e) {
							unhandledException = true;
						}
						if (!unhandledException)
							ksSave = Application.langYesNoQuestion("AddCertToTrustedList");
					} else
						throw excep;
				}
			}
		} catch (KeyStoreException e) {
			unhandledException = true;
		}
		if (ksSave)
			unhandledException = !common.storeKeyStore();
		if (unhandledException)
			throw new CertificateException("Problems with server certificate !");
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return m_JSSEX509TrustManager.getAcceptedIssuers();
	}

}
