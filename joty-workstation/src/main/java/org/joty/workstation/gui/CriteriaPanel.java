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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.Beans;

import javax.swing.JComboBox;

import org.joty.data.SearchQueryBuilderFront;
import org.joty.data.WrappedField;

/**
 * Provides a {@code TermContainerPanel} class that help the developer to
 * compose the data searching criteria related to some representative database
 * field, existing in the context of interest and having the evident role of
 * identifying key.
 * <p>
 * The {@code QueryBuilder} class helps collecting any contribute represented by
 * the existence of a {@code Term} object in the Panel and to make it
 * participate in the resulting 'where' clause: this happens by means of the
 * {@link org.joty.data.SearchQueryBuilderFront#addToWhere(WrappedField term)} method invoked by the client
 * {@code SearcherPanel} instance on all the @code Term} objects.
 * <p>
 * Because of this asset the developer can concentrate, mainly, on the choice of
 * the field to be located in the Panel.
 * 
 * @see TermContainerPanel
 * @see org.joty.data.SearchQueryBuilderFront
 * @see SearcherPanel
 */
public class CriteriaPanel extends TermContainerPanel {

	public SearcherPanel m_searcherPanel;

	public CriteriaPanel() {
		this(null);
	}

	public CriteriaPanel(SearcherPanel searcherPanel) {
		m_searcherPanel = searcherPanel;
	}

	public void addOperatorsComboToTerm(final Term term, JComboBox<String> comboBox) {
		comboBox.addItem("");
		comboBox.addItem(">");
		comboBox.addItem("<");
		term.m_operatorsCombo = comboBox;
		comboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				term.m_panel.notifyEditingAction(null);
			}});
	}

	/**
	 * To be overridden in oder to add contribution to the composition of the
	 * gri.
	 * 
	 * @see DataAccessPanel#defineGrid
	 */ 
	public void defineGrid(TermContainerPanel driverPanel) {}

	@Override
	public boolean init() {
		if (m_dialog instanceof SearcherDialog)
			defineGrid(this);
		return super.init();
	}

	@Override
	public void notifyEditingAction(ActionEvent e) {
		if (m_listenForActions) {
			if (!m_dirty)
				if (m_searcherPanel != null) {
					m_dirty = true;
					m_searcherPanel.reset();
				}
			if (injectedDialog().m_progressiveAction || m_app.m_dialogOpeningAsValueSelector) {
				guiDataExch(true);
				m_searcherPanel.doSearch(false);
			}
		}
	}

	public void setOrderByExpr(String definition) {
		if (!Beans.isDesignTime())
			if (m_searcherPanel != null && m_searcherPanel.isAnInsidePanel())
				m_queryBuilder.m_orderByClause = definition;
			else
				m_dialog.m_queryBuilder.m_orderByClause = definition;
	}

	public void setQuerySetDef(String definition) {
		if (!Beans.isDesignTime()) {
			SearchQueryBuilderFront qBuilder = m_searcherPanel != null && m_searcherPanel.isAnInsidePanel() ? m_queryBuilder : m_dialog.m_queryBuilder;
			qBuilder.m_setDefinition = definition;
		}
	}

}
