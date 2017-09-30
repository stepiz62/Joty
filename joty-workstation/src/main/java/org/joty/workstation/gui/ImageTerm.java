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

import org.joty.common.JotyTypes;
import org.joty.workstation.gui.TermContainerPanel.TermParams;

/**
 * Extends the BlobTerm class in order to host an {@code ImageComponent} object.
 * <p>
 * The light part of data of the component, the preview, participates in the
 * presentation process made by the DataAccessPanel. This is reason for the
 * {@code guiDataExch} method returning to take a role, at least in the 'out'
 * direction.
 * <p>
 * This class informs the framework that the light part of the component is the
 * only one that participates in collective data management (see
 * {@code resultSetDataType}, switching the behavior of the ancestor that lets
 * the one only part, the heavy one, implicitly be away from collective
 * management.
 * 
 * @see ImageComponent
 * @see DataAccessPanel
 * 
 */
public class ImageTerm extends BlobTerm {

	public String m_previewDbField;
	public boolean m_previewBuffered;

	public ImageTerm(TermContainerPanel panel, int dataType, TermParams params) {
		super(panel, dataType, params);
		m_previewBuffered = true;
	}

	@Override
	void clearNonStructuredCtrl() {
		m_blobComponent.clear();
	}

	@Override
	protected void createComponent(TermContainerPanel panel) {
		m_blobComponent = new ImageComponent(panel, this);
	}

	@Override
	public void guiDataExch(boolean in) {
		super.guiDataExch(in);
		if (!in)
			termRender();
	}

	public void openAllAsTiles() {
		((ImageComponent) m_blobComponent).openAllAsTiles();
	}

	@Override
	public int resultSetDataType() {
		return JotyTypes._smallBlob;
	}

	@Override
	public String resultSetFieldName() {
		return m_previewDbField;
	}

	public void setActorButton(JotyButton actorButton) {
		m_blobComponent.setActorButton(actorButton);
	}

	/**
	 * This method makes the loading of data performed by a
	 * {@code DataScrollingPanel} instance not involving the small blob of the
	 * preview, allowing a faster process. However, in this case, the data base
	 * will be accessed (in web mode via http) to load the preview at any change
	 * of the selection in the DataScrollingPanel object.
	 * <p>
	 * For this method to be invoked, the term instance needs not to be added to
	 * the grid.
	 * 
	 * @see DataScrollingPanel
	 * @see GridManager
	 * @see TermContainerPanel#addTermToGrid
	 */
	public void setPreviewUnbuffered() { 
		m_previewBuffered = false;
	}

	@Override
	public void termRender() {
		m_blobComponent.render();
	}

}
