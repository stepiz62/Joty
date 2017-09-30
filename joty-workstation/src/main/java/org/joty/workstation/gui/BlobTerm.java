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

import javax.swing.JComponent;

import org.joty.workstation.app.Application;
import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * It is a wrapper for a {@code BlobComponent}.
 * <p>
 * It makes the {@code guiDataExch} method having no effect because in the Joty
 * framework blob data are managed in a dedicated way and don't participate in
 * the editing session of the DataAccessDialog.
 * 
 * @see BlobComponent
 * @see DataAccessDialog
 */
public class BlobTerm extends Term {

	BlobComponent m_blobComponent;

	public BlobTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
	}

	@Override
	protected void clearComponent() {
		m_blobComponent.clear();
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		m_blobComponent = new BlobComponent(panel, this);
	}

	@Override
	public void enableComponent(boolean enablCtx, boolean bState, boolean docIdentified, boolean basicallyEditable, boolean editability) {
		m_blobComponent.setEnabled(!bState && docIdentified);
	}

	@Override
	public JComponent getComponent() {
		return m_blobComponent;
	}

	@Override
	public void guiDataExch(boolean in) {}

	@Override
	void init() {
		super.init();
		if (m_mandatory && isWindowVisible())
			m_app.JotyMsg(this, "The attribute ''mandatory'' for BlobTerm (and descendants) instances is not currently supported !");
	}

	@Override
	public boolean isWindowEnabled() {
		return m_blobComponent.isEnabled();
	}

	@Override
	public boolean isWindowVisible() {
		return m_blobComponent.isVisible();
	}

	@Override
	public void show(boolean truth) {
		m_blobComponent.setVisible(truth);
	}

}
