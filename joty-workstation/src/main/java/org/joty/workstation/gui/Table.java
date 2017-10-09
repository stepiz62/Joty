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
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.beans.Beans;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.*;

import org.joty.access.Logger;
import org.joty.common.JotyTypes;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.JotyDataBuffer;
import org.joty.workstation.gui.DataAccessPanel.Permission;
import org.joty.workstation.gui.GridRowDescriptor.CellDescriptor;
import org.joty.workstation.gui.GridRowDescriptor.RowCellMappingType;
import org.joty.workstation.gui.NavigatorEditorPanel.FieldActionDescriptor;

/**
 * It is a {@code ScrollGridPane} that instantiates a {@code JotyJTable} object
 * as scroll-able object.
 * <p>
 * Provides the instantiated object with several features: as model is used the
 * {@code DataTableModel} class that maps cell data on the data of the
 * {@code JotyDataBuffer} object associated to the container {@code TableTerm},
 * by means of the {@code field} method, and makes meta-data available from the
 * {@code GridRowDescriptor} object conveniently discovered in the TableTerm
 * container or in the {@code GridManager} object depending on which the role of
 * 'this' instance is.
 * <p>
 * The rendering and the horizontal alignment depend on the type of the datum in
 * the column but methods are provided to customize them.
 * <p>
 * The class supports the click action on the header of the JotyJTable object in
 * order to have data re-ordered in the buffer.
 * <p>
 * An inner {@code JotyCellEditor} instance is used as editor when the
 * descriptor of the column states that editing is possible.
 * <p>
 * The class specializes the dispatching role of the double click action: if
 * the column contains an image (that is a {@code _smallBlob} typed datum) the
 * double click action open a ViewerFrame instance for displaying the associated
 * 'heavy' image. If the instance lives in a TableTerm object then the
 * {@code ViewersManager} object of the cell descriptor is used else the one in
 * the associated {@code ImageComponent} will be used.
 * <p>
 * 
 * 
 * @see ScrollGridPane
 * @see JotyJTable
 * @see TableTerm
 * @see GridManager
 * @see ViewerFrame
 * @see ImageComponent
 * @see ViewersManager
 * 
 */

public class Table extends ScrollGridPane {

	class CenterAlignTableCellRenderer extends DataTableCellRenderer {
		public CenterAlignTableCellRenderer() {
			super();
			setHorizontalAlignment(SwingConstants.CENTER);
		}
	}

	class DataTableCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			component.setForeground(m_enableColorOn ? (isSelected ? m_whiteColor : m_enableForeColor) : m_disableForeColor);
			component.setBackground(m_enableColorOn ? (isSelected ? m_blueColor : m_whiteColor) : (isSelected ? m_lightBlueColor : m_lightGreyColor));
			if (m_headerDirty) {
				m_headerDirty = false;
				if (m_sortColIndex >= 0) {
					JTableHeader th = table.getTableHeader();
					TableColumnModel tcm = th.getColumnModel();
					TableColumn tc = tcm.getColumn(m_sortColIndex);
					tc.setHeaderValue(tc.getHeaderValue().toString() + (m_sortAscend ? "  v" : "  ^"));
					th.repaint();
				}
			}
			return component;
		}
	}

	class DataTableModel extends DefaultTableModel {

		GridRowDescriptor m_gridRowDescriptor;
		boolean m_colDescrAsFlag;

		public DataTableModel() {
			super();
			m_gridRowDescriptor = Beans.isDesignTime() ? new GridRowDescriptor() : gridRowDescriptor();
		}

		public int columnType(int col) {
			return getBuffer().getFieldType(mappedWFieldIndex(col));
		}

		private JotyDataBuffer getBuffer() {
			return m_gridTerm == null ? m_panel.m_gridManager.m_gridBuffer : m_gridTerm.m_dataBuffer;
		}

		private CellDescriptor getCellDescriptor(int col) {
			return m_gridRowDescriptor.vector.get(col);
		}

		@Override
		public Class<?> getColumnClass(int col) {
			if (getRowCount() == 0 || col == -1)
				return String.class;
			else {
				Object obj = getValueAt(0, col);
				if (obj == null)
					return columnType(col) == JotyTypes._smallBlob ? BufferedImage.class : String.class;
				else
					return obj.getClass();
			}

		}

		@Override
		public int getColumnCount() {
			if (Application.m_app == null)
				return 0;
			else {
				if (m_gridRowDescriptor == null)
					m_gridRowDescriptor = gridRowDescriptor();
				return m_gridRowDescriptor == null ? 0 : m_gridRowDescriptor.vector.size();
			}
		}

		@Override
		public String getColumnName(int pos) {
			CellDescriptor cellDescr = getCellDescriptor(pos);
			if (cellDescr == null)
				return getBuffer().m_fieldNames.get(pos);
			else if (cellDescr.m_label == null)
				return "";
			else
				return cellDescr.m_label;
		}

		@Override
		public int getRowCount() {
			int retVal = 0;
			if (!Beans.isDesignTime()) {
				JotyDataBuffer buffer = getBuffer();
				if (buffer != null)
					retVal = buffer.m_records.size();
			}
			return retVal;
		}

		@Override
		public Object getValueAt(int row, int col) {
			WrappedField wfield = null;
			Term panelMappedTerm = null;
			if (col >= 0) {
				col = mappedWFieldIndex(col);
				wfield = field(row, col);
				if (m_gridTerm == null)
					panelMappedTerm = m_panel.m_gridManager.m_termContainerPanel.m_terms.get(col);
			}
			Object retVal = null;
			if (wfield != null && !wfield.isNull())
				switch (wfield.resultSetDataType()) {
					case JotyTypes._text:
					case JotyTypes._single:
					case JotyTypes._double:
					case JotyTypes._date:
					case JotyTypes._dateTime:
						return wfield.render();
					case JotyTypes._int:
					case JotyTypes._long:
					case JotyTypes._dbDrivenInteger:
						return wfield.m_len == 1 || 
								m_colDescrAsFlag ? 
										(wfield.getInteger() == 0 ? "" : "X") : 
										(panelMappedTerm == null ? wfield.getInteger() : panelMappedTerm.toString(wfield));
					case JotyTypes._smallBlob:
						try {
							return wfield.m_previewBytes == null ? null : ImageIO.read(new ByteArrayInputStream(wfield.m_previewBytes));
						} catch (IOException e) {
							Logger.exceptionToHostLog(e);
						}
				}
			return retVal;
		}

		private GridRowDescriptor gridRowDescriptor() {
			return m_gridTerm == null ? 
						(m_panel.m_gridManager.m_termContainerPanel == null ? 
								null : 
								m_panel.m_gridManager.m_termContainerPanel.m_gridRowDescriptor) : 
						m_gridTerm.m_gridRowDescriptor;
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return m_gridRowDescriptor.vector.get(col).m_editable;
		}

		private int mappedWFieldIndex(int col) {
			m_colDescrAsFlag = false;
			CellDescriptor cellDescr = getCellDescriptor(col);
			if (cellDescr != null)
				m_colDescrAsFlag = cellDescr.m_isFlag;
			return cellDescr == null ? col : cellDescr.m_mappedWFieldIdx;
		}

		@Override
		public void setValueAt(Object aValue, int row, int col) {}

	}

	class ImageRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			setIcon(value == null ? null : new ImageIcon((BufferedImage) value));
			setHorizontalAlignment(SwingConstants.CENTER);
			setVerticalAlignment(SwingConstants.CENTER);
			setText(null);
			return this;
		}
	}

	/**
	 * This class extends javax.swing.DefaultCellEditor in order to implement
	 * custom support for three different types of cell editing; it can build a
	 * cell editor made by, respectively, a {@code CheckBox}, a {@code ComboBox}
	 * or a {@code JotyTextField}.
	 * <p>
	 * The type of data managed in each case derives naturally from type of
	 * component.
	 * <p>
	 * The class applies on each component the same rendering elaboration that
	 * the framework operates when it is wrapped by a {@code Term} instance.
	 * <p>
	 * The class connects the editor component with the cell of the underlying
	 * NavigatorBuffer, in reading and in writing
	 * 
	 * @see CheckBox
	 * @see ComboBox
	 * @see JotyTextField
	 */
	public class JotyCellEditor extends DefaultCellEditor {
		JotyTextField m_textField = null;
		ComboBox m_comboBox = null;
		CheckBox m_checkBox = null;
		int m_rowBeingEdited;
		int m_colBeingEdited;

		public JotyCellEditor(CheckBox checkBox) {
			super(checkBox);
			m_checkBox = checkBox;
		}

		public JotyCellEditor(ComboBox comboBox) {
			super(comboBox);
			m_comboBox = comboBox;
		}

		public JotyCellEditor(JotyTextField textField) {
			super(textField);
			m_textField = textField;
			textField.m_asCellEditor = true;
			textField.setCellEditor(this);
			setClickCountToStart(1);
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			m_rowBeingEdited = row;
			m_panel.m_gridManager.m_gridBuffer.m_cursorPos = row;
			m_colBeingEdited = mappedWFieldIndex(column);
			Component component = super.getTableCellEditorComponent(table, value, isSelected, row, column);
			long nonTextValue = m_panel.m_gridManager.m_gridBuffer.getWField(row, column).getInteger();
			if (component instanceof CheckBox)
				((CheckBox) component).setSelected(nonTextValue != 0);
			else if (component instanceof ComboBox)
				((ComboBoxTerm) ((ComboBox) component).m_term).setSelection(nonTextValue, true);
			return component;
		}

		@Override
		public boolean stopCellEditing() {
			boolean retVal = m_textField == null || m_textField.doValidate();
			if (retVal) {
				retVal = false;
				if (m_panel instanceof NavigatorEditorPanel) {
					Object oldValue = m_model.getValueAt(m_rowBeingEdited, m_colBeingEdited);
					WrappedField wfield = field(m_rowBeingEdited, m_colBeingEdited);
					if (wfield != null) {
						if (m_checkBox != null)
							wfield.setInteger(m_checkBox.getCheck());
						else if (m_comboBox != null)
							wfield.setInteger(m_comboBox.m_term.selectionData());
						else
							wfield.setData((String) getCellEditorValue(), m_textField.m_numberFormat);
						FieldActionDescriptor actionDescriptor = ((NavigatorEditorPanel) m_panel).processValueIfNeeded(wfield.dbFieldName());
						if (!actionDescriptor.success) {
							if (m_checkBox != null)
								wfield.setInteger((Long) oldValue);
							else if (m_comboBox != null)
								wfield.setInteger((Long) oldValue);
							else
								wfield.setData((String) oldValue, m_textField.m_numberFormat);
						}
						retVal = actionDescriptor.success || !actionDescriptor.actionIsNeeded;

					}
				} else
					Application.m_app.JotyMsg(this, "Editing of table different from the main one of a NavigatorEditorPanel is not implemented !");
			}
			return retVal && super.stopCellEditing();
		}

	}

	/**
	 * 
	 * The class defining the actual JTable.
	 *
	 */
	public class JotyJTable extends JTable {
		private Table m_table;
		private JTableHeader tableHeader;
		boolean m_toolTipsEnabled = Application.m_app.m_toolTipsEnabled();
		String m_toolTipText;

		JotyJTable(Table table) {
			m_table = table;
			tableHeader = getTableHeader();
			tableHeader.addMouseMotionListener(new MouseMotionListener() {
				@Override
				public void mouseDragged(MouseEvent e) {}

				@Override
				public void mouseMoved(MouseEvent e) {
					if (m_model instanceof DataTableModel) {
						int colIndex = convertColumnIndexToModel(columnAtPoint(e.getPoint()));
						if (colIndex >= 0)
							tableHeader.setToolTipText(m_toolTipsEnabled ? ((DataTableModel) m_model).getCellDescriptor(colIndex).m_label : null);
					}
				}

			});
		}

		public Table getTable() {
			return m_table;
		}

		@Override
		protected void paintComponent(Graphics g) {
			JTable.DropLocation dl = getDropLocation();
			if (dl == null || !dl.isInsertColumn())
				super.paintComponent(g);
		}

		@Override
		public void setToolTipText(String text) {
			m_toolTipText = text;
		}

		@Override
		public String getToolTipText() {
			return m_toolTipText;
		}

		@Override
		public String getToolTipText(MouseEvent event) {
			if (m_model instanceof DataTableModel) {
				CellDescriptor cellDescr = ((DataTableModel) m_model).getCellDescriptor(columnAtPoint(event.getPoint()));
				return m_toolTipsEnabled ? 
						(cellDescr.m_editable && m_cellEditingPermitted ? Application.m_common.jotyLang("ColEditable") : m_toolTipText) : 
						null;
				}
			else 
				return getToolTipText();
		}

	}

	class RightAlignTableCellRenderer extends DataTableCellRenderer {
		public RightAlignTableCellRenderer() {
			super();
			setHorizontalAlignment(SwingConstants.RIGHT);
		}
	}

	public static void repaintCell(JTable table, int row, int column) {
		table.repaint(table.getCellRect(row, column, true));
	}

	public JotyJTable m_jtable;
	public HashMap<Integer, Integer> m_colAlignement;
	public boolean m_sortClickDenied;
	protected DefaultTableModel m_model;
	java.awt.Color m_enableForeColor, m_disableForeColor;
	boolean m_enableColorOn;
	protected Color m_whiteColor;
	protected Color m_lightGreyColor;
	protected Color m_blueColor;
	protected Color m_lightBlueColor;
	public int[] m_colWidths;
	public boolean m_absoluteColWidths;
	private int m_sortColIndex;
	private int m_oldSortColIndex;
	private boolean m_sortAscend;
	private boolean m_headerDirty;

	protected DefaultTableCellRenderer m_dataTableCellRenderer;
	private DefaultTableCellRenderer m_alignRightRenderer;
	private DefaultTableCellRenderer m_alignCenterRenderer;
	private DefaultTableCellRenderer m_imageRenderer;
	private String m_sortByField;
	private Integer m_allColsAlignement;
	private boolean m_cellEditingPermitted;

	public Table(Panel panel, GridTerm term) {
		super(panel, term);
		m_jtable = new JotyJTable(this);
		createModel();
		m_jtable.setModel(m_model);
		m_jtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_jtable.setColumnSelectionAllowed(false);
		m_jtable.setRowSelectionAllowed(true);
		m_jtable.getSelectionModel().addListSelectionListener(new ListSelectionHandler());
		m_jtable.addMouseListener(new ClickHandler());
		m_jtable.setFillsViewportHeight(true);
		setViewportView(m_jtable);
		setDataTableCellRenderer();
		m_enableForeColor = new Color(0, 0, 0);
		m_disableForeColor = new Color(128, 128, 128);
		m_whiteColor = new Color(255, 255, 255);
		m_lightGreyColor = new Color(192, 192, 184);
		m_blueColor = new Color(16, 16, 96);
		m_lightBlueColor = new Color(64, 64, 96);
		m_enableColorOn = true;
		m_absoluteColWidths = false;
		m_sortAscend = true;
		m_sortColIndex = -1;
		m_oldSortColIndex = -1;
		m_headerDirty = false;
		setHeaderListener();
		m_jtable.getTableHeader().setFocusable(false);
		m_jtable.getTableHeader().setReorderingAllowed(false);
		m_sortClickDenied = false;
		m_sortByField = null;
		m_colAlignement = new HashMap<Integer, Integer>();
	}

	public void addToolTipRow(String text) {
		Application.m_app.addToolTipRowToComponent(m_jtable, text);
	}

	protected boolean alignmentAndCellEditingRequired() {
		return true;
	}

	protected void applyCellRenderer() {
		int colCount = m_model.getColumnCount();
		for (int i = 0; i < colCount; i++)
			m_jtable.setDefaultRenderer(m_model.getColumnClass(i), getDefaultRenderer(i));
	}

	public void createModel() {
		m_model = new DataTableModel();
	}

	@Override
	protected void doActualRemoval() {
		JotyDataBuffer buffer = getBuffer();
		if (buffer != null)
			buffer.empty(false);
	}

	@Override
	public void ensureIndexIsVisible(int row) {
		m_jtable.scrollRectToVisible(m_jtable.getCellRect(row, 0, true));
	}

	private WrappedField field(int row, int col) {
		JotyDataBuffer buffer = getBuffer();
		if (buffer.m_queueManager != null)
			row = buffer.m_queueManager.getMappedRow(row);
		return buffer.m_records.get(row).m_data.get(col);
	}

	@Override
	public JotyDataBuffer getBuffer() {
		return ((DataTableModel) m_model).getBuffer();
	}

	public Object getCurValueAt(int col) {
		return m_model.getValueAt(getSelection(), col);
	}

	protected DefaultTableCellRenderer getDefaultRenderer(int colIndex) {
		return (m_model.getRowCount() > 0 && ((DataTableModel) m_model).columnType(colIndex) == JotyTypes._smallBlob) ? m_imageRenderer : m_dataTableCellRenderer;
	}

	private Term getMappedTerm(int index) {
		return m_panel.m_gridManager.m_termContainerPanel.m_terms.get(mappedWFieldIndex(index));
	}

	@Override
	public JComponent getPaneComponent() {
		return m_jtable;
	}

	@Override
	public int getRowQty() {
		return m_jtable.getRowCount();
	}

	@Override
	public int getSelectedColumn(MouseEvent e) {
		return ((JTable) e.getSource()).getSelectedColumn();
	}

	@Override
	public int getSelection() {
		return getRowQty() == 0 ? -1 : m_jtable.getSelectedRow();
	}


	@Override
	public void init() {
		m_model.newDataAvailable(null);
		super.init();
		if ((m_panel instanceof DataAccessPanel) && m_term == null)
			m_cellEditingPermitted = ((DataAccessPanel) m_panel).permission().compareTo(Permission.readWrite) >= 0;
	}

	@Override
	protected boolean manageBarEnable() {
		return getSelection() >= 0;
	}

	@Override
	public void managedAppend(GridManager gridManager, int iDim) {
		newDataAvailable();
		super.managedAppend(gridManager, iDim);
	}

	@Override
	public void managedDeleteRow(int m_currentRowPos) {
		newDataAvailable();
		super.managedDeleteRow(m_currentRowPos);
	}

	@Override
	public void managedListLoad(TermContainerPanel termContainerPanel) {
		newDataAvailableWithoutEvents(false);
		if (termContainerPanel.isAnInsidePanel())
			if (getRowQty() > 0 && !termContainerPanel.injectedDialog().m_new_command && !termContainerPanel.injectedDialog().m_is_deleting)
				setSelection(0);
		super.managedListLoad(termContainerPanel);
	}

	@Override
	protected void manageDoubleClick(MouseEvent e) {
		int col = getSelectedColumn(e);
		boolean normalProcess = true;
		if (!e.isConsumed()) {
			if (e.getClickCount() == 2) {
				if (m_model.getColumnClass(col) == BufferedImage.class) {
					if (m_gridTerm == null) {
						ImageTerm term = (ImageTerm) m_panel.m_gridManager.m_termContainerPanel.m_terms.get(mappedWFieldIndex(col));
						((ImageComponent) term.m_blobComponent).open(true);
					} else {
						openViewer(col);
						normalProcess = false;
					}
				}
			}
			e.consume();
		}
		if (normalProcess)
			super.manageDoubleClick(e);
	}

	TermContainerPanel mappedTermContainerPanel() {
		return (m_panel.m_gridManager == null ? (TermContainerPanel) m_panel : m_panel.m_gridManager.m_termContainerPanel);
	}

	private int mappedWFieldIndex(int col) {
		return ((DataTableModel) m_model).mappedWFieldIndex(col);
	}

	public void newDataAvailable() {
		newDataAvailable(true);
	}

	public void newDataAvailable(boolean setColProps) {
		m_headerDirty = true;
		m_model.newDataAvailable(null);
		if (setColProps)
			setColsProperties();
	}

	public void newDataAvailableWithoutEvents(boolean preserveSelection) {
		long oldSelection = getSelection();
		m_changeEventsEnabled = false;
		newDataAvailable();
		if (preserveSelection && oldSelection >= 0)
			setSelection(oldSelection);
		m_changeEventsEnabled = true;
	}

	@Override
	public void nextRow() {
		setSelection(getSelection() + 1);
	}

	private void openViewer(int col) {
		CellDescriptor cellDescr = ((DataTableModel) m_model).getCellDescriptor(col);
		JotyDataBuffer buffer = getBuffer();
		String whereClauseTermContrib = m_gridTerm.whereClause(false);
		String whereClause = (whereClauseTermContrib.length() == 0 ? 
									"WHERE " : 
										(whereClauseTermContrib + " AND ")) + 
							buffer.m_keyName + " = " + buffer.getKeyLongVal();
		byte[] bytes = Application.m_app.m_db.getBytesFromDb(
								String.format("SELECT %1$s FROM %2$s %3$s", 
												cellDescr.m_targetFieldName, Application.m_app.codedTabName(m_gridTerm.m_dataTable), whereClause), 
								m_term.m_panel.createContextPostStatement(m_term.m_name));
		if (bytes == null || bytes.length == 0)
			Application.langWarningMsg("EmptyBlob");
		else
			cellDescr.m_viewersManager.openDocument(bytes);
	}

	@Override
	public void previousRow() {
		setSelection(getSelection() - 1);
	}

	public void repaintCell(int row, int column) {
		repaintCell(m_jtable, row, column);
	}

	public void setAllColsAlignement(int alignement) {
		m_allColsAlignement = alignement;
	}

	public void setColAlignement(int colIndex, int alignement) {
		m_colAlignement.put(colIndex, alignement);
	}

	/**
	 * Sets the properties of the columns of the JotyJTable object.
	 * <p>
	 * It makes direct use of the the information in the
	 * {@code GridRowDescriptor} object localized in the building of the model.
	 * 
	 * @see GridRowDescriptor
	 */
	public void setColsProperties() {
		if (m_panel != null) {
			applyCellRenderer();
			if (m_gridTerm == null && m_colWidths == null) {
				m_colWidths = m_panel.m_gridColumnWidths;
				m_absoluteColWidths = m_panel.m_absoluteGridColumnWidths;
			}
			int colCount = m_model.getColumnCount();
			TableColumnModel colModel = m_jtable.getColumnModel();
			int modelColCount = colModel.getColumnCount();
			TableColumn column = null;
			if (m_colWidths != null) {
				if (colCount > m_colWidths.length)
					Application.m_app.JotyMsg(this, "Not enough column widths have been specified !");
				else {
					m_jtable.setAutoResizeMode(m_absoluteColWidths ? JTable.AUTO_RESIZE_OFF : JTable.AUTO_RESIZE_LAST_COLUMN);
					if (colCount == modelColCount) {
						for (int i = 0; i < colCount; i++) {
							column = colModel.getColumn(i);
							if (m_colWidths[i] < column.getMinWidth())
								Application.m_app.JotyMsg(this, "The setting value of column width in position " + i + 
															" is less than then minimum allowed (" + column.getMinWidth() + ")");
							column.setPreferredWidth(m_colWidths[i]);
							if (i == colCount - 1)
								column.setWidth(m_colWidths[i]);
						}
					} else
						Application.m_app.JotyMsg(this, "Table component has not been rendered yet, so no column resizing is possible");
				}
			}
			if (alignmentAndCellEditingRequired()) {
				JotyDataBuffer buffer = getBuffer();
				if (buffer != null && (buffer.m_descriptorBuilt || m_gridTerm != null && m_gridTerm.isDbConnectionFree())) {
					if (colCount == modelColCount) {
						Term mappedTerm = null;
						CellDescriptor cellDescr = null;
						Integer colAlignement = null;
						int fieldType;
						for (int i = 0; i < colCount; i++) {
							column = colModel.getColumn(i);
							if (!(m_model.getColumnClass(i) == BufferedImage.class)) {
								cellDescr = ((DataTableModel) m_model).getCellDescriptor(i);
								mappedTerm = cellDescr.m_mappingType == RowCellMappingType.PANEL_TERM ? getMappedTerm(i) : null;
								colAlignement = m_colAlignement.get(i);
								if (colAlignement == null)
									colAlignement = m_allColsAlignement;
								if (colAlignement == null) {
									colAlignement = SwingConstants.LEFT;
									if (mappedTerm != null) {
										if (!(mappedTerm instanceof TextAreaTerm || 
												mappedTerm instanceof TextTerm && mappedTerm.m_dataType == JotyTypes._text) && !(mappedTerm instanceof ComboBoxTerm))
											colAlignement = mappedTerm instanceof TextTerm && 
																mappedTerm.m_dataType == JotyTypes._date ? SwingConstants.CENTER : SwingConstants.RIGHT;
									} else {
										fieldType = buffer.m_fieldTypes.get(cellDescr.m_mappedWFieldIdx);
										if (fieldType != JotyTypes._text)
											colAlignement = SwingConstants.RIGHT;
										else if (fieldType == JotyTypes._date)
											colAlignement = SwingConstants.CENTER;
									}
								}
								if (colAlignement == SwingConstants.CENTER)
									column.setCellRenderer(m_alignCenterRenderer);
								else if (colAlignement == SwingConstants.RIGHT)
									column.setCellRenderer(m_alignRightRenderer);
								if (mappedTerm != null && cellDescr.m_editable && m_cellEditingPermitted) {
									if (cellDescr.m_cellEditor == null) {
										JComponent component = mappedTerm.getComponent();
										if (component instanceof JotyTextField)
											cellDescr.m_cellEditor = new JotyCellEditor((JotyTextField) component);
										else if ((component instanceof CheckBox))
											cellDescr.m_cellEditor = new JotyCellEditor((CheckBox) component);
										else
											cellDescr.m_cellEditor = new JotyCellEditor((ComboBox) component);
									}
									column.setCellEditor(cellDescr.m_cellEditor);
								}
							}
						}
					}
				}
			}
		}
	}

	public void setCustomRowHeight(int customRowHeight) {
		m_jtable.setRowHeight(customRowHeight);
	}

	protected void setDataTableCellRenderer() {
		m_dataTableCellRenderer = new DataTableCellRenderer();
		m_alignRightRenderer = new RightAlignTableCellRenderer();
		m_alignCenterRenderer = new CenterAlignTableCellRenderer();
		m_imageRenderer = new ImageRenderer();
	}

	@Override
	public void setEnabled(boolean truth) {
		m_enableColorOn = truth;
		m_jtable.getTableHeader().setForeground(truth ? m_enableForeColor : m_disableForeColor);
		super.setEnabled(truth);
		m_jtable.setBackground(m_enableColorOn ? m_whiteColor : m_lightGreyColor);
		if (Application.m_app.m_macOs)
			m_jtable.setGridColor(m_enableColorOn ? Color.lightGray : Color.gray);

	}

	/**
	 * Installs a mouse listener on the header of the JotyJtable object to
	 * allow the invoking of the re-sorting of the grid data.
	 */
	protected void setHeaderListener() {
		m_jtable.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 1 && isEnabled()) {
					if (m_sortClickDenied)
						Application.langWarningMsg("GridNotSortable");
					else {
						TableColumnModel cModel = m_jtable.getColumnModel();
						int clickedColumn = cModel.getColumnIndexAtX(e.getX());
						CellDescriptor cellDescr = ((DataTableModel) m_model).getCellDescriptor(clickedColumn);
						Term term;
						if (cellDescr.m_mappingType == RowCellMappingType.PANEL_TERM && ((term = mappedTermContainerPanel().m_terms.get(cellDescr.m_mappedWFieldIdx)) instanceof DescrTerm || term instanceof ImageTerm))
							Application.langWarningMsg("ColNotSortable");
						else if (m_panel.m_gridManager != null && m_panel.m_gridManager.m_gridBuffer.m_queueManager != null)
							Application.langWarningMsg("NotSortingInQueue");
						else {
							if (getRowQty() > 0) {
								String gotBufferField = getBuffer().m_records.get(0).m_data.get(mappedWFieldIndex(clickedColumn)).dbFieldName();
								if (gotBufferField == null)
									Application.langWarningMsg("NoFieldToSortBy");
								else if (!(m_panel instanceof SearcherPanel) && m_panel.getDialog().isEditing())
									Application.langWarningMsg("NotSortableInEditing");
								else {
									if (cellDescr.m_fieldName == null || cellDescr.m_fieldName.compareToIgnoreCase(gotBufferField) == 0) {
										m_sortColIndex = clickedColumn;
										if (m_sortColIndex == m_oldSortColIndex || m_oldSortColIndex == -1)
											m_sortAscend = !m_sortAscend;
										else
											m_oldSortColIndex = m_sortColIndex;
										newDataAvailable(false);
										setSortExpression(gotBufferField, true);
									} else
										Application.langWarningMsg("ColNotSortable");
								}
							}
						}
					}
				}
				super.mouseClicked(e);
			}

		});
	}

	@Override
	public void setSelection(long val) {
		if (val == -1)
			m_jtable.clearSelection();
		else {
			m_jtable.getSelectionModel().setSelectionInterval((int) val, (int) val);
			ensureIndexIsVisible((int) val);
		}

	}

	private void setSortExpression(String dbField, boolean loadData) {
		m_sortByField = dbField;
		String sortByExpr = dbField + (m_sortAscend ? " asc" : " desc");
		if (m_panel instanceof SearcherPanel) {
			SearcherPanel panel = (SearcherPanel) m_panel;
			panel.m_criteriaPanel.setOrderByExpr(sortByExpr);
			panel.reset();
			if (loadData)
				panel.doSearch(false);
		} else {
			DataAccessPanel panel = (DataAccessPanel) m_panel;
			if (panel.m_gridManager != null && panel.m_gridManager.m_listComponent == this) {
				panel.m_sortExpr = sortByExpr;
				if (loadData)
					panel.loadData();
				panel.resetUpdateActorsState();
			} else {
				m_gridTerm.m_sortExpr = sortByExpr;
				if (loadData)
					m_gridTerm.loadData();
			}
		}
	}

	
	@Override
	public String getSortInfo() {
		if (m_sortColIndex == -1)
			return super.getSortInfo();
		else
			return (m_sortAscend ? "+1" : "-1") + String.format("%03d", m_sortColIndex) + m_sortByField;
	}

	@Override
	public void setSortInfo(String mainSortInfo) {
		m_sortAscend = Integer.parseInt(mainSortInfo.substring(0, 2)) > 0;
		m_sortColIndex = Integer.parseInt(mainSortInfo.substring(2, 5));
		setSortExpression(mainSortInfo.substring(5), false);
	}

	@Override
	public void setToolTipText(String text) {
		m_jtable.setToolTipText(text);
	}

	@Override
	protected void signalRemoval() {
		newDataAvailable();
	}

}
