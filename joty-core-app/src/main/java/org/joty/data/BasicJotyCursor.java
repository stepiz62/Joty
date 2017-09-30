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

package org.joty.data;

import org.joty.app.JotyApplication;
import org.joty.common.CaselessStringKeyMap;

/**
 *
 * This class represents the minimal implementation of the Joty cursor, 
 * built by only a set of {@code FieldDescriptor} objects and a name-base 
 * map for addressing their positions in the set. 
 *
 * @see FieldDescriptor
 */
public class BasicJotyCursor{

    public FieldDescriptor m_fields[];
    public CaselessStringKeyMap<FieldDescriptor> m_fieldsMap;
    public boolean m_closed;
 
    public BasicJotyCursor(int size, JotyApplication app) {
        m_fields = new FieldDescriptor[size];
        m_fieldsMap = new CaselessStringKeyMap<FieldDescriptor>(app);
    }

    public void setToNull() {
        if (m_fields != null)
            for (int i = 0; i < m_fields.length; i++)
                m_fields[i].m_isNull = true;
    }
}
