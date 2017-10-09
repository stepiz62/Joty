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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.joty.access.Logger;
import org.joty.workstation.app.Application;

/**
 * This class is instantiated by the {@code ImageComponent} class and has the
 * responsibility to open, close, relocate the various instances of the
 * {@code ViewerFrame} class, that take care of painting the image in the mapped
 * data base field, each of them painting different record content.
 * <p>
 * The main simple task of the manager is to open a ViewerFrame instance for
 * displaying the image contained in the ('large') blob field managed by the
 * ImageComponent, for the current (currently selected) record in the
 * DataAccessPanel.
 * <p>
 * When the ImageComponent object is hosted in a DataScrollingPanel instance,
 * say X the database field managed by the ImageComponent, the class has also
 * the ability to open as many ViewerFrame objects as the rows are in the
 * managed data grid, each showing the image in X that occurs at a different
 * record.
 * <p>
 * It is also responsible to populate the 'window' menu with the titles of the
 * added windows (the viewers') synchronizing its content with the actual life
 * of them.
 * <p>
 * Furthermore it can dispose various Viewers as tiles (up to 16) or as in a
 * stack, and, even, collectively close them.
 * <p>
 * Its management methods are (also) invoked by the ViewerFrame instances
 * themselves, like in a scenario in which the coordinated resource asks to be
 * coordinated. Actually this derived from the needs to make available the
 * management ability from the inside of the viewer currently active.
 * 
 * 
 * @see ImageComponent
 * @see ViewerFrame
 * 
 */
public class ViewersManager {
	public Map<Long, ViewerFrame> m_viewersMap;
	BufferedImage m_img = null;
	private Vector<ViewerFrame> m_viewers;
	private Map<Long, WindowsMenuItem> m_windowsMenuItemsMap;
	int m_screenPartsTemplate[] = { 1, 2, 4, 6, 9, 12, 16 };
	int m_widthDivisorTemplate[] = { 1, 2, 2, 3, 3, 4, 4 };
	int m_screenPartsTemplateIndex;
	boolean m_viewersRestoring;
	boolean m_componentDirector;
	Term m_term;
	private DataAccessDialog m_dataDialog = null;
	String m_GridTermField;

	public ViewersManager(Term term) {
		m_term = term;
		m_viewersMap = new HashMap<Long, ViewerFrame>();
		m_viewers = new Vector<ViewerFrame>();
		m_windowsMenuItemsMap = new HashMap<Long, WindowsMenuItem>();
		m_componentDirector = m_term instanceof ImageTerm;
	}

	public ViewersManager(Term term, String fieldName) {
		this(term);
		m_GridTermField = fieldName;
	}

	protected DataAccessDialog dataDialog() {
		if (m_dataDialog == null)
			m_dataDialog = ((DataAccessDialog) m_term.m_panel.getDialog());
		return m_dataDialog;
	}

	public void doCloseViewer(boolean removeComponent, long viewerIdentity, boolean removeViewerFromMap) {
		ViewerFrame viewerFrame = m_viewersMap.get(viewerIdentity);
		if (removeViewerFromMap && viewerFrame != null)
			m_viewersMap.remove(viewerIdentity);
		m_viewers.remove(viewerFrame);
		if (removeComponent) {
			if (m_viewersMap.size() == 0)
				dataDialog().m_viewersManagers.remove(this);
		} else if (viewerFrame != null)
			viewerFrame.dispose();
		m_img = null;
		WindowsMenuItem wMenuItem = m_windowsMenuItemsMap.get(viewerIdentity);
		if (wMenuItem != null) {
			m_windowsMenuItemsMap.remove(viewerIdentity);
			wMenuItem.remove();
		}
	}

	public void doCloseViewers(long viewerIndentity, boolean removeViewerFromMap) {
		if (viewerIndentity > 0)
			doCloseViewer(true, viewerIndentity, removeViewerFromMap);
		else {
			for (Iterator it = m_viewersMap.entrySet().iterator(); it.hasNext();) {
				doCloseViewer(false, ((Entry<Long, ViewerFrame>) it.next()).getKey(), false);
				it.remove();
			}
		}
	}

	private int getScreePartQty(int viewersQty) {
		m_screenPartsTemplateIndex++;
		int templateValue = m_screenPartsTemplate[m_screenPartsTemplateIndex];
		return viewersQty > templateValue && m_screenPartsTemplateIndex < m_screenPartsTemplate.length - 1 ? getScreePartQty(viewersQty) : templateValue;
	}

	protected void openDocument(byte[] bytes) {
		long viewerIdentity = viewerIdentity();
		doCloseViewers(viewerIdentity, false);
		try {
			m_img = ImageIO.read(new ByteArrayInputStream(bytes));
		} catch (IOException e) {
			Logger.exceptionToHostLog(e);
		}
		ViewerFrame viewerFrame = m_viewersMap.get(viewerIdentity);
		if (viewerFrame == null) {
			viewerFrame = new ViewerFrame(this, viewerIdentity);
			m_viewersMap.put(viewerIdentity, viewerFrame);
			m_viewers.add(viewerFrame);
		}
		viewerFrame.showViewer(m_img);
		String viewerTitle = viewerTitle();
		viewerFrame.setTitle(viewerTitle);

		if (m_viewersMap.size() == 1)
			dataDialog().m_viewersManagers.add(this);
		m_windowsMenuItemsMap.put(viewerIdentity, new WindowsMenuItem(viewerTitle, viewerFrame));
	}

	public void stackViewers() {
		m_viewersRestoring = true;
		for (ViewerFrame viewerFrame : m_viewers)
			viewerFrame.setToDefaultLocation();
		m_viewersRestoring = false;
		for (ViewerFrame viewerFrame : m_viewers)
			viewerFrame.reset();
	}

	public void tileViewers() {
		int viewersQty = m_viewers.size();
		m_screenPartsTemplateIndex = -1;
		int screenPartsQty = getScreePartQty(viewersQty);
		int widthDivisor = m_widthDivisorTemplate[m_screenPartsTemplateIndex];
		int heightDivisor = screenPartsQty / widthDivisor;
		int partWidth = Application.m_app.m_screenSize.width / widthDivisor;
		int partHeight = Application.m_app.m_screenSize.height / heightDivisor;
		int viewerIndex = 0;
		Application.m_app.setMainFrameFloating(true);
		for (ViewerFrame viewerFrame : m_viewers) {
			viewerFrame.setBounds((viewerIndex % widthDivisor) * partWidth, 
									(viewerIndex / widthDivisor) * partHeight, 
									partWidth, partHeight);
			viewerFrame.reset();
			viewerIndex++;
		}
	}

	public long viewerIdentity() {
		return m_componentDirector ? 
				((m_term.m_panel.m_gridManager != null) ? m_term.m_panel.m_gridManager.getCurrId() : 1) : 
				((GridTerm) m_term).m_dataBuffer.getKeyLongVal();
	}

	protected String viewerTitle() {
		return m_term.renderedIdentity(m_GridTermField);
	}

}
