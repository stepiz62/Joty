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

import java.sql.Connection;
import java.util.Vector;

import org.joty.access.DbManager.DbConnectionGrabber;
import org.joty.common.CaselessStringKeyMap;
import org.joty.common.ConfigFile;
import org.joty.common.ErrorCarrier;
import org.joty.common.JotyMessenger;
import org.joty.common.LangLiteralRetCodeMapper;
import org.joty.common.ParamContext;
import org.joty.common.SearchQueryBuilderBack;
import org.joty.common.Utilities;
import org.joty.common.ConfigFile.ConfigException;

/**
 * This class has been thought to have the responsibility to access in a general
 * sense, from this has been chosen its name.
 * <p>
 * Primarily it has been design to support a robust encapsulating laboratory
 * that can host constructs and more in general methods that make access
 * somewhere, hiding the name space used in their implementation, and, what is
 * the most important thing, is capable to live either in the client side of the
 * Joty technology (that is instantiated by the {@code Application} object) or
 * on the server side (that is instantiated by the {@code JotyServlet} object).
 * <p>
 * The purpose of hiding the operative name space is directed towards the
 * database, however, as it is, an Accessor instance can be used to have it
 * interacting with any system is 'visible' in the context of life of the
 * accessor itself.
 * <p>
 * Then, in the class, there is support for writing sql statements selecting and
 * disposing data, support for invoking database stored procedures, support for
 * pure substitution maps that may serve for replacements of place-holders
 * located in the sql statements formed in the client side.
 * <p>
 * All the features implemented in a Accessor descendant class using the
 * built-in defining constructs can be invoked by name or by the context of the
 * client visual object.
 * <p>
 * An accessor instance, as it is, can hosts even pure computation that is
 * demanded far away from the client machine, implementing, this way, simple
 * client-server paradigm.
 * <p>
 * The design of this class is strictly connected to those ones of the
 * {@code ParamContext} class and {@code PostStatement} class. All together they
 * support the transferability of context parameters (or more specifically
 * dialog context parameters) from the location where they take value to the
 * location where their values are used, location, the latter, that may be the
 * same as the former (in the client side) or in the server side.
 * <p>
 * All that is defined in an Accessor object, is stored in maps accessible by
 * name. A part from the map used for literal substitution all is stored in maps
 * the values of which are {@code  DataDef} objects.
 * <p>
 * The {@code DataDef} class comes with several methods and data structures
 * suited to the definition of the statements/methods/computations that it can
 * hold inside, and at the same time provides methods to retrieve those
 * implementation objects.
 *
 
 * @see ParamContext
 * @see PostStatement
 * @see org.joty.workstation.gui.JotyDialog
 * @see org.joty.server.JotyServer
 * 
 */
public class Accessor {
	/**
	 * The core inner class for the definition and the retrieving of an sql
	 * statement capable to support context parameters, the value of which is
	 * resolved either at the store time of the statement definition or at the
	 * statement execution time.
	 * 
	 * @see ParamHolder
	 *
	 */
	public class DataDef {
		/** 
		 * An instance of this class holds the value or the way to get the value of a context parameter. 
		 */
		public class ParamHolder {
			String m_contextParamName;
			ParamActuator m_paramActuator;
			String m_value;

			public ParamHolder(String contextParamName, ParamActuator paramActuator) {
				this(contextParamName, paramActuator, null);
			}

			/**
			 * @param contextParamName the name of the parameter
			 * @param paramActuator an implementation of the {@code ParamActuator} interface
			 * @param value the string value for the context parameter
			 * 
			 * @see ParamActuator
			 */
			public ParamHolder(String contextParamName, ParamActuator paramActuator, String value) {
				m_contextParamName = contextParamName;
				m_paramActuator = paramActuator;
				m_value = value;
			}
		}

		private String m_updatableSet;
		private String m_sql;
		ExprHolder m_updatableSetHolder;
		ExprHolder m_statementHolder;
		public Vector<ParamHolder> m_paramHolders;
		private String m_sharingAlias;
		private boolean m_noSharingClause;

		public DataDef() {
			m_paramHolders = new Vector<ParamHolder>();
		}

		/**
		 * Allows to add an addressable by name param context holder.
		 * <p>
		 * the order by which it is called must respect the position of the
		 * corresponding place holder in the target sqlExpression. To identify
		 * the call order participate all the following methods:
		 * {@link #addContextParamName}, {@link #addParamActuator}, 
		 * {@link #addParamValue}, that is, any call of this methods makes the
		 * order to increment: the next call will define a param holder
		 * associated to the next place holder in the sql statement.
		 * 
		 */
		public void addContextParamName(String name) {
			m_paramHolders.add(new ParamHolder(name, null));
		}

		/**
		 * Adds a param context holder object and provides its value actuator
		 * implementation to be stored.
		 * <p>
		 * see {@link #addContextParamName(String)} for a rule for calling this method
		 */
		public void addParamActuator(ParamActuator paramActuator) {
			m_paramHolders.add(new ParamHolder(null, paramActuator));
		}

		/**
		 * Adds a param context holder object and provides its value to be
		 * stored: it requires the observance of the position correspondence in
		 * the sqlExpression
		 * <p>
		 * see {@link #addContextParamName(String)} for a rule for calling this method
		 */
		public void addParamValue(String value) {
			m_paramHolders.add(new ParamHolder(null, null, value));
		}

		/** Provides the basic select sql statement of the database table identified by name by the {@code m_updatableSet} member. */
		protected String defaultSql() {
			return selectStmnt(m_updatableSet);
		}

		public String getSearchSql(String whereClause, String orderByClause, int iteration, String mode, String sharingExpr) {
			return m_searchQueryBuilder.getQuery(getStatement(mode) == null ? 
													getUpdatableSet(mode) : 
													getStatement(mode),
												whereClause, orderByClause, iteration, sharingExpr);
		}

		public String getStatement(String mode) {
			if (m_statementHolder == null) {
				String[] valueArray = new String[m_paramHolders.size()];
				int i = 0;
				for (ParamHolder paramHolder : m_paramHolders) {
					valueArray[i] = paramHolder.m_paramActuator != null ? 
										paramHolder.m_paramActuator.getValue() : 
										paramHolder.m_contextParamName != null ? 
												m_paramContext.contextParameter(paramHolder.m_contextParamName) : 
												paramHolder.m_value;
					i++;
				}
				return m_sql == null ? null : String.format(m_sql, valueArray);
			} else
				return m_statementHolder.getSql(mode);
		}

		public String getUpdatableSet(String mode) {
			return m_updatableSetHolder == null ? m_updatableSet : m_updatableSetHolder.getSql(mode);
		}

		public boolean noSharingClause() {
			return m_noSharingClause;
		}

		protected String selectStmnt(String tabName) {
			return "Select " + tabName + ".*" + " from " + tabName;
		}

		protected void setNoSharingClause() {
			m_noSharingClause = true;
		}

		public void setSharingAlias(String tableAlias) {
			m_sharingAlias = tableAlias;
		}

		protected void setSqlToDefault() {
			setStatementSql(defaultSql());
		}

		public void setStatementHolder(ExprHolder exprHolder) {
			m_statementHolder = exprHolder;
		}

		public void setStatementSql(String sql) {
			m_sql = sql;
		}

		public void setUpdatableSet(String text) {
			m_updatableSet = text;
		}

		public void setUpdatableSetHolder(ExprHolder exprHolder) {
			m_updatableSetHolder = exprHolder;
		}
	}

	public class DialogDataDef {

		public Object m_openMode;
		public Vector<PanelDataDef> m_panelDataDefs;

		public DialogDataDef() {
			m_panelDataDefs = new Vector<PanelDataDef>();
		}
	}

	public class ExprHolder {
		public String getExpr() {
			return getSelectiveExpr(null);
		}

		public String getSelectiveExpr(String mode) {
			return null;
		}

		final public String getSql(String mode) {
			try {
				return mode == null || mode.length() == 0 ? getExpr() : getSelectiveExpr(mode);
			} catch (Throwable th) {
				Logger.throwableToHostLog(th);
			}
			return null;
		}
	}

	public class PanelDataDef extends DataDef {
		public CaselessStringKeyMap<DataDef> m_termDataDefs;
		public CaselessStringKeyMap<DataDef> m_statementDefs;

		public PanelDataDef() {
			m_termDataDefs = new CaselessStringKeyMap<DataDef>(m_jotyMessanger);
			m_statementDefs = new CaselessStringKeyMap<DataDef>(m_jotyMessanger);
		}

		public DataDef getDataDef(String termName) {
			return m_termDataDefs.get(termName);
		}

	}

	/**
	 * Its implementation must provide a 'way' to get a value for the context
	 * parameter which is related to.
	 */
	public interface ParamActuator {
		String getValue();
	}

	public ErrorCarrier m_errorCarrier;
	public CaselessStringKeyMap<DialogDataDef> m_dialogDataDefs;
	public CaselessStringKeyMap<DataDef> m_statementDefs;
	public CaselessStringKeyMap<String> m_literalSubsts;
	protected ParamContext m_paramContext;
	protected SearchQueryBuilderBack m_searchQueryBuilder;
	private PostStatement m_postStatement;
	private boolean m_dataDefFound;
	public String m_ddNotFoundMsg = "DataDef not found in the accessor !";
	protected Connection m_conn;
	private boolean m_substitutingLiteral;
	public boolean m_shared;
	public String m_sharingKeyField;
	protected DbConnectionGrabber m_connGrabber;
	JotyMessenger m_jotyMessanger;

	private String m_sharingKey;

	private boolean m_sharingViolation;

	protected LangLiteralRetCodeMapper m_langLiteralRetCodeMapper;

	public void init(JotyMessenger jotyMessanger) {
		m_jotyMessanger = jotyMessanger;
		m_dialogDataDefs = new CaselessStringKeyMap<DialogDataDef>(m_jotyMessanger);
		m_statementDefs = new CaselessStringKeyMap<DataDef>(m_jotyMessanger);
		m_literalSubsts = new CaselessStringKeyMap<String>(m_jotyMessanger);
		m_paramContext = new ParamContext(m_jotyMessanger);
	}

	public void clearParamContext() {
		m_paramContext.clear();
	}

	public boolean dataDefFound() {
		return m_dataDefFound;
	}

	public DataDef getDataDef() {
		PanelDataDef panelDataDef = getPanelDataDef(m_postStatement);
		String termName = m_postStatement.m_termName;
		DataDef retVal = termName == null || termName.length() == 0 ? 
							(Utilities.isLiteral(m_postStatement.m_sql) ? 
									panelDataDef.m_statementDefs.get(m_postStatement.m_sql) : 
									panelDataDef) : 
							(panelDataDef == null ? m_statementDefs.get(termName) : panelDataDef.getDataDef(termName));
		m_dataDefFound = retVal != null;
		return retVal;
	}

	public PanelDataDef getPanelDataDef(PostStatement postStatement) {
		PanelDataDef retVal = null;
		if (postStatement.m_AccessorContext != null && postStatement.m_AccessorContext.length() > 0)
			retVal = getPanelDataDef(postStatement.m_AccessorContext, Integer.parseInt(postStatement.m_dataPanelIdx));
		return retVal;
	}

	public PanelDataDef getPanelDataDef(String m_dialogName, int m_panelIdxInDialog) {
		PanelDataDef retVal = null;
		DialogDataDef dialogDataDef = m_dialogDataDefs.get(m_dialogName);
		if (dialogDataDef != null && dialogDataDef.m_panelDataDefs.size() > m_panelIdxInDialog)
			retVal = dialogDataDef.m_panelDataDefs.get(m_panelIdxInDialog);
		return retVal;
	}

	public ParamContext getParamContext() {
		return m_paramContext;
	}

	public String getQueryFromPostStatement() {
		DataDef dataDef = getDataDef();
		boolean literalSubtitutionCandidate = Utilities.isMoreThanOneWord(m_postStatement.m_sql) && m_postStatement.m_sql.indexOf("<JOTY_CTX>") >= 0;
		m_substitutingLiteral = (m_postStatement.m_AccessorContext == null || m_postStatement.m_AccessorContext.length() == 0) && 
								literalSubtitutionCandidate && 
								m_postStatement.m_termName != null && 
								m_postStatement.m_termName.length() > 0 && 
								m_literalSubsts.get(m_postStatement.m_termName) != null;
		if (literalSubtitutionCandidate)
			return m_postStatement.nameSubst(this, m_postStatement.m_sql);
		else if (dataDef == null) {
			Logger.appendToHostLog(m_ddNotFoundMsg);
			return null;
		} else
			return m_postStatement.getQueryFromDataDef(dataDef, this);
	}

	public String getSharingAlias(DataDef dataDef) {
		return dataDef.m_sharingAlias;
	}

	public String getUpdatableSetFromDataDef() {
		DataDef dataDef = getDataDef();
		if (dataDef == null)
			Logger.appendToHostLog(m_ddNotFoundMsg);
		return dataDef == null ? null : dataDef.m_updatableSet;
	}

	public void init() {
		m_searchQueryBuilder = new SearchQueryBuilderBack();
		namer();
		customInit();
		loadDefs();
	}
	
	public void setDbConnectionGrabber (DbConnectionGrabber connGrabber) {
		m_connGrabber = connGrabber;
	}
	
	protected void namer () { }
	
	protected void customInit() { }
	
	protected void loadDefs() {
		/* DIALOG TEMPLATE	-------------------------------------------------------------------------------------
		m_dialogDataDefs.put("<full dialog class name>", new DialogDataDef() {
			{			
				m_panelDataDefs.add(new PanelDataDef() {   // respect the panel position in the adding sequence
					{
						// dataDef section BEGIN =========================================
						// A
						setUpdatableSet("< panel table name >");
						// - OR - B
						setUpdatableSetHolder(new ExprHolder() {
							public String getExpr() {	return null; }
							public String getSelectiveExpr(String mode) {	return null; }
						});
						// A2
						setStatementHolder(new ExprHolder() {
							public String getExpr() {	return null; }
							public String getSelectiveExpr(String mode) {	return null; }
						});
						// - OR - B2 (only if dialog is opened with no modes)
						setStatementSql("< panel Sql text >");
						addContextParamName("< a ContextParam name >");
						addParamActuator(new ParamActuator() {
							public String getValue() {
								return "";
							}
						});		
						addParamValue("< a param value >");		
						// dataDef section END ====================================================
						
						m_termDataDefs.put("< term name >", new DataDef() {  // for each term that requires ....
							{
								// dataDef section (see above)
							}
						});
					}
				});
			}
		});
		---------------------------------------------------------------------------------------------------------*/		
	}

	public boolean isMissingContextParam(String name) {
		return m_paramContext.isMissingParam(name);
	}
	
	public String paramValue(String name) {
		return m_paramContext.contextParameter(name);
	}

	public void setConn(Connection conn) {
		m_conn = conn;
	}

	public void setFromConfiguration(ConfigFile configFile) {}

	public void setLangLiteralRetCodeMapper(LangLiteralRetCodeMapper mapper) {
		m_langLiteralRetCodeMapper = mapper;
	}

	public void setPaginationQuery(String query, String pageSize) {
		m_searchQueryBuilder.setPaginationQuery(query, pageSize);
	}

	public void setPostStatement(PostStatement postStatement) {
		setPostStatement(postStatement, false);
	}

	public void setPostStatement(PostStatement postStatement, boolean inTransaction) {
		m_postStatement = postStatement;
		if (m_postStatement != null)
			m_postStatement.loadParamContext(m_paramContext, !inTransaction);
	}

	public void setSharingData(ConfigFile configFile) throws ConfigException {
		m_shared = Boolean.parseBoolean(configFile.configTermValue("shared"));
		m_sharingKeyField = configFile.configTermValue("sharingKeyField");
	}

	public void setSharingKey(String sharingKey) {
		m_sharingKey = sharingKey;
	}

	public String sharingClause() {
		String filterValue = null;
		m_sharingViolation = false;
		if (m_sharingKey != null && m_sharingKey.compareTo(m_paramContext.contextParameter("sharingKey")) != 0) {
			filterValue = "XXXXXXXXXXXX";
			m_errorCarrier.m_exceptionMsg.append("Global ID violation !");
			m_sharingViolation = true;
		} else
			filterValue = m_paramContext.contextParameter("sharingKey");
		return m_sharingKeyField + " = '" + filterValue + "'";
	}

	public boolean sharingViolation() {
		return m_sharingViolation;
	}

	public boolean substitutingLiteral() {
		return m_substitutingLiteral;
	}

}
