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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gives a chance to the developer to decode the content of the
 * java.sql.SQLException object, building a sort of mapping to a semantically
 * defined set of identifiers.
 * <p>
 * Offers an interface to generate instance capable of holding attitudes to
 * manage a database connection.
 * <p>
 *
 */

public abstract class AbstractDbManager {

	public enum ExcCheckType {
		INVALID_CREDENTIALS, CONN_CLOSED, CONSTR_VIOLATION_ON_UPDATE, CONSTR_VIOLATION_ON_DELETE, DBMS_CONN_FAILURE, DBMS_CREATEUSER_FAILURE
	}

	public ErrorCarrier m_errorCarrier;
	public ConfigFile m_configuration;
	
	/**
	 * {@code m_derivedCode} must receive a value in the descendant of this
	 * class. This member is thought to have some role at application semantic
	 * level so that all the framework messenger methods can be customized
	 * (overridden) in their behavior depending on the value of it. It is a
	 * vehicle from the context in which an exception is analyzed to the context
	 * in which the corresponding message to the user is built and chosen,
	 * nothing else.
	 */
 	protected int m_derivedCode;

	public int derivedCode() {
		return m_derivedCode;
	}
	
	public boolean dbExceptionCheck(SQLException e, ExcCheckType exceptionCheckType) {
		return dbExceptionCheck(e.getMessage(), String.valueOf(e.getErrorCode()), exceptionCheckType);
	}
	/**
	 * Must return true if the exception of the assigned type is detected.  
	 * @param textToInspect the text target of the analysis dedicated to catch some significant pattern to make the decision
	 * @param code the error code
	 * @param exceptionCheckType the type of exception which the check is made for
	 */
	abstract public boolean dbExceptionCheck(String textToInspect, String code, ExcCheckType exceptionCheckType);

	public long getId(String sql) {
		return 0;
	}

	public String getIdGeneratorSql(String sequenceName) {
		return null;
	}

	public boolean validate(String expr) {
		return true;
	}
}
