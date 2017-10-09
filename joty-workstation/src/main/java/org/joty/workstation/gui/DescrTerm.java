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

package org.joty.workstation.gui;

import java.beans.Beans;

import org.joty.workstation.app.Application;
import org.joty.workstation.app.Application.LiteralStruct;
import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * Adds to the Term class the ability to hold a reference to a
 * {@code LiteralStruct} object. Provides methods for accessing it in order to
 * get a description literal querying by name or by id. Informs this object that
 * a use of it is made locally, so that the framework can drive this term to
 * refresh the use of the object if its content changes.
 * 
 * @see org.joty.workstation.app.Application.LiteralStruct
 * 
 */
public abstract class DescrTerm extends Term {

	public class DynamicLiteralStructParams {
		String m_structName;
		String m_dbTableName;
		String m_keyFldName;
		String m_descrFldName;
	}

	public DynamicLiteralStructParams m_dynamicLiteralStructParams;
	boolean m_defaultLiteralStructLost;
	public LiteralStruct m_literalStruct;

	public DescrTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
		m_literalStruct = null;
		if (params.m_descrSetName != null) {
			LiteralStruct literalStruct = (LiteralStruct) Application.m_common.literalStruct(params.m_descrSetName);
			if (literalStruct == null) {
				if (!Beans.isDesignTime())
					panel.notifyJotyDesignError(getComponent(), "Term : '" + m_name + "' - Description name '" + params.m_descrSetName + "' unexisting !");
			} else {
				m_literalStruct = literalStruct;
				m_literalStruct.m_termsSet.add(this);
			}
		}
		m_defaultLiteralStruct = m_literalStruct;
	}

	@Override
	public void clearAppReferences() {
		if (m_literalStruct != null)
			m_literalStruct.m_termsSet.remove(this);
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {}

	String descrByItemData(long itemData) {
		Integer posIdx = null;
		if (m_literalStruct.m_descrReverseMap != null)
			posIdx = m_literalStruct.m_descrReverseMap.get(itemData);
		if (itemData >= 0 && posIdx != null)
			return m_literalStruct.m_descrArray.get(posIdx).descr;
		else
			return "";
	}

	String descrByItemData(String itemData) {
		Integer posIdx = m_literalStruct.m_strKeyRevMap.get(itemData);
		if (itemData.length() > 0 && posIdx != null)
			return m_literalStruct.m_descrArray.get(posIdx).descr;
		else
			return "";
	}

	public String getCurSelStrKey() {
		return "";
	}

	public void loadDescrList() {}

	public void reloadDescrList() {
		int currentSelection = (int) getCurSelData(false);
		loadDescrList();
		setSelection(currentSelection, true);
		if (m_panel instanceof DataAccessPanel)
			((DataAccessPanel) m_panel).updateRecordOnController();
	}

	public void setDynamicLiteralStructParams(String structName, String tabName, String keyFldName, String descrFldName) {
		m_dynamicLiteralStructParams = new DynamicLiteralStructParams();
		m_dynamicLiteralStructParams.m_structName = structName;
		m_dynamicLiteralStructParams.m_dbTableName = tabName;
		m_dynamicLiteralStructParams.m_keyFldName = keyFldName;
		m_dynamicLiteralStructParams.m_descrFldName = descrFldName;
	}

}
