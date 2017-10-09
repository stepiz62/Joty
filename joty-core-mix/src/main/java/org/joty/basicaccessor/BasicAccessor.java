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

package org.joty.basicaccessor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

import org.joty.access.Accessor;
import org.joty.access.Logger;
import org.joty.common.*;
import org.joty.common.ConfigFile.ConfigException;


/**
 * Descending from the {@code Accessor} class it defines basic access statements
 * and encapsulates critical name space names, like those ones addressing
 * authorization information located in the database. It serves, indeed, for the
 * most part, the visual objects of the {@code org.joty.workstation.authorization} and
 * of the {@code org.joty.mobile.authorization} packages.
 * <p>
 * It can be said also that the framework must provide the minimum of an
 * Accessor instance because some built-in visual class, always instantiated in
 * a regular Joty application, works in Accessor mode.
 * <p>
 * The content definition for the {@code UsersDialog} instance is such that only
 * the users not having the 'Administrators' role are listed in the result.
 * However the available set for role attribution includes the 'Administrators'
 * item: if an administrator elects another user to be administrator he/she
 * cannot anymore remove that user from having the role, a super user is needed
 * (typically a database administrator}.
 * 
 * @see Accessor
 * @see LangLiteralRetCodeMapper
 * @see org.joty.workstation.authorization.UsersDialog
 * @see org.joty.workstation.authorization.RolesDialog
 * @see org.joty.workstation.gui.AppOptionsDialog
 * 
 */

public class BasicAccessor extends Accessor {

	String m_roleTable;
	String m_userTable;
	String m_userRoleTable;
	String m_applicationTable;
	String m_localeTable;

	String m_userDbField;
	String m_passDbField;
	String m_forcePwdChangeDbField;
	String m_updatePassDbField;
	int m_passwordExpDays;
	String m_sqlDateExpr;

	private ConfigFile m_configFile;

	@Override
	public void setFromConfiguration(ConfigFile configFile) {
		try {
			m_configFile = configFile;
			m_roleTable = confGet("roleTable", "role");
			m_userTable = confGet("userTable", "appuser");
			m_userRoleTable = confGet("userRoleTable", "userrole");
			m_applicationTable = confGet("applicationTable", "application");
			m_localeTable = confGet("localeTable", "locale");
			m_userDbField = confGet("userDbField", "Username");
			m_passDbField = confGet("passDbField", "Password");
			m_forcePwdChangeDbField = confGet("forcePwdChangeDbField", "forcePwdChange");
			m_updatePassDbField = confGet("updatePassDbField", "forcePwdChange");
			m_passwordExpDays = Integer.parseInt(m_configFile.configTermValue("passwordExpDays", true));
			m_sqlDateExpr = confGet("sqlDateExpr", "'%1$s'");
		} catch (ConfigException e) {
			Logger.exceptionToHostLog(e);
		}
	}

	private String confGet(String literal, String defaultVal) throws ConfigException {
		String retVal =  m_configFile.configTermValue(literal, true);
		return retVal == null ? defaultVal : retVal;
	}

	private String selectUserStmnt(String userName, String sharingKey) {
		return String.format("Select %1$s.* from %1$s where UPPER(%2$s) = '%3$s'" + (sharingKey == null ? "" : " and sharingKey = '%4$s'"),
				m_userTable, m_userDbField, userName.toUpperCase(), sharingKey);
	}

	public long verifyLogin(String userName, String password, String sharingKey) throws SQLException {
		return __verifyLogin(selectUserStmnt(userName, sharingKey), password);
	}

	public long verifyLogin(String userName, String password) throws SQLException {
		return __verifyLogin(selectUserStmnt(userName, null), password);
	}

	private long __verifyLogin(String sqlText, String password) throws SQLException {
		long retVal = 0;
		ResultSet result = m_conn.createStatement().executeQuery(sqlText);
		if (result != null) {
			if (result.next()) {
				String pwdOnDB = result.getString(m_passDbField);
				if (pwdOnDB == null)
					retVal = m_langLiteralRetCodeMapper.retCode("PwdCheckNotPossible");
				else {
					String pwd = Utilities.md5Digest(password);
					if (pwd.compareToIgnoreCase(pwdOnDB) != 0)
						retVal = m_langLiteralRetCodeMapper.retCode("WrongUserOrPwd");
				}
			} else
				retVal = m_langLiteralRetCodeMapper.retCode("UserNotFound");
			result.close();
		}
		return retVal;
	}

	public long isDateExpired(String userName) throws SQLException { 
		return __isDateExpired(selectUserStmnt(userName, null));
	}

	public long isDateExpired(String userName, String sharingKey) throws SQLException { 
		return __isDateExpired(selectUserStmnt(userName, sharingKey));
	}

	public long __isDateExpired(String sqlText) throws SQLException { 
		long retVal = -1;
		ResultSet result = m_conn.createStatement().executeQuery(sqlText);
		if (result != null) {
			if (result.next()) {
				Date dateFromDb = null;	
				dateFromDb = result.getDate(m_updatePassDbField);
				if (dateFromDb == null)
					retVal = 0;
				else {
					Calendar calDateFromDb = Calendar.getInstance();
					calDateFromDb.setTime(dateFromDb);
					retVal = Utilities.daysQtyBetween(calDateFromDb, Calendar.getInstance()) > m_passwordExpDays ? 0 : -1;
				}
			}
			result.close();
		}
		return retVal;
	}

	public long mustPasswordBeChanged(String userName, String sharingKey) throws SQLException {
		return __mustPasswordBeChanged(selectUserStmnt(userName, sharingKey));
	}

	public long mustPasswordBeChanged(String userName) throws SQLException {
		return __mustPasswordBeChanged(selectUserStmnt(userName, null));
	}

	public long __mustPasswordBeChanged(String sqlText) throws SQLException {
		long retVal = -1;
		ResultSet result = m_conn.createStatement().executeQuery(sqlText);
		if (result != null) {
			if (result.next()) 
				if (result.getInt(m_forcePwdChangeDbField) == 1)
					retVal = 0;
			result.close();
		}
		return retVal;
	}

	@Override
	protected void loadDefs() {

		m_literalSubsts.put("D0_1", m_roleTable);

		m_statementDefs.put("LoadUserRoles", new DataDef() {
			{
				setStatementHolder(new ExprHolder() {
					@Override
					public String getExpr() {
						return String.format(
								"	SELECT %1$s.NAME AS roleName               			" +
										"	FROM %1$s, %2$s, %3$s                      	" +
										"	WHERE %2$s.ID = %3$s.userID                	" +
										"		AND %1$s.ID = %3$s.roleID              	" +
										"		AND Upper(username) = upper('%4$s')    	",
										m_roleTable, 
										m_userTable,
										m_userRoleTable, 
										paramValue("userName")
								);

					}					
				});
			}
		});
		m_statementDefs.put("setUserPassword", new DataDef() {
			{
				setStatementHolder(new ExprHolder() {
					@Override
					public String getExpr() {
						return String.format(
								"Update %1$s set %2$s = '%3$s', %4$s = %5$s, %6$s = %7$s where upper(%8$s) = upper('%9$s')",
								m_userTable, 
								m_passDbField,
								Utilities.md5Digest(paramValue("newPwd")), 
								m_updatePassDbField,
								Utilities.todaySqlExpr(m_sqlDateExpr), 
								m_forcePwdChangeDbField, 
								isMissingContextParam("forcePwdChange") ? "0" : paramValue("forcePwdChange"), 
								m_userDbField, 
								paramValue("setPwdUsername")
								);
					};

				});
			}
		});
		m_dialogDataDefs.put("org.joty.workstation.authorization.RolesDialog", new DialogDataDef() {
			{			
				m_panelDataDefs.add(new PanelDataDef() {  
					{
						setUpdatableSet(m_roleTable);
						setSqlToDefault();
						setNoSharingClause();
					}
				});
			}
		});
		m_dialogDataDefs.put("org.joty.workstation.authorization.UsersDialog", new DialogDataDef() {
			{			
				m_panelDataDefs.add(new PanelDataDef() {  
					{
						setUpdatableSet(m_userTable);
						setStatementSql(String.format(
								"Select distinct %1$s.* from %1$s left join %2$s on %1$s.id = %2$s.userID Where " + 
										(m_shared ? "%2$s.roleID" : "%1$s.id") + " != 1", 
										m_userTable, m_userRoleTable));
						m_termDataDefs.put("Roles", new DataDef() {
							{
								setUpdatableSet(m_userRoleTable);
								setSqlToDefault();
							}
						});
					}
				});
			}
		});
		m_dialogDataDefs.put("org.joty.workstation.gui.AppOptionsDialog", new DialogDataDef() {
			{			
				m_panelDataDefs.add(new PanelDataDef() {
					{
						setUpdatableSet(m_applicationTable);
						setSqlToDefault();
						m_termDataDefs.put("loc_literal", new DataDef() {
							{
								setUpdatableSet(m_localeTable);
								setSqlToDefault();
							}
						});
					}
				});
			}
		});
	}
}
