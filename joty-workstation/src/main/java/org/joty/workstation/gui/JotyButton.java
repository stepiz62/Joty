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

import javax.swing.Icon;
import javax.swing.JButton;

import org.joty.workstation.app.Application;

/**
 * Provides icon and tool-tip management.
 * 
 */
public class JotyButton extends JButton {
	public JotyButton() {}

	public JotyButton(Icon icon) {
		super(icon);
	}

	public JotyButton(String text) {
		this(text, null);
	}

	public JotyButton(String text, Term browsedTerm) {
		super(text);
		setToolTipText(text);
		if (browsedTerm != null)
			browsedTerm.m_browseButton = this;
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		setToolTipText(text);
	}

	@Override
	public void setToolTipText(String text) {
		super.setToolTipText(Application.m_app.m_toolTipsEnabled() ? text : null);
	}

}
