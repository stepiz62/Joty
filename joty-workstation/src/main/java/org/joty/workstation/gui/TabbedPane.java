/*
	Copyright (c) 2013-2015, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

	This file is part of Joty 2.0 Workstation.

	Joty 2.0 Workstation is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Joty 2.0 Workstation is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with Joty 2.0 Workstation.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.joty.workstation.gui;

import java.awt.Color;
import java.awt.Component;
import java.beans.Beans;

import javax.swing.Icon;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.joty.workstation.app.Application;

/**
 * The class implements the inner container of the {@code MultiPanelDialog}
 * class.
 * <p>
 * In each of its tabs it 'hosts' a {@code DataAccessPanel} instance and
 * overrides the {@code addTab} method to contextually make the attachment.
 * <p>
 * Then it manages the tab selection change event for making the right decision
 * on several scenarios like abandoning the old panel, possibly managing the
 * ending of the editing session, if the end is needed, by letting the choice to
 * the user, and like reloading data of the target panel if needed.
 * <p>
 * The class manages small unexpected sizing results between different
 * platforms.
 * 
 * @see MultiPanelDialog
 * @see DataAccessPanel
 * 
 */
public class TabbedPane extends JTabbedPane {

	public Application m_app;
	private boolean m_tabsFeeding;
	private Color m_unselectedColor;
	boolean m_regular = true;
	boolean m_explode = false;

	private int m_dx = 9;
	private int m_dy = 3;
	private int m_deltaWidth = 18;
	private int m_deltaHeight = 17;

	public TabbedPane() {
		init();
	}

	public TabbedPane(int top) {
		super(top);
		init();
	}

	@Override
	public void addTab(String title, Icon icon, Component component, String tip) {
		if (	Beans.isDesignTime() || 
				!(component instanceof DataAccessPanel) || 
				((DataAccessPanel) component).m_permission.compareTo(DataAccessPanel.Permission.no_access) != 0) {
			m_tabsFeeding = true;
			MultiPanelDialog dlg = getDialog();
			dlg.m_panelsTitleMap.put(title, dlg.m_dataPanels.size() - 1);
			super.addTab(title, icon, component, tip);
			m_tabsFeeding = false;
		}
	}

	private void explodingProlog() {
		if (!Beans.isDesignTime()) {
			if (m_app.m_dialogsDesignedOnMac) {
				if (!m_app.m_macOs)
					m_regular = false;
			} else {
				if (m_app.m_macOs) {
					m_regular = false;
					m_explode = true;
				}
			}

		}
	}

	@Override
	public Color getBackgroundAt(int index) {
		return getSelectedIndex() == index ? super.getBackgroundAt(index) : m_unselectedColor;
	}

	public MultiPanelDialog getDialog() {
		return (MultiPanelDialog) (Beans.isDesignTime() ? new MultiPanelDialog() : Application.getDialog(this));
	}

	protected void init() {
		getBackground();
		m_unselectedColor = new Color(150, 150, 150);
		m_app = Application.m_app;
		ChangeListener changeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent changeEvent) {
				if (!m_tabsFeeding)
					getDialog().onTabSelectionChanged(changeEvent);
			}
		};
		addChangeListener(changeListener);
		setFocusable(false);
		explodingProlog();
	}

	/**
	 * Returns true if the panel found at the index passed by parameter is the
	 * 'target' panel the currently selected panel or the inverse relation is
	 * true
	 * 
	 * @see TermContainerPanel#setTargetPanel(DataAccessPanel)
	 */
	boolean isLinked(MultiPanelDialog dlg, int index) {
		DataAccessPanel targetPanel = dlg.m_dataPanels.get(dlg.m_tabPanePanelClassIndex.get(index));
		return targetPanel.m_targetPanel == dlg.m_currSheet || 
				dlg.m_currSheet.m_targetPanel == targetPanel || 
				targetPanel.m_targetPanel == dlg.m_currSheet.m_targetPanel && dlg.m_currSheet.m_targetPanel != null;
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		if (m_regular)
			super.setBounds(x, y, width, height);
		else if (m_explode)
			super.setBounds(x - m_dx, y - m_dy, width + m_deltaWidth, height + m_deltaHeight);
		else
			super.setBounds(x + m_dx, y + m_dy, width - m_deltaWidth, height - m_deltaHeight);
	}

	@Override
	public void setLocation(int x, int y) {
		if (m_regular)
			super.setLocation(x, y);
		else if (m_explode)
			super.setLocation(x - m_dx, y - m_dy);
		else
			super.setLocation(x + m_dx, y + m_dy);
	}

	@Override
	public void setSelectedIndex(int index) {
		MultiPanelDialog dlg = getDialog();
		boolean allowChange;
		boolean justEdited = false;
		if (dlg.m_editOrNew_command || dlg.m_newDocument) {
			if (isLinked(dlg, index)) {
				if (dlg.m_currSheet.m_validatingComponents)
					allowChange = true;
				else {
					dlg.m_currSheet.guiDataExch(true);
					if (dlg.m_currSheet.m_panelIdxInDialog == index)
						allowChange = dlg.m_currSheet.validateComponents();
					else
						allowChange = true;
				}
			} else {
				allowChange = dlg.m_currSheet.checkHasDone();
				if (allowChange)
					allowChange = dlg.shouldDo();
			}
			justEdited = true;
		} else
			allowChange = true;
		if (allowChange) {
			if (!dlg.isInitializing() && justEdited) {
				if (dlg.multiTabDocumentExists() && dlg.checkForNormalBehavior() && !dlg.m_new_command && !dlg.m_editOrNew_command) {
					if (dlg.m_currSheet.dataToBeLoaded())
						dlg.m_currSheet.loadData();
				}
				if (!Beans.isDesignTime())
					dlg.m_currSheet.guiDataExch(false);
			}
			super.setSelectedIndex(index);
		}
	}

	@Override
	public void setSize(int width, int height) {
		if (m_regular)
			super.setSize(width, height);
		else if (m_explode)
			super.setSize(width + m_deltaWidth, height + m_deltaHeight);
		else
			super.setSize(width - m_deltaWidth, height - m_deltaHeight);
	}

}
