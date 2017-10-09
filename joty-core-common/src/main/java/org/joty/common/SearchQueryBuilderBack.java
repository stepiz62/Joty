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

package org.joty.common;

/**
 * The class builds the final query for the search process, using the dbms
 * dependent template, located in the configuration file, and the information
 * needed to resolve all the parametric parts of it.
 * <p>
 * So that the {@code getQuery} method, invoked either by the
 * {@code SearchQueryBuilderFront} object or by a {@code org.joty.access.Accessor.DataDef} instance,
 * returns the 'page' of result corresponding to the number of iteration
 * received as parameter.
 * 
 * @see org.joty.data.SearchQueryBuilderFront
 * 
 */
public class SearchQueryBuilderBack {
	String m_paginationQuery;
	String m_paginationPageSize;

	public String getQuery(String setDefinition, String whereClause, String orderByClause, int iteration, String sharingExpr) {
		String retVal = null;
		boolean setDefinitionAsManyWords = setDefinition != null && Utilities.isMoreThanOneWord(setDefinition);
		if (sharingExpr != null)	
			setDefinition +=  " Where " + sharingExpr;
		retVal = m_paginationQuery;
		retVal = literalSubst(retVal, "openStm", setDefinitionAsManyWords ? "(" : "");
		retVal = literalSubst(retVal, "set", setDefinition);
		retVal = literalSubst(retVal, "whereConj", whereConj(whereClause, sharingExpr));
		retVal = literalSubst(retVal, "whereClause", whereClause);
		retVal = literalSubst(retVal, "sortField", orderByClause);
		retVal = literalSubst(retVal, "closeStm", setDefinitionAsManyWords ? ") xx" : "");
		retVal = literalSubst(retVal, "pageSize", m_paginationPageSize);
		retVal = literalSubst(retVal, "iteration", String.valueOf(iteration));
		return retVal;
	}

	String literalSubst(String inString, String paramName, String valueStr) {
		return literalSubst(inString, paramName, valueStr, -1);
	}

	String literalSubst(String inString, String paramName, String valueStr, int occurrence) {
		String literal = String.format("'<%1$s>'", paramName);
		return replace(inString, literal, valueStr, occurrence);
	}

	String replace(String inString, String lpszOld, String lpszNew) {
		return replace(inString, lpszOld, lpszNew, -1);
	}

	String replace(String inString, String lpszOld, String lpszNew, int occurrence) {
		String retVal = null;
		if (occurrence == -1)
			retVal = inString.replace(lpszOld, lpszNew);
		else {
			int pos = 0;
			int occurrenceCount = 0;
			while (occurrenceCount < occurrence) {
				pos = inString.indexOf(lpszOld);
				occurrenceCount++;
				if (pos < 0)
					break;
			}
			if (pos >= 0) {
				String leftTerm = inString.substring(0, pos);
				String rightTerm = inString.substring(pos + lpszOld.length());
				retVal = leftTerm + lpszNew + rightTerm;
			}
		}
		return retVal;
	}

	public void setPaginationQuery(String query, String pageSize) {
		m_paginationQuery = query;
		m_paginationPageSize = pageSize;
	}

	private String whereConj(String whereClause, String sharingExpr) {
	    return whereClause.length() > 0 ? (sharingExpr == null ? "WHERE" : "AND") : "";
	}

}
