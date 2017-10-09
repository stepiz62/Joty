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
import java.awt.event.*;
import java.beans.Beans;
import java.text.ParseException;
import java.util.Calendar;

import javax.swing.DefaultCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.MaskFormatter;

import org.joty.access.Logger;
import org.joty.app.Common;
import org.joty.common.ApplMessenger;
import org.joty.common.JotyTypes;
import org.joty.gui.NumberFormatter;
import org.joty.workstation.app.Application;
import org.joty.data.JotyDate;
import org.joty.workstation.gui.TextEditManager;
import org.joty.workstation.gui.Term.CcpCommand;
import org.joty.workstation.gui.Term.TermEnclosable;

/**
 * Extends the javax.swing.JFormattedTextField for editing all the JotyTypes
 * types but the blob ones.
 * <p>
 * At the presentation level a {@code NumberFormatter} instance is used for the
 * numerical types then the built-in formatting features of the {@code JotyDate}
 * class are used for presenting {@code _date} and {@code _dateTime} types.
 * <p>
 * For editing purposes it is used a javax.swing.text.MaskFormatter object
 * created by the {@code TextTerm} containing instance, by the invocation of
 * {@code Application.createFormatter} method: this uses the type information
 * for embedding the right constraints into the MaskFormatter object.
 * <p>
 * It also instantiates a {@code TextEditManager} as helper in decoding the
 * typed keys.
 * <p>
 * Further assistance for inputing text is provided by the
 * {@code checkForCompletion} method and, in the case of date types, the
 * {@code JotyCalendarDialog} object that opens on a double click on the text
 * box.
 * <p>
 * Its implementation of the java.awt.event.KeyListener and of the
 * java.awt.event.FocusListener interfaces covers all the desired behavior in
 * any circumstance. The content validation process happens on loosing focus and,
 * for date types, it in mainly dispatched to the JotyDate class.
 * 
 * @see TextEditManager
 * @see NumberFormatter
 * @see JotyDate
 * @see Application#createFormatter
 * @see JotyCalendarDialog
 * 
 */
public class JotyTextField extends JFormattedTextField implements FocusListener, KeyListener, TermEnclosable {
	int m_size;
	public TermContainerPanel m_panel;
	public Term m_term;
	public boolean m_readOnly;
	public Object m_iLenMaskDec;
	public boolean m_changed;
	final Color HILIT_COLOR = Color.WHITE;
	final Color m_bgColor;
	final Highlighter m_hilit;
	final Highlighter.HighlightPainter m_painter;
	public MaskFormatter m_formatter;
	protected NumberFormatter m_numberFormat;
	protected Common m_common;
	private boolean m_formatterInstalled;
	private String m_oldTxt;
	private int m_keyPressedCode;
	private boolean m_modifiersPressed;
	private Color m_disableForeColor;
	private Color m_enableForeColor;
	private boolean alreadyAlerted;
	private Application m_app;
	boolean m_dirty;
	boolean m_asCellEditor;
	private TextEditManager m_textManager;
	private boolean m_modifyingKey;
	private boolean m_asViewer;

	public JotyTextField(TermContainerPanel panel, Term term, MaskFormatter maskFormatter) {
		this(panel, term, maskFormatter, false);
	}

	public JotyTextField(TermContainerPanel panel, Term term, MaskFormatter maskFormatter, boolean asCellEditor) {
		super(maskFormatter);
		m_formatter = maskFormatter;
		m_formatterInstalled = true;
		m_panel = panel;
		m_term = term;
		m_asCellEditor = asCellEditor;
		m_textManager = new TextEditManager(this, term);
		if (!Beans.isDesignTime()) {
			m_app = Application.m_app;
			m_common = (Common) ((ApplMessenger) m_app).getCommon();
			if (m_term.m_dataType == JotyTypes._text) {
				uninstallFormatter();
				m_formatter.setOverwriteMode(false);
			} else if (!isDateType()) {
				setHorizontalAlignment(JTextField.RIGHT);
				uninstallFormatter();
				m_numberFormat = new NumberFormatter(m_app, m_term);
			}
		}
		// next line is for avoiding default behavior on 'Esc' key down event
		getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), new Object());
		addFocusListener(this);
		m_hilit = new DefaultHighlighter();
		m_painter = new DefaultHighlighter.DefaultHighlightPainter(HILIT_COLOR);
		m_bgColor = HILIT_COLOR;
		addKeyListener(this);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!e.isConsumed()) {
					if (e.getClickCount() == 2 && m_term != null && isDateType() && isEditable()) {
						JotyCalendarDialog dlg = new JotyCalendarDialog(m_common.m_sundayIsFDOW, JotyTextField.this);
						dlg.perform();
					}
					e.consume();
				}
			}
		});
		m_disableForeColor = new java.awt.Color(128, 128, 128);
		m_enableForeColor = new java.awt.Color(0, 0, 0);
		m_dirty = false;
		setDropTarget(null);
	}

	private boolean checkDate(String content, boolean withTime) {
		boolean retval = true;
		JotyDate date = new JotyDate(m_app);
		retval = date.setDate(content, withTime);
		if (retval)
			retval = date.validate();
		return retval;
	}

	/**
	 * Tries to complete the text content basing on the type (actually the date
	 * types are those which a completion can be thought for, a part from numbers that
	 * require special cases treated by the {@code floatingPointValueStr}
	 * method).
	 * 
	 * @return the completed text
	 */
	private String checkForCompletion() {
		String content = null;
		switch (m_term.m_dataType) {
			case JotyTypes._text:
				content = getText();
				break;
			case JotyTypes._date:
			case JotyTypes._dateTime:
				String dateFormat = m_term.m_dataType == JotyTypes._dateTime ? m_common.defDateTimeFormat() : m_common.defDateFormat();
				content = getText();
				Calendar todayCal = Calendar.getInstance();
				int parsableDay = parsableDateComponent(content, dateFormat.indexOf("d"), 2, false);
				int parsableMonth = parsableDateComponent(content, dateFormat.indexOf("M"), 2, false);
				int parsableYear = parsableDateComponent(content, dateFormat.indexOf("y"), 4, false);
				int parsableHour = 0;
				int parsableMinute = 0;
				int parsableSecond = 0;
				if (m_term.m_dataType == JotyTypes._dateTime) {
					parsableHour = parsableDateComponent(content, dateFormat.indexOf("H"), 2, true);
					parsableMinute = parsableDateComponent(content, dateFormat.indexOf("m"), 2, true);
					parsableSecond = parsableDateComponent(content, dateFormat.indexOf("s"), 2, true);
				}
				boolean completing = false;
				if (parsableDay != 0) {
					if (parsableDay < 10)
						completing = true;
					if (parsableYear == 0) {
						parsableYear = todayCal.get(Calendar.YEAR);
						if (parsableMonth == 0)
							parsableMonth = todayCal.get(Calendar.MONTH) + 1;
						completing = true;
					} else if (parsableYear < 100) {
						parsableYear += 2000;
						completing = true;
					}
					if (parsableHour == -1) {
						completing = true;
						parsableHour = 0;
					}
					if (parsableMinute == -1) {
						completing = true;
						parsableMinute = 0;
					}
					if (parsableSecond == -1) {
						completing = true;
						parsableSecond = 0;
					}
				}
				if (completing) {
					content = dateFormat;
					content = content.replace("dd", String.format("%1$02d", parsableDay));
					content = content.replace("MM", String.format("%1$02d", parsableMonth));
					content = content.replace("yyyy", String.format("%1$04d", parsableYear));
				}
				if (m_term.m_dataType == JotyTypes._dateTime) {
					content = content.replace("HH", String.format("%1$02d", parsableHour));
					content = content.replace("mm", String.format("%1$02d", parsableMinute));
					content = content.replace("ss", String.format("%1$02d", parsableSecond));
				}
				break;
			case JotyTypes._single:
			case JotyTypes._double:
			case JotyTypes._int:
			case JotyTypes._long:
			case JotyTypes._dbDrivenInteger:
				content = floatingPointValueStr(getText().trim());
				break;
		}
		return content;
	}

	public boolean doValidate() {
		boolean retVal = true;
		String settledContent = checkForCompletion();
		if (validateContent(settledContent)) {
			if (isDateType()) {
				if (m_dirty)
					setTextAndNotify(settledContent);
			} else {
				if (m_term.m_dataType != JotyTypes._text) {
					uninstallFormatter();
					if (settledContent.length() > 0)
						formatNumber(m_term.formatWrap(settledContent));
				}
			}
			if (!m_asCellEditor)
				m_term.guiDataExch(true);
		} else {
			retVal = false;
		}
		return retVal;
	}

	String floatingPointValueStr(String str) {
		String retVal;
		retVal = str.replace(m_common.m_thousandsSeparator, "").replace(" ", "").replace(m_common.m_currencySymbol, "");
		if (retVal.compareTo("-") == 0 || retVal.compareTo(m_common.m_decimalsSeparator) == 0 || retVal.compareTo("-" + m_common.m_decimalsSeparator) == 0)
			retVal = "0";
		return retVal;
	}

	@Override
	public void focusGained(FocusEvent focusevent) {
		if (isDateType())
			setCaretPosition(0);
		else {
			if (m_term.m_dataType == JotyTypes._text) {
				uninstallFormatter();
				String text = getText().trim();
				if (getText().length() > text.length())
					setText(text);
			} else {
				if (isEditable()) {
					String text = floatingPointValueStr(getText().trim());
					installFormatter();
					if (text.length() > 0)
						setText(text);
				}
			}
		}
	}

	@Override
	public void focusLost(FocusEvent focusevent) {
		if (m_asCellEditor)
			return;
		if (m_panel != null) {
			if (m_panel.m_dialog.criticalValidation()) {
				m_panel.m_dialog.setValidationUncritical();
				if (m_term != null)
					m_term.killFocus();
				return;
			}
		}
		if (doValidate()) {
			if (m_term != null)
				m_term.killFocus();
		} else
			grabFocus();
	}

	public void formatNumber(String content) {
		try {
			Double parsedDouble = m_numberFormat.m_format.parse(content).doubleValue();
			String formattedText = m_numberFormat.m_format.format(parsedDouble);
			setText(formattedText);
		} catch (ParseException e) {
			Logger.exceptionToHostLog(e);
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

	protected void installFormatter() {
		if (!m_formatterInstalled) {
			m_formatter.install(this);
			m_formatter.setOverwriteMode(false);
			m_formatterInstalled = true;
		}
	}

	private boolean isDateType() {
		return m_term.m_dataType == JotyTypes._date || m_term.m_dataType == JotyTypes._dateTime;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		m_keyPressedCode = e.getKeyCode();
		m_modifiersPressed = (e.getModifiersEx() & (m_term.commandDownMask() | KeyEvent.ALT_DOWN_MASK)) > 0;
		if (m_term.m_dataType == JotyTypes._text)
			m_textManager.getDotPos();
		m_modifyingKey = m_textManager.isAmodifyingKey(e);
		if (m_modifyingKey)
			m_dirty = true;
		if (m_keyPressedCode == KeyEvent.VK_ENTER && !m_asCellEditor)
			m_panel.doClickOnDefaultButton();
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (m_term.m_dataType == JotyTypes._text) {
			m_textManager.checkSize();
			if (m_modifyingKey)
				setTextAndNotify(getText());
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if (m_keyPressedCode == KeyEvent.VK_ENTER && m_asCellEditor)
			return;
		if (	m_keyPressedCode != KeyEvent.VK_ENTER && isEditable() && 
				!m_textManager.m_undoRedoAction && 
				(!m_modifiersPressed || 
					m_textManager.m_ccpCommand != CcpCommand.CCP_NONE && m_textManager.m_ccpCommand != CcpCommand.CCP_COPY || 
					(e.getModifiersEx() & KeyEvent.ALT_GRAPH_DOWN_MASK) > 0
				)
			)   {
			if (m_term.m_dataType != JotyTypes._text) {
				int m_selStart = m_textManager.m_selStart;
				int m_selEnd = m_textManager.m_selEnd;
				m_oldTxt = getText();
				String newContent = null;
				String leftOfSelectionPart = null;
				String caretRightPart = null;
				int caretShift = 0;
				if (m_dirty) {
					boolean selection = m_textManager.m_selEnd != m_textManager.m_selStart;
					if (m_keyPressedCode == KeyEvent.VK_BACK_SPACE || selection && m_textManager.m_ccpCommand == CcpCommand.CCP_CUT) {
						leftOfSelectionPart = m_oldTxt.substring(0, m_textManager.m_selStart - ((selection || m_textManager.m_selStart == 0) ? 0 : 1));
						caretRightPart = m_oldTxt.substring(m_textManager.m_selEnd);
						if (caretRightPart.trim().compareTo(m_common.m_dateSeparator) == 0)
							caretRightPart = "";
						newContent = leftOfSelectionPart + caretRightPart;
						if (m_textManager.m_ccpCommand == CcpCommand.CCP_CUT && !isDateType())
							m_textManager.m_undoManager.undo();
						setTextAndNotify(newContent.trim());
						caretShift = selection ? 0 : -1;
					} else if (m_keyPressedCode == KeyEvent.VK_DELETE) {
						leftOfSelectionPart = m_oldTxt.substring(0, m_textManager.m_selStart);
						caretRightPart = m_oldTxt.substring(m_textManager.m_selEnd + (selection || m_textManager.m_selEnd >= m_oldTxt.length() ? 0 : 1));
						newContent = leftOfSelectionPart + caretRightPart;
						setTextAndNotify(newContent.trim());
					} else {
						String incomingContent = "";
						boolean validContent = true;
						boolean thereIsEnoughRoom = false;
						String validChars = "";
						switch (m_term.m_dataType) {
							case JotyTypes._double:
							case JotyTypes._single:
								validChars = "-" + Application.INTEGER_MASK_VALID_CHARS + m_common.m_decimalsSeparator;
								break;
							case JotyTypes._int:
							case JotyTypes._long:
							case JotyTypes._dbDrivenInteger:
								validChars = "-" + Application.INTEGER_MASK_VALID_CHARS;
								break;
							case JotyTypes._date:
							case JotyTypes._dateTime:
								validChars = Application.INTEGER_MASK_VALID_CHARS + m_common.m_dateSeparator;
								if (m_term.m_dataType == JotyTypes._dateTime)
									validChars += m_common.m_timeSeparator;
								break;
						}
						switch (m_textManager.m_ccpCommand) {
							case CCP_NONE:
								incomingContent = m_textManager.m_concreteChar ? Character.toString(e.getKeyChar()) : "";
								break;
							case CCP_PASTE:
								incomingContent = m_app.getClipboardContents();
								break;
							default:
								break;
						}
						switch (m_term.m_dataType) {
							case JotyTypes._double:
							case JotyTypes._single:
							case JotyTypes._int:
							case JotyTypes._long:
							case JotyTypes._dbDrivenInteger:
								if (incomingContent.indexOf("-") == 0 && m_selStart > 0 || incomingContent.indexOf("-") > 0)
									validContent = false;
						}
						switch (m_term.m_dataType) {
							case JotyTypes._double:
							case JotyTypes._single:
								if (incomingContent.indexOf(m_common.m_decimalsSeparator) >= 0 && m_oldTxt.indexOf(m_common.m_decimalsSeparator) >= 0 
										|| incomingContent.indexOf(m_common.m_decimalsSeparator) == 0 && m_oldTxt.indexOf("-") == 0 && m_selStart == 0)
									validContent = false;
						}
						if (validContent) {
							String rightOfSelectionPart = null;
							if (isDateType())
								thereIsEnoughRoom = true; // OverwriteMode mode
							else {
								int trailingBlanks = 0;
								int minimumTrailingBlankPos = m_oldTxt.length();
								while (minimumTrailingBlankPos > m_selEnd && m_oldTxt.charAt(minimumTrailingBlankPos - 1) == ' ') {
									trailingBlanks++;
									minimumTrailingBlankPos--;
								}
								leftOfSelectionPart = m_oldTxt.substring(0, m_selStart);
								rightOfSelectionPart = m_oldTxt.substring(m_selEnd);
								int leftMarginLen = 0;
								if (leftOfSelectionPart.length() > 0 && leftOfSelectionPart.trim().length() == 0) {
									leftMarginLen = leftOfSelectionPart.length();
									int oldSelStart = m_selStart;
									int oldSelEnd = m_selEnd;
									m_selStart = 0;
									m_selEnd = oldSelEnd - oldSelStart;
									leftOfSelectionPart = "";
								}
								thereIsEnoughRoom = incomingContent.length() <= trailingBlanks + m_selEnd - m_selStart + leftMarginLen;
							}
							if (validChars.length() > 0) {
								int charCount = 0;
								while (charCount < incomingContent.length() && validChars.indexOf(incomingContent.charAt(charCount)) >= 0)
									charCount++;
								validContent = charCount == incomingContent.length();
							}
							if (validContent && thereIsEnoughRoom) {
								newContent = leftOfSelectionPart + incomingContent + rightOfSelectionPart;
								setTextAndNotify(isDateType() ? null : newContent.trim());
								caretShift = incomingContent.length();
							}
						}
					}
				}
				if (!isDateType())
					getCaret().setDot(m_selStart + caretShift);
			}
		}
	}

	private int parsableDateComponent(String content, int startPos, int size, boolean encodeEmpty) {
		int retVal = 0;
		try {
			String expr = content.substring(startPos, startPos + size).trim();
			if (expr.isEmpty() && encodeEmpty)
				expr = "-1";
			retVal = Integer.parseInt(expr);
		} catch (NumberFormatException e) {}
		return retVal;
	}

	public void render() {
		if (m_term.isNull()) {
			setText(m_term.m_dataType == JotyTypes._date ? 
					m_common.emptyDateRendering(false) : 
						(m_term.m_dataType == JotyTypes._dateTime ? m_common.emptyDateRendering(true) : ""));
		} else
			switch (m_term.m_dataType) {
				case JotyTypes._double:
				case JotyTypes._single:
				case JotyTypes._long:
				case JotyTypes._int:
				case JotyTypes._dbDrivenInteger:
					uninstallFormatter();
					setText(m_numberFormat.render());
					break;
				case JotyTypes._text:
					setText(m_term.m_strVal);
					setSelectionStart(0);
					setSelectionEnd(0);
					break;
				case JotyTypes._date:
				case JotyTypes._dateTime:
					setText(m_term.m_dateVal.toString(m_term.m_dataType == JotyTypes._dateTime));
					break;
			}
	}

	public void setAsViewer() {
		m_asViewer = true;
		m_disableForeColor = m_enableForeColor;
		setBorder(new EmptyBorder(0, 0, 0, 0));
		setBackground(null);
	}

	@Override
	public void setBackground(Color bg) {
		super.setBackground(getParent() == null || !m_asViewer ? bg : getParent().getBackground());
	}

	public void setCellEditor(DefaultCellEditor cellEditor) {}

	@Override
	public void setEditable(boolean b) {
		if (m_textManager != null && m_textManager.m_undoManager != null)
			m_textManager.m_undoManager.discardAllEdits();
		setForeground(b ? m_enableForeColor : m_disableForeColor);
		super.setEditable(b);
	}

	@Override
	public void setFocusLostBehavior(int behavior) {
		super.setFocusLostBehavior(JFormattedTextField.COMMIT);
	}

	public void setReadOnly(boolean truth) {
		setEditable(!truth);
	}

	@Override
	public void setSize(int arg0, int arg1) {
		super.setSize(120, 20);
	}

	public void setTextAndNotify(String text) {
		setTextAndNotify(text, false);
	}

	public void setTextAndNotify(String text, boolean updateData) {
		if (text != null)
			setText(text);
		m_term.notifyEditingAction(null);
		if (updateData)
			m_term.guiDataExch(true);
		m_panel.notifyEditingAction(null);
	}

	protected void uninstallFormatter() {
		getFormatter().uninstall();
		m_formatterInstalled = false;
	}

	public boolean validateContent(String content) {
		boolean isValid = true;
		String msg = null;
		if (m_term != null) {
			switch (m_term.m_dataType) {
				case JotyTypes._date:
				case JotyTypes._dateTime:
					isValid = checkDate(content, m_term.m_dataType == JotyTypes._dateTime);
					if (!isValid)
						msg = m_common.jotyLang("InvalidDate");
					break;
				case JotyTypes._double:
				case JotyTypes._single:
					int decSeparatorIndex = content.indexOf(m_common.m_decimalsSeparator);
					isValid = decSeparatorIndex < 0 && content.length() <= m_common.m_moneyDigitDim || 
								decSeparatorIndex >= 0 && decSeparatorIndex <= m_common.m_moneyDigitDim;
					if (!isValid)
						msg = m_common.jotyLang("NumberExceeding");
					break;
			}
		}
		if (!isValid) {
			if (!alreadyAlerted) {
				alreadyAlerted = true;
				warningMsg(msg);
				alreadyAlerted = false;
			}
		}
		return isValid;
	}

	public void warningMsg(String theMsg) {
		Application.warningMsg(theMsg, (String) null);
	}

}
