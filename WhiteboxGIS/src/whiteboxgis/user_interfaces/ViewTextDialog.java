/*
 * Copyright (C) 2014 Dr. John Lindsay <jlindsay@uoguelph.ca>
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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import whitebox.interfaces.WhiteboxPluginHost;
import static whiteboxgis.WhiteboxGui.logger;

/**
 *
 * @author johnlindsay
 */
public class ViewTextDialog extends JDialog implements ActionListener, KeyListener {

    private JTextArea textArea = new JTextArea();
    private JPopupMenu textPopup = new JPopupMenu();
    private ResourceBundle bundle;
    private WhiteboxPluginHost host = null;
    private JCheckBoxMenuItem wordWrap;
    private JCheckBoxMenuItem wordWrap2;
    private String currentTextFile = "";
    private String workingDirectory;
    private boolean editorDirty = false;

    public ViewTextDialog(Frame owner, boolean modal) {
        super(owner, modal);

        if (owner != null && owner instanceof WhiteboxPluginHost) {
            host = (WhiteboxPluginHost) owner;
            bundle = host.getGuiLabelsBundle();
            workingDirectory = host.getWorkingDirectory();
        }

        createUI();

    }

    private void createUI() {
        this.setTitle("Whitebox GAT - Text Viewer");

        textArea.setLineWrap(false);
        textArea.setWrapStyleWord(false);
        textArea.setFont(new Font(host.getDefaultFont().getName(), Font.PLAIN, 12));

        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                textAreaMousePress(e);
            }
        });

        Container c = getContentPane();

        JScrollPane scroller = new JScrollPane(textArea);
        scroller.setPreferredSize(new Dimension(500, 450));

        this.setPreferredSize(new Dimension(500, 450));

        c.add(scroller, BorderLayout.CENTER);
        pack();

        createMenu();

        createPopupMenu();
    }

    private void createMenu() {
        try {
            JMenuBar menubar = new JMenuBar();

            menubar.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    textAreaMousePress(e);
                }
            });

            JMenu fileMenu = new JMenu(bundle.getString("File"));
            JMenuItem open = new JMenuItem(bundle.getString("Open"));
            open.setActionCommand("openText");
            open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            open.addActionListener(this);
            fileMenu.add(open);
            JMenuItem save = new JMenuItem(bundle.getString("Save"));
            save.setActionCommand("saveText");
            save.addActionListener(this);
            save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            fileMenu.add(save);
            JMenuItem saveAs = new JMenuItem(bundle.getString("SaveAs") + "...");
            saveAs.setActionCommand("saveAs");
            saveAs.addActionListener(this);
            fileMenu.add(saveAs);
            JMenuItem print = new JMenuItem(bundle.getString("Print"));
            print.setActionCommand("print");
            print.addActionListener(this);
            print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            fileMenu.add(print);
            JMenuItem close = new JMenuItem(bundle.getString("CloseFile"));
            close.setActionCommand("closeText");
            close.addActionListener(this);
            fileMenu.add(close);

            fileMenu.addSeparator();

            JMenuItem exit = new JMenuItem(bundle.getString("Exit"));
            exit.setActionCommand("exit");
            exit.addActionListener(this);
            exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            fileMenu.add(exit);

            menubar.add(fileMenu);

            JMenu editMenu = new JMenu(bundle.getString("Edit"));

            JMenuItem mi = new JMenuItem(bundle.getString("Clear"));
            mi.addActionListener(this);
            mi.setActionCommand("clearText");
            editMenu.add(mi);

            JMenuItem cut = new JMenuItem(bundle.getString("Cut"));
            cut.setActionCommand("cutText");
            cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            cut.addActionListener(this);
            editMenu.add(cut);

            JMenuItem copy = new JMenuItem(bundle.getString("Copy"));
            copy.setActionCommand("copyText");
            copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            copy.addActionListener(this);
            editMenu.add(copy);

            JMenuItem paste = new JMenuItem(bundle.getString("Paste"));
            paste.setActionCommand("pasteText");
            paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            paste.addActionListener(this);
            editMenu.add(paste);

            JMenuItem selectAll = new JMenuItem("Select All");
            selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            selectAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    textArea.selectAll();
                }
            });
            editMenu.add(selectAll);

            editMenu.addSeparator();
            wordWrap2 = new JCheckBoxMenuItem(bundle.getString("WordWrap"));
            wordWrap2.addActionListener(this);
            wordWrap2.setActionCommand("wordWrap2");
            wordWrap2.setState(false);
            editMenu.add(wordWrap2);

            menubar.add(editMenu);

            this.setJMenuBar(menubar);

        } catch (Exception e) {
            host.logException("Error in Scripter.", e);
        }
    }

    private void createPopupMenu() {
        // text popup menu
        textPopup = new JPopupMenu();

        JMenuItem mi = new JMenuItem(bundle.getString("Open"));
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

    public void setText(String text) {
        textArea.setText(text);
    }

    private void saveAs() {
        currentTextFile = null;
        saveText();
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
        file.delete();
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

            editorDirty = false;
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
            String fileDirectory = file.getParentFile() + File.separator;
            if (!fileDirectory.equals(workingDirectory)) {
                host.setWorkingDirectory(fileDirectory);
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
            }
        }

    }

    private void print() {
        try {
            textArea.print();
        } catch (Exception e) {
            host.logException("Error in Scripter.", e);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand().toLowerCase()) {
            case "print":
                print();
                break;
            case "exit":
                if (editorDirty) {
                    Object[] options = {"Yes", "No", "Cancel"};
                    int n = JOptionPane.showOptionDialog(this,
                            "The text has changed. Would you like to save it?",
                            "Whitebox GAT Message",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null, //do not use a custom Icon
                            options, //the titles of buttons
                            options[0]); //default button title

                    if (n == JOptionPane.YES_OPTION) {
                        saveText();
                    } else if (n == JOptionPane.NO_OPTION) {
                        // do nothing
                    } else if (n == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                }
                this.dispose();
                break;
            case "selectalltext":
                textArea.selectAll();
                break;
            case "copytext":
                textArea.copy();
                break;
            case "pastetext":
                textArea.paste();
                break;
            case "cuttext":
                textArea.cut();
                break;
            case "cleartext":
                textArea.setText("");
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
            case "opentext":
                openText();
                break;
            case "savetext":
                saveText();
                break;
            case "saveas":
                saveAs();
                break;
            case "closetext":
                textArea.setText("");
                currentTextFile = null;
                break;
            case "wordwrap":
                textArea.setLineWrap(wordWrap.getState());
                textArea.setWrapStyleWord(wordWrap.getState());
                wordWrap2.setState(wordWrap.getState());
                break;
            case "wordwrap2":
                textArea.setLineWrap(wordWrap2.getState());
                textArea.setWrapStyleWord(wordWrap2.getState());
                wordWrap.setState(wordWrap2.getState());
                break;
        }
    }

    private void textAreaMousePress(MouseEvent e) {
        if (e.getButton() == 3 || e.isPopupTrigger()) {
            textPopup.show((JComponent) e.getSource(), e.getX(), e.getY());
        }
    }

    public static void main(String args[]) {
        ViewTextDialog vtd = new ViewTextDialog(null, false);
        vtd.setVisible(true);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {
        try {
            if (e.getKeyCode() != KeyEvent.VK_UP
                    && e.getKeyCode() != KeyEvent.VK_DOWN
                    && e.getKeyCode() != KeyEvent.VK_LEFT
                    && e.getKeyCode() != KeyEvent.VK_RIGHT) {
                editorDirty = true;
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

}
