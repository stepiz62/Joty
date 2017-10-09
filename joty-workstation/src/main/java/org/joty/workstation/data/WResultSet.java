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

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import javax.naming.NamingException;

import org.joty.access.Accessor;
import org.joty.access.Accessor.DataDef;
import org.joty.access.DbManager;
import org.joty.access.Logger;
import org.joty.access.PostStatement;
import org.joty.app.Common;
import org.joty.app.JotyException;
import org.joty.app.JotyException.reason;
import org.joty.common.ApplMessenger;
import org.joty.common.BasicPostStatement;
import org.joty.common.JotyTypes;
import org.joty.common.Utilities;
import org.joty.common.Utilities.Stocker;
import org.joty.data.BasicJotyCursor;
import org.joty.data.FieldDescriptor;
import org.joty.data.WrappedField;
import org.joty.data.WrappedResultSet;
import org.joty.workstation.app.Application;
import org.joty.workstation.web.WebClient;

/**
 * 
 * This descendant of {@code WrappedResultSet} participates in the
 * implementation of the duality of the running mode of the Joty application for
 * workstation.
 * <p>
 * It is typical to have, indeed, methods that show either an ordinary
 * processing on a wrapped {@code java.sql.ResultSet} object or the invocation
 * of the web client instance, in order to communicate with the Joty Server and
 * to get data or returned codes from it, as response to inquiries
 * or sql commands, respectively.
 * <p>
 * The {@code open} and the {@code next} methods are examples of how the
 * {@code m_cursor} member is populated from two different possible sources.
 * <p>
 * In the case of direct access to the jdbc layer the primitives of it are used.
 * <p>
 * The class supports the Accessor mode also and takes the suited action after
 * having detected it on the invoking environment.
 * 
 * @see org.joty.access.Accessor
 * @see org.joty.workstation.web.WebClient
 */

public class WResultSet extends WrappedResultSet {
	
	public ResultSet m_result;
	Statement m_statement;
	private boolean m_isEof;
	private boolean m_isBof;
	private boolean m_isOpen;
	private boolean m_forUpdate;
	private DbManager m_dbManager;
	public Accessor m_accessor;
	public String m_extractedWhereClause;
	private Application m_application;

	@Override
	protected FieldDescriptor fieldDescriptor(String fieldName) {
		if (m_app.debug()) {
			FieldDescriptor retVal = m_cursor.m_fieldsMap.get(fieldName);
			if (retVal == null) {
				String msgTheme = "Database field '" + fieldName + "' not found ! " + (m_metadataReuse ? "Turn the option 'reuseMetadataOnLoadForStore' off in the configuration context \n" + " or overwrite the method 'DataAccessPanel.updatableFieldsHaveDescriptorsAvailable' having it to return false !" : "") + "\n\nThe set definition is:\n\n" + Utilities.formattedSql(m_sql);
				m_app.jotyWarning(msgTheme);
			}
			return retVal;
		} else
			return m_cursor.m_fieldsMap.get(fieldName);
	}

	@Override
	public void addNew() {
		super.addNew();
		if (!m_application.m_webMode) {
			try {
				m_result.moveToInsertRow();
				jdbcAccess();
			} catch (SQLException e) {
				Logger.exceptionToHostLog(e);
			}
		}
	}
	
    @Override
    public String getSql() {
        return m_sql;
    }

    @Override
    public int colCount() {
        return m_colCount;
    }

    @Override
	public void setDescriptor(BasicJotyCursor descriptor) {
		super.setDescriptor(descriptor);
		m_colCount = descriptor.m_fields.length;
	}

  
    @Override
	public void setSql(String sql) {
		m_sql = sql;
	}

    @Override
    public  boolean webMode() {
    	return m_application.m_webMode;
    }

    @Override
	public boolean isBOF() {
		return webMode() ? super.isBOF() : m_isBof;
	}

    @Override
	public boolean isEOF() {
		return webMode() ? super.isEOF() : m_isEof;
	}

	public WResultSet(Object fictitious, String sql) {
		this(null, sql, false);
	}

	public WResultSet(String tableName, String sql, boolean forUpdate) {
		this(tableName, sql, forUpdate, null);
	}

	public WResultSet(String tableName, String sql, boolean forUpdate, Stocker openForUpdateFields) {
		this(tableName, sql, forUpdate, openForUpdateFields, null);
	}

	public WResultSet(String tableName, String sql, boolean forUpdate, Stocker openForUpdateFields, PostStatement postStatement) {
		m_application = Application.m_app;
        m_app = m_application;
	    checkSetName(tableName);
		if (getAccessor() == null || postStatement == null)
			m_sql = initSql(tableName, sql, openForUpdateFields, postStatement);
		else {
			getAccessor().setPostStatement(postStatement);
			DataDef panelDef = getAccessor().getDataDef();
			String updatableSet = panelDef.getUpdatableSet(postStatement.m_method);
			Boolean manyWordsInUpdatableSet = Utilities.isMoreThanOneWord(updatableSet);
			String dbTable = manyWordsInUpdatableSet ? Utilities.getMainTableNameFromSql(updatableSet) : updatableSet;
			m_extractedWhereClause = manyWordsInUpdatableSet ? Utilities.getWhereClauseNameFromSql(updatableSet) : null;
			m_tableName = dbTable;
			m_sql = selectStmnt(dbTable);
		}
		init(forUpdate);
	}

	@Override
	public boolean getValue(WrappedField wfield) {
		return setValueToWField(wfield.dbFieldName(), wfield);
	}

	private void init(boolean forUpdate) {
		m_forUpdate = forUpdate;
		initialize();
		m_isBof = true;
		m_isEof = false;
		m_dbManager = m_application.m_dbManager;
		m_accessor = m_application.m_accessor;
	}


	
	@Override
	public void next() {
		if (webMode())
			super.next();
		else
			try {
				m_isEof = !m_result.next();
				if (!m_isEof)
					jdbcAccess();
			} catch (SQLException e) {
				Logger.exceptionToHostLog(e);
			}
	}


	public boolean isOpen() {
		return m_isOpen;
	}

	public void jdbcAccess() throws SQLException {
		jdbcAccess(false);
	}

	public void jdbcAccess(boolean store) throws SQLException {
		FieldDescriptor col;
		if (store) {
			for (int i = 0; i < m_colCount; i++) {
				col = m_cursor.m_fields[i];
				if (col.m_toUpdate) {
					if (col.m_nType != JotyTypes._blob && col.m_nType != JotyTypes._smallBlob) {
						if (col.m_isNull)
							m_result.updateNull(col.m_strName);
						else
							switch (col.m_nType) {
								case JotyTypes._text:
									m_result.updateString(col.m_strName, col.m_strVal);
									break;
								case JotyTypes._int:
									m_result.updateInt(col.m_strName, col.m_iVal);
									break;
								case JotyTypes._long:
									m_result.updateLong(col.m_strName, col.m_lVal);
									break;
								case JotyTypes._single:
									m_result.updateFloat(col.m_strName, col.m_fltVal);
									break;
								case JotyTypes._double:
									m_result.updateDouble(col.m_strName, col.m_dblVal);
									break;
								case JotyTypes._date:
									m_result.updateDate(col.m_strName, col.m_dateVal.getSqlDate());
									break;
								case JotyTypes._dateTime:
									m_result.updateTimestamp(col.m_strName, col.m_dateVal.getSqlDateTime());
									break;
								default:
									m_app.JotyMsg(null, col.m_strName + " : no manageable data type for saving");
							}
					}
				}
			}
		} else
			for (int i = 0; i < m_colCount; i++) {
				col = m_cursor.m_fields[i];
				switch (col.m_nType) {
					case JotyTypes._text:
						col.m_strVal = m_result.getString(col.m_strName);
						break;
					case JotyTypes._int:
						col.m_iVal = m_result.getInt(col.m_strName);
						break;
					case JotyTypes._long:
						col.m_lVal = m_result.getLong(col.m_strName);
						break;
					case JotyTypes._single:
						col.m_fltVal = m_result.getFloat(col.m_strName);
						break;
					case JotyTypes._double:
						col.m_dblVal = m_result.getDouble(col.m_strName);
						break;
					case JotyTypes._date:
						col.m_dateVal.setDate(m_result.getDate(col.m_strName));
						break;
					case JotyTypes._dateTime:
						col.m_dateVal.setDate(m_result.getTimestamp(col.m_strName));
						break;
					case JotyTypes._blob: // no op
						break;
					case JotyTypes._smallBlob: {
						Blob blob = m_result.getBlob(col.m_strName);
						col.m_previewBytes = blob == null ? null : blob.getBytes(1, (int) (blob.length()));
					}
						break;
				}
				col.m_isNull = m_result.wasNull();
			}
	}

	public boolean jdbcOpen(BasicPostStatement postStatement) throws SQLException {
		boolean retVal = false;
		try {
			m_statement = JotyDB.createStmnt(m_forUpdate);
			m_application.m_db.setPostStatement(postStatement);
			m_result = m_statement.executeQuery(m_application.m_db.nameSubst(postStatement, m_sql));
			ResultSetMetaData metadata = m_result.getMetaData();
			m_colCount = metadata.getColumnCount();
			m_cursor = new BasicJotyCursor(m_colCount, m_app);
			int index;
			FieldDescriptor currColumn;
			for (int i = 0; i < m_colCount; i++) {
				currColumn = new FieldDescriptor(m_app);
				m_cursor.m_fields[i] = currColumn;
				m_cursor.m_fields[i].m_pos = i;
				index = i + 1;
				currColumn.m_strName = metadata.getColumnLabel(index);
				m_cursor.m_fieldsMap.put(currColumn.m_strName, currColumn);
				currColumn.m_nSqlType = metadata.getColumnType(index);
				currColumn.m_lPrecision = metadata.getPrecision(index); // size
				currColumn.m_nScale = metadata.getScale(index); // decimals
				switch (currColumn.m_nSqlType) {
					case Types.CHAR:
					case Types.VARCHAR:
						currColumn.m_nType = JotyTypes._text;
						break;
					case Types.FLOAT:
						currColumn.m_nType = JotyTypes._single;
						break;
					case Types.DOUBLE:
						currColumn.m_nType = JotyTypes._double;
						break;
					case Types.REAL:
					case Types.NUMERIC:
					case Types.BIGINT:
						if (currColumn.m_nScale == 0 || currColumn.m_nScale == -127)
							if (currColumn.m_lPrecision > ((Common) ((ApplMessenger) m_app).getCommon()).m_intDigitDim)
								currColumn.m_nType = JotyTypes._long;
							else
								currColumn.m_nType = JotyTypes._int;
						else
							currColumn.m_nType = JotyTypes._double;
						break;
					case Types.INTEGER:
						currColumn.m_nType = JotyTypes._long;
						break;
					case Types.SMALLINT:
						currColumn.m_nType = JotyTypes._int;
						break;
					case 11:
					case Types.DATE:
						currColumn.m_nType = JotyTypes._date;
						break;
					case Types.TIMESTAMP:
						currColumn.m_nType = JotyTypes._dateTime;
						break;
					case Types.BLOB:
					case Types.VARBINARY:
					case Types.LONGVARBINARY:
						currColumn.m_nType = m_smallBlobs == null || m_smallBlobs.contains(currColumn.m_strName) ? JotyTypes._smallBlob : JotyTypes._blob;
						break;
					default:
						if (currColumn.m_nSqlType == Types.LONGVARCHAR && currColumn.m_lPrecision > 255 && currColumn.m_nScale == 0)
							currColumn.m_nType = JotyTypes._text;
						else {
							if (Application.m_app.debug())
								m_app.JotyMsg(this, "Unmapped Sql type " + String.valueOf(currColumn.m_nSqlType) + " for field '" + currColumn.m_strName + "' !");
							currColumn.m_nType = JotyTypes._none;
						}
				}
			}
			next();
			retVal = true;
			m_isOpen = true;
		} catch (NamingException e) {
			Logger.exceptionToHostLog(e);
		}
		return retVal;
	}

	public boolean open() {
		return open(null);
	}

	public boolean open(boolean forMetadataOnly) {
		return open(forMetadataOnly, null);
	}

	public boolean open(BasicPostStatement postStatement) {
		return open(false, postStatement);
	}

	public boolean open(boolean forMetadataOnly, BasicPostStatement postStatement) {
		m_actionFields.clear();
		try {
			return webMode() ? webOpen(forMetadataOnly, postStatement, null) : jdbcOpen(postStatement);
		} catch (SQLException e) {
			checkForReconnect(e);
			if (m_application.m_connected)
				m_app.JotyMsg(this, "Sql is:\n\n" + Utilities.formattedSql(m_sql));
			return false;
		}
	}

	public boolean update(boolean newRec, boolean withAutoIncrId) {
		return update(newRec, withAutoIncrId, null);
	}

	public boolean update(boolean newRec, boolean withAutoIncrId, BasicPostStatement contextPostStatement) {
		boolean retVal = true;
		if (webMode() || newRec && withAutoIncrId)
			retVal = updateByStatement(newRec, withAutoIncrId, contextPostStatement);
		else {
			try {
				jdbcAccess(true);
				if (newRec)
					m_result.insertRow();
				else
					m_result.updateRow();
			} catch (SQLException e) {
				retVal = false;
				if (m_dbManager.dbExceptionCheck(e, DbManager.ExcCheckType.CONSTR_VIOLATION_ON_UPDATE)) {
					try {
						throw new JotyException(JotyException.reason.CONSTR_VIOLATION_ON_UPDATE, e.getMessage(), m_app);
					} catch (JotyException e1) {}
				} else
					Logger.exceptionToHostLog(e);
			}
		}
		return retVal;
	}

	public boolean updateByStatement(boolean newRec, boolean withAutoIncrId, BasicPostStatement contextPostStatement) {
        BuildStatementValues values = buildStatement(newRec, withAutoIncrId, contextPostStatement);
		boolean retVal = true;
		if (values.sql != null)
			if (webMode())
				retVal = m_application.m_webClient.manageCommand(values.sql, newRec && withAutoIncrId, m_autoId, false, values.postStatement, 0);
			else
				retVal = m_application.m_db.executeReturningStmnt(values.sql, m_autoId, values.postStatement);
		if (m_app.debug() && ! webMode())
			Logger.appendToHostLog("sql : " + values.sql, false, true);
		return retVal;
	}
	
	@Override
	protected boolean webOpen(boolean forOnlyMetadata, BasicPostStatement postStatement, Object manager) {
		boolean retVal = true;
		if (!m_metadataReuse) {
			WebClient wClient = m_application.m_webClient;
			wClient.setSmallBlobsList(m_smallBlobs);
			retVal = onOpened(wClient.sqlQuery(m_sql, forOnlyMetadata, false, postStatement, null));
		}
		return retVal;
	}
		
	@Override
	public int getColCount() {
		return m_colCount;
	}

	@Override
	public FieldDescriptor getFieldDescriptor(short fldIdx) {
		return m_cursor.m_fields[fldIdx];
	}

	@Override
	public boolean actionFieldsContains(String fieldName) {
		return m_actionFields.contains(fieldName);
	}

    @Override
    public BasicJotyCursor createCursor(int fieldQty) {
    	return new BasicJotyCursor(fieldQty, m_app);
    }
    
	public void checkForReconnect(SQLException e) {
		boolean reconnect = m_dbManager.dbExceptionCheck(e, DbManager.ExcCheckType.CONN_CLOSED);
		if (reconnect) {
			reconnect = Application.langYesNoQuestion("WantReconnect");
			if (!reconnect)
				m_application.m_connected = false;
		} else
			Logger.exceptionToHostLog(e);
		if (reconnect)
			try {
				throw new JotyException(JotyException.reason.SESSION_EXPIRED, null, m_app);
			} catch (JotyException e1) {}
	}

	public void close() {
		try {
			if (!m_application.m_webMode)
				m_statement.close();
			m_isOpen = false;
		} catch (SQLException e) {
			Logger.exceptionToHostLog(e);
		}
	}
	
	public Accessor getAccessor() {
		if (m_accessor == null)
			m_accessor = m_application.m_accessor;
		return m_accessor;
	}



}
