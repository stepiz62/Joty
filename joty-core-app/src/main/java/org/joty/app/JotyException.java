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

import org.joty.common.ApplMessenger;

/**
 * The class has been thought to help collecting and presenting a semantically
 * predefined set of exceptional circumstances. For this it extends
 * java.lang.Exception class and encodes these scenarios by means of the
 * {@code reason} enumeration; then, thrown by the Joty code when various
 * scenarios are detected, it takes the decision of informing the user or about
 * other convenient action.
 * <p>
 * As in other part of the framework, here, also, a 'switch' implementation is
 * preferred to the polymorphic inheritance by specialized classes.
 *
 * @see org.joty.common.AbstractDbManager#dbExceptionCheck
 *
 */

public class JotyException extends Exception {

    public enum reason {
        NO_REASON, CONSTR_VIOLATION_ON_UPDATE, CONSTR_VIOLATION_ON_DELETE, SESSION_EXPIRED, GENERIC, WEB_LAB,
        NO_BIRT_ENGINE, INVALID_CREDENTIALS, DBMS_UNREACHABLE, SILENT, DBMS_CREATEUSER_FAILURE
    };

    public reason m_webReason;
    public String m_verboseReason;

    public JotyException(int forcedId, JotyApplication app) {
        this(reason.WEB_LAB, String.valueOf(forcedId), app);
    };

    public JotyException(reason theReason, String verboseReason, JotyApplication app) {
    	Common common = (Common) ((ApplMessenger) app).getCommon();
        common.resetRemoteTransactionBuilding();
        m_webReason = theReason;
        m_verboseReason = verboseReason;
        switch (theReason) {
            case INVALID_CREDENTIALS:
                app.jotyMessage(common.jotyLang("WrongUserOrPwd"));
                break;
            case SESSION_EXPIRED:
                app.manageExpiredSession();
                break;
            case NO_BIRT_ENGINE:
                app.jotyMessage(common.MSG_NO_BIRT);
                break;
            case WEB_LAB:
                common.m_commitExit = true;
                app.jotyMessage(m_verboseReason);
                break;
            case SILENT:
            case GENERIC:
                break;
            case CONSTR_VIOLATION_ON_UPDATE:
                app.constraintViolationMsg(true, this);
                break;
            case CONSTR_VIOLATION_ON_DELETE:
                app.constraintViolationMsg(false, this);
                break;
            case DBMS_UNREACHABLE:
                app.jotyMessage(common.jotyLang("DbmsUnreachable"));
                break;
            case DBMS_CREATEUSER_FAILURE:
                app.jotyMessage(common.jotyLang("DbmsCreateUserFailed") +
                        "\n\n" + common.jotyLang(common.m_shared ? "DbmsCreateUserFailed2" : "DbmsCreateUserFailed3"));
                break;
            default:
        }
    }

    @Override
    public String getMessage() {
        return m_verboseReason;
    }

    @Override
    public String toString() {
        return m_verboseReason;
    }

}
