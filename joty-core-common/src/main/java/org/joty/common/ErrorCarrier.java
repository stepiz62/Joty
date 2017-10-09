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

import java.sql.SQLException;

/**
 * Simply a wrapper for error notification content.
 * 
 */
public class ErrorCarrier {
	public StringBuilder m_exceptionMsg;
	public String code;

	public ErrorCarrier() {
		m_exceptionMsg = new StringBuilder();
	}

	public void clear() {
		m_exceptionMsg.setLength(0);
		code = "";
	}

	public void setSqlErrorCode(SQLException e) {
		code = String.valueOf(e.getErrorCode());
	}

	public void setSqlException(SQLException e) {
		m_exceptionMsg.append(e.getMessage());
		setSqlErrorCode(e);
	}

}
