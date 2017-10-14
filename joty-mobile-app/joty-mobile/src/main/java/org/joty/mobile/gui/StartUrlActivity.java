/*
	Copyright (c) 2016, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

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

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.joty.mobile.R;
import org.joty.mobile.app.JotyApp;

/**
 * This Activity acquires the credentials to be submitted to the server for the user authentication.
 * If the running asset of the application is "shared" (as it happens in demonstration environment
 * on the web as opposed to the production environments) a third field is offered to the user to be
 * filled: the sharing identifier.
 * <p>
 * The Activity is launched by the {@code loginAccessController} method and on the "Ok" action invokes
 * the {@code idleAccessController} method that in turn, if all is correct, invokes {@code acquireConnection}
 * else return "the control" to the {@code loginAccessController} method.
 *
 */

public class StartUrlActivity extends JotyActivity {
    private EditText startUrl;
    private Button btnOk, btnCancel;
    JotyApp m_app = JotyApp.m_app;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_url);
        startUrl = (EditText) findViewById(R.id.editStartUrl);
        startUrl.setHint("Server Url");
        startUrl.setText(m_app.m_defaultStartPath);
        btnCancel = (Button) findViewById(R.id.cancelBtn);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_app.exit();
            }
        });
        btnCancel.setText("Cancel");
        btnOk = (Button) findViewById(R.id.okBtn);
        btnOk.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 m_app.m_startPath = startUrl.getText().toString();
                 m_app.m_applicationPreferences.put("startPath", m_app.m_startPath);
                 m_common.accessLocalData(m_app.m_applicationPreferencesFile, m_app.m_applicationPreferences);
                 finish();
                 m_app.getUrlFromHomePage();
             }
        });
        btnOk.setText("Ok");
    }

 }
