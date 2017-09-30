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
import java.awt.Container;
import java.awt.Rectangle;
import java.beans.Beans;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;

import org.joty.access.PostStatement;
import org.joty.access.Accessor.PanelDataDef;
import org.joty.common.BasicPostStatement;
import org.joty.data.WrappedField;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.JotyDataBuffer.Record;
import org.joty.workstation.gui.DataAccessPanel.ActionOnRowInterface;

/**
 * This class is a root for JPanel instances to be added to a {@code JotyDialog}
 * instance as objects optionally hosting a {@code GridManager} object and a set
 * of {@code AnalogicalSelector} objects.
 * <p>
 * It defines the root behavior for processing the selection of a row of the
 * grid referenced by the optional GridManager instance.
 * <p>
 * It provides a set of methods for getting {@code BasicPostStatement} instances for
 * use with the {@code Accessor} object.
 * <p>
 * It holds the definition of a {@code m_defaultButton} member variable that is
 * assigned to reference the default button of the hosting {@code JotyDialog}
 * instance and that handles press events of the 'Enter' key originated when the
 * focus is owned by an hosted {@code JotyTextField} or
 * {@code JotyPasswordField} object.
 *
 * 
 */
public class Panel extends JPanel {

	public class JotySeparator extends JSeparator {
		public JotySeparator() {
			super();
			if (!Beans.isDesignTime() && m_app.m_macOs) {
				setBorder(new LineBorder(Color.gray));
				setForeground(Color.gray);
			}
		}

		@Override
		public void setBounds(int x, int y, int width, int height) {
			if (!Beans.isDesignTime() && m_app.m_macOs)
				super.setBounds(x, y, width > height ? width : 1, height > width ? height : 1);
			else
				super.setBounds(x, y, width, height);
		}

	}

	public JotyDialog m_dialog;
	public boolean m_storing;
	public GridManager m_gridManager;
	public JButton m_defaultButton;
	public int[] m_gridColumnWidths;
	public boolean m_absoluteGridColumnWidths;
	protected Application m_app;
	protected boolean m_insidePanel = false;
	protected JotyTableTransferHandler m_selectorsTransferHandler;
	public ActionOnRowInterface m_actionOnRowHandler;

	public PostStatement m_queryDefPostStatement = null;
	public PanelDataDef m_panelDataDef;
	public int m_panelIdxInDialog = 0;

	public Panel() {
		super();
		Application.checkWBE(this);
		m_app = Application.m_app;
		setLayout(null);
	}

	@Override
	public Component add(Component comp) {
		Component retVal = super.add(comp);
		if (comp instanceof TermContainerPanel) {
			if (getParent() != null && !(getParent() instanceof TermContainerPanel)) {
				Rectangle rv = getBounds(null);
				rv.setSize(rv.width - 2, rv.height - 2);
				comp.setBounds(rv);
				comp.setLocation(1, 1);
			}
			if (getParent() instanceof SearcherPanel) {
				SearcherPanel searchPanel = (SearcherPanel) getParent();
				searchPanel.m_criteriaPanel = (CriteriaPanel) comp;
				if (searchPanel.m_gridManager != null && searchPanel.m_insidePanel || !(getDialog() instanceof SearcherMultiPanelDialog))
					searchPanel.m_gridManager.setSourcePanel((TermContainerPanel) comp);
				searchPanel.m_criteriaPanel.m_defaultButton = searchPanel.m_defaultButton;
			} else if (comp instanceof TermContainerPanel) {
				if (!Beans.isDesignTime())
					((TermContainerPanel) comp).m_defaultButton = Application.m_app.m_definingDialog.m_defaultButton;
			}
		}
		return retVal;
	}

	protected String appLang(String literal) {
		return Beans.isDesignTime() ? literal : m_app.m_common.appLang(literal);
	}

	/**
	 * see {@link #createContextPostStatement(String, int)}
	 */
	public PostStatement createContextPostStatement() {
		return createContextPostStatement(null, m_panelIdxInDialog);
	}

	/**
	 * see {@link #createContextPostStatement(String, int)}
	 */
	public PostStatement createContextPostStatement(String termName) {
		return createContextPostStatement(termName, m_panelIdxInDialog);
	}

	/**
	 * Creates a {@code PostStatement} object able to direct the Accessor
	 * context either it is relative to a {@code DataAccessPanel} object or
	 * relative to a {@code Term} object in it contained.
	 * 
	 * @param termName
	 *            {@code Term} instance name
	 * @param panelIdx
	 *            zero based index associated to the creation of the panel
	 *            within the JotyDialog object that may be a
	 *            {@code MultiPanelDialog} instance
	 * @return the object created
	 * 
	 * @see PostStatement
	 * @see DataAccessPanel
	 * @see MultiPanelDialog
	 */
	public PostStatement createContextPostStatement(String termName, int panelIdx) {
		PostStatement postStatement = null;
		if (getDialog().m_accessorMode)
			postStatement = m_app.accessorPostStatement(panelIdx == -1 ? null : getDialog(), panelIdx, termName, null);
		return postStatement;
	}

	public PostStatement createQueryDefPostStatement(String termName, String filter, String sortExpr, int panelIdx) {
		PostStatement postStatement = createContextPostStatement(termName, panelIdx);
		if (postStatement != null) {
			postStatement.m_mainFilter = filter;
			postStatement.m_sortExpr = sortExpr;
		}
		return postStatement;
	}

	public PostStatement createQueryDefPostStatement(String termName, String filter, String sortExpr, int panelIdx, String iteration) {
		PostStatement postStatement = createQueryDefPostStatement(termName, filter, sortExpr, panelIdx);
		postStatement.m_iteration = iteration;
		return postStatement;
	}

	public void doClickOnDefaultButton() {
		if (m_defaultButton != null) {
			getDialog().m_actionEnabled = true;
			m_defaultButton.doClick();
		}
	}

	public boolean init() {
		if (m_defaultButton != null)
			m_defaultButton.setBorder(new BevelBorder(0, Color.WHITE, Color.BLACK));
		return true;
	}

	protected void doGuiDataExch(boolean store) {
		m_storing = store;
	}

	public JotyDialog getDialog() {
		return (Beans.isDesignTime() ? new JotyDialog() : Application.getDialog(this));
	}

	protected void getKeyDataFromRow(Record row) {
		getKeyDataFromRow(row, null);
	}

	/**
	 * Updates the {@code m_keyElems} member of the container {@code JotyDialog}
	 * instance with the values got from the grid row passed as parameter.
	 * 
	 * @param row
	 *            the source grid row
	 * @param keyVector
	 *            an optional vector hosting the {@code WrappedField} objects from the
	 *            {@code m_keyElems} member of the class. If missing the one
	 *            only field identifiable in the buffer of the GridManager
	 *            object will be used.
	 * @see GridManager
	 * @see org.joty.workstation.data.JotyDataBuffer
	 */
	protected void getKeyDataFromRow(Record row, Vector<WrappedField> keyVector) {
		WrappedField rowCell;
		WrappedField keyWField;
		Vector<String> keyFields = new Vector<String>();
		if (keyVector != null)
			for (int i = 0; i < keyVector.size(); i++)
				keyFields.add(keyVector.get(i).m_dbFieldName);
		else
			keyFields.add(m_gridManager.m_gridBuffer.m_keyName);
		int keyFieldRowPos;
		for (int i = 0; i < keyFields.size(); i++) {
			keyFieldRowPos = m_gridManager.m_gridBuffer.m_fieldNamesMap.get(keyFields.get(i));
			rowCell = row.m_data.get(keyFieldRowPos);
			keyWField = contextKeyElem(rowCell.dbFieldName());
			if (keyWField != null)
				keyWField.getValFrom(rowCell);
		}
	}
	
	protected WrappedField contextKeyElem(String dbFieldName) {
		return injectedDialog().keyElem(dbFieldName);
	}


	public JotyTableTransferHandler getSelectorsTransferHandler() {
		return m_selectorsTransferHandler;
	}

	public void guiDataExch() {
		guiDataExch(true);
	}

	public void guiDataExch(boolean store) {
		doGuiDataExch(store);
	}

	protected void increaseRect(Container component, int deltaX, int deltaY) {
		Rectangle rect = component.getBounds();
		component.setBounds(rect.x, rect.y, rect.width + deltaX, rect.height + deltaY);
	}

	public JotyDialog injectedDialog() {
		return m_dialog;
	}

	public boolean isAnInsidePanel() {
		return m_insidePanel;
	}

	protected String jotyLang(String literal) {
		return Beans.isDesignTime() ? literal : m_app.m_common.jotyLang(literal);
	}

	public String openModeStr() {
		return getDialog().getMode() == null ? null : getDialog().getMode().toString();
	}

	public void setAsInsidePanel(boolean predicate) {
		m_insidePanel = predicate;
	}

	public void setSelectorsTransferHandler(JotyTableTransferHandler handler) {
		m_selectorsTransferHandler = handler;
	}

	protected void statusChangeProc() {
		if (m_gridManager != null)
			m_gridManager.storeSelection();
	}

}
