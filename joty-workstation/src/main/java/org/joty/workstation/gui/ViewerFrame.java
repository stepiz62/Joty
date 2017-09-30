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
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputAdapter;

import org.joty.workstation.app.Application;

/**
 * This class is responsible to display as image the binary content of the
 * database field managed by an {@code ImageComponent} instance.
 * <p>
 * It is instantiated by the {@code ViewersManager} of the ImageComponent
 * object, and by it is also directed, that is it receives the image content to
 * be display and the command to show itself.
 * <p>
 * The class extends the javax.swing.JFrame class and beyond having added the
 * listeners for managing its resizing and closing operations, for processing
 * key-stroke actions, it adds to its layout a javax.swing.JComponent instance
 * dedicated to the painting activity; this 'drawer' uses an instance of the
 * internally defined {@code ViewerMouseAdapter} class as various flavors of
 * mouse events listener, in order to allow dragging and zooming of the image
 * content, directly controlled respectively by the movement of the left clicked
 * mouse and the mouse wheel rolling.
 * <p>
 * In the constructor a pop-up menu is also added to the frame layout as an
 * alternative way to deliver commands to the viewer.
 * <p>
 * All the actions on the viewer different from (internal) painting occur
 * through a strict communication with the ViewersManager instance that must
 * keep track of the state of the viewer instance.
 * 
 * @see ImageComponent
 * @see ViewersManager
 * 
 */
public class ViewerFrame extends JFrame {

	class ViewerMouseAdapter extends MouseInputAdapter {
		private boolean m_mousePressed = false; /*
												 * to deal with swing spurious
												 * drag events (d-click on title
												 * bar)
												 */
		// multi-platform support .....
		private boolean m_popUpTriggerOnPressed = false; 

		@Override
		public void mouseDragged(MouseEvent e) {
			if (m_mousePressed) {
				m_X_shift = (int) ((e.getX() - m_dragStartX) / m_zoomFactor);
				m_Y_shift = (int) ((e.getY() - m_dragStartY) / m_zoomFactor);
				super.mouseDragged(e);
				repaint();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			m_mousePressed = true;
			m_popUpTriggerOnPressed = e.isPopupTrigger();
			m_dragStartX = (int) (e.getX() - (m_X_shift * m_zoomFactor));
			m_dragStartY = (int) (e.getY() - (m_Y_shift * m_zoomFactor));
			super.mousePressed(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			m_mousePressed = false;
			if (m_popUpTriggerOnPressed || e.isPopupTrigger())
				m_popUpMenu.show(e.getComponent(), e.getX(), e.getY());
			super.mouseReleased(e);
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int steps = e.getWheelRotation();
			boolean in = steps > 0;
			zoom(in ? steps : -steps, in);
			super.mouseWheelMoved(e);
		}
	}

	ViewersManager m_director;
	BufferedImage m_img;
	private long m_identity;
	int m_defaultWidth;
	int m_defaultHeight;
	int m_defaultX = 0;
	int m_defaultY = 0;
	double m_zoomFactor = 1;
	private int m_dragStartX;
	private int m_dragStartY;
	private int m_X_shift = 0;
	private int m_Y_shift = 0;
	Dimension m_targetDim;
	Point m_handle;
	JComponent m_drawer;
	JPopupMenu m_popUpMenu;
	double m_zoomStep = 1.1;
	Application m_app = Application.m_app;

	ViewerFrame(ViewersManager director, long indentity) {
		m_director = director;
		m_identity = indentity;
		m_targetDim = new Dimension();
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (!m_director.m_viewersRestoring)
					reset();
				super.componentResized(e);
			}
		});
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
				m_director.doCloseViewers(m_identity, true);
			}
		});
		if (m_app != null) {
			m_drawer = new JComponent() {
				{
					for (int i = 1; i <= 5; i++)
						toolTipRow("ViewerAction" + i);
					ViewerMouseAdapter viewerMouseAdapter = new ViewerMouseAdapter();
					addMouseListener(viewerMouseAdapter);
					addMouseMotionListener(viewerMouseAdapter);
					addMouseWheelListener(viewerMouseAdapter);
				}

				@Override
				public void paint(Graphics g) {
					super.paint(g);
					if (m_img != null && m_handle != null) {
						double handleZoomAdjustmentFactor = (m_zoomFactor - 1) / 2;
						int currentXhandle = m_handle.x + m_X_shift;
						int currentYhandle = m_handle.y + m_Y_shift;
						g.drawImage(m_img, 
									currentXhandle - (int) (2 * handleZoomAdjustmentFactor * (getWidth() / 2 - currentXhandle)), 
									currentYhandle - (int) (2 * handleZoomAdjustmentFactor * (getHeight() / 2 - currentYhandle)), 
									(int) (m_targetDim.width * m_zoomFactor), 
									(int) (m_targetDim.height * m_zoomFactor), 
									this);
					}
				}

				private void toolTipRow(String langLiteral) {
					m_app.addToolTipRowToComponent(this, m_app.m_common.jotyLang(langLiteral));
				}
			};
			add(m_drawer);
			addKeyListener(new KeyListener() {

				@Override
				public void keyPressed(KeyEvent e) {
					if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0)
						switch (e.getKeyCode()) {
							case KeyEvent.VK_T:
								m_director.tileViewers();
								break;
							case KeyEvent.VK_R:
								m_director.stackViewers();
								break;
							case KeyEvent.VK_W:
								m_director.doCloseViewers(0, true);
								break;
							case KeyEvent.VK_PLUS:
								zoomIn(1);
								break;
							case KeyEvent.VK_MINUS:
								zoomOut(1);
								break;
						}
				}

				@Override
				public void keyReleased(KeyEvent e) {}

				@Override
				public void keyTyped(KeyEvent e) {}
			});
			setIconImages(m_app.m_iconImages);
			addPopupMenu();
		}
	}

	private void addPopupMenu() {
		m_popUpMenu = new JPopupMenu();
		Application.addLangItemToMenu(m_popUpMenu, "ViewersAsTiles", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_director.tileViewers();
			}
		});
		Application.addLangItemToMenu(m_popUpMenu, "ViewersRestore", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_director.stackViewers();
			}
		});
		Application.addLangItemToMenu(m_popUpMenu, "ViewersClose", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_director.doCloseViewers(0, true);
			}
		});
		Application.addItemToMenu(m_popUpMenu, "Zoom +", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				zoomIn(3);
			}
		});
		Application.addItemToMenu(m_popUpMenu, "Zoom -", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				zoomOut(3);
			}
		});
	}

	public void reset() {
		m_zoomFactor = 1;
		m_X_shift = 0;
		m_Y_shift = 0;
		m_app.insideResize(m_targetDim, m_img, m_drawer);
		m_handle = Application.centeredWindowHandle(m_targetDim.width, m_targetDim.height, m_drawer.getWidth(), m_drawer.getHeight());
		setVisible(true);
		repaint();
	}

	public void setImage(BufferedImage img) {
		m_img = img;
	}

	public void setToDefaultLocation() {
		if (m_defaultX == 0) {
			m_defaultWidth = m_app.m_screenSize.width / 2;
			m_defaultHeight = m_app.m_screenSize.height / 2;
			int alreadyOpenedViewers = m_director.m_viewersMap.size();
			m_defaultX = m_defaultWidth / 2 + alreadyOpenedViewers * 20;
			m_defaultY = m_defaultHeight / 2 + alreadyOpenedViewers * 20;
		}
		setBounds(m_defaultX, m_defaultY, m_defaultWidth, m_defaultHeight);
		m_zoomFactor = 1;
		m_X_shift = 0;
		m_Y_shift = 0;
	}

	public void showViewer(BufferedImage m_img) {
		setImage(m_img);
		setToDefaultLocation();
		reset();
	}

	private void zoom(int steps, boolean in) {
		if (in && m_zoomFactor * m_zoomStep < 100 || !in && m_zoomFactor * m_zoomStep > 0.05)
			for (int i = 0; i < steps; i++)
				m_zoomFactor = in ? m_zoomFactor * m_zoomStep : m_zoomFactor / m_zoomStep;
		repaint();
	}

	private void zoomIn(int steps) {
		zoom(steps, true);
	}

	private void zoomOut(int steps) {
		zoom(steps, false);
	}

}
