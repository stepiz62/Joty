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

import java.awt.event.MouseEvent;
import java.beans.Beans;
import java.util.HashMap;
import java.util.Map;

import org.joty.access.PostStatement;
import org.joty.access.Accessor.DataDef;
import org.joty.common.JotyTypes;
import org.joty.common.BasicPostStatement;
import org.joty.common.Utilities;
import org.joty.common.Utilities.Stocker;
import org.joty.data.BasicJotyCursor;
import org.joty.data.WrappedField;
import org.joty.gui.WFieldSet;
import org.joty.workstation.app.Application;
import org.joty.workstation.app.Application.LiteralStruct;
import org.joty.workstation.data.JotyDataBuffer;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.gui.DataAccessPanel.ActionOnRowInterface;
import org.joty.workstation.gui.DataAccessPanel.DefinedInsertMethodInterface;
import org.joty.workstation.gui.DataAccessPanel.DefinedSetMethodInterface;
import org.joty.workstation.gui.DataAccessPanel.GetDelStmntInterface;
import org.joty.workstation.gui.DataAccessPanel.GetWhereClauseInterface;
import org.joty.workstation.gui.DataAccessPanel.RenderRowMethodInterface;
import org.joty.workstation.gui.GridRowDescriptor.CellDescriptor;
import org.joty.workstation.gui.GridRowDescriptor.RowCellMappingType;
import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * This class abandons the single datum management typical of all its siblings
 * and treats the set of data associated to the primary entity, that one
 * "hosted" by the containing DataAccessPanel, and that are presented to the
 * user by a tabular structure.
 * <p>
 * Beyond the reference to the LiteralStruct object, that is still useful for
 * single column structure, it adds a JotyDataBuffer object. This makes this
 * abstract class ready to support either LiteralStruct based components or
 * multi-column components.
 * <p>
 * So it loads the record set associated ad allows its modification and, at
 * last, stores it in the database. It participates in the editing session of
 * the containing DataAccessPanel and its methods are invoked by the framework
 * in a consistent way with the Joty technology, either in terms of being
 * processing within a transaction or of making use of the Accessor object (if
 * the accessor mode is set).
 * <p>
 * Then it provides support for the instantiated object to control another
 * StructuturedTerm object in a "one-to-many" relation: the selection of a row
 * of the object identifies a key for data selection on the controlled object
 * (in the code this relation is said master-slave). For supporting this
 * scenario it holds a vector of JotDataBuffer for buffering all the subset of
 * secondary data. The one to many relation ability applies to LiteralStructs
 * also: the {@code Application.m_2L_literalMap} map is used for displaying the
 * suited LiteralStruct descriptions in the controlled object depending on
 * selection made in the 'master' object. The class can impersonate, indeed,
 * either the master or the slave object: the mode is held by the
 * {@code m_slave} member variable.
 * <p>
 * Its data management behavior derives by the parameters provided in its
 * creation and/or by the possible implementation of various available
 * interfaces. Many of these interfaces however are usable only if the Accessor
 * mode is not set.
 * <p>
 * In the case the contained component has a multi-column structure, the class
 * provides methods for defining the grid composition and to add fields only to
 * the buffer structure.
 * <p>
 * On the storing of the data, all the record set in the database is deleted and
 * the new one is inserted. In the 'slave' case the preparing deletion operates
 * on the id of the main entity, that one managed by the DataAccessPanel object,
 * and the storing is performed using the entire set of JotyDataBuffer objects.
 * 
 * @see TermContainerPanel
 * @see org.joty.workstation.app.Application.LiteralStruct
 * @see JotyDataBuffer
 * @see Application#m_2L_literalMap
 * @see Factory
 * @see DataAccessPanel
 * 
 */
public abstract class GridTerm extends DescrTerm {

	/** Allows the definition of a whatever procedure for updating actions 'linked' to the data changing of this object. 
	 * @see #m_linkedAspectUpdater
	 */
	
	public interface LinkedAspectUpdater {
		void update();
	}

	/**
	 * Allows an overall definition of the underlying query in the case in which
	 * the dialog doesn't work in Accessor mode.
	 * 
	 * @see #m_linkedAspectUpdater
	 */
	public interface QueryInterface {
		String get(GridTerm term);
	}

	/** @see LinkedAspectUpdater */
	public LinkedAspectUpdater m_linkedAspectUpdater;
	public boolean m_variableDescrSet;
	public boolean m_loadOnly;
	public String m_targetDatumField;
	public JotyDataBuffer m_dataBuffer;
	public String m_dataTable;
	public String m_definedWhereClause;
	public String m_mainEntityKeyField;
	public String m_mainTermName;
	public GetWhereClauseInterface m_whereClauseImplementor;
	public GetDelStmntInterface m_delStatementImplementor;
	public DefinedSetMethodInterface m_definedSetMethod;
	public DefinedInsertMethodInterface m_definedInsertMethod;
	String m_slaveTermName;
	public LiteralStruct m_mainTermLiteralStruct;
	public boolean m_slave;
	protected GridTerm m_mainTerm;
	protected Map<Long, JotyDataBuffer> m_dataBuffers;
	public String m_genIdField;
	public boolean m_autoIncrGenID;
	public String m_seq_name;
	protected boolean m_newRowJustCreated;
	public QueryInterface m_queryInterface;
	public int m_mainIterator;
	public DataDef m_dataDef;
	private boolean m_accessorMode;
	protected PostStatement m_queryDefPostStatement;
	protected boolean m_dbFree;

	public interface SelectionHandler {
		public void selChange(GridTerm term);
	}

	public interface Selector {
		public void select(GridTerm term);
	}

	public ActionOnRowInterface m_actionOnRowHandler;
	public WFieldSet m_wfields;
	public GridRowDescriptor m_gridRowDescriptor;
	protected boolean m_gridRowDescriptorInitied;
	public SelectionHandler m_selectionHandler;
	public Selector m_selector;
	public BasicJotyCursor m_descriptorOnLoad;
	Stocker m_foreignFields;

	public GridTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
		m_accessorMode = m_panel.getDialog().m_accessorMode;
		m_dataTable = params.m_dataTable;
		if (m_accessorMode && m_dataTable != null)
			m_app.JotyMsg(this, m_app.m_msgDataDefExpectedAsEntry);
		m_targetDatumField = params.m_targetDatumField;
		m_mainTermName = params.mainTermName;
		m_mainEntityKeyField = params.mainEntityKeyField;
		m_loadOnly = params.m_dataSetLoadOnly;
		m_slaveTermName = null;
		m_mainTerm = null;
		if (params.mainTermName != null) {
			m_mainTerm = m_panel.GridTerm(params.mainTermName);
			if (m_mainTerm != null) {
				m_mainTerm.m_slaveTermName = m_name;
				m_literalStruct = m_mainTerm.m_literalStruct;
				m_mainTermLiteralStruct = m_mainTerm.m_literalStruct;
			}
			m_slave = true;
		} else
			m_mainTermLiteralStruct = null;
		if (m_slave)
			m_loadOnly = false;
		if (m_slave)
			m_dataBuffers = new HashMap<Long, JotyDataBuffer>();
		m_genIdField = null;
		m_seq_name = Application.m_common.m_seq_name;
		m_autoIncrGenID = Application.m_common.m_idFieldAutoIncrement;
		m_newRowJustCreated = false;
		m_mainIterator = -1;
		m_wfields = new WFieldSet(m_app);
		m_gridRowDescriptor = new GridRowDescriptor();
		m_gridRowDescriptorInitied = false;
		m_foreignFields = Utilities.m_me.new Stocker();
	}

	boolean bufferToLoad() {
		return m_dataBuffer != null && 
					(m_slave && m_dataBuffers.size() == 0 || !m_slave && m_dataBuffer.m_records.size() == 0 || explicitQuery());
	};

	void checkComponentsState() {
		for (Term term : m_panel.m_terms) {
			if (term.m_drivenBufferTerm == this)
				term.enableComponent(m_dataBuffer != null && m_dataBuffer.m_cursorPos >= 0, m_panel.editability(), true);
		}
	}

	public void checkLinkedAspectUpdater() {
		if (m_linkedAspectUpdater != null)
			m_linkedAspectUpdater.update();
	}

	@Override
	public void checkSelection() {
		if (m_dataBuffer != null && getSelection() == -1)
			m_dataBuffer.m_cursorPos = -1;
		checkComponentsState();
		updateDriverTerms();
	}

	@Override
	protected void clearComponent() {
		if (m_dataTable != null || explicitQuery()) {
			removeAll();
			checkSelection();
			if (m_asideLoadMethod != null)
				m_asideLoadMethod.method(m_panel, null);
		}
	}

	boolean clearData() {
		return clearData(true);
	}

	boolean clearData(boolean updateAspect) {
		if (m_dataTable == null && !m_app.m_accessorMode)
			return true;
		boolean retVal = true;
		boolean stillToDo = true;
		if (m_delStatementImplementor != null)
			stillToDo = !m_delStatementImplementor.method((DataAccessPanel) m_panel);
		if (stillToDo) {
			String whereClause = whereClause(true);
			if (whereClause.length() > 0)
				retVal = Application.m_app.executeSQL(
							String.format("Delete from %1$s %2$s", m_app.codedTabName(m_dataTable), whereClause), 
							null, 
							createContextPostStatement());
		}
		if (retVal && updateAspect)
			checkAndClear();
		return retVal;
	}

	void clearDataBuffer() {
		if (m_slave)
			clearSlaveBuffers();
		else if (m_dataBuffer != null)
			m_dataBuffer.empty();
		if (m_dataBuffer != null)
			m_dataBuffer.m_cursorPos = -1;
	}

	void clearSlaveBuffers() {
		m_dataBuffers.clear();
	}

	void clearSlaveTermBuffer() {
		if (m_slaveTermName != null) {
			GridTerm slaveTerm = m_panel.GridTerm(m_slaveTermName);
			Long key = m_dataBuffer.integerValue(m_targetDatumField);
			JotyDataBuffer dataBuffer = slaveTerm.m_dataBuffers.get(key);
			if (dataBuffer != null) {
				slaveTerm.m_dataBuffers.remove(key);
				slaveTerm.refresh();
			}
		}
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {}

	String dataQuery() {
		return dataQuery(false);
	}

	String dataQuery(boolean forAddNew) {
		String sqlStr = null;
		if (m_accessorMode) {
			if (m_queryInterface != null) {
				m_app.JotyMsg(this, m_app.m_msgDataDefExpectedAsEntry);
				return "";
			}
			DataAccessPanel dataPanel = (DataAccessPanel) m_panel;
			String filter = whereClause(false);
			m_queryDefPostStatement = dataPanel.createQueryDefPostStatement(
										m_name, 
										filter == null || filter.length() == 0 ? 
															null : 
															filter.substring(filter.toUpperCase().indexOf(" WHERE ") + 6), 
										m_sortExpr, 
										dataPanel.m_panelIdxInDialog);
			if (dataPanel.m_localAccessor) {
				m_app.m_accessor.setPostStatement(m_queryDefPostStatement);
				m_dataTable = m_app.m_accessor.getUpdatableSetFromDataDef();
				sqlStr = m_app.m_accessor.getQueryFromPostStatement();
				m_queryDefPostStatement = null;
			}
		} else {
			if (m_queryInterface == null) {
				if (m_dataTable != null && (m_dataDef != null || sqlStr == null)) {
					if (sqlStr == null)
						sqlStr = String.format("Select %1$s from %2$s", m_dataTable + ".*", m_dataTable);
					if (!forAddNew) {
						sqlStr += whereClause(false);
						if (m_sortExpr != null && !m_sortExpr.isEmpty())
							sqlStr += " Order by " + m_sortExpr;
					}
				}
			} else
				sqlStr = m_queryInterface.get(this);
		}
		return sqlStr;
	}

	void deleteTermRow() {
		if (m_dataBuffer != null) {
			clearSlaveTermBuffer();
			m_dataBuffer.deleteRecord();
			checkSelection();
			notifyEditingAction(null);
			m_panel.notifyEditingAction(null);
		}
		renderAfterDeletion();
	}

	boolean descrVectToBeLoaded() {
		return m_dataTable == null && !explicitQuery();
	}

	protected boolean explicitQuery() {
		return m_queryInterface != null || m_panel.getDialog().m_accessorMode && !m_dbFree;
	}

	@Override
	void innerClear() {
		clearDataBuffer();
	}

	@Override
	public boolean innerClearData() {
		return clearData();
	}

	@Override
	public boolean innerStore() {
		return storeData();
	}

	public boolean isDbConnectionFree() {
		return m_dataTable == null && !explicitQuery();
	}

	@Override
	public boolean isWindowVisible() {
		return false;
	}

	void loadData() {
		if (m_dataTable != null || explicitQuery())
			doLoadData();
	}

	void loadVerboseFromDescrArray() {
		ScrollGridPane cntrl = (ScrollGridPane) getComponent();
		if (m_slave && m_mainIterator < 0)
			return;
		setLiteralStruct();
		cntrl.initVerboseLayout();
		m_reloadNeeded = false;
	}

	protected long mainEntityVal() {
		return m_mainTermName == null ? ((DataAccessPanel) m_panel).entityID() : mainTermData();
	}

	int mainSelection() {
		Term term = m_panel.term(m_mainTermName);
		if (term != null)
			return term.getSelection();
		else
			return -1;
	}

	public long mainTermData() {
		if (m_slave && m_mainTermLiteralStruct != null)
			return (int) m_mainTermLiteralStruct.m_descrArray.get(m_mainIterator).id;
		else
			return mainTermSelectionData();
	}

	long mainTermData(int indexPos) {
		if (m_mainTermName == null)
			return -1;
		else {
			GridTerm mainTerm = (GridTerm) m_panel.term(m_mainTermName);
			if (indexPos >= mainTerm.m_dataBuffer.m_records.size())
				return -1;
			else
				return mainTerm.m_dataBuffer.integerValue(mainTerm.m_targetDatumField, indexPos);
		}
	}

	long mainTermSelectionData() {
		Term term = m_panel.term(m_mainTermName);
		if (term != null)
			return term.selectionData();
		else
			return -1;
	}

	@Override
	public void manageAsRelated(LiteralStruct masterLiteralStruct, boolean loadSetIfAvailable) {
		m_mainTermLiteralStruct = masterLiteralStruct;
		m_mainIterator = mainSelection();
		if (masterLiteralStruct == null)
			clearSlaveBuffers();
		else
			setLiteralStruct();
		m_reloadNeeded = true;
		structuredInit(loadSetIfAvailable);
	}

	public void newRow() {
		if (m_dataBuffer != null) {
			WResultSet sourceRs = m_dataBuffer.m_descriptorBuilt ? null : new WResultSet(null, dataQuery());
			if (sourceRs != null)
				sourceRs.open();
			m_dataBuffer.newRecord(sourceRs);
			if (sourceRs != null)
				sourceRs.close();
			checkComponentsState();
			updateDriverTerms();
			m_newRowJustCreated = true;
			notifyEditingAction(null);
			m_panel.notifyEditingAction(null);
		}
	}

	void refreshSlaveTerm() {
		if (m_slaveTermName != null)
			m_panel.term(m_slaveTermName).refresh();
	}

	protected void removeAll() {}

	protected void renderAfterDeletion() {
		termRender(true, false);
	}

	public void renderRecord(WResultSet rs, String m_keyFieldName) {}

	@Override
	public void reset() {
		if (m_slave)
			m_mainIterator = -1;
		if (!isAControlTerm() && m_dataBuffer != null)
			if (isDataComplement())
				innerLoad();
			else
				m_dataBuffer.empty();
	}

	public void selectLastRow() {}

	@Override
	protected void set(Term srcTerm) {
		if (srcTerm instanceof GridTerm) {
			GridTerm structuredSrcTerm = (GridTerm) srcTerm;
			m_targetDatumField = structuredSrcTerm.m_targetDatumField;
			m_dataTable = structuredSrcTerm.m_dataTable;
			m_loadOnly = structuredSrcTerm.m_loadOnly;
		}
		super.set(srcTerm);
	}

	public void setAsDbFree() {
		m_dbFree = true;
	}

	public void setLiteralStruct() {
		if (m_slave && m_mainTermLiteralStruct != null) {
			if (m_mainTermLiteralStruct.m_descrArray.size() == 0 || m_mainIterator < 0)
				return;
			String slaveLiteralStructName = 
					descr2lMap(m_mainTermLiteralStruct).
								get(m_mainTermLiteralStruct.m_descrArray.get(m_mainIterator).id);
			if (slaveLiteralStructName != null)
				m_literalStruct = (LiteralStruct) Application.m_common.m_literalStructMap.get(slaveLiteralStructName);
			else
				m_app.JotyMsg(this, "Trouble on second level description array ");
		}
	}

	public Map<Long, String> descr2lMap(LiteralStruct literalStruct) {
		return m_app.m_2L_literalMap.get(literalStruct.m_name);
	}

	public void setGenIdAutoIncr() {
		m_autoIncrGenID = true;
	}

	public void setGenIdField(String fieldName) {
		m_genIdField = fieldName;
	}

	boolean setIsToLoad() {
		return !m_slave && m_dataBuffer != null || m_literalStruct != null || m_slave && m_mainTermLiteralStruct != null;
	}

	public void setKeyName(String keyName) {
		if (!Beans.isDesignTime())
			m_dataBuffer.setKeyFieldName(keyName);
	}

	JotyDataBuffer slaveBuffer() {
		JotyDataBuffer retVal = null;
		if (m_mainIterator >= 0 && m_dataBuffers != null) {
			long termData = mainTermData(m_mainIterator);
			retVal = termData == -1 ? null : m_dataBuffers.get(termData);
		}
		return retVal;
	}

	boolean storeData() {
		boolean success = true;
		if ((m_dataTable != null || m_app.m_accessorMode) && !m_loadOnly && !isAControlTerm() && !isDataComplement() || m_definedInsertMethod != null)
			success = doStoreData();
		resetDirtyStatus();
		return success;
	}

	@Override
	public void structuredInit() {
		structuredInit(true);
	}

	@Override
	void structuredInit(boolean loadSetIfAvailable) {
		clearComponent();
		if (setIsToLoad() && !isAControlTerm()) {
			if (descrVectToBeLoaded())
				loadVerboseFromDescrArray();
			if (loadSetIfAvailable)
				loadData();
		}
	}


	void updateDriverTerms() {
		for (Term term : m_panel.m_terms) {
			if (term.m_drivenBufferTerm == this)
				if (m_dataBuffer != null && m_dataBuffer.m_cursorPos >= 0)
					term.copyWField(m_dataBuffer.wfield(term.m_fieldToDrive), false);
				else
					term.checkAndClear();
		}
	}

	@Override
	public void updateState(WrappedField rowCell) {
		if (!m_reloadNeeded && !m_panel.m_inhibitGridTermsEffect) {
			m_reloadNeeded = true;
			updateAspect();
			m_reloadNeeded = false;
		}
	}

	String whereClause(boolean usedInTransaction) {
		if (m_definedWhereClause == null || m_definedWhereClause.isEmpty()) {
			String clauses = "", builtInClause = "", customClause = "";
			if (m_mainEntityKeyField != null && !m_mainEntityKeyField.isEmpty()) {
				boolean doAdd = m_mainTermName == null ? m_mainEntityKeyField != null : m_mainIterator >= 0;
				if (doAdd) {
					String mainValueStr = m_mainTermName == null ? 
											((DataAccessPanel) m_panel).entityIdExpr(usedInTransaction) : 
											String.valueOf(mainTermData());
					builtInClause = String.format("%1$s = %2$s", m_mainEntityKeyField, mainValueStr);
					clauses += builtInClause;
				}
			}
			if (m_whereClauseImplementor != null) {
				customClause = m_whereClauseImplementor.method((DataAccessPanel) m_panel, usedInTransaction);
				if (customClause == null)
					m_app.JotyMsg(this, m_name + " : whereClauseImplementor doesn't build any where clause !");
				if (!builtInClause.isEmpty() && !customClause.isEmpty())
					clauses += " AND ";
				clauses += customClause;
			}
			return customClause == null ? 
						"" : 
						customClause.toUpperCase().indexOf("WHERE ") >= 0 ? 
								(" " + customClause) : 
								(clauses.isEmpty() ? "" : " WHERE " + clauses);
		} else
			return m_definedWhereClause;
	}

	public void addField(String fieldName, String header) {
		addField(fieldName, header, 0);
	}

	public void addField(String fieldName, String header, int dataType) {
		addField(fieldName, header, dataType, false);
	}

	public void addField(String fieldName, String header, int dataType, boolean isFlag) {
		addField(fieldName, header, dataType, isFlag, false);
	}

	public void addField(String fieldName, String header, int dataType, boolean isFlag, boolean asCurrency) {
		addFieldToBuffer(fieldName, dataType, asCurrency);
		m_gridRowDescriptor.add(fieldName, RowCellMappingType.FIELD, m_wfields.vector.size() - 1, header);
		if (isFlag && !Beans.isDesignTime())
			m_gridRowDescriptor.get(fieldName).m_isFlag = true;
	}

	public void addFieldAsCurrency(String fieldName, String header) {
		addField(fieldName, header, JotyTypes._double, false, true);
	}

	public void addFieldAsFlag(String fieldName, String header) {
		addField(fieldName, header, 0, true);
	}

	public void addFieldToBuffer(String fieldName) {
		addFieldToBuffer(fieldName, 0);
	}

	public void addFieldToBuffer(String fieldName, int dataType) {
		addFieldToBuffer(fieldName, dataType, false);
	}

	public void addFieldToBuffer(String fieldName, int dataType, boolean asCurrency) {
		m_wfields.add(fieldName, dataType, asCurrency);
	}

	public void bufferRender() {}

	public void checkSlaveTermBuffer() {
		if (m_mainIterator >= 0 && slaveBuffer() == null && m_targetDatumField != null)
			m_dataBuffers.put(mainTermData(m_mainIterator), new TermBuffer(this, m_targetDatumField));
	}

	protected WResultSet createAndOpenWRset() {
		PostStatement postStatement = null;
		if (m_panel.getDialog().m_accessorMode)
			postStatement = createContextPostStatement();
		WResultSet rs = new WResultSet(m_dataTable, null, true, null, postStatement);
		if (m_app.m_webMode && m_app.m_common.m_reuseMetadataOnLoadForStore && m_descriptorOnLoad != null)
			rs.setDescriptor(m_descriptorOnLoad);
		return rs.open(true, postStatement) ? rs : null;
	}

	protected void createSlaveTermBuffers(int pilotSize) {
		int oldMainIterator = m_mainIterator;
		for (m_mainIterator = 0; m_mainIterator < pilotSize; m_mainIterator++) {
			JotyDataBuffer newBuffer = new TermBuffer(this, m_targetDatumField);
			m_dataBuffers.put(mainTermData(m_mainIterator), newBuffer);
			WResultSet rs = new WResultSet(null, dataQuery());
			newBuffer.loadData(rs, null, m_mainIterator == oldMainIterator ? this : null);
		}
		m_mainIterator = oldMainIterator;
	}

	protected void doLoadData() {
		m_app.beginWaitCursor();
		if (m_slave) {
			int pilotSize = ((GridTerm) m_panel.term(m_mainTermName)).getRowQty();
			clearSlaveBuffers();
			createSlaveTermBuffers(pilotSize);
			m_dataBuffer = slaveBuffer();
			if (pilotSize > 0) {
				m_updatingActor = true;
				m_panel.guiDataExch(false);
			}
		} else {
			WResultSet rs = new WResultSet(null, dataQuery());
			prepareRs(rs);
			m_dataBuffer.empty(false);
			m_dataBuffer.loadData(rs, m_queryDefPostStatement, this);
			m_updatingActor = true;
			lookForDataStructure(rs);
		}
		m_app.endWaitCursor();
	}

	protected boolean doStoreData() {
		boolean success = true;
		if (m_slave) {
			int pilotSize = m_panel.termBuffer(m_mainTermName).m_records.size();
			int oldPrimaryIterator = m_mainIterator;
			JotyDataBuffer buffer;
			clearData(false);
			for (int k = 0; k < pilotSize; k++) {
				m_mainIterator = k;
				buffer = slaveBuffer();
				if (buffer != null)
					success = storeRecords(buffer);
			}
			m_mainIterator = oldPrimaryIterator;
		} else {
			if (m_dataBuffer != null) {
				clearData(false);
				success = storeRecords(m_dataBuffer);
			}
		}
		return success;
	}

	@Override
	public CellDescriptor fieldDescr(String name) {
		CellDescriptor retVal = (Beans.isDesignTime() ? m_gridRowDescriptor.new CellDescriptor() : m_gridRowDescriptor.get(name));
		if (retVal == null)
			m_app.JotyMsg(this, "No field '" + name + "' present in the set of term '" + m_name + " !");
		return retVal;
	}

	protected int getRowQty() {
		return 0;
	}

	public ScrollGridPane getScrollPane() {
		return null;
	}

	@Override
	void init() {
		super.init();
		getScrollPane().init();
	}

	public void lookForDataStructure(WResultSet rs) {
		if (m_app.m_webMode && m_descriptorOnLoad == null && rs != null)
			m_descriptorOnLoad = rs.m_cursor;
		for (String fieldName : m_foreignFields)
			rs.m_cursor.m_fieldsMap.get(fieldName).m_foreign = true;
	}

	public void manageDoubleClick(MouseEvent e) {
		if (isWindowEnabled()) {
			if (m_actionOnRowHandler != null)
				m_actionOnRowHandler.doAction(this, getScrollPane().getSelectedColumn(e));
		}
	}

	protected void prepareRs(WResultSet rs) {}

	@Override
	public void preRender() {
		clearComponent();
	}

	@Override
	public void refresh() {
		if (m_slave)
			m_mainIterator = mainSelection();
		if (bufferToLoad() || isAControlTerm())
			loadData();
		else
			termRender();
		checkSelection();
		if (!m_slave)
			refreshSlaveTerm();
	}

	public void setAsForeignField(String name) {
		m_foreignFields.add(name);
	}

	public void setMainEntityField(String fieldName) {
		m_mainEntityKeyField = fieldName;
	}

	private void setMainEntityFieldValue(JotyDataBuffer dataBuffer, int recordIndex) {
		WrappedField wfield = dataBuffer.wfield(m_mainEntityKeyField, recordIndex);
		wfield.setInteger(mainEntityVal());
		DataAccessPanel panel = (DataAccessPanel) m_panel;
		panel.setDelayed(wfield, panel.entityIdValuePending());
	}

	private boolean storeRecords(JotyDataBuffer dataBuffer) {
		boolean success = true;
		WResultSet rs = createAndOpenWRset();
		if (rs != null) {
			for (int i = 0; i < dataBuffer.m_records.size(); i++) {
				rs.addNew();
				if (m_mainEntityKeyField != null && m_mainTermName == null)
					setMainEntityFieldValue(dataBuffer, i);
				dataBuffer.writeRecord(i, rs);
				if (m_definedSetMethod != null)
					m_definedSetMethod.method(rs, (DataAccessPanel) m_panel);
				success = rs.update(true, false, createContextPostStatement());
				if (!success)
					break;
			}
			rs.close();
		}
		return success;
	}

	@Override
	public void termRender(boolean checkUnselection) {
		int oldPos = 0;
		m_mainIterator = mainSelection();
		if (m_dataBuffer != null)
			oldPos = m_dataBuffer.m_cursorPos;
		if (m_slave && m_mainIterator >= 0)
			m_dataBuffer = slaveBuffer();
		bufferRender();
		if (m_dataBuffer != null) {
			if (m_dataBuffer.m_records.size() <= oldPos)
				oldPos = m_dataBuffer.m_records.size() - 1;
			m_dataBuffer.m_cursorPos = oldPos;
			if (checkUnselection)
				checkSelection();
			else {
				checkComponentsState();
				updateDriverTerms();
			}
			m_panel.guiDataExch();
			if (checkUnselection) {
				if (m_selector != null)
					m_selector.select(this);
				else
					setCurSel(m_dataBuffer.m_cursorPos);
			} else
				setCurSel(oldPos);
		}
		if (m_slaveTermName != null)
			m_panel.term(m_slaveTermName).termRender(true, true);
	}

	@Override
	protected void updateAspect() {
		if (!m_updatingActor)
			if (m_reloadNeeded && (dataQuery() != null || explicitQuery())) {
				clearComponent();
				if (	getComponent().isVisible() && 
						(!m_panel.injectedDialog().m_is_deleting && (
								!(m_panel instanceof DataAccessPanel) || !((DataAccessPanel) m_panel).m_isNewRec) || isDataComplement()
						))
					loadData();
				m_reloadNeeded = false;
			}
	}


}
