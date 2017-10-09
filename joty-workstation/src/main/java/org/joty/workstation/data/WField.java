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

package org.joty.workstation.data;

import org.joty.app.JotyApplication;
import org.joty.data.WrappedField;

public class WField extends WrappedField{
	public WField(JotyApplication jotyApplication) {
		super(jotyApplication);
	}

	public JotyDataBuffer m_metaDataSource;

	@Override
	public String dbFieldNameFromMetadataSource() {
		return dbFieldSpecifiedFromMetadataSource() ? 
				m_metaDataSource.m_fieldNames.get(m_idx) : m_dbFieldName;
	}

	@Override
	public int dataTypeFromMetadataSource() {
		return dbFieldSpecifiedFromMetadataSource() ? 
				m_metaDataSource.m_fieldTypes.get(m_idx) : m_dataType;
	}

	@Override
	public boolean dbFieldSpecifiedFromMetadataSource() {
		return  m_metaDataSource != null && m_idx >= 0;
	}

	
}
