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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.joty.common.CaselessStringKeyMap;
import org.joty.common.ParamContext;
import org.joty.common.BasicPostStatement;
import org.joty.data.WrappedField;
import org.joty.mobile.R;
import org.joty.mobile.app.JotyApp;


import java.util.HashMap;

/**
 * It provides menu item builder methods that make no use of the resource inflating: the
 * {@code addMenuItemToAppMenu} method (with various overloads) and the {@code addItemAsSubMenu} method are
 * able to contribute to the building of the menu as they are used in the overriding of the {@code onCreateOptionsMenu} Android method.
 * These methods feed a map ({@code m_stockedMenuItems}) that binds each id with the respective action
 * and is used later by the overriding of the {@code onOptionsItemSelected} Android method.
 * <p>
 * To help avoiding Id collisions when menu items are added basing of dynamic data these builder methods
 * take care to update the JotyApp.m_maxUsedMenuItemID that must be taken in consideration by the dynamic
 * building and is reset in the creation of the main activity of the app (a descendant of {@code IdelActivity}
 * <p>
 * Pre-built menu items typical of the user interface of the JotyApp are also provided through dedicated
 * methods that map that action with an handler method to be implemented.
 * <p>
 * Beyond being a provider of builder methods this class takes care of inizializing the "back" botton in
 * the action bar and to add the "(Joty) Home" menu item.
 * <p>
 * Another function of the class is to provide the identification of the Joty permission
 * ( see {@code Permission} for accessing the activity basing on the information stored in the
 * {@code m_rolesPerm} member, where the joty permission for each user role is gettable (the default is "no access").
 * <p>
 * Further helps are opening an image from a binary field and assisting the Accessor mode management with a
 * BasicPostStatement creator based on the accessor coordinates submitted.
 * <p>
 * The override of {@code finish} introduces tha availability of an overridable {@code beforeFinish} method
 * that assures the execution before the invocation of the ancestor method and comes with a parameter that
 * informs about the re-opening of the same class.
 */

public class MenuActivity extends JotyActivity {
    protected interface Action {
        void doIt();
    }

    protected HashMap<Integer, Action> m_stockedMenuItems;
    public Permission m_permission = Permission.no_access;
    public AccessorCoordinates m_accessorCoordinates;
    public boolean m_accessorMode;
    protected ParamContext m_paramContext;
    protected MenuActivity m_contextActivity;
    protected DataMainActivity m_mainActivity;

    protected void setContextActivity(MenuActivity activity) {
        m_app.m_contextActivity = activity;
        m_contextActivity = activity;
    }


    protected enum Permission {
        no_access, read, readWrite, readWriteAdd, all
    }

    private CaselessStringKeyMap<Permission> m_rolesPerm;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_rolesPerm = new CaselessStringKeyMap<Permission>(m_app);
        m_stockedMenuItems = new HashMap<Integer, Action>();
        m_mainActivity = mainActivity();
    }

    protected boolean accessIsAllowed() {
        return hasPermission(Permission.read);
    }

    protected boolean hasPermission(Permission permission) {
        return getMaxPermission() ? m_permission.compareTo(permission) >= 0 : false;
    }

    public MenuItem addMenuItemToAppMenu(Menu menu, int id, CharSequence caption, int resId, boolean menuIsSubMenu) {
        traceMenuItemID(id);
        MenuItem item = menu.add(Menu.NONE, id, menu.size(), caption);
        if (resId > 0) {
            item.setIcon(getResources().getDrawable(resId));
            if (menuIsSubMenu)
                setItem(item);
            else
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }
        return item;
    }

    private void traceMenuItemID(int id) {
        if (id > m_app.m_maxUsedMenuItemID)
            m_app.m_maxUsedMenuItemID = id;
    }

    public MenuItem setItem(MenuItem item) {
        return item.setVisible(true).setEnabled(true);
    }

    public SubMenu addItemAsSubMenu(Menu menu, JotyApp.JotyMenus id, CharSequence caption, int resId) {
        traceMenuItemID(id.ordinal());
        SubMenu subMenu = menu.addSubMenu(Menu.NONE, id.ordinal(), menu.size(), caption);
        MenuItem menuItem = subMenu.getItem();
        setItem(menuItem);
        menuItem.setIcon(getResources().getDrawable(resId));
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return subMenu;
    }

    public MenuItem addMenuItemToAppMenu(Menu menu, JotyApp.JotyMenus id, CharSequence caption, int resId, boolean isSubMenu) {
        return addMenuItemToAppMenu(menu, id, caption, resId, isSubMenu, null);
    }

    public MenuItem addMenuItemToAppMenu(Menu menu, JotyApp.JotyMenus id, CharSequence caption, int resId, boolean isSubMenu, Action action) {
        return addMenuItemToAppMenu(menu, id.ordinal(), caption, resId, isSubMenu, action);
    }

    public MenuItem addMenuItemToAppMenu(Menu menu, int id, CharSequence caption, int resId, boolean isSubMenu, Action action) {
        if (action != null)
            m_stockedMenuItems.put(id, action);
        return addMenuItemToAppMenu(menu, id, caption, resId, isSubMenu);
    }

    protected void addMenuItemsToAppMenu(Menu menu) {
    }

    protected void addHome(Menu menu) {
        addMenuItemToAppMenu(menu, JotyApp.JotyMenus.HomeMenu, jotyLang("LBL_HOME"), R.drawable.home, false,
                new Action() {
                    public void doIt() {
                        home();
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (navigationNeeded())
            addHome(menu);
        setBack();
        return super.onCreateOptionsMenu(menu);
    }

    protected boolean navigationNeeded() {
        return true;
    }

    protected void setBack() {
        if (getClass() != m_app.m_idleActivityClass)
            getSupportActionBar().setDisplayHomeAsUpEnabled(navigationNeeded());
    }

    protected void addNewRecordMenu(Menu menu) {
        addMenuItemToAppMenu(menu, JotyApp.JotyMenus.NewRecMenu, jotyLang("LBL_NEW"), R.drawable.newrec, false,
                new Action() {
                    public void doIt() {
                        newRecord();
                    }
                });
    }

    protected void newRecord() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Action stockedAction = m_stockedMenuItems.get(id);
        if (stockedAction != null) {
            stockedAction.doIt();
            return true;
        } else if (id == android.R.id.home) {
            doBackAction();
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    protected void doBackAction() {
        m_app.endCurrentNonIdleActivity();
    }

    protected void enableRole(String roleName, Permission permission) {
        m_rolesPerm.put(roleName, permission);
    }

    public boolean getMaxPermission() {
        boolean retVal = true;
        if (m_app.m_userRoles.size() > 0) {
            Permission gotPermission = null;
            for (String role : m_app.m_userRoles) {
                gotPermission = m_rolesPerm.get(role);
                if (gotPermission != null && gotPermission.compareTo(m_permission) > 0) {
                    m_permission = gotPermission;
                    if (m_permission == Permission.all)
                        break;
                }
            }
        } else
            retVal = false;
        return retVal;
    }

    public ParamContext createParamContext() {
        ParamContext retVal = new ParamContext(m_app);
        if (m_common.m_shared)
            retVal.setContextParam("sharingKey", m_common.m_sharingKey);
        return retVal;
    }

    public ParamContext createParamContext(ActivityController source) {
        ParamContext retVal = new ParamContext(m_app);
        retVal.copy(source.m_accessorCoordinates.paramContext);
        return retVal;
    }

    protected void setContextParams() { }

    protected boolean accessorMode() {
        return m_mainActivity.m_accessorMode;
    }

    public BasicPostStatement createContextPostStatement(AccessorCoordinates accessorCoordinatesParam) {
        BasicPostStatement postStatement = null;
        AccessorCoordinates accessorCoordinates = accessorCoordinatesParam == null ? m_accessorCoordinates : accessorCoordinatesParam;
        if (accessorMode()) {
            postStatement = m_app.accessorPostStatement(accessorCoordinates.jotyDialogClassName, accessorCoordinates.jotyPanelIndex,
                    accessorCoordinates.jotyTermName, accessorCoordinates.paramContext, accessorCoordinates.mode);
        }
        return postStatement;
    }

    protected void setLangText(int textViewResId, String text) {
        setText(textViewResId, m_common.appLang(text));
    }

    protected void setText(int textViewResId, String text) {
        View view = findViewById(textViewResId);
        if (view instanceof TextView)
            ((TextView) view).setText(text);
        else
            ((Button) view).setText(text);
    }

    protected void setLangHint(int textViewResId, String text) {
        setHint(textViewResId, m_common.appLang(text));
    }

    protected void setHint(int textViewResId, String text) {
        View view = findViewById(textViewResId);
        if (view instanceof TextView)
            ((TextView) view).setHint(text);
        else
            ((EditText) view).setHint(text);
    }

    public void putFieldAsExtra(JotyResourceCursorAdapter adapter, Bundle m_extras, String fieldName) {
        WrappedField wfield = new WrappedField(m_app);
        adapter.m_cursor.setValueToWField(fieldName, wfield, true);
        m_extras.putString(fieldName, wfield.render());
    }

    public DataMainActivity mainActivity() {
        return m_app.dataMainActivity();
    }

    @Override
    public void finish() {
        finish(false);
    }

    public void finish(boolean forRenewing) {
        beforeFinish(forRenewing);
        super.finish();
    }

    protected void beforeFinish(boolean forRenewing) {}

    /**
     * Gets the bytes from the database through the server and save them as temporary file. Than invoke
     * the system to find an activity for opening the file. To accomplish these tasks it calls
     * {@code getBytesFromDb} and {@code openDocumentFromBytes} on the handler thread.
     *
     * @param tableName           In accessor mode must be null
     * @param targetField
     * @param whereClause
     * @param accessorCoordinates In accessor mode must specified. in Normal mode is ignored.
     */
    protected void openImage(String tableName, String targetField, String whereClause, AccessorCoordinates accessorCoordinates) {
        m_app.beginWaitCursor();
        JotyApp.ResponseHandlersManager respManager = m_app.new ResponseHandlersManager();
        respManager.push(m_app.new ResponseHandler() {
            @Override
            public void handle(boolean result, JotyApp.ResponseHandlersManager respManager) {
                if (result) {
                    m_app.openDocumentFromBytes((byte[]) respManager.getParam("bytes"), "jpg");
                }
            }
        });
        m_app.m_db.getBytesFromDb(
                String.format("SELECT %1$s FROM %2$s WHERE %3$s",
                        targetField, m_app.codedTabName(tableName), whereClause),
                m_mainActivity.createContextPostStatement(accessorCoordinates),
                respManager);
    }


}
