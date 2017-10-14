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

import org.joty.common.BasicPostStatement;
import org.joty.common.SearchQueryBuilderBack;
import org.joty.mobile.app.JotyApp;

/**
 * An ActivityController specialized to serve the {@code DataResultActivity} object and its embedded
 * {@code JotyResultFragment} object.
 * <p>
 * Its main contribution to the framework is the {@code accessResult} method that takes care of building
 * a QueryResponseHandlersManager and the QueryResponseHandler objects (for the latter see the {@code createRespHandler}
 * ancestor method, both the Accessor mode is active and not.
 *
 * @see ActivityController
 * @see DataResultActivity
 * @see JotyApp.QueryResponseHandlersManager
 * @see DataMainActivity
 */

public class ResultController extends ActivityController {
    public int m_resultListLowestRefPos;
    public int m_searchIteration, m_performingIteration;
    public boolean m_furtherRecords;
    public String m_orderByClause, m_whereClause, m_mainFilter;
    public DataMainActivity.Type m_type;
    public Class m_detailsActivityClass;
    /** even not used by the methods of the class it is important that this member be hosted here as
     * controller of details activity directly concerning one record of the underlying set
     *
     * @see DetailsController
     */
    public DetailsController m_detailsController;
    public String m_setDefinition;
    private SearchQueryBuilderBack m_backDelegate;
    public boolean m_newAction;

    public ResultController(Class targetClass) {
        super(targetClass);
        m_resultListLowestRefPos = -1;
        m_backDelegate = new SearchQueryBuilderBack();
        m_backDelegate.setPaginationQuery(m_app.m_common.m_paginationQuery, m_app.m_common.m_paginationPageSize);
    }


    public void searchReset() {
        resetIteration();
        updatePaginationStatus(true, false);
    }

    public void resetIteration() {
        m_searchIteration = 0;
        m_furtherRecords = false;
    }

    public void updatePaginationStatus(boolean onReset, boolean backward) {
        if (m_searchIteration == 0 && backward)
            backward = false;
        if (!onReset)
            m_searchIteration++;
    }


    public void doLook(final JotyActivity starterActivity) {
        accessResult(starterActivity, false, false);
    }


    public void accessResult(final JotyActivity starterActivity, boolean pagination, boolean backward) {
        if (backward)
            m_searchIteration -= 2;
        m_performingIteration = m_searchIteration;
        starterActivity.setWaitCursor(true);
        JotyApp.QueryResponseHandlersManager qRespManager = null;
        int respManagerCount = getRespManagerCount();
        JotyApp.QueryResponseHandler respHandler = createRespHandler(starterActivity, respManagerCount);
        if (m_app.m_contextActivity.m_accessorMode) {
            BasicPostStatement postStatement = m_app.dataMainActivity().createContextPostStatement(m_accessorCoordinates);
            if (m_type == DataMainActivity.Type.searcher) {
                postStatement.m_mainFilter = m_whereClause;
                if (m_mainFilter != null) {
                    if (postStatement.m_mainFilter == null)
                        postStatement.m_mainFilter = "";
                    if (postStatement.m_mainFilter.length() > 0)
                        postStatement.m_mainFilter += " AND ";
                    postStatement.m_mainFilter += m_mainFilter;
                }
                postStatement.m_sortExpr = m_orderByClause;
                postStatement.m_iteration = pagination ? String.valueOf(m_searchIteration) : null;
            }
            qRespManager = m_app.new QueryResponseHandlersManager(postStatement, respHandler);
        } else
            qRespManager = m_app.new QueryResponseHandlersManager(getQuery(), respHandler);
        qRespManager.m_forPagination = pagination;
        qRespManager.m_backward = backward;
        qRespManager.setSmallBlobsList(m_smallBlobs);
        openRespManager(qRespManager, respManagerCount);
    }

    public void restoreIteration() {
        m_searchIteration = m_performingIteration;
    }

    public String getQuery() {
        String retVal = null;
        if (m_setDefinition == null)
            m_app.warningMsg("Set definition missing in query builder ! \n(use 'setQuerySetDef()' method in " +
                    getClass() + " or switch to the Accessor mode implementation)");
        else
            retVal = m_backDelegate.getQuery(m_setDefinition, m_whereClause, m_orderByClause, m_searchIteration, null);
        return retVal;
    }

}
