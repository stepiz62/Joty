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

package org.joty.mobile.authorization;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.joty.mobile.app.JotyApp;
import org.joty.mobile.R;
import org.joty.mobile.gui.JotyActivity;

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

public class LoginActivity extends JotyActivity {
    private EditText uName, pass, sk;
    private Button btnOk, btnCancel;
    boolean okBtnEnabled = true;
    JotyApp m_app = JotyApp.m_app;

    public void enableOkBtn() {
        okBtnEnabled = true;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        uName = (EditText) findViewById(R.id.editUName);
        uName.setHint(jotyLang("UserName"));
        pass = (EditText) findViewById(R.id.editPass);
        pass.setHint(jotyLang("Password"));
        sk = (EditText) findViewById(R.id.editSK);
        sk.setHint(jotyLang("SharingKeyLBL"));
        if (m_common.m_shared)
            sk.setText(m_common.m_sharingKey);
        else
            sk.setVisibility(View.INVISIBLE);
        uName.setText(m_common.m_userName);
        btnCancel = (Button) findViewById(R.id.cancelBtn);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_app.m_loginAbandoned = true;
                m_app.idleAccessController();
            }
        });
        btnCancel.setText(jotyLang("LBL_CNC"));
        btnOk = (Button) findViewById(R.id.okBtn);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (okBtnEnabled)
                    okBtnEnabled = false;
                else
                    return;
                m_common.m_userName = uName.getText().toString();
                m_common.m_password = pass.getText().toString();
                if (m_common.m_shared)
                    m_common.m_sharingKey = sk.getText().toString();

//  assignments for testing purposes
/*
                 m_common.m_sharingKey = .....;
                 m_common.m_userName =  .....;
                 m_common.m_password =  .....;
*/
                m_app.idleAccessController();
            }
        });
        btnOk.setText(jotyLang("LBL_OK"));
    }

}
