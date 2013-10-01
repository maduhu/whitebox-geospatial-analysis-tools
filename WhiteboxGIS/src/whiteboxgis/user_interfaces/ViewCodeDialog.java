/*
 * Copyright (C) 2011-2012 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

import java.awt.Container;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
//import jsyntaxpane.DefaultSyntaxKit;
import whitebox.interfaces.Communicator;
import whitebox.utilities.FileUtilities;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class ViewCodeDialog extends JDialog implements ActionListener {

    private String pathSep;
    private String resourcesDirectory;
    private String sourceFile;
    private String pluginName;
    private Communicator host = null;
    //private JEditorPane editor = new JEditorPane();
    //private JScrollPane scroll = new JScrollPane();
    private RSyntaxTextArea editor = new RSyntaxTextArea();
    private RTextScrollPane scroll;
    private boolean editable = false;
    private String fileExtension = "java";

    public ViewCodeDialog(Frame owner, boolean modal, String pluginName, String title) {
        super(owner, modal);
        host = (Communicator) owner;
        this.pathSep = File.separator;
        this.resourcesDirectory = host.getResourcesDirectory();
        this.pluginName = pluginName;
        File sourceFileDir = new File(resourcesDirectory + "plugins"
                + pathSep + "source_files");
        findSourceFile(sourceFileDir);

        if (!(new File(this.sourceFile).exists())) {
            host.showFeedback("The tool's source file could not be located.");
        }

        createGui(title);
    }

    public ViewCodeDialog(Frame owner, boolean modal, File fileName, boolean editable) {
        super(owner, modal);
        host = (Communicator) owner;
        this.sourceFile = fileName.toString();

        if (!(new File(this.sourceFile).exists())) {
            host.showFeedback("The tool's source file could not be located.");
        }

        this.editable = editable;

        createGui(fileName.getName());
    }

    private void createGui(String title) {
        if (System.getProperty("os.name").contains("Mac")) {
            this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
        }

        this.setTitle(title);

        // find the file extension
        fileExtension = FileUtilities.getFileExtension(sourceFile);

        editor.setCodeFoldingEnabled(true);
        editor.setAntiAliasingEnabled(true);
        editor.setCloseCurlyBraces(true);
        editor.setBracketMatchingEnabled(true);
        editor.setAutoIndentEnabled(true);
        editor.setMarkOccurrences(true);
        editor.setCloseMarkupTags(true);
        scroll = new RTextScrollPane(editor);
        scroll.setFoldIndicatorEnabled(true);

        //scroll = new JScrollPane(editor);
//        this.getContentPane().add(scroll);
        Container c = this.getContentPane();
        c.add(scroll);
        c.doLayout();
        //        DefaultSyntaxKit.initKit();
        switch (fileExtension.toLowerCase()) {
            case "java":
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                //editor.setContentType("text/java");
                break;
            case "html":
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
                //editor.setContentType("text/xhtml");
                break;
            case "py":
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
                break;
            case "groovy":
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
                break;
            case "js":
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                break;
            case "scala":
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SCALA);
                break;
            default:
                // does not support this type of file.
                host.showFeedback("Unsupported file type.");
                return;
        }
//        editor.setEditable(true);

        DataInputStream in = null;
        BufferedReader br = null;
        try {
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(this.sourceFile);
            // Get the object of DataInputStream
            in = new DataInputStream(fstream);

            br = new BufferedReader(new InputStreamReader(in));
            String line;
            String str = "";

            if (this.sourceFile != null) {
                //Read File Line By Line
                while ((line = br.readLine()) != null) {
                    str += line + "\n";
                }
            }
            editor.setText(str);
        } catch (Exception e) {
        }

        editor.setEditable(editable);
        editor.setCaretPosition(0);

        createMenu();
    }

    private void createMenu() {
        try {
            JMenuBar menubar = new JMenuBar();
            JMenu FileMenu = new JMenu("File");

//            JMenuItem open = new JMenuItem("Open");
//            FileMenu.add(open);
//            open.setActionCommand("open");
//            open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, 
//                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
//            open.addActionListener(this);

            JMenuItem save = new JMenuItem("Save");
            FileMenu.add(save);
            save.setActionCommand("save");
            save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            save.addActionListener(this);
            save.setVisible(editable);

            JMenuItem saveAs = new JMenuItem("Save As");
            FileMenu.add(saveAs);
            saveAs.setActionCommand("saveAs");
            saveAs.addActionListener(this);
            saveAs.setVisible(editable);

            JMenuItem close = new JMenuItem("Quit");
            FileMenu.add(close);
            close.setActionCommand("quit");
            close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            close.addActionListener(this);

            menubar.add(FileMenu);

            this.setJMenuBar(menubar);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void findSourceFile(File dir) {
        File[] files = dir.listFiles();
        for (int x = 0; x < files.length; x++) {
            if (files[x].isDirectory()) {
                findSourceFile(files[x]);
            } else if (files[x].toString().contains(pathSep + pluginName + ".java")) {
                sourceFile = files[x].toString();
                break;
            }
        }
    }

    private void saveAs() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(true);
        fc.setFileHidingEnabled(true);

        FileFilter ft = new FileNameExtensionFilter("Text Files", "txt");
        fc.addChoosableFileFilter(ft);
        ft = new FileNameExtensionFilter("Whitebox Raster Files", "dep");
        fc.addChoosableFileFilter(ft);

        fc.setCurrentDirectory(new File(host.getWorkingDirectory()));
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
                } else if (n == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            sourceFile = file.toString();
        } else {
            return;
        }

        file = new File(sourceFile);
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        try {
            fw = new FileWriter(file, false);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw, true);

            out.print(editor.getText());

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

    private void save() {
        if (sourceFile == null) {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
            fc.setAcceptAllFileFilterUsed(true);
            fc.setFileHidingEnabled(true);

            FileFilter ft = new FileNameExtensionFilter("Text Files", "txt");
            fc.addChoosableFileFilter(ft);
            ft = new FileNameExtensionFilter("Whitebox Raster Files", "dep");
            fc.addChoosableFileFilter(ft);

            fc.setCurrentDirectory(new File(host.getWorkingDirectory()));
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
                    } else if (n == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
                sourceFile = file.toString();
            } else {
                return;
            }
        }

        File file = new File(sourceFile);
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        try {
            fw = new FileWriter(file, false);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw, true);

            out.print(editor.getText());

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

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("quit")) {
            this.dispose();
        } else if (actionCommand.equals("save")) {
            if (editable) {
                save();
            }
        } else if (actionCommand.equals("saveAs")) {
            if (editable) {
                saveAs();
            }
        } else if (actionCommand.equals("open")) {
        }
    }
}
