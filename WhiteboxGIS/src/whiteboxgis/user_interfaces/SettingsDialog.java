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
package whiteboxgis.user_interfaces;

import whitebox.ui.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.swing.*;
import java.text.DecimalFormat;
import whitebox.interfaces.Communicator;
import whiteboxgis.WhiteboxGui;

/**
 *
 * @author johnlindsay
 */
public class SettingsDialog extends JDialog implements Communicator, ActionListener, PropertyChangeListener {

    private int rightMargin = 10;
    private int leftMargin = 10;
    private Color backColour = new Color(225, 245, 255);
    private JButton ok;
    private JButton close;
    private String pathSep = "";
    private WhiteboxGui host = null;
    private String applicationDirectory = "";
    private String resourcesDirectory = "";
    private String logDirectory = "";
    private ResourceBundle bundle;
    private ResourceBundle messages;
    private String language;
    private String country;

    public SettingsDialog(Frame owner, boolean modal) {
        super(owner, modal);
        pathSep = File.separator;
        host = (WhiteboxGui) owner;
        applicationDirectory = host.getApplicationDirectory();
        resourcesDirectory = host.getResourcesDirectory();
        bundle = host.getGuiLabelsBundle();
        messages = host.getMessageBundle();
        createGui();
    }

    private void createGui() {
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }

        this.setTitle("Whitebox " + bundle.getString("Settings"));

        Box box2 = Box.createHorizontalBox();
        box2.add(Box.createHorizontalStrut(10));
        ok = new JButton(bundle.getString("Update"));
        box2.add(ok);
        ok.setActionCommand("ok");
        ok.addActionListener(this);
        box2.add(Box.createRigidArea(new Dimension(5, 30)));
        close = new JButton(bundle.getString("Close"));
        box2.add(close);
        close.setActionCommand("close");
        close.addActionListener(this);
        box2.add(Box.createHorizontalStrut(100));


        box2.add(Box.createHorizontalGlue());

        this.getContentPane().add(box2, BorderLayout.SOUTH);



        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);

        Box mainBox = Box.createVerticalBox();
        int preferredWidth = 310;

        // print resolution
        NumericProperty printResolution = new NumericProperty(bundle.getString("PrintResolution")
                + ":", String.valueOf(host.getPrintResolution()));
        printResolution.setName("printResolution");
        printResolution.setLeftMargin(leftMargin);
        printResolution.setRightMargin(rightMargin);
        printResolution.setBackColour(backColour);
        printResolution.setPreferredWidth(preferredWidth);
        printResolution.setParseIntegersOnly(true);
        printResolution.setTextboxWidth(5);
        printResolution.revalidate();
        printResolution.addPropertyChangeListener("value", this);
        mainBox.add(printResolution);

        BooleanProperty autoHideAlignToolbar = new BooleanProperty(bundle.getString("HideToolbar"),
                host.isHideAlignToolbar());
        autoHideAlignToolbar.setName("autoHideAlignToolbar");
        autoHideAlignToolbar.setLeftMargin(leftMargin);
        autoHideAlignToolbar.setRightMargin(rightMargin);
        autoHideAlignToolbar.setBackColour(Color.WHITE);
        autoHideAlignToolbar.setPreferredWidth(preferredWidth);
        autoHideAlignToolbar.revalidate();
        autoHideAlignToolbar.addPropertyChangeListener("value", this);
        mainBox.add(autoHideAlignToolbar);

        FontProperty fontProperty = new FontProperty(bundle.getString("DefaultFont")
                + ":", host.getDefaultFont());
        fontProperty.setName("fontProperty");
        fontProperty.setLeftMargin(leftMargin);
        fontProperty.setRightMargin(rightMargin);
        fontProperty.setBackColour(backColour);
        fontProperty.setTextboxWidth(10);
        fontProperty.setPreferredWidth(preferredWidth);
        fontProperty.addPropertyChangeListener("value", this);
        fontProperty.revalidate();
        mainBox.add(fontProperty);

        // number of recent items
        NumericProperty numRecentItems = new NumericProperty(bundle.getString("NumberOfRecentItems")
                + ":", String.valueOf(host.getNumberOfRecentItemsToStore()));
        numRecentItems.setName("numRecentItems");
        numRecentItems.setLeftMargin(leftMargin);
        numRecentItems.setRightMargin(rightMargin);
        numRecentItems.setBackColour(Color.WHITE);
        numRecentItems.setPreferredWidth(preferredWidth);
        numRecentItems.setParseIntegersOnly(true);
        numRecentItems.setTextboxWidth(5);
        numRecentItems.revalidate();
        numRecentItems.addPropertyChangeListener("value", this);
        mainBox.add(numRecentItems);


        ComboBoxProperty languageChooser =
                SupportedLanguageChooser.getLanguageChooser(host, false);
        languageChooser.setName("languageChooser");
        languageChooser.setLeftMargin(leftMargin);
        languageChooser.setRightMargin(rightMargin);
        languageChooser.setBackColour(backColour);
        languageChooser.setPreferredWidth(preferredWidth);
        languageChooser.revalidate();

        mainBox.add(languageChooser);


        BooleanProperty checkForUpdates = new BooleanProperty(bundle.getString("CheckForUpdates"),
                host.isCheckForUpdates());
        checkForUpdates.setName("checkForUpdates");
        checkForUpdates.setLeftMargin(leftMargin);
        checkForUpdates.setRightMargin(rightMargin);
        checkForUpdates.setBackColour(Color.WHITE);
        checkForUpdates.setPreferredWidth(preferredWidth);
        checkForUpdates.revalidate();
        checkForUpdates.addPropertyChangeListener("value", this);
        mainBox.add(checkForUpdates);


        BooleanProperty receiveAnnouncements = new BooleanProperty(bundle.getString("ReceiveAnnouncements"),
                host.isReceiveAnnouncements());
        receiveAnnouncements.setName("receiveAnnouncements");
        receiveAnnouncements.setLeftMargin(leftMargin);
        receiveAnnouncements.setRightMargin(rightMargin);
        receiveAnnouncements.setBackColour(backColour);
        receiveAnnouncements.setPreferredWidth(preferredWidth);
        receiveAnnouncements.revalidate();
        receiveAnnouncements.addPropertyChangeListener("value", this);
        mainBox.add(receiveAnnouncements);

        mainBox.add(Box.createVerticalStrut(4));
        
        DecimalFormat df = new DecimalFormat("###,##0.0");
        
        Box heapBox = Box.createHorizontalBox();
        heapBox.add(Box.createHorizontalStrut(10));
        String str = "Maximum heap size: " + df.format(Runtime.getRuntime().maxMemory() / 1073741824.0) + "GB";
        heapBox.add(new JLabel(str));
        heapBox.add(Box.createHorizontalStrut(30));
        str = "Available memory: " + df.format(Runtime.getRuntime().freeMemory() / 1073741824.0) + "GB";
        heapBox.add(new JLabel(str));
        heapBox.add(Box.createHorizontalGlue());
        mainBox.add(heapBox);

        mainBox.add(Box.createVerticalStrut(4));
        
        Box bitBox = Box.createHorizontalBox();
        bitBox.add(Box.createHorizontalStrut(10));
        str = "Running on ";
        if (System.getProperty("sun.arch.data.model").contains("32")) {
            str += "32-bit Java";
        } else {
            str += "64-bit Java";
        }
        JLabel bitLabel = new JLabel(str);
        bitBox.add(bitLabel);
        bitBox.add(Box.createHorizontalGlue());
        
        Box bitVBox = Box.createVerticalBox();
        bitVBox.setBackground(backColour);
        bitVBox.setOpaque(true);
        bitVBox.add(Box.createVerticalStrut(4));
        bitVBox.add(bitBox);
        bitVBox.add(Box.createVerticalStrut(4));
        mainBox.add(bitVBox);

        JScrollPane scroll = new JScrollPane(mainBox);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainPanel.add(scroll);

        this.getContentPane().add(mainPanel, BorderLayout.CENTER);

        pack();

        setLocationRelativeTo(null);
        
    }

    private void okPressed() {
        this.dispose();
    }

    @Override
    public String getWorkingDirectory() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setWorkingDirectory(String workingDirectory) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getApplicationDirectory() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setApplicationDirectory(String applicationDirectory) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getResourcesDirectory() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int showFeedback(String message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int showFeedback(String message, int optionType, int messageType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void runPlugin(String pluginName, String[] args) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        switch (actionCommand) {
            case "close":
                this.dispose();
                break;
            case "ok":
                okPressed();
                break;
        }
    }

    @Override
    public void dispose() {
        ok = null;
        close = null;
        host = null;
        super.dispose();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Object source = evt.getSource();
        if (!evt.getPropertyName().equals("value")) {
            return;
        }
        String componentName = "";
        if (source instanceof JComponent) {
            JComponent component = (JComponent) source;
            componentName = component.getName();
        }
        switch (componentName) {
            case "printResolution":
                host.setPrintResolution(Integer.parseInt((String) evt.getNewValue()));
                break;

            case "autoHideAlignToolbar":
                host.setHideAlignToolbar((Boolean) evt.getNewValue());
                break;

            case "fontProperty":
                host.setDefaultFont((Font) evt.getNewValue());
                break;

            case "numRecentItems":
                host.setNumberOfRecentItemsToStore(Integer.parseInt((String) evt.getNewValue()));
                break;

            case "checkForUpdates":
                host.setCheckForUpdates((Boolean) evt.getNewValue());
                break;

            case "receiveAnnouncements":
                host.setReceiveAnnouncements((Boolean) evt.getNewValue());
                break;
        }
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
    public String getLogDirectory() {
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

    @Override
    public void logMessage(Level level, String message) {
        if (host != null) {
            host.logMessage(level, message);
        }
    }

    @Override
    public void runPlugin(String pluginName, String[] args, boolean runOnDedicatedThread) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void runPlugin(String pluginName, String[] args, boolean runOnDedicatedThread, boolean suppressReturnedData) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String[] getCurrentlyDisplayedFiles() {
        return host.getCurrentlyDisplayedFiles();
    }
}
