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

import java.util.HashMap;


/**
 * An HashMap descendant that offers simple way of use.
 * <p>
 * It is for dealing with set of objects of some type {@code <T>} mapped on key String-s
 * by being unaware of case of the keys.
 * <p>
 * the class provides a bit of assistance for key collisions: the
 * {@code m_overwritable} member can be set to true in order to have values
 * overwritten on collision.
 * 
 * @param <T>
 *            the type of the value instances stored and mapped to the keys.
 */
public class CaselessStringKeyMap<T> extends HashMap<String, T> {

	boolean m_overwritable = false;
	JotyMessenger m_jotyMessanger;

	public CaselessStringKeyMap(JotyMessenger jotyAppInstance) {
		m_jotyMessanger = jotyAppInstance;
	}
	
	public T get(String key) {
		return super.get(key.toLowerCase());
	}

	public T getWithWarning(String key) {
		T elem = get(key);
		if (elem == null) {
			String msg = String.format("'%1$s' not found !", key);
			m_jotyMessanger.jotyWarning(msg);
		}
		return elem;
	}

	@Override
	public T put(String key, T value) {
		String lcKey = key.toLowerCase();
		if (!m_overwritable && get(lcKey) != null)
			m_jotyMessanger.jotyMessage("In a map, the pre-exisiting slot for the key '" + key + "' is being overwritten !");
		return super.put(lcKey, value);
	}

	@Override
	public T remove(Object key) {
		return super.remove(((String) key).toLowerCase());
	}

	public void setOverWritable() {
		m_overwritable = true;
	}

}