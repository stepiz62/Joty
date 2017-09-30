/*
	Copyright (c) 2013-2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

	This file is part of Joty 2.0 Core.

	Joty 2.0 Core is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Joty 2.0 Core is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with Joty 2.0 Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.joty.data;

import org.joty.app.Common;
import org.joty.app.JotyApplication;
import org.joty.common.ApplMessenger;
import org.joty.common.JotyTypes;
import org.joty.common.SearchQueryBuilderBack;
import org.joty.common.Utilities;

/**
 * The class assists the {@code SearcherPanel} instance and the {@code DataMainActivity} in providing the final
 * query to be used in the search process.
 * <p>
 * For this it provides the {@code addToWhere(Term term)} that builds the where
 * clause contribution due to the {@code Term} object. If the Accessor mode is
 * on a direct contribution is provided by the class: the where clause, in this way
 * composed, will be packaged on a BasicPostStatement object for delivery.
 * <p>
 * If the Accessor mode is off the class offers the {@code getQuery} method in
 * which a delegated object (a {@code SearchQueryBuilderBack} instance), using
 * the data up to that point built, returns the final query.
 *
 *
 * @see org.joty.common.SearchQueryBuilderBack
 *
 */
public class SearchQueryBuilderFront {
    public String m_setDefinition, m_whereClause, m_orderByClause;
    public SearchQueryBuilderBack m_backDelegate;
    JotyApplication m_app;
    TermContributor m_termContributor;


    public SearchQueryBuilderFront(JotyApplication app, TermContributor termContributor) {
        m_app = app;
        m_termContributor = termContributor;
        m_backDelegate = new SearchQueryBuilderBack();
       	Common common = (Common) ((ApplMessenger) app).getCommon();
       m_backDelegate.setPaginationQuery(common.paginationQuery(),common.paginationPageSize());
    }

    public interface TermContributor {
        public String sqlValueExpr(WrappedField term);
        public String getOperator(WrappedField term, String matchOperator);
    }

    public void addToWhere(WrappedField term) {
        String literal = "";
        int termDataType = term.dataType();
        boolean termIsTextual = termDataType == JotyTypes._text;
        String valExpr = termDataType == JotyTypes._date && term.isNull() ? "" : m_termContributor.sqlValueExpr(term).trim();
        if (termIsTextual && valExpr.length() > 0)
            valExpr += "%";
        if (valExpr.length() > 0) {
            if (termIsTextual) {
                valExpr = Utilities.sqlEncoded(valExpr);
                valExpr = "'" + valExpr + "'";
                valExpr = valExpr.toUpperCase();
            }
            String operator = m_termContributor.getOperator(term, termIsTextual ? "LIKE" : "=");
            literal = String.format(termIsTextual ? "UPPER(%1$s) %2$s %3$s" : "%1$s %2$s %3$s", term.m_dbFieldName, operator, valExpr);
        }
        if (literal.length() > 0) {
            if (m_whereClause == null)
                m_whereClause = "";
            if (m_whereClause.length() > 0)
                m_whereClause += " AND ";
            m_whereClause += literal;
        }
    }

    public void clearWhere() {
        m_whereClause = "";
    }


}
