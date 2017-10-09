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

import java.beans.Beans;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;

import org.joty.common.ApplMessenger;
import org.joty.common.Utilities;

/**
 * The class provides any logging features needed by the framework.
 * <p>
 * Starting from the interaction with Eclipse WindowBuilder, by means of the
 * {@code  TermContainerPanel.notifyJotyDesignError} method, for the storage in
 * the log file read by the {@code org.joty.jotyplugin.JotyDesignErrorWiew}, it
 * provides the logging activity either for the {@code Application} object or
 * for the {@code JotyServlet} object.
 * <p>
 * As part of the framework, it supports the duality of its own localization, as
 * it is invoked, stated that such a duality is provided by the calling
 * environment (typically other classes for the {@code org.joty.common} package.
 * <p>
 * Any way the class provides enough degree of flexibility to be used along the
 * application and in the server side, as it is needed.
 * <p>
 * The class introduces the concept of 'Host log' as the main target (file) for
 * logging activity, no matter whether on the client side or on the server side
 * its instance lives.
 * <p>
 * To make faster the detection of bugs, during the development, that is in
 * 'debug' mode (see Configuration files), on the client side it uses the
 * {@code Application#JotyMsg} method to intercept logging activity for direct
 * delivering of information to the tester/developer.
 * 
 * 
 * @see Application#checkWBE(java.awt.Container)
 * @see org.joty.workstation.gui.TermContainerPanel#notifyJotyDesignError
 * @see org.joty.jotyplugin.JotyDesignErrorWiew
 * @see org.joty.workstation.app.Application
 * @see org.joty.server.JotyServer
 * 
 */
public class Logger {

	static String m_hostLogName;
	static String m_hostLogDirName;
	static boolean m_serverSide = true;
	static boolean m_debug = true;
	private static boolean m_silent;
	public static ApplMessenger m_app;

	public static void appendToHostLog(String text) {
		appendToHostLog(text, false);
	}

	public static void appendToHostLog(String text, boolean stackTraceToo) {
		appendToHostLog(text, stackTraceToo, false);
	}

	public static void appendToHostLog(String text, boolean stackTraceToo, boolean silent) {
		m_silent = silent;
		appendToLog(m_hostLogName, text, stackTraceToo, null, false);
	}

	public static void appendToLog(String fileName, String text) {
		appendToLog(fileName, text, false, null, false);
	}

	public static void appendToLog(String fileName, String text, Boolean stackTraceToo, String user, boolean finalName) {
		PrintWriter writer = null;
		String actualFileName;
		if (finalName)
			actualFileName = fileName;
		else {
			Calendar calendar = Calendar.getInstance();
			actualFileName = realDirName(m_hostLogDirName) + "/" + fileName + "_" + String.format("%1$04d-", calendar.get(Calendar.YEAR)) + String.format("%1$02d-", calendar.get(Calendar.MONTH) + 1) + String.format("%1$02d", calendar.get(Calendar.DAY_OF_MONTH)) + ".log";
		}
		try {
			StringBuilder labString = null;
			if (!designState()) {
				labString = new StringBuilder();
				labString.append(user == null ? "" : "USER : " + user + " ");
				labString.append((new Date().toString()));
				labString.append("\n");
				labString.append(text);
				labString.append("\n");
			}
			writer = new PrintWriter(new BufferedWriter(new FileWriter(actualFileName, true)));
			if (text.length() > 0)
				writer.println(designState() ? text : labString.toString());
			if (stackTraceToo)
				printStackTrace(writer);
			writer.close();
			if (!m_serverSide && m_debug && !m_silent)
				m_app.JotyMsg(null, text);
		} catch (IOException e) {
			if (!m_serverSide)
				m_app.JotyMsg(null, "It has not been possible to access the log file :\n\n" + (new File(actualFileName)).getAbsolutePath() + "\n\nReason : " + e.toString());
		}
	}

	public static void appInit() {
		m_serverSide = false;
		m_silent = designState();
	}

	static boolean designState() {
		return !m_serverSide && Beans.isDesignTime();
	}

	public static void exceptionToHostLog(Exception e) {
		if (!m_serverSide)
			m_app.getCommon().resetRemoteTransactionBuilding();
		String logtext = e.toString() == null ? e.getMessage() : e.toString();
		if (logtext != null)
			appendToHostLog(logtext, true);
	}

	public static void printStackTrace(PrintWriter writer) {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		for (StackTraceElement stackTraceElement : stackTraceElements)
			writer.println("     " + stackTraceElement.toString());
	}

	private static String realDirName(String dirName) {
		return (m_serverSide ? "" : (System.getProperty("user.home") + "/")) + dirName;
	}

	public static void setDebugMode(boolean debug) {
		m_debug = debug;
	}

	public static void setHostLogName(String fileName, String dirPathName) {
		m_hostLogName = fileName;
		m_hostLogDirName = dirPathName;
		Utilities.checkDirectory(realDirName(m_hostLogDirName));
	}

	public static void stackTraceToHostLog(String text) {
		appendToHostLog("", true);
	}

	public static void throwableToHostLog(Throwable th) {
		if (th instanceof Exception)
			Logger.exceptionToHostLog((Exception) th);
		else
			Logger.appendToHostLog(th.toString());
	}

	public static void writeToLog(String fileName, String text) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileName);
		} catch (FileNotFoundException e1) {
			Logger.exceptionToHostLog(e1);
		}
		byte buf[] = text.getBytes();
		if (fos != null)
			try {
				fos.write(buf, 0, buf.length);
				fos.close();
			} catch (IOException e) {}

	}

}
