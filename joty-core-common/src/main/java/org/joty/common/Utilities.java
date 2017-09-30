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

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * It is a collection of multi-purpose static methods thought for use in the
 * Joty framework and in the Joty application.
 *
 */
public class Utilities {

    /**
	 * {@code Stocker} an HashSet descendant that offers simpler syntax of use.
	 * It is for dealing with set of String-s by being unaware of case.
	 */
	public class Stocker extends HashSet<String> {
	
		public void add(Stocker addingStocker) {
			for (String literal : addingStocker)
				add(literal);
		}
	
		@Override
		public boolean add(String literal) {
			return super.add(literal.toLowerCase());
		}
	
		public String asCommaSeparatedList() {
			String retVal = "";
			boolean first = true;
			for (String literal : this) {
				if (first)
					first = false;
				else
					retVal += ",";
				retVal += literal;
			}
			return retVal;
		}
	
		@Override
		public boolean contains(Object literal) {
			return super.contains(((String) literal).toLowerCase());
		}
	
	}

	static private JotyMessenger m_jotyMessanger;
	static public Utilities m_me;

    public static void setMessanger(JotyMessenger jotyMessanger) {
        m_jotyMessanger = jotyMessanger;
        m_me = new Utilities();
    }

    public static void checkDirectory(String pathName) {
        (new File(pathName)).mkdirs();
    }

    /**
     * Returns the quantity of days between two dates plus 1. The quantity may
     * be negative, in this case 1 is added to the scalar. Dates are expressed
     * as Java {@code Calendar} objects.
     *
     * @param startDate
     * @param endDate
     */

    public static long daysQtyBetween(final Calendar startDate, final Calendar endDate) {
        long daysBetween = 0;
        boolean positiveDelta = startDate.before(endDate) || startDate.compareTo(endDate) == 0;
        Calendar labStartDate = (Calendar) (positiveDelta ? startDate.clone() : endDate.clone());
        Calendar labEndDate = (Calendar) (positiveDelta ? endDate.clone() : startDate.clone());
        int startYear = labStartDate.get(Calendar.YEAR);
        int endYear = labEndDate.get(Calendar.YEAR);
        int startMonth = labStartDate.get(Calendar.MONTH);
        int endMonth = labEndDate.get(Calendar.MONTH);
        while (((endYear - startYear) * 12 + (endMonth - startMonth)) > 12) {
            if (labStartDate.get(Calendar.MONTH) == Calendar.JANUARY && labStartDate.get(Calendar.DAY_OF_MONTH) == labStartDate.getActualMinimum(Calendar.DAY_OF_MONTH)) {
                daysBetween += labStartDate.getActualMaximum(Calendar.DAY_OF_YEAR);
                labStartDate.add(Calendar.YEAR, 1);
            } else {
                int diff = 1 + labStartDate.getActualMaximum(Calendar.DAY_OF_YEAR) - labStartDate.get(Calendar.DAY_OF_YEAR);
                labStartDate.add(Calendar.DAY_OF_YEAR, diff);
                daysBetween += diff;
            }
            startYear = labStartDate.get(Calendar.YEAR);
        }
        while ((endMonth - startMonth) % 12 > 1) {
            daysBetween += labStartDate.getActualMaximum(Calendar.DAY_OF_MONTH);
            labStartDate.add(Calendar.MONTH, 1);
            startMonth = labStartDate.get(Calendar.MONTH);
        }
        while (labStartDate.before(labEndDate)) {
            labStartDate.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }
        return positiveDelta ? daysBetween : -daysBetween;
    }

    public static void escapeSingleQuote(StringBuffer labString) {
        String snapShot = labString.toString();
        labString.setLength(0);
        for (int i = 0; i < snapShot.length(); i++) {
            labString.append(snapShot.charAt(i));
            if (snapShot.charAt(i) == '\'')
                labString.append('\'');
        }
    }

    public static String formattedSql(String m_sql) {
        return reduceToBlanks(m_sql).toLowerCase().replace("  ", " ").replace("select ", "\n  SELECT ").replace(" from ", "\n    FROM ").replace(" where ", "\n      WHERE ");
    }

    public static String getFileContent(String fileName, String encoding) throws FileNotFoundException {
        return getFileContent(fileName, encoding, false);
    }

    public static String getFileContent(String fileName, String encoding, boolean silent) throws FileNotFoundException {
        return stringFromInputStream(new FileInputStream(fileName), encoding, silent);
    }

    public static Object getJotyVersion() {
        return "2.0.0";
    }

    public static String getMainTableNameFromSql(String simpleSelectStatement) {
        return getPartAfterWord(simpleSelectStatement, "FROM", true);
    }

    public static String getPartAfterWord(String text, String word, boolean onlyFirstLiteral) {
        text = reduceToBlanks(text);
        String upperCaseStr = new String(text.toUpperCase());
        int fromKeywordPos = upperCaseStr.indexOf(" " + word.toUpperCase().trim() + " ");
        StringBuffer strBuffer = new StringBuffer(text);
        strBuffer.delete(0, fromKeywordPos + word.length() + 2);
        if (onlyFirstLiteral) {
            while (strBuffer.charAt(0) == ' ')
                strBuffer.deleteCharAt(0);
            int currPos = 0;
            char ch;
            while (currPos < strBuffer.length() && (ch = strBuffer.charAt(currPos)) != ',' && ch != ' ')
                currPos++;
            strBuffer.delete(currPos, strBuffer.length());
        }
        return strBuffer.toString();

    }

    public static String getWhereClauseNameFromSql(String simpleSelectStatement) {
        return getPartAfterWord(simpleSelectStatement, "WHERE", false);
    }

    /**
     * Gets a Java {@code Document} object from an xml text expressing an xml document.
     * As it can be seen, the parser is asked to support namespaces.
     * @param xmlText source text
     * @return the Document object
     */
    public static Document getXmlDocument(String xmlText) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            m_jotyMessanger.jotyMessage(e);
        }
        StringReader reader = new StringReader(xmlText);
        InputSource inputSource = new InputSource(reader);
        Document doc = null;
        try {
            doc = builder.parse(inputSource);
        } catch (SAXException e) {
             m_jotyMessanger.jotyMessage(e);
        } catch (IOException e) {
             m_jotyMessanger.jotyMessage(e);
        }
        reader.close();
        return doc;
    }

    public static boolean isLiteral(String text) {
        if (text == null)
            return false;
        return text.length() > 0 && text.indexOf(" ") < 0;
    }

    public static boolean isMoreThanOneWord(String text) {
        if (text == null)
            return false;
        text = reduceToBlanks(text);
        return text.trim().indexOf(" ") >= 0;
    }

    public static boolean isMsSqlServer(String jdbcClassName) {
        return jdbcClassName.indexOf("com.micro") >= 0;
    }

    /**
     * Returns the MD5 Digest code converted in hexadecimal notation of the text received in input.
     */

    public static String md5Digest(String input) {
        String retVal = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] msg = input.getBytes();
            md.update(msg);
            byte[] aMessageDigest = md.digest();
            String hexaDigest = "";
            String tmp;
            for (int i = 0; i < 16; i++) {
                tmp = String.format("%1$02x", aMessageDigest[i]);
                hexaDigest += tmp;
            }
            retVal = new String(hexaDigest);
        } catch (NoSuchAlgorithmException e) {
             m_jotyMessanger.jotyMessage(e);
        }
        return retVal;
    }

    public static String monoCharString(char theChar, int len) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < len; i++)
            buffer.append(theChar);
        return buffer.toString();
    }

    public static String reduceToBlanks(String text) {
        return text.replace("\r", " ").replace("\n", " ").replace("\t", " ");
    }

    /**
     * Uses a StringTokenizer to implement a 'split' functionality.
     *
     * @param sourceString
     *            the source text
     * @param targetVector
     *            the target to be loaded with the extracted times
     * @param separator
     *            the separator
     */
    public static void split(String sourceString, Vector<String> targetVector, String separator) {
        if (sourceString == null)
            return;
        targetVector.removeAllElements();
        StringTokenizer tokenizer = new StringTokenizer(sourceString, separator);
        int dim = tokenizer.countTokens();
        for (int i = 0; i < dim; i++)
            targetVector.add(tokenizer.nextToken());
    }

    public static String sqlEncoded(String valueString) {
        StringBuffer labString = new StringBuffer(valueString);
        escapeSingleQuote(labString);
        return labString.toString();
    }

    public static String stringFromInputStream(InputStream fin, String encoding, boolean silent) {
        StringBuffer content = new StringBuffer("");
        try {
            InputStreamReader isr = encoding == null ? new InputStreamReader(fin) : new InputStreamReader(fin, encoding);
            Reader in = new BufferedReader(isr);
            int ch;
            while ((ch = in.read()) != -1)
                content.append((char) ch);
            in.close();
        } catch (IOException e) {
            if (!silent)
                 m_jotyMessanger.jotyMessage(e);
        }
        return content.length() > 0 ? content.toString() : null;
    }

    public static int strOccurrencesCount(String text, String literal) {
        int retVal = 0;
        int detectorIndex = -1;
        while ((detectorIndex = text.indexOf(literal, detectorIndex + 1)) > -1)
            retVal++;
        return retVal;
    }

    public static String todaySqlExpr(String sqlDateExpr) {
        return String.format(sqlDateExpr, new java.sql.Date(Calendar.getInstance().getTime().getTime()).toString());
    }

    public static String unquote(String singleQuotedText) {
        return singleQuotedText.substring(1, singleQuotedText.length() - 1);
    }

    public static boolean xsdValidate(Document xmlDoc, Source schemaSource, String direction) {
        boolean retVal = false;
        DOMSource xmlSource = new DOMSource(xmlDoc);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Exception exc = null;
        try {
            Schema schema = schemaFactory.newSchema(schemaSource);
            Validator validator = schema.newValidator();
            validator.validate(xmlSource);
            retVal = true;
        } catch (IOException e) {
            exc = e;
        } catch (SAXException e) {
            exc = e;
        }
        if (exc != null)
            m_jotyMessanger.jotyWarning(direction + " : schema validation failed !\n" + exc.getMessage());
        return retVal;
    }

    public static boolean xsdValidate(Document xmlDoc, String schemaFile, String direction) {
        return xsdValidate(xmlDoc, new StreamSource(new File(schemaFile)), direction);
    }

    public static boolean xsdValidate(Document xmlDoc, URL schemaFileUrl, String direction) {
        boolean retVal = false;
        try {
            retVal = xsdValidate(xmlDoc, new StreamSource(schemaFileUrl.openStream()), direction);
        } catch (IOException e) {
            m_jotyMessanger.jotyWarning(direction + " : schema file opening failed !\n");
        }
        return retVal;
    }

    public final static String m_singleByteEncoding = "ISO-8859-1";

    public static String m_encoding;

    public static void composeSelectClauses(StringBuilder target, String filter, String sharing, String sort) {
        boolean whereAlreadyPresent = tailingWhere(target);
        boolean filterPresent = filter != null && filter.length() > 0;
        if (filterPresent)
            target.append((whereAlreadyPresent ? " and " : " where ") + filter);
        if (sharing != null)
            target.append((whereAlreadyPresent || filterPresent ? " and " : " where ") + sharing);
        if (sort != null && sort.length() > 0)
            target.append(" order by " + sort);
    }

    public static boolean tailingWhere(StringBuilder text) {
        boolean retVal = false;
        String flatText = reduceToBlanks(text.toString()).toLowerCase();
        int whereLastOccurrenceIndex = -1;
        int detectorIndex = -1;
        while ((detectorIndex = flatText.indexOf(" where ", whereLastOccurrenceIndex + 1)) > -1)
            whereLastOccurrenceIndex = detectorIndex;
        if (whereLastOccurrenceIndex >= 0) {
            String remainderText = flatText.substring(whereLastOccurrenceIndex);
            retVal = strOccurrencesCount(remainderText, "(") - strOccurrencesCount(remainderText, ")") == 0;
        }
        return retVal;
    }
}
