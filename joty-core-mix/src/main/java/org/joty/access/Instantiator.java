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

import org.joty.access.DbManager.DbConnectionGrabber;
import org.joty.common.ConfigFile;
import org.joty.common.ErrorCarrier;
import org.joty.common.JotyMessenger;
import org.joty.common.ConfigFile.ConfigException;

/**
 * Uses Java Reflection to instantiate classes specified by name or by a literal
 * that is used to pick the actual class name up from a {@code ConfigFile}
 * object.
 * <p>
 * It provides two methods that wrap the configuration access for two specific
 * classes of the framework and allows the injection of an {@code ErrorCarrier}
 * object into them.
 * 
 * @see ConfigFile
 * @see ErrorCarrier
 * 
 */
public class Instantiator {
	public static <T> T create(String className, ConfigFile configuration) throws ClassNotFoundException {
		String generatorClass = null;
		try {
			generatorClass = configuration == null ? className : configuration.configTermValue(className);
		} catch (ConfigException e1) {
			Logger.exceptionToHostLog(e1);
		}
		T retVal = null;
		if (generatorClass != null)
			try {
				retVal = (T) Class.forName(generatorClass).newInstance();
			} catch (InstantiationException e) {
				Logger.exceptionToHostLog(e);
				Logger.appendToHostLog("   class name: " +  generatorClass, false);
			} catch (IllegalAccessException e) {
				Logger.exceptionToHostLog(e);
			}
		return retVal;
	}

	/**
	 * {@code extraConfig} argument exists because on the server side there are
	 * two different configuration files and one of them is kept far from the
	 * {@code Accessor} object a part from a small chunk of data that here are
	 * injected into the object.
	 * 
	 * @see Accessor
	 */
	public static Accessor createAccessor(JotyMessenger jotyMessanger, ErrorCarrier errorCarrier, ConfigFile config, ConfigFile extraConfig, DbConnectionGrabber dbConnGrabber) throws ClassNotFoundException {
		Accessor retVal = create("accessorClass", config);
		if (retVal != null) {
			retVal.init(jotyMessanger);
			retVal.m_errorCarrier = errorCarrier;
			try {
				retVal.setSharingData(extraConfig);
			} catch (ConfigException e) {}
			retVal.setFromConfiguration(config);
			retVal.setDbConnectionGrabber(dbConnGrabber);
			retVal.init();
		}
		return retVal;
	}

	public static DbManager createDbManager(ErrorCarrier errorCarrier, ConfigFile configuration) throws ClassNotFoundException {
		DbManager retVal = create("dbManagerClass", configuration);
		if (retVal != null) {
			retVal.m_configuration = configuration;
			retVal.m_errorCarrier = errorCarrier;
		}
		return retVal;
	}
}
