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

import java.io.FileNotFoundException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * Hosts the content of a configuration file within an internal map of String
 * objects.
 * <p>
 * Its constructor loads the content from the file, the name of which is
 * received as parameter, and the {@code configTermValue} method makes the
 * access to each term available.
 * <p>
 * Its member {@code m_fileContent} is keep public for the use on the serve side
 * where its value must be returned to the client just as it is.
 * 
 */
public class ConfigFile {
	public class ConfigException extends Exception {}

	public Document m_document = null;
	public String m_fileContent;
	private String m_fileName;
	private boolean m_serverSide;
	public String m_type;
	CaselessStringKeyMap<String> m_map;
	JotyMessenger m_jotyMessanger;
	
	public ConfigFile(JotyMessenger jotyMessanger) {
		m_jotyMessanger = jotyMessanger;
	}

	public ConfigFile(JotyMessenger jotyMessanger, String fileName) {
		this(jotyMessanger, fileName, false);
	}

	public ConfigFile(JotyMessenger jotyMessanger, String fileName, boolean serverSide) {
		this(jotyMessanger, fileName, serverSide, null, false);
	}

	public ConfigFile(JotyMessenger jotyMessanger, String fileName, boolean serverSide, String lang, boolean silent) {
		if (!serverSide && jotyMessanger.isDesignTime())
			return;
        m_jotyMessanger = jotyMessanger;
		m_fileName = lang == null || serverSide ? fileName : "./lang/" + lang + "/" + fileName;
		m_serverSide = serverSide;
		try {
			m_fileContent = Utilities.getFileContent(m_fileName, "UTF-8", silent);
		} catch (FileNotFoundException e) {}
		buildDoc(m_fileContent);
		if (lang != null)
			buildMap();
	}

	public void buildDoc(String content) {
		if (content != null)
			m_document = Utilities.getXmlDocument(content);
	}

	public void buildMap() {
		if (m_document != null) {
			m_map = new CaselessStringKeyMap<String>(m_jotyMessanger);
			NodeList nodeList = m_document.getDocumentElement().getChildNodes();
			Node valueNode;
			for (int i = 0; i < nodeList.getLength(); i++) {
				valueNode = nodeList.item(i);
				if (valueNode.getFirstChild() != null)
					m_map.put(valueNode.getNodeName(), valueNode.getFirstChild().getNodeValue());
			}
			if (!m_serverSide)
				m_document = null;
		}
	}

	public String configTermValue(String termName) throws ConfigException {
		return configTermValue(termName, false);
	}

	public String configTermValue(String element, boolean silentFailure) throws ConfigException {
		String retVal = null;
		if (m_map == null) {
			if (m_document != null) {
				NodeList nodeList = m_document.getElementsByTagName(element);
				if (nodeList != null && nodeList.getLength() > 0)
					retVal = nodeList.item(0).getTextContent();
			}
		} else
			retVal = m_map.get(element);
		if (retVal == null && !silentFailure && !m_serverSide) {
            m_jotyMessanger.jotyMessage((m_fileName == null ? ("Type :" + m_type) : ("File :" + m_fileName)) + " - Item '" + element + "' not found !");
			throw new ConfigException();
		}
		return retVal;
	}

	public String fileName() {
		return m_fileName;
	}

	public boolean isMissing() {
		return m_document == null && m_map == null;
	}

}
