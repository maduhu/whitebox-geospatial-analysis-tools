/*
 *  Copyright (C) 2011-2012 John Lindsay
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whiteboxgis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import whiteboxgis.user_interfaces.ToolDialog;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.plaf.basic.BasicToolBarUI;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import rastercalculator.RasterCalculator;
import whitebox.cartographic.MapArea;
import whitebox.cartographic.MapInfo;
import whitebox.cartographic.*;
import whitebox.geospatialfiles.RasterLayerInfo;
import whitebox.interfaces.MapLayer.MapLayerType;
import whitebox.interfaces.*;
import whitebox.structures.BoundingBox;
import whitebox.structures.ExtensionFileFilter;
import whitebox.utilities.FileUtilities;
import whitebox.geospatialfiles.VectorLayerInfo;
import whitebox.serialization.MapInfoSerializer;
import whitebox.serialization.MapInfoDeserializer;
import whiteboxgis.user_interfaces.FeatureSelectionPanel;
import whiteboxgis.user_interfaces.SettingsDialog;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class WhiteboxGui extends JFrame implements ThreadListener, ActionListener, WhiteboxPluginHost, Communicator {

    private static PluginService pluginService = null;
    StatusBar status = new StatusBar(this);
    // common variables
    static private String versionNumber = "2.1.0 BETA (Milestone 2)";
    private String applicationDirectory;
    private String resourcesDirectory;
    private String graphicsDirectory;
    private String pluginDirectory;
    private String helpDirectory;
    private String workingDirectory;
    private String paletteDirectory;
    private String defaultQuantPalette;
    private String defaultQualPalette;
    private String pathSep;
    private String toolboxFile;
    private String propsFile;
    private String userName;
    private int splitterLoc1 = 250;
    private int splitterToolboxLoc;
    private int tbTabsIndex = 0;
    private int qlTabsIndex = 0;
    private int[] selectedMapAndLayer = new int[3];
    private boolean linkAllOpenMaps = false;
    private boolean hideAlignToolbar = true;
    // Gui items.
    private JList allTools;
    private JList recentTools;
    private JList mostUsedTools;
    private JTextArea textArea = new JTextArea();
    private JSplitPane splitPane;
    private JSplitPane splitPane2;
    private JSplitPane splitPane3;
    private JTabbedPane tabs = new JTabbedPane();
    private JTree tree = null;
    private JTabbedPane qlTabs = null;
    private JTabbedPane tb = null;
    private MapRenderer2 drawingArea = new MapRenderer2();
    private ArrayList<MapInfo> openMaps = new ArrayList<>();
    private int activeMap = 0;
    private int numOpenMaps = 1;
    private JPopupMenu layersPopup = null;
    private JPopupMenu mapsPopup = null;
    private JPopupMenu mapAreaPopup = null;
    private JPopupMenu textPopup = null;
    private JButton pan = null;
    private JButton zoomIntoBox = null;
    private JButton zoomOut = null;
    private JButton select = null;
    private JButton modifyPixelsVals = null;
    private JButton distanceToolButton = null;
    private JCheckBoxMenuItem modifyPixels = null;
    private JCheckBoxMenuItem zoomToBox = null;
    private JCheckBoxMenuItem panMenuItem = null;
    private JCheckBoxMenuItem selectMenuItem = null;
    private JCheckBoxMenuItem distanceToolMenuItem = null;
    private JCheckBoxMenuItem linkMap = null;
    private JCheckBoxMenuItem wordWrap = null;
    private JMenuItem menuItemHisto = null;
    private JMenuItem menuItemAttributeTable = null;
    private JTextField searchText = new JTextField();
    private JMenuItem menuChangePalette = null;
    private JMenuItem menuReversePalette = null;
    private HashMap<String, ImageIcon> icons = new HashMap<String, ImageIcon>();
    private HashMap<String, Font> fonts = new HashMap<String, Font>();
    private JTextField scaleText = new JTextField();
    private PageFormat defaultPageFormat = new PageFormat();

    public static void main(String[] args) {

        //setLookAndFeel("Nimbus");
        setLookAndFeel("systemLAF");
        if (System.getProperty("os.name").contains("Mac")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            //System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Whitebox GAT");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
            //System.setProperty("Xdock:name", "Whitebox");
            System.setProperty("apple.awt.fileDialogForDirectories", "true");

            System.setProperty("apple.awt.textantialiasing", "true");

            System.setProperty("apple.awt.graphics.EnableQ2DX", "true");
        }

        WhiteboxGui wb = new WhiteboxGui();
        wb.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        wb.setVisible(true);
    }
    private String retFile;
    private boolean flag = true;

    private void findFile(File dir, String fileName) {
        if (flag) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    findFile(files[i], fileName);
                } else if (files[i].getName().equals(fileName)) {
                    retFile = files[i].toString();
                    flag = false;
                    break;
                }
            }
        }
    }

    private static void setLookAndFeel(String lafName) {
        try {

            if (lafName.equals("systemLAF")) {
                lafName = getSystemLookAndFeelName();
            }

            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (lafName.equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }


        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static String getSystemLookAndFeelName() {
        try {
            String className = UIManager.getSystemLookAndFeelClassName();
            String name = null;
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (className.equals(info.getClassName())) {
                    name = info.getName();
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            return name;
        } catch (Exception e) {
            return null;
        }
    }

    public WhiteboxGui() {
        super("Whitebox GAT v." + versionNumber);
        try {
            // initialize the pathSep and GraphicsDirectory variables
            pathSep = File.separator;

            applicationDirectory = java.net.URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            //getClass().getProtectionDomain().
            if (applicationDirectory.endsWith(".exe") || applicationDirectory.endsWith(".jar")) {
                applicationDirectory = new File(applicationDirectory).getParent();
            } else {
                // Add the path to the class files
                applicationDirectory += getClass().getName().replace('.', File.separatorChar);

                // Step one level up as we are only interested in the
                // directory containing the class files
                applicationDirectory = new File(applicationDirectory).getParent();
            }

            resourcesDirectory = applicationDirectory + pathSep + "resources" + pathSep;
            graphicsDirectory = resourcesDirectory + "Images" + pathSep;
            helpDirectory = resourcesDirectory + "Help" + pathSep;
            pluginDirectory = resourcesDirectory + "plugins" + pathSep;
            toolboxFile = resourcesDirectory + "toolbox.xml";
            propsFile = resourcesDirectory + "app.config";
            workingDirectory = resourcesDirectory + "samples" + pathSep;
            paletteDirectory = resourcesDirectory + "palettes" + pathSep;

            findFile(new File(applicationDirectory + pathSep), "wbGAT.png");
            if (retFile != null) {
                this.setIconImage(new ImageIcon(retFile).getImage());

            }

            drawingArea.setPrintResolution(printResolution);

            callSplashScreen();

            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // set the application icon
            String imgLocation = graphicsDirectory + "wbGAT.png";
            setIconImage(Toolkit.getDefaultToolkit().getImage(imgLocation));

            String[] fontName = {"Serif", "SanSerif", "Monospaced", "Dialog", "DialogInput"};

            fonts.put("root", new Font(fontName[1], Font.PLAIN, 12));
            fonts.put("activeMap", new Font(fontName[1], Font.BOLD, 12));
            fonts.put("inactiveMap", new Font(fontName[1], Font.PLAIN, 12));
            fonts.put("inactiveLayer", new Font(fontName[1], Font.PLAIN, 12));
            fonts.put("activeLayer", new Font(fontName[1], Font.BOLD, 12));
            icons.put("root", new ImageIcon(graphicsDirectory + "map.png", ""));
            icons.put("activeMap", new ImageIcon(graphicsDirectory + "map.png", ""));
            icons.put("inactiveMap", new ImageIcon(graphicsDirectory + "map.png", ""));
            icons.put("activeLayer", new ImageIcon(graphicsDirectory + "rgb.png", ""));
            icons.put("inactiveLayer", new ImageIcon(graphicsDirectory + "rgb.png", ""));

            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    close();
                }
            });

            this.loadPlugins();
            this.getApplicationProperties();

            if (defaultQuantPalette.equals("")) {
                defaultQuantPalette = "spectrum.pal";
            }
            if (defaultQualPalette.equals("")) {
                defaultQualPalette = "qual.pal";
            }

            this.createGui();

            checkForNewInstallation();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    private ArrayList<PluginInfo> plugInfo = null;

    private void checkForNewInstallation() {
        if (userName == null || !userName.equals(System.getProperty("user.name"))) {
            refreshToolUsage();
            userName = System.getProperty("user.name");
//            String message = "Welcome to Whitebox " + userName + ".\n" 
//                    + "System memory = " + ((com.sun.management.OperatingSystemMXBean) ManagementFactory
//                .getOperatingSystemMXBean()).getTotalPhysicalMemorySize() / 1073741824.0 + " Gb\n" 
//                    + "Max heap size = " + Runtime.getRuntime().maxMemory() / 1073741824.0 + " Gb";
            String message = "Welcome to Whitebox " + userName + ".";
            showFeedback(message);
        }
    }

    private void loadPlugins() {
        pluginService = PluginServiceFactory.createPluginService(pluginDirectory);
        pluginService.initPlugins();
        plugInfo = pluginService.getPluginList();
    }
    ArrayList<WhiteboxPlugin> activePlugs = new ArrayList<WhiteboxPlugin>();

    @Override
    public List returnPluginList() {
        List<String> ret = new ArrayList<String>();
        for (int i = 0; i < plugInfo.size(); i++) {
            ret.add(plugInfo.get(i).getName());
        }
        Collections.sort(ret);
        return ret;
    }

    @Override
    public void runPlugin(String pluginName, String[] args) {
        try {
            WhiteboxPlugin plug = pluginService.getPlugin(pluginName, StandardPluginService.SIMPLE_NAME);
            plug.setPluginHost(this);
            plug.setArgs(args);
            activePlugs.add(plug);
            if (plug instanceof NotifyingThread) {
                NotifyingThread t = (NotifyingThread) (plug);
                t.addListener(this);
            }
            new Thread(plug).start();

            //pool.submit(plug);
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
        }
    }
    private boolean automaticallyDisplayReturns = true;

    @Override
    public void returnData(Object ret) {
        // this is where all of the data returned by plugins is handled.
        if (ret instanceof String) {
            String retStr = ret.toString();
            if (retStr.endsWith(".dep") && retStr.contains(pathSep)) {
                if (automaticallyDisplayReturns) {
                    displayLayer(retStr);
                }
            } else if (retStr.endsWith(".shp") && retStr.contains(pathSep)) {
                if (automaticallyDisplayReturns) {
                    displayLayer(retStr);
                }
            } else if (retStr.toLowerCase().endsWith(".dbf") && retStr.contains(pathSep)) {
                AttributesFileViewer afv = new AttributesFileViewer(this, false, retStr.replace(".dbf", ".shp"));
                int height = 500;
                afv.setSize((int) (height * 1.61803399), height); // golden ratio.
                afv.setVisible(true);
            } else if (retStr.endsWith(".html") && retStr.contains(pathSep)) {
                // display this markup in a webbrowser component
                try {
                    JFrame frame = new HTMLViewer(retStr);
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.setSize(600, 600);
                    frame.setVisible(true);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
//            } else if (retStr.contains("DOCTYPE html")) {
//                // display this markup in a webbrowser component
//                try {
//                    JFrame frame = new HTMLViewer(retStr);
//                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//                    frame.setSize(600, 600);
//                    frame.setVisible(true);
//                } catch (Exception e) {
//                }
            } else {
                // display the text area, if it's not already.
                if (splitPane3.getDividerLocation() / splitPane3.getHeight() < 0.75) {
                    splitPane3.setDividerLocation(0.75);
                }
                textArea.setText("");
                textArea.setText(retStr);
            }
        } else if (ret instanceof JPanel) {
            // Create a dialog and place it in that. Then display the dialog.
            JDialog dialog = new JDialog(this, "");
            Container contentPane = dialog.getContentPane();
            JPanel panel = (JPanel) ret;
            contentPane.add(panel, BorderLayout.CENTER);
            int k = panel.getPreferredSize().height;
            if (panel.getPreferredSize().height > 100) {
                dialog.setPreferredSize(panel.getPreferredSize());
            } else {
                dialog.setPreferredSize(new Dimension(500, 500));
            }
            dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            dialog.pack();
            dialog.setVisible(true);
        }
    }

    @Override
    public void launchDialog(String pluginName) {

        for (int i = 0; i < plugInfo.size(); i++) {
            if (plugInfo.get(i).getDescriptiveName().equals(pluginName)) {
                plugInfo.get(i).setLastUsedToNow();
                plugInfo.get(i).incrementNumTimesUsed();
                break;
            }
        }

        populateToolTabs();

        WhiteboxPlugin plug = pluginService.getPlugin(pluginName, StandardPluginService.DESCRIPTIVE_NAME);

        // does this plugin provide it's own dialog?
        boolean pluginProvidesDialog = false;

        String parameterFile = resourcesDirectory + "plugins"
                + pathSep + "Dialogs" + pathSep + plug.getName() + ".xml";
        File file = new File(parameterFile);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            Node topNode = doc.getFirstChild();
            Element docElement = doc.getDocumentElement();

            NodeList nl = docElement.getElementsByTagName("DialogComponent");
            String componentType;
            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {

                    Element el = (Element) nl.item(i);
                    componentType = el.getAttribute("type");

                    if (componentType.equals("CustomDialogProvidedByPlugin")) {
                        pluginProvidesDialog = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        if (pluginProvidesDialog) {
            String[] args = {""};
            runPlugin(plug.getName(), args);
        } else {
            // use the xml-based dialog provided in the Dialog folder.
            String helpFile = helpDirectory + plug.getName() + ".html";
            ToolDialog dlg = new ToolDialog(this, false, plug.getName(), plug.getDescriptiveName(), helpFile);
            dlg.setSize(800, 400);
            dlg.setVisible(true);
        }
    }

    @Override
    public void cancelOperation() {
        Iterator<WhiteboxPlugin> iterator = activePlugs.iterator();
        while (iterator.hasNext()) {
            WhiteboxPlugin plugin = iterator.next();
            plugin.setCancelOp(true);
        }
        activePlugs.clear();
    }

    @Override
    public void pluginComplete() {
        // remove inactive plugins from activePlugs.
        Iterator<WhiteboxPlugin> iterator = activePlugs.iterator();
        ArrayList<WhiteboxPlugin> toRemove = new ArrayList<WhiteboxPlugin>();
        while (iterator.hasNext()) {
            WhiteboxPlugin plugin = iterator.next();
            if (!plugin.isActive()) {
                toRemove.add(plugin);
            }
        }
        activePlugs.removeAll(toRemove);
    }

    @Override
    public void refreshMap(boolean updateLayers) {
        try {
            drawingArea.repaint();
            if (updateLayers) {
                legendEntries.clear();
                updateLayersTab();
            }
        } catch (Exception e) {
            showFeedback(e.getStackTrace().toString());
        }
    }

    @Override
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String getApplicationDirectory() {
        return applicationDirectory;
    }

    @Override
    public void setApplicationDirectory(String applicationDirectory) {
        this.applicationDirectory = applicationDirectory;
    }

    @Override
    public String getResourcesDirectory() {
        return resourcesDirectory;
    }

    private void getApplicationProperties() {
        // see if the app.config file exists
        File propertiesFile = new File(propsFile);
        if (propertiesFile.exists()) {
            Properties props = new Properties();

            try {
                FileInputStream in = new FileInputStream(propsFile);
                props.load(in);
                workingDirectory = props.getProperty("workingDirectory");
                File wd = new File(workingDirectory);
                if (!wd.exists()) {
                    workingDirectory = resourcesDirectory + "samples";
                }
                splitterLoc1 = Integer.parseInt(props.getProperty("splitterLoc1"));
                splitterToolboxLoc = Integer.parseInt(props.getProperty("splitterToolboxLoc"));
                tbTabsIndex = Integer.parseInt(props.getProperty("tbTabsIndex"));
                qlTabsIndex = Integer.parseInt(props.getProperty("qlTabsIndex"));
                defaultQuantPalette = props.getProperty("defaultQuantPalette");
                defaultQualPalette = props.getProperty("defaultQualPalette");
                defaultQuantPalette = defaultQuantPalette.replace(".plt", ".pal"); // just in case the old style palette is specified.
                defaultQualPalette = defaultQualPalette.replace(".plt", ".pal");
                userName = props.getProperty("userName");
                defaultPageFormat.setOrientation(Integer.parseInt(props.getProperty("defaultPageOrientation")));
                double width = Float.parseFloat(props.getProperty("defaultPageWidth"));
                double height = Float.parseFloat(props.getProperty("defaultPageHeight"));
                Paper paper = defaultPageFormat.getPaper();
                paper.setSize(width, height);
                defaultPageFormat.setPaper(paper);
                if (props.containsKey("printResolution")) {
                    printResolution = Integer.parseInt(props.getProperty("printResolution"));
                }
                if (props.containsKey("hideAlignToolbar")) {
                    hideAlignToolbar = Boolean.parseBoolean(props.getProperty("hideAlignToolbar"));
                }
                // retrieve the plugin usage information
                String[] pluginNames = props.getProperty("pluginNames").split(",");
                String[] pluginUsage = props.getProperty("pluginUsage").split(",");
                String[] pluginLastUse = props.getProperty("toolLastUse").split(",");
                String plugName;
                DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
                Date lastUsed = null;
                for (int i = 0; i < plugInfo.size(); i++) {
                    plugName = plugInfo.get(i).getName();
                    for (int j = 0; j < pluginNames.length; j++) {
                        if (pluginNames[j].equals(plugName)) {
                            try {
                                lastUsed = df.parse(pluginLastUse[j]);
                            } catch (ParseException e) {
                            }
                            plugInfo.get(i).setLastUsed(lastUsed);
                            plugInfo.get(i).setNumTimesUsed(Integer.parseInt(pluginUsage[j]));
                        }
                    }
                }

            } catch (IOException e) {
                showFeedback("Error while reading properties file.");
            }

        } else {
            workingDirectory = resourcesDirectory + "samples";
            splitterLoc1 = 250;
            splitterToolboxLoc = 250;
            tbTabsIndex = 0;
            qlTabsIndex = 0;
            defaultQuantPalette = "spectrum.pal";
            defaultQualPalette = "qual.pal";

            int k = 0;
        }
    }

    private void setApplicationProperties() {
        // see if the app.config file exists
        //File propertiesFile = new File(propsFile);
        if (!(new File(propsFile).exists())) {
            return;
        }
        Properties props = new Properties();
        props.setProperty("workingDirectory", workingDirectory);
        props.setProperty("splitterLoc1", Integer.toString(splitPane.getDividerLocation() - 2));
        props.setProperty("splitterToolboxLoc", Integer.toString(qlTabs.getSize().height));
        //props.setProperty("splitterLocTextArea",
        //        Integer.toString(splitPane3.getDividerLocation()));
        props.setProperty("tbTabsIndex", Integer.toString(tb.getSelectedIndex()));
        props.setProperty("qlTabsIndex", Integer.toString(qlTabs.getSelectedIndex()));
        props.setProperty("defaultQuantPalette", defaultQuantPalette);
        props.setProperty("defaultQualPalette", defaultQualPalette);
        props.setProperty("userName", System.getProperty("user.name"));
        props.setProperty("defaultPageOrientation", Integer.toString(defaultPageFormat.getOrientation()));
        props.setProperty("defaultPageHeight", Double.toString(defaultPageFormat.getPaper().getHeight()));
        props.setProperty("defaultPageWidth", Double.toString(defaultPageFormat.getPaper().getWidth()));
        props.setProperty("printResolution", Integer.toString(getPrintResolution()));
        props.setProperty("hideAlignToolbar", Boolean.toString(hideAlignToolbar));

        // set the tool usage properties

        // first sort plugInfo alphabetacally.
        for (int i = 0; i < plugInfo.size(); i++) {
            plugInfo.get(i).setSortMode(PluginInfo.SORT_MODE_NAMES);
        }
        Collections.sort(plugInfo);

        // create a string of tool names
        String toolNames = plugInfo.get(0).getName();
        for (int i = 1; i < plugInfo.size(); i++) {
            toolNames += "," + plugInfo.get(i).getName();
        }
        props.setProperty("pluginNames", toolNames);

        // create a string of tool usage
        String toolUsage = Integer.toString(plugInfo.get(0).getNumTimesUsed());
        for (int i = 1; i < plugInfo.size(); i++) {
            toolUsage += "," + Integer.toString(plugInfo.get(i).getNumTimesUsed());
        }
        props.setProperty("pluginUsage", toolUsage);

        // create a string of tool last used dates
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
        String toolLastUse = df.format(plugInfo.get(0).getLastUsed());
        for (int i = 1; i < plugInfo.size(); i++) {
            toolLastUse += "," + df.format(plugInfo.get(i).getLastUsed());
        }
        props.setProperty("toolLastUse", toolLastUse);


        try {
            FileOutputStream out = new FileOutputStream(propsFile);
            props.store(out, "--No comments--");
            out.close();
        } catch (IOException e) {
            // do nothing
        }

    }

    private void showToolDescription(String pluginName) {
        WhiteboxPlugin plug = pluginService.getPlugin(pluginName, StandardPluginService.DESCRIPTIVE_NAME);
        status.setMessage(plug.getToolDescription());
    }

    private void createGui() {
        try {
            // add the menubar and toolbar
            createMenu();
            createPopupMenus();
            createToolbar();
            createCartoElementToolbar();

            MapInfo mapinfo = new MapInfo("Map1");
            mapinfo.setMapName("Map1");
            mapinfo.setPageFormat(defaultPageFormat);
            mapinfo.setWorkingDirectory(workingDirectory);

            MapArea ma = new MapArea("MapArea1");
            ma.setUpperLeftX(-32768);
            ma.setUpperLeftY(-32768);
            mapinfo.addNewCartographicElement(ma);

            openMaps.add(mapinfo);
            activeMap = 0;
            drawingArea.setMapInfo(mapinfo);
            drawingArea.setStatusBar(status);
            drawingArea.setScaleText(scaleText);
            drawingArea.setHost(this);


            textArea.setLineWrap(false);
            textArea.setWrapStyleWord(false);
            MouseListener ml = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    TextAreaMousePress(e);
                }
            };
            textArea.addMouseListener(ml);
            JScrollPane scrollText = new JScrollPane(textArea);
            scrollText.setMinimumSize(new Dimension(0, 0));
            splitPane3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, drawingArea, scrollText);
            splitPane3.setResizeWeight(1.0);
            splitPane3.setOneTouchExpandable(true);
            splitPane3.setDividerLocation(1.0);

            tb = createTabbedPane();
            tb.setMaximumSize(new Dimension(150, 50));
            tb.setPreferredSize(new Dimension(splitterLoc1, 50));
            tb.setSelectedIndex(tbTabsIndex);
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tb, splitPane3);
            splitPane.setResizeWeight(0);
            splitPane.setOneTouchExpandable(false);
            splitPane.setDividerSize(3);
            this.getContentPane().add(splitPane);

            // add the status bar
            this.getContentPane().add(status, java.awt.BorderLayout.SOUTH);

            if (System.getProperty("os.name").contains("Mac")) {

                try {
                    Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
                    Class params[] = new Class[2];
                    params[0] = Window.class;
                    params[1] = Boolean.TYPE;
                    Method method = util.getMethod("setWindowCanFullScreen", params);
                    method.invoke(util, this, true);
                } catch (ClassNotFoundException e) {
                    // log exception
                } catch (NoSuchMethodException e) {
                    // log exception
                    //} catch (InvocationTargetException e) {
                    // log exception
                } catch (IllegalAccessException e) {
                    // log exception
                }

                this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
            }
            this.setMinimumSize(new Dimension(700, 500));
            this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);

            // set the message indicating the number of plugins that were located.
            status.setMessage(" " + plugInfo.size() + " plugins were located");
        } catch (Exception e) {
            showFeedback(e.getMessage());
        }
    }

    private void createMenu() {
        try {
            JMenuBar menubar = new JMenuBar();

            JMenuItem newMap = new JMenuItem("New Map", new ImageIcon(graphicsDirectory + "map.png"));
            JMenuItem openMap = new JMenuItem("Open Map", new ImageIcon(graphicsDirectory + "open.png"));
            JMenuItem saveMap = new JMenuItem("Save Map", new ImageIcon(graphicsDirectory + "SaveMap.png"));
            JMenuItem closeMap = new JMenuItem("Close Map");
            JMenuItem close = new JMenuItem("Close");

            JMenuItem layerProperties = new JMenuItem("Layer Display Properties");
            layerProperties.setActionCommand("layerProperties");
            layerProperties.addActionListener(this);
            JMenuItem options = new JMenuItem("Options and Settings");

            JMenuItem rasterCalc = new JMenuItem("Raster Calculator", new ImageIcon(graphicsDirectory + "RasterCalculator.png"));
            modifyPixels = new JCheckBoxMenuItem("Modify Pixel Values", new ImageIcon(graphicsDirectory + "ModifyPixels.png"));
            JMenuItem paletteManager = new JMenuItem("Palette Manager", new ImageIcon(graphicsDirectory + "paletteManager.png"));
            JMenuItem refreshTools = new JMenuItem("Refresh Tool Usage");


            // File menu
            JMenu FileMenu = new JMenu("File");
            FileMenu.add(newMap);
            newMap.setActionCommand("newMap");
            newMap.addActionListener(this);
            newMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            FileMenu.add(openMap);
            openMap.setActionCommand("openMap");
            openMap.addActionListener(this);
            openMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            FileMenu.add(saveMap);
            saveMap.addActionListener(this);
            saveMap.addActionListener(this);
            saveMap.setActionCommand("saveMap");
            saveMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            FileMenu.add(closeMap);
            closeMap.setActionCommand("closeMap");
            //closeMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            closeMap.addActionListener(this);
            FileMenu.addSeparator();
            JMenuItem printMap = new JMenuItem("Print Map", new ImageIcon(graphicsDirectory + "Print.png"));
            FileMenu.add(printMap);
            printMap.addActionListener(this);
            printMap.setActionCommand("printMap");
            printMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            JMenuItem exportMap = new JMenuItem("Export Map As Image");
            FileMenu.add(exportMap);
            exportMap.addActionListener(this);
            exportMap.setActionCommand("exportMapAsImage");
            if (System.getProperty("os.name").contains("Mac") == false) {
                FileMenu.addSeparator();
                FileMenu.add(close);
                close.setActionCommand("close");
                close.addActionListener(this);
            }
            menubar.add(FileMenu);

            // Layers menu
            JMenu LayersMenu = new JMenu("Data Layers");
            JMenuItem addLayers = new JMenuItem("Add Layers to Map", new ImageIcon(graphicsDirectory + "AddLayer.png"));
            LayersMenu.add(addLayers);
            addLayers.addActionListener(this);
            addLayers.setActionCommand("addLayer");
            addLayers.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            JMenuItem removeLayers = new JMenuItem("Remove Active Layer From Map", new ImageIcon(graphicsDirectory + "RemoveLayer.png"));
            LayersMenu.add(removeLayers);
            removeLayers.addActionListener(this);
            removeLayers.setActionCommand("removeLayer");
            removeLayers.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            JMenuItem removeAllLayers = new JMenuItem("Remove All Layers");
            LayersMenu.add(removeAllLayers);
            removeAllLayers.addActionListener(this);
            removeAllLayers.setActionCommand("removeAllLayers");
            JMenuItem allLayersInvisible = new JMenuItem("Hide All Layers");
            LayersMenu.add(allLayersInvisible);
            allLayersInvisible.addActionListener(this);
            allLayersInvisible.setActionCommand("allLayersInvisible");
            JMenuItem allLayersVisible = new JMenuItem("Show All Layers");
            LayersMenu.add(allLayersVisible);
            allLayersVisible.addActionListener(this);
            allLayersVisible.setActionCommand("allLayersVisible");
            LayersMenu.addSeparator();
            JMenuItem raiseLayers = new JMenuItem("Raise Layer", new ImageIcon(graphicsDirectory + "PromoteLayer.png"));
            LayersMenu.add(raiseLayers);
            raiseLayers.addActionListener(this);
            raiseLayers.setActionCommand("raiseLayer");
            JMenuItem lowerLayers = new JMenuItem("Lower Layer", new ImageIcon(graphicsDirectory + "DemoteLayer.png"));
            LayersMenu.add(lowerLayers);
            lowerLayers.addActionListener(this);
            lowerLayers.setActionCommand("lowerLayer");
            JMenuItem layerToTop = new JMenuItem("Layer to Top", new ImageIcon(graphicsDirectory + "LayerToTop.png"));
            LayersMenu.add(layerToTop);
            layerToTop.addActionListener(this);
            layerToTop.setActionCommand("layerToTop");
            JMenuItem layerToBottom = new JMenuItem("Layer to Bottom", new ImageIcon(graphicsDirectory + "LayerToBottom.png"));
            LayersMenu.add(layerToBottom);
            layerToBottom.addActionListener(this);
            layerToBottom.setActionCommand("layerToBottom");
            LayersMenu.addSeparator();
            JMenuItem clipLayerToExtent = new JMenuItem("Clip Layer to Current Extent");
            clipLayerToExtent.addActionListener(this);
            clipLayerToExtent.setActionCommand("clipLayerToExtent");
            LayersMenu.add(clipLayerToExtent);
            menubar.add(LayersMenu);



            // View menu
            JMenu viewMenu = new JMenu("View");
            zoomToBox = new JCheckBoxMenuItem("Zoom to Box", new ImageIcon(graphicsDirectory + "ZoomIn.png"));
            viewMenu.add(zoomToBox);
            zoomToBox.addActionListener(this);
            zoomToBox.setActionCommand("zoomToBox");
            panMenuItem = new JCheckBoxMenuItem("Pan", new ImageIcon(graphicsDirectory + "Pan2.png"));
            viewMenu.add(panMenuItem);
            panMenuItem.addActionListener(this);
            panMenuItem.setActionCommand("pan");
            selectMenuItem = new JCheckBoxMenuItem("Select", new ImageIcon(graphicsDirectory + "select.png"));
            viewMenu.add(selectMenuItem);
            selectMenuItem.addActionListener(this);
            selectMenuItem.setActionCommand("select");
            JMenuItem zoomToFullExtent = new JMenuItem("Zoom Map Area to Full Extent", new ImageIcon(graphicsDirectory + "Globe.png"));
            zoomToFullExtent.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            viewMenu.add(zoomToFullExtent);
            zoomToFullExtent.addActionListener(this);
            zoomToFullExtent.setActionCommand("zoomToFullExtent");
//            JMenuItem zoomToActiveLayer = new JMenuItem("Zoom to Active Layer", new ImageIcon(graphicsDirectory + "ZoomToActiveLayer.png"));
//            viewMenu.add(zoomToActiveLayer);
//            zoomToActiveLayer.addActionListener(this);
//            zoomToActiveLayer.setActionCommand("zoomToLayer");
//            zoomToActiveLayer.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//            JMenuItem zoomIn = new JMenuItem("Zoom In", new ImageIcon(graphicsDirectory + "ZoomIn.png"));
//            viewMenu.add(zoomIn);
//            zoomIn.addActionListener(this);
//            zoomIn.setActionCommand("zoomIn");
//            zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            JMenuItem zoomOut = new JMenuItem("Zoom Out", new ImageIcon(graphicsDirectory + "ZoomOut.png"));
            viewMenu.add(zoomOut);
            zoomOut.addActionListener(this);
            zoomOut.setActionCommand("zoomOut");
            zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            zoomToBox.setState(true);
            panMenuItem.setState(false);

            JMenuItem panLeft = new JMenuItem("Pan Left");
            viewMenu.add(panLeft);
            panLeft.addActionListener(this);
            panLeft.setActionCommand("panLeft");
            panLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            JMenuItem panRight = new JMenuItem("Pan Right");
            viewMenu.add(panRight);
            panRight.addActionListener(this);
            panRight.setActionCommand("panRight");
            panRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            JMenuItem panUp = new JMenuItem("Pan Up");
            viewMenu.add(panUp);
            panUp.addActionListener(this);
            panUp.setActionCommand("panUp");
            panUp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            JMenuItem panDown = new JMenuItem("Pan Down");
            viewMenu.add(panDown);
            panDown.addActionListener(this);
            panDown.setActionCommand("panDown");
            panDown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            JMenuItem previousExtent = new JMenuItem("Previous Extent", new ImageIcon(graphicsDirectory + "back.png"));
            viewMenu.add(previousExtent);
            previousExtent.addActionListener(this);
            previousExtent.setActionCommand("previousExtent");
            JMenuItem nextExtent = new JMenuItem("Next Extent", new ImageIcon(graphicsDirectory + "forward.png"));
            viewMenu.add(nextExtent);
            nextExtent.addActionListener(this);
            nextExtent.setActionCommand("nextExtent");

            viewMenu.addSeparator();
            JMenuItem refresh = new JMenuItem("Refresh Map");
            viewMenu.add(refresh);
            refresh.addActionListener(this);
            refresh.setActionCommand("refreshMap");

            linkMap = new JCheckBoxMenuItem("Link Open Maps");
            viewMenu.add(linkMap);
            linkMap.setActionCommand("linkMap");
            linkMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            linkMap.addActionListener(this);

            viewMenu.addSeparator();
            JMenuItem mapProperties = new JMenuItem("Map Properties");
            mapProperties.addActionListener(this);
            mapProperties.setActionCommand("mapProperties");
            viewMenu.add(mapProperties);
            viewMenu.add(layerProperties);
            JMenuItem viewHistogram = new JMenuItem("View Histogram");
            viewHistogram.setActionCommand("viewHistogram");
            viewHistogram.addActionListener(this);
            viewMenu.add(viewHistogram);
            viewMenu.add(options);
            options.addActionListener(this);
            options.setActionCommand("options");

            menubar.add(viewMenu);


            // Cartographic menu
            JMenu cartoMenu = new JMenu("Cartographic");

            JMenuItem insertTitle = new JMenuItem("Insert Map Title");
            cartoMenu.add(insertTitle);
            insertTitle.addActionListener(this);
            insertTitle.setActionCommand("insertTitle");

            JMenuItem insertNorthArrow = new JMenuItem("Insert North Arrow");
            cartoMenu.add(insertNorthArrow);
            insertNorthArrow.addActionListener(this);
            insertNorthArrow.setActionCommand("insertNorthArrow");

            JMenuItem insertScale = new JMenuItem("Insert Scale");
            cartoMenu.add(insertScale);
            insertScale.addActionListener(this);
            insertScale.setActionCommand("insertScale");

            JMenuItem insertLegend = new JMenuItem("Insert Legend");
            cartoMenu.add(insertLegend);
            insertLegend.addActionListener(this);
            insertLegend.setActionCommand("insertLegend");

            JMenuItem insertNeatline = new JMenuItem("Insert Neatline");
            cartoMenu.add(insertNeatline);
            insertNeatline.addActionListener(this);
            insertNeatline.setActionCommand("insertNeatline");

            JMenuItem insertMapArea = new JMenuItem("Insert Map Area", new ImageIcon(graphicsDirectory + "mapArea.png"));
            cartoMenu.add(insertMapArea);
            insertMapArea.addActionListener(this);
            insertMapArea.setActionCommand("insertMapArea");

            JMenuItem insertTextArea = new JMenuItem("Insert Text Area");
            cartoMenu.add(insertTextArea);
            insertTextArea.addActionListener(this);
            insertTextArea.setActionCommand("insertTextArea");

            cartoMenu.addSeparator();

            JMenuItem pageProps = new JMenuItem("Page Properties", new ImageIcon(graphicsDirectory + "page.png"));
            cartoMenu.add(pageProps);
            pageProps.addActionListener(this);
            pageProps.setActionCommand("pageProps");

            menubar.add(cartoMenu);


            // Tools menu
            JMenu ToolsMenu = new JMenu("Tools");
            ToolsMenu.add(rasterCalc);
            rasterCalc.setActionCommand("rasterCalculator");
            rasterCalc.addActionListener(this);
            ToolsMenu.add(paletteManager);
            paletteManager.addActionListener(this);
            paletteManager.setActionCommand("paletteManager");
            paletteManager.addActionListener(this);
            ToolsMenu.add(modifyPixels);
            modifyPixels.addActionListener(this);
            modifyPixels.setActionCommand("modifyPixels");
            distanceToolMenuItem = new JCheckBoxMenuItem("Measure Distance", new ImageIcon(graphicsDirectory + "DistanceTool.png"));
            ToolsMenu.add(distanceToolMenuItem);
            distanceToolMenuItem.addActionListener(this);
            refreshTools.setActionCommand("distanceTool");
            ToolsMenu.add(refreshTools);
            refreshTools.addActionListener(this);
            refreshTools.setActionCommand("refreshTools");
            JMenuItem newHelp = new JMenuItem("Create New Help Entry");
            newHelp.addActionListener(this);
            newHelp.setActionCommand("newHelp");
            ToolsMenu.add(newHelp);
            menubar.add(ToolsMenu);
            JMenuItem scripter = new JMenuItem("Scripter", new ImageIcon(graphicsDirectory + "ScriptIcon2.png"));
            scripter.addActionListener(this);
            scripter.setActionCommand("scripter");
            ToolsMenu.add(scripter);

            // Help menu
            JMenu HelpMenu = new JMenu("Help");
            JMenuItem helpIndex = new JMenuItem("Index", new ImageIcon(graphicsDirectory + "help.png"));
            HelpMenu.add(helpIndex);
            helpIndex.setActionCommand("helpIndex");
            helpIndex.addActionListener(this);
            JMenuItem helpSearch = new JMenuItem("Search");
            helpSearch.setActionCommand("helpSearch");
            helpSearch.addActionListener(this);
            HelpMenu.add(helpSearch);
            JMenuItem helpAbout = new JMenuItem("About Whitebox GAT");
            helpAbout.setActionCommand("helpAbout");
            helpAbout.addActionListener(this);
            HelpMenu.add(helpAbout);

            HelpMenu.addSeparator();

            JMenuItem helpReport = new JMenuItem("Help Completeness Report");
            helpReport.setActionCommand("helpReport");
            helpReport.addActionListener(this);
            HelpMenu.add(helpReport);

            menubar.add(HelpMenu);


            this.setJMenuBar(menubar);

        } catch (Exception e) {
            showFeedback(e.getMessage());
        }
    }

    private void createPopupMenus() {
        // layers menu
        layersPopup = new JPopupMenu();

        JMenuItem mi = new JMenuItem("Layer Display Properties");
        mi.addActionListener(this);
        mi.setActionCommand("layerProperties");
        layersPopup.add(mi);

        menuItemHisto = new JMenuItem("View Histogram");
        menuItemHisto.addActionListener(this);
        menuItemHisto.setActionCommand("viewHistogram");
        layersPopup.add(menuItemHisto);

        menuItemAttributeTable = new JMenuItem("View Attribute Table");
        menuItemAttributeTable.addActionListener(this);
        menuItemAttributeTable.setActionCommand("viewAttributeTable");
        layersPopup.add(menuItemAttributeTable);

        layersPopup.addSeparator();

        mi = new JMenuItem("Toggle Layer Visibility");
        mi.addActionListener(this);
        mi.setActionCommand("toggleLayerVisibility");
        layersPopup.add(mi);

        mi = new JMenuItem("Change Layer Title");
        mi.addActionListener(this);
        mi.setActionCommand("changeLayerTitle");
        layersPopup.add(mi);

        mi = new JMenuItem("Set As Active Layer");
        mi.addActionListener(this);
        mi.setActionCommand("setAsActiveLayer");
        layersPopup.add(mi);

        menuChangePalette = new JMenuItem("Change Palette");
        menuChangePalette.addActionListener(this);
        menuChangePalette.setActionCommand("changePalette");
        layersPopup.add(menuChangePalette);

        menuReversePalette = new JMenuItem("Reverse Palette");
        menuReversePalette.addActionListener(this);
        menuReversePalette.setActionCommand("reversePalette");
        layersPopup.add(menuReversePalette);
        
        mi = new JMenuItem("Toggle Layer Visibility In Legend");
        mi.addActionListener(this);
        mi.setActionCommand("toggleLayerVisibilityInLegend");
        layersPopup.add(mi);

        layersPopup.addSeparator();

        mi = new JMenuItem("Add Layer", new ImageIcon(graphicsDirectory + "AddLayer.png"));
        mi.addActionListener(this);
        mi.setActionCommand("addLayer");
        layersPopup.add(mi);

        mi = new JMenuItem("Remove Layer", new ImageIcon(graphicsDirectory + "RemoveLayer.png"));
        mi.addActionListener(this);
        mi.setActionCommand("removeLayer");
        layersPopup.add(mi);

        layersPopup.addSeparator();

        mi = new JMenuItem("Raise Layer", new ImageIcon(graphicsDirectory + "PromoteLayer.png"));
        mi.addActionListener(this);
        mi.setActionCommand("raiseLayer");
        layersPopup.add(mi);

        mi = new JMenuItem("Lower Layer", new ImageIcon(graphicsDirectory + "DemoteLayer.png"));
        mi.addActionListener(this);
        mi.setActionCommand("lowerLayer");
        layersPopup.add(mi);

        mi = new JMenuItem("Layer to Top", new ImageIcon(graphicsDirectory + "LayerToTop.png"));
        mi.addActionListener(this);
        mi.setActionCommand("layerToTop");
        layersPopup.add(mi);

        mi = new JMenuItem("Layer to Bottom", new ImageIcon(graphicsDirectory + "LayerToBottom.png"));
        mi.addActionListener(this);
        mi.setActionCommand("layerToBottom");
        layersPopup.add(mi);

        layersPopup.addSeparator();

        mi = new JMenuItem("Zoom To Layer", new ImageIcon(graphicsDirectory + "ZoomToActiveLayer.png"));
        mi.addActionListener(this);
        mi.setActionCommand("zoomToLayer");
        layersPopup.add(mi);

        mi = new JMenuItem("Clip Layer to Current Extent");
        mi.addActionListener(this);
        mi.setActionCommand("clipLayerToExtent");
        layersPopup.add(mi);

        layersPopup.setOpaque(true);
        layersPopup.setLightWeightPopupEnabled(true);

        // maps menu
        mapsPopup = new JPopupMenu();

        mi = new JMenuItem("Map Properties");
        mi.addActionListener(this);
        mi.setActionCommand("mapProperties");
        mapsPopup.add(mi);

        mi = new JMenuItem("Rename Map");
        mi.addActionListener(this);
        mi.setActionCommand("renameMap");
        mapsPopup.add(mi);

        mi = new JMenuItem("Set As Active Map");
        mi.addActionListener(this);
        mi.setActionCommand("setAsActiveMap");
        mapsPopup.add(mi);

        mapsPopup.addSeparator();

        mi = new JMenuItem("Save Map", new ImageIcon(graphicsDirectory + "SaveMap.png"));
        mi.addActionListener(this);
        mi.setActionCommand("saveMap");
        mapsPopup.add(mi);

        mi = new JMenuItem("Open Map", new ImageIcon(graphicsDirectory + "open.png"));
        mi.addActionListener(this);
        mi.setActionCommand("openMap");
        mapsPopup.add(mi);

        mi = new JMenuItem("Add New Map", new ImageIcon(graphicsDirectory + "map.png"));
        mi.addActionListener(this);
        mi.setActionCommand("newMap");
        mapsPopup.add(mi);

        mi = new JMenuItem("Print Map", new ImageIcon(graphicsDirectory + "Print.png"));
        mi.addActionListener(this);
        mi.setActionCommand("printMap");
        mapsPopup.add(mi);

        mi = new JMenuItem("Export Map As Image");
        mi.addActionListener(this);
        mi.setActionCommand("exportMapAsImage");
        mapsPopup.add(mi);

        mapsPopup.addSeparator();

        mi = new JMenuItem("Refresh Map");
        mi.addActionListener(this);
        mi.setActionCommand("refreshMap");
        mapsPopup.add(mi);

        mi = new JMenuItem("Zoom to Page", new ImageIcon(graphicsDirectory + "ZoomFullExtent3.png"));
        mi.addActionListener(this);
        mi.setActionCommand("zoomToPage");
        mapsPopup.add(mi);

        mapsPopup.addSeparator();

        mi = new JMenuItem("Close Map");
        mi.addActionListener(this);
        mi.setActionCommand("closeMap");
        mapsPopup.add(mi);

        mapsPopup.setOpaque(true);
        mapsPopup.setLightWeightPopupEnabled(true);



        // map area popup menu
        mapAreaPopup = new JPopupMenu();

        mi = new JMenuItem("Add Layer", new ImageIcon(graphicsDirectory + "AddLayer.png"));
        mi.addActionListener(this);
        mi.setActionCommand("addLayer");

        mapAreaPopup.add(mi);
        mi = new JMenuItem("Remove Layer", new ImageIcon(graphicsDirectory + "RemoveLayer.png"));
        mi.addActionListener(this);
        mi.setActionCommand("removeLayer");
        mapAreaPopup.add(mi);

        mi = new JMenuItem("Remove All Layers");
        mi.addActionListener(this);
        mi.setActionCommand("removeAllLayers");
        mapAreaPopup.add(mi);

        mapAreaPopup.addSeparator();

        mi = new JMenuItem("Fit to Map to Page");
        mi.addActionListener(this);
        mi.setActionCommand("fitMapAreaToPage");
        mapAreaPopup.add(mi);

        mi = new JMenuItem("Fit to Data");
        mi.addActionListener(this);
        mi.setActionCommand("fitMapAreaToData");
        mapAreaPopup.add(mi);

        JCheckBoxMenuItem miCheck = new JCheckBoxMenuItem("Maximize Screen Size");
        miCheck.addActionListener(this);
        miCheck.setActionCommand("maximizeMapAreaScreenSize");
        mapAreaPopup.add(miCheck);


        mi = new JMenuItem("Zoom To Active Layer", new ImageIcon(graphicsDirectory + "ZoomToActiveLayer.png"));
        mi.addActionListener(this);
        mi.setActionCommand("zoomToLayer");
        mapAreaPopup.add(mi);

        mi = new JMenuItem("Zoom To Full Extent", new ImageIcon(graphicsDirectory + "Globe.png"));
        mi.addActionListener(this);
        mi.setActionCommand("zoomToFullExtent");
        mapAreaPopup.add(mi);

        mapAreaPopup.addSeparator();

        mi = new JMenuItem("Show All Layers");
        mi.addActionListener(this);
        mi.setActionCommand("allLayersVisible");
        mapAreaPopup.add(mi);

        mi = new JMenuItem("Hide All Layers");
        mi.addActionListener(this);
        mi.setActionCommand("allLayersInvisible");
        mapAreaPopup.add(mi);

        mi = new JMenuItem("Toggle Visibility of All Layers");
        mi.addActionListener(this);
        mi.setActionCommand("toggleAllLayerVisibility");
        mapAreaPopup.add(mi);

        mapAreaPopup.addSeparator();

        mi = new JMenuItem("Show Properties");
        mi.addActionListener(this);
        mi.setActionCommand("mapAreaProperties");
        mapAreaPopup.add(mi);

        mapAreaPopup.addSeparator();

        mi = new JMenuItem("Delete Map Area");
        mi.addActionListener(this);
        mi.setActionCommand("deleteMapArea");
        mapAreaPopup.add(mi);



        // text popup menu
        textPopup = new JPopupMenu();

        mi = new JMenuItem("Open");
        mi.addActionListener(this);
        mi.setActionCommand("openText");
        textPopup.add(mi);

        mi = new JMenuItem("Save");
        mi.addActionListener(this);
        mi.setActionCommand("saveText");
        textPopup.add(mi);

        mi = new JMenuItem("Close");
        mi.addActionListener(this);
        mi.setActionCommand("closeText");
        textPopup.add(mi);

        textPopup.addSeparator();

        mi = new JMenuItem("Clear");
        mi.addActionListener(this);
        mi.setActionCommand("clearText");
        textPopup.add(mi);

        mi = new JMenuItem("Cut");
        mi.addActionListener(this);
        mi.setActionCommand("cutText");
        textPopup.add(mi);
        mi = new JMenuItem("Copy");
        mi.addActionListener(this);
        mi.setActionCommand("copyText");
        textPopup.add(mi);

        mi = new JMenuItem("Paste");
        mi.addActionListener(this);
        mi.setActionCommand("pasteText");
        textPopup.add(mi);

        mi = new JMenuItem("Select All");
        mi.addActionListener(this);
        mi.setActionCommand("selectAllText");
        textPopup.add(mi);

        textPopup.addSeparator();
        wordWrap = new JCheckBoxMenuItem("Word Wrap");
        wordWrap.addActionListener(this);
        wordWrap.setActionCommand("wordWrap");
        wordWrap.setState(false);
        textPopup.add(wordWrap);



        textPopup.setOpaque(true);
        textPopup.setLightWeightPopupEnabled(true);

    }
    JToolBar cartoToolbar = new JToolBar();

    private void createCartoElementToolbar() {
        this.getContentPane().add(cartoToolbar, BorderLayout.EAST);

        JButton alignRight = makeToolBarButton("AlignRight.png", "alignRight", "alignRight", "alignRight");
        cartoToolbar.add(alignRight);

        JButton centerVerticalBtn = makeToolBarButton("CenterVertical.png",
                "centerVertical", "Center Vertical", "centerVertical");
        cartoToolbar.add(centerVerticalBtn);

        JButton alignLeft = makeToolBarButton("AlignLeft.png", "alignLeft",
                "alignLeft", "alignLeft");
        cartoToolbar.add(alignLeft);

        JButton alignTop = makeToolBarButton("AlignTop.png", "alignTop",
                "alignTop", "alignTop");
        cartoToolbar.add(alignTop);

        JButton centerHorizontalBtn = makeToolBarButton("CenterHorizontal.png",
                "centerHorizontal", "Center Horizontal", "centerHorizontal");
        cartoToolbar.add(centerHorizontalBtn);

        JButton alignBottom = makeToolBarButton("AlignBottom.png", "alignBottom",
                "Align Bottom", "alignBottom");
        cartoToolbar.add(alignBottom);

        cartoToolbar.addSeparator();

        JButton distributeVertically = makeToolBarButton("DistributeVertically.png",
                "distributeVertically", "Distribute Vertically", "distributeVertically");
        cartoToolbar.add(distributeVertically);

        JButton distributeHorizontally = makeToolBarButton("DistributeHorizontally.png",
                "distributeHorizontally", "Distribute Horizontally", "distributeHorizontally");
        cartoToolbar.add(distributeHorizontally);

        cartoToolbar.setOrientation(1);
        cartoToolbar.setVisible(false);

    }

    public void setCartoElementToolbarVisibility(boolean value) {
        cartoToolbar.setVisible(value);
    }

    private void createToolbar() {
        try {
            JToolBar toolbar = new JToolBar();
            JButton newMap = makeToolBarButton("map.png", "newMap", "Create a new map", "New");

            toolbar.add(newMap);
            JButton openMap = makeToolBarButton("open.png", "openMap", "Open an existing map", "Open");
            toolbar.add(openMap);
            JButton saveMap = makeToolBarButton("SaveMap.png", "saveMap", "Save an open map", "Save");
            toolbar.add(saveMap);
            JButton printMap = makeToolBarButton("Print.png", "printMap", "Print map", "Print");
            toolbar.add(printMap);
            toolbar.addSeparator();
            JButton addLayer = makeToolBarButton("AddLayer.png", "addLayer", "Add data layers to active map", "Add Layer");
            toolbar.add(addLayer);
            JButton removeLayer = makeToolBarButton("RemoveLayer.png", "removeLayer", "Remove active data layer from map", "Remove Layer");
            toolbar.add(removeLayer);
            JButton raiseLayer = makeToolBarButton("PromoteLayer.png", "raiseLayer", "Raise layer", "Raise Layer");
            toolbar.add(raiseLayer);
            JButton lowerLayer = makeToolBarButton("DemoteLayer.png", "lowerLayer", "Lower layer", "Lower Layer");
            toolbar.add(lowerLayer);
            JButton layerToTop = makeToolBarButton("LayerToTop.png", "layerToTop", "Layer To Top", "Layer To Top");
            toolbar.add(layerToTop);
            JButton layerToBottom = makeToolBarButton("LayerToBottom.png", "layerToBottom", "Layer To Bottom", "Layer To Bottom");
            toolbar.add(layerToBottom);
            toolbar.addSeparator();
            JButton zoomToPage = makeToolBarButton("ZoomFullExtent3.png", "zoomToPage", "Zoom To Page", "Zoom To Page");
            toolbar.add(zoomToPage);
            toolbar.addSeparator();
            select = makeToolBarButton("select.png", "select", "Select", "Select");
            toolbar.add(select);
            // Feature selection should go here.
            pan = makeToolBarButton("Pan2.png", "pan", "Pan", "Pan");
            toolbar.add(pan);
            zoomIntoBox = makeToolBarButton("ZoomIn.png", "zoomToBox", "Zoom In To Box", "Zoom");
            zoomIntoBox.setBorderPainted(true);
            toolbar.add(zoomIntoBox);
            zoomOut = makeToolBarButton("ZoomOut.png", "zoomOut", "Zoom Out", "Zoom Out");
            zoomOut.setBorderPainted(false);
            toolbar.add(zoomOut);
            JButton zoomToFullExtent = makeToolBarButton("Globe.png", "zoomToFullExtent", "Zoom Map Area To Full Extent", "Zoom To Full Extent");
            toolbar.add(zoomToFullExtent);
//            JButton zoomToActiveLayer = makeToolBarButton("ZoomToActiveLayer.png", "zoomToLayer", "Zoom To Active Layer", "Zoom To Active");
//            toolbar.add(zoomToActiveLayer);
//            JButton zoomIn = makeToolBarButton("ZoomIn.png", "zoomIn", "Zoom In", "Zoom In");
//            toolbar.add(zoomIn);
            JButton previousExtent = makeToolBarButton("back.png", "previousExtent", "Previous Extent", "Prev Extent");
            toolbar.add(previousExtent);
            JButton nextExtent = makeToolBarButton("forward.png", "nextExtent", "Next Extent", "Next Extent");
            nextExtent.setActionCommand("nextExtent");
            toolbar.add(nextExtent);
            toolbar.addSeparator();
            JButton rasterCalculator = makeToolBarButton("RasterCalculator.png", "rasterCalculator", "Raster Calculator", "Raster Calc");
            toolbar.add(rasterCalculator);
            JButton paletteManager = makeToolBarButton("paletteManager.png", "paletteManager", "Create or modify palette files", "Palette Manager");
            toolbar.add(paletteManager);
            modifyPixelsVals = makeToolBarButton("ModifyPixels.png", "modifyPixels", "Modify Pixel Values in Active Layer", "Modify Pixels");
            toolbar.add(modifyPixelsVals);
            distanceToolButton = makeToolBarButton("DistanceTool.png", "distanceTool", "Measure Distance", "Distance Tool");
            toolbar.add(distanceToolButton);
            JButton scripter = makeToolBarButton("ScriptIcon2.png", "scripter", "Scripter", "Scripter");
            toolbar.add(scripter);
            toolbar.addSeparator();
            JButton help = makeToolBarButton("Help.png", "helpIndex", "Help", "Help");
            toolbar.add(help);

            toolbar.addSeparator();
            toolbar.add(Box.createHorizontalGlue());
            JPanel scalePanel = new JPanel();
            scalePanel.setLayout(new BoxLayout(scalePanel, BoxLayout.X_AXIS));
            JLabel scaleLabel = new JLabel("1:");
            scalePanel.add(scaleLabel);
            scalePanel.add(scaleText);
            scaleText.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        updateMapScale();
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                }

                @Override
                public void keyTyped(KeyEvent e) {
                }
            });
            scalePanel.setMinimumSize(new Dimension(120, 22));
            scalePanel.setPreferredSize(new Dimension(120, 22));
            scalePanel.setMaximumSize(new Dimension(120, 22));
            toolbar.add(scalePanel);
            toolbar.add(Box.createHorizontalStrut(15));

            this.getContentPane().add(toolbar, BorderLayout.PAGE_START);

        } catch (Exception e) {
            showFeedback(e.getMessage());
        }

    }

    private void updateMapScale() {
        try {
            String input = scaleText.getText().replace(",", "");
            double newScale = Double.parseDouble(input);
            MapArea mapArea = openMaps.get(activeMap).getActiveMapArea();
            mapArea.setScale(newScale);
            refreshMap(false);
        } catch (Exception e) {
            // do nothing
        }
    }

    private JButton makeToolBarButton(String imageName, String actionCommand, String toolTipText, String altText) {
        //Look for the image.
        String imgLocation = graphicsDirectory + imageName;
        ImageIcon image = new ImageIcon(imgLocation, "");

        //Create and initialize the button.
        JButton button = new JButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(this);
        button.setOpaque(false);
        button.setBorderPainted(false);
        try {
            button.setIcon(image);
        } catch (Exception e) {
            button.setText(altText);
            showFeedback(e.getMessage());
        }

        return button;
    }
    private JPanel layersPanel;
    private FeatureSelectionPanel featuresPanel;

    private JTabbedPane createTabbedPane() {
        try {
            JSplitPane wbTools = getToolbox();
            tabs.insertTab("Tools", null, wbTools, "", 0);
            layersPanel = new JPanel(new BorderLayout());
            layersPanel.setBackground(Color.white);
            updateLayersTab();
            tabs.insertTab("Layers", null, layersPanel, "", 1);
            featuresPanel = new FeatureSelectionPanel();
            tabs.insertTab("Features", null, featuresPanel, "", 2);

            return tabs;
        } catch (Exception e) {
            showFeedback(e.getStackTrace().toString());
            return null;
        }

    }
    ArrayList<LegendEntryPanel> legendEntries = new ArrayList<LegendEntryPanel>();
    JScrollPane scrollView = new JScrollPane();

    private void updateLayersTab() {
        try {
            int pos = scrollView.getVerticalScrollBar().getValue();
            layersPanel.removeAll();
            if (legendEntries.size() <= 0) {
                getLegendEntries();
            } else {
                // how many legend entries should there be?
                int numLegendEntries = 0;
                for (MapInfo mi : openMaps) {
                    numLegendEntries++; // one for the map entry.
                    for (MapArea ma : mi.getMapAreas()) {
                        numLegendEntries++; // plus one for the mapArea
                        numLegendEntries += ma.getNumLayers();
                    }
                }
                if (numLegendEntries != legendEntries.size()) {
                    getLegendEntries();
                }
            }

            Box legendBox = Box.createVerticalBox();
            legendBox.add(Box.createVerticalStrut(5));

            JPanel legend = new JPanel();
            legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
            legend.setOpaque(true);

            // add the legend nodes
            for (LegendEntryPanel lep : legendEntries) {
                if (lep.getLegendEntryType() == 0) { // it's a map
                    if (lep.getMapNum() == activeMap) {
                        lep.setTitleFont(fonts.get("activeMap"));
                    } else {
                        lep.setTitleFont(fonts.get("inactiveMap"));

                        if (linkAllOpenMaps && (openMaps.get(lep.getMapNum()).getActiveMapArea().getCurrentExtent() != openMaps.get(activeMap).getActiveMapArea().getCurrentExtent())) {
                            openMaps.get(lep.getMapNum()).getActiveMapArea().setCurrentExtent(openMaps.get(activeMap).getActiveMapArea().getCurrentExtent());
                        }

                    }

                    if ((lep.getMapNum() == selectedMapAndLayer[0])
                            & (selectedMapAndLayer[1]) == -1
                            & (selectedMapAndLayer[2]) == -1) {
                        lep.setSelected(true);
                    } else {
                        lep.setSelected(false);
                    }

                    if (lep.getMapNum() == 0) {
                        legend.add(Box.createVerticalStrut(5));
                    } else {
                        legend.add(Box.createVerticalStrut(15));
                    }
                    legend.add(lep);

                } else if (lep.getLegendEntryType() == 2) { // it's a map area
                    if (lep.getMapArea() == openMaps.get(activeMap).getActiveMapArea().getElementNumber()
                            && lep.getMapNum() == activeMap) {
                        lep.setTitleFont(fonts.get("activeMap"));
                    } else {
                        lep.setTitleFont(fonts.get("inactiveMap"));
                    }
                    if ((lep.getMapArea() == selectedMapAndLayer[2])
                            & (selectedMapAndLayer[1]) == -1
                            && lep.getMapNum() == selectedMapAndLayer[0]) {
                        lep.setSelected(true);
                    } else {
                        lep.setSelected(false);
                    }
                    legend.add(Box.createVerticalStrut(8));
                    legend.add(lep);

                } else if (lep.getLegendEntryType() == 1) { // it's a layer
                    if ((lep.getMapNum() == selectedMapAndLayer[0])
                            && (lep.getLayerNum() == selectedMapAndLayer[1])
                            && (lep.getMapArea() == selectedMapAndLayer[2])) {
                        lep.setSelected(true);
                    } else {
                        lep.setSelected(false);
                    }
                    if (lep.getMapNum() == activeMap) {
                        if (lep.getMapArea() == openMaps.get(activeMap).getActiveMapArea().getElementNumber()) {
                            if (lep.getLayerNum() == openMaps.get(activeMap).getActiveMapArea().getActiveLayerOverlayNumber()) {
                                lep.setTitleFont(fonts.get("activeLayer"));
                            } else {
                                lep.setTitleFont(fonts.get("inactiveLayer"));
                            }
                        } else {
                            lep.setTitleFont(fonts.get("inactiveLayer"));
                        }
                    } else {
                        lep.setTitleFont(fonts.get("inactiveLayer"));
                    }

//                JPanel layerBox = new JPanel();
//                layerBox.setLayout(new BoxLayout(layerBox, BoxLayout.X_AXIS));
//                layerBox.setMaximumSize(new Dimension(1000, 20));
                    Box layerBox = Box.createHorizontalBox();
                    layerBox.setOpaque(false);
                    layerBox.add(Box.createHorizontalStrut(10));
                    layerBox.add(lep);
                    layerBox.add(Box.createHorizontalGlue());
                    legend.add(Box.createVerticalStrut(5));
                    legend.add(lep);
                }
            }

            legend.add(Box.createVerticalGlue());
            legend.setBackground(Color.white);
            scrollView = new JScrollPane(legend);
            layersPanel.add(scrollView, BorderLayout.CENTER);
            layersPanel.validate();
            layersPanel.repaint();

            scrollView.getVerticalScrollBar().setValue(pos);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void getLegendEntries() {
        legendEntries.clear();
        // add the map nodes
        int i = 0;
        for (MapInfo mi : openMaps) {
            LegendEntryPanel legendMapEntry;
            if (i == activeMap) {
                legendMapEntry = new LegendEntryPanel(mi.getMapName(),
                        this, fonts.get("activeMap"), i, -1, -1, (i == selectedMapAndLayer[0]));
            } else {
                legendMapEntry = new LegendEntryPanel(mi.getMapName(),
                        this, fonts.get("inactiveMap"), i, -1, -1, (i == selectedMapAndLayer[0]));
            }
            legendEntries.add(legendMapEntry);

            for (MapArea mapArea : mi.getMapAreas()) {
                LegendEntryPanel legendMapAreaEntry;
//                legendMapAreaEntry = new LegendEntryPanel(mapArea, 
//                    this, fonts.get("inactiveMap"), i, mapArea.getElementNumber(), 
//                        -1, (mapArea.getElementNumber() == selectedMapAndLayer[2]));
                legendMapAreaEntry = new LegendEntryPanel(mapArea,
                        this, fonts.get("inactiveMap"), i, mapArea.getElementNumber(),
                        -1, (mapArea.getElementNumber() == selectedMapAndLayer[2]
                        & selectedMapAndLayer[1] == -1));
                legendEntries.add(legendMapAreaEntry);

                for (int j = mapArea.getNumLayers() - 1; j >= 0; j--) {
                    // add them to the tree in the order of their overlayNumber
                    MapLayer layer = mapArea.getLayer(j);
                    LegendEntryPanel legendLayer;
                    if (j == mapArea.getActiveLayerOverlayNumber()) {
                        legendLayer = new LegendEntryPanel(layer, this, fonts.get("activeLayer"),
                                i, mapArea.getElementNumber(), j, (j == selectedMapAndLayer[1]));
                        if (layer.getLayerType() == MapLayer.MapLayerType.VECTOR
                                && legendLayer.getMapArea() == openMaps.get(activeMap).getActiveMapArea().getElementNumber()
                                && legendLayer.getMapNum() == activeMap) {
                            // get the name of the shapefile
                            VectorLayerInfo vli = (VectorLayerInfo) layer;
                            String fileName = vli.getFileName();
                            // see if this is the current shapefile on the feature selection panel and
                            // if not, update it.
                            if (!featuresPanel.getShapeFileName().equals(fileName)) {
                                featuresPanel.setVectorLayerInfo(vli);
                            }
                        }
                    } else {
                        legendLayer = new LegendEntryPanel(layer, this, fonts.get("inactiveLayer"),
                                i, mapArea.getElementNumber(), j, (j == selectedMapAndLayer[1]));
                    }
                    legendEntries.add(legendLayer);
                }
            }
            i++;

        }

    }

//    private void expandAll(JTree tree, TreePath parent, boolean expand) {
//        // Traverse children
//        TreeNode node = (TreeNode) parent.getLastPathComponent();
//        if (node.getChildCount() >= 0) {
//            for (Enumeration e = node.children(); e.hasMoreElements();) {
//                TreeNode n = (TreeNode) e.nextElement();
//                TreePath path = parent.pathByAddingChild(n);
//                expandAll(tree, path, expand);
//            }
//        }
//
//        // Expansion or collapse must be done bottom-up
//        if (expand) {
//            tree.expandPath(parent);
//        } else {
//            tree.collapsePath(parent);
//        }
//    }
    private void TextAreaMousePress(MouseEvent e) {

        if (e.getButton() == 3 || e.isPopupTrigger()) {
            textPopup.show((JComponent) e.getSource(), e.getX(), e.getY());
        }
    }

    public void layersTabMousePress(MouseEvent e, int mapNum, int mapArea, int layerNum) {
        // update the selected map and layer
        selectedMapAndLayer[0] = mapNum;
        selectedMapAndLayer[1] = layerNum;
        selectedMapAndLayer[2] = mapArea;

//        if (selectedMapAndLayer[0] != mapNum ||
//                selectedMapAndLayer[1] != layerNum ||
//                selectedMapAndLayer[2] != mapArea) {
//            selectedMapAndLayer[0] = mapNum;
//            selectedMapAndLayer[1] = layerNum;
//            selectedMapAndLayer[2] = mapArea;
//        } else if (e.getClickCount() != 2) {
//            selectedMapAndLayer[0] = -1;
//            selectedMapAndLayer[1] = -1;
//            selectedMapAndLayer[2] = -1;
//        }

        if (e.getButton() == 3 || e.isPopupTrigger()) {
            // is it a map?
            if (layerNum == -1 && mapArea == -1) { // it's a map
                mapsPopup.show((JComponent) e.getSource(), e.getX(), e.getY());
            } else if (layerNum == -1) { // it's a mapArea
                mapAreaPopup.show((JComponent) e.getSource(), e.getX(), e.getY());
            } else { // it's a layer
                // see if it's a raster or vector layer
                openMaps.get(mapNum).deslectAllCartographicElements();
                ArrayList<MapArea> mapAreas = openMaps.get(mapNum).getMapAreas();

                MapArea activeMapArea = null;
                for (MapArea ma : mapAreas) {
                    if (ma.getElementNumber() == mapArea) {
                        activeMapArea = ma;
                    }
                }
                if (activeMapArea != null) {
                    if (activeMapArea.getLayer(layerNum) instanceof RasterLayerInfo) {
                        menuItemAttributeTable.setVisible(false);
                        menuItemHisto.setVisible(true);
                        menuChangePalette.setVisible(true);
                        menuReversePalette.setVisible(true);
                        layersPopup.show((JComponent) e.getSource(), e.getX(), e.getY());
                    } else if (activeMapArea.getLayer(layerNum) instanceof VectorLayerInfo) {
                        menuItemAttributeTable.setVisible(true);
                        menuItemHisto.setVisible(false);
                        menuChangePalette.setVisible(false);
                        menuReversePalette.setVisible(false);
                        layersPopup.show((JComponent) e.getSource(), e.getX(), e.getY());
                    }
                }
            }
            //updateLayersTab();
        } else if (e.getClickCount() == 2) {
            openMaps.get(activeMap).deslectAllCartographicElements();
            if (layerNum == -1) {
                setAsActiveMap();

            } else {
                setAsActiveLayer();
            }
            //updateLayersTab();
            refreshMap(true);
        } else if (e.getClickCount() == 1) {
            updateLayersTab();
        }
    }

    private JSplitPane getToolbox() {
        try {
            // create the tool treeview
            File file = new File(toolboxFile);

            if (!file.exists()) {
                showFeedback("Toolbox file (toolbox.xml) does not exist in main directory.");
                return null;
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            Node topNode = doc.getFirstChild();
            tree = new JTree(populateTree(topNode));
            MouseListener ml = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int selRow = tree.getRowForLocation(e.getX(), e.getY());
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    String label;
                    if (selRow != -1) {
                        if (e.getClickCount() == 1) {
                            DefaultMutableTreeNode n = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                            if (n.getChildCount() == 0) {
                                label = selPath.getLastPathComponent().toString();
                                showToolDescription(label);
                            } else if (n.toString().equals("Available Tools")) {
                                // set the message indicating the number of plugins that were located.
                                status.setMessage(" " + plugInfo.size() + " plugins were located");
                            }
                        } else if (e.getClickCount() == 2) {
                            DefaultMutableTreeNode n = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                            if (n.getChildCount() == 0) {
                                label = selPath.getLastPathComponent().toString();
                                launchDialog(label);
                            }
                        }// else {
                        //   System.out.println("click count: " + e.getClickCount());
                        //}
                    }
                }
            };
            tree.addMouseListener(ml);

            ImageIcon leafIcon = new ImageIcon(graphicsDirectory + "tool.png", "");
            ImageIcon stemIcon = new ImageIcon(graphicsDirectory + "opentools.png", "");
            //if (leafIcon != null && stemIcon != null) {
            DefaultTreeCellRenderer renderer =
                    new DefaultTreeCellRenderer();
            renderer.setLeafIcon(leafIcon);
            renderer.setClosedIcon(stemIcon);
            renderer.setOpenIcon(stemIcon);
            tree.setCellRenderer(renderer);
            //}

            JScrollPane treeView = new JScrollPane(tree);

            // create the quick launch
            qlTabs = new JTabbedPane();


            MouseListener ml2 = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    JList theList = (JList) e.getSource();
                    String label = null;
                    int index = theList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        Object o = theList.getModel().getElementAt(index);
                        label = o.toString();
                    }
                    if (e.getClickCount() == 1) {
                        showToolDescription(label);
                    } else if (e.getClickCount() == 2) {
                        launchDialog(label);

                    }

                }
            };

            //DefaultListModel model = new DefaultListModel();
            allTools = new JList();
            recentTools = new JList();
            mostUsedTools = new JList();

            populateToolTabs();

            allTools.addMouseListener(ml2);
            recentTools.addMouseListener(ml2);
            mostUsedTools.addMouseListener(ml2);

            JScrollPane scroller1 = new JScrollPane(allTools);
            JScrollPane scroller2 = new JScrollPane(recentTools);
            JScrollPane scroller3 = new JScrollPane(mostUsedTools);

            JPanel allToolsPanel = new JPanel();
            allToolsPanel.setLayout(new BoxLayout(allToolsPanel, BoxLayout.Y_AXIS));
            Box box = Box.createHorizontalBox();
            box.add(Box.createHorizontalStrut(3));
            box.add(new JLabel("Search:"));
            box.add(Box.createHorizontalStrut(3));
            searchText.setMaximumSize(new Dimension(275, 22));
            box.setMaximumSize(new Dimension(275, 24));
            searchText.addActionListener(searchFieldListener);
            box.add(searchText);
            allToolsPanel.add(box);
            allToolsPanel.add(scroller1);

            qlTabs.insertTab("Recent", null, scroller2, "", 0);
            qlTabs.insertTab("Most Used", null, scroller3, "", 1);
            qlTabs.insertTab("All tools", null, allToolsPanel, "", 2); // + plugInfo.size() + " tools", null, scroller1, "", 2);

            qlTabs.setPreferredSize(new Dimension(200, splitterToolboxLoc));

            qlTabs.setSelectedIndex(qlTabsIndex);

            splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeView, qlTabs);
            splitPane2.setResizeWeight(1);
            splitPane2.setOneTouchExpandable(true);

            return splitPane2;
        } catch (Exception e) {
            showFeedback(e.getStackTrace().toString());
            return null;
        }
    }
    private ActionListener searchFieldListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            searchForWords();
        }
    };

    private void searchForWords() {
        DefaultListModel model = new DefaultListModel();
        String searchString = searchText.getText().toLowerCase();
        String descriptiveName, shortName, description;

        if (searchString.equals("") || searchString == null) {

            for (int i = 0; i < plugInfo.size(); i++) {
                plugInfo.get(i).setSortMode(PluginInfo.SORT_MODE_NAMES);
            }
            Collections.sort(plugInfo);
            for (int i = 0; i < plugInfo.size(); i++) {
                model.add(i, plugInfo.get(i).getDescriptiveName());
            }

        } else {

            // find quotations
            ArrayList<String> quotedStrings = new ArrayList<String>();
            Pattern p = Pattern.compile("\"([^\"]*)\"");
            Matcher m = p.matcher(searchString);
            while (m.find()) {
                quotedStrings.add(m.group(1));
            }

            // now remove all quotedStrings from the line
            for (int i = 0; i < quotedStrings.size(); i++) {
                searchString = searchString.replace(quotedStrings.get(i), "");
            }

            searchString = searchString.replace("\"", "");

            int count = 0;
            boolean containsWord;

            searchString = searchString.replace("-", " ");
            searchString = searchString.replace(" the ", "");
            searchString = searchString.replace(" a ", "");
            searchString = searchString.replace(" of ", "");
            searchString = searchString.replace(" to ", "");
            searchString = searchString.replace(" and ", "");
            searchString = searchString.replace(" be ", "");
            searchString = searchString.replace(" in ", "");
            searchString = searchString.replace(" it ", "");

            String[] words = searchString.split(" ");
            for (int i = 0; i < plugInfo.size(); i++) {
                plugInfo.get(i).setSortMode(PluginInfo.SORT_MODE_NAMES);
            }
            Collections.sort(plugInfo);
            for (int i = 0; i < plugInfo.size(); i++) {
                descriptiveName = plugInfo.get(i).getDescriptiveName().toLowerCase().replace("-", " ");
                shortName = plugInfo.get(i).getName().toLowerCase().replace("-", " ");
                WhiteboxPlugin plug = pluginService.getPlugin(plugInfo.get(i).getDescriptiveName(), StandardPluginService.DESCRIPTIVE_NAME);
                description = plug.getToolDescription().toLowerCase().replace("-", " ");

                containsWord = false;

                for (String word : words) {
                    if (descriptiveName.contains(word)) {
                        containsWord = true;
                    }
                    if (shortName.contains(word)) {
                        containsWord = true;
                    }
                    if (description.contains(" " + word + " ")) {
                        containsWord = true;
                    }
                }

                for (String word : quotedStrings) {
                    if (descriptiveName.contains(word)) {
                        containsWord = true;
                    }
                    if (shortName.contains(word)) {
                        containsWord = true;
                    }
                    if (description.contains(" " + word + " ")) {
                        containsWord = true;
                    }
                }
                if (containsWord) {
                    model.add(count, plugInfo.get(i).getDescriptiveName());
                    count++;
                }

            }
        }
        allTools.setModel(model);
    }

    private DefaultMutableTreeNode populateTree(Node n) {
        Element e = (Element) n;
        String label = e.getAttribute("label");
        String toolboxName = e.getAttribute("name");
        DefaultMutableTreeNode result = new DefaultMutableTreeNode(label);

        NodeList nodeList = n.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) nodeList.item(i);
                label = childElement.getAttribute("label");
                toolboxName = childElement.getAttribute("name");
                DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(label);

                ArrayList<String> t = findToolsInToolbox(toolboxName);

                if (nodeList.item(i).getFirstChild() != null) {
                    NodeList childNodeList = nodeList.item(i).getChildNodes();
                    for (int j = 0; j < childNodeList.getLength(); j++) {
                        if (childNodeList.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            childTreeNode.add(populateTree(childNodeList.item(j)));
                        }
                    }

                }

                if (t.size() > 0) {
                    for (int k = 0; k < t.size(); k++) {
                        childTreeNode.add(new DefaultMutableTreeNode(t.get(k)));
                    }
                } else if (nodeList.item(i).getFirstChild() == null) {
                    childTreeNode.add(new DefaultMutableTreeNode("No tools"));
                }

                result.add(childTreeNode);
            }
        }
        if (nodeList.getLength() == 0) {
            ArrayList<String> t = findToolsInToolbox(toolboxName);
            if (t.size() > 0) {
                for (int k = 0; k < t.size(); k++) {
                    result.add(new DefaultMutableTreeNode(t.get(k)));
                }
            } else {
                result.add(new DefaultMutableTreeNode("No tools"));
            }
        }

        return result;

    }

    private ArrayList<String> findToolsInToolbox(String toolbox) {
        Iterator<WhiteboxPlugin> iterator = pluginService.getPlugins();
        ArrayList<String> plugs = new ArrayList<String>();

        while (iterator.hasNext()) {
            WhiteboxPlugin plugin = iterator.next();
            String[] tbox = plugin.getToolbox();
            for (int i = 0; i < tbox.length; i++) {
                if (tbox[i].equals(toolbox)) {
                    plugs.add(plugin.getDescriptiveName());
                }
            }

        }
        Collections.sort(plugs, new SortIgnoreCase());
        return plugs;
    }

    public class SortIgnoreCase implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {
            String s1 = (String) o1;
            String s2 = (String) o2;
            return s1.toLowerCase().compareTo(s2.toLowerCase());
        }
    }

//    private void setLookAndFeel(String lnfName) {
//        try {
//            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//                if (lnfName.equals(info.getName())) {
//                    UIManager.setLookAndFeel(info.getClassName());
//                    SwingUtilities.updateComponentTreeUI(this);
//                    this.pack();
//                    this.setExtendedState(JFrame.MAXIMIZED_BOTH);
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            showFeedback(e.getMessage());
//        }
//    }
//    private String getSystemLookAndFeelName() {
//        String className = UIManager.getSystemLookAndFeelClassName();
//        String name = null;
//        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//            if (className.equals(info.getClassName())) {
//                name = info.getName();
//                break;
//            }
//        }
//        return name;
//    }
    @Override
    public int showFeedback(String message) {
        JOptionPane.showMessageDialog(this, message);
        return -1;
    }

    @Override
    public int showFeedback(String message, int optionType, int messageType) {
        Object[] options = {"Yes", "No"};
        int n = JOptionPane.showOptionDialog(this,
                message,
                "Whitebox GAT Message",
                optionType,
                messageType,
                null, //do not use a custom Icon
                options, //the titles of buttons
                options[0]); //default button title

        return n;
    }

    public void displayLayer(String file) {
        try {
            // first make sure that there is an open map.
            if (numOpenMaps <= 0) {
                // add a new map
                numOpenMaps++;
                MapInfo mapinfo = new MapInfo("Map");
                mapinfo.setMapName("Map");
                mapinfo.setWorkingDirectory(workingDirectory);

                MapArea ma = new MapArea("MapArea1");
                ma.setUpperLeftX(-32768);
                ma.setUpperLeftY(-32768);
                mapinfo.addNewCartographicElement(ma);

                openMaps.add(mapinfo); //new MapInfo(str));

                activeMap = numOpenMaps - 1;
                drawingArea.setMapInfo(openMaps.get(activeMap));

                selectedMapAndLayer[0] = -1;
                selectedMapAndLayer[1] = -1;
                selectedMapAndLayer[2] = -1;
            }
            MapArea activeMapArea = openMaps.get(activeMap).getActiveMapArea();
            if (file.contains(".dep")) {
                String[] defaultPalettes = {defaultQuantPalette, defaultQualPalette, "rgb.pal"};
                // first get the active map

                RasterLayerInfo newLayer = new RasterLayerInfo(file, paletteDirectory,
                        defaultPalettes, 255, activeMapArea.getNumLayers());
                activeMapArea.addLayer(newLayer);
                newLayer.setOverlayNumber(activeMapArea.getNumLayers() - 1);
                activeMapArea.setActiveLayer(activeMapArea.getNumLayers() - 1);
                refreshMap(true);
            } else if (file.contains(".shp")) {
                VectorLayerInfo newLayer = new VectorLayerInfo(file, paletteDirectory,
                        255, activeMapArea.getNumLayers());
                activeMapArea.addLayer(newLayer);
                newLayer.setOverlayNumber(activeMapArea.getNumLayers() - 1);
                activeMapArea.setActiveLayer(activeMapArea.getNumLayers() - 1);
                refreshMap(true);
            }
        } catch (Exception e) {
            showFeedback(e.getStackTrace().toString());
        }
    }
    String progressString = "";
    int progressValue = 0;

    @Override
    public void updateProgress(String progressLabel, int progress) {
        if (!progressLabel.equals(progressString) || progress != progressValue) {
            if (progress < 0) {
                progress = 0;
            }
            if (progress > 100) {
                progress = 100;
            }
            status.setProgress(progress);
            status.setProgressLabel(progressLabel);
            progressValue = progress;
            progressString = progressLabel;
        }
    }

    @Override
    public void updateProgress(int progress) {
        if (progress != progressValue) {
            if (progress < 0) {
                progress = 0;
            }
            if (progress > 100) {
                progress = 100;
            }
            status.setProgress(progress);
            progressValue = progress;
        }
    }

    private void populateToolTabs() {
        int maxIndex;
        if (plugInfo.size() <= 10) {
            maxIndex = plugInfo.size();
        } else {
            maxIndex = 10;
        }
        allTools.removeAll();
        DefaultListModel model = new DefaultListModel();
        for (int i = 0; i < plugInfo.size(); i++) {
            plugInfo.get(i).setSortMode(PluginInfo.SORT_MODE_NAMES);
        }
        Collections.sort(plugInfo);
        for (int i = 0; i < plugInfo.size(); i++) {
            model.add(i, plugInfo.get(i).getDescriptiveName());
        }
        allTools.setModel(model);

        recentTools.removeAll();
        DefaultListModel model2 = new DefaultListModel();
        for (int i = 0; i < plugInfo.size(); i++) {
            plugInfo.get(i).setSortMode(PluginInfo.SORT_MODE_RECENT);
        }
        Collections.sort(plugInfo);
        for (int i = 0; i < maxIndex; i++) {
            model2.add(i, plugInfo.get(i).getDescriptiveName());
        }
        recentTools.setModel(model2);

        mostUsedTools.removeAll();
        DefaultListModel model3 = new DefaultListModel();
        for (int i = 0; i < plugInfo.size(); i++) {
            plugInfo.get(i).setSortMode(PluginInfo.SORT_MODE_USAGE);
        }
        Collections.sort(plugInfo);
        for (int i = 0; i < maxIndex; i++) {
            model3.add(i, plugInfo.get(i).getDescriptiveName());
        }
        mostUsedTools.setModel(model3);



    }

    private void refreshToolUsage() {
        for (int i = 0; i < plugInfo.size(); i++) {
            plugInfo.get(i).setNumTimesUsed(0);
            plugInfo.get(i).setLastUsedToNow();
        }

        populateToolTabs();
    }

    private void addLayer() {

        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
        } else if (openMaps.isEmpty()) {
            mapNum = 0;
            mapAreaNum = 0;
        } else {
            mapNum = activeMap;
            mapAreaNum = openMaps.get(activeMap).getActiveMapAreaOverlayNumber();
        }

        // set the filter.
        ArrayList<ExtensionFileFilter> filters = new ArrayList<ExtensionFileFilter>();
        String filterDescription = "Shapefiles (*.shp)";
        String[] extensions = {"SHP"};
        ExtensionFileFilter eff = new ExtensionFileFilter(filterDescription, extensions);
        filters.add(eff);

        filterDescription = "Whitebox Raster Files (*.dep)";
        extensions = new String[]{"DEP"};
        eff = new ExtensionFileFilter(filterDescription, extensions);
        filters.add(eff);

        filterDescription = "Whitebox Layer Files (*.dep, *.shp)";
        extensions = new String[]{"DEP", "SHP"};
        eff = new ExtensionFileFilter(filterDescription, extensions);
        filters.add(eff);


        JFileChooser fc = new JFileChooser();

        fc.setCurrentDirectory(new File(workingDirectory));
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        fc.setAcceptAllFileFilterUsed(false);

        for (int i = 0; i < filters.size(); i++) {
            fc.setFileFilter(filters.get(i));
        }

        int result = fc.showOpenDialog(this);
        File[] files = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            if (openMaps.isEmpty()) {
                // create a new map to overlay the layer onto.
                numOpenMaps = 1;
                MapInfo mapinfo = new MapInfo("Map1");
                mapinfo.setMapName("Map1");
                MapArea ma = new MapArea("MapArea1");
                ma.setUpperLeftX(-32768);
                ma.setUpperLeftY(-32768);
                mapinfo.addNewCartographicElement(ma);
                openMaps.add(mapinfo);
                drawingArea.setMapInfo(openMaps.get(0));
                activeMap = 0;
            }
            files = fc.getSelectedFiles();
            String fileDirectory = files[0].getParentFile() + pathSep;
            if (!fileDirectory.equals(workingDirectory)) {
                workingDirectory = fileDirectory;
            }
            String[] defaultPalettes = {defaultQuantPalette, defaultQualPalette, "rgb.pal"};
            MapArea activeMapArea = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
            for (int i = 0; i < files.length; i++) {
                // get the file extension.
                if (files[i].toString().toLowerCase().contains(".dep")) {
                    RasterLayerInfo newLayer = new RasterLayerInfo(files[i].toString(), paletteDirectory,
                            defaultPalettes, 255, activeMapArea.getNumLayers());
                    activeMapArea.addLayer(newLayer);
                    newLayer.setOverlayNumber(activeMapArea.getNumLayers() - 1);
                } else if (files[i].toString().toLowerCase().contains(".shp")) {
                    VectorLayerInfo newLayer = new VectorLayerInfo(files[i].toString(), paletteDirectory,
                            255, activeMapArea.getNumLayers());
                    activeMapArea.addLayer(newLayer);
                    newLayer.setOverlayNumber(activeMapArea.getNumLayers() - 1);
                }
            }
            activeMapArea.setActiveLayer(activeMapArea.getNumLayers() - 1);
        }
        refreshMap(true);
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    private void removeLayer() {
        if (selectedMapAndLayer[1] != -1) {
            // a layer has been selected for removal.
            openMaps.get(selectedMapAndLayer[0]).getMapAreaByElementNum(selectedMapAndLayer[2]).removeLayer(selectedMapAndLayer[1]);
            drawingArea.repaint();
            updateLayersTab();
        } else if (selectedMapAndLayer[2] != -1) {
            // a mapArea has been selected. remove it's active layer.
            MapArea ma = openMaps.get(selectedMapAndLayer[0]).getMapAreaByElementNum(selectedMapAndLayer[2]);
            ma.removeLayer(ma.getActiveLayerOverlayNumber());
            drawingArea.repaint();
            updateLayersTab();
        } else {
            // remove the active layer
            int activeLayer = openMaps.get(activeMap).getActiveMapArea().getActiveLayerOverlayNumber();
            openMaps.get(activeMap).getActiveMapArea().removeLayer(activeLayer);
            drawingArea.repaint();
            updateLayersTab();
        }
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    private void newMap() {

        String str = JOptionPane.showInputDialog("Enter the name of the new Map: ", "Map" + (numOpenMaps + 1));

        if (str != null) {
            numOpenMaps++;
            MapInfo mapinfo = new MapInfo(str);
            mapinfo.setMapName(str);
            mapinfo.setWorkingDirectory(workingDirectory);
            mapinfo.setPageFormat(defaultPageFormat);

            MapArea ma = new MapArea("MapArea1");
            ma.setUpperLeftX(-32768);
            ma.setUpperLeftY(-32768);
            mapinfo.addNewCartographicElement(ma);

            openMaps.add(mapinfo); //new MapInfo(str));
            activeMap = numOpenMaps - 1;
            drawingArea.setMapInfo(openMaps.get(activeMap));
            drawingArea.repaint();

            updateLayersTab();

            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        }

    }

    private void closeMap() {
        if (numOpenMaps > 0) {
            if (selectedMapAndLayer[0] == -1) {
                selectedMapAndLayer[0] = activeMap;
            }
            if (selectedMapAndLayer[0] == activeMap) {
                openMaps.remove(activeMap);
                numOpenMaps--;
                activeMap--;
                if (activeMap < 0) {
                    activeMap = 0;
                }
                if (numOpenMaps > 0) {
                    drawingArea.setMapInfo(openMaps.get(activeMap));
                } else {
                    drawingArea.setMapInfo(new MapInfo("Map"));
                }
                drawingArea.repaint();
            } else {
                openMaps.remove(selectedMapAndLayer[0]);
                numOpenMaps--;
                if (selectedMapAndLayer[0] < activeMap) {
                    activeMap--;
                }

                if (activeMap < 0) {
                    activeMap = 0;
                }
                if (numOpenMaps > 0) {
                    drawingArea.setMapInfo(openMaps.get(activeMap));
                } else {
                    drawingArea.setMapInfo(new MapInfo("Map"));
                }
                drawingArea.repaint();

            }
        }
        updateLayersTab();
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    /* Prints the active map. */
    private void printMap() {
        // The map must be displayed in the drawing area.
        if (selectedMapAndLayer[0] == -1) {
            selectedMapAndLayer[0] = activeMap;
        }
        if (selectedMapAndLayer[0] != activeMap) {
            setAsActiveMap();
        }
        PrinterJob job = PrinterJob.getPrinterJob();
//        PageFormat pf = openMaps.get(selectedMapAndLayer[0]).getPageFormat();
//        Book book = new Book();//java.awt.print.Book
//        book.append(drawingArea, pf);
//        job.setPageable(book);
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
//        job.setPrintable(drawingArea);
        boolean ok = job.printDialog(aset);
        if (ok) {
            try {
                PageFormat pf = job.defaultPage();
                Book book = new Book();//java.awt.print.Book
                book.append(drawingArea, pf);
                job.setPageable(book);
                job.print(aset);
            } catch (PrinterException ex) {
                showFeedback("An error was encountered while printing." + ex);
                /* The job did not successfully complete */
            }
        }
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    // Renames the selected map.
    private void renameMap() {
        if (selectedMapAndLayer[0] == - 1) {
            selectedMapAndLayer[0] = activeMap;
        }
        // find the title element
        int i = -1;
        for (CartographicElement ce : openMaps.get(activeMap).getCartographicElementList()) {
            if (ce instanceof MapTitle) {
                i = ce.getElementNumber();
                break;
            }
        }
//        showMapProperties(i);

        String str = JOptionPane.showInputDialog("Enter the new name: ",
                openMaps.get(selectedMapAndLayer[0]).getMapName());
        if (str != null) {
            openMaps.get(selectedMapAndLayer[0]).setMapName(str);
            updateLayersTab();
        }

        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    // Saves the selected active map.
    private void saveMap() {
        if (numOpenMaps < 1) {
            return;
        } // do nothing
        if (selectedMapAndLayer[0] == - 1) {
            selectedMapAndLayer[0] = activeMap;
        }
        if (openMaps.get(selectedMapAndLayer[0]).getFileName().equals("")) {
            saveMapAs();
        } else {

            File file = new File(openMaps.get(selectedMapAndLayer[0]).getFileName());

            if (file.exists()) {
                file.delete();
            }


            FileWriter fw = null;
            BufferedWriter bw = null;
            PrintWriter out = null;
            try {
                fw = new FileWriter(file, false);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw, true);

                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.setPrettyPrinting();
                gsonBuilder.registerTypeAdapter(MapInfo.class, new MapInfoSerializer());
                Gson gson = gsonBuilder.create();

                String json = gson.toJson(openMaps.get(selectedMapAndLayer[0]));
                out.println(json);
            } catch (java.io.IOException e) {
                System.err.println("Error: " + e.getMessage());
            } catch (Exception e) { //Catch exception if any
                System.err.println("Error: " + e.getMessage());
            } finally {
                if (out != null || bw != null) {
                    out.flush();
                    out.close();
                }

            }
        }
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    // Finds a file name to save the active map to.
    private void saveMapAs() {
        if (numOpenMaps < 1) {
            return;
        } // do nothing
        // get the title of the active map.
        String mapTitle = openMaps.get(selectedMapAndLayer[0]).getMapName();

        // Ask the user to specify a file name for saving the active map.
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setCurrentDirectory(new File(workingDirectory + pathSep + mapTitle + ".wmap"));
        fc.setAcceptAllFileFilterUsed(false);

        File f = new File(workingDirectory + pathSep + mapTitle + ".wmap");
        fc.setSelectedFile(f);

        // set the filter.
        ArrayList<ExtensionFileFilter> filters = new ArrayList<ExtensionFileFilter>();
        String filterDescription = "Whitebox Map Files (*.wmap)";
        String[] extensions = {"WMAP"};
        ExtensionFileFilter eff = new ExtensionFileFilter(filterDescription, extensions);
        fc.setFileFilter(eff);

        int result = fc.showSaveDialog(this);
        File file = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            // see if file has an extension.
            String mapFile = file.toString();
            if (!mapFile.contains(".wmap")) {
                mapFile = mapFile + ".wmap";
                file = new File(mapFile);
            }

            String fileDirectory = file.getParentFile() + pathSep;
            if (!fileDirectory.equals(workingDirectory)) {
                workingDirectory = fileDirectory;
            }

            // see if the file exists already, and if so, should it be overwritten?
            if (file.exists()) {
                int n = showFeedback("The file already exists.\n"
                        + "Would you like to overwrite it?", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (n == JOptionPane.YES_OPTION) {
                    file.delete();
                } else if (n == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            openMaps.get(selectedMapAndLayer[0]).setFileName(mapFile);

            saveMap();

        }
    }

    private void openMap() {
        // set the filter.
        ArrayList<ExtensionFileFilter> filters = new ArrayList<ExtensionFileFilter>();
        String filterDescription = "Whitebox Map Files (*.wmap)";
        String[] extensions = {"WMAP", "XML"};
        ExtensionFileFilter eff = new ExtensionFileFilter(filterDescription, extensions);

        filters.add(eff);

        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(new File(workingDirectory));
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.setMultiSelectionEnabled(true);
        fc.setAcceptAllFileFilterUsed(false);

        for (int i = 0; i < filters.size(); i++) {
            fc.setFileFilter(filters.get(i));
        }

        int result = fc.showOpenDialog(this);
        File[] files = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            files = fc.getSelectedFiles();
            String fileDirectory = files[0].getParentFile() + pathSep;
            if (!fileDirectory.equals(workingDirectory)) {
                workingDirectory = fileDirectory;
            }
            for (int i = 0; i < files.length; i++) {

                try {
                    // first read the text from the file into a string
                    String mapTextData = whitebox.utilities.FileUtilities.readFileAsString(files[i].toString());

                    // now use gson to create a new MapInfo object by deserialization
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    gsonBuilder.setPrettyPrinting();
                    gsonBuilder.registerTypeAdapter(MapInfo.class,
                            new MapInfoDeserializer(workingDirectory, paletteDirectory));
                    Gson gson = gsonBuilder.create();

                    MapInfo map = gson.fromJson(mapTextData, MapInfo.class);

//                MapInfo map = new MapInfo("");
//                if (!map.openMap(files[i].toString())) {
//                    showFeedback("Map file " + files[i].toString() + " not read properly.");
//                    break;
//                }
                    openMaps.add(map);
                } catch (Exception e) {
                    showFeedback("Map file " + files[i].toString() + " not read properly.");
                    break;
                }
                //int k = map.getNumLayers();
            }
//            openMaps.get(openMaps.size() - 1).setCartoView(cartographicView.getState());
            activeMap = openMaps.size() - 1;
            drawingArea.setMapInfo(openMaps.get(activeMap));
            drawingArea.repaint();
            updateLayersTab();
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
            numOpenMaps++;
            //refreshMap();
        }
    }
    private int numExportedImages = 0;

    private void exportMapAsImage() {
        if (numOpenMaps < 1) {
            return;
        } // do nothing
        if (selectedMapAndLayer[0] == - 1) {
            selectedMapAndLayer[0] = activeMap;
        }

        if (numExportedImages == 0) {
            showFeedback("The current print resolution is " + printResolution
                    + " dpi.\nTo change this value, select View => Options and Settings.");
        }

        // get the title of the active map.
        String mapTitle = openMaps.get(selectedMapAndLayer[0]).getMapName();

        // Ask the user to specify a file name for saving the active map.
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setCurrentDirectory(new File(workingDirectory + pathSep + mapTitle + ".png"));
        fc.setAcceptAllFileFilterUsed(false);

        File f = new File(workingDirectory + pathSep + mapTitle + ".png");
        fc.setSelectedFile(f);

        // set the filter.
        ArrayList<ExtensionFileFilter> filters = new ArrayList<ExtensionFileFilter>();
        String[] extensions = ImageIO.getReaderFormatNames(); //{"PNG", "JPEG", "JPG"};
        String filterDescription = "Image Files (" + extensions[0];
        for (int i = 1; i < extensions.length; i++) {
            filterDescription += ", " + extensions[i];
        }
        filterDescription += ")";
        ExtensionFileFilter eff = new ExtensionFileFilter(filterDescription, extensions);
        fc.setFileFilter(eff);

        int result = fc.showSaveDialog(this);
        File file = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            // see if file has an extension.
            if (file.toString().lastIndexOf(".") <= 0) {
                String fileName = file.toString() + ".png";
                file = new File(fileName);
            }

            String fileDirectory = file.getParentFile() + pathSep;
            if (!fileDirectory.equals(workingDirectory)) {
                workingDirectory = fileDirectory;
            }

            // see if the file exists already, and if so, should it be overwritten?
            if (file.exists()) {
                int n = showFeedback("The file already exists.\n"
                        + "Would you like to overwrite it?", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (n == JOptionPane.YES_OPTION) {
                    file.delete();
                } else if (n == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            if (!drawingArea.saveToImage(file.toString())) {
                showFeedback("An error occurred while saving the map to the image file.");
            }
        }

        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;

        numExportedImages++;
    }

    private void setAsActiveLayer() {
        if (selectedMapAndLayer[0] == activeMap) {
            openMaps.get(activeMap).getMapAreaByElementNum(selectedMapAndLayer[2]).setActiveLayer(selectedMapAndLayer[1]);
            openMaps.get(activeMap).setActiveMapAreaByElementNum(selectedMapAndLayer[2]);

            updateLayersTab();
        } else {
            if (selectedMapAndLayer[0] == -1) {
                return;
            }
            // first update the activeMap
            activeMap = selectedMapAndLayer[0];
            openMaps.get(activeMap).getMapAreaByElementNum(selectedMapAndLayer[2]).setActiveLayer(selectedMapAndLayer[1]);
            openMaps.get(activeMap).setActiveMapAreaByElementNum(selectedMapAndLayer[2]);
            drawingArea.setMapInfo(openMaps.get(activeMap));
            drawingArea.repaint();
            updateLayersTab();
        }
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    private void toggleLayerVisibility() {
        openMaps.get(selectedMapAndLayer[0]).getMapAreaByElementNum(selectedMapAndLayer[2]).toggleLayerVisibility(selectedMapAndLayer[1]);
        drawingArea.repaint();
        updateLayersTab();
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    private void toggleAllLayerVisibility() {
        for (int i = 0; i < openMaps.get(selectedMapAndLayer[0]).getMapAreaByElementNum(selectedMapAndLayer[2]).getNumLayers(); i++) {
            openMaps.get(selectedMapAndLayer[0]).getMapAreaByElementNum(selectedMapAndLayer[2]).toggleLayerVisibility(i);
        }
        drawingArea.repaint();
        updateLayersTab();
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    private void allLayersVisibile() {
        int j, k;
        if (selectedMapAndLayer[0] != -1) {
            j = selectedMapAndLayer[0];
            k = selectedMapAndLayer[2];
        } else {
            j = activeMap;
            k = openMaps.get(j).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(j).getMapAreaByElementNum(k);
        for (int i = 0; i < ma.getNumLayers(); i++) {
            ma.getLayer(i).setVisible(true);
        }
        drawingArea.repaint();
        updateLayersTab();
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    private void allLayersInvisibile() {
        int j, k;
        if (selectedMapAndLayer[0] != -1) {
            j = selectedMapAndLayer[0];
            k = selectedMapAndLayer[2];
        } else {
            j = activeMap;
            k = openMaps.get(j).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(j).getMapAreaByElementNum(k);
        for (int i = 0; i < ma.getNumLayers(); i++) {
            ma.getLayer(i).setVisible(false);
        }
        drawingArea.repaint();
        updateLayersTab();
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    private void toggleLayerVisibilityInLegend() {
        openMaps.get(selectedMapAndLayer[0]).getMapAreaByElementNum(selectedMapAndLayer[2]).toggleLayerVisibilityInLegend(selectedMapAndLayer[1]);
        drawingArea.repaint();
        updateLayersTab();
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }
    
    private void setAsActiveMap() {
        if (selectedMapAndLayer[0] != -1) {
            activeMap = selectedMapAndLayer[0];
            if (selectedMapAndLayer[2] != -1) {
                //openMaps.get(activeMap).setActiveMapAreaByElementNum(selectedMapAndLayer[2]); // this may have to be changed to the overlay number rather than the element number
                openMaps.get(activeMap).setActiveMapAreaByElementNum(openMaps.get(activeMap).getCartographicElement(selectedMapAndLayer[2]).getElementNumber());
            }
            drawingArea.setMapInfo(openMaps.get(activeMap));
            drawingArea.repaint();
            updateLayersTab();
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        }
    }

    private void changeLayerTitle() {
        String str = JOptionPane.showInputDialog("Enter the new title: ",
                openMaps.get(selectedMapAndLayer[0]).getMapAreaByElementNum(selectedMapAndLayer[2]).getLayer(selectedMapAndLayer[1]).getLayerTitle());
        if (str != null) {
            openMaps.get(selectedMapAndLayer[0]).getMapAreaByElementNum(selectedMapAndLayer[2]).getLayer(selectedMapAndLayer[1]).setLayerTitle(str);
            updateLayersTab();
        }
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    private void layerToTop() {
        int mapNum;
        int mapAreaNum;
        int layerNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            layerNum = selectedMapAndLayer[1];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active layer and map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
            layerNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 1) {
            ma.promoteLayerToTop(layerNum);
        }
        legendEntries.clear();
        updateLayersTab();
        drawingArea.repaint();
    }

    private void layerToBottom() {
        int mapNum;
        int mapAreaNum;
        int layerNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            layerNum = selectedMapAndLayer[1];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active layer and map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
            layerNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }

        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 1) {
            ma.demoteLayerToBottom(layerNum);
        }
        legendEntries.clear();
        updateLayersTab();
        drawingArea.repaint();
    }

    private void promoteLayer() {
        int mapNum;
        int mapAreaNum;
        int layerNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            layerNum = selectedMapAndLayer[1];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active layer and map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
            layerNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 1) {
            ma.promoteLayer(layerNum);
        }
        legendEntries.clear();
        updateLayersTab();
        drawingArea.repaint();
    }

    private void demoteLayer() {
        int mapNum;
        int mapAreaNum;
        int layerNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            layerNum = selectedMapAndLayer[1];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active layer and map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
            layerNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 1) {
            ma.demoteLayer(layerNum);
        }
        legendEntries.clear();
        updateLayersTab();
        drawingArea.repaint();
    }

    /**
     * Changes the palette of a displayed raster layer.
     */
    private void changePalette() {
        int mapNum;
        int mapAreaNum;
        int layerNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            layerNum = selectedMapAndLayer[1];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active layer and map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
            layerNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getLayer(layerNum).getLayerType() == MapLayerType.RASTER) {
            RasterLayerInfo rli = (RasterLayerInfo) ma.getLayer(layerNum);
            String palette = rli.getPaletteFile();
            boolean isReversed = rli.isPaletteReversed();
            double nonlinearity = rli.getNonlinearity();
            PaletteChooser chooser = new PaletteChooser(this, true, paletteDirectory, palette,
                    isReversed, nonlinearity);
            chooser.setSize(300, 300);
            chooser.setVisible(true);

            String newPaletteFile = chooser.getValue();
            chooser.dispose();
            if (newPaletteFile != null) {
                if (!newPaletteFile.equals("") && !newPaletteFile.equals("createNewPalette")) {
                    rli.setPaletteFile(newPaletteFile);
                    rli.update();
                    refreshMap(true);
                } else if (newPaletteFile.equals("createNewPalette")) {
                    PaletteManager pm = new PaletteManager(paletteDirectory);
                    pm.setVisible(true);
                }
            }
        }
    }

    private void reversePalette() {
        int mapNum;
        int mapAreaNum;
        int layerOverlayNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            layerOverlayNum = selectedMapAndLayer[1];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active layer and map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
            layerOverlayNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            ma.reversePaletteOfLayer(layerOverlayNum);
            refreshMap(true);
        }
    }

    private void zoomToFullExtent() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active layer and map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            BoundingBox db = ma.getFullExtent();
            ma.setCurrentExtent(db.clone());
            refreshMap(false);
        }
    }

    private void fitToData() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active layer and map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            ma.fitToData();
            refreshMap(false);
        }
    }

    private void fitToPage() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active layer and map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        BoundingBox pageExtent = openMaps.get(mapNum).getPageExtent();
        int margin = (int) (openMaps.get(mapNum).getMargin() * 72);
        int referenceMarkSize = ma.getReferenceMarksSize();
        ma.setUpperLeftX(margin);
        ma.setUpperLeftY(margin);
        ma.setWidth((int) (pageExtent.getWidth() - 2 * margin - referenceMarkSize));
        ma.setHeight((int) (pageExtent.getHeight() - 2 * margin - referenceMarkSize));
        refreshMap(false);
    }

    private void maximizeMapAreaScreenSize() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active layer and map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            ma.setSizeMaximizedToScreenSize(!ma.isSizeMaximizedToScreenSize());
            refreshMap(false);
        }
    }

    private void zoomToPage() {
        int mapNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the map
            mapNum = activeMap;
        }
        openMaps.get(mapNum).zoomToPage();
        refreshMap(false);
    }

    private void zoomToLayer() {
        int mapNum;
        int mapAreaNum;
        int layerOverlayNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            if (selectedMapAndLayer[1] != -1) {
                layerOverlayNum = selectedMapAndLayer[1];
            } else {
                // use the active layer
                layerOverlayNum = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum).getActiveLayerOverlayNumber();
            }
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active layer and map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
            layerOverlayNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            ma.calculateFullExtent();
            BoundingBox db = ma.getLayer(layerOverlayNum).getFullExtent();
            ma.setCurrentExtent(db);

            refreshMap(false);
        }
    }

    private void zoomIn() {
        int mapNum;
        int mapAreaNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            ma.zoomIn();
            refreshMap(false);
        }
    }

    private void zoomOut() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            ma.zoomOut();
            refreshMap(false);
        }
    }

    private void panUp() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            ma.panUp();
            refreshMap(false);
        }
    }

    private void panDown() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            ma.panDown();
            refreshMap(false);
        }
    }

    private void panLeft() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            ma.panLeft();
            refreshMap(false);
        }
    }

    private void panRight() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            ma.panRight();
            refreshMap(false);
        }
    }

    private void nextExtent() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            boolean ret = ma.nextExtent();
            if (ret) {
                refreshMap(false);
            }
        }
    }

    private void previousExtent() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(activeMap).getActiveMapAreaOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getNumLayers() > 0) {
            boolean ret = ma.previousExtent();
            if (ret) {
                refreshMap(false);
            }
        }
    }

    private void showLayerProperties() {
        int mapNum;
        int mapAreaNum;
        int layerOverlayNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            layerOverlayNum = selectedMapAndLayer[1];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            layerOverlayNum = openMaps.get(activeMap).getActiveMapArea().getActiveLayerOverlayNumber();
            mapAreaNum = openMaps.get(activeMap).getActiveMapArea().getElementNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getLayer(layerOverlayNum).getLayerType() == MapLayerType.RASTER
                || ma.getLayer(layerOverlayNum).getLayerType() == MapLayerType.VECTOR) {
            MapLayer layer = ma.getLayer(layerOverlayNum);
            LayerProperties lp = new LayerProperties(this, false, layer, openMaps.get(mapNum));
            lp.setSize(640, 420);
            lp.setVisible(true);
            //lp.dispose();
        }
    }

    public void showMapProperties(int activeElement) {
        int mapNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
        }
        MapProperties mp;
        if (activeElement >= 0) {
            mp = new MapProperties(this, false, openMaps.get(mapNum), activeElement);
        } else {
            mp = new MapProperties(this, false, openMaps.get(mapNum));
        }
        mp.setSize(440, 600);
        mp.setLocation(new Point(10, 30));
        mp.setVisible(true);
    }

    public void showMapAreaProperties() {
        int mapNum;
        int mapAreaNum;
        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
        }
        MapProperties mp;
        if (mapAreaNum >= 0) {
            mp = new MapProperties(this, false, openMaps.get(mapNum), mapAreaNum);
        } else {
            mp = new MapProperties(this, false, openMaps.get(mapNum));
        }
        mp.setSize(440, 600);
        mp.setLocation(new Point(10, 30));
        mp.setVisible(true);
    }

    private void showAttributesFile() {
        int mapNum;
        int mapAreaNum;
        int layerOverlayNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            layerOverlayNum = selectedMapAndLayer[1];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(activeMap).getActiveMapAreaOverlayNumber();
            layerOverlayNum = openMaps.get(activeMap).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getLayer(layerOverlayNum).getLayerType() == MapLayerType.RASTER
                || ma.getLayer(layerOverlayNum).getLayerType() == MapLayerType.VECTOR) {
            MapLayer layer = ma.getLayer(layerOverlayNum);
            VectorLayerInfo vli = (VectorLayerInfo) layer;
            String shapeFileName = vli.getFileName();
            AttributesFileViewer afv = new AttributesFileViewer(this, false, shapeFileName);
            int height = 500;
            afv.setSize((int) (height * 1.61803399), height); // golden ratio.
            afv.setVisible(true);
        }
    }
    String currentTextFile = null;

    private void openText() {

        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(true);
        fc.setCurrentDirectory(new File(workingDirectory));

        FileFilter ft = new FileNameExtensionFilter("Whitebox Raster Files", "dep");
        fc.addChoosableFileFilter(ft);
        ft = new FileNameExtensionFilter("Text Files", "txt");
        fc.addChoosableFileFilter(ft);

        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            currentTextFile = file.toString();
            String fileDirectory = file.getParentFile() + pathSep;
            if (!fileDirectory.equals(workingDirectory)) {
                workingDirectory = fileDirectory;
            }
            // display the text area, if it's not already.
            if (splitPane3.getDividerLocation() / splitPane3.getHeight() < 0.75) {
                splitPane3.setDividerLocation(0.75);
            }

            textArea.setText("");

            // Read in data file to JTextArea
            try {
                String strLine;
                FileInputStream in = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                while ((strLine = br.readLine()) != null) {
                    textArea.append(strLine + "\n");
                }
                br.close();
                in.close();
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

    }

    private void saveText() {
        if (currentTextFile == null) {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
            fc.setAcceptAllFileFilterUsed(true);
            fc.setFileHidingEnabled(true);

            FileFilter ft = new FileNameExtensionFilter("Text Files", "txt");
            fc.addChoosableFileFilter(ft);
            ft = new FileNameExtensionFilter("Whitebox Raster Files", "dep");
            fc.addChoosableFileFilter(ft);

            fc.setCurrentDirectory(new File(workingDirectory));
            int result = fc.showSaveDialog(this);
            File file = null;
            if (result == JFileChooser.APPROVE_OPTION) {
                file = fc.getSelectedFile();
                if (file.exists()) {
                    Object[] options = {"Yes", "No"};
                    int n = JOptionPane.showOptionDialog(this,
                            "The file already exists.\n"
                            + "Would you like to overwrite it?",
                            "Whitebox GAT Message",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null, //do not use a custom Icon
                            options, //the titles of buttons
                            options[0]); //default button title

                    if (n == JOptionPane.YES_OPTION) {
                        file.delete();
                        new File(file.toString().replace(".dep", ".tas")).delete();
                    } else if (n == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
                currentTextFile = file.toString();
            } else {
                return;
            }
        }

        File file = new File(currentTextFile);
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        try {
            fw = new FileWriter(file, false);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw, true);

            out.print(textArea.getText());

            bw.close();
            fw.close();
        } catch (java.io.IOException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) { //Catch exception if any
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (out != null || bw != null) {
                out.flush();
                out.close();
            }
        }
    }

    private void showAboutDialog() {
        AboutWhitebox about = new AboutWhitebox(this, true, graphicsDirectory, versionNumber);
    }

    private void callSplashScreen() {
        String splashFile = graphicsDirectory + "WhiteboxLogo.png"; //"SplashScreen.png";
        SplashWindow sw = new SplashWindow(splashFile, 2000, versionNumber);
        long t0, t1;
        t0 = System.currentTimeMillis();
        do {
            if (!sw.getValue()) {
                sw.dispose();
            }
            t1 = System.currentTimeMillis();
        } while ((t1 - t0 < 2000) && sw.getValue());
    }

    private void modifyPixelValues() {
        if (openMaps.get(activeMap).getActiveMapArea().getNumRasterLayers() > 0) {
            if (drawingArea.isModifyingPixels()) { // is true; unset
                drawingArea.setModifyingPixels(false);
                modifyPixelsVals.setBorderPainted(false);
                modifyPixels.setState(false);
            } else {
                drawingArea.setModifyingPixels(true);
                modifyPixelsVals.setBorderPainted(true);
                modifyPixels.setState(true);
                // you can't modify pixels and measure distances
                drawingArea.setUsingDistanceTool(false);
                distanceToolButton.setBorderPainted(false);
                distanceToolMenuItem.setState(false);
            }
        } else {
            showFeedback("The active map does not contain any raster layers.");
        }
    }

    private void distanceTool() {
        if (openMaps.get(activeMap).getActiveMapArea().getNumLayers() > 0) {
            if (drawingArea.isUsingDistanceTool()) { // is true; unset
                drawingArea.setUsingDistanceTool(false);
                distanceToolButton.setBorderPainted(false);
                distanceToolMenuItem.setState(false);
            } else {
                drawingArea.setUsingDistanceTool(true);
                distanceToolButton.setBorderPainted(true);
                distanceToolMenuItem.setState(true);
                // you can't modify pixels and measure distances
                drawingArea.setModifyingPixels(false);
                modifyPixelsVals.setBorderPainted(false);
                modifyPixels.setState(false);
            }
        } else {
            showFeedback("The active map does not contain any layers.");
        }
    }

    private void clipLayerToExtent() {
        int mapNum;
        int mapAreaNum;
        int layerOverlayNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            mapAreaNum = selectedMapAndLayer[2];
            layerOverlayNum = selectedMapAndLayer[1];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
            layerOverlayNum = openMaps.get(activeMap).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getLayer(layerOverlayNum).getLayerType() == MapLayerType.RASTER) {
            // What file name should the clipped image be given?
            String str = JOptionPane.showInputDialog(null, "Enter the name of the new file: ",
                    "Whitebox", 1);

            if (str == null) {
                return;
            }

            RasterLayerInfo layer = (RasterLayerInfo) ma.getLayer(layerOverlayNum);

            // what directory is the layer in?
            String dir = layer.getHeaderFile().substring(0,
                    layer.getHeaderFile().lastIndexOf(pathSep) + 1);

            String fileName = dir + str + ".dep";

            if (new File(fileName).exists()) {
                Object[] options = {"Yes", "No"};
                int n = JOptionPane.showOptionDialog(this,
                        "The file already exists.\n"
                        + "Would you like to overwrite it?",
                        "Whitebox GAT Message",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, //do not use a custom Icon
                        options, //the titles of buttons
                        options[0]); //default button title

                if (n == JOptionPane.YES_OPTION) {
                    // do nothing
                } else if (n == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            layer.clipLayerToExtent(ma.getCurrentExtent(), fileName);

        } else {
            showFeedback("This function is not currently available for vector layers.");
        }
    }

    private void viewHistogram() {
        int mapNum;
        int mapAreaNum;
        int layerOverlayNum;

        if (selectedMapAndLayer[0] != -1) {
            mapNum = selectedMapAndLayer[0];
            layerOverlayNum = selectedMapAndLayer[1];
            mapAreaNum = selectedMapAndLayer[2];
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
        } else {
            // use the active map
            mapNum = activeMap;
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaOverlayNumber();
            layerOverlayNum = openMaps.get(activeMap).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma.getLayer(layerOverlayNum).getLayerType() == MapLayerType.RASTER) {
            RasterLayerInfo layer = (RasterLayerInfo) ma.getLayer(layerOverlayNum);
            HistogramView histo = new HistogramView(this, false, layer.getHeaderFile(), workingDirectory);
        }
    }

    private void removeAllLayers() {
        int j, k;
        if (selectedMapAndLayer[0] != -1) {
            j = selectedMapAndLayer[0];
            k = selectedMapAndLayer[2];
        } else {
            j = activeMap;
            k = openMaps.get(activeMap).getActiveMapAreaOverlayNumber();
        }
        MapArea mapArea = openMaps.get(j).getMapAreaByElementNum(k);
        do {
            mapArea.removeLayer(openMaps.get(j).getActiveMapArea().getNumLayers() - 1);
        } while (mapArea.getNumLayers() > 0);

        drawingArea.repaint();
        updateLayersTab();
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
    }

    private void newHelp() {
        String str = JOptionPane.showInputDialog("Help File Name: ", "");
        if (str != null) {
            String fileName = helpDirectory + str;
            if (!str.endsWith(".html")) {
                fileName += ".html";
            }

            // see if the filename exists already.
            File file = new File(fileName);
            if (file.exists()) {
                Object[] options = {"Yes", "No"};
                int n = JOptionPane.showOptionDialog(this,
                        "The file already exists.\n"
                        + "Would you like to overwrite it?",
                        "Whitebox GAT Message",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, //do not use a custom Icon
                        options, //the titles of buttons
                        options[0]); //default button title

                if (n == JOptionPane.YES_OPTION) {
                    file.delete();
                } else if (n == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            // grab the text within the "NewHelp.txt" file in the helpDirectory;
            String defaultHelp = helpDirectory + "NewHelp.txt";
            if (!(new File(defaultHelp)).exists()) {
                showFeedback("Could not find default help file (\'NewHelp.txt\') in help directory.");
                return;
            }
            try {
                String defaultText = FileUtilities.readFileAsString(defaultHelp);
                // now place this text into the new file.
                FileUtilities.fillFileWithString(fileName, defaultText);

                ViewCodeDialog vcd = new ViewCodeDialog(this, new File(fileName), true);
                vcd.setSize(new Dimension(800, 600));
                vcd.setVisible(true);
            } catch (IOException ioe) {
                showFeedback("Could not read default help file (\'NewHelp.txt\') correctly.");
                return;
            }

        }
    }

    private void helpReport() {
        String pluginName;
        String fileName;
        ArrayList<String> pluginsWithoutHelpFiles = new ArrayList<>();

        for (int i = 0; i < plugInfo.size(); i++) {
            plugInfo.get(i).setSortMode(PluginInfo.SORT_MODE_NAMES);
        }
        Collections.sort(plugInfo);

        for (PluginInfo pi : plugInfo) {
            pluginName = pi.getName();
            fileName = helpDirectory + pluginName + ".html";
            File helpFile = new File(fileName);
            if (!helpFile.exists()) {
                pluginsWithoutHelpFiles.add(pi.getDescriptiveName()); //pluginName);
            }
        }
        DecimalFormat df = new DecimalFormat("###.0");
        String percentWithoutHelp = df.format((double) pluginsWithoutHelpFiles.size() / plugInfo.size() * 100.0);
        String reportOutput;
        reportOutput = "HELP COMPLETENESS REPORT:\n\n" + "We're working hard to ensure that Whitebox's help files are "
                + "complete. Currently, " + pluginsWithoutHelpFiles.size() + " (" + percentWithoutHelp
                + "%) plugins don't have help files.\n";
        if (pluginsWithoutHelpFiles.size() > 0) {
            reportOutput += "These include the following plugins:\n\n";
            for (int i = 0; i < pluginsWithoutHelpFiles.size(); i++) {
                reportOutput += pluginsWithoutHelpFiles.get(i) + "\n";
            }
        }

        reportOutput += "\nYou can contribute by writing a help entry for a plugin tool that doesn't currently have "
                + "one (press the 'Create new help entry' button on the tool's dialog) or by improving the help entry "
                + "for a tool that already has one. Email your work to jlindsay@uoguelph.ca.";

        returnData(reportOutput);

    }
    int printResolution = 600;

    public void setPrintResolution(int resolution) {
        this.printResolution = resolution;
        drawingArea.setPrintResolution(resolution);
    }

    public int getPrintResolution() {
        return printResolution;
    }

    public boolean isHideAlignToolbar() {
        return hideAlignToolbar;
    }

    public void setHideAlignToolbar(boolean hideAlignToolbar) {
        this.hideAlignToolbar = hideAlignToolbar;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("addLayer")) {
            addLayer();
        } else if (actionCommand.equals("removeLayer")) {
            removeLayer();
        } else if (actionCommand.equals("close")) {
            close();
        } else if (actionCommand.equals("linkMap")) {
            linkAllOpenMaps = !linkAllOpenMaps;
            linkMap.setState(linkAllOpenMaps);
        } else if (actionCommand.equals("nimbusLAF")) {
            setLookAndFeel("Nimbus");
        } else if (actionCommand.equals("systemLAF")) {
            setLookAndFeel(getSystemLookAndFeelName());
        } else if (actionCommand.equals("motifLAF")) {
            setLookAndFeel("CDE/Motif");
        } else if (actionCommand.equals("refreshTools")) {
            refreshToolUsage();
        } else if (actionCommand.equals("newMap")) {
            newMap();
        } else if (actionCommand.equals("closeMap")) {
            closeMap();
        } else if (actionCommand.equals("setAsActiveLayer")) {
            setAsActiveLayer();
        } else if (actionCommand.equals("toggleLayerVisibility")) {
            toggleLayerVisibility();
        } else if (actionCommand.equals("toggleAllLayerVisibility")) {
            toggleAllLayerVisibility();
        } else if (actionCommand.equals("allLayersVisible")) {
            allLayersVisibile();
        } else if (actionCommand.equals("allLayersInvisible")) {
            allLayersInvisibile();
        } else if (actionCommand.equals("toggleLayerVisibilityInLegend")) {
            toggleLayerVisibilityInLegend();
        } else if (actionCommand.equals("setAsActiveMap")) {
            setAsActiveMap();
        } else if (actionCommand.equals("renameMap")) {
            renameMap();
        } else if (actionCommand.equals("changeLayerTitle")) {
            changeLayerTitle();
        } else if (actionCommand.equals("layerToTop")) {
            layerToTop();
        } else if (actionCommand.equals("layerToBottom")) {
            layerToBottom();
        } else if (actionCommand.equals("raiseLayer")) {
            promoteLayer();
        } else if (actionCommand.equals("lowerLayer")) {
            demoteLayer();
        } else if (actionCommand.equals("changePalette")) {
            changePalette();
        } else if (actionCommand.equals("reversePalette")) {
            reversePalette();
        } else if (actionCommand.equals("zoomToFullExtent")) {
            zoomToFullExtent();
        } else if (actionCommand.equals("zoomToLayer")) {
            zoomToLayer();
        } else if (actionCommand.equals("zoomToPage")) {
            zoomToPage();
        } else if (actionCommand.equals("layerProperties")) {
            showLayerProperties();
        } else if (actionCommand.equals("zoomIn")) {
            zoomIn();
        } else if (actionCommand.equals("zoomOut")) {
//            zoomOut();
            openMaps.get(activeMap).deslectAllCartographicElements();
            refreshMap(false);
            drawingArea.setMouseMode(MapRenderer2.MOUSE_MODE_ZOOMOUT);
            zoomIntoBox.setBorderPainted(false);
            zoomOut.setBorderPainted(true);
            pan.setBorderPainted(false);
            select.setBorderPainted(false);
            zoomToBox.setState(true);
            panMenuItem.setState(false);
            selectMenuItem.setState(false);
        } else if (actionCommand.equals("panUp")) {
            panUp();
        } else if (actionCommand.equals("panDown")) {
            panDown();
        } else if (actionCommand.equals("panLeft")) {
            panLeft();
        } else if (actionCommand.equals("panRight")) {
            panRight();
        } else if (actionCommand.equals("zoomToBox")) {
            openMaps.get(activeMap).deslectAllCartographicElements();
            refreshMap(false);
            drawingArea.setMouseMode(MapRenderer2.MOUSE_MODE_ZOOM);
            zoomIntoBox.setBorderPainted(true);
            zoomOut.setBorderPainted(false);
            pan.setBorderPainted(false);
            select.setBorderPainted(false);
            zoomToBox.setState(true);
            panMenuItem.setState(false);
            selectMenuItem.setState(false);
        } else if (actionCommand.equals("pan")) {
            drawingArea.setMouseMode(MapRenderer2.MOUSE_MODE_PAN);
            zoomIntoBox.setBorderPainted(false);
            zoomOut.setBorderPainted(false);
            pan.setBorderPainted(true);
            select.setBorderPainted(false);
            zoomToBox.setState(false);
            panMenuItem.setState(true);
            selectMenuItem.setState(false);
        } else if (actionCommand.equals("select")) {
            drawingArea.setMouseMode(MapRenderer2.MOUSE_MODE_SELECT);
            zoomIntoBox.setBorderPainted(false);
            zoomOut.setBorderPainted(false);
            pan.setBorderPainted(false);
            select.setBorderPainted(true);
            zoomToBox.setState(false);
            panMenuItem.setState(false);
            selectMenuItem.setState(true);
        } else if (actionCommand.equals("nextExtent")) {
            nextExtent();
        } else if (actionCommand.equals("previousExtent")) {
            previousExtent();
        } else if (actionCommand.equals("paletteManager")) {
            PaletteManager pm = new PaletteManager(paletteDirectory);
            pm.setVisible(true);
        } else if (actionCommand.equals("rasterCalculator")) {
            RasterCalculator rc = new RasterCalculator(this, false, workingDirectory);
        } else if (actionCommand.equals("selectAllText")) {
            textArea.selectAll();
        } else if (actionCommand.equals("copyText")) {
            textArea.copy();
        } else if (actionCommand.equals("pasteText")) {
            textArea.paste();
        } else if (actionCommand.equals("cutText")) {
            textArea.cut();
        } else if (actionCommand.equals("clearText")) {
            textArea.setText("");
        } else if (actionCommand.equals("openText")) {
            openText();
        } else if (actionCommand.equals("saveText")) {
            saveText();
        } else if (actionCommand.equals("closeText")) {
            textArea.setText("");
            currentTextFile = null;
        } else if (actionCommand.equals("printMap")) {
            printMap();
        } else if (actionCommand.equals("saveMap")) {
            saveMap();
        } else if (actionCommand.equals("openMap")) {
            openMap();
        } else if (actionCommand.equals("exportMapAsImage")) {
            exportMapAsImage();
        } else if (actionCommand.equals("scripter")) {
            Scripter scripter = new Scripter(this, false);
            scripter.setVisible(true);
        } else if (actionCommand.equals("options")) {
            //showFeedback("This feature is under development.");
            SettingsDialog dlg = new SettingsDialog(this, false);
            dlg.setSize(500, 400);
            dlg.setVisible(true);
        } else if (actionCommand.equals("modifyPixels")) {
            modifyPixelValues();
        } else if (actionCommand.equals("helpIndex")) {
            Help help = new Help(this, false, "index");
            help.setVisible(true);
        } else if (actionCommand.equals("helpSearch")) {
            Help help = new Help(this, false, "search");
            help.setVisible(true);
        } else if (actionCommand.equals("helpAbout")) {
            showAboutDialog();
        } else if (actionCommand.equals("refreshMap")) {
            refreshMap(true);
        } else if (actionCommand.equals("distanceTool")) {
            distanceTool();
        } else if (actionCommand.equals("clipLayerToExtent")) {
            clipLayerToExtent();
        } else if (actionCommand.equals("viewHistogram")) {
            viewHistogram();
        } else if (actionCommand.equals("removeAllLayers")) {
            removeAllLayers();
        } else if (actionCommand.equals("wordWrap")) {
            textArea.setLineWrap(wordWrap.getState());
            textArea.setWrapStyleWord(wordWrap.getState());
        } else if (actionCommand.equals("viewAttributeTable")) {
            showAttributesFile();
        } else if (actionCommand.equals("newHelp")) {
            newHelp();
        } else if (actionCommand.equals("mapProperties")) {
            showMapProperties(0);
        } else if (actionCommand.equals("mapAreaProperties")) {
            showMapAreaProperties();
        } else if (actionCommand.equals("pageProps")) {
            showMapProperties(-1);
        } else if (actionCommand.equals("insertTitle")) {
            openMaps.get(activeMap).addMapTitle();
            refreshMap(false);
        } else if (actionCommand.equals("insertTextArea")) {
            openMaps.get(activeMap).addMapTextArea();
            refreshMap(false);
        } else if (actionCommand.equals("insertScale")) {
            openMaps.get(activeMap).addMapScale();
            refreshMap(false);
        } else if (actionCommand.equals("insertNorthArrow")) {
            openMaps.get(activeMap).addNorthArrow();
            refreshMap(false);
        } else if (actionCommand.equals("insertLegend")) {
            openMaps.get(activeMap).addLegend();
            refreshMap(false);
        } else if (actionCommand.equals("insertNeatline")) {
            openMaps.get(activeMap).addNeatline();
            refreshMap(false);
        } else if (actionCommand.equals("insertMapArea")) {
            int numMapAreas = openMaps.get(activeMap).getMapAreas().size();
            MapArea ma = new MapArea("MapArea" + (numMapAreas + 1));
            ma.setUpperLeftX(0);
            ma.setUpperLeftY(0);
            ma.setWidth(200);
            ma.setHeight(200);
            openMaps.get(activeMap).addNewCartographicElement(ma);
            refreshMap(true);
        } else if (actionCommand.equals("deleteMapArea")) {
            openMaps.get(activeMap).removeCartographicElement(selectedMapAndLayer[2]);
            refreshMap(true);
        } else if (actionCommand.equals("fitMapAreaToData")) {
            fitToData();
        } else if (actionCommand.equals("fitMapAreaToPage")) {
            fitToPage();
        } else if (actionCommand.equals("maximizeMapAreaScreenSize")) {
            maximizeMapAreaScreenSize();
        } else if (actionCommand.equals("helpReport")) {
            helpReport();
        } else if (actionCommand.equals("centerVertical")) {
            if (openMaps.get(activeMap).centerSelectedElementsVertically()) {
                refreshMap(false);
            }
        } else if (actionCommand.equals("centerHorizontal")) {
            if (openMaps.get(activeMap).centerSelectedElementsHorizontally()) {
                refreshMap(false);
            }
        } else if (actionCommand.equals("alignTop")) {
            if (openMaps.get(activeMap).alignSelectedElementsTop()) {
                refreshMap(false);
            }
        } else if (actionCommand.equals("alignBottom")) {
            if (openMaps.get(activeMap).alignSelectedElementsBottom()) {
                refreshMap(false);
            }
        } else if (actionCommand.equals("alignRight")) {
            if (openMaps.get(activeMap).alignSelectedElementsRight()) {
                refreshMap(false);
            }
        } else if (actionCommand.equals("alignLeft")) {
            if (openMaps.get(activeMap).alignSelectedElementsLeft()) {
                refreshMap(false);
            }
        } else if (actionCommand.equals("distributeVertically")) {
            if (openMaps.get(activeMap).distributeSelectedElementsVertically()) {
                refreshMap(false);
            }
        } else if (actionCommand.equals("distributeHorizontally")) {
            if (openMaps.get(activeMap).distributeSelectedElementsHorizontally()) {
                refreshMap(false);
            }
        }

    }

    private void close() {
        setApplicationProperties();
        dispose();
        System.exit(0);
    }

    @Override
    public void notifyOfThreadComplete(Runnable thread) {
        System.out.println("Thread " + thread.toString() + " complete");
    }

    @Override
    public void notifyOfProgress(int value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void passOnThreadException(Exception e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void notifyOfReturn(String ret) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
