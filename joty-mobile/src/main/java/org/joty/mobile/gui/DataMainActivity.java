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

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;

import org.joty.app.Common;
import org.joty.app.Common.*;
import org.joty.mobile.app.JotyApp;
import org.joty.mobile.R;
import org.joty.data.SearchQueryBuilderFront;

/**
 * The class implements the starting point for data inspection and data management for an entity
 * modelled in the database.
 * <p>
 * It hosts, in its layout, View instances (widgets) wrapped, each, by an enclosing {@code Term}
 * instance. The set of Term objects helps to identify the set of data that will open.
 * Then the class offers two different mode of running: as searcher and as contextor.
 * As searcher it processes the Term objects as defining a filtering criteria arbitrarily compiled and
 * the expected record set, in turn, has not a known size so that it is paginated.
 * As contextor the class expects the criteria components to be fully filled and the expected record set
 * must be known in its invariant size, or, at any rate, is not paginated.
 * So that, to the default equipment of the action bar, the class adds, in the first mode, the "search"
 * command and optionally the "new record" command; while in the second mode the "look" command is added.
 * <p>
 * The class, indeed collaborates with two other children of the MenuActivity class: {@code DataResultActivity}
 * and  {@code DataDetailsActivity}. However tha collaboration passes through an intermediary for each of these
 * classes: both have a controller class with which are strictly bound; they are the (@code ResultController}
 * and {@code DetailsController} classes.
 * <p>
 * The {@code setCollaboratorActivities} method allows the developer to specify the two collaborator activities
 * only the first one of which, the DataResultActivity, is required. But the method creates the respective controller
 * that, in turn, will create the controlled activity when the time of its opening comes.
 * <p>
 * The "search" and the "look" command invoke the method of the same name that carries to the execution of the
 * {@code ResultController.accessResult} method, that deliver the request to the Joty Server and will open
 * the DataResultActivity upon the response got from the server.
 * The "new record", instead, directly comes, by means of the {@code DetailsController.openDetailsActivity} method, to
 * the opening of the DataDetailsActivity, opened as empty form.
 * <p>
 * Actually a further running mode of the class does exist: the one that makes it behaving like a context of
 * selection of a record of the entity for which it has been implemented: the {@code m_asSelector}
 * member holds the activation of this behavior. This mode implies to work with the {@code JotyApp.ValuesOnChoice}
 * class for checking if, on resuming, this instance must be closed (because the job has been done).
 * <p>
 * Another important role of this class is contributing in control of the navigation flow.
 * One contribution in this domain is to track itself into the {@code JotyApp.m_dataMainActivityStack}
 * member, a stack of instances of this class.
 * The class put its instance in the stack upon the creation and removes it from the stack on finishing.
 * Another contribution in navigation control is to hold one stack of {@code DataResultActivity} objects
 * and a one stack of {@code DataDetailsActivity} objects, both growing as the navigation goes forward,
 * far from the current activity and decreasing as the navigation turns back, next to the current activity.
 * From this asset, evidently, derives a sort of hierarchy in navigation control where at the top is the application
 * object and then a set of DataMainActivity objects follows as the navigation proceeds. The multiplicity of these objects
 * is possible because, from a DataDetailsActivity context, is possible to open the inspection/management of another entity,
 * typically related to the previous one.
 *
 * @see DataResultActivity
 * @see DataDetailsActivity
 * @see ResultController
 * @see DetailsController
 * @see JotyApp#m_dataMainActivityStack
 * @see JotyApp.ValuesOnChoice
 *
 */

public class DataMainActivity extends MenuActivity {

    public boolean m_asSelector;
    public enum Type {none, searcher, contextor};

    public Class m_dataResultActivityClass;
    protected Class m_dataDetailsActivityClass;
    public String m_orderByClause;
    public String m_IdFieldName;
    public DataResultActivity m_resultActivity;
    public JotyStack<DetailsController> m_detailsControllersStack;
    public DetailsController m_detailsController;
    public JotyStack<ResultController> m_resultControllersStack;
    public ResultController m_resultController;
    SearchQueryBuilderFront m_qBuilder;

    protected void setCollaboratorActivities(Class resultActivityClass, Class detailsActivityClass) {
        boolean success = false;
        String msg = null;
        if (!DataResultActivity.class.isAssignableFrom(resultActivityClass))
            msg = "org.joty.mobile.gui.DataMainActivity.setCollaboratorActivities: first arg must inherit from DataResultActivity class !";
        else if (detailsActivityClass != null && !DataDetailsActivity.class.isAssignableFrom(detailsActivityClass))
            msg = "org.joty.mobile.gui.DataMainActivity.setCollaboratorActivities: second arg must inherit from DataDetailsActivity class !";
        else
            success = true;
        m_resultController = new ResultController(resultActivityClass);
        m_resultController.m_detailsActivityClass = detailsActivityClass;
        if (detailsActivityClass != null) {
            m_detailsController = new DetailsController(detailsActivityClass);
            m_resultController.m_detailsController = m_detailsController;
        }
        if (success) {
            m_dataResultActivityClass = resultActivityClass;
            m_dataDetailsActivityClass = detailsActivityClass;
        } else {
            m_common.m_commitExit = true;
            m_app.warningMsg(msg);
        }
    }

    protected void setType(Type type) {
        m_resultController.m_type = type;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_mainActivity = this;
        m_detailsControllersStack = m_common.new JotyStack<DetailsController>();
        m_resultControllersStack = m_common.new JotyStack<ResultController>();
        m_app.m_dataMainActivityStack.push(this);
        setContextActivity(this);
        m_accessorCoordinates = new JotyActivity.AccessorCoordinates();
        if (m_extras != null)
            m_asSelector = m_extras.getBoolean("asSelector");
        m_qBuilder = new SearchQueryBuilderFront(m_app, m_app.new ClauseContribution());
    }

    @Override
    public void finish() {
        m_app.m_dataMainActivityStack.pop();
        super.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (m_resultController == null)
            return false;
        else {
            switch (m_resultController.m_type) {
                case searcher:
                    addMenuItemToAppMenu(menu, JotyApp.JotyMenus.SearchMenu, jotyLang("LBL_SEARCH"), R.drawable.search, false,
                            new Action() {
                                public void doIt() {
                                    search();
                                }
                            });
                    if (newIsAllowed())
                        addNewRecordMenu(menu);
                    break;
                case contextor:
                    addMenuItemToAppMenu(menu, JotyApp.JotyMenus.SearchMenu, jotyLang("ViewMenu"), R.drawable.look, false,
                            new Action() {
                                public void doIt() {
                                    look();
                                }
                            });
                    break;
            }
            return super.onCreateOptionsMenu(menu);
        }
    }


    protected void search() {
        if (m_resultController.isEnabled()) {
            guiDataExchange();
            m_resultController.searchReset();
            m_resultController.m_mainFilter = getMainFilter();
            resetDirtyStatus();
            if (m_orderByClause == null) {
                m_app.warningMsg("The orderByClause is needed but not specified in the DataMainActivity !");
                return;
            } else
                m_resultController.m_orderByClause = m_orderByClause;

            m_qBuilder.clearWhere();
            for (Term term : m_terms)
                if (term.dbFieldSpecified())
                    m_qBuilder.addToWhere(term);
            m_resultController.m_whereClause = m_qBuilder.m_whereClause;
            m_resultController.m_mainFilter = getMainFilter();
            m_resultControllersStack.push(m_resultController);
            m_resultController.accessResult(this, true, false);
        }
    }

    @Override
    protected void newRecord() {
        m_detailsControllersStack.push(m_detailsController);
        m_detailsController.openDetailsActivity(this, null);
    }

    protected void look() {
        if (m_resultController.isEnabled()) {
            guiDataExchange();
            setContextParams();
            m_resultControllersStack.push(m_resultController);
            m_resultController.doLook(this);
        }
    }


    protected boolean newIsAllowed() {
        return hasPermission(Permission.readWriteAdd);
    }

    public void setOrderByExpr(String definition) {
        m_orderByClause = definition;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!accessIsAllowed()) {
            m_app.langWarningMsg("AccessDenied", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (m_app.m_home)
            return;
        m_app.m_dataModified = false;
        if (accomplished())
            finish();
    }


    public boolean accomplished() {
        return m_asSelector && m_app.m_valuesOnChoice.selected();
    }

    public String getMainFilter() {
        return null;
    }

}
