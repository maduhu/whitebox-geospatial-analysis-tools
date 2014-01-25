/*
 * Copyright (C) 2013 Dr. John Lindsay <jlindsay@uoguelph.ca>
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
import java.util.logging.Level;
import whitebox.interfaces.DialogComponent;
import whitebox.interfaces.Communicator;
import javax.swing.event.HyperlinkListener;
import whitebox.utilities.FileUtilities;

/**
 * This class is used to provide a dialog box for a Whitebox GAT script tool.
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ScriptDialog extends JDialog implements Communicator, ActionListener, HyperlinkListener {

    private JButton ok = new JButton("OK");
    private JButton close = new JButton("Close");
    private JButton viewCode = new JButton("View Code");
    private JButton back = new JButton();
    private JButton forward = new JButton();
    private JButton newHelp = new JButton();
    private JButton modifyHelp = new JButton();
    private JEditorPane helpPane = new JEditorPane();
    private JScrollPane mainScrollPane = null; //new JScrollPane();
    private JPanel mainPanel = new JPanel();
    private String helpFile = "";
    private String graphicsDirectory = "";
    private String workingDirectory = "";
    private String applicationDirectory = "";
    private String resourcesDirectory = "";
    private String logDirectory = "";
    private String pathSep = "";
    private String sourceFile = "";
    private ArrayList<DialogComponent> components = new ArrayList<>();
    private Communicator host = null;
    private boolean automaticallyClose = true;
    private ArrayList<String> helpHistory = new ArrayList<>();
    private int helpHistoryIndex = 0;
    private ActionListener buttonActionListener = null;
    private ResourceBundle bundle;
    private ResourceBundle messages;

    /**
     * Constructor
     * @param owner the name of the Whitebox GAT GUI class
     * @param title A string that should be the same name as the script's descriptive name
     * @param buttonActionListener A listener to attach to the OK button
     */
    public ScriptDialog(Frame owner, String title, ActionListener buttonActionListener) {
        super(owner, false);
        pathSep = File.separator;
        host = (Communicator) owner;
        workingDirectory = host.getWorkingDirectory();
        applicationDirectory = host.getApplicationDirectory();
        resourcesDirectory = host.getResourcesDirectory();
        graphicsDirectory = resourcesDirectory + "Images" + pathSep;
        bundle = host.getGuiLabelsBundle();
        messages = host.getMessageBundle();

        setTitle(title);

        this.buttonActionListener = buttonActionListener;

        createGui();
    }

    private void createGui() {
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }

        ok = new JButton(bundle.getString("Run"));
        close = new JButton(bundle.getString("Close"));
        viewCode = new JButton(bundle.getString("ViewCode"));
        newHelp = new JButton(bundle.getString("NewHelp"));
        modifyHelp = new JButton(bundle.getString("ModifyHelp"));

        String imgLocation = null;
        ImageIcon image = null;

        helpPane.addHyperlinkListener(this);
        helpPane.setContentType("text/html");

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainScrollPane = new JScrollPane(mainPanel);

        JScrollPane helpScroll = new JScrollPane(helpPane);

        Box box2 = Box.createHorizontalBox();
        box2.add(Box.createHorizontalStrut(10));
        box2.add(ok);
        ok.setActionCommand("ok");
        ok.addActionListener(this);
        if (buttonActionListener != null) {
            ok.addActionListener(buttonActionListener);
        }
        box2.add(Box.createRigidArea(new Dimension(5, 30)));
        box2.add(close);
        close.setActionCommand("close");
        close.addActionListener(this);
        if (buttonActionListener != null) {
            close.addActionListener(buttonActionListener);
        }

        box2.add(Box.createHorizontalStrut(5));

        viewCode.setActionCommand("viewCode");
        viewCode.addActionListener(this);
        viewCode.setVisible(false);
        box2.add(viewCode);

        box2.add(Box.createHorizontalGlue());

        // create the newHelp button
        newHelp.setActionCommand("newHelp");
        newHelp.setToolTipText("Create New HelpEntry");
        newHelp.addActionListener(this);
        newHelp.setVisible(false);
        box2.add(newHelp);
        box2.add(Box.createHorizontalStrut(5));

        // create the newHelp button
        modifyHelp.setActionCommand("modifyHelp");
        modifyHelp.setToolTipText("Modify Help File");
        modifyHelp.addActionListener(this);
        modifyHelp.setVisible(false);
        box2.add(modifyHelp);
        box2.add(Box.createHorizontalStrut(5));


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

        JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainScrollPane, helpScroll);
        splitter.setDividerLocation(365);
        splitter.setResizeWeight(0.0);
        splitter.setDividerSize(4);

        this.getContentPane().add(splitter, BorderLayout.CENTER);
        this.getContentPane().add(box2, BorderLayout.SOUTH);

        pack();

        setSize(800, 400);

        // Centre the dialog on the screen.
        // Get the size of the screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = dim.height;
        int screenWidth = dim.width;
        //setSize(screenWidth / 2, screenHeight / 2);
        setLocation(screenWidth / 4, screenHeight / 4);
    }

    /**
     * Adds a checkbox to the dialog
     * @param description This string is entered into the pop-up description.
     * @param labelText This string is added to the checkbox label.
     * @param initialState Determines whether the checkbox is checked at startup.
     * @return DialogCheckBox a reference to the created object.
     */
    public DialogCheckBox addDialogCheckBox(String description, String labelText,
            boolean initialState) {
        String[] args = new String[4];
        args[0] = "checkbox";
        args[1] = description;
        args[2] = labelText;
        args[3] = Boolean.toString(initialState);

        DialogCheckBox dcb = new DialogCheckBox();
        dcb.setArgs(args);
        components.add(dcb);
        mainPanel.add(dcb);
        return dcb;
    }

    public DialogDataInput addDialogDataInput(String description, String labelText,
            String initialText, boolean numericalInput, boolean makeOptional) {
        String[] args = new String[6];
        args[0] = "data";
        args[1] = description;
        args[2] = labelText;
        args[3] = initialText;
        args[4] = Boolean.toString(numericalInput);
        args[5] = Boolean.toString(makeOptional);

        DialogDataInput ddi = new DialogDataInput();
        ddi.setArgs(args);
        components.add(ddi);
        mainPanel.add(ddi);
        return ddi;
    }

    public DialogFile addDialogFile(String description, String labelText,
            String dialogMode, String filter, boolean showButton, boolean makeOptional) {
        String[] args = new String[7];
        args[0] = "file";
        args[1] = description;
        args[2] = labelText;
        if (dialogMode.toLowerCase().contains("open")) {
            args[3] = Integer.toString(DialogFile.MODE_OPEN);
        } else {
            args[3] = Integer.toString(DialogFile.MODE_SAVEAS);
        }
        args[4] = Boolean.toString(showButton);
        args[5] = filter;
        args[6] = Boolean.toString(makeOptional);

        DialogFile df = new DialogFile(host);
        df.setArgs(args);
        components.add(df);
        mainPanel.add(df);
        return df;
    }
    
//    public DialogFile addDialogFile(String description, String labelText,
//            String dialogMode, String filter, boolean showButton, 
//            boolean makeOptional, ActionListener actionListener) {
//        String[] args = new String[7];
//        args[0] = "file";
//        args[1] = description;
//        args[2] = labelText;
//        if (dialogMode.toLowerCase().contains("open")) {
//            args[3] = Integer.toString(DialogFile.MODE_OPEN);
//        } else {
//            args[3] = Integer.toString(DialogFile.MODE_SAVEAS);
//        }
//        args[4] = Boolean.toString(showButton);
//        args[5] = filter;
//        args[6] = Boolean.toString(makeOptional);
//
//        DialogFile df = new DialogFile(host);
//        df.setArgs(args);
//        df.setTextFieldActionListener(actionListener);
//        components.add(df);
//        mainPanel.add(df);
//        return df;
//    }

    public DialogMultiFile addDialogMultiFile(String description, String labelText,
            String filter) {
        String[] args = new String[4];
        args[0] = "multifile";
        args[1] = description;
        args[2] = labelText;
        args[3] = filter;

        DialogMultiFile dmf = new DialogMultiFile(host);
        dmf.setArgs(args);
        components.add(dmf);
        mainPanel.add(dmf);
        return dmf;
    }

    
    public JButton addDialogButton(String text, String align) {
        Box box = Box.createHorizontalBox();
        JButton btn = new JButton(text);
        //box.add(Box.createHorizontalStrut(5));
        
        if (align.toLowerCase().contains("right")) {
            box.add(Box.createHorizontalGlue());
            box.add(btn);
        } else if (align.toLowerCase().contains("left")) {
            box.add(btn);
            box.add(Box.createHorizontalGlue());
        } else { // centre
            box.add(Box.createHorizontalGlue());
            box.add(btn);
            box.add(Box.createHorizontalGlue());
        }
        
        mainPanel.add(box);
        return btn;
    }
    
    public JLabel addDialogLabel(String text) {
        Box box = Box.createHorizontalBox();
        JLabel lbl = new JLabel(text);
        box.add(Box.createHorizontalStrut(5));
        box.add(lbl);
        box.add(Box.createHorizontalGlue());
        mainPanel.add(box);
        return lbl;
    }
    
    public DialogFieldSelector addDialogFieldSelector(String description, String labelText,
            boolean allowMultipleSelection) {
        String[] args = new String[4];
        args[0] = "fieldselector";
        args[1] = description;
        args[2] = labelText;
        args[3] = Boolean.toString(allowMultipleSelection);

        DialogFieldSelector dfs = new DialogFieldSelector();
        dfs.setHostDialog(host);
        dfs.setArgs(args);
        components.add(dfs);
        mainPanel.add(dfs);
        return dfs;
    }

    public DialogComboBox addDialogComboBox(String description, String labelText,
            ArrayList<String> listItems, int defaultItem) {
        String[] args = new String[5];
        args[0] = "combobox";
        args[1] = description;
        args[2] = labelText;
        StringBuilder items = new StringBuilder();
        for (int a = 0; a < listItems.size(); a++) {
            if (a > 0) {
                items.append(",").append(listItems.get(a));
            } else {
                items.append(listItems.get(a));
            }
        }
        args[3] = items.toString();
        args[4] = Integer.toString(defaultItem);

        DialogComboBox dcb = new DialogComboBox();
        dcb.setArgs(args);
        components.add(dcb);
        mainPanel.add(dcb);
        return dcb;
    }

    public DialogComboBox addDialogComboBox(String description, String labelText,
            String[] listItems, int defaultItem) {
        String[] args = new String[5];
        args[0] = "combobox";
        args[1] = description;
        args[2] = labelText;
        StringBuilder items = new StringBuilder();
        for (int a = 0; a < listItems.length; a++) {
            if (a > 0) {
                items.append(",").append(listItems[a]);
            } else {
                items.append(listItems[a]);
            }
        }
        args[3] = items.toString();
        args[4] = Integer.toString(defaultItem);

        DialogComboBox dcb = new DialogComboBox();
        dcb.setArgs(args);
        components.add(dcb);
        mainPanel.add(dcb);
        return dcb;
    }

    public DialogOption addDialogOption(String description, String labelText,
            String button1String, String button2String) {
        String[] args = new String[5];
        args[0] = "dialogoption";
        args[1] = description;
        args[2] = labelText;
        args[3] = button1String;
        args[4] = button2String;

        DialogOption d = new DialogOption();
        d.setArgs(args);
        components.add(d);
        mainPanel.add(d);
        return d;
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
     * Used to communicate feedback pop-up messages between a plugin tool and
     * the main Whitebox user-interface.
     *
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
     * Used to find whether the dialog will automatically close after being
     * launched.
     *
     * @return boolean.
     */
    public boolean getAutomaticallyClose() {
        return automaticallyClose;
    }

    /**
     * Used to set whether the dialog should automatically close after bing
     * launched.
     *
     * @param value Boolean value. True if dialog should close, otherwise false.
     */
    public void setAutomaticallyClose(boolean value) {
        automaticallyClose = value;
    }

    public void setSourceFile(String file) {
        sourceFile = file;
        if (new File(sourceFile).exists()) {
            viewCode.setVisible(true);
        }
    }
    String scriptsHelpFile = null;

    public void setHelpFile(String newHelpFile) {
        this.helpFile = newHelpFile;

        if (!helpFile.contains(pathSep)) {
            helpFile = resourcesDirectory + "Help" + pathSep + helpFile;
        }
        if (!helpFile.endsWith(".html")) {
            helpFile = this.helpFile + ".html";
        }

        File hlp = new File(helpFile);

        scriptsHelpFile = helpFile;
        if (!hlp.exists()) {
            // use the NoHelp.html file.
            helpFile = resourcesDirectory + "Help" + pathSep + "other" + pathSep + "NoHelp.html";
            newHelp.setVisible(true);
            modifyHelp.setVisible(false);
        } else {
            modifyHelp.setVisible(true);
            newHelp.setVisible(false);
        }

        this.helpHistory.add(helpFile);

        helpPane.setEditable(false);
        try {
            //URL helpURL = getClass().getResource(helpFile);
            //helpPane.setPage(helpURL);
            helpPane.setPage("file:" + helpFile);
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
        }
    }

    /**
     * Used to run a plugin through the Host app.
     *
     * @param pluginName String containing the descriptive name of the plugin.
     * @param args String array containing the parameters to feed to the plugin.
     * @param runOnDedicatedThread boolean value; set to true if the tool should
     * be run on a dedicated thread and false if it should be run on the same
     * thread as the calling Communicator.
     */
    @Override
    public void runPlugin(String pluginName, String[] args, boolean runOnDedicatedThread) {
        host.runPlugin(pluginName, args, runOnDedicatedThread);
        if (automaticallyClose) {
            this.dispose();
        }
    }

    /**
     * Used to run a plugin through the Host app.
     *
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

    @Override
    public void runPlugin(String pluginName, String[] args, boolean runOnDedicatedThread, boolean suppressReturnedData) {
        host.runPlugin(pluginName, args, runOnDedicatedThread, suppressReturnedData);
        if (automaticallyClose) {
            //this.setVisible(false);
            this.dispose();
        }
    }

    private void back() {
        if (helpHistoryIndex == 0) {
            return;
        }
        helpHistoryIndex--;
        try {
            helpPane.setPage("file:" + helpHistory.get(helpHistoryIndex));
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
        }

    }

    private void forward() {
        if (helpHistoryIndex == helpHistory.size() - 1) {
            return;
        }
        helpHistoryIndex++;
        try {
            helpPane.setPage("file:" + helpHistory.get(helpHistoryIndex));
        } catch (IOException e) {
            System.err.println(e.getStackTrace());
        }

    }

    public String[] collectParameters() {
        String[] args = collectValues();
        if (userButtonSelection == 1) {
            //boolean containsNull = false;
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    //containsNull = true;
                    showFeedback("Parameter " + components.get(i).getComponentName() + " has not been specified. The tool will not execute.");
                    break;
                }
            }
        }
        return args;
    }
    private int userButtonSelection = -1;

    public int getUserButtonSelection() {
        return userButtonSelection;
    }

    private void newHelp() {
        String helpDirectory = host.getResourcesDirectory() + "Help" + pathSep;

        // grab the text within the "NewHelp.txt" file in the helpDirectory;
        String defaultHelp = helpDirectory + "NewHelp.txt";
        if (!(new File(defaultHelp)).exists()) {
            showFeedback(messages.getString("NoHelp"));
            return;
        }
        try {
            String defaultText = FileUtilities.readFileAsString(defaultHelp);
            // now place this text into the new file.
            FileUtilities.fillFileWithString(scriptsHelpFile, defaultText);

            ViewCodeDialog vcd = new ViewCodeDialog((Frame) host, false, new File(scriptsHelpFile), true);
            vcd.setSize(new Dimension(800, 600));
            vcd.setVisible(true);
        } catch (IOException ioe) {
            showFeedback(messages.getString("HelpNotRead"));
            return;
        }
    }

    private void modifyHelp() {
        String helpDirectory = host.getResourcesDirectory() + "Help" + pathSep;

        // grab the text within the "NewHelp.txt" file in the helpDirectory;
        if (!(new File(scriptsHelpFile)).exists()) {
            showFeedback(messages.getString("NoHelpDirectory"));
            return;
        }
        ViewCodeDialog vcd = new ViewCodeDialog((Frame) host, false, new File(scriptsHelpFile), true);
        vcd.setSize(new Dimension(800, 600));
        vcd.setVisible(true);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("close")) {
            userButtonSelection = 2;
            this.setVisible(false);
        } else if (actionCommand.equals("ok")) {
            userButtonSelection = 1;
            this.setVisible(false);
        } else if (actionCommand.equals("viewCode") && sourceFile != null) {
            ViewCodeDialog vcd = new ViewCodeDialog((Frame) host, false, new File(sourceFile), false);
            vcd.setSize(new Dimension(800, 600));
            vcd.setVisible(true);
        } else if (actionCommand.equals("back")) {
            back();
        } else if (actionCommand.equals("forward")) {
            forward();
        } else if (actionCommand.equals("newHelp")) {
            newHelp();
        } else if (actionCommand.equals("modifyHelp")) {
            modifyHelp();
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
        if (host != null) {
            return host.getGuiLabelsBundle();
        } else {
            return null;
        }
    }

    @Override
    public ResourceBundle getMessageBundle() {
        if (host != null) {
            return host.getMessageBundle();
        } else {
            return null;
        }
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

    @Override
    public void logMessage(Level level, String message) {
        if (host != null) {
            host.logMessage(level, message);
        }
    }

    @Override
    public String[] getCurrentlyDisplayedFiles() {
        return host.getCurrentlyDisplayedFiles();
    }

    @Override
    public String getHelpDirectory() {
        return host.getHelpDirectory();
    }
}
