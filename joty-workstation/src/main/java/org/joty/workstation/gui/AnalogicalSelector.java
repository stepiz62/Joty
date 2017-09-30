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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

/**
 * A JPanel descendant that keeps associated a long value for identification
 * purposes and implementing the MouseListener interface in order to expose a
 * sensitive visual behavior.
 * <p>
 * Then it supports, for its appearance, three different visual states
 * represented by the truth of the members {@code m_selected} and
 * {@code m_heavy} allowing three different colors for the following scenarios :
 * {unselected without significance, unselected with significance, selected}.
 * <p>
 * In all states the instance of this class listens for the hovering of the
 * mouse pointer by reacting changing its color and/or its border color (when
 * the state is 'selected' only the border color changes).
 * 
 */
public class AnalogicalSelector extends JPanel implements MouseListener {

	public long m_keyValue;

	private Color m_highLightColor;
	private Color m_defaultColor;
	private Color m_selectedColor;
	private Color m_heavyColor;
	private Color m_highLightBorderColor;
	private Color m_selectedBorderColor;
	protected LineBorder m_defaultBorder;
	private LineBorder m_highLightBorder;
	private LineBorder m_selectedBorder;
	private boolean m_inside;
	private boolean m_selected;
	private boolean m_heavy;

	public AnalogicalSelector(Long keyValue) {
		m_keyValue = keyValue;
		setDefaultBorder();
		m_highLightBorderColor = new Color(255, 128, 0);
		m_highLightBorder = new LineBorder(m_highLightBorderColor);
		m_selectedBorderColor = new Color(0, 255, 0);
		m_selectedBorder = new LineBorder(m_selectedBorderColor);
		setBorder(m_defaultBorder);
		m_highLightColor = new Color(255, 255, 255);
		m_selectedColor = new Color(0, 128, 255);
		m_defaultColor = getBackground();
		m_heavyColor = new Color(160, 160, 160);
		addMouseListener(this);
		m_selected = false;
		m_inside = false;
	}

	public void doMouseReleased() {
		setBorder(m_selected ? m_highLightBorder : m_defaultBorder);
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	public boolean isInside() {
		return m_inside;
	}

	protected void manageMousePressed(MouseEvent e) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (isEnabled()) {
			m_inside = true;
			setBorder(m_highLightBorder);
			if (!m_selected)
				setBackground(m_highLightColor);
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if (isEnabled()) {
			m_inside = false;
			setGraphics();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (isEnabled()) {
			setBorder(m_defaultBorder);
			manageMousePressed(e);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (isEnabled())
			doMouseReleased();
	}

	protected void setDefaultBorder() {
		m_defaultBorder = new LineBorder(new Color(0, 0, 0));
	}

	private void setGraphics() {
		setBorder(m_selected ? m_selectedBorder : m_defaultBorder);
		setBackground(m_selected ? m_selectedColor : (m_heavy ? m_heavyColor : m_defaultColor));
	}

	public void setHeavyStatus(boolean heavy) {
		m_heavy = heavy;
		setGraphics();
	}

	public void setSelectionStatus(boolean selected) {
		m_selected = selected;
		setGraphics();
	}

}
