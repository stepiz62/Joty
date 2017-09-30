/*
	Copyright (c) 2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

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

package org.joty.mobile.gui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import org.joty.common.Utilities;
import org.joty.mobile.R;
import org.joty.mobile.app.JotyApp.JotyMenus;
import org.joty.mobile.app.JotyApp.ResponseHandlersManager;

import java.util.HashMap;
import java.util.Vector;

/**
 * The main activity of the application must inherit from this class.
 * The class builds the default application menu that includes the list of languages available in the application.
 * <p>
 * Then it provides also the handlers for all the item built.
 *
 */

public class IdleActivity extends MenuActivity {
    private HashMap<Integer, String> m_langMenuItemIDsMap;
    private boolean m_menuCreationTried;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_app.setIdleActivityClass(getClass());
        m_app.m_idleActivity = this;
        m_langMenuItemIDsMap = new HashMap<Integer, String>();
        m_app.postMainActivityCreated();
    }


    public void createJotyMenu(Menu menu) {
        m_app.m_maxUsedMenuItemID = 0;
        m_app.m_currentlySelectedLangMenuItemId = 0;
        addMenuItemsToAppMenu(menu);
        SubMenu langSubMenu = addItemAsSubMenu(menu, JotyMenus.LangMenu, jotyLang("Languages"), R.drawable.lang);
        loadLanguagesIntoMenu(langSubMenu);
        addMenuItemToAppMenu(menu, JotyMenus.chgPwdMenu, jotyLang("ChangePwd"), R.drawable.changepwd, false);
        addMenuItemToAppMenu(menu, JotyMenus.ExitMenu, jotyLang("LBL_EXIT"), R.drawable.exit, false);
        m_app.m_menuBuilt = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        m_app.m_menu = menu;
        if (m_app.m_langLoaded && (!m_app.m_menuBuilt || ! m_menuCreationTried)) {
            createJotyMenu(menu);
            return super.onCreateOptionsMenu(menu);
        } else {
            m_menuCreationTried = true;
            return true;
        }
    }

    private void loadLanguagesIntoMenu(SubMenu langSubMenu) {
        Vector<String> langVector = new Vector<String>();
        Utilities.split(m_common.m_languages, langVector, ";");
        MenuItem langItem;
        int menuId = m_app.m_maxUsedMenuItemID;
        for (String lang : langVector) {
            menuId++;
            langItem = addMenuItemToAppMenu(langSubMenu, menuId, lang, 0, true).setCheckable(true);
            if (lang.compareTo(m_common.m_language) == 0) {
                langItem.setChecked(true);
                m_app.m_currentlySelectedLangMenuItemId = menuId;
            }
            m_langMenuItemIDsMap.put(menuId, lang);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        boolean processed = true;
        if (id == JotyMenus.chgPwdMenu.ordinal()) {
            ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
            respManager.push(m_app.new ResponseHandler() {
                @Override
                public void handle(boolean result, ResponseHandlersManager respManager) {
                    if (result)
                        m_app.setPassword(m_common.m_userName, (String) respManager.getParam("newPwdStr"), null);
                }
            });
            m_app.setPassword(true, respManager);
        } else if (id == JotyMenus.LangMenu.ordinal())
            ;
        else if (id == JotyMenus.ExitMenu.ordinal())
            m_app.endApp();
        else {
            String language = m_langMenuItemIDsMap.get(id);
            if (language != null) {
                m_app.m_applicationPreferences.put("language", language);
                m_app.informationMsg(String.format(jotyLang("OptionAcquired"), jotyLang("LangChoice")));
                MenuItem oldSelectedMenuItem =  m_app.m_menu.findItem(m_app.m_currentlySelectedLangMenuItemId);
                if (oldSelectedMenuItem != null)
                    oldSelectedMenuItem.setChecked(false);
                item.setChecked(true);
                m_app.m_currentlySelectedLangMenuItemId = id;
             } else
                processed = false;
        }
        return processed ? true : super.onOptionsItemSelected(item);
    }

    @Override
    protected void addHome(Menu menu) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (m_app.m_inited) {
            m_app.checkDataLoaded();
            m_app.m_respManagerCounters.clear();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        m_menuCreationTried = false;
    }

    @Override
    protected boolean accessIsAllowed() {
        return true;
    }
}
