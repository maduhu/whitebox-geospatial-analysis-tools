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
package whitebox.plugins.dialog;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
import java.awt.event.*;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.util.ArrayList;
import whitebox.interfaces.DialogComponent;
import whitebox.interfaces.Communicator;
import whitebox.structures.ExtensionFileFilter;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class DialogMultiFile extends JPanel implements ActionListener, DialogComponent {
   
    static final byte MODE_OPEN = 0;
    static final byte MODE_SAVEAS = 1;
    
    private int numArgs = 4;
    private String name;
    private String description;
    private ArrayList<String> value = new ArrayList<String>();
    private JLabel label;
    private JButton button = new JButton();
    private JButton delButton = new JButton();
    private JList list = new JList();
    private DefaultListModel model = new DefaultListModel();
    private String graphicsDirectory;
    private String workingDirectory;
    private String resourcesDirectory;
    private String pathSep;
    private ArrayList<ExtensionFileFilter> filters = new ArrayList<ExtensionFileFilter>();
    private Communicator hostDialog = null;
    
    public DialogMultiFile(Communicator host) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setMaximumSize(new Dimension(2500, 150));
        this.setPreferredSize(new Dimension(350, 150));
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
            list.setModel(model);
            JScrollPane scroller1 = new JScrollPane(list);
            box2.add(scroller1);
            String imgLocation = graphicsDirectory + "open.png";
            ImageIcon image = new ImageIcon(imgLocation, "");

            Box box3 = Box.createVerticalBox();
            //Create and initialize the buttons.
            button.setActionCommand("open");
            button.setToolTipText("Open File...");
            button.addActionListener(this);
            try {
                button.setIcon(image);
            } catch (Exception e) {
                button.setText("...");
            }
            box3.add(button);
            
            imgLocation = graphicsDirectory + "delete.png";
            image = new ImageIcon(imgLocation, "");
            delButton.setActionCommand("delete");
            delButton.setToolTipText("Delete Entry");
            delButton.addActionListener(this);
            try {
                delButton.setIcon(image);
            } catch (Exception e) {
                delButton.setText("del");
            }
            box3.add(delButton);
            
            box3.add(Box.createVerticalGlue());
            
            box2.add(box3);
            MouseListener ml = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        openFiles();
                    }
                }
            };
            list.addMouseListener(ml);
            this.add(box1);
            this.add(box2);
        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }
    
    public String getValue() {
        String ret = "";
        for (int a = 0; a < value.size(); a++) {
            ret = ret + value.get(a) + ";";
        }
        return ret;
    }
    
    public String getComponentName() {
        return name;
    }
    
    public boolean getOptionalStatus() {
        return false;
    }
    
    public boolean setArgs(String[] args) {
        try {
            // first make sure that there are the right number of args
            if (args.length != numArgs) {
                return false;
            }
            name = args[0];
            description = args[1];
            this.setToolTipText(description);
            list.setToolTipText(description);
            label = new JLabel(args[2]);
            setFilters(args[3]);
            
            createUI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public String[] getArgsDescriptors() {
        String[] argsDescriptors = new String[numArgs];
        argsDescriptors[0] = "String name";
        argsDescriptors[1] = "String description";
        argsDescriptors[2] = "String label";
        argsDescriptors[3] = "String fileFilter";
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
    
    private void openFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(true);
        fc.setAcceptAllFileFilterUsed(acceptAllFiles);
    
        for (int i = 0; i < filters.size(); i++) {
            fc.setFileFilter(filters.get(i));
        }
        
        workingDirectory = hostDialog.getWorkingDirectory();
        fc.setCurrentDirectory(new File(workingDirectory));
        int result = fc.showOpenDialog(this);
        File[] files = null;
        if(result == JFileChooser.APPROVE_OPTION) {
            files = fc.getSelectedFiles();
            String fileDirectory = files[0].getParentFile() + pathSep;
            if (!fileDirectory.equals(workingDirectory)) {
                hostDialog.setWorkingDirectory(fileDirectory);
            }
            String shortFileName;
            int j, k;
            for (int a = 0; a < files.length; a++) {
                this.value.add(files[a].toString());
                j = files[a].toString().lastIndexOf(pathSep);
                k = files[a].toString().lastIndexOf(".");
                shortFileName = files[a].toString().substring(j + 1, k);
                model.add(model.getSize(), shortFileName);
            }
            list.setModel(model);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("open")) {
            openFiles();
        } else if (actionCommand.equals("delete")) {
            int i = list.getSelectedIndex();
            value.remove(i);
            model.remove(i);
            list.setModel(model);
        }
    }
}
