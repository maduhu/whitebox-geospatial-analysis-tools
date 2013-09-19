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
package whitebox.ui.plugin_dialog;

import java.awt.event.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import whitebox.interfaces.Communicator;
import whitebox.interfaces.DialogComponent;
import whitebox.structures.ExtensionFileFilter;
import org.fife.ui.autocomplete.*;
import whitebox.utilities.FileUtilities;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class DialogFile extends JPanel implements ActionListener, DialogComponent,
        PropertyChangeListener {

    static final byte MODE_OPEN = 0;
    static final byte MODE_SAVEAS = 1;
    private int numArgs = 7;
    private String name;
    private String description;
    private boolean makeOptional = false;
    private String value = "";
    private byte mode;
    private JLabel label;
    private JButton button = new JButton();
    private JTextField text = new JTextField(25);
    private boolean showButton = true;
    private String graphicsDirectory;
    private String workingDirectory;
    private String resourcesDirectory;
    private String pathSep;
    private ArrayList<ExtensionFileFilter> filters = new ArrayList<>();
    private Communicator hostDialog = null;
    private AutoCompletion ac;
    private String spaces = "    ";

    /**
     * Initialization method
     *
     * @param host Communicator
     */
    public DialogFile() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setMaximumSize(new Dimension(2500, 50));
        this.setPreferredSize(new Dimension(350, 50));
        resourcesDirectory = hostDialog.getResourcesDirectory();
        pathSep = File.separator;
        graphicsDirectory = resourcesDirectory + "Images" + pathSep;
        workingDirectory = hostDialog.getWorkingDirectory();

    }

    public DialogFile(Communicator host) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setMaximumSize(new Dimension(2500, 50));
        this.setPreferredSize(new Dimension(350, 50));
        hostDialog = host;
        resourcesDirectory = hostDialog.getResourcesDirectory();
        pathSep = File.separator;
        graphicsDirectory = resourcesDirectory + "Images" + pathSep;
        workingDirectory = hostDialog.getWorkingDirectory();

    }

    private void createUI() {
        try {
            Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            this.setBorder(border);
            Box box1 = Box.createHorizontalBox();
            box1.add(label);
            box1.add(Box.createHorizontalGlue());
            Box box2 = Box.createHorizontalBox();
            text.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                    text.getPreferredSize().height));
            
            if (this.mode == MODE_OPEN) {
                spaces = "    ";
                JButton dropDownBtn = makeToolBarButton("GetOpenLayers.png", "GetOpenLayers",
                        "Displayed Layers", "GetOpenLayers");
                dropDownBtn.setBorderPainted(false);
                dropDownBtn.setFocusPainted(false);
                dropDownBtn.setContentAreaFilled(false);
                dropDownBtn.setMargin(new Insets(0, 0, 0, 0));
                dropDownBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int numLayers = getOpenLayers();
                        if (numLayers > 0 && ac != null) {
                            text.setText("");
                            ac.doCompletion();
                        }
                    }
                });
                JPanel dropDownPanel = new JPanel(new BorderLayout());
                dropDownPanel.setBackground(Color.white);
                dropDownPanel.setPreferredSize(new Dimension(14, 0));
//                dropDownPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                dropDownPanel.add(dropDownBtn, BorderLayout.CENTER);
                text.setLayout(new BorderLayout());
                text.add(dropDownPanel, BorderLayout.EAST);
                box2.add(text);
            } else {
                spaces = "";
                box2.add(text);
            }

            if (showButton) {
                String imgLocation = graphicsDirectory + "open.png";
                ImageIcon image = new ImageIcon(imgLocation, "");

                //Create and initialize the button.
                if (this.mode == MODE_OPEN) {
                    button.setActionCommand("open");
                    button.setToolTipText("Open File...");
                } else {
                    button.setActionCommand("save");
                    button.setToolTipText("Save File As...");
                }
                button.addActionListener(this);
                try {
                    button.setIcon(image);
                } catch (Exception e) {
                    button.setText("...");
                }
                box2.add(button);
            }

            MouseListener ml = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        if (mode == MODE_OPEN) {
                            openFile();
                        }
                        if (mode == MODE_SAVEAS) {
                            saveFile();
                        }
                    }
                }
            };
            text.addMouseListener(ml);

            text.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void changedUpdate(DocumentEvent e) {
                    update();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    update();
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    update();
                }

                public void update() {
                    String oldValue = value;
                    value = text.getText();
                    firePropertyChange("value", oldValue, value);
                }
            });

//            if (mode == MODE_OPEN) {
//            }

            this.add(box1);
            this.add(box2);
            //if (System.getProperty("os.name").contains("Mac")) {
            //    this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
            //}
        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }

    private int getOpenLayers() {
        int ret = 0;
        DefaultCompletionProvider provider = new DefaultCompletionProvider();
        String[] displayedFiles = hostDialog.getCurrentlyDisplayedFiles();
        for (int i = 0; i < displayedFiles.length; i++) {
            if (isFileOfAllowableType(displayedFiles[i])) {
                String fileName = FileUtilities.getShortFileName(displayedFiles[i]);
                provider.addCompletion(new ShorthandCompletion(provider, fileName,
                        displayedFiles[i] + spaces, displayedFiles[i]));
                ret++;
            }
        }

        ac = new AutoCompletion(provider);
        ac.setAutoCompleteEnabled(true);
        ac.setAutoActivationEnabled(true);
        ac.setShowDescWindow(false);
        ac.setParameterAssistanceEnabled(false);
        ac.install(text);
        return ret;
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
        if (!(new File(imgLocation).exists())) {
            button.setText(altText);
            return button;
        }
        button.setOpaque(false);
        button.setBorderPainted(false);

        try {
            button.setIcon(image);
        } catch (Exception e) {
            button.setText(altText);
            System.out.println(e.getMessage());
        }

        return button;
    }

    private boolean isFileOfAllowableType(String fileName) {
        String myExtension = FileUtilities.getFileExtension(fileName);
        for (ExtensionFileFilter filter : filters) {
            String[] extensions = filter.getExtensions();
            for (int i = 0; i < extensions.length; i++) {
                if (extensions[i].toLowerCase().endsWith(myExtension)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getValue() {
        if (!value.equals(text.getText())) {
            value = text.getText();
        }
        // see if there is a directory attached to the value
        if (!value.contains(pathSep) && !value.equals("")) {
            value = workingDirectory + value;
        }
        // see if there is a file extension attached to the value
        boolean flag = false;
        for (int i = 0; i < filters.size(); i++) {
            for (int j = 0; j < filters.get(i).getExtensions().length; j++) {
                if (value.toLowerCase().contains("." + filters.get(i).getExtensions()[j])) {
                    flag = true;
                }
            }
        }
        if (!flag && !value.equals("")) {
            value = value + "." + filters.get(0).getExtensions()[0];
        }
        if (!value.trim().equals("")) {
            File file = new File(value.trim());
            if (mode == MODE_SAVEAS && file.exists()) {
                int n = hostDialog.showFeedback("The file already exists.\n"
                        + "Would you like to overwrite it?", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (n == JOptionPane.YES_OPTION) {
                    file.delete();
                    new File(file.toString().replace(".dep", ".tas")).delete();
                } else if (n == JOptionPane.NO_OPTION) {
                    return null;
                }
            }
            return value.trim();
        } else {
            if (makeOptional) {
                return "not specified";
            }
        }
        return null;
    }

    @Override
    public String getComponentName() {
        return name;
    }

    @Override
    public boolean getOptionalStatus() {
        return makeOptional;
    }

    @Override
    public boolean setArgs(String[] args) {
        try {
            // first make sure that there are the right number of args
            if (args.length != numArgs) {
                return false;
            }
            name = args[0];
            description = args[1];
            this.setToolTipText(description);
            text.setToolTipText(description);
            label = new JLabel(args[2]);
            mode = Byte.parseByte(args[3]);
            showButton = Boolean.parseBoolean(args[4]);
            setFilters(args[5]);
            makeOptional = Boolean.parseBoolean(args[6]);

            createUI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void setTextFieldActionListener(ActionListener al) {
        text.addActionListener(al);
    }

    @Override
    public String[] getArgsDescriptors() {
        String[] argsDescriptors = new String[numArgs];
        argsDescriptors[0] = "String name";
        argsDescriptors[1] = "String description";
        argsDescriptors[2] = "String label";
        argsDescriptors[3] = "byte mode (MODE_OPEN or MODE_SAVEAS)";
        argsDescriptors[4] = "boolean showButton";
        argsDescriptors[5] = "String fileFilter";
        argsDescriptors[6] = "boolean makeOptional";
        return argsDescriptors;
    }
    boolean acceptAllFiles = false;

    private void setFilters(String filterStr) {
        try {
            // filters are delimited by a pipe '|'
            String[] str1 = filterStr.split("\\|");
            String filterDescription;
            for (int i = 0; i < str1.length; i++) {
                // the description and extension(s) are delimited by commas.
                String[] str2 = str1[i].split(",");

                filterDescription = str2[0].trim();

                if (!filterDescription.toLowerCase().contains("all files")) {
                    String[] extensions = new String[str2.length - 1];
                    for (int j = 1; j < str2.length; j++) {
                        extensions[j - 1] = str2[j].trim();
                    }

                    ExtensionFileFilter eff = new ExtensionFileFilter(filterDescription, extensions);

                    filters.add(eff);
                } else {
                    acceptAllFiles = true;
                }
            }
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    private void openFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(acceptAllFiles);

        for (int i = 0; i < filters.size(); i++) {
            fc.setFileFilter(filters.get(i));
        }

        workingDirectory = hostDialog.getWorkingDirectory();
        fc.setCurrentDirectory(new File(workingDirectory));
        int result = fc.showOpenDialog(this);
        File file = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            String fileDirectory = file.getParentFile() + pathSep;
            if (!fileDirectory.equals(workingDirectory)) {
                hostDialog.setWorkingDirectory(fileDirectory);
            }
            text.setText(file.toString() + spaces);
            String oldValue = this.value;
            this.value = file.toString();
            firePropertyChange("value", oldValue, value);
        }
    }

    private void saveFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        workingDirectory = hostDialog.getWorkingDirectory();
        fc.setCurrentDirectory(new File(workingDirectory));
        fc.setAcceptAllFileFilterUsed(false);

        for (int i = 0; i < filters.size(); i++) {
            fc.setFileFilter(filters.get(i));
        }

        int result = fc.showSaveDialog(this);
        File file = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            // see if file has an extension.
            int dot = file.toString().lastIndexOf(".");
            if (dot == -1) {
                String fileStr = file.toString() + "." + filters.get(0).getExtensions()[0];
                file = new File(fileStr);
            }

            String fileDirectory = file.getParentFile() + pathSep;
            if (!fileDirectory.equals(workingDirectory)) {
                hostDialog.setWorkingDirectory(fileDirectory);
            }

            // see if the file exists already, and if so, should it be overwritten?
            if (file.exists()) {
                int n = hostDialog.showFeedback("The file already exists.\n"
                        + "Would you like to overwrite it?", JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (n == JOptionPane.YES_OPTION) {
                    file.delete();
                    new File(file.toString().replace(".dep", ".tas")).delete();
                } else if (n == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            text.setText(file.toString() + spaces);

            String oldValue = this.value;
            this.value = file.toString();
            firePropertyChange("value", oldValue, value);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("open")) {
            openFile();
        } else if (actionCommand.equals("save")) {
            saveFile();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
