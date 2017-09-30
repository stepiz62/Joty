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
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.Beans;

import javax.swing.JLabel;
import javax.swing.border.LineBorder;

import org.joty.common.SearchQueryBuilderBack;
import org.joty.data.SearchQueryBuilderFront;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.data.JotyDataBuffer.Record;

/**
 * Responsible to perform a data search on the database this class uses a
 * {@code CriteriaPanel} instance as helper to define the 'where' clause for the
 * final sql expression that, on the other side, for its main body, is defined
 * thorough the template picked up from the configuration file. Actually the
 * helper object instance starts leaving by the construction of the
 * {@code JotyDialog} descendant container object; there, the helper is also
 * added to the layout of the {@code m_criteriaContainer} member object here
 * instantiated.
 * <p>
 * The basic template is designed to support 'pagination' so that, during the
 * search process, the resulting query performs it. The size of the resulting
 * page is also got from the configuration file.
 * <p>
 * The class controls the availability of the search/navigation buttons
 * depending on the effect of the research.
 * 
 * @see Panel#createQueryDefPostStatement(String, String, String, int)
 * @see org.joty.access.Accessor#getQueryFromPostStatement()
 * @see org.joty.common.BasicPostStatement#getQueryFromDataDef(org.joty.access.Accessor.DataDef,
 *      org.joty.access.Accessor)
 */

public class SearcherPanel extends Panel {


	public interface SearcherPanelContainer {
		void searchCallback();
	};

	public Panel m_criteriaContainer;
	public CriteriaPanel m_criteriaPanel;
	public JLabel m_lblCriteria;
	public JLabel m_lblSearchResult;
	public JotyButton m_btnFind;
	public JotyButton m_btnPrevious;
	public JotyButton m_btnSelect;
	public String m_keyFieldName;
	public WrappedField m_detailsKeyWField;
	private boolean m_backward;
	public int m_iteration;
	public boolean m_furtherRecords;
	private String m_lblNextText;
	private String m_lblSearchText;
	public Table m_table;
	public boolean m_initAction;
	private boolean m_firstEventPending;
	private int m_panelContextIndex = 0;

	private int m_expandSign = 1;

	public boolean m_accessorMode;

	public SearcherPanel() {
		this(null);
	}

	public SearcherPanel(JotyDialog dialog) {
		m_dialog = dialog;
		m_initAction = false;
		setLayout(null);
		m_lblCriteria = new JotyLabel(jotyLang("Criteria"));
		m_lblCriteria.setBounds(10, 11, 93, 14);
		add(m_lblCriteria);

		m_btnFind = new JotyButton("");
		m_btnFind.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (m_btnFind.isVisible())
					doSearch(false);
			}
		});
		m_btnFind.setBounds(168, 44, 63, 23);
		m_defaultButton = m_btnFind;
		add(m_btnFind);

		String pageSizeExt = String.format(" (%s)", m_app.m_common.m_paginationPageSize);

		m_btnPrevious = new JotyButton(jotyLang("LBL_PREV2") + pageSizeExt);
		m_btnPrevious.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doSearch(true);
			}
		});
		m_btnPrevious.setBounds(168, 19, 63, 23);
		add(m_btnPrevious);

		m_criteriaContainer = new Panel();
		m_criteriaContainer.setBorder(new LineBorder(new Color(0, 0, 0)));
		m_criteriaContainer.setBounds(10, 28, 138, 32);
		add(m_criteriaContainer);

		m_lblSearchResult = new JotyLabel(jotyLang("SearchResult"));
		m_lblSearchResult.setBounds(10, 71, 126, 14);
		add(m_lblSearchResult);

		m_lblNextText = jotyLang("LBL_NEXT2") + pageSizeExt;
		m_lblSearchText = jotyLang("LBL_SEARCH");
		m_firstEventPending = true;
		if (!Beans.isDesignTime())
			reset();
		m_table = Factory.createTable(this);
		m_table.setBounds(10, 96, 180, 58);
		add(m_table);

		m_btnSelect = new JotyButton(jotyLang("LBL_SELECT"));
		m_btnSelect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rowAction();
			}
		});
		m_btnSelect.setBounds(168, 70, 63, 23);
		add(m_btnSelect);

	}

	public void addLongKeyElem(String name) {
		setKeyFieldName(name);
		m_criteriaPanel.addIntegerKeyElem(name);
	}

	public void addStrKeyElem(String name) {
		if (!Beans.isDesignTime()) {
			setKeyFieldName(name);
			m_criteriaPanel.addStrKeyElem(name);
		}
	}

	/**
	 * Makes the inquiry on the database and loads the resulting data in the
	 * main grid, by invoking {@code loadResult()}, keeping track and depending
	 * on the iteration currently occurring.
	 * <p>
	 * Optionally (if the containing JotyDialog operates in Accessor mode), the
	 * method uses the {@code Accessor} object to dispatch the final statements
	 * to the server or to compose it for a direct access to the dbms.
	 * 
	 * @param backward
	 *            true if the previous iteration is to be re-launched.
	 * @see GridManager#loadData(WResultSet, org.joty.common.BasicPostStatement)
	 */
	
	public void doSearch(boolean backward) {
		m_criteriaPanel.resetDirtyStatus();
		injectIntoBuffer();
		m_backward = backward;
		if (m_backward)
			m_iteration -= 2;
		if (m_keyFieldName == null || m_keyFieldName.length() == 0) {
			m_app.JotyMsg(this, "The key FIELD for row identification on result is needed but not specified in the container dialog");
			return;
		}
		guiDataExch();
		boolean oldCursorIsWaiting = m_app.setWaitCursor(true);
		SearchQueryBuilderFront qBuilder = m_insidePanel ? m_criteriaPanel.m_queryBuilder : getDialog().m_queryBuilder;
		qBuilder.clearWhere();
		for (Term term : m_criteriaPanel.m_terms)
			if (term.dbFieldSpecified())
				qBuilder.addToWhere(term);
		DataAccessPanel connectedDataPanel = null;
		if (!m_insidePanel && getDialog() instanceof SearcherMultiPanelDialog) {
			SearcherMultiPanelDialog dlg = (SearcherMultiPanelDialog) getDialog();
			connectedDataPanel = dlg.m_dataPanels.get(dlg.m_gridMasterClassIndex);
			connectedDataPanel.clearTerms(true);
		}
		emptyResult();
		String query = null;
		if (m_accessorMode) {
			m_queryDefPostStatement = createQueryDefPostStatement(
					null, qBuilder.m_whereClause, qBuilder.m_orderByClause, m_panelContextIndex, String.valueOf(m_iteration));
			if (!m_app.remoteAccessorMode()) {
				m_app.m_accessor.setPostStatement(m_queryDefPostStatement);
				query = m_app.m_accessor.getQueryFromPostStatement();
				m_queryDefPostStatement = null;
			}
		} else
			query = getQuery(qBuilder);

		WResultSet rs = new WResultSet(null, query);
		loadResult(rs);
		updateSearchNavigator(false);
		if (connectedDataPanel != null)
			connectedDataPanel.lookForDataStructure(rs);
		m_app.setWaitCursor(oldCursorIsWaiting);
		((SearcherPanelContainer) getDialog()).searchCallback();
	}
	/**
	 * Invoked only in normal mode (not Accessor mode), it uses the 
	 * {@code SearchQueryBuilderBack.getQuery()} method and forces the  {@code sharingExpr}
	 * parameter to null since it is assumed that no convenience does exist in implementing the shared mode without
	 * the adoption of the Accessor mode (meaning 'remote' accessor mode)
	 * 
	 * @see SearchQueryBuilderBack
	 */
	
	public String getQuery(SearchQueryBuilderFront qBuilder) {
		String retVal = null;
		if (qBuilder.m_setDefinition == null)			
			m_app.JotyMsg(this, "Set definition missing in query builder ! \n(use 'setQuerySetDef()' method in " + 
					this.m_criteriaPanel.getClass() + " or switch the dialog to the AccessorMode)");
		else
			retVal = qBuilder.m_backDelegate.getQuery(qBuilder.m_setDefinition, qBuilder.m_whereClause, 
														qBuilder.m_orderByClause, this.m_iteration, null);
		return retVal;
	}

	protected void emptyResult() {
		m_gridManager.removeAll();
	}

	@Override
	public void guiDataExch() {
		if (m_criteriaPanel == null)
			Application.warningMsg("CriteriaPanel object not specified !");
		else
			m_criteriaPanel.guiDataExch();
	}

	public void initContainer(JotyDialog dialog, Rectangle adjacentPaneRect) {
		m_accessorMode = dialog.m_accessorMode;
		m_btnSelect.setVisible(m_app.m_dialogOpeningAsValueSelector);
		m_btnSelect.setEnabled(false);
		if (m_app.m_dialogOpeningAsValueSelector)
			setSearcherLayout(dialog, adjacentPaneRect);
		if (initAction())
			doSearch(false);
	}

	public void injectIntoBuffer() {
		if (!Beans.isDesignTime())
			m_gridManager.m_gridBuffer.m_searcher = this;
	}

	protected void loadResult(WResultSet rs) {
		m_gridManager.loadData(rs, m_queryDefPostStatement);
		m_gridManager.loadGrid();
	}

	/**
	 * Takes convenient actions associated with the row: by a double click on
	 * the row it opens the details dialog while in a 'Select' action is
	 * explicitly made when the pane serves a Dialog opened as data selector,
	 * then the {@code GridManager.storeSelectedValues} method is called to get inherent selected choice.
	 * 
	 * @see GridManager#storeSelectedValues(String)
	 * @see JotyDialog#openDetailsDialog()
	 */ 
	public void rowAction() {
		boolean oldCursorIsWaiting = m_app.setWaitCursor(true);
		m_detailsKeyWField = m_gridManager.m_gridBuffer.getWField(m_keyFieldName);
		if (m_app.m_dialogOpeningAsValueSelector) {
			m_gridManager.storeSelectedValues(m_keyFieldName);
			injectedDialog().close();
		} else
			m_dialog.openDetailsDialog();
		m_app.setWaitCursor(oldCursorIsWaiting);
	}

	public void setKeyFieldName(String name) {
		m_keyFieldName = name;
		if (!Beans.isDesignTime())
			m_gridManager.m_gridBuffer.setKeyFieldName(name);
	}

	/**
	 * Shows conditionally the JotyDialog content panel basing on the state
	 * represented by the {@code m_expandSign} member variable: every call to
	 * the method inverts its value so that the showing status inverts itself.
	 * <p>
	 * The actor of the invocation can be the
	 * {@code SearcherMultiPaneDialog#m_btnSearcherExpand} button.
	 */
	public void setSearcherLayout(JotyDialog dialog, Rectangle adjacentPaneRect) {
		dialog.m_contentPanel.setVisible(m_expandSign < 0);
		Rectangle searchRect = getBounds();
		if (adjacentPaneRect == null
				// ...or adjacentPaneRect is below
				|| adjacentPaneRect.x < searchRect.x + searchRect.width) {
			int enlargement = (adjacentPaneRect == null ? 0 : adjacentPaneRect.height) + dialog.m_buttonPane.getBounds().height;
			increaseRect(this, 0, enlargement * m_expandSign);
			increaseRect(m_table, 0, enlargement * m_expandSign);
			m_table.newDataAvailableWithoutEvents(true);
		} else
			increaseRect(this, -adjacentPaneRect.width * m_expandSign, 0);
		m_expandSign *= -1;
		JotyButton expandButton = dialog.getSearcherExpandButton();
		if (expandButton != null) {
			expandButton.setToolTipText(jotyLang(m_expandSign > 0 ? "LBL_Explode" : "LBL_Implode"));
			expandButton.setText(m_expandSign > 0 ? "v" : "^");
		}
	}

	@Override
	protected void statusChangeProc() {
		super.statusChangeProc();
		if (m_gridManager != null) {
			Record record = m_gridManager.getRecordBuffer();
			if (record != null)
				getKeyDataFromRow(m_gridManager.getRecordBuffer());
		}
	}

	public String strKeyElemVal(String name) {
		return m_criteriaPanel.keyElem(name).m_strVal;
	}

	/**
	 * Updates the appearance of the panel depending on the status/history of the search
	 * and on whether the containing dialog is set to support 'progressive
	 * action' for the search.
	 * 
	 * @see JotyDialog#m_progressiveAction
	 */
	private void updateSearchNavigator(boolean onReset) {
		if (m_iteration == 0 && m_backward)
			m_backward = false;
		m_btnPrevious.setVisible(m_iteration > 0);
		if (! onReset)
			m_iteration++;
		m_btnFind.setVisible((m_criteriaPanel != null && m_criteriaPanel.m_dirty) ||
							m_iteration == 0 || 
							m_furtherRecords);
		m_btnFind.setText(m_furtherRecords ? m_lblNextText : m_lblSearchText);
		m_firstEventPending = false;
		if (getDialog().m_progressiveAction)
			setToolTipText(jotyLang("ToolTipDirectSearch"));
	}
	
	public void reset() {
		resetIteration();
		updateSearchNavigator(true);
	}

	public void resetIteration() {
		m_iteration = 0;
		m_furtherRecords = false;
	}

	private boolean initAction() {
		return m_insidePanel ? m_initAction : 
								(getDialog().m_initAction || m_app.m_dialogOpeningAsValueSelector);
	}


}
