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

import java.awt.Component;
import java.beans.Beans;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.joty.common.JotyTypes;
import org.joty.workstation.app.Application;
import org.joty.workstation.gui.BufferedComboBoxTerm.BufferedComboBox;
import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * It is the class that allows the adding of Joty Visual content into the
 * classes that can have a role of container, and, at the same time, prepare,
 * set and initialize interconnections from the various instances such that The
 * Joty architecture is preserved.
 * <p>
 * 
 */
public final class Factory {
	
	public static boolean m_DnDfeatures;

	/**
	 * Adds an {@code AnalogicalRowSelector} object to a {@code DataScrollingPanel} instance.
	 * @param containerPanel the target container.
	 * @param rowKeyValue	the value of the grid key corresponding to the row to which it is associated.
	 */
	public static JPanel addAnalogicalRowSelector(DataScrollingPanel containerPanel, long rowKeyValue) {
		AnalogicalRowSelector retObj = new AnalogicalRowSelector(containerPanel.getGridManager(), rowKeyValue, m_DnDfeatures);
		return retObj;
	}

	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source criteriaPanel new CriteriaPanel()
	 *
	 */
	public static CriteriaPanel addCriteriaPanel(CriteriaPanel criteriaPanel) {
		return criteriaPanel; // ready for implementations of side effects
	}

	/**
	 * Add a CriteriaPanel to a SeacherPanel layout
	 * 
	 * @param searcherPanel
	 * @param criteriaPanel
	 * @return the added object
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source searcherPanel m_searcherPanel
	 * @wbp.factory.parameter.source criteriaPanel new CriteriaPanel()
	 * 
	 */
	public static CriteriaPanel addCriteriaPanel(SearcherPanel searcherPanel, CriteriaPanel criteriaPanel) {
		if (!Beans.isDesignTime())
			criteriaPanel.m_dialog = searcherPanel.getDialog();
		criteriaPanel.m_searcherPanel = searcherPanel;
		return criteriaPanel;
	}

	/**
	 * Adds a {@code DataAccessPanel} object to the layout of a {@code Panel} object.
	 * @param contentPanel the target panel
	 * @param panel the adding panel
	 * @return the added panel
	 * @wbp.factory
	 * @wbp.factory.parameter.source contentPanel m_contentPanel
	 * @wbp.factory.parameter.source panel new DataAccessPanel()
	 */
	public static DataAccessPanel addDataAccessPanel(Panel contentPanel, DataAccessPanel panel) {
		return addDataAccessPanel(contentPanel, panel, null, null);
	}

	/**
	 * @param contentPanel
	 * @param panel
	 * @param updateableSet
	 * @return
	 * @wbp.factory
	 * @wbp.factory.parameter.source contentPanel m_contentPanel
	 * @wbp.factory.parameter.source panel new DataAccessPanel()
	 * @wbp.factory.parameter.source updateableSet null
	 */
	public static DataAccessPanel addDataAccessPanel(Panel contentPanel, DataAccessPanel panel, String updateableSet) {
		return addDataAccessPanel(contentPanel, panel, updateableSet, null);
	}

	/**
	 * Adds a {@code DataAccessPanel} to the layout of a content panel.
	 * 
	 * @param contentPanel
	 * @param panel
	 * @param updateableSet
	 *            Used in the case the accessor mode in not set, this is the
	 *            expression of the updateable set (typically a database table
	 *            name). It can be null.
	 * @param completeSet
	 *            Used in the case the accessor mode in not set, this is the
	 *            text of the database query that select the entire set of
	 *            fields. It can be null.
	 * @return the added object
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source contentPanel m_contentPanel
	 * @wbp.factory.parameter.source panel new DataAccessPanel()
	 * @wbp.factory.parameter.source updateableSet null
	 * @wbp.factory.parameter.source completeSet null
	 */
	public static DataAccessPanel addDataAccessPanel(Panel contentPanel, DataAccessPanel panel, String updateableSet, String completeSet) {
		addTermContainerPanel(contentPanel, panel);
		panel.m_dialog.m_currSheet = panel;
		Application m_app = Application.m_app;
		if (panel.m_localAccessor)
			panel.m_panelDataDef = m_app.m_accessor.getPanelDataDef(m_app.m_definingDialog.getClass().getName(), 0);
		panel.setDefinitions(completeSet, updateableSet, (DataAccessDialog) (Beans.isDesignTime() ? new DataAccessDialog() : panel.getDialog()));
		panel.lookForIdFieldTermIndex();
		return panel;
	}


	/**
	 * Tells the framework to put a text column into the main grid of a
	 * {@code TermContainerPanel} in which to resolve the value of a specified
	 * integer upon a specified {@code LiteralStruct} object.
	 * 
	 * @param panel
	 *            The panel which the grid is contained in.
	 * @param fieldName
	 *            the database field name
	 * @param label
	 *            the text in the table header
	 * @param descrSetName
	 *            the name of the LiteralStruct object
	 */
	public static void addComboToGrid(TermContainerPanel panel, String fieldName, String label, String descrSetName) {
		addComponentToGrid(JotyTypes._long, panel, fieldName, 0, label, descrSetName);
	}

	/**
	 * Tells the framework to put a date time column into the main grid of a
	 * {@code TermContainerPanel} for the specified database field.
	 * 
	 * @param panel
	 *            The panel which the grid is contained in.
	 * @param fieldName
	 *            the database field name
	 * @param label
	 *            the text in the table header
	 */
	public static void addDateTimeToGrid(TermContainerPanel panel, String fieldName, String label) {
		addComponentToGrid(JotyTypes._dateTime, panel, fieldName, 0, label);
	}
	/**
	 * Like the {@link #addDateTimeToGrid} method but for a date field.
	 */
	public static void addDateToGrid(TermContainerPanel panel, String fieldName, String label) {
		addComponentToGrid(JotyTypes._date, panel, fieldName, 0, label);
	}

	/**
	 * Like the {@link #addDateTimeToGrid} method but for a logic field.
	 */
	public static void addCheckToGrid(TermContainerPanel panel, String fieldName, String label) {
		addComponentToGrid(JotyTypes._int, panel, fieldName, 1, label);
	}
	/**
	 * Like the {@link #addDateTimeToGrid} method but for a decimal number field.
	 */
	public static void addDecimalToGrid(TermContainerPanel panel, String fieldName, String label) {
		addComponentToGrid(JotyTypes._double, panel, fieldName, 0, label);
	}

	/**
	 * Like the {@link #addDateTimeToGrid} method but for a long integer field.
	 */
	public static void addLongNumToGrid(TermContainerPanel panel, String fieldName, String label) {
		addComponentToGrid(JotyTypes._long, panel, fieldName, 0, label);
	}

	/**
	 * Like the {@link #addDateTimeToGrid} method but for a integer field.
	 */
	public static void addNumToGrid(TermContainerPanel panel, String fieldName, String label) {
		addComponentToGrid(JotyTypes._int, panel, fieldName, 0, label);
	}

	/**
	 * Like the {@link #addDateTimeToGrid} method but for a text field.
	 */
	public static void addTextToGrid(TermContainerPanel panel, String fieldName, int iSize, String label) {
		addComponentToGrid(JotyTypes._text, panel, fieldName, iSize, label);
	}

	/**
	 * Sets to 50 the size parameter of the {@link #addTextToGrid} method.
	 */
	public static void addTextToGrid(TermContainerPanel panel, String fieldName, String label) {
		addComponentToGrid(JotyTypes._text, panel, fieldName, 50, label);
	}

	/**
	 * Adds a {@code SearcherPanel} to the layout of a contentPanel.
	 * @wbp.factory
	 * @wbp.factory.parameter.source contentPanel this
	 * @wbp.factory.parameter.source panel new TableSearcherPanel()
	 */
	public static SearcherPanel addSearcherPanel(Panel contentPanel, SearcherPanel panel) {
		panel.m_dialog = contentPanel.getDialog();
		panel.setAsInsidePanel(true);
		return panel;
	}

	/**
	 * See {@link #addTab(TabbedPane, DataAccessPanel, String, String)}
	 * @wbp.factory
	 * @wbp.factory.parameter.source tabbedPane m_tabbedPane
	 * @wbp.factory.parameter.source panel new DataAccessPanel()
	 */
	public static DataAccessPanel addTab(TabbedPane tabbedPane, DataAccessPanel panel) {
		return addTab(tabbedPane, panel, null, null);
	}

	/**
	 * See {@link #addTab(TabbedPane, DataAccessPanel, String, String)}
	 * @wbp.factory
	 * @wbp.factory.parameter.source tabbedPane m_tabbedPane
	 * @wbp.factory.parameter.source panel new DataAccessPanel()
	 * @wbp.factory.parameter.source updateableSet null
	 */
	public static DataAccessPanel addTab(TabbedPane tabbedPane, DataAccessPanel panel, String updateableSet) {
		return addTab(tabbedPane, panel, updateableSet, updateableSet);
	}

	/**
	 * Adds a tab to a {@code TabbedPane} object and attach a specified
	 * {@code DataAccessPanel} object to it.
	 * 
	 * @param tabbedPane the TabbedPane object 
	 * @param panel the DataAccessPanel object to attach to added tab
	 * @param updateableSet
	 *            Used in the case the accessor mode in not set, this is the
	 *            expression of the updateable set (typically a database table
	 *            name) of the DataAccessPanel object. It can be null.
	 * @param completeSet
	 *            Used in the case the accessor mode in not set, this is the
	 *            text of the database query (associated to the DataAccessPanel
	 *            object) that select the entire set of fields. It can be null.
	 * @return the attached DataAccessPanel object
	 * @see TabbedPane
	 * @see DataAccessPanel
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source tabbedPane m_tabbedPane
	 * @wbp.factory.parameter.source panel new DataAccessPanel()
	 * @wbp.factory.parameter.source updateableSet null
	 * @wbp.factory.parameter.source completeSet null
	 */
	public static DataAccessPanel addTab(TabbedPane tabbedPane, DataAccessPanel panel, String updateableSet, String completeSet) {
		tabbedPane.getDialog().m_definedPanels++;
		if (Beans.isDesignTime() || panel.getMaxPermission())
			if (panel.m_permission.compareTo(DataAccessPanel.Permission.no_access) != 0) {
				MultiPanelDialog dialog = tabbedPane.getDialog();
				panel.m_dialog = dialog;
				int tabPos = tabbedPane.getTabCount();
				Application m_app = Application.m_app;
				panel.m_panelIdxInDialog = dialog.m_definedPanels - 1;
				if (panel.m_localAccessor)
					panel.m_panelDataDef = m_app.m_accessor.getPanelDataDef(m_app.m_definingDialog.getClass().getName(), tabPos);
				if (panel.m_operative)
					dialog.m_tabPanePanelClassIndex.add(tabPos);
				dialog.m_dataPanels.add(dialog.m_tabPanePanelClassIndex.get(tabPos), panel);
				if (tabbedPane.getTabCount() == 0)
					panel.m_dialog.m_currSheet = panel;
				panel.setDefinitions(completeSet, updateableSet, tabbedPane.getTabCount() == 0 ? dialog : null);
				panel.lookForIdFieldTermIndex();
				if (panel instanceof DataScrollingPanel)
					if (!(panel instanceof NavigatorPanel))
						if (!((DataScrollingPanel) panel).m_builtInGridManager)
							panel.m_gridManager = tabbedPane.getDialog().getGridManager();
			}
		return panel;
	}

	/**
	 * Lets the contentPanel to have {@code m_dialog} assigned with the
	 * {@code JotyDialog} detected by the {@code TermContainerPanel} specified.
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source contentPanel m_contentPanel
	 * @wbp.factory.parameter.source panel new TermContainerPanel()
	 */
	public static TermContainerPanel addTermContainerPanel(Panel contentPanel, TermContainerPanel panel) {
		panel.m_dialog = contentPanel.getDialog();
		return panel;
	}

	/**
	 * Creates a {@code BufferedComboBox} object and adds it to the layout of a
	 * {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code BufferedComboBoxTerm} object that will
	 *            be created by the method for wrapping the component.
	 * @param fieldName
	 *            the database field name
	 * @return the created object
	 * 
	 * @see BufferedComboBox
	 * @see TermContainerPanel
	 * @see BufferedComboBoxTerm
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myCombo"
	 * @wbp.factory.parameter.source fieldName null
	 */
	public static BufferedComboBox createBufferedComboBox(TermContainerPanel panel, String termName, String fieldName) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		return (BufferedComboBox) wbeTermContainerPanel(panel).createBufferedComboTerm(termPrms).m_cmb;
	}

	/**
	 * Creates a {@code CheckBox} object and adds it to the layout of a
	 * {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code CheckTerm} object that will be created
	 *            by the method for wrapping the component.
	 * @param fieldName
	 *            the database field name
	 * @return the created object
	 * 
	 * @see CheckBox
	 * @see TermContainerPanel
	 * @see CheckTerm
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myCheck"
	 * @wbp.factory.parameter.source fieldName null
	 */
	public static CheckBox createCheck(TermContainerPanel panel, String termName, String fieldName) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		return wbeTermContainerPanel(panel).createCheckTerm(JotyTypes._int, termPrms).m_chk;
	}

	/**
	 * Creates a {@code CheckBoxList} object and adds it to the layout of a
	 * {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code CheckListTerm} object that will be
	 *            created by the method for wrapping the component.
	 * @param descrSetName
	 *            the name of the LiteralStruct object used to populate the
	 *            verbose column of the object.
	 * @param dataTable
	 *            used if the accessor mode is not set, is the expression of the
	 *            updateable set (typically a database table name)
	 * @param dataSetDatumFld
	 *            the name of the database field that hosts the datum managed by
	 *            the CheckBoxes column.
	 * @param mainEntityKeyField
	 *            possible name of the foreign key field
	 * @param mainTermName
	 *            name of the Term (a CheckListTerm object that wraps another
	 *            component that behaves like a master selection component)
	 * @param setLoadOnly
	 *            true if the component is required to make no store of data,
	 *            when the target panel is an instance of the
	 *            {@code DataAccessPanel} and the 'save' command is dispatched
	 *            to it.
	 * @return the created object
	 * 
	 * @see CheckBoxList
	 * @see TermContainerPanel
	 * @see CheckListTerm
	 * @see org.joty.workstation.app.Application.LiteralStruct
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel this
	 * @wbp.factory.parameter.source termName "myCheckBoxList"
	 * @wbp.factory.parameter.source descrSetName null
	 * @wbp.factory.parameter.source dataTable null
	 * @wbp.factory.parameter.source dataSetDatumFld null
	 * @wbp.factory.parameter.source mainEntityKeyField null
	 * @wbp.factory.parameter.source mainTermName null
	 * @wbp.factory.parameter.source setLoadOnly false
	 */
	public static CheckBoxList createCheckBoxList(TermContainerPanel panel, String termName, String descrSetName, String dataTable, 
											String dataSetDatumFld, String mainEntityKeyField, String mainTermName, boolean setLoadOnly) {
		TermParams termPrms = panel.new TermParams(termName, null);
		termPrms.m_descrSetName = descrSetName;
		termPrms.m_dataTable = dataTable;
		termPrms.m_targetDatumField = dataSetDatumFld;
		termPrms.mainEntityKeyField = mainEntityKeyField;
		termPrms.mainTermName = mainTermName;
		termPrms.m_dataSetLoadOnly = setLoadOnly;
		return (CheckBoxList) checkAdmittance(panel, wbeTermContainerPanel(panel).createCheckListTerm(JotyTypes._none, termPrms).m_checkBoxList);
	}

	/**
	 * Creates a {@code ComboBox} object and adds it to the layout of a
	 * {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code ComboBoxTerm} object that will be
	 *            created by the method for wrapping the component.
	 * @param fieldName
	 *            the database field name
	 * @param descrSetName
	 *            the name of the LiteralStruct object used to populate the object.
	 * @return the created object
	 * 
	 * @see ComboBox
	 * @see TermContainerPanel
	 * @see ComboBoxTerm
	 * @see org.joty.workstation.app.Application.LiteralStruct
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myCombo"
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source textValue false
	 * @wbp.factory.parameter.source descrSetName null
	 */
	public static ComboBox createComboBox(TermContainerPanel panel, String termName, String fieldName, boolean textValue, String descrSetName) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		termPrms.m_descrSetName = descrSetName;
		return createComboTerm(panel, termPrms, textValue).m_cmb;
	}


	/**
	 * Creates a {@code JotyTextField} object to manage a date typed datum and
	 * adds it to the layout of a {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code TextTerm} object that will be created
	 *            by the method for wrapping the component.
	 * @param fieldName
	 *            the database field name
	 * @return the created object
	 * 
	 * @see JotyTextField
	 * @see TermContainerPanel
	 * @see TextTerm
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myDate"
	 * @wbp.factory.parameter.source fieldName null
	 */
	public static JotyTextField createDate(TermContainerPanel panel, String termName, String fieldName) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		return wbeTermContainerPanel(panel).createTextTerm(JotyTypes._date, termPrms).m_edit;
	}


	/**
	 * Like the {link {@link #createDate(TermContainerPanel, String, String)} method but for date time typed datum.
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myDateTime"
	 * @wbp.factory.parameter.source fieldName null
	 */
	public static JotyTextField createDateTime(TermContainerPanel panel, String termName, String fieldName) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		return wbeTermContainerPanel(panel).createTextTerm(JotyTypes._dateTime, termPrms).m_edit;
	}

	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myNum"
	 * @wbp.factory.parameter.source fieldName null
	 */
	public static JotyTextField createDecimal(TermContainerPanel panel, String termName, String fieldName) {
		return createDecimal(panel, termName, fieldName, 0);
	}

	/**
	 * Like the {@link #createDate(TermContainerPanel, String, String)} method but for a decimal number.
	 * 
	 * @param iSize Is the number of character that can be inputed in the box. It can be omitted (if so zero is assumed that means no limit).
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myNum"
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source iSize 0
	 */
	public static JotyTextField createDecimal(TermContainerPanel panel, String termName, String fieldName, int iSize) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		termPrms.m_len = iSize;
		return wbeTermContainerPanel(panel).createTextTerm(JotyTypes._double, termPrms).m_edit;
	}

	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName null
	 * @wbp.factory.parameter.source loadOnly false
	 */
	public static JotyTextField createDateRenderer(TermContainerPanel panel, String termName, boolean loadOnly) {
		return createDateRenderer(panel, termName, termName, loadOnly);
	}

	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName null
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source loadOnly false
	 */
	public static JotyTextField createDateRenderer(TermContainerPanel panel, String termName, String fieldName, boolean loadOnly) {
		return createDateRenderer(panel, termName, fieldName, null, loadOnly);
	}

	/**
	 * It is Like the 
	 * {@link #createDate(TermContainerPanel, String, String)}
	 * method but for a read-only component that, however, can get value for a
	 * context parameter and that is enable to have its field saved in the save
	 * action.
	 * @param paramName name of the context parameter which the value is got from
	 * @param loadOnly true if the database field doesn't receive value from the component
	 * @param fieldName May be omitted only if {@code paramName} is omitted too
	 * 
	 * @see org.joty.common.ParamContext
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName null
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source paramName null
	 * @wbp.factory.parameter.source loadOnly false
	 */
	public static JotyTextField createDateRenderer(TermContainerPanel panel, String termName, String fieldName, String paramName, boolean loadOnly) {
		return (JotyTextField) createComponentRenderer(panel, termName, fieldName, JotyTypes._date, paramName, 0, null, loadOnly);
	}
	
	
	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myDDRenderer"
	 * @wbp.factory.parameter.source descrSetName null
	 * @wbp.factory.parameter.source loadOnly false
	 */
	public static ComboBox createDescrRenderer(TermContainerPanel panel, String termName, String descrSetName, boolean loadOnly) {
		return createDescrRenderer(panel, termName, termName, descrSetName, loadOnly);
	}

	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myDDRenderer"
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source descrSetName null
	 * @wbp.factory.parameter.source loadOnly false
	 */
	public static ComboBox createDescrRenderer(TermContainerPanel panel, String termName, String fieldName, String descrSetName, boolean loadOnly) {
		return createDescrRenderer(panel, termName, fieldName, descrSetName, null, loadOnly);
	}

	/**
	 * It is Like the
	 * {@link #createComboBox(TermContainerPanel, String, String, boolean, String)}
	 * method but for a read-only component that, however, can get value for a
	 * context parameter and that is enable to have its field saved in the save
	 * action.
	 * @param paramName name of the context parameter which the value is got from. May be omitted.
	 * @param loadOnly true if the database field doesn't receive value from the component
	 * @param fieldName May be omitted only if {@code paramName} is omitted too
	 * @see org.joty.common.ParamContext
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myDDRenderer"
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source descrSetName null
	 * @wbp.factory.parameter.source paramName null
	 * @wbp.factory.parameter.source loadOnly false
	 */
	public static ComboBox createDescrRenderer(TermContainerPanel panel, String termName, String fieldName, String descrSetName, 
												String paramName, boolean loadOnly) {
		return (ComboBox) createComponentRenderer(panel, termName, fieldName, JotyTypes._long, paramName, 0, descrSetName, loadOnly);
	}

	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName null
	 */
	public static JotyTextField createHiddenLong(TermContainerPanel panel, String termName) {
		return createHiddenLong(panel, termName, null);
	}

	/**
	 * Creates an hidden {@code JotyTextField} object and adds it to a
	 * {@code TermContainerPanel} object
	 * 
	 * @param panel
	 *            The target panel
	 * @param termName
	 *            the name of the {@code TextTerm} object that will be created
	 *            by the method for wrapping the component.
	 * @param paramName
	 *            name of the context parameter which the value is got from. May
	 *            be omitted.
	 * @return the created object
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName null
	 * @wbp.factory.parameter.source paramName null
	 */
	public static JotyTextField createHiddenLong(TermContainerPanel panel, String termName, String paramName) {
		JotyTextField retVal = createLongNumTerm(panel, termName, termName, 0).m_edit;
		Term term = panel.term(termName);
		term.setReadOnly(true);
		if (!Beans.isDesignTime() && paramName != null)
			setTermValuedFromParam(panel, term, paramName, JotyTypes._long);
		return retVal;
	}

	/**
	 * Creates a {@code BlobComponent} object and adds it to the layout of a
	 * {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code BlobTerm} object that will be created
	 *            by the method for wrapping the component.
	 * @param fieldName
	 *            the database field name
	 * @param fileExt
	 *            the extension of the file as this will be saved by the
	 *            component.
	 * @param verboseFileType
	 *            the file type as presented by the
	 *            {@code BlobComponent.buildFileChooser}
	 * @return the created object
	 * 
	 * @see BlobComponent
	 * @see TermContainerPanel
	 * @see BlobTerm
	 * @see BlobComponent#buildFileChooser
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myBlob"
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source fileExt null
	 * @wbp.factory.parameter.source verboseFileType null
	 */
	public static BlobComponent createBlobComponent(TermContainerPanel panel, String termName, String fieldName, 
													String fileExt, String verboseFileType) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		BlobComponent component = wbeTermContainerPanel(panel).createBlobTerm(JotyTypes._blob, termPrms).m_blobComponent;
		component.m_fileExt = fileExt;
		component.m_verboseFileType = verboseFileType;
		return component;
	}

	/**
	 * Like the
	 * {@link #createBlobComponent(TermContainerPanel, String, String, String, String)}
	 * but for the creation of an {@code ImageComponent} object and its
	 * {@code ImageTerm} wrapper object.
	 * 
	 * @param mainImageField
	 *            the name of the database field holding the actual image
	 * @param previewImageField
	 *            the name of the database field holding the preview image
	 * 
	 * @see ImageComponent
	 * @see ImageTerm
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myImage"
	 * @wbp.factory.parameter.source mainImageField null
	 * @wbp.factory.parameter.source previewImageField null
	 * @wbp.factory.parameter.source fileExt null
	 * @wbp.factory.parameter.source verboseFileType null
	 */
	public static ImageComponent createImageComponent(TermContainerPanel panel, String termName, String mainImageField, 
														String previewImageField, String fileExt, String verboseFileType) {
		TermParams termPrms = panel.new TermParams(termName, mainImageField);
		ImageComponent component = (ImageComponent) wbeTermContainerPanel(panel).createImageTerm(JotyTypes._blob, termPrms, previewImageField).m_blobComponent;
		component.m_fileExt = fileExt;
		component.m_verboseFileType = verboseFileType;
		return component;
	}

	/**
	 * Creates a {@code List} object and adds it to the layout of a
	 * {@code Panel} object.
	 * 
	 * @param panel
	 *            the target Panel object
	 * @param masterList
	 *            true if the List object created has the role of master list
	 *            for the panel
	 * @return the created object
	 * 
	 * @see List
	 * @see Panel
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel this
	 * @wbp.factory.parameter.source masterList false
	 */
	public static List createList(Panel panel, boolean masterList) {
		List listP = new List(panel, null);
		if (masterList) {
			panel.m_gridManager = Application.m_app == null ? 
					new GridManager() : 
					new GridManager((TermContainerPanel) ((panel instanceof SearcherPanel) ? ((SearcherPanel) panel).m_criteriaPanel : panel));
			panel.m_gridManager.m_listComponent = listP;
		}
		return listP;
	}

	/**
	 * Creates a {@code List} object and adds it to
	 * the layout of a {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code ListTerm} object that will be created
	 *            by the method for wrapping the component.
	 * @param descrSetName
	 *            the name of the LiteralStruct object used to populate the
	 *            object. It may be null.
	 * @param dataTable
	 *            May be used if {@code DescrSetName} is null. It is
	 *            used if the accessor mode is not set. It is the expression of the
	 *            updateable set (typically a database table name).
	 * @param dataSetDatumFld
	 *            May be used if {@code DescrSetName} is null. It is the name of the database field that hosts the datum
	 *            associated to the buffer row key.
	 * @return the created object
	 * 
	 * @see List
	 * @see TermContainerPanel
	 * @see ListTerm
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel this
	 * @wbp.factory.parameter.source termName "myList"
	 * @wbp.factory.parameter.source descrSetName null
	 * @wbp.factory.parameter.source dataTable null
	 * @wbp.factory.parameter.source dataSetDatumFld null
	 */
	public static List createList(TermContainerPanel panel, String termName, String descrSetName, String dataTable, String dataSetDatumFld) {
		TermParams termPrms = panel.new TermParams(termName, null);
		termPrms.m_descrSetName = descrSetName;
		termPrms.m_dataTable = dataTable;
		termPrms.m_targetDatumField = dataSetDatumFld;
		return (List) checkAdmittance(panel, wbeTermContainerPanel(panel).createListTerm(JotyTypes._none, termPrms).m_list);
	}

	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myLong"
	 * @wbp.factory.parameter.source fieldName null
	 */
	public static JotyTextField createLongNum(TermContainerPanel panel, String termName, String fieldName) {
		return createLongNum(panel, termName, fieldName, 0);
	}

	/**
	 * Like the {link {@link #createDate(TermContainerPanel, String, String)} method but for a long integer number.
	 * @param iSize Is the number of character that can be inputed in the box. It can be omitted (zero is assumed).

	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myLong"
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source iSize 0
	 */
	public static JotyTextField createLongNum(TermContainerPanel panel, String termName, String fieldName, int iSize) {
		return createLongNumTerm(panel, termName, fieldName, iSize).m_edit;
	}

	/**
	 * Like the {link {@link #createDate(TermContainerPanel, String, String)} method but for a currency amount.
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myMoney"
	 * @wbp.factory.parameter.source fieldName null
	 */
	public static JotyTextField createMoney(TermContainerPanel panel, String termName, String fieldName) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		termPrms.m_currency = true;
		return wbeTermContainerPanel(panel).createTextTerm(JotyTypes._double, termPrms).m_edit;
	}

	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myNum"
	 * @wbp.factory.parameter.source fieldName null
	 */
	public static JotyTextField createNum(TermContainerPanel panel, String termName, String fieldName) {
		return createNum(panel, termName, fieldName, 0);
	}

	/**
	 * Like the {link {@link #createDate(TermContainerPanel, String, String)} method but for a integer number.
	 * @param iSize Is the number of character that can be inputed in the box. It can be omitted (if so zero is assumed that means no limit).
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myNum"
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source iSize 0
	 */
	public static JotyTextField createNum(TermContainerPanel panel, String termName, String fieldName, int iSize) {
		return createNumTerm(panel, termName, fieldName, iSize).m_edit;
	}

	/**
	 * Creates a {@code RadioButton} object with the role of datum holder (as master radio, indeed) and
	 * adds it to the layout of a {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code MasterRadioTerm} object that will be created
	 *            by the method for wrapping the component.
	 * @param fieldName
	 *            the database field name holding the datum (an integer)
	 * @return the created object
	 * 
	 * @see RadioButton
	 * @see TermContainerPanel
	 * @see MasterRadioTerm
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myRadio"
	 * @wbp.factory.parameter.source fieldName null
	 */
	public static RadioButton createGroupMasterRadio(TermContainerPanel panel, String termName, String fieldName) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		return wbeTermContainerPanel(panel).createMasterRadioTerm(JotyTypes._dbDrivenInteger, termPrms).m_btn;
	}

	/**
	 * Creates a {@code RadioButton} object with the role of datum holder (as master radio, indeed) and
	 * adds it to the layout of a {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code RadioTerm} object that will be created
	 *            by the method for wrapping the component.
	 * @param masterTermName
	 *            the name of the {@code Term} object that wraps the master radio
	 * @return the created object
	 * 
	 * @see RadioButton
	 * @see TermContainerPanel
	 * @see RadioTerm
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myRadio"
	 * @wbp.factory.parameter.source masterTermName null
	 */
	public static RadioButton createRadioForGroup(TermContainerPanel panel, String termName, String masterTermName) {
		TermParams termPrms = panel.new TermParams(termName, null);
		RadioTerm newTerm = wbeTermContainerPanel(panel).createRadioTerm(JotyTypes._none, termPrms);
		if (!Beans.isDesignTime()) {
			MasterRadioTerm masterTerm = ((MasterRadioTerm) panel.term(masterTermName));
			masterTerm.addRadioToGrp(termName);
			panel.implementDependency(termName);
		}
		return newTerm.m_btn;
	}

	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source dialog this
	 */
	public static SearcherPanel createSearcherPanel(JotyDialog dialog) {
		return new SearcherPanel(dialog);
	}

	/**
	 * Creates a {@code Table}. A {@code GridManager} object is also created and
	 * the Table object is assigned to it.
	 * 
	 * @param panel the Panel object to which the created GridManager instance is assigned 
	 * @return the created Table object
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel this
	 */
	public static Table createTable(Panel panel) {
		panel.m_gridManager = Beans.isDesignTime() ? 
				new GridManager() : 
				new GridManager((TermContainerPanel) ((panel instanceof SearcherPanel) ? ((SearcherPanel) panel).m_criteriaPanel : panel));
		Table table = new Table(panel, null);
		panel.m_gridManager.m_listComponent = table;
		return table;
	}

	/**
	 * Creates a {@code Table} object and adds it to the layout of a
	 * {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code TableTerm} object that will
	 *            be created by the method for wrapping the component.
	 * @return the created object
	 * 
	 * @see Table
	 * @see TermContainerPanel
	 * @see TableTerm
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel this
	 * @wbp.factory.parameter.source termName "myTable"
	 */
	public static Table createTable(TermContainerPanel panel, String termName) {
		return createTable(panel, termName, null, null, null, null);
	}

	/**
	 * Creates a {@code Table} object and adds it to the layout of a
	 * {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code TableTerm} object that will be created
	 *            by the method for wrapping the component.
	 * @param dataTable
	 *            It is used if the accessor mode is not set. It is the
	 *            expression of the updateable set (typically a database table
	 *            name).
	 * @param dataSetDatumFld
	 *            It is the name of the database field that hosts the datum,
	 *            usually is the id of the set used to populate the rows.
	 *            associated to the buffer row key.
	 * @return the created object
	 * 
	 * @see Table
	 * @see TermContainerPanel
	 * @see TableTerm
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel this
	 * @wbp.factory.parameter.source termName "myTable"
	 * @wbp.factory.parameter.source dataTable null
	 * @wbp.factory.parameter.source dataSetDatumFld null
	 * @wbp.factory.parameter.source mainEntityKeyField null
	 * @wbp.factory.parameter.source mainTermName null
	 */
	public static Table createTable(TermContainerPanel panel, String termName, String dataTable, String dataSetDatumFld, 
												String mainEntityKeyField, String mainTermName) {
		TermParams termPrms = panel.new TermParams(termName, null);
		termPrms.m_dataTable = dataTable;
		termPrms.m_targetDatumField = dataSetDatumFld;
		termPrms.mainEntityKeyField = mainEntityKeyField;
		termPrms.mainTermName = mainTermName;
		return (Table) checkAdmittance(panel, wbeTermContainerPanel(panel).createTableTerm(JotyTypes._none, termPrms).m_table);
	}

	/**
	 * Like the {link {@link #createDate(TermContainerPanel, String, String)}
	 * method but for text.
	 * 
	 * @param iSize
	 *            Is the number of character that can be inputed in the box. It
	 *            can be omitted (zero is assumed).
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myText"
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source iSize 50
	 */
	public static JotyTextField createText(TermContainerPanel panel, String termName, String fieldName, int iSize) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		termPrms.m_len = iSize;
		return wbeTermContainerPanel(panel).createTextTerm(JotyTypes._text, termPrms).m_edit;
	}

	/**
	 * Creates a {@code TextArea} object to manage a date typed datum and adds
	 * it to the layout of a {@code TermContainerPanel} object.
	 * 
	 * @param panel
	 *            the target panel
	 * @param termName
	 *            the name of the {@code TextAreaTerm} object that will be created
	 *            by the method for wrapping the component.
	 * @param fieldName
	 *            the database field name
	 * @param iSize
	 *            Is the number of character that can be inputed in the box.
	 * @return the created object
	 * 
	 * @see TextArea
	 * @see TermContainerPanel
	 * @see TextAreaTerm
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName "myText"
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source iSize 50
	 */
	public static TextArea createTextArea(TermContainerPanel panel, String termName, String fieldName, int iSize) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		termPrms.m_len = iSize;
		return wbeTermContainerPanel(panel).createTextAreaTerm(termPrms).m_textArea;
	}

	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName null
	 * @wbp.factory.parameter.source loadOnly false
	 */
	public static JotyTextField createTextRenderer(TermContainerPanel panel, String termName, boolean loadOnly) {
		return createTextRenderer(panel, termName, termName, loadOnly);
	}

	/**
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName null
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source loadOnly false
	 */
	public static JotyTextField createTextRenderer(TermContainerPanel panel, String termName, String fieldName, boolean loadOnly) {
		return createTextRenderer(panel, termName, fieldName, null, 50, loadOnly);
	}

	/**
	 * /** It is Like the
	 * {@link #createText(TermContainerPanel, String, String, int)} method but
	 * for a read-only component that, however, can get value for a context
	 * parameter and that is enable to have its field saved in the save action.
	 * 
	 * @param paramName
	 *            name of the context parameter which the value is got from. May
	 *            be omitted.
	 * @param loadOnly
	 *            true if the database field doesn't receive value from the
	 *            component
	 * @param fieldName
	 *            May be omitted only if {@code paramName} is omitted too
	 * @see org.joty.common.ParamContext
	 * 
	 * @wbp.factory
	 * @wbp.factory.parameter.source panel (TermContainerPanel) this
	 * @wbp.factory.parameter.source termName null
	 * @wbp.factory.parameter.source fieldName null
	 * @wbp.factory.parameter.source paramName null
	 * @wbp.factory.parameter.source size 50
	 * @wbp.factory.parameter.source loadOnly false
	 */
	public static JotyTextField createTextRenderer(TermContainerPanel panel, String termName, String fieldName, String paramName, int size, boolean loadOnly) {
		return (JotyTextField) createComponentRenderer(panel, termName, fieldName, JotyTypes._text, paramName, size, null, loadOnly);
	}

	private static String getHiddenTermName(TermContainerPanel panel) {
		panel.m_hiddenTermsCount++;
		return "JotyHidden" + panel.m_hiddenTermsCount;
	}

	/** Prepare a {@code Term} object with a value got from a context parameter. */
	private static void setTermValuedFromParam(TermContainerPanel panel, Term term, String paramName, int type) {
		String value = panel.contextParameter(paramName);
		if (value != "-1")
			switch (type) {
				case JotyTypes._text:
					term.contextValue().setValue(value);
					break;
				case JotyTypes._long:
					term.contextValue().setValue(Long.parseLong(value));
					break;
				case JotyTypes._date:
					term.contextValue().setValue(java.sql.Date.valueOf(value.replace("'", "")));
					break;
			}
		term.setAsControlTerm();
		term.m_ctrlTermInitedByParam = true;
	}

	// wbeTermContainerPanel(..): ...in order to solve wbe trouble
	private static TermContainerPanel wbeTermContainerPanel(TermContainerPanel panel) {
		return panel == null ? (new TermContainerPanel()) : panel;
	}

	private static void addComponentToGrid(int type, TermContainerPanel panel, String fieldName, int iSize, String label) {
		addComponentToGrid(type, panel, fieldName, iSize, label, null);
	}

	private static void addComponentToGrid(int type, TermContainerPanel panel, String fieldName, int iSize, String label, String descrSetName) {
		String name = getHiddenTermName(panel);
		Component component = null;
		switch (type) {
			case JotyTypes._text:
				component = Factory.createText(panel, name, fieldName, iSize);
				break;
			case JotyTypes._date:
				component = Factory.createDate(panel, name, fieldName);
				break;
			case JotyTypes._int:
				component = iSize == 1 ? Factory.createCheck(panel, name, fieldName) : Factory.createNum(panel, name, fieldName);
				break;
			case JotyTypes._long:
				if (descrSetName == null)
					component = Factory.createLongNum(panel, name, fieldName);
				else
					component = Factory.createComboBox(panel, name, fieldName, false, descrSetName);
				break;
			case JotyTypes._dateTime:
				component = Factory.createDateTime(panel, name, fieldName);
				break;
			case JotyTypes._double:
				component = Factory.createDecimal(panel, name, fieldName);
				break;
		}
		if (component != null) {
			panel.term(name).setOnlyLoadingData(true);
			panel.addTermToGrid(name, label);
		}
	}

	private static ComboBoxTerm createComboTerm(TermContainerPanel panel, TermParams termPrms, boolean textValue) {
		return wbeTermContainerPanel(panel).createComboTerm(textValue ? JotyTypes._text : JotyTypes._dbDrivenInteger, termPrms);
	}

	private static Component createComponentRenderer(TermContainerPanel panel, String termName, String fieldName, int type, String paramName, 
													int size, String descrSet, boolean loadOnly) {
		Component retVal = null;
		switch (type) {
			case JotyTypes._text:
				retVal = createText(panel, termName, fieldName, size);
				break;
			case JotyTypes._long:
				retVal = createComboBox(panel, termName, fieldName, false, descrSet);
				break;
			case JotyTypes._date:
				retVal = createDate(panel, termName, fieldName);
				break;
		}
		Term term = panel.term(termName);
		term.setReadOnly(true);
		if (!Beans.isDesignTime() && paramName != null)
			setTermValuedFromParam(panel, term, paramName, type);
		if (loadOnly)
			term.setOnlyLoadingData(true);
		return retVal;
	}
	
	private static JComponent checkAdmittance(TermContainerPanel panel, JComponent component) {
		boolean admittable = true;
		String msg = "";
		if (!(panel instanceof DataAccessPanel)) {
			msg = String.format("Instance of class " + component.getClass().getName() + 
										" can be added only to a %1$s or its descendants !", 
								DataAccessPanel.class.getName());
			admittable = false;
		}
		if (!admittable)
			panel.notifyJotyDesignError(component, msg);
		return component;
	}

	private static TextTerm createLongNumTerm(TermContainerPanel panel, String termName, String fieldName, int iSize) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		termPrms.m_len = iSize;
		return wbeTermContainerPanel(panel).createTextTerm(JotyTypes._dbDrivenInteger, termPrms);
	}

	private static TextTerm createNumTerm(TermContainerPanel panel, String termName, String fieldName, int iSize) {
		TermParams termPrms = panel.new TermParams(termName, fieldName);
		termPrms.m_len = iSize;
		return wbeTermContainerPanel(panel).createTextTerm(JotyTypes._int, termPrms);
	}



}
