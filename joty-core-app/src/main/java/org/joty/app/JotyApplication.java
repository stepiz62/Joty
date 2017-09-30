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

import org.joty.common.JotyMessenger;
import org.joty.data.JotyDate;
import org.joty.data.WrappedField;
import org.joty.web.AbstractWebClient;

public interface JotyApplication extends JotyMessenger {
    public WrappedField createWrappedField();
    public JotyDate createDate();
    int returnedValuesAvailablePos();
    public boolean debug();
    public String localFilesPath();
    public String getKeyStoreType();
    public void firstChanceKeyStore() throws Throwable;
    public String keyStorePath();
    public LiteralsCollection instantiateLiteralsCollection(JotyMessenger jotyMessanger);
    public boolean remoteAccessorMode();
    public void volatileMessage(String langLiteral, boolean appSpecific);
    public boolean setWaitCursor(boolean truth);
    public AbstractWebClient getWebClient();
    public void constraintViolationMsg(boolean onUpdate, JotyException jotyException);
    public void manageExpiredSession();
	public void openInfoDialog(String message);
	public void closeInfoDialog();
	public void openUri(String uri, boolean webLocator);
	public void JotyMsg(Object object, String text);
	public boolean designTime();
}
