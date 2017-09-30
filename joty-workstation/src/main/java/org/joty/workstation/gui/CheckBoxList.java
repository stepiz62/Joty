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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.joty.app.LiteralsCollection.DescrStruct;
import org.joty.workstation.app.Application;

/**
 * Extends {@code Table} to implement a check-able list of literals.
 * <p>
 * The model for the embedded JotyJTable object maps the second column on the
 * literals of the {@code LiteralStruct} associated with the
 * {@code CheckListTerm} instance.
 * <p>
 * Then a data hosting vector of integer, of the same size of the list in that
 * way derived, is built.
 * <p>
 * The first column renders a {@code javax.swing.JCheckBox} object that is
 * mapped to the content of the data vector (it behaves checked if the
 * corresponding - topologically speaking - integer is 1). The vector values are
 * set by means of the {@code setChecked} method that is invoked either during
 * the loading of data, managed by the CheListTerm instance, or on a user action
 * on the check-box.
 * <p>
 * A particular implementation is made when the component works as secondary
 * item: if some row is 'checked' the row of the master CheckBoxList,
 * corresponding to the secondary buffer currently used by the CheckListTerm
 * instance, is also checked; typically the primary CheckListTerm instance has
 * the 'dataLoadOnly' attribute set, that is, it doesn't participate in the
 * storage transaction and it has its checks checked only on data loading and as
 * effect of the selection made in the secondary component.
 * 
 * @see org.joty.workstation.app.Application.LiteralStruct
 * @see org.joty.workstation.gui.CheckListTerm
 * 
 */
public class CheckBoxList extends Table {

	class CheckListCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component component;
			if (column == 0) {
				JCheckBox chk = new JCheckBox();
				chk.setSelected((Boolean) value);
				chk.setEnabled(isEditing() && !((GridTerm) m_gridTerm).m_loadOnly);
				component = chk;
			} else
				component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			component.setForeground(m_enableColorOn ? (isSelected ? m_whiteColor : m_enableForeColor) : m_disableForeColor);
			component.setBackground(m_enableColorOn ? (isSelected ? m_blueColor : m_whiteColor) : (isSelected ? m_lightBlueColor : m_lightGreyColor));

			return component;
		}
	}

	class CheckListModel extends DefaultTableModel {

		private static final long serialVersionUID = 1L;

		@Override
		public Class<?> getColumnClass(int col) {
			return getRowCount() > 0 ? getValueAt(0, col).getClass() : String.class;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int pos) {
			return pos == 1 ? m_caption : "";
		}

		@Override
		public int getRowCount() {
			return Application.m_app == null || m_gridTerm.m_literalStruct == null ? 0 : m_truthVect.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			if (col == 1 && row >= descrArray().size() || col == 0 && row >= m_truthVect.size())
				return 0;
			if (col == 1)
				return descrArray().get(row).descr;
			else
				return m_truthVect.get(row);
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return col == 0 && !((GridTerm) m_gridTerm).m_loadOnly;
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			if (isEditing())
				if (column == 0)
					setCheck(row, ((Boolean) aValue) ? 1 : 0);
				else
					super.setValueAt(aValue, row, column);
		}

	}

	public TermContainerPanel m_termContainerPanel;
	Vector<Boolean> m_truthVect;

	public String m_caption;

	public String m_truthFieldName;

	public CheckBoxList(Panel panel, GridTerm term) {
		super(panel, term);
		m_truthVect = new Vector<Boolean>();
		setListener();
	}

	@Override
	protected boolean alignmentAndCellEditingRequired() {
		return false;
	}

	public void buildTruthVector() {
		m_truthVect.removeAllElements();
		if (m_gridTerm.m_literalStruct != null)
			for (int i = 0; i < descrArray().size(); i++)
				m_truthVect.add(new Boolean(false));
	}

	@Override
	public void createModel() {
		m_model = new CheckListModel();
	}

	private Vector<DescrStruct> descrArray() {
		return m_gridTerm.m_literalStruct.m_descrArray;
	}

	public void enableChkList(boolean state) {
		setEnabled(state);
		paintAll(getGraphics());
	}

	public Integer getCheck(int index) {
		return m_truthVect.get(index) ? 1 : 0;
	}

	@Override
	protected DefaultTableCellRenderer getDefaultRenderer(int colIndex) {
		return m_dataTableCellRenderer;
	}

	public long getItemData(int row) {
		return row == -1 ? -1 : descrArray().get(row).id;
	}

	public String getSelectedText() {
		return getText(m_jtable.getSelectedRow());
	}

	public String getText(int row) {
		return descrArray().get(row).descr;
	}

	@Override
	public void initVerboseLayout() {
		buildTruthVector();
		newDataAvailable();
	}

	protected boolean isEditing() {
		return m_gridTerm.m_panel.getDialog().isEditing();
	}

	@Override
	protected void onSelchange(ListSelectionEvent e) {
		TermContainerPanel termContainerPanel = (TermContainerPanel) m_panel;
		if (m_gridTerm != null && termContainerPanel != null) {
			String secondaryTermName = ((GridTerm) m_gridTerm).m_slaveTermName;
			if (secondaryTermName != null)
				termContainerPanel.term(secondaryTermName).refresh();
		}
	}

	@Override
	public void removeAll() {
		m_truthVect.removeAllElements();
	}

	public void setCheck(int index, int val) {
		m_truthVect.set(index, val == 1);
	}

	@Override
	public void setColsProperties() {
		m_colWidths = new int[] { 30, 400 };
		super.setColsProperties();
	}

	@Override
	protected void setDataTableCellRenderer() {
		m_dataTableCellRenderer = new CheckListCellRenderer();
	}

	@Override
	protected void setHeaderListener() {}

	@Override
	protected void setInnerComponentEnabled(boolean truth) {
		getPaneComponent().setEnabled(true);
	}

	public void setListener() {
		m_jtable.addMouseListener(new MouseAdapter() {
			int column;
			int row;
			JTable table;

			private void getCell(MouseEvent event) {
				table = (JTable) event.getSource();
				column = table.columnAtPoint(event.getPoint());
				row = table.rowAtPoint(event.getPoint());
			}

			@Override
			public void mousePressed(MouseEvent event) {
				getCell(event);
				table.getSelectionModel().setSelectionInterval(0, row);
				if (column == 0 && isEditing() && !((GridTerm) m_gridTerm).m_loadOnly) {
					setCheck(row, getCheck(row) == 1 ? 0 : 1);
					m_gridTerm.notifyEditingAction(null);
					if (m_panel instanceof TermContainerPanel)
						((TermContainerPanel) m_panel).notifyEditingAction(null);
				}
				super.mousePressed(event);
				Table.repaintCell(table, row, column);
				TermContainerPanel termContainerPanel = (TermContainerPanel) m_panel;
				if (m_gridTerm != null && termContainerPanel != null) {
					String primaryTermName = ((GridTerm) m_gridTerm).m_mainTermName;
					if (primaryTermName != null) {
						boolean secondarySetIsSet = false;
						for (int i = 0; i < getRowQty(); i++) {
							if (getCheck(i) == 1) {
								secondarySetIsSet = true;
								break;
							}
						}
						termContainerPanel.chkListTerm(primaryTermName).m_checkBoxList.setCheck(m_gridTerm.m_mainIterator, secondarySetIsSet ? 1 : 0);
						termContainerPanel.chkListTerm(primaryTermName).m_checkBoxList.repaintCell(m_gridTerm.m_mainIterator, 0);
					}
				}
			}
		});
	}

	public void setTermData(int iPos, Object rowKey) {}

}
