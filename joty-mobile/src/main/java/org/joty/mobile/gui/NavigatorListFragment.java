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

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import org.joty.mobile.app.JotyApp;
import org.joty.mobile.gui.JotyListFragment;

/**
 * Not directly connected with data it descends from the {@code JotyListFragment}, just to set itself
 * as implementor of the {@code OnItemLongClickListener} interface, to let itself addressable by the ResultActivity instance
 * and to redirect the simple click action to an alternative method of it, typically dedicated to the "navigation"
 * upon the {@code JotyResultFragment} object.
 */

public abstract class NavigatorListFragment extends JotyListFragment implements android.widget.AdapterView.OnItemLongClickListener {

    protected abstract BaseAdapter createAdapter();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_app = JotyApp.m_app;
     }

    @Override
    protected void init() {
        m_adapter = createAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View retVal = super.onCreateView(inflater, container, savedInstanceState);
        m_listView.setOnItemLongClickListener(this);
        return retVal;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        m_resultActivity.onNavigatorFragmentItemClick(parent, id, m_adapter, view, position);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        m_resultActivity.m_navigatorListFragment = this;
    }

    public void update(JotyListFragment listFragment) {
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }
}
