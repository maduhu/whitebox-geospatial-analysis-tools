/*
 *  Copyright (C) 2011-2013 John Lindsay
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

import whiteboxgis.user_interfaces.Scripter;
import whiteboxgis.user_interfaces.ViewCodeDialog;
import whiteboxgis.user_interfaces.PaletteManager;
import whiteboxgis.user_interfaces.AttributesFileViewer;
import whiteboxgis.user_interfaces.AboutWhitebox;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import whiteboxgis.user_interfaces.ToolDialog;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.script.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.logging.*;
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
import whiteboxgis.user_interfaces.TreeNodeRenderer;
import whiteboxgis.user_interfaces.IconTreeNode;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
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
import whitebox.geospatialfiles.shapefile.ShapeTypeDimension;
import whitebox.serialization.MapInfoSerializer;
import whitebox.serialization.MapInfoDeserializer;
import whitebox.ui.ComboBoxProperty;
import whitebox.ui.ShapefileDatabaseRecordEntry;
import whitebox.ui.SupportedLanguageChooser;
import whiteboxgis.user_interfaces.FeatureSelectionPanel;
import whiteboxgis.user_interfaces.SettingsDialog;
import whiteboxgis.user_interfaces.RecentMenu;
import whiteboxgis.user_interfaces.CartographicToolbar;
import whiteboxgis.user_interfaces.LayersPopupMenu;
import whitebox.internationalization.WhiteboxInternationalizationTools;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
@SuppressWarnings("unchecked")
public class WhiteboxGui extends JFrame implements ThreadListener, ActionListener, WhiteboxPluginHost, Communicator {

    public static final Logger logger = Logger.getLogger(WhiteboxGui.class.getPackage().getName());
    private static PluginService pluginService = null;
    private StatusBar status;
    // common variables
    static private String versionName = "3.0 'Iguazu'";
    static private String versionNumber = "3.0.4";
    private String skipVersionNumber = versionNumber;
    private ArrayList<PluginInfo> plugInfo = null;
    private String applicationDirectory;
    private String resourcesDirectory;
    private String graphicsDirectory;
    private String pluginDirectory;
    private String helpDirectory;
    private String workingDirectory;
    private String paletteDirectory;
    private String logDirectory;
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
//    private JPopupMenu layersPopup = null;
    private JPopupMenu mapsPopup = null;
    private JPopupMenu mapAreaPopup = null;
    private JPopupMenu textPopup = null;
    private JToggleButton pan = null;
    private JToggleButton zoomIntoBox = null;
    private JToggleButton zoomOut = null;
    private JToggleButton select = null;
    private JToggleButton selectFeature = null;
    private JToggleButton modifyPixelVals = null;
    private JToggleButton distanceToolButton = null;
    private JToggleButton editVectorButton = null;
    private JToggleButton digitizeNewFeatureButton = null;
//    private JButton moveNodesButton = null;
    private JButton deleteFeatureButton = null;
    private JCheckBoxMenuItem modifyPixels = null;
    private JCheckBoxMenuItem zoomMenuItem = null;
    private JCheckBoxMenuItem zoomOutMenuItem = null;
    private JCheckBoxMenuItem panMenuItem = null;
    private JCheckBoxMenuItem selectMenuItem = null;
    private JCheckBoxMenuItem selectFeatureMenuItem = null;
    private JCheckBoxMenuItem distanceToolMenuItem = null;
    private JCheckBoxMenuItem editVectorMenuItem = null;
    private JCheckBoxMenuItem digitizeNewFeatureMenuItem = null;
    private JMenuItem deleteFeatureMenuItem = null;
    private JCheckBoxMenuItem linkMap = null;
    private JCheckBoxMenuItem wordWrap = null;
//    private JCheckBoxMenuItem editLayerMenuItem = null;
    private JTextField searchText = new JTextField();
    private HashMap<String, ImageIcon> icons = new HashMap<>();
    private HashMap<String, Font> fonts = new HashMap<>();
    private JTextField scaleText = new JTextField();
    private PageFormat defaultPageFormat = new PageFormat();
    private Font defaultFont = null;
    private int numberOfRecentItemsToStore = 5;
    private RecentMenu recentDirectoriesMenu = new RecentMenu();
    private RecentMenu recentFilesMenu = new RecentMenu();
    private RecentMenu recentFilesPopupMenu = new RecentMenu();
    private RecentMenu recentMapsMenu = new RecentMenu();
    private Color backgroundColour = new Color(225, 245, 255);
    private CartographicToolbar ctb;
    private double defaultMapMargin = 0.0;
    private ArrayList<WhiteboxAnnouncement> announcements = new ArrayList<>();
    private int announcementNumber = 0;
//    private Locale currentLocale;
    private ResourceBundle bundle;
    private ResourceBundle pluginBundle;
    private ResourceBundle messages;
    private String language = "en";
    private String country = "CA";
    private boolean checkForUpdates = true;
    private boolean receiveAnnouncements = true;

    public static void main(String[] args) {
        try {

            //setLookAndFeel("Nimbus");
            setLookAndFeel("systemLAF");
            if (System.getProperty("os.name").contains("Mac")) {
                System.setProperty("apple.awt.brushMetalLook", "true");
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Whitebox GAT");
                System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
                System.setProperty("Xdock:name", "Whitebox");
                System.setProperty("apple.awt.fileDialogForDirectories", "true");

                System.setProperty("apple.awt.textantialiasing", "true");

                System.setProperty("apple.awt.graphics.EnableQ2DX", "true");
            }

            WhiteboxGui wb = new WhiteboxGui();
            wb.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            wb.setVisible(true);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
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


        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException | UnsupportedLookAndFeelException e) {
            logger.log(Level.SEVERE, "WhiteboxGui.setLookAndFeel", e);
            //System.err.println(e.getMessage());
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
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException | UnsupportedLookAndFeelException e) {
            return null;
        }
    }

    public WhiteboxGui() {
        super("Whitebox GAT " + versionName);

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
            logDirectory = resourcesDirectory + "logs" + pathSep;

            // set up the logger
            int limit = 1000000; // 1 Mb
            int numLogFiles = 3;
            FileHandler fh = new FileHandler(logDirectory + "WhiteboxLog%g_%u.xml", limit, numLogFiles, true);
            fh.setFormatter(new XMLFormatter());
            //fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);

            //this.loadPlugins();
            this.getApplicationProperties();

            // i18n
            //currentLocale = new Locale(language, country);
            WhiteboxInternationalizationTools.setLocale(language, country);
            bundle = WhiteboxInternationalizationTools.getGuiLabelsBundle(); //ResourceBundle.getBundle("whiteboxgis.i18n.GuiLabelsBundle", currentLocale);
            messages = WhiteboxInternationalizationTools.getMessagesBundle(); //ResourceBundle.getBundle("whiteboxgis.i18n.messages", currentLocale);
            pluginBundle = WhiteboxInternationalizationTools.getPluginsBundle();
            this.loadPlugins();

            boolean newInstall = checkForNewInstallation();


            // create the gui

            status = new StatusBar(this);

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

            if (defaultQuantPalette.equals("")) {
                defaultQuantPalette = "spectrum.pal";
            }
            if (defaultQualPalette.equals("")) {
                defaultQualPalette = "qual.pal";
            }

            this.createGui();

            if (newInstall) {
                refreshToolUsage();
                recentDirectoriesMenu.removeAllMenuItems();
                recentFilesMenu.removeAllMenuItems();
                recentFilesPopupMenu.removeAllMenuItems();
                recentMapsMenu.removeAllMenuItems();
            }

            checkVersionIsUpToDate();




            /* The following code is only used to create a plugins.properties listing 
             * for internationalization.
             */
//            File file = new File(resourcesDirectory + "pluginNames.txt");
//            FileWriter fw = null;
//            BufferedWriter bw = null;
//            PrintWriter out = null;
//            try {
//                fw = new FileWriter(file, false);
//                bw = new BufferedWriter(fw);
//                out = new PrintWriter(bw, true);
//                String str;
//                for (PluginInfo plug : plugInfo) {
//                    str = plug.getName() + " = " + plug.getDescriptiveName();
//                    out.println(str);
//                    str = plug.getName() + "Description" + " = " + plug.getDescription();
//                    out.println(str);
//                }
//            } catch (Exception e) {
//            }

        } catch (IOException | SecurityException e) {
            logger.log(Level.SEVERE, "WhiteboxGui.constructor", e);
            //System.out.println(e.getMessage());
        }
    }

    private boolean checkVersionIsUpToDate() {
        // Throwing this on the EDT to allow the window to pop up faster
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    if (checkForUpdates || receiveAnnouncements) {
                        String currentVersionName = "";
                        String currentVersionNumber = "";
                        String downloadLocation = "";

                        //make a URL to a known source
                        String baseUrl = "http://www.uoguelph.ca/~hydrogeo/Whitebox/VersionInfo.xml";
                        URL url = new URL(baseUrl);

                        //open a connection to that source
                        HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();

                        //trying to retrieve data from the source. If there
                        //is no connection, this line will fail
                        Object objData = urlConnect.getContent();

                        InputStream inputStream = (InputStream) urlConnect.getContent();
                        DocumentBuilderFactory docbf = DocumentBuilderFactory.newInstance();
                        docbf.setNamespaceAware(true);
                        DocumentBuilder docbuilder = docbf.newDocumentBuilder();
                        Document document = docbuilder.parse(inputStream, baseUrl);

                        document.getDocumentElement().normalize();
                        Element docElement = document.getDocumentElement();

                        Element el;
                        NodeList nl = docElement.getElementsByTagName("VersionName");
                        if (nl.getLength() > 0) {
                            el = (Element) nl.item(0);
                            currentVersionName = el.getFirstChild().getNodeValue().replace("\"", "");
                        }

                        nl = docElement.getElementsByTagName("VersionNumber");
                        if (nl.getLength() > 0) {
                            el = (Element) nl.item(0);
                            currentVersionNumber = el.getFirstChild().getNodeValue().replace("\"", "");
                        }

                        nl = docElement.getElementsByTagName("DownloadLocation");
                        if (nl.getLength() > 0) {
                            el = (Element) nl.item(0);
                            downloadLocation = el.getFirstChild().getNodeValue().replace("\"", "");
                        }

                        if (receiveAnnouncements) {
                            // read the announcement data, if any
                            nl = docElement.getElementsByTagName("Announcements");
                            if (nl != null && nl.getLength() > 0) {
                                el = (Element) nl.item(0);
                                int thisAnnouncementNumber = Integer.parseInt(el.getAttribute("number"));
                                if (thisAnnouncementNumber > announcementNumber) {
                                    NodeList nl2 = el.getElementsByTagName("Announcement");
                                    if (nl2.getLength() > 0) {
                                        for (int i = 0; i < nl2.getLength(); i++) {
                                            Element el2 = (Element) nl2.item(i);
                                            String date = getTextValue(el2, "Date");
                                            String title = getTextValue(el2, "Title");
                                            String message = getTextValue(el2, "Message");
                                            if (!message.replace("\n", "").isEmpty()) {
                                                WhiteboxAnnouncement wba =
                                                        new WhiteboxAnnouncement(message, title, date);
                                                announcements.add(wba);
                                            }
                                        }
                                    }
                                    announcementNumber = thisAnnouncementNumber;
                                }
                            }

                            if (announcements.size() > 0) {
                                displayAnnouncements();
                            }

                        }

                        if (currentVersionName.isEmpty()
                                || currentVersionNumber.isEmpty()
                                || downloadLocation.isEmpty()) {
                            return;
                        }

                        if (checkForUpdates) {
                            if (Integer.parseInt(versionNumber.replace(".", ""))
                                    < Integer.parseInt(currentVersionNumber.replace(".", ""))
                                    && Integer.parseInt(skipVersionNumber.replace(".", ""))
                                    < Integer.parseInt(currentVersionNumber.replace(".", ""))) {
                                //Custom button text
                                Object[] options = {"Yes, proceed to download site", "Not now", "Don't ask again"};
                                int n = JOptionPane.showOptionDialog(null,
                                        "A newer version is available. "
                                        + "Would you like to download Whitebox "
                                        + currentVersionName + " (" + currentVersionNumber
                                        + ")?" + "\nYou are currently using Whitebox " + versionName
                                        + " (" + versionNumber + ").",
                                        "Whitebox Version",
                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                        JOptionPane.QUESTION_MESSAGE,
                                        null,
                                        options,
                                        options[0]);

                                if (n == 0) {
                                    Desktop d = Desktop.getDesktop();
                                    d.browse(new URI(downloadLocation));
                                } else if (n == 2) {
                                    skipVersionNumber = currentVersionNumber;
                                }
                            }
                        }
                    }
                } catch (UnknownHostException e) {
                    // no internet connection...no big deal.
                    //return false;
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "WhiteboxGui.checkVersionIsUpToDate", e);
                    //return false;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "WhiteboxGui.checkVersionIsUpToDate", e);
                    //return false;
                }
            }
        });
//        try {
//            String currentVersionName = "";
//            String currentVersionNumber = "";
//            String downloadLocation = "";
//
//            //make a URL to a known source
//            String baseUrl = "http://www.uoguelph.ca/~hydrogeo/Whitebox/VersionInfo.xml";
//            URL url = new URL(baseUrl);
//
//            //open a connection to that source
//            HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();
//
//            //trying to retrieve data from the source. If there
//            //is no connection, this line will fail
//            Object objData = urlConnect.getContent();
//
//            InputStream inputStream = (InputStream) urlConnect.getContent();
//            DocumentBuilderFactory docbf = DocumentBuilderFactory.newInstance();
//            docbf.setNamespaceAware(true);
//            DocumentBuilder docbuilder = docbf.newDocumentBuilder();
//            Document document = docbuilder.parse(inputStream, baseUrl);
//
//            document.getDocumentElement().normalize();
//            Element docElement = document.getDocumentElement();
//
//            Element el;
//            NodeList nl = docElement.getElementsByTagName("VersionName");
//            if (nl.getLength() > 0) {
//                el = (Element) nl.item(0);
//                currentVersionName = el.getFirstChild().getNodeValue().replace("\"", "");
//            }
//
//            nl = docElement.getElementsByTagName("VersionNumber");
//            if (nl.getLength() > 0) {
//                el = (Element) nl.item(0);
//                currentVersionNumber = el.getFirstChild().getNodeValue().replace("\"", "");
//            }
//
//            nl = docElement.getElementsByTagName("DownloadLocation");
//            if (nl.getLength() > 0) {
//                el = (Element) nl.item(0);
//                downloadLocation = el.getFirstChild().getNodeValue().replace("\"", "");
//            }
//
//            // read the announcement data, if any
//            nl = docElement.getElementsByTagName("Announcements");
//            if (nl != null && nl.getLength() > 0) {
//                el = (Element) nl.item(0);
//                int thisAnnouncementNumber = Integer.parseInt(el.getAttribute("number"));
//                if (thisAnnouncementNumber > announcementNumber) {
//                    NodeList nl2 = el.getElementsByTagName("Announcement");
//                    if (nl2.getLength() > 0) {
//                        for (int i = 0; i < nl2.getLength(); i++) {
//                            Element el2 = (Element) nl2.item(i);
//                            String date = getTextValue(el2, "Date");
//                            String title = getTextValue(el2, "Title");
//                            String message = getTextValue(el2, "Message");
//                            if (!message.replace("\n", "").isEmpty()) {
//                                WhiteboxAnnouncement wba =
//                                        new WhiteboxAnnouncement(message, title, date);
//                                announcements.add(wba);
//                            }
//                        }
//                    }
//                    announcementNumber = thisAnnouncementNumber;
//                }
//            }
//
//            if (currentVersionName.isEmpty()
//                    || currentVersionNumber.isEmpty()
//                    || downloadLocation.isEmpty()) {
//                return false;
//            }
//
//            if (Integer.parseInt(versionNumber.replace(".", ""))
//                    < Integer.parseInt(currentVersionNumber.replace(".", ""))
//                    && Integer.parseInt(skipVersionNumber.replace(".", ""))
//                    < Integer.parseInt(currentVersionNumber.replace(".", ""))) {
//                //Custom button text
//                Object[] options = {"Yes, proceed to download site", "Not now", "Don't ask again"};
//                int n = JOptionPane.showOptionDialog(this,
//                        "A newer version is available. "
//                        + "Would you like to download Whitebox "
//                        + currentVersionName + " (" + currentVersionNumber
//                        + ")?" + "\nYou are currently using Whitebox " + versionName
//                        + " (" + versionNumber + ").",
//                        "Whitebox Version",
//                        JOptionPane.YES_NO_CANCEL_OPTION,
//                        JOptionPane.QUESTION_MESSAGE,
//                        null,
//                        options,
//                        options[0]);
//
//                if (n == 0) {
//                    Desktop d = Desktop.getDesktop();
//                    d.browse(new URI(downloadLocation));
//                } else if (n == 2) {
//                    skipVersionNumber = currentVersionNumber;
//                }
//            }
//        } catch (UnknownHostException e) {
//            // no internet connection...no big deal.
//            return false;
//        } catch (IOException e) {
//            logger.log(Level.SEVERE, "WhiteboxGui.checkVersionIsUpToDate", e);
//            return false;
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, "WhiteboxGui.checkVersionIsUpToDate", e);
//            return false;
//        }
        return true;
    }

    private String getTextValue(Element ele, String tagName) {
        String textVal = "";
        try {
            NodeList nl = ele.getElementsByTagName(tagName);
            if (nl != null && nl.getLength() > 0) {
                Element el = (Element) nl.item(0);
                textVal = el.getFirstChild().getNodeValue().replace("\"", "");
            }
        } catch (Exception e) {
        } finally {
            return textVal;
        }
    }

    private boolean checkForNewInstallation() {
        if (userName == null || !userName.equals(System.getProperty("user.name"))) {

            userName = System.getProperty("user.name");

            final JDialog dialog = new JDialog(this, "", true);

            Box mainBox = Box.createVerticalBox();
            mainBox.add(Box.createVerticalStrut(15));

            Box hbox1 = Box.createHorizontalBox();
            hbox1.add(Box.createHorizontalGlue());
            String message = messages.getString("Welcome") + " Whitebox " + userName + ".";
            JLabel welcomeLabel = new JLabel(message);
            hbox1.add(welcomeLabel);
            hbox1.add(Box.createHorizontalGlue());
            mainBox.add(hbox1);

            mainBox.add(Box.createVerticalStrut(15));

            Box hbox2 = Box.createHorizontalBox();
            hbox2.add(Box.createHorizontalStrut(15));
            hbox2.add(new JLabel("Please select your preferred language..."));
            hbox2.add(Box.createHorizontalGlue());
            mainBox.add(hbox2);

            mainBox.add(Box.createVerticalStrut(5));

            Box hbox3 = Box.createHorizontalBox();
            hbox3.add(Box.createHorizontalStrut(15));
            ComboBoxProperty languageChooser =
                    SupportedLanguageChooser.getLanguageChooser(this, true);
            languageChooser.setName("languageChooser");
            hbox3.add(languageChooser);
            hbox3.add(Box.createHorizontalStrut(15));
            mainBox.add(hbox3);

            mainBox.add(Box.createVerticalStrut(15));

            Box btnBox = Box.createHorizontalBox();
            btnBox.add(Box.createHorizontalGlue());
            JButton ok = new JButton(bundle.getString("OK"));
            ok.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });
            btnBox.add(ok);

            btnBox.add(Box.createHorizontalGlue());

            mainBox.add(btnBox);

            mainBox.add(Box.createVerticalStrut(15));

            Container contentPane = dialog.getContentPane();
            contentPane.add(mainBox, BorderLayout.CENTER);
            dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            dialog.pack();

            // centers on screen
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);

            return true;

        }

        return false;
    }

    private void loadPlugins() {
        pluginService = PluginServiceFactory.createPluginService(pluginDirectory);
        pluginService.initPlugins();
        plugInfo = pluginService.getPluginList();

        loadScripts();
    }

    private void loadScripts() {
        ArrayList<String> pythonScripts = FileUtilities.findAllFilesWithExtension(resourcesDirectory, ".py", true);
        ArrayList<String> groovyScripts = FileUtilities.findAllFilesWithExtension(resourcesDirectory, ".groovy", true);
        ArrayList<String> jsScripts = FileUtilities.findAllFilesWithExtension(resourcesDirectory, ".js", true);
        //ArrayList<PluginInfo> scriptPlugins = new ArrayList<>();
        for (String str : pythonScripts) {
            try {
                // Open the file
                FileInputStream fstream = new FileInputStream(str);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

                String strLine;

                //Read File Line By Line
                boolean containsName = false;
                boolean containsDescriptiveName = false;
                boolean containsDescription = false;
                boolean containsToolboxes = false;
                String name = "";
                String descriptiveName = "";
                String description = "";
                String[] toolboxes = null;
                while ((strLine = br.readLine()) != null
                        && (!containsName || !containsDescriptiveName
                        || !containsDescription || !containsToolboxes)) {
                    if (strLine.startsWith("name = \"")) {
                        containsName = true;
                        // now retreive the name
                        String[] str2 = strLine.split("=");
                        name = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.startsWith("descriptiveName = \"")) {
                        containsDescriptiveName = true;
                        String[] str2 = strLine.split("=");
                        descriptiveName = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.startsWith("description = \"")) {
                        containsDescription = true;
                        String[] str2 = strLine.split("=");
                        description = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.startsWith("toolboxes = [\"")) {
                        containsToolboxes = true;
                        String[] str2 = strLine.split("=");
                        toolboxes = str2[str2.length - 1].replace("\"", "").replace("\'", "").replace("[", "").replace("]", "").trim().split(",");
                        for (int i = 0; i < toolboxes.length; i++) {
                            toolboxes[i] = toolboxes[i].trim();
                        }
                    }
                }

                //Close the input stream
                br.close();

                if (containsName && containsDescriptiveName
                        && containsDescription && containsToolboxes) {
                    // it's a plugin!
                    PluginInfo pi = new PluginInfo(name, descriptiveName,
                            description, toolboxes, PluginInfo.SORT_MODE_NAMES);
                    pi.setScript(true);
                    pi.setScriptFile(str);
                    plugInfo.add(pi);
                    //scriptPlugins.add(pi);
                }
            } catch (IOException ioe) {
                System.out.println(ioe.getStackTrace());
            }
        }

        for (String str : jsScripts) {
            try {
                // Open the file
                FileInputStream fstream = new FileInputStream(str);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

                String strLine;

                //Read File Line By Line
                boolean containsName = false;
                boolean containsDescriptiveName = false;
                boolean containsDescription = false;
                boolean containsToolboxes = false;
                String name = "";
                String descriptiveName = "";
                String description = "";
                String[] toolboxes = null;
                while ((strLine = br.readLine()) != null
                        && (!containsName || !containsDescriptiveName
                        || !containsDescription || !containsToolboxes)) {
                    if (strLine.toLowerCase().contains("name = \"")
                            && !strLine.toLowerCase().contains("descriptivename")) {
                        containsName = true;
                        // now retreive the name
                        String[] str2 = strLine.split("=");
                        name = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("descriptivename = \"")) {
                        containsDescriptiveName = true;
                        String[] str2 = strLine.split("=");
                        descriptiveName = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("description = \"")) {
                        containsDescription = true;
                        String[] str2 = strLine.split("=");
                        description = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("toolboxes = [\"")) {
                        containsToolboxes = true;
                        String[] str2 = strLine.split("=");
                        toolboxes = str2[str2.length - 1].replace("\"", "").replace("\'", "").replace("[", "").replace("]", "").trim().split(",");
                        for (int i = 0; i < toolboxes.length; i++) {
                            toolboxes[i] = toolboxes[i].trim();
                        }
                    }
                }

                //Close the input stream
                br.close();

                if (containsName && containsDescriptiveName
                        && containsDescription && containsToolboxes) {
                    // it's a plugin!
                    PluginInfo pi = new PluginInfo(name, descriptiveName,
                            description, toolboxes, PluginInfo.SORT_MODE_NAMES);
                    pi.setScript(true);
                    pi.setScriptFile(str);
                    plugInfo.add(pi);
                    //scriptPlugins.add(pi);
                }
            } catch (IOException ioe) {
                System.out.println(ioe.getStackTrace());
            }
        }

        for (String str : groovyScripts) {
            try {
                // Open the file
                FileInputStream fstream = new FileInputStream(str);
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

                String strLine;

                //Read File Line By Line
                boolean containsName = false;
                boolean containsDescriptiveName = false;
                boolean containsDescription = false;
                boolean containsToolboxes = false;
                String name = "";
                String descriptiveName = "";
                String description = "";
                String[] toolboxes = null;
                while ((strLine = br.readLine()) != null
                        && (!containsName || !containsDescriptiveName
                        || !containsDescription || !containsToolboxes)) {
                    if (strLine.toLowerCase().contains("name = \"")
                            && !strLine.toLowerCase().contains("descriptivename")) {
                        containsName = true;
                        // now retreive the name
                        String[] str2 = strLine.split("=");
                        name = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("descriptivename = \"")) {
                        containsDescriptiveName = true;
                        String[] str2 = strLine.split("=");
                        descriptiveName = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("description = \"")) {
                        containsDescription = true;
                        String[] str2 = strLine.split("=");
                        description = str2[str2.length - 1].replace("\"", "").replace("\'", "").trim();
                    } else if (strLine.toLowerCase().contains("toolboxes = [\"")) {
                        containsToolboxes = true;
                        String[] str2 = strLine.split("=");
                        toolboxes = str2[str2.length - 1].replace("\"", "").replace("\'", "").replace("[", "").replace("]", "").trim().split(",");
                        for (int i = 0; i < toolboxes.length; i++) {
                            toolboxes[i] = toolboxes[i].trim();
                        }
                    }
                }

                //Close the input stream
                br.close();

                if (containsName && containsDescriptiveName
                        && containsDescription && containsToolboxes) {
                    // it's a plugin!
                    PluginInfo pi = new PluginInfo(name, descriptiveName,
                            description, toolboxes, PluginInfo.SORT_MODE_NAMES);
                    pi.setScript(true);
                    pi.setScriptFile(str);
                    plugInfo.add(pi);
                    //scriptPlugins.add(pi);
                }
            } catch (IOException ioe) {
                System.out.println(ioe.getStackTrace());
            }
        }
    }
    private ArrayList<WhiteboxPlugin> activePlugs = new ArrayList<>();

    @Override
    public List returnPluginList() {
        List<String> ret = new ArrayList<>();
        for (int i = 0; i < plugInfo.size(); i++) {
            ret.add(plugInfo.get(i).getName());
        }
        Collections.sort(ret);
        return ret;
    }

    public boolean isPluginAScript(String pluginName) {
        boolean isScript = false;
        for (int i = 0; i < plugInfo.size(); i++) {
            PluginInfo pi = plugInfo.get(i);
            if (pi.getDescriptiveName().equals(pluginName)
                    || pi.getName().equals(pluginName)) {
                pi.setLastUsedToNow();
                pi.incrementNumTimesUsed();
                if (pi.isScript()) {
                    isScript = true;
                }
                break;
            }
        }
        return isScript;
    }

    @Override
    public void runPlugin(String pluginName, String[] args, boolean runOnDedicatedThread) {
        try {
            if (!runOnDedicatedThread) { // run on current thread
                boolean isScript = false;
                String scriptFile = null;
                for (int i = 0; i < plugInfo.size(); i++) {
                    PluginInfo pi = plugInfo.get(i);
                    if (pi.getDescriptiveName().equals(pluginName)
                            || pi.getName().equals(pluginName)) {
                        pi.setLastUsedToNow();
                        pi.incrementNumTimesUsed();
                        if (pi.isScript()) {
                            isScript = true;
                            scriptFile = pi.getScriptFile();
                        }
                        break;
                    }
                }


                if (!isScript) {
                    requestForOperationCancel = false;
                    WhiteboxPlugin plug = pluginService.getPlugin(pluginName, StandardPluginService.SIMPLE_NAME);
                    if (plug == null) {
                        throw new Exception("Plugin not located.");
                    }
                    plug.setPluginHost(this);
                    plug.setArgs(args);
                    plug.run();
                } else {
                    // what is the scripting language?
                    if (scriptFile == null) {
                        return; // can't find scriptFile
                    }

                    String myScriptingLanguage;
                    if (scriptFile.toLowerCase().endsWith(".py")) {
                        myScriptingLanguage = "python";
                    } else if (scriptFile.toLowerCase().endsWith(".groovy")) {
                        myScriptingLanguage = "groovy";
                    } else if (scriptFile.toLowerCase().endsWith(".js")) {
                        myScriptingLanguage = "javascript";
                    } else {
                        showFeedback("Unsupported script type.");
                        return;
                    }

                    // has the scripting engine been initialized for the current script language.
                    if (engine == null || !myScriptingLanguage.equals(scriptingLanguage)) {
                        scriptingLanguage = myScriptingLanguage;
                        ScriptEngineManager mgr = new ScriptEngineManager();
                        engine = mgr.getEngineByName(scriptingLanguage);
                    }

                    PrintWriter out = new PrintWriter(new TextAreaWriter(textArea));
                    engine.getContext().setWriter(out);

                    if (scriptingLanguage.equals("python")) {
                        engine.put("__file__", scriptFile);
                    }
                    requestForOperationCancel = false;
                    engine.put("pluginHost", (WhiteboxPluginHost) this);
                    engine.put("args", args);

                    // run the script
                    PrintWriter errOut = new PrintWriter(new TextAreaWriter(textArea));
                    try {
                        // read the contents of the file
                        String scriptContents = new String(Files.readAllBytes(Paths.get(scriptFile)));

                        Object result = engine.eval(scriptContents);
                    } catch (IOException | ScriptException e) {
                        errOut.append(e.getMessage() + "\n");
                    }
                }

            } else { // run on a dedicated thread
                runPlugin(pluginName, args);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "WhiteboxGui.runPlugin", e);
        }
    }
    private boolean suppressReturnedData;

    @Override
    public void runPlugin(String pluginName, String[] args, boolean runOnDedicatedThread,
            boolean suppressReturnedData) {
        try {

            this.suppressReturnedData = suppressReturnedData;
            runPlugin(pluginName, args, runOnDedicatedThread);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "WhiteboxGui.runPlugin", e);
        }
    }

    @Override
    public void runPlugin(String pluginName, String[] args) {
        try {
            boolean isScript = false;
            String scriptFile = null;
            for (int i = 0; i < plugInfo.size(); i++) {
                PluginInfo pi = plugInfo.get(i);
                if (pi.getDescriptiveName().equals(pluginName)
                        || pi.getName().equals(pluginName)) {
                    pi.setLastUsedToNow();
                    pi.incrementNumTimesUsed();
                    if (pi.isScript()) {
                        isScript = true;
                        scriptFile = pi.getScriptFile();
                    }
                    break;
                }
            }


            if (!isScript) {
                requestForOperationCancel = false;
                WhiteboxPlugin plug = pluginService.getPlugin(pluginName, StandardPluginService.SIMPLE_NAME);
                if (plug == null) {
                    throw new Exception("Plugin not located.");
                }
                plug.setPluginHost(this);
                plug.setArgs(args);
                activePlugs.add(plug);
                if (plug instanceof NotifyingThread) {
                    NotifyingThread t = (NotifyingThread) (plug);
                    t.addListener(this);
                }
                new Thread(plug).start();
            } else {
                // what is the scripting language?
                if (scriptFile == null) {
                    return; // can't find scriptFile
                }

                String myScriptingLanguage;
                if (scriptFile.toLowerCase().endsWith(".py")) {
                    myScriptingLanguage = "python";
                } else if (scriptFile.toLowerCase().endsWith(".groovy")) {
                    myScriptingLanguage = "groovy";
                } else if (scriptFile.toLowerCase().endsWith(".js")) {
                    myScriptingLanguage = "javascript";
                } else {
                    showFeedback("Unsupported script type.");
                    return;
                }

                // has the scripting engine been initialized for the current script language.
                if (engine == null || !myScriptingLanguage.equals(scriptingLanguage)) {
                    scriptingLanguage = myScriptingLanguage;
                    ScriptEngineManager mgr = new ScriptEngineManager();
                    engine = mgr.getEngineByName(scriptingLanguage);
                }

                PrintWriter out = new PrintWriter(new TextAreaWriter(textArea));
                engine.getContext().setWriter(out);

                if (scriptingLanguage.equals("python")) {
                    engine.put("__file__", scriptFile);
                }
                requestForOperationCancel = false;
                engine.put("pluginHost", (WhiteboxPluginHost) this);
                engine.put("args", args);

                // run the script
                // read the contents of the file
                final String scriptContents = new String(Files.readAllBytes(Paths.get(scriptFile)));

                //Object result = engine.eval(scriptContents);
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            engine.eval(scriptContents);
                        } catch (ScriptException e) {
                            System.out.println(e.getStackTrace());
                        }
                    }
                };
                final Thread t = new Thread(r);
                t.start();

            }

            //pool.submit(plug);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "WhiteboxGui.runPlugin", e);
            //System.err.println(e.getLocalizedMessage());
        }
    }
    private boolean automaticallyDisplayReturns = true;

    @Override
    public void returnData(Object ret) {
        try {
            if (suppressReturnedData) {
                return;
            }
            // this is where all of the data returned by plugins is handled.
            if (ret instanceof String) {
                String retStr = ret.toString();
                if (retStr.endsWith(".dep") && retStr.contains(pathSep)) {
                    if (automaticallyDisplayReturns) {
                        addLayer(retStr);
                    }
                } else if (retStr.endsWith(".shp") && retStr.contains(pathSep)) {
                    if (automaticallyDisplayReturns) {
                        addLayer(retStr);
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
//            } else if (retStr.contains("<html>") && retStr.contains("</html>")) {
//                // display this markup in a webbrowser component
//                try {
//                    JFrame frame = new HTMLViewer(retStr);
//                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//                    frame.setSize(600, 600);
//                    frame.setVisible(true);
//                } catch (Exception e) {
//                }
                } else if (retStr.toLowerCase().startsWith("newmap")) {
                    String mapName = "NewMap";
                    if (retStr.contains(":")) {
                        String[] val = retStr.split(":");
                        mapName = val[val.length - 1].trim();
                    }
                    newMap(mapName);
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
//                int k = panel.getPreferredSize().height;
                if (panel.getPreferredSize().height > 100) {
                    dialog.setPreferredSize(panel.getPreferredSize());
                } else {
                    dialog.setPreferredSize(new Dimension(500, 500));
                }
                if (panel.getName() != null) {
                    dialog.setTitle(panel.getName());
                }
                dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "WhiteboxGui.returnData", e);
        } finally {
            suppressReturnedData = false;
        }
    }

    @Override
    public void launchDialog(String pluginName) {
        // update the tools lists
        populateToolTabs();
        
        boolean isScript = false;
        String scriptFile = null;
        for (int i = 0; i < plugInfo.size(); i++) {
            PluginInfo pi = plugInfo.get(i);
            if (pi.getDescriptiveName().equals(pluginName) ||
                    pi.getName().equals(pluginName)) {
                pi.setLastUsedToNow();
                pi.incrementNumTimesUsed();
                if (pi.isScript()) {
                    isScript = true;
                    scriptFile = pi.getScriptFile();
                }
                break;
            }
        }
        
        if (!isScript) {
            

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
            } catch (ParserConfigurationException | SAXException | IOException e) {
                logger.log(Level.SEVERE, "WhiteboxGui.launchDialog", e);
                //System.out.println(e.getMessage());
            }

            if (pluginProvidesDialog) {
                String[] args = {""};
                runPlugin(plug.getName(), args);
            } else {
                // use the xml-based dialog provided in the Dialog folder.
                String helpFile = helpDirectory + plug.getName() + ".html";
                String descriptiveName;
                if (pluginBundle.containsKey(plug.getName())) {
                    descriptiveName = pluginBundle.getString(plug.getName());
                } else {
                    descriptiveName = plug.getDescriptiveName();
                }
                ToolDialog dlg = new ToolDialog(this, false, plug.getName(), descriptiveName, helpFile);
                dlg.setSize(800, 400);
                dlg.setVisible(true);
            }
        } else {
            // what is the scripting language?
            if (scriptFile == null) {
                return; // can't find scriptFile
            }

            String myScriptingLanguage;
            if (scriptFile.toLowerCase().endsWith(".py")) {
                myScriptingLanguage = "python";
            } else if (scriptFile.toLowerCase().endsWith(".groovy")) {
                myScriptingLanguage = "groovy";
            } else if (scriptFile.toLowerCase().endsWith(".js")) {
                myScriptingLanguage = "javascript";
            } else {
                showFeedback("Unsupported script type.");
                return;
            }

            // has the scripting engine been initialized for the current script language.
            if (engine == null || !myScriptingLanguage.equals(scriptingLanguage)) {
                scriptingLanguage = myScriptingLanguage;
                ScriptEngineManager mgr = new ScriptEngineManager();
                engine = mgr.getEngineByName(scriptingLanguage);
            }

            PrintWriter out = new PrintWriter(new TextAreaWriter(textArea));
            engine.getContext().setWriter(out);

            if (scriptingLanguage.equals("python")) {
                engine.put("__file__", scriptFile);
            }
            requestForOperationCancel = false;
            engine.put("pluginHost", (WhiteboxPluginHost) this);
            engine.put("args", new String[0]);

            //ScriptEngineFactory scriptFactory = engine.getFactory();

            // run the script
            //PrintWriter errOut = new PrintWriter(new TextAreaWriter(textArea));
            try {
                // read the contents of the file
                String scriptContents = new String(Files.readAllBytes(Paths.get(scriptFile)));

                Object result = engine.eval(scriptContents);
            } catch (IOException | ScriptException e) {
                System.out.println(e.getStackTrace());
            }
        }
    }
    private ScriptEngine engine;
    private String scriptingLanguage = "python";

    public final class TextAreaWriter extends Writer {

        private final JTextArea textArea;

        public TextAreaWriter(final JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            textArea.append(new String(cbuf, off, len));
        }
    }
    private boolean requestForOperationCancel = false;

    /**
     * Used to communicate a request to cancel an operation
     */
    @Override
    public boolean isRequestForOperationCancelSet() {
        return requestForOperationCancel;
    }

    /**
     * Used to ensure that there is no active cancel operation request
     */
    @Override
    public void resetRequestForOperationCancel() {
        requestForOperationCancel = false;
    }

    @Override
    public void cancelOperation() {
        requestForOperationCancel = true;

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
        ArrayList<WhiteboxPlugin> toRemove = new ArrayList<>();
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
            logger.log(Level.SEVERE, "WhiteboxGui.refreshMap", e);
            //showFeedback(e.getStackTrace().toString());
        }
    }

    @Override
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        recentDirectoriesMenu.addMenuItem(workingDirectory);
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
    public String getLogDirectory() {
        return logDirectory;
    }

    @Override
    public String getResourcesDirectory() {
        return resourcesDirectory;
    }
    
    @Override
    /**
     * Used to retrieve all of the files currently displayed in the active map.
     * @return String[] of file names of displayed raster and vector files.
     */
    public String[] getCurrentlyDisplayedFiles() {
        MapArea activeMapArea = openMaps.get(activeMap).getActiveMapArea();
        ArrayList<MapLayer> myLayers = activeMapArea.getLayersList();
        String[] ret = new String[myLayers.size()];
        int i = 0;
        for (MapLayer maplayer : myLayers) {
            if (maplayer.getLayerType() == MapLayer.MapLayerType.RASTER) {
                      RasterLayerInfo raster = (RasterLayerInfo) maplayer;
                        ret[i] = raster.getHeaderFile();
            } else if (maplayer.getLayerType() == MapLayer.MapLayerType.VECTOR) {
                VectorLayerInfo vector = (VectorLayerInfo)maplayer;
                ret[i] = vector.getFileName();
            }
            i++;
        }
        return ret;
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
                if (props.containsKey("defaultMapMargin")) {
                    defaultMapMargin = Double.parseDouble(props.getProperty("defaultMapMargin"));
                }
                String[] FONTS = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                if (props.containsKey("defaultFont")) {
                    String fontName = props.getProperty("defaultFont");
                    for (String fnt : FONTS) {
                        if (fnt.equals(fontName)) {
                            defaultFont = new Font(fnt, Font.PLAIN, 11);
                            break;
                        }
                    }
                }
                // See if the defaultFont was found. If not, see if Arial is available.
                if (defaultFont == null) {
                    String fontName = "arial";
                    for (String fnt : FONTS) {
                        if (fnt.toLowerCase().equals(fontName)) {
                            defaultFont = new Font(fnt, Font.PLAIN, 11);
                            break;
                        }
                    }
                    if (defaultFont == null) { // if arial is not available go with the java SanSerif default.
                        defaultFont = new Font("SanSerif", Font.PLAIN, 11);
                    }
                }

                if (props.containsKey("numberOfRecentItemsToStore")) {
                    numberOfRecentItemsToStore =
                            Integer.parseInt(props.getProperty("numberOfRecentItemsToStore"));
                    recentFilesMenu.setNumItemsToStore(numberOfRecentItemsToStore);
                    recentFilesPopupMenu.setNumItemsToStore(numberOfRecentItemsToStore);
                    recentMapsMenu.setNumItemsToStore(numberOfRecentItemsToStore);
                    recentDirectoriesMenu.setNumItemsToStore(numberOfRecentItemsToStore);
                }

                // retrieve the recent data layers info
                if (props.containsKey("recentDataLayers")) {
                    String[] recentDataLayers = props.getProperty("recentDataLayers").split(",");
                    for (int i = recentDataLayers.length - 1; i >= 0; i--) { // add them in reverse order
                        recentFilesMenu.addMenuItem(recentDataLayers[i]);
                        recentFilesPopupMenu.addMenuItem(recentDataLayers[i]);
                    }
                }

                // retrieve the recent maps info
                if (props.containsKey("recentMaps")) {
                    String[] recentMaps = props.getProperty("recentMaps").split(",");
                    for (int i = recentMaps.length - 1; i >= 0; i--) { // add them in reverse order
                        recentMapsMenu.addMenuItem(recentMaps[i]);
                    }
                }

                // retrieve the recent workingDirectories info
                if (props.containsKey("recentWorkingDirectories")) {
                    String[] recentDirectories = props.getProperty("recentWorkingDirectories").split(",");
                    for (int i = recentDirectories.length - 1; i >= 0; i--) { // add them in reverse order
                        recentDirectoriesMenu.addMenuItem(recentDirectories[i]);
                    }
                }

                // retrieve the skipVersionNumber
                if (props.containsKey("skipVersionNumber")) {
                    skipVersionNumber = props.getProperty("skipVersionNumber");
                }

                // retrieve the announcementNumber
                if (props.containsKey("announcementNumber")) {
                    announcementNumber = Integer.parseInt(props.getProperty("announcementNumber"));
                }

                // retrieve the langauge setting
                if (props.containsKey("language")) {
                    language = props.getProperty("language");
                }

                // retrieve the country setting
                if (props.containsKey("country")) {
                    country = props.getProperty("country");
                }

                // receive announcements
                if (props.containsKey("receiveAnnouncements")) {
                    receiveAnnouncements = Boolean.parseBoolean(props.getProperty("receiveAnnouncements"));
                }

                // check for updates
                if (props.containsKey("checkForUpdates")) {
                    checkForUpdates = Boolean.parseBoolean(props.getProperty("checkForUpdates"));
                }

                // retrieve the plugin usage information
                String[] pluginNames = props.getProperty("pluginNames").split(",");
                String[] pluginUsage = props.getProperty("pluginUsage").split(",");
                String[] pluginLastUse = props.getProperty("toolLastUse").split(",");
                String plugName;
                DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
                Date lastUsed = null;
//                for (int i = 0; i < plugInfo.size(); i++) {
//                    plugName = plugInfo.get(i).getName();
//                    for (int j = 0; j < pluginNames.length; j++) {
//                        if (pluginNames[j].equals(plugName)) {
//                            try {
//                                lastUsed = df.parse(pluginLastUse[j]);
//                            } catch (ParseException e) {
//                            }
//                            plugInfo.get(i).setLastUsed(lastUsed);
//                            plugInfo.get(i).setNumTimesUsed(Integer.parseInt(pluginUsage[j]));
//                        }
//                    }
//                }

            } catch (IOException e) {
                logger.log(Level.SEVERE, "WhiteboxGui.getApplicationProperties", e);
            }

        } else {
            setWorkingDirectory(resourcesDirectory + "samples");
            splitterLoc1 = 250;
            splitterToolboxLoc = 250;
            tbTabsIndex = 0;
            qlTabsIndex = 0;
            defaultQuantPalette = "spectrum.pal";
            defaultQualPalette = "qual.pal";

//            int k = 0;
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
        props.setProperty("splitterToolboxLoc", Integer.toString(splitPane2.getDividerLocation() - 2)); //qlTabs.getSize().height));
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
        props.setProperty("defaultFont", defaultFont.getName());
        props.setProperty("numberOfRecentItemsToStore", Integer.toString(numberOfRecentItemsToStore));
        props.setProperty("defaultMapMargin", Double.toString(defaultMapMargin));
        props.setProperty("skipVersionNumber", skipVersionNumber);
        props.setProperty("announcementNumber", Integer.toString(announcementNumber));
        props.setProperty("language", language);
        props.setProperty("country", country);
        props.setProperty("receiveAnnouncements", Boolean.toString(receiveAnnouncements));
        props.setProperty("checkForUpdates", Boolean.toString(checkForUpdates));

        // set the recent data layers
        String recentDataLayers = "";
        List<String> layersList = recentFilesMenu.getList();
        for (String str : layersList) {
            if (!recentDataLayers.isEmpty()) {
                recentDataLayers += "," + str;
            } else {
                recentDataLayers += str;
            }
        }
        props.setProperty("recentDataLayers", recentDataLayers);

        // set the recent maps info
        String recentMaps = "";
        List<String> mapList = recentMapsMenu.getList();
        for (String str : mapList) {
            if (!recentMaps.isEmpty()) {
                recentMaps += "," + str;
            } else {
                recentMaps += str;
            }
        }
        props.setProperty("recentMaps", recentMaps);


        // set the recent working directories info
        String recentDirectories = "";
        List<String> directoriesList = recentDirectoriesMenu.getList();
        for (String str : directoriesList) {
            if (!recentDirectories.isEmpty()) {
                recentDirectories += "," + str;
            } else {
                recentDirectories += str;
            }
        }
        props.setProperty("recentWorkingDirectories", recentDirectories);


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
            try (FileOutputStream out = new FileOutputStream(propsFile)) {
                props.store(out, "--No comments--");
            }
        } catch (IOException e) {
            // do nothing
        }

    }

    private void showToolDescription(String pluginName) {
        for (PluginInfo pi : plugInfo) {
            if (pi.getDescriptiveName().equals(pluginName)) {
                status.setMessage(pi.getDescription());
                break;
            }
        }
//        WhiteboxPlugin plug = pluginService.getPlugin(pluginName, StandardPluginService.DESCRIPTIVE_NAME);
//        status.setMessage(plug.getToolDescription());
    }

    private void createGui() {
        try {
            if (System.getProperty("os.name").contains("Mac")) {

                try {
                    Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
                    Class params[] = new Class[2];
                    params[0] = Window.class;
                    params[1] = Boolean.TYPE;
                    Method method = util.getMethod("setWindowCanFullScreen", params);
                    method.invoke(util, this, true);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
                    logger.log(Level.SEVERE, "WhiteboxGui.createGui", e);
                }

                //this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
                UIManager.put("apple.awt.brushMetalLook", Boolean.TRUE);
            }

            // add the menubar and toolbar
            createMenu();
            createPopupMenus();
            createToolbar();
            ctb = new CartographicToolbar(this, false);
            ctb.setOrientation(SwingConstants.VERTICAL);
            this.getContentPane().add(ctb, BorderLayout.EAST);

            MapInfo mapinfo = new MapInfo(bundle.getString("Map") + "1");
            mapinfo.setMapName(bundle.getString("Map") + "1");
            mapinfo.setPageFormat(defaultPageFormat);
            mapinfo.setWorkingDirectory(workingDirectory);
            mapinfo.setDefaultFont(defaultFont);
            mapinfo.setMargin(defaultMapMargin);

            MapArea ma = new MapArea(bundle.getString("MapArea").replace(" ", "") + "1");
            ma.setUpperLeftX(-32768);
            ma.setUpperLeftY(-32768);
            ma.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
            mapinfo.addNewCartographicElement(ma);

            openMaps.add(mapinfo);
            activeMap = 0;
            drawingArea.setMapInfo(mapinfo);
            drawingArea.setStatusBar(status);
            drawingArea.setScaleText(scaleText);
            drawingArea.setHost(this);


            textArea.setLineWrap(false);
            textArea.setWrapStyleWord(false);
            textArea.setFont(new Font(defaultFont.getName(), Font.PLAIN, 12));
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

//            if (System.getProperty("os.name").contains("Mac")) {
//
//                try {
//                    Class util = Class.forName("com.apple.eawt.FullScreenUtilities");
//                    Class params[] = new Class[2];
//                    params[0] = Window.class;
//                    params[1] = Boolean.TYPE;
//                    Method method = util.getMethod("setWindowCanFullScreen", params);
//                    method.invoke(util, this, true);
//                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
//                    logger.log(Level.SEVERE, "WhiteboxGui.createGui", e);
//                }
//
//                //this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
//                UIManager.put("apple.awt.brushMetalLook", Boolean.TRUE);
//            }
            this.setMinimumSize(new Dimension(700, 500));
            this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);

            // set the message indicating the number of plugins that were located.
            status.setMessage(" " + plugInfo.size() + " plugins were located");

            splitPane2.setDividerLocation(0.75); //splitterToolboxLoc);

            pack();
            restoreDefaults();
        } catch (SecurityException | IllegalArgumentException | InvocationTargetException e) {
            logger.log(Level.SEVERE, "WhiteboxGui.createGui", e);
            showFeedback(e.toString());
        }
    }

    private void restoreDefaults() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                splitPane3.setDividerLocation(1.0);
            }
        });
    }

    private void displayAnnouncements() {
        if (announcements.size() < 1) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        sb.append("<html>\n");
        sb.append("  <head>\n");
        sb.append("    <title>Whitebox Announcements");
//        sb.append(announcementNumber);
        sb.append("</title>\n");
        //sb.append("<meta content=\"text/html; charset=iso-8859-1\" http-equiv=\"content-type\">\n");
        sb.append("<style media=\"screen\" type=\"text/css\">\n"
                + "h1\n"
                + "{\n"
                + "font-family: Helvetica, Verdana, Geneva, Arial, sans-serif;\n"
                + "font-size: 12pt;\n"
                + "background-color: rgb(200,215,250); \n"
                + "font-weight: bold;\n"
                + "line-height: 20pt;\n"
                + "margin-left: 10px;\n"
                + "margin-right: 10px;\n"
                + "}\n"
                + "p\n"
                + "{\n"
                + "text-align: left;\n"
                + "color:black;\n"
                + "font-family:Verdana, Geneva, Arial, Helvetica, sans-serif;\n"
                + "font-size: 10pt;\n"
                + "background-color: transparent;\n"
                + "line-height: normal;\n"
                + "margin-left: 10px;\n"
                + "margin-right: 10px;\n"
                + "}\n"
                + "ul\n"
                + "{\n"
                + "list-style-type: square;\n"
                + "list-style-position: inside;\n"
                + "font-family:Verdana, Geneva, Arial, Helvetica, sans-serif;\n"
                + "font-size: 10pt;\n"
                + "margin-left: 10px;\n"
                + "margin-bottom: 0;\n"
                + "margin-top: 5px;\n"
                + "}"
                + "\n"
                + "</style>\n");
        sb.append("  </head>\n");
        sb.append("  <body><h1><b>Whitebox Announcements").append("</b></h1>\n");
        for (WhiteboxAnnouncement wba : announcements) {
            sb.append("    <p>");
            if (!wba.getTitle().isEmpty()) {
                sb.append("<b>").append(wba.getTitle()).append("</b><br>\n");
            }
            sb.append("      ").append(wba.getMessage()).append("\n");
            if (!wba.getDate().isEmpty()) {
                sb.append("<br>Date: ").append(wba.getDate());
            }
            sb.append("    </p>\n");
        }
        sb.append("  </body>\n");
        sb.append("</html>\n");


        //JFrame frame = new JFrame();
        //frame.setAlwaysOnTop(true);
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setText(sb.toString());
        pane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent r) {
                try {
                    if (r.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        Desktop d = Desktop.getDesktop();
                        String linkName = r.getURL().toString();
                        d.browse(new URI(linkName));
                    }
                } catch (URISyntaxException | IOException e) {
                    logger.log(Level.SEVERE, "WhiteboxGui.displayAnnouncement", e);
                    //System.err.println("WhiteboxGui.displayAnnouncement Error: " + e.toString());
                }
            }
        });
        pane.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(pane);
        JDialog dialog = new JDialog(this, "");
        Container contentPane = dialog.getContentPane();
        contentPane.add(scroll, BorderLayout.CENTER);
        dialog.setPreferredSize(new Dimension(500, 500));
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void createMenu() {
        try {
            JMenuBar menubar = new JMenuBar();

            JMenuItem newMap = new JMenuItem(bundle.getString("NewMap"),
                    new ImageIcon(graphicsDirectory + "map.png"));
            JMenuItem openMap = new JMenuItem(bundle.getString("OpenMap"),
                    new ImageIcon(graphicsDirectory + "open.png"));
            JMenuItem saveMap = new JMenuItem(bundle.getString("SaveMap"),
                    new ImageIcon(graphicsDirectory + "SaveMap.png"));
            JMenuItem closeMap = new JMenuItem(bundle.getString("CloseMap"));
            JMenuItem close = new JMenuItem(bundle.getString("Close"));

            JMenuItem layerProperties = new JMenuItem(bundle.getString("LayerDisplayProperties"));
            layerProperties.setActionCommand("layerProperties");
            layerProperties.addActionListener(this);
            JMenuItem options = new JMenuItem(bundle.getString("OptionsAndSettings"));

            JMenuItem rasterCalc = new JMenuItem(bundle.getString("RasterCalculator"),
                    new ImageIcon(graphicsDirectory + "RasterCalculator.png"));
            modifyPixels = new JCheckBoxMenuItem(bundle.getString("ModifyPixelValues"),
                    new ImageIcon(graphicsDirectory + "ModifyPixels.png"));
            JMenuItem paletteManager = new JMenuItem(bundle.getString("PaletteManager"),
                    new ImageIcon(graphicsDirectory + "paletteManager.png"));
            JMenuItem refreshTools = new JMenuItem(bundle.getString("RefreshToolUsage"));


            // File menu
            JMenu FileMenu = new JMenu(bundle.getString("File"));
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
            JMenuItem printMap = new JMenuItem(bundle.getString("PrintMap"), new ImageIcon(graphicsDirectory + "Print.png"));
            FileMenu.add(printMap);
            printMap.addActionListener(this);
            printMap.setActionCommand("printMap");
            printMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            JMenuItem exportMap = new JMenuItem(bundle.getString("ExportMapAsImage"));
            FileMenu.add(exportMap);
            exportMap.addActionListener(this);
            exportMap.setActionCommand("exportMapAsImage");
            if (System.getProperty("os.name").contains("Mac") == false) {
                FileMenu.addSeparator();
                FileMenu.add(close);
                close.setActionCommand("close");
                close.addActionListener(this);
            }

            FileMenu.addSeparator();


            recentFilesMenu.setNumItemsToStore(numberOfRecentItemsToStore);
            recentFilesMenu.setText(bundle.getString("RecentDataLayers"));
            recentFilesMenu.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addLayer(e.getActionCommand());
                }
            });
            FileMenu.add(recentFilesMenu);

            recentMapsMenu.setNumItemsToStore(numberOfRecentItemsToStore);
            recentMapsMenu.setText(bundle.getString("RecentMaps"));
            recentMapsMenu.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openMap(e.getActionCommand());
                }
            });
            FileMenu.add(recentMapsMenu);

            recentDirectoriesMenu.setNumItemsToStore(numberOfRecentItemsToStore);
            recentDirectoriesMenu.setText(bundle.getString("RecentWorkingDirectories"));
            recentDirectoriesMenu.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setWorkingDirectory(e.getActionCommand());
                }
            });
            FileMenu.add(recentDirectoriesMenu);

            menubar.add(FileMenu);

            // Layers menu
            JMenu LayersMenu = new JMenu(bundle.getString("Data_Layers"));
            JMenuItem addLayers = new JMenuItem(bundle.getString("AddLayersToMap"),
                    new ImageIcon(graphicsDirectory + "AddLayer.png"));
            LayersMenu.add(addLayers);
            addLayers.addActionListener(this);
            addLayers.setActionCommand("addLayer");
            addLayers.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            JMenuItem removeLayers = new JMenuItem(
                    bundle.getString("RemoveActiveLayerFromMap"),
                    new ImageIcon(graphicsDirectory + "RemoveLayer.png"));
            LayersMenu.add(removeLayers);
            removeLayers.addActionListener(this);
            removeLayers.setActionCommand("removeLayer");
            removeLayers.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            JMenuItem removeAllLayers = new JMenuItem(
                    bundle.getString("RemoveAllLayers"));
            LayersMenu.add(removeAllLayers);
            removeAllLayers.addActionListener(this);
            removeAllLayers.setActionCommand("removeAllLayers");
            JMenuItem allLayersInvisible = new JMenuItem(
                    bundle.getString("HideAllLayers"));
            LayersMenu.add(allLayersInvisible);
            allLayersInvisible.addActionListener(this);
            allLayersInvisible.setActionCommand("allLayersInvisible");
            JMenuItem allLayersVisible = new JMenuItem(
                    bundle.getString("ShowAllLayers"));
            LayersMenu.add(allLayersVisible);
            allLayersVisible.addActionListener(this);
            allLayersVisible.setActionCommand("allLayersVisible");
            LayersMenu.addSeparator();
            JMenuItem raiseLayers = new JMenuItem(bundle.getString("RaiseLayer"),
                    new ImageIcon(graphicsDirectory + "PromoteLayer.png"));
            LayersMenu.add(raiseLayers);
            raiseLayers.addActionListener(this);
            raiseLayers.setActionCommand("raiseLayer");
            JMenuItem lowerLayers = new JMenuItem(bundle.getString("LowerLayer"),
                    new ImageIcon(graphicsDirectory + "DemoteLayer.png"));
            LayersMenu.add(lowerLayers);
            lowerLayers.addActionListener(this);
            lowerLayers.setActionCommand("lowerLayer");
            JMenuItem layerToTop = new JMenuItem(bundle.getString("LayerToTop"),
                    new ImageIcon(graphicsDirectory + "LayerToTop.png"));
            LayersMenu.add(layerToTop);
            layerToTop.addActionListener(this);
            layerToTop.setActionCommand("layerToTop");
            JMenuItem layerToBottom = new JMenuItem(bundle.getString("LayerToBottom"),
                    new ImageIcon(graphicsDirectory + "LayerToBottom.png"));
            LayersMenu.add(layerToBottom);
            layerToBottom.addActionListener(this);
            layerToBottom.setActionCommand("layerToBottom");

            LayersMenu.addSeparator();

            JMenuItem viewAttributeTable = new JMenuItem(bundle.getString("ViewAttributeTable"),
                    new ImageIcon(graphicsDirectory + "AttributeTable.png"));
            LayersMenu.add(viewAttributeTable);
            viewAttributeTable.addActionListener(this);
            viewAttributeTable.setActionCommand("viewAttributeTable");

            JMenuItem histoMenuItem = new JMenuItem(bundle.getString("ViewHistogram"));
            histoMenuItem.addActionListener(this);
            histoMenuItem.setActionCommand("viewHistogram");
            LayersMenu.add(histoMenuItem);

            LayersMenu.addSeparator();
            JMenuItem clipLayerToExtent = new JMenuItem(bundle.getString("ClipLayerToCurrentExtent"));
            clipLayerToExtent.addActionListener(this);
            clipLayerToExtent.setActionCommand("clipLayerToExtent");
            LayersMenu.add(clipLayerToExtent);
            menubar.add(LayersMenu);



            // View menu
            JMenu viewMenu = new JMenu(bundle.getString("View"));

            selectMenuItem = new JCheckBoxMenuItem(bundle.getString("SelectMapElement"),
                    new ImageIcon(graphicsDirectory + "select.png"));
            viewMenu.add(selectMenuItem);
            selectMenuItem.addActionListener(this);
            selectMenuItem.setActionCommand("select");

            selectFeatureMenuItem = new JCheckBoxMenuItem(bundle.getString("SelectFeature"),
                    new ImageIcon(graphicsDirectory + "SelectFeature.png"));
            viewMenu.add(selectFeatureMenuItem);
            selectFeatureMenuItem.addActionListener(this);
            selectFeatureMenuItem.setActionCommand("selectFeature");

            panMenuItem = new JCheckBoxMenuItem(bundle.getString("Pan"),
                    new ImageIcon(graphicsDirectory + "Pan2.png"));
            viewMenu.add(panMenuItem);
            panMenuItem.addActionListener(this);
            panMenuItem.setActionCommand("pan");

            zoomMenuItem = new JCheckBoxMenuItem(bundle.getString("ZoomIn"),
                    new ImageIcon(graphicsDirectory + "ZoomIn.png"));
            viewMenu.add(zoomMenuItem);
            zoomMenuItem.addActionListener(this);
            zoomMenuItem.setActionCommand("zoomToBox");
            zoomMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            zoomOutMenuItem = new JCheckBoxMenuItem(bundle.getString("ZoomOut"),
                    new ImageIcon(graphicsDirectory + "ZoomOut.png"));
            viewMenu.add(zoomOutMenuItem);
            zoomOutMenuItem.addActionListener(this);
            zoomOutMenuItem.setActionCommand("zoomOut");
            zoomOutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            JMenuItem zoomToFullExtent = new JMenuItem(bundle.getString("ZoomMapAreaToFullExtent"),
                    new ImageIcon(graphicsDirectory + "Globe.png"));
            zoomToFullExtent.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            viewMenu.add(zoomToFullExtent);
            zoomToFullExtent.addActionListener(this);
            zoomToFullExtent.setActionCommand("zoomToFullExtent");

            JMenuItem zoomToPage = new JMenuItem(bundle.getString("ZoomToPage"),
                    new ImageIcon(graphicsDirectory + "ZoomToPage.png"));
            viewMenu.add(zoomToPage);
            zoomToPage.addActionListener(this);
            zoomToPage.setActionCommand("zoomToPage");

            selectMenuItem.setState(true);
            selectFeatureMenuItem.setState(false);
            zoomMenuItem.setState(false);
            zoomOutMenuItem.setState(false);
            panMenuItem.setState(false);

            JMenuItem panLeft = new JMenuItem(bundle.getString("PanLeft"));
            viewMenu.add(panLeft);
            panLeft.addActionListener(this);
            panLeft.setActionCommand("panLeft");
            panLeft.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            JMenuItem panRight = new JMenuItem(bundle.getString("PanRight"));
            viewMenu.add(panRight);
            panRight.addActionListener(this);
            panRight.setActionCommand("panRight");
            panRight.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            JMenuItem panUp = new JMenuItem(bundle.getString("PanUp"));
            viewMenu.add(panUp);
            panUp.addActionListener(this);
            panUp.setActionCommand("panUp");
            panUp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            JMenuItem panDown = new JMenuItem(bundle.getString("PanDown"));
            viewMenu.add(panDown);
            panDown.addActionListener(this);
            panDown.setActionCommand("panDown");
            panDown.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

            JMenuItem previousExtent = new JMenuItem(bundle.getString("PreviousExtent"),
                    new ImageIcon(graphicsDirectory + "back.png"));
            viewMenu.add(previousExtent);
            previousExtent.addActionListener(this);
            previousExtent.setActionCommand("previousExtent");

            JMenuItem nextExtent = new JMenuItem(bundle.getString("NextExtent"),
                    new ImageIcon(graphicsDirectory + "forward.png"));
            viewMenu.add(nextExtent);
            nextExtent.addActionListener(this);
            nextExtent.setActionCommand("nextExtent");

            viewMenu.addSeparator();
            JMenuItem refresh = new JMenuItem(bundle.getString("RefreshMap"));
            viewMenu.add(refresh);
            refresh.addActionListener(this);
            refresh.setActionCommand("refreshMap");

//            linkMap = new JCheckBoxMenuItem(bundle.getString("LinkOpenMaps"));
//            viewMenu.add(linkMap);
//            linkMap.setActionCommand("linkMap");
//            linkMap.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//            linkMap.addActionListener(this);

            viewMenu.addSeparator();
            JMenuItem mapProperties = new JMenuItem(bundle.getString("MapProperties"));
            mapProperties.addActionListener(this);
            mapProperties.setActionCommand("mapProperties");
            viewMenu.add(mapProperties);
            viewMenu.add(layerProperties);
            JMenuItem viewHistogram = new JMenuItem(bundle.getString("ViewHistogram"));
            viewHistogram.setActionCommand("viewHistogram");
            viewHistogram.addActionListener(this);
            viewMenu.add(viewHistogram);
            viewMenu.add(options);
            options.addActionListener(this);
            options.setActionCommand("options");

            menubar.add(viewMenu);


            // Cartographic menu
            JMenu cartoMenu = new JMenu(bundle.getString("Cartographic"));

            JMenuItem insertTitle = new JMenuItem(bundle.getString("InsertMapTitle"));
            cartoMenu.add(insertTitle);
            insertTitle.addActionListener(this);
            insertTitle.setActionCommand("insertTitle");

            JMenuItem insertNorthArrow = new JMenuItem(bundle.getString("InsertNorthArrow"));
            cartoMenu.add(insertNorthArrow);
            insertNorthArrow.addActionListener(this);
            insertNorthArrow.setActionCommand("insertNorthArrow");

            JMenuItem insertScale = new JMenuItem(bundle.getString("InsertScale"));
            cartoMenu.add(insertScale);
            insertScale.addActionListener(this);
            insertScale.setActionCommand("insertScale");

            JMenuItem insertLegend = new JMenuItem(bundle.getString("InsertLegend"));
            cartoMenu.add(insertLegend);
            insertLegend.addActionListener(this);
            insertLegend.setActionCommand("insertLegend");

            JMenuItem insertNeatline = new JMenuItem(bundle.getString("InsertNeatline"));
            cartoMenu.add(insertNeatline);
            insertNeatline.addActionListener(this);
            insertNeatline.setActionCommand("insertNeatline");

            JMenuItem insertMapArea = new JMenuItem(bundle.getString("InsertMapArea"),
                    new ImageIcon(graphicsDirectory + "mapArea.png"));
            cartoMenu.add(insertMapArea);
            insertMapArea.addActionListener(this);
            insertMapArea.setActionCommand("insertMapArea");

            JMenuItem insertTextArea = new JMenuItem(bundle.getString("InsertTextArea"));
            cartoMenu.add(insertTextArea);
            insertTextArea.addActionListener(this);
            insertTextArea.setActionCommand("insertTextArea");

            JMenuItem insertImage = new JMenuItem(bundle.getString("InsertImage"));
            cartoMenu.add(insertImage);
            insertImage.addActionListener(this);
            insertImage.setActionCommand("insertImage");

            cartoMenu.addSeparator();

            JMenuItem pageProps = new JMenuItem(bundle.getString("PageProperties"),
                    new ImageIcon(graphicsDirectory + "page.png"));
            cartoMenu.add(pageProps);
            pageProps.addActionListener(this);
            pageProps.setActionCommand("pageProps");


            // align and distribute sub-menu
            cartoMenu.addSeparator();
            JMenu alignAndDistribute = new JMenu(bundle.getString("AlignAndDistribute"));
            cartoMenu.add(alignAndDistribute);

            JMenuItem alignRightMenu = new JMenuItem(bundle.getString("AlignRight"),
                    new ImageIcon(graphicsDirectory + "AlignRight.png"));
            alignRightMenu.addActionListener(this);
            alignRightMenu.setActionCommand("alignRight");
            alignAndDistribute.add(alignRightMenu);

            JMenuItem centerVerticalMenu = new JMenuItem(bundle.getString("CenterVertically"),
                    new ImageIcon(graphicsDirectory + "CenterVertical.png"));
            centerVerticalMenu.addActionListener(this);
            centerVerticalMenu.setActionCommand("centerVertical");
            alignAndDistribute.add(centerVerticalMenu);

            JMenuItem alignLeftMenu = new JMenuItem(bundle.getString("AlignLeft"),
                    new ImageIcon(graphicsDirectory + "AlignLeft.png"));
            alignLeftMenu.addActionListener(this);
            alignLeftMenu.setActionCommand("alignLeft");
            alignAndDistribute.add(alignLeftMenu);

            JMenuItem alignTopMenu = new JMenuItem(bundle.getString("AlignTop"),
                    new ImageIcon(graphicsDirectory + "AlignTop.png"));
            alignTopMenu.addActionListener(this);
            alignTopMenu.setActionCommand("alignTop");
            alignAndDistribute.add(alignTopMenu);

            JMenuItem centerHorizontalMenu = new JMenuItem(bundle.getString("CenterHorizontally"),
                    new ImageIcon(graphicsDirectory + "CenterHorizontal.png"));
            centerHorizontalMenu.addActionListener(this);
            centerHorizontalMenu.setActionCommand("centerHorizontal");
            alignAndDistribute.add(centerHorizontalMenu);

            JMenuItem alignBottomMenu = new JMenuItem(bundle.getString("AlignBottom"),
                    new ImageIcon(graphicsDirectory + "AlignBottom.png"));
            alignBottomMenu.addActionListener(this);
            alignBottomMenu.setActionCommand("alignBottom");
            alignAndDistribute.add(alignBottomMenu);

            alignAndDistribute.addSeparator();

            JMenuItem distributeVerticallyMenu = new JMenuItem(bundle.getString("DistributeVertically"),
                    new ImageIcon(graphicsDirectory + "DistributeVertically.png"));
            distributeVerticallyMenu.addActionListener(this);
            distributeVerticallyMenu.setActionCommand("distributeVertically");
            alignAndDistribute.add(distributeVerticallyMenu);

            JMenuItem distributeHorizontallyMenu = new JMenuItem(bundle.getString("DistributeHorizontally"),
                    new ImageIcon(graphicsDirectory + "DistributeHorizontally.png"));
            distributeHorizontallyMenu.addActionListener(this);
            distributeHorizontallyMenu.setActionCommand("distributeHorizontally");
            alignAndDistribute.add(distributeHorizontallyMenu);

            JMenuItem groupMenu = new JMenuItem(bundle.getString("GroupElements"),
                    new ImageIcon(graphicsDirectory + "GroupElements.png"));
            groupMenu.addActionListener(this);
            groupMenu.setActionCommand("groupElements");
            cartoMenu.add(groupMenu);

            JMenuItem ungroupMenu = new JMenuItem(bundle.getString("UngroupElements"),
                    new ImageIcon(graphicsDirectory + "UngroupElements.png"));
            ungroupMenu.addActionListener(this);
            ungroupMenu.setActionCommand("ungroupElements");
            cartoMenu.add(ungroupMenu);

            menubar.add(cartoMenu);


            // Tools menu
            JMenu ToolsMenu = new JMenu(bundle.getString("Tools"));

            ToolsMenu.add(rasterCalc);
            rasterCalc.setActionCommand("rasterCalculator");
            rasterCalc.addActionListener(this);

            ToolsMenu.add(paletteManager);
            paletteManager.addActionListener(this);
            paletteManager.setActionCommand("paletteManager");
            paletteManager.addActionListener(this);

            JMenuItem scripter = new JMenuItem(bundle.getString("Scripting"),
                    new ImageIcon(graphicsDirectory + "ScriptIcon2.png"));
            scripter.addActionListener(this);
            scripter.setActionCommand("scripter");
            ToolsMenu.add(scripter);

            ToolsMenu.add(modifyPixels);
            modifyPixels.addActionListener(this);
            modifyPixels.setActionCommand("modifyPixels");

            distanceToolMenuItem = new JCheckBoxMenuItem(bundle.getString("MeasureDistance"),
                    new ImageIcon(graphicsDirectory + "DistanceTool.png"));
            ToolsMenu.add(distanceToolMenuItem);
            distanceToolMenuItem.addActionListener(this);
            distanceToolMenuItem.setActionCommand("distanceTool");

            ToolsMenu.add(refreshTools);
            refreshTools.addActionListener(this);
            refreshTools.setActionCommand("refreshTools");

            JMenuItem newHelp = new JMenuItem(bundle.getString("CreateNewHelpEntry"));
            newHelp.addActionListener(this);
            newHelp.setActionCommand("newHelp");
            ToolsMenu.add(newHelp);
            menubar.add(ToolsMenu);


            ToolsMenu.addSeparator();

            JMenu editVectorMenu = new JMenu(bundle.getString("On-ScreenDigitizing"));

            editVectorMenuItem = new JCheckBoxMenuItem(bundle.getString("EditVector"),
                    new ImageIcon(graphicsDirectory + "Digitize.png"));
            editVectorMenu.add(editVectorMenuItem);
            editVectorMenuItem.addActionListener(this);
            editVectorMenuItem.setActionCommand("editVector");
            editVectorMenuItem.setEnabled(false);

            digitizeNewFeatureMenuItem = new JCheckBoxMenuItem(bundle.getString("DigitizeNewFeature"),
                    new ImageIcon(graphicsDirectory + "DigitizeNewFeature.png"));
            editVectorMenu.add(digitizeNewFeatureMenuItem);
            digitizeNewFeatureMenuItem.addActionListener(this);
            digitizeNewFeatureMenuItem.setActionCommand("digitizeNewFeature");
            digitizeNewFeatureMenuItem.setEnabled(false);

            deleteFeatureMenuItem = new JMenuItem(bundle.getString("DeleteFeature"),
                    new ImageIcon(graphicsDirectory + "DeleteFeature.png"));
            editVectorMenu.add(deleteFeatureMenuItem);
            deleteFeatureMenuItem.addActionListener(this);
            deleteFeatureMenuItem.setActionCommand("deleteFeature");
            deleteFeatureMenuItem.setEnabled(false);

            ToolsMenu.add(editVectorMenu);




            // Help menu
            JMenu HelpMenu = new JMenu(bundle.getString("Help"));
            JMenuItem helpIndex = new JMenuItem(bundle.getString("Index"),
                    new ImageIcon(graphicsDirectory + "help.png"));
            HelpMenu.add(helpIndex);
            helpIndex.setActionCommand("helpIndex");
            helpIndex.addActionListener(this);

            JMenuItem helpSearch = new JMenuItem(bundle.getString("Search"));
            helpSearch.setActionCommand("helpSearch");
            helpSearch.addActionListener(this);
            HelpMenu.add(helpSearch);

            JMenuItem helpTutorials = new JMenuItem(bundle.getString("Tutorials"));
            HelpMenu.add(helpTutorials);
            helpTutorials.setActionCommand("helpTutorials");
            helpTutorials.addActionListener(this);

            JMenuItem helpAbout = new JMenuItem(bundle.getString("About") + " Whitebox GAT");
            helpAbout.setActionCommand("helpAbout");
            helpAbout.addActionListener(this);
            HelpMenu.add(helpAbout);

//            HelpMenu.addSeparator();
//
//            JMenuItem helpReport = new JMenuItem(bundle.getString("HelpCompletenessReport"));
//            helpReport.setActionCommand("helpReport");
//            helpReport.addActionListener(this);
//            HelpMenu.add(helpReport);

            menubar.add(HelpMenu);


            this.setJMenuBar(menubar);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "WhiteboxGui.createMenu", e);
            showFeedback(e.getMessage());
        }
    }

    private void createPopupMenus() {

        // maps menu
        mapsPopup = new JPopupMenu();

        JMenuItem mi = new JMenuItem(bundle.getString("MapProperties"));
        mi.addActionListener(this);
        mi.setActionCommand("mapProperties");
        mapsPopup.add(mi);

        mi = new JMenuItem(bundle.getString("RenameMap"));
        mi.addActionListener(this);
        mi.setActionCommand("renameMap");
        mapsPopup.add(mi);

        mi = new JMenuItem(bundle.getString("SetAsActiveMap"));
        mi.addActionListener(this);
        mi.setActionCommand("setAsActiveMap");
        mapsPopup.add(mi);

        mapsPopup.addSeparator();

        mi = new JMenuItem(bundle.getString("SaveMap"),
                new ImageIcon(graphicsDirectory + "SaveMap.png"));
        mi.addActionListener(this);
        mi.setActionCommand("saveMap");
        mapsPopup.add(mi);

        mi = new JMenuItem(bundle.getString("OpenMap"),
                new ImageIcon(graphicsDirectory + "open.png"));
        mi.addActionListener(this);
        mi.setActionCommand("openMap");
        mapsPopup.add(mi);

        mi = new JMenuItem(bundle.getString("AddNewMap"),
                new ImageIcon(graphicsDirectory + "map.png"));
        mi.addActionListener(this);
        mi.setActionCommand("newMap");
        mapsPopup.add(mi);

        mi = new JMenuItem(bundle.getString("PrintMap"),
                new ImageIcon(graphicsDirectory + "Print.png"));
        mi.addActionListener(this);
        mi.setActionCommand("printMap");
        mapsPopup.add(mi);

        mi = new JMenuItem(bundle.getString("ExportMapAsImage"));
        mi.addActionListener(this);
        mi.setActionCommand("exportMapAsImage");
        mapsPopup.add(mi);

        mapsPopup.addSeparator();

        mi = new JMenuItem(bundle.getString("RefreshMap"));
        mi.addActionListener(this);
        mi.setActionCommand("refreshMap");
        mapsPopup.add(mi);

        mi = new JMenuItem(bundle.getString("ZoomToPage"),
                new ImageIcon(graphicsDirectory + "ZoomFullExtent3.png"));
        mi.addActionListener(this);
        mi.setActionCommand("zoomToPage");
        mapsPopup.add(mi);

        mapsPopup.addSeparator();

        mi = new JMenuItem(bundle.getString("CloseMap"));
        mi.addActionListener(this);
        mi.setActionCommand("closeMap");
        mapsPopup.add(mi);

        mapsPopup.setOpaque(true);
        mapsPopup.setLightWeightPopupEnabled(true);



        // map area popup menu
        mapAreaPopup = new JPopupMenu();

        mi = new JMenuItem(bundle.getString("AddLayer"),
                new ImageIcon(graphicsDirectory + "AddLayer.png"));
        mi.addActionListener(this);
        mi.setActionCommand("addLayer");
        mapAreaPopup.add(mi);

        mi = new JMenuItem(bundle.getString("RemoveLayer"),
                new ImageIcon(graphicsDirectory + "RemoveLayer.png"));
        mi.addActionListener(this);
        mi.setActionCommand("removeLayer");
        mapAreaPopup.add(mi);

        mi = new JMenuItem(bundle.getString("RemoveAllLayers"));
        mi.addActionListener(this);
        mi.setActionCommand("removeAllLayers");
        mapAreaPopup.add(mi);

        recentFilesPopupMenu.setNumItemsToStore(numberOfRecentItemsToStore);
        recentFilesPopupMenu.setText(bundle.getString("AddRecentDataLayer"));
        recentFilesPopupMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addLayer(e.getActionCommand());
            }
        });
        mapAreaPopup.add(recentFilesPopupMenu);

        mapAreaPopup.addSeparator();

        mi = new JMenuItem(bundle.getString("FitMapToPage"));
        mi.addActionListener(this);
        mi.setActionCommand("fitMapAreaToPage");
        mapAreaPopup.add(mi);

        mi = new JMenuItem(bundle.getString("FitToData"));
        mi.addActionListener(this);
        mi.setActionCommand("fitMapAreaToData");
        mapAreaPopup.add(mi);

        JCheckBoxMenuItem miCheck = new JCheckBoxMenuItem(
                bundle.getString("MaximizeScreenSize"));
        miCheck.addActionListener(this);
        miCheck.setActionCommand("maximizeMapAreaScreenSize");
        mapAreaPopup.add(miCheck);


        mi = new JMenuItem(bundle.getString("ZoomToActiveLayer"),
                new ImageIcon(graphicsDirectory + "ZoomToActiveLayer.png"));
        mi.addActionListener(this);
        mi.setActionCommand("zoomToLayer");
        mapAreaPopup.add(mi);

        mi = new JMenuItem(bundle.getString("ZoomToFullExtent"),
                new ImageIcon(graphicsDirectory + "Globe.png"));
        mi.addActionListener(this);
        mi.setActionCommand("zoomToFullExtent");
        mapAreaPopup.add(mi);

        mapAreaPopup.addSeparator();

        mi = new JMenuItem(bundle.getString("ShowAllLayers"));
        mi.addActionListener(this);
        mi.setActionCommand("allLayersVisible");
        mapAreaPopup.add(mi);

        mi = new JMenuItem(bundle.getString("HideAllLayers"));
        mi.addActionListener(this);
        mi.setActionCommand("allLayersInvisible");
        mapAreaPopup.add(mi);

        mi = new JMenuItem(bundle.getString("ToggleVisibilityOfAllLayers"));
        mi.addActionListener(this);
        mi.setActionCommand("toggleAllLayerVisibility");
        mapAreaPopup.add(mi);

        mapAreaPopup.addSeparator();

        mi = new JMenuItem(bundle.getString("ShowProperties"));
        mi.addActionListener(this);
        mi.setActionCommand("mapAreaProperties");
        mapAreaPopup.add(mi);

        mapAreaPopup.addSeparator();

        mi = new JMenuItem(bundle.getString("DeleteMapArea"));
        mi.addActionListener(this);
        mi.setActionCommand("deleteMapArea");
        mapAreaPopup.add(mi);



        // text popup menu
        textPopup = new JPopupMenu();

        mi = new JMenuItem(bundle.getString("Open"));
        mi.addActionListener(this);
        mi.setActionCommand("openText");
        textPopup.add(mi);

        mi = new JMenuItem(bundle.getString("Save"));
        mi.addActionListener(this);
        mi.setActionCommand("saveText");
        textPopup.add(mi);

        mi = new JMenuItem(bundle.getString("Close"));
        mi.addActionListener(this);
        mi.setActionCommand("closeText");
        textPopup.add(mi);

        textPopup.addSeparator();

        mi = new JMenuItem(bundle.getString("Clear"));
        mi.addActionListener(this);
        mi.setActionCommand("clearText");
        textPopup.add(mi);

        mi = new JMenuItem(bundle.getString("Cut"));
        mi.addActionListener(this);
        mi.setActionCommand("cutText");
        textPopup.add(mi);
        mi = new JMenuItem(bundle.getString("Copy"));
        mi.addActionListener(this);
        mi.setActionCommand("copyText");
        textPopup.add(mi);

        mi = new JMenuItem(bundle.getString("Paste"));
        mi.addActionListener(this);
        mi.setActionCommand("pasteText");
        textPopup.add(mi);

        mi = new JMenuItem(bundle.getString("SelectAll"));
        mi.addActionListener(this);
        mi.setActionCommand("selectAllText");
        textPopup.add(mi);

        textPopup.addSeparator();
        wordWrap = new JCheckBoxMenuItem(bundle.getString("WordWrap"));
        wordWrap.addActionListener(this);
        wordWrap.setActionCommand("wordWrap");
        wordWrap.setState(false);
        textPopup.add(wordWrap);

        textPopup.setOpaque(true);
        textPopup.setLightWeightPopupEnabled(true);

    }

    public void setCartoElementToolbarVisibility(boolean value) {
        ctb.setVisible(value);
    }

    private void createToolbar() {
        try {
            JToolBar toolbar = new JToolBar();
//            toolbar.setBackground(backgroundColour);
            JButton newMap = makeToolBarButton("map.png", "newMap",
                    bundle.getString("NewMap"), "New");

            toolbar.add(newMap);
            JButton openMap = makeToolBarButton("open.png", "openMap",
                    bundle.getString("OpenMap"), "Open");
            toolbar.add(openMap);
            JButton saveMap = makeToolBarButton("SaveMap.png", "saveMap",
                    bundle.getString("SaveMap"), "Save");
            toolbar.add(saveMap);
            JButton printMap = makeToolBarButton("Print.png", "printMap",
                    bundle.getString("PrintMap"), "Print");
            toolbar.add(printMap);
            toolbar.addSeparator();
            JButton addLayer = makeToolBarButton("AddLayer.png", "addLayer",
                    bundle.getString("AddLayer"), "Add Layer");
            toolbar.add(addLayer);
            JButton removeLayer = makeToolBarButton("RemoveLayer.png", "removeLayer",
                    bundle.getString("RemoveLayer"), "Remove Layer");
            toolbar.add(removeLayer);
            JButton raiseLayer = makeToolBarButton("PromoteLayer.png", "raiseLayer",
                    bundle.getString("RaiseLayer"), "Raise Layer");
            toolbar.add(raiseLayer);
            JButton lowerLayer = makeToolBarButton("DemoteLayer.png", "lowerLayer",
                    bundle.getString("LowerLayer"), "Lower Layer");
            toolbar.add(lowerLayer);
            JButton layerToTop = makeToolBarButton("LayerToTop.png", "layerToTop",
                    bundle.getString("LayerToTop"), "Layer To Top");
            toolbar.add(layerToTop);
            JButton layerToBottom = makeToolBarButton("LayerToBottom.png", "layerToBottom",
                    bundle.getString("LayerToBottom"), "Layer To Bottom");
            toolbar.add(layerToBottom);
            JButton attributeTable = makeToolBarButton("AttributeTable.png", "viewAttributeTable",
                    bundle.getString("ViewAttributeTable"), "View Attribute Table");
            toolbar.add(attributeTable);

            toolbar.addSeparator();
            select = makeToggleToolBarButton("select.png", "select",
                    bundle.getString("SelectMapElement"), "Select");
            toolbar.add(select);
            selectFeature = makeToggleToolBarButton("SelectFeature.png", "selectFeature",
                    bundle.getString("SelectFeature"), "Select Feature");
            toolbar.add(selectFeature);
            // Feature selection should go here.
            pan = makeToggleToolBarButton("Pan2.png", "pan",
                    bundle.getString("Pan"), "Pan");
            toolbar.add(pan);
            zoomIntoBox = makeToggleToolBarButton("ZoomIn.png", "zoomToBox",
                    bundle.getString("ZoomIn"), "Zoom");
            toolbar.add(zoomIntoBox);
            zoomOut = makeToggleToolBarButton("ZoomOut.png", "zoomOut",
                    bundle.getString("ZoomOut"), "Zoom out");
            toolbar.add(zoomOut);
            JButton zoomToFullExtent = makeToolBarButton("Globe.png", "zoomToFullExtent",
                    bundle.getString("ZoomToFullExtent"), "Zoom To Full Extent");
            toolbar.add(zoomToFullExtent);
            JButton zoomToPage = makeToolBarButton("ZoomFullExtent3.png", "zoomToPage",
                    bundle.getString("ZoomToPage"), "Zoom To Page");
            toolbar.add(zoomToPage);

            ButtonGroup viewButtonGroup = new ButtonGroup();
            viewButtonGroup.add(select);
            viewButtonGroup.add(selectFeature);
            viewButtonGroup.add(pan);
            viewButtonGroup.add(zoomOut);
            viewButtonGroup.add(zoomIntoBox);
            select.setSelected(true);

            JButton previousExtent = makeToolBarButton("back.png", "previousExtent",
                    bundle.getString("PreviousExtent"), "Prev Extent");
            toolbar.add(previousExtent);
            JButton nextExtent = makeToolBarButton("forward.png", "nextExtent",
                    bundle.getString("NextExtent"), "Next Extent");
            nextExtent.setActionCommand("nextExtent");
            toolbar.add(nextExtent);
            toolbar.addSeparator();
            JButton rasterCalculator = makeToolBarButton("RasterCalculator.png", "rasterCalculator",
                    bundle.getString("RasterCalculator"), "Raster Calc");
            toolbar.add(rasterCalculator);
            JButton paletteManager = makeToolBarButton("paletteManager.png", "paletteManager",
                    bundle.getString("PaletteManager"), "Palette Manager");
            toolbar.add(paletteManager);
            JButton scripter = makeToolBarButton("ScriptIcon2.png", "scripter",
                    bundle.getString("Scripting"), "Scripter");
            toolbar.add(scripter);
            modifyPixelVals = makeToggleToolBarButton("ModifyPixels.png", "modifyPixels",
                    bundle.getString("ModifyPixelValues"), "Modify Pixels");
            toolbar.add(modifyPixelVals);
            distanceToolButton = makeToggleToolBarButton("DistanceTool.png", "distanceTool",
                    bundle.getString("MeasureDistance"), "Distance Tool");
            toolbar.add(distanceToolButton);

            toolbar.addSeparator();

            editVectorButton = makeToggleToolBarButton("Digitize.png", "editVector",
                    bundle.getString("EditVector"), "Edit Vector");
            editVectorButton.setEnabled(false);
            toolbar.add(editVectorButton);

//            ButtonGroup toolsButtonGroup = new ButtonGroup();
//            toolsButtonGroup.add(modifyPixelVals);
//            toolsButtonGroup.add(distanceToolButton);
//            toolsButtonGroup.add(editVectorButton);

            digitizeNewFeatureButton = makeToggleToolBarButton("DigitizeNewFeature.png", "digitizeNewFeature",
                    bundle.getString("DigitizeNewFeature"), "Digitize New Feature");
            digitizeNewFeatureButton.setVisible(false);
            toolbar.add(digitizeNewFeatureButton);

            deleteFeatureButton = makeToolBarButton("DeleteFeature.png", "deleteFeature",
                    bundle.getString("DeleteFeature"), "Delete Feature");
            deleteFeatureButton.setVisible(false);
            toolbar.add(deleteFeatureButton);

//            moveNodesButton = makeToolBarButton("MoveNodes.png", "moveNodes", "Move Feature Nodes", "Move Feature Nodes");
//            moveNodesButton.setVisible(false);
//            toolbar.add(moveNodesButton);

            toolbar.addSeparator();
            JButton help = makeToolBarButton("Help.png", "helpIndex",
                    bundle.getString("Help"), "Help");
            toolbar.add(help);

            toolbar.addSeparator();
            toolbar.add(Box.createHorizontalGlue());
            JPanel scalePanel = new JPanel();
            scalePanel.setLayout(new BoxLayout(scalePanel, BoxLayout.X_AXIS));
            JLabel scaleLabel = new JLabel("1:");
            scalePanel.add(scaleLabel);
            scalePanel.add(scaleText);
            scalePanel.setOpaque(false);
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
            logger.log(Level.SEVERE, "WhiteboxGui.createToolbar", e);
            showFeedback(e.toString());
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
            logger.log(Level.WARNING, "WhiteboxGui.makeToolbarButton", e);
        }

        return button;
    }

    private JToggleButton makeToggleToolBarButton(String imageName, String actionCommand, String toolTipText, String altText) {
        //Look for the image.
        String imgLocation = graphicsDirectory + imageName;
        ImageIcon image = new ImageIcon(imgLocation, "");

        //Create and initialize the button.
        JToggleButton button = new JToggleButton();
        button.setActionCommand(actionCommand);
        button.setToolTipText(toolTipText);
        button.addActionListener(this);
        button.setOpaque(false);
        try {
            button.setIcon(image);
        } catch (Exception e) {
            showFeedback(e.getMessage());
            logger.log(Level.WARNING, "WhiteboxGui.makeToggleToolbarButton", e);
        }

        return button;
    }
    private JPanel layersPanel;
    private FeatureSelectionPanel featuresPanel;

    private JTabbedPane createTabbedPane() {
        try {
            JSplitPane wbTools = getToolbox();
            tabs.insertTab(bundle.getString("Tools"), null, wbTools, "", 0);
            layersPanel = new JPanel(new BorderLayout());
            layersPanel.setBackground(Color.white);
            updateLayersTab();
            tabs.insertTab(bundle.getString("Layers"), null, layersPanel, "", 1);
            featuresPanel = new FeatureSelectionPanel(bundle);
            tabs.insertTab(bundle.getString("Features"), null, featuresPanel, "", 2);

            return tabs;
        } catch (Exception e) {
            showFeedback(e.toString());
            logger.log(Level.SEVERE, "WhiteboxGui.createTabbedPane", e);
            return null;
        }

    }
    ArrayList<LegendEntryPanel> legendEntries = new ArrayList<>();
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
                                lep.setSelected(true);
                                if (openMaps.get(activeMap).getActiveMapArea().getActiveLayer() instanceof VectorLayerInfo) {
                                    VectorLayerInfo vli = (VectorLayerInfo) openMaps.get(activeMap).getActiveMapArea().getActiveLayer();
                                    if (vli.isActivelyEdited()) {
                                        editVectorButton.setEnabled(true);
                                        editVectorMenuItem.setState(true);
                                        digitizeNewFeatureButton.setVisible(true);
//                                        moveNodesButton.setVisible(true);
                                        deleteFeatureButton.setVisible(true);
                                        digitizeNewFeatureMenuItem.setEnabled(true);
                                        deleteFeatureMenuItem.setEnabled(true);

                                    } else {
                                        editVectorButton.setEnabled(true);
                                        editVectorMenuItem.setEnabled(true);
                                        editVectorMenuItem.setState(false);
                                        digitizeNewFeatureButton.setVisible(false);
                                        drawingArea.setDigitizingNewFeature(false);
//                                        moveNodesButton.setVisible(false);
                                        deleteFeatureButton.setVisible(false);
                                        digitizeNewFeatureMenuItem.setState(false);
                                        digitizeNewFeatureMenuItem.setEnabled(false);
                                        deleteFeatureMenuItem.setEnabled(false);
                                    }
                                } else {
                                    editVectorButton.setEnabled(false);
                                    editVectorMenuItem.setEnabled(false);
                                    editVectorMenuItem.setState(false);
                                    digitizeNewFeatureButton.setVisible(false);
                                    drawingArea.setDigitizingNewFeature(false);
//                                        moveNodesButton.setVisible(false);
                                    deleteFeatureButton.setVisible(false);
                                    digitizeNewFeatureMenuItem.setState(false);
                                    digitizeNewFeatureMenuItem.setEnabled(false);
                                    deleteFeatureMenuItem.setEnabled(false);
                                }
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
            System.out.println(e.toString());
            logger.log(Level.WARNING, "WhiteboxGui.updateLayersTab", e);
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
                    LayersPopupMenu layersPopup = new LayersPopupMenu(activeMapArea.getLayer(layerNum),
                            this, graphicsDirectory, bundle);
                    layersPopup.show((JComponent) e.getSource(), e.getX(), e.getY());
                }
            }
        } else { //if (e.getClickCount() == 1) {
            openMaps.get(activeMap).deslectAllCartographicElements();
            if (layerNum == -1) {
                setAsActiveMap();

            } else {
                setAsActiveLayer();
            }
            refreshMap(true);
        } //else if (e.getClickCount() == 1) {
//            updateLayersTab();
//        }
    }

    private JSplitPane getToolbox() {
        try {
            // create the tool treeview
            File file = new File(toolboxFile);

            if (!file.exists()) {
                showFeedback(messages.getString("MissingToolbox"));
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
                            } else if (n.toString().equals(bundle.getString("topmost"))) {
                                // set the message indicating the number of plugins that were located.
                                status.setMessage(" " + plugInfo.size() + " " + messages.getString("PluginsWereLocated"));
                            }
                        } else if (e.getClickCount() == 2) {
                            DefaultMutableTreeNode n = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                            if (n.getChildCount() == 0) {
                                label = selPath.getLastPathComponent().toString();
                                launchDialog(label);
                            }
                        }
                    }
                }
            };
            tree.addMouseListener(ml);

            HashMap icons = new HashMap();
            icons.put("toolbox", new ImageIcon(graphicsDirectory + "opentools.png", ""));
            icons.put("tool", new ImageIcon(graphicsDirectory + "tool.png", ""));
            icons.put("script", new ImageIcon(graphicsDirectory + "ScriptIcon2.png", ""));

//            ImageIcon leafIcon = new ImageIcon(graphicsDirectory + "tool.png", "");
//            ImageIcon leafIconScript = new ImageIcon(graphicsDirectory + "ScriptIcon2.png", "");
//            ImageIcon stemIcon = new ImageIcon(graphicsDirectory + "opentools.png", "");
//            DefaultTreeCellRenderer renderer =
//                    new DefaultTreeCellRenderer();
//            renderer.setLeafIcon(leafIcon);
//            renderer.setClosedIcon(stemIcon);
//            renderer.setOpenIcon(stemIcon);
            tree.putClientProperty("JTree.icons", icons);
            tree.setCellRenderer(new TreeNodeRenderer()); //renderer);

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
            box.add(new JLabel(bundle.getString("Search") + ":"));
            box.add(Box.createHorizontalStrut(3));
            searchText.setMaximumSize(new Dimension(275, 22));
            box.setMaximumSize(new Dimension(275, 24));
            searchText.addActionListener(searchFieldListener);
            box.add(searchText);
            allToolsPanel.add(box);
            allToolsPanel.add(scroller1);

            qlTabs.insertTab(bundle.getString("All"), null, allToolsPanel, "", 0); // + plugInfo.size() + " tools", null, scroller1, "", 2);
            qlTabs.insertTab(bundle.getString("Most_Used"), null, scroller3, "", 1);
            qlTabs.insertTab(bundle.getString("Recent"), null, scroller2, "", 2);

            //qlTabs.setPreferredSize(new Dimension(200, splitterToolboxLoc));

            qlTabs.setSelectedIndex(qlTabsIndex);

            splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeView, qlTabs);
            splitPane2.setResizeWeight(1);
            splitPane2.setDividerLocation(0.75); //splitterToolboxLoc);
            splitPane2.setOneTouchExpandable(true);

            return splitPane2;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.log(Level.SEVERE, "WhiteboxGui.getToolbox", e);
            showFeedback(e.toString());
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
        try {
            DefaultListModel model = new DefaultListModel();
            String searchString = searchText.getText().toLowerCase();
            String descriptiveName, shortName, description;

            if (searchString == null || searchString.equals("")) {

                for (int i = 0; i < plugInfo.size(); i++) {
                    plugInfo.get(i).setSortMode(PluginInfo.SORT_MODE_NAMES);
                }
                Collections.sort(plugInfo);
                for (int i = 0; i < plugInfo.size(); i++) {
                    model.add(i, plugInfo.get(i).getDescriptiveName());
                }

            } else {

                // find quotations
                ArrayList<String> quotedStrings = new ArrayList<>();
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
//                    WhiteboxPlugin plug = pluginService.getPlugin(plugInfo.get(i).getDescriptiveName(), StandardPluginService.DESCRIPTIVE_NAME);
//                    description = plug.getToolDescription().toLowerCase().replace("-", " ");
                    description = plugInfo.get(i).getDescription();
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
        } catch (Exception e) {
            logger.log(Level.SEVERE, "WhiteboxGui.searchForWords", e);
        }
    }

    private DefaultMutableTreeNode populateTree(Node n) {
        Element e = (Element) n;
        String label = e.getAttribute("label");
        String toolboxName = e.getAttribute("name");
        //DefaultMutableTreeNode result;
        IconTreeNode result;
        if (bundle.containsKey(toolboxName)) {
            result = new IconTreeNode(bundle.getString(toolboxName));
            //result = new DefaultMutableTreeNode(bundle.getString(toolboxName));
        } else {
            result = new IconTreeNode(label);
            //result = new DefaultMutableTreeNode(label);
        }
        result.setIconName("toolbox");

        NodeList nodeList = n.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) nodeList.item(i);
                label = childElement.getAttribute("label");
                toolboxName = childElement.getAttribute("name");
                //DefaultMutableTreeNode childTreeNode;
                IconTreeNode childTreeNode;
                if (bundle.containsKey(toolboxName)) {
                    //childTreeNode = new DefaultMutableTreeNode(bundle.getString(toolboxName));
                    childTreeNode = new IconTreeNode(bundle.getString(toolboxName));
                } else {
                    childTreeNode = new IconTreeNode(label);
                    //childTreeNode = new DefaultMutableTreeNode(label);
                }
                childTreeNode.setIconName("toolbox");

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
                        String[] toolDetails = t.get(k).split(";");
                        IconTreeNode childTreeNode2 = new IconTreeNode(toolDetails[0]);
                        if (toolDetails[1].equals("false")) {
                            childTreeNode2.setIconName("tool");
                        } else {
                            childTreeNode2.setIconName("script");
                        }
//                        IconTreeNode childTreeNode2 = new IconTreeNode(t.get(k));
//                        childTreeNode2.setIconName("tool");
                        childTreeNode.add(childTreeNode2);
                        //childTreeNode.add(new DefaultMutableTreeNode(t.get(k)));
                    }
                } else if (nodeList.item(i).getFirstChild() == null) {
                    IconTreeNode childTreeNode2 = new IconTreeNode("No tools");
                    childTreeNode2.setIconName("tool");
                    childTreeNode.add(childTreeNode2);
                    //childTreeNode.add(new DefaultMutableTreeNode("No tools"));
                }

                result.add(childTreeNode);
            }
        }
        if (nodeList.getLength() == 0) {
            ArrayList<String> t = findToolsInToolbox(toolboxName);
            if (t.size() > 0) {
                for (int k = 0; k < t.size(); k++) {
                    String[] toolDetails = t.get(k).split(";");
                    IconTreeNode childTreeNode2 = new IconTreeNode(toolDetails[0]);
                    if (toolDetails[1].equals("false")) {
                        childTreeNode2.setIconName("tool");
                    } else {
                        childTreeNode2.setIconName("script");
                    }
                    result.add(childTreeNode2);
                    //result.add(new DefaultMutableTreeNode(t.get(k)));
                }
            } else {
                IconTreeNode childTreeNode2 = new IconTreeNode("No tools");
                childTreeNode2.setIconName("tool");
                result.add(childTreeNode2);
                //result.add(new DefaultMutableTreeNode("No tools"));
            }
        }

        return result;

    }

    private ArrayList<String> findToolsInToolbox(String toolbox) {
        Iterator<WhiteboxPlugin> iterator = pluginService.getPlugins();
        ArrayList<String> plugs = new ArrayList<>();
        String plugName;
        String plugDescriptiveName;
//        while (iterator.hasNext()) {
//            WhiteboxPlugin plugin = iterator.next();
//            String[] tbox = plugin.getToolbox();
//            for (int i = 0; i < tbox.length; i++) {
//                if (tbox[i].equals(toolbox)) {
//                    plugName = plugin.getName();
//                    if (pluginBundle.containsKey(plugName)) {
//                        plugDescriptiveName = pluginBundle.getString(plugName);
//                    } else {
//                        plugDescriptiveName = plugin.getDescriptiveName();
//                    }
//                    plugs.add(plugDescriptiveName);
//                }
//            }
//
//        }
        for (PluginInfo pi : plugInfo) {
            String[] tbox = pi.getToolboxes();
            for (int i = 0; i < tbox.length; i++) {
                if (tbox[i].equals(toolbox)) {
                    plugName = pi.getName();
                    if (pluginBundle.containsKey(plugName)) {
                        plugDescriptiveName = pluginBundle.getString(plugName);
                    } else {
                        plugDescriptiveName = pi.getDescriptiveName();
                    }
                    plugs.add(plugDescriptiveName + ";" + Boolean.toString(pi.isScript()));
                }
            }
        }
        Collections.sort(plugs, new SortIgnoreCase());
        return plugs;
    }

    @Override
    public ResourceBundle getGuiLabelsBundle() {
        return bundle;
    }

    @Override
    public ResourceBundle getMessageBundle() {
        return messages;
    }

    @Override
    public String getLanguageCountryCode() {
        return language + "_" + country;
    }

    @Override
    public void setLanguageCountryCode(String code) {
        String[] str = code.split("_");
        if (str.length != 2) {
            showFeedback("Language-Country code improperly formated");
            return;
        }
        language = str[0];
        country = str[1];

        //currentLocale = new Locale(language, country);
        WhiteboxInternationalizationTools.setLocale(language, country);
        bundle = WhiteboxInternationalizationTools.getGuiLabelsBundle(); //ResourceBundle.getBundle("whiteboxgis.i18n.GuiLabelsBundle", currentLocale);
        messages = WhiteboxInternationalizationTools.getMessagesBundle(); //ResourceBundle.getBundle("whiteboxgis.i18n.messages", currentLocale);
    }

    public boolean isCheckForUpdates() {
        return checkForUpdates;
    }

    public void setCheckForUpdates(boolean checkForUpdates) {
        this.checkForUpdates = checkForUpdates;
    }

    public boolean isReceiveAnnouncements() {
        return receiveAnnouncements;
    }

    public void setReceiveAnnouncements(boolean receiveAnnouncements) {
        this.receiveAnnouncements = receiveAnnouncements;
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
            mapAreaNum = openMaps.get(activeMap).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            if (openMaps.isEmpty()) {
                // create a new map to overlay the layer onto.
                numOpenMaps = 1;
                MapInfo mapinfo = new MapInfo("Map1");
                mapinfo.setMapName("Map1");
                mapinfo.setMargin(defaultMapMargin);
                MapArea ma = new MapArea("MapArea1");
                ma.setUpperLeftX(-32768);
                ma.setUpperLeftY(-32768);
                ma.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
                mapinfo.addNewCartographicElement(ma);
                openMaps.add(mapinfo);
                drawingArea.setMapInfo(openMaps.get(0));
                activeMap = 0;
                mapAreaNum = openMaps.get(activeMap).getActiveMapAreaElementNumber();
            } else {
                MapArea ma = new MapArea("MapArea1");
                ma.setUpperLeftX(-32768);
                ma.setUpperLeftY(-32768);
                ma.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
                openMaps.get(activeMap).addNewCartographicElement(ma);
                mapAreaNum = openMaps.get(activeMap).getActiveMapAreaElementNumber();
            }
        }

        // set the filter.
        ArrayList<ExtensionFileFilter> filters = new ArrayList<>();
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
                mapinfo.setMargin(defaultMapMargin);
                MapArea ma = new MapArea("MapArea1");
                ma.setUpperLeftX(-32768);
                ma.setUpperLeftY(-32768);
                ma.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
                mapinfo.addNewCartographicElement(ma);
                openMaps.add(mapinfo);
                drawingArea.setMapInfo(openMaps.get(0));
                activeMap = 0;
            }
            files = fc.getSelectedFiles();
            String fileDirectory = files[0].getParentFile() + pathSep;
            if (!fileDirectory.equals(workingDirectory)) {
                setWorkingDirectory(fileDirectory);
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
                recentFilesMenu.addMenuItem(files[i].toString());
                recentFilesPopupMenu.addMenuItem(files[i].toString());
            }
            if (files.length > 1) {
                // zoom to full extent
                BoundingBox db = activeMapArea.getFullExtent();
                activeMapArea.setCurrentExtent(db.clone());
            }
            activeMapArea.setActiveLayer(activeMapArea.getNumLayers() - 1);
        }
        refreshMap(true);
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;

    }

    private void addLayer(String fileName) {

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
            mapAreaNum = openMaps.get(activeMap).getActiveMapAreaElementNumber();
        }

        if (openMaps.isEmpty()) {
            // create a new map to overlay the layer onto.
            numOpenMaps = 1;
            MapInfo mapinfo = new MapInfo("Map1");
            mapinfo.setMargin(defaultMapMargin);
            mapinfo.setMapName("Map1");
            MapArea ma = new MapArea("MapArea1");
            ma.setUpperLeftX(-32768);
            ma.setUpperLeftY(-32768);
            ma.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
            mapinfo.addNewCartographicElement(ma);
            openMaps.add(mapinfo);
            drawingArea.setMapInfo(openMaps.get(0));
            activeMap = 0;
            mapAreaNum = openMaps.get(activeMap).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            MapArea ma = new MapArea("MapArea1");
            ma.setUpperLeftX(-32768);
            ma.setUpperLeftY(-32768);
            ma.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
            openMaps.get(activeMap).addNewCartographicElement(ma);
            mapAreaNum = openMaps.get(activeMap).getActiveMapAreaElementNumber();
        }
        File file = new File(fileName);
        if (!file.exists()) {
            showFeedback(messages.getString("NoDataLayer"));
            return;
        }
        String fileDirectory = file.getParentFile() + pathSep;
        if (!fileDirectory.equals(workingDirectory)) {
            setWorkingDirectory(fileDirectory);
        }
        String[] defaultPalettes = {defaultQuantPalette, defaultQualPalette, "rgb.pal"};
        MapArea activeMapArea = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        // get the file extension.
        if (file.toString().toLowerCase().contains(".dep")) {
            RasterLayerInfo newLayer = new RasterLayerInfo(file.toString(), paletteDirectory,
                    defaultPalettes, 255, activeMapArea.getNumLayers());
            activeMapArea.addLayer(newLayer);
            newLayer.setOverlayNumber(activeMapArea.getNumLayers() - 1);
        } else if (file.toString().toLowerCase().contains(".shp")) {
            VectorLayerInfo newLayer = new VectorLayerInfo(file.toString(), paletteDirectory,
                    255, activeMapArea.getNumLayers());
            activeMapArea.addLayer(newLayer);
            newLayer.setOverlayNumber(activeMapArea.getNumLayers() - 1);
        }

        activeMapArea.setActiveLayer(activeMapArea.getNumLayers() - 1);

        recentFilesMenu.addMenuItem(fileName);
        recentFilesPopupMenu.addMenuItem(fileName);

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
            if (ma == null) {
                return;
            }
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

    private void newMap(String mapName) {
        numOpenMaps++;
        MapInfo mapinfo = new MapInfo(mapName);
        mapinfo.setMapName(mapName);
        mapinfo.setWorkingDirectory(workingDirectory);
        mapinfo.setPageFormat(defaultPageFormat);
        mapinfo.setDefaultFont(defaultFont);
        mapinfo.setMargin(defaultMapMargin);

        MapArea ma = new MapArea(bundle.getString("MapArea").replace(" ", "") + "1");
        ma.setUpperLeftX(-32768);
        ma.setUpperLeftY(-32768);
        ma.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
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

    private void newMap() {

        String str = JOptionPane.showInputDialog(messages.getString("NewMapName")
                + ": ", bundle.getString("Map") + (numOpenMaps + 1));

        if (str != null) {
            numOpenMaps++;
            MapInfo mapinfo = new MapInfo(str);
            mapinfo.setMapName(str);
            mapinfo.setWorkingDirectory(workingDirectory);
            mapinfo.setPageFormat(defaultPageFormat);
            mapinfo.setDefaultFont(defaultFont);
            mapinfo.setMargin(defaultMapMargin);

            MapArea ma = new MapArea(bundle.getString("MapArea").replace(" ", "") + "1");
            ma.setUpperLeftX(-32768);
            ma.setUpperLeftY(-32768);
            ma.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
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
                    drawingArea.setMapInfo(new MapInfo(bundle.getString("Map")));
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
                    drawingArea.setMapInfo(new MapInfo(bundle.getString("Map")));
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
                showFeedback(messages.getString("PrintingError") + ex);
                logger.log(Level.SEVERE, "WhiteboxGui.printMap", ex);
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

            // Any CartographicElementGroups in the map will need to be ungrouped.
            int howManyGroups = openMaps.get(selectedMapAndLayer[0]).numberOfElementGroups();
            if (howManyGroups > 0) {
                showFeedback(messages.getString("NoGroupsInSavedMap"));
                openMaps.get(selectedMapAndLayer[0]).ungroupAllElements();
            }

            File file = new File(openMaps.get(selectedMapAndLayer[0]).getFileName());

            if (file.exists()) {
                file.delete();
            }

            recentMapsMenu.addMenuItem(openMaps.get(selectedMapAndLayer[0]).getFileName());

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
                logger.log(Level.SEVERE, "WhiteboxGui.saveMap", e);
                //System.err.println("Error: " + e.getMessage());
            } catch (Exception e) { //Catch exception if any
                logger.log(Level.SEVERE, "WhiteboxGui.saveMap", e);
                //System.err.println("Error: " + e.getMessage());
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
//        ArrayList<ExtensionFileFilter> filters = new ArrayList<ExtensionFileFilter>();
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
                setWorkingDirectory(fileDirectory);
            }

            // see if the file exists already, and if so, should it be overwritten?
            if (file.exists()) {
                int n = showFeedback(messages.getString("FileExists"), JOptionPane.YES_NO_OPTION,
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
                setWorkingDirectory(fileDirectory);
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

                    openMaps.add(map);
                } catch (Exception e) {
                    showFeedback(messages.getString("MapFile")
                            + files[i].toString() + " "
                            + messages.getString("NotReadProperly"));
                    logger.log(Level.SEVERE, "WhiteboxGui.openMap", e);
                    return;
                }

                recentMapsMenu.addMenuItem(files[i].toString());
            }

            activeMap = openMaps.size() - 1;
            drawingArea.setMapInfo(openMaps.get(activeMap));
            drawingArea.repaint();
            updateLayersTab();
            selectedMapAndLayer[0] = -1;
            selectedMapAndLayer[1] = -1;
            selectedMapAndLayer[2] = -1;
            numOpenMaps++;
        }
    }

    private void openMap(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            showFeedback(messages.getString("NoMapFile"));
            return;
        }

        String fileDirectory = file.getParentFile() + pathSep;
        if (!fileDirectory.equals(workingDirectory)) {
            setWorkingDirectory(fileDirectory);
        }

        try {
            // first read the text from the file into a string
            String mapTextData = whitebox.utilities.FileUtilities.readFileAsString(fileName);

            // now use gson to create a new MapInfo object by deserialization
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setPrettyPrinting();
            gsonBuilder.registerTypeAdapter(MapInfo.class,
                    new MapInfoDeserializer(workingDirectory, paletteDirectory));
            Gson gson = gsonBuilder.create();

            MapInfo map = gson.fromJson(mapTextData, MapInfo.class);

            openMaps.add(map);
        } catch (IOException | JsonSyntaxException e) {
            showFeedback(messages.getString("MapFile") + " "
                    + fileName + " " + messages.getString("NotReadProperly"));
            logger.log(Level.SEVERE, "WhiteboxGui.openMap", e);
            return;
        }

        recentMapsMenu.addMenuItem(fileName);

        activeMap = openMaps.size() - 1;
        drawingArea.setMapInfo(openMaps.get(activeMap));
        drawingArea.repaint();
        updateLayersTab();
        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;
        numOpenMaps++;
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
            showFeedback(messages.getString("CurrentPrintResolution") + " "
                    + printResolution + " dpi.\n"
                    + messages.getString("ChangePrintResolution"));
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
        ArrayList<ExtensionFileFilter> filters = new ArrayList<>();
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
                setWorkingDirectory(fileDirectory);
            }

            // see if the file exists already, and if so, should it be overwritten?
            if (file.exists()) {
                int n = showFeedback(messages.getString("FileExists") + "\n"
                        + messages.getString("Overwrite"), JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (n == JOptionPane.YES_OPTION) {
                    file.delete();
                } else if (n == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            if (!drawingArea.saveToImage(file.toString())) {
                showFeedback(messages.getString("ErrorWhileSavingMap"));
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
            k = openMaps.get(j).getActiveMapAreaElementNumber();
        }
        MapArea ma = openMaps.get(j).getMapAreaByElementNum(k);
        if (ma == null) {
            return;
        }
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
            k = openMaps.get(j).getActiveMapAreaElementNumber();
        }
        MapArea ma = openMaps.get(j).getMapAreaByElementNum(k);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
            if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
                showFeedback(messages.getString("NoMapAreas"));
                return;
            }
            layerNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
            if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
                showFeedback(messages.getString("NoMapAreas"));
                return;
            }
            layerNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
            if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
                showFeedback(messages.getString("NoMapAreas"));
                return;
            }
            layerNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }

        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
            if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
                showFeedback(messages.getString("NoMapAreas"));
                return;
            }
            layerNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
            if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
                showFeedback(messages.getString("NoMapAreas"));
                return;
            }
            layerNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
                    PaletteManager pm = new PaletteManager(paletteDirectory,
                            bundle);
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
            if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
                showFeedback(messages.getString("NoMapAreas"));
                return;
            }
            layerOverlayNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
            if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
                showFeedback(messages.getString("NoMapAreas"));
                return;
            }
            layerOverlayNum = openMaps.get(mapNum).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(activeMap).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
        }
        if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
            showFeedback(messages.getString("NoMapAreas"));
            return;
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
            mapAreaNum = openMaps.get(activeMap).getActiveMapAreaElementNumber();
            if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
                showFeedback(messages.getString("NoMapAreas"));
                return;
            }
            layerOverlayNum = openMaps.get(activeMap).getActiveMapArea().getActiveLayerOverlayNumber();
            if (layerOverlayNum < 0) {
                showFeedback(messages.getString("NoVectorLayers"));
                return;
            }
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
        if (ma.getNumLayers() == 0) {
            showFeedback(messages.getString("NoVectorLayers"));
            return;
        }
        if (ma.getLayer(layerOverlayNum).getLayerType() == MapLayerType.VECTOR) {
            VectorLayerInfo vli = (VectorLayerInfo) ma.getLayer(layerOverlayNum);
            String shapeFileName = vli.getFileName();
            AttributesFileViewer afv = new AttributesFileViewer(this, false, shapeFileName);
            int height = 500;
            afv.setSize((int) (height * 1.61803399), height); // golden ratio.
            afv.setVisible(true);
        } else {
            showFeedback(messages.getString("FunctionForVectorsOnly"));
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
                setWorkingDirectory(fileDirectory);
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
                logger.log(Level.SEVERE, "WhiteboxGui.openText", e);
                //System.out.println("Error: " + e.getMessage());
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
            logger.log(Level.SEVERE, "WhiteboxGui.saveText", e);
            //System.err.println("Error: " + e.getMessage());
        } catch (Exception e) { //Catch exception if any
            logger.log(Level.SEVERE, "WhiteboxGui.saveText", e);
            //System.err.println("Error: " + e.getMessage());
        } finally {
            if (out != null || bw != null) {
                out.flush();
                out.close();
            }
        }
    }

    private void showAboutDialog() {
        AboutWhitebox about = new AboutWhitebox(this, true, graphicsDirectory,
                versionName, versionNumber);
    }

    private void callSplashScreen() {
        String splashFile = graphicsDirectory + "WhiteboxLogo.png"; //"SplashScreen.png";
        SplashWindow sw = new SplashWindow(splashFile, 2000, versionName);
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
        if (drawingArea.isModifyingPixels()) { // is true; unset
            drawingArea.setModifyingPixels(false);
            modifyPixels.setState(false);
        } else {
            if (openMaps.get(activeMap).getActiveMapArea().getNumRasterLayers() > 0) {
                drawingArea.setModifyingPixels(true);
                modifyPixels.setState(true);
                // you can't modify pixels and measure distances
                drawingArea.setUsingDistanceTool(false);
                distanceToolMenuItem.setState(false);
            } else {
                showFeedback(messages.getString("NoRaster"));
            }
        }
    }

    private void distanceTool() {
        if (drawingArea.isUsingDistanceTool()) { // is true; unset
            drawingArea.setUsingDistanceTool(false);
            distanceToolMenuItem.setState(false);
        } else {
            if (openMaps.get(activeMap).getActiveMapArea().getNumLayers() > 0) {
                drawingArea.setUsingDistanceTool(true);
                distanceToolMenuItem.setState(true);
                // you can't modify pixels and measure distances
                drawingArea.setModifyingPixels(false);
                modifyPixels.setState(false);
            } else {
                showFeedback(messages.getString("NoLayers"));
            }
        }

    }

    @Override
    public void editVector() {
        int mapNum, layerOverlayNum, mapAreaNum;
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
        if (ma == null) {
            return;
        }
        MapLayer layer = ma.getLayer(layerOverlayNum);
        if (layer.getLayerType() == MapLayerType.VECTOR) {
            VectorLayerInfo vli = (VectorLayerInfo) layer;
            if (vli.isActivelyEdited()) {
                vli.setActivelyEdited(false);
            } else {
                if (!editVectorButton.isSelected()) {
                    editVectorButton.setSelected(true);
                }
                if (!editVectorMenuItem.getState()) {
                    editVectorMenuItem.setState(true);
                }
                vli.setActivelyEdited(true);

                drawingArea.setModifyingPixels(false);
                modifyPixels.setState(false);
                drawingArea.setUsingDistanceTool(false);
                distanceToolMenuItem.setState(false);

                // make sure this is the active layer.
                ma.setActiveLayer(layerOverlayNum);
            }
            refreshMap(true);
        } else {
            showFeedback(messages.getString("ActiveLayerNotVector"));
        }
    }
    boolean currentlyDigitizingNewFeature = false;

    public void digitizeNewFeature() {
        int mapNum, layerOverlayNum, mapAreaNum;
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
        if (ma == null) {
            return;
        }
        MapLayer layer = ma.getLayer(layerOverlayNum);
        if (layer.getLayerType() == MapLayerType.VECTOR) {
            VectorLayerInfo vli = (VectorLayerInfo) layer;
            if (currentlyDigitizingNewFeature) {
                digitizeNewFeatureButton.setSelected(false);
                digitizeNewFeatureMenuItem.setState(false);
                drawingArea.setDigitizingNewFeature(false);
                currentlyDigitizingNewFeature = false;
            } else {
                currentlyDigitizingNewFeature = true;
                digitizeNewFeatureMenuItem.setState(true);
                drawingArea.setDigitizingNewFeature(true);
                vli.getShapefile().refreshAttributeTable();
                ShapefileDatabaseRecordEntry dataRecordEntry = new ShapefileDatabaseRecordEntry(this, true, vli.getShapefile());
                dataRecordEntry.setSize(400, 300);
                dataRecordEntry.setLocation(100, 100);
                dataRecordEntry.setVisible(true);
                Object[] recData = dataRecordEntry.getValue();
                if (recData == null) {
                    digitizeNewFeatureButton.setSelected(false);
                    digitizeNewFeatureMenuItem.setState(false);
                    drawingArea.setDigitizingNewFeature(false);
                    currentlyDigitizingNewFeature = false;
                    return;
                }
                if (vli.getShapeType().getDimension() == ShapeTypeDimension.M) {
                    vli.setMValue(dataRecordEntry.getMValue());
                }
                if (vli.getShapeType().getDimension() == ShapeTypeDimension.Z) {
                    vli.setMValue(dataRecordEntry.getZValue());
                    vli.setMValue(dataRecordEntry.getMValue());
                }
                vli.openNewFeature(recData);
            }

            if (!vli.isActivelyEdited()) {
                digitizeNewFeatureButton.setSelected(false);
                digitizeNewFeatureMenuItem.setState(false);
                drawingArea.setDigitizingNewFeature(false);
            }
        } else {
            showFeedback(messages.getString("ActiveLayerNotVector"));
        }
    }

    /**
     * Used to delete a selected vector feature that is actively being edited.
     */
    @Override
    public void deleteFeature() {
        try {
            MapLayer layer = openMaps.get(activeMap).getActiveMapArea().getActiveLayer();
            if (layer instanceof VectorLayerInfo) {
                VectorLayerInfo vli = (VectorLayerInfo) layer;
                // which feature is selected?
                if (!vli.isActivelyEdited()) {
                    showFeedback(messages.getString("NotEditingVector") + " \n"
                            + messages.getString("SelectEditVector"));
                    return;
                }
                int selectedFeature = vli.getSelectedFeatureNumber();
                if (selectedFeature < 0) {
                    showFeedback(messages.getString("NoFeaturesSelected"));
                    return;
                } else {
                    int n = showFeedback(messages.getString("DeleteFeature") + " "
                            + selectedFeature + "?", JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                    if (n == JOptionPane.YES_OPTION) {
                        vli.getShapefile().deleteRecord(selectedFeature);
                        vli.reloadShapefile();
                        refreshMap(false);
                    } else if (n == JOptionPane.NO_OPTION) {
                        return;
                    }
                }

            } else {
                showFeedback(messages.getString("ActiveLayerNotVector"));
            }
        } catch (Exception e) {
            showFeedback(messages.getString("Error") + e.getMessage());
            logger.log(Level.SEVERE, "WhiteboxGui.deleteFeature", e);
        }
    }

    private void digitizeTool() {
        MapLayer layer = openMaps.get(activeMap).getActiveMapArea().getActiveLayer();
        if (layer instanceof VectorLayerInfo) {
            VectorLayerInfo vli = (VectorLayerInfo) layer;
            if (vli.isActivelyEdited()) {
                vli.setActivelyEdited(false);

            } else {
                vli.setActivelyEdited(true);
                drawingArea.setModifyingPixels(false);
                modifyPixels.setState(false);
                drawingArea.setUsingDistanceTool(false);
                distanceToolMenuItem.setState(false);
            }
        } else {
            showFeedback(messages.getString("ActiveLayerNotVector"));
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
            if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
                showFeedback(messages.getString("NoMapAreas"));
                return;
            }
            layerOverlayNum = openMaps.get(activeMap).getActiveMapArea().getActiveLayerOverlayNumber();
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
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
            showFeedback(messages.getString("FunctionNotAvailableVectors"));
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
            mapAreaNum = openMaps.get(mapNum).getActiveMapAreaElementNumber();
            if (mapAreaNum < 0) { // there is not mapArea or the only mapArea is part of a CartographicElementGroup.
                showFeedback(messages.getString("NoMapAreas"));
                return;
            }
            layerOverlayNum = openMaps.get(activeMap).getActiveMapArea().getActiveLayerOverlayNumber();
            if (layerOverlayNum < 0) {
                showFeedback(messages.getString("NoVectorLayers"));
                return;
            }
        }
        MapArea ma = openMaps.get(mapNum).getMapAreaByElementNum(mapAreaNum);
        if (ma == null) {
            return;
        }
        if (ma.getNumLayers() == 0) {
            showFeedback(messages.getString("NoRaster"));
            return;
        }
        if (ma.getLayer(layerOverlayNum).getLayerType() == MapLayerType.RASTER) {
            RasterLayerInfo layer = (RasterLayerInfo) ma.getLayer(layerOverlayNum);
            HistogramView histo = new HistogramView(this, false, layer.getHeaderFile(), workingDirectory);
        } else {
            showFeedback(messages.getString("RastersOnly"));
        }
    }

    private void removeAllLayers() {
        int j, k;
        if (selectedMapAndLayer[0] != -1) {
            j = selectedMapAndLayer[0];
            k = selectedMapAndLayer[2];
        } else {
            j = activeMap;
            k = openMaps.get(activeMap).getActiveMapAreaElementNumber();
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
                showFeedback(messages.getString("NoHelp"));
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
                showFeedback(messages.getString("HelpNotRead"));
                logger.log(Level.SEVERE, "WhiteboxGui.newHelp", ioe);
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

    public double getDefaultMapMargin() {
        return defaultMapMargin;
    }

    public void setDefaultMapMargin(double defaultMapMargin) {
        this.defaultMapMargin = defaultMapMargin;
    }

    public int getNumberOfRecentItemsToStore() {
        return numberOfRecentItemsToStore;
    }

    public void setNumberOfRecentItemsToStore(int numberOfRecentItemsToStore) {
        this.numberOfRecentItemsToStore = numberOfRecentItemsToStore;
    }

    @Override
    public Font getDefaultFont() {
        return defaultFont;
    }

    public void setDefaultFont(Font font) {
        this.defaultFont = font;
        if (openMaps.size() > 0) {
            for (MapInfo mi : openMaps) {
                mi.setDefaultFont(font);
            }
        }
    }

    public boolean isHideAlignToolbar() {
        return hideAlignToolbar;
    }

    public void setHideAlignToolbar(boolean hideAlignToolbar) {
        this.hideAlignToolbar = hideAlignToolbar;
    }

    private void addMapImage() {
        whitebox.ui.ImageFileChooser ifc = new whitebox.ui.ImageFileChooser();
        ifc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        ifc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        ifc.setMultiSelectionEnabled(false);
        ifc.setAcceptAllFileFilterUsed(false);
        ifc.setCurrentDirectory(new File(workingDirectory));

        int result = ifc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = ifc.getSelectedFile();
            String selectedFile = file.toString();
            openMaps.get(activeMap).addMapImage(selectedFile);
            refreshMap(false);
        }
    }

    @Override
    public void logException(String message, Exception e) {
        logger.log(Level.SEVERE, message, e);
    }

    @Override
    public void logThrowable(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
    }

    @Override
    public void logMessage(Level level, String message) {
        logger.log(level, message);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        switch (actionCommand) {
            case "addLayer":
                addLayer();
                break;
            case "removeLayer":
                removeLayer();
                break;
            case "close":
                close();
                break;
            case "linkMap":
                linkAllOpenMaps = !linkAllOpenMaps;
                linkMap.setState(linkAllOpenMaps);
                break;
            case "nimbusLAF":
                setLookAndFeel("Nimbus");
                break;
            case "systemLAF":
                setLookAndFeel(getSystemLookAndFeelName());
                break;
            case "motifLAF":
                setLookAndFeel("CDE/Motif");
                break;
            case "refreshTools":
                refreshToolUsage();
                break;
            case "newMap":
                newMap();
                break;
            case "closeMap":
                closeMap();
                break;
            case "setAsActiveLayer":
                setAsActiveLayer();
                break;
            case "toggleLayerVisibility":
                toggleLayerVisibility();
                break;
            case "toggleAllLayerVisibility":
                toggleAllLayerVisibility();
                break;
            case "allLayersVisible":
                allLayersVisibile();
                break;
            case "allLayersInvisible":
                allLayersInvisibile();
                break;
            case "toggleLayerVisibilityInLegend":
                toggleLayerVisibilityInLegend();
                break;
            case "setAsActiveMap":
                setAsActiveMap();
                break;
            case "renameMap":
                renameMap();
                break;
            case "changeLayerTitle":
                changeLayerTitle();
                break;
            case "layerToTop":
                layerToTop();
                break;
            case "layerToBottom":
                layerToBottom();
                break;
            case "raiseLayer":
                promoteLayer();
                break;
            case "lowerLayer":
                demoteLayer();
                break;
            case "changePalette":
                changePalette();
                break;
            case "reversePalette":
                reversePalette();
                break;
            case "zoomToFullExtent":
                zoomToFullExtent();
                break;
            case "zoomToLayer":
                zoomToLayer();
                break;
            case "zoomToPage":
                zoomToPage();
                break;
            case "layerProperties":
                showLayerProperties();
                break;
            case "zoomIn":
                zoomIn();
                break;
            case "zoomOut":
                //            zoomOut();
                openMaps.get(activeMap).deslectAllCartographicElements();
                refreshMap(false);
                drawingArea.setMouseMode(MapRenderer2.MOUSE_MODE_ZOOMOUT);
                selectMenuItem.setState(false);
                selectFeatureMenuItem.setState(false);
                zoomMenuItem.setState(false);
                zoomOutMenuItem.setState(true);
                panMenuItem.setState(false);
                break;
            case "panUp":
                panUp();
                break;
            case "panDown":
                panDown();
                break;
            case "panLeft":
                panLeft();
                break;
            case "panRight":
                panRight();
                break;
            case "zoomToBox":
                openMaps.get(activeMap).deslectAllCartographicElements();
                refreshMap(false);
                drawingArea.setMouseMode(MapRenderer2.MOUSE_MODE_ZOOM);
                selectMenuItem.setState(false);
                selectFeatureMenuItem.setState(false);
                zoomMenuItem.setState(true);
                zoomOutMenuItem.setState(false);
                panMenuItem.setState(false);
                break;
            case "pan":
                drawingArea.setMouseMode(MapRenderer2.MOUSE_MODE_PAN);
                selectMenuItem.setState(false);
                selectFeatureMenuItem.setState(false);
                zoomMenuItem.setState(false);
                zoomOutMenuItem.setState(false);
                panMenuItem.setState(true);
                break;
            case "select":
                drawingArea.setMouseMode(MapRenderer2.MOUSE_MODE_SELECT);
                selectMenuItem.setState(true);
                selectFeatureMenuItem.setState(false);
                zoomMenuItem.setState(false);
                zoomOutMenuItem.setState(false);
                panMenuItem.setState(false);
                break;
            case "selectFeature":
                drawingArea.setMouseMode(MapRenderer2.MOUSE_MODE_FEATURE_SELECT);
                selectMenuItem.setState(false);
                selectFeatureMenuItem.setState(true);
                zoomMenuItem.setState(false);
                zoomOutMenuItem.setState(false);
                panMenuItem.setState(false);
                tabs.setSelectedIndex(2);
                break;
            case "nextExtent":
                nextExtent();
                break;
            case "previousExtent":
                previousExtent();
                break;
            case "paletteManager":
                PaletteManager pm = new PaletteManager(paletteDirectory,
                        bundle);
                pm.setVisible(true);
                break;
            case "rasterCalculator":
                RasterCalculator rc = new RasterCalculator(this, false, workingDirectory);
                rc.setLocation(250, 250);
                rc.setVisible(true);
                break;
            case "selectAllText":
                textArea.selectAll();
                break;
            case "copyText":
                textArea.copy();
                break;
            case "pasteText":
                textArea.paste();
                break;
            case "cutText":
                textArea.cut();
                break;
            case "clearText":
                textArea.setText("");
                break;
            case "openText":
                openText();
                break;
            case "saveText":
                saveText();
                break;
            case "closeText":
                textArea.setText("");
                currentTextFile = null;
                break;
            case "printMap":
                printMap();
                break;
            case "saveMap":
                saveMap();
                break;
            case "openMap":
                openMap();
                break;
            case "exportMapAsImage":
                exportMapAsImage();
                break;
            case "scripter":
                Scripter scripter = new Scripter(this, false);
                scripter.setVisible(true);
                break;
            case "options":
                SettingsDialog dlg = new SettingsDialog(this, false);
                dlg.setSize(500, 400);
                dlg.setVisible(true);
                break;
            case "modifyPixels":
                modifyPixelValues();
                break;
            case "helpIndex": {
                Help help = new Help(this, false, "index");
                help.setVisible(true);
                break;
            }
            case "helpSearch": {
                Help help = new Help(this, false, "search");
                help.setVisible(true);
                break;
            }
            case "helpTutorials": {
                Help help = new Help(this, false, "tutorials");
                help.setVisible(true);
                break;
            }
            case "helpAbout":
                showAboutDialog();
                break;
            case "refreshMap":
                refreshMap(true);
                break;
            case "distanceTool":
                distanceTool();
                break;
            case "clipLayerToExtent":
                clipLayerToExtent();
                break;
            case "viewHistogram":
                viewHistogram();
                break;
            case "removeAllLayers":
                removeAllLayers();
                break;
            case "wordWrap":
                textArea.setLineWrap(wordWrap.getState());
                textArea.setWrapStyleWord(wordWrap.getState());
                break;
            case "viewAttributeTable":
                showAttributesFile();
                break;
            case "newHelp":
                newHelp();
                break;
            case "mapProperties":
                showMapProperties(0);
                break;
            case "mapAreaProperties":
                showMapAreaProperties();
                break;
            case "pageProps":
                showMapProperties(-1);
                break;
            case "insertTitle":
                openMaps.get(activeMap).addMapTitle();
                refreshMap(false);
                break;
            case "insertTextArea":
                openMaps.get(activeMap).addMapTextArea();
                refreshMap(false);
                break;
            case "insertScale":
                openMaps.get(activeMap).addMapScale();
                refreshMap(false);
                break;
            case "insertNorthArrow":
                openMaps.get(activeMap).addNorthArrow();
                refreshMap(false);
                break;
            case "insertLegend":
                openMaps.get(activeMap).addLegend();
                refreshMap(false);
                break;
            case "insertNeatline":
                openMaps.get(activeMap).addNeatline();
                refreshMap(false);
                break;
            case "insertMapArea":
                int numMapAreas = openMaps.get(activeMap).getMapAreas().size();
                MapArea ma = new MapArea(bundle.getString("MapArea").replace(" ", "") + (numMapAreas + 1));
                ma.setUpperLeftX(0);
                ma.setUpperLeftY(0);
                ma.setWidth(300);
                ma.setHeight(300);
                ma.setLabelFont(new Font(defaultFont.getName(), Font.PLAIN, 10));
                openMaps.get(activeMap).addNewCartographicElement(ma);
                refreshMap(true);
                break;
            case "insertImage":
                addMapImage();
                break;
            case "deleteMapArea":
                openMaps.get(activeMap).removeCartographicElement(selectedMapAndLayer[2]);
                refreshMap(true);
                break;
            case "fitMapAreaToData":
                fitToData();
                break;
            case "fitMapAreaToPage":
                fitToPage();
                break;
            case "maximizeMapAreaScreenSize":
                maximizeMapAreaScreenSize();
                break;
            case "helpReport":
                helpReport();
                break;
            case "centerVertical":
                if (openMaps.get(activeMap).centerSelectedElementsVertically()) {
                    refreshMap(false);
                }
                break;
            case "centerHorizontal":
                if (openMaps.get(activeMap).centerSelectedElementsHorizontally()) {
                    refreshMap(false);
                }
                break;
            case "alignTop":
                if (openMaps.get(activeMap).alignSelectedElementsTop()) {
                    refreshMap(false);
                }
                break;
            case "alignBottom":
                if (openMaps.get(activeMap).alignSelectedElementsBottom()) {
                    refreshMap(false);
                }
                break;
            case "alignRight":
                if (openMaps.get(activeMap).alignSelectedElementsRight()) {
                    refreshMap(false);
                }
                break;
            case "alignLeft":
                if (openMaps.get(activeMap).alignSelectedElementsLeft()) {
                    refreshMap(false);
                }
                break;
            case "distributeVertically":
                if (openMaps.get(activeMap).distributeSelectedElementsVertically()) {
                    refreshMap(false);
                }
                break;
            case "distributeHorizontally":
                if (openMaps.get(activeMap).distributeSelectedElementsHorizontally()) {
                    refreshMap(false);
                }
                break;
            case "digitizeTool":
                digitizeTool();
                break;
            case "groupElements":
                if (openMaps.get(activeMap).groupElements()) {
                    refreshMap(false);
                }
                break;
            case "ungroupElements":
                if (openMaps.get(activeMap).ungroupElements()) {
                    refreshMap(false);
                }
                break;
            case "editVector":
                editVector();
                break;
            case "digitizeNewFeature":
                digitizeNewFeature();
                break;
            case "deleteFeature":
                deleteFeature();
                break;
        }

        selectedMapAndLayer[0] = -1;
        selectedMapAndLayer[1] = -1;
        selectedMapAndLayer[2] = -1;

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
