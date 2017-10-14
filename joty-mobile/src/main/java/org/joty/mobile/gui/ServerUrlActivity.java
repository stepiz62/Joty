/*
	Copyright (c) 2015-2017, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

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

package org.joty.mobile.gui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.webkit.WebView;

import org.joty.mobile.R;
import org.joty.mobile.app.JotyApp;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


/**
 * It builds an inner class {@code ServerUrlPicker} that, descending from the {@code android.os.AsyncTask}
 * class, allows to perform on a background thread the following operations:
 * browsing the web for web page used as dispenser of the url the JotyApp have to work on, and, then,
 * by making use of the org.jsoup library, building an org.jsoup.nodes.Document object
 * and, from this, extracting the url.
 *
 */

public class ServerUrlActivity extends JotyActivity {

    String m_targetUrl;
    Throwable m_throwable;

    public class ServerUrlPicker extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... urls) {
            int count = urls.length;
            m_targetUrl = null;
            for (int i = 0; i < count; i++) {
                Document doc = null;
                String url = urls[i].toString();
                Connection connection = Jsoup.connect(url);
                try {
                    doc = connection.get();
                } catch (Throwable t) {
                    m_throwable = t;
                }
                if (doc != null) {
                    Element input = doc.select("input").first();
                    if (input != null)
                        m_targetUrl = input.attr("value");
                }
                if (m_targetUrl == null)
                    m_app.m_messageText = "The home page was not reachable !";
            }
            return null;
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (m_app.m_messageText == null) {
                m_common.m_appUrl = m_targetUrl;
                JotyApp.m_app.webInitAndGetConfig();
            } else {
                m_common.m_commitExit = true;
                m_app.jotyMessage(m_app.m_messageText + " - " + (m_throwable == null ? "" : m_throwable.toString()));
            }
         }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_url);
        m_app.m_messageText = null;
        WebView myWebView = (WebView) findViewById(R.id.serverUrlView);
        String url = m_app.m_startPath + "/start.html";
        myWebView.loadUrl(url);
        new ServerUrlPicker().execute(new String[]{url});
    }


 }
