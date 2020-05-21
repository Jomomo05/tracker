/*
 * The tracker package defines a set of video/image analysis tools
 * built on the Open Source Physics framework by Wolfgang Christian.
 *
 * Copyright (c) 2019  Douglas Brown
 *
 * Tracker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tracker; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at <http://www.gnu.org/copyleft/gpl.html>
 *
 * For additional Tracker information and documentation, please see
 * <http://physlets.org/tracker/>.
 */
package org.opensourcephysics.cabrillo.tracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.Document;

import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XMLProperty;
import org.opensourcephysics.display.OSPFrame;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.media.core.BaselineFilter;
import org.opensourcephysics.media.core.BrightnessFilter;
import org.opensourcephysics.media.core.ClipInspector;
import org.opensourcephysics.media.core.DarkGhostFilter;
import org.opensourcephysics.media.core.DataTrack;
import org.opensourcephysics.media.core.DeinterlaceFilter;
import org.opensourcephysics.media.core.GhostFilter;
import org.opensourcephysics.media.core.GrayScaleFilter;
import org.opensourcephysics.media.core.MediaRes;
import org.opensourcephysics.media.core.NegativeFilter;
import org.opensourcephysics.media.core.PerspectiveFilter;
import org.opensourcephysics.media.core.RadialDistortionFilter;
import org.opensourcephysics.media.core.ResizeFilter;
import org.opensourcephysics.media.core.RotateFilter;
import org.opensourcephysics.media.core.StrobeFilter;
import org.opensourcephysics.media.core.SumFilter;
import org.opensourcephysics.media.core.Video;
import org.opensourcephysics.media.core.VideoFileFilter;
import org.opensourcephysics.media.core.VideoIO;
import org.opensourcephysics.media.core.VideoPanel;
import org.opensourcephysics.media.core.VideoPlayer;
import org.opensourcephysics.media.mov.MovieVideoI;
import org.opensourcephysics.tools.FontSizer;
import org.opensourcephysics.tools.LaunchPanel;
import org.opensourcephysics.tools.Launcher;
import org.opensourcephysics.tools.Launcher.HTMLPane;
import org.opensourcephysics.tools.LibraryBrowser;
import org.opensourcephysics.tools.LibraryComPADRE;
import org.opensourcephysics.tools.LibraryResource;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

import javajs.async.SwingJSUtils.Performance;
import javajs.async.SwingJSUtils.StateHelper;
import javajs.async.SwingJSUtils.StateMachine;

/**
 * This is the main frame for Tracker.
 *
 * @author Douglas Brown
 */
@SuppressWarnings("serial")
public class TFrame extends OSPFrame implements PropertyChangeListener {

	// static fields
	
	public static final String PROPERTY_TFRAME_TAB = "tab";
	public static final String PROPERTY_TFRAME_RADIANANGLES = "radian_angles";
	public static final String PROPERTY_TFRAME_WINDOWFOCUS = "windowfocus";

	protected final static String helpPath = "/org/opensourcephysics/cabrillo/tracker/resources/help/"; //$NON-NLS-1$
	protected final static String helpPathWeb = "https://physlets.org/tracker/help/"; //$NON-NLS-1$
	protected final static Color yellow = new Color(255, 255, 105);

//	private final static int VIEW_PLOT = 0;
//	private final static int VIEW_TABLE = 1;
//	private final static int VIEW_WORLD = 2;
//	private final static int VIEW_TEXT = 3;

	// instance fields
	private JToolBar playerBar;
	private JPopupMenu popup = new JPopupMenu();
	private JMenuItem closeItem;
	private JMenuBar defaultMenuBar;
	private JMenu recentMenu;

	private static final int PANEL_MAINVIEW = 0;
	private static final int PANEL_VIEWS = 1;
	private static final int PANEL_SPLITPANES = 2;
	private static final int PANEL_TOOLBAR = 3;
	private static final int PANEL_MENUBAR = 4;
	private static final int PANEL_TRACKBAR = 5;
	
	public static boolean haveExportDialog = false;
	public static boolean haveThumbnailDialog = false;

	private Map<JPanel, Object[]> panelObjects = new HashMap<JPanel, Object[]>();
	protected JTabbedPane tabbedPane;
	protected JTextPane notesTextPane;
	protected Action saveNotesAction;
	protected JButton cancelNotesDialogButton, closeNotesDialogButton;
	protected JCheckBox displayWhenLoadedCheckbox;
	protected JDialog notesDialog;
	protected JDialog helpDialog;
	protected LibraryBrowser libraryBrowser;
	protected Launcher helpLauncher;
	protected JDialog dataToolDialog;
	protected TrackerPanel prevPanel;
	protected double defaultRightDivider = 0.7;
	protected double defaultBottomDivider = 0.5;
	protected FileDropHandler fileDropHandler;
	protected Action openRecentAction;
	protected boolean splashing = true;
	protected ArrayList<String> loadedFiles = new ArrayList<String>();
	protected boolean anglesInRadians = Tracker.isRadians;
	protected File tabsetFile; // used when saving tabsets
	protected int framesLoaded, prevFramesLoaded; // used when loading xuggle videos
//  protected JProgressBar monitor;
	protected PrefsDialog prefsDialog;
	protected ClipboardListener clipboardListener;
	protected boolean alwaysListenToClipboard;

	/**
	 * Constructs an empty TFrame.
	 */
	public TFrame() {
		super("Tracker"); //$NON-NLS-1$
		setName("Tracker"); //$NON-NLS-1$
		if (Tracker.TRACKER_ICON != null)
			setIconImage(Tracker.TRACKER_ICON.getImage());
		// set default close operation
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		createGUI();
		// size and position this frame
		pack();

		// set transfer handler on tabbedPane
		fileDropHandler = new FileDropHandler(this);
		tabbedPane.setTransferHandler(fileDropHandler);

		// set size and limit maximized size so taskbar not covered
		GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Rectangle screenRect = e.getMaximumWindowBounds();
		setMaximizedBounds(screenRect);
		double extra = FontSizer.getFactor(Tracker.preferredFontLevel) - 1;
		int w = Math.min(screenRect.width, (int) (1024 + extra * 800));
		int h = Math.min(screenRect.height, 3 * w / 4);
		Dimension dim = new Dimension(w, h);
		setSize(dim);
		// center frame on the screen
		int x = (screenRect.width - dim.width) / 2;
		int y = (screenRect.height - dim.height) / 2;
		setLocation(x, y);
		TrackerRes.addPropertyChangeListener("locale", this); //$NON-NLS-1$
	}

	/**
	 * Constructs a TFrame with the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 */
	public TFrame(TrackerPanel trackerPanel) {
		this();
		addTab(trackerPanel, null);
	}

	/**
	 * All repaints funnel through this method
	 * 
	 */
	@Override
	public void repaint(long time, int x, int y, int w, int h) {
		if (!isVisible())
			return;
		OSPLog.debug("TFrame repaint");
		// TFrame.addTab -> initialize -> TrackerPanel.addTrack -> fire(PROPERTY_TRACKERPANEL_TRACK) 
		//   -> TViewChooser -> PlotTView -> TFrame.repaint();
		
		
		// Window.resize -> BorderLayout.layoutContainer -> JRootPane.reshape -> TFrame.repaint()
		
		super.repaint(time, x, y, w, h);
	}
	
	/**
	 * Swing does not use this method. It's only for AWT.
	 * 
	 */
	@Override
	public void update(Graphics g) {
		super.paint(g);
	}

	@Override
	public void paint(Graphics g) {
		// RepaintManager.paintDirtyRegions -> TFrame.paint(g)
		super.paint(g);
	}
	
	/**
	 * Adds a tab that displays the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @param whenDone
	 */
	public void addTab(final TrackerPanel trackerPanel, Runnable whenDone) {
		int tab = getTab(trackerPanel);
		if (tab>-1) { // tab exists
			String name = trackerPanel.getTitle();
			synchronized (tabbedPane) {
				tabbedPane.setTitleAt(tab, name);
				tabbedPane.setToolTipTextAt(tab, trackerPanel.getToolTipPath());
			}
			OSPLog.debug("AddTab existing tab " +tab);
		} else {
			OSPLog.debug("AddTab new tab");
			// tab does not already exist
			// listen for changes that affect tab title
			trackerPanel.addPropertyChangeListener(VideoPanel.PROPERTY_VIDEOPANEL_DATAFILE, this); //$NON-NLS-1$
			trackerPanel.addPropertyChangeListener(TrackerPanel.PROPERTY_TRACKERPANEL_VIDEO, this); //$NON-NLS-1$
			// set up trackerPanel to listen for angle format property change
			addPropertyChangeListener(PROPERTY_TFRAME_RADIANANGLES, trackerPanel); //$NON-NLS-1$
			// create the tab
			JPanel panel = new JPanel(new BorderLayout());
			// create the tab panel components
			Tracker.setProgress(30);
			MainTView mainView = getMainView(trackerPanel);
			Container[] views = createViews(trackerPanel);
			JSplitPane[] panes = getSplitPanes(trackerPanel);
			Tracker.setProgress(50);
			TToolBar toolbar = getToolBar(trackerPanel);
			Tracker.setProgress(60);
			TMenuBar menubar = getMenuBar(trackerPanel);
			TTrackBar trackbar = getTrackBar(trackerPanel);
			// put the components into the tabs map
			Object[] objects = new Object[] { mainView, views, panes, toolbar, menubar, trackbar };
			panelObjects.put(panel, objects);
			// add the tab
			setIgnoreRepaint(true);
			String name = trackerPanel.getTitle();
			synchronized (tabbedPane) {
				tabbedPane.addTab(name, panel);
				tab = getTab(trackerPanel);
				tabbedPane.setToolTipTextAt(tab, trackerPanel.getToolTipPath());
			}
			// from now on trackerPanel's top level container is this TFrame,
			// so trackerPanel.getFrame() method will return non-null

//			boolean hasViews = (trackerPanel.viewsProperty != null);
//			// select the views
//			if (trackerPanel.selectedViewsProperty != null) {
//				if (!hasViews) { // no custom views, just load and show selected views
//					XMLControl[] controls = trackerPanel.selectedViewsProperty.getChildControls();
//					for (int i = 0; i < Math.min(controls.length, views.length); i++) {
//						if (controls[i] == null)
//							continue;
//						if (views[i] instanceof TViewChooser) {
//							TViewChooser chooser = (TViewChooser) views[i];
//							Class<?> viewType = controls[i].getObjectClass();
//							TView view = chooser.getView(viewType);
//							if (view != null) {
//								TView tview = chooser.getSelectedView();
//								if (tview != null && tview != view) {
//									chooser.setSelectedView(view);
//								}
//								controls[i].loadObject(view);
//							}
//						}
//					}
//				} else { // has custom views
//					Iterator<Object> it = trackerPanel.selectedViewsProperty.getPropertyContent().iterator();
//					int i = -1;
//					while (it.hasNext() && ++i < views.length) {
//						if (views[i] instanceof TViewChooser) {
//							TViewChooser chooser = (TViewChooser) views[i];
//							XMLProperty next = (XMLProperty) it.next();
//							if (next == null)
//								continue;
//							String viewName = (String) next.getPropertyContent().get(0);
//							chooser.setSelectedView(chooser.getView(viewName));
//						}
//					}
//				}
//				trackerPanel.selectedViewsProperty = null;
//			}
//			// load the views
//			if (hasViews) {
//				java.util.List<Object> arrayItems = trackerPanel.viewsProperty.getPropertyContent();
//				Iterator<Object> it = arrayItems.iterator();
//				while (it.hasNext()) {
//					XMLProperty next = (XMLProperty) it.next();
//					if (next == null)
//						continue;
//					String index = next.getPropertyName().substring(1);
//					index = index.substring(0, index.length() - 1);
//					int i = Integer.parseInt(index);
//					if (i < views.length && views[i] instanceof TViewChooser) {
//						XMLControl[] elements = next.getChildControls();
//						TViewChooser chooser = (TViewChooser) views[i];
//						for (int j = 0; j < elements.length; j++) {
//							Class<?> viewType = elements[j].getObjectClass();
//							TView view = chooser.getView(viewType);
//							if (view != null) {
//								elements[j].loadObject(view);
//							}
//						}
//					}
//				}
//				trackerPanel.viewsProperty = null;
//			}
//			setViews(trackerPanel, views);
//			initialize(trackerPanel);
//			
//			FontSizer.setFonts(panel);
//			// inform all tracks of current angle display format
//			for (TTrack track : trackerPanel.getTracks()) {
//				track.setAnglesInRadians(anglesInRadians);
//			}
//			setIgnoreRepaint(false);
//			trackerPanel.changed = false;

//			Timer timer = new Timer(500, new ActionListener() {
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					// close blank tab at position 0, if any
//					if (getTabCount() > 1) {
//						TrackerPanel existingPanel = getTrackerPanel(0);
//						if (tabbedPane.getTitleAt(0).equals(TrackerRes.getString("TrackerPanel.NewTab.Name")) //$NON-NLS-1$
//								&& !existingPanel.changed) {
//							removeTab(existingPanel);
//						}
//					}
//					trackerPanel.refreshTrackData();
//					refresh();
//				}
//			});
//			timer.setRepeats(false);
//			timer.start();
		}
		
		// set views, etc for every trackerPanel, not just new ones
		Container[] views = getViews(trackerPanel);
		boolean hasViews = (trackerPanel.viewsProperty != null);
		// select the views
		if (trackerPanel.selectedViewsProperty != null) {
			if (!hasViews) { // no custom views, just load and show selected views
				XMLControl[] controls = trackerPanel.selectedViewsProperty.getChildControls();
				for (int i = 0; i < Math.min(controls.length, views.length); i++) {
					if (controls[i] == null)
						continue;
					if (views[i] instanceof TViewChooser) {
						TViewChooser chooser = (TViewChooser) views[i];
						Class<?> viewType = controls[i].getObjectClass();
						TView view = chooser.getView(viewType);
						if (view != null) {
							TView tview = chooser.getSelectedView();
							if (tview != null && tview != view) {
								chooser.setSelectedView(view);
							}
							controls[i].loadObject(view);
						}
					}
				}
			} else { // has custom views
				Iterator<Object> it = trackerPanel.selectedViewsProperty.getPropertyContent().iterator();
				int i = -1;
				while (it.hasNext() && ++i < views.length) {
					if (views[i] instanceof TViewChooser) {
						TViewChooser chooser = (TViewChooser) views[i];
						XMLProperty next = (XMLProperty) it.next();
						if (next == null)
							continue;
						String viewName = (String) next.getPropertyContent().get(0);
						chooser.setSelectedView(chooser.getView(viewName));
					}
				}
			}
			trackerPanel.selectedViewsProperty = null;
		}
		// load the views
		if (hasViews) {
			java.util.List<Object> arrayItems = trackerPanel.viewsProperty.getPropertyContent();
			Iterator<Object> it = arrayItems.iterator();
			while (it.hasNext()) {
				XMLProperty next = (XMLProperty) it.next();
				if (next == null)
					continue;
				String index = next.getPropertyName().substring(1);
				index = index.substring(0, index.length() - 1);
				int i = Integer.parseInt(index);
				if (i < views.length && views[i] instanceof TViewChooser) {
					XMLControl[] elements = next.getChildControls();
					TViewChooser chooser = (TViewChooser) views[i];
					for (int j = 0; j < elements.length; j++) {
						Class<?> viewType = elements[j].getObjectClass();
						TView view = chooser.getView(viewType);
						if (view != null) {
							elements[j].loadObject(view);
						}
					}
				}
			}
			trackerPanel.viewsProperty = null;
		}
		setViews(trackerPanel, views);
		initialize(trackerPanel);
			
		JPanel panel = (JPanel)tabbedPane.getComponentAt(tab);
		FontSizer.setFonts(panel);
		// inform all tracks of current angle display format
		for (TTrack track : trackerPanel.getTracks()) {
			track.setAnglesInRadians(anglesInRadians);
		}
		setIgnoreRepaint(false);
		trackerPanel.changed = false;
		trackerPanel.refreshTrackData();
		refresh();


		if (whenDone != null) {
			whenDone.run();
		}
	}

	/**
	 * Removes all tabs.
	 */
	public void removeAllTabs() {
		// remove all tabs
		for (int i = getTabCount() - 1; i >= 0; i--) {
			// save/close tabs in try/catch block so always closes
			try {
				if (!getTrackerPanel(i).save()) {
					// action is cancelled
					return;
				}
				removeTab(getTrackerPanel(i));
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * Removes the tab that displays the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 */
	public void removeTab(TrackerPanel trackerPanel) {
		OSPLog.debug(Performance.timeCheckStr("TFrame.removeTab start", Performance.TIME_MARK));
		long t0 = Performance.now(0);
		int tab = getTab(trackerPanel);
		if (tab == -1)
			return; // tab doesn't exist
		if (!trackerPanel.save())
			return; // user cancelled

		trackerPanel.selectedPoint = null;
		trackerPanel.selectedStep = null;
		trackerPanel.selectedTrack = null;

		// hide the info dialog if trackerPanel is in selected tab
		if (tab == getSelectedTab()) {
			notesDialog.setVisible(false);
		}

		// inform listeners
		firePropertyChange(PROPERTY_TFRAME_TAB, trackerPanel, null); //$NON-NLS-1$

		// clean up mouse handler
		trackerPanel.mouseHandler.selectedTrack = null;
		trackerPanel.mouseHandler.p = null;
		trackerPanel.mouseHandler.iad = null;

		// clear filter classes
		trackerPanel.clearFilters();
		// remove transfer handler
		trackerPanel.setTransferHandler(null);

		// remove property change listeners
		trackerPanel.removePropertyChangeListener("datafile", this); //$NON-NLS-1$
		trackerPanel.removePropertyChangeListener("video", this); //$NON-NLS-1$
		removePropertyChangeListener("radian_angles", trackerPanel); //$NON-NLS-1$

		// dispose of the track control, clip inspector and player bar
		TrackControl.getControl(trackerPanel).dispose();
		ClipInspector ci = trackerPanel.getPlayer().getVideoClip().getClipInspector();
		if (ci != null) {
			ci.dispose();
		}

		// set the video to null
		trackerPanel.setVideo(null);

		// dispose of TViewChoosers and TViews
		Container[] views = getViews(trackerPanel);
		for (int i = 0; i < views.length; i++) {
			if (views[i] instanceof TViewChooser) {
				TViewChooser chooser = (TViewChooser) views[i];
				chooser.dispose();
			}
		}

		// clean up main view
		MainTView mainView = getMainView(trackerPanel);
		mainView.dispose();
		trackerPanel.setScrollPane(null);

		// clear the drawables AFTER disposing of main view
		ArrayList<TTrack> tracks = trackerPanel.getTracks();
		trackerPanel.clear();
		for (TTrack track : tracks) {
			track.dispose();
		}

		// get the tab panel and remove components from it
		JPanel tabPanel = (JPanel) tabbedPane.getComponentAt(tab);
		tabPanel.removeAll();

		// remove the tab
		synchronized (tabbedPane) {
			tabbedPane.remove(tabPanel);
		}

		// dispose of trackbar, toolbar, menubar AFTER removing tab
		TToolBar toolbar = getToolBar(trackerPanel);
		toolbar.dispose();
		TMenuBar menubar = getMenuBar(trackerPanel);
		menubar.dispose();
		TTrackBar trackbar = getTrackBar(trackerPanel);
		trackbar.dispose();
		JSplitPane[] panes = getSplitPanes(trackerPanel);
		for (int i = 0; i < panes.length; i++) {
			JSplitPane pane = panes[i];
			pane.removeAll();
		}
		for (int i = 0; i < panes.length; i++) {
			panes[i] = null;
		}

		// remove the components from the tabs map
		Object[] objects = panelObjects.get(tabPanel);
//  objects is {mainView, views, panes, toolbar, menubar, trackbar};
		if (objects != null) {
			for (int i = 0; i < objects.length; i++) {
				objects[i] = null;
			}
		}
		panelObjects.remove(tabPanel);

		TActions.getActions(trackerPanel).clear();
		TActions.actionMaps.remove(trackerPanel);
		if (prefsDialog != null) {
			prefsDialog.trackerPanel = null;
		}
		Undo.undomap.remove(trackerPanel);

		trackerPanel.dispose();

		// change menubar and show floating player of newly selected tab, if any
		objects = panelObjects.get(tabbedPane.getSelectedComponent());
		if (objects != null) {
			setJMenuBar((JMenuBar) objects[PANEL_MENUBAR]);
			((TTrackBar) objects[PANEL_TRACKBAR]).refresh();
			playerBar = ((MainTView) objects[PANEL_MAINVIEW]).getPlayerBar();
			Container frame = playerBar.getTopLevelAncestor();
			if (frame != null && frame != TFrame.this)
				frame.setVisible(true);
		} else {
			// show defaultMenuBar
			refreshOpenRecentMenu(recentMenu);
			setJMenuBar(defaultMenuBar);
		}
		
		OSPLog.debug("!!! " + Performance.now(t0) + " TFrame.removeTab");
		OSPLog.debug(Performance.timeCheckStr("TFrame.removeTab end", Performance.TIME_MARK));

	}

	/**
	 * Returns the tab index for the specified tracker panel, or -1 if no tab is
	 * found.
	 *
	 * @param trackerPanel the tracker panel
	 * @return the tab index
	 */
	public int getTab(TrackerPanel trackerPanel) {
		for (int i = 0; i < getTabCount(); i++) {
			Object[] objects = panelObjects.get(tabbedPane.getComponentAt(i));
			if (objects == null)
				return -1;
			MainTView mainView = (MainTView) objects[PANEL_MAINVIEW];
			if (mainView.getTrackerPanel() == trackerPanel) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the tab index for the specified data file, or -1 if no tab is found.
	 *
	 * @param dataFile the data file used to load the tab
	 * @return the tab index
	 */
	public int getTab(File dataFile) {
		if (dataFile == null)
			return -1;
		try {
			String path = dataFile.getCanonicalPath();
			for (int i = getTabCount() - 1; i >= 0; i--) {
				Object[] objects = panelObjects.get(tabbedPane.getComponentAt(i));
				if (objects == null)
					return -1;
				MainTView mainView = (MainTView) objects[PANEL_MAINVIEW];
				File file = mainView.getTrackerPanel().getDataFile();
				if (file != null && path.equals(file.getCanonicalPath())) {
					return i;
				}
			}
		} catch (IOException e) {
		}
		return -1;
	}

	/**
	 * Gets the tab count.
	 *
	 * @return the tab count
	 */
	public int getTabCount() {
		return tabbedPane.getTabCount();
	}

	/**
	 * Gets the selected tab index.
	 *
	 * @return the tab index
	 */
	public int getSelectedTab() {
		return tabbedPane.getSelectedIndex();
	}

	/**
	 * Sets the selected tab index.
	 *
	 * @param tab the tab index
	 */
	public void setSelectedTab(int tab) {
		if (tab < 0 || tab >= getTabCount())
			return;
		tabbedPane.setSelectedIndex(tab);
		TrackerPanel trackerPanel = getTrackerPanel(tab);
		if (trackerPanel != null)
			trackerPanel.refreshNotesDialog();
	}

	/**
	 * Sets the selected tab specified by tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 */
	public void setSelectedTab(TrackerPanel trackerPanel) {
		setSelectedTab(getTab(trackerPanel));
	}

	/**
	 * Gets the tracker panel at the specified tab index.
	 *
	 * @param tab the tab index
	 * @return the tracker panel
	 */
	public TrackerPanel getTrackerPanel(int tab) {
		if (tab < 0 || tab >= tabbedPane.getTabCount())
			return null;
		Object[] objects = panelObjects.get(tabbedPane.getComponentAt(tab));
		MainTView mainView = (MainTView) objects[PANEL_MAINVIEW];
		return mainView.getTrackerPanel();
	}

	/**
	 * Gets the title of the specified tab.
	 *
	 * @param tab the tab index
	 * @return the title
	 */
	public String getTabTitle(int tab) {
		return tabbedPane.getTitleAt(tab);
	}

	/**
	 * Refreshes the tab for the specified tracker panel.
	 *
	 * @param panel the tracker panel
	 */
	public void refreshTab(TrackerPanel panel) {
		int tab = getTab(panel);
		tabbedPane.setTitleAt(tab, panel.getTitle());
		tabbedPane.setToolTipTextAt(tab, panel.getToolTipPath());
	}

	/**
	 * Sets the title of the specified tab.
	 *
	 * @param tab   the tab index
	 * @param title the title
	 */
	public void setTabTitle(int tab, String title) {
		tabbedPane.setTitleAt(tab, title);
	}

	/**
	 * Sets the view for a specified tracker panel and pane.
	 *
	 * @param trackerPanel the tracker panel
	 * @param view         the new view
	 * @param pane         the pane number
	 */
	public void setView(TrackerPanel trackerPanel, Container view, int pane) {
		if (view == null || pane > 3)
			return;
		Container[] views = getViews(trackerPanel);
		views[pane] = view;
		setViews(trackerPanel, views);
	}

	/**
	 * Sets the views for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @param newViews     an array of up to 4 views
	 */
	public void setViews(TrackerPanel trackerPanel, Container[] newViews) {
		if (newViews == null)
			newViews = new Container[0];
		int tab = getTab(trackerPanel);
		JPanel panel = (JPanel) tabbedPane.getComponentAt(tab);
		panel.removeAll();
		Object[] objects = panelObjects.get(panel);
		Container[] views = (Container[]) objects[PANEL_VIEWS];
		for (int i = 0; i < Math.min(newViews.length, views.length); i++) {
			if (newViews[i] != null)
				views[i] = newViews[i];
		}
		objects[PANEL_VIEWS] = views;
		MainTView mainView = (MainTView) objects[PANEL_MAINVIEW];
		JSplitPane[] panes = (JSplitPane[]) objects[PANEL_SPLITPANES];
		panel.add(panes[0], BorderLayout.CENTER);
		panes[0].setLeftComponent(panes[2]);
		panes[0].setRightComponent(panes[1]);
		panes[2].setTopComponent(mainView);
		panes[2].setBottomComponent(panes[3]);
		panes[1].setTopComponent(views[0]);
		panes[1].setBottomComponent(views[1]);
		panes[3].setRightComponent(views[2]);
		panes[3].setLeftComponent(views[3]);
		// add toolbars at north position
		Box north = Box.createVerticalBox();
		north.add((JToolBar) objects[PANEL_TOOLBAR]);
		north.add((TTrackBar) objects[PANEL_TRACKBAR]);
		panel.add(north, BorderLayout.NORTH);
	}

	/**
	 * Gets the views for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @return an objects of views
	 */
	public Container[] getViews(TrackerPanel trackerPanel) {
		Object[] objects = getObjects(trackerPanel);
		if (objects == null)
			return new Container[4];
		Container[] views = (Container[]) objects[PANEL_VIEWS];
		return views.clone();
	}

	/**
	 * Gets the views for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @return an array of views
	 */
	public TView[][] getTViews(TrackerPanel trackerPanel) {
		return getTViews(trackerPanel, false);
	}

	/**
	 * Gets the views for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @param customOnly   true to return only customized views
	 * @return an array of views
	 */
	public TView[][] getTViews(TrackerPanel trackerPanel, boolean customOnly) {
		Container[] views = getViews(trackerPanel);
		TView[][] array = new TView[views.length][];
		for (int i = 0; i < array.length; i++) {
			if (views[i] instanceof TViewChooser) {
				TViewChooser chooser = (TViewChooser) views[i];
				Collection<TView> c = chooser.getViews();
				array[i] = new TView[c.size()];
				boolean empty = true;
				Iterator<TView> it = c.iterator();
				for (int j = 0; j < c.size(); j++) {
					TView next = it.next();
					if (!customOnly || next.isCustomState()) {
						array[i][j] = next;
						empty = false;
					}
				}
				if (empty)
					array[i] = null;
			}
		}
		return array;
	}

	/**
	 * Gets the selected TViews for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @return an array of TViews (some elements may be null)
	 */
	public String[] getSelectedTViews(TrackerPanel trackerPanel) {
		Container[] views = getViews(trackerPanel);
		String[] selectedViews = new String[views.length];
		for (int i = 0; i < selectedViews.length; i++) {
			if (views[i] instanceof TViewChooser) {
				TViewChooser chooser = (TViewChooser) views[i];
				TView tview = chooser.getSelectedView();
				selectedViews[i] = tview == null ? null : tview.getViewName();
			}
		}
		return selectedViews;
	}

	/**
	 * Determines whether a view is open for the specified tracker panel.
	 *
	 * @param index        the view index
	 * @param trackerPanel the tracker panel
	 * @return true if it is open
	 */
	public boolean isViewOpen(int index, TrackerPanel trackerPanel) {
		JSplitPane[] panes = getSplitPanes(trackerPanel);
		double[] locs = new double[panes.length];
		for (int i = 0; i < panes.length; i++) {
			int max = panes[i].getMaximumDividerLocation();
			locs[i] = 1.0 * panes[i].getDividerLocation() / max;
		}
		switch (index) {
		case 0:
			return locs[0] < 0.95 && locs[1] > 0.05;
		case 1:
			return locs[0] < 0.95 && locs[1] < 0.95;
		case 2:
			return locs[2] < 0.95 && locs[3] < 0.95;
		case 3:
			return locs[2] < 0.95 && locs[3] > 0.05;
		}
		return false;
	}

	/**
	 * Sets the location of a splitpane divider for a tracker panel
	 *
	 * @param trackerPanel the tracker panel
	 * @param paneIndex    the index of the split pane
	 * @param loc          the desired relative divider location
	 */
	public void setDividerLocation(TrackerPanel trackerPanel, int paneIndex, double loc) {
		JSplitPane[] panes = getSplitPanes(trackerPanel);
		if (paneIndex < panes.length) {
			panes[paneIndex].setDividerLocation(loc);
			validate();
		}
	}

	/**
	 * Sets the location of a splitpane divider for a tracker panel
	 *
	 * @param trackerPanel the tracker panel
	 * @param paneIndex    the index of the split pane
	 * @param loc          the desired absolute divider location
	 */
	public void setDividerLocation(TrackerPanel trackerPanel, int paneIndex, int loc) {
		JSplitPane[] panes = getSplitPanes(trackerPanel);
		if (paneIndex < panes.length) {
			panes[paneIndex].setDividerLocation(loc);
			validate();
		}
	}

	/**
	 * Gets a splitpane for a tracker panel
	 *
	 * @param trackerPanel the tracker panel
	 * @param paneIndex    the index of the split pane
	 * @return the splitpane
	 */
	JSplitPane getSplitPane(TrackerPanel trackerPanel, int paneIndex) {
		JSplitPane[] panes = getSplitPanes(trackerPanel);
		if (paneIndex < panes.length) {
			return panes[paneIndex];
		}
		return null;
	}

	/**
	 * Gets the main view for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @return a MainTView
	 */
	public MainTView getMainView(TrackerPanel trackerPanel) {
		Object[] objects = getObjects(trackerPanel);
		if (objects != null) {
			return (MainTView) objects[PANEL_MAINVIEW];
		}
		return new MainTView(trackerPanel);
	}

	/**
	 * Responds to property change events.
	 *
	 * @param e the property change event
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		TrackerPanel trackerPanel;
		String name = e.getPropertyName();
		switch (name) {
		case VideoPanel.PROPERTY_VIDEOPANEL_DATAFILE:
		case TrackerPanel.PROPERTY_TRACKERPANEL_VIDEO: // from TrackerPanel //$NON-NLS-1$ 
			trackerPanel = (TrackerPanel) e.getSource();
			refreshTab(trackerPanel);
			break;
		case MovieVideoI.PROPERTY_VIDEO_PROGRESS: // from currently loading (xuggle) video 
			Object val = e.getNewValue(); // may be null
			String vidName = XML.forwardSlash((String) e.getOldValue());
			if (val != null) try {
				framesLoaded = Integer.parseInt(val.toString());
				TrackerIO.setProgress(vidName, val.toString(), framesLoaded);
			} catch (Exception ex) {
			}
			break;
		case MovieVideoI.PROPERTY_VIDEO_STALLED: // from stalled xuggle video 
			String fileName = XML.getName((String) e.getNewValue());
			String s = TrackerRes.getString("TFrame.Dialog.StalledVideo.Message0") //$NON-NLS-1$
					+ "\n" + TrackerRes.getString("TFrame.Dialog.StalledVideo.Message1") //$NON-NLS-1$ //$NON-NLS-2$
					+ "\n" + TrackerRes.getString("TFrame.Dialog.StalledVideo.Message2") //$NON-NLS-1$ //$NON-NLS-2$
					+ "\n\n" + TrackerRes.getString("TFrame.Dialog.StalledVideo.Message3"); //$NON-NLS-1$ //$NON-NLS-2$
			String stop = TrackerRes.getString("TFrame.Dialog.StalledVideo.Button.Stop"); //$NON-NLS-1$
			String wait = TrackerRes.getString("TFrame.Dialog.StalledVideo.Button.Wait"); //$NON-NLS-1$
			int response = JOptionPane.showOptionDialog(TFrame.this, s,
					TrackerRes.getString("TFrame.Dialog.StalledVideo.Title") + ": " + fileName, //$NON-NLS-1$ //$NON-NLS-2$
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] { stop, wait }, stop);
			if (response == 0) { // user chose to stop loading
				VideoIO.setCanceled(true);
				TrackerIO.closeMonitor(fileName);
			}
			break;
		case TrackerRes.PROPERTY_TRACKERRES_LOCALE: // from TrackerRes //$NON-NLS-1$
			// clear the existing menubars and actions
			TMenuBar.clear();
			TActions.clear();
			// create new actions
			Tracker.createActions();
			// create new default menubar
			createDefaultMenuBar();
			// replace and refresh the stored menubars and toolbars
			Iterator<Object[]> it = panelObjects.values().iterator();
			while (it.hasNext()) {
				Object[] objects = it.next();
				MainTView mainView = (MainTView) objects[PANEL_MAINVIEW];
				trackerPanel = mainView.getTrackerPanel();
				boolean changed = trackerPanel.changed; // save changed state and restore below
				// replace the stored menubar
				objects[PANEL_MENUBAR] = TMenuBar.getMenuBar(trackerPanel);
				CoordAxes axes = trackerPanel.getAxes();
				if (axes != null) {
					axes.setName(TrackerRes.getString("CoordAxes.New.Name")); //$NON-NLS-1$
				}
				trackerPanel.changed = changed;
				TToolBar toolbar = (TToolBar) objects[PANEL_TOOLBAR];
				toolbar.refresh(false);
				TTrackBar trackbar = (TTrackBar) objects[PANEL_TRACKBAR];
				trackbar.refresh();
			}
			trackerPanel = getTrackerPanel(getSelectedTab());
			if (trackerPanel != null) {
				// replace current menubar
				TMenuBar menuBar = getMenuBar(trackerPanel);
				if (menuBar != null) {
					setJMenuBar(menuBar);
					menuBar.refresh(TMenuBar.REFRESH_TFRAME_LOCALE);
				}
				// show hint
				if (Tracker.startupHintShown) {
					trackerPanel.setMessage(TrackerRes.getString("Tracker.Startup.Hint")); //$NON-NLS-1$
				} else {
					// shows hint as side effect
					trackerPanel.setCursorForMarking(false, null);
				}
			} else {
				// show defaultMenuBar
				refreshOpenRecentMenu(recentMenu);
				setJMenuBar(defaultMenuBar);
			}
			// refresh tabs
			for (int i = 0; i < tabbedPane.getTabCount(); i++) {
				trackerPanel = getTrackerPanel(i);
				tabbedPane.setTitleAt(i, trackerPanel.getTitle());
				VideoPlayer player = trackerPanel.getPlayer();
				player.refresh();
				player.setLocale((Locale) e.getNewValue());
				Video vid = trackerPanel.getVideo();
				if (vid != null) {
					vid.getFilterStack().refresh();
				}
				// refresh track controls and toolbars
				TrackControl.getControl(trackerPanel).refresh();
				getToolBar(trackerPanel).refresh(false);
				getTrackBar(trackerPanel).refresh();
				// refresh views
				Container[] views = getViews(trackerPanel);
				for (int j = 0; j < views.length; j++) {
					if (views[j] instanceof TViewChooser) {
						((TViewChooser) views[j]).refresh();
					}
				}
				// refresh autotracker
				if (trackerPanel.autoTracker != null) {
					trackerPanel.autoTracker.getWizard().textPaneSize = null;
					trackerPanel.autoTracker.getWizard().refreshGUI();
					trackerPanel.autoTracker.getWizard().pack();
				}
				// refresh prefs dialog
				if (prefsDialog != null && prefsDialog.isVisible()) {
					prefsDialog.refreshGUI();
				}
				// refresh pencil drawer
				PencilDrawer.getDrawer(trackerPanel).refresh();
				// refresh info dialog
				cancelNotesDialogButton.setText(TrackerRes.getString("Dialog.Button.Cancel")); //$NON-NLS-1$
				closeNotesDialogButton.setText(TrackerRes.getString("Dialog.Button.Close")); //$NON-NLS-1$
				displayWhenLoadedCheckbox.setText(TrackerRes.getString("TFrame.NotesDialog.Checkbox.ShowByDefault")); //$NON-NLS-1$
			}
			// refresh memory button
			TTrackBar.refreshMemoryButton();
			validate();
			if (helpLauncher != null) {
				// refresh navigation bar components
				Component[] search = HelpFinder.getNavComponentsFor(helpLauncher);
				Component[] comps = new Component[search.length + 2];
				System.arraycopy(search, 0, comps, 0, search.length);
				Tracker.pdfHelpButton.setText(TrackerRes.getString("Tracker.Button.PDFHelp")); //$NON-NLS-1$
				comps[comps.length - 2] = Tracker.pdfHelpButton;
				comps[comps.length - 1] = Box.createHorizontalStrut(4);
				helpLauncher.setNavbarRightEndComponents(comps);
			}
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!Tracker.splash.isVisible())
			return;
//    Tracker.setProgress(100);
		// dispose of splash automatically after short time
		Timer timer = new Timer(1500, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Tracker.splash.dispose();
			}
		});
		timer.setRepeats(false);
		timer.start();
	}

	/**
	 * Sets the display units for angles.
	 * 
	 * @param inRadians true to display radians, false to display degrees
	 */
	public void setAnglesInRadians(boolean inRadians) {
		if (anglesInRadians == inRadians)
			return;
		anglesInRadians = inRadians;
		firePropertyChange(PROPERTY_TFRAME_RADIANANGLES, null, inRadians); //$NON-NLS-1$
	}

	/**
	 * Gets the preferences dialog.
	 * 
	 * @return the preferences dialog
	 */
	public PrefsDialog getPrefsDialog() {
		TrackerPanel trackerPanel = getTrackerPanel(getSelectedTab());
		if (prefsDialog != null) {
			if (prefsDialog.trackerPanel != trackerPanel) {
				prefsDialog.trackerPanel = trackerPanel;
				prefsDialog.refreshGUI();
			}
		} else {
			// create PrefsDialog
			prefsDialog = new PrefsDialog(trackerPanel, TFrame.this);
			// center on screen
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			int x = (dim.width - prefsDialog.getBounds().width) / 2;
			int y = (dim.height - prefsDialog.getBounds().height) / 2;
			prefsDialog.setLocation(x, y);
		}
		return prefsDialog;
	}

	/**
	 * Shows the preferences dialog.
	 */
	public void showPrefsDialog() {
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				PrefsDialog prefsDialog = getPrefsDialog();
				prefsDialog.setVisible(true);
				prefsDialog.requestFocus();
			}
		};
		new Thread(runner).start();
	}

	/**
	 * Shows the preferences dialog set to a specified tab.
	 * 
	 * @param tabName the name of the tab: config, runtime, video, general, display
	 */
	public void showPrefsDialog(final String tabName) {
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				// show prefs dialog and select video tab
				PrefsDialog prefsDialog = getPrefsDialog();
				if (tabName != null) {
					if (tabName.contains("runtime")) //$NON-NLS-1$
						prefsDialog.tabbedPane.setSelectedComponent(prefsDialog.runtimePanel);
					else if (tabName.contains("video")) //$NON-NLS-1$
						prefsDialog.tabbedPane.setSelectedComponent(prefsDialog.videoPanel);
					else if (tabName.contains("general")) //$NON-NLS-1$
						prefsDialog.tabbedPane.setSelectedComponent(prefsDialog.generalPanel);
					else if (tabName.contains("display")) //$NON-NLS-1$
						prefsDialog.tabbedPane.setSelectedComponent(prefsDialog.displayPanel);
				}
				prefsDialog.setVisible(true);
				prefsDialog.requestFocus();
			}
		};
		new Thread(runner).start();
	}

//_________________________________ private methods _________________________

	/**
	 * Creates the default views of the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @return a Container[numberOfViews] array of views
	 */
	private Container[] createViews(TrackerPanel trackerPanel) {
		if (!Tracker.allowViews) {
			OSPLog.debug("TFrame allowViews is false");
			return new Container[4];
		}
		TViewChooser chooser1 = new TViewChooser(trackerPanel);
		chooser1.setSelectedView(chooser1.getView(TrackerRes.getString("TFrame.View.Plot"))); //$NON-NLS-1$
		TViewChooser chooser2 = new TViewChooser(trackerPanel);
		chooser2.setSelectedView(chooser2.getView(TrackerRes.getString("TFrame.View.Table"))); //$NON-NLS-1$
		TViewChooser chooser3 = new TViewChooser(trackerPanel);
		chooser3.setSelectedView(chooser3.getView(TrackerRes.getString("TFrame.View.World"))); //$NON-NLS-1$
		TViewChooser chooser4 = new TViewChooser(trackerPanel);
		chooser4.setSelectedView(chooser4.getView(TrackerRes.getString("TFrame.View.Text"))); //$NON-NLS-1$
		return new Container[] { chooser1, chooser2, chooser3, chooser4 };
	}

	/**
	 * Gets the split panes for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @return an array of split panes
	 */
	JSplitPane[] getSplitPanes(final TrackerPanel trackerPanel) {
		Object[] objects = getObjects(trackerPanel);
		if (objects != null) {
			return (JSplitPane[]) objects[PANEL_SPLITPANES];
		}
		JSplitPane[] panes = new JSplitPane[4];
		panes[0] = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT) { // right pane
			// override setDividerLocation to update Window menu item
			@Override
			public void setDividerLocation(int loc) {
				int cur = getDividerLocation();
				int max = getMaximumDividerLocation();
				if (loc == max) {
					// BH 2020.02.09 Java bug here.
					// super.setDividerLocation(double) will call this method again, infinitely.
					// here I substitute the result of double location 1.0 to not trigger that.
					// Probably not caught in Java because Java can return a location > max,
					// actually (987 > 983),
					// which is probably a Java bug.
					// super.setDividerLocation(1.0);
					super.setDividerLocation(getWidth() - getDividerSize());
				} else if (loc != cur)
					super.setDividerLocation(loc);
				cur = getDividerLocation();
				boolean open = (1.0 * cur / max) < 0.98;
				TMenuBar.setSelectedItem(trackerPanel, TMenuBar.TFRAME_RIGHT, open);
			}
		};
		panes[1] = new JSplitPane(JSplitPane.VERTICAL_SPLIT); // plot/table split
		panes[2] = new JSplitPane(JSplitPane.VERTICAL_SPLIT) { // bottom pane
			@Override
			public void setDividerLocation(int loc) {
				int cur = getDividerLocation();
				int max = getMaximumDividerLocation();
				if (loc == max) {
					// BH 2020.02.09 Java bug here.
					// super.setDividerLocation(double) will call this method again, infinitely.
					// here I substitute the result of double location 1.0 to not trigger that.
					// super.setDividerLocation(1.0);
					super.setDividerLocation(getHeight() - getDividerSize());
				} else if (loc != cur)
					super.setDividerLocation(loc);
				cur = getDividerLocation();
				boolean open = (1.0 * cur / max) < 0.98;
				TMenuBar.setSelectedItem(trackerPanel, TMenuBar.TFRAME_BOTTOM, open);
			}
		};
		panes[3] = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT) { // world/html
			@Override
			public void setDividerLocation(int loc) {
				int cur = getDividerLocation();
				int min = getMinimumDividerLocation();
				if (loc == min) {
					if (cur != 0)
						super.setDividerLocation(0);
				} else if (loc != cur)
					super.setDividerLocation(loc);
			}
		};
		panes[0].setResizeWeight(1.0);
		panes[1].setResizeWeight(0.5);
		panes[2].setResizeWeight(1.0);
		panes[3].setResizeWeight(0.5);
		panes[0].setOneTouchExpandable(true);
		panes[1].setOneTouchExpandable(true);
		panes[2].setOneTouchExpandable(true);
		panes[3].setOneTouchExpandable(true);
		return panes;
	}

	/**
	 * Gets the toolbar for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @return a TToolBar
	 */
	public TToolBar getToolBar(TrackerPanel trackerPanel) {
		Object[] objects = getObjects(trackerPanel);
		if (objects != null) {
			return (TToolBar) objects[PANEL_TOOLBAR];
		}
		return TToolBar.getToolbar(trackerPanel);
	}

	/**
	 * Gets the menubar for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @return a TMenuBar
	 */
	public TMenuBar getMenuBar(TrackerPanel trackerPanel) {
		Object[] objects = getObjects(trackerPanel);
		if (objects != null && objects[PANEL_MENUBAR] != null) {
			return (TMenuBar) objects[PANEL_MENUBAR];
		}
		return TMenuBar.getMenuBar(trackerPanel);
	}

	/**
	 * Sets the menubar for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @param menubar      a TMenuBar
	 */
	public void setMenuBar(TrackerPanel trackerPanel, TMenuBar menubar) {
		Object[] objects = getObjects(trackerPanel);
		if (objects != null && objects.length > 4) {
			objects[PANEL_MENUBAR] = menubar;
			setJMenuBar(menubar);
		}
	}

	/**
	 * Gets the selected track bar for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @return a TSelectedTrackBar
	 */
	public TTrackBar getTrackBar(TrackerPanel trackerPanel) {
		Object[] objects = getObjects(trackerPanel);
		if (objects != null) {
			return (TTrackBar) objects[PANEL_TRACKBAR];
		}
		return TTrackBar.getTrackbar(trackerPanel);
	}

	/**
	 * Refreshes the open recent files menu.
	 *
	 * @param menu the menu to refresh
	 */
	public void refreshOpenRecentMenu(final JMenu menu) {
		if (!OSPRuntime.isJS) /** @j2sNative */
		{
			synchronized (Tracker.recentFiles) {
				menu.setText(TrackerRes.getString("TMenuBar.Menu.OpenRecent")); //$NON-NLS-1$
				menu.setEnabled(!Tracker.recentFiles.isEmpty());
				if (openRecentAction == null) {
					openRecentAction = new AbstractAction() {
						@Override
						public void actionPerformed(ActionEvent e) {
							doRecentFiles(e.getActionCommand());
						}
					};
				}
				menu.removeAll();
				menu.setEnabled(!Tracker.recentFiles.isEmpty());
				for (String next : Tracker.recentFiles) {
					JMenuItem item = new JMenuItem(XML.getName(next));
					FontSizer.setFont(item);
					item.setActionCommand(next);
					item.setToolTipText(next);
					item.addActionListener(openRecentAction);
					menu.add(item);
				}
			}
		}
	}

	protected void doRecentFiles(String path) {
		URL url = null;
		File file = new File(path);
		if (!file.exists()) {
			int n = path.indexOf("!"); //$NON-NLS-1$
			if (n >= 0 && !(file = new File(path.substring(0, n))).exists()) {
				try {
					url = new URL(path);
				} catch (MalformedURLException e1) {
				}
			}
		}
		if (!file.exists() && url == null) {
			Tracker.recentFiles.remove(path);
			int n = getSelectedTab();
			if (n > -1) {
				TMenuBar.refreshMenus(getTrackerPanel(n), TMenuBar.REFRESH_TFRAME_OPENRECENT);
			} else {
				refreshOpenRecentMenu(recentMenu);
			}
			JOptionPane.showMessageDialog(TFrame.this, TrackerRes.getString("TFrame.Dialog.FileNotFound.Message") //$NON-NLS-1$
					+ "\n" + MediaRes.getString("VideoIO.Dialog.Label.Path") + ": " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ path, TrackerRes.getString("TFrame.Dialog.FileNotFound.Title"), //$NON-NLS-1$
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		TrackerPanel selected = getTrackerPanel(getSelectedTab());
		if (selected != null) {
			selected.setMouseCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}
		TrackerIO.open(path, TFrame.this);
//		if (url!=null)
//		TrackerIO.open(url, TFrame.this);
//		else 
//		TrackerIO.open(file, TFrame.this);
		setCursor(Cursor.getDefaultCursor());
	}

	/**
	 * Refreshes the GUI.
	 */
	public void refresh() {
		clearHoldPainting();
		int i = getSelectedTab();
		if (i < 0)
			return;
		TrackerPanel trackerPanel = getTrackerPanel(i);
		getMenuBar(trackerPanel).refresh(TMenuBar.REFRESH_TFRAME);
		getToolBar(trackerPanel).refresh(true);
		getTrackBar(trackerPanel).refresh();
		for (Container next : getViews(trackerPanel)) {
			if (next instanceof TViewChooser) {
				TViewChooser chooser = (TViewChooser) next;
				chooser.refreshMenus();
			}
		}
		trackerPanel.refreshNotesDialog();
//    checkMemory();
	}

	/**
	 * Sets the font level.
	 *
	 * @param level the desired font level
	 */
	@Override
	public void setFontLevel(int level) {
		if (!OSPRuntime.allowSetFonts)
			return;
		try {
			super.setFontLevel(level);			
			if (libraryBrowser != null) {
				libraryBrowser.setFontLevel(level);
			}
			
		} catch (Exception e) {
		}
		if (tabbedPane == null)
			return;
		FontSizer.setFonts(tabbedPane, level);

		Step.textLayoutFont = FontSizer.getResizedFont(Step.textLayoutFont, level);

		for (int i = 0; i < getTabCount(); i++) {
			TrackerPanel trackerPanel = getTrackerPanel(i);
			trackerPanel.setFontLevel(level);
		}

		if (haveExportDialog) {
			// lazy initialization
			ExportZipDialog.setFontLevels(level);
			if (ExportVideoDialog.videoExporter != null) {
				ExportVideoDialog.videoExporter.setFontLevel(level);
			}
		}
		if (haveThumbnailDialog) {
			// lazy initialization
			if (ThumbnailDialog.thumbnailDialog != null) {
				FontSizer.setFonts(ThumbnailDialog.thumbnailDialog, level);
				ThumbnailDialog.thumbnailDialog.refreshGUI();
			}
		}
		if (prefsDialog != null) {
			prefsDialog.refreshGUI();
		}
		if (libraryBrowser != null) {
			libraryBrowser.setFontLevel(level);
		}
		
		FontSizer.setFonts(notesDialog, level);
		FontSizer.setFonts(OSPLog.getOSPLog(), level);
		if (Tracker.readmeDialog != null) {
			FontSizer.setFonts(Tracker.readmeDialog, level);
		}
		if (Tracker.startLogDialog != null) {
			FontSizer.setFonts(Tracker.startLogDialog, level);
		}
		FontSizer.setFonts(defaultMenuBar, level);
		if (helpLauncher != null) {
			helpLauncher.setFontLevel(level);
			for (int i = 0; i < helpLauncher.getTabCount(); i++) {
				LaunchPanel tab = helpLauncher.getTab(i);
				if (level > 0) {
					String newValue = "help" + level + ".css"; //$NON-NLS-1$ //$NON-NLS-2$
					tab.getHTMLSubstitutionMap().put("help.css", newValue); //$NON-NLS-1$
				} else {
					tab.getHTMLSubstitutionMap().remove("help.css"); //$NON-NLS-1$
				}
			}
			for (int i = 0; i < helpLauncher.getHTMLTabCount(); i++) {
				HTMLPane pane = helpLauncher.getHTMLTab(i);
				pane.editorPane.getDocument().putProperty(Document.StreamDescriptionProperty, null);
			}
			helpLauncher.setDivider((int) (175 * FontSizer.getFactor(level)));
			helpLauncher.refreshSelectedTab();
		}
	}

	/**
	 * Gets the library browser.
	 *
	 * @return the library browser
	 */
	protected LibraryBrowser getLibraryBrowser() {
		if (libraryBrowser == null) {
			try {
				LibraryComPADRE.desiredOSPType = "Tracker"; //$NON-NLS-1$
//    	JDialog dialog = new JDialog(this, false);
				libraryBrowser = LibraryBrowser.getBrowser(null);
//    	libraryBrowser = LibraryBrowser.getBrowser(dialog);
				libraryBrowser.addOSPLibrary(LibraryBrowser.TRACKER_LIBRARY);
				libraryBrowser.addOSPLibrary(LibraryBrowser.SHARED_LIBRARY);
				libraryBrowser
						.addComPADRECollection(LibraryComPADRE.TRACKER_SERVER_TREE + LibraryComPADRE.PRIMARY_ONLY);
				libraryBrowser.refreshCollectionsMenu();
				libraryBrowser.addPropertyChangeListener(LibraryBrowser.PROPERTY_LIBRARY_TARGET,
						new PropertyChangeListener() { // $NON-NLS-1$
							@Override
							public void propertyChange(PropertyChangeEvent e) {
								openLibraryResource((LibraryResource) e.getNewValue());
								TFrame.this.requestFocus();
							}
						});
				LibraryBrowser.fireHelpEvent = true;
				libraryBrowser.addPropertyChangeListener("help", new PropertyChangeListener() { //$NON-NLS-1$
					@Override
					public void propertyChange(PropertyChangeEvent e) {
						showHelp("library", 0); //$NON-NLS-1$
					}
				});
				libraryBrowser.setFontLevel(FontSizer.getLevel());
//      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
//      int x = (dim.width - dialog.getBounds().width) / 2;
//      int y = (dim.height - dialog.getBounds().height) / 2;
//      dialog.setLocation(x, y);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return libraryBrowser;
	}

	protected void openLibraryResource(LibraryResource record) {
		libraryBrowser.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			String target = XML.getResolvedPath(record.getTarget(), record.getBasePath());
			target = ResourceLoader.getURIPath(target);

			// download comPADRE targets to osp cache
			if (target.indexOf("document/ServeFile.cfm?") > -1) { //$NON-NLS-1$
				String fileName = record.getProperty("download_filename"); //$NON-NLS-1$
				try {
					File file = ResourceLoader.downloadToOSPCache(target, fileName, false);
					target = file.toURI().toString();
				} catch (Exception ex) {
					String s = TrackerRes.getString("TFrame.Dialog.LibraryError.Message"); //$NON-NLS-1$
					JOptionPane.showMessageDialog(libraryBrowser, s + " \"" + record.getName() + "\"", //$NON-NLS-1$ //$NON-NLS-2$
							TrackerRes.getString("TFrame.Dialog.LibraryError.Title"), //$NON-NLS-1$
							JOptionPane.WARNING_MESSAGE);
					return;
				}
			}

			String lcTarget = target.toLowerCase();
			boolean accept = lcTarget.endsWith(".trk") //$NON-NLS-1$
					|| lcTarget.endsWith(".zip") //$NON-NLS-1$
					|| lcTarget.endsWith(".trz"); //$NON-NLS-1$
			if (!accept) {
				for (String ext : VideoIO.getVideoExtensions()) {
					accept |= lcTarget.endsWith("." + ext); //$NON-NLS-1$
					if (accept)
						break;
				}
			}
			if (accept) {
//	libraryBrowser.setVisible(false);
				Resource res = ResourceLoader.getResourceZipURLsOK(target);
				if (res == null) {
					String s = TrackerRes.getString("TFrame.Dialog.LibraryError.FileNotFound.Message"); //$NON-NLS-1$
					JOptionPane.showMessageDialog(libraryBrowser, s + " \"" + XML.getName(target) + "\"", //$NON-NLS-1$ //$NON-NLS-2$
							TrackerRes.getString("TFrame.Dialog.LibraryError.FileNotFound.Title"), //$NON-NLS-1$
							JOptionPane.WARNING_MESSAGE);
					libraryBrowser.setVisible(true);
					return;
				}
				ArrayList<String> uriPaths = new ArrayList<String>();
				uriPaths.add(target);
				TrackerIO.openCollection(uriPaths, TFrame.this, null, null);
			}
		} finally {
			libraryBrowser.setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Gets the properties dialog for a specified TrackerPanel.
	 *
	 * @param trackerPanel
	 * @return the properties dialog
	 */
	protected PropertiesDialog getPropertiesDialog(TrackerPanel trackerPanel) {
		// return a new dialog every time
		PropertiesDialog dialog = new PropertiesDialog(trackerPanel);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (dim.width - dialog.getBounds().width) / 2;
		int y = (dim.height - dialog.getBounds().height) / 2;
		dialog.setLocation(x, y);
		return dialog;
	}

	/**
	 * Gets the help dialog.
	 *
	 * @return the help dialog
	 */
	protected Component getHelpDialog() {
		if (helpDialog == null) {
			helpDialog = new JDialog(this, TrackerRes.getString("TFrame.Dialog.Help.Title"), false); //$NON-NLS-1$
			String help_path = helpPath + "help_set.xml"; //$NON-NLS-1$
//      String lang = TrackerRes.locale.getLanguage();
//      String webHelp = helpPathWeb+"help_"+lang+"/help_set.xml"; //$NON-NLS-1$ //$NON-NLS-2$
//      Resource res = ResourceLoader.getResource(webHelp);
//      if (res!=null) { 
//      	help_path = res.getString(); // open launcher with xml string
//      }
//      System.out.println(help_path);
			helpLauncher = new Launcher(help_path, false);
			helpLauncher.popupEnabled = false;
			int level = FontSizer.getLevel();
			if (helpLauncher.getTabCount() > 0) {
				LaunchPanel tab = helpLauncher.getTab(0);
				if (level > 0) {
					String newValue = "help" + level + ".css"; //$NON-NLS-1$ //$NON-NLS-2$
					tab.getHTMLSubstitutionMap().put("help.css", newValue); //$NON-NLS-1$
				} else {
					tab.getHTMLSubstitutionMap().remove("help.css"); //$NON-NLS-1$
				}
			}
			helpLauncher.setDivider((int) (175 * FontSizer.getFactor(level)));

			// navigation bar and search components
			helpLauncher.setNavigationVisible(true);

			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension dim = helpLauncher.getSize();
			dim.width = Math.min((9 * screen.width) / 10, (int) ((1 + level * 0.35) * dim.width));
			dim.height = Math.min((9 * screen.height) / 10, (int) ((1 + level * 0.35) * dim.height));
			helpLauncher.setSize(dim);

			helpDialog.setContentPane(helpLauncher.getContentPane());
			FontSizer.setFonts(helpDialog, FontSizer.getLevel());

			helpDialog.pack();
			int x = (screen.width - helpDialog.getBounds().width) / 2;
			int y = (screen.height - helpDialog.getBounds().height) / 2;
			helpDialog.setLocation(x, y);
		}
		// refresh navigation bar components in case locale has changed
		Component[] search = HelpFinder.getNavComponentsFor(helpLauncher);
		Component[] comps = new Component[search.length + 2];
		System.arraycopy(search, 0, comps, 0, search.length);
		Tracker.pdfHelpButton.setText(TrackerRes.getString("Tracker.Button.PDFHelp")); //$NON-NLS-1$
		comps[comps.length - 2] = Tracker.pdfHelpButton;
		comps[comps.length - 1] = Box.createHorizontalStrut(4);
		helpLauncher.setNavbarRightEndComponents(comps);
		return helpDialog;
	}

	/**
	 * Shows a specified help topic.
	 *
	 * @param selectedNode the name of the help node to be displayed
	 */
	protected void showHelp(String selectedNode) {
		getHelpDialog(); // create dialog and launcher if needed
		helpLauncher.setSelectedNode(selectedNode);
		helpDialog.setVisible(true);
	}

	/**
	 * Shows a specified help topic by keyword: gettingstarted, install, linux, GUI,
	 * video, filters, tracks, coords, axes, tape, offset, calibration, pointmass,
	 * cm, vector, vectorsum, profile, rgbregion, particle, plot, datatable, xml,
	 * etc.
	 *
	 * @param keywords   the keywords of the help node to be displayed
	 * @param pageNumber the html page number
	 */
	protected void showHelp(String keywords, int pageNumber) {
		boolean firstTime = helpDialog == null;
		getHelpDialog(); // create dialog and launcher if needed
		if (keywords == null && firstTime) {
			keywords = "help"; //$NON-NLS-1$
		}
		helpLauncher.setSelectedNodeByKey(keywords, pageNumber);
		if (firstTime)
			helpLauncher.clearHistory();
		helpDialog.setVisible(true);
	}

	/**
	 * Shows the track control if any user tracks are present.
	 *
	 * @param panel the tracker panel
	 */
	protected void showTrackControl(final TrackerPanel panel) {
		if (panel.getUserTracks().size() > 0) {
			Runnable runner = new Runnable() {
				@Override
				public void run() {
					TrackControl tc = TrackControl.getControl(panel);
					if (tc.positioned && !tc.isEmpty()) {
						tc.setVisible(true);
					}
				}
			};
			EventQueue.invokeLater(runner);
		}
	}

	/**
	 * Shows the notes, if any.
	 *
	 * @param panel the tracker panel
	 */
	protected void showNotes(final TrackerPanel panel) {
		final JButton button = getToolBar(panel).notesButton;
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				TTrack track = panel.getSelectedTrack();
				if (!panel.hideDescriptionWhenLoaded && ((track != null && track.getDescription() != null
						&& !track.getDescription().trim().equals("")) || //$NON-NLS-1$
				(track == null && panel.getDescription() != null && !panel.getDescription().trim().equals("")))) { //$NON-NLS-1$
					if (!button.isSelected())
						button.doClick();
				} else if (button.isSelected())
					button.doClick();
			}
		};
		EventQueue.invokeLater(runner);
	}

//  /**
//   * Checks the current memory usage. If the total memory being used approaches 
//   * the max available, this reopens Tracker in a new larger java vm.
//   */
//  public void checkMemory() {
//  	System.gc();
//  	Runtime runtime = Runtime.getRuntime();
//  	double total = runtime.totalMemory();
//  	double max = runtime.maxMemory();
//  	JOptionPane.showMessageDialog(this, "memory "+total+" of "+max); //$NON-NLS-1$ //$NON-NLS-2$
//  	if (total/max > 0.6 && OSPRuntime.getLaunchJarPath() != null) {
//  		int result = JOptionPane.showConfirmDialog(this, "Resize memory to "+2*max+"?"); //$NON-NLS-1$ //$NON-NLS-2$
//      if (result != JOptionPane.YES_OPTION) return;
//  		// save trackerPanel fileNames
//  		ArrayList<File> files = new ArrayList<File>();
//  		for (int i = 0; i < getTabCount(); i++) {
//  			File file = getTrackerPanel(i).getDataFile();
//  			if (file != null) files.add(file);
//  		}
//  		// dispose of this frame 
//  		this.dispose();
//  		// launch Tracker in new vm
//      // construct the command to execute
//      final java.util.Vector<String> cmd = new java.util.Vector<String>();
//      cmd.add("java"); //$NON-NLS-1$
//      String classPath = OSPRuntime.getLaunchJarPath();
//      // convert colons to semicolons
//      classPath = classPath.replace(':', ';');
//      // replace semicolons with platform-dependent path separator
//      char pathSeparator = System.getProperty("path.separator").charAt(0);   //$NON-NLS-1$
//      classPath = classPath.replace(';', pathSeparator);
//      cmd.add("-classpath");                                                 //$NON-NLS-1$
//      cmd.add(classPath);
//      cmd.add(Tracker.class.getName());
//      String memoryArg = "-Xmx"+2*max; //$NON-NLS-1$
//      cmd.add(memoryArg);
//      memoryArg = "-Xms"+2*max; //$NON-NLS-1$
//      cmd.add(memoryArg);
//      Iterator<File> it = files.iterator();
//      while (it.hasNext()) {
//      	String arg = it.next().getPath();
//        cmd.add(arg);
//      }
//      // launch thread for new VM
//      Runnable launchRunner = new Runnable() {
//         public void run() {
//            OSPLog.finer(cmd.toString());
//            String[] cmdarray = cmd.toArray(new String[0]);
//            try {
//               Process proc = Runtime.getRuntime().exec(cmdarray);
//               BufferedInputStream errStream=new BufferedInputStream(proc.getErrorStream());
//               StringBuffer buff= new StringBuffer();
//               while(true){
//                 int datum=errStream.read();
//                 if(datum==-1) break;
//                 buff.append((char)datum);
//               }
//               errStream.close();
//               String msg=buff.toString().trim();
//               if(msg.length()>0){
//                 OSPLog.info("error buffer: " + buff.toString()); //$NON-NLS-1$
//               }
//            } catch(Exception ex) {
//               ex.printStackTrace();
//            }
//         }
//      };
//      Thread relauncher = new Thread(launchRunner);
//      relauncher.setPriority(Thread.NORM_PRIORITY);
//      relauncher.start();           
//  	}
//  }

	/**
	 * Gets the object array for the specified tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 * @return the object array
	 */
	private Object[] getObjects(TrackerPanel trackerPanel) {
		int tab = getTab(trackerPanel);
		if (tab >= 0)
			return panelObjects.get(tabbedPane.getComponentAt(tab));
		return null;
	}

	/**
	 * Gets the (singleton) clipboard listener.
	 *
	 * @return the ClipboardListener
	 */
	protected ClipboardListener getClipboardListener() {
		if (clipboardListener == null) {
			clipboardListener = new ClipboardListener(this);
			clipboardListener.start();
		}
		return clipboardListener;
	}

	/**
	 * Starts or ends the clipboard listener as needed.
	 */
	protected void checkClipboardListener() {
		// do we need clipboard listener?
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				boolean needListener = alwaysListenToClipboard;
				if (!needListener) {
					// do any pasted data tracks exist?
					try {
						for (int i = 0; i < getTabCount(); i++) {
							TrackerPanel trackerPanel = getTrackerPanel(i);
							ArrayList<DataTrack> list = trackerPanel.getDrawables(DataTrack.class);
							// do any tracks have null source?
							for (int m = 0, n = list.size(); m < n; m++) {
								DataTrack next = list.get(m);
								if (next.getSource() == null) {
									// null source, so data is pasted
									needListener = true;
									break;
								}
							}
						}
					} catch (Exception ex) {
					}
				}

				if (needListener) {
					getClipboardListener();
				} else {
					if (clipboardListener == null)
						return;
					// end existing listener
					clipboardListener.end();
					clipboardListener = null;
				}
			}
		};
//  	new Thread(runner).start();
		SwingUtilities.invokeLater(runner);

	}

	/**
	 * Creates the GUI.
	 */
	private void createGUI() {
		// add focus listener to notify ParticleDataTracks and other listeners
		addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				firePropertyChange(PROPERTY_TFRAME_WINDOWFOCUS, null, null); //$NON-NLS-1$
			}
		});
		// create notes actions and dialog
		saveNotesAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (notesTextPane.getBackground() == Color.WHITE)
					return;
				String desc = notesTextPane.getText();
				if (getSelectedTab() > -1 && notesDialog.getName() != "canceled") { //$NON-NLS-1$
					TrackerPanel trackerPanel = getTrackerPanel(getSelectedTab());
					trackerPanel.changed = true;
					TTrack track = trackerPanel.getTrack(notesDialog.getName());
					if (track != null && !desc.equals(track.getDescription())) {
						track.setDescription(desc);
					} else if (!desc.equals(trackerPanel.getDescription())) {
						trackerPanel.setDescription(desc);
						trackerPanel.hideDescriptionWhenLoaded = !displayWhenLoadedCheckbox.isSelected();
					}
				}
				notesTextPane.setBackground(Color.WHITE);
				cancelNotesDialogButton.setEnabled(false);
				closeNotesDialogButton.setEnabled(true);
				closeNotesDialogButton.setText(TrackerRes.getString("Dialog.Button.Close")); //$NON-NLS-1$
			}
		};
		notesDialog = new JDialog(this, false) {
			@Override
			public void setVisible(boolean vis) {
				super.setVisible(vis);

				if (getSelectedTab() > -1) {
					TrackerPanel trackerPanel = getTrackerPanel(getSelectedTab());
					TToolBar toolbar = getToolBar(trackerPanel);
					toolbar.notesButton.setSelected(vis);
				}
			}
		};
		JPanel infoContentPane = new JPanel(new BorderLayout());
		notesDialog.setContentPane(infoContentPane);
		notesTextPane = new JTextPane();
		notesTextPane.setBackground(Color.WHITE);
		notesTextPane.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					String url = e.getURL().toString();
					org.opensourcephysics.desktop.OSPDesktop.displayURL(url);
				}
			}
		});
		notesTextPane.setPreferredSize(new Dimension(420, 200));
		notesTextPane.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				TrackerPanel trackerPanel = getTrackerPanel(getSelectedTab());
				if (!trackerPanel.isEnabled("notes.edit")) //$NON-NLS-1$
					return;
				notesTextPane.setBackground(yellow);
				closeNotesDialogButton.setText(TrackerRes.getString("PrefsDialog.Button.Save")); //$NON-NLS-1$
				cancelNotesDialogButton.setEnabled(true);
			}
		});
		notesTextPane.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				if (e.getOppositeComponent() != cancelNotesDialogButton)
					saveNotesAction.actionPerformed(null);
			}
		});
		JScrollPane textScroller = new JScrollPane(notesTextPane);
		infoContentPane.add(textScroller, BorderLayout.CENTER);
		JPanel buttonbar = new JPanel(new FlowLayout());
		infoContentPane.add(buttonbar, BorderLayout.SOUTH);
		displayWhenLoadedCheckbox = new JCheckBox(TrackerRes.getString("TFrame.NotesDialog.Checkbox.ShowByDefault")); //$NON-NLS-1$
		displayWhenLoadedCheckbox.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TrackerPanel trackerPanel = getTrackerPanel(getSelectedTab());
				if (trackerPanel != null) {
					trackerPanel.hideDescriptionWhenLoaded = !displayWhenLoadedCheckbox.isSelected();
				}
			}
		});
		buttonbar.add(displayWhenLoadedCheckbox);
		buttonbar.add(Box.createHorizontalStrut(50));
		cancelNotesDialogButton = new JButton(TrackerRes.getString("Dialog.Button.Cancel")); //$NON-NLS-1$
		cancelNotesDialogButton.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				notesDialog.setName("canceled"); //$NON-NLS-1$
				notesDialog.setVisible(false);
			}
		});
		buttonbar.add(cancelNotesDialogButton);
		closeNotesDialogButton = new JButton(TrackerRes.getString("Dialog.Button.Close")); //$NON-NLS-1$
		closeNotesDialogButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				notesDialog.setVisible(false);
			}
		});
		buttonbar.add(closeNotesDialogButton);
		notesDialog.pack();
		// create the tabbed pane
		tabbedPane = new JTabbedPane(SwingConstants.BOTTOM);
		setContentPane(new JPanel(new BorderLayout()));
		getContentPane().add(tabbedPane, BorderLayout.CENTER);
		// create the default menubar
		createDefaultMenuBar();
		// add listener to change menubar, toolbar, track control when tab changes
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				OSPLog.debug("TFrame state changed " + e.getSource().getClass().getName());
				TrackerPanel newPanel = null;
				TrackerPanel oldPanel = prevPanel;

				// hide exportZipDialog
				if (haveExportDialog && ExportVideoDialog.videoExporter != null) {
					ExportVideoDialog.videoExporter.trackerPanel = null;
				}
				if (haveThumbnailDialog && ThumbnailDialog.thumbnailDialog != null) {
					ThumbnailDialog.thumbnailDialog.trackerPanel = null;
				}
				// update prefsDialog
				if (prefsDialog != null) {
					prefsDialog.trackerPanel = null;
				}
				// clean up items associated with old panel
				if (playerBar != null) {
					Container frame = playerBar.getTopLevelAncestor();
					if (frame != null && frame != TFrame.this)
						frame.setVisible(false);
				}
				if (prevPanel != null) {
					if (prevPanel.dataBuilder != null) {
						boolean vis = prevPanel.dataToolVisible;
						prevPanel.dataBuilder.setVisible(false);
						prevPanel.dataToolVisible = vis;
					}
					if (prevPanel.getPlayer() != null) {
						ClipInspector ci = prevPanel.getPlayer().getVideoClip().getClipInspector();
						if (ci != null)
							ci.setVisible(false);
					}
					Video vid = prevPanel.getVideo();
					if (vid != null) {
						vid.getFilterStack().setInspectorsVisible(false);
					}
				}
				// refresh current tab items
				Object[] objects = panelObjects.get(tabbedPane.getSelectedComponent());
				if (objects != null) {
					MainTView mainView = (MainTView) objects[PANEL_MAINVIEW];
					newPanel = mainView.getTrackerPanel();
					prevPanel = newPanel;
					// update prefsDialog
					if (prefsDialog != null) {
						prefsDialog.trackerPanel = newPanel;
					}
					// refresh the notes dialog and button
					newPanel.refreshNotesDialog();
					JButton notesButton = getToolBar(newPanel).notesButton;
					notesButton.setSelected(notesDialog.isVisible());
					// refresh trackbar
					((TTrackBar) objects[PANEL_TRACKBAR]).refresh();
					// refresh and replace menu bar
					TMenuBar menubar = (TMenuBar) objects[PANEL_MENUBAR];
					refreshOpenRecentMenu(menubar.file_openRecentMenu);

//          menubar.refresh();
					setJMenuBar(menubar);
					// show floating player
					playerBar = mainView.getPlayerBar();
					Container frame = playerBar.getTopLevelAncestor();
					if (frame != null && frame != TFrame.this)
						frame.setVisible(true);
					if (newPanel.dataBuilder != null)
						newPanel.dataBuilder.setVisible(newPanel.dataToolVisible);
					Video vid = newPanel.getVideo();
					if (vid != null) {
						vid.getFilterStack().setInspectorsVisible(true);
					}
				} else {
					// show defaultMenuBar
					refreshOpenRecentMenu(recentMenu);
					setJMenuBar(defaultMenuBar);
				}
				// update prefsDialog
				if (prefsDialog != null && prefsDialog.isVisible()) {
					prefsDialog.refreshGUI();
				}
				firePropertyChange(PROPERTY_TFRAME_TAB, oldPanel, newPanel); //$NON-NLS-1$
			}
		});
		closeItem = new JMenuItem();
		closeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeTab(getTrackerPanel(getSelectedTab()));
			}
		});
		popup.add(closeItem);
		tabbedPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int i = getSelectedTab();
				if (i < 0)
					return;
				TrackerPanel panel = getTrackerPanel(i);
				if (panel == null || !panel.isEnabled("file.close")) //$NON-NLS-1$
					return;
				if (OSPRuntime.isPopupTrigger(e)) {
					closeItem.setText(TrackerRes.getString("TActions.Action.Close") + " \"" //$NON-NLS-1$ //$NON-NLS-2$
							+ tabbedPane.getTitleAt(getSelectedTab()) + "\""); //$NON-NLS-1$
					FontSizer.setFonts(popup, FontSizer.getLevel());
					popup.show(tabbedPane, e.getX(), e.getY());
				}
			}
		});
	}

	private void createDefaultMenuBar() {
		// create the default (empty) menubar
		int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		defaultMenuBar = new JMenuBar();
		setJMenuBar(defaultMenuBar);
		// file menu
		JMenu fileMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.File")); //$NON-NLS-1$
		defaultMenuBar.add(fileMenu);
		// new tab item
		JMenuItem newItem = new JMenuItem(TrackerRes.getString("TActions.Action.NewTab")); //$NON-NLS-1$
		newItem.setAccelerator(KeyStroke.getKeyStroke('N', keyMask));
		newItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TFrame.this.addTrackerPane(false);
			}
		});
		fileMenu.add(newItem);
		if (!OSPRuntime.isApplet) {
			fileMenu.addSeparator();
			// open file item
			Icon icon = new ImageIcon(Tracker.getClassResource("resources/images/open.gif")); //$NON-NLS-1$
			JMenuItem openItem = new JMenuItem(TrackerRes.getString("TActions.Action.Open"), icon); //$NON-NLS-1$
			openItem.setAccelerator(KeyStroke.getKeyStroke('O', keyMask));
			openItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					TFrame.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					TrackerIO.openTabFile((File) null, TFrame.this);
					TFrame.this.setCursor(Cursor.getDefaultCursor());
				}
			});
			fileMenu.add(openItem);
			// open recent menu
			recentMenu = new JMenu();
			fileMenu.add(recentMenu);
			refreshOpenRecentMenu(recentMenu);
			fileMenu.addSeparator();
			// openBrowser item
			icon = new ImageIcon(Tracker.getClassResource("resources/images/open_catalog.gif")); //$NON-NLS-1$
			JMenuItem openBrowserItem = new JMenuItem(TrackerRes.getString("TActions.Action.OpenBrowser"), icon); //$NON-NLS-1$
			openBrowserItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					getLibraryBrowser().setVisible(true);
				}
			});
			fileMenu.add(openBrowserItem);
			fileMenu.addSeparator();
			// exit item
			JMenuItem exitItem = new JMenuItem(TrackerRes.getString("TActions.Action.Exit")); //$NON-NLS-1$
			exitItem.setAccelerator(KeyStroke.getKeyStroke('Q', keyMask));
			exitItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});
			fileMenu.add(exitItem);
		}
		// edit menu
		JMenu editMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Edit")); //$NON-NLS-1$
		defaultMenuBar.add(editMenu);
		// language menu
		JMenu languageMenu = new JMenu(TrackerRes.getString("TMenuBar.MenuItem.Language")); //$NON-NLS-1$
		editMenu.add(languageMenu);
		Action languageAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String language = e.getActionCommand();
				for (int i = 0; i < Tracker.incompleteLocales.length; i++) {
					if (language.equals(Tracker.incompleteLocales[i][0].toString())) {
						Locale locale = (Locale) Tracker.incompleteLocales[i][0];
						String lang = OSPRuntime.getDisplayLanguage(locale);
						// the following message is purposely not translated
						JOptionPane.showMessageDialog(TFrame.this,
								"This translation has not been updated since " + Tracker.incompleteLocales[i][1] //$NON-NLS-1$
										+ ".\nIf you speak " + lang + " and would like to help translate" //$NON-NLS-1$ //$NON-NLS-2$
										+ "\nplease contact Douglas Brown at dobrown@cabrillo.edu.", //$NON-NLS-1$
								"Incomplete Translation: " + lang, //$NON-NLS-1$
								JOptionPane.WARNING_MESSAGE);
					}
				}
				for (int i = 0; i < Tracker.locales.length; i++) {
					if (language.equals(Tracker.locales[i].toString())) {
						TrackerRes.setLocale(Tracker.locales[i]);
						return;
					}
				}
			}
		};
		ButtonGroup languageGroup = new ButtonGroup();
		for (int i = 0; i < Tracker.locales.length; i++) {
			String lang = OSPRuntime.getDisplayLanguage(Tracker.locales[i]);
			// special handling for portuguese BR and PT
			if (Tracker.locales[i].getLanguage().equals("pt")) { //$NON-NLS-1$
				lang += " (" + Tracker.locales[i].getCountry() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			JMenuItem item = new JRadioButtonMenuItem(lang);
			item.setActionCommand(Tracker.locales[i].toString());
			item.addActionListener(languageAction);
			languageMenu.add(item);
			languageGroup.add(item);
			if (Tracker.locales[i].toString().equals(TrackerRes.locale.toString())) {
				item.setSelected(true);
			}
		}
		// add "other" language item at end
		// the following item and message is purposely not translated
		JMenuItem otherLanguageItem = new JMenuItem("Other"); //$NON-NLS-1$
		languageMenu.addSeparator();
		languageMenu.add(otherLanguageItem);
		otherLanguageItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(TFrame.this, "Do you speak a language not yet available in Tracker?" //$NON-NLS-1$
						+ "\nTo learn more about translating Tracker into your language" //$NON-NLS-1$
						+ "\nplease contact Douglas Brown at dobrown@cabrillo.edu.", //$NON-NLS-1$
						"New Translation", //$NON-NLS-1$
						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		// preferences item
		JMenuItem prefsItem = new JMenuItem(TrackerRes.getString("TActions.Action.Config")); //$NON-NLS-1$
		prefsItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPrefsDialog();
			}
		});
		editMenu.addSeparator();
		editMenu.add(prefsItem);

		// video menu
		JMenu videoMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Video")); //$NON-NLS-1$
		videoMenu.setEnabled(false);
		defaultMenuBar.add(videoMenu);
		// tracks menu
		JMenu tracksMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Tracks")); //$NON-NLS-1$
		tracksMenu.setEnabled(false);
		defaultMenuBar.add(tracksMenu);
		// coords menu
		JMenu coordsMenu = new JMenu(TrackerRes.getString("TMenuBar.Menu.Coords")); //$NON-NLS-1$
		coordsMenu.setEnabled(false);
		defaultMenuBar.add(coordsMenu);
		// help menu
		JMenu helpMenu = TMenuBar.getTrackerHelpMenu(null, null);
		defaultMenuBar.add(helpMenu);
	}

	/**
	 * Initializes a new tracker panel.
	 *
	 * @param trackerPanel the tracker panel
	 */
	private void initialize(TrackerPanel trackerPanel) {
		TMenuBar.getMenuBar(trackerPanel).setAllowRefresh(false);
		// add a background mat if none exists
		if (trackerPanel.getMat() == null) {
			trackerPanel.addDrawable(new TMat(trackerPanel)); // constructor adds mat to panel
		}
		// add coordinate axes if none exists
		if (trackerPanel.getAxes() == null) {
			Tracker.setProgress(81);
			CoordAxes axes = new CoordAxes();
			axes.setVisible(false);
			trackerPanel.addTrack(axes);
		}
		// add video filters to the tracker panel
		trackerPanel.addFilter(DeinterlaceFilter.class);
		trackerPanel.addFilter(GhostFilter.class);
		trackerPanel.addFilter(StrobeFilter.class);
		trackerPanel.addFilter(DarkGhostFilter.class);
		trackerPanel.addFilter(NegativeFilter.class);
		trackerPanel.addFilter(GrayScaleFilter.class);
		trackerPanel.addFilter(BrightnessFilter.class);
		trackerPanel.addFilter(BaselineFilter.class);
		trackerPanel.addFilter(SumFilter.class);
		trackerPanel.addFilter(ResizeFilter.class);
		trackerPanel.addFilter(RotateFilter.class);
		trackerPanel.addFilter(PerspectiveFilter.class);
		trackerPanel.addFilter(RadialDistortionFilter.class);
		// set mouse handler
		trackerPanel.mouseHandler = new TMouseHandler();
		trackerPanel.setInteractiveMouseHandler(trackerPanel.mouseHandler);
		// set file drop handler
		trackerPanel.setTransferHandler(fileDropHandler);
		// set divider locations
		validate(); // in advance of setting divider locations
		if (trackerPanel.dividerLocs != null) {
			int w = 0;
			for (int i = 0; i < trackerPanel.dividerLocs.length; i++) {
				JSplitPane pane = getSplitPane(trackerPanel, i);
				if (i == 0)
					w = pane.getMaximumDividerLocation();
				int max = i == 3 ? w : pane.getMaximumDividerLocation();
				int loc = (int) (trackerPanel.dividerLocs[i] * max);
				pane.setDividerLocation(loc);
			}
			trackerPanel.dividerLocs = null;
		} else {
			setDividerLocation(trackerPanel, 0, 1.0); // becomes previous
			setDividerLocation(trackerPanel, 0, defaultRightDivider);
			setDividerLocation(trackerPanel, 1, 0.5);
			setDividerLocation(trackerPanel, 2, defaultBottomDivider); // becomes previous
			setDividerLocation(trackerPanel, 2, 1.0);
			setDividerLocation(trackerPanel, 3, 1.0); // becomes previous
			JSplitPane pane = getSplitPane(trackerPanel, 0);
			int max = pane.getMaximumDividerLocation();
			int loc = (int) (.5 * defaultRightDivider * max);
			pane = getSplitPane(trackerPanel, 3);
			pane.setDividerLocation(loc);
		}
		validate(); // after setting divider locations
		// set track control location
		if (trackerPanel.trackControlX != Integer.MIN_VALUE) {
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			TrackControl tc = TrackControl.getControl(trackerPanel);
			int x = Math.max(getLocation().x + trackerPanel.trackControlX, 0);
			x = Math.min(x, dim.width - tc.getWidth());
			int y = Math.max(getLocation().y + trackerPanel.trackControlY, 0);
			y = Math.min(y, dim.height - tc.getHeight());
			tc.setLocation(x, y);
			tc.positioned = true;
		}
		// show filter inspectors
		trackerPanel.showFilterInspectors();
		// set initial format patterns for existing tracks
		trackerPanel.setInitialFormatPatterns();
		Tracker.setProgress(90);
		TMenuBar.getMenuBar(trackerPanel).setAllowRefresh(true);
	}

	/**
	 * Returns an ObjectLoader to save and load data for this class.
	 *
	 * @return the object loader
	 */
	public static XML.ObjectLoader getLoader() {
		return new Loader();
	}

	/**
	 * A class to save and load data for this class.
	 */
	static class Loader implements XML.ObjectLoader {

		/**
		 * Saves an object's data to an XMLControl.
		 *
		 * @param control the control to save to
		 * @param obj     the object to save
		 */
		@Override
		public void saveObject(XMLControl control, Object obj) {
			TFrame frame = (TFrame) obj;
			// save tabs with data files or unchanged videos
			// save both relative paths (relative to tabsetFile) and absolute paths
			String relativeTo = frame.tabsetFile != null ? XML.getDirectoryPath(XML.getAbsolutePath(frame.tabsetFile))
					: XML.getUserDirectory();
			relativeTo = XML.forwardSlash(relativeTo);
			ArrayList<String[]> pathList = new ArrayList<String[]>();
			for (int i = 0; i < frame.getTabCount(); i++) {
				TrackerPanel trackerPanel = frame.getTrackerPanel(i);
				File file = trackerPanel.getDataFile();
				if (file != null) {
					String path = XML.getAbsolutePath(file);
					String relativePath = XML.getPathRelativeTo(path, relativeTo);
					pathList.add(new String[] { path, relativePath });
				} else {
					Video video = trackerPanel.getVideo();
					if (!trackerPanel.changed && video != null) {
						String path = (String) video.getProperty("absolutePath"); //$NON-NLS-1$
						if (path != null) {
							path = XML.forwardSlash(path);
							String relativePath = XML.getPathRelativeTo(path, relativeTo);
							pathList.add(new String[] { path, relativePath });
						}
					}
				}
			}
			String[][] paths = pathList.toArray(new String[0][0]);
			control.setValue("tabs", paths); //$NON-NLS-1$
		}

		/**
		 * Creates a new object. This returns null--must load an existing TFrame.
		 *
		 * @param control the XMLControl with the object data
		 * @return the newly created object
		 */
		@Override
		public Object createObject(XMLControl control) {
			return null;
		}

		/**
		 * Loads an object with data from an XMLControl synchronously
		 *
		 * @param control the control
		 * @param obj     the object
		 * @return the loaded object
		 */
		@Override
		public Object loadObject(XMLControl control, Object obj) {
			TFrame frame = (TFrame) obj;
			String[][] tabs = (String[][]) control.getObject("tabs"); //$NON-NLS-1$
			if (tabs == null)
				return loadObjectFinally(frame, null, null);
			FileFilter videoFilter = new VideoFileFilter();
			String base = control.getString("basepath"); //$NON-NLS-1$
			File dataFile = null;
			boolean prev = TrackerIO.loadInSeparateThread;
			TrackerIO.loadInSeparateThread = false;
			for (String[] next : tabs) {
				File file = null;
				Resource res = null;
				if (base != null) {
					file = new File(base, next[1]); // next[1] is relative path
					res = ResourceLoader.getResource(file.getPath());
				}
				if (res == null) {
					file = new File(XML.getUserDirectory(), next[1]);
					res = ResourceLoader.getResource(file.getPath());
				}
				if (res == null && next[0] != null) {
					file = new File(next[0]); // next[0] is absolute path
					res = ResourceLoader.getResource(file.getPath());
				}
				if (res == null) {
					if (OSPRuntime.isJS) {
						JOptionPane.showMessageDialog(frame, 
						"\"" + next[1] + "\" " //$NON-NLS-1$ //$NON-NLS-2$
						+ MediaRes.getString("VideoClip.Dialog.VideoNotFound.Message")); //$NON-NLS-1$
						continue;
					} else /** @j2sNative */ {
						int i = JOptionPane.showConfirmDialog(frame, "\"" + next[1] + "\" " //$NON-NLS-1$ //$NON-NLS-2$
								+ MediaRes.getString("VideoClip.Dialog.VideoNotFound.Message"), //$NON-NLS-1$
								TrackerRes.getString("TFrame.Dialog.FileNotFound.Title"), //$NON-NLS-1$
								JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if (i == JOptionPane.YES_OPTION) {
							TrackerIO.getChooser().setSelectedFile(file);
							@SuppressWarnings("deprecation")
							java.io.File[] files = TrackerIO.getChooserFiles("open"); //$NON-NLS-1$
							if (files != null) {
								file = files[0];
							} else {
								continue;
							}
						} else {
							continue;
						}
					}
				}
				// BH! but if the file is set in the prev block, res is still null. So this will
				// not work?
				if (res != null && !videoFilter.accept(file)) {
					if (dataFile == null)
						dataFile = file;
					TrackerIO.openTabFile(file, frame);
				}
			}
			TrackerIO.loadInSeparateThread = prev;
			return loadObjectFinally(frame, dataFile, null);
		}

		/**
		 * Just the tail end of loadObject
		 * 
		 * @param frame
		 * @param dataFile
		 * @param whenDone
		 * @return
		 */
		protected TFrame loadObjectFinally(TFrame frame, File dataFile, Function<Object, Void> whenDone) {
			int n = frame.getTab(dataFile);
			OSPLog.finest("selecting first tabset tab at index " + n); //$NON-NLS-1$
			frame.setSelectedTab(n);
			if (whenDone != null)
				whenDone.apply(frame);
			return frame;
		}

		// BH 2020.04.16 just a sketch; not implemented yet.

		/**
		 * The same as above, just asynchronous (untested).
		 * 
		 * @param control
		 * @param frame
		 * @param whenDone
		 */
		public synchronized void loadObjectAsync(XMLControl control, TFrame frame, Function<Object, Void> whenDone) {
			Object tabs = control.getObject("tabs"); //$NON-NLS-1$
			if (tabs == null) {
				loadObjectFinally(frame, null, whenDone);
				return;
			}
			new State(control, frame, whenDone, (String[][]) tabs).start();
		}

		/**
		 * The essential SwingJS state machine works in Java and JavaScript and consists
		 * of:
		 * 
		 * 1) a set of all possible states (as final static int) Typically, these are
		 * INIT, LOOP (or NEXT), and DONE, but they could be far more complex.
		 * 
		 * 2) a set of final and nonfinal fields that persist only as long as the state
		 * exists.
		 * 
		 * 3) at least one loop defining the course of actions for the state.
		 * 
		 * Action starts with starting of a StateHelper dedicated to this State. When
		 * complete, you can provide a "whenDone" Function or Runnable. Or configure it
		 * any way you want.
		 * 
		 * Action can be interrupted (reversibly) any time by calling
		 * stateHelper.interrupt().
		 * 
		 * Action can be made synchronous or restarted using stateHelper.next(STATE_XXX)
		 * or asynchronous using stateHelper.delayedState(ms, STATE_XXX).
		 * 
		 * @author hansonr
		 *
		 */
		private class State implements StateMachine {

			// possible states

			final static int STATE_IDLE = -1; // used sometimes for animation holds; not used in this class

			final static int STATE_INIT = 0;
			final static int STATE_NEXT = 1;
			final static int STATE_DONE = 2;

			private final StateHelper stateHelper;
			private final TFrame frame;
			private final String[][] tabs;
			private final String base;
			private final VideoFileFilter videoFilter;
			private final boolean wasLoadThread;
			private final Function<Object, Void> whenDone;

			private int index;
			private File dataFile;

			public State(XMLControl control, TFrame frame, Function<Object, Void> whenDone, String[][] tabs) {

				this.whenDone = whenDone;
				this.frame = frame;
				this.base = control.getString("basepath"); //$NON-NLS-1$
				this.tabs = tabs;
				videoFilter = new VideoFileFilter();
				wasLoadThread = TrackerIO.loadInSeparateThread;
				stateHelper = new StateHelper(this);
				TrackerIO.loadInSeparateThread = false;
			}

			public void start() {
				stateHelper.next(STATE_INIT);

			}

			@Override
			public synchronized boolean stateLoop() {

				while (stateHelper.isAlive()) {
					switch (stateHelper.getState()) {
					case STATE_INIT:
						index = 0;
						stateHelper.setState(STATE_NEXT);
						continue;
					case STATE_NEXT:
						// for (String[] next : tabs) {
						if (index >= tabs.length)
							return stateHelper.next(STATE_DONE);
						String[] next = tabs[index];
						File file = null;
						Resource res = null;
						if (base != null) {
							file = new File(base, next[1]); // next[1] is relative path
							res = ResourceLoader.getResource(file.getPath());
						}
						if (res == null) {
							file = new File(XML.getUserDirectory(), next[1]);
							res = ResourceLoader.getResource(file.getPath());
						}
						if (res == null && next[0] != null) {
							file = new File(next[0]); // next[0] is absolute path
							res = ResourceLoader.getResource(file.getPath());
						}
						if (res != null) {
							processResource(res, file);
							continue; // STATE_NEXT
						}
						int i = JOptionPane.showConfirmDialog(frame, "\"" + next[1] + "\" " //$NON-NLS-1$ //$NON-NLS-2$
								+ MediaRes.getString("VideoClip.Dialog.VideoNotFound.Message"), //$NON-NLS-1$
								TrackerRes.getString("TFrame.Dialog.FileNotFound.Title"), //$NON-NLS-1$
								JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if (i != JOptionPane.YES_OPTION) {
							continue;
						}
						TrackerIO.getChooser().setSelectedFile(file);
						TrackerIO.getChooserFilesAsync("open", new Function<File[], Void>() {

							@Override
							public Void apply(File[] files) {
								if (files != null) {
									File file = files[0];
									// BH makes more sense to me...
									processResource(null, file);
								}
								stateHelper.next(STATE_NEXT);
								return null;
							}
						});
						// } // end for
						break;
					case STATE_DONE:
						TrackerIO.loadInSeparateThread = wasLoadThread;
						loadObjectFinally(frame, dataFile, whenDone);
						break;
					default:
					case STATE_IDLE:
						break;
					}
				}
				return false;
			}

			private void processResource(Resource res, File file) {
				if (res != null && !videoFilter.accept(file)) {
					if (dataFile == null)
						dataFile = file;
					TrackerIO.openTabFile(file, frame);
				}
			}

		}

	}

	public void addTrackerPane(boolean changedState) {
		TrackerPanel newPanel = new TrackerPanel();
		if (!changedState)
			newPanel.changed = false;
		addTab(newPanel, new Runnable() {

			@Override
			public void run() {
				setSelectedTab(newPanel);
				JSplitPane pane = getSplitPane(newPanel, 0);
				pane.setDividerLocation(defaultRightDivider);
				refresh();
			}

		});
	}

	/**
	 * a nonnegative number; when 0 we are painting; when positive, some operation
	 * is holding repaints
	 * 
	 */
	private int paintHold = 0;
	
	/**
	 * Increment/decrement the paintHold counter. Will not decrement below 0.
	 *  
	 * @param b true to increment the counter; false to decrement
	 * 
	 */
	public void holdPainting(boolean b) {
		paintHold += (b ? 1 : paintHold > 0 ? -1 : 0);
		OSPLog.debug("TFrame.paintHold=" + paintHold);
		if (b || paintHold == 0)
			tabbedPane.setVisible(!b);
	}

	/**
	 * check the paintHold counter
	 * 
	 * @return true if paintHold is zero
	 * 
	 */
	public boolean isPainting() {
		return paintHold == 0;
	}
	
	/**
	 * For emergency use only!
	 * 
	 */
	public void clearHoldPainting() {
		paintHold = 0;
	}
}
