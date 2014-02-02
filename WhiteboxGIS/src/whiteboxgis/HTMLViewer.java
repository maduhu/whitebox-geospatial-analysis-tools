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
package whiteboxgis;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ResourceBundle;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import whiteboxgis.user_interfaces.Scripter;
import whitebox.interfaces.WhiteboxPluginHost;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class HTMLViewer extends JFrame implements HyperlinkListener, ActionListener {

    private final ArrayList<String> helpHistory = new ArrayList<>();
    private int helpHistoryIndex = 0;
    JEditorPane helpPane = new JEditorPane();
    WhiteboxPluginHost host;

    public HTMLViewer(WhiteboxPluginHost host, String stringFileOrURL) throws Exception {

        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);

            System.setProperty("apple.laf.useScreenMenuBar", "true");
            //System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Whitebox GAT");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
            //System.setProperty("Xdock:name", "Whitebox");
            System.setProperty("apple.awt.fileDialogForDirectories", "true");

            System.setProperty("apple.awt.textantialiasing", "true");

            System.setProperty("apple.awt.graphics.EnableQ2DX", "true");
        }

        this.host = host;

        createGui(stringFileOrURL);
    }
    
    private void createGui(String stringFileOrURL) {
        try {
            helpPane.addHyperlinkListener(this);
            helpPane.setContentType("text/html");

            JScrollPane helpScroll = new JScrollPane(helpPane);
            this.getContentPane().add(helpScroll);

            if (stringFileOrURL.toLowerCase().endsWith("html")) {

                if (helpHistoryIndex == helpHistory.size() - 1) {
                    helpHistory.add(stringFileOrURL);
                    helpHistoryIndex = helpHistory.size() - 1;
                } else {
                    for (int i = helpHistory.size() - 1; i > helpHistoryIndex; i--) {
                        helpHistory.remove(i);
                    }
                    helpHistory.add(stringFileOrURL);
                    helpHistoryIndex = helpHistory.size() - 1;
                }
                try {
                    if (!stringFileOrURL.toLowerCase().startsWith("http://")) {
                        helpPane.setPage(new URL("file:///" + stringFileOrURL));
                    } else {
                        helpPane.setPage(new URL(stringFileOrURL));
                    }
                } catch (IOException e) {
                    System.err.println(e.getStackTrace());
                }

                this.setTitle("HTML Viewer: " + (new File(stringFileOrURL)).getName());
            } else {
                helpPane.setText(stringFileOrURL);
            }
            helpPane.setEditable(false);

            // text popup menu
            JPopupMenu textPopup = new JPopupMenu();

            JMenuItem mi;
//        mi = new JMenuItem("Clear");
//        mi.addActionListener(this);
//        mi.setActionCommand("clear");
//        textPopup.add(mi);
//
//        mi = new JMenuItem("Cut");
//        mi.addActionListener(this);
//        mi.setActionCommand("cut");
//        textPopup.add(mi);

            mi = new JMenuItem(host.getGuiLabelsBundle().getString("Copy"));
            mi.addActionListener(this);
            mi.setActionCommand("copy");
            textPopup.add(mi);

//        mi = new JMenuItem("Paste");
//        mi.addActionListener(this);
//        mi.setActionCommand("paste");
//        textPopup.add(mi);
            mi = new JMenuItem(host.getGuiLabelsBundle().getString("SelectAll"));
            mi.addActionListener(this);
            mi.setActionCommand("selectAll");
            textPopup.add(mi);

            textPopup.addSeparator();
            mi = new JMenuItem(host.getGuiLabelsBundle().getString("Save"));
            mi.addActionListener(this);
            mi.setActionCommand("save");
            textPopup.add(mi);
            
            mi = new JMenuItem(host.getGuiLabelsBundle().getString("SaveAs") + "...");
            mi.addActionListener(this);
            mi.setActionCommand("saveAs");
            textPopup.add(mi);
            
            mi = new JMenuItem(host.getGuiLabelsBundle().getString("Print"));
            mi.addActionListener(this);
            mi.setActionCommand("print");
            textPopup.add(mi);

            textPopup.setOpaque(true);
            textPopup.setLightWeightPopupEnabled(true);

            helpPane.setComponentPopupMenu(textPopup);
        } catch (Exception e) {
            host.logException("Error in HTMLViewer.", e);
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
                host.logException("Error in HTMLViewer.", ioe);
            }
        }
    }

    private void save() {
        if (fileName == null) {
            String extension = ".html";

            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
            fc.setAcceptAllFileFilterUsed(false);
            fc.setFileHidingEnabled(true);

            FileFilter ft = new FileNameExtensionFilter("HTML " + host.getGuiLabelsBundle().getString("Files"), "html");
            fc.addChoosableFileFilter(ft);

            fc.setCurrentDirectory(new File(host.getWorkingDirectory()));
            int result = fc.showSaveDialog(this);
            File file = null;
            if (result == JFileChooser.APPROVE_OPTION) {
                file = fc.getSelectedFile();
                // does the file contain an extension?
                if (!file.toString().endsWith(extension)) {
                    file = new File(file.toString() + extension);
                }
                if (file.exists()) {
                    Object[] options = {"Yes", "No"};
                    int n = JOptionPane.showOptionDialog(this,
                            host.getMessageBundle().getString("FileExists") + "\n"
                            + host.getMessageBundle().getString("Overwrite"),
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
                fileName = file.toString();

                this.setTitle(new File(fileName).getName());
            } else {
                return;
            }
        }

        File file = new File(fileName);
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        try {
            fw = new FileWriter(file, false);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw, true);

            out.print(helpPane.getText());

            bw.close();
            fw.close();

        } catch (java.io.IOException e) {
            host.logException("Error in HTMLViewer.", e);
        } catch (Exception e) { //Catch exception if any
            host.logException("Error in HTMLViewer.", e);
        } finally {
            if (out != null || bw != null) {
                out.flush();
                out.close();
            }
        }
    }

    String fileName;

    private void saveAs() {
        fileName = null;
        save();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        switch (actionCommand.toLowerCase()) {
            case "print":
                try {
                    helpPane.print();
                } catch (PrinterException pe) {
                    host.logException("Error in HTMLViewer.", pe);
                }
                break;

            case "save":
                save();
                break;
            case "saveas":
                saveAs();
                break;
            case "selectall":
                helpPane.selectAll();
                break;
            case "copy":
                helpPane.copy();
                break;
            case "paste":
                helpPane.paste();
                break;
            case "cut":
                helpPane.cut();
                break;
            case "clear":
                helpPane.setText("");
                break;
        }
    }
}
