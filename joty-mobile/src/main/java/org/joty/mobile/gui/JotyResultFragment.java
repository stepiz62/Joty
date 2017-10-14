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
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.joty.mobile.app.JotyApp;
import org.joty.mobile.data.JotyCursorWrapper;

/**
 * It is an abstract class that retrieves the {@code QueryResponseHandlersManager}, which the container Activity works with.
 * Then extracts the {@code JotyCursor} object embedded in the {@code WResulSet} object hosted by the response manager,
 * and wraps it with an instance of {@code JotyCursorWrapper}. Indeed the wrapper is passed as argument
 * to the abstract {@code createAdapter} method that builds a {@code JotyResourceCursorAdapter} instance;
 * <p>
 * On attaching to the container it gets a reference to the {@code ResultController} object and provides
 * itself as reference in order to be "invoked" by the container (a {@code DataResultActivity}).
 * <p>
 * On the View creation handles the visualization of the navigation buttons and stores the index of the lowest visible row.
 *  <p>
 * The class works with two row index markers used to scroll the list, upon the initialization,  to a certain position:
 * the first one, {@code m_resultActivity.m_initialSelection}, is assumed to be a "long life" value, available
 * for specific implementations, surviving among different launches of the application;
 * the second one, {@code ResultController.m_resultListLowestRefPos}, holds the value gettable from
 * {@code AdapterView.getFirstVisiblePosition} method invoked on the parent of the row view inflated by
 * the adapter created in the implementation of the {@code createAdapter} method, as the parent is received,
 * as argument, in the implementation that this class does of the {@code AbsListView.OnItemClickListener.onItemClick} method.
 * The second marker, indeed has the role of maintain a position in the visualization of the list upon a renewing of it.
 * <p>
 * The {@code onItemClick} implementation, then, if the ResultActivity is running as "selector", uses the {@code ValuesOnChoice}
 * instance to make the "context selected" available to the calling environment (waht is selected here wil be typically usable at
 * in the onResume event handler of the Activity that committed the opening of the ResultActivity.
 * If the ResultActivity is not running as selector then its {@code onFragmentItemClick} method is invoked.
 *
 * @see DataResultActivity
 * @see ResultController
 * @see JotyApp.QueryResponseHandlersManager
 * @see JotyListFragment
 * @see JotyActivity#getRespManager
 * @see JotyResourceCursorAdapter
 * @see org.joty.mobile.app.JotyApp.ValuesOnChoice
 */

public abstract class JotyResultFragment extends JotyListFragment {
    boolean m_pagination;
    JotyApp.QueryResponseHandlersManager m_qRespManager;
    int m_underlayingRecords;
    JotyCursorWrapper m_cursor;
    protected ResultController m_resultController;

    protected abstract String getKeyFieldName();

    protected abstract JotyResourceCursorAdapter createAdapter(JotyCursorWrapper cursorWrapper);

    @Override
    protected void init() {
        m_qRespManager = m_resultActivity.getRespManager();
        if (m_qRespManager != null) {
            m_cursor = new JotyCursorWrapper( m_qRespManager.m_rs.m_jotyCursor, getKeyFieldName());
            m_underlayingRecords = m_cursor.getCount();
            m_pagination = m_qRespManager.m_forPagination;
            int maxRecord = 0;
            if (m_pagination) {
                maxRecord = Integer.parseInt(m_app.m_common.m_paginationPageSize);
                m_resultController.m_furtherRecords = maxRecord > 0 && m_underlayingRecords > maxRecord;
            } else
                m_resultController.m_furtherRecords = false;
            m_adapter = createAdapter(m_cursor);
            if (m_pagination)
                ((JotyResourceCursorAdapter) m_adapter).m_maxRecordQty = maxRecord;
        }
    }

    @Override
    protected boolean isEmptyViewVisible() {
        return m_underlayingRecords == 0;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (m_pagination) {
            m_resultActivity.m_previousVisible = m_resultController.m_searchIteration > 0;
            m_resultController.updatePaginationStatus(false, m_qRespManager.m_backward);
            m_resultActivity.m_nextVisible = m_resultController.m_furtherRecords;
        }
        if (m_resultActivity.m_navigatorListFragment != null)
            m_resultActivity.updateNavigator();
        ListView listView = getListView(view);
        if (m_resultActivity.m_initialSelection >= 0) {
            listView.setSelection(m_resultActivity.m_initialSelection);
            m_resultActivity.m_initialSelection = -1;
        }
        int lowestRefPos = m_resultController.m_resultListLowestRefPos;
        if (lowestRefPos >= 0) {
            if (lowestRefPos >= listView.getCount())
                lowestRefPos = listView.getCount() - 1;
            listView.setSelection(lowestRefPos);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mainActivity().m_resultControllersStack.size() == 1 && mainActivity().m_asSelector) {
            m_app.m_valuesOnChoice.acquire(id, m_cursor);
            m_resultActivity.finish();
        } else {
            m_resultController.m_resultListLowestRefPos = parent.getFirstVisiblePosition();
            m_resultActivity.onFragmentItemClick(id, (JotyResourceCursorAdapter) m_adapter, position);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        m_resultActivity.m_listFragment = this;
        m_resultController = m_resultActivity.m_resultController;
    }

    public DataMainActivity mainActivity() {
        return m_app.dataMainActivity();
    }

    public JotyCursorWrapper getCursor() {
        return (JotyCursorWrapper) ((JotyResourceCursorAdapter) getAdapter()).getCursor();
    }
}
