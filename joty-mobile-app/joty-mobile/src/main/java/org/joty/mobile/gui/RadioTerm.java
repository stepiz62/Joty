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

package org.joty.mobile.gui;

import android.view.View;
import android.widget.RadioButton;

import org.joty.common.JotyTypes;

import java.util.Vector;

/**
 * Created by Stefano on 9/2/2015.
 */
public class RadioTerm extends Term {
    public RadioTerm m_masterTerm;

    RadioButton m_radioButton;
    private int m_storedValue;
    public int m_maxSiblingsValue = 0;
    public boolean m_siblingEvent = false;
    public Vector<RadioButton> m_buttonArray;

    public RadioTerm(JotyActivity activity, int resId, String dbFieldName) {
        this(activity, resId, null, dbFieldName);
    }

    public RadioTerm(JotyActivity activity, int resId, RadioTerm masterTerm, String dbFieldName) {
        super(activity, masterTerm == null ? JotyTypes._int : JotyTypes._none, resId, dbFieldName);
        m_masterTerm = masterTerm;
        if (m_masterTerm == null) {
            m_storedValue = 0;
            m_buttonArray = new Vector<RadioButton>();
            m_buttonArray.add(m_radioButton);
        }
        else {
            m_storedValue = ++m_masterTerm.m_maxSiblingsValue;
            m_masterTerm.m_buttonArray.add(m_radioButton);
        }
    }

    @Override
    public void storeView() {
        m_radioButton = (RadioButton) findViewById();
    }

    @Override
    public void setListener() {
        m_radioButton.setOnClickListener(m_onClickListener);
    }

    @Override
    public View getView() {
        return m_radioButton;
    }

    @Override
    protected void onClick(View view) {
        if (m_masterTerm == null) {
            if (! m_siblingEvent)
                 m_iVal = m_storedValue;
            setToNull(false);
            m_dirty = true;
            super.onClick(view);
            }
        else {
            m_masterTerm.m_iVal = m_storedValue;
            m_masterTerm.m_siblingEvent = true;
            m_masterTerm.onClick(null);
            m_masterTerm.m_siblingEvent = false;
         }
    }


    @Override
    public void guiDataExch(boolean in) {
        if (m_masterTerm == null) {
            super.guiDataExch(in);
            if (! in) {
                if (!isNull()) {
                    int value = (int) getInteger();
                    if (value >= 0)
                        m_buttonArray.get(value).setChecked(true);
                }
            }
        }
    }

    @Override
    public void enable(boolean truth) {
        m_radioButton.setEnabled(truth);
    }


    @Override
    protected boolean validate() {
        boolean success = true;
        if (m_masterTerm == null) {
            if (m_required && m_iVal == -1) {
                String list = "";
                for (RadioButton button : m_buttonArray)
                    list += "\n" + button.getText();
                m_app.toast(String.format(m_app.m_common.appLang("ChooseOneOpt"),list));
                success = false;
            }
        }
        return success;
    }

    @Override
    public void clear() {
        super.clear();
        m_radioButton.setSelected(false);
        if (m_masterTerm == null)
            m_iVal = -1;
    }


}
