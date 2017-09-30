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
import android.widget.RadioGroup;

import org.joty.app.LiteralsCollection;
import org.joty.data.WrappedField;
import org.joty.gui.InitialValue;
import org.joty.mobile.app.JotyApp;
import org.joty.mobile.data.WResultSet;

/**
 * Created by Stefano on 9/2/2015.
 */
public abstract class Term extends WrappedField {

    JotyActivity m_activity;
    public int m_resId;
    JotyApp m_app = JotyApp.m_app;
    /** Hosts the default value if defined */
    private InitialValue m_defaultValue, m_contextValue;
    protected boolean m_dirty;

    protected View.OnClickListener m_onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Term.this.onClick(v);
            m_activity.onWidgetClick(v);
        }
    };
    public RadioGroup m_operatorsRadioGroup;
    public boolean m_required;
    public boolean m_asRenderer;
    LiteralsCollection m_literalStruct;

    public Term(JotyActivity activity, int dataType, int resId, String dbFieldName) {
        this(activity, dataType, resId, dbFieldName, null, false);
    }

    public Term(JotyActivity activity, int dataType, int resId, String dbFieldName, String literalStructName, boolean asRenderer) {
        super(JotyApp.m_app);
        if (literalStructName != null) {
            m_asRenderer = true;
            m_literalStruct = m_app.m_common.m_literalStructMap.get(literalStructName);
        } else
            m_asRenderer = asRenderer;
        m_activity = activity;
        m_dataType = dataType;
        m_resId = resId;
        m_dbFieldName = dbFieldName;
        storeView();
        setListener();
        m_activity.m_terms.add(this);
        m_activity.m_termMap.put(resId, this);
    }

    public abstract void storeView();

    public abstract void setListener();

    public abstract View getView();

    public View findViewById() {
         return  m_activity.findViewById(m_resId);
    }

    protected void onClick(View view) {
    }

    public void resetDirtyStatus() {
        m_dirty = false;
    }

    public abstract void enable(boolean truth);

    public void termRender() {
    }

    public void guiDataExch(boolean in) {
    }

    public String sqlValueExpr() {
        return render(false, true);
    }

    public void setOperatorsRadioGroup(int operatorsRadioGroupResId) {
        m_operatorsRadioGroup = (RadioGroup) m_activity.findViewById(operatorsRadioGroupResId);
    }

    public long selectionData() {
        return getCurSelData();
    }

    long getCurSelData() {
        return -1;
    }

    boolean validate() {
        return true;
    }

    public InitialValue defaultValue() {
        if (m_defaultValue == null)
            m_defaultValue = new InitialValue(m_app, this);
        return m_defaultValue;
    }

    public InitialValue contextValue() {
        if (m_contextValue == null)
            m_contextValue = new InitialValue(m_app, this);
        return m_contextValue;
    }

    public void storeState(WResultSet rs) {
        setWField(rs);
    }

    public  boolean isDirty() { return m_dirty;};

    public void setDirty() {
        m_dirty = true;
    }

}
