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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import whitebox.interfaces.Communicator;
import whitebox.utilities.FileUtilities;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class Help extends JDialog implements ActionListener, HyperlinkListener {

    private JButton viewSource = new JButton("View HTML Source");
    private JButton close = new JButton("Close");
    private JButton back = new JButton();
    private JButton forward = new JButton();
    private String helpFile = "";
    //private FramePanel helpPane;
    private JEditorPane helpPane = new JEditorPane();
    private String graphicsDirectory = "";
    private String helpDirectory = "";
    private String resourcesDirectory = "";
    private String pathSep = "";
    private Communicator host = null;
    private String title = "Whitebox Help";
    private JTextField indexField = new JTextField();
    private static JList availableHelpFiles;
    private JTextField searchField = new JTextField();
    private static JList searchOutput;
    private String[][] helpFiles;
    private int[][] searchCounts;
    private String activeTab = "index";
    private ArrayList<String> helpHistory = new ArrayList<String>();
    private int helpHistoryIndex = 0;

    public Help(Frame owner, boolean modal, String startMode) {
        super(owner, modal);
        pathSep = File.separator;
        host = (Communicator) owner;
        resourcesDirectory = host.getResourcesDirectory();
        helpDirectory = resourcesDirectory + "Help" + pathSep;
        graphicsDirectory = resourcesDirectory + "Images" + pathSep;
        helpFile = helpDirectory + "Welcome.html";
        activeTab = startMode;
        createGui();
    }

    public Help(Frame owner, boolean modal, String startMode, String helpFile) {
        super(owner, modal);
        pathSep = File.separator;
        host = (Communicator) owner;
        this.helpFile = helpFile;
        resourcesDirectory = host.getResourcesDirectory();
        helpDirectory = resourcesDirectory + "Help" + pathSep;
        graphicsDirectory = resourcesDirectory + "Images" + pathSep;
        activeTab = startMode;
        createGui();
    }
    
    public Help() {
        // this is really not to be used by anything other than the 'main' method for testing.
        try {
            pathSep = File.separator;

            String applicationDirectory = java.net.URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
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
            graphicsDirectory = resourcesDirectory + "Images" + pathSep;
            helpFile = helpDirectory + "Welcome.html";
        } catch (Exception e) {

        }
    }

    private void createGui() {
        try {
            if (System.getProperty("os.name").contains("Mac")) {
                this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
            }
            this.setPreferredSize(new Dimension(950, 700));

            JTabbedPane sidebarPane = createSidePanel();

            String imgLocation = null;
            ImageIcon image = null;

            helpPane.addHyperlinkListener(this);
            helpPane.setContentType("text/html");
            JScrollPane helpScroll = new JScrollPane(helpPane);

            //helpPane = new FramePanel();

            Box box2 = Box.createHorizontalBox();
            box2.add(Box.createRigidArea(new Dimension(10, 30)));
            box2.add(close);
            close.setActionCommand("close");
            close.addActionListener(this);
            box2.add(Box.createHorizontalGlue());
            box2.add(viewSource);
            viewSource.setActionCommand("viewSource");
            viewSource.addActionListener(this);
            box2.add(Box.createHorizontalStrut(5));
            //box2.add(Box.createHorizontalGlue());

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

            JSplitPane splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPane, helpScroll);
            splitter.setDividerLocation(270);
            splitter.setResizeWeight(0.0);
            splitter.setDividerSize(1);

            this.getContentPane().add(splitter, BorderLayout.CENTER);
            this.getContentPane().add(box2, BorderLayout.SOUTH);


            File hlp = new File(helpFile);

            if (!hlp.exists()) {
                // use the NoHelp.html file.
                helpFile = resourcesDirectory + "Help" + pathSep + "NoHelp.html";
            }
            if (helpHistoryIndex == helpHistory.size() - 1) {
                helpHistory.add(helpFile);
                helpHistoryIndex = helpHistory.size() - 1;
            } else {
                for (int i = helpHistory.size() - 1; i > helpHistoryIndex; i--) {
                    helpHistory.remove(i);
                }
                helpHistory.add(helpFile);
                helpHistoryIndex = helpHistory.size() - 1;
            }
            helpPane.setPage(new URL("file:///" + helpFile));
            //helpPane.navigate(helpFile);

            createPopupMenus();
//            helpPane.addMouseListener(new MouseAdapter() {
//
//                @Override
//                public void mousePressed(MouseEvent e) {
//                    helpMousePress(e);
//                }
//            });

            setTitle(title);

            pack();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private JTabbedPane createSidePanel() {
        JTabbedPane sidebarTab = new JTabbedPane();
        JPanel indexPanel = new JPanel();
        JPanel searchPanel = new JPanel();
        try {
            // create the list of available help files.
            findAvailableHelpFiles();

            // add them to a list
            availableHelpFiles = new JList();
            availableHelpFiles.removeAll();
            DefaultListModel model = new DefaultListModel();
            for (int i = 0; i < helpFiles.length; i++) {
                model.add(i, helpFiles[i][1]);
            }
            availableHelpFiles.setModel(model);

            MouseListener ml = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    JList theList = (JList) e.getSource();
                    String label = null;
                    int index = theList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        Object o = theList.getModel().getElementAt(index);
                        label = o.toString();
                    }
                    if (e.getClickCount() == 1 && e.getButton() == 1) {
                        try {
                            helpFile = helpFiles[index][0];
                            if (helpHistoryIndex == helpHistory.size() - 1) {
                                helpHistory.add(helpFile);
                                helpHistoryIndex = helpHistory.size() - 1;
                            } else {
                                for (int i = helpHistory.size() - 1; i > helpHistoryIndex; i--) {
                                    helpHistory.remove(i);
                                }
                                helpHistory.add(helpFile);
                                helpHistoryIndex = helpHistory.size() - 1;
                            }
                            helpPane.setPage(new URL("file:///" + helpFile));
                            //helpPane.navigate(helpFile);
                        } catch (MalformedURLException me) {
                        } catch (IOException ioe) {
                        }
                    }
                }
            };
            availableHelpFiles.addMouseListener(ml);
            JScrollPane scroller1 = new JScrollPane(availableHelpFiles);

            indexField.setMaximumSize(new Dimension(500, 20));
            indexField.addKeyListener(new indexFieldKeyListener());
            indexField.addActionListener(indexFieldListener);
            indexPanel.setLayout(new BoxLayout(indexPanel, BoxLayout.Y_AXIS));
            indexPanel.add(indexField);
            indexPanel.add(Box.createVerticalStrut(10));
            indexPanel.add(scroller1);
            sidebarTab.add(indexPanel, "Index");


            searchField.setMaximumSize(new Dimension(500, 20));
            searchField.addActionListener(searchFieldListener);
            searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.Y_AXIS));
            searchPanel.add(searchField);
            searchPanel.add(Box.createVerticalStrut(10));
            searchOutput = new JList();
            JScrollPane scroller2 = new JScrollPane(searchOutput);
            searchPanel.add(scroller2);
            sidebarTab.add(searchPanel, "Search");

            if (activeTab.toLowerCase().equals("index")) {
                sidebarTab.setSelectedIndex(0);
            } else if (activeTab.toLowerCase().equals("search")) {
                sidebarTab.setSelectedIndex(1);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            return sidebarTab;
        }
    }
    private JPopupMenu helpPopup;

    private void createPopupMenus() {
        helpPopup = new JPopupMenu();

        JMenuItem mi = new JMenuItem("View Help File Source");
        mi.addActionListener(this);
        mi.setActionCommand("viewHelpFileSource");
        helpPopup.add(mi);
    }

    private void findAvailableHelpFiles() {
        try {
            ArrayList<String> allHelpFiles =
                    FileUtilities.findAllFilesWithExtension(new File(helpDirectory), ".html", false);

            String[] helpFiles1 = new String[allHelpFiles.size()];
            helpFiles1 = allHelpFiles.toArray(helpFiles1);

            Arrays.sort(helpFiles1, new Comparator<String>() {
                @Override
                public int compare(String str1, String str2) {
                    return str1.toLowerCase().compareTo(str2.toLowerCase());
                }
            });
            helpFiles = new String[helpFiles1.length][2];
            for (int a = 0; a < helpFiles1.length; a++) {
                helpFiles[a][0] = helpFiles1[a];
                // find out the short name of the file.
                File file = new File(helpFiles1[a]);
                helpFiles[a][1] = file.getName().replace(".html", "");
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
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

    private void createHelpTableOfContents() {
        // This method is used to generate the table of contents html page
        // used on the Whitebox GAT online help. It is not meant for general use.
        findAvailableHelpFiles();
        String outputFile = helpDirectory + "Help_TOC.html";
        if (new File(outputFile).exists()) {
            new File(outputFile).delete();
        }
        File file = new File(outputFile);
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter out = null;
        try {
            fw = new FileWriter(file, false);
            bw = new BufferedWriter(fw);
            out = new PrintWriter(bw, true);

            String str;

            str = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html>";
            out.println(str);
            str = "<head>";
            out.println(str);
            str = "<meta content=\"text/html; charset=iso-8859-1\" http-equiv=\"content-type\"><title>Help Topics</title>"
                    + "<link rel=\"stylesheet\" type=\"text/css\" href=\"Help.css\"></meta>";
            out.println(str);
            str = "</head>";
            out.println(str);
            str = "<body><h1>Help Topics</h1>";
            out.println(str);
            str = "<p>";
            out.println(str);
            
            for (int i = 0; i < helpFiles.length; i++) {
                //model.add(i, helpFiles[i][1]);
                str = "<a href=\"http://www.uoguelph.ca/~hydrogeo/Whitebox/Help/" + helpFiles[i][1] + ".html\" " +
                "target=\"Body_Frame\">" + helpFiles[i][1] + "</a><br>";
                out.println(str);
            }
            
            str = "</p>";
            out.println(str);

            str = "</body>\n</html>";
            out.println(str);

        } catch (java.io.IOException e) {
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
        if (actionCommand.equals("close")) {
            this.dispose();
        } else if (actionCommand.equals("back")) {
            back();
        } else if (actionCommand.equals("forward")) {
            forward();
        } else if (actionCommand.equals("viewSource")) {
            ViewCodeDialog vcd = new ViewCodeDialog((Frame) host, new File(helpFile), true);
            vcd.setSize(new Dimension(800, 600));
            vcd.setVisible(true);
        }
    }
    
    // This method is only used during testing.
    public static void main(String[] args) {

        Help help = new Help();
        help.createHelpTableOfContents();
    }
    
    private ActionListener indexFieldListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            try {
                helpFile = helpFiles[availableHelpFiles.getSelectedIndex()][0];
                helpPane.setPage("file:" + helpHistory.get(helpHistoryIndex));
            } catch (MalformedURLException me) {
            } catch (IOException ioe) {
            }
        }
    };
    private ActionListener searchFieldListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            searchForWords();

        }
    };

//    private void helpMousePress(MouseEvent e) {
//        //int selRow = layersTree.getRowForLocation(e.getX(), e.getY());
//        //TreePath selPath = layersTree.getPathForLocation(e.getX(), e.getY());
//        //String label;
//        if (e.getButton() == 3 || e.isPopupTrigger()) {
//            helpPopup.show((JComponent) e.getSource(), e.getX(), e.getY());
//        }
//    }
    private void searchForWords() {
        try {
            DefaultListModel model = new DefaultListModel();

            searchCounts = new int[helpFiles.length][2];

            String line = searchField.getText().trim().toLowerCase();

            // find quotations
            ArrayList<String> quotedStrings = new ArrayList<String>();
            Pattern p = Pattern.compile("\"([^\"]*)\"");
            Matcher m = p.matcher(line);
            while (m.find()) {
                quotedStrings.add(m.group(1));
            }

            // now remove all quotedStrings from the line
            for (int i = 0; i < quotedStrings.size(); i++) {
                line = line.replace(quotedStrings.get(i), "");
            }

            line = line.replace("\"", "");

            int count = 0;
            int index = 0;

            line = line.replace("-", " ");
            line = line.replace(" the ", "");
            line = line.replace(" a ", "");
            line = line.replace(" of ", "");
            line = line.replace(" to ", "");
            line = line.replace(" and ", "");
            line = line.replace(" be ", "");
            line = line.replace(" in ", "");
            line = line.replace(" it ", "");

            String[] words = line.split(" ");

            int numWordsToMatch = words.length + quotedStrings.size();

            for (int i = 0; i < helpFiles.length; i++) {
                String fileContents = FileUtilities.readFileAsString(helpFiles[i][0]).toLowerCase();
                fileContents = fileContents.replace("-", " ");
                count = 0;
                for (String word : words) {
                    if (fileContents.contains(" " + word + " ")) {
                        count++;
                    }
                }

                for (String word : quotedStrings) {
                    if (fileContents.contains(word)) {
                        count++;
                    }
                }

                searchCounts[i][0] = count;
                searchCounts[i][1] = i;

            }


            Arrays.sort(searchCounts, new Comparator<int[]>() {
                @Override
                public int compare(int[] entry1, int[] entry2) {
                    final int int1 = entry1[0];
                    final int int2 = entry2[0];
                    if (int1 > int2) {
                        return -1;
                    } else if (int1 == int2) {
                        String str1 = helpFiles[entry1[1]][1];
                        String str2 = helpFiles[entry2[1]][1];
                        return str1.compareTo(str2);
                    } else {
                        return 1;
                    }
                }
            });

            DecimalFormat df = new DecimalFormat("0.0%");
            for (int i = 19; i >= 0; i--) {
                if (searchCounts[i][0] > 0) {
                    model.add(index, helpFiles[searchCounts[i][1]][1] + " (" + df.format((double) searchCounts[i][0] / numWordsToMatch) + ")");
                }
            }

            searchOutput.setModel(model);

            MouseListener ml = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    JList theList = (JList) e.getSource();
                    String label = null;
                    int index = theList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        Object o = theList.getModel().getElementAt(index);
                        label = o.toString();
                    }
                    if (e.getClickCount() == 1 && e.getButton() == 1) {
                        try {

                            helpFile = helpFiles[searchCounts[index][1]][0];
                            if (helpHistoryIndex == helpHistory.size() - 1) {
                                helpHistory.add(helpFile);
                                helpHistoryIndex = helpHistory.size() - 1;
                            } else {
                                for (int i = helpHistory.size() - 1; i > helpHistoryIndex; i--) {
                                    helpHistory.remove(i);
                                }
                                helpHistory.add(helpFile);
                                helpHistoryIndex = helpHistory.size() - 1;
                            }

                            helpPane.setPage("file:" + helpHistory.get(helpHistoryIndex));
                        } catch (MalformedURLException me) {
                        } catch (IOException ioe) {
                        }
                    }
                }
            };
            searchOutput.addMouseListener(ml);

        } catch (IOException ioe) {
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

    private class indexFieldKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent ke) {
        }

        @Override
        public void keyPressed(KeyEvent ke) {
        }

        @Override
        public void keyReleased(KeyEvent ke) {
            String line = indexField.getText().trim().toLowerCase();
            if (!line.equals("")) {
                for (int i = 0; i < helpFiles.length; i++) {
                    if (helpFiles[i][1].toLowerCase().startsWith(line)) {
                        availableHelpFiles.setSelectedIndex(i);
                        availableHelpFiles.ensureIndexIsVisible(i);
                        break;
                    }
                }
            } else {
                availableHelpFiles.setSelectedIndex(0);
                availableHelpFiles.ensureIndexIsVisible(0);
            }
            //throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}