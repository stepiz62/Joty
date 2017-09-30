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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.joty.access.Logger;
import org.joty.app.Common;
import org.joty.common.ApplMessenger;
import org.joty.common.BasicPostStatement;
import org.joty.workstation.app.Application;
import org.joty.workstation.gui.DataAccessPanel.Permission;
import org.joty.workstation.gui.Term.TermEnclosable;

/**
 * It is a visual class, descending from JPanel, that manages the storing and
 * the retrieving of a binary object into and from the database, through the web
 * or directly.
 * <p>
 * Actually the retrieving is even performed as a multiple object operation,
 * that is performed on several record of the binary database field, by means of
 * the {@code ActionIterationActor} delegated internal instance.
 * <p>
 * The class defines a set of four small buttons each of them having the
 * following task acting on the binary object: opening, uploading, deleting,
 * downloading.
 * <p>
 * Differently from all other Joty visual objects managing the datum living in a
 * database field, a BlobComponent instance is enabled acting when the
 * containing {@code DataAccessDialog} instance id not in the 'editing' state.
 * This choice has been made to keep things simple and robust from the point of
 * view of the software factory, with a little loss of comfort for the user that
 * has to familiarize with this special object that does not participate in the
 * editing session of the dialog.
 * <p>
 * 
 * @see BlobComponent.ActionIterationActor
 */
public class BlobComponent extends JPanel implements TermEnclosable {
	/** 
	 * This class is run by an added thread instantiated to offer multi-threading behavior to the {@code InfoDialog} instance launched 
	 */
	
	class ActionIterationActor implements Runnable {
		protected OptionedAction m_action;

		ActionIterationActor(OptionedAction action) {
			m_action = action;
		}

		@Override
		public void run() {
			synchronized (this) {
				try {
					while (!m_infoDialogOpened)
						wait();
				} catch (InterruptedException e1) {}
			}
			GridManager gridManager = m_panel.getGridManager();
			int oldSelectedRow = gridManager.getCurSel();
			m_panel.getDialog().m_frozen = true;
			m_iterating = true;
			try {
				for (int i = 0; i < gridManager.getRowQty(); i++) {
					gridManager.setCurSel(i);
					actionFromThisRecord(m_action);
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {} finally {
				m_app.closeInfoDialog();
				m_panel.getDialog().m_frozen = false;
				m_panel.getDialog().doActivationChange(true);
				gridManager.setCurSel(oldSelectedRow);
				gridManager.ensureSelectionIsVisible();
				m_iterating = false;
				finalizeIteration(m_action);
			}
		}
	};

	interface Actuator {
		void act();
	}

	/**
	 * Holds the type of action processed.
	 * <p>
	 * Differently from the 'download' operation, for the 'open' operation
	 * there is an interest to provide a dedicated command that
	 * unconditionally (without asking the user) triggers an iteration on the set of the underlying
	 * records: 'iteratingOpen' derives from this.  
	 */
	public enum OptionedAction {
		open, download, iteratingOpen
	}

	public DataAccessPanel m_panel;
	public Term m_term;
	protected byte[] m_bytes;
	private boolean m_isNull;
	protected FileInputStream m_is;
	public JotyButton m_btnOpen;
	public JotyButton m_btnUpload;
	public JotyButton m_btnDelete;
	public JotyButton m_btnDownload;
	private String m_openToolTip;
	private String m_uploadToolTip;
	private String m_deleteToolTip;
	private String m_downloadToolTip;
	String m_fileExt;
	String m_verboseFileType;
	Color m_defColor = getBackground();
	protected boolean m_success;
	protected String m_editPreconditionText;
	private File m_file;
	protected Application m_app = Application.m_app;
	private JotyButton m_actorButton;
	private boolean m_iterating;
	private String m_targetDir;
	private String m_openDlgCurrentDir;
	private boolean m_preDialogTitle;

	private boolean m_infoDialogOpened;
	protected Common m_common;

	public BlobComponent(TermContainerPanel panel, Term term) {
		m_common = (Common) ((ApplMessenger) m_app).getCommon();
		if (!(panel instanceof DataAccessPanel)) {
			if (m_app != null)
				m_app.JotyMsg(this, "Wrong container type !");
			return;
		}
		m_panel = (DataAccessPanel) panel;
		m_term = term;
		setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		setLayout(null);

		m_openToolTip = m_common.jotyLang("LBL_OPEN");
		m_uploadToolTip = m_common.jotyLang("LBL_UPLD");
		m_deleteToolTip = m_common.jotyLang("LBL_DEL");
		m_downloadToolTip = m_common.jotyLang("LBL_DNLD");

		m_btnOpen = addButton("openBtn.jpg", m_openToolTip, new Actuator() {
			@Override
			public void act() {
				open(false);
			}
		});
		m_btnOpen.setBounds(3, 3, 20, 19);

		m_btnUpload = addButton("uploadBtn.jpg", m_uploadToolTip, new Actuator() {
			@Override
			public void act() {
				uploadProlog();
				if (prepareUpload())
					doUpload();
				uploadEpilog();
			}
		});
		m_btnUpload.setBounds(23, 3, 20, 19);

		m_btnDelete = addButton("delBlobBtn.jpg", m_deleteToolTip, new Actuator() {
			@Override
			public void act() {
				deleteObject();
			}
		});
		m_btnDelete.setBounds(43, 3, 20, 19);

		m_btnDownload = addButton("downloadBtn.jpg", m_downloadToolTip, new Actuator() {
			@Override
			public void act() {
				download();
			}
		});
		m_btnDownload.setBounds(63, 3, 20, 19);

		m_editPreconditionText = m_common.jotyLang("BinaryEditNeedsRec");
		setBounds(0, 0, 85, 24);
		m_iterating = false;
	}

	private void actionFromThisRecord(OptionedAction action) {
		switch (action) {
			case open:
			case iteratingOpen:
				openFromThisRecord();
				break;
			case download:
				downloadFromThisRecord();
				break;
		}
	}

	private JotyButton addButton(String iconFile, String toolTiptext, final Actuator actuator) {
		final JotyButton btn = new JotyButton(imageIcon(iconFile));
		btn.setFocusable(false);
		btn.setToolTipText(toolTiptext);
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setActorButton(btn);
				actuator.act();
			}
		});
		add(btn);
		return btn;
	}

	private void basicSetButtonEnable(JotyButton button, boolean truth, String ToolTipText, boolean considerPrecondition) {
		button.setEnabled(truth);
		button.setToolTipText(truth || !considerPrecondition ? ToolTipText : m_editPreconditionText);
	}

	/**
	 * A file chooser that remember the location of the last choice.
	 * @param msgLangItem
	 * @param onlyDirectory
	 * @return the File object corresponding to the selection made or null if no selection has been made
	 */
	private File buildFileChooser(String msgLangItem, boolean onlyDirectory) {
		if (m_openDlgCurrentDir == null)
			m_openDlgCurrentDir = m_app.m_openDlgCurrentDir;
		if (m_openDlgCurrentDir == null)
			m_openDlgCurrentDir = System.getProperty("user.home");
		m_app.beginWaitCursor();
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileNameExtensionFilter(m_verboseFileType, m_fileExt));
		String verboseText = m_common.jotyLang(msgLangItem);
		fc.setDialogTitle(verboseText);
		fc.setApproveButtonText("Ok");
		fc.setApproveButtonToolTipText(verboseText);
		if (m_openDlgCurrentDir != null)
			fc.setCurrentDirectory(new java.io.File(m_openDlgCurrentDir));
		if (onlyDirectory)
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		m_app.endWaitCursor();
		File retFile = null;
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			retFile = fc.getSelectedFile();
			if (retFile != null) {
				m_openDlgCurrentDir = retFile.getAbsolutePath();
				m_app.m_openDlgCurrentDir = m_openDlgCurrentDir;
			}
		}
		return retFile;
	}

	protected boolean checkEmpty() {
		return getAndCheckEmpty();
	}

	protected boolean checkForOptions(OptionedAction action) {
		return action == OptionedAction.download;
	}

	public void clear() {}

	protected String contentIdentityTheme() {
		return ((m_preDialogTitle ? (m_panel.getDialog().getTitle() + "_") : "") + m_term.m_name + "_" + m_term.renderedIdentity(null)).replace("/", "-").replace(":", "_");
	}

	public void deleteObject() {
		if (!checkEmpty())
			if (Application.langYesNoQuestion("WantDelBlob")) {
				m_isNull = true;
				setBytesToNull();
				clear();
				uploadProlog();
				doUpload();
				uploadEpilog();
			}
		render();
	}

	protected void doUpload() {
		executeUploadStatement(m_bytes, m_term.m_dbFieldName, false);
		successFeedBack();
	}

	private void download() {
		m_file = buildFileChooser("SelTargetDir", true);
		if (m_file != null) {
			m_preDialogTitle = Application.yesNoQuestion(m_common.jotyLang("DnldPreDialogTitle"));
			m_targetDir = m_file.getAbsolutePath();
			optionsForAction(OptionedAction.download);
		}
	}

	private void downloadDocument() {
		Application.m_common.saveBytesAsFile(m_bytes, m_targetDir, contentIdentityTheme() + "." + m_fileExt, false);
	}

	private void downloadFromThisRecord() {
		if (!getAndCheckEmpty())
			downloadDocument();
	}

	protected void executeUploadStatement(byte[] bytes, String fieldName, boolean auxiliary) {
		m_app.beginWaitCursor();
		BasicPostStatement postStatement = m_term.m_panel.createContextPostStatement();
		String sql = String.format("UPDATE %1$s SET %2$s = %3$s WHERE %4$s", 
									m_app.codedTabName(m_panel.m_mainDataTable), fieldName, m_isNull ? "NULL" : "?", getWhereClause());
		m_success = m_isNull ? 
						m_app.executeSQL(sql, null, postStatement) : 
						m_app.m_webMode ? 
								m_app.m_webClient.updateBinary(sql, bytes, auxiliary, postStatement) : 
								m_app.m_db.executeBytesStmnt(sql, bytes, postStatement);
		m_app.endWaitCursor();
	}

	protected void finalizeIteration(OptionedAction action) {}

	public JotyButton getActorButton() {
		return m_actorButton;
	}

	protected boolean getAndCheckEmpty() {
		getBytes();
		boolean retVal = isBytesEmpty();
		if (retVal && !m_iterating && (!m_app.m_webMode || m_app.m_webClient.m_responseSuccess))
			Application.langWarningMsg("EmptyBlob");
		return retVal;
	}

	public void getBytes() {
		String tabName = m_app.codedTabName(m_panel.m_mainDataTable);
		if (tabName == null)
			m_app.JotyMsg(this, "Joty requires, for imaging, that updatable set is specified even if no update is planned !");
		m_bytes = m_app.m_db.getBytesFromDb(String.format("SELECT %1$s FROM %2$s WHERE %3$s", 
															m_term.m_dbFieldName, tabName, getWhereClause()), 
											m_term.m_panel.createContextPostStatement(m_term.m_name));
	}

	protected void getData() throws IOException {
		int dataLength = (int) m_file.length();
		m_bytes = new byte[dataLength];
		int byteRead = 0;
		int totalBytesRead = 0;
		while (totalBytesRead < dataLength) {
			byteRead = m_is.read(m_bytes, totalBytesRead, dataLength);
			totalBytesRead += byteRead;
		}
	}

	@Override
	public boolean getRelatedEnable() {
		return true;
	}

	@Override
	public Term getTerm() {
		return m_term;
	}

	String getWhereClause() {
		m_panel.m_blobManaging = true;
		String retVal = m_panel.getWhereClause();
		m_panel.m_blobManaging = false;
		return retVal;
	}

	private Icon imageIcon(String fileName) {
		return m_app.imageIcon(fileName);
	}

	public boolean isBytesEmpty() {
		return m_bytes == null || m_bytes.length == 0;
	}

	
	/**
	 * This method instantiates an added thread that is made running an
	 * {@code ActionIterationActor} instance for iterating the action specified
	 * by {@code action}, over the entire set of records, by providing the user
	 * with a multi-threading behavior: the {@code InfoDialog} instance will
	 * allow the user to interrupt the iteration.
	 * 
	 * @param action a value of the {@code OptionedAction} enumeration
	 * 
	 * @see ActionIterationActor
	 * @see Application#openInfoDialog(String, Thread, boolean)
	 * @see OptionedAction
	 */ 
	private void iterate(OptionedAction action) {
		m_app.setMainFrameFloating(true);
		ActionIterationActor actionIteratorActor = new ActionIterationActor(action);
		Thread thread = new Thread(actionIteratorActor);
		m_infoDialogOpened = false;
		thread.start();
		m_app.openInfoDialog(String.format(m_common.jotyLang("ActionIsRunning"), getActorButton().getToolTipText()), thread, true);
		m_infoDialogOpened = true;
		synchronized (actionIteratorActor) {
			actionIteratorActor.notify();
		}
	}

	public void open(boolean mandatoryCurrent) {
		m_app.beginWaitCursor();
		open(mandatoryCurrent, false);
		m_app.endWaitCursor();
	}

	public void open(boolean mandatoryCurrent, boolean mandatoryIterating) {
		if (mandatoryCurrent)
			openFromThisRecord();
		else
			optionsForAction(mandatoryIterating ? OptionedAction.iteratingOpen : OptionedAction.open);
	}

	protected void openDocument() {
		Application.openDocumentFromBytes(m_bytes, m_fileExt);
	}

	private void openFromThisRecord() {
		if (!getAndCheckEmpty())
			openDocument();
	}

	private void optionsForAction(OptionedAction action) {
		if (m_panel.getGridManager() != null && 
				checkForOptions(action) && 
				(action == OptionedAction.iteratingOpen || Application.yesNoQuestion(String.format(m_common.jotyLang("ExtendedToAll"), getActorButton().getToolTipText())))) {
			iterate(action);
		} else
			actionFromThisRecord(action);
	}

	protected Boolean prepareUpload() {
		m_bytes = null;
		m_success = false;
		m_file = buildFileChooser("SelFileToUpload", false);
		if (m_file != null) {
			try {
				m_is = new FileInputStream(m_file);
				getData();
				m_is.close();
				m_success = true;
			} catch (FileNotFoundException e) {
				Logger.exceptionToHostLog(e);
			} catch (IOException e) {
				Logger.exceptionToHostLog(e);
			}
			m_isNull = isBytesEmpty();
			m_term.setToNull(m_isNull);
		}
		return m_success;
	}

	public void render() {}

	public void setActorButton(JotyButton actorButton) {
		m_actorButton = actorButton;
	}

	protected void setButtonEnable(JotyButton button, boolean truth, String ToolTipText, boolean considerPrecondition) {
		basicSetButtonEnable(button, truth, ToolTipText, considerPrecondition);
	}

	protected void setBytesToNull() {
		m_bytes = null;
	}

	@Override
	public void setEnabled(boolean truth) {
		setButtonEnable(m_btnOpen, truth && m_panel.m_permission != Permission.no_access, m_openToolTip, true);
		setButtonEnable(m_btnDelete, truth && m_panel.m_permission.compareTo(Permission.readWrite) >= 0, m_deleteToolTip, true);
		basicSetButtonEnable(m_btnUpload, truth && m_panel.m_permission.compareTo(Permission.readWrite) >= 0, m_uploadToolTip, true);
		setButtonEnable(m_btnDownload, truth && m_panel.m_permission != Permission.no_access, m_downloadToolTip, true);
		super.setEnabled(truth);
	}

	protected void successFeedBack() {
		if (m_success)
			Application.langInformationMsg("ActionSuccess");
	}

	protected void uploadEpilog() {}

	protected void uploadProlog() {}

}
