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

import org.joty.common.ParamContext.ContextParam;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

/**
 * It is the 'box' for the delivery of a generalized statement through the Joty
 * channel.
 * <p>
 * The class holds a set of attributes that let Client and the server to
 * communicate; it supports the generic sql statement to be stored either for
 * data modification or for retrieving a record set from the database.
 * <p>
 * In the {@code m_items} member it contains a java.util.Vector where the
 * elements (of type {@code Item}) transfer multi purpose parameters.
 * <p>
 * By having the exclusive role, in the framework, to work as container of the
 * single generalized statement, its implementation is such that the instances
 * of it live on both sides of the Joty technology: on the client, to allow the
 * composition the box for delivering the statement on the channel, and on the
 * server, to compose the received content in a form known to the framework.
 * Even if this happens either the Accessor mode is active or not, most of its
 * implementation is relative to the use with the {@code Accessor} object, that
 * is in Accessor mode.
 * <p>
 * The most frequent use of this class when the Accessor mode is on, is the
 * packaging of the information needed to address the statement within the
 * Accessor object plus the parameters context used by the Accessor object to
 * actualize the parameters of the statement addressed (see
 * {@code addItemsFromParamContext} method). Another task of this class is to
 * carry the invocation of a method stored in the Accessor object, this method
 * definition can define an invocation of a dbms stored-procedure or something
 * different about whatever computation, accessing something or not. In this
 * task comes in to the scene another dual class: the {@code org.joty.common.MethodExecutor}
 * class; the instance of this class follows the Accessor object: the values
 * returned from the method call are collected, as final step, by this class
 * that makes them available to possible consumers (see
 * {@code Application.invokeAccessMethod}).
 * <p>
 *
 * Its methods load the content of the ParamContext and store it in an empty
 * instance. They help in retrieving statements from the Accessor object or
 * making literal substitutions when the contribution of the Accessor object is
 * the obfuscation of the database name space.
 * 
 * @see ParamContext
 *
 */
public class BasicPostStatement {
	/**
	 * This inner class contains in a textual form a value compatible with the
	 * datum wrapped by the {@code WrappedField} object or the value of a {@code ContextParam} object.
	 */
	public class Item {

		public static final int _text = 0;
		public static final int _long = 1;
		public static final int _double = 2;
		public static final int _date = 3;
		/**
		 * Helps {@code org.joty.common.MethodExecutor#exec(org.joty.common.BasicPostStatement, Boolean, java.sql.Connection)} method in addressing reflection.
		 * @return the Class object corresponding to type hosted by the Item instance.
		 */
		public Class type() {
			switch (type) {
				case _text:
					return String.class;
				case _long:
					return Long.class;
				case _double:
					return Double.class;
				case _date:
					return java.sql.Date.class;
			}
			return null;
		}

		public int type;
		public String name;
		public String valueLiteral;
		public boolean m_buildFromDirtyPrm;

		public Item() {
			this(null, null);
		}

		public Item(String name, String valueLiteral) {
			this(name, valueLiteral, Item._text);
		}

		public Item(String name, String valueLiteral, int type) {
			this.name = name;
			this.valueLiteral = valueLiteral;
			this.type = type;
		}

		public String unquoted() {
			switch (type) {
				case _date:
				case _text:
					return Utilities.unquote(valueLiteral.replace("''", "'"));
				default:
					return valueLiteral;
			}
		}

		public Object value() {
			if (type != _text && (valueLiteral == null || valueLiteral.length() == 0))
				return null;
			switch (type) {
				case _text:
					return valueLiteral;
				case _long:
					return Long.parseLong(valueLiteral);
				case _double:
					return Double.parseDouble(valueLiteral);
				case _date:
					return valueLiteral == null ? null : java.sql.Date.valueOf(valueLiteral.replace("'", ""));
			}
			return null;
		}

	}

	public class ReturnedValueItem extends Item {
		public int m_returnedValPosition;
	}

	public String m_sql;
	/** the name of the autoincrement id field */
	public String m_autoId;
	public String m_genTable;
	public String m_verifyExpr;
	public int m_nonManagedRollbackActionIden;
	/**
	 * the name of the method stored in the {@code Accessor} object or the mode
	 * of opening of a {@code DataAccessDialog} object having its statements context
	 * stored in the Accessor object
	 */
	public String m_method;
	public String m_firstOutParamPos;
	public String m_outParamsQty;
	/** the absolute class name of the {@code DataAccessDialog} that is used for discovering the corresponding Accessor context */
	public String m_AccessorContext;
	/**
	 * the zero based position of the {@code DataAccessPanel} within the
	 * {@code TabbedPane} object: a second further coordinate added to the contribution of the {@code m_AccessorContext} member.
	 *
	 */
	public String m_dataPanelIdx;
	/**
	 * the name of the {@code Term} object within the {@code DataAccessPanel}
	 * object layout: the third coordinate added to the contribution of the
	 * {@code m_dataPanelIdx} member
	 */
	public String m_termName;
	public String m_mainFilter;
	public String m_sortExpr;
	public String m_iteration;

	/** A vector of a general purpose parameters */
	public Vector<Item> m_items;

	public Vector<ReturnedValueItem> m_returnedValues;
	public CaselessStringKeyMap<ReturnedValueItem> m_returnedValuesMap;
	public String m_retVal;
	public ParamContext m_paramContext;
	JotyMessenger m_jotyMessanger;


	public BasicPostStatement(JotyMessenger jotyMessanger) {
		m_jotyMessanger = jotyMessanger;
		m_items = new Vector<Item>();
		clear();
	};

	public void addItem(String item, String valueLiteral) {
		m_items.add(new Item(item, valueLiteral));
	}

	public void addItem(String item, String valueLiteral, int type) {
		m_items.add(new Item(item, valueLiteral, type));
	}

	public void addItemsFromParamContext(ParamContext paramContext) {
		m_paramContext = paramContext;
		if (paramContext.m_paramsSet != null)
			for (Iterator it = paramContext.m_paramsSet.entrySet().iterator(); it.hasNext();) {
				Entry<String, ContextParam> entry = ((Entry<String, ContextParam>) it.next());
				addItem(entry.getKey(), paramContext.contextParameter(entry.getKey()));
				m_items.get(m_items.size() - 1).m_buildFromDirtyPrm = entry.getValue().m_dirty;
			}
	}

	public void addOutParam(String name, int type, int returnPosition) {
		ReturnedValueItem item = new ReturnedValueItem();
		item.type = type;
		item.m_returnedValPosition = returnPosition;
		m_returnedValues.add(item);
		m_returnedValuesMap.put(name, item);
	}

	public void clear() {
		m_sql = "";
		m_autoId = "";
		m_genTable = "";
		m_verifyExpr = "";
		m_method = "";
		m_AccessorContext = "";
		m_dataPanelIdx = "";
		m_termName = "";
		m_firstOutParamPos = "";
		m_outParamsQty = "";
		m_items.removeAllElements();
		if (m_returnedValues != null) {
			m_returnedValues.clear();
			m_returnedValuesMap.clear();
		}
	}


	public void setDataDefCoordinates(String dialogName, int panelIdx, String termName) {
		m_AccessorContext = dialogName;
		m_dataPanelIdx = String.valueOf(panelIdx);
		m_termName = termName;
	}

	public void setMethod(String method, Integer returnedValuePos, Integer returnedValuesQty) {
		m_method = method;
		if (returnedValuePos != null) {
			if (returnedValuesQty != null) {
				m_firstOutParamPos = returnedValuePos.toString();
				m_outParamsQty = returnedValuesQty.toString();
			}
			m_returnedValues = new Vector<ReturnedValueItem>();
			m_returnedValuesMap = new CaselessStringKeyMap<ReturnedValueItem>(m_jotyMessanger);
		}
	}

}
