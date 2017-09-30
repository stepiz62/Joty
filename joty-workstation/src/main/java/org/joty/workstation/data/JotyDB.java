/*
	Copyright (c) 2013-2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

	This file is part of Joty 2.0 Workstation.

	Joty 2.0 Workstation is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Joty 2.0 Workstation is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with Joty 2.0 Workstation.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.joty.workstation.data;

import java.sql.*;

import javax.naming.NamingException;

import org.joty.access.Accessor;
import org.joty.access.DbManager;
import org.joty.access.Logger;
import org.joty.access.PostStatement;
import org.joty.app.JotyException;
import org.joty.common.*;
import org.joty.common.ConfigFile.ConfigException;
import org.joty.common.AbstractDbManager.ExcCheckType;
import org.joty.web.AbstractWebClient.DocumentDescriptor;
import org.joty.workstation.app.Application;
import org.joty.workstation.web.WebClient;

/**
 * Provides a considerable contribution to the duality of the framework in
 * accessing the dbms: the ways are directly, via the jdbc layer, or via web,
 * through the WebClient object and the Joty Server.
 * <p>
 * Together with the {@code org.joty.workstation.data.WResultSet} class realizes
 * the interface with the virtual data level that the duality forms.
 * 
 */
public class JotyDB {

	public static Statement createStmnt() throws SQLException, NamingException {
		return createStmnt(false);
	}

	public static Statement createStmnt(boolean forUpdate) throws SQLException, NamingException {
		return forUpdate || m_doCreateStmnt ? 
					m_conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE) : 
					m_conn.prepareStatement(new String("fictitious"));
	}

	public static Connection m_conn;
	boolean m_webTransationOn;
	long m_lDbOutParam, m_lDbOutParam2;
	int m_nParams;
	public boolean m_dbSessionIsOpen;
	static boolean m_doCreateStmnt;

	private long m_autoIdVal;

	private DbManager m_dbManager;

	private Accessor m_accessor;

	Application m_app;

	public boolean acquireLock(String tabName) {
		return false;
	}

	private Application application() {
		if (m_app == null)
			m_app = Application.m_app;
		return m_app;
	}

	public void beginTrans() {
		if (application().m_webMode)
			application().m_webClient.beginTransaction();
		else {
			try {
				m_conn.setAutoCommit(false);
				m_conn.setSavepoint();
				m_app.clearReturnStatus();
			} catch (SQLException e) {
				Logger.exceptionToHostLog(e);
			}
		}
	}

	public void commitTrans() throws JotyException {
		endTrans(true);
	}

	void endTrans(boolean committing) throws JotyException {
		if (application().m_webMode) {
			WebClient webClient = application().m_webClient;
			if (committing) {
				if (!webClient.endTransaction())
					throw new JotyException(JotyException.reason.GENERIC, application().m_common.jotyLang("NoTransaction"), application());
			} else
				; // nothing to do with the server !
		} else {
			try {
				if (committing)
					m_conn.commit();
				else
					m_conn.rollback();
				m_conn.setAutoCommit(true);
			} catch (SQLException e) {
				Logger.exceptionToHostLog(e);
			}
		}
	}

	public boolean executeBytesStmnt(String sql, byte[] bytes, BasicPostStatement postStatement) {
		boolean success = false;
		try {
			PreparedStatement stmnt = m_conn.prepareStatement(nameSubst(postStatement, sql));
			stmnt.setBytes(1, bytes);
			stmnt.executeUpdate();
			stmnt.close();
			success = true;
		} catch (SQLException e) {
			Logger.exceptionToHostLog(e);
		}
		return success;
	}

	public boolean executeReturningStmnt(String sqlStmnt, String autoID, BasicPostStatement postStatement) {
		setPostStatement(postStatement);
		boolean retVal = false;
		PreparedStatement stmnt = null;
		if (autoID != null && autoID.length() > 0) {
			try {
				stmnt = m_conn.prepareStatement(nameSubst(postStatement, sqlStmnt), new String[] { autoID });
				stmnt.executeUpdate();
				ResultSet rset = stmnt.getGeneratedKeys();
				m_autoIdVal = 0;
				if (rset.next())
					m_autoIdVal = rset.getLong(1);
				stmnt.close();
				retVal = true;
			} catch (SQLException e) {
				try {
					if (getDbManager().dbExceptionCheck(e, ExcCheckType.CONSTR_VIOLATION_ON_UPDATE))
						throw new JotyException(JotyException.reason.CONSTR_VIOLATION_ON_UPDATE, e.getMessage(), application());
					else if (getDbManager().dbExceptionCheck(e, ExcCheckType.CONSTR_VIOLATION_ON_DELETE))
						throw new JotyException(JotyException.reason.CONSTR_VIOLATION_ON_DELETE, e.getMessage(), application());
					else
						Logger.exceptionToHostLog(e);
				} catch (JotyException e1) {}
			}
		} else
			Logger.appendToHostLog("Sql statement not executed since Joty is unable to identify a key elem for collecting ID generation \n\n" + 
									"statement : " + sqlStmnt);
		return retVal;
	}

	public boolean executeSQL(String sql) {
		return executeSQL(sql, null);
	}

	public boolean executeSQL(String sql, String autoID) {
		return executeSQL(sql, autoID, null);
	}

	public boolean executeSQL(String sql, String autoID, BasicPostStatement contextPostStatement) {
		return executeSQL(sql, autoID, contextPostStatement, 0);
	}
	
	/**
	 * A dual method for performing or 'delaying' the execution of a sql
	 * statement
	 * 
	 * @param sql
	 *            the statement text
	 * @param autoID
	 *            a possible auto incrementing field name
	 * @param contextPostStatement
	 *            the BasicPostStatement object
	 * @param nonManagedRollbackAction
	 *            only used in web mode : if greater than zero it identifies this statement as non managed by the transaction system - the execution of the statement must be tracked autonomously
	 *            for looking, in the case of transaction failure and success of its execution, for a corresponding action identified by the same scalar value but of negative sign;
	 *            if less than zero it identifies this statement as rollback action non manageable by the transaction manager of the dbms system and corresponding to the direct action
	 *            identified by the same scalar value.  
	 * @return true on success
	 */
	public boolean executeSQL(String sql, String autoID, BasicPostStatement contextPostStatement, int nonManagedRollbackAction) {
		boolean retVal;
		if (application().m_webMode)
			retVal = application().m_webClient.manageCommand(sql, autoID != null, autoID, contextPostStatement, nonManagedRollbackAction);
		else if (autoID == null)
			retVal = executeStmnt(sql, contextPostStatement);
		else
			retVal = executeReturningStmnt(sql, autoID, contextPostStatement);
		return retVal;
	}

	boolean executeStmnt(String sql, BasicPostStatement postStatement) {
		setPostStatement(postStatement);
		boolean success = false;
		Exception exception = null;
		try {
			Statement stmnt = createStmnt();
			stmnt.executeUpdate(nameSubst(postStatement, sql));
			stmnt.close();
			success = true;
		} catch (SQLException e) {
			try {
				if (getDbManager().dbExceptionCheck(e, ExcCheckType.CONSTR_VIOLATION_ON_UPDATE))
					throw new JotyException(JotyException.reason.CONSTR_VIOLATION_ON_UPDATE, e.getMessage(), application());
				else if (getDbManager().dbExceptionCheck(e, ExcCheckType.CONSTR_VIOLATION_ON_DELETE))
					throw new JotyException(JotyException.reason.CONSTR_VIOLATION_ON_DELETE, e.getMessage(), application());
				else if (getDbManager().dbExceptionCheck(e, ExcCheckType.DBMS_CREATEUSER_FAILURE))
					throw new JotyException(JotyException.reason.DBMS_CREATEUSER_FAILURE, e.getMessage(), application());
				else
					exception = e;
			} catch (JotyException e1) {}
			exception = e;
		} catch (NamingException e) {
			exception = e;
		}
		if (!success) {
			Logger.exceptionToHostLog(exception);
			if (Application.m_debug)
				m_app.JotyMsg(this, exception.getMessage());
		}
		return success;
	}

	public Accessor getAccessor() {
		if (m_accessor == null)
			m_accessor = application().m_accessor;
		return m_accessor;
	}

	public long getAutoIdVal() {
		return m_autoIdVal;
	}

	public byte[] getBytesFromDb(String sql, BasicPostStatement postStatement) {
		application().beginWaitCursor();
		byte[] retVal = application().m_webMode ? getBytesFromDbWeb(sql, postStatement) : getBytesFromDbJdbc(sql, postStatement);
		application().endWaitCursor();
		return retVal;
	}

	private byte[] getBytesFromDbJdbc(String sql, BasicPostStatement postStatement) {
		byte[] retVal = null;
		try {
			Statement stmnt = JotyDB.createStmnt();
			ResultSet rs = stmnt.executeQuery(postStatement == null ? sql : ((PostStatement) postStatement).nameSubst(getAccessor(), sql));
			Blob blob = null;
			if (rs.next()) {
				blob = rs.getBlob(1);
				if (blob != null)
					retVal = blob.getBytes(1, (int) (blob.length()));
			}
			stmnt.close();
		} catch (NamingException e) {
			Logger.exceptionToHostLog(e);
		} catch (SQLException e) {
			Logger.exceptionToHostLog(e);
		}
		return retVal;
	}

	private byte[] getBytesFromDbWeb(String sql, BasicPostStatement postStatement) {
		byte[] retVal = null;
		String querySql = null;
		WebClient wClient = application().m_webClient;
		if (m_app.m_accessorMode)
			postStatement.m_sql = sql;
		else
			postStatement = null;
		DocumentDescriptor docDescriptor = wClient.getDocumentFromRespContent(wClient.sqlQuery(querySql, false, true, postStatement, null));
		if (docDescriptor.xml != null)
			retVal = wClient.getBytesFromRespDocument(docDescriptor, application().m_common.m_fieldOrdinality ? "c1" : "c");
		return retVal;
	}

	/**
	 * Gets a jdbc {@code Connection} object specifying the database url caught
	 * from the configuration file and the user credentials living in the
	 * {@code Application} object. The Connection object is then stored in
	 * {@code m_conn} member variable. If the dbms is MS SqlServer the method
	 * drives the framework to use unconditionally
	 * {@code CreateStatement} method of the Connection object instead of
	 * {@code PrepareStatement} method. 
	 * 
	 * @param autoCommit
	 *            true if the connection will perform auto-commit statement
	 *            executions.
	 * @return true on success
	 * @throws SQLException
	 */
	public boolean getDbConn(boolean autoCommit) throws SQLException {
		Application app = application();
		try {
			m_conn = DriverManager.getConnection(app.m_common.m_configuration.configTermValue("connection-url"), app.m_common.m_userName, app.m_common.m_password);
		} catch (ConfigException e) {
			Logger.exceptionToHostLog(e);
		}
		if (m_conn != null) {
			m_doCreateStmnt = m_conn.getClass().getName().indexOf("com.micro") >= 0;
			m_conn.setAutoCommit(autoCommit);
		}
		return m_conn != null;
	}

	public DbManager getDbManager() {
		if (m_dbManager == null)
			m_dbManager = application().m_dbManager;
		return m_dbManager;
	}

	String nameSubst(BasicPostStatement postStatement, String text) {
		return postStatement == null ? text : ((PostStatement) postStatement).nameSubst(m_accessor, text);
	}

	public void rollbackTrans() throws JotyException {
		endTrans(false);
	}

	public void setPostStatement(BasicPostStatement postStatement) {
		if (getAccessor() != null)
			getAccessor().setPostStatement((PostStatement)postStatement);
	}

}
