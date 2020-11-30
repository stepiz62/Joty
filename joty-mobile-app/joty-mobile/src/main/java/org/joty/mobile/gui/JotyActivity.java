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

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ProgressBar;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.joty.app.Common;
import org.joty.common.ParamContext;
import org.joty.mobile.R;
import org.joty.mobile.app.JotyApp;

import java.util.HashMap;
import java.util.Vector;

/**
 * Base class for any implementation of the {@code android.app.Activity} class made in Joty 2.0 Mobile.
 * It inherits from the android.support.v7.app.AppCompatActivity to benefits of the action bar features
 * present in that class.
 * <p>
 * It hosts the references to the {@code Term} objects that are instantiated in the activity, either
 * they are targeted to wrap Views that compose the criteria area in searching data or simply a data editing area.
 * <p>
 * The class collaborates to maintain the {@code m_respManagerCatalog} member of the {@code JotyApp}
 * instance, essential to enable this class to identify the
 * ResponseHandlersManager to work with, by means of the {@code getRespManager} method.
 * <p>
 * Handles the chance to manage the wait cursor if present in the layout file.
 * <p>
 * Manages the storing and the restoring of the visual state upon the stop and start events of the Activity
 * instance for all the Joty objects added to it.
 * <p>
 * In its {@code onResume} activity event handler it manages the chance of ending, of going back to the
 * home view or, simply, to stop the wait cursor or even to interrupt the viewing of a {@code JotyToast} object.
 * <p>
 * The class has also methods that assist the state of the {@code Term} objects hosted.
 * <p>
 * It has a built class ({@code AccessorCoordinates}) that helps in addressing the Accessor coordinates when the
 * the Joty activity works in (remote) accessor mode.
 *
 *
 * @see JotyApp#m_respManagerCatalog
 * @see JotyApp.JotyToast
 * @see ActivityController
 */

public class JotyActivity extends AppCompatActivity {

    public void addExtrasInOpenActivity(Bundle extras) {
    }

    public class AccessorCoordinates {
        public AccessorCoordinates() {
            this(null);
        }

        public AccessorCoordinates(ParamContext srcContext) {
            if (srcContext != null) {
                paramContext = new ParamContext(m_app);
                paramContext.copy(srcContext);
            }
        }

        public String jotyDialogClassName;
        public int jotyPanelIndex;
        public String jotyTermName;
        public String mode;
        public ParamContext paramContext;
    }

    public class WaitCursor {
        protected ProgressBar m_progressBar;


        public void setProgressBar() {
            m_progressBar = (ProgressBar) findViewById(R.id.progressBar);
        }

        public void wait(boolean truth) {
            if (m_progressBar != null)
                m_progressBar.setVisibility(truth ? ProgressBar.VISIBLE : ProgressBar.GONE);
        }

    }

    protected JotyApp m_app;
    public Vector<Term> m_terms;
    public HashMap<Integer, Term> m_termMap;
    public WaitCursor m_waitCursor;
    public Bundle m_extras;
    protected Common m_common;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_app = JotyApp.m_app;
        m_app.m_activity = this;
        m_common = m_app.m_common;
        if (m_app.m_firstActivityActivation) {
            if (!m_app.m_nextActivityActivation)
                m_app.m_nextActivityActivation = true;
        } else
            m_app.m_firstActivityActivation = true;
        m_terms = new Vector<Term>();
        m_termMap = new HashMap<Integer, Term>();
        m_waitCursor = new WaitCursor();
        m_extras = getIntent().getExtras();
    }

    public String jotyLang(String literal) {
        return m_common.jotyLang(literal);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        m_waitCursor.setProgressBar();
        if (m_app.m_inited)
            m_waitCursor.wait(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        for (Term term : m_terms)
            if (term instanceof TextTerm) {
                TextTerm textTerm = (TextTerm) term;
                if (textTerm.isDateType())
                    outState.putLong("term_" + String.valueOf(textTerm.m_resId),
                            textTerm.isNull() ? 0 :
                                    (textTerm.m_asRenderer ? textTerm.m_lVal : textTerm.m_dateVal.getTime()));
            }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (savedInstanceState != null)
            for (Term term : m_terms)
                if (term instanceof TextTerm) {
                    TextTerm textTerm = (TextTerm) term;
                    if (textTerm.isDateType() || textTerm.m_asRenderer) {
                        Long savedVal = savedInstanceState.getLong("term_" + String.valueOf(textTerm.m_resId));
                        textTerm.setToNull(savedVal == 0);
                        if (savedVal != 0)
                            if (textTerm.m_asRenderer)
                                textTerm.m_lVal = savedVal;
                            else
                                textTerm.m_dateVal.setTime(savedVal);
                    }

                }
        postInit();
        guiDataExchange(false);
    }

    protected void postInit() {}

    @Override
    public void onBackPressed() {
        if (m_app.m_nextActivityActivation)
            m_app.langWarningMsg("PleaseUseNavig");
        else
            super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_app.m_activity = this;
        if (m_common.m_commitExit)
            finish();
        checkHome();
        if (m_app.m_currToast != null) {
            if (m_app.m_currToast.isInterruptable())
                m_app.m_currToast.cancel();
        }
        if (!(this instanceof IdleActivity) || m_app.m_nextActivityActivation)
            setWaitCursor(false);
    }

    protected void home() {
        m_app.m_home = true;
        checkHome();
    }

    private void checkHome() {
        if (m_app.m_home)
            m_app.endCurrentNonIdleActivity();
    }

    public void onWidgetClick(View view) {
    }

    public void onWidgetItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    public void resetDirtyStatus() {
        for (Term term : m_terms)
            term.resetDirtyStatus();
    }

    boolean isDirty() {
        for (Term term : m_terms)
            if (term.isDirty())
                return true;
        return false;
    }

    protected void guiDataExchange() {
        guiDataExchange(true);
    }


    protected void guiDataExchange(boolean save) {
        for (Term term : m_terms)
            term.guiDataExch(save);
    }


    public Term term(int resId) {
        return m_termMap.get(resId);
    }

    public void setWaitCursor(boolean truth) {
        m_waitCursor.wait(truth);
    }

    protected void lockRotation(boolean truth) {
        int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if (truth) {
            switch (((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation()) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
            }
        }
        setRequestedOrientation(orientation);
    }

    protected boolean isPortrait() {
        int rotation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180;
    }

    @Override
    public void finish() {
        super.finish();
        Integer count = m_app.m_activitiesInstancesCounters.get(getClass().getName());
        if (count != null) {
            if (count == 1)
                m_app.m_activitiesInstancesCounters.remove(getClass().getName());
            else
                m_app.m_activitiesInstancesCounters.put(getClass().getName(), 1);
        }
        m_app.m_respManagerCatalog.remove(getRespManagerKey());
    }

    public void startActivity(Class activityClass) {
        startActivity(activityClass, false);
    }

    public void startActivity(Class activityClass, boolean forRenewing) {
        startActivity(activityClass, forRenewing, null);
    }

    /**
     * It performs the starting of another activity, blocking the command if an instance of the class is
     * already started (a part from the case in which one activity is going to renew itself: then, one
     * pre-existing instance is admitted, and the framework assures the subsequent finishing of the old one -
     * see {@code ActivityController.createRespHandler}
     *
     * @param activityClass Class object that defines the class of the activity to start
     * @param forRenewing true if "this" activity is of the same type the starting activity (it happens when an Activity is going to finish to renew itself).
     * @param respManagerCount when not null, an extra value is put in the Extras of the Intent so that the launched
     *                         JotyActivity can address the  QueryResponseHandlersManager object by
     *                         means of the {@code JotyActivity.getRespManager} method.
     * @see #finish
     * @see JotyApp#m_activitiesInstancesCounters
     * @see ActivityController#createRespHandler
     */
    public void startActivity(Class activityClass, boolean forRenewing, final Integer respManagerCount) {
        Integer count = m_app.m_activitiesInstancesCounters.get(activityClass.getName());
        if (count == null || count == 1 && forRenewing) {
            if (count == null)
                count = 1;
            else
                count++;
            m_app.m_activitiesInstancesCounters.put(activityClass.getName(), count);
            Intent myIntent = new Intent(this, activityClass);
            if (respManagerCount != null)
                myIntent.putExtra("respManagerCount", String.valueOf(respManagerCount));
            startActivity(myIntent);
        }
    }

    protected JotyApp.QueryResponseHandlersManager getRespManager() {
        return (JotyApp.QueryResponseHandlersManager) m_app.m_respManagerCatalog.get(getRespManagerKey());
    }

    /**
	 * composes the key to inquiry the {@code JotyApp.m_respManagerCatalog} map.
	 * If the "respManagerCount" extra value is available, its value is appended
	 * to the class name.
	 *
	 * @return the key as String
	 */
    protected String getRespManagerKey() {
        String key = getClass().getName();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String count = extras.getString("respManagerCount");
            if (count != null)
                key += count;
        }
        return key;
    }
}
