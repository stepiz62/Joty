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
import java.util.Vector;

import javax.swing.DefaultCellEditor;

import org.joty.common.CaselessStringKeyMap;
import org.joty.data.WrappedField.IdentityRenderer;
import org.joty.workstation.app.Application;

/**
 * Holds meta-data relative to the cells that compose any row of a
 * {@code GridBuffer} instance, thas is it is a row structure descriptor.
 * 
 * @see GridBuffer
 * 
 */
public class GridRowDescriptor {
	public class CellDescriptor {

		public int m_mappedWFieldIdx;
		public String m_label;
		public RowCellMappingType m_mappingType;
		public String header;
		public String m_fieldName = null;
		public String m_targetFieldName;
		public IdentityRenderer m_identityRenderer;
		public ViewersManager m_viewersManager;
		public boolean m_isFlag;
		public boolean m_editable = false;
		public DefaultCellEditor m_cellEditor;

		public void setEditable() {
			m_editable = true;
		}

		public void setIdentityRenderer(IdentityRenderer renderer) {
			m_identityRenderer = renderer;
		}

		public void setTargetField(String name) {
			m_targetFieldName = name;
		}
	};

	public enum RowCellMappingType {
		PANEL_TERM, KEY_ELEM, FIELD
	}

	Vector<CellDescriptor> vector;
	CaselessStringKeyMap<CellDescriptor> map;

	public GridRowDescriptor() {
		vector = new Vector<CellDescriptor>();
		map = new CaselessStringKeyMap<CellDescriptor>(Application.m_app);
	}

	public void add(String fieldName, RowCellMappingType mappingType, int pos, String label) {
		CellDescriptor cellDescriptor = new CellDescriptor();
		cellDescriptor.m_mappingType = mappingType;
		cellDescriptor.m_mappedWFieldIdx = pos;
		cellDescriptor.m_label = label;
		cellDescriptor.m_fieldName = fieldName;
		if (!Beans.isDesignTime()) {
			vector.add(cellDescriptor);
			if (fieldName == null)
				fieldName = "FieldIdx__" + String.valueOf(pos);
			map.put(fieldName, cellDescriptor);
		}
	}

	public CellDescriptor get(String fieldName) {
		return map.get(fieldName);
	}

}
