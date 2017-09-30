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

package org.joty.server;

import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import javax.xml.bind.DatatypeConverter;

import org.joty.access.Accessor;
import org.joty.access.BirtManager;
import org.joty.access.DbManager;
import org.joty.access.Instantiator;
import org.joty.access.Logger;
import org.joty.access.MethodExecutor;
import org.joty.access.PostStatement;
import org.joty.access.DbManager.DbConnectionGrabber;
import org.joty.common.*;
import org.joty.common.ConfigFile.ConfigException;
import org.joty.common.BasicPostStatement.Item;
import org.joty.common.ReportManager.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * It is the Joty Server implementation.
 * <p>
 * It accepts Http requests from the org.joty.web.WebClient object running on
 * the client side. On the first call from a client or on new Http session (due
 * to expiration of the preceding one), it forces the user to authenticate and
 * manages his authentication on the dbms.
 * <p>
 * the server looks in the requesting Url for the query string parameter
 * 'command' that can worth as follow: when the request is
 * forwarded as http GET admitted values are {config, end}, when the request is
 * a http POST possible values are {query, exec, trans, report}.
 * <p>
 * In the case of 'config' value for the Joty command another query string
 * parameter comes in to play: 'type' = {conf, confX, jotyLang, appLang}. Even
 * for other Joty commands other query string parameters play a role: this can
 * be encountered along the {@code doPost} implementation.
 * <p>
 * As the WebClient the server uses UTF8 encoding a part from binary content for
 * which a single byte encoding is used.
 * 
 */
public class JotyServer extends HttpServlet implements JotyMessenger {
	/**
	 * An inner class for getting the opportunity to have intrinsic log activity
	 * for the returned stream without worry about it. It is a simple wrapper
	 * for the {@code PrintWriter} used that adds a StringBuilder as further
	 * delegate, that will grow in its content as the PrintWriter object receives content.
	 */
	class OutPrinterWrapper {
		PrintWriter m_writer;

		OutPrinterWrapper() {
			m_writer = null;
		};

		OutPrinterWrapper(PrintWriter val) {
			m_writer = val;
		};

		void append(String text) {
			m_writer.append(text);
			if (m_debug)
				m_responseText.append(text);
		}
	}

	final String MY_USERUSERNAME = "me.userName";
	final String MY_PWD = "me.password";
	final String MY_SHK = "me.sharingKey";
	private static final long serialVersionUID = 1L;
	final String m_genIDtheme = "GenID";
	private PrintWriter m_outWriter;
	protected Connection m_conn;
	private String m_user;
	private String m_password;

	protected String m_command;
	protected Vector<PostStatement> m_postStatements;
	protected PostStatement m_queryDefPostStatement;
	protected Vector<byte[]> m_bytesElems;
	protected boolean m_success;
	protected ErrorCarrier m_errorCarrier;
	/**
	 * holds the generated values resulting by the execution of {@link #dbExecute()}
	 */
	protected Vector<String> m_returnedValues;
	private String m_sessionID;
	private byte[] m_bytes;
	protected ConfigFile m_configuration;
	protected ConfigFile m_serverConfig;
	private long m_autoIdVal;
	private String m_autoId;
	private DbManager m_dbManager;
	private String m_dbmsSessionPreset;
	private int m_intDigitDim;
	private boolean m_configurationLoaded;
	private boolean m_fieldOrdinality;
	protected String m_singleByteEncoding = Utilities.m_singleByteEncoding;
	protected boolean m_logDbActions;
	protected XmlTextEncoder m_xmlEncoder;
	protected boolean m_debug;
	protected String m_dbLogName;
	protected boolean m_use_BIRT;
	protected BirtManager m_BirtManager;
	private ReportManager m_reportManager;
	CaselessStringKeyMap<Integer> m_smallBlobs;
	private int m_statementIndex;
	StringBuilder m_responseText;
	protected Accessor m_accessor;
	protected boolean m_remoteAccessor;
	protected ParamContext m_queryDefParamContext;
	private String m_paginationPageSize;
	private String m_paginationQuery;
	protected DbConnectionGrabber m_connGrabber;
	protected MethodExecutor m_methodExecutor;
	private CaselessStringKeyMap<ConfigFile> m_JotyLangs;
	private CaselessStringKeyMap<ConfigFile> m_JotyAppLangs;
	private String m_languages;
	private Vector<String> m_langVector;
	private boolean m_shared;
	private String m_sharingKeyField;
	private String m_sharingKey;
	private LangLiteralRetCodeMapper m_langLiteralRetCodeMapper;
	protected boolean m_msSqlServer;
	private boolean m_hostLogNameSet;


	public JotyServer() {
		super();
		m_postStatements = new Vector<PostStatement>();
		m_bytesElems = new Vector<byte[]>();
		m_returnedValues = new Vector<String>();
		m_smallBlobs = new CaselessStringKeyMap<Integer>(this);
		m_sessionID = "";
		m_queryDefPostStatement = new PostStatement(this);
		Utilities.setMessanger(this);
	}

	/**
	 * This method manages the creation of a record when no Insert sql statement
	 * has been rendered by the client but only the table and the
	 * auto-incrementing id field are indicated and, furthermore, the jdbc
	 * driver is expected to support the creation of records by means of
	 * {@code moveToInsertRow} and {@code insertRow} invocations of the
	 * ResultSet object, built on the specified database table.
	 * 
	 * @param postedStmnt
	 *            the BasicPostStatement object containing needed information
	 * @return the auto generated id long value.
	 * @throws SQLException
	 * @throws NamingException
	 * 
	 * @see #dbExecute()
	 */
	private long addNewAndGetID(BasicPostStatement postedStmnt) throws SQLException, NamingException {
		Statement statement;
		long retVal = 0;
		m_connGrabber.acquireConnection();
		statement = m_conn.createStatement();
		String sql = String.format("Select %1$s.* from %1$s", postedStmnt.m_genTable);
		ResultSet result = statement.executeQuery(sql);
		ResultSetMetaData metadata = result.getMetaData();
		int colCount = metadata.getColumnCount();
		int index;
		String fieldName;
		int nSqlType;
		long lPrecision; // size
		int nScale; // decimals
		CaselessStringKeyMap<String> setFields = new CaselessStringKeyMap<String>(this);
		for (Item item : postedStmnt.m_items)
			setFields.put(item.name, item.valueLiteral);
		result.moveToInsertRow();
		String valueStr = null;
		for (int i = 0; i < colCount; i++) {
			index = i + 1;
			fieldName = metadata.getColumnLabel(index);
			valueStr = setFields.get(fieldName);
			if (valueStr != null) {
				if (valueStr.compareToIgnoreCase("null") == 0)
					result.updateNull(fieldName);
				else {
					nSqlType = metadata.getColumnType(index);
					lPrecision = metadata.getPrecision(index);
					nScale = metadata.getScale(index);
					switch (nSqlType) {
						case Types.CHAR:
						case Types.VARCHAR:
							result.updateString(fieldName, valueStr);
							break;
						case Types.FLOAT:
							result.updateFloat(fieldName, Float.parseFloat(valueStr));
							break;
						case Types.DOUBLE:
							result.updateDouble(fieldName, Double.parseDouble(valueStr));
							break;
						case Types.REAL:
						case Types.NUMERIC:
						case Types.BIGINT:
							if (nScale == 0 || nScale == -127)
								if (lPrecision > m_intDigitDim)
									result.updateLong(fieldName, Long.parseLong(valueStr));
								else
									result.updateInt(fieldName, Integer.parseInt(valueStr));
							else
								result.updateDouble(fieldName, Double.parseDouble(valueStr));
							break;
						case Types.INTEGER:
							result.updateLong(fieldName, Long.parseLong(valueStr));
							break;
						case Types.SMALLINT:
							result.updateInt(fieldName, Integer.parseInt(valueStr));
							break;
						case 11:
						case Types.DATE:
							result.updateDate(fieldName, Date.valueOf(valueStr));
							break;
						case Types.TIMESTAMP:
							result.updateDate(fieldName, Date.valueOf(valueStr));
					}
				}
			}
		}
		result.insertRow();
		result.last();
		retVal = result.getInt(m_autoId);
		statement.close();
		m_connGrabber.releaseConnection();
		return retVal;
	}

	protected boolean attributePredicate(HttpServletRequest request, String attr, boolean defTruth) {
		String valueStr = request.getParameter(attr);
		return valueStr == null ? defTruth : valueStr.equals("y");
	}

	protected void beginTrans() throws SQLException, NamingException {
		m_connGrabber.acquireConnection();
	}

	/**
	 * Envelopes the content of the {@code m_returnedValues} member in a convenient xml structure.
	 * 
	 * @return the rendered xml text.
	 *
	 * @see #dbExecute()
	 * @see #m_returnedValues
	 */
	protected String buildXmlResult() {
		StringBuilder retVal = new StringBuilder();
		retVal.append("<Result><Value>");
		retVal.append(m_success ? "Ok" : "Nok");
		retVal.append("</Value><Reason>");
		retVal.append(m_xmlEncoder.encode(m_errorCarrier.m_exceptionMsg.toString(), false));
		retVal.append("</Reason>");
		String openTag, closeTag;
		if (m_success) {
			retVal.append(String.format("<%1$s>", m_genIDtheme + "s"));
			for (int i = 0; i < m_returnedValues.size(); i++) {
				openTag = String.format("<%1$s>", m_genIDtheme);
				closeTag = String.format("</%1$s>", m_genIDtheme);
				retVal.append(openTag);
				retVal.append(String.valueOf(m_returnedValues.get(i)));
				retVal.append(closeTag);
			}
			retVal.append(String.format("</%1$s>", m_genIDtheme + "s"));
		} else
			retVal.append("<Code>" + m_errorCarrier.code + "</Code>");
		retVal.append("</Result>");
		if (!m_success)
			logUncodedFailureReport(m_errorCarrier.m_exceptionMsg.toString());
		return retVal.toString();
	}

	private boolean checkConfigFileLoading(CaselessStringKeyMap<ConfigFile> cfMap) {
		for (Iterator it = cfMap.entrySet().iterator(); it.hasNext();)
			if (((Entry<String, ConfigFile>) it.next()).getValue().m_document == null) {
				m_configurationLoaded = false;
				break;
			}
		return m_configurationLoaded;
	}

	protected boolean checkCredentials(HttpServletRequest request, String command, boolean sessionIsALive) throws IOException {
		HttpSession session = request.getSession(true);
		m_user = null;
		m_password = null;
		if (sessionIsALive) {
			m_user = (String) session.getAttribute(MY_USERUSERNAME);
			m_password = (String) session.getAttribute(MY_PWD);
			if (m_shared)
				m_sharingKey = (String) session.getAttribute(MY_SHK);
		}
		if (command != null && command.equals("login")) {
			m_user = request.getParameter("user");
			m_password = request.getParameter("pwd");
			session.setAttribute(MY_USERUSERNAME, m_user);
			session.setAttribute(MY_PWD, m_password);
			if (m_shared) {
				m_sharingKey = request.getParameter("shK");
				session.setAttribute(MY_SHK, m_sharingKey);
			}
		}
		return m_user != null && !m_user.isEmpty();
	}

	private boolean checkDbManager() {
		boolean retVal = m_dbManager != null;
		if (!retVal) {
			m_errorCarrier.m_exceptionMsg.append("DbManager object not instantiated - check the log about the server start up");
			m_success = false;
		}
		return retVal;
	}

	protected boolean checkRequestPostContent(String postedContent) {
		boolean retVal = Utilities.xsdValidate(Utilities.getXmlDocument(postedContent), 
												getServletContext().getRealPath("/JotyRequest.xsd"), 
												"request");
		if (!retVal)
			m_errorCarrier.m_exceptionMsg.append("Invalid request format");
		return retVal;
	}

	/**
	 * Checks if session is alive.
	 * <p>
	 * If session is not identified by cookies the session id is saved to be
	 * returned, later (see {@code renderXmlFooter}), to the client for future
	 * session-identifying requests.
	 * 
	 * @param request
	 *            the request object
	 * @return true if session is still alive
	 * @throws IOException
	 */
	private boolean checkSession(HttpServletRequest request) throws IOException {
		HttpSession session = request.getSession(true);
		boolean expiredOrNew = session.isNew();
		if (!request.isRequestedSessionIdFromCookie())
			m_sessionID = session.getId();
		return !expiredOrNew;
	}

	protected boolean commit() {
		boolean retVal = false;
		Exception exception = null;
		try {
			m_conn.commit();
			retVal = true;
		} catch (SecurityException e) {
			exception = e;
		} catch (IllegalStateException e) {
			exception = e;
		} catch (SQLException e) {
			m_errorCarrier.setSqlErrorCode(e);
			exception = e;
		}
		if (!retVal) {
			if (exception != null) {
				jotyMessage(exception);
				m_errorCarrier.m_exceptionMsg.append(exception.getMessage());
			}
			rollback();
		}
		return retVal;
	}

	/**
	 * Performs the execution of the statements held by the
	 * {@code m_postStatements} member either the transaction is the case or a
	 * single statement is to be executed. For each PostStatement object found
	 * in the vector, it calls
	 * {@link org.joty.access.Accessor#setPostStatement(PostStatement, boolean)} by means of
	 * which the ParamContext instance used by the Accessor object is fed with
	 * new ContextParam objects extracted by the set of Item objects.
	 * <p>
	 * If the {@code m_method} member has a value the
	 * {@code MethodExecutor#exec(BasicPostStatement, Boolean, Connection)} is
	 * invoked.
	 * <p>
	 * If the {@code m_sql} member has a value {@code executeSql} is called
	 * after the sql text could have been transformed either by a literal
	 * substitution or by the substitutions of every numbered 'gen-id' place
	 * holders with their respective Id instances previously generated upon
	 * the execution of preceding BasicPostStatement objects in the same call of this
	 * method ;
	 * <p>
	 * In all other case suitable actions take place; these cases manage old
	 * features of the previous version of Joty that have been made surviving.
	 * 
	 * @throws SQLException
	 * @throws NamingException
	 * 
	 * @see MethodExecutor
	 * @see #executeSql(String)
	 * @see PostStatement#nameSubst(Accessor, String)
	 * @see #idSqlSubst(String)
	 */
	protected void dbExecute() throws SQLException, NamingException {
		if (m_command.equals("trans"))
			beginTrans();
		String getTable;
		String sqlStmnt;
		String verifyExpr;
		PostStatement postedStmnt = null;
		HashSet<Integer> nonManagedRollbackIndexes = new HashSet<Integer>();
		m_statementIndex = 0;
		if (m_accessor != null)
			m_accessor.clearParamContext();
		for (int i = 0; i < m_postStatements.size(); i++) {
			postedStmnt = (PostStatement) m_postStatements.get(i);
			if (postedStmnt.m_nonManagedRollbackActionIden >= 0) {					
				if (m_accessor != null)
					m_accessor.setPostStatement(postedStmnt, true);
				getTable = postedStmnt.m_genTable;
				verifyExpr = postedStmnt.m_verifyExpr;
				sqlStmnt = postedStmnt.m_sql;
				m_autoId = postedStmnt.m_autoId;
				if (postedStmnt.m_method.length() > 0 && postedStmnt.m_AccessorContext.length() == 0)
					m_success = m_methodExecutor.exec(postedStmnt, m_command.equals("exec"), m_conn);
				else if (postedStmnt.m_autoId.length() > 0) {
					long id;
					if (sqlStmnt.length() > 0) {
						m_autoIdVal = 0;
						m_success = executeSql(idSqlSubst(postedStmnt.nameSubst(m_accessor, sqlStmnt)));
						id = m_autoIdVal;
					} else
						id = addNewAndGetID(postedStmnt);
					m_success &= id != 0;
					if (!m_success)
						m_errorCarrier.m_exceptionMsg.append(" - Failure on getting auto-increment ID !");
					m_returnedValues.add(String.valueOf(id));
				} else if (checkDbManager()) {
					if (getTable.length() > 0) {
						m_dbManager.setConn(m_conn);
						m_success = true;
						if (m_success) {
							long idGot = m_dbManager.getId(sqlStmnt);
							if (idGot == 0) {
								m_success = false;
								m_errorCarrier.m_exceptionMsg.append(" - Failure on getting table generated ID !");
							} else
								m_returnedValues.add(String.valueOf(idGot));
						}
					} else {
						if (verifyExpr.length() > 0) {
							m_dbManager.setConn(m_conn);
							m_success = true;
							if (m_success)
								m_success = m_dbManager.validate(verifyExpr);
						}
						if (m_success && sqlStmnt.length() > 0)
							m_success = executeSql(idSqlSubst(postedStmnt.nameSubst(m_accessor, sqlStmnt)));
					}
				}
			}
			if (m_success) {
				if (postedStmnt.m_nonManagedRollbackActionIden > 0)
					nonManagedRollbackIndexes.add(postedStmnt.m_nonManagedRollbackActionIden);
			} else
				break;
		}
		if ( ! m_success) {
			for (int i = 0; i < m_postStatements.size(); i++) {
				postedStmnt = (PostStatement) m_postStatements.get(i);
				if (postedStmnt.m_nonManagedRollbackActionIden < 0 && nonManagedRollbackIndexes.contains(-postedStmnt.m_nonManagedRollbackActionIden)) {					
					if (m_accessor != null)
						m_accessor.setPostStatement(postedStmnt, true);
					sqlStmnt = postedStmnt.m_sql;
					if (sqlStmnt.length() > 0)
						executeSql(postedStmnt.nameSubst(m_accessor, sqlStmnt));
				}
			}
			m_success = false;
		}
		if (m_command.equals("trans"))
			endTrans();
	}

	
	/**
	 * It is responsible of building the Joty response for the request of configuration
	 * data and for the request of ending the session.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String command = request.getParameter("command");
			OutPrinterWrapper outPrinterWrapper = new OutPrinterWrapper(m_outWriter);
			if (command == null)
				outPrinterWrapper.append("<html><body>Hi,<br><br>this is Joty Server (v. 2.0.2) !<br><br>www.joty.org</body></html>");
			else {
				boolean sessionWasAlive = checkSession(request);
				renderXmlHeader(outPrinterWrapper);
				if (command.equals("end")) {
					if (sessionWasAlive) {
						HttpSession session = request.getSession(true);
						String userName = (String) session.getAttribute(MY_USERUSERNAME);
						session.removeAttribute(MY_USERUSERNAME);
						session.removeAttribute(MY_PWD);
						session.invalidate();
						jotyWarning("    User " + userName + " exited (session id = " + m_sessionID + ")");
					}
					outPrinterWrapper.append("<Result><Value>Ok</Value></Result>");
				} else if (command.equals("config")) {
					if (m_configurationLoaded)
						try {
							String type = request.getParameter("type");
							String lang = request.getParameter("lang");
							if (type.compareToIgnoreCase("confX") == 0 && m_remoteAccessor)
								renderError(outPrinterWrapper, "Forbidden request !");
							else
								outPrinterWrapper.append(
										String.format("<Result><Value>Ok</Value><ConfigData>%1$s</ConfigData></Result>", 
														m_xmlEncoder.encode(
																			(type.compareToIgnoreCase("conf") == 0 ? 
																					m_configuration : 
																					type.compareToIgnoreCase("appLang") == 0 ? 
																							m_JotyAppLangs.get(lang) : 
																							m_JotyLangs.get(lang)
																				).m_fileContent, 
																			false)));
						} catch (Exception e) {
							jotyMessage(e);
							m_configurationLoaded = false;
						}
					if (!m_configurationLoaded)
						renderError(outPrinterWrapper, "At least one configuration source is missing on the server !");
				} else if (sessionWasAlive)
					renderError(outPrinterWrapper, "Illegal Joty server invocation : GET/" + command);
				else
					renderSessExpXml(outPrinterWrapper);
				renderXmlFooter(outPrinterWrapper);
			}
			endWriter(outPrinterWrapper);
		} catch (Exception e) {
			jotyMessage(e);
		}
	}
	
	/**
	 * Builds the most part of the responses since almost all the Joty web
	 * commands are dispatched through an http post command, because they
	 * require a body to be delivered. It invokes {@code getPostedBodyElements}
	 * for loading into convenient structures the data present in the request
	 * body.
	 * <p>
	 * then, again depending on the particular Joty web command, the task is
	 * dispatched to other serving methods to be accomplished.
	 * <p>
	 * If the application works in 'Accessor mode' and the application
	 * {@code Accessor} object lives on the server side this object is used
	 * either for picking the effective statements up in order to be executed or to apply
	 * literals substitution on the statements coming from the request.
	 * 
	 * @see #getPostedBodyElements
	 * @see #getXmlFromDb
	 * @see #getResponseFromLogin
	 * @see #getReportXml
	 * @see #dbExecute
	 * @see Accessor
	 * 
	 * 
	 */

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String postedContent = getPostedContent(request);
			if (m_debug)
				jotyWarning("    Posted content : \n    " + postedContent);
			m_errorCarrier.clear();
			boolean tryReturnInvalidReqFormat = false;
			if (m_debug)
				tryReturnInvalidReqFormat = !checkRequestPostContent(postedContent);
			String command = request.getParameter("command");
			OutPrinterWrapper outPrinterWrapper = new OutPrinterWrapper(m_outWriter);
			boolean credentialsAvailable = checkCredentials(request, command, checkSession(request));
			if (command == null) {
				outPrinterWrapper.append("<html><body>Hi,<br><br>this is Joty Server !<br><br>You posted: " + 
											postedContent + "</body></html>");
			} else {
				m_command = command;
				if (outPrinterWrapper.m_writer != null)
					renderXmlHeader(outPrinterWrapper);
				if (credentialsAvailable) {
					m_success = !tryReturnInvalidReqFormat;
					if (m_success)
						try {
							String mainSqlStmnt = getPostedBodyElements(postedContent);
							boolean login = m_command.equals("login");
							if (m_command.equals("query") || login) {
								m_connGrabber.acquireConnection();
								String sql = null;
								if (mainSqlStmnt == null) {
									m_success = false;
									if (m_accessor == null && !login)
										m_errorCarrier.m_exceptionMsg.append("Accessor missing !");
									else {
										if (login) {
											sql = "select 1 as joty" + (m_msSqlServer ? "" : " from dual");
											m_success = true;
										} else {
											m_accessor.setPostStatement(m_queryDefPostStatement);
											if (m_shared)
												m_accessor.setSharingKey(m_sharingKey);
											sql = m_accessor.getQueryFromPostStatement();
											if (!m_shared || !m_accessor.sharingViolation()) {
												if (m_accessor.dataDefFound() || m_accessor.substitutingLiteral())
													m_success = true;
												else
													m_errorCarrier.m_exceptionMsg.append(m_accessor.m_ddNotFoundMsg);
											}
										}
									}
								} else
									sql = mainSqlStmnt;
								if (m_success) {
									String xml = m_command.equals("query") ? 
													getXmlFromDb(sql, !attributePredicate(request, "data", true), attributePredicate(request, "bin", false)) : 
													getResponseFromLogin(m_user, m_password, sql);
									m_connGrabber.releaseConnection();
									outPrinterWrapper.append(xml);
								}
							} else if (m_command.equals("report")) {
								outPrinterWrapper.append(m_BirtManager == null ? 
										getResultFromFailure("NO_BIRT") : 
										getReportXml(request.getParameter("name"), 
													request.getParameter("type"), 
													request.getParameter("lang"), 
													attributePredicate(request, "twoProc", true)));
							} else {
								dbExecute();
								if (m_success)
									outPrinterWrapper.append(buildXmlResult());
							}
						} catch (SQLException e) {
							jotyMessage(e);
							m_errorCarrier.setSqlException(e);
							m_success = false;
						} catch (NamingException e) {
							jotyMessage(e);
							m_errorCarrier.m_exceptionMsg.append(e.getMessage());
							m_success = false;
						} catch (Exception e) {
							jotyMessage(e);
							m_errorCarrier.m_exceptionMsg.append(" Joty Server low level error !");
							m_success = false;
						}
					if (!m_success) {
						outPrinterWrapper.append(buildXmlResult());
					}
				} else
					renderSessExpXml(outPrinterWrapper);
				renderXmlFooter(outPrinterWrapper);
			}
			endWriter(outPrinterWrapper);
		} catch (Exception e) {
			jotyMessage(e);
		}
	}

	protected String encodedBytes() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			if (m_bytes != null && m_bytes.length > 0) {
				baos.write(m_bytes);
				return m_xmlEncoder.encode(baos.toString(m_singleByteEncoding), true);
			}
		} catch (IOException e) {
			jotyMessage(e);
			return null;
		}
		return null;
	}

	protected void endTrans() {
		if (m_success)
			m_success = commit();
		else
			rollback();
		m_connGrabber.releaseConnection();
	}

	private void endWriter(OutPrinterWrapper wrapper) {
		wrapper.m_writer.flush();
		wrapper.m_writer.close();
		if (m_debug)
			jotyWarning("    Response content : \n    " + m_responseText.toString());
	}

	/**
	 * Executes an sql statement either it be participating in a transaction or standing alone
	 * @param sql the statement text
	 * @return true if success
	 * 
	 * @see #executeStmnt
	 */
	protected boolean executeSql(String sql) {
		m_success = true;
		Boolean atomic = m_command.equals("exec");
		Exception exc = null;
		try {
			if (atomic)
				m_connGrabber.acquireConnection(true);
			if (m_success)
				m_success = executeStmnt(sql);
			if (atomic)
				m_connGrabber.releaseConnection();
		} catch (SQLException e) {
			jotyMessage(e);
			exc = e;
		} catch (NamingException e) {
			jotyMessage(e);
			exc = e;
		} finally {
			if (atomic)
				m_connGrabber.releaseConnection();
		}
		if (exc != null) {
			m_success = false;
			Logger.appendToHostLog("    " + exc.getMessage(), true);
			Logger.appendToLog(m_dbLogName, exc.getMessage());
			m_errorCarrier.m_exceptionMsg.append(exc.getMessage());
		}
		return m_success;
	}
	
	/**
	 * Serves {@code executeSql} in executing an sql statement.
	 * <p>
	 * As it can be noted currently the method manages only binary writing if
	 * the {@code m_bytesElems} vector is not empty. This is not a limitation
	 * because in Joty 2.0 still a writing of a large blob object is managed in
	 * a dedicated way and now, at most, is accompanied by the writing of a
	 * small blob object. That is, it is never embedded in a transaction set of
	 * statements composed by sql of various nature.
	 * <p>
	 * In the other case the method manages statement execution that either
	 * infers value generation for the field indicated by the {@code m_autoId}
	 * member or not.
	 * 
	 * 
	 * @param sql
	 *            the statement text
	 * @return true if success
	 * @throws NamingException
	 * @see org.joty.workstation.gui.ImageComponent
	 *
	 */

	protected Boolean executeStmnt(String sql) throws NamingException {
		boolean success = true;
		try {
			if (m_logDbActions)
				Logger.appendToLog(m_dbLogName, sql, false, m_user, false);
			if (m_bytesElems.size() > 0) {
				PreparedStatement stmnt = m_conn.prepareStatement(sql);
				stmnt.setBytes(1, m_bytesElems.get(m_statementIndex));
				stmnt.executeUpdate();
				stmnt.close();
				m_statementIndex++;
			} else {
				PreparedStatement stmnt = null;
				if (m_autoId.length() > 0) {
					stmnt = m_conn.prepareStatement(sql, new String[] { m_autoId });
					stmnt.executeUpdate();
					ResultSet rset = stmnt.getGeneratedKeys();
					m_autoIdVal = 0;
					if (rset.next())
						m_autoIdVal = rset.getLong(1);
				} else {
					stmnt = m_conn.prepareStatement(sql);
					stmnt.executeUpdate();
				}
				stmnt.close();
			}
		} catch (SQLException e) {
			jotyWarning("Sql : \n " + sql);
			jotyMessage(e);
			m_errorCarrier.setSqlException(e);
			success = false;
		}
		return success;
	}

	private boolean getConfBool(String literal) throws ConfigException {
		String strVal = getConfStr(literal);
		return strVal == null ? false : Boolean.parseBoolean(getConfStr(literal));
	}

	private int getConfInt(String literal) throws ConfigException {
		String strVal = getConfStr(literal);
		return strVal == null ? 0 : Integer.parseInt(getConfStr(literal));
	}

	private String getConfStr(String literal) throws ConfigException {
		return m_configuration.configTermValue(literal);
	}

	protected String getNodeValue(Node node, String name) {
		String retVal = "";
		if (node.getNodeName().compareToIgnoreCase(name) == 0)
			retVal = node.getTextContent();
		return retVal;
	}

	/**
	 * It extracts the request content and depending on the type of the command,
	 * read from the query string parameters, loads it in suitable data
	 * structures.
	 * 
	 * @param postedContent the body of the Http Post command
	 * @return the first sql statement encountered during the parsing
	 * @throws IOException
	 * 
	 * @see #m_queryDefPostStatement
	 * @see #m_postStatements
	 * @see #m_reportManager
	 * @see ReportManager#m_params
	 * @see #m_smallBlobs
	 * 
	 */
	
	private String getPostedBodyElements(String postedContent) throws IOException {
		m_postStatements.removeAllElements();
		m_bytesElems.removeAllElements();
		m_returnedValues.removeAllElements();
		Document doc = Utilities.getXmlDocument(postedContent);
		Node root = doc.getDocumentElement();
		Node firstLevelNode = root.getFirstChild();
		Node currNode = firstLevelNode;
		String firstStmnt = null;
		if (m_command.equals("trans") || m_command.equals("exec")) {
			currNode = currNode.getFirstChild();
			firstStmnt = "";
			if (currNode.getNodeName().equals("Stmnt")) {
				Node currStmnt;
				currStmnt = currNode;
				while (currStmnt != null) {
					currNode = currStmnt.getFirstChild();
					PostStatement postStatement = new PostStatement(this);
					readPostStatement(postStatement, currNode);
					if (firstStmnt.length() == 0)
						firstStmnt = postStatement.m_sql;
					m_postStatements.add(postStatement);
					currStmnt = currStmnt.getNextSibling();
				}
			}
			firstLevelNode = firstLevelNode.getNextSibling();
			while (firstLevelNode != null) {
				if (firstLevelNode.getNodeName().equals("Binary"))
					m_bytesElems.add(m_xmlEncoder.decode(firstLevelNode.getTextContent(), true).getBytes(m_singleByteEncoding));
				firstLevelNode = firstLevelNode.getNextSibling();
			}
		} else if (m_command.equals("report")) {
			if (m_BirtManager != null) {
				m_reportManager.resetParams();
				currNode = currNode.getFirstChild();
				Node currParam;
				currParam = currNode;
				int paramType;
				String literalValue = null;
				while (currParam != null) {
					currNode = currParam.getFirstChild();
					Parameter param = m_reportManager.new Parameter();
					param.name = getNodeValue(currNode, "Name");
					currNode = currNode.getNextSibling();
					literalValue = getNodeValue(currNode, "Val");
					currNode = currNode.getNextSibling();
					paramType = Integer.parseInt(getNodeValue(currNode, "Type"));
					param.type = paramType;
					param.setValue(literalValue, paramType);
					m_reportManager.m_params.add(param);
					currParam = currParam.getNextSibling();
				}
			}
		} else if (currNode.getNodeName().equals("QueryStmnt") || currNode.getNodeName().equals("QueryDef")) {
			Node queryNode = currNode;
			if (currNode.getNodeName().equals("QueryStmnt"))
				firstStmnt = currNode.getTextContent();
			else {
				currNode = currNode.getFirstChild();
				m_queryDefPostStatement.clear();
				readPostStatement((PostStatement) m_queryDefPostStatement, currNode);
			}
			currNode = queryNode.getNextSibling();
			m_smallBlobs.clear();
			if (currNode != null && currNode.getNodeName().equals("SmallBlobs")) {
				currNode = currNode.getFirstChild();
				int i = 0;
				while (currNode != null) {
					m_smallBlobs.put(currNode.getTextContent(), i);
					currNode = currNode.getNextSibling();
					i++;
				}
			}
		}
		return firstStmnt;
	}

	/**
	 * Gets the body of the Http Post command.
	 * @param request
	 * @return the content of the body 
	 */
	protected String getPostedContent(HttpServletRequest request) {
		try {
			return Utilities.stringFromInputStream(request.getInputStream(), "UTF-8", false);
		} catch (IOException e) {
			jotyMessage(e);
		}
		return null;
	}

	/**
	 * Accepts data identifying the report to be executed, use the BIRT Engine
	 * for the elaboration of it. On success it returns the 'Report' xml node
	 * populated with the binary output got from the report engine output
	 * directory; the 'Report' node is preceded by the 'Result' node positively
	 * populated.
	 * <p>
	 * On failure the output of the call to {@code getResultFromFailure} is
	 * returned instead.
	 * <p>
	 * For the meaning of the parameters here not documented see
	 * {@link BirtManager#buildReport(String, String, boolean)} method.
	 * 
	 * @param reportName
	 * @param formatType
	 * @param language
	 *            the language so that the suitable report design folder is
	 *            chosen as input context.
	 * @param twoProcesses
	 * @return the rendered xml text.
	 * 
	 * @see BirtManager
	 * 
	 */
	protected String getReportXml(String reportName, String formatType, String language, boolean twoProcesses) {
		StringBuilder retVal = new StringBuilder();
		reportManager().setUser(m_user);
		reportManager().setPassword(m_password);
		reportManager().setLanguage(language);
		reportManager().buildReport(reportName, formatType, twoProcesses);
		if (reportManager().m_exception == null) {
			retVal.append("<Result><Value>Ok</Value></Result><Report>");
			try {
				m_bytes = Utilities.getFileContent( reportManager().outputFileDir() + reportName + "." + formatType, 
													m_singleByteEncoding
												).getBytes(m_singleByteEncoding);
			} catch (FileNotFoundException e) {
				jotyMessage(e);
			} catch (UnsupportedEncodingException e) {
				jotyMessage(e);
			}
			retVal.append(encodedBytes());
			retVal.append("</Report>");
		} else
			retVal.append(getResultFromFailure(reportManager().m_exception));
		return retVal.toString();
	}

	protected String getResponseFromLogin(String user, String pwd, String query) throws SQLException {
		m_user = user;
		m_password = pwd;
		return getXmlFromDb(query, false, false);
	}

	protected String getResultFromException(Exception e) {
		jotyMessage(e);
		return getResultFromFailure(e.getMessage(), (e instanceof SQLException ? String.valueOf(((SQLException) e).getErrorCode()) : ""));
	}

	protected String getResultFromFailure(String msg) {
		return getResultFromFailure(msg, null);
	}

	protected String getResultFromFailure(String msg, String code) {
		StringBuilder retVal = new StringBuilder();
		retVal.append("<Result><Value>Nok</Value><Reason>");
		retVal.append(m_xmlEncoder.encode(msg, false));
		retVal.append("</Reason><Code>");
		retVal.append(code == null ? "" : code);
		retVal.append("</Code></Result>");
		logUncodedFailureReport(msg);
		return retVal.toString();
	}

	
	/**
	 * Basing on the Joty protocol this method builds the xml code corresponding
	 * to the result received in input and destined to be returned to the client
	 * 
	 * @param result
	 *            the ResultSet object to be encoded in Joty response.
	 * @param onlyMetadata
	 *            if true only the 'Structure' xml node is returned (the
	 *            response will contain only the description of the fields o the
	 *            result set)
	 * @param withBinaries
	 *            if true the binary content of the possible fields of type
	 *            {@code JotyTypes._smallBlobs} is included
	 * @return the rendered xml text.
	 * @throws SQLException
	 */
	protected String getResultSetAsXml(ResultSet result, boolean onlyMetadata, boolean withBinaries) throws SQLException {
		StringBuilder retVal = new StringBuilder();

		ResultSetMetaData metadata = result.getMetaData();
		int colCount = metadata.getColumnCount();
		retVal.append("<Result><Value>Ok</Value></Result>");
		retVal.append("<Structure>");
		int rsetColType;
		int size;
		int decimals;
		int[] types = new int[colCount];
		int index;
		for (int i = 0; i < colCount; i++) {
			index = i + 1;
			String name = metadata.getColumnLabel(index);
			rsetColType = metadata.getColumnType(index);
			size = metadata.getPrecision(index);
			decimals = metadata.getScale(index);
			String typeName = "";

			switch (rsetColType) {
				case Types.CHAR:
				case Types.VARCHAR:
					types[i] = JotyTypes._text;
					break;
				case Types.FLOAT:
					types[i] = JotyTypes._single;
					break;
				case Types.DOUBLE:
					types[i] = JotyTypes._double;
					break;
				case Types.REAL:
				case Types.NUMERIC:
				case Types.BIGINT:
					if (decimals == 0 || decimals == -127)
						if (size > m_intDigitDim)
							types[i] = JotyTypes._long;
						else
							types[i] = JotyTypes._int;
					else
						types[i] = JotyTypes._double;
					break;
				case Types.INTEGER:
					types[i] = JotyTypes._long;
					break;
				case Types.SMALLINT:
					types[i] = JotyTypes._int;
					break;
				case 11: // - !
				case Types.DATE:
					types[i] = JotyTypes._date;
					break;
				case Types.TIMESTAMP:
					types[i] = JotyTypes._dateTime;
					break;
				case Types.BLOB:
				case Types.VARBINARY:
				case Types.LONGVARBINARY:
					types[i] = m_smallBlobs.get(name) == null ? JotyTypes._blob : JotyTypes._smallBlob;
					break;
				default:
					if (rsetColType == -1 && size > 255 && decimals == 0)
						types[i] = JotyTypes._text;
					else
						types[i] = JotyTypes._none;
					;
			}
			if (types[i] != JotyTypes._none)
				typeName = String.valueOf(types[i]);
			retVal.append("<Field name='" + name + "' type='" + typeName + "' len='" + String.valueOf(size) + 
							"' dec='" + String.valueOf(decimals) + "'" + 
							(m_fieldOrdinality ? (" pos='" + String.valueOf(index) + "'") : "") + 
							" />");
		}
		retVal.append("</Structure>");
		if (!onlyMetadata) {
			retVal.append("<Data>");
			String[] openTags = new String[colCount];
			String[] closeTags = new String[colCount];
			String[] nullTags = new String[colCount];
			String[] fields = new String[colCount];
			for (int i = 0; i < colCount; i++) {
				index = i + 1;
				openTags[i] = "<c" + (m_fieldOrdinality ? String.valueOf(index) : "") + ">";
				closeTags[i] = "</c" + (m_fieldOrdinality ? String.valueOf(index) : "") + ">";
				nullTags[i] = "<c" + (m_fieldOrdinality ? String.valueOf(index) : "") + "/>";
				fields[i] = metadata.getColumnLabel(index);
			}
			StringBuilder retValB = new StringBuilder();
			StringBuilder valueString = new StringBuilder();
			SimpleDateFormat format = null;
			try {
				format = new SimpleDateFormat(getConfStr("xmlDateFormat"));
			} catch (ConfigException e) {
				retVal.setLength(0);
				retVal.append(getResultFromException(e));
			}
			Date dtVal;
			boolean isNull = false;
			while (result.next()) {
				retValB.append("<Record>");
				boolean nullValue;
				for (int i = 0; i < colCount; i++) {
					nullValue = false;
					valueString.setLength(0);
					if (types[i] == JotyTypes._blob || types[i] == JotyTypes._smallBlob) {
						if (withBinaries || types[i] == JotyTypes._smallBlob) {
							Blob blob = null;
							blob = result.getBlob(fields[i]);
							if (blob == null)
								nullValue = true;
							else {
								m_bytes = blob.getBytes(1, (int) (blob.length()));
								valueString.append(encodedBytes());
							}
						} else
							valueString.append("0");
					} else {
						isNull = result.getString(fields[i]) == null || result.getString(fields[i]).isEmpty();
						if (isNull)
							nullValue = true;
						else {
							if (types[i] == JotyTypes._date || types[i] == JotyTypes._dateTime) {
								if (types[i] == JotyTypes._dateTime) {
									result.getTimestamp(fields[i]);
									dtVal = new Date(result.getTimestamp(fields[i]).getTime());
								} else
									dtVal = result.getDate(fields[i]);
								valueString.append(format.format(dtVal));
							} else
								valueString.append(result.getString(fields[i]));
						}
					}
					retValB.append(nullValue ? 
										nullTags[i] : 
										(openTags[i] + 
											((types[i] == JotyTypes._text && !isNull) ? 
													m_xmlEncoder.encode(valueString.toString(), false) : 
													valueString) + 
											closeTags[i])
									);
				}
				retValB.append("</Record>");
			}
			retVal.append(retValB);
			retVal.append("</Data>");
		}
		return retVal.toString();
	}

	/**
	 * From the xml node received in input this method extracts the list of
	 * {@code Item} objects to be assigned to the {@code BasicPostStatement.m_items}
	 * member.
	 * 
	 * @param currNode
	 *            the source xml node-
	 * @param stnmt
	 *            the target PostStatement object-
	 *            
	 * @see Item
	 */
	protected void getStmntItems(Node currNode, PostStatement stnmt) {
		Node itemNode = currNode.getFirstChild();
		Node itemPart;
		String name, val, type;
		while (itemNode != null) {
			itemPart = itemNode.getFirstChild();
			if (itemPart == null)
				break;
			name = getNodeValue(itemPart, "Name");
			itemPart = itemPart.getNextSibling();
			val = getNodeValue(itemPart, "Val");
			itemPart = itemPart.getNextSibling();
			type = getNodeValue(itemPart, "Type");
			stnmt.m_items.add(stnmt.new Item(name, val, Integer.parseInt(type)));
			itemNode = itemNode.getNextSibling();
		}
	}

	/**
	 * Executes the sql statetement received in input and encodes the derived
	 * result set in xml code, basing on the Joty protocol.
	 * 
	 * @param sqlText
	 *            input sql code.
	 * @param onlyMetadata
	 *            see {@link getResultSetAsXml}
	 * @param withBinaries
	 *            "    "
	 * @return the result set encoded in xml
	 * 
	 * @see #getResultSetAsXml
	 */
	protected String getXmlFromDb(String sqlText, boolean onlyMetadata, boolean withBinaries) {
		String retVal = null;
		try {
			ResultSet result = m_conn.createStatement().executeQuery(sqlText);
			retVal = getResultSetAsXml(result, onlyMetadata, withBinaries);
		} catch (SQLException e) {
			jotyWarning("Sql : \n " + sqlText);
			retVal = getResultFromException(e);
		}
		return retVal;
	}

	/**
	 * Replaces any numbered place holder occurrence  with the corresponding element in the vector {@code m_returnedValues} 
	 * @param stmnt the sql statement 
	 * @return the resulting sql text
	 */
	protected String idSqlSubst(String stmnt) {
		if (m_returnedValues.size() == 0)
			return stmnt;
		else {
			String replacedSql = stmnt;
			for (int i = 0; i < m_returnedValues.size(); i++)
				replacedSql = replacedSql.replace(String.format("'<%1$s%2$d>'", m_genIDtheme, i + 1), m_returnedValues.get(i));
			return replacedSql;
		}
	}

	/**
	 * Executed once, it performs initialization of the JotyServlet instance:
	 * <p>
	 * creates server logs, loads configuration content from two different
	 * configuration file 'ServerSideJoty.xml' and 'JotyServe.xml', either to be
	 * used on the server side or to be dispatched to the client, loads all the
	 * language vectors in order to serve any different language request, load
	 * the proper jdbc driver and prepares it for connections.
	 * <p>
	 * If the configuration 'remoteAccessor' item is true the method
	 * instantiates the application {@code Accessor} object.
	 * <p>
	 * On the success of the previously described activities, at last, it creates the
	 * {@code MethodExecutor}, the {@code ReportManager} and the
	 * {@code BirtManager} objects (the latter only if the configuration
	 * 'use_BIRT_engine' item is true)
	 * <p>
	 * The method, also, instantiate a {@code DbConnectionGrabber} object the
	 *  methods of which are used along the whole life of the
	 * servlet .
	 * 
	 * @see ConfigFile
	 * @see MethodExecutor
	 * @see ReportManager
	 * @see BirtManager
	 * @see DbConnectionGrabber
	 */
	@Override
	public void init() throws ServletException {
		try {
			m_debug = true;
			m_xmlEncoder = new XmlTextEncoder(this) {
				@Override
				protected byte[] base64decode(String src) {
					return DatatypeConverter.parseBase64Binary(src);
				}
				@Override
				protected String base64encode(byte[] src) {
					return DatatypeConverter.printBase64Binary(src);
				}};

			m_dbLogName = "JotyDbActionLog";
			checkHostLogLocation();
			m_configuration = new ConfigFile(this, getServletContext().getRealPath("/ServerSideJoty.xml"), true);
			m_serverConfig = new ConfigFile(this,getServletContext().getRealPath("/JotyServer.xml"), true);
			m_JotyLangs = new CaselessStringKeyMap<ConfigFile>(this);
			m_JotyAppLangs = new CaselessStringKeyMap<ConfigFile>(this);
			m_intDigitDim = getConfInt("intDigitDim");
			m_fieldOrdinality = getConfBool("fieldOrdinality");
			m_remoteAccessor = getConfBool("remoteAccessor");
			m_paginationPageSize = getConfStr("pageSize");

			m_dbmsSessionPreset = m_serverConfig.configTermValue("dbmsSessionPreset");
			m_paginationQuery = m_serverConfig.configTermValue("selectorStatement");

			Utilities.m_encoding = "UTF-8";
			loadLanguages();
			m_configurationLoaded = m_configuration.m_document != null && m_serverConfig.m_document != null;
			if (m_configurationLoaded && checkConfigFileLoading(m_JotyLangs))
				checkConfigFileLoading(m_JotyAppLangs);
			if (m_configurationLoaded) {
				m_debug = getConfBool("debug");
				m_logDbActions = getConfBool("logDbActions");
				m_shared = getConfBool("shared");
				m_sharingKeyField = getConfStr("sharingKeyField");
			}
			m_errorCarrier = new ErrorCarrier();
			responseText();
			if (m_configurationLoaded) {
				try {
					m_dbManager = Instantiator.createDbManager(m_errorCarrier, m_configuration);
				} catch (ClassNotFoundException e) {
					jotyMessage(e);
				}
				m_connGrabber = new DbConnectionGrabber() {
					@Override
					public Connection acquireConnection() throws SQLException, NamingException {
						return acquireConnection(false);
					}

					@Override
					public Connection acquireConnection(boolean autoCommit) throws SQLException, NamingException {
						return acquireConnection(autoCommit, true);
					}

					@Override
					public Connection acquireConnection(boolean autoCommit, boolean setContainerReference) throws SQLException, NamingException {
						Connection retVal = null;
						if (! setContainerReference || m_conn == null) {
							InitialContext ic;
							ic = new InitialContext();
							DataSource ds = null;
							try {
								ds = (DataSource) ic.lookup("java:/comp/env/" + getConfStr("dataSourceName"));
							} catch (ConfigException e) {
								jotyMessage(e);
							}
							retVal = ds.getConnection(m_user, m_password);
							retVal.setAutoCommit(autoCommit);
							if (m_dbmsSessionPreset != null)
								retVal.createStatement().execute(m_dbmsSessionPreset);
						}
						if (setContainerReference)
							m_conn = retVal;
						return retVal;
					}

					@Override
					public void releaseConnection() {
						if (m_conn != null) {
							try {
								m_conn.close();
							} catch (SQLException e) {
								jotyMessage(e);
							}
							m_conn = null;
						}
					}
				};
				if (m_remoteAccessor)
					try {
						m_accessor = Instantiator.createAccessor(this, m_errorCarrier, m_serverConfig, m_configuration, m_connGrabber);
						if (m_accessor == null)
							jotyWarning("Accessor not specified or not found !");
						else {
							m_accessor.setPaginationQuery(m_paginationQuery, m_paginationPageSize);
							m_accessor.setLangLiteralRetCodeMapper(m_langLiteralRetCodeMapper);
							m_queryDefParamContext = new ParamContext(this);
						}
					} catch (ClassNotFoundException e) {
						jotyMessage(e);
					}
				if (!m_remoteAccessor || m_accessor != null) {
					m_methodExecutor = new MethodExecutor(m_accessor, m_errorCarrier, m_returnedValues, m_connGrabber);
					m_use_BIRT = getConfBool("use_BIRT_engine");
					m_reportManager = new ReportManager();
					m_reportManager.setXmlEncoder(m_xmlEncoder);
					if (m_use_BIRT) {
						String reportsDirectoryName = "/JotyServerReports";
						Utilities.checkDirectory(getServletContext().getRealPath(reportsDirectoryName));
						m_BirtManager = new BirtManager(m_reportManager);
						m_BirtManager.setRealPath(getServletContext().getRealPath(reportsDirectoryName) + "/");
						m_BirtManager.init(getConfStr("rptDesignsPath"), getConfStr("rptDocumentsPath"), 
											getConfStr("rptOutputsPath"), getConfStr("rptLogsPath"));
						m_BirtManager.setDbUrl(m_serverConfig.configTermValue("connection-url"));
						String jdbcClassName = getConfStr("jdbcDriverClass");
						m_msSqlServer = Utilities.isMsSqlServer(jdbcClassName);
						m_BirtManager.setJdbcDriverClass(jdbcClassName);
					}
				}
			}
		} catch (Exception e) {
			jotyMessage(e);
		}
		super.init();
	}

	private void checkHostLogLocation() {
		if ( ! m_hostLogNameSet) {
			Logger.setHostLogName("JotyServerLog", getServletContext().getRealPath("/JotyLogs"));
			m_hostLogNameSet = true;
		}
	}

	private String langPath(String lang, String fileName) {
		return getServletContext().getRealPath("/lang/" + lang + "/" + fileName);
	}

	/**
	 * For each language identifier encountered in the semicolon separated list
	 * in the configuration item 'languages' the method looks for a language
	 * folder named like the literal and containing the file jotyLang.xml and
	 * the file appLang.xml; the former is the dictionary for the Joty framework
	 * and the latter is the specific dictionary for the application. 
	 * Then load in memory the content of the files.
	 * 
	 * @throws ConfigException
	 */
	private void loadLanguages() throws ConfigException {
		m_languages = getConfStr("languages");
		m_langVector = new Vector<String>();
		Utilities.split(m_languages, m_langVector, ";");
		ConfigFile mappedConfigFile = null;
		ConfigFile configFile = null;
		int index = 0;
		for (String lang : m_langVector) {
			configFile = new ConfigFile(this, langPath(lang, "JotyLang.xml"), true, index == 0 ? lang : null, false);
			if (index == 0)
				mappedConfigFile = configFile;
			m_JotyLangs.put(lang, configFile);
			m_JotyAppLangs.put(lang, new ConfigFile(this, langPath(lang, "AppLang.xml"), true));
			index++;
		}
		if (m_langVector.size() > 0) {
			m_langLiteralRetCodeMapper = new LangLiteralRetCodeMapper(this);
			if (mappedConfigFile.m_document != null)
				m_langLiteralRetCodeMapper.load(mappedConfigFile);
		} else
			jotyWarning("Languages not specified !");
	}

	private void logUncodedFailureReport(String reason) {
		if (m_debug)
			jotyWarning("\n    Failure report forwarded to the client : \n    " + reason);
	}

	/**
	 * Makes the PostStatement object members to receive their values by extracting them from the xml node {@code currNode}.
	 * @param postStatement target PostStatement object 
	 * @param currNode source xml node
	 */
	void readPostStatement(PostStatement postStatement, Node currNode) {
		postStatement.m_sql = getNodeValue(currNode, "SqlStmnt");
		currNode = currNode.getNextSibling();
		postStatement.m_autoId = getNodeValue(currNode, "AutoId");
		currNode = currNode.getNextSibling();
		postStatement.m_genTable = getNodeValue(currNode, "GenTable");
		currNode = currNode.getNextSibling();
		postStatement.m_verifyExpr = getNodeValue(currNode, "VerifyExpr");
		currNode = currNode.getNextSibling();
		postStatement.m_method = getNodeValue(currNode, "Method");
		currNode = currNode.getNextSibling();

		postStatement.m_firstOutParamPos = getNodeValue(currNode, "FOPP");
		currNode = currNode.getNextSibling();
		postStatement.m_outParamsQty = getNodeValue(currNode, "OPQ");
		currNode = currNode.getNextSibling();

		postStatement.m_AccessorContext = getNodeValue(currNode, "AccessContext");
		currNode = currNode.getNextSibling();
		postStatement.m_dataPanelIdx = getNodeValue(currNode, "PanelIdx");
		currNode = currNode.getNextSibling();
		postStatement.m_termName = getNodeValue(currNode, "TermName");
		currNode = currNode.getNextSibling();
		postStatement.m_mainFilter = getNodeValue(currNode, "MainFilter");
		currNode = currNode.getNextSibling();
		postStatement.m_sortExpr = getNodeValue(currNode, "SortExpr");
		currNode = currNode.getNextSibling();
		postStatement.m_iteration = getNodeValue(currNode, "Iteration");
		currNode = currNode.getNextSibling();
		String attributeNTRA = getNodeValue(currNode, "NMRA");
		postStatement.m_nonManagedRollbackActionIden = (attributeNTRA == null || attributeNTRA.length() == 0) ? 0 : Integer.parseInt(attributeNTRA);
		if (postStatement.m_nonManagedRollbackActionIden != 0)
			currNode = currNode.getNextSibling();
		getStmntItems(currNode, postStatement);

	}

	private void renderError(OutPrinterWrapper wrapper, String reason) {
		logUncodedFailureReport(reason);
		wrapper.append("<Result><Value>Nok</Value><Reason>" + m_xmlEncoder.encode(reason, false) + "</Reason></Result>");
	}

	private void renderSessExpXml(OutPrinterWrapper wrapper) {
		renderError(wrapper, "SESSION_EXP");
	}

	private void renderSessionID(OutPrinterWrapper wrapper) {
		wrapper.append("<S_ID>" + m_sessionID + "</S_ID>");
	}

	private void renderXmlFooter(OutPrinterWrapper wrapper) {
		renderSessionID(wrapper);
		wrapper.append(xmlRootNode(false));
	}

	private void renderXmlHeader(OutPrinterWrapper wrapper) {
		wrapper.append("<?xml version='1.0' encoding='UTF-8' ?>" + xmlRootNode(true));
	}

	protected BirtManager reportManager() {
		return m_BirtManager;
	}

	protected boolean rollback() {
		boolean retVal = false;
		try {
			m_conn.rollback();
			retVal = true;
		} catch (SQLException e) {
			jotyMessage(e);
		}
		return retVal;
	}

	StringBuilder responseText() {
		if (m_responseText==null)
			m_responseText = new StringBuilder();
		return m_responseText;
	}
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		checkHostLogLocation();
		if (m_debug) {
			jotyWarning("HTTP" + (request.isSecure() ? "S" : "") + " - " + request.getMethod() + "\n    Querystring : \n    " + request.getQueryString());
			responseText().setLength(0);
		}
		m_outWriter = response.getWriter();
		setNoCacheHeaders(response);
		response.setCharacterEncoding("UTF-8");
		super.service(request, response);
	}

	private void setNoCacheHeaders(HttpServletResponse response) {
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "must-revalidate");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Cache-Control", "no-store");
		response.setDateHeader("Expires", 0);
	}

	protected String singleByteEncoding() {
		return m_singleByteEncoding;
	}

	protected String xmlEncodeBinaryContent(String binaryContent) throws Exception {
		if (binaryContent == null)
			return null;
		return m_xmlEncoder.encode(binaryContent, true);
	}

	private String xmlRootNode(boolean opening) {
		return "<" + (opening ? "" : "/") + "JotyResp" + 
						(m_debug && opening ? 
								" xmlns='http://www.joty.org' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + 
									" xsi:schemaLocation='http://www.joty.org JotyResponse.xsd' " : 
								"") +
				">";
	}

	@Override
	public void jotyWarning(String text) {
		Logger.appendToHostLog(text);		
	}

	@Override
	public void jotyMessage(String text) {
		jotyWarning(text);
	}
	
   @Override
    public void jotyMessage(Throwable t) {
        Logger.throwableToHostLog(t);
    }

	@Override
	public void jotyMessage(Exception e) {
		Logger.exceptionToHostLog(e);	
	}

	@Override
	public void ASSERT(boolean predicate) {
		if (m_debug && !predicate)
			Logger.stackTraceToHostLog("ASSERTION VIOLATED");
	}
	
    @Override
    public boolean isDesignTime() {
        return false;
    }

	@Override
	public void afterReportRender(String arg0) {
	}

	@Override
	public void beforeReportRender() {
	}

}
