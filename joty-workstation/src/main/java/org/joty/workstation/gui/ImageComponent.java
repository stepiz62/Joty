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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.border.LineBorder;

import org.joty.access.Logger;
import org.joty.app.JotyException;
import org.joty.workstation.app.Application;

/**
 * Extends the {@code BlobComponent} for dealing with an image as binary object.
 * <p>
 * It adds an auxiliary 'small' binary object to the management, the preview of
 * the image. Always present if so is the main binary content, it requires that
 * one more database filed name is specified in the creation of the component.
 * the preview is always present when so is the actual image, because it is
 * created and stored by the framework contextually to the acquisition of the
 * main content. This small binary object (its size is limited and predefined by
 * the framework) is the only one that the framework qualifies having a
 * {@code JotyTypes._smallBlob} type.
 * <p>
 * The class instantiates and manages an {@code ImagePane} object that serves
 * mainly as preview viewer.
 * <p>
 * Furthermore, for the visualization of the actual image, it uses a built in
 * viewer: an instance of the {@code ViewerFrame}, but through the management of
 * a ViewersManager instance delegated to administer the life of various
 * ViewerFrame objects corresponding to different records of the image
 * controlled my the ImageComponent instance.
 * 
 * @see ImagePane
 * @see BlobComponent
 * @see ViewerFrame
 * @see ViewersManager
 * 
 */
public class ImageComponent extends BlobComponent {

	BufferedImage m_img = null;
	BufferedImage m_previewImg = null;
	public ImagePane m_previewPanel;
	public static int previewWidth = 80;
	public static int previewHeight = 60;
	private boolean m_openingAsTiles;
	Application m_app = Application.m_app;

	public ImageComponent(TermContainerPanel panel, Term term) {
		super(panel, term);
		m_previewPanel = new ImagePane();
		m_previewPanel.setImageComponent(this);
		m_previewPanel.setBorder(new LineBorder(null));
		m_previewPanel.setSize(previewWidth, previewHeight);
		add(m_previewPanel);

		m_previewPanel.setLocation(3, 3);
		m_btnDownload.setLocation(63, 64);
		m_btnDelete.setLocation(43, 64);
		m_btnUpload.setLocation(23, 64);
		m_btnOpen.setLocation(3, 64);
		m_openingAsTiles = false;
		term.m_viewersManager = new ViewersManager(term);
	}

	@Override
	protected boolean checkEmpty() {
		return m_previewImg == null;
	}

	@Override
	protected boolean checkForOptions(OptionedAction action) {
		return true;
	}

	@Override
	public void clear() {
		clearPreview();
	}

	public void clearPreview() {
		m_previewPanel.setImage(null);
		m_previewImg = null;
		m_term.m_previewBytes = null;
	}

	@Override
	protected void doUpload() {
		super.doUpload();
		if (m_success)
			executeUploadStatement(m_term.m_previewBytes, ((ImageTerm) m_term).m_previewDbField, true);
	}

	@Override
	protected void finalizeIteration(OptionedAction action) {
		if (action == OptionedAction.iteratingOpen && m_openingAsTiles) {
			m_term.m_viewersManager.tileViewers();
			m_openingAsTiles = false;
		}
	}

	@Override
	protected void getData() throws IOException {
		super.getData();
		m_img = ImageIO.read(new ByteArrayInputStream(m_bytes));
	}

	public void openAllAsTiles() {
		m_openingAsTiles = true;
		open(false, true);
	}

	@Override
	protected void openDocument() {
		m_term.m_viewersManager.openDocument(m_bytes);
	}

	@Override
	protected Boolean prepareUpload() {
		if (super.prepareUpload()) {
			Dimension targetDim = new Dimension();
			m_app.insideResize(targetDim, m_img, m_previewPanel);
			int width = targetDim.width, height = targetDim.height;

			m_previewImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = m_previewImg.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.clearRect(0, 0, width, height);
			g.drawImage(m_img, 0, 0, width, height, null);
			g.dispose();
			m_previewPanel.setImage(m_previewImg);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(m_previewImg, "jpg", baos);
				baos.flush();
				m_term.m_previewBytes = baos.toByteArray();
				baos.close();
			} catch (IOException e) {
				Logger.exceptionToHostLog(e);
			}
		}
		return m_success;
	}

	@Override
	public void render() {
		try {
			m_previewImg = m_term.m_previewBytes == null ? null : ImageIO.read(new ByteArrayInputStream(m_term.m_previewBytes));
			m_previewPanel.setImage(m_previewImg);
			m_previewPanel.repaint();
		} catch (IOException e) {
			Logger.exceptionToHostLog(e);
		}
	}

	@Override
	protected void setButtonEnable(JotyButton button, boolean truth, String ToolTipText, boolean considerPrecondition) {
		super.setButtonEnable(button, truth && m_previewImg != null, ToolTipText, !truth || m_previewImg != null);
	}

	@Override
	protected void setBytesToNull() {
		super.setBytesToNull();
		m_term.m_previewBytes = null;
	}

	@Override
	protected void successFeedBack() {}

	@Override
	protected void uploadEpilog() {
		try {
			if (m_success) {
				m_app.commitTrans();
				m_panel.checkForPublishing();
			} else
				m_app.rollbackTrans();
		} catch (JotyException e) {
			m_success = false;
		}
		m_term.guiDataExch(false);
		if (m_success) {
			m_panel.updateRecordOnController();
			m_panel.refresh();
		}
	}

	@Override
	protected void uploadProlog() {
		m_app.beginTrans();
	}

}
