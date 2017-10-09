/*
	Copyright (c) 2013-2017, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

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

import java.util.logging.Level;

import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.EngineConstants;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IEngineTask;
import org.eclipse.birt.report.engine.api.IPDFRenderOption;
import org.eclipse.birt.report.engine.api.IRenderOption;
import org.eclipse.birt.report.engine.api.IRenderTask;
import org.eclipse.birt.report.engine.api.IReportDocument;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.IRunTask;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.core.internal.registry.RegistryProviderFactory;
import org.joty.common.JotyMessenger;
import org.joty.common.ApplMessenger;
import org.joty.common.JotyTypes;
import org.joty.common.ReportManager;
import org.joty.common.ReportManager.Parameter;

/**
 * This class is a wrapper of the interaction of the Joty framework with the
 * BIRT engine api-s.
 * <p>
 * All the settling phase for the engine and the launch of the its elaboration
 * on a Report design document are made here. The context being searched by the
 * engine for a named report design document, is defined in the application
 * configuration file, and even the target directories for the output of the
 * engine are defined there.
 * <p>
 * The class is instantiated either by the Joty Server or by the {@code Application} object 
 *  depending on the application running in web mode or not.
 * <p>
 * {@code buildReport} is the most significant method.
 * 
 * @see #buildReport(String, String, boolean)
 * 
 */
public class BirtManager {

	public String m_rptDesignsPath;
	public String m_rptDocumentsPath;
	public String m_rptOutputsPath;
	public String m_rptLogsPath;

	private String m_user;
	private String m_realPath;
	private IReportEngine m_engine;
	private IRunTask m_runTask = null;

	private String m_dbUrl;
	private String m_jdbcDriverClass;
	private String m_password;

	private boolean m_initied;

	public String m_exception;

	private ReportManager m_reportManager;
	private String m_language;
	private Object m_app;

	public BirtManager(ReportManager reportManager, Object app) {
		m_reportManager = reportManager;
		m_app = app;
	}

	public BirtManager(ReportManager reportManager) {
		this(reportManager, null);
	}

	public void appInit() {
	}


	protected IRenderOption buildOptions(String reportName, String renderType) {
		String fileExtension = renderType;
		if (renderType.compareToIgnoreCase("postscript") == 0)
			fileExtension = "ps";
		IRenderOption options = new RenderOption();
		options.setOutputFormat(renderType);
		options.setOutputFileName(outputFileDir() + reportName + "." + fileExtension);
		if (renderType.compareToIgnoreCase("pdf") == 0) {
			options.setOption(IPDFRenderOption.PAGE_OVERFLOW, new Integer(IPDFRenderOption.OUTPUT_TO_MULTIPLE_PAGES));
			options.setOption(IPDFRenderOption.CLOSE_OUTPUTSTREAM_ON_EXIT, new Boolean(true));
		} else if (renderType.compareToIgnoreCase("html") == 0) {
			HTMLRenderOption htmlOptions = new HTMLRenderOption(options);
			htmlOptions.setImageDirectory(outputFileDir() + "image");
			htmlOptions.setHtmlPagination(false);
		}
		return options;
	}

	/**
	 * This method supports either the one process mode or the two process mode
	 * offered by the BIRT Engine.
	 * 
	 * @param name
	 *            the name of the report as it will be searched by the BIRT
	 *            Engine in the file-system (a part from the extension)
	 * @param renderType
	 *            one value chosen among the following { "pdf", "html", "ods",
	 *            "xls", "ppt", "doc" }
	 * @param twoProcess
	 *            if true the Birt Engine is requested to run in two distinct
	 *            phases: 'run' with the production of an intermediate and final
	 *            document object and then 'render' where the engine works only
	 *            for the presentation of the output. (see the BIRT Report Engine documentation)
	 * @see BirtManager
	 */
  	public void buildReport(String name, String renderType, boolean twoProcess) {
		if (m_initied) {
			if (m_app != null)
				((JotyMessenger) m_app).beforeReportRender();
			m_exception = null;
			if (twoProcess)
				runThenRenderReport(name, renderType);
			else
				runAndRenderReport(name, renderType);
			if (m_app != null)
				((JotyMessenger) m_app).afterReportRender(outputFileDir() + name + "." + renderType);
		} else
			Logger.appendToHostLog("Report engine initialization failure !");
	}

	protected IEngineTask createTaskOnDesign(String reportName, boolean forAnOnlyProcess) {
		IEngineTask task = null;
		boolean success = true;
		try {
			IReportRunnable design;
			design = m_engine.openReportDesign(makePath(m_rptDesignsPath) + m_language + "/" + reportName + ".rptdesign");
			task = forAnOnlyProcess ? m_engine.createRunAndRenderTask(design) : m_engine.createRunTask(design);
			manageAppContext(task);
			task.setParameterValue("DbUrl", m_dbUrl);
			task.setParameterValue("DriverClass", m_jdbcDriverClass);
			task.setParameterValue("UserName", m_user);
			task.setParameterValue("Password", m_password);
			for (Parameter param : m_reportManager.m_params) {
				switch (param.type) {
					case JotyTypes._int:
						task.setParameterValue(param.name, param.intVal);
						break;
					case JotyTypes._text:
						task.setParameterValue(param.name, param.strVal);
						break;
				}
			}
			if (!task.validateParameters()) {
				m_exception = "Birt: parameters not correctly set !";
				Logger.appendToHostLog(m_exception);
				success = false;
			}
		} catch (Exception e) {
			manageException(e);
			success = false;
		}
		if (!success) {
			if (task != null) {
				task.close();
				task = null;
			}
		}
		return task;
	}

	public void end() {
		m_engine.destroy();
		Platform.shutdown();
		RegistryProviderFactory.releaseDefault();
	}


	public void init() {
		if (m_initied) {
			try {
				final EngineConfig config = new EngineConfig();
				if (m_rptLogsPath.length() > 0)
					config.setLogConfig(makePath(m_rptLogsPath), Level.FINE);
				Platform.startup(config);
				IReportEngineFactory factory = (IReportEngineFactory) Platform.createFactoryObject(IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY);
				m_engine = factory.createReportEngine(config);
				m_engine.changeLogLevel(Level.WARNING);
			} catch (Exception e) {
				Logger.exceptionToHostLog(e);
				m_initied = false;
			}
		}
	}

	public void init(String designsPath, String documentsPath, String outputsPath, String logsPath) {
		m_rptDesignsPath = designsPath;
		m_rptDocumentsPath = documentsPath;
		m_rptOutputsPath = outputsPath;
		m_rptLogsPath = logsPath;
		m_initied = m_rptDesignsPath != null && m_rptDocumentsPath != null && m_rptOutputsPath != null && m_rptLogsPath != null;
		init();
	}

	public boolean initied() {
		return m_initied;
	}

	private String makePath(String path) {
		return makePath(path, false);
	}

	private String makePath(String path, boolean userSpecific) {
		return ((m_realPath == null ? "" : (m_realPath + "/")) + path + "/" + ((userSpecific && m_user != null ? (m_user + "/") : "")));
	}

	private void manageAppContext(IEngineTask task) {
		task.getAppContext().put(EngineConstants.APPCONTEXT_CLASSLOADER_KEY, BirtManager.class.getClassLoader());
	}

	public void manageException(Exception e) {
		Logger.appendToHostLog("Birt: " + e.getMessage());
		m_exception = e.getMessage();
	}

	public String outputFileDir() {
		return makePath(m_rptOutputsPath, true);
	}

	public boolean renderReport(String reportName, String renderType) {
		boolean retVal = false;
		IReportDocument iReportDocument = null;
		IRenderTask task = null;
		try {
			iReportDocument = m_engine.openReportDocument(makePath(m_rptDocumentsPath, true) + reportName + ".rptdocument");
			retVal = true;
		} catch (EngineException e) {
			manageException(e);
		}
		if (iReportDocument != null) {
			task = m_engine.createRenderTask(iReportDocument);
			if (task != null) {
				manageAppContext(task);
				task.setRenderOption(buildOptions(reportName, renderType));
				try {
					task.render();
				} catch (EngineException e) {
					retVal = false;
					manageException(e);
				}
				task.close();
			}
			iReportDocument.close();
		}
		return retVal;
	}

	public boolean runAndRenderReport(String reportName, String renderType) {
		boolean retVal = false;
		IRunAndRenderTask task = (IRunAndRenderTask) createTaskOnDesign(reportName, true);
		if (task != null) {
			task.setRenderOption(buildOptions(reportName, renderType));
			try {
				task.run();
				retVal = true;
			} catch (EngineException e) {
				manageException(e);
			}
			task.close();
		}
		return retVal;
	}

	public boolean runReport(String reportName) {
		boolean retVal = false;
		m_runTask = (IRunTask) createTaskOnDesign(reportName, false);
		if (m_runTask != null) {
			manageAppContext(m_runTask);
			try {
				m_runTask.run(makePath(m_rptDocumentsPath, true) + reportName + ".rptdocument");
				retVal = true;
			} catch (EngineException e) {
				manageException(e);
			}
			m_runTask.close();
		}
		return retVal;
	}

	public boolean runThenRenderReport(String reportName, String renderType) {
		boolean retVal = runReport(reportName);
		if (retVal)
			retVal = renderReport(reportName, renderType);
		return retVal;
	}

	public void setDbUrl(String dbUrl) {
		m_dbUrl = dbUrl;
	}

	public void setJdbcDriverClass(String jdbcDriverClass) {
		m_jdbcDriverClass = jdbcDriverClass;
	}

	public void setLanguage(String language) {
		m_language = language;
	}

	public void setPassword(String password) {
		m_password = password;
	}

	public void setRealPath(String path) {
		m_realPath = path;
	}

	public void setUser(String user) {
		m_user = user;
	}

}
