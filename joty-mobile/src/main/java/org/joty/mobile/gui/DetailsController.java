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


import android.content.Intent;

import org.joty.common.BasicPostStatement;
import org.joty.common.JotyTypes;
import org.joty.common.Utilities;
import org.joty.data.WrappedField;
import org.joty.mobile.app.JotyApp;
import org.joty.gui.WFieldSet;
import org.joty.mobile.data.WResultSet;

import java.util.Vector;


/**
 * The class is an {@code ActivityController} specialized to coordinate the life of a {@code DataDetailsActivity} object.
 * <p>
 * It can be instantiated in every place where an inspecting of a record is required. The controlled Activity is launched by
 * {@code openDetailsActivity} that is the most important method in the class and is invoked also from the DataDetailsActivity
 * object itself, either for switching to another record context or to renew itself.
 * <p>
 * {@code addKeyElem} and more specifically {@code addIntegerKeyElem} are methods to be invoked to inform the framework about
 * the keys of the data context, then these information are used by the {@code getWhereClause} that serves the various data access actions.
 *
 * @see ActivityController
 * @see DataDetailsActivity
 */

public class DetailsController extends ActivityController {

    public String m_id;
    public String m_mainDataTable;
    public String m_definedQuery;
    public String m_finalQuery;
    public String m_IdFieldName;
    public WFieldSet m_keyElems;
    public Vector<WrappedField> m_keyElemDefaults;
    public boolean m_readOnly;
    public boolean m_keysFilterOnReading;

    public DetailsController(Class targetClass) {
        super(targetClass);

        m_IdFieldName = m_app.dataMainActivity().m_IdFieldName;
        m_keyElems = new WFieldSet(m_app);
        m_keyElemDefaults = new Vector<WrappedField>();
    }

    String idFieldName() {
        return m_IdFieldName;
    }

    public boolean isNewRecord() {
        return m_id == null || m_id.compareTo("0") == 0;
    }

    public void setQuery() {
        String mainFilter = getWhereClause(0, false, isNewRecord(), false, false);
        if (m_app.dataMainActivity().m_accessorMode) {
            m_postStatement = m_app.dataMainActivity().createContextPostStatement(m_accessorCoordinates);
            m_postStatement.m_mainFilter = mainFilter;
        } else {
            m_finalQuery = m_definedQuery;
            if (mainFilter.length() > 0)
                m_finalQuery += " WHERE " + mainFilter;
        }
    }

    /**
     * Defines the query to be applied in opening, updating and deleting the record identifying the various different context by means of its parameters value.
     */
    String getWhereClause(long idKey, boolean wideAction, boolean newRecord, boolean saving, boolean deleting) {
        String retStr = "", addingClause;
        boolean preExisting = false;
        boolean addAnd;
        if (saving && !newRecord || !saving && m_keysFilterOnReading || deleting) {
            boolean keyElemIsKeyForActivity;
            for (WrappedField keyElem : m_keyElems.vector) {
                addingClause = "";
                addAnd = false;
                keyElemIsKeyForActivity = idFieldName() != null && idFieldName().length() > 0 && keyElem.m_dbFieldName.compareToIgnoreCase(idFieldName()) == 0;
                if (keyElemIsKeyForActivity && !wideAction) {
                    if (!newRecord || deleting) {
                        if (idKey == 0)
                            idKey = Long.parseLong(m_id);
                        addingClause = String.format("%1$s = %2$d", idFieldName(), idKey);
                        if (preExisting)
                            addAnd = true;
                        preExisting = true;
                    }
                } else {
                    if (!keyElemIsKeyForActivity || !wideAction) {
                        if (keyElem.dataType() == JotyTypes._text) {
                            if (keyElem.m_strVal == null)
                                m_app.warningMsg("Key value for " + keyElem.m_dbFieldName + " is null !");
                            else
                                addingClause = String.format("%1$s = '%2$s'", keyElem.m_dbFieldName, Utilities.sqlEncoded(keyElem.m_strVal));
                        } else
                            addingClause = String.format("%1$s = %2$d ", keyElem.m_dbFieldName, keyElem.integerVal());
                        if (preExisting)
                            addAnd = true;
                        preExisting = true;
                    }
                }
                if (addAnd)
                    retStr += " AND ";
                retStr += addingClause;
            }
        }
        return retStr;
    }

    public void addIntegerKeyElem(String fieldName) {
        addKeyElem(fieldName, JotyTypes._dbDrivenInteger);
    }

    public WrappedField addKeyElem(String fieldName, int dataType) {
        return addKeyElem(fieldName, dataType, true, null);
    }

    public WrappedField addKeyElem(String fieldName, int dataType, boolean contextIdentifying, WrappedField defaultVal) {
        WrappedField wfield = m_keyElems.add(fieldName, dataType);
        if (contextIdentifying)
            if (m_IdFieldName != null && m_IdFieldName.length() > 0)
                m_app.warningMsg("ID key term already specified ");
            else
                m_IdFieldName = fieldName;
        if (defaultVal != null) {
            defaultVal.m_dbFieldName = fieldName;
            m_keyElemDefaults.add(defaultVal);
        }
        return wfield;
    }

    int keyElemsSize() {
        return m_keyElems.vector.size();
    }

    void clearMainData() {
        for (int i = 0; i < keyElemsSize(); i++)
            m_keyElems.get(i).clear(false);
    }

    public void setUp() {
        m_keyElems.get(idFieldName()).setInteger(Long.parseLong(m_id));
        setQuery();
    }

    public void openDetailsActivity(final JotyActivity starterActivity, String id, JotyResourceCursorAdapter adapter) {
        m_id = (id != null && Long.parseLong(id) > 0) ? id : "0";
        boolean goOn = true;
        starterActivity.addExtrasInOpenActivity(m_extras);
        if (starterActivity.getClass() == m_controlledActivityClass)
            ((DataDetailsActivity) starterActivity).forwardExtraExtras();
        else {
            if (starterActivity instanceof DataDetailsActivity)
                ((DataDetailsActivity) starterActivity).m_furtherDetailsInspecting = true;
            if (starterActivity instanceof DataResultActivity && adapter != null)
                goOn = ((DataResultActivity) starterActivity).checkRowForOpeningDetail(adapter, m_extras);
        }
        if (goOn) {
            setUp();
            WResultSet rs = new WResultSet(null, m_finalQuery);
            clearMainData();
            starterActivity.setWaitCursor(true);
            int respManagerCount = getRespManagerCount();
            JotyApp.QueryResponseHandler respHandler = createRespHandler(starterActivity, respManagerCount);
            JotyApp.QueryResponseHandlersManager qRespManager = m_app.m_contextActivity.m_accessorMode ?
                    m_app.new QueryResponseHandlersManager(m_postStatement, respHandler, rs) :
                    m_app.new QueryResponseHandlersManager((String) null, respHandler, rs);
            qRespManager.setSmallBlobsList(m_smallBlobs);
            openRespManager(qRespManager, respManagerCount);
        }
    }

    public void openDetailsActivity(final JotyActivity starterActivity, String id) {
        openDetailsActivity(starterActivity, id, null);
    }

}
