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
import android.widget.CheckBox;

import org.joty.common.JotyTypes;

/**
 * Created by Stefano on 9/2/2015.
 */
public class CheckTerm extends Term  {

    public CheckBox m_chkBox;
    boolean m_stateChecked;

    public CheckTerm(JotyActivity activity, int resId, String dbFieldName) {
        super(activity, JotyTypes._int, resId, dbFieldName);
    }

    @Override
    public void storeView() {
        m_chkBox = (CheckBox) findViewById();
    }

    @Override
    public void setListener() {
        m_chkBox.setOnClickListener(m_onClickListener);
    }

    @Override
    public View getView() {
        return m_chkBox;
    }

    @Override
    protected void onClick(View view) {
        m_iVal = getValueFromStatus(view);
        m_dirty = true;
        super.onClick(view);
    }

    @Override
    public String sqlValueExpr() {
        return String.valueOf(getValueFromStatus(null));
    }

    private int getValueFromStatus(View view) {
        return (view == null ? m_chkBox : ((CheckBox) view)).isChecked() ? 1 : 0;
    }

    @Override
    public void guiDataExch(boolean in) {
        super.guiDataExch(in);
        if (in)
            setInteger(getValueFromStatus(null));
        else
            m_chkBox.setChecked(! isNull() && (int) getInteger() == 1);
     }

    @Override
    public void enable(boolean truth) {
        m_chkBox.setEnabled(truth);
    }


    @Override
    public boolean validate() {
        return true;
    }


    @Override
    public void clear() {
        super.clear();
        m_chkBox.setSelected(false);
    }

}
