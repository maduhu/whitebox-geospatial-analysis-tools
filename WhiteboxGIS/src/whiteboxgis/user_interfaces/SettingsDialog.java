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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.*;
import whitebox.interfaces.Communicator;
import whitebox.interfaces.DialogComponent;
import whitebox.utilities.FileUtilities;
import whitebox.ui.carto_properties.*;
import whiteboxgis.WhiteboxGui;

/**
 *
 * @author johnlindsay
 */
public class SettingsDialog extends JDialog implements Communicator, ActionListener, PropertyChangeListener {

    private int rightMargin = 10;
    private int leftMargin = 10;
    private Color backColour = new Color(225, 245, 255);
    private JButton ok = new JButton("OK");
    private JButton close = new JButton("Close");
    private String pathSep = "";
    private WhiteboxGui host = null;
    private String applicationDirectory = "";
    private String resourcesDirectory = "";
    private NumericProperty printResolution;
    private BooleanProperty autoHideAlignToolbar;

    public SettingsDialog(Frame owner, boolean modal) {
        super(owner, modal);
        pathSep = File.separator;
        host = (WhiteboxGui) owner;
        applicationDirectory = host.getApplicationDirectory();
        resourcesDirectory = host.getResourcesDirectory();
        createGui();
    }

    private void createGui() {
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }

        this.setTitle("Whitebox Settings");

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


        box2.add(Box.createHorizontalGlue());

        this.getContentPane().add(box2, BorderLayout.SOUTH);



        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(Color.WHITE);

        Box mainBox = Box.createVerticalBox();
        int preferredWidth = 410;

        // print resolution
        printResolution = new NumericProperty("Print resolution:", String.valueOf(host.getPrintResolution()));
        printResolution.setLeftMargin(leftMargin);
        printResolution.setRightMargin(rightMargin);
        printResolution.setBackColour(backColour);
        printResolution.setPreferredWidth(preferredWidth);
        printResolution.setParseIntegersOnly(true);
        printResolution.setTextboxWidth(5);
        printResolution.revalidate();
        printResolution.addPropertyChangeListener("value", this);
        mainBox.add(printResolution);

        autoHideAlignToolbar = new BooleanProperty("Automatically hide align toolbar?",
                host.isHideAlignToolbar());
        autoHideAlignToolbar.setLeftMargin(leftMargin);
        autoHideAlignToolbar.setRightMargin(rightMargin);
        autoHideAlignToolbar.setBackColour(Color.WHITE);
        autoHideAlignToolbar.setPreferredWidth(preferredWidth);
        autoHideAlignToolbar.revalidate();
        autoHideAlignToolbar.addPropertyChangeListener("value", this);
        mainBox.add(autoHideAlignToolbar);

        mainPanel.add(mainBox);

        this.getContentPane().add(mainPanel, BorderLayout.CENTER);

        pack();

        // Centre the dialog on the screen.
        // Get the size of the screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = dim.height;
        int screenWidth = dim.width;
        int myWidth = 800; //this.getWidth();
        int myHeight = 400; //this.getHeight();
        setLocation((int) (screenWidth / 2.0 - myWidth / 2.0), (int) (screenHeight / 2.0 - myHeight / 2.0));
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
        if (source == printResolution) {
            host.setPrintResolution(Integer.parseInt((String) evt.getNewValue()));
        } else if (source == autoHideAlignToolbar) {
            host.setHideAlignToolbar((Boolean) evt.getNewValue());
        }
    }
}
