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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.joty.app.LiteralsCollection;
import org.joty.common.JotyTypes;
import org.joty.mobile.R;
import org.joty.mobile.app.JotyApp;
import org.joty.mobile.data.WResultSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Stefano on 9/2/2015.
 */
public class SpinnerTerm extends Term {
    public Spinner m_spinner;
    LiteralsCollection m_literalStruct;

    public SpinnerTerm(JotyActivity activity, int resId, String dbFieldName) {
        this(activity, resId, dbFieldName, null);
    }

    public SpinnerTerm(JotyActivity activity, int resId, String dbFieldName, String literalStructName) {
        super(activity, JotyTypes._long, resId, dbFieldName);
        if (literalStructName != null) {
            m_literalStruct = m_app.m_common.m_literalStructMap.get(literalStructName);
            if (m_literalStruct == null) {
                m_app.m_common.m_commitExit = true;
                m_app.warningMsg("Unexisting literal structure with the name : '" + literalStructName + "' !");
            }
            else {
                List<String> spinnerArray = new ArrayList<String>();
                for (LiteralsCollection.DescrStruct descrStruct : m_literalStruct.m_descrArray)
                    spinnerArray.add(descrStruct.descr);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        m_activity, R.layout.spinner_item, spinnerArray);
                m_spinner.setAdapter(adapter);
            }
        }
    }

    @Override
    public void storeView() {
        m_spinner = (Spinner) findViewById();
    }

    @Override
    public void setListener() {
          m_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                long actualId = m_literalStruct.m_descrArray.get(position).id;
                SpinnerTerm.this.onItemSelected(parent, view, position, actualId);
                m_activity.onWidgetItemSelected(parent, view, position, actualId);
                m_dirty = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public View getView() {
        return m_spinner;
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int pippo = 1;
    }

    @Override
    public void guiDataExch(boolean in) {
        super.guiDataExch(in);
        if (in) {
            if (m_dataType == JotyTypes._text)
                m_strVal = m_spinner.getSelectedItem().toString();
            else {
                setInteger(selectionData());
                if (m_spinner.getSelectedItemPosition() == -1)
                    setToNull(true);
            }
        } else {
            if (isNull())
                clear();
            else if (m_dataType == JotyTypes._text)
                setSelectionBasedOnData(null, strVal());
            else
                setSelectionBasedOnData(getInteger(), null);
       }
    }

    @Override
    public long getCurSelData() {
        if (m_literalStruct == null)
            return -1;
        else
            return m_spinner.getCount() > 0 ? m_literalStruct.m_descrArray.get(m_spinner.getSelectedItemPosition()).id : -1;
    }


    @Override
    protected boolean validate() {
        boolean success = true;
        switch (m_dataType) {
            case JotyTypes._text:
                if (m_required)
                    success = m_strVal != null && !m_strVal.isEmpty();
                break;
            case JotyTypes._long:
            case JotyTypes._int:
            case JotyTypes._dbDrivenInteger:
                if (m_required)
                    success = m_spinner.getSelectedItemPosition() != -1;
                break;
        }
        if (!success)
            m_app.toast( m_spinner.getPrompt() + ": " +  m_app.jotyLang("MustNotBeEmpty"));
        return success;
    }

    @Override
    public void storeState(WResultSet rs) {
        String temp;
        if (m_dataType == JotyTypes._dbDrivenInteger || m_literalStruct != null) {
            if (isNull())
                rs.setMemberToNull(m_dbFieldName);
            else
                rs.setIntegerValue(m_dbFieldName, selectionData());

        } else
            super.storeState(rs);
    }


    @Override
    public void enable(boolean truth) {
        m_spinner.setEnabled(truth);
    }

    @Override
    public void clear() {
        super.clear();
        m_spinner.setSelection(-1);
    }


    public int setSelectionBasedOnData(Long lonVal, String strVal) {
        int retVal = -1;
        Integer posIdx = lonVal == null ? m_literalStruct.m_strKeyRevMap.get(strVal) : m_literalStruct.m_descrReverseMap.get(lonVal);
        if (posIdx == null)
            m_spinner.setSelection(-1);
        else {
            m_spinner.setSelection(posIdx);
            retVal = posIdx;
        }
        if ( lonVal == null)
            setVal(strVal);
        else
            setInteger(lonVal);
        return retVal;
    }

    @Override
    public String render() {
        return m_literalStruct == null ? super.render() : m_spinner.getSelectedItem().toString();
    }


}
