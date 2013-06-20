/*
 * Copyright (C) 2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package whitebox.ui.plugin_dialog;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.io.*;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import whitebox.interfaces.DialogComponent;
import whitebox.interfaces.Communicator;
import javax.swing.event.HyperlinkListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ToolDialog extends JDialog implements Communicator, ActionListener, HyperlinkListener {
    private JButton ok = new JButton("OK");
    private JButton close = new JButton("Close");
    private JButton viewCode = new JButton("View Code");
    private JButton back = new JButton();
    private JButton forward = new JButton();
    private JEditorPane helpPane = new JEditorPane();
    private JScrollPane mainScrollPane = null; //new JScrollPane();
    private JPanel mainPanel = new JPanel();
    private String helpFile = "";
    private String parameterFile = "";
    private String graphicsDirectory = "";
    private String workingDirectory = "";
    private String applicationDirectory = "";
    private String resourcesDirectory = "";
    private String logDirectory = "";
    private String pluginName = "";
    private String pathSep = "";
    private String sourceFile = "";
    private ArrayList<DialogComponent> components = new ArrayList<>();
    private Communicator host = null;
    private boolean automaticallyClose = true;
    private ArrayList<String> helpHistory = new ArrayList<>();
    private int helpHistoryIndex = 0;
    
    public ToolDialog(Frame owner, boolean modal, String pluginName, String title, String helpFile) {
        super(owner, modal);
        pathSep = File.separator;
        host = (Communicator) owner;
        workingDirectory = host.getWorkingDirectory();
        this.helpFile = helpFile;
        this.helpHistory.add(helpFile);
        this.pluginName = pluginName;
        applicationDirectory = host.getApplicationDirectory();
        resourcesDirectory = host.getResourcesDirectory();
        parameterFile = resourcesDirectory + "plugins" + 
                pathSep + "Dialogs" + pathSep + pluginName + ".xml";
        // see if the parameterFile exists.
        if (!(new File(parameterFile).exists())) {
            host.showFeedback("The tool's parameter file could not be located.");
        }
        graphicsDirectory = resourcesDirectory + "Images" + pathSep;
        
        createGui(title);
    }
    
    
    public ToolDialog(Communicator com, boolean modal, String pluginName, String title, String helpFile) {
        //super(owner, modal);
        pathSep = File.separator;
        host = com;
        workingDirectory = host.getWorkingDirectory();
        this.helpFile = helpFile;
        this.helpHistory.add(helpFile);
        this.pluginName = pluginName;
        applicationDirectory = host.getApplicationDirectory();
        resourcesDirectory = host.getResourcesDirectory();
        parameterFile = resourcesDirectory + "plugins" + 
                pathSep + "Dialogs" + pathSep + pluginName + ".xml";
        // see if the parameterFile exists.
        if (!(new File(parameterFile).exists())) {
            host.showFeedback("The tool's parameter file could not be located.");
        }
        graphicsDirectory = resourcesDirectory + "Images" + pathSep;
        
        createGui(title);
    }

    private void createGui(String title) {
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }
        String imgLocation = null;
        ImageIcon image = null;
        
        helpPane.addHyperlinkListener(this);
        helpPane.setContentType("text/html");
        drawMainPanel();
        //mainScrollPane.setMinimumSize(new Dimension(380, 100));
        
        JScrollPane helpScroll = new JScrollPane(helpPane);
        
        Box box2 = Box.createHorizontalBox();
        box2.add(Box.createHorizontalStrut(10));
        box2.add(ok);
        ok.setActionCommand("ok");
        ok.addActionListener(this);
        box2.add(Box.createRigidArea(new Dimension(5, 30)));
        box2.add(close);
        close.setActionCommand("close");
        close.addActionListener(this);
        box2.add(Box.createHorizontalStrut(100));
        
        File sourceFileDir = new File(resourcesDirectory + "plugins" + 
                pathSep + "source_files"); // + pathSep + pluginName + ".java";
        findSourceFile(sourceFileDir);
        // see if the source file exists.
        if (sourceFile.length() > 0) {
            viewCode.setActionCommand("viewCode");
            viewCode.addActionListener(this);
            box2.add(viewCode);
        }
        
        box2.add(Box.createHorizontalGlue());
        
        // create the back button
        imgLocation = graphicsDirectory + "HelpBack.png";
        image = new ImageIcon(imgLocation, "");
        back.setActionCommand("back");
        back.setToolTipText("back");
        back.addActionListener(this);
        try {
            back.setIcon(image);
        } catch (Exception e) {
            back.setText("<");
        }
        box2.add(back);
        box2.add(Box.createHorizontalStrut(5));
        
        // create the forward button
        imgLocation = graphicsDirectory + "HelpForward.png";
        image = new ImageIcon(imgLocation, "");
        forward.setActionCommand("forward");
        forward.setToolTipText("forward");
        forward.addActionListener(this);
        try {
            forward.setIcon(image);
        } catch (Exception e) {
            forward.setText(">");
        }
        box2.add(forward);
        box2.add(Box.createHorizontalStrut(10));
        
        JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainScrollPane, helpScroll); //mainScrollPane, helpScroll);
        splitter.setDividerLocation(365);
        splitter.setResizeWeight(0.0);
        splitter.setDividerSize(4);
        
        //Box box3 = Box.createHorizontalBox();
        //box3.add(mainScrollPane);
        //box3.add(helpScroll);
        this.getContentPane().add(splitter, BorderLayout.CENTER);
        //this.getContentPane().add(box3, BorderLayout.CENTER);
        this.getContentPane().add(box2, BorderLayout.SOUTH);
        
        
        File hlp = new File(helpFile);
        
        if (!hlp.exists()) {
            // use the NoHelp.html file.
            helpFile = resourcesDirectory + "Help" + pathSep + "other" + pathSep + "NoHelp.html";
        }
        
        helpPane.setEditable(false);
        try {
            //URL helpURL = getClass().getResource(helpFile);
            //helpPane.setPage(helpURL);
            helpPane.setPage("file:" + helpFile);
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
        } 
        
        //mainScrollPane.setSize(750, 750);
        
        setTitle(title);

        pack();

        // Centre the dialog on the screen.
        // Get the size of the screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = dim.height;
        int screenWidth = dim.width;
        setSize(screenWidth / 2, screenHeight / 2);
        setLocation(screenWidth / 4, screenHeight / 4);
    }
    
    private void drawMainPanel() {
        try {
            //Box box = Box.createVerticalBox();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
            String[] args;
            
            File file = new File(parameterFile);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            Node topNode = doc.getFirstChild();
            Element docElement = doc.getDocumentElement();
            
            NodeList nl = docElement.getElementsByTagName("DialogComponent");
            String componentType = "";
            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {

                    Element el = (Element) nl.item(i);
                    componentType = el.getAttribute("type");
                    
                    if (componentType.equals("DialogFile")) {
                        args = new String[7];
                        args[0] = getTextValue(el, "Name");
                        args[1] = getTextValue(el, "Description");
                        args[2] = getTextValue(el, "LabelText");
                        String dialogMode = getTextValue(el, "DialogMode");
                        if (dialogMode.toLowerCase().contains("open")) {
                            args[3] = Integer.toString(DialogFile.MODE_OPEN);
                        } else {
                            args[3] = Integer.toString(DialogFile.MODE_SAVEAS);
                        }
                        args[4] = getTextValue(el, "ShowButton").toLowerCase();
                        args[5] = getTextValue(el, "Filter");
                        args[6] = getTextValue(el, "MakeOptional").toLowerCase();
                        
                        DialogFile df = new DialogFile(this);
                        df.setArgs(args);
                        components.add(df);
                        mainPanel.add(df);
                        //box.add(df);
                    } else if (componentType.equals("DialogMultiFile")) {
                        args = new String[4];
                        args[0] = getTextValue(el, "Name");
                        args[1] = getTextValue(el, "Description");
                        args[2] = getTextValue(el, "LabelText");
                        args[3] = getTextValue(el, "Filter");
                        
                        DialogMultiFile dmf = new DialogMultiFile(this);
                        dmf.setArgs(args);
                        components.add(dmf);
                        mainPanel.add(dmf);
                        //box.add(dmf);
                    } else if (componentType.equals("DialogCheckBox")) {
                        args = new String[4];
                        args[0] = getTextValue(el, "Name");
                        args[1] = getTextValue(el, "Description");
                        args[2] = getTextValue(el, "LabelText");
                        args[3] = getTextValue(el, "InitialState").toLowerCase();
                        
                        DialogCheckBox dc = new DialogCheckBox();
                        dc.setArgs(args);
                        components.add(dc);
                        mainPanel.add(dc);
                        //box.add(dc);
                    } else if (componentType.equals("DialogComboBox")) {
                        args = new String[5];
                        args[0] = getTextValue(el, "Name");
                        args[1] = getTextValue(el, "Description");
                        args[2] = getTextValue(el, "LabelText");
                        args[3] = getTextValue(el, "ListItems");
                        args[4] = getTextValue(el, "DefaultItem");
                        
                        DialogComboBox cb = new DialogComboBox();
                        cb.setArgs(args);
                        components.add(cb);
                        mainPanel.add(cb);
                        //box.add(cb);
                    } else if (componentType.equals("DialogDataInput")) {
                        args = new String[6];
                        args[0] = getTextValue(el, "Name");
                        args[1] = getTextValue(el, "Description");
                        args[2] = getTextValue(el, "LabelText");
                        args[3] = getTextValue(el, "InitialText");
                        args[4] = getTextValue(el, "NumericalInputOnly").toLowerCase();
                        args[5] = getTextValue(el, "MakeOptional").toLowerCase();
                        
                        DialogDataInput di = new DialogDataInput();
                        di.setArgs(args);
                        components.add(di);
                        mainPanel.add(di);
                        //box.add(di);
                    } else if (componentType.equals("DialogOption")) {
                        args = new String[5];
                        args[0] = getTextValue(el, "Name");
                        args[1] = getTextValue(el, "Description");
                        args[2] = getTextValue(el, "LabelText");
                        args[3] = getTextValue(el, "Button1Label");
                        args[4] = getTextValue(el, "Button2Label");
                        
                        DialogOption opt = new DialogOption();
                        opt.setArgs(args);
                        components.add(opt);
                        mainPanel.add(opt);
                        //box.add(opt);
                    } else if (componentType.equals("DialogReclassGrid")) {
                        args = new String[2];
                        args[0] = getTextValue(el, "Name");
                        args[1] = getTextValue(el, "Description");
                        
                        DialogReclassGrid drg = new DialogReclassGrid(this);
                        drg.setArgs(args);
                        components.add(drg);
                        mainPanel.add(drg);
                    } else if (componentType.equals("DialogWeightedMultiFile")) {
                        args = new String[4];
                        args[0] = getTextValue(el, "Name");
                        args[1] = getTextValue(el, "Description");
                        args[2] = getTextValue(el, "ShowCheck");
                        args[3] = getTextValue(el, "Filter");
                        
                        DialogWeightedMultiFile dwmf = new DialogWeightedMultiFile(this);
                        dwmf.setArgs(args);
                        components.add(dwmf);
                        mainPanel.add(dwmf);
                    } else if (componentType.equals("Label")) {
                        args = new String[2];
                        args[0] = getTextValue(el, "Name");
                        args[1] = getTextValue(el, "LabelText");
                        Box box = Box.createHorizontalBox();
                        JLabel lbl = new JLabel(args[1]);
                        box.add(Box.createHorizontalStrut(5));
                        box.add(lbl);
                        box.add(Box.createHorizontalGlue());
                        mainPanel.add(box);
                    }
                }
            }

            //box.setBackground(Color.DARK_GRAY);
            //mainPanel.add(box);
            mainScrollPane = new JScrollPane(mainPanel);
        } catch (Exception e) {
            showFeedback(e.getMessage());
        }
    }
    
    private String getTextValue(Element ele, String tagName) {
        String textVal = "";
        try {
            NodeList nl = ele.getElementsByTagName(tagName);
            if (nl != null && nl.getLength() > 0) {
                Element el = (Element) nl.item(0);
                textVal = el.getFirstChild().getNodeValue();
            }
        } catch (Exception e) {
            
        } finally {
            return textVal;
        }
    }
    
    private String[] collectValues() {
        int numComponents = components.size();
        String[] ret = new String[numComponents];
        for (int i = 0; i < numComponents; i++) {
            ret[i] = components.get(i).getValue();
        }
        return ret;
    }
    
    @Override
    public String getWorkingDirectory() {
        // update the workingDirectory
        workingDirectory = host.getWorkingDirectory();
        return workingDirectory;
    }
    
    @Override
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        // update the workingDirectory
        host.setWorkingDirectory(workingDirectory);
    }

    @Override
    public String getApplicationDirectory() {
        // update the applicationDirectory
        applicationDirectory = host.getApplicationDirectory();
        return applicationDirectory;
    }
    
    @Override
    public void setApplicationDirectory(String applicationDirectory) {
        this.applicationDirectory = applicationDirectory;
        host.setApplicationDirectory(applicationDirectory);
    }
    
    @Override
    public String getResourcesDirectory() {
        // update the applicationDirectory
        resourcesDirectory = host.getResourcesDirectory();
        return resourcesDirectory;
    }
    
    @Override
    public String getLogDirectory() {
        // update the applicationDirectory
        logDirectory = host.getLogDirectory();
        return logDirectory;
    }
    
    /**
     * Used to communicate feedback pop-up messages between a plugin tool and the main Whitebox user-interface.
     * @param feedback String containing the text to display.
     */
    @Override
    public int showFeedback(String message) {
        host.showFeedback(message);
        return -1;
    }
    
    @Override
    public int showFeedback(String message, int optionType, int messageType) {
        int n = host.showFeedback(message, optionType, messageType);
        return n;
    }
    
    /**
     * Used to find whether the dialog will automatically close after being launched.
     * @return boolean.
     */
    public boolean getAutomaticallyClose() {
        return automaticallyClose;
    }
    
    /**
     * Used to set whether the dialog should automatically close after bing launched.
     * @param value Boolean value. True if dialog should close, otherwise false.
     */
    public void setAutomaticallyClose(boolean value) {
        automaticallyClose = value;
    }
    
    /**
     * Used to run a plugin through the Host app.
     * @param pluginName String containing the descriptive name of the plugin.
     * @param args String array containing the parameters to feed to the plugin.
     */
    @Override
    public void runPlugin(String pluginName, String[] args) {
        host.runPlugin(pluginName, args);
        if (automaticallyClose) {
            //this.setVisible(false);
            this.dispose();
        }
    }
    
    private void back() {
        if (helpHistoryIndex == 0) { return; }
        helpHistoryIndex--;
        try {
            helpPane.setPage("file:" + helpHistory.get(helpHistoryIndex));
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
        } 
        
    }
    
    private void forward() {
        if (helpHistoryIndex == helpHistory.size() - 1) { return; }
        helpHistoryIndex++;
        try {
            helpPane.setPage("file:" + helpHistory.get(helpHistoryIndex));
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
        } 
        
    }
    
    private void findSourceFile(File dir) {
        File[] files = dir.listFiles();
        for (int x = 0; x < files.length; x++) {
            if (files[x].isDirectory()) {
                findSourceFile(files[x]);
            } else if (files[x].toString().contains(pluginName + ".java")) {
                sourceFile = files[x].toString();
                break;
            }
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("close")) {
            //this.setVisible(false);
            this.dispose();
        } else if (actionCommand.equals("ok")) {
            String[] args = collectValues();
            boolean containsNull = false;
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    containsNull = true;
                    showFeedback("Parameter " + components.get(i).getComponentName() + " has not been specified. The tool will not execute.");
                    break;
                }
            }
            
            if (!containsNull) { runPlugin(pluginName, args); }
        } else if (actionCommand.equals("viewCode")) {
//            ViewCodeDialog vcd = new ViewCodeDialog((Frame)host, true, pluginName, this.getTitle());
//            vcd.setSize(new Dimension(800, 600));
//            vcd.setVisible(true);
            
        } else if (actionCommand.equals("back")) {
            back();
        } else if (actionCommand.equals("forward")) {
            forward();
        }
    }
    
    @Override
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                if (helpHistoryIndex == helpHistory.size() - 1) {
                    helpHistory.add(event.getURL().getFile());
                    helpHistoryIndex = helpHistory.size() - 1;
                } else {
                    for (int i = helpHistory.size() - 1; i > helpHistoryIndex; i--) {
                        helpHistory.remove(i);
                    }
                    helpHistory.add(event.getURL().getFile());
                    helpHistoryIndex = helpHistory.size() - 1;
                }
                helpPane.setPage(event.getURL());
            } catch (IOException ioe) {
                // Some warning to user
            }
        }
    }
    
    @Override
    public void dispose() {
        ok = null;
        close = null;
        viewCode = null;
        back = null;
        forward = null;
        helpPane = null;
        mainScrollPane = null;
        mainPanel = null;
        components = null;
        host = null;
        super.dispose();
    }

    @Override
    public ResourceBundle getGuiLabelsBundle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ResourceBundle getMessageBundle() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void logException(String message, Exception e) {
        if (host != null) {
            host.logException(message, e);
        }
    }
    
    @Override
    public void logThrowable(String message, Throwable t) {
        if (host != null) {
            host.logThrowable(message, t);
        }
    }
}
