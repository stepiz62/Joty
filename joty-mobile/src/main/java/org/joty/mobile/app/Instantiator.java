/*
	Copyright (c) 2013-2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

	This file is part of Joty 2.0 Mobile.

	Joty 2.0 Mobile is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Joty 2.0 Mobile is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with Joty 2.0 Mobile.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.joty.mobile.app;


import org.joty.common.AbstractDbManager;
import org.joty.common.ConfigFile;
import org.joty.common.ErrorCarrier;
import org.joty.mobile.app.JotyApp;

/**
 * Like {@code org.joty.common.Instantiator} but adapted to the mobile part for sharing the same config info with the workstation part.
 * 
 * @see org.joty.common.ConfigFile
 * @see org.joty.common.ErrorCarrier
 * 
 */
public class Instantiator {
	public static <T> T create(String className, ConfigFile configuration) throws ClassNotFoundException {
		String generatorClass = null;
		try {
			generatorClass = configuration == null ? className : configuration.configTermValue(className).replace(".common.", ".mobile.");
		} catch (ConfigFile.ConfigException e1) {
            JotyApp.m_app.exceptionToToast(e1);
		}
		T retVal = null;
		if (generatorClass != null)
			try {
				retVal = (T) Class.forName(generatorClass).newInstance();
			} catch (InstantiationException e) {
                JotyApp.exceptionToToast(e);
                JotyApp.m_app.toast("   class name: " + generatorClass);
			} catch (IllegalAccessException e) {
                JotyApp.exceptionToToast(e);
			}
		return retVal;
	}

	public static AbstractDbManager createDbManager(ErrorCarrier errorCarrier, ConfigFile configuration) throws ClassNotFoundException {
		AbstractDbManager retVal = create("dbManagerClass", configuration);
		if (retVal != null) {
			retVal.m_configuration = configuration;
			retVal.m_errorCarrier = errorCarrier;
		}
		return retVal;
	}
}
