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

import org.joty.common.ParamContext;
import org.joty.common.BasicPostStatement;
import org.joty.common.Utilities;
import org.joty.common.Utilities.*;
import org.joty.mobile.app.JotyApp;
import org.joty.mobile.data.WResultSet;

/**
 * It provides reference and instantiation capabilities of all the data needed to a {@code DataResultActivity}
 * object or to a {@code DataDetailsActivity} object, during its lifecycle.
 * <p>
 * Uses the {@code JotyApp.dataMainActivity()} method to get the "top" DataMainActivity object, and,
 * by default, initializes the local members defining the operating context (for the "controlled" activity)
 * by replicating the ones of the got DataMainActivity;
 * <p>
 * The class administers and addresses the {@code QueryResponseHandlersManager} object the controlled Activity has to work with.
 * The QueryResponseHandlersManager si created, opened by this class that provides all the necessary methods
 * but would be nice that it were retrieved also by it along the various states the Activity traverses.
 * <p>
 * Look at the following design beliefs:
 * 1) the QueryResponseHandlersManager is used to face with the long running network operation the effect of which
 * is rendered by the "Data" Activity,
 * 2) a controller is needed, that encapsulates dedicated metadata that survive to the Activity traversing
 * several different states as the Android Os determines,
 * 3) the QueryResponseHandlersManager is a repository of working parameters (like the WResultSet object processed) and, as structured object,
 * inspecting the possibility to pass it to the activity as extra is not in the mind of the Joty designer,
 * 4) Joty 2.0 Mobile works with an instance of the {@code android.app.Application} class (JotyApp  class) that is, just by self, a "context container"....
 * ... then the design choice is the following:  to use a map of QueryResponseHandlersManager objects hosted by the JotyApp instance: the {@code m_respManagerCatalog} member;
 * the key of the map is the controlled Activity class name plus, optionally, a tailing count value, based on the quantity of instantiations of
 * Activities of the "controlled" type, made since the navigation left the main activity of the application (an instance of the {@code IdleActivity} class).
 * In the above scenario, this class provides the creator method that relies on the map (see the {@code createRespHandler} method).
 */

public class ActivityController {
    protected JotyApp m_app = JotyApp.m_app;
    public JotyActivity.AccessorCoordinates m_accessorCoordinates;
    public ParamContext m_paramContext;
    public Class m_controlledActivityClass;
    public Bundle m_extras;
    public Stocker m_smallBlobs;
    public boolean m_processing;
    BasicPostStatement m_postStatement;

    public ActivityController(Class targetClass) {
        m_accessorCoordinates = m_app.dataMainActivity().m_accessorCoordinates;
        m_paramContext = m_app.dataMainActivity().m_paramContext;
        m_controlledActivityClass = targetClass;
        m_extras = new Bundle();
        m_smallBlobs = Utilities.m_me.new Stocker();

    }

    public void instantiateOwnData() {
        m_accessorCoordinates = m_app.dataMainActivity().new AccessorCoordinates();
        m_paramContext = m_app.dataMainActivity().createParamContext();
        m_accessorCoordinates.paramContext = m_paramContext;
    }

    /**
     * Creates a {@code JotyApp.QueryResponseHandler} object to be used with a {@code JotyApp.QueryResponseHandlersManager}
     * object that holds the context used by the controlled activity.
     * As factory of such an object it has a synchronous part and a handling part that we know to be
     * asynchronous because forwarded,  through the manager to the {@code WebConn.Connector} object.
     * This method, together with the {@code getRespManagerCount} method realizes the chance
     * (as can be found in the subclasses of this class) to create an instance of QueryResponseHandler
     * equipping it with the way to address its manager (a QueryResponseHandlersManager object) before that one one exists;
     * this is possible thanks to the existence of the {@code JotyApp.m_respManagerCatalog} map and to the count value
     * computable in advance by the {@code getRespManagerCount} and passed as argument to this method.
     *
     * @param starterActivity the calling activity
     * @param respManagerCount the computed count value. By being final can be referenced even in the
     *                         "handling" part of the code: it is passed in the invocation of
     *                         JotyActivity.startActivity where the value is, in turn, passed as extra to
     *                         the opening activity, that this way can address the QueryResponseHandlersManager
     *                         object by means of the {@code JotyActivity.getRespManager} method.
     * @return the created object
     *
     * @see JotyActivity#startActivity
     * @see MenuActivity#finish(boolean)
     */
    protected JotyApp.QueryResponseHandler createRespHandler(final JotyActivity starterActivity, final int respManagerCount) {
       return  m_app.new QueryResponseHandler() {
            @Override
            public void handleQuery(boolean result, WResultSet rs, BasicPostStatement postStatement) {
                if (result) {
                    boolean forRenewing = starterActivity.getClass() == m_controlledActivityClass;
                    starterActivity.startActivity(m_controlledActivityClass, forRenewing, respManagerCount);
                    if (forRenewing)
                        ((MenuActivity) starterActivity).finish(true);
                 }
            }
        };
    }

    public boolean isEnabled() {
        return ! m_processing;
    }

    /**
     * Relies on the {@code JotyApp.RespManagerCountProvider} to get the next count value available.
     * The counting grows at every call and is reset when the navigation goes back to the {@code IdleActivity}
     * class (the main activity of the Joty application).
     * @return the count value
     */
    protected int getRespManagerCount() {
        return m_app.m_respManagerCounters.get(m_controlledActivityClass.getName());
    }

    /**
     * The method, beyond "opening" the manager, eventually, inserts the manager into the map so that
     * it is available to the handler, and indeed to the controlled activity too.
     * @param qRespManager the QueryResponseHandlersManager object
     * @param count the count value preliminarily computed by the {@code getRespManagerCount} method
     *
     * @see JotyApp.QueryResponseHandlersManager
     */

    protected void openRespManager(JotyApp.QueryResponseHandlersManager qRespManager, int count) {
        m_processing = true;
        m_app.m_respManagerCatalog.put(m_controlledActivityClass.getName() + count, qRespManager);
        qRespManager.open();
    }

 }
