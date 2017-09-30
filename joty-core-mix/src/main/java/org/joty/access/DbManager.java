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
import java.sql.SQLException;

import javax.naming.NamingException;

import org.joty.common.AbstractDbManager;

/**
 * A once again abstract implementation for specific use either in AWT/Swing Client or in the Joty Server environment.
 *  
 */

public abstract class DbManager extends AbstractDbManager {

	public interface DbConnectionGrabber {
		public Connection acquireConnection() throws SQLException, NamingException;
		public Connection acquireConnection(boolean autoCommit) throws SQLException, NamingException;
		public Connection acquireConnection(boolean autoCommit, boolean setContainerReference) throws SQLException, NamingException;
		public void releaseConnection();
	}

	public Connection m_conn;
	public void setConn(Connection conn) {
		m_conn = conn;
	}
}
