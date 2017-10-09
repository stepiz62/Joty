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
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.Beans;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;

import org.joty.access.Logger;
import org.joty.common.JotyMessenger;
import org.joty.common.ParamContext;
import org.joty.data.JotyDate;
import org.joty.data.SearchQueryBuilderFront;
import org.joty.data.WrappedField;
import org.joty.gui.WFieldSet;
import org.joty.workstation.app.Application;
import org.joty.workstation.data.WField;

/**
 * It is the root for the javax.swing.JDialog derivations in the Joty framework.
 * It leaves opened the choice for its "modal-ity" to its extending classes but
 * takes a decision to be Modal if member {@code m_entityName} takes no value or
 * if the {@code Appplication.m_dialogOpeningAsValueSelector} member is true.
 * <p>
 * It provides the minimum set of buttons {Ok, Cancel} and little visual
 * behavior on activation events to keep the scene simple, and it collaborates
 * with the Application class to relate itself (in the case it is opened not
 * modal) to the other dialogs currently opened, and, more, in these behavior,
 * it takes in account the three different cases of OS platform. If the dialog
 * represented by the child class 'has name' then, on the instantiation, its
 * name is added to the 'window' menu. On closing of the dialog, the name is
 * deleted from there.
 * <p>
 * The class provides a static {@code tryCreate} method thought to instantiate
 * its extending implementations through reflection, and to take care that no
 * multiple instantiations of the same class happens at the same time; the
 * Application object helps by keeping track of the currently opened dialogs.
 * <p>
 * Even if the class is not specialized in data access it hosts a member of type
 * {@code DataAccessPanel} and a member of type a {@code GridManager}; these two
 * members may or may not be references to object actually contained in 'this'
 * instance: this class is root also for classes that need these references but
 * don't contain (visually) any of the actual objects.
 * <p>
 * The class, by means of its static {@code tryCreate} method, its constructors
 * and a member ({@code m_callContext}) hosting a {@code CallContext} object,
 * supports the propagation of {ParamContext} instances along the opening flow
 * that the various JotyDialog instances track when they are the invokers of the
 * opening of another dialog.
 * 
 * @see Application#m_activationStack
 * @see WindowsMenuItem
 * @see DataAccessPanel
 * @see GridManager
 * @see Application
 * @see ParamContext
 * 
 */
public class JotyDialog extends JDialog {

	/**
	 * A ParamContext equipped with two JotyDialog references used to build and
	 * to 'scroll' the propagation chain of ParamContext objects.
	 * 
	 */
	public class CallContext extends ParamContext {
		public CallContext(JotyMessenger jotyMessanger) {
			super(jotyMessanger);
		}
		public JotyDialog m_dialog;
		public JotyDialog m_caller;
	}
	
	/**
	 * Instantiates a JotyDialog extension class by reflection.
	 * @param className
	 *            the class name
	 * @param argTypes
	 *            the array of Class objects for the types of the arguments ( as
	 *            used in invoking a constructor by reflection )
	 * @param argValues
	 *            the array of objects for the types of the arguments ( as used
	 *            in invoking a constructor by reflection )
	 * @return the created instance
	 */
	protected static JotyDialog create(String className, Class[] argTypes, Object[] argValues) throws ClassNotFoundException {
		JotyDialog retVal = null;
		try {
			Class<?> Object = Class.forName(className);
			Constructor<?> constructor = Object.getDeclaredConstructor(argTypes);
			retVal = (JotyDialog) constructor.newInstance(argValues);
		} catch (InstantiationException e) {
			Logger.exceptionToHostLog(e);
		} catch (IllegalAccessException e) {
			Logger.exceptionToHostLog(e);
		} catch (NoSuchMethodException e) {
			Logger.exceptionToHostLog(e);
		} catch (SecurityException e) {
			Logger.exceptionToHostLog(e);
		} catch (IllegalArgumentException e) {
			Logger.exceptionToHostLog(e);
		} catch (InvocationTargetException e) {
			Logger.exceptionToHostLog(e);
			Logger.appendToHostLog(e.getCause().toString());
		}
		return retVal;
	}

	/** see {@link #getInstance(String, Class[], Object[])} */
	public static JotyDialog getInstance(String className) {
		return getInstance(className, (Class[]) null, null);
	}

	/**
	 * Try to close a possible living instance of the class of name specified
	 * with {@code className} parameter (by means of {@link #dialogStillLives} )
	 * and when no instance is living gets an new instance by invoking the
	 * {@link #create} method. If the possible living instance is not closable
	 * this method returns null.
	 * <p>
	 * For the meaning of the parameters see the {@link #create} method.
	 * 
	 * @return the instance created
	 */
	public static JotyDialog getInstance(String className, Class[] argTypes, Object[] argValues) {
		JotyDialog retVal = null;
		try {
			String fullClassName = Application.m_app.getDialogClassFullName(className);
			retVal = dialogStillLives(fullClassName) ? null : create(fullClassName, argTypes, argValues);
		} catch (ClassNotFoundException e) {
			Application.m_app.JotyMsg(null, e.toString());
		}
		return retVal;
	}

	/**
	 * see {@link #getInstance(String, Object, Object)} 
	 */
	public static JotyDialog getInstance(String className, Object callContext) {
		return getInstance(className, new Class[] { Object.class }, new Object[] { callContext });
	}

	/**
	 * As {@link #getInstance(String, Class[], Object[])} but with two specific parameters
	 * 
	 * @param callContext the {@code CallContext} object of the invoking JotyDialog.
	 * @param openingMode the opening mode of the target dialog among those defined in its class.  
	 */
	public static JotyDialog getInstance(String className, Object callContext, Object openingMode) {
		return getInstance(className, new Class[] { Object.class, Object.class }, new Object[] { callContext, openingMode });
	}

	/**
	 * Detects whether the Dialog is still living and in the positive case it
	 * tries to close it. It returns the truth of the predicate of its name as it
	 * is after its execution.
	 * 
	 * @param dialogClassName
	 *            is the full name of the class
	 * @return if it does
	 */
	public static boolean dialogStillLives(String dialogClassName) {
		JotyDialog dialog = Application.m_app.m_openedDialogs.get(dialogClassName);
		if (dialog == null)
			return false;
		else
			return !dialog.close();
	}

	protected static ImageIcon imageIcon(String fileName) {
		return Application.m_app.imageIcon(fileName);
	}

	/** see {@link #tryCreate(String, Class[], Object[])} */
	public static JotyDialog tryCreate(String className) {
		return tryCreate(className, (Class[]) null, null);
	}
	
	/**
	 * Tries to create a JotyDialog instance by getting and instance from the
	 * {@link #getInstance} method and then it calls the {@code perform} method to
	 * initialize and to show the JotyDialog instance created.
	 * <p>
	 * For the meaning of the parameters see the {@link #create} method.
	 * @return the instance created
	 */
	public static JotyDialog tryCreate(String className, Class[] argTypes, Object[] argValues) {
		JotyDialog dlg = getInstance(className, argTypes, argValues);
		if (dlg != null)
			dlg.perform();
		return dlg;
	}
	
	/** as  {@link #tryCreate(String, Class[], Object[])} but receives an Object parameter */
	public static JotyDialog tryCreate(String className, Object oneObjectParam) {
		return tryCreate(className, new Class[] { Object.class }, new Object[] { oneObjectParam });
	}

	/**
	 * As {@link #tryCreate(String, Class[], Object[])} but with two specific
	 * parameters
	 * 
	 * @param callContext the {@code CallContext} object of the invoking JotyDialog.
	 * @param openingMode the opening mode of the target dialog among those defined in its class.  
	 */
	public static JotyDialog tryCreate(String className, Object callContext, Object openingMode) {
		return tryCreate(className, new Class[] { Object.class, Object.class }, new Object[] { callContext, openingMode });
	}

	public Application m_app;
	public boolean m_gotData = false;
	protected Panel m_contentPanel;
	/**
	 * The associated command edits a record or even, in a single record
	 * management context, adds a new record (in the latter context it is
	 * mutable: its behavior depends on the existence of the underlying record)
	 */
	public boolean m_editOrNew_command;
	/**
	 * The associated command adds a record in a multi-record management context
	 */
	public boolean m_new_command;
	public DataAccessPanel m_currSheet;

	public boolean m_is_deleting;

	public GridManager m_gridManager;
	protected boolean m_newDocument;

	protected boolean m_isViewer;
	private boolean m_isActivating;
	public WFieldSet m_keyElems;
	protected String m_entityName;
	public SearchQueryBuilderFront m_queryBuilder;
	protected boolean m_isEntityNamed;
	public boolean m_actionEnabled;
	public JPanel m_buttonPane;
	public JotyButton m_btnClose;

	public JotyButton m_btnCancel;
	public JotyButton m_btnOk;
	public boolean m_canceling;
	protected boolean m_initializing;
	public boolean m_dataUpdate_pending;

	public boolean m_guiUpdate_pending;
	public JotyButton m_defaultButton;
	public boolean m_gridSelChanging;
	protected boolean m_needsExplicitCloseCommand;

	public DataAccessPanel m_parentDataPanel;
	public boolean m_listenForPanelActions;
	private WindowsMenuItem m_windowMenuItem;
	public boolean m_frozen;
	protected boolean m_initAction;
	protected boolean m_progressiveAction;
	protected String m_className = getClass().getName();
	public Object m_dialogOpeningMode;
	/**
	 * Sets the mode by which the JotyDialog instance and all its contained
	 * {@code Term} objects look in the Accessor object for their data definition statements.
	 */
	public boolean m_accessorMode;
	public boolean m_enforcedVisible;
	public boolean m_activatedByWindowsMenu;
	public Vector<Panel> m_panelsTobeInited;

	public CallContext m_callContext;

	public JotyDialog() {
		this((Object) null);
	}

	public JotyDialog(Frame owner) {
		super(owner);
	}

	public JotyDialog(Object callContext) {
		this(callContext, null);
	}

	public JotyDialog(Object callContext, Object openingMode) {
		this(callContext, openingMode, false);
	}

	public JotyDialog(Object callContext, Object openingMode, boolean initAction) {
		super(Application.m_app != null && Application.m_app.m_macOs ? Application.m_app.m_frame : null);
		m_app = Application.m_app;
		initContext(callContext);
		m_dialogOpeningMode = openingMode;
		m_initAction = initAction;
		setEntityName();
		Application.checkWBE(this);
		m_isEntityNamed = m_entityName != null && m_entityName.length() > 0;
		if (m_isEntityNamed)
			setTitle(m_entityName);
		if (!m_isEntityNamed || m_app.m_dialogOpeningAsValueSelector)
			setToModal();
		if (!Beans.isDesignTime())
			m_app.m_definingDialog = this;
		getContentPane().setLayout(null);
		m_contentPanel = new Panel();
		m_panelsTobeInited = new Vector<Panel>();
		m_contentPanel.setBounds(2, 2, 596, 334);
		m_contentPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		getContentPane().add(m_contentPanel);
		m_contentPanel.setLayout(null);
		setResizable(false);
		m_buttonPane = new JPanel();
		m_buttonPane.setBackground(Color.LIGHT_GRAY);
		m_buttonPane.setBounds(2, 339, 596, 37);
		m_buttonPane.setLayout(null);
		m_needsExplicitCloseCommand = false;
		m_progressiveAction = false;

		m_btnCancel = new JotyButton(imageIcon("cncBtn.jpg"));
		buildButton(m_btnCancel, jotyLang("LBL_CNC"), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkEnableAction())
					onCancel();
			}
		});
		m_btnCancel.setBounds(75, 5, 28, 28);
		m_buttonPane.add(m_btnCancel);

		m_btnOk = new JotyButton(imageIcon("okBtn.jpg"));
		buildButton(m_btnOk, jotyLang("LBL_OK"), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkEnableAction())
					onOK();
			}
		});
		m_btnOk.setBounds(112, 5, 28, 28);
		m_buttonPane.add(m_btnOk);

		m_btnClose = new JotyButton(imageIcon("exitBtn.jpg"));
		buildButton(m_btnClose, jotyLang("LBL_EXIT"), new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkEnableAction())
					onClose();
			}
		});
		m_btnClose.setBounds(558, 5, 28, 28);
		m_buttonPane.add(m_btnClose);

		m_defaultButton = m_btnOk;

		getContentPane().add(m_buttonPane);

		if (!Beans.isDesignTime())
			m_queryBuilder = new SearchQueryBuilderFront(m_app, m_app.new ClauseContribution());
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				super.windowActivated(e);
				if (m_enforcedVisible)
					m_enforcedVisible = false;
				else {
					if (m_isEntityNamed) {
						m_app.m_activationStack.remove(JotyDialog.this);
						m_app.m_activationStack.push(JotyDialog.this);
						if (m_app.m_macOs) {
							if (m_app.m_frame.getExtendedState() == JFrame.ICONIFIED)
								m_app.m_frame.setExtendedState(JFrame.NORMAL);

						}
						if (m_activatedByWindowsMenu) {
							m_activatedByWindowsMenu = false;
							if (m_app.m_dialogsAreToBeForcedOnTop) {
								m_enforcedVisible = true;
								JotyDialog.this.setVisible(false);
								JotyDialog.this.setVisible(true);
							}
						}
					}
					Application.setFrontMostContainer(JotyDialog.this);
					if (!m_frozen)
						doActivationChange(true);
				}
			}

			@Override
			public void windowClosing(final WindowEvent event) {
				if (m_needsExplicitCloseCommand)
					Application.langWarningMsg("UseEmbeddedCloseBtn");
				else if (!m_frozen)
					close();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				super.windowDeactivated(e);
				if (!m_enforcedVisible) {
					doActivationChange(false);
					m_actionEnabled = false;
				}
			}
		});

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				m_actionEnabled = true;
				super.mouseClicked(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				m_actionEnabled = ((JotyDialog) e.getSource()).isActive();
				super.mouseEntered(e);
			}

		});

		if (!Beans.isDesignTime()) {
			if (m_isEntityNamed) {
				m_app.m_openedDialogs.put(m_className, this);
				m_windowMenuItem = new WindowsMenuItem(m_entityName, this);
			}
		}
	}

	public void addIdentifierFromCallerToTitle(String identifierFromCaller) {
		if (identifierFromCaller != null)
			setTitle(getTitle() + " - " + identifierFromCaller);
	}

	protected void afterPerformed() {}

	protected String appLang(String literal) {
		return Beans.isDesignTime() ? literal : m_app.m_common.appLang(literal);
	}

	protected JotyButton buildButton(JotyButton btn, String verbose, ActionListener actionListener) {
		btn.setToolTipText(verbose);
		btn.addActionListener(actionListener);
		return btn;
	}

	boolean buttonAvailableOn(JotyButton button, boolean predicate) {
		showButton(button, predicate, predicate);
		return true;
	}

	protected void checkAndSetLook() {
		m_currSheet.checkAndSetLook();
	}

	protected boolean checkEnableAction() {
		boolean retVal = m_actionEnabled;
		m_actionEnabled = true;
		return retVal;
	}

	protected void checkForHooveringListener() {}

	protected void checkForHooveringListener(JComponent component) {}

	public boolean checkIfFrozen() {
		if (m_frozen)
			Application.langWarningMsg("DialogBusy");
		return m_frozen;
	}

	protected void checkPanelForNewRec() {}

	protected void clearAppReferences() {}

	public boolean close() {
		boolean retVal = !checkIfFrozen() && shouldDo();
		if (retVal) {
			closeDependentDialogs();
			clearAppReferences();
			if (Application.m_frontMostContainer == this)
				Application.setFrontMostContainer(null);
			dispose();
		}
		if (retVal && m_parentDataPanel != null)
			m_parentDataPanel.m_dependentDialogs.remove(this);
		if (retVal && m_isEntityNamed) {
			m_app.m_openedDialogs.remove(m_className);
			m_app.m_activationStack.remove(this);
			if (m_windowMenuItem != null)
				m_windowMenuItem.remove();
			m_windowMenuItem = null;
			m_app.checkForegroundObject();
		}
		return retVal;
	}

	public void closeDependentDialogs() {}

	protected boolean componentsEnabledOnIdle() {
		return false;
	}

	public boolean compoundDocument() {
		return false;
	}

	public String contextParameter(String name) {
		return m_callContext.contextParameter(name);
	}

	protected boolean criticalValidation() {
		return false;
	}

	protected void doActivationChange(Boolean activating) {
		m_buttonPane.setVisible(activating && !m_app.m_dialogOpeningAsValueSelector);
		if (activating && m_currSheet != null) 
			m_currSheet.doActivation();
	}

	boolean documentIdentified() {
		return m_currSheet.documentIdentified();
	}

	public GridManager getGridManager() {
		return getGridManager(false);
	}

	public GridManager getGridManager(boolean DialogLevelImperative) {
		return m_gridManager;
	}

	DataAccessPanel getGridMaster() {
		return m_currSheet.getGridManager() != null ? m_currSheet : null;
	}

	public long getMainSetSize() {
		return getGridManager().getMainSetSize();
	}

	public Object getMode() {
		return m_dialogOpeningMode;
	}

	public JotyButton getSearcherExpandButton() {
		return null;
	}

	protected JotyButton getSelectorButton() {
		return null;
	}

	boolean gridManagerExists() {
		return getGridManager() != null;
	}

	int gridPos() {
		GridManager gridManager = getGridManager();
		return gridManager == null ? -1 : gridManager.m_gridBuffer == null ? -1 : gridManager.m_gridBuffer.m_cursorPos;
	}

	public void guiDataExch(boolean predicate) {
		if (predicate && m_dataUpdate_pending || !predicate && m_guiUpdate_pending)
			return;
		if (predicate)
			m_dataUpdate_pending = true;
		else
			m_guiUpdate_pending = true;
		m_currSheet.guiDataExch(predicate);
		if (predicate)
			m_dataUpdate_pending = false;
		else
			m_guiUpdate_pending = false;
	}

	public boolean initChildren() {
		boolean retVal = true;
		for (Panel panel : m_panelsTobeInited) 
			if (!panel.init()) {
				retVal = false;
				break;
			}
		return retVal;
	}

	protected void initContext(Object callContext) {
		m_callContext = new CallContext(m_app);
		m_callContext.m_dialog = this;
		if (callContext != null) {
			m_callContext.m_caller = ((CallContext) callContext).m_dialog;
			m_callContext.copy(callContext);
		}
		if (m_app != null && m_app.m_common.m_shared) {
			if (m_callContext.isMissingParam("sharingKey"))
				setContextParam("sharingKey", m_app.m_common.m_sharingKey);
		}
	}

	protected boolean initDialog() {
		updateCommandButtons(true);
		preInitChildren();
		boolean retVal = initChildren();
		if (!Beans.isDesignTime()) {
			if (m_app.m_definingDialog == null)
				retVal = false;
			else
				m_app.m_definingDialog = null;
		}
		if (retVal)
			setIconImages();
		return retVal;
	}

	public boolean IsCreatable() {
		return true;
	}

	public boolean isEditing() {
		return m_editOrNew_command || m_new_command;
	}

	public boolean isInitializing() {
		return m_initializing;
	}

	public boolean isViewer() {
		return m_isViewer;
	}

	protected String jotyLang(String literal) {
		return Beans.isDesignTime() ? literal : m_app.m_common.jotyLang(literal);
	}

	WrappedField keyElem(int index) {
		return m_keyElems.vector.get(index);
	}

	public WrappedField keyElem(String keyField) {
		if (m_keyElems.map == null)
			return wrongWField();
		return m_keyElems.get(keyField);
	}

	public GridManager masterGridManager() {
		return m_currSheet.getGridManager();
	}

	public void onCancel() {}

	public void onClose() {
		close();
	}

	void onGridSelChange(ListSelectionEvent e, Panel panel) {}

	public void onOK() {
		close();
	}

	public void openDetailsDialog() {}

	public boolean perform() {
		if (initDialog()) {
			postInitDialog();
			setVisible(true);
			afterPerformed();
		} else {
			close();
			processFault();
		}
		return m_gotData;
	}

	void postInitDialog() {
		checkForHooveringListener();
	}

	protected void preInitChildren() {}

	protected void processFault() {}

	@Override
	public void repaint() {
		paintAll(getGraphics());
	}

	public void resetPanel(String panelTitle) {}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		Application.center(this, width, height);
	}

	public void setContextParam(String varName, JotyDate valueExpr) {
		m_callContext.setContextParam(varName, valueExpr);
	}

	public void setContextParam(String varName, long valueExpr) {
		m_callContext.setContextParam(varName, valueExpr);
	}

	public void setContextParam(String varName, String valueExpr) {
		m_callContext.setContextParam(varName, valueExpr);
	}

	protected void setEntityName() {}

	protected void setIconImages() {
		setIconImages(m_app.m_iconImages);
	}

	public void setToModal() {
		setModal(true);
		setModalityType(ModalityType.TOOLKIT_MODAL);
		setAlwaysOnTop(true);
	}

	protected void setValidationUncritical() {}

	boolean shouldDo() {
		return true;
	}

	protected void showButton(JotyButton theButton) {
		showButton(theButton, true);
	}

	protected void showButton(JotyButton theButton, boolean visible) {
		showButton(theButton, visible, true);
	}

	protected void showButton(JotyButton button, boolean visible, boolean enabled) {
		if (button != null) {
			button.setVisible(visible && isEnabled());
			if (!m_isActivating)
				button.setEnabled(enabled);
		}
	}

	protected void showDirtyEffect() {}

	protected void updateCommandButtons(boolean Idle) {}

	WrappedField wrongWField() {
		if (!Beans.isDesignTime())
			Application.warningMsg("Joty : failure");
		return new WField(m_app);
	}
}
