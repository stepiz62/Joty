/*
	Copyright (c) 2013-2017, Stefano Pizzocaro. All rights reserved. Use is subject to license terms.

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

package org.joty.workstation.app;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.Beans;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.text.MaskFormatter;
import javax.xml.bind.DatatypeConverter;

import org.joty.access.Accessor;
import org.joty.access.BirtManager;
import org.joty.access.DbManager;
import org.joty.access.Instantiator;
import org.joty.access.Logger;
import org.joty.access.MethodExecutor;
import org.joty.access.PostStatement;
import org.joty.app.Common;
import org.joty.app.JotyApplication;
import org.joty.app.JotyException;
import org.joty.app.LiteralsCollection;
import org.joty.app.LiteralsCollection.LiteralStructParams;
import org.joty.common.ApplMessenger;
import org.joty.common.BasicPostStatement;
import org.joty.common.BasicPostStatement.Item;
import org.joty.common.BasicPostStatement.ReturnedValueItem;
import org.joty.common.CaselessStringKeyMap;
import org.joty.common.ConfigFile;
import org.joty.common.ConfigFile.ConfigException;
import org.joty.common.ErrorCarrier;
import org.joty.common.ICommon;
import org.joty.common.JotyMessenger;
import org.joty.common.JotyTypes;
import org.joty.common.LangLiteralRetCodeMapper;
import org.joty.common.ParamContext;
import org.joty.common.ReportManager;
import org.joty.common.Utilities;
import org.joty.common.Utilities.Stocker;
import org.joty.common.XmlTextEncoder;
import org.joty.data.JotyDate;
import org.joty.data.SearchQueryBuilderFront;
import org.joty.data.WrappedField;
import org.joty.web.AbstractWebClient;
import org.joty.web.AbstractWebClient.DocumentDescriptor;
import org.joty.workstation.authorization.ChangePasswordDialog;
import org.joty.workstation.authorization.LoginDialog;
import org.joty.workstation.authorization.UsersPanel;
import org.joty.workstation.data.JotyDB;
import org.joty.workstation.data.JotyDataBuffer;
import org.joty.workstation.data.WField;
import org.joty.workstation.data.WResultSet;
import org.joty.workstation.gui.AboutDialog;
import org.joty.workstation.gui.AppOptionsDialog;
import org.joty.workstation.gui.DataAccessDialog;
import org.joty.workstation.gui.DataAccessPanel;
import org.joty.workstation.gui.DescrTerm;
import org.joty.workstation.gui.InfoDialog;
import org.joty.workstation.gui.JotyDialog;
import org.joty.workstation.gui.JotyFrame;
import org.joty.workstation.gui.JotyTextField;
import org.joty.workstation.gui.Panel;
import org.joty.workstation.gui.Table.JotyJTable;
import org.joty.workstation.gui.Term;
import org.joty.workstation.gui.TermContainerPanel;
import org.joty.workstation.web.WebClient;

/**
 * {@code  Application} provides initialization of the main functionalities:
 * detects the mode of running (desktop or web), manages the user's
 * authentication, builds the asset of the application by loading data from the
 * configuration file/s and from the user's home directory, loads the language
 * vector depending on the choice made by the user, loads rarely changing data
 * into memory from the database and keeps them in {@link LiteralStruct} objects.
 * 
 * <p>
 * Furthermore it exposes a large set of methods for presenting the user with framework 
 * and application messages and for getting simple input, keeps track of the currently 
 * opened dialogs and of the state of the application main frame.
 *
 * <p>
 * Offers methods for accessing the database server by hiding all is needed to
 * manage different assets derived by running in desktop or web mode and in
 * 'accessor' or 'non accessor' mode.
 * 
 */
public class Application implements ClipboardOwner, ApplMessenger, JotyApplication {

	/**
	 * Provides a way to concentrate on the execution body of a transaction
	 * implementing the exec method.
	 */
	public abstract class JotyTransaction {
		public boolean success;

		public JotyTransaction() {
			this(false);
		}

		public JotyTransaction(boolean insideExternalTrans) {
			if (!insideExternalTrans)
				beginTrans();
			try {
				success = exec();
				if (!insideExternalTrans)
					if (success)
						commitTrans();
					else
						rollbackTrans();
			} catch (JotyException e) {
				success = false;
				if (!insideExternalTrans)
					try {
						rollbackTrans();
					} catch (JotyException e1) {}
			}
		}

		public abstract boolean exec() throws JotyException;
	}

	/**
	 * {@code LangActionListener} implements the selection of the language. An
	 * instance of this class is passed to {@code addItemToMenu} method.
	 * 
	 * @see Application#addItemToMenu(JComponent, String, ActionListener)
	 */
	class LangActionListener implements ActionListener {
		String m_lang;

		LangActionListener(String lang) {
			m_lang = lang;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JCheckBoxMenuItem langItem = (JCheckBoxMenuItem) evt.getSource();
			boolean newState = langItem.isSelected();
			m_applicationPreferences.put("language", m_lang);
			informationMsg(String.format(m_common.jotyLang("OptionAcquired"), m_common.jotyLang("LangChoice")));
			langItem.setSelected(!newState);
		}
	}

	public class LiteralStruct extends LiteralsCollection{
		public LiteralStruct(JotyMessenger jotyMessangerInstance) {
			super(jotyMessangerInstance);
			m_termsSet = new HashSet<DescrTerm>();
		}
		public Set<DescrTerm> m_termsSet;
		public void updateTerms() {
			for (DescrTerm term : m_termsSet)
				term.reloadDescrList();
		}
	}


	/**
	 * It is the invocation target of the proxy class defined in the
	 * {@code manageMacOSenvironment} method. The proxy is defined as the
	 * dispatcher of calls to the com.apple.eawt.ApplicationListener, there
	 * instantiated through reflection. It receives the calls to the listener
	 * methods and specifically manages the call to the {@code handleAbout}
	 * method, in order to address the locally defined method {@link
	 * #showAboutDialog()}. Secondarily it invokes the {@code setHandled} method
	 * of the {@code ApplicationEvent} object received in {@code args[0]}.
	 * 
	 * @see #manageMacOSenvironment()
	 * @see #showAboutDialog()
	 * 
	 */
	public class MacAppListenerInvocationHandler implements java.lang.reflect.InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("handleAbout"))
				showAboutDialog();
			Method setHandledMethod = args[0].getClass().getDeclaredMethod("setHandled", new Class[] { boolean.class });
			setHandledMethod.invoke(args[0], new Object[] { Boolean.valueOf(true) });
			return null;
		}

	}

	/**
	 * An instance of this class is provided as the default
	 * {@code DragSourceListener} for the JVM in order to have the correct
	 * behavior of the cursor during dragging operations. For this it uses the
	 * {@code insideDataDialogs} method to manage also the cursor when it is
	 * over graphical objects that are not part of the Joty application.
	 * 
	 * @see Application#insideDataDialogs
	 */
	class MyDragSourceListener implements DragSourceListener {

		private void checkLocation(DragSourceEvent e) {
			m_insideDataDialogs = insideDataDialogs(e.getLocation());
		}

		@Override
		public void dragDropEnd(DragSourceDropEvent dsde) {}

		@Override
		public void dragEnter(DragSourceDragEvent dsde) {
			dsde.getDragSourceContext().setCursor(DragSource.DefaultCopyNoDrop);
		}

		@Override
		public void dragExit(DragSourceEvent dse) {
			dse.getDragSourceContext().setCursor(DragSource.DefaultCopyNoDrop);
		}

		@Override
		public void dragOver(DragSourceDragEvent dsde) {
			checkLocation(dsde);
			setCursor(dsde);
		}

		@Override
		public void dropActionChanged(DragSourceDragEvent dsde) {}

		private void setCursor(DragSourceDragEvent dsde) {
			DragSourceContext context = dsde.getDragSourceContext();
			int action = dsde.getDropAction();
			if (action == DnDConstants.ACTION_NONE || !m_insideDataDialogs)
				context.setCursor(DragSource.DefaultCopyNoDrop);
			else
				switch (action) {
					case DnDConstants.ACTION_MOVE:
						context.setCursor(m_dragDrainTriggerOn ? m_drainDropCursor : DragSource.DefaultMoveDrop);
						break;
					case DnDConstants.ACTION_COPY:
						context.setCursor(DragSource.DefaultCopyDrop);
						break;
					case DnDConstants.ACTION_LINK:
						context.setCursor(DragSource.DefaultLinkDrop);
						break;
				}
			if (m_DnDdrainIn && (!m_currDnDsourceIsDrainEnabled || !m_dragDrainTriggerOn))
				context.setCursor(DragSource.DefaultCopyNoDrop);
		}

	}

	public interface PasswordValidator {
		boolean validate(String password);
	}

	/**
	 * 
	 * It is the vehicle for values identified by the selection made by the user
	 * in the context of the opening of identity selector dialog. The selection
	 * in general, beyond the id value of the selected identity, identifies also
	 * fields of interest owning to the record just isolated. This set of fields
	 * takes definition during the call of
	 * {@code TermContainerPanel.acquireSelectedValueFrom}. It uses convenient
	 * data structures for keeping association between database fields and their
	 * values, furthermore it maps, when needed, TermContainerPanel Term-s on
	 * the corresponding database fields
	 * 
	 * @see TermContainerPanel#acquireSelectedValueFrom
	 */
	public class ValuesContainer {
		Vector<String> fields;
		CaselessStringKeyMap<String> terms2fieldsMap;
		CaselessStringKeyMap<WrappedField> values;

		public ValuesContainer() {
			fields = new Vector<String>();
			values = new CaselessStringKeyMap<WrappedField>(Application.this);
			values.setOverWritable();
			terms2fieldsMap = new CaselessStringKeyMap<String>(Application.this);
		}

		public void add(String termName, String fieldName) {
			fields.add(fieldName);
			terms2fieldsMap.put(termName, fieldName);
		}

		public void clear() {
			fields.removeAllElements();
			values.clear();
			terms2fieldsMap.clear();
		}

		public Vector<String> fieldNames() {
			return fields;
		}

		public WrappedField getValue(String termName) {
			return values.get(terms2fieldsMap.get(termName));
		}

		public boolean isEmpty() {
			return fields.size() == 0;
		}

		public void putValue(String fieldName, WrappedField value) {
			values.put(fieldName, value);
		}
	}
	
	public class ClauseContribution implements SearchQueryBuilderFront.TermContributor{
		@Override
		public String sqlValueExpr(WrappedField term) {
			return ((Term) term).sqlValueExpr();
		}
		@Override
		public String getOperator(WrappedField term, String matchOperator) {
			JComboBox opCombo = ((Term) term).m_operatorsCombo;
			String operator;
			if (opCombo == null)
				operator = matchOperator;
			else {
				String cmbOperator = (String) opCombo.getItemAt(opCombo.getSelectedIndex());
				operator = cmbOperator.length() == 0 ? matchOperator : cmbOperator;
			}
			return operator;
		}
	}

	private static boolean m_exclamationIconMissing;
	private static boolean m_exclamationIconChecked;

	public static JMenuItem addItemToMenu(JComponent menu, String string, ActionListener actionListener) {
		return addItemToMenu(menu, string, actionListener, null);
	}

	public static JMenuItem addItemToMenu(JComponent menu, String string, ActionListener actionListener, JMenuItem instancedItem) {
		JMenuItem menuItem = instancedItem == null ? new JMenuItem() : instancedItem;
		menuItem.setText(string);
		menu.add(menuItem);
		if (actionListener != null)
			menuItem.addActionListener(actionListener);
		return menuItem;
	}

	public static JMenuItem addItemToMenu(JComponent menu, String string, JMenuItem instancedItem) {
		return addItemToMenu(menu, string, null, instancedItem);
	}

	public static JMenuItem addLangItemToMenu(JComponent menu, String string, ActionListener actionListener) {
		return addItemToMenu(menu, m_app.m_common.jotyLang(string), actionListener);
	}

	public static JMenuItem addLangItemToMenu(JComponent menu, String string, ActionListener actionListener, JMenuItem instancedItem) {
		return addItemToMenu(menu, m_app.m_common.jotyLang(string), actionListener, instancedItem);
	}

	public static JMenuItem addLangItemToMenu(JComponent menu, String string, JMenuItem instancedItem) {
		return addLangItemToMenu(menu, string, null, instancedItem);
	}

	public static void appInit() {
		setFrontMostContainer(null);
	}


	public static boolean beginWaitPossibleCursor() {
		return m_app == null ? false : m_app.setWaitCursor(true);
	}

	public static void center(Window window, int width, int height) {
		if (!m_settingBound) {
			m_settingBound = true;
			Point handle = centeredWindowHandle(width, height);
			window.setLocation(handle.x, handle.y);
			m_settingBound = false;
		}
	}

	/**
	 * Uses its overridden flavor to get the top-left corner for centering,
	 * within the screen, the window the dimensions of which are passed as
	 * parameters.
	 * 
	 * @return top-left corner coordinates.
	 */
	public static Point centeredWindowHandle(int width, int height) {
		Toolkit tk = Toolkit.getDefaultToolkit();
		Dimension screenSize = tk.getScreenSize();
		return centeredWindowHandle(width, height, screenSize.width, screenSize.height);
	}

	/**
	 * Returns the top-left corner position of a sized window located inside a
	 * sized container such that the window is centered within the container
	 * 
	 * @return the Point object containing the coordinates that the top-left
	 *         corner
	 * 
	 */
	public static Point centeredWindowHandle(int windowWidth, int windowHeight, int containerWidth, int containerHeght) {
		Point handle = new Point();
		handle.x = (containerWidth - windowWidth) / 2;
		handle.y = (containerHeght - windowHeight) / 2;
		return handle;
	}

	/**
	 * Checks if design mode is on and creates the log file if it still doesn't
	 * exist. It also sets the framework with a reference to the container
	 * possibly used in communication to the user during the design
	 * session.
	 */
	public static void checkWBE(Container container) {
		if (Beans.isDesignTime()) {
			if (m_app == null) {
				m_app = new Application();
				Application.m_debug = false;
			}
			try {
				Files.deleteIfExists(Paths.get(m_app.m_JotyDesignLog));
			} catch (IOException e) {
			}
			Logger.appendToLog(m_app.m_JotyDesignLog, "", false, null, true);
			if (container != null)
				setFrontMostContainer(container.getParent());
		}
	}

	public static void endWaitPossibleCursor() {
		if (m_app != null)
			m_app.setWaitCursor(false);
	}

	private static String formatMessage(String text, Object[] objects) {
		return String.format(text, objects);
	}

	/**
	 * Returns the container {@code JotyDialog}
	 * 
	 * @param component
	 *            the operating component
	 * @return the JotyDialog
	 * @see JotyDialog
	 */
	public static JotyDialog getDialog(Component component) {
		Container retVal = component.getParent();
		while (retVal != null && !(retVal instanceof JotyDialog))
			retVal = retVal.getParent();
		return (JotyDialog) (retVal == null ? m_app.m_definingDialog : retVal);
	}

	/**
	 * See its override
	 * {@link #getInputFromUser(Container , String , Object[], Object)}
	 */
	public static String getInputFromUser(Container container, String text) {
		return getInputFromUser(container, text, null, null);
	}

	/**
	 * Opens the standard input dialog to get one choice from the user. The
	 * choice can be made by simple text or by a value selected from a pull-down
	 * list.
	 * 
	 * @param container
	 *            the container in which the input will open
	 * @param text
	 *            the text presented to the user
	 * @param selectionValues
	 *            (optional) an array of string composing the list of values
	 *            that will be presented in the pull down-list
	 * @param initialSelectionValue
	 *            (optional) the default selection for the pull-down list
	 * @return the text inputed by the user. If the choice was made by the
	 *         pull-down list, it is the text corresponding to the item
	 *         selected.
	 * 
	 * @see Application#getWrapAction(Container)
	 * @see JotyFrame#setAsFloatingBar(boolean)
	 */
	public static String getInputFromUser(Container container, String text, Object[] selectionValues, Object initialSelectionValue) {
		String retVal = null;
		boolean wrapAction = getWrapAction(container);
		Container actualContainer = wrapAction && !m_app.m_macOs ? m_app.m_frame : container instanceof JotyDialog ? ((JotyDialog) container).getContentPane() : container;
		ImageIcon icon = m_app.imageIcon("Userinput.png");
		if (actualContainer instanceof JotyFrame)
			actualContainer = ((JotyFrame) actualContainer).getContentPane();
		retVal = (String) JOptionPane.showInputDialog(actualContainer, text, joptionPaneAppName(), JOptionPane.PLAIN_MESSAGE, icon, selectionValues, initialSelectionValue);
		if (wrapAction)
			m_app.m_frame.setAsFloatingBar(true);
		return retVal;
	}

	/**
	 * It is the prologue for any message to the user, for determining whether,
	 * before opening the message, the main frame is to be restored to the
	 * centered position and to its default size.
	 * 
	 * @param container
	 *            the container in which the message will live
	 * @return true if the main frame has been restored
	 * 
	 * @see JotyFrame#setAsFloatingBar(boolean)
	 */
	private static boolean getWrapAction(Container container) {
		boolean wrapAction = (container == m_app.m_frame || container == null && !(m_frontMostContainer instanceof JotyDialog)) && m_app.m_mntmSetFrame != null && m_app.m_mntmSetFrame.isSelected();
		if (wrapAction)
			m_app.m_frame.setAsFloatingBar(false);
		return wrapAction;
	}

	public static void informationMsg(Container container, String text) {
		message(JOptionPane.INFORMATION_MESSAGE, container, text, 0, null);
	}

	public static void informationMsg(String text) {
		informationMsg(null, text);
	}

	/**
	 * Presents informational message to he user by means of an optionally
	 * parameterized format string.
	 * 
	 * @param text
	 *            the format string
	 * @param objects
	 *            the parameters for the string
	 * 
	 */
	public static void informationMsg(String text, Object[] objects) {
		informationMsg(formatMessage(text, objects));
	}

	/**
	 * This method is used for presenting a message to the developer of the Joty
	 * application. It is thought to help the testing activity.
	 * 
	 * @param object
	 *            the class object of interest in the context (typically
	 *            'this').
	 * @param text
	 *            the message body.
	 */
	public void JotyMsg(Object object, String text) {
		m_app.m_common.resetRemoteTransactionBuilding();
		if (!m_exclamationIconMissing && ! m_exclamationIconChecked) {
			m_exclamationIconChecked = true;
			try {
				if (m_app.getClass().getClassLoader().getResource("res/Exclamation.png") == null)
					m_exclamationIconMissing = true;
			} catch (Throwable th) {
				m_exclamationIconMissing = true;
			}
		}
		warningMsg(m_app.onTopDialog() != null && m_app.onTopDialog().isModal() ? m_frontMostContainer : m_app.m_frame, (object == null ? "" : (object.getClass().getName() + " : " + "")) + text, "Joty");
	}

	public static void langInformationMsg(String literal) {
		langInformationMsg(literal, null);
	}

	/**
	 * Like {@link #informationMsg(String , Object[])} but, instead of text, it
	 * accepts the literal that identifies the actual text within the
	 * {@code jotyLang.xml} file.
	 * 
	 * @param literal
	 *            the identifying literal.
	 * @param objects
	 *            (optional) if not null it is the list of the parameters for
	 *            the text format template, as the actual text can be.
	 */
	public static void langInformationMsg(String literal, Object[] objects) {
		informationMsg(formatMessage(Application.m_common.jotyLang(literal), objects));
	}

	public static void langWarningMsg(String literal) {
		langWarningMsg(literal, null);
	}

	/**
	 * Like {@link #langInformationMsg(String , Object[])} but for a warning
	 * message.
	 * 
	 */
	public static void langWarningMsg(String literal, Object[] objects) {
		warningMsg(formatMessage(Application.m_common.jotyLang(literal), objects));
	}

	public static boolean langYesNoQuestion(String literal) {
		return langYesNoQuestion(literal, null);
	}

	public static boolean langYesNoQuestion(String literal, Container container) {
		return langYesNoQuestion(literal, container, null);
	}

	/**
	 * Like {@link #langInformationMsg(String , Object[])} but for presenting a
	 * question and getting an answer from the user.
	 * 
	 */
	public static boolean langYesNoQuestion(String literal, Container container, Object[] objects) {
		m_askingToQuit = literal.compareTo("WantExitApp") == 0;
		boolean retVal = yesNoQuestion(formatMessage(Application.m_common.jotyLang(literal), objects), container);
		if (m_askingToQuit)
			m_askingToQuit = false;
		return retVal;
	}

	private static String joptionPaneAppName() {
		return m_app.m_name + (System.getProperty("os.name").toLowerCase().compareTo("linux") == 0 ? " ." : "");
	}
	
	/**
	 * It is the core method for messages to the user made by the framework. It
	 * relies on the Java Swing class {@code JOptionPane}. It invokes
	 * {@code getWrapAction} to eventually restore temporarily the main frame to
	 * its default asset in order to present the message in the context of the
	 * overall application.
	 *
	 * @return zero a part from the case in which it serves calls made by
	 *         {@link #yesNoQuestion(String)} method and its callers. In this
	 *         case it returns what returned by
	 *         {@code JOptionPane.showConfirmDialog}.
	 *
	 * @see Application#getWrapAction(Container)
	 * @see JotyFrame#setAsFloatingBar(boolean)
	 */
	private static int message(int type, Container container, String text, int option, String caption) { 
		boolean wrapAction = getWrapAction(container);
		int retVal = 0;
		String appName = joptionPaneAppName();
		Container context = container != null ? container : (m_frontMostContainer instanceof JotyDialog ? m_frontMostContainer : m_app.m_frame);
		switch (type) {
			case JOptionPane.QUESTION_MESSAGE:
				retVal = JOptionPane.showConfirmDialog(context, text, appName, option, type, m_app.imageIcon("Question.png"));
				break;
			case JOptionPane.INFORMATION_MESSAGE:
				JOptionPane.showMessageDialog(context, text, appName, type, m_app.imageIcon("Information.png"));
				break;
			case JOptionPane.WARNING_MESSAGE:
				JOptionPane.showMessageDialog(context, 
											text + (m_exclamationIconMissing ? " + Missing : res/Exclamation.png !" : ""), 
											caption, 
											type, 
											m_exclamationIconMissing ? null : m_app.imageIcon("Exclamation.png"));
				break;
		}
		if (wrapAction && (!m_askingToQuit || retVal != 0))
			m_app.m_frame.setAsFloatingBar(true);
		return retVal;
	}

	public static void warningMsg(Container container, String text, String caption) {
		message(JOptionPane.WARNING_MESSAGE, container, text, 0, caption);
	}

	public static void warningMsg(String text) {
		warningMsg(text, m_app == null ? "Joty" : joptionPaneAppName());
	}

	public static void warningMsg(String text, Object[] objects) {
		warningMsg(formatMessage(text, objects));
	}

	public static void warningMsg(String text, String caption) {
		warningMsg(null, text, caption == null ? joptionPaneAppName() : caption);
	}

	public static int yesNoCancelQuestion(Container container, String text, int option) {
		return message(JOptionPane.QUESTION_MESSAGE, container, text, option, null);
	}

	public static int yesNoCancelQuestion(String text) {
		return yesNoCancelQuestion(text, JOptionPane.YES_NO_CANCEL_OPTION);
	}

	public static int yesNoCancelQuestion(String text, int option) {
		return yesNoCancelQuestion(null, text, option);
	}

	public static boolean yesNoQuestion(String text) {
		return yesNoQuestion(text, (Container) null);
	}

	public static boolean yesNoQuestion(String text, Container container) {
		return yesNoCancelQuestion(container, text, JOptionPane.YES_NO_OPTION) == 0;
	}

	public static boolean yesNoQuestion(String text, Object[] objects) {
		return yesNoQuestion(formatMessage(text, objects));
	}

	/**
	 * Saves the bytes received as parameter in a temporary file. Then open this
	 * file;
	 * 
	 * @param bytes
	 *            the source buffer of bytes
	 * @param fileExt
	 *            the file name extension
	 * 
	 * @see org.joty.app.Common#saveBytesAsFile(byte[], String, String, boolean)
	 * @see #openUri(String, boolean)
	 */
	public static void openDocumentFromBytes(byte[] bytes, String fileExt) {
		m_app.beginWaitCursor();
		String tempDir = System.getProperty("java.io.tmpdir");
		String fileName = "JotyTemp" + String.valueOf(m_app.m_random.nextInt()) + "." + fileExt;
		m_common.saveBytesAsFile(bytes, tempDir, fileName, true);
		Application.m_app.openUri(tempDir + "/" + fileName, false);
		m_app.endWaitCursor();
	}

	/**
	 * Opens a resource from the local file system or from the web.
	 * 
	 * @param uri
	 *            the uri of the resource.
	 * @param webLocator
	 *            if true the resource is considered coming from the web and the
	 *            default Internet browser is used.
	 */
	public void openUri(String uri, boolean webLocator) {
		String exceptionMsg = null;
		Desktop desktop = null;
		if (!Desktop.isDesktopSupported())
			exceptionMsg = "Desktop is not supported !";
		if (exceptionMsg == null) {
			desktop = Desktop.getDesktop();
			if (!desktop.isSupported(Desktop.Action.BROWSE))
				exceptionMsg = "Desktop doesn't support the browse action !";
		}
		if (exceptionMsg == null) {
			try {
				if (webLocator)
					desktop.browse(new URI(uri));
				else
					desktop.open(new File(uri));
			} catch (Exception e) {
				Logger.exceptionToHostLog(e);
			}
		} else {
			Logger.appendToHostLog(exceptionMsg);
		}
	}

	
	public static void setFrontMostContainer(Container container) {
		m_frontMostContainer = container == null ? (m_app == null ? null : m_app.m_frame.getContentPane()) : container;
	}

	public static void setLocation(Window window, Point location) {
		m_settingBound = true;
		window.setLocation(location);
		m_settingBound = false;
	}

	public  boolean setWaitCursor(boolean set) {
		boolean oldIsWaitCursor = m_frontMostContainer.getCursor().equals(waitCursor);
		m_frontMostContainer.setCursor(set ? waitCursor : defCursor);
		return oldIsWaitCursor;
	}

	public static void writeOnDeskLog(String descr, String valStr) {
		if (m_deskTracing && m_tracing) {
			String outStr = descr + " : " + valStr;
			Logger.appendToHostLog(outStr);
			if (m_debug) {
				String msg = outStr + "\n\nDo you want entering debugging ?\n(Click on 'Cancel' not to be asked again)";
				if (yesNoCancelQuestion(msg) != JOptionPane.NO_OPTION)
					if (yesNoQuestion("Do you want to turn logging off (until an activation is reached on code execution) ?"))
						m_tracing = false;
			}
		}
	}

	public PasswordValidator m_passwordValidator;
	public boolean m_webMode;
	public boolean m_accessorMode;
	public JotyFrame m_frame;
	public String m_name;
	public JotyDB m_db;
	public String m_versionString;
	public String m_startPath;


	/**
	 * Associates a name of a LiteralStruct object with a map that holds
	 * association between id values and LiteralStruct object names. It is used,
	 * indeed, by a {@code GridTerm} class, to support the identification
	 * of a LiteralStruct instance depending on the literal selection made on a
	 * 'first level' LiteralStruct object.
	 */
	public CaselessStringKeyMap<HashMap<Long, String>> m_2L_literalMap;

	public CaselessStringKeyMap<JotyDialog> m_openedDialogs;
	public Stack<JotyDialog> m_activationStack;
	public static Application m_app;
	public boolean m_oldLoggingActivation;
	public WebClient m_webClient;
	public static Clipboard m_clipboard;
	public JotyDialog m_definingDialog;
	public Random m_random;

	public ConfigFile m_configExtension;


	
	public DbManager m_dbManager;
	public Accessor m_accessor;
	public ErrorCarrier m_errorCarrier;

	public Vector<Image> m_iconImages;
	public boolean m_committedClose;
	public JMenuBar m_menuBar;
	public boolean m_alreadyCertDeletionOffered;

	public String m_JotyDesignLog = System.getProperty("user.home") + "/JotyDesignLog.log";
	public String m_userHomeDataDir = "JotyData";
	public String m_ksPath = "/sec";
	public String m_ksFileName = "ks";
	public String m_JotyDeskLog;
	public static boolean m_deskTracing;
	public static boolean m_debug = true;

	public ReportManager m_reportManager;
	public Stocker m_userRoles;
	public JMenu m_windowsMenu;
	public JCheckBoxMenuItem m_mntmSetFrame;
	public JCheckBoxMenuItem m_mntmEnableTooltips;
	public Map<String, Point> m_windowsLocations;
	public Map<String, String> m_dialogMainSortInfos;

	public Map<String, String> m_applicationPreferences;
	public String m_openDlgCurrentDir;
	public JotyJTable m_currDnDjtable;
	public boolean m_currDnDsourceIsDrainEnabled;
	public boolean m_DnDdrainIn;
	public boolean m_currDnDcanDrop;
	public boolean m_dragDrainTriggerOn;
	public boolean m_dragDrainDropped;
	public boolean m_insideDataDialogs;
	public boolean m_dialogOpeningAsValueSelector;
	public long m_justSelectedValue;

	public ValuesContainer m_valuesContainer;
	public CaselessStringKeyMap<HashSet<String>> m_refreshMap;

	public String m_msgDataDefExpectedAsEntry = "Data definitions are expected to be defined as entries in the accessor !";
	public ParamContext m_paramContext;
	public boolean m_exitByMenu;
	/** If true the {@code Accessor} object lives within the Joty Server instead of in the Application object. */ 
	public boolean m_remoteAccessor;
	public boolean m_dialogsDesignedOnMac;
	public Vector<String> m_returnedValues;
	public Dimension m_screenSize;
	public ParamContext m_webTransPrmContext;

	public boolean m_dialogsAreToBeForcedOnTop;
	public ImageIcon m_appLogo;
	public ImageIcon m_jotyLogo;
	public String m_applicationLink;
	public String m_author;
	public String m_copyrightYears;
	public boolean m_macOs;
	public int m_applicationDefaultFontSize;

	private int m_returnedValuesAvailablePos;
	private InfoDialog m_infoDialog;
	private DragSourceListener m_dragSourceListener = new MyDragSourceListener();
	private Cursor m_drainDropCursor;
	private String m_dbmsSessionPreset;
	private CaselessStringKeyMap<Stocker> m_reportsRolePerms;

	protected String m_configuredUser;
	protected static Object m_macOSXapp;


	protected JMenu m_toolsMenu;
	protected JMenu m_fileMenu;
	protected JMenu m_viewMenu;
	protected JMenu m_authMenu;
	protected JMenu m_helpMenu;

	int m_fullOptions;
	/** holds the mapping between {@code JotyDataBuffer} objects and their names */
	CaselessStringKeyMap<JotyDataBuffer> m_setMap;
	String m_dataSourceTerm;
	String m_serverUrl;
	long m_userID;
	boolean m_xmlLogging;
	boolean m_bChargeIcon;
	boolean m_dbAuthOnly;
	Utilities m_utils;
	int m_iReTryCount;
	int m_iReTryMax;
	MethodExecutor m_methodExecutor;
	final String m_windowsLocationsFile = "WindowsLocations";
	final String m_dialogMainSortInfosFile = "DialogMainSortInfos";
	final String m_applicationPreferencesFile = "ApplicationPreferences";

	private BirtManager m_BirtManager;
	/**
	 * see {@link LangLiteralRetCodeMapper}
	 */
	private LangLiteralRetCodeMapper m_langLiteralRetCodeMapper;
	private String m_defaultBirtRender;
	private String m_workStationFontSize;
	public boolean m_connected;
	static boolean m_tracing;

	public static final Cursor defCursor = Cursor.getDefaultCursor();
	public static final Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
	public static final String INTEGER_MASK_VALID_CHARS = "0123456789";
	public static final String MSG_ListNotDef = "Main ScrollListPane Object not defined !";
	public static boolean m_settingBound = false;
	public static Container m_frontMostContainer;
	public static Common m_common;

	/**
	 * It keeps the detected intention of the user to exit. Its truth is used
	 * for avoiding main frame restoring during exiting.
	 */
	private static boolean m_askingToQuit;

/**
	 * It is the only constructor. Part of the initialization of the Joty
	 * application happens here.
	 * <p>
	 * It performs the detection of the mode of running (Web/desktop client) and
	 * creates operational java objects (among them the ReportManager) and creates the main frame and the menu
	 * bar. 
	 * <p>
	 * It detects the platform OS.
	 * <p>
	 * If the running mode is desktop client it performs the jdbc initialization.
	 * Further initialization occurs in the {@link #init(String, String, String, String)} method.
	 * 
	 * @see ReportManager
	 */

	public Application() {
		Utilities.setMessanger(this);
	    m_common = new Common(this);
		m_app = this;
		Logger.m_app = m_app;  
		Logger.appInit();
		Logger.setHostLogName("JotyDeskLog", "JotyLogs");
		m_db = new JotyDB();
		getClipboard();
		m_common.m_configuration = new ConfigFile(Application.this, "Joty.xml", false, null, true);
		m_webMode = m_common.m_configuration.m_document == null;
		m_common.m_commitExit = false;
		m_common.m_literalStructMap = new CaselessStringKeyMap<LiteralsCollection>(this);
		m_2L_literalMap = new CaselessStringKeyMap<HashMap<Long, String>>(this);
		m_setMap = new CaselessStringKeyMap<JotyDataBuffer>(Application.this);
		m_openedDialogs = new CaselessStringKeyMap<JotyDialog>(Application.this);
		m_activationStack = new Stack<JotyDialog>();
		m_reportsRolePerms = new CaselessStringKeyMap<Stocker>(Application.this);
		m_userRoles = Utilities.m_me.new Stocker();
		m_valuesContainer = new ValuesContainer();
		m_refreshMap = new CaselessStringKeyMap<HashSet<String>>(Application.this);
		m_paramContext = new ParamContext(this);
		m_tracing = false;
		if (!m_webMode) {
			try {
				DriverManager.registerDriver((Driver) Class.forName(m_common.getConfStr("jdbcDriverClass")).newInstance());
			} catch (Exception e) {
				jotyMessage("jdbc initialization failure !");
				Logger.exceptionToHostLog(e);
			}
		}
		m_xmlLogging = false;
		m_startPath = "";
		m_configuredUser = "";
		m_iReTryMax = 3;
		m_random = new Random();
		m_webClient = null;
		m_committedClose = false;
		m_alreadyCertDeletionOffered = false;
		m_passwordValidator = null;
		m_reportManager = new ReportManager();
		String os = System.getProperty("os.name").toLowerCase();
		m_dialogsAreToBeForcedOnTop = os.compareTo("linux") == 0;
		m_macOs = os.compareTo("mac os x") == 0;
		if (m_macOs)
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		m_frame = new JotyFrame();
		m_menuBar = new JMenuBar();
		m_frame.setJMenuBar(m_menuBar);
		m_frame.setDefaultRectDim(1024, 640);
		DragSource.getDefaultDragSource().addDragSourceListener(m_dragSourceListener);

		if (!designTime())
			m_drainDropCursor = Toolkit.getDefaultToolkit().createCustomCursor(image("trashcan.gif"), new Point(15, 0), "trashcan");
		m_common.m_dataReLoad = false;
		m_screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	}

	/**
	 * may be overridden for implementing actions upon initialization failure.
	 */

	protected void abortJobs() {}


	/**
	 * Creates a {@code BasicPostStatement} object hosting the name and the
	 * parameters of a method that will be invoked outside of the client
	 * context.
	 * <p>
	 * Typically it prepares a server-side call of the method, in the web
	 * running mode of the client, whichever is the task of the method (either a
	 * wrapper for a dbms store procedure or anything else to be executed
	 * 'far').
	 * <p>
	 * the definition of the method must exists in the application
	 * {@code Accessor} object.
	 * 
	 * @param method
	 *            the method name
	 * @param returnedValuePos
	 *            the position of the first returned value (among output
	 *            parameters and method return value) in the array of the values
	 *            returned by the invocation context - the {@code Accessor}
	 *            object - living either on the client side - in desktop client
	 *            mode - or on the Joty server side - in web mode - The position
	 *            is intended within the entirety of the set of values returned
	 *            in a transaction in which the method invocation participates.
	 * @param returnedValuesQty
	 *            integer value that, if positive, represents the quantity of
	 *            the output parameters values, if negative, represents again
	 *            the same quantity plus the method return value
	 * @return the created BasicPostStatement object
	 * 
	 * @see Accessor
	 * @see BasicPostStatement
	 */
	public BasicPostStatement accessorMethodPostStatement(String method, Integer returnedValuePos, Integer returnedValuesQty) {
		if (!m_webMode && !inDbTransaction())
			clearReturnStatus();
		BasicPostStatement postStatement = new BasicPostStatement(this);
		postStatement.setMethod(method, returnedValuePos, returnedValuesQty);
		return postStatement;
	}

	public boolean accessorExecute(String literal) {
		return accessorExecute(literal, m_paramContext);
	}

	/**
	 * creates a {@code BasicPostStatement} hosting an sql statement and a
	 * substitution literal (for dbms names obfuscation purposes) and executes
	 * it in terms of the Joty technology.
	 * 
	 * @param literal
	 *            the literal the substitution value of which is define in the
	 *            {@code Accessor} object of the application.
	 * @param paramContext
	 *            (optional) the {@code ParamContext} object (if missing the
	 *            {@code m_paramContext} member will be used.
	 * @return true in web mode and the actual execution result in desktop
	 *         client mode. mode.
	 */
	public boolean accessorExecute(String literal, ParamContext paramContext) {
		return m_db.executeSQL(literal, null, accessorPostStatement(null, -1, null, paramContext));
	}

	/**
	 * It is the core method for the creation of a {@code BasicPostStatement} object.
	 * <p>
	 * It exposes parameters for identifying a specific Accessor definition
	 * context.
	 * <p>
	 * If {@code dialog} is not null and the DataAccesorDialog object supports
	 * several modes along with be opened the BasicPostStatement.m_method of the
	 * creating object is assigned the value of the string value for the current
	 * opening mode
	 * <p>
	 * (note that on the client side, dialog opening modes are referred as
	 * object instance values - typically they are enumeration values - on the
	 * server side, however, they are referred as string values: here the
	 * conversion takes place).
	 * <p>
	 * In a Joty transaction this method is called typically several times. In
	 * web mode it keeps track of the current ParamContext object in the
	 * {@link Application#m_webTransPrmContext} member; on every call the method
	 * checks if ParamContext object changes and at each new different object
	 * encountered the method resets the dirty status of the
	 * {@code ContextParameter} objects contained in it, such that the
	 * ParamContext object is ready for new value assignments to its
	 * ContextParam objects and the WebClient will compose, in the JotyRequest,
	 * only the context parameters that has been assigned during the current
	 * Joty transaction definition session.
	 * <p>
	 * 
	 * @param dialog
	 *            the {@code DataAccessDialog} object the Accessor context of
	 *            which is to be addressed (actually a DialogDataDef object).
	 * @param panelIdx
	 *            the ordinal position of the {@code DataAccessPanel} object
	 *            the Accessor sub-context of which is to be addressed (actually
	 *            a DataDef object) (it must be equal to -1 if {@code dialog} is
	 *            null).
	 * @param termName
	 *            the name of the {@code GridTerm} object within the
	 *            DataAccessPanel, the Accessor sub-context of which is to be
	 *            addressed (actually a DataDef object), or the name of a
	 *            Accessor context-less {@code DataDef} object (see
	 *            {@link Accessor#m_statementDefs}) hosting an sql statement,
	 *            or, even, a literal residing in
	 *            {@link Accessor#m_literalSubsts} map that will be used for
	 *            dbms table name obfuscation acting on sql statement specified
	 *            later, during further definition of the BasicPostStatement object.
	 * 
	 * @param prmParamContext
	 *            the {@code ParamContext} object the {@code ContextParameter}
	 *            objects of which need to be built in the creating object as
	 *            {@code Item} objects. These items will be used by the Accessor
	 *            object (see
	 *            {@link Accessor#setPostStatement(PostStatement, boolean)}) to
	 *            re-create there the ParamContext object, no matter which its living 
	 *            location could be (either the server or the client side).
	 *            <p>
	 *            Actually currently an hybrid solution is adopted that seems to
	 *            support successfully the most needs: within a transaction
	 *            definition many ParamContext instances may be identified by
	 *            means of calls to this method, however, on the Accessor side a
	 *            merge of all ContextParam objects of all ParanContext objects
	 *            encountered is performed upon an only ParamContext map there
	 *            managed.
	 * @return the created PostStatement object
	 * 
	 * @see org.joty.access.PostStatement
	 * @see org.joty.common.BasicPostStatement.Item
	 * @see DataAccessDialog
	 * @see DataAccessPanel
	 * @see org.joty.access.Accessor
	 * @see org.joty.access.Accessor.DataDef
	 * @see org.joty.access.Accessor.DialogDataDef
	 * @see org.joty.server.JotyServer#dbExecute()
	 */
	public PostStatement accessorPostStatement(JotyDialog dialog, int panelIdx, String termName, ParamContext prmParamContext) {
		return accessorPostStatement(dialog == null ? null : dialog.getClass().getName(), 
									panelIdx, 
									termName,
									prmParamContext == null ? (dialog == null ? null : dialog.m_callContext) : prmParamContext,
									dialog != null && dialog.getMode() != null	? dialog.getMode().toString() : null);
	}
	
	/**
	 * It does the job for {@code accessorPostStatement(JotyDialog, int, String, ParamContext)} 
	 * but allows also to be called without the living instance of the {@code JotyDialog}, addressing 
	 * freely a resource within the implemented {@code Accessor} object, indeed.
	 * @see accessorPostStatement(JotyDialog, int, String, ParamContext)} 
	 */
	public PostStatement accessorPostStatement(String dialogName, int panelIdx, String termName, ParamContext paramContext, String mode) {
		if (!m_webMode && !inDbTransaction())
			clearReturnStatus();
		PostStatement postStatement = new PostStatement(this);
		postStatement.setDataDefCoordinates(dialogName, panelIdx, termName);
		if (mode != null)
			postStatement.m_method = mode;
		if (paramContext != null) {
			if (m_webMode && m_webClient.buildingRemoteTransaction() || !m_webMode && inDbTransaction()) {
				if (paramContext != m_webTransPrmContext) {
					if (m_webTransPrmContext == null)
						m_webTransPrmContext = paramContext;
					paramContext.setDirty(false);
				}
			}
			postStatement.addItemsFromParamContext(paramContext);
		}
		return postStatement;
	}

	/**
	 * Creates a {@code PostStatement} object for a literal substitution to be
	 * operated on an sql statement provided later to the object
	 * 
	 * @param literalName the literal for the substitution 
	 * @return the object created
	 * 
	 * @see #openAccessorSubstWResultSet
	 */
	public PostStatement createLiteralSubstPostStatement(String literalName) {
		return accessorPostStatement(null, -1, literalName, null);
	}

	/**
	 * One of the methods providing the duality of the framework behavior, it
	 * returns the currently available position within the vector of long values
	 * returned to the caller, vector that holds the invoked method return value
	 * or the value of one of its output parameters.
	 */
	 @Override
	 public int returnedValuesAvailablePos() {
		return m_webMode ? m_webClient.returnedValuesAvailablePos() : m_returnedValuesAvailablePos + 1;
	}

	/**
	 * Opens a WResultSet object through a BasicPostStatement object in which may be
	 * programmed a literal substitution to occur for the place holder of the
	 * database table name conveniently prepared in the selector sql statement.
	 * 
	 * @param tabLiteral
	 *            the table name or the literal used by the {@code Accessor}
	 *            object to return the actual table name.
	 * @param sql
	 *            the selector sql statement
	 * 
	 * @return true on success
	 * @see WResultSet
	 * @see #openDbWResultSetByPostStatement(PostStatement)
	 */
	public WResultSet openAccessorSubstWResultSet(String tabLiteral, String sql) {
		PostStatement postStatement = createLiteralSubstPostStatement(tabLiteral);
		postStatement.m_sql = sql;
		return openDbWResultSetByPostStatement(postStatement);
	}

	/** see {@link #openAccessorWResultSet(String, Panel, ParamContext)}  */
	public WResultSet openAccessorWResultSet(String literal) {
		return openAccessorWResultSet(literal, null, null);
	}

	/** see {@link #openAccessorWResultSet(String, Panel, ParamContext)}  */
	public WResultSet openAccessorWResultSet(String literal, Panel panel) {
		return openAccessorWResultSet(literal, panel, null);
	}

	/** see {@link #openAccessorWResultSet(String, Panel, ParamContext)}  */
	public WResultSet openAccessorWResultSet(String literal, ParamContext paramContext) {
		return openAccessorWResultSet(literal, null, paramContext);
	}

	/**
	 * Flexible method to open a {@code WresultSet} object my means of the
	 * {@code Accessor} object by the use of a {@code BasicPostStatement} instance
	 * conveniently prepared.
	 * 
	 * @param literal
	 *            possible name of the statement the body of which is stored
	 *            inside the Accessor object
	 * @param panel
	 *            possible {@code Panel} object used either for identifying the
	 *            parameters context or for locating the main statements for its
	 *            access to the database
	 * @param paramContext
	 *            parameters context explicitly passed by the caller.
	 * @return the {@code WResultSet} object
	 * 
	 * @see #openDbWResultSetByPostStatement
	 * @see Panel#createContextPostStatement()
	 * @see #accessorPostStatement(JotyDialog, int, String, ParamContext)
	 * 
	 */
	public WResultSet openAccessorWResultSet(String literal, Panel panel, ParamContext paramContext) {
		PostStatement postStatement = panel != null ? panel.createContextPostStatement() : accessorPostStatement(null, -1, literal, paramContext);
		postStatement.m_sql = literal;
		return openDbWResultSetByPostStatement(postStatement);
	}

	/**
	 * Provides different behaviors for opening a WResutSet object depending on
	 * the execution context being in Accessor mode and being in web mode. It is
	 * one of the core methods by which the framework can offer the invariance
	 * of interface to the developer, whether the application runs in web mode
	 * or not.
	 * 
	 * @param postStatement
	 *            the prepared PostStatement object
	 * @return the instantiated object
	 */
	public WResultSet openDbWResultSetByPostStatement(PostStatement postStatement) {
		String query = null;
		boolean byPostStatement = m_accessorMode || m_common.m_applicationScopeAccessorMode;
		if (m_accessor != null) {
			if (byPostStatement) {
				m_accessor.setPostStatement(postStatement);
				query = m_accessor.getQueryFromPostStatement();
			}
		}
		if (!byPostStatement)
			query = postStatement.m_sql;
		if (m_errorCarrier.m_exceptionMsg.length() > 0)
			jotyWarning(m_errorCarrier.m_exceptionMsg.toString());
		WResultSet rs = new WResultSet(null, query);
		if (rs.open(m_accessor != null && m_webMode || !byPostStatement ? null : postStatement))
			return rs;
		else
			return null;
	}

	/**
	 * It is a dual behavior method: it manages user authentication on the dbms
	 * in both the Joty modes.
	 * <p>
	 * Then it delegates the {@code Accessor} object for performing the
	 * following tasks: - manages validation of credentials accessing a map
	 * stored in the database table - checks if password is expired - checks if
	 * password must be changed
	 * 
	 * @return true if authentication completes successfully.
	 * @throws SQLException
	 * 
	 * @see #webAuthentication()
	 * @see #dbmsAuthentication()
	 * @see #verifyLogin()
	 * @see #doFurtherJobs()
	 * 
	 */
	boolean acquireConnection() throws SQLException {
		beginWaitCursor();
		boolean retVal = true;
		if (m_webMode) {
			retVal = webAuthentication();
			retVal = retVal && m_common.m_webSessionOn;
		} else
			retVal = dbmsAuthentication();
		endWaitCursor();

		if (retVal) {
			verificationProlog();
			retVal = verifyLogin();
			if (retVal)
				retVal = doFurtherJobs();
			else
				m_common.m_webSessionOn = false;
		}
		return retVal;
	}

	/**
	 * Enable a role to launch a report.
	 * 
	 * @param reportName
	 * @param roleName
	 * 
	 * @see #launchReport(String, String)
	 */

	protected void enableRoleToReport(String reportName, String roleName) {
		if (m_reportsRolePerms.get(reportName) == null)
			m_reportsRolePerms.put(reportName, Utilities.m_me.new Stocker());
		m_reportsRolePerms.get(reportName).add(roleName);
	}

	public void addToolTipRowToComponent(JComponent comp, String text) {
		if (m_toolTipsEnabled()) {
			String currentText = comp.getToolTipText();
			String newText;
			newText = currentText == null ? "<html><p>" : currentText.substring(0, currentText.indexOf("</html>")) + "<p>";
			comp.setToolTipText(newText + text + "</p></html>");
		}
	}


	protected boolean authenticate() {
		return authenticate(false);
	}

	/**
	 * Manages the interaction with the user during his use of the
	 * {@code LoginDialog} for authentication purposes and holds the
	 * {@code m_secure} member set to true during the process.
	 * 
	 * @param forLostSession
	 *            in web mode, if true, retry chance is not offered to the user
	 *            and the flag {@code Common.m_webSessionOn} is reset, so that
	 *            any access to the server must traverse the authentication
	 *            process.
	 * @return true if user authentication completed successfully.
	 * 
	 * @see LoginDialog
	 * @see WebClient#sqlQuery(String, boolean)
	 * @see #acquireConnection()
	 * @see org.joty.app.Common#m_secure
	 * 
	 */
	public boolean authenticate(boolean forLostSession) {
		if (forLostSession)
			m_common.m_webSessionOn = false;
		LoginDialog loginDlg = new LoginDialog(m_common.m_shared);
		m_connected = false;
		m_iReTryCount = 0;
		boolean exit = false;
		boolean reEnteringLogin;
		while (!loginDlg.m_valid && !loginDlg.m_abandoned && !m_common.m_commitExit) {
			m_connected = false;
			reEnteringLogin = false;
			if (m_iReTryCount > 0) {
				if (m_iReTryCount >= m_iReTryMax || loginDlg.m_abandoned || exit) {
					loginDlg.m_abandoned = true;
					break;
				}
			}
			if (!exit) {
				m_definingDialog = loginDlg;
				loginDlg.perform();
				m_common.m_userName = loginDlg.getUserName();
				m_common.m_password = loginDlg.getPassword();
				if (m_common.m_shared)
					m_common.m_sharingKey = loginDlg.getSharingKey();
				exit = false;
				if (loginDlg.m_abandoned)
					if (m_iReTryCount < m_iReTryMax) {
						exit = true;
						if (forLostSession) {
							exit = langYesNoQuestion("LeaveAuthExit");
							if (!exit) {
								reEnteringLogin = true;
								m_iReTryCount = 0;
							}
						}
						loginDlg.m_abandoned = exit;
					} else {
						exitMsg();
						exit = true;
					}
				if (!exit && !reEnteringLogin) {
					if (m_common.m_userName.length() == 0 || m_common.m_password.length() == 0 || m_common.m_shared && m_common.m_sharingKey.length() == 0)
						langWarningMsg("FillTextFields");
					else if (!m_connected) {
						try {
							m_common.m_secure = true;
							m_connected = acquireConnection();
							m_common.m_secure = false;
						} catch (SQLException e) {
							Logger.exceptionToHostLog(e);
						}
						if (!m_webMode || m_common.m_webSessionOn)
							m_iReTryCount++;
					}
					loginDlg.m_valid = m_connected;
				}
			}
		}
		if (loginDlg.m_abandoned)
			exit();
		if (m_connected && m_webMode) {
			m_connected = postAuthenticate();
			if (m_connected && m_common.m_shared)
				m_applicationPreferences.put("sharingKey", m_common.m_sharingKey);
		}
		return m_connected;
	}

	public void beginTrans() {
		m_db.beginTrans();
		m_webTransPrmContext = null;
	}

	public void beginWaitCursor() {
		setWaitCursor(true);
	}

	/**
	 * Returns the current instance of the {@code BirtManager} class.
	 * <p>
	 * If it is not yet available the method instantiates it and initializes it.
	 * 
	 * @return the {@code BirtManager} instance object.
	 * 
	 * @see BirtManager
	 */
	public BirtManager birtManager() {
		if (m_BirtManager == null) {
			m_BirtManager = new BirtManager(m_reportManager, this);
			m_BirtManager.appInit();
			if (m_webMode)
				m_BirtManager.init();
			else {
				m_BirtManager.setLanguage(m_common.m_language);
				boolean oldWaitStatus = setWaitCursor(true);
				openInfoDialog(m_common.jotyLang("InitializingRepEngineMsg"));
				try {
					ConfigFile config = m_common.m_configuration;
					m_BirtManager.init(
							config.configTermValue("rptDesignsPath"),
							config.configTermValue("rptDocumentsPath"),
							config.configTermValue("rptOutputsPath"),
							config.configTermValue("rptLogsPath"));
					m_BirtManager.setDbUrl(config.configTermValue("connection-url"));
					m_BirtManager.setJdbcDriverClass(config.configTermValue("jdbcDriverClass"));
				} catch (ConfigException e) {
				}
				m_BirtManager.setUser(m_common.m_userName);
				m_BirtManager.setPassword(m_common.m_password);
				closeInfoDialog();
				setWaitCursor(oldWaitStatus);
			}
		}
		return m_BirtManager;
	}

	protected void buildAppMenuBar() {}

	/** see {@link #buildLiteralStructMain} */
	public void buildLiteralStruct(String tabName, String keyField, String literalField, LiteralStruct literalStruct, LiteralStructParams prmDescrParms) {
		buildLiteralStructMain(tabName, keyField, literalField, null, literalStruct, prmDescrParms);
	}

	/** see {@link #buildLiteralStructMain} */
	public void buildLiteralStruct(String tabName, String keyField, String literalField, String name) {
		buildLiteralStruct(tabName, keyField, literalField, name, null);
	}

	/** see {@link #buildLiteralStructMain} */
	public void buildLiteralStruct(String tabName, String keyField, String literalField, String name, LiteralStructParams prmDescrParms) {
		buildLiteralStructMain(tabName, keyField, literalField, name, null, prmDescrParms);
	}

	/**
	 * Prepares and invokes {@code loadDataIntoLiteralStruct}.
	 * <p>
	 * If the {@code LiteralStruct} object is not passed as parameter it checks its
	 * existence in the map {@code m_literalMap} and, if found, gets it, else a
	 * new instance is created and is put in the map.
	 * 
	 * @param tabName the database table 
	 * @param keyField the database field hosting the id
	 * @param literalField the database field hosting the description
	 * @param name
	 *            the name of the structured object
	 * @param paramLiteralStruct possible already instantiated LiteralStruct object
	 * @param prmLsParams {@code LiteralStructParams} object carrying parameters
	 * 
	 * @see #loadDataIntoLiteralStruct(String, String, String, LiteralStruct,
	 *      LiteralStructParams)
	 *      
	 * @see org.joty.app.Common#buildLiteralStructMain     
	 * @see LiteralStructParams
	 */
	public void buildLiteralStructMain(String tabName, String keyField, String literalField, String name, 
												LiteralStruct paramLiteralStruct, LiteralStructParams prmLsParams) {
	      Common.LiteralsCollectionData data = m_common.buildLiteralStructMain(tabName, keyField, literalField, name, 
	    		  																paramLiteralStruct, prmLsParams);
	      loadDataIntoLiteralStruct(tabName, keyField, literalField, (LiteralStruct) data.literalsCollection, data.lsParms);
	 	}

	/**
	 * Loads in memory, in a convenient data structure ( a {@code LiteralStruct}
	 * object ) a set of records of type {long id, String description}. Note
	 * that possible re-iterated calls to this method on the same data set
	 * produces an update of any gui object the makes use of the inherent
	 * buffered data.
	 * 
	 * For the meaning of the parameters see {@link org.joty.app.Common#prepareToLoadIntoLiteralStruct}
	 * 
	 * @see org.joty.workstation.app.Application.LiteralStruct
	 * @see org.joty.app.LiteralsCollection.LiteralStructParams
	 * @see #buildLiteralStructMain(String, String, String, String,
	 *      LiteralStruct, LiteralStructParams)
	 * @see org.joty.workstation.app.Application.LiteralStruct#updateTerms()
	 * @see org.joty.app.Common#prepareToLoadIntoLiteralStruct
	 */
	public void loadDataIntoLiteralStruct(String tabName, String keyField, String literalField, LiteralStruct literalsCollection, LiteralStructParams lsParams) {
	    String sqlStmnt = m_common.prepareToLoadIntoLiteralStruct(tabName, keyField, literalField, literalsCollection, lsParams);	      		
		long IDval;
		int posIdx = literalsCollection.m_descrArray.size();
		WResultSet rs = openAccessorSubstWResultSet(tabName, sqlStmnt);
		if (rs != null) {
			if (rs.isEOF() && !literalsCollection.m_dynamic) {
				String msg = String.format("Unexisting description data for %1$s (literal structure = %2$s) !", 
											lsParams.selectStmnt != null ? lsParams.selectStmnt : tabName, literalsCollection.m_name);
				jotyWarning(msg);
			}
			while (!rs.isEOF()) {
				IDval = rs.integerValue(keyField);
				literalsCollection.addLiteral(rs, lsParams, literalField, IDval, posIdx);
				posIdx++;
				rs.next();
			}
			rs.close();
			literalsCollection.updateTerms();
		}
		m_common.m_literalStructFilter = "";
	}

	/**
	 * It is invoked by the framework and typically must be overridden in the
	 * Joty application project (in the descendant of the {@code Application}
	 * class) in order to load in memory all almost static data that is desired
	 * to access to without accessing the server. The framework implementation
	 * only loads roles in memory obfuscating the inherent database table; the
	 * override will have to include a call to the super implementation, indeed.
	 */
	public void loadDescriptions() {
		boolean old_applicationScopeAccessorMode = m_common.setApplicationScopeAccessorMode();
		if (m_common.m_shared)
			m_common.m_literalStructFilter = "id != 1";
		buildLiteralStruct("D0_1", "id", "name", "joty_roles");
		m_common.setApplicationScopeAccessorMode(old_applicationScopeAccessorMode);
	}

	/**
	 * Checks if the passed {@code JotyDataBuffer} object is already present in
	 * the map {@code m_setMap}. If it is not, then loads into it a set of
	 * records identified by the {@code sqlStmnt} sql text.
	 * 
	 * @param set
	 *            the buffer
	 * @param literal
	 *            the buffer name
	 * @param sqlStmnt
	 *            the selecting sql statement
	 * @see JotyDataBuffer
	 */
	public void buildSet(JotyDataBuffer set, String literal, String sqlStmnt) {
		if (m_setMap.get(literal) == null) {
			WResultSet rs = new WResultSet(null, sqlStmnt);
			set.loadData(rs);
			m_setMap.put(literal, set);
		} else {
			if (m_debug) {
				String msg = String.format(m_common.MSG_BUFFERNAME_ALREADY_TAKEN, literal);
				jotyMessage(msg);
			}
		}
	}

	/**
	 * Build all the menus general purpose menus (that is those ones offered by
	 * the Joty framework) without adding them to the menu bar, so the
	 * application developer can chose to add them or not, to improve them or to
	 * completely newly design them.
	 * 
	 * @see #addItemToMenu(JComponent, String, ActionListener, JMenuItem)
	 * @see #addLangItemToMenu(JComponent, String, ActionListener, JMenuItem)
	 * @see DataAccessDialog#tryCreate(String, Object, Object)
	 */
	protected void buildMenuBarComponent() {
		m_fileMenu = new JMenu(m_common.jotyLang("FileMenu"));
		m_viewMenu = new JMenu(m_common.jotyLang("ViewMenu"));
		m_mntmSetFrame = (JCheckBoxMenuItem) addLangItemToMenu(m_viewMenu, "MainFloating", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_frame.setAsFloatingBar(m_mntmSetFrame.isSelected(), true);
			}
		}, new JCheckBoxMenuItem());
		if (m_macOs)
			m_mntmSetFrame.setVisible(false);
		m_viewMenu.addSeparator();
		m_mntmEnableTooltips = (JCheckBoxMenuItem) addLangItemToMenu(m_viewMenu, "EnableTooltips", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				m_applicationPreferences.put("enableTooltips", m_mntmEnableTooltips.isSelected() ? "true" : "false");
				if (m_app.m_openedDialogs.size() > 0)
					informationMsg(m_frame, m_common.jotyLang("ToolTipNextOpening"));
			}
		}, new JCheckBoxMenuItem() {
			{
				boolean enable = true;
				String valueStored = m_applicationPreferences.get("enableTooltips");
				if (valueStored != null)
					enable = valueStored.compareTo("true") == 0;
				setSelected(enable);
			}
		});
		m_viewMenu.addSeparator();
		addLangItemToMenu(m_viewMenu, "ChangeStationFontSize", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (checkRoleForExecution("Administrators"))
					setWorkStationFontSize();
			}
		});

		m_authMenu = new JMenu(m_common.jotyLang("AuthMenu"));
		addLangItemToMenu(m_authMenu, "ChangePwd", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setPassword(true, m_common.m_userName);
			}
		});
		m_authMenu.addSeparator();
		addLangItemToMenu(m_authMenu, "RolesPanel", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				DataAccessDialog.tryCreate("org.joty.workstation.authorization.RolesDialog");
			}
		});
		addLangItemToMenu(m_authMenu, "UserPanel", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				DataAccessDialog.tryCreate("org.joty.workstation.authorization.UsersDialog");
			}
		});
		m_windowsMenu = new JMenu(m_common.jotyLang("WindowsMenu"));
		addLangItemToMenu(m_windowsMenu, "CloseAllW", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				closeAllWindows();
			}
		});
		m_windowsMenu.addSeparator();
		m_toolsMenu = new JMenu(m_common.jotyLang("ToolsMenu"));
		JMenu langMenu = (JMenu) addLangItemToMenu(m_toolsMenu, "Languages", new JMenu());
		Vector<String> langVector = new Vector<String>();
		Utilities.split(m_common.m_languages, langVector, ";");
		JCheckBoxMenuItem langItem;
		for (String lang : langVector) {
			langItem = (JCheckBoxMenuItem) addItemToMenu(langMenu, lang, new LangActionListener(lang), new JCheckBoxMenuItem());
			if (lang.compareTo(m_common.m_language) == 0)
				langItem.setSelected(true);
		}
		if (m_common.m_useAppOptions) {
			m_toolsMenu.addSeparator();
			addItemToMenu(m_toolsMenu, m_common.jotyLang("AppOptions"), new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					DataAccessDialog.tryCreate("org.joty.workstation.gui.AppOptionsDialog", null, AppOptionsDialog.mode.normal);
				}
			});
		}
		m_helpMenu = new JMenu(m_common.jotyLang("HelpMenu"));
		if (!m_macOs)
			addItemToMenu(m_helpMenu, String.format(m_common.jotyLang("About"), m_name), new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					showAboutDialog();
				}
			});
	}

	/**
	 * Called on closure of a {@code JotyDialog} object the method bring to
	 * front the second last activated object, holding by the stack
	 * {@code m_activationStack} member.
	 * 
	 * @see JotyDialog
	 */
	public void checkForegroundObject() {
		if (m_activationStack.size() == 0)
			m_frame.setVisible(true);
		else
			m_activationStack.peek().setVisible(true);
	}

	public boolean checkRoleForExecution(String role) {
		boolean retVal = m_app.m_userRoles.contains(role);
		if (!retVal)
			warningMsg(m_common.jotyLang("AccessDenied"));
		return retVal;
	}

	/**
	 * Used in desktop client mode, it clears data structures adopted for
	 * accessing the database server through the application {@code Accessor}
	 * object, instantiated locally indeed.
	 */
	public void clearReturnStatus() {
		if (!m_webMode) {
			m_errorCarrier.clear();
			m_returnedValuesAvailablePos = 0;
			m_returnedValues.removeAllElements();
		}
	}

	public boolean closeAllWindows() {
		boolean retVal = true;
		JotyDialog dlg = null;
		while (m_activationStack.size() > 0 && (dlg = m_activationStack.peek()) != null) {
			if (!dlg.close()) {
				retVal = false;
				break;
			}
		}
		return retVal;
	}

	public void closeContainer(Window container) {
		WindowEvent wev = new WindowEvent(container, WindowEvent.WINDOW_CLOSING);
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
	}

	/**
	 * A concise functional method to provide a place holder for the obfuscation
	 * of a database table name, to be used in sql expressions, the behavior of
	 * which is conditioned by the availability of the name itself as parameter.
	 */

	public String codedTabName(String tabName) {
		return tabName == null ? "<JOTY_CTX>" : tabName;
	}

	public void commitTrans() throws JotyException {
		m_db.commitTrans();
	}

	/**
	 * Creates a {@code ConfigFile} object for the client side. If it is the
	 * case of a language file the {@code m_common.m_language} member is used for the
	 * identification
	 * 
	 * @param fileName
	 * 
	 * @return the {@code ConfigFile} object.
	 * 
	 * @see ConfigFile
	 */
	private ConfigFile createConfigFile(String fileName) {
		ConfigFile configFile = new ConfigFile(Application.this, fileName, false, m_common.m_language, false);
		boolean success = true;
		if (configFile.isMissing()) {
			debugMsg(configFile.fileName() + " file not found or malformed !");
			success = false;
		}
		return success ? configFile : null;
	}

	/**
	 * Creates a {@code MaskFormatter} object to be used with the
	 * {@code JotyTextField} class.
	 * 
	 * @param dataType
	 *            one value form the {@code JotyTypes} enumeration.
	 * @param isCurrency
	 * @param len
	 *            the text length or the number size
	 * @return the instantiated object.
	 * 
	 * @see JotyTextField
	 * @see JotyTypes
	 */
	public MaskFormatter createFormatter(int dataType, boolean isCurrency, int len) {
		String mask = null;
		switch (dataType) {
			case JotyTypes._date:
				mask = getDateMask();
				break;
			case JotyTypes._dateTime:
				mask = getDateTimeMask();
				break;
			case JotyTypes._double:
				mask = Utilities.monoCharString('*', len > 0 ? len : (isCurrency ? m_common.m_moneyDigitDim : m_common.m_fisicalDigitDim));
				break;
			case JotyTypes._long:
			case JotyTypes._dbDrivenInteger:
				mask = "*" + Utilities.monoCharString('#', len > 0 ? len : m_common.m_longDigitDim);
				break;
			case JotyTypes._int:
				mask = "*" + Utilities.monoCharString('#', len > 0 ? len : m_common.m_intDigitDim);
				break;
			case JotyTypes._text:
				mask = Utilities.monoCharString('*', len);
				break;
		}
		MaskFormatter maskFormatter = null;
		try {
			maskFormatter = new MaskFormatter(mask);
		} catch (java.text.ParseException e) {
			Logger.exceptionToHostLog(e);
		}
		if (dataType == JotyTypes._double || dataType == JotyTypes._single)
			maskFormatter.setValidCharacters("-" + INTEGER_MASK_VALID_CHARS + m_common.m_decimalsSeparator);
		if (dataType == JotyTypes._date || dataType == JotyTypes._dateTime)
			maskFormatter.setValidCharacters(INTEGER_MASK_VALID_CHARS + " ");
		return maskFormatter;
	}

	/**
	 * Tries to access the database instance for the application through the
	 * jdbc layer .
	 * 
	 * @return true on success
	 * 
	 * @see JotyDB#getDbConn(boolean)
	 */
	protected boolean dbmsAuthentication() {
		boolean retVal = false;
		try {
			retVal = m_db.getDbConn(true);
			if (m_dbmsSessionPreset != null)
				m_db.executeSQL(m_dbmsSessionPreset);
		} catch (SQLException e) {
			if (m_dbManager.dbExceptionCheck(e, DbManager.ExcCheckType.INVALID_CREDENTIALS))
				try {
					throw new JotyException(JotyException.reason.INVALID_CREDENTIALS, null, this);
				} catch (JotyException e1) {}
			else if (m_dbManager.dbExceptionCheck(e, DbManager.ExcCheckType.DBMS_CONN_FAILURE))
				try {
					throw new JotyException(JotyException.reason.DBMS_UNREACHABLE, null, this);
				} catch (JotyException e1) {}

			else
				jotyMessage(e.getMessage());
		}
		return retVal;
	}

	public void debugMsg(String msg) {
		if (m_debug)
			jotyMessage(msg);
	}

	/**
	 * If the password must be changed because of settings on the user account
	 * about first login or if password validity is expired, this method opens
	 * the new password acquisition dialog.
	 * 
	 * @return true if things flew normal 
	 */
	protected boolean doFurtherJobs() {
		boolean retVal = true;
		if (mustPasswordBeChanged() || isDateExpired()) {
			retVal = false;
			langWarningMsg("NewPwdMustDef");
			retVal = setPassword(false, m_common.m_userName);
		}
		return retVal;
	}

	/**
	 * Delegates the BasicAccessor object to set the password by means of a
	 * there stored method.
	 */
	public boolean doSetPassword(String userName, String newPwd) {
		boolean old_applicationScopeAccessorMode = m_common.setApplicationScopeAccessorMode();
		m_paramContext.setContextParam("newPwd", newPwd);
		m_paramContext.setContextParam("setPwdUsername", userName);
		boolean retVal = accessorExecute("setUserPassword");
		m_common.setApplicationScopeAccessorMode(old_applicationScopeAccessorMode);
		return retVal;
	}

	public void endApp() {
		if (m_BirtManager != null)
			m_BirtManager.end();
		m_common.accessLocalData(m_windowsLocationsFile, m_windowsLocations);
		m_common.accessLocalData(m_dialogMainSortInfosFile, m_dialogMainSortInfos);
		m_common.accessLocalData(m_applicationPreferencesFile, m_applicationPreferences);
	}

	public void endWaitCursor() {
		setWaitCursor(false);
	}

	public boolean executeSQL(String stmt) {
		return executeSQL(stmt, null);
	}

	public boolean executeSQL(String stmt, String autoID) {
		return executeSQL(stmt, autoID, null);
	}

	/** see {@link JotyDB#executeSQL(String, String, BasicPostStatement)}	 */
	public boolean executeSQL(String stmt, String autoID, BasicPostStatement contextPostStatement) {
		return m_db.executeSQL(stmt, autoID, contextPostStatement);
	}

	public void exit() {
		closeContainer(m_frame);
	}

	public void exitByMenu() {
		m_exitByMenu = true;
		exit();
	}

	protected void exitMsg() {
		langWarningMsg("AppExiting");
	}

	public void getClipboard() {
		try {
			m_clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		} catch (SecurityException ex) {
			m_clipboard = new Clipboard("Sandboxed Clipboard");
		}
	}

	public String getClipboardContents() {
		String result = "";
		Transferable contents = m_clipboard.getContents(null);
		boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		if (hasTransferableText) {
			try {
				result = (String) contents.getTransferData(DataFlavor.stringFlavor);
			} catch (UnsupportedFlavorException ex) {
				Logger.exceptionToHostLog(ex);
			} catch (IOException ex) {
				Logger.exceptionToHostLog(ex);
			}
		}
		return result;
	}


	public String getDateMask() {
		return m_common.getDateAuxRendering("*", m_common.m_dateFormat);
	}

	public String getDateTimeMask() {
		return m_common.getDateAuxRendering("*", m_common.m_dateTimeFormat);
	}

	/**
	 * If {@code className} contain a simple name the method builds the fully
	 * qualified name of the class using the current package else the same value of the argument is returned.
	 * 
	 */
	public String getDialogClassFullName(String className) {
		return className.indexOf(".") == -1 ? (getClass().getPackage().getName() + "." + className) : className;
	}

	
	public long getNextId(DataAccessPanel dPanel) {
		long lRet = 0;
		String sql = m_dbManager.getIdGeneratorSql(dPanel.m_seq_name);
		if (m_webMode)
			m_webClient.addSqlToPostStmnt(sql, dPanel.m_mainDataTable);
		else
			lRet = m_dbManager.getId(sql);
		return lRet;
	}

	public JotyDialog getOpenedDialog(String className) {
		return getOpenedDialog(className, false);
	}

	public JotyDialog getOpenedDialog(String className, boolean silent) {
		JotyDialog retVal = m_openedDialogs.get(getDialogClassFullName(className));
		if (retVal == null && !silent)
			jotyMessage("Dialog class '" + className + "' not found !");
		return retVal;
	}

	public String getServerUrl() {
		if (m_serverUrl.length() == 0)
			langWarningMsg("PleaseStartAgain");
		return m_serverUrl;
	}

	public Image image(String fileName) {
		Image image = null;
		if (!designTime()) {
			String path = "res/" + fileName;
			java.net.URL iconURL = getClass().getClassLoader().getResource(path);
			if (iconURL == null)
				jotyWarning("Missing : " + path);
			else
				image = Toolkit.getDefaultToolkit().getImage(iconURL);
		}
		return image;
	}

	public ImageIcon imageIcon(String fileName) {
		ImageIcon icon = new ImageIcon();
		if (!designTime())
			icon.setImage(image(fileName));
		return icon;
	}

	public void incrementRetValIndex() {
		if (m_webMode)
			m_webClient.m_currentReturnedValueIndex++;
		else
			m_returnedValuesAvailablePos++;
	}

	public boolean inDbTransaction() {
		boolean retVal = false;
		try {
			retVal = !JotyDB.m_conn.getAutoCommit();
		} catch (SQLException e) {
			Logger.exceptionToHostLog(e);
		}
		return retVal;
	}

	public boolean infoDialogIsVisible() {
		return m_infoDialog != null && m_infoDialog.isVisible();
	}

	/**
	 * Initializes the {@code Application} instance.
	 * 
	 * @param name
	 *            The name of the application as it is shown in the title bar
	 * @param version
	 *            The version of the application
	 * @param servletName
	 *            The servlet name (if null, 'JotyServlet' will be used): the
	 *            argument must have a value conforming with the actual servlet
	 *            name as it is declared in the servlet container
	 * @param webClientClass
	 *            Normally null. It is the name of the possible
	 *            {@code WebClient} extension class.
	 * 
	 * @return true on success
	 * 
	 * @see WebClient
	 * @see org.joty.server.JotyServer
	 */
	public boolean init(String name, String version, String servletName, String webClientClass) {
		if (m_common.m_commitExit)
			return false;
		if (m_macOs)
			manageMacOSenvironment();
		if (servletName != null)
			m_common.m_servlet = servletName;
		boolean success = true;
		m_versionString = version;
		String strTitle = name;
		m_name = name;
		appInit();
		if (m_webMode) {
			boolean urlInClipboard = false;
			boolean urlInCodeBase = false;
			String str = getClipboardContents();
			if (str.indexOf("http://") >= 0 || str.indexOf("https://") >= 0) {
				urlInClipboard = true;
				m_common.m_appUrl = str;
			}
			BasicService bs = null;
			try {
				bs = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
			} catch (UnavailableServiceException e1) {
				jotyMessage("You have to start the application by means of the internet home page (web browser) or by means of a dedicated shortcut !");
			}
			if (success) {
				String codBaseUrl = null;
				if (bs != null) {
					URL codeBase = bs.getCodeBase();
					codBaseUrl = codeBase.toString();
					urlInCodeBase = codBaseUrl.length() > 0;
				}
				success = urlInCodeBase || urlInClipboard;
				if (urlInCodeBase)
					m_common.m_appUrl = codBaseUrl;
				if (urlInClipboard && !urlInCodeBase)
					jotyMessage("The url found in the clipboard will be used for the connection !\n\n" + m_common.m_appUrl);
				if (success)
					try {
						m_webClient = webClientClass == null ? new WebClient(this) : ((WebClient) Instantiator.create(webClientClass, null));
						success = m_webClient.m_urlValid;
					} catch (ClassNotFoundException e) {
						jotyMessage("WebClient class not found !");
						success = false;
					}
			}
		}
		repaint();
		if (success) {
			m_common.m_xmlEncoder = new XmlTextEncoder(this) {
				@Override
				protected byte[] base64decode(String src) {
					return DatatypeConverter.parseBase64Binary(src);
				}
				@Override
				protected String base64encode(byte[] src) {
					return DatatypeConverter.printBase64Binary(src);
				}};
			m_reportManager.setXmlEncoder(m_common.m_xmlEncoder);
			m_windowsLocations = (Map<String, Point>) m_common.accessLocalData(m_windowsLocationsFile, null);
			if (m_windowsLocations == null)
				m_windowsLocations = new HashMap<String, Point>();
			m_frame.setAsFloatingBar(false);
			m_frame.setName(strTitle);
			m_frame.setTitle(strTitle);
			m_frame.setVisible(true);
			m_dialogMainSortInfos = (Map<String, String>) m_common.accessLocalData(m_dialogMainSortInfosFile, null);
			if (m_dialogMainSortInfos == null)
				m_dialogMainSortInfos = new HashMap<String, String>();
			m_applicationPreferences = (Map<String, String>) m_common.accessLocalData(m_applicationPreferencesFile, null);
			if (m_applicationPreferences == null)
				m_applicationPreferences = new HashMap<String, String>();
			m_common.m_language = m_applicationPreferences.get("language");
			m_defaultBirtRender = m_applicationPreferences.get("defaultBirtRender");
			m_workStationFontSize = m_applicationPreferences.get("workStationFontSize");
			m_common.m_sharingKey =  m_applicationPreferences.get("sharingKey");
			if (m_common.m_language == null)
				m_common.m_language = "en";
			if (m_webMode) {
				// clean from previous usage
				m_common.m_configuration = null;
				if (m_webClient != null) {
					openInfoDialog("Connecting to server");
					m_common.m_configuration = m_webClient.getConfig("conf");
					if (m_common.m_configuration != null) {
						openInfoDialog("Getting configuration", null, false);
						try {
							m_remoteAccessor = m_common.getConfBool("remoteAccessor");
							m_debug = m_common.getConfBool("debug");
						} catch (ConfigException e) {
							Logger.exceptionToHostLog(e);
						}
						if (!m_remoteAccessor)
							m_configExtension = m_webClient.getConfig("confX");
						if (m_remoteAccessor || m_configExtension != null) {
							m_common.m_JotyLang = m_webClient.getConfig("jotyLang", m_common.m_language);
							if (m_common.m_JotyLang != null) {
								m_common.m_JotyLang.buildMap();
								m_common.m_JotyAppLang = m_webClient.getConfig("appLang", m_common.m_language);
								if (m_common.m_JotyAppLang != null)
									m_common.m_JotyAppLang.buildMap();
							}
						}
					}
					closeInfoDialog();
				}
				success = m_common.m_JotyAppLang != null;
				if (success)
					m_common.loadKeyStore();
			} else {
				m_common.m_commitExit = (m_common.m_JotyLang = createConfigFile("JotyLang.xml")) == null;
				if (!m_common.m_commitExit)
					m_common.m_commitExit = (m_common.m_JotyAppLang = createConfigFile("AppLang.xml")) == null;
				if (m_common.m_commitExit)
					success = false;
			}
		}
		m_errorCarrier = new ErrorCarrier();
		if (success) {
			m_langLiteralRetCodeMapper = new LangLiteralRetCodeMapper(this);
			m_langLiteralRetCodeMapper.load(m_common.m_JotyLang);
			success = loadConfigProperties();
			setDefaultFont();
			buildMenuBarComponent();
			buildAppMenuBar();
			Logger.setDebugMode(m_debug);
			if (success) {
				try {
					m_dbManager = Instantiator.createDbManager(m_errorCarrier, m_common.m_configuration);
				} catch (ClassNotFoundException e1) {
					jotyMessage("DbManager class not found !");
					success = false;
				}
				if (!m_webMode || !m_remoteAccessor)
					try {
						m_accessor = Instantiator.createAccessor(this, m_errorCarrier, m_common.m_configuration, m_common.m_configuration, null);
						m_accessor.setPaginationQuery(m_common.m_paginationQuery, m_common.m_paginationPageSize);
						m_accessor.setLangLiteralRetCodeMapper(m_langLiteralRetCodeMapper);
					} catch (ClassNotFoundException e1) {
						if (m_debug)
							jotyMessage("Accessor class not found !");
					}
			}
			if (!m_webMode) {
				m_returnedValues = new Vector<String>();
				m_methodExecutor = new MethodExecutor(m_accessor, m_errorCarrier, m_returnedValues, null);
			}
		}
		if (success) {
			success = authenticate();
			m_paramContext.setContextParam("userName", m_common.m_userName);
			if (success && m_common.m_authServer == null)
				loadUserRoles();
			repaint();
		}
		if (success) {
			if (m_common.m_shared)
				m_paramContext.setContextParam("sharingKey", m_common.m_sharingKey);
			Utilities.checkDirectory(userHomeDataPath());
			m_dbManager.setConn(JotyDB.m_conn);
			if (m_accessor != null)
				m_accessor.setConn(JotyDB.m_conn);
			loadData();
			m_common.m_dataReLoad = true;
			registerReports();
		} else {
			m_committedClose = true;
			abortJobs();
		}
		if (success) {
			if (m_common.m_useAppOptions)
				DataAccessDialog.tryCreate("org.joty.workstation.gui.AppOptionsDialog", null, AppOptionsDialog.mode.asFetcher);
			else {
				try {
	                m_common.acquireLocsFromConf();
	                m_common.acquireLocaleInfo();
				} catch (ConfigException e) {
					Logger.exceptionToHostLog(e);
				}
			}
		} else
			m_frame.close();
		return success;
	}

	public boolean insideDataDialogs(Point point) {
		boolean retVal = false;
		for (Iterator it = m_openedDialogs.entrySet().iterator(); it.hasNext();) {
			if (((Entry<String, JotyDialog>) it.next()).getValue().getBounds().contains(point)) {
				retVal = true;
				break;
			}
		}
		return retVal;
	}

	public void insideResize(Dimension target, BufferedImage srcImg, Component container) {
		if (srcImg == null)
			return;
		target.width = srcImg.getWidth(null);
		target.height = srcImg.getHeight(null);
		boolean horizontalConstraint = ((double) target.width / (double) target.height) > ((double) container.getWidth() / (double) container.getHeight());
		resize(target, horizontalConstraint ? (double) container.getWidth() / (double) target.width : (double) container.getHeight() / (double) target.height);
	}

	public boolean invokeAccessMethod(BasicPostStatement postStatement)  {
		boolean success = m_webMode ? 
				m_webClient.manageCommand(null, false, null, true, postStatement, 0) : 
				m_methodExecutor.exec(postStatement, !inDbTransaction(), JotyDB.m_conn);
		if (success) {
			if (! m_webMode || !  m_webClient.buildingRemoteTransaction())  {
				Vector<String> returnedValues = m_webMode ? m_webClient.m_returnedValues : m_returnedValues;
				int vectorIndex = -1;
				if (postStatement.m_returnedValues != null)
					for (ReturnedValueItem item : postStatement.m_returnedValues) {
						vectorIndex = item.m_returnedValPosition - 1;
						item.valueLiteral = returnedValues.get(vectorIndex);
					}
				if (postStatement.m_outParamsQty.length() > 0 && Integer.parseInt(postStatement.m_outParamsQty) < 0)
					postStatement.m_retVal = returnedValues.get(vectorIndex + 1);
			}
		} else {
			String msg = m_errorCarrier.m_exceptionMsg.toString();
			if (! m_webMode)
				try {
					m_common.checkAndThrow(m_dbManager, msg, null);
				} catch (JotyException e) {}
			jotyWarning(msg);
		}
		return success;
	}
	

	public String languageItem(String literal, ConfigFile langCF) {
		try {
			return designTime() ? literal : langCF.configTermValue(literal);
		} catch (ConfigException e) {
			return null;
		}
	}

	public void launchReport(String name, String renderType) {
		launchReport(name, renderType, true);
	}

	public void launchReport(String name, String renderType, boolean twoProcesses) {
		launchReport(name, renderType, twoProcesses, m_frame);
	}

	public void launchReport(String name, String renderType, boolean twoProcesses, Container container) {
		Boolean userAuthorized = false;
		Stocker rolesForReport = m_reportsRolePerms.get(name);
		if (rolesForReport != null) {
			for (String role : m_userRoles) {
				if (rolesForReport.contains(role)) {
					userAuthorized = true;
					break;
				}
			}
		}
		if (userAuthorized) {
			if (renderType == null) {
				renderType = getInputFromUser(container, m_common.jotyLang("SelectBirtFormat"), getBirtRenderTypes(), m_defaultBirtRender);
				if (renderType != null)
					m_applicationPreferences.put("defaultBirtRender", renderType);
				else
					return;
			}
			if (m_webMode) {
				WebClient wClient = m_webClient;
				DocumentDescriptor docDescriptor = wClient.getDocumentFromRespContent(wClient.report(name, renderType, twoProcesses, m_reportManager.m_params));
				if (docDescriptor.xml != null) {
					byte[] m_bytes;
					m_bytes = wClient.getBytesFromRespDocument(docDescriptor, "Report");
					if (m_bytes != null && m_bytes.length > 0)
						openDocumentFromBytes(m_bytes, renderType);
				}
			} else
				birtManager().buildReport(name, renderType, twoProcesses);
		} else
			langWarningMsg("AccessDenied");
	}

	protected Object[] getBirtRenderTypes() {
		return  new Object[] { "pdf", "html", "ods", "xls", "ppt", "doc" };
	}
	
	private boolean loadConfigProperties() {
		boolean retVal = true;
		try {
			boolean extensionAvailable = !m_remoteAccessor;
			ConfigFile extensionConfigFile = m_webMode ? m_configExtension : m_common.m_configuration;
			m_iconImages = new Vector<Image>();
			m_iconImages.add(image(m_common.getConfStr("appIconFile16")));
			m_iconImages.add(image(m_common.getConfStr("appIconFile32")));
			m_frame.setIconImages(m_iconImages);
			m_common.loadConfigProperties(m_webMode);
			if (extensionAvailable) {
				m_common.m_paginationQuery = extensionConfigFile.configTermValue("selectorStatement");
				m_dbmsSessionPreset = extensionConfigFile.configTermValue("dbmsSessionPreset");
			}
			m_JotyDeskLog = m_common.getConfStr("JotyDeskLog");
			m_deskTracing = m_common.getConfBool("deskTracing");
			if (!m_webMode)
				m_debug = m_common.getConfBool("debug");
			if (m_webMode) 
				m_webClient.setAuthServerUrl();
			m_dialogsDesignedOnMac = m_common.getConfBool("dialogsDesignedOnMac");
		} catch (ConfigException e) {
			retVal = false;
		}
		return retVal;
	}

	protected void loadData() {
		openInfoDialog(m_common.jotyLang("LoadingData"));
		loadDescriptions();
		m_jotyLogo = imageIcon("JotyLogo.png");
		
		m_common.loadCalendarElems();

		closeInfoDialog();
	}

	/** Loads in memory the list or the user roles in "Accessor" mode.
	 *
	 * @see org.joty.basicaccessor.BasicAccessor
	 */
	protected void loadUserRoles() {
		boolean old_applicationScopeAccessorMode = m_common.setApplicationScopeAccessorMode();
		WResultSet rs = openAccessorWResultSet("LoadUserRoles", m_paramContext);
		m_common.setApplicationScopeAccessorMode(old_applicationScopeAccessorMode);
		if (rs != null) {
			while (!rs.isEOF()) {
				m_userRoles.add(rs.stringValue("roleName"));
				rs.next();
			}
			rs.close();
		}
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {}

	public boolean m_toolTipsEnabled() {
		return designTime() ? true : (m_mntmEnableTooltips == null ? true : m_mntmEnableTooltips.isSelected());
	}

	/**
	 * On MacOS X this method gets, through reflection, an instance of
	 * com.apple.eawt.Application class and, on it, sets as
	 * {@code ApplicationListener}, again trough reflection, an object created
	 * by means of the {@code Proxy.newProxyInstance} method, such that the
	 * locally defined {@link MacAppListenerInvocationHandler} method can be
	 * specified as the target handler of the listener.
	 * 
	 */
	private void manageMacOSenvironment() {
		Class<?> macApplicationClass;
		try {
			macApplicationClass = Class.forName("com.apple.eawt.Application");
			if (m_macOSXapp == null) {
				m_macOSXapp = macApplicationClass.getConstructor((Class[]) null).newInstance((Object[]) null);
			}
			Class<?> applicationListenerClass = Class.forName("com.apple.eawt.ApplicationListener");
			Method addApplicationListenerMethod = macApplicationClass.getDeclaredMethod("addApplicationListener", new Class[] { applicationListenerClass });
			Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { applicationListenerClass }, new MacAppListenerInvocationHandler());
			addApplicationListenerMethod.invoke(m_macOSXapp, new Object[] { proxy });
		} catch (Exception e) {
			Logger.exceptionToHostLog(e);
		}
	}

	public void map2LdescrArrayTo1L(String firstLevelDescrArrayLiteral, long itemData, String secondLevelDescrArrayLiteral) {
		if (m_2L_literalMap.get(m_common.literalStruct(firstLevelDescrArrayLiteral).m_name) == null)
			m_2L_literalMap.put(m_common.literalStruct(firstLevelDescrArrayLiteral).m_name, new HashMap<Long, String>());
		m_2L_literalMap.get(m_common.literalStruct(firstLevelDescrArrayLiteral).m_name).put(itemData, secondLevelDescrArrayLiteral);
	}

	/** see {@link #userOperation(String, boolean, boolean)} */
	protected boolean mustPasswordBeChanged() {
		return userOperation("mustPasswordBeChanged", false, false);
	}

	/** see {@link #userOperation(String, boolean, boolean)} */
	protected boolean isDateExpired() {
		return userOperation("isDateExpired", false, false);
	}

	/** see {@link #userOperation(String, boolean, boolean)} */
	protected boolean verifyLogin() {
		return userOperation("verifyLogin", true, true);
	}

	protected void verificationProlog() {}

	public JotyDialog onTopDialog() {
		return m_activationStack == null ? null : m_activationStack.size() == 0 ? null : m_activationStack.peek();
	}

	public void openInfoDialog(String message) {
		openInfoDialog(message, null, true);
	}

	public void openInfoDialog(String message, boolean waitCursor) {
		openInfoDialog(message, null, waitCursor);
	} 

	/**
	 * Opens the {@code InfoDialog} object and optionally provides it with the
	 * ability to stop a {@code Thread} object passed as parameter.
	 * 
	 * @param message
	 * @param thread
	 * @param waitCursor
	 * 
	 * @see InfoDialog
	 */
	public void openInfoDialog(String message, Thread thread, boolean waitCursor) {
		if (m_infoDialog == null)
			m_infoDialog = new InfoDialog(m_activationStack.size() > 0 ?  m_activationStack.pop() : m_frame, message, thread);
		m_infoDialog.setLocationRelativeTo(null);
		if (m_dialogsAreToBeForcedOnTop) {
			m_infoDialog.repaint();
			m_infoDialog.setVisible(false);
			m_infoDialog.setVisible(true);
			m_infoDialog.setVisible(false);
		}
		m_infoDialog.setVisible(true);
		m_infoDialog.setText(message);
		m_infoDialog.repaint();
		if (waitCursor)
			setWaitCursor(true);
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(100);
					if (m_infoDialog != null)
						m_infoDialog.repaint();
				} catch (InterruptedException e) {
				}
			}
		};
		new Thread(runnable).start();
	}

	public void closeInfoDialog() {
		closeInfoDialog(true);
	}

	public void closeInfoDialog(boolean waitCursor) {
		if (waitCursor)
			setWaitCursor(false);
		m_infoDialog.setVisible(false);
		m_infoDialog.dispose();
		m_infoDialog = null;
	}

	protected boolean postAuthenticate() {
		return true;
	}

	/**
	 * To be overridden in order to define the users accessibility to the
	 * various reports of the application.
	 * 
	 * @see Application#enableRoleToReport(String, String)
	 */
	protected void registerReports() {}

	public boolean remoteAccessorMode() {
		return m_remoteAccessor && (m_accessorMode || m_common.m_applicationScopeAccessorMode);
	}

	public void repaint() {
		m_frame.paintAll(m_frame.getGraphics());
	}

	private void resize(Dimension target, double factor) {
		target.height = (int) (target.height * factor);
		target.width = (int) (target.width * factor);
	}

	public void rollbackTrans() throws JotyException {
		m_db.rollbackTrans();
	}

	public void setClipboardContents(String aString) {
		StringSelection stringSelection = new StringSelection(aString);
		m_clipboard.setContents(stringSelection, this);
	}

	protected void setDefaultFont() {
		Font defaultFont = null;
		int defaultFontSize = 0;
		try {
			m_applicationDefaultFontSize = m_common.getConfInt("defaultFontSize");
			defaultFontSize = m_applicationDefaultFontSize;
			if (m_workStationFontSize == null)
				m_workStationFontSize = String.valueOf(defaultFontSize);
			else
				defaultFontSize = Integer.parseInt(m_workStationFontSize);
			defaultFont = new Font(m_common.getConfStr("defaultFont"), Font.PLAIN, defaultFontSize);
		} catch (NumberFormatException e) {} catch (ConfigException e) {
			Logger.exceptionToHostLog(e);
		}
		if (defaultFont != null) {
			UIManager.put("Button.font", defaultFont);
			UIManager.put("ToggleButton.font", defaultFont);
			UIManager.put("RadioButton.font", defaultFont);
			UIManager.put("CheckBox.font", defaultFont);
			UIManager.put("ComboBox.font", defaultFont);
			UIManager.put("Label.font", defaultFont);
			UIManager.put("List.font", defaultFont);
			UIManager.put("MenuBar.font", defaultFont);
			UIManager.put("MenuItem.font", defaultFont);
			UIManager.put("CheckBoxMenuItem.font", defaultFont);

			UIManager.put("Menu.font", defaultFont);
			UIManager.put("PopupMenu.font", defaultFont);
			UIManager.put("OptionPane.font", defaultFont);
			UIManager.put("OptionPane.messageFont", defaultFont);
			UIManager.put("OptionPane.buttonFont", defaultFont);
			UIManager.put("Panel.font", defaultFont);
			UIManager.put("ScrollPane.font", defaultFont);
			UIManager.put("TabbedPane.font", defaultFont);
			UIManager.put("Table.font", defaultFont);
			UIManager.put("TableHeader.font", defaultFont);
			UIManager.put("TextField.font", defaultFont);
			UIManager.put("FormattedTextField.font", defaultFont);
			UIManager.put("PasswordField.font", defaultFont);
			UIManager.put("JTextArea.font", defaultFont);
			UIManager.put("TitledBorder.font", defaultFont);
			UIManager.put("ToolTip.font", defaultFont);
		}
	}

	/** see {@link JotyFrame#setAsFloatingBar(boolean)}  */
	public void setMainFrameFloating(boolean truth) {
		m_frame.setAsFloatingBar(truth);
	}

	public boolean setPassword(boolean getOldPassword, String userName) {
		boolean retVal = false;
		ChangePasswordDialog dlg = new ChangePasswordDialog(getOldPassword);
		if (dlg.perform())
			retVal = setPassword(userName, dlg.getNewPassword());
		return retVal;
	}

	/**
	 * Manages the setting of the new password either in into the dbms or into
	 * the list maintained in the user table.
	 * <p>
	 * It sets the {@code m_secure} member to true.
	 * 
	 * @param userName
	 * @param newPassword
	 * @return true on success
	 * @see org.joty.app.Common#m_secure
	 */
	public boolean setPassword(String userName, String newPassword) {
		boolean oldSecureStatus = m_common.m_secure;
		m_common.m_secure = true;
		boolean retVal = setUserPwd(userName, newPassword, "ALTER", m_common.m_password, 0);
		if (retVal) {
			retVal = doSetPassword(userName, newPassword);
			if (retVal) {
				m_common.m_password = newPassword;
				informationMsg(onTopDialog(), m_common.jotyLang("ActionSuccess"));
			} else {
				boolean pwdDbmsResumed = false;
				pwdDbmsResumed = m_db.executeSQL(String.format(m_common.m_dbmsUserPwdStatement, userName, m_common.m_password));
				if (pwdDbmsResumed)
					langWarningMsg("PwdNotChanged");
				else
					jotyMessage("DBMS password instance was changed but the change of database password instance failed !");
			}
		}
		m_common.m_secure = oldSecureStatus;
		return retVal;
	}

	protected void setPasswordValidator(PasswordValidator validator) {
		m_passwordValidator = validator;
	}

	void setServerUrl(String serverHost, String serverPort) {
		m_serverUrl = "http://" + serverHost + ":" + serverPort;
	}

	/**
	 * Got the template from the configuration file, this method forms and
	 * executes the dbms statement to set the user's password
	 */
	public boolean setUserPwd(String userName, String newPwd, String command, String oldPwd, int nonManagedRollbackIndex) {
		boolean retVal = false;
		boolean specializedStmnt = command.compareTo("ALTER") == 0 && m_common.m_dbmsChangePwdStatement != null;
		String templateStmnt = specializedStmnt ? m_common.m_dbmsChangePwdStatement : m_common.m_dbmsUserPwdStatement;
		if (templateStmnt != null) {
			String sql = specializedStmnt ? String.format(templateStmnt, userName, newPwd, oldPwd) : String.format(templateStmnt, command, userName, newPwd);
			retVal = m_db.executeSQL(sql, null, null, nonManagedRollbackIndex);
		}
		return retVal;
	}

	/**
	 * Allow the administrator user to set the font size of the workstation or
	 * to reset it to the centralized value.
	 */
	public void setWorkStationFontSize() {
		String choice = getInputFromUser(m_frame, m_common.jotyLang("ChangeStationFontSize"), new Object[] { "Default", "8", "9", "10", "11", "12", "13" }, m_workStationFontSize);
		if (choice != null) {
			m_workStationFontSize = choice.equals("Default") ? String.valueOf(m_applicationDefaultFontSize) : choice;
			m_applicationPreferences.put("workStationFontSize", m_workStationFontSize);
			informationMsg(String.format(m_common.jotyLang("OptionAcquired"), m_common.jotyLang("NewFontSize")));
		}
	}

	protected void showAboutDialog() {
		AboutDialog dlg = new AboutDialog();
		dlg.perform();
	}

	public void turnTracingOff() {
		m_tracing = false;
	}

	public void turnTracingOn() {
		m_tracing = true;
	}

	public String userHomeDataPath() {
		return System.getProperty("user.home") + "/" + m_userHomeDataDir + "/" + m_name;
	}

	/**
	 * Multi-purpose wrapper for invoking methods, defined in the
	 * {@code BasicAccessor} class, that are related to the management of the
	 * user data the container name of which, also, is stored in the definition
	 * of that class.
	 * <p>
	 * The method supports the shared mode of the Joty application.
	 * 
	 * @param methodName
	 *            the name of methos defined in the BasicAccessor class
	 * @param login
	 *            true if it is a login operation
	 * @param retCodeAsLangLiteral
	 *            if true code returned from the invoked method is treated as a
	 *            {@code LangLiteralRetCodeMapper} object to be conveniently
	 *            decoded.
	 * @return true on success
	 * 
	 * @see BasicPostStatement
	 * @see LangLiteralRetCodeMapper
	 * @see org.joty.app.Common#sharingClause()
	 * @see org.joty.basicaccessor.BasicAccessor
	 */
	protected boolean userOperation(String methodName, boolean login, boolean retCodeAsLangLiteral) {
		boolean retVal = false;
		beginWaitCursor();
		BasicPostStatement postStatement = accessorMethodPostStatement(methodName, 1, -1);
		postStatement.addItem("userName", m_common.m_userName, Item._text);
		if (login)
			postStatement.addItem("password", m_common.m_password, Item._text);
		if (m_common.m_shared)
			postStatement.addItem("sharingKey", m_common.m_sharingKey, Item._text);
		boolean inquirySuccess = invokeAccessMethod(postStatement);
		endWaitCursor();
		if (inquirySuccess) {
			Long retCode = Long.parseLong(postStatement.m_retVal);
			if (retCode == 0)
				retVal = true;
			else if (retCodeAsLangLiteral)
				langWarningMsg(m_langLiteralRetCodeMapper.literal(retCode));
		}
		return retVal;
	}

	/**
	 * Address the login process to be performed on the Joty Server directing it
	 * through the use of the basic part of the Accessor object.
	 * 
	 * @return true on success
	 * 
	 * @see WebClient
	 * @see org.joty.basicaccessor.BasicAccessor
	 */
	protected boolean webAuthentication() {
		boolean bRet = false;
		bRet = m_webClient.login(null);
		if (bRet)
			m_common.m_webSessionOn = true;
		return bRet;
	}

	public UsersPanel createUserPanel() {
		return new UsersPanel();
	}

	@Override
	public void jotyWarning(String text) {
		Logger.appendToHostLog(text);		
	}

	@Override
	public void jotyMessage(String text) {
		Application.m_app.JotyMsg(this, text);
	}

	@Override
	public void jotyMessage(Throwable t) {
		Logger.throwableToHostLog(t);	
	}

	@Override
	public void jotyMessage(Exception e) {
		Logger.exceptionToHostLog(e);	
	}

	@Override
	public  void ASSERT(boolean predicate) {
		if (m_debug)
			if (!predicate) {
				String text = "ASSERTION VIOLATED";
				JotyMsg(m_app, text);
				Logger.stackTraceToHostLog(text);
			}
	}

	@Override
	public JotyDate createDate() {
		return new JotyDate(this);
	}

	@Override
	public boolean debug() {
		return m_debug;
	}


	@Override
	public boolean isDesignTime() {
		return Beans.isDesignTime();
	}

	@Override
	public WrappedField createWrappedField() {
		return new WField(this);
	}

	@Override
	public String localFilesPath() {
		return userHomeDataPath();
	}

	@Override
	public ICommon getCommon() {
		return m_common;
	}

	@Override
	public String getKeyStoreType() {
		return "JKS";
	}

	@Override
	public void firstChanceKeyStore() throws Throwable {
		throw new Throwable();
	}

	@Override
	public String keyStorePath() {
		Utilities.checkDirectory(userHomeDataPath() + m_ksPath);
		return userHomeDataPath() + m_ksPath + "/" + m_ksFileName;
	}

	@Override
	public LiteralsCollection instantiateLiteralsCollection(JotyMessenger jotyMessanger) {
		return new LiteralStruct(jotyMessanger);
	}

	@Override
	public void volatileMessage(String langLiteral, boolean appSpecific) {
		m_app.openInfoDialog(m_common.langMessage(langLiteral, appSpecific));	
	}

	@Override
	public AbstractWebClient getWebClient() {
		return m_webClient;
	}

	@Override
	public void constraintViolationMsg(boolean onUpdate, JotyException jotyException) {
		if (onUpdate)
			onTopDialog().m_currSheet.costraintViolationMsgOnUpdate();
		else
			onTopDialog().m_currSheet.costraintViolationMsgOnDelete();
	}

	@Override
	public void manageExpiredSession() {
		if (m_definingDialog != null)
			m_definingDialog = null;
		Application.langWarningMsg("SessionExpMustLogon");
		if (authenticate(true))
			Application.langInformationMsg("NowIsPossible");
		else
			exit();
	}

	@Override
	public boolean designTime() {
		return Beans.isDesignTime();
	}

	@Override
	public void beforeReportRender() {
		setWaitCursor(true);
		openInfoDialog(((Common) getCommon()).jotyLang("InitializingRepEngineMsg"));
	}
	
	@Override
	public void afterReportRender(String location) {
		((JotyApplication) m_app).openUri(location, false);
		((JotyApplication) m_app).closeInfoDialog();
		setWaitCursor(false);
	}

}
