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

package org.joty.access;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import org.joty.access.Accessor.DataDef;
import org.joty.access.Accessor.PanelDataDef;
import org.joty.common.BasicPostStatement;
import org.joty.common.JotyMessenger;
import org.joty.common.ParamContext;
import org.joty.common.Utilities;
import org.joty.common.BasicPostStatement.Item;
import org.joty.common.ParamContext.ContextParam;

/**
 * It is the overriding that allows the direct interaction with the Accessor object.
 * <p>
 * The specificity of the implementation is the support of both the sides,
 * Application instance and Joty Server, for the life of the Accessor instance.
 * In collaboration with the Accessor class and the {@code ParamContext} class
 * (and the {@code MethodExecutor} class, too, for a stored method invocation),
 * the PostStatement class performs some sort of mirror of the communication
 * with server when the mode of running is local and the Accessor object lives
 * inside the Application object. This is the core of Joty 2.0: not only the
 * technology allows to switch, after the test has been made locally, to the web
 * mode, but, by the use of the Accessor object, it allows that the switch of
 * the running mode is made with the transfer of an entire business object to
 * the server side, after this object has been tested locally: "locally" means
 * without the JotyServer and, indeed, the servlet container running.
 * <p>
 * 
 * @see Accessor
 * @see org.joty.workstation.app.Application
 * @see org.joty.common.ParamContext
 * @see MethodExecutor
 * 
 */
public class PostStatement extends BasicPostStatement {
	public PostStatement(JotyMessenger jotyMessanger) {
		super(jotyMessanger);
	}

	/**
	 * This inner class contains in a textual form a value compatible with the
	 * datum wrapped by the {@code WrappedField} object or the value of a {@code ContextParam} object.
	 */

	public String getQueryFromDataDef(DataDef dataDef, Accessor accessor) {
		StringBuilder retVal = new StringBuilder();
		if (dataDef != null) {
			String sharedAlias = accessor.getSharingAlias(dataDef);
			String sharingExpr = null;
			if (accessor.m_shared && (m_termName == null || m_termName.length() == 0) && !dataDef.noSharingClause())
				sharingExpr = (sharedAlias == null ? "" : (sharedAlias + ".")) + accessor.sharingClause();			
			if (m_iteration != null && m_iteration.length() > 0 && Integer.parseInt(m_iteration) > -1)
				retVal.append(dataDef.getSearchSql(m_mainFilter, m_sortExpr, Integer.parseInt(m_iteration), m_method, sharingExpr));
			else {
				retVal.append(dataDef.getStatement(m_method));
				Utilities.composeSelectClauses(retVal, m_mainFilter, sharingExpr, m_sortExpr);
			}
		}
		return retVal.toString();
	}
	
	public void loadParamContext(ParamContext paramContext, boolean clearFirst) {
		if (clearFirst)
			paramContext.clear();
		for (Item item : m_items)
			paramContext.setContextParam(item.name, item.valueLiteral);
	}

	public String nameSubst(Accessor accessor, String sql) {
		if (accessor == null)
			return sql;
		else {
			PanelDataDef panelDataDef = accessor.getPanelDataDef(this);
			if (Utilities.isMoreThanOneWord(sql)) {
				if (sql.indexOf("<JOTY_CTX>") >= 0) {
					DataDef termDataDef = null;
					String name = panelDataDef == null ? 
									accessor.m_literalSubsts.get(m_termName) : 
									m_termName == null || m_termName.length() == 0 ? 
											panelDataDef.getUpdatableSet(m_method) : 
											(termDataDef = panelDataDef.m_termDataDefs.get(m_termName)) == null ? null : termDataDef.getUpdatableSet(m_method);
					Boolean manyWords = Utilities.isMoreThanOneWord(name);
					if (manyWords)
						name = Utilities.getMainTableNameFromSql(name);
					if (name == null) {
						accessor.m_errorCarrier.m_exceptionMsg.append( (panelDataDef == null ? 
																				("Substitution Literal '" + m_termName) : 
																				(m_termName == null ? 
																						("Substitution source from context '" + m_AccessorContext) : 
																						"Term name '" + m_termName + "' in context '" + m_AccessorContext)
																		) + 
																		"' not found !");
						Logger.appendToHostLog(accessor.m_errorCarrier.m_exceptionMsg.toString());
					}
					return name == null ? sql : sql.replace("<JOTY_CTX>", name);
				} else
					return sql;
			} else {
				DataDef dataDef = panelDataDef == null ? accessor.m_statementDefs.get(sql) : panelDataDef.m_statementDefs.get(sql);
				return dataDef == null ? null : dataDef.getStatement(m_method);
			}
		}
	}


}
