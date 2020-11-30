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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import org.joty.common.JotyTypes;
import org.joty.data.JotyDate;
import org.joty.gui.NumberFormatter;
import org.joty.mobile.R;

/**
 * Created by Stefano on 9/2/2015.
 */
public class TextTerm extends Term {


    private int m_inputType;
    private int m_textColor = 0, m_disabledTextColor = Color.GRAY;
//    private boolean proceduralChange;

    public void showDatePicker() {
        JotyDatePicker datePicker = new JotyDatePicker();
        datePicker.setTargetTerm(this);
        FragmentManager fragmentManager = m_activity.getSupportFragmentManager();
        datePicker.show(fragmentManager, "datePicker");
    }

    public EditText m_editText;
    public TextView m_textView;
    private boolean m_textViewEnabled;
    public boolean m_nullable;
    NumberFormatter m_numberFormat;
    TextView m_auxView;
    protected boolean m_clickable;


    public void setAuxView(int resId) {
        m_auxView = (TextView) m_activity.findViewById(resId);
        m_auxView.setOnClickListener(m_onClickListener);
        m_auxView.setBackgroundResource(R.drawable.textviewstates);
    }

    public TextTerm(JotyActivity activity, int dataType, int resId, String dbFieldName) {
        this(activity, dataType, resId, dbFieldName, null, false);
    }

    public TextTerm(JotyActivity activity, int dataType, int resId, String dbFieldName, boolean asRenderer) {
        this(activity, dataType, resId, dbFieldName, null, asRenderer);
    }

    public TextTerm(JotyActivity activity, int resId, String dbFieldName, String literalStructName) {
        this(activity, JotyTypes._long, resId, dbFieldName, literalStructName, false);
    }

    private TextTerm(JotyActivity activity, int dataType, int resId, String dbFieldName, String literalStructName, boolean asRenderer) {
        super(activity, dataType, resId, dbFieldName, literalStructName, asRenderer);
        if (dataType != JotyTypes._text && !isDateType())
            m_numberFormat = new NumberFormatter(m_app, this);
        m_nullable = true;
        if (m_editText != null) {
            m_textColor = m_editText.getCurrentTextColor();
            m_inputType = m_editText.getInputType();
            m_editText.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (m_editText.getInputType() != InputType.TYPE_NULL)
                        ((InputMethodManager) m_activity.getSystemService(Activity.INPUT_METHOD_SERVICE)).showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                    return false;
                }
            });
            m_editText.setTextIsSelectable(true);
        }
    }

    public boolean isDateType() {
        return m_dataType == JotyTypes._date || m_dataType == JotyTypes._dateTime;
    }

    @Override
    public void storeView() {
        if (isDateType() || m_asRenderer) {
            m_textView = (TextView) findViewById();
            m_textView.setBackgroundResource(R.drawable.textviewstates);
        } else
            m_editText = (EditText) findViewById();
    }

    @Override
    public void setListener() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
 //               if (!proceduralChange)
                    m_dirty = true;
                if (isDateType() && m_auxView != null)
                    m_auxView.setText(isNull() ? "" : ((JotyDate) m_dateVal).dow());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        if (isDateType()) {
            m_textView.addTextChangedListener(textWatcher);
            if (!m_asRenderer) {
                m_textView.setOnClickListener(m_onClickListener);
                m_textView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (textViewIsEnabled() && m_nullable && !isNull()) {
                            m_app.yesNoQuestion(String.format(m_app.jotyLang("WantToDelete"), m_app.jotyLang("Date")), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE) {
                                        setToNull(true);
                                        renderText();
                                    }
                                }
                            });
                            return true;
                        }
                        return false;
                    }
                });
            }
        } else if (!m_asRenderer) {
            m_editText.addTextChangedListener(textWatcher);
            m_editText.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (m_inputType == InputType.TYPE_CLASS_PHONE && m_editText.getText().length() > 0) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + m_editText.getText().toString()) );
                        m_activity.startActivity(intent);
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private boolean textViewIsEnabled() {
        return m_textViewEnabled || m_activity instanceof DataMainActivity;
    }

    @Override
    public View getView() {
        return isDateType() || m_asRenderer ? m_textView : m_editText;
    }

    @Override
    public void guiDataExch(boolean in) {
        super.guiDataExch(in);
        if (in) {
            if (!isDateType() && !m_asRenderer)
                setData(m_editText.getText().toString(), m_numberFormat);
        } else
            termRender();
    }

    @Override
    protected boolean validate() {
        boolean success = true;
        if (m_required) {
            if (isNull()) {
                m_app.toast((isDateType() || m_asRenderer ? m_textView.getHint() : m_editText.getHint()) + ": " + m_app.jotyLang("MustNotBeEmpty"));
                success = false;
            }
        }
        return success;
    }

    @Override
    public void enable(final boolean truth) {
        if (isDateType() || m_asRenderer) {
            m_textViewEnabled = truth && (!m_asRenderer || m_clickable);
            m_textView.setEnabled(m_textViewEnabled);
            if (m_auxView != null)
                m_auxView.setEnabled(m_textViewEnabled);
        } else {
            //           m_editText.setEnabled(truth);
            m_editText.setTextColor(truth ? m_textColor : m_disabledTextColor);
            m_editText.setInputType(truth ? m_inputType : InputType.TYPE_NULL);

//           m_editText.setTextIsSelectable(true);

//            m_editText.setFocusable(truth);
//            m_editText.setFocusableInTouchMode(true);
//            if (truth)
//                m_editText.setLongClickable(false);
//            m_editText.setLongClickable(true);

            m_editText.setCustomSelectionActionModeCallback(truth ? null : new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    if (!truth) {
                        menu.removeItem(android.R.id.cut);
                        menu.removeItem(android.R.id.paste);
                        menu.removeItem(android.R.id.selectAll);
                    }
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return true;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                }
            });
        }
    }

    @Override
    public void clear() {
     //   proceduralChange = true;
        super.clear();
        if (isDateType() || m_asRenderer)
            m_textView.setText(null);
        else
            m_editText.setText(null);
    //    proceduralChange = false;
    }

    @Override
    public void termRender() {
        super.termRender();
        renderText();
    }

    public void renderText() {
         if (isNull()) {
            String renderText = null;
            if (isDateType() && m_textView.getHint() == null)
                renderText = m_dataType == JotyTypes._date ?
                        m_app.m_common.emptyDateRendering(false) : m_app.m_common.emptyDateRendering(true);
            if (isDateType() || m_asRenderer)
                m_textView.setText(renderText);
            else
                m_editText.setText(renderText);
        } else
            switch (m_dataType) {
                case JotyTypes._double:
                case JotyTypes._single:
                case JotyTypes._long:
                case JotyTypes._int:
                case JotyTypes._dbDrivenInteger:
//                    uninstallFormatter();
                    if (m_asRenderer)
                        m_textView.setText(m_literalStruct == null ? m_strVal : m_literalStruct.literal(m_lVal));
                    else
                        m_editText.setText(m_numberFormat.render());
                    break;
                case JotyTypes._text:
                    m_editText.setText(m_strVal);
//                    setSelectionStart(0);
//                    setSelectionEnd(0);
                    break;
                case JotyTypes._date:
                case JotyTypes._dateTime:
                    m_textView.setText(m_dateVal.toString(m_dataType == JotyTypes._dateTime));
                    break;
            }
    //    proceduralChange = false;
    }

    @Override
    protected void onClick(View view) {
        super.onClick(view);
        if (isDateType() && textViewIsEnabled())
            showDatePicker();
    }

    public void setToNow() {
        m_dateVal.setDate(new JotyDate(m_app, "now"));
        setToNull(false);
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        m_clickable = true;
        m_textView.setOnClickListener(onClickListener);
    }


}
