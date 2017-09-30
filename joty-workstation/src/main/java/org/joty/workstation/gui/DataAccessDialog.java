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

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.Beans;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.event.ListSelectionEvent;

import org.joty.app.JotyException;
import org.joty.data.WrappedField;
import org.joty.workstation.app.Application;
import org.joty.workstation.gui.DataAccessPanel.Permission;
import org.joty.workstation.gui.SearcherPanel.SearcherPanelContainer;

/**
 * The most apparent task of this class is to coordinate the editing of the data
 * managed by the contained {@code DataAccessPanel} instance(-s). This feature
 * is performed by adding to the {@code JotyDialog#m_buttonPane} object all
 * typical command buttons for editing operations and by mapping them on
 * convenient control actions that, beyond internal processing, gives the user
 * visual and operative feedback.
 * <p>
 * The class is featured with the tools needed to manage the possible situations
 * in which the user seems implicitly to abandon the editing session and the
 * class is provided with convenient messages to the user in any of those
 * situation. One of these tools is the inner
 * {@code EditAbandon_HooveringListener} class;
 * <p>
 * The JDialog object instantiated by means of this child class is not modal,
 * that is, at the same time, different application DataAccessDialog objects
 * can be opened, all of them originating from a different classes: it is not
 * possible (because it is not desired) to open more instance of the same class;
 * this is assured by the {@code JotyDialog@tryCreate} method that takes care of
 * anything for leaving the user comfortable with that.
 * <p>
 * Even originating from different classes all DataAccessDialog objects opened
 * at the same time can share the same data, especially in referencing them;
 * since one of them (typically one only) can also modify these data a way to
 * coordinate visual refreshing in all occurrences is needed: the Joty framework
 * implements a publisher-subscribers model to fulfill memory update needs,
 * keeping it simple and efficient. This class, indeed participates in the
 * implementation.
 * <p>
 * Furthermore the class drives the behavior of other visual object.
 * <p>
 * Its initialization makes the application main frame to be reduced as small
 * floating window.
 * <p>
 * It hosts a collection of {@code ViewersManager} objects that address (each of
 * them) the life of a group of {@code ViewerFrame} instances; the closing of
 * this class all the ViewersManager-s and the managed ViewerFrames.
 * <p>
 * This implementation is also responsible to store, in the workstation
 * environment, the coordinate of the enclosing window and the sort information
 * (the column and the sense) of main grid.
 ** <p>
 * When in the overall data management the implementation class has a data navigation role it can be
 * conveniently instantiated by the
 * {@code TermContainerPanel.acquireSelectedValueFrom} method; its behavior in
 * this case, is strongly determined by the context prepared by this caller.
 * 
 * @see ViewersManager
 * @see JotyFrame
 * @see JotyDialog
 */
public class DataAccessDialog extends JotyDialog {

	/**
	 * In is a MouseListener implementation installed in the most critical
	 * visual parts of the container class to help the identification of
	 * antagonist scenarios, like JotyTextField validation on invalid content,
	 * that itself is a blocking situation, versus the abandoning of the editing
	 * session, that implicitly means renouncing to modifications made to the
	 * data.
	 */
	public class EditAbandon_HooveringListener extends MouseAdapter {
		boolean m_direct;
		boolean m_exitActionEnabled;

		public EditAbandon_HooveringListener(boolean direct, boolean exitActionEnabled) {
			super();
			m_direct = direct;
			m_exitActionEnabled = exitActionEnabled;
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			m_editAbandoning = m_direct;
		}

		@Override
		public void mouseExited(MouseEvent e) {
			if (m_exitActionEnabled)
				m_editAbandoning = !m_direct;
		}
	}

	public JotyButton m_btnNew;

	public JotyButton m_btnEditOrNew;

	public JotyButton m_btnSave;
	public JotyButton m_btnDelete;
	public JotyButton m_btnNext;
	public JotyButton m_btnPrevious;
	public JotyButton m_btnHome;
	public JotyButton m_btnSelect;
	public Vector<ViewersManager> m_viewersManagers;
	public boolean m_editAbandoning;

	public boolean m_saving;
	protected EditAbandon_HooveringListener m_endEditHooveringListener;
	private String m_verboseDelEntity;

	String m_recordEntity;

	public DataAccessDialog() { // for WBE only
		if (!Beans.isDesignTime())
			Application.m_app.JotyMsg(this, "Call appropriate constructor !");;
	}

	public DataAccessDialog(Object callContext, Object openingMode) {
		this(callContext, openingMode, false);
	}

	public DataAccessDialog(Object callContext, Object openingMode, boolean initAction) {
		super(callContext, openingMode, initAction);
		m_defaultButton.setSize(28, 28);
		m_defaultButton.setLocation(558, 5);
		initButtonPane();
		m_btnSelect = new JotyButton(jotyLang("LBL_SELECT"));
		m_btnSelect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_gridManager.storeSelectedValues(null);
				close();
			}
		});
		getContentPane().add(m_btnSelect);
		m_btnSelect.setBounds(558, 339, 28, 28);
		m_viewersManagers = new Vector<ViewersManager>();
		m_endEditHooveringListener = new EditAbandon_HooveringListener(true, true);
		m_editAbandoning = false;
		if (!Beans.isDesignTime())
			m_btnOk.setEnabled(false);

	}

	protected DataAccessPanel addEnabling_driver() {
		return m_currSheet;
	}

	public boolean askAndSave() {
		return askAndSave(false);
	}

	boolean askAndSave(boolean leaveEditingOn) {
		boolean retVal = true;
		setVisible(true);
		retVal = Application.langYesNoQuestion("WantToSave", this);
		if (retVal) {
			if (save())
				saveEffects(leaveEditingOn);
			else
				retVal = false;
		}
		return retVal;
	}

	protected void beginEditing() {
		m_currSheet.beginEditing();
	}

	@Override
	protected void checkForHooveringListener() {
		m_btnCancel.addMouseListener(m_endEditHooveringListener);
		m_contentPanel.addMouseListener(new EditAbandon_HooveringListener(false, true));
	}

	@Override
	public void checkForHooveringListener(JComponent component) {
		component.addMouseListener(new EditAbandon_HooveringListener(false, false));
	}

	protected boolean checkForNormalBehavior() {
		return true;
	}

	private void checkForPublishing() {
		m_currSheet.checkForPublishing();
	}

	protected void checkForPublishings() {
		checkForPublishing();
	}

	protected String classIdentityName() {
		return getClass().getName() + "-" + m_entityName;
	}

	@Override
	protected void clearAppReferences() {
		if (m_currSheet != null)
			m_currSheet.clearAppReferences();
	}

	@Override
	public boolean close() {
		boolean retVal = true;
		if (m_currSheet != null)
			retVal = m_currSheet.checkHasDone();
		if (retVal)
			retVal = super.close();
		if (retVal) {
			m_app.m_windowsLocations.put(classIdentityName(), getLocation());
			GridManager gridManager = getGridManager();
			if (gridManager != null) {
				String mainSortInfo = masterGridManager().getListComponent().getSortInfo();
				if (mainSortInfo != null)
					m_app.m_dialogMainSortInfos.put(classIdentityName(), mainSortInfo);
			}
			for (ViewersManager comp : m_viewersManagers)
				comp.doCloseViewers(0, true);
		}
		return retVal;
	}

	@Override
	public void closeDependentDialogs() {
		if (m_currSheet != null)
			m_currSheet.closeDependentDialogs();
	}

	@Override
	protected boolean criticalValidation() {
		return m_editAbandoning && !m_isViewer;
	}

	protected boolean dataCreationEnabled() {
		return true;
	}

	protected boolean dataToBeLoaded() {
		return m_currSheet.dataToBeLoaded();
	}

	protected DataAccessPanel deleteEnabling_driver() {
		return m_currSheet;
	}

	protected void deletionEffects() {
		m_currSheet.deletionEffects();
		reloadAsLiteralStruct();
	}

	@Override
	protected void doActivationChange(Boolean activating) {
		updateCommandButtons(!isEditing());
		if (activating) 
			m_app.m_accessorMode = activating ? m_accessorMode : false;		
		m_btnSelect.setVisible(activating && m_app.m_dialogOpeningAsValueSelector && !(this instanceof SearcherPanelContainer));
		m_btnSelect.setEnabled(false);
		super.doActivationChange(activating);
	}

	private boolean doSave() {
		m_currSheet.manageTermsVisibleState();
		return save();
	}

	protected void endEditing(boolean justSaved) {
		m_newDocument = false;
		m_new_command = false;
		m_editOrNew_command = false;
		if (justSaved)
			m_currSheet.m_isNewRec = false;
		else {
			m_currSheet.m_isNewRec = m_currSheet.m_isNewRecOnIdle;
			if (!documentIdentified())
				m_currSheet.ensureClearTerms();
		}
		m_currSheet.endEditing(justSaved);
		m_currSheet.manageTermsConsistence();
		m_listenForPanelActions = false;
		guiDataExch(false);
		checkAndSetLook();
		resetDirtyStatus();
		m_editAbandoning = false;
		if (justSaved)
			reloadAsLiteralStruct();
	}

	public void filterInit(WrappedField keyWField) {
		m_currSheet.filterInit(keyWField);
	}

	protected boolean getAdd_EnablingState() {
		DataAccessPanel driver = addEnabling_driver();
		return !driver.isReadOnly() && !driver.isModifyOnly() && driver.m_permission.compareTo(Permission.readWriteAdd) >= 0 && gridManagerExists();
	}

	boolean getDelete_EnablingState() {
		return !m_isViewer && !m_currSheet.isReadOnly() && !m_currSheet.isModifyOnly() && m_currSheet.m_permission.compareTo(Permission.all) == 0 && !m_currSheet.m_isUnDeletable;
	}

	@Override
	public GridManager getGridManager() {
		if (m_currSheet == null) {
			if (m_app.m_debug)
				m_app.JotyMsg(this, "No data panel currently instantiated !");
			return null;
		} else
			return m_currSheet.m_targetPanel != null ? m_currSheet.m_targetPanel.getGridManager() : m_currSheet.getGridManager();
	}

	DataAccessPanel getMainPanel() {
		return m_currSheet;
	}

	public String getQuerySet() {
		String retVal = getMode() == null ? query() : selectiveQuery();
		if (retVal == null && getMode() != null && !m_accessorMode)
			m_app.JotyMsg(this, "No data set definition !");
		return retVal;
	}

	@Override
	protected JotyButton getSelectorButton() {
		return m_btnSelect;
	}

	public String getUpdatableSet() {
		String retVal = getMode() == null ? updatableSet() : selectiveUpdatableSet();
		if (retVal == null && getMode() != null && !m_accessorMode)
			m_app.JotyMsg(this, "No updatable set defined !");
		return retVal;
	}

	@Override
	int gridPos() {
		return gridManagerExists() ? super.gridPos() : -1;
	}

	protected void initButtonPane() {
		m_btnOk.setVisible(false);
		m_btnCancel.setBounds(128, 5, 28, 28);
		m_btnClose.setBounds(511, 5, 28, 28);

		m_btnNew = new JotyButton(imageIcon("newBtn.jpg"));
		buildButton(m_btnNew, jotyLang("LBL_NEW"), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkEnableAction())
					onNew();
			}
		});
		m_btnNew.setBounds(29, 5, 28, 28);
		m_buttonPane.add(m_btnNew);

		m_btnEditOrNew = new JotyButton();
		buildButton(m_btnEditOrNew, null, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkEnableAction())
					onEditOrNew();
			}
		});
		m_btnEditOrNew.setBounds(94, 5, 28, 28);
		m_buttonPane.add(m_btnEditOrNew);

		m_btnDelete = new JotyButton(imageIcon("delBtn.jpg"));
		buildButton(m_btnDelete, jotyLang("LBL_DEL"), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkEnableAction())
					onDelete();
			}
		});
		m_btnDelete.setBounds(166, 5, 28, 28);
		m_buttonPane.add(m_btnDelete);

		m_btnSave = new JotyButton(imageIcon("saveBtn.jpg"));
		buildButton(m_btnSave, jotyLang("LBL_SAVE"), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkEnableAction())
					onSave();
			}
		});
		m_btnSave.setBounds(94, 5, 28, 28);
		m_buttonPane.add(m_btnSave);
		m_btnSave.addMouseListener(m_endEditHooveringListener);

		m_btnNext = new JotyButton(imageIcon("nextBtn.jpg"));
		buildButton(m_btnNext, jotyLang("LBL_NEXT") + recordEntityTail(), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkEnableAction())
					onNext();
			}
		});
		m_btnNext.setBounds(314, 5, 28, 28);
		m_buttonPane.add(m_btnNext);

		m_btnPrevious = new JotyButton(imageIcon("prevBtn.jpg"));
		buildButton(m_btnPrevious, jotyLang("LBL_PREV") + recordEntityTail(), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkEnableAction())
					onPrevious();
			}
		});
		m_btnPrevious.setBounds(285, 5, 28, 28);
		m_buttonPane.add(m_btnPrevious);

		m_btnHome = new JotyButton(imageIcon("homeBtn.jpg"));
		buildButton(m_btnHome, jotyLang("LBL_HOME"), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkEnableAction()) {
					m_app.m_frame.setState(JFrame.NORMAL);
					m_app.setMainFrameFloating(true);
					m_app.m_frame.setVisible(true);
				}
			}
		});
		m_btnHome.setBounds(477, 5, 28, 28);
		m_buttonPane.add(m_btnHome);
	}

	@Override
	public boolean initChildren() {
		if (m_currSheet == null) {
			m_app.JotyMsg(this, "Contained data panel not yet specified!");
			return false;
		} else
			return m_currSheet.init();
	}

	@Override
	protected boolean initDialog() {
		m_app.m_accessorMode = m_accessorMode;
		boolean retVal = super.initDialog();
		m_verboseDelEntity = jotyLang("ItemToDelete");
		if (!m_isEntityNamed)
			m_app.JotyMsg(this, "must have a name !");
		Point location = m_app.m_windowsLocations.get(classIdentityName());
		if (location != null)
			Application.setLocation(this, location);
		if (retVal)
			m_app.setMainFrameFloating(true);
		return retVal;
	}

	public boolean isDeletable() {
		return true;
	}

	private boolean isModifiable() {
		return m_currSheet.isEditable();
	}

	public boolean loadData() {
		return m_currSheet.loadData();
	}

	boolean manageDeletions() {
		boolean success = m_currSheet.doDeletion();
		if (success)
			m_currSheet.deletionEffects();
		return success;
	}

	@Override
	public void onCancel() {
		if (!m_editOrNew_command) {
			m_currSheet.reloadGrid();
			return;
		}
		m_new_command = false;
		m_editOrNew_command = false;
		m_canceling = true;
		m_currSheet.statusChangeProc();
		if ((getGridManager() == null || m_currSheet.isAListDrivenSlave()) && dataToBeLoaded())
			loadData();
		endEditing(false);
		m_canceling = false;
	}

	public void onDelete() {
		if (gridManagerExists()) {
			if (getGridManager().getRowQty() == 0) {
				Application.langWarningMsg("NoDataToDelete");
				return;
			}
			if (getGridManager().getCurSel() == -1)
				return;
			getGridManager().storeSelection();
		}
		if (m_currSheet.isDeletable()) {
			String msg = String.format(jotyLang("WantToDelete"), compoundDocument() && !checkForNormalBehavior() ? m_app.m_common.jotyLang("Document") : m_verboseDelEntity);
			if (!Application.yesNoQuestion(msg))
				return;
			m_is_deleting = true;
			m_app.beginTrans();
			boolean done = checkForNormalBehavior() ? m_currSheet.doDeletion() : manageDeletions();
			try {
				if (done) {
					m_app.commitTrans();
					deletionEffects();
					if (checkForNormalBehavior())
						checkForPublishing();
					else
						checkForPublishings();
					if (getGridManager() != null)
						m_currSheet.statusChangeProc();
					if (!dataCreationEnabled() && m_currSheet.m_isNewRec)
						close();
				} else
					m_app.rollbackTrans();
			} catch (JotyException e) {}
			m_is_deleting = false;
		}
		guiDataExch(false);
	}

	public void onEditOrNew() {
		beginEditing();
		if (m_currSheet.edit()) {
			m_editOrNew_command = true;
			checkAndSetLook();
		}
	}

	@Override
	void onGridSelChange(ListSelectionEvent e, Panel panel) {
		m_gridSelChanging = true;
		if (m_currSheet != null)
			m_currSheet.statusChangeProc();
		m_gridSelChanging = false;
	}

	public void onNew() {
		beginEditing();
		m_app.setWaitCursor(true);
		checkPanelForNewRec();
		if (isModifiable() && IsCreatable()) {
			m_editOrNew_command = true;
			m_new_command = true;
			m_listenForPanelActions = true;
			m_currSheet.doNew();
			checkAndSetLook();
		}
		m_app.setWaitCursor(false);
	}

	public void onNext() {
		m_currSheet.nextRecord();
	}

	public void onPrevious() {
		m_currSheet.previousRecord();
	}

	public void onSave() {
		m_saving = true;
		if (doSave())
			saveEffects(false);
		m_saving = false;
	}

	@Override
	protected void preInitChildren() {
		super.preInitChildren();
		String mainSortInfo = m_app.m_dialogMainSortInfos.get(classIdentityName());
		if (mainSortInfo != null) {
			GridManager gridManager = getGridManager();
			if (gridManager != null)
				gridManager.getListComponent().setSortInfo(mainSortInfo);
		}
	}

	protected String query() {
		return null;
	}

	private String recordEntityTail() {
		return m_recordEntity == null ? "" : (" " + m_recordEntity);
	}

	/**
	 * To be implemented when the data managed by the implementation class also
	 * live in memory as a {@code LiteralStruct} object.
	 */
	protected void reloadAsLiteralStruct() {}

	void resetDirtyStatus() {
		m_currSheet.resetDirtyStatus();
	}

	protected boolean save() {
		boolean retVal;
		guiDataExch(true);
		retVal = m_currSheet.save();
		if (retVal)
			guiDataExch(false);
		return retVal;
	}

	private void saveEffects(boolean leaveEditingOn) {
		m_currSheet.saveEffects(leaveEditingOn);
		if (m_new_command)
			if (getGridManager() != null) {
				GridManager gridMngr = getGridManager();
				gridMngr.m_gridBuffer.addTermsAsRecord();
				int iDim = gridMngr.getMainSetSize();
				gridMngr.m_gridBuffer.m_cursorPos = iDim - 1;
				gridMngr.renderOnAppend(iDim);
				gridMngr.ensureSelectionIsVisible();

			}
		if (!leaveEditingOn)
			endEditing(true);
	}

	protected String selectiveQuery() {
		return null;
	}

	protected String selectiveUpdatableSet() {
		return null;
	}

	public void setRecordEntity(String recordEntity) {
		m_recordEntity = recordEntity;
	}

	@Override
	protected void setValidationUncritical() {
		m_editAbandoning = false;
	}

	@Override
	public boolean shouldDo() {
		return shouldDo(false);
	}

	public boolean shouldDo(boolean leaveEditingOn) {
		boolean allow = true;
		if (m_currSheet != null) {
			boolean needSaving = m_currSheet.needSaving();
			if (needSaving)
				allow = askAndSave(leaveEditingOn);
		}
		return allow;
	}

	@Override
	protected void showDirtyEffect() {
		showButton(m_btnSave, true, true);
	}

	protected String updatableSet() {
		return null;
	}

	@Override
	protected void updateCommandButtons(boolean Idle) {
		if (m_currSheet != null) {
			boolean editable = Idle && !m_currSheet.isReadOnly() && m_currSheet.m_permission.compareTo(Permission.readWrite) >= 0;
			boolean singleRecordManagement = !gridManagerExists();

			boolean docIdentified = checkForNormalBehavior() ? m_currSheet.documentIdentified() : documentIdentified();
			if (!m_isViewer) {
				boolean editOrNew_isEdit = !singleRecordManagement || docIdentified;
				m_btnEditOrNew.setToolTipText(jotyLang(editOrNew_isEdit ? "LBL_MNG" : "LBL_NEW"));
				m_btnEditOrNew.setIcon(editOrNew_isEdit ? imageIcon("mngBtn.jpg") : imageIcon("newBtn.jpg"));
			}
			if (singleRecordManagement) {
				showButton(m_btnNext, false);
				showButton(m_btnPrevious, false);
			} else {
				NavigatorBuffer gridBuffer = getGridManager().m_gridBuffer;
				boolean queueing = gridBuffer.m_queueManager != null;
				if (gridPos() > -1)
					docIdentified = true;
				showButton(m_btnNext, !queueing && Idle && docIdentified, gridPos() < gridBuffer.m_records.size() - 1);
				showButton(m_btnPrevious, !queueing && Idle && docIdentified, gridPos() > 0);
			}

			boolean deletable = Idle && getDelete_EnablingState() && docIdentified;
			showButton(m_btnDelete, deletable, deletable);

			showButton(m_btnEditOrNew, !m_isViewer && editable && (docIdentified || !gridManagerExists() && dataCreationEnabled()), editable);
			boolean btnNewVisible = !m_isViewer && Idle && getAdd_EnablingState() && dataCreationEnabled();
			showButton(m_btnNew, btnNewVisible, btnNewVisible);
			showButton(m_btnCancel, !Idle, !Idle);
			showButton(m_btnSave, !m_isViewer && !Idle, !Idle && m_currSheet.needSaving());
			showButton(m_btnClose, Idle, Idle);
			m_currSheet.updateCommandButtons(Idle);
		}
		m_btnHome.setVisible(!m_app.m_frame.isAlwaysOnTop() || m_app.m_frame.getExtendedState() == JFrame.ICONIFIED);
	}

}
