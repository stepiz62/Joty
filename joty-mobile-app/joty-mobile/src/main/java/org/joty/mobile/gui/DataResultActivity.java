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

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import androidx.annotation.Nullable;

import org.joty.mobile.app.JotyApp;
import org.joty.mobile.R;

/**
 * It renders the result set got in response to an inquiry submitted to the Joty Server.
 * It retrieves its controller (a {@code ResultController} object) from the stack hosted by the "top"
 * {@code DataMainActivity} object, accessible by the {@code m_dataMainActivity} member.
 * <p>
 * Uses a {@code JotyResultFragment} object to make the rendering.
 * Optionally provides an auxiliary {@code JotyResultFragment} instance to drive some helper action
 * mapped to the rows of the main rendered list.
 * <p>
 * Provides the "next" and "previous" action for navigating by the data pagination provided by
 * {@code ResultController.accessResult} method.
 * <p>
 * Offers the methods either to open a {@code DataDetailsActivity} instance for the navigation to
 * focus on an existing record or to create a new record to be added in the underlying set.
 * <p>
 * On the onResume event, if data have been modified, that is one ore more record has been deleted or modified,
 * the {@code ResultController.accessResult} method is invoked again.
 * <p>
 * On creation it pushes itself into the {@code JotyApp.m_dataResultActivityStack} stack.
 *
 * @see ResultController
 * @see JotyResultFragment
 * @see MenuActivity
 *
 */

public abstract class DataResultActivity extends MenuActivity {

    public boolean m_nextVisible, m_previousVisible;
    public String m_openableDetailIdField;
    public JotyResultFragment m_listFragment;
    public NavigatorListFragment m_navigatorListFragment;
    public int m_initialSelection = -1;
    protected ResultController m_resultController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_app.m_dataResultActivityStack.push(this);
        m_mainActivity.m_resultActivity = this;
        m_resultController = m_mainActivity.m_resultControllersStack.top();
        if (savedInstanceState != null && savedInstanceState.getString("fictitious") != null)
            m_resultController.restoreIteration();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("fictitious", "***");
        super.onSaveInstanceState(outState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (m_resultController.m_newAction)
            addNewRecordMenu(menu);
        if (m_previousVisible)
            addMenuItemToAppMenu(menu, JotyApp.JotyMenus.PrevMenu, jotyLang("LBL_PREV"), R.drawable.prev, false,
                    new Action() {
                        public void doIt() {
                            previous();
                        }
                    });
        if (m_nextVisible)
            addMenuItemToAppMenu(menu, JotyApp.JotyMenus.NextMenu, jotyLang("LBL_NEXT"), R.drawable.next, false,
                    new Action() {
                        public void doIt() {
                            next();
                        }
                    });
        return super.onCreateOptionsMenu(menu);
    }

    protected void next() {
        if(m_resultController.isEnabled())
            m_resultController.accessResult(this, true, false);
    }

    protected void previous() {
        if(m_resultController.isEnabled())
            m_resultController.accessResult(this, true, true);
    }

    public void onFragmentItemClick(long id, JotyResourceCursorAdapter adapter, int position) {
        long detailRecordID = m_openableDetailIdField == null ? id : adapter.getLong(m_openableDetailIdField);
        if (m_resultController.m_detailsActivityClass == null)
            doActionOnRecord(id, adapter, position);
        else {
            DetailsController detailsController = m_resultController.m_detailsController;
            if (detailsController == null)
                detailsController = new DetailsController(m_resultController.m_detailsActivityClass);
            m_mainActivity.m_detailsControllersStack.push(detailsController);
            openDetailsPrepare(id, adapter, position);
            detailsController.m_keysFilterOnReading = m_resultController.m_type == DataMainActivity.Type.searcher;
            detailsController.openDetailsActivity(this, String.valueOf(detailRecordID), adapter);
        }
    }

    protected void openDetailsPrepare(long id, JotyResourceCursorAdapter adapter, int position) {
        mainActivity().m_detailsControllersStack.top().m_paramContext = createParamContext(m_resultController);
    }

    protected void doActionOnRecord(long id, JotyResourceCursorAdapter adapter, int position) {
    }

    public void onNavigatorFragmentItemClick(AdapterView<?> parent, long id, BaseAdapter adapter, View view, int position) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (m_app.m_home)
            return;
        if (m_app.m_dataModified) {
            if (m_resultController.m_type == DataMainActivity.Type.searcher) {
                m_resultController.m_searchIteration--;
                m_resultController.accessResult(this, true, false);
            } else
                m_resultController.doLook(this);
            m_app.m_dataModified = false;
        }
        m_resultController.m_processing = false;
    }

    public boolean checkRowForOpeningDetail(JotyResourceCursorAdapter adapter, Bundle m_extras) {
        return true;
    }

    public void updateNavigator() {
        m_navigatorListFragment.update(m_listFragment);
    }

    public void selectItem(int position) {
        if (m_listFragment != null)
            m_listFragment.setSelection(position);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (m_mainActivity.m_resultActivity == this)
            m_mainActivity.m_resultActivity = null;
    }

    @Override
    protected void beforeFinish(boolean forRenewing) {
        if (!forRenewing)
            m_mainActivity.m_resultControllersStack.pop();
        m_app.m_dataResultActivityStack.pop();
    }

    @Override
    protected void newRecord() {
        m_mainActivity.m_detailsControllersStack.push(m_resultController.m_detailsController);
        m_resultController.m_detailsController.openDetailsActivity(this, null);
    }
}
