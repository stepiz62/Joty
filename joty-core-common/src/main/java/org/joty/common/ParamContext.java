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

import java.util.Iterator;
import java.util.Map.Entry;


/**
 * It contains a map of named items ({@code ContextParam}) capable to host the
 * value of a Joty datum.
 * <p>
 * It is used as intermediate medium in parameter passing activities: Identified
 * the 'context' in which a caller/client invokes a method of a provider of some
 * functionality, in all cases in which it is manifest that the actualization of
 * the generic parameter (typically performed through some computation itself)
 * is reusable some where in the same context during its life, then it is
 * convenient to store the value here as if the caller is the server for the
 * parameter value. At the same time or 'later' the 'client' of the parameter
 * value can get it by name from this storage area.
 * <p>
 * Joty uses this structure along the whole framework, anywhere the above
 * scenario occurs, but, also, uses it in communication from the client to the
 * Joty Server. Actually this class takes an important role in the main feature
 * the framework offers, the duality of the mode of running web or not web: the
 * instantiated object in non web mode is a decoupler of the interaction between
 * caller and called part. This decoupling is used in web mode where the object
 * is in some way disassembled within a {@code BasicPostStatement} vehicle and is
 * re-composed on the server side.
 * <p>
 * Furthermore instances of this class are used to allocate different
 * environments, that can contain even the same set of parameters but each
 * environment keeping its copy of the values isolated and protected. An
 * instance of the class is kept in the {@code JotyDialog} object (in the
 * {@code JotyDialog.m_callContext} member), just right with this task.
 * In a {@code MenuActivity} object, instead, only a reference to an object of
 * this type is held; the instance may be embedded or coming from the outside
 * since shared with the external context.
 * 
 * 
 * @see BasicPostStatement
 * 
 */
public class ParamContext {
	JotyMessenger m_jotyMessanger;

	public ParamContext(JotyMessenger jotyMessanger) {
		m_jotyMessanger = jotyMessanger;
	}

	public class ContextParam {
		public CTXP_type m_type;
		public Long m_lVal = null;
		public String m_strVal = null;
		public IJotyDate m_dateVal = null;
		public Double m_dblVal = null;
		public boolean m_dirty = false;
	};

	public enum CTXP_type {
		CTXP_NUM, CTXP_STR, CTXP_DATE, CTXP_DBL
	}

	public CaselessStringKeyMap<ContextParam> m_paramsSet;

	private void checkParamsSet() {
		if (m_paramsSet == null)
			m_paramsSet = new CaselessStringKeyMap<ContextParam>(m_jotyMessanger);
	}

	public void clear() {
		if (m_paramsSet != null)
			m_paramsSet.clear();
	}

	public String contextParameter(String name) {
		String retVal = "-1";
		ContextParam storedQueryParam = null;
		if (m_paramsSet == null)
			return retVal;
		else
			storedQueryParam = m_paramsSet.get(name);
		if (storedQueryParam != null)
			switch (storedQueryParam.m_type) {
				case CTXP_STR:
					retVal = storedQueryParam.m_strVal;
					break;
				case CTXP_NUM:
					retVal = String.valueOf(storedQueryParam.m_lVal);
					break;
				case CTXP_DATE:
					retVal = storedQueryParam.m_dateVal.render(true, false);
					break;
				case CTXP_DBL:
					retVal = String.valueOf(storedQueryParam.m_dblVal);
					break;
			}
		return retVal;
	}

	public void copy(Object callContext) {
		checkParamsSet();
		for (Iterator it = ((ParamContext) callContext).m_paramsSet.entrySet().iterator(); it.hasNext();) {
			Entry<String, ContextParam> entry = ((Entry<String, ContextParam>) it.next());
			m_paramsSet.put(entry.getKey(), entry.getValue());
		}
	}

	public ContextParam getContextParam(String varName, CTXP_type type) {
		checkParamsSet();
		m_paramsSet.remove(varName);
		ContextParam storedQueryParam = m_paramsSet.get(varName);
		if (storedQueryParam == null) {
			storedQueryParam = new ContextParam();
			storedQueryParam.m_type = type;
		}
		storedQueryParam.m_dirty = true;
		return storedQueryParam;
	}

	public boolean isMissingParam(String name) {
		return contextParameter(name).compareTo("-1") == 0;
	}

	public void setContextParam(String varName, Double valueExpr) {
		ContextParam storedQueryParam = getContextParam(varName, CTXP_type.CTXP_DBL);
		storedQueryParam.m_dblVal = valueExpr;
		m_paramsSet.put(varName, storedQueryParam);
	}

	public void setContextParam(String varName, IJotyDate valueExpr) {
		ContextParam storedQueryParam = getContextParam(varName, CTXP_type.CTXP_DATE);
		storedQueryParam.m_dateVal = valueExpr;
		m_paramsSet.put(varName, storedQueryParam);
	}

	public void setContextParam(String varName, long valueExpr) {
		ContextParam storedQueryParam = getContextParam(varName, CTXP_type.CTXP_NUM);
		storedQueryParam.m_lVal = valueExpr;
		m_paramsSet.put(varName, storedQueryParam);
	}

	public void setContextParam(String varName, String valueExpr) {
		ContextParam storedQueryParam = getContextParam(varName, CTXP_type.CTXP_STR);
		storedQueryParam.m_strVal = valueExpr;
		m_paramsSet.put(varName, storedQueryParam);
	}

	public void setDirty(boolean predicate) {
		if (m_paramsSet != null)
			for (Iterator it = m_paramsSet.entrySet().iterator(); it.hasNext();)
				((Entry<String, ContextParam>) it.next()).getValue().m_dirty = predicate;
	}

}
