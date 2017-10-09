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

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.joty.workstation.app.Application;

/**
 * Just a javax.swing.JPanel for painting the content preview of an
 * {@code ImageComponent} object and accepting mouse events for allowing the
 * opening of its actual content.
 * 
 * 
 * @see ImageComponent
 */
public class ImagePane extends JPanel {
	BufferedImage m_img;
	ImageComponent m_imageComponent;

	ImagePane() {
		super();
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!e.isConsumed()) {
					if (e.getClickCount() == 2)
						m_imageComponent.open(true);
					e.consume();
				}
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Point handle = null;
		if (m_img != null)
			handle = Application.centeredWindowHandle(m_img.getWidth(null), m_img.getHeight(null), getWidth(), getHeight());
		g.drawImage(m_img, handle == null ? 0 : handle.x, handle == null ? 0 : handle.y, null);
	}

	public void setImage(BufferedImage img) {
		m_img = img;
	}

	public void setImageComponent(ImageComponent imageComponent) {
		m_imageComponent = imageComponent;
	}

}
