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

import org.joty.app.JotyApplication;
import org.joty.common.BasicPostStatement;
import org.joty.common.JotyMessenger;


public class StatementBuilder {

    JotyMessenger m_jotyMessanger;

    public StatementBuilder(JotyApplication jotyApplication) {
        m_jotyMessanger = jotyApplication;
    }

    public interface StatementBuilderHelper {
		int colCount();
		String fieldForInsert(short index);
		String fieldForUpdate(short index);
		String getValueForInsert(String fieldName);
		String getValueForUpdate(String fieldName);
		void setContextPostStatement(BasicPostStatement contextPostStatement);
		String tableName();
		String updateWhereClause();
	}
	public StatementBuilderHelper helper;

	public String genInsertStmnt() {
		String builtStr = "INSERT INTO ";
		builtStr += helper.tableName();
		builtStr += " ";
		String sqlValuesClause = "Values (";
		String CommaSepFieldList = "(";
		int fieldQty = helper.colCount();
		String fieldName;
		boolean listEmpty = true;
		for (short fldIdx = 0; fldIdx < fieldQty; fldIdx++) {
			try {
				fieldName = helper.fieldForInsert(fldIdx);
				if (fieldName != null) {
					if (!listEmpty) {
						sqlValuesClause += ",";
						CommaSepFieldList += ",";
					}
					CommaSepFieldList += fieldName;
					sqlValuesClause += helper.getValueForInsert(fieldName);
					if (listEmpty)
						listEmpty = false;
				}
			} catch (Exception e) {
                m_jotyMessanger.jotyMessage(e);
			}
		}
		sqlValuesClause += ")";
		CommaSepFieldList += ")";
		builtStr += CommaSepFieldList;
		builtStr += " ";
		builtStr += sqlValuesClause;
		return listEmpty ? null : builtStr;
	}

	public String genUpdateStmnt() {
		String builtStr = "UPDATE ";
		builtStr += helper.tableName();
		builtStr += " SET ";
		int fieldQty = helper.colCount();
		String fieldName;
		boolean listEmpty = true;
		for (short fldIdx = 0; fldIdx < fieldQty; fldIdx++) {
			try {
				fieldName = helper.fieldForUpdate(fldIdx);
				if (fieldName != null) {
					if (!listEmpty)
						builtStr += ",";
					builtStr += fieldName;
					builtStr += " = ";
					builtStr += helper.getValueForUpdate(fieldName);
					if (listEmpty)
						listEmpty = false;
				}
			} catch (Exception e) {
                m_jotyMessanger.jotyMessage(e);
			}
		}
		if (!listEmpty)
			builtStr += helper.updateWhereClause();
		return listEmpty ? null : builtStr;
	}

	public void setHelper(StatementBuilderHelper helper) {
		this.helper = helper;
	}

}
