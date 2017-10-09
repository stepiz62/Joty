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

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Vector;

import org.joty.access.DbManager.DbConnectionGrabber;
import org.joty.common.BasicPostStatement;
import org.joty.common.ErrorCarrier;
import org.joty.common.BasicPostStatement.Item;

/**
 * It is instantiated by the {@code Application} object or by the
 * {@code JotyServlet} object to work with the application {@code Accessor}
 * object.
 * <p>
 * Essentially made by its {@code exec} method, this class uses the Java
 * Reflection to invoke a method defined in the Accessor implementation of the
 * application and specified in the {@code BasicPostStatement} object passed as
 * parameter. It uses all the information of the BasicPostStatement instance relative
 * to the parameters of the stored method, to prepare the call to
 * java.lang.reflect.Method.invoke method and to compose, after the call, the
 * content of the {@code m_returnedValues} member that has been mapped by the
 * constructor on the Vector of the calling environment.
 * <p>
 * The actual returned value of the store method is added as last value in the
 * vector.
 * 
 * @see BasicPostStatement
 * @see Accessor
 * @see org.joty.workstation.app.Application#accessorMethodPostStatement
 * 
 */
public class MethodExecutor {
	Accessor m_accessor;
	ErrorCarrier m_errorCarrier;
	Vector<String> m_returnedValues;
	protected DbConnectionGrabber m_dbConnGrabber;

	public MethodExecutor(Accessor accessor, ErrorCarrier errorCarrier, Vector<String> returnedValues, DbConnectionGrabber dbConnGrabber) {
		m_accessor = accessor;
		m_errorCarrier = errorCarrier;
		m_returnedValues = returnedValues;
		m_dbConnGrabber = dbConnGrabber;
	}

	public boolean exec(BasicPostStatement postedStmnt, Boolean atomic, Connection dbConn) {
		Class[] itemsTypes;
		Object[] itemsValues = null;
		boolean success = false;
		m_accessor.setConn(dbConn);
		Method method = null;
		Throwable throwable = null;	
		Vector<Item> outParams = null;
		int outParamsQty = postedStmnt.m_outParamsQty.length() == 0 ? 0 : Integer.parseInt(postedStmnt.m_outParamsQty);
		boolean methodRetValueExpected = outParamsQty < 0;
		outParamsQty = outParamsQty < 0 ? -(outParamsQty + 1) : outParamsQty;
		try {
			int itemsSize = postedStmnt.m_items.size();
			int reflectionParamQty = itemsSize + (outParamsQty > 0 ? 1 : 0);
			itemsTypes = new Class[reflectionParamQty];
			itemsValues = new Object[reflectionParamQty];
			int index = 0;
			for (Item item : postedStmnt.m_items) {
				itemsTypes[index] = item.type();
				itemsValues[index] = item.value();
				index++;
			}
			if (postedStmnt.m_firstOutParamPos.length() > 0) {
				outParams = new Vector<Item>();
				for (int i = 0; i < outParamsQty; i++)
					outParams.add(postedStmnt.new Item());
				if (outParamsQty > 0) {
					itemsTypes[index] = outParams.getClass();
					itemsValues[index] = outParams;
				}
			}
			method = m_accessor.getClass().getMethod(postedStmnt.m_method, itemsTypes);
			success = true;
		} catch (Throwable t) {
			throwable = t;
		}
		if (success) {
			success = false;
			try {
				if (atomic && m_dbConnGrabber != null)
					dbConn = m_dbConnGrabber.acquireConnection(true);
				m_accessor.setConn(dbConn);
				Object methodRetVal = method.invoke(m_accessor, itemsValues);
				if (atomic && m_dbConnGrabber != null)
					m_dbConnGrabber.releaseConnection();
				success = true;
				if (postedStmnt.m_firstOutParamPos.length() > 0) {
					int firstReturnedValuePosExpected = Integer.parseInt(postedStmnt.m_firstOutParamPos);
					int currentPositionForAddingRetVal = m_returnedValues.size() + 1;
					String preambleMsg = " - Method " + postedStmnt.m_method + " :";
					if (firstReturnedValuePosExpected != m_returnedValues.size() + 1) {
						m_errorCarrier.m_exceptionMsg.append(preambleMsg + " current first position for returning values = " + 
											currentPositionForAddingRetVal + " instead of pos.: " + firstReturnedValuePosExpected + " as expected ! ");
						success = false;
					}
					if (methodRetValueExpected && methodRetVal == null || methodRetVal != null && !methodRetValueExpected) {
						m_errorCarrier.m_exceptionMsg.append(preambleMsg + " method return value " + (methodRetValueExpected ? "" : " not ") + " expected !");
						success = false;
					}
					for (Item item : outParams)
						m_returnedValues.add(item.valueLiteral);
					if (methodRetValueExpected)
						m_returnedValues.add(String.valueOf(methodRetVal));
				}
			} catch (Throwable t) {
				throwable = t;
			} finally {
				if (atomic && m_dbConnGrabber != null)
					m_dbConnGrabber.releaseConnection();
			}
		}
		if (throwable != null) {
			Logger.throwableToHostLog(throwable);
			m_errorCarrier.m_exceptionMsg.append(m_accessor.getClass().getName() + " - " + postedStmnt.m_method + " - " + 
									throwable.toString() + (throwable.getCause() == null ? "" : ("\n\n" + throwable.getCause().toString())));
		}
		return success;
	}

}
