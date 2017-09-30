/*
	Copyright (c) 2015-2017, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.util.Base64;
import android.view.Menu;
import android.webkit.MimeTypeMap;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.joty.app.Common;
import org.joty.app.Common.*;
import org.joty.app.JotyApplication;
import org.joty.app.JotyException;
import org.joty.app.LiteralsCollection;
import org.joty.app.LiteralsCollection.LiteralStructParams;
import org.joty.common.AbstractDbManager;
import org.joty.common.ApplMessenger;
import org.joty.common.BasicPostStatement;
import org.joty.common.CaselessStringKeyMap;
import org.joty.common.ConfigFile;
import org.joty.common.ErrorCarrier;
import org.joty.common.JotyMessenger;
import org.joty.common.JotyTypes;
import org.joty.common.LangLiteralRetCodeMapper;
import org.joty.common.ParamContext;
import org.joty.common.ReportManager;
import org.joty.common.Utilities;
import org.joty.common.Utilities.Stocker;
import org.joty.common.XmlTextEncoder;
import org.joty.common.ICommon;
import org.joty.data.JotyDate;
import org.joty.data.SearchQueryBuilderFront;
import org.joty.data.WrappedField;
import org.joty.gui.WFieldSet;
import org.joty.mobile.R;
import org.joty.mobile.authorization.ChangePasswordActivity;
import org.joty.mobile.authorization.LoginActivity;
import org.joty.mobile.data.JotyCursorWrapper;
import org.joty.mobile.data.JotyDB;
import org.joty.mobile.data.WResultSet;
import org.joty.mobile.gui.DataDetailsActivity;
import org.joty.mobile.gui.DataMainActivity;
import org.joty.mobile.gui.IdleActivity;
import org.joty.mobile.gui.JotyActivity;
import org.joty.mobile.gui.MenuActivity;
import org.joty.mobile.gui.ServerUrlActivity;
import org.joty.mobile.gui.StartUrlActivity;
import org.joty.mobile.gui.Term;
import org.joty.mobile.web.WebClient;
import org.joty.mobile.web.WebConn;
import org.joty.web.AbstractWebClient;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

/**
 * Most of the methods of this class have been derived from the ones with the same name in the
 * {@code org.joty.workstation.app.Application}
 * class through the use of the completely new crucial inner class {@code ResponseHandlersManager}.
 * So that, in the case of missing documentation of methods of the {@code JotyApp} class look for
 * possible methods with the same name in the workstation counterpart and read the documentation
 * of those methods together with the explanation of the {@code ResponseHandlersManager} class here given.
 * <p/>
 * The authentication and initialization process has been decomposed right by the use of the
 * {@code ResponseHandlersManager} to fulfill both the Joty 2.0 requirements and the Android
 * responsiveness constraint.
 *
 * If a corresponding method is found in the workstation counterpart here the developer must expect also,
 * typically, a further method that takes care of the asynchronous handling of the response coming
 * from the Joty Server.
 *
 * @see ResponseHandlersManager
 * @see QueryResponseHandlersManager
 */
public class JotyApp extends android.app.Application implements ApplMessenger, JotyApplication {

    public enum JotyMenus {LangMenu, chgPwdMenu, ExitMenu, DeleteMenu, SearchMenu, NewRecMenu, EditMenu, SaveMenu, CancelMenu, HomeMenu, NextMenu, PrevMenu}

    public static JotyApp m_app;
    public JotyActivity m_activity;
    public boolean m_firstActivityActivation = false;
    public boolean m_nextActivityActivation = false;
    public Class m_idleActivityClass;
    private String m_webClientClass;
    public String m_messageText;
    /** maintains, along the application, a map of {@code ResponseHandlersManager} objects based on
     * the name of the working context (for example a JotyActivity object) which the ResponseHandlersManager is built for.
     */
    public HashMap<String, ResponseHandlersManager> m_respManagerCatalog;
    /** @see RespManagerCountProvider */
    public RespManagerCountProvider m_respManagerCounters;
    public boolean m_inited;
    public ResponseHandlersManager m_currentRespHandlersManager;
    private String m_ksFileName = "ks";
    private boolean m_dataLoaded;
    public boolean m_configDataLoaded;
    public boolean m_langLoaded;
    public boolean m_langChange;
    private boolean m_wasInMemoryYet;
    public Menu m_menu;
    public boolean m_menuBuilt;
    public int m_maxUsedMenuItemID;
    public int m_currentlySelectedLangMenuItemId;
    public boolean m_home;
    public JotyStack<JotyActivity> m_dataMainActivityStack;
    public JotyStack<JotyActivity> m_dataDetailsActivityStack;
    public JotyStack<JotyActivity> m_dataResultActivityStack;

    public boolean m_dataModified;
    public MenuActivity m_contextActivity;
    public IdleActivity m_idleActivity;
    public JotyToast m_currToast;
    public Common m_common;

    public PasswordValidator m_passwordValidator;
    public String m_name;
    public JotyDB m_db;
    public String m_versionString;
    public String m_startPath;
    public String m_defaultStartPath;
    public boolean m_startPathLocallyStored;
    public String m_testPath;
    public boolean m_testing;

    public WebClient m_webClient;
    public Random m_random;

    public AbstractDbManager m_dbManager;
    public ErrorCarrier m_errorCarrier;

    public boolean m_committedClose;

    public boolean m_alreadyCertDeletionOffered;
    public ReportManager m_reportManager;

    public static boolean m_debug = true;

    public Stocker m_userRoles;

    public CaselessStringKeyMap<HashSet<String>> m_refreshMap;
    public ParamContext m_paramContext;

    /**
     * If true the {@code Accessor} object lives within the Joty Server instead of in the Application object.
     */
    public boolean m_remoteAccessor;
    public ParamContext m_webTransPrmContext;
    public Map<String, String> m_applicationPreferences;
    public Vector<Image> m_iconImages;
    public boolean m_connected;
    public boolean m_loginAbandoned;
    public boolean m_loginValid;
    public boolean m_forLostSession;

    protected String m_configuredUser;

    String m_serverUrl;
    boolean m_xmlLogging;
    int m_iReTryCount;
    int m_iReTryMax;
    public final String m_applicationPreferencesFile = "ApplicationPreferences";

    private LangLiteralRetCodeMapper m_langLiteralRetCodeMapper;

    static boolean m_tracing;

    private CaselessStringKeyMap<Stocker> m_reportsRolePerms;

    /**
     * Provides and imposes the needs to define an handling method for the response coming from the
     * Joty Server upon a command/commands forwarded by the hosting manager of type {@code ResponseHandlersManager}.
     * It holds a count down counter to face with the waiting of several responses arrivals.
     *
     * @see ResponseHandlersManager
     */
    public abstract class ResponseHandler {
        int m_downCounter;

        public abstract void handle(boolean result, ResponseHandlersManager respManager);

        public ResponseHandler(int downCounter) {
            m_downCounter = downCounter;
        }

        public ResponseHandler() {
            this(0);
        }
    }

    /**
     * Derived from {@code ResponseHandler} provides an implementation method focused to the
     * WResultSet object that is managed by the manager.
     */

    public abstract class QueryResponseHandler extends ResponseHandler {
        protected ResponseHandlersManager m_respManager;

        public void handle(boolean result, ResponseHandlersManager respManager) {
            m_respManager = respManager;
            QueryResponseHandlersManager qRespManager = (QueryResponseHandlersManager) respManager;
            handleQuery(result, qRespManager.m_rs, qRespManager.m_postStatement);
        }

        public abstract void handleQuery(boolean result, WResultSet rs, BasicPostStatement postStatement);

        public QueryResponseHandler() {
            super();
        }

    }

    /**
     * Provides comfortable get method for a counters map, having the name of the context used as key.
     */
    public class RespManagerCountProvider {
        HashMap<String, Integer> counters = new HashMap<String, Integer>();

        public int get(String key) {
            Integer retVal = counters.get(key);
            if (retVal == null)
                retVal = 1;
            else
                retVal++;
            counters.put(key, retVal);
            return retVal;
        }

        public void clear() {
            counters.clear();
        }
    }

    /**
     * ResponseHandlersManager is a core class of Joty 2.0 Mobile:
     * with the {@code org.joty.mobile.web.WebConn.Connector} it helps the developer to assemble
     * the code for long running operations that must be performed over the network.
     * All the typical accesses to the Joty Server, either they be disposing or only querying the
     * database, pass through the use of an instance of this class.
     * <p/>
     * The class allows the definition of what is to be forwarded to the server and collects a stack
     * of {@code org.joty.mobile.app.JotyApp.ResponseHandler} abstract class implementations that
     * represent, in their entirety, the chain of processing of the response got from the server.
     * The WebConn.Connector object, on its asynch branch, starts the response handling process, driving
     * the manager to peek the handler at the top of the stack, to execute its handler method implementation
     * and then, to check if another handler does exist down along the stack and so on ...until the stack is empty.
     * <p/>
     * The instance of the manager, before being passed to synchro part of the WebConn.Connector object,
     * can collect many ResponseHandler objects;
     * The {@code m_params} member hosts possible named object values to allow communications either in
     * the forward or backward processing. Each manager instance, since the backward processing flow is
     * completely independent from the forwarding flow, must be referenced properly and, indeed, is
     * stored in the {@code JotyApp.m_respManagerCatalog} member and is referenced by its name that may
     * be composed by the name of the response handling context plus the counting of the instance of it.
     * <p/>
     * The instance count of the backward processing context (typically an Activity), needs to be hosted
     * inside for proper referencing of the embedded ResponseHandlersManager; the counting is made in
     * the {@code JotyApp.m_respManagerCounters} member.
     * <p/>
     * The class can be used also to send many commands to the server and to configure a one only (overall) handling action,
     * that "waits" for the arrival of all the responses from the server: this is obtained by storing a
     * "count down" counter in the ResponseHandler object that is set in the configuration section of
     * the manager, when the ResponseHandler object is pushed into the stack: in the response handling
     * phase the manager will check this counter : if greater than zero the counter will be decreased else
     * the handling will take place; see the {@code checkToExecute} method.
     * <p>
     * Thanks to its structure and behavior the class can be used also in any "callback" scenario that,
     * even without involving the communication with the Joty Server, requires the decoupling of the executor
     * from the code that uses the result of the execution: for an example of such a usage see the
     * {@code tryChangePassword} method.
     *
     *
     * @see ResponseHandler
     * @see RespManagerCountProvider
     */

    public class ResponseHandlersManager {
        Stack<ResponseHandler> m_responseHandlers;
        CaselessStringKeyMap<Object> m_params;
        ResponseHandlersManager m_parentResponseHandlersManager;
        boolean m_parentResponseHandlerManagerChecked;
        public boolean m_exceptionalResponse;
        public WebConn m_webConn;

        public ResponseHandlersManager() {
            this(null);
        }

        public ResponseHandlersManager(ResponseHandlersManager parentResponseHandlersManager) {
            m_parentResponseHandlersManager = parentResponseHandlersManager;
            m_responseHandlers = new Stack<ResponseHandler>();
            m_params = new CaselessStringKeyMap<Object>(JotyApp.this);
            m_params.setOverWritable();
            m_parentResponseHandlerManagerChecked = false;
            m_exceptionalResponse = false;
        }

        public void push(ResponseHandler respHandler) {
            m_responseHandlers.push(respHandler);
        }

        public void setParam(String name, Object value) {
            m_params.put(name, value);
        }

        public Object getParam(String name) {
            return m_params.get(name);
        }

        public void popAndcheckToExecute(boolean result) {
            m_responseHandlers.pop();
            checkToExecute(result);
        }

        public void checkToExecute(boolean result) {
            if (!m_responseHandlers.empty()) {
                ResponseHandler respHandler = m_responseHandlers.peek();
                if (respHandler.m_downCounter == 0) {
                    respHandler.handle(result, this);
                    if (m_parentResponseHandlersManager != null && !m_parentResponseHandlerManagerChecked) {
                        m_parentResponseHandlersManager.checkToExecute(result);
                        m_parentResponseHandlerManagerChecked = true;
                    }
                } else
                    respHandler.m_downCounter--;
            }
        }

        /**
         * Increments the count down counter of the {@code ResponseHandler} object located at the top
         * of the stack.
         *
         * @param byUnits the quantity (added to the one currently stored) of the response events upon
         *                which tha manager will wait (longer) instead of invoking the {@code handle} method.
         */
        public void topHandlerIncrementCounter(int byUnits) {
            ResponseHandler respHandler = m_responseHandlers.peek();
            if (respHandler != null)
                respHandler.m_downCounter += byUnits;
        }

    }

    /**
     * Derived from {@code }ResponseHandlersManager} it specializes its abilities in order to use a
     * {@code }WResultSet} object, the opening of which composes the forwarding part of the process while the
     * getting of the result set constitutes the handling part.
     * <p/>
     * Its constructors accept only a {@code QueryResponseHandler} object that is store as the base of
     * the stack of handlers;
     * <p/>
     * For providing the compatibility with the paradigm of the Joty "protocol", a
     * {@code BasicPostStatement} is accepted: beyond providing a parameter for opening the WResultSet, it is hosted
     * for later use in the response handling section, when the {@code QueryResponseHandler.handleQuery}
     * method will be invoked.
     *
     * @see QueryResponseHandler
     */

    public class QueryResponseHandlersManager extends ResponseHandlersManager {
        public WResultSet m_rs;
        String m_sql;
        QueryResponseHandler m_respHandler;
        BasicPostStatement m_postStatement;
        public boolean m_forPagination, m_backward;
        public Stocker m_smallBlobs;

        public QueryResponseHandlersManager(String sqlQuery, QueryResponseHandler respHandler, WResultSet rs) {
            m_rs = rs;
            m_sql = sqlQuery;
            m_respHandler = respHandler;
            init();
        }

        public QueryResponseHandlersManager(String sqlQuery, QueryResponseHandler respHandler) {
            this(sqlQuery, respHandler, null);
        }

        public QueryResponseHandlersManager(BasicPostStatement postStatement, QueryResponseHandler respHandler, WResultSet rs) {
            m_rs = rs;
            m_sql = null;
            m_postStatement = postStatement;
            m_respHandler = respHandler;
            init();
        }

        public QueryResponseHandlersManager(BasicPostStatement postStatement, QueryResponseHandler respHandler) {
            this(postStatement, respHandler, null);
        }

        public void open() {
            open(false);
        }

        public void open(boolean forMetadataOnly) {
            m_rs.setSmallBlobsList(m_smallBlobs);
            m_rs.open(forMetadataOnly, m_postStatement, this);
        }

        void init() {
            if (m_rs == null)
                m_rs = new WResultSet(null, m_sql);
            push(m_respHandler);
        }

        public void setSmallBlobsList(Stocker list) {
            m_smallBlobs = list;
        }
    }

    public class JotyToast {
        boolean m_interruptable = false;
        public Toast m_toast;

        public void cancel() {
            m_toast.cancel();
            m_currToast = null;
        }

        public void show() {
            if (m_currToast != null && m_currToast.isInterruptable())
                m_currToast.cancel();
            m_toast.show();
            m_currToast = this;
        }

        public JotyToast(String text, boolean shortLen, boolean interruptable) {
            m_toast = Toast.makeText(m_activity.getBaseContext(), text, shortLen ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
            m_interruptable = interruptable;
            show();
        }

        public boolean isInterruptable() {
            return m_interruptable;
        }
    }


    @Override
    public void jotyWarning(String text) {
        JotyApp.m_app.toast(text);
    }

    @Override
    public void jotyMessage(String text) {
        JotyApp.m_app.warningMsg(text);
    }

    @Override
    public void ASSERT(boolean predicate) {
        if (m_debug && !predicate)
            m_app.warningMsg("ASSERTION VIOLATED");
    }

    @Override
    public String localFilesPath() {
        return getFilesDir().toString();
    }

    @Override
    public void jotyMessage(Throwable t) {
        JotyApp.m_app.toast(t.toString());
    }

    @Override
    public void jotyMessage(Exception e) {
        JotyApp.m_app.toast(e.toString());
    }


    @Override
    public JotyDate createDate() {
        return new JotyDate(this);
    }



    public DataMainActivity dataMainActivity() {
        return (DataMainActivity) m_dataMainActivityStack.top();
    }

    void setIdle(boolean idle) {
        m_activity.setWaitCursor(!idle);
    }

    public void checkDataLoaded() {
        if (m_dataLoaded)
            setIdle(true);
        else {
            setIdle(false);
            m_app.doLoadData();
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        setIdleActivityClass(IdleActivity.class);
    }

    public void setIdleActivityClass(Class theClass) {
        m_idleActivityClass = theClass;
    }

    public void exit() {
        if (!m_common.m_commitExit)
            m_common.m_commitExit = true;
        if (m_activity != null)
            m_activity.finish();
        m_firstActivityActivation = false;
        m_nextActivityActivation = false;
        if (m_webClient != null)
            m_webClient.endSession();
    }

    public interface PasswordValidator {
        boolean validate(String password);
    }

    public class ClauseContribution implements SearchQueryBuilderFront.TermContributor {
        @Override
        public String sqlValueExpr(WrappedField term) {
            return ((Term) term).sqlValueExpr();
        }

        @Override
        public String getOperator(WrappedField term, String matchOperator) {
            RadioGroup opGroup = ((Term) term).m_operatorsRadioGroup;
            String operator;
            if (opGroup == null)
                operator = matchOperator;
            else
                operator = ((RadioButton) m_app.m_activity.findViewById(opGroup.getCheckedRadioButtonId())).getText().toString();
            return operator;
        }
    }

    public void toast(String text, boolean shortLen, boolean interruptable) {
        new JotyToast(text, shortLen, interruptable);
    }

    public void toast(String text, boolean shortLen) {
        toast(text, shortLen, false);
    }

    public void toast(String text) {
        toast(text, true);
    }

    public static void exceptionToToast(Exception e) {
        m_app.resetRemoteTransactionBuilding();
        String logText = e.toString() == null ? e.getMessage() : e.toString();
        if (logText != null)
            m_app.toast(logText);
    }

    public static void throwableToToast(Throwable th) {
        m_app.resetRemoteTransactionBuilding();
        if (th instanceof Exception)
            exceptionToToast((Exception) th);
        else
            m_app.toast(th.toString());
    }

    enum msgType {QUESTION_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE}

    ;


    private void message(msgType type, String text, DialogInterface.OnClickListener dialogClickListener) {
        int retVal = 0;
        AlertDialog.Builder builder = new AlertDialog.Builder(m_activity);
        builder.setTitle(m_name);
        switch (type) {
            case QUESTION_MESSAGE:
                builder.setMessage(text).setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener);
                break;
            case INFORMATION_MESSAGE:
            case WARNING_MESSAGE:
                builder.setMessage(text).setCancelable(false).setPositiveButton("Ok", dialogClickListener);
                break;
        }
        builder.show();
    }

    public void yesNoQuestion(String text, DialogInterface.OnClickListener dialogClickListener) {
        message(msgType.QUESTION_MESSAGE, text, dialogClickListener);
    }

    public void langYesNoQuestion(String literal, DialogInterface.OnClickListener dialogClickListener) {
        yesNoQuestion(jotyLang(literal), dialogClickListener);
    }

    public void warningMsg(String text) {
        warningMsg(text, null);
    }

    public void warningMsg(String text, DialogInterface.OnClickListener dialogClickListener) {
        DialogInterface.OnClickListener listener = dialogClickListener;
        if (listener == null && m_common.m_commitExit)
            listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    exit();
                }
            };
        message(msgType.WARNING_MESSAGE, text, listener);
    }

    public void langWarningMsg(String literal, DialogInterface.OnClickListener dialogClickListener) {
        warningMsg(jotyLang(literal), dialogClickListener);
    }

    public void langWarningMsg(String literal) {
        langWarningMsg(literal, null);
    }

    public void informationMsg(String text, DialogInterface.OnClickListener dialogClickListener) {
        message(msgType.INFORMATION_MESSAGE, text, dialogClickListener);
    }

    public void informationMsg(String text) {
        message(msgType.INFORMATION_MESSAGE, text, null);
    }

    public void langInformationMsg(String literal, DialogInterface.OnClickListener dialogClickListener) {
        informationMsg(jotyLang(literal), dialogClickListener);
    }

    public void langInformationMsg(String literal) {
        langInformationMsg(jotyLang(literal), null);
    }

    public static void openUri(String uri, String mimeType) {
        File file = new File(uri);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(file), mimeType);
        Intent actualIntent = Intent.createChooser(intent, "Open file");
        m_app.m_activity.startActivity(actualIntent);
    }


    public JotyApp() {
        Utilities.setMessanger(this);
        m_common = new Common(this);
        m_app = this;
        m_db = new JotyDB();
        m_common.m_commitExit = false;
        m_reportsRolePerms = new CaselessStringKeyMap<Stocker>(JotyApp.this);
        m_userRoles = Utilities.m_me.new Stocker();
        m_refreshMap = new CaselessStringKeyMap<HashSet<String>>(JotyApp.this);
        m_paramContext = new ParamContext(this);
        m_respManagerCatalog = new HashMap<String, ResponseHandlersManager>();
        m_respManagerCounters = new RespManagerCountProvider();
        m_dataMainActivityStack = m_common.new JotyStack();
        m_dataDetailsActivityStack = m_common.new JotyStack();
        m_dataResultActivityStack = m_common.new JotyStack();
        m_tracing = false;
        m_xmlLogging = false;
        m_startPath = null;
        m_testPath = null;
        m_testing = false;
        m_configuredUser = "";
        m_iReTryMax = 3;
        m_random = new Random();
        m_webClient = null;
        m_committedClose = false;
        m_alreadyCertDeletionOffered = false;
        m_passwordValidator = null;
        m_reportManager = new ReportManager();
    }

    public BasicPostStatement accessorMethodPostStatement(String method, Integer returnedValuePos, Integer returnedValuesQty) {
        BasicPostStatement postStatement = new BasicPostStatement(this);
        postStatement.setMethod(method, returnedValuePos, returnedValuesQty);
        return postStatement;
    }

    public void accessorExecute(String literal, ResponseHandlersManager respManager) {
        accessorExecute(literal, m_paramContext, respManager);
    }

    public void accessorExecute(String literal, ParamContext paramContext, ResponseHandlersManager respManager) {
        m_db.executeSQL(literal, null, accessorPostStatement(null, -1, null, paramContext, null), respManager);
    }

    public BasicPostStatement accessorPostStatement(String jotyDialogFullClassName, int panelIdx, String termName, ParamContext prmParamContext, String mode) {
        BasicPostStatement postStatement = new BasicPostStatement(this);
        postStatement.setDataDefCoordinates(jotyDialogFullClassName, panelIdx, termName);
        if (mode != null)
            postStatement.m_method = mode;
        if (prmParamContext != null) {
            if (m_webClient.buildingRemoteTransaction()) {
                if (prmParamContext != m_webTransPrmContext) {
                    if (m_webTransPrmContext == null)
                        m_webTransPrmContext = prmParamContext;
                    prmParamContext.setDirty(false);
                }
            }
            postStatement.addItemsFromParamContext(prmParamContext);
        }
        return postStatement;
    }

    public BasicPostStatement createLiteralSubstPostStatement(String literalName) {
        return accessorPostStatement(null, -1, literalName, null, null);
    }

    @Override
    public int returnedValuesAvailablePos() {
        return m_webClient.returnedValuesAvailablePos();
    }

    public void openAccessorSubstWResultSet(String tabLiteral, String sql, ResponseHandlersManager respManager) {
        BasicPostStatement postStatement = createLiteralSubstPostStatement(tabLiteral);
        postStatement.m_sql = sql;
        openDbWResultSetByPostStatement(postStatement, respManager);
    }

    public void openAccessorWResultSet(String literal, ResponseHandlersManager respManager) {
        openAccessorWResultSet(literal, null, respManager);
    }


    public void openAccessorWResultSet(String literal, ParamContext paramContext, ResponseHandlersManager respManager) {
        BasicPostStatement postStatement = accessorPostStatement(null, -1, literal, paramContext, null);
        postStatement.m_sql = literal;
        openDbWResultSetByPostStatement(postStatement, respManager);
    }


    public void openDbWResultSetByPostStatement(BasicPostStatement postStatement, ResponseHandlersManager respManager) {
        WResultSet rs;
        String query = null;
        boolean byPostStatement = remoteAccessorMode();
        if (!byPostStatement)
            query = postStatement.m_sql;
        if (m_errorCarrier.m_exceptionMsg.length() > 0)
            toast(m_errorCarrier.m_exceptionMsg.toString());
        rs = new WResultSet(null, query);

        respManager.setParam("rs", rs);
        respManager.push(m_app.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                if (!result) {
                    respManager.m_params.remove("rs");
                    respManager.setParam("rs", null);
                }
                respManager.popAndcheckToExecute(result);
            }
        });
        rs.open(byPostStatement ? postStatement : null, respManager);
    }

    void acquireConnection() {
        beginWaitCursor();
        ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
        respManager.push(m_app.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                if (result) {
                    m_common.m_webSessionOn = true;
                    if (!m_common.m_commitExit)
                        verifyWebAuthentication();
                }
                if (m_activity instanceof LoginActivity) {
                    ((LoginActivity) m_activity).enableOkBtn();
                    endWaitCursor();
                }
            }
        });
        boolean oldSecure = m_common.setSecure(true);
        m_webClient.login(respManager);
        m_common.setSecure(oldSecure);
    }

    public void verifyWebAuthentication() {
        boolean sessionOn = m_common.m_webSessionOn;
        if (sessionOn) {
            verificationProlog();
            verifyLogin();
        } else
            acquireConnectionFollows(sessionOn);
    }

    public void acquireConnectionFollows(boolean result) {
        if (m_common.m_webSessionOn)
            m_iReTryCount++;
        m_connected = result;
        completeIdleAccess();
    }

    protected void enableRoleToReport(String reportName, String roleName) {
        if (m_reportsRolePerms.get(reportName) == null)
            m_reportsRolePerms.put(reportName, Utilities.m_me.new Stocker());
        m_reportsRolePerms.get(reportName).add(roleName);
    }

    public void loginAccessController() {
        endCurrentNonIdleActivity();
        if (!m_wasInMemoryYet || m_app.m_langChange)
            if (m_activity instanceof IdleActivity)
                ((IdleActivity) m_activity).createJotyMenu(m_menu);
        if (!m_loginValid && !m_loginAbandoned && !m_common.m_commitExit) {
            m_connected = false;
            boolean broken = false;
            if (m_iReTryCount > 0) {
                if (m_iReTryCount >= m_iReTryMax || m_loginAbandoned) {
                    m_loginAbandoned = true;
                    broken = false;
                }
            }
            if (!broken) {
                Intent intent = new Intent(m_activity, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    public void idleAccessController() {
        if (m_loginAbandoned) {
            if (m_iReTryCount < m_iReTryMax) {
                if (m_forLostSession) {
                    langYesNoQuestion("LeaveAuthExit",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE)
                                        exit();
                                    else {
                                        m_iReTryCount = 0;
                                        m_loginAbandoned = false;
                                        loginAccessController();
                                    }
                                }
                            });
                } else {
                    m_common.m_commitExit = true;
                    m_activity.finish();
                }
            } else {
                m_common.m_commitExit = true;
                langWarningMsg("AppExiting");
            }
        } else {
            if (m_common.m_userName.length() == 0 || m_common.m_password.length() == 0 || m_common.m_shared && m_common.m_sharingKey.length() == 0) {
                toast(jotyLang("FillTextFields"));
                loginAccessController();
            } else if (!m_connected)
                acquireConnection();
        }
    }


    public void completeIdleAccess() {
        m_loginValid = m_connected;
        if (m_loginAbandoned)
            exit();
        if (m_connected) {
            if (m_forLostSession) {
                langInformationMsg("NowIsPossible");
                endCurrentNonIdleActivity();
            } else {
                if (m_common.m_shared)
                    m_applicationPreferences.put("sharingKey", m_common.m_sharingKey);
                ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
                respManager.push(m_app.new ResponseHandler() {
                    @Override
                    public void handle(boolean result, ResponseHandlersManager respManager) {
                        endCurrentNonIdleActivity();
                    }
                });
                m_paramContext.setContextParam("userName", m_common.m_userName);
                if (m_common.m_authServer == null)
                    loadUserRoles(respManager);
                else
                    endCurrentNonIdleActivity();
                m_inited = true;
            }
        } else
            loginAccessController();
    }

    public void endCurrentNonIdleActivity() {
        if (m_activity.getClass() == m_idleActivityClass)
            m_home = false;
        else
            m_activity.finish();
    }

    /** used by the {@code JotyActivity.startActivity} method to avoid the repeated launch of an activity
     *
     * @see JotyActivity#startActivity
     */
    public HashMap<String, Integer> m_activitiesInstancesCounters = new HashMap<String, Integer>();


    protected void authenticate() {
        authenticate(false);
    }

    public void authenticate(boolean forLostSession) {
        m_forLostSession = forLostSession;
        if (forLostSession)
            m_common.m_webSessionOn = false;
        m_connected = false;
        m_iReTryCount = 0;
        m_loginAbandoned = false;
        m_loginValid = false;
        loginAccessController();
    }

    public void beginTrans() {
        m_db.beginTrans();
        m_webTransPrmContext = null;
    }


    public void buildLiteralStruct(String tabName, String keyField, String literalField, LiteralsCollection literalStruct, LiteralStructParams prmDescrParms, ResponseHandlersManager respManager) {
        buildLiteralStructMain(tabName, keyField, literalField, null, literalStruct, prmDescrParms, respManager);
    }

    public void buildLiteralStruct(String tabName, String keyField, String literalField, String name, ResponseHandlersManager respManager) {
        buildLiteralStruct(tabName, keyField, literalField, name, null, respManager);
    }

    public void buildLiteralStruct(String tabName, String keyField, String literalField, String name, LiteralStructParams prmDescrParms, ResponseHandlersManager respManager) {
        buildLiteralStructMain(tabName, keyField, literalField, name, null, prmDescrParms, respManager);
    }

    public void buildLiteralStructMain(String tabName, String keyField, String literalField, String name,
                                       LiteralsCollection paramLiteralStruct, LiteralStructParams prmLsParams, ResponseHandlersManager respManager) {
        Common.LiteralsCollectionData data = m_common.buildLiteralStructMain(tabName, keyField, literalField, name, paramLiteralStruct, prmLsParams);
        loadDataIntoLiteralStruct(tabName, keyField, literalField, data.literalsCollection, data.lsParms, respManager);
    }

    /**
     * Loads in memory, in a convenient data structure ( a {@code LiteralsCollection}
     * object ) a set of records of type {long id, String description}.
     * <p/>
     * For the meaning of the parameters see {@link org.joty.app.Common#prepareToLoadIntoLiteralStruct}
     */

    public void loadDataIntoLiteralStruct(String tabName, String keyField, String literalField, LiteralsCollection literalStruct,
                                          LiteralStructParams lsParams, ResponseHandlersManager prmRespManager) {
        String sqlStmnt = m_common.prepareToLoadIntoLiteralStruct(tabName, keyField, literalField, literalStruct, lsParams);
        ResponseHandlersManager respManager = m_app.new ResponseHandlersManager(prmRespManager);
        respManager.setParam("literalStruct", literalStruct);
        respManager.setParam("lsParams", lsParams);
        respManager.setParam("tabName", tabName);
        respManager.setParam("keyField", keyField);
        respManager.setParam("literalField", literalField);
        respManager.push(m_app.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                onLiteralStructWResultSetOpened(result, respManager);
            }
        });
        openAccessorSubstWResultSet(tabName, sqlStmnt, respManager);
        m_common.m_literalStructFilter = "";
    }

    public void onLiteralStructWResultSetOpened(boolean result, ResponseHandlersManager respManager) {
        if (result) {
            long IDval;
            String tabName = (String) respManager.getParam("tabName");
            String keyField = (String) respManager.getParam("keyField");
            String literalField = (String) respManager.getParam("literalField");
            LiteralsCollection literalStruct = (LiteralsCollection) respManager.getParam("literalStruct");
            LiteralStructParams lsParams = (LiteralStructParams) respManager.getParam("lsParams");
            WResultSet rs = (WResultSet) respManager.getParam("rs");
            int posIdx = literalStruct.m_descrArray.size();
            if (rs.isEOF() && !literalStruct.m_dynamic && m_debug) {
                String msg = String.format("Unexisting description data for %1$s (literal structure = %2$s) !",
                        lsParams.selectStmnt != null ? lsParams.selectStmnt : tabName, literalStruct.m_name);
                toast(msg);
            }
            while (!rs.isEOF()) {
                IDval = rs.integerValue(keyField);
                literalStruct.addLiteral(rs, lsParams, literalField, IDval, posIdx);
                posIdx++;
                rs.next();
            }
        }
    }


    public void loadDescriptions(ResponseHandlersManager respManager) {
        respManager.topHandlerIncrementCounter(1);
        if (m_common.m_shared)
            m_common.m_literalStructFilter = "id != 1";
        buildLiteralStruct("D0_1", "id", "name", "joty_roles", respManager);
    }

    public boolean checkRoleForExecution(String role) {
        boolean retVal = m_userRoles.contains(role);
        if (!retVal)
            warningMsg(jotyLang("AccessDenied"));
        return retVal;
    }

    public String codedTabName(String tabName) {
        return tabName == null ? "<JOTY_CTX>" : tabName;
    }

    public void commitTrans(ResponseHandlersManager respManager) {
        m_db.commitTrans(respManager);
    }

    public void endApp() {
        m_common.accessLocalData(m_applicationPreferencesFile, m_applicationPreferences);
        exit();
    }

    protected void doFurtherJobs() {
        ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
        respManager.push(m_app.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                boolean oldSecure = m_common.setSecure(true);
                onPasswordMustBeChanged(result);
                m_common.setSecure(oldSecure);
            }
        });
        mustPasswordBeChanged(respManager);
    }

    public void onPasswordMustBeChanged(boolean result) {
        if (result)
            tryChangePassword();
        else {
            ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
            respManager.push(m_app.new ResponseHandler() {
                @Override
                public void handle(boolean result, ResponseHandlersManager respManager) {
                    onIsDateExpired(result);
                }
            });
            boolean oldSecure = m_common.setSecure(true);
            isDateExpired(respManager);
            m_common.setSecure(oldSecure);
        }
    }

    public void onIsDateExpired(boolean result) {
        if (result)
            tryChangePassword();
        else
            acquireConnectionFollows(true);
    }

    public void tryChangePassword() {
        langWarningMsg("NewPwdMustDef", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ResponseHandlersManager guiOpRespManager = m_app.new ResponseHandlersManager();
                guiOpRespManager.push(m_app.new ResponseHandler() {
                    @Override
                    public void handle(boolean result, ResponseHandlersManager respManager) {
                        if (result) {
                            ResponseHandlersManager batchOpRespManager = new ResponseHandlersManager();
                            batchOpRespManager.push(new ResponseHandler() {
                                @Override
                                public void handle(boolean result, ResponseHandlersManager respManager) {
                                    onPasswordChangeTried(result);
                                }
                            });
                            m_app.setPassword(m_common.m_userName, (String) respManager.getParam("newPwdStr"), batchOpRespManager);
                        } else
                            onPasswordChangeTried(result);
                    }
                });
                setPassword(false, guiOpRespManager);
            }
        });
    }

    public void onPasswordChangeTried(boolean result) {
        if (!result)
            m_loginAbandoned = true;
        acquireConnectionFollows(result);
    }

    public void doSetPassword(String userName, String newPwd, ResponseHandlersManager respManager) {
        boolean old_applicationScopeAccessorMode = m_common.setApplicationScopeAccessorMode();
        m_paramContext.setContextParam("newPwd", newPwd);
        m_paramContext.setContextParam("setPwdUsername", userName);
        accessorExecute("setUserPassword", respManager);
        m_common.setApplicationScopeAccessorMode(old_applicationScopeAccessorMode);
    }

    private JotyActivity getWaitCursorActivity() {
        return (m_activity instanceof MenuActivity || m_activity instanceof LoginActivity) ? m_activity : m_idleActivity;
    }

    public void beginWaitCursor() {
        setWaitCursor(true);
    }


    public void endWaitCursor() {
        setWaitCursor(false);
    }

    public void executeSQL(String stmt, ResponseHandlersManager respManager) {
        executeSQL(stmt, null, respManager);
    }

    public void executeSQL(String stmt, String autoID, ResponseHandlersManager respManager) {
        executeSQL(stmt, autoID, null, respManager);
    }

    public void executeSQL(String stmt, String autoID, BasicPostStatement contextPostStatement, ResponseHandlersManager respManager) {
        m_db.executeSQL(stmt, autoID, contextPostStatement, respManager);
    }


    public String getDialogClassFullName(String className) {
        return className.indexOf(".") == -1 ? (getClass().getPackage().getName() + "." + className) : className;
    }


    public ConfigFile m_configFile;

    public void webInitAndGetConfig() {
        boolean success = m_common.m_appUrl != null;
        if (!success) {
            m_common.m_commitExit = true;
            warningMsg(m_messageText);
            return;
        }
        if (!m_wasInMemoryYet)
            try {
                m_webClient = m_webClientClass == null ? new WebClient(this) : ((WebClient) Instantiator.create(m_webClientClass, null));
                success = m_webClient.m_urlValid;
            } catch (ClassNotFoundException e) {
                warningMsg("WebClient class not found !");
                success = false;
            }
        if (success) {
            if (!m_wasInMemoryYet) {
                m_common.m_xmlEncoder = new XmlTextEncoder(this) {
                    @Override
                    protected byte[] base64decode(String src) {
                        return Base64.decode(src, Base64.DEFAULT);
                    }

                    @Override
                    protected String base64encode(byte[] src) {
                        return Base64.encodeToString(src, Base64.DEFAULT);
                    }
                };
                m_reportManager.setXmlEncoder(m_common.m_xmlEncoder);
            }
            getPreferences();
            if (m_webClient != null) {
                toast("Connecting to server", true, true);
                m_webClient.getConfig("conf");
            }
        }
        m_configDataLoaded = true;
    }

    protected void getPreferences() {
        m_applicationPreferences = (Map<String, String>) m_common.accessLocalData(m_applicationPreferencesFile, null);
        if (m_applicationPreferences == null)
            m_applicationPreferences = new HashMap<String, String>();
        m_common.m_language = m_applicationPreferences.get("language");
        m_common.m_sharingKey = m_applicationPreferences.get("sharingKey");
        if (m_common.m_language == null) {
            m_common.m_language = "en";
            m_app.m_applicationPreferences.put("language", m_common.m_language);
        }
        m_startPath = m_app.m_applicationPreferences.get("startPath");
        m_startPathLocallyStored = m_startPath != null;
        if (! m_startPathLocallyStored)
            m_startPath = m_defaultStartPath;
    }

    public void doLoadData() {
        if (m_common.m_shared)
            m_paramContext.setContextParam("sharingKey", m_common.m_sharingKey);
        Utilities.checkDirectory(getFilesDir().toString());
        loadData();
    }

    protected void loadData() {
        toast(jotyLang("LoadingData"));
        beginWaitCursor();
        ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
        respManager.push(m_app.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                endWaitCursor();
                onDataLoaded();
            }
        });
        m_common.setApplicationScopeAccessorMode();
        loadDescriptions(respManager);
        m_common.setApplicationScopeAccessorMode(false);
    }


    public void getLocale() {
        beginWaitCursor();
        QueryResponseHandlersManager respManager = new QueryResponseHandlersManager(
                accessorPostStatement("org.joty.workstation.gui.AppOptionsDialog", 0, null, m_app.m_paramContext, null),
                new QueryResponseHandler() {
                    @Override
                    public void handleQuery(boolean result, WResultSet rs, BasicPostStatement postStatement) {
                        endWaitCursor();
                        if (result) {
                            int localID = 0;
                            if (!rs.isEOF())
                                localID = (int) rs.integerValue("locale_id");
                            getLocaleInfo(localID);
                        }
                    }
                });
        respManager.open();
    }

    public void getLocaleInfo(int localeID) {
        BasicPostStatement postStatement = accessorPostStatement("org.joty.workstation.gui.AppOptionsDialog", 0, "loc_literal", m_app.m_paramContext, null);
        postStatement.m_mainFilter = "id = " + localeID;
        QueryResponseHandlersManager respManager = new QueryResponseHandlersManager(
                postStatement,
                new QueryResponseHandler() {
                    @Override
                    public void handleQuery(boolean result, WResultSet rs, BasicPostStatement postStatement) {
                        if (!result || rs.isEOF()) {
                            m_common.m_loc_lang = "en";
                            m_common.m_loc_country = "US";
                            toast("Failure on getting locale from Joty Server: country = 'US' and language = 'en' are assumed !");
                        } else {
                            m_common.m_loc_lang = rs.getValueStr("language", false);
                            m_common.m_loc_country = rs.getValueStr("country", false);
                        }
                        m_common.acquireLocaleInfo();
                        onInitComplete();
                    }
                });
        respManager.open();
    }

    public void onInitComplete() {
        m_dataLoaded = true;
        setIdle(true);
    }


    public void onDataLoaded() {
        m_common.m_dataReLoad = true;
        registerReports();
        if (m_common.m_useAppOptions)
            getLocale();
        else {
            try {
                m_common.acquireLocsFromConf();
                m_common.acquireLocaleInfo();
            } catch (ConfigFile.ConfigException e) {
                exceptionToToast(e);
            }
            onInitComplete();
        }
    }


    public void onGonfigurationGot(String type) {
        if (m_configFile != null)
            m_configFile.m_type = type;
        switch (type) {
            case "conf":
                m_common.m_configuration = m_configFile;
                if (m_common.m_configuration != null) {
                    toast("Getting configuration", true, true);
                    try {
                        m_remoteAccessor = m_common.getConfBool("remoteAccessor");
                        if (!m_remoteAccessor) {
                            m_common.m_commitExit = true;
                            warningMsg("Joty Mobile requires Joty Server to be configured for the remote Accessor feature !");
                        }
                        m_debug = m_common.getConfBool("debug");
                    } catch (ConfigFile.ConfigException e) {
                        exceptionToToast(e);
                    }
                    m_webClient.getConfig("jotyLang", m_common.m_language);
                }
                break;
            case "jotyLang":
                m_common.m_JotyLang = m_configFile;
                if (m_common.m_JotyLang == null)
                    exit();
                else {
                    m_common.m_JotyLang.buildMap();
                    m_webClient.getConfig("appLang", m_common.m_language);
                }
                break;
            case "appLang":
                m_common.m_JotyAppLang = m_configFile;
                if (m_common.m_JotyAppLang != null)
                    m_common.m_JotyAppLang.buildMap();
                boolean success = m_common.m_JotyAppLang != null;
                if (success) {
                    m_common.loadKeyStore();
                    m_errorCarrier = new ErrorCarrier();
                    m_langLiteralRetCodeMapper = new LangLiteralRetCodeMapper(this);
                    m_langLiteralRetCodeMapper.load(m_common.m_JotyLang);
                    success = loadConfigProperties();
                    if (success) {
                        m_common.loadCalendarElems();
                        try {
                            m_dbManager = Instantiator.createDbManager(m_errorCarrier, m_common.m_configuration);
                        } catch (ClassNotFoundException e1) {
                            warningMsg("AbstractDbManager class not found !");
                            success = false;
                        }
                        if (!m_remoteAccessor) {
                            if (m_debug)
                                warningMsg("Joty Mobile requires Joty Server to be configured for remote Accessor feature !");
                            success = false;
                        }
                    }
                }
                if (success) {
                    m_langLoaded = true;
                    authenticate();
                } else
                    exit();

                break;
        }
    }

    public void setApp(String name, String version, String servletName, String webClientClass) {
        m_webClientClass = webClientClass;
        if (m_common.m_commitExit)
            return;
        if (servletName != null)
            m_common.m_servlet = servletName;
        m_versionString = version;
        m_name = name;
    }

    public void postMainActivityCreated() {
        m_wasInMemoryYet = m_common.m_commitExit && m_configDataLoaded;
        m_langChange = false;
        m_common.m_commitExit = false;
        if (m_wasInMemoryYet) {
            m_common.m_webSessionOn = false;
            m_connected = false;
            m_loginValid = false;
            m_loginAbandoned = false;
            m_iReTryCount = 0;
            m_menuBuilt = false;
            if (m_webClient != null)
                m_webClient.m_sessionID = "";
            m_langLoaded = m_app.m_applicationPreferences.get("language").compareTo(m_common.m_language) == 0;
            m_langChange = !m_langLoaded;
            if (m_langLoaded)
                loginAccessController();
            else
                m_configDataLoaded = false;
        }

        if (!m_configDataLoaded) {
            getPreferences();
            if (m_testing) {
                m_common.m_appUrl = m_testPath;
                webInitAndGetConfig();
            } else if (m_startPathLocallyStored)
                getUrlFromHomePage();
            else
                getStartUrl();
        }
    }

    public void getUrlFromHomePage() {
        Intent intent = new Intent(m_activity, ServerUrlActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void getStartUrl() {
        Intent intent = new Intent(m_activity, StartUrlActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void invokeAccessMethod(BasicPostStatement postStatement, ResponseHandlersManager respManager) {
        if (respManager != null) {
            respManager.setParam("postStatement", postStatement);
            respManager.push(m_app.new ResponseHandler() {
                @Override
                public void handle(boolean result, ResponseHandlersManager respManager) {
                    respManager.popAndcheckToExecute(onAccessMethodInvoked(result, respManager));
                }
            });
        }
        m_webClient.manageCommand(null, false, null, true, postStatement, 0, respManager);
    }

    public boolean onAccessMethodInvoked(boolean result, ResponseHandlersManager respManager) {
        if (result) {
            Vector<String> returnedValues = m_webClient.m_returnedValues;
            int vectorIndex = -1;
            BasicPostStatement postStatement = (BasicPostStatement) respManager.getParam("postStatement");
            for (BasicPostStatement.ReturnedValueItem item : postStatement.m_returnedValues) {
                vectorIndex = item.m_returnedValPosition - 1;
                item.valueLiteral = returnedValues.get(vectorIndex);
            }
            if (Integer.parseInt(postStatement.m_outParamsQty) < 0)
                postStatement.m_retVal = returnedValues.get(vectorIndex + 1);
        } else
            toast(m_errorCarrier.m_exceptionMsg.toString());
        return result;
    }


    public String languageItem(String literal, ConfigFile langCF) {
        try {
            return langCF.configTermValue(literal);
        } catch (ConfigFile.ConfigException e) {
            return null;
        }
    }

    public void launchReport(String name, String renderType) {
        launchReport(name, renderType, true);
    }


    public void launchReport(final String name, String renderType, final boolean twoProcesses) {
        Boolean userAuthorized = false;
        Stocker rolesForReport = m_reportsRolePerms.get(name);
        if (rolesForReport != null) {
            for (String role : m_userRoles) {
                if (rolesForReport.contains(role)) {
                    userAuthorized = true;
                    break;
                }
            }
        }
        if (userAuthorized) {
            if (renderType == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(m_activity);
                final String types[] = getBirtRenderTypes();
                builder.setItems(types, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (which != -1) {
                            m_applicationPreferences.put("defaultBirtRender", types[which]);
                            m_webClient.report(name, types[which], twoProcesses, m_reportManager.m_params);
                        }
                    }
                });
                builder.setTitle(m_app.jotyLang("SelectBirtFormat"));
                builder.show();
            } else
                m_webClient.report(name, renderType, twoProcesses, m_reportManager.m_params);
        } else
            langWarningMsg("AccessDenied");
    }

    protected String[] getBirtRenderTypes() {
        return new String[]{"pdf", "html"};
    }

    protected String mimeType(String extension) {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    public void onReport(String renderType) {
        WebClient.DocumentDescriptor docDescriptor = m_webClient.getDocumentFromRespContent(m_webClient.m_responseText != null);
        if (docDescriptor.xml != null)
            if (docDescriptor.success) {
                byte[] m_bytes;
                m_bytes = m_webClient.getBytesFromRespDocument(docDescriptor, "Report");
                if (m_bytes != null && m_bytes.length > 0)
                    openDocumentFromBytes(m_bytes, renderType);
            } else
                setWaitCursor(false);
    }

    /**
     * Delegates {@code openUri} to render the pdf content, that is here preliminarly saved
     * as a file located in an external folder: getCacheDir() is un-accessible to
     * (other) viewer applications so getExternalCacheDir is used and with it WRITE_EXTERNAL_STORAGE
     * permission is required in the app Manifest
     */
    public void openDocumentFromBytes(byte[] bytes, String fileExt) {
        File outputDir = m_app.m_activity.getExternalCacheDir();
        String tempDir = outputDir.getAbsolutePath();
        String fileName = String.valueOf(m_app.m_random.nextInt()) + "." + fileExt;
        m_common.saveBytesAsFile(bytes, tempDir, fileName, true);
        openUri(tempDir + "/" + fileName, mimeType(fileExt));
    }


    private boolean loadConfigProperties() {
        m_iconImages = new Vector<Image>();
        boolean retVal = m_common.loadConfigProperties(true);
        if (retVal)
            m_webClient.setAuthServerUrl();
        return retVal;
    }


    protected void loadUserRoles(ResponseHandlersManager respManager) {
        respManager.push(m_app.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                WResultSet rs = (WResultSet) respManager.getParam("rs");
                if (rs != null) {
                    while (!rs.isEOF()) {
                        m_userRoles.add(rs.stringValue("roleName"));
                        rs.next();
                    }
                }
                respManager.popAndcheckToExecute(true);
                m_app.m_idleActivity.invalidateOptionsMenu();
            }
        });
        boolean old_applicationScopeAccessorMode = m_common.setApplicationScopeAccessorMode();
        openAccessorWResultSet("LoadUserRoles", m_paramContext, respManager);
        m_common.setApplicationScopeAccessorMode(old_applicationScopeAccessorMode);
    }

    protected void mustPasswordBeChanged(ResponseHandlersManager respManager) {
        userOperation("mustPasswordBeChanged", false, false, respManager);
    }

    protected void isDateExpired(ResponseHandlersManager respManager) {
        userOperation("isDateExpired", false, false, respManager);
    }

    protected void verifyLogin() {
        ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
        respManager.push(m_app.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                onLoginVerified(result);
            }
        });
        boolean oldSecure = m_common.setSecure(true);
        userOperation("verifyLogin", true, true, respManager);
        m_common.setSecure(oldSecure);
    }

    public void onLoginVerified(boolean result) {
        if (result)
            doFurtherJobs();
        else {
            m_common.m_webSessionOn = false;
            acquireConnectionFollows(result);
        }
    }

    protected void verificationProlog() {
    }


    protected void registerReports() {
    }

    public void resetRemoteTransactionBuilding() {
        m_webClient.buildingRemoteTransaction_reset();
        m_dataModified = false;
    }

    public void setPassword(boolean getOldPassword, ResponseHandlersManager respManager) {
        Intent myIntent = new Intent(m_activity, ChangePasswordActivity.class);
        myIntent.putExtra("getOldPassword", getOldPassword);
        m_respManagerCatalog.put("ChangePasswordActivity", respManager);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myIntent);
    }

    public void setPassword(String userName, String newPassword, ResponseHandlersManager prmRespManager) {
        boolean oldSecureStatus = m_common.setSecure(true);
        ResponseHandlersManager respManager = prmRespManager == null ? m_app.new ResponseHandlersManager() : prmRespManager;
        respManager.setParam("userName", userName);
        respManager.setParam("newPassword", newPassword);
        respManager.push(m_app.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                onUserPwdSet(result, respManager);
                respManager.popAndcheckToExecute(result);
            }
        });
        setUserPwd(userName, newPassword, "ALTER", m_common.m_password, 0, respManager);
        m_common.setSecure(oldSecureStatus);
    }

    public void onUserPwdSet(boolean result, ResponseHandlersManager prmRespManager) {
        if (result) {
            ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
            respManager.setParam("userName", prmRespManager.getParam("userName"));
            respManager.setParam("newPassword", prmRespManager.getParam("newPassword"));
            respManager.push(m_app.new ResponseHandler() {
                @Override
                public void handle(boolean result, ResponseHandlersManager respManager) {
                    onPasswordSet(result, respManager);
                }
            });
            boolean oldSecureStatus = m_common.setSecure(true);
            doSetPassword((String) prmRespManager.getParam("userName"), (String) prmRespManager.getParam("newPassword"), respManager);
            m_common.setSecure(oldSecureStatus);
        }
    }

    public void onPasswordSet(boolean result, ResponseHandlersManager prmRespManager) {
        if (result) {
            m_common.m_password = (String) prmRespManager.getParam("newPassword");
            toast(jotyLang("ActionSuccess"));
        } else {
            // try resuming password at dbms level
            ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
            respManager.setParam("userName", prmRespManager.getParam("userName"));
            respManager.setParam("newPassword", prmRespManager.getParam("newPassword"));
            respManager.push(m_app.new ResponseHandler() {
                @Override
                public void handle(boolean result, ResponseHandlersManager respManager) {
                    if (result)
                        langWarningMsg("PwdNotChanged");
                    else
                        warningMsg("DBMS password instance was changed but the change of database password instance failed !");
                }
            });
            boolean oldSecureStatus = m_common.setSecure(true);
            m_db.executeSQL(String.format(m_common.m_dbmsUserPwdStatement, prmRespManager.getParam("userName"), m_common.m_password), respManager);
            m_common.setSecure(oldSecureStatus);
        }
    }

    public void setUserPwd(String userName, String newPwd, String command, String oldPwd, int nonManagedRollbackIndex, ResponseHandlersManager prmRespManager) {
        boolean specializedStmnt = command.compareTo("ALTER") == 0 && m_common.m_dbmsChangePwdStatement != null;
        String templateStmnt = specializedStmnt ? m_common.m_dbmsChangePwdStatement : m_common.m_dbmsUserPwdStatement;
        if (templateStmnt != null) {
            String sql = specializedStmnt ? String.format(templateStmnt, userName, newPwd, oldPwd) : String.format(templateStmnt, command, userName, newPwd);
            m_db.executeSQL(sql, null, null, nonManagedRollbackIndex, prmRespManager);
        }
    }


    protected void userOperation(String methodName, boolean login,
                                 boolean retCodeAsLangLiteral, ResponseHandlersManager respManager) {
        beginWaitCursor();
        BasicPostStatement postStatement = accessorMethodPostStatement(methodName, 1, -1);
        postStatement.addItem("userName", m_common.m_userName, BasicPostStatement.Item._text);
        if (login)
            postStatement.addItem("password", m_common.m_password, BasicPostStatement.Item._text);
        if (m_common.m_shared)
            postStatement.addItem("sharingKey", m_common.m_sharingKey, BasicPostStatement.Item._text);
        respManager.setParam("postStatement", postStatement);
        respManager.setParam("retCodeAsLangLiteral", retCodeAsLangLiteral);
        respManager.push(m_app.new ResponseHandler() {
            @Override
            public void handle(boolean result, ResponseHandlersManager respManager) {
                boolean success = false;
                endWaitCursor();
                if (result) {
                    Long retCode = Long.parseLong(((BasicPostStatement) respManager.getParam("postStatement")).m_retVal);
                    if (retCode == 0)
                        success = true;
                    else if ((boolean) respManager.getParam("retCodeAsLangLiteral"))
                        langWarningMsg(m_langLiteralRetCodeMapper.literal(retCode));
                }
                respManager.popAndcheckToExecute(success);
            }
        });
        invokeAccessMethod(postStatement, respManager);
    }


    public class ValuesOnChoice extends WFieldSet {
        public String m_fieldsToCatch[];
        public long m_id;

        public ValuesOnChoice(JotyMessenger JotyMessenger) {
            super(JotyMessenger);
        }

        public void clear() {
            m_id = 0;
            m_fieldsToCatch = null;
            super.clear();
        }

        public boolean auxFields() {
            return m_fieldsToCatch != null && m_fieldsToCatch.length > 0;
        }

        public boolean selected() {
            return m_id != 0;
        }

        public void acquire(long id, JotyCursorWrapper cursor) {
            m_id = id;
            WrappedField wfield = null;
            for (String name : m_fieldsToCatch) {
                wfield = add(name, JotyTypes._none);
                cursor.setValueToWField(name, wfield);
            }
        }

        public long getId() {
            return m_id;
        }
    }

    public ValuesOnChoice m_valuesOnChoice = new ValuesOnChoice(this);

    public void openSearcherAsSelector(Class mainActivityClass, String fieldsToCatch[]) {
        DataDetailsActivity callerActivity = (DataDetailsActivity) m_activity;
        callerActivity.m_selectionIsRunning = true;
        Intent myIntent = new Intent(callerActivity, mainActivityClass);
        m_valuesOnChoice.clear();
        m_valuesOnChoice.m_fieldsToCatch = fieldsToCatch;
        myIntent.putExtra("asSelector", true);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myIntent);
    }

    @Override
    public WrappedField createWrappedField() {
        return new WrappedField(this);
    }

    @Override
    public boolean debug() {
        return m_debug;
    }

    @Override
    public boolean isDesignTime() {
        return false;
    }

    @Override
    public void beforeReportRender() {
    }

    @Override
    public void afterReportRender(String s) {
    }

    @Override
    public ICommon getCommon() {
        return m_common;
    }

    @Override
    public String getKeyStoreType() {
        return "BKS";
    }

    @Override
    public void firstChanceKeyStore() throws Throwable {
        m_common.m_ks.load(m_activity.getBaseContext().getResources().openRawResource(R.raw.bks_store), "jotymobile".toCharArray());
    }

    @Override
    public String keyStorePath() {
        return m_app.m_activity.getFilesDir() + "/" + m_ksFileName;
    }

    @Override
    public LiteralsCollection instantiateLiteralsCollection(JotyMessenger jotyMessanger) {
        return new LiteralsCollection(jotyMessanger);
    }

    @Override
    public boolean remoteAccessorMode() {
        return m_remoteAccessor && (m_contextActivity != null && m_contextActivity.m_accessorMode || m_common.m_applicationScopeAccessorMode);
    }

    @Override
    public void volatileMessage(String langLiteral, boolean appSpecific) {
        m_app.toast(m_common.langMessage(langLiteral, appSpecific));
    }

    @Override
    public boolean setWaitCursor(boolean truth) {
        getWaitCursorActivity().setWaitCursor(truth);
        return true;
    }

    @Override
    public AbstractWebClient getWebClient() {
        return m_webClient;
    }

    @Override
    public void constraintViolationMsg(boolean onUpdate, JotyException jotyException) {
        DataDetailsActivity activity = (DataDetailsActivity) m_activity;
        boolean result = onUpdate ?
                activity.costraintViolationMsgOnUpdate() :
                activity.costraintViolationMsgOnDelete();
        if (result && ! debug())
            jotyException.m_verboseReason = null;
    }

    @Override
    public void manageExpiredSession() {
        toast(jotyLang("SessionExpMustLogon"));
        authenticate(true);
    }

    public String jotyLang(String literal) {
        return m_common.jotyLang(literal);
    }

    public void openInfoDialog(String message){}
    public void closeInfoDialog(){}
    public boolean designTime(){return false;}
    public void JotyMsg(Object object, String text){}
    public void openUri(String uri, boolean webLocator){}

}


