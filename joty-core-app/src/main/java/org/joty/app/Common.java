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

package org.joty.app;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Stack;
import java.util.Vector;

import org.joty.app.LiteralsCollection.LiteralStructParams;
import org.joty.common.AbstractDbManager;
import org.joty.common.CaselessStringKeyMap;
import org.joty.common.ConfigFile;
import org.joty.common.ICommon;
import org.joty.common.Utilities;
import org.joty.common.XmlTextEncoder;
import org.joty.web.AbstractWebClient;

public class Common implements ICommon {
    JotyApplication m_app;
    public String m_dateFormat;
    public String m_dateTimeFormat;
    public String m_dbmsDateFormat;
    public String m_dbmsDateTimeFormat;

    public String m_thousandsSeparator;
    public String m_decimalsSeparator;
    public String m_emptyDateRendering;

    public String m_emptyDateTimeRendering;
    public String m_xmlDateFormat;
    public String m_currencySymbol;
    public boolean m_currencySymbolAnte;
    public boolean m_currencySymbolSpace;
    public int m_longDigitDim;
    public int m_intDigitDim;
    public int m_moneyDigitDim;
    public int m_fisicalDigitDim;
    public String m_paginationQuery;
    public String m_paginationPageSize;
    public String m_loc_lang;
    public String m_loc_country;
    public ConfigFile m_configuration;
    public boolean m_idFieldAutoIncrement;
    public boolean m_autoIncrementByAddNew;
    public String m_sqlDateExpr;
    public int m_passwordLen;
    public String m_dbmsUserPwdStatement;
    public String m_dbmsChangePwdStatement;
    public String m_dbmsUserSecondaryStatement;
    public String m_dbmsUserGrantedRolesStmnt;
    public boolean m_addRemoveUsers;
    public boolean m_sundayIsFDOW;
    public String m_certAlias;
    public String m_certTesting;
    public String m_authServer;
    public String m_sslPort;
    public boolean m_fieldOrdinality;
    public boolean m_reuseMetadataOnLoadForStore;
    public String m_seq_name;
    public String m_dateSeparator;
    public String m_timeSeparator;
    public String m_languages;
    public boolean m_shared;
    public String m_sharingKeyField;
    public boolean m_useAppOptions;
    public KeyStore m_ks;
    /** holds the mapping between {@code LiteralsCollection} objects and their names */
    public CaselessStringKeyMap<LiteralsCollection> m_literalStructMap;
    /**
     * Sql expression that helps in the
     * definition of the data set of the building LiteralsCollection.
     */
    public String m_literalStructFilter;
    public boolean m_applicationScopeAccessorMode;
    public String m_sharingKey;
    public LiteralsCollection m_literalCollectionInstance;
    public boolean m_dataReLoad;
    public String m_appUrl;
    public String m_servlet;
    public XmlTextEncoder m_xmlEncoder;
    /** if true it drives the framework to operate in ssl mode (web mode) */
    public boolean m_secure;
    /** Holds the content of the {@code /lang/<language>/jotyLang.xml} file. */
    public ConfigFile m_JotyLang;
    /** Holds the content of the {@code /lang/<language>/appLang.xml} file. */
    public ConfigFile m_JotyAppLang;

    public static final String MSG_BUFFERNAME_ALREADY_TAKEN = "%1$s has already been used for another data structure !";
    public static final String MSG_NO_BIRT = "The BIRT report engine is not loaded: check the server installation or the item 'use_BIRT_engine' in the ServerSideJoty.xml file !";

    public LiteralStructParams m_modifiableLsParams;
    public String m_language = "en";
    public String m_userName;
    public String m_password;
    public boolean m_webSessionOn;
    public boolean m_commitExit;

    public Vector<String> m_dows = new Vector<String>();
    public Vector<String> m_months = new Vector<String>();

    public LiteralStructParams modifiableLsParams() {
        return m_modifiableLsParams;
    }

    public Common(JotyApplication app) {
        m_app = app;
        m_literalStructFilter = "";
        m_literalStructMap = new CaselessStringKeyMap<LiteralsCollection>(m_app);
        m_literalCollectionInstance = new LiteralsCollection(m_app);
        m_modifiableLsParams = m_literalCollectionInstance.new LiteralStructParams() {
            {
                modifiable = true;
            }
        };
        m_servlet = "JotyServlet";
    }

    public String emptyDateRendering(boolean withTime) {
        return withTime ? m_emptyDateTimeRendering : m_emptyDateRendering;
    }

    public NumberFormat currencyFormat() {
        return NumberFormat.getCurrencyInstance(new Locale(m_loc_lang, m_loc_country));
    }

    /**
     * Acquires currency notations and separators from language and country
     * values located in {@code m_loc_lang}, {@code m_loc_country} members.
     *
     */
    public void acquireLocaleInfo() {
        NumberFormat currFormat = currencyFormat();
        String probeStr = currFormat.format(345.56);
        m_currencySymbol = currFormat.getCurrency().getSymbol(new Locale(m_loc_lang, m_loc_country));
        int symbolPos = probeStr.indexOf(m_currencySymbol);
        m_currencySymbolAnte = symbolPos == 0;
        m_currencySymbolSpace = probeStr.charAt(m_currencySymbolAnte ? 1 : probeStr.length() - 2) == ' ';
        boolean dotAsDecimalSeparator = probeStr.indexOf(".") > 0;
        m_thousandsSeparator = dotAsDecimalSeparator ? "," : ".";
        m_decimalsSeparator = dotAsDecimalSeparator ? "." : ",";
    }

    public String getDateAuxRendering(String filler, String format) {
        String retVal = null;
        if (format != null) {
            retVal = new String(format);
            retVal = retVal.replace("d", filler);
            retVal = retVal.replace("M", filler);
            retVal = retVal.replace("y", filler);
            retVal = retVal.replace("H", filler);
            retVal = retVal.replace("m", filler);
            retVal = retVal.replace("s", filler);
        }
        return retVal;
    }

    public String getEmptyDateRendering() {
        return getDateAuxRendering(" ", m_dateFormat);
    }

    public String getEmptyDateTimeRendering() {
        return getDateAuxRendering(" ", m_dateTimeFormat);
    }

    public boolean getConfBool(String literal) throws ConfigFile.ConfigException {
        return Boolean.parseBoolean(getConfStr(literal));
    }

    public int getConfInt(String literal) throws ConfigFile.ConfigException {
        return Integer.parseInt(getConfStr(literal));
    }

    public String getConfStr(String literal) throws ConfigFile.ConfigException {
        return m_configuration.configTermValue(literal);
    }

    public void acquireLocsFromConf() throws ConfigFile.ConfigException {
        m_loc_lang = getConfStr("loc_lang");
        m_loc_country = getConfStr("loc_country");
    }

    public boolean loadConfigProperties(boolean webMode) {
        boolean retVal = true;
        try {
            String jdbcClassName = getConfStr("jdbcDriverClass");
            boolean msSqlServer = Utilities.isMsSqlServer(jdbcClassName);
            m_idFieldAutoIncrement = getConfBool("autoIncrementID");
            m_autoIncrementByAddNew = getConfBool("autoIncrementByAddNew");
            m_sqlDateExpr = getConfStr("sqlDateExpr");
            m_dateFormat = getConfStr("dateFormat");
            m_dateTimeFormat = getConfStr("dateTimeFormat");
            m_dbmsDateFormat = getConfStr("dbmsDateFormat");
            m_dbmsDateTimeFormat = getConfStr("dbmsDateTimeFormat");
            m_emptyDateRendering = getEmptyDateRendering();
            m_emptyDateTimeRendering = getEmptyDateTimeRendering();
            m_paginationPageSize = getConfStr("pageSize");
            m_passwordLen = getConfInt("passwordLen");
            m_dbmsUserPwdStatement = getConfStr("dbmsUserPwdStatement");
            m_dbmsChangePwdStatement = m_configuration.configTermValue("dbmsChangePwdStatement", jdbcClassName.indexOf("Oracle") >= 0);
            m_dbmsUserSecondaryStatement = m_configuration.configTermValue("dbmsUserSecondaryStatement", !msSqlServer);
            m_dbmsUserGrantedRolesStmnt = getConfStr("dbmsUserGrantedRolesStmnt");
            m_addRemoveUsers = getConfBool("addRemoveUsers");
            m_longDigitDim = 20;
            m_intDigitDim = 15;
            m_moneyDigitDim = 15;
            m_fisicalDigitDim = 15;
            m_sundayIsFDOW = getConfBool("sundayIsFDOW");
            if (webMode) {
                m_certAlias = getConfStr("certificateAlias");
                m_certTesting = getConfStr("certTesting");
                m_authServer = m_configuration.configTermValue("authServer", true);
                m_sslPort = getConfStr("sslPort");
                m_xmlDateFormat = getConfStr("xmlDateFormat");
                m_fieldOrdinality = getConfBool("fieldOrdinality");
                m_reuseMetadataOnLoadForStore = getConfBool("reuseMetadataOnLoadForStore");
            }
            m_seq_name = getConfStr("defaultSeqName");
            if (m_emptyDateRendering.length() != 10 || m_emptyDateTimeRendering.length() != 19)
                m_app.jotyMessage("Date and date-time formats must be respectively 10 and 19 characters long !");
            else {
                m_dateSeparator = m_emptyDateRendering.trim().substring(0, 1);
                m_timeSeparator = m_emptyDateTimeRendering.trim().substring(14, 15);
            }
            m_languages = getConfStr("languages");
            m_shared = getConfBool("shared");
            m_sharingKeyField = getConfStr("sharingKeyField");
            m_useAppOptions = getConfBool("useAppOptions");
        } catch (ConfigFile.ConfigException e) {
            retVal = false;
        }
        return retVal;
    }

    public NumberFormat numberFormat() {
        return NumberFormat.getNumberInstance(new Locale(m_loc_lang, m_loc_country));
    }

    public String currencySymbol() {
        return m_currencySymbol;
    }

    public boolean currencySymbolAnte() {
        return m_currencySymbolAnte;
    }

    public boolean currencySymbolSpace() {
        return m_currencySymbolSpace;
    }

    public String defDateFormat() {
        return m_dateFormat;
    }

    public String defDateTimeFormat() {
        return m_dateTimeFormat;
    }

    public int moneyDigitDim() {
        return m_moneyDigitDim;
    }

    public int intDigitDim() {
        return m_intDigitDim;
    }

    public int longDigitDim() {
        return m_longDigitDim;
    }

    public int fisicalDigitDim() {
        return m_fisicalDigitDim;
    }

    public String paginationQuery() {
        return m_paginationQuery;
    }

    public String paginationPageSize() {
        return m_paginationPageSize;
    }

    /**
     * Accesses in writing and reading mode, chunks of data organized as
     * {@code Map<String, Object>} and saved on disk, by means of the Java serialization
     * features.
     * 
     * The location of the accessed file is determined by the
     * {@code userHomeDataPath} method.
     *
     * @param fileName the filename a part from the extension that is 'ser'
     * @param object   the data to be written or null if data is to be read
     * @return null in writing mode and the got data in the reading mode
     */
    public Object accessLocalData(String fileName, Object object) {
        Object retVal = null;
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        String filePath = m_app.localFilesPath() + "/" + fileName + ".ser";
        try {
            if (object == null) {
                in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)));
                retVal = in.readObject();
                in.close();
            } else {
                out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
                out.writeObject(object);
                out.close();
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            m_app.jotyMessage(e);
        } catch (ClassNotFoundException e) {
            m_app.jotyMessage(e);
        }
        return retVal;
    }

    public class JotyStack<T> {
        Stack<T> m_stack = new Stack<T>();

        public T top() {
            return m_stack.isEmpty() ? null : m_stack.peek();
        }

        public void push(T elem) {
            m_stack.push(elem);
        }

        public void pop() {
            if (!m_stack.isEmpty())
                m_stack.pop();
        }

        public int size() {
            return m_stack.size();
        }
    }

    /**
     * as its name says ...
     *
     * @return true on success
     */
    public boolean saveBytesAsFile(byte[] bytes, String dir, String fileName, boolean temporary) {
        boolean success = true;
        File file = new File(dir, fileName);
        if (temporary)
            file.delete();
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes, 0, bytes.length);
            fos.close();
        } catch (IOException e) {
            success = false;
            m_app.jotyMessage(e);
        }
        if (temporary)
            file.deleteOnExit();
        return success;
    }

    /** Get the keyStore from disk */
    public boolean loadKeyStore() {
        boolean retVal = false;
        try {
            m_ks = KeyStore.getInstance(m_app.getKeyStoreType());
            retVal = true;
        } catch (KeyStoreException e) {
            m_app.jotyMessage(e);
        }
        if (retVal)
            try {
                m_app.firstChanceKeyStore();
            } catch (Throwable th) {
                try {
                    m_ks.load(new FileInputStream(m_app.keyStorePath()), "jotyCert".toCharArray());
                } catch (Throwable th2) {
                    try {
                    	if (m_app.getKeyStoreType().compareToIgnoreCase("JKS")==0)
                			m_ks.load(new FileInputStream(System.getProperty("java.home") + "/lib/security/cacerts"), "changeit".toCharArray());
                    	else
                    		m_ks.load(null, null);
                    } catch (Throwable th3) {
                        m_app.jotyMessage(th3);
                        retVal = false;
                    }
                }
            }
        return retVal;
    }

    /** Writes the keyStore on disk */
    public boolean storeKeyStore() {
        boolean retVal = false;
        try {
            m_ks.store(new FileOutputStream(m_app.keyStorePath()), "jotyCert".toCharArray());
            retVal = true;
        } catch (Throwable th) {
            m_app.jotyMessage(th);
        }
        return retVal;
    }

    /**
     * Returns a newly instantiated {@code LiteralsCollection} object or the
     * pre-existing one.
     *
     * @param name
     * @param withStrKey
     * @return the {@code LiteralsCollection}
     */

    public LiteralsCollection createLiteralsCollection(String name, boolean withStrKey) {
        boolean literalAlreadyUsed = false;
        LiteralsCollection literalStruct = m_literalStructMap.get(name);
        if (literalStruct != null) {
            if (m_app.debug()) {
                if (m_literalStructFilter.length() == 0) {
                    String msg = String.format(MSG_BUFFERNAME_ALREADY_TAKEN, name);
                    m_app.jotyMessage(msg);
                }
                literalAlreadyUsed = true;
            }
            literalStruct.clear();
        }
        if (!literalAlreadyUsed) {
            literalStruct = m_app.instantiateLiteralsCollection(m_app);
            m_literalStructMap.put(name, literalStruct);
        }
        literalStruct.init(name, withStrKey);
        return literalStruct;
    }

    /**
     * Gets a description value from a {@code LiteralStruct} object basing on
     * integer identifier on which is mapped.
     *
     * @param literalStruct
     *            {@code LiteralStruct} object
     * @param itemData
     *            integer identifier
     */
    String descrByCode(LiteralsCollection literalStruct, int itemData) {
        String retVal = "";
        if (itemData >= 0) {
            Integer posIdx;
            posIdx = literalStruct.m_descrReverseMap.get(itemData);
            if (posIdx != null)
                retVal = literalStruct.m_descrArray.get(posIdx).descr;
        }
        return retVal;
    }

    /**
     * Gets a description value from a {@code LiteralStruct} object basing on
     * String identifier on which is mapped in the special case in which the
     * LiteralStruct object hase been built by means of a textual key.
     *
     * @param literalsCollection
     *            {@code LiteralsCollection} object
     * @param itemData
     *            integer identifier
     */
    String descrByCode(LiteralsCollection literalsCollection, String itemData) {
        String retVal = "";
        if (itemData.length() > 0) {
            Integer posIdx;
            posIdx = literalsCollection.m_strKeyRevMap.get(itemData);
            if (posIdx != null)
                retVal = literalsCollection.m_descrArray.get(posIdx).descr;
        }
        return retVal;
    }


    /**
     * see {@link #literalStruct(String) and
     * {@link #descrByCode(LiteralsCollection, int)}
     */
    String descrByCode(String descrLiteral, int itemData) {
        return descrByCode(literalStruct(descrLiteral), itemData);
    }

    /**
     * see {@link #literalStruct(String) and
     * {@link #descrByCode(LiteralsCollection, String)}
     */
    String descrByCode(String descrLiteral, String itemData) {
        return descrByCode(literalStruct(descrLiteral), itemData);
    }

    /**
     * Gets a {@code LiteralStruct} object from the {@code m_literalStructMap} member
     * accessing it by name.
     *
     * @param name
     *            the given name
     * @return the got object
     */
    public LiteralsCollection literalStruct(String name) {
        LiteralsCollection struct = m_literalStructMap.get(name);
        if (struct == null && m_app.debug()) {
            String msg = String.format("Descriptions literal %1$s not found !", name);
            m_app.jotyMessage(msg);
        }
        return struct;
    }

    /**
     * Prepares data for loading them into a LiteralsCollection object
     *
     * @param tabName       if the {@code m_applicationScopeAccessorMode} member variable
     *                      is true it is the literal for the name substitution stored in
     *                      application {@code Accessor} object, else it is directly the
     *                      database table name.
     * @param keyField      the 'id' field name.
     * @param literalField  the 'description' field name
     * @param literalStruct the {@code LiteralStruct} object.
     * @param lsParams      a {@code LiteralStructParams} object as a vehicle for most
     *                      parameters needed.
     * @return the sql statement to be used for inquiring the database
     */
    public String prepareToLoadIntoLiteralStruct(String tabName, String keyField, String literalField, LiteralsCollection literalStruct, LiteralStructParams lsParams) {
        if (lsParams.withBlank)
            literalStruct.addLiteral(-1, "", null);
        String tableName = m_applicationScopeAccessorMode ? "<JOTY_CTX>" : tabName;
        String sqlStmnt = lsParams.selectStmnt != null ?
                lsParams.selectStmnt :
                lsParams.strKeyFldName == null ?
                        String.format("SELECT %s, %s FROM %s", keyField, literalField, tableName) :
                        String.format("SELECT * FROM %s", tableName);
        boolean preExisting = false;
        if (!m_literalStructFilter.isEmpty()) {
            sqlStmnt += " WHERE " + m_literalStructFilter;
            preExisting = true;
        }
        if (lsParams.modifiable && m_shared) {
            sqlStmnt += preExisting ? " AND " : " WHERE ";
            sqlStmnt += sharingClause();
        }
        String contrib = String.format(" ORDER BY %1$s ", lsParams.sortedByID ? keyField : literalField);
        sqlStmnt += contrib;
        return sqlStmnt;
    }

    /** Forms the sub clause to be added to the selecting statements when the application runs in a "shared" asset. */
    public String sharingClause() {
        return " " + m_sharingKeyField + " = '" + m_sharingKey + "'";
    }


    /**
     * Assembles data and metadata for loading literals from db
     * 
     * If the {@code LiteralStruct} object is not passed as parameter it checks its
     * existence in the map {@code m_literalMap} and, if found, gets it, else a
     * new instance is created and is put in the map.
     *
     * @param tabName            the database table
     * @param keyField           the database field hosting the id
     * @param literalField       the database field hosting the description
     * @param name               the name of the structured object
     * @param paramLiteralStruct possible already instantiated LiteralStruct object
     * @param prmLsParams        {@code LiteralStructParams} object carrying parameters
     *                           *
     * @see org.joty.app.LiteralsCollection
     * @see org.joty.app.LiteralsCollection.LiteralStructParams
     */
    public LiteralsCollectionData buildLiteralStructMain(String tabName, String keyField, String literalField, String name, LiteralsCollection paramLiteralStruct, LiteralStructParams prmLsParams) {
        LiteralsCollectionData retVal = null;
        if (!m_app.isDesignTime()) {
            retVal = new LiteralsCollectionData();
            retVal.lsParms = prmLsParams == null ? m_literalCollectionInstance.new LiteralStructParams() : prmLsParams;
            if (prmLsParams == null)
                retVal.lsParms.sortedByID = false;
            retVal.literalsCollection = paramLiteralStruct == null ? m_literalStructMap.get(name) : paramLiteralStruct;
            boolean byStrKey = retVal.lsParms.strKeyFldName != null;
            if (retVal.literalsCollection == null)
                retVal.literalsCollection = createLiteralsCollection(name, byStrKey);
            else if (m_dataReLoad)
                retVal.literalsCollection.clear();
            else
                retVal.literalsCollection.init(name, byStrKey);
        }
        return retVal;
    }

    public class LiteralsCollectionData {
        public LiteralStructParams lsParms;
        public LiteralsCollection literalsCollection;
    }

    public boolean setApplicationScopeAccessorMode() {
        return setApplicationScopeAccessorMode(null);
    }

    /**
     * Switches the framework to use the {@code Accessor} object. This coming into play for the code
     * of the JotyApplication implemented instance that relies on it.
     *
     */
    public boolean setApplicationScopeAccessorMode(Boolean sourceTruth) {
        boolean oldValue = m_applicationScopeAccessorMode;
        m_applicationScopeAccessorMode = sourceTruth == null ? true : sourceTruth;
        return oldValue;
    }

    public boolean setSecure(boolean truth) {
        boolean retVal = m_secure;
        m_secure = truth;
        return retVal;
    }

    public String languageItem(String literal, ConfigFile langCF) {
        try {
            return m_app.isDesignTime() ? literal : langCF.configTermValue(literal);
        } catch (ConfigFile.ConfigException e) {
            return null;
        }
    }

    /**
     * It catches from the map {@code m_JotyLang} the text corresponding to the
     * literal received as argument
     *
     * @param literal
     * @return the text caught
     *
     * @see #m_JotyLang
     */
    public String jotyLang(String literal) {
        return languageItem(literal, m_JotyLang);
    }

    /**
     * It catches from the map {@code m_JotyAppLang} the text corresponding to the
     * literal received as argument
     *
     * @param literal
     * @return the text caught
     *
     * @see #m_JotyAppLang
     */
    public String appLang(String literal) {
        return languageItem(literal, m_JotyAppLang);
    }

    public String langMessage(String langLiteral, boolean appSpecific) {
        return appSpecific ? appLang(langLiteral) : jotyLang(langLiteral);
    }

    public void loadCalendarElems() {
        m_dows.clear();
        m_months.clear();
        if (m_sundayIsFDOW)
            m_dows.add(jotyLang("Sunday"));
        m_dows.add(jotyLang("Monday"));
        m_dows.add(jotyLang("Tuesday"));
        m_dows.add(jotyLang("Wednesday"));
        m_dows.add(jotyLang("Thurdsday"));
        m_dows.add(jotyLang("Friday"));
        m_dows.add(jotyLang("Saturday"));
        if (!m_sundayIsFDOW)
            m_dows.add(jotyLang("Sunday"));

        m_months.add(jotyLang("January"));
        m_months.add(jotyLang("February"));
        m_months.add(jotyLang("March"));
        m_months.add(jotyLang("April"));
        m_months.add(jotyLang("May"));
        m_months.add(jotyLang("June"));
        m_months.add(jotyLang("July"));
        m_months.add(jotyLang("August"));
        m_months.add(jotyLang("September"));
        m_months.add(jotyLang("October"));
        m_months.add(jotyLang("November"));
        m_months.add(jotyLang("December"));
    }

    public void checkAndThrow(AbstractDbManager abstractDbManager, String reason, String code) throws JotyException {
        if (abstractDbManager.dbExceptionCheck(reason, code, AbstractDbManager.ExcCheckType.INVALID_CREDENTIALS))
            throw new JotyException(JotyException.reason.INVALID_CREDENTIALS, null, m_app);
        else if (abstractDbManager.dbExceptionCheck(reason, code, AbstractDbManager.ExcCheckType.CONSTR_VIOLATION_ON_UPDATE))
            throw new JotyException(JotyException.reason.CONSTR_VIOLATION_ON_UPDATE, reason, m_app);
        else if (abstractDbManager.dbExceptionCheck(reason, code, AbstractDbManager.ExcCheckType.CONSTR_VIOLATION_ON_DELETE))
            throw new JotyException(JotyException.reason.CONSTR_VIOLATION_ON_DELETE, reason, m_app);
        else if (abstractDbManager.dbExceptionCheck(reason, code, AbstractDbManager.ExcCheckType.DBMS_CREATEUSER_FAILURE))
            throw new JotyException(JotyException.reason.DBMS_CREATEUSER_FAILURE, reason, m_app);
    }

    /** see {@link AbstractWebClient#buildingRemoteTransaction_reset()}  */
    @Override
    public void resetRemoteTransactionBuilding() {
        if (m_app.getWebClient() != null)
            m_app.getWebClient().buildingRemoteTransaction_reset();
    }

}
