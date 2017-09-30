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
import java.util.HashSet;

import javax.swing.JComponent;

import org.joty.common.Utilities;
import org.joty.common.Utilities.Stocker;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.gui.TermContainerPanel.BuildDetailsDialogAdapter;
import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * It is the more 'verbose' GridTerm implementation because relies on the
 * powerful {@code Table} component. Here a more specialized flavor of buffer is
 * used also : a {@code GridBuffer} instance. This gets information to build the
 * record structure from the Joty declarative statements used for adding Term
 * objects to a DataAccessPanel or for explicitly adding fields to the TableTerm
 * component, then some sort of intersection with the actual availability of the
 * database fields is performed on the query set definition.
 * <p>
 * The class participates in the publishers-subscribers model implementation for
 * the updating propagation of the collected information.
 * <p>
 * The double click on the row of the component is here made easy to implement
 * thanks to the {@code rowAction} method implementation that takes a
 * {@code BuildDetailsDialogAdapter} instance as argument: it is enough to
 * assign the implementation of the BuildDetailsDialogAdapter to the public
 * {@code m_buildDetailsHandler} member variable.
 * <p>
 * Another features of the rowAction method is to be invoked by
 * {@code DataAccessPanel.openDetail} for finalizing a
 * BuildDetailsDialogAdapter implementation there defined.
 * 
 * 
 * @see GridBuffer
 * @see Table
 * @see Factory
 * @see TermContainerPanel.BuildDetailsDialogAdapter
 * @see org.joty.workstation.gui.DataAccessPanel#openDetail(TableTerm, TermContainerPanel.BuildDetailsDialogAdapter)
 * @see TableTerm#rowAction(TermContainerPanel.BuildDetailsDialogAdapter)
 * 
 */

public class TableTerm extends GridTerm {

	public BuildDetailsDialogAdapter m_buildDetailsHandler;
	public long m_detailsKey;
	public JotyDialog m_detailsDialog;
	public boolean m_reloadOnDetailsDialogChange;
	public Stocker m_smallBlobs;
	Table m_table;
	HashSet<String> m_publishersSet;
	private boolean m_reloadBecauseOfPublishing;
	private boolean m_isPublisher;

	public TableTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
		if (!m_slave && !Beans.isDesignTime())
			m_dataBuffer = new TermBuffer(this, m_targetDatumField);
		m_reloadOnDetailsDialogChange = true;
		m_smallBlobs = Utilities.m_me.new Stocker();
		m_publishersSet = new HashSet<String>();
	}

	public void addFieldAsImage(String fieldName, String header) {
		super.addField(fieldName, header);
		fieldDescr(fieldName).m_viewersManager = new ViewersManager(this, fieldName);
		m_smallBlobs.add(fieldName);
	}

	@Override
	public void bufferRender() {
		m_table.newDataAvailable();
	}

	@Override
	protected void checkForPublishing() {
		if (m_isPublisher)
			((DataAccessPanel) m_panel).publishThisDialog();
	}

	@Override
	public void checkPublishers() {
		if (m_reloadBecauseOfPublishing) {
			loadData();
			checkSelection();
			updateAspectOnDataChange();
			m_reloadBecauseOfPublishing = false;
		}
	}

	public void updateAspectOnDataChange() {
		boolean oldReloadNeeded = m_reloadNeeded;
		boolean oldUpdatingActor = m_updatingActor;
		m_reloadNeeded = true;
		m_updatingActor = false;
		updateAspect();
		m_reloadNeeded = oldReloadNeeded;
		m_updatingActor = oldUpdatingActor;
		checkLinkedAspectUpdater();
		checkForPublishing();
	}

	@Override
	public void clear() {
		clearComponent();
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		m_table = new Table(panel, this);
	}

	@Override
	protected void doLoadData() {
		super.doLoadData();
		m_table.newDataAvailable();
	}

	public void fitHeightToPreview() {
		m_table.setCustomRowHeight(ImageComponent.previewHeight);
	}

	@Override
	public JComponent getComponent() {
		return m_table;
	}

	@Override
	public int getRowQty() {
		return m_table.getRowQty();
	}

	@Override
	public ScrollGridPane getScrollPane() {
		return m_table;
	}

	@Override
	public int getSelection() {
		return m_table.getSelection();
	}

	@Override
	public void innerLoad() {
		if (getComponent().isVisible())
			loadData();
	}

	@Override
	public boolean isWindowEnabled() {
		return m_table.isEnabled();
	}

	@Override
	public boolean isWindowVisible() {
		return m_table.isVisible();
	}

	@Override
	public void manageDoubleClick(MouseEvent e) {
		if (isWindowEnabled()) {
			if (m_actionOnRowHandler == null)
				rowAction();
			else
				super.manageDoubleClick(e);
		}
	}

	@Override
	public void notifyPublishing(String publisherDialogClassName) {
		if (m_publishersSet.contains(publisherDialogClassName))
			m_reloadBecauseOfPublishing = true;
	}

	public void openDetailsDialog() {
		if (m_buildDetailsHandler == null)
			m_app.JotyMsg(m_panel, "The term " + m_name + " has not m_buildDetailsHandler member assigned yet !");
		else {
			JotyDialog dlg = m_buildDetailsHandler.createDialog(this);
			if (dlg != null)
				dlg.perform();
		}
	}

	public void openDetailsDialog(JotyDialog dlg) {
		if (dlg instanceof DataAccessDialog) {
			GridManager gridManager = dlg.getGridManager();
			if (gridManager == null)
				;
			else
				gridManager.setSelectionOnKeyVal(m_detailsKey);
		}
		dlg.perform();
	}

	@Override
	protected void prepareRs(WResultSet rs) {
		rs.setSmallBlobsList(m_smallBlobs);
	}

	public boolean reloadOnActivate() {
		return m_reloadBecauseOfPublishing;
	}

	@Override
	protected void removeAll() {
		m_table.removeAll();
	}

	@Override
	public String renderedIdentity(String fieldSpecified) {
		IdentityRenderer identityRenderer = fieldDescr(fieldSpecified).m_identityRenderer;
		return identityRenderer == null ? null : identityRenderer.render();
	}

	public void rowAction() {
		rowAction(null);
	}

	/**
	 * Allows to execute an internal or external implementation of the
	 * BuildDetailsDialogAdapter class and tracks links for backward updating
	 * propagations of possibly shared data between the caller and the target
	 * dialog and for possible control on the target dialog; then if the target
	 * data context has navigation features, finalizes its effect to select the
	 * record identified by {@code m_detailsKey} member value.
	 * 
	 * @see BuildDetailsDialogAdapter
	 */
	public void rowAction(BuildDetailsDialogAdapter buildDetailsHandler) {
		BuildDetailsDialogAdapter detailsHandler = buildDetailsHandler == null ? m_buildDetailsHandler : buildDetailsHandler;
		m_detailsKey = m_dataBuffer.getKeyLongVal();
		if (detailsHandler != null) {
			m_detailsDialog = detailsHandler.createDialog(this);
			if (m_detailsDialog != null) {
				DataAccessPanel panel = (DataAccessPanel) m_panel;
				if (detailsHandler.identifierFromCaller() == null)
					panel.m_dependentDialogs.add(m_detailsDialog);
				m_detailsDialog.m_parentDataPanel = panel;
				m_detailsDialog.addIdentifierFromCallerToTitle(detailsHandler.identifierFromCaller());
				if (m_detailsDialog != null) {
					if (m_detailsDialog instanceof DataAccessDialog) {
						if (m_reloadOnDetailsDialogChange) {
							subscribe(m_detailsDialog.getClass().getName());
							m_detailsDialog.m_currSheet.setAsPublisher();
						}
						if (m_detailsDialog.getGridManager() != null)
							m_detailsDialog.m_currSheet.setControllerOnKey(m_detailsKey);
					}
					m_detailsDialog.perform();
				}
			}
		}
	}

	@Override
	public void selectLastRow() {
		if (m_table.getRowQty() > 0) {
			int targetSel = m_table.getRowQty() - 1;
			m_table.setSelection(targetSel);
		}
	}

	public void setAsPublisher() {
		m_isPublisher = true;
	}

	@Override
	public void setEnabledAsDetail() {
		m_app.addToolTipRowToComponent(m_table.m_jtable, m_app.m_common.jotyLang("EditAddDetails"));
		super.setEnabledAsDetail();
	}

	@Override
	public int setSelection(long val, boolean basedOnData) {
		long selPos = basedOnData ? m_dataBuffer.getKeyPos(val) : val;
		m_table.setSelection(selPos);
		return super.setSelection(selPos, basedOnData);
	}

	public void setToolTipText(String text) {
		m_table.m_jtable.setToolTipText(text);
	}

	@Override
	public void show(boolean truth) {
		m_table.setVisible(truth);
	}

	public void subscribe(String publisherDialogName) {
		if (!Beans.isDesignTime())
			m_publishersSet.add(((DataAccessPanel) m_panel).enrollThisDialog(publisherDialogName));
	}

}
