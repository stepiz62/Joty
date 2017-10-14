/*
	Copyright (c) 2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

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
 * As its name says ...the class acquires data for the change.
 * It relies on the outer ResponseHandlersManager object set in the {@code JotyAppp.tryChangePassword}
 * method and it drives the execution of {@code JotyAppp.setPassword(String, String, ResponseHandlersManager)}
 * that dispatches the request to the Joty Server.
 *
 */

public class ChangePasswordActivity extends JotyActivity {

    private EditText oldPwd, newPwd, repeatPwd;
    private Button btnOk, btnCancel;
    JotyApp m_app = JotyApp.m_app;
    boolean m_getOldPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        setTitle(jotyLang("PwdChange"));
        m_getOldPassword = m_extras.getBoolean("getOldPassword");
        oldPwd = (EditText) findViewById(R.id.OldPwd);
        oldPwd.setHint(jotyLang("OldPwd"));
        if (!m_getOldPassword)
            oldPwd.setVisibility(View.INVISIBLE);
        newPwd = (EditText) findViewById(R.id.NewPwd);
        newPwd.setHint(jotyLang("NewPwd"));
        repeatPwd = (EditText) findViewById(R.id.RepeatNewPwd);
        repeatPwd.setHint(jotyLang("RepeatNewPwd"));
        btnCancel = (Button) findViewById(R.id.cancelBtn);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangePasswordActivity.this.finish();
                m_app.m_respManagerCatalog.get("ChangePasswordActivity").checkToExecute(false);
            }
        });
        btnCancel.setText(jotyLang("LBL_CNC"));
        btnOk = (Button) findViewById(R.id.okBtn);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPwdStr = newPwd.getText().toString();
                String repeatPwdStr = repeatPwd.getText().toString();
                boolean valid = true;
                JotyApp.ResponseHandlersManager respManager = m_app.m_respManagerCatalog.get("ChangePasswordActivity");
                if (m_getOldPassword) {
                    if (oldPwd.getText().toString().compareTo(m_common.m_password) != 0) {
                        valid = false;
                        m_app.langWarningMsg("WrongOldPwd");
                    }
                }
                if (valid)
                    if (newPwdStr.compareTo(repeatPwdStr) == 0) {
                        respManager.setParam("newPwdStr", newPwdStr);
                    } else {
                        valid = false;
                        m_app.langWarningMsg("NewPwdNotRepeated");
                    }
                if (valid) {
                    ChangePasswordActivity.this.finish();
                    respManager.checkToExecute(m_app.m_passwordValidator == null || m_app.m_passwordValidator.validate(newPwdStr));
                }
            }
        });
        btnOk.setText(jotyLang("LBL_OK"));
    }

}
