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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.fragment.app.DialogFragment;

import org.joty.mobile.gui.TextTerm;

import java.util.Calendar;

/**
 * Implements the {@code DatePickerDialog.OnDateSetListener} interface to serve the TextView wrapped
 * by a TextTerm object. The selected value is directed to be formatted basing on the Joty requirements.
 *
 * @see Term
 * @see TextTerm
 * @see org.joty.data.JotyDate
 */

public class JotyDatePicker extends DialogFragment<>
        implements android.app.DatePickerDialog.OnDateSetListener {
    TextTerm m_term;

    public void setTargetTerm( TextTerm term) {
        m_term = term;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar cal = Calendar.getInstance();
        if (!m_term.isNull())
            cal.setTime(m_term.m_dateVal);
        return new DatePickerDialog(getActivity(), this,  cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, day);
        m_term.m_dateVal.setTime(c.getTime().getTime());
        m_term.setToNull(false);
        m_term.m_textView.setText(m_term.m_dateVal.render());
    }
}
