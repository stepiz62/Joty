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
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.joty.mobile.R;
import org.joty.mobile.app.JotyApp;

/**
 * As root for the instances of the {@code android.app.Fragment} class used in the Joty 2.0 framework
 * it just manages the attachment to the container (a {@code JotyResultActivity} object) for getting
 * a reference in the {@code m_resultActivity} member, imposes the implementation of the  android.widget.AbsListView.OnItemClickListener
 * interface to act on the {@code android.view.View} object inflated from the R.layout.fragment_list layout resource file and provides an accessor
 * method to the ListView there modelled and manages the visualization of the TextView, again, there modelled, that informs about the absence of data.
 * <p>
 * Hosts the place holder of the reference to the BaseAdapter implementation that will be used in the subclasses.
 *
 */

public abstract class JotyListFragment extends android.app.Fragment implements android.widget.AbsListView.OnItemClickListener {
    protected AbsListView m_listView;
    protected BaseAdapter m_adapter;
    protected DataResultActivity m_resultActivity;
    JotyApp m_app;


    protected int getResId() {
        return R.layout.fragment_list;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_app = JotyApp.m_app;
        if (! m_app.m_home)
            init();
    }

    protected abstract void init();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(getResId(), container, false);
        m_listView = getListView(view);
        m_listView.setAdapter(m_adapter);
        m_listView.setOnItemClickListener(this);
        manageAuxView(view);
        return view;
    }

    protected void manageAuxView(View view) {
        TextView emptyView = ((TextView) view.findViewById(android.R.id.empty));
        if (isEmptyViewVisible())
            emptyView.setText(m_app.jotyLang("NoData"));
         else
            emptyView.setVisibility(View.INVISIBLE);
    }

    protected boolean isEmptyViewVisible() {
        return false;
    }

    protected ListView getListView(View view) {
        return (ListView) view.findViewById(android.R.id.list);
    }

    ListView getListView() {
        return getListView(getView());
    }

    public void setSelection(int position) {
        getListView().setSelection(position);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onAttach(Activity activity) {
        m_resultActivity = (DataResultActivity) activity;
        super.onAttach(activity);
    }

    public BaseAdapter getAdapter() {
        return m_adapter;
    }
}
