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

package org.joty.workstation.data;

import java.util.*;
import java.util.Map.Entry;

import org.joty.access.Logger;
import org.joty.common.CaselessStringKeyMap;
import org.joty.common.JotyTypes;
import org.joty.common.BasicPostStatement;
import org.joty.data.JotyDate;
import org.joty.data.FieldDescriptor;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.gui.GridTerm;

/**
 * It is the most important class for data buffering.
 * <p>
 * It constructs an object made by {@code Record} objects all having the same
 * structure (like the records in a database table). The structure of the record
 * is usually determined by the collection descriptors objects (of type
 * {@code FieldDescriptor} of the {@code WResultSet} object, normal source for
 * the data population of the buffer. This structure is 'imported in a local
 * 'record descriptor' (currently spread on more members) for independent use
 * within the buffer.
 * <p>
 * Each record is made by {@code WrappedField} objects the data type of which could be
 * derived by the descriptor on position basis, and the same can be said for the
 * name of the database field, nevertheless each WrappedField instance holds a
 * built-in reference to the descriptor host, that is to the container buffer
 * instance, and the positional index, such that to be able to get meta-data
 * from its inside.
 * <p>
 * The class offers several methods for accessing the single 'cell' of the
 * buffer and to get complete information from it or from helper structures, and
 * methods for modifying values of a record, for adding and deleting records.
 * <p>
 * The buffer may have defined one data field as key; then, on the value assumed
 * by this field a map is built during data loading, so that normal lookup
 * operations on the contained data may be performed.
 * 
 * @see Record
 * @see WrappedField
 * @see WResultSet
 * @see FieldDescriptor
 * 
 */

public class JotyDataBuffer {
	public class IdsStock extends HashMap<Long, Integer> {
		public boolean isPresent(Long key) {
			return get(key) != null;
		}

		public Integer put(Long key) {
			return super.put(key, size());
		}
	}

	/**
	 * This class implements a linked list of IDs to provide a queuing service
	 * for a group of entities all identified by the own unique index.
	 * <p>
	 * It supports adding and deleting operations to the queue, indeed, but, not
	 * only, what is the most useful feature of this object, a linear
	 * representation of the queue is offered to the user: all items are
	 * presented with a whatever verbose description, derived by any reasonable
	 * data joining, and they are listed in the queuing order. This is obtained
	 * with a mapping between the positioning index on the presented list and
	 * the positioning index of the record as loaded in memory.
	 * <p>
	 * The most of the actions of the user that imply the modification of the
	 * queue must be performed as transaction: this class makes available tools
	 * for making it easy.
	 * <p>
	 * At a user's actions level this class strictly collaborates with the
	 * {@code QueuedDataTransferHandler}.
	 * 
	 * @see org.joty.workstation.gui.QueuedDataTransferHandler
	 * 
	 */
	public class QueueManager {

		/** the list atom in memory */
		class QueueRecord {
			long ID;
			long previousID;
			long nextID;
		}

		public String idField;
		public String prevField;
		public String nextField;
		public long m_sourcePrevID;
		public long m_sourceNextID;
		public int m_sourcePrecBP;
		public int m_sourceNextBP;
		public Integer m_sourceBP;
		public int m_targetBP;
		String m_managedDbTable;

		String m_id_dbField;

		/** the queue records as loaded in memory */
		private Vector<QueueRecord> m_queueRecords;

		private Map<Long, Integer> m_idsMap;
		private Map<Integer, Integer> m_orderMap;
		private Map<Integer, Integer> m_orderReverseMap;
		private int m_startIdx;

		QueueManager() {
			m_queueRecords = new Vector<QueueRecord>();
			m_idsMap = new HashMap<Long, Integer>();
			m_orderMap = new HashMap<Integer, Integer>();
			m_orderReverseMap = new HashMap<Integer, Integer>();
			m_startIdx = 0;
		}

		public void addRecord(WResultSet rs) {
			QueueRecord qRecord = new QueueRecord();
			getQueueRecord(qRecord, rs);
			int currIndex = m_queueRecords.size();
			m_queueRecords.add(qRecord);
			m_idsMap.put(qRecord.ID, currIndex);
			if (qRecord.previousID == 0)
				m_startIdx = currIndex;
		}

		/**
		 * Called by {@code loadData} of the container class, it builds the core
		 * data structures used by the GUI part of the queue, implemented by the
		 * {@code Table} object.
		 * 
		 * @see org.joty.workstation.gui.Table
		 */
		public void buildOrderMap() {
			int currIdx = m_startIdx;
			for (int orderPos = 0; orderPos < m_queueRecords.size(); orderPos++) {
				m_orderMap.put(orderPos, currIdx);
				m_orderReverseMap.put(currIdx, orderPos);
				if (m_queueRecords.get(currIdx).nextID != 0)
					currIdx = m_idsMap.get(m_queueRecords.get(currIdx).nextID);
			}
		}

		public boolean deleteRecord(long id) {
			return m_app.executeSQL("Delete from " + managedDbTable() + " Where " + m_id_dbField + " = " + id, null, createContextPostStatement());
		}

		public void empty() {
			m_queueRecords.removeAllElements();
			m_idsMap.clear();
			m_orderMap.clear();
			m_orderReverseMap.clear();
		}

		public int getID(int buffPosition) {
			return (int) m_queueRecords.get(buffPosition).ID;
		}

		public int getIDFromRowIndex(int buffPosition) {
			return getID(getMappedRow(buffPosition));
		}

		public Integer getIdRow(long id) {
			return m_idsMap.get(id);
		}

		public Integer getMappedRow(int row) {
			Integer rowIndex = m_orderMap.get(row);
			return row == -1 ? -1 : (rowIndex == null ? -1 : rowIndex);
		}

		public int getNextID(int buffPosition) {
			return (int) m_queueRecords.get(buffPosition).nextID;
		}

		public int getPrevID(int buffPosition) {
			return (int) m_queueRecords.get(buffPosition).previousID;
		}

		private void getQueueRecord(QueueRecord qRecord, WResultSet rs) {
			qRecord.ID = rs.integerValue(idField);
			qRecord.previousID = rs.integerValue(prevField);
			qRecord.nextID = rs.integerValue(nextField);
		}

		public Integer getReverseMappedRow(int row) {
			return m_orderReverseMap.get(row);
		}

		protected String managedDbTable() {
			return m_app.codedTabName(m_managedDbTable);
		}

		/**
		 * Makes the move.
		 * <p>
		 * It worth noting that the method relies on the features of the
		 * framework that sustain a transaction through the web or not.
		 * 
		 * @param id
		 *            the entity id
		 * @param m_targetIndex
		 *            (optionally valued) if the list is target of the move it
		 *            is the index of target row before of which or upon on
		 *            which the new record must take place.
		 * @param internalMove
		 *            true in the case of a move of an 'atom' from a position in
		 *            the queue to a new one.
		 * @param delayedId
		 *            true if in web mode and a new record is being created
		 * @return true on success
		 */
		public boolean manageQueueOnDbTable(long id, int m_targetIndex, boolean internalMove, boolean delayedId) {
			boolean m_success = true;
			long targetNextID = 0;
			if (m_targetIndex > 0) {
				m_targetBP = getMappedRow(m_targetIndex - 1);
				targetNextID = getNextID(m_targetBP);
			}
			int targetNextBP = -1;
			if (targetNextID > 0)
				targetNextBP = getIdRow(targetNextID);
			long sourceID = internalMove ? getID(m_sourceBP) : id;
			try {
				// update the source
				if (internalMove)
					m_success = manageRemoval();
				if (m_success)
					m_success = updateTable(prevField, m_targetIndex > 0 ? getID(m_targetBP) : 0, sourceID, false, delayedId);
				if (m_success)
					m_success = updateTable(nextField, m_targetIndex > 0 ? targetNextID : (m_records.size() == 0 ? 0 : getIDFromRowIndex(0)), sourceID, false, delayedId);
				// update NEW neighborhood
				if (m_success && m_targetIndex > 0)
					m_success = updateTable(nextField, sourceID, getID(m_targetBP), delayedId, false);
				if (m_success && m_records.size() > 0)
					m_success = updateTable(prevField, sourceID, m_targetIndex > 0 ? (targetNextID > 0 ? getID(targetNextBP) : 0) : getIDFromRowIndex(0), delayedId, false);
			} catch (Exception e) {
				m_success = false;
				Logger.exceptionToHostLog(e);
			}
			return m_success;
		}

		public boolean manageRemoval() {
			boolean retVal = true;
			if (m_sourcePrevID > 0)
				retVal = updateTable(nextField, m_sourceNextID, getID(m_sourcePrecBP));
			if (m_sourceNextID > 0 && retVal)
				retVal = updateTable(prevField, m_sourcePrevID, getID(m_sourceNextBP));
			return retVal;
		}

		/**
		 * Identifies all the sources and all the targets for the assignments
		 * that will make the 'move' in the queue
		 */
		public void prepareTransaction(int m_index) {
			m_sourceBP = getMappedRow(m_index);
			m_sourcePrevID = getPrevID(m_sourceBP);
			m_sourceNextID = getNextID(m_sourceBP);
			m_sourcePrecBP = -1;
			if (m_sourcePrevID > 0)
				m_sourcePrecBP = getIdRow(m_sourcePrevID);
			m_sourceNextBP = -1;
			if (m_sourceNextID > 0)
				m_sourceNextBP = getIdRow(m_sourceNextID);
		}

		public void setMainMetadata(String managedDbTable, String id_dbField) {
			m_managedDbTable = managedDbTable;
			m_id_dbField = id_dbField;
		}

		private boolean updateTable(String field, long value, long sourceID) {
			return updateTable(field, value, sourceID, false, false);
		}

		private boolean updateTable(String field, long value, long sourceID, boolean valueDelayed, boolean sourceIdDelayed) {
			return m_app.executeSQL("Update " + managedDbTable() + " set " + field + " = " + valueExpr(value, valueDelayed) + " where " + m_id_dbField + " = " + valueExpr(sourceID, sourceIdDelayed), null, createContextPostStatement());
		}

		private String valueExpr(long val, boolean valDelayed) {
			return (valDelayed ? m_app.m_webClient.getGenToken(0) : (val == 0 ? "NULL" : String.valueOf(val)));
		}

	}

	public class Record {
		public Vector<WrappedField> m_data;

		public Record() {
			super();
			m_data = new Vector<WrappedField>();
		}

		void clear() {
			for (WrappedField wfield : m_data)
				wfield.clear();
		}

	}

	Application m_app = Application.m_app;

	public String m_keyName;

	public boolean m_textKey;
	public Vector<Record> m_records;
	public int m_cursorPos;
	public boolean m_descriptorBuilt;
	protected int m_keyIndex;
	public int m_firstKeyPos;
	protected Record m_record;
	public CaselessStringKeyMap<Integer> m_fieldNamesMap;
	protected CaselessStringKeyMap<Integer> m_strKeyMap;
	protected Map<Long, Integer> m_longKeyMap;
	public Vector<String> m_fieldNames;
	public Vector<Integer> m_fieldTypes;
	protected int m_maxRecord;
	protected int m_recPos;
	public QueueManager m_queueManager;
	public CaselessStringKeyMap<IdsStock> m_idsStocksMap;

	public JotyDataBuffer() {
		m_keyIndex = -1;
		m_firstKeyPos = -1;
		m_descriptorBuilt = false;
		m_textKey = false;
		m_keyName = null;
		m_cursorPos = -1;
		m_record = null;
		m_records = new Vector<Record>();
		m_fieldNamesMap = new CaselessStringKeyMap<Integer>(m_app);
		m_strKeyMap = new CaselessStringKeyMap<Integer>(m_app);
		m_longKeyMap = new HashMap<Long, Integer>();
		m_fieldNames = new Vector<String>();
		m_fieldTypes = new Vector<Integer>();
		m_idsStocksMap = new CaselessStringKeyMap<IdsStock>(m_app);
		m_maxRecord = 0;
		m_queueManager = null;
	}

	public JotyDataBuffer(String keyFieldName) {
		this();
		setKeyFieldName(keyFieldName);
	}

	protected void acquireRecordDescriptor(WResultSet rs) {
		for (int i = 0; i < rs.m_colCount; i++)
			colDescriptorIntoRecordDescriptor(rs.m_cursor.m_fields[i], i);
	}

	protected void addFromDataLayer(WResultSet rs) {
		m_cursorPos++;
		Record record = new Record();
		buildRecord(record, rs);
		getFromDataLayer(record, rs);
		m_records.add(record);
		if (m_queueManager != null)
			m_queueManager.addRecord(rs);
		updateBuffIndex();
		updateIdsStocks(m_cursorPos, true);
	}

	protected void buildRecord(Record record, WResultSet sourceRs) {
		checkRecordDescriptor(sourceRs);
		if (m_descriptorBuilt) {
			for (int i = 0; i < m_fieldNames.size(); i++)
				record.m_data.add(new WField(m_app));
			for (int i = 0; i < m_fieldNames.size(); i++) {
				((WField)record.m_data.get(i)).m_metaDataSource = this;
				record.m_data.get(i).m_idx = i;
				record.m_data.get(i).clear();
			}
		}
	}

	public void buildRecordDescriptor(WResultSet rs) {
		if (!m_descriptorBuilt) {
			acquireRecordDescriptor(rs);
			if (m_fieldTypes.size() == 0)
				m_app.JotyMsg(this, "Record descriptor contains no elements !");
			else {
				if (m_keyName != null && m_keyName.length() > 0) {
					m_keyIndex = getFieldIndex(m_keyName);
					if (m_keyIndex >= 0)
						m_textKey = m_fieldTypes.get(m_keyIndex) == JotyTypes._text;
				}
			}
			m_descriptorBuilt = true;
		}
	}

	private void checkBufferIndex(String fieldName) {
		if (fieldName == null)
			m_app.JotyMsg(this, "No field name found in the WrappedField !");
		else if (m_keyName.compareToIgnoreCase(fieldName) == 0)
			updateBuffIndex();
	}

	boolean checkDataAccess(int typeID, int fieldIdx, String methodBeingChecked, int recordPos) {
		return checkType(typeID, fieldIdx, methodBeingChecked) && recordPos >= 0;
	}

	protected void checkRecordDescriptor(WResultSet sourceRs) {
		if (!m_descriptorBuilt)
			if (sourceRs != null)
				buildRecordDescriptor(sourceRs);
			else {
				m_app.JotyMsg(this, "Record structure not loaded !");
				m_app.ASSERT(false);
			}
	}

	boolean checkType(int typeID, int fieldIdx, String methodBeingChecked) {
		boolean typeMatch = true;
		if (Application.m_debug) {
			if (fieldIdx >= 0) {
				int definedType = m_fieldTypes.get(fieldIdx);
				typeMatch = WrappedField.checkType(definedType, typeID);
				if (!typeMatch) {
					String msg;
					msg = String.format("Uncompatible parameter type in calling '%1$s' relating to database type of FIELD '%2$s'", methodBeingChecked, m_fieldNames.get(fieldIdx));
					m_app.JotyMsg(this, msg);
				}
			}
		}
		return typeMatch && fieldIdx >= 0;
	}

	protected void colDescriptorIntoRecordDescriptor(FieldDescriptor colDescr, int pos) {
		m_fieldNamesMap.put(colDescr.m_strName, pos);
		m_fieldNames.add(colDescr.m_strName);
		m_fieldTypes.add(colDescr.m_nType);
	}

	public void copyFrom(JotyDataBuffer srcBuffer) {
		String fldName;
		for (int i = 0; i < srcBuffer.m_fieldNames.size(); i++) {
			fldName = srcBuffer.m_fieldNames.get(i);
			WrappedField destWField = wfield(fldName, false, true);
			if (destWField != null)
				destWField.copyWField(srcBuffer.wfield(fldName), false);
		}
	}

	protected BasicPostStatement createContextPostStatement() {
		return null;
	}

	public JotyDate dateTimeValue(String fieldName) {
		return dateTimeValue(fieldName, -1);
	}

	public JotyDate dateTimeValue(String fieldName, int recPos) {
		return (JotyDate) wfield(fieldName, JotyTypes._dateTime, "dateTimeValue", recPos).m_dateVal;
	}

	public JotyDate dateValue(String fieldName) {
		return dateValue(fieldName, -1);
	}

	public JotyDate dateValue(String fieldName, int recPos) {
		return (JotyDate) wfield(fieldName, JotyTypes._date, "dateValue", recPos).m_dateVal;
	}

	public double dblValue(String fieldName) {
		return dblValue(fieldName, -1);
	}

	public double dblValue(String fieldName, int recPos) {
		return wfield(fieldName, JotyTypes._double, "dblValue", recPos).m_dblVal;
	}

	public void deleteRecord() {
		if (m_keyName != null) {
			if (m_textKey)
				m_strKeyMap.remove(getKeyVal().m_strVal);
			else
				m_longKeyMap.remove(getKeyVal().getInteger());
		}
		updateIdsStocks(m_cursorPos, false);
		m_records.removeElementAt(m_cursorPos);
		reloadBuffIndex();
		m_cursorPos = -1;
	}

	void deleteRecords(String fieldName, long value) {
		int oldCursorPos = m_cursorPos;
		for (int i = 0; i < m_records.size(); i++) {
			m_cursorPos = i;
			if (integerValue(fieldName) == value)
				deleteRecord();
		}
		m_cursorPos = oldCursorPos;
	}

	public void empty() {
		empty(true);
	}

	public void empty(boolean withDescriptor) {
		for (int i = 0; i < m_records.size(); i++)
			m_records.removeElementAt(i);
		m_records.removeAllElements();
		m_strKeyMap.clear();
		m_longKeyMap.clear();
		emptyIdsStocks();
		if (withDescriptor && m_descriptorBuilt) {
			if (m_fieldNamesMap.size() > 0)
				m_fieldNamesMap.clear();
			if (m_fieldTypes.size() > 0)
				m_fieldTypes.removeAllElements();
			if (m_fieldNames.size() > 0)
				m_fieldNames.removeAllElements();
			m_descriptorBuilt = false;
		}
		m_cursorPos = -1;
		if (m_queueManager != null)
			m_queueManager.empty();
	}

	private void emptyIdsStocks() {
		IdsStock idsStock;
		for (Iterator it = m_idsStocksMap.entrySet().iterator(); it.hasNext();) {
			idsStock = ((Entry<String, IdsStock>) it.next()).getValue();
			if (idsStock != null)
				idsStock.clear();
		}
	}

	public String getCellValue(int colIndex) {
		return m_records.get(m_cursorPos).m_data.get(colIndex).m_strVal;
	}

	public String getCellValue(int row, int col) {
		return m_records.get(row).m_data.get(col).m_strVal;
	}

	public int getFieldIndex(String fieldName) {
		return getFieldIndex(fieldName, false, false);
	}

	int getFieldIndex(String fieldName, boolean updatedRecPos, boolean debugSilent) {
		if (updatedRecPos && m_recPos == -1)
			m_recPos = m_cursorPos;
		int retVal = -1;
		Integer idx = m_fieldNamesMap.get(fieldName);
		if (idx == null) {
			if (Application.m_debug && !debugSilent) {
				if (m_fieldNamesMap.size() > 0)
					m_app.JotyMsg(this, String.format("DEBUG INFO : WrappedField '%1$s' not found ! (it could be correct ! it is to you checking the scenario)", fieldName));
			}
		} else
			retVal = idx;
		return retVal;
	}

	public int getFieldType(int index) {
		return m_fieldTypes.get(index);
	}

	protected void getFromDataLayer(Record record, WResultSet rs) {
		for (int i = 0; i < rs.m_colCount; i++) {
			record.m_data.get(i).m_dataType = rs.m_cursor.m_fields[i].m_nType;
			record.m_data.get(i).getWField(rs);
		}
	}

	public long getKeyLongVal() {
		return getKeyLongVal(-1);
	}

	public long getKeyLongVal(int pos) {
		return pos == -1 && m_cursorPos == -1 ? -1 : getKeyVal(m_keyName, pos).getInteger();
	}

	public Integer getKeyPos(Long lkey) {
		if (m_keyName == null)
			m_app.JotyMsg(this, "m_keyName is null: the key map doesn't work !");
		return m_longKeyMap.get(lkey);
	}

	public Integer getKeyPos(String strKey) {
		return m_strKeyMap.get(strKey);
	}

	public String getKeyStrVal(int pos) {
		return getKeyVal(m_keyName, pos).m_strVal;
	}

	public WrappedField getKeyVal() {
		return getKeyVal(null, -1);
	}

	public WrappedField getKeyVal(String fieldName) {
		return getKeyVal(fieldName, -1);
	}

	WrappedField getKeyVal(String fieldName, int pos) {
		if ((m_firstKeyPos >= 0 || m_keyName != null) && m_records.size() > 0) {
			return m_records.get(pos >= 0 ? pos : m_cursorPos).m_data.get(fieldName == null ? (m_keyName != null ? m_fieldNamesMap.get(m_keyName) : m_firstKeyPos) : m_fieldNamesMap.get(fieldName));
		} else
			return new WField(m_app);
	}

	public Record getRecord() {
		return m_cursorPos >= 0 ? m_records.get(m_cursorPos) : null;
	}

	public String getValueForSql(String fieldName) {
		String retStr = "", tempStr;
		try {
			WrappedField wfield = getWField(fieldName);
			if (wfield != null) {
				if (wfield.isNull())
					retStr = "null";
				else {
					tempStr = wfield.render(false, true);
					switch (wfield.dataType()) {
						case JotyTypes._text:
							retStr = "'" + tempStr.replace("'", "''") + "'";
							break;
						case JotyTypes._long:
						case JotyTypes._int:
						case JotyTypes._double:
						case JotyTypes._single:
							if (wfield.m_delayed)
								retStr = m_app.m_webClient.getGenToken(wfield.m_posIndexAsReturningValue);
							else
								retStr = tempStr.isEmpty() ? "null" : tempStr;
							break;
						case JotyTypes._date:
						case JotyTypes._dateTime:
							retStr = tempStr;
							break;
					}
				}
			}
		} catch (Exception e) {
			Logger.exceptionToHostLog(e);
		}
		return retStr;
	}

	public WrappedField getWField(int index) {
		WrappedField wfield = null;
		if (index >= 0 && m_cursorPos >= 0)
			wfield = m_records.get(m_cursorPos).m_data.get(index);
		return wfield;
	}

	public WrappedField getWField(int row, int col) {
		return m_records.get(row).m_data.get(col);
	}

	public WrappedField getWField(String fieldName) {
		return getWField(getFieldIndex(fieldName));
	}

	public boolean inSetKeyCheck(WrappedField wfield) {
		boolean retVal = false;
		int keyIdx;
		WrappedField keyInRecord = null;
		for (Record record : m_records) {
			keyIdx = keyIndex();
			if (keyIdx >= 0) {
				keyInRecord = record.m_data.get(keyIdx);
				if (m_textKey && keyInRecord.m_strVal.compareToIgnoreCase(wfield.m_strVal) == 0 || 
						!m_textKey && (keyInRecord.m_jotyTypeFromDb == JotyTypes._long && keyInRecord.m_lVal == wfield.m_lVal || 
						keyInRecord.m_jotyTypeFromDb == JotyTypes._int && keyInRecord.m_iVal == wfield.m_iVal)) {
					retVal = true;
					break;
				}
			}
		}
		return retVal;
	}

	public long integerValue(String fieldName) {
		return integerValue(fieldName, -1);
	}

	public long integerValue(String fieldName, int recPos) {
		return wfield(fieldName, JotyTypes._dbDrivenInteger, "IntegerValue", recPos).getInteger();
	}

	public int intValue(String fieldName, int recPos) {
		return wfield(fieldName, JotyTypes._int, "intValue", recPos).m_iVal;
	}

	public boolean isNull(String fieldName) {
		return isNull(fieldName, -1);
	}

	public boolean isNull(String fieldName, int recPos) {
		return wfield(fieldName, recPos).isNull();
	}

	public int keyIndex() {
		if (m_keyIndex < 0 && m_keyName != null)
			m_keyIndex = getFieldIndex(m_keyName);
		return m_keyIndex;
	}

	public boolean keyIsText() {
		return m_textKey;
	}

	public boolean loadData(WResultSet rs) {
		return loadData(rs, null, null);
	}

	public boolean loadData(WResultSet rs, BasicPostStatement postStatement) {
		return loadData(rs, postStatement, null);
	}

	public boolean loadData(WResultSet rs, BasicPostStatement postStatement, GridTerm renderer) {
		boolean retVal = false;
		if (rs.open(postStatement)) {
			buildRecordDescriptor(rs);
			int i = 0;
			loadDataProlog(rs);
			while (!rs.isEOF()) {
				i++;
				if (loadDataBreak(i))
					break;
				addFromDataLayer(rs);
				if (renderer != null)
					renderer.renderRecord(rs, m_keyName);
				rs.next();
			}
			rs.close();
			retVal = true;
			if (m_queueManager != null)
				m_queueManager.buildOrderMap();
		}
		return retVal;
	}

	protected boolean loadDataBreak(int count) {
		return false;
	}

	protected void loadDataProlog(WResultSet rs) {}

	public long longValue(String fieldName, int recPos) {
		return wfield(fieldName, JotyTypes._long, "longValue", recPos).m_lVal;
	}

	public void newRecord(WResultSet sourceRs) {
		Record record = new Record();
		buildRecord(record, sourceRs);
		m_records.add(record);
		m_cursorPos = m_records.size() - 1;
	}

	public boolean recordLookup(long lKey) {
		if (lKey >= 0) {
			Integer currentRecordPos = getKeyPos(lKey);
			m_cursorPos = currentRecordPos == null ? -1 : currentRecordPos;
			return currentRecordPos != null;
		} else
			return false;
	}

	public boolean recordLookup(String strKey) {
		m_cursorPos = -1;
		Integer currentRecordPos = getKeyPos(strKey);
		m_cursorPos = currentRecordPos == null ? -1 : currentRecordPos;
		return currentRecordPos != null;
	}

	void reloadBuffIndex() {
		m_strKeyMap.clear();
		m_longKeyMap.clear();
		for (int i = 0; i < m_records.size(); i++) {
			if (m_textKey)
				m_strKeyMap.put(strValue(m_keyName, i), i);
			else
				m_longKeyMap.put(integerValue(m_keyName, i), i);
		}
	}

	public void setCellValue(String value, int row, int col) {
		WrappedField wfiend = m_records.get(row).m_data.get(col);
		wfiend.m_strVal = value;
		wfiend.setToNull(false);
	}

	public void setInteger(String fieldName, long lVal) {
		setInteger(fieldName, lVal, -1);
	}

	public void setInteger(String fieldName, long lVal, int recPos) {
		wfield(fieldName, JotyTypes._dbDrivenInteger, recPos).setInteger(lVal);
		checkBufferIndex(fieldName);
	}

	public void setKeyFieldName(String name) {
		m_keyName = name;
	}

	/**
	 * Instantiates the QueueManager object and sets it with the data table
	 * fields participating in the queue implementation.
	 * 
	 * @see QueueManager
	 */

	public void setRowsQueuing(String idField, String prevField, String nextField) {
		m_queueManager = new QueueManager();
		m_queueManager.idField = idField;
		m_queueManager.prevField = prevField;
		m_queueManager.nextField = nextField;
	}

	public void setValue(String fieldName, double dblVal) {
		setValue(fieldName, dblVal, -1);
	}

	void setValue(String fieldName, double dblVal, int recPos) {
		wfield(fieldName, JotyTypes._double, recPos).setVal(dblVal);
	}

	public void setValue(String fieldName, float fltVal) {
		setValue(fieldName, fltVal, -1);
	}

	void setValue(String fieldName, float fltVal, int recPos) {
		wfield(fieldName, JotyTypes._single, recPos).setVal(fltVal);
	}

	public void setValue(String fieldName, int iVal) {
		setValue(fieldName, iVal, -1);
	}

	public void setValue(String fieldName, int iVal, int recPos) {
		wfield(fieldName, JotyTypes._int, recPos).setVal(iVal);
	}

	public void setValue(String fieldName, JotyDate dateVal) {
		setValue(fieldName, dateVal, -1);
	}

	void setValue(String fieldName, JotyDate dateVal, int recPos) {
		wfield(fieldName, JotyTypes._date, recPos).setVal(dateVal);
	}

	public void setValue(String fieldName, long lVal) {
		setValue(fieldName, lVal, -1);
	}

	public void setValue(String fieldName, long lVal, int recPos) {
		wfield(fieldName, JotyTypes._long, recPos).setVal(lVal);
		checkBufferIndex(fieldName);
	}

	public void setValue(String fieldName, String strVal) {
		setValue(fieldName, strVal, -1);
	}

	public void setValue(String fieldName, String strVal, int recPos) {
		wfield(fieldName, JotyTypes._text, recPos).setVal(strVal);
		checkBufferIndex(fieldName);
	}

	public void setValue(String fieldName, WrappedField source) {
		wfield(fieldName).copyWField(source, false);
	}

	public float sngValue(String fieldName) {
		return sngValue(fieldName, -1);
	}

	public float sngValue(String fieldName, int recPos) {
		return wfield(fieldName, JotyTypes._single, "sngValue", recPos).m_fltVal;
	}

	public String strValue(String fieldName) {
		return strValue(fieldName, -1);
	}

	public String strValue(String fieldName, int recPos) {
		return wfield(fieldName, JotyTypes._text, "strValue", recPos).m_strVal;
	}

	protected void updateBuffIndex() {
		if (m_keyName != null && m_keyName.length() > 0) {
			m_app.ASSERT(m_cursorPos >= 0);
			if (m_textKey)
				m_strKeyMap.put(strValue(m_keyName), m_cursorPos);
			else
				m_longKeyMap.put(integerValue(m_keyName), m_cursorPos);
		}
	}

	private void updateIdsStocks(int cursorPos, boolean forAdd) {
		IdsStock idsStock;
		String fieldName;
		Entry<String, IdsStock> entry;
		Long value;
		for (Iterator it = m_idsStocksMap.entrySet().iterator(); it.hasNext();) {
			entry = ((Entry<String, IdsStock>) it.next());
			idsStock = entry.getValue();
			if (idsStock != null) {
				fieldName = entry.getKey();
				value = integerValue(fieldName, cursorPos);
				if (value != 0)
					if (forAdd)
						idsStock.put(value);
					else
						idsStock.remove(value);
			}
		}
	}

	public WrappedField wfield(String fieldName) {
		return wfield(fieldName, -1);
	}

	public WrappedField wfield(String fieldName, boolean debugSilent) {
		return wfield(fieldName, JotyTypes._none, "wfield", -1, debugSilent);
	}

	public WrappedField wfield(String fieldName, boolean debugSilent, boolean canBeNull) {
		return wfield(fieldName, JotyTypes._none, "wfield", -1, debugSilent, canBeNull);
	}

	public WrappedField wfield(String fieldName, int recPos) {
		return wfield(fieldName, JotyTypes._none, "wfield", recPos);
	}

	WrappedField wfield(String fieldName, int type, int recPos) {
		return wfield(fieldName, type, "setValue", recPos);
	}

	WrappedField wfield(String fieldName, int type, String verbose, int recPos) {
		return wfield(fieldName, type, "setValue", recPos, false);
	}

	WrappedField wfield(String fieldName, int type, String verbose, int recPos, boolean debugSilent) {
		return wfield(fieldName, type, verbose, recPos, debugSilent, false);
	}

	WrappedField wfield(String fieldName, int type, String verbose, int recPos, boolean debugSilent, boolean canBeNull) {
		m_recPos = recPos;
		int i = getFieldIndex(fieldName, true, debugSilent);
		if (canBeNull && i < 0)
			return null;
		if (checkDataAccess(type, i, verbose, m_recPos))
			return m_records.get(m_recPos).m_data.get(i);
		else
			return new WField(m_app);
	}

	public boolean writeRecord(int recordPos, WResultSet rs) {
		boolean success = true;
		int oldCurrPos = m_cursorPos;
		m_cursorPos = recordPos;
		FieldDescriptor fieldDescr;
		for (int i = 0; i < rs.m_colCount; i++) {
			fieldDescr = rs.m_cursor.m_fields[i];
			if (fieldDescr.m_toUpdate && !fieldDescr.m_foreign)
				wfield(fieldDescr.m_strName).setWField(rs);
		}
		if (m_app.m_common.m_shared) {
			WrappedField wfield = wfield(m_app.m_common.m_sharingKeyField, true, true);
			if (wfield != null)
				/*
				 * the set could be relative to data shared by the sharing users
				 * (no exception is thrown if sharing key is not found)
				 */
				wfield.setWField(rs);
		}
		m_cursorPos = oldCurrPos;
		return success;
	}

	public boolean writeRecord(WResultSet rs) {
		return writeRecord(m_cursorPos, rs);
	}

}
