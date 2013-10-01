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

//import java.awt.event.KeyAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;
//import whitebox.interfaces.Communicator;
import whitebox.interfaces.WhiteboxPluginHost;
import whitebox.utilities.ClassEnumerator;
import static whiteboxgis.user_interfaces.Scripter.PROP_SCRIPTING_LANGUAGE;
import static whiteboxgis.user_interfaces.Scripter.ScriptingLanguage.GROOVY;
import static whiteboxgis.user_interfaces.Scripter.ScriptingLanguage.JAVASCRIPT;
import static whiteboxgis.user_interfaces.Scripter.ScriptingLanguage.PYTHON;
import whiteboxgis.ScripterCompletionProvider;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class Scripter extends JDialog implements ActionListener, KeyListener {

    private String pathSep;
    private String graphicsDirectory;
    private String scriptsDirectory;
    private String sourceFile = null;
    private WhiteboxPluginHost host = null;
    private RSyntaxTextArea editor = new RSyntaxTextArea();
    private RTextScrollPane scroll;
    private ScriptEngineManager mgr = new ScriptEngineManager();
//    private List<ScriptEngineFactory> factories = mgr.getEngineFactories();
    private ScriptEngine engine;
    private JTextArea textArea = new JTextArea();
    private JSplitPane splitPane;
    private PrintWriter errOut = new PrintWriter(new Scripter.TextAreaWriter(textArea));
    private Scripter.ScriptingLanguage language = Scripter.ScriptingLanguage.PYTHON;
    private JPanel status = null;
    private JLabel statusLabel = new JLabel();
    private JCheckBoxMenuItem python = new JCheckBoxMenuItem("Python");
    private JCheckBoxMenuItem groovy = new JCheckBoxMenuItem("Groovy");
    private JCheckBoxMenuItem javascript = new JCheckBoxMenuItem("Javascript");
    private ResourceBundle bundle;
    private JButton generateDataButton;
    public static final String PROP_SCRIPTING_LANGUAGE = "languageChanged";
    public static final String PROP_GENERATE_DATA = "generateData";
    private int numLinesInDoc = 1;
    private boolean editorDirty = false;
    private JTextField searchField;
    private JTextField replaceField;
    private JCheckBox regexCB;
    private JCheckBox matchCaseCB;
    private JCheckBox wholeWordCB;
    private JToolBar findAndReplaceToolbar;
    private JButton nextButton;
    private JButton prevButton;
    private JLabel findLabel;
    private JLabel replaceLabel;
    private JButton replaceButton;

    public enum ScriptingLanguage {

        PYTHON("Python", "#"), GROOVY("Groovy", "//"), JAVASCRIPT("Javascript", "//"),
        RUBY("Ruby", "#");
        private String displayName;
        private String commentMarker;

        private ScriptingLanguage(String displayName, String commentMarker) {
            this.displayName = displayName;
            this.commentMarker = commentMarker;
        }

        public String getCommentMarker() {
            return this.commentMarker;
        }

        @Override
        public String toString() {
            return this.displayName;
        }
    }

    public Scripter(Frame owner, boolean modal) {
        super(owner, modal);
        try {
            this.pathSep = File.separator;
            String applicationDirectory = java.net.URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            if (applicationDirectory.endsWith(".exe") || applicationDirectory.endsWith(".jar")) {
                applicationDirectory = new File(applicationDirectory).getParent();
            } else {
                // Add the path to the class files
                applicationDirectory += getClass().getName().replace('.', File.separatorChar);

                // Step one level up as we are only interested in the
                // directory containing the class files
                applicationDirectory = new File(applicationDirectory).getParent();
            }
            applicationDirectory = new File(applicationDirectory).getParent();
            findGraphicsDirectory(new File(applicationDirectory));
            findScriptDirectory(new File(applicationDirectory));

            if (owner != null && owner instanceof WhiteboxPluginHost) {
                host = (WhiteboxPluginHost) owner;
                bundle = host.getGuiLabelsBundle();
            }

            initUI();
        } catch (Exception e) {
            host.logException("Error in Scripter.", e);
        }
    }

    private void findScriptDirectory(File dir) {
        File[] files = dir.listFiles();
        for (int x = 0; x < files.length; x++) {
            if (files[x].isDirectory()) {
                if (files[x].toString().endsWith(pathSep + "Scripts")) {
                    scriptsDirectory = files[x].toString() + pathSep;
                    break;
                } else {
                    findScriptDirectory(files[x]);
                }
            }
        }
    }

    private void findGraphicsDirectory(File dir) {
        File[] files = dir.listFiles();
        for (int x = 0; x < files.length; x++) {
            if (files[x].isDirectory()) {
                if (files[x].toString().endsWith(pathSep + "Images")) {
                    graphicsDirectory = files[x].toString() + pathSep;
                    break;
                } else {
                    findGraphicsDirectory(files[x]);
                }
            }
        }
    }

    private void initUI() {
        try {

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

            if (System.getProperty("mrj.version") != null) {
                System.setProperty("com.apple.macos.useScreenMenuBar", "true");
                System.setProperty("apple.laf.useScreenMenuBar", "true");
            }

            errOut = new PrintWriter(new Scripter.TextAreaWriter(textArea));

            initScriptEngine();

            this.setTitle("Whitebox Scripter");
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            this.setPreferredSize(new Dimension(550, 650));

            Container c = this.getContentPane();

            createMenu();

            JToolBar toolbar = createToolbar();
            //c.add(toolbar, BorderLayout.PAGE_START);

            findAndReplaceToolbar = createFindToolbar();
            findAndReplaceToolbar.setVisible(false);

            JPanel toolbarPanel = new JPanel();
            toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.Y_AXIS));
            toolbarPanel.add(toolbar);
            toolbarPanel.add(findAndReplaceToolbar);
            c.add(toolbarPanel, BorderLayout.PAGE_START);

            editor = new RSyntaxTextArea(20, 60);
            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
            editor.setCodeFoldingEnabled(true);
            editor.setAntiAliasingEnabled(true);
            editor.setCloseCurlyBraces(true);
            editor.addKeyListener(this);
            editor.setTabSize(4);
            editor.setBracketMatchingEnabled(true);
            editor.setAutoIndentEnabled(true);
            editor.setCloseCurlyBraces(true);
            editor.setMarkOccurrences(true);
            resetAutocompletion();
//            setupAutocomplete();
            scroll = new RTextScrollPane(editor);
            scroll.setFoldIndicatorEnabled(true);

            Box outputBox = Box.createHorizontalBox();
            Box outputToolbarBox = Box.createVerticalBox();
            JToolBar outputToolbar = createOutputToolbar();
            outputToolbar.setOrientation(SwingConstants.VERTICAL);
            outputToolbar.setFloatable(false);
            outputToolbarBox.add(outputToolbar);
            outputToolbarBox.add(Box.createVerticalGlue());
            JScrollPane scroll2 = new JScrollPane(textArea);
            outputBox.add(outputToolbarBox);
            outputBox.add(scroll2);

            splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, outputBox);
            splitPane.setDividerLocation(400);

            c.add(splitPane);

            status = new JPanel();
            status.setLayout(new BoxLayout(status, BoxLayout.LINE_AXIS));
            status.setPreferredSize(new Dimension(10, 24));
            status.add(Box.createHorizontalStrut(5));
            status.add(statusLabel);
            status.add(Box.createHorizontalGlue());

            c.add(status, BorderLayout.PAGE_END);

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    checkIfEditorIsDirty();
                }
            });

            this.pack();
        } catch (Exception e) {
            host.logException("Error in Scripter.", e);
        }
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();

        JButton openBtn = makeToolBarButton("open.png", "open", bundle.getString("OpenFile"), "Open");
        toolbar.add(openBtn);

        JButton saveBtn = makeToolBarButton("SaveMap.png", "save", bundle.getString("SaveFile"), "Save");
        toolbar.add(saveBtn);

        JButton printBtn = makeToolBarButton("print.png", "print",
                bundle.getString("Print"), "Print");
        toolbar.add(printBtn);

        toolbar.addSeparator();

        JButton toggleComment = makeToolBarButton("Comment.png", "Comment",
                bundle.getString("ToggleComments"), "Comment");
        toolbar.add(toggleComment);
        
        JButton indent = makeToolBarButton("Indent.png", "Indent",
                "Indent", "Indent");
        toolbar.add(indent);
        
        JButton outdent = makeToolBarButton("Outdent.png", "Outdent",
                "Outdent", "Outdent");
        toolbar.add(outdent);
        
        toolbar.addSeparator();

        JButton executeBtn = makeToolBarButton("Execute.png", "execute",
                bundle.getString("ExecuteCode"), "Execute");
        toolbar.add(executeBtn);

        generateDataButton = makeToolBarButton("GenerateData.png", "generateData",
                bundle.getString("GenerateColumnData"), "Generate Data");
        toolbar.add(generateDataButton);

        showGenerateDataButton(false);

        toolbar.add(Box.createHorizontalGlue());

        return toolbar;

    }

    private JToolBar createFindToolbar() {
        // Create a toolbar with searching options.
        JToolBar toolbar = new JToolBar();

        toolbar.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        findLabel = new JLabel("Find:");
        //c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.15;
        c.gridx = 0;
        c.gridy = 0;
        toolbar.add(findLabel, c);

        searchField = new JTextField(7);
        searchField.setMinimumSize(searchField.getPreferredSize());
        c.weightx = 0.25;
        c.gridx = 1;
        c.gridy = 0;
        toolbar.add(searchField, c);

        JPanel findButtonBox = new JPanel(); //Box.createHorizontalBox();
        findButtonBox.setLayout(new BoxLayout(findButtonBox, BoxLayout.X_AXIS));

        prevButton = new JButton("\u25C0"); //"\u2190"); // previous
        prevButton.setActionCommand("FindPrev");
        prevButton.addActionListener(this);
        findButtonBox.add(prevButton);

        nextButton = new JButton("\u25B6"); //"\u2192"); // next
        nextButton.setActionCommand("FindNext");
        nextButton.addActionListener(this);
        findButtonBox.add(nextButton);

        findButtonBox.add(Box.createHorizontalGlue());

        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextButton.doClick(0);
            }
        });


        matchCaseCB = new JCheckBox("Match Case");
        findButtonBox.add(matchCaseCB);
        wholeWordCB = new JCheckBox("Whole Words");
        findButtonBox.add(wholeWordCB);
        regexCB = new JCheckBox("Regex");
        findButtonBox.add(regexCB);

        findButtonBox.add(Box.createHorizontalGlue());

        JButton closeBtn = new JButton("x");
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                findAndReplaceToolbar.setVisible(false);
            }
        });
        //toolBar.add(closeBtn);
        findButtonBox.add(closeBtn);

        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.gridy = 0;
        toolbar.add(findButtonBox, c);



        replaceLabel = new JLabel("Replace:");
        c.weightx = 0.15;
        c.gridx = 0;
        c.gridy = 1;
        toolbar.add(replaceLabel, c);


        replaceField = new JTextField(7);
        replaceField.setMinimumSize(replaceField.getPreferredSize());
        replaceField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchContext context = new SearchContext();
                String text = searchField.getText();
                if (text.length() == 0) {
                    return;
                }
                context.setSearchFor(text);
                context.setReplaceWith(replaceField.getText());
                context.setMatchCase(matchCaseCB.isSelected());
                context.setRegularExpression(regexCB.isSelected());
                context.setWholeWord(wholeWordCB.isSelected());
                SearchEngine.replaceAll(editor, context);
            }
        });
        c.weightx = 0.25;
        c.gridx = 1;
        c.gridy = 1;
        toolbar.add(replaceField, c);

        replaceButton = new JButton("replace");
        replaceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchContext context = new SearchContext();
                String text = searchField.getText();
                if (text.length() == 0) {
                    return;
                }
                context.setSearchFor(text);
                context.setReplaceWith(replaceField.getText());
                context.setMatchCase(matchCaseCB.isSelected());
                context.setRegularExpression(regexCB.isSelected());
                context.setWholeWord(wholeWordCB.isSelected());
                SearchEngine.replaceAll(editor, context);
            }
        });

        JPanel replaceButtonPanel = new JPanel();
        replaceButtonPanel.setLayout(new BoxLayout(replaceButtonPanel, BoxLayout.X_AXIS));
        replaceButtonPanel.add(replaceButton);
        replaceButtonPanel.add(Box.createHorizontalGlue());
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.gridy = 1;
        toolbar.add(replaceButtonPanel, c);

//        Box textFields = Box.createVerticalBox();
//        
//        Box findBox = Box.createHorizontalBox();
//        findLabel = new JLabel("Find:");
//        findBox.add(findLabel);
//        searchField = new JTextField(10);
//        //toolBar.add(searchField);
//        findBox.add(searchField);
//        textFields.add(findBox);
//        
//        Box replaceBox = Box.createHorizontalBox();
//        replaceLabel = new JLabel("Replace:");
//        replaceBox.add(replaceLabel);
//        replaceField = new JTextField(10);
//        replaceField.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                SearchContext context = new SearchContext();
//                String text = searchField.getText();
//                if (text.length() == 0) {
//                    return;
//                }
//                context.setSearchFor(text);
//                context.setReplaceWith(replaceField.getText());
//                context.setMatchCase(matchCaseCB.isSelected());
//                context.setRegularExpression(regexCB.isSelected());
////                context.setWholeWord(false);
//                
//                SearchEngine.replaceAll(editor, context);
//            }
//        });
//        
//        replaceBox.add(replaceField);
//        textFields.add(replaceBox);
//        
//        toolbar.add(textFields);
//        
//        Box findButtonBox = Box.createHorizontalBox();
//        nextButton = new JButton("Next");
//        nextButton.setActionCommand("FindNext");
//        nextButton.addActionListener(this);
//        findButtonBox.add(nextButton);
//        //toolBar.add(nextButton);
//        
//        searchField.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                nextButton.doClick(0);
//            }
//        });
//        
//        
//        prevButton = new JButton("Previous");
//        prevButton.setActionCommand("FindPrev");
//        prevButton.addActionListener(this);
//        //toolBar.add(prevButton);
//        findButtonBox.add(prevButton);
//        
//        regexCB = new JCheckBox("Regex");
//        //toolBar.add(regexCB);
//        findButtonBox.add(regexCB);
//        matchCaseCB = new JCheckBox("Match Case");
//        //toolBar.add(matchCaseCB);
//        findButtonBox.add(matchCaseCB);
//        
//        findButtonBox.add(Box.createHorizontalGlue());
//        //toolBar.add(Box.createHorizontalGlue());
//        
//        JButton closeBtn = new JButton("x");
//        closeBtn.setBorderPainted(false);
//        closeBtn.setFocusPainted(false);
//        closeBtn.setContentAreaFilled(false);
//        closeBtn.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                findAndReplaceToolbar.setVisible(false);
//            }
//        });
//        //toolBar.add(closeBtn);
//        findButtonBox.add(closeBtn);
//        
//        Box box2 = Box.createVerticalBox();
//        
//        box2.add(findButtonBox);
//        box2.add(Box.createVerticalGlue());
//        
//        toolbar.add(box2);

        return toolbar;
    }

    private JToolBar createOutputToolbar() {
        JToolBar toolbar = new JToolBar();

        JButton clearConsole = makeToolBarButton("ClearConsole.png", "clearConsole",
                bundle.getString("ClearConsole"), "X"); //bundle.getString("ClearConsole"));
        toolbar.add(clearConsole);

        return toolbar;

    }

    private void createMenu() {
        try {
            JMenuBar menubar = new JMenuBar();

            JMenu fileMenu = new JMenu(bundle.getString("File"));
            JMenuItem open = new JMenuItem(bundle.getString("OpenFile"));
            open.setActionCommand("open");
            open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            open.addActionListener(this);
            fileMenu.add(open);
            JMenuItem save = new JMenuItem(bundle.getString("Save"));
            save.setActionCommand("save");
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
            close.setActionCommand("close");
            //close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
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
            JMenuItem undo = new JMenuItem(bundle.getString("Undo"));
            undo.setActionCommand("undo");
            undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            undo.addActionListener(this);
            editMenu.add(undo);

            JMenuItem redo = new JMenuItem(bundle.getString("Redo"));
            redo.setActionCommand("redo");
            redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            redo.addActionListener(this);
            editMenu.add(redo);

            JMenuItem cut = new JMenuItem(bundle.getString("Cut"));
            cut.setActionCommand("cut");
            cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            cut.addActionListener(this);
            editMenu.add(cut);

            JMenuItem copy = new JMenuItem(bundle.getString("Copy"));
            copy.setActionCommand("copy");
            copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            copy.addActionListener(this);
            editMenu.add(copy);

            JMenuItem paste = new JMenuItem(bundle.getString("Paste"));
            paste.setActionCommand("paste");
            paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            paste.addActionListener(this);
            editMenu.add(paste);

            JMenuItem selectAll = new JMenuItem("Select All");
            selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            selectAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    editor.selectAll();
                }
            });
            editMenu.add(selectAll);

            editMenu.addSeparator();

            JMenuItem find = new JMenuItem("Find");
            find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            find.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    findAndReplaceToolbar.setVisible(true);
                    searchField.requestFocus();
                    replaceField.setVisible(false);
                    replaceLabel.setVisible(false);
                    replaceButton.setVisible(false);
//                    searchField.setVisible(true);
//                    findLabel.setVisible(true);
                    prevButton.setVisible(true);
                    nextButton.setVisible(true);
                }
            });
            editMenu.add(find);

            JMenuItem replace = new JMenuItem("Replace");
            replace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            replace.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    findAndReplaceToolbar.setVisible(true);
                    searchField.requestFocus();
                    replaceField.setVisible(true);
                    replaceLabel.setVisible(true);
                    replaceButton.setVisible(true);
//                    searchField.setVisible(false);
//                    findLabel.setVisible(false);
                    prevButton.setVisible(false);
                    nextButton.setVisible(false);
                }
            });
            editMenu.add(replace);

            menubar.add(editMenu);

//            JMenu viewMenu = new JMenu("View");
//            
//            JMenuItem groovyConsole = new JMenuItem("View Groovy Console");
//            groovyConsole.setActionCommand("viewGroovyConsole");
//            groovyConsole.addActionListener(this);
//            viewMenu.add(groovyConsole);
//            
//            menubar.add(viewMenu);

            JMenu languageMenu = new JMenu(bundle.getString("Language"));
            python.setActionCommand("python");
            python.addActionListener(this);
            python.setState(true);
            languageMenu.add(python);

            groovy.setActionCommand("groovy");
            groovy.addActionListener(this);
            groovy.setState(false);
            languageMenu.add(groovy);

            javascript.setActionCommand("javascript");
            javascript.addActionListener(this);
            javascript.setState(false);
            languageMenu.add(javascript);

            menubar.add(languageMenu);

            JMenu sourceMenu = new JMenu(bundle.getString("Source"));
            JMenuItem execute = new JMenuItem(bundle.getString("ExecuteCode"));
            execute.setActionCommand("execute");
            execute.addActionListener(this);
            execute.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            sourceMenu.add(execute);

            sourceMenu.addSeparator();
            JMenuItem toggleComments = new JMenuItem(bundle.getString("ToggleComments"));
            toggleComments.setActionCommand("Comment");
            toggleComments.addActionListener(this);
            toggleComments.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            sourceMenu.add(toggleComments);
            
            JMenuItem indent = new JMenuItem("Indent");
//            indent.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            indent.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    indentSelection();
                }
            });
            sourceMenu.add(indent);
            
            JMenuItem outdent = new JMenuItem("Outdent");
//            outdent.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            outdent.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    outdentSelection();
                }
            });
            sourceMenu.add(outdent);

            menubar.add(sourceMenu);


            this.setJMenuBar(menubar);

        } catch (Exception e) {
            host.logException("Error in Scripter.", e);
        }
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
            host.logException("Error in Scripter.", e);
        }

        return button;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
            if (e.getKeyCode() == 10) { //pressed enter
                if (editor.getLineCount() != numLinesInDoc) {
                    scanDoc();
                    numLinesInDoc = editor.getLineCount();
                }
            } else if (e.getKeyCode() == KeyEvent.VK_DELETE
                    || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                if (editor.getLineCount() != numLinesInDoc) {
                    resetAutocompletion();
                    scanDoc();
                    numLinesInDoc = editor.getLineCount();
                }
            } else if (e.getKeyCode() == KeyEvent.VK_PERIOD) { // pressed dot
                int start = editor.getLineStartOffsetOfCurrentLine();
                int end = editor.getLineEndOffsetOfCurrentLine();
                String line = editor.getText(start, end - start + 1);
                line = line.split("\n")[0];
                if (line.startsWith("import") || line.startsWith("from")) {
                    String pck = line.replace("import", "").replace("from", "").trim();
                    if (pck.endsWith(".")) {
                        pck = pck.substring(0, pck.length() - 1);
                    }
                    importPackage(pck);
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    private void checkIfEditorIsDirty() {
        if (editorDirty) {
            Object[] options = {"Yes", "No"};
            int n = JOptionPane.showOptionDialog(this,
                    "The source code has changed. Would you like to save it?",
                    "Whitebox GAT Message",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, //do not use a custom Icon
                    options, //the titles of buttons
                    options[0]); //default button title

            if (n == JOptionPane.YES_OPTION) {
                save();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }
    private ScripterCompletionProvider provider;
    private AutoCompletion ac;

    private void setupAutocomplete() {
        provider = new ScripterCompletionProvider();
        provider.setAutoActivationRules(false, ".");

        addLanguageKeywords();

        importVariableAs("pluginHost", WhiteboxPluginHost.class.getCanonicalName());

        // An AutoCompletion acts as a "middle-man" between a text component
        // and a CompletionProvider. It manages any options associated with
        // the auto-completion (the popup trigger key, whether to display a
        // documentation window along with completion choices, etc.). Unlike
        // CompletionProviders, instances of AutoCompletion cannot be shared
        // among multiple text components.
        ac = new AutoCompletion(provider);

        ac.setAutoCompleteEnabled(true);
        ac.setAutoActivationEnabled(true);
        ac.setShowDescWindow(true);
        ac.setParameterAssistanceEnabled(true);
        ac.install(editor);
    }

    private void resetAutocompletion() {
        listOfImportedClasses.clear();
        listOfImportedItems.clear();
        listOfImportedVariables.clear();
        listOfImportedMethods.clear();
        listOfMethodReturns.clear();
        setupAutocomplete();

//        ArrayList<String> jars = FileUtilities.findAllFilesWithExtension(new File(host.getApplicationDirectory()), ".jar", true);
//        for (String str : jars) {
//            System.out.println(str);
//        }

//        if (language == PYTHON) {
//            importClass("org.python.core.__builtin__");
//        }

//        if (language == GROOVY) {
//            importPackage("java.lang");
//            importPackage("java.io");
//            importPackage("java.net");
//            importPackage("java.util");
//            importPackage("groovy.lang");
//            importPackage("groovy.util");
//            
//            importClass("java.lang.*");
//            importClass("java.io.*");
//            importClass("java.math.BigDecimal");
//            importClass("java.math.BigInteger");
//            importClass("java.net.*");
//            importClass("java.util.*");
//            importClass("groovy.lang.*");
//            importClass("groovy.util.*");
//        }
    }

    private boolean scanDoc() {
        try {
            // read each line in the doc
            String[] lines = editor.getText().split(System.lineSeparator());

            for (String line : lines) {
                line = line.replace("\t", "");
                if (!line.isEmpty()) {
                    if (line.startsWith("import")) {
                        // import a class
                        String className = line.replace("import", "").trim();
                        if (!listOfImportedClasses.contains(className)) {
                            importClass(className);
                        }
                    } else if (line.contains("import") && line.contains("from")) {
                        // python class import
                        String className = line.replace("import", ".").replace("from", "").trim().replace(" ", "");
                        if (!listOfImportedClasses.contains(className)) {
                            importClass(className);
                        }
                    } else if (line.contains("=") && !line.contains("==")) {
                        // variable definition
                        // first find the name of the variable
                        String[] s1 = line.split("=");
                        if (s1.length == 2) {
                            String[] s2 = s1[0].split(" ");
                            String variableName = s2[s2.length - 1].trim();

                            if (!listOfImportedVariables.contains(variableName)) {
                                // now figure out the class
                                String className = null;
                                String variableType = "";

                                if (language == GROOVY) {
                                    variableType = s1[1].replace(";", "").trim();
                                    if ((variableType.startsWith("\"") && variableType.endsWith("\""))
                                            || (variableType.startsWith("\'") && variableType.endsWith("\'"))) {
                                        // it is a string
                                        className = "groovy.lang.GString";
                                    } else if (whitebox.utilities.StringUtilities.isInteger(variableType)) {
                                        className = "java.lang.Integer";
                                    } else if (whitebox.utilities.StringUtilities.isNumeric(variableType)) {
                                        className = "java.lang.Double";
                                    } else if (whitebox.utilities.StringUtilities.isBoolean(variableType)) {
                                        className = "java.lang.Boolean";
                                    } else if (variableType.startsWith("[")
                                            && variableType.endsWith("]")
                                            && !variableType.contains(":")) {
                                        className = "java.util.List";
                                    } else if (variableType.startsWith("[")
                                            && variableType.endsWith("]")
                                            && !variableType.contains(":")) {
                                        className = "java.util.MapWithDefault";
                                    } else if (variableType.contains("(")) {
                                        variableType = s1[1].substring(0, s1[1].indexOf("(")).replace(" new ", "").trim();
                                        // is the type known?String className = null;
                                        for (String str : listOfImportedClasses) {
                                            if (str.endsWith("." + variableType)) {
                                                className = str;
                                                break;
                                            }
                                        }
                                    } else if (className == null) {
                                        className = "java.lang.Object";
                                    }
                                } else if (language == PYTHON) {
                                    variableType = s1[1].trim();
                                    if ((variableType.startsWith("\"") && variableType.endsWith("\""))
                                            || (variableType.startsWith("\'") && variableType.endsWith("\'"))) {
                                        // it is a string
                                        className = "org.python.core.PyString";
                                    } else if (variableType.startsWith("float(")
                                            && variableType.endsWith(")")) {
                                        className = "org.python.core.PyFloat";
                                    } else if (variableType.startsWith("int(")
                                            && variableType.endsWith(")")) {
                                        className = "org.python.core.PyInteger";
                                    } else if (variableType.startsWith("long(")
                                            && variableType.endsWith(")")) {
                                        className = "org.python.core.PyLong";
                                    } else if (variableType.startsWith("complex(")
                                            && variableType.endsWith(")")) {
                                        className = "org.python.core.PyComplex";
                                    } else if (whitebox.utilities.StringUtilities.isInteger(variableType)) {
                                        className = "org.python.core.PyLong";
                                    } else if (whitebox.utilities.StringUtilities.isNumeric(variableType)) {
                                        className = "org.python.core.PyFloat";
                                    } else if (whitebox.utilities.StringUtilities.isBoolean(variableType)) {
                                        className = "org.python.core.PyBoolean";
                                    } else if (variableType.startsWith("[")
                                            && variableType.endsWith("]")) {
                                        className = "org.python.core.PyList";
                                    } else if (variableType.startsWith("(")
                                            && variableType.endsWith(")")) {
                                        className = "org.python.core.PyTuple";
                                    } else if (variableType.contains("(")) {
                                        variableType = s1[1].substring(0, s1[1].indexOf("(")).replace(" new ", "").trim();
                                        // is the type known?String className = null;
                                        for (String str : listOfImportedClasses) {
                                            if (str.endsWith("." + variableType)) {
                                                className = str;
                                                break;
                                            }
                                        }
                                    } else if (className == null) {
                                        className = "org.python.core.PyObject";
                                    }

                                } else if (language == JAVASCRIPT) {
                                    variableType = s1[1].replace(";", "").trim();
                                }

                                importVariableAs(variableName, className);

                            }
                        }
                    } else if (line.contains("class")) {
                        String[] s1 = line.split(" ");
                        // the class name is the one after the class keyword
                        String className = "";
                        for (int i = 0; i < s1.length; i++) {
                            if (s1[i].trim().toLowerCase().equals("class")) {
                                String[] s2 = s1[i + 1].split("\\(");
                                className = s2[0];
                                break;
                            }
                        }

                        if (!className.isEmpty()) {
                            if (!listOfImportedClasses.contains(className)) {
                                listOfImportedClasses.add(className);
                                provider.addCompletion(new BasicCompletion(provider, className));
                            }
                        }
                    }
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
    ArrayList<String> listOfImportedItems = new ArrayList<>();
    ArrayList<String> listOfImportedClasses = new ArrayList<>();
    ArrayList<String> listOfImportedVariables = new ArrayList<>();
    ArrayList<String> listOfImportedMethods = new ArrayList<>();
    HashMap<String, String> listOfMethodReturns = new HashMap<>();

    private void importPackage(String pck) {
        ArrayList<String> myClasses = ClassEnumerator.getClassNamesForPackage(pck);
        if (myClasses == null) {
            return;
        }
        Collections.sort(myClasses);
        for (String str : myClasses) {
            if (!listOfImportedItems.contains(str)) {
                provider.addCompletion(new BasicCompletion(provider, str));
                listOfImportedItems.add(str);
            }
        }
    }

    private void importPackage(Package pck) {
        ArrayList<String> myClasses = ClassEnumerator.getClassNamesForPackage(pck);
        if (myClasses == null) {
            return;
        }
        Collections.sort(myClasses);
        for (String str : myClasses) {
            if (!listOfImportedItems.contains(str)) {
                provider.addCompletion(new BasicCompletion(provider, str));
                listOfImportedItems.add(str);
            }
        }
    }

    private void importVariableAs(String variableName, String className) {
        if (!listOfImportedVariables.contains(variableName)) {
            try {
                if (className != null) {
                    Class c = Class.forName(className);
                    if (addVariableToProvider(variableName, c)) {
                        listOfImportedVariables.add(variableName);
                    }
                } else {
                    if (addVariableToProvider(variableName, null)) {
                        listOfImportedVariables.add(variableName);
                    }
                }
            } catch (ClassNotFoundException | SecurityException e) {
                // class not found.
            }
        }
    }

    private void importClass(String className) {
        if (!listOfImportedClasses.contains(className)) {
            try {
                if (!className.endsWith("*")) {
                    if (addClassToProvider(className)) {
                        listOfImportedClasses.add(className);
                    }
                } else {
                    // import all of the classes in this package
                    String packageName = className.replace("*", "");
                    for (String str : listOfImportedItems) {
                        if (str.startsWith(packageName) && !listOfImportedClasses.contains(str)) {
                            if (addClassToProvider(str)) {
                                listOfImportedClasses.add(str);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // class not found.
            }
        }
    }

    private void addLanguageKeywords() {
        provider.setParameterizedCompletionParams("(".charAt(0), ", ", ")".charAt(0));

        //LanguageAwareCompletionProvider lacProvider = new LanguageAwareCompletionProvider(provider);

        switch (language) {
            case PYTHON:

                provider.addCompletion(new BasicCompletion(provider, "and"));
                provider.addCompletion(new BasicCompletion(provider, "as"));
                provider.addCompletion(new BasicCompletion(provider, "assert"));
                provider.addCompletion(new BasicCompletion(provider, "break"));
                provider.addCompletion(new BasicCompletion(provider, "class"));
                provider.addCompletion(new BasicCompletion(provider, "continue"));
                provider.addCompletion(new BasicCompletion(provider, "def"));
                provider.addCompletion(new BasicCompletion(provider, "del"));
                provider.addCompletion(new BasicCompletion(provider, "elif"));
                provider.addCompletion(new BasicCompletion(provider, "else"));
                provider.addCompletion(new BasicCompletion(provider, "except"));
                provider.addCompletion(new BasicCompletion(provider, "exec"));
                provider.addCompletion(new BasicCompletion(provider, "finally"));
                provider.addCompletion(new BasicCompletion(provider, "for"));
                provider.addCompletion(new BasicCompletion(provider, "from"));
                provider.addCompletion(new BasicCompletion(provider, "global"));
                provider.addCompletion(new BasicCompletion(provider, "if"));
                provider.addCompletion(new BasicCompletion(provider, "import"));
                provider.addCompletion(new BasicCompletion(provider, "in"));
                provider.addCompletion(new BasicCompletion(provider, "is"));
                provider.addCompletion(new BasicCompletion(provider, "lambda"));
                provider.addCompletion(new BasicCompletion(provider, "not"));
                provider.addCompletion(new BasicCompletion(provider, "or"));
                provider.addCompletion(new BasicCompletion(provider, "pass"));
                provider.addCompletion(new BasicCompletion(provider, "print"));
                provider.addCompletion(new BasicCompletion(provider, "raise"));
                provider.addCompletion(new BasicCompletion(provider, "return"));
                provider.addCompletion(new BasicCompletion(provider, "try"));
                provider.addCompletion(new BasicCompletion(provider, "while"));
                provider.addCompletion(new BasicCompletion(provider, "with"));
                provider.addCompletion(new BasicCompletion(provider, "yield"));
                break;
            case GROOVY:
                // Add completions for all groovy keywords. A BasicCompletion is just
                // a straightforward word completion.
                provider.addCompletion(new BasicCompletion(provider, "abstract"));
                provider.addCompletion(new BasicCompletion(provider, "as"));
                provider.addCompletion(new BasicCompletion(provider, "assert"));
                provider.addCompletion(new BasicCompletion(provider, "break"));
                provider.addCompletion(new BasicCompletion(provider, "case"));
                provider.addCompletion(new BasicCompletion(provider, "catch"));
                provider.addCompletion(new BasicCompletion(provider, "class"));
                provider.addCompletion(new BasicCompletion(provider, "const"));
                provider.addCompletion(new BasicCompletion(provider, "continue"));
                provider.addCompletion(new BasicCompletion(provider, "def"));
                provider.addCompletion(new BasicCompletion(provider, "default"));
                provider.addCompletion(new BasicCompletion(provider, "do"));
                provider.addCompletion(new BasicCompletion(provider, "else"));
                provider.addCompletion(new BasicCompletion(provider, "enum"));
                provider.addCompletion(new BasicCompletion(provider, "extends"));
                provider.addCompletion(new BasicCompletion(provider, "final"));
                provider.addCompletion(new BasicCompletion(provider, "finally"));
                provider.addCompletion(new BasicCompletion(provider, "for"));
                provider.addCompletion(new BasicCompletion(provider, "goto"));
                provider.addCompletion(new BasicCompletion(provider, "if"));
                provider.addCompletion(new BasicCompletion(provider, "implements"));
                provider.addCompletion(new BasicCompletion(provider, "import"));
                provider.addCompletion(new BasicCompletion(provider, "in"));
                provider.addCompletion(new BasicCompletion(provider, "instanceof"));
                provider.addCompletion(new BasicCompletion(provider, "interface"));
                provider.addCompletion(new BasicCompletion(provider, "native"));
                provider.addCompletion(new BasicCompletion(provider, "new"));
                provider.addCompletion(new BasicCompletion(provider, "package"));
                provider.addCompletion(new BasicCompletion(provider, "private"));
                provider.addCompletion(new BasicCompletion(provider, "property"));
                provider.addCompletion(new BasicCompletion(provider, "protected"));
                provider.addCompletion(new BasicCompletion(provider, "public"));
                provider.addCompletion(new BasicCompletion(provider, "return"));
                provider.addCompletion(new BasicCompletion(provider, "static"));
                provider.addCompletion(new BasicCompletion(provider, "strictfp"));
                provider.addCompletion(new BasicCompletion(provider, "super"));
                provider.addCompletion(new BasicCompletion(provider, "switch"));
                provider.addCompletion(new BasicCompletion(provider, "synchronized"));
                provider.addCompletion(new BasicCompletion(provider, "this"));
                provider.addCompletion(new BasicCompletion(provider, "throw"));
                provider.addCompletion(new BasicCompletion(provider, "throws"));
                provider.addCompletion(new BasicCompletion(provider, "transient"));
                provider.addCompletion(new BasicCompletion(provider, "try"));
                provider.addCompletion(new BasicCompletion(provider, "void"));
                provider.addCompletion(new BasicCompletion(provider, "volatile"));
                provider.addCompletion(new BasicCompletion(provider, "while"));

                break;
            case JAVASCRIPT:
                provider.addCompletion(new BasicCompletion(provider, "break"));
                provider.addCompletion(new BasicCompletion(provider, "case"));
                provider.addCompletion(new BasicCompletion(provider, "catch"));
                provider.addCompletion(new BasicCompletion(provider, "continue"));
                provider.addCompletion(new BasicCompletion(provider, "debugger"));
                provider.addCompletion(new BasicCompletion(provider, "default"));
                provider.addCompletion(new BasicCompletion(provider, "delete"));
                provider.addCompletion(new BasicCompletion(provider, "do"));
                provider.addCompletion(new BasicCompletion(provider, "else"));
                provider.addCompletion(new BasicCompletion(provider, "finally"));
                provider.addCompletion(new BasicCompletion(provider, "for"));
                provider.addCompletion(new BasicCompletion(provider, "function"));
                provider.addCompletion(new BasicCompletion(provider, "if"));
                provider.addCompletion(new BasicCompletion(provider, "in"));
                provider.addCompletion(new BasicCompletion(provider, "instanceof"));
                provider.addCompletion(new BasicCompletion(provider, "new"));
                provider.addCompletion(new BasicCompletion(provider, "return"));
                provider.addCompletion(new BasicCompletion(provider, "switch"));
                provider.addCompletion(new BasicCompletion(provider, "this"));
                provider.addCompletion(new BasicCompletion(provider, "throw"));
                provider.addCompletion(new BasicCompletion(provider, "try"));
                provider.addCompletion(new BasicCompletion(provider, "typeof"));
                provider.addCompletion(new BasicCompletion(provider, "var"));
                provider.addCompletion(new BasicCompletion(provider, "void"));
                provider.addCompletion(new BasicCompletion(provider, "while"));
                provider.addCompletion(new BasicCompletion(provider, "with"));
                break;
        }
    }

    private boolean addClassToProvider(String className) {
        try {
            Class c = Class.forName(className);

            Constructor[] con = c.getConstructors();
            for (int i = 0; i < con.length; i++) {
                FunctionCompletion fc = new FunctionCompletion(provider, c.getSimpleName(), "none");

                Class<?>[] p = con[i].getParameterTypes();
                List<ParameterizedCompletion.Parameter> params = new ArrayList<>();
                ParameterizedCompletion.Parameter param;
                for (int j = 0; j < p.length; j++) {
                    param = new ParameterizedCompletion.Parameter(p[j].getSimpleName(), "arg" + (j + 1));
                    //param.setDescription("This is the string to print.");
                    params.add(param);
                }

                fc.setParams(params);
                fc.setShortDescription("Constructor");
                fc.setDefinedIn(con[i].getDeclaringClass().getName());
                provider.addCompletion(fc);
            }

            //Class c = Class.forName("whitebox");
            Method m[] = c.getMethods();
            for (int i = 0; i < m.length; i++) {
                FunctionCompletion fc = new FunctionCompletion(provider, c.getSimpleName() + "." + m[i].getName(), m[i].getReturnType().getSimpleName());
                Class<?>[] p = m[i].getParameterTypes();
                List<ParameterizedCompletion.Parameter> params = new ArrayList<>();
                ParameterizedCompletion.Parameter param;
                for (int j = 0; j < p.length; j++) {
                    param = new ParameterizedCompletion.Parameter(p[j].getSimpleName(), "arg" + (j + 1));
                    params.add(param);
                }
                fc.setParams(params);
                fc.setDefinedIn(m[i].getDeclaringClass().getName());
                provider.addCompletion(fc);
                listOfImportedMethods.add(c.getSimpleName() + "." + m[i].getName());
                listOfMethodReturns.put(c.getSimpleName() + "." + m[i].getName(), m[i].getReturnType().getCanonicalName());
            }

            Field f[] = c.getFields();
            for (int i = 0; i < f.length; i++) {
                VariableCompletion vc = new VariableCompletion(provider, c.getSimpleName() + "." + f[i].getName(), f[i].getType().toString());
                vc.setDefinedIn(f[i].getDeclaringClass().getName());
                provider.addCompletion(vc);
            }
            return true;
        } catch (ClassNotFoundException | SecurityException e) {
            return false;
        }
    }

    private boolean addVariableToProvider(String variableName, Class c) {
        try {
            if (c != null) {

                String className = c.getCanonicalName();
                if (!listOfImportedClasses.contains(className)) {
                    if (addClassToProvider(className)) {
                        listOfImportedClasses.add(className);
//                        Package p = c.getPackage();
//                        importPackage(p);
                    }
                }


                VariableCompletion vc = new VariableCompletion(provider, variableName, c.getCanonicalName());
                provider.addCompletion(vc);

                //provider.addCompletion(new BasicCompletion(provider, variableName));
                Method m[] = c.getMethods();
                for (int i = 0; i < m.length; i++) {
                    StringBuilder name = new StringBuilder(variableName);
                    name.append(".").append(m[i].getName());

                    FunctionCompletion fc = new FunctionCompletion(provider, name.toString(), m[i].getReturnType().getSimpleName());

                    Class<?>[] p = m[i].getParameterTypes();
                    List<ParameterizedCompletion.Parameter> params = new ArrayList<>();
                    ParameterizedCompletion.Parameter param;
                    for (int j = 0; j < p.length; j++) {
                        param = new ParameterizedCompletion.Parameter(p[j].getSimpleName(), "arg" + (j + 1));
                        params.add(param);
                    }

                    fc.setParams(params);
                    fc.setDefinedIn(m[i].getDeclaringClass().getName());
                    provider.addCompletion(fc);
                    listOfImportedMethods.add(name.toString());
                    listOfMethodReturns.put(name.toString(), m[i].getReturnType().getCanonicalName());
                }

                Field f[] = c.getFields();
                for (int i = 0; i < f.length; i++) {
                    StringBuilder name = new StringBuilder(variableName);
                    name.append(".").append(f[i].getName());

                    vc = new VariableCompletion(provider, name.toString(), f[i].getType().toString());
                    vc.setDefinedIn(f[i].getDeclaringClass().getName());
                    provider.addCompletion(vc);
                }
            } else {
                VariableCompletion vc = new VariableCompletion(provider, variableName, "Object");
                provider.addCompletion(vc);
            }
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    private void handleError(String msg) {
        errOut.append(msg + "\n");
    }

    private void initScriptEngine() {
        try {

            if (language == PYTHON) {
                if (System.getProperty("python.home") == null) {
                    System.setProperty("python.home", "");
                }
            }
            engine = mgr.getEngineByName(language.toString().toLowerCase());
            PrintWriter out = new PrintWriter(new Scripter.TextAreaWriter(textArea));

            engine.getContext().setWriter(out);
            engine.getContext().setErrorWriter(out);

            if (language == PYTHON && sourceFile != null) {
                engine.put("__file__", sourceFile);
            }
            engine.put("pluginHost", (WhiteboxPluginHost) host);
            engine.put("args", new String[0]);

            // update the statusbar
            ScriptEngineFactory scriptFactory = engine.getFactory();
            statusLabel.setText(bundle.getString("ScriptingLanguage") + ": " + scriptFactory.getLanguageName());
        } catch (Exception e) {
            handleError(e.getMessage());
        }
    }

    private void openFile() {

        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(true);
        fc.setCurrentDirectory(new File(scriptsDirectory));

        FileFilter ft = new FileNameExtensionFilter("Javascript " + bundle.getString("Files"), "js");
        fc.addChoosableFileFilter(ft);
        ft = new FileNameExtensionFilter("Groovy " + bundle.getString("Files"), "groovy");
        fc.addChoosableFileFilter(ft);
        ft = new FileNameExtensionFilter("Python " + bundle.getString("Files"), "py");
        fc.addChoosableFileFilter(ft);

        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            sourceFile = file.toString();
            //String fileDirectory = file.getParentFile() + pathSep;

            if (sourceFile.toLowerCase().contains(".py")) {
                //language = Scripter.ScriptingLanguage.PYTHON;
                setLanguage(Scripter.ScriptingLanguage.PYTHON);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
            } else if (sourceFile.toLowerCase().contains(".groovy")) {
                //language = Scripter.ScriptingLanguage.GROOVY;
                setLanguage(Scripter.ScriptingLanguage.GROOVY);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
            } else {
                //language = Scripter.ScriptingLanguage.JAVASCRIPT;
                setLanguage(Scripter.ScriptingLanguage.JAVASCRIPT);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
            }

            //editor.setContentType("text/" + language);
            editor.setEditable(true);


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

            editor.setEditable(true);
            editor.setCaretPosition(0);

            this.setTitle("Whitebox Scripter: " + new File(sourceFile).getName());
        }
    }

    private void save() {
        if (sourceFile == null) {
            String extension = "";
            switch (language) {
                case PYTHON:
                    extension = ".py";
                    break;
                case GROOVY:
                    extension = ".groovy";
                    break;
                case JAVASCRIPT:
                    extension = ".js";
                    break;
            }

            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
            fc.setAcceptAllFileFilterUsed(true);
            fc.setFileHidingEnabled(true);

            FileFilter ft = new FileNameExtensionFilter("Javascript " + bundle.getString("Files"), "js");
            fc.addChoosableFileFilter(ft);
            ft = new FileNameExtensionFilter("Groovy " + bundle.getString("Files"), "groovy");
            fc.addChoosableFileFilter(ft);
            ft = new FileNameExtensionFilter("Python " + bundle.getString("Files"), "py");
            fc.addChoosableFileFilter(ft);
            //fc.setFileFilter(ft);

            fc.setCurrentDirectory(new File(scriptsDirectory));
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
                sourceFile = file.toString();
                if (sourceFile.toLowerCase().contains(".py")) {
                    setLanguage(Scripter.ScriptingLanguage.PYTHON);
                    //language = Scripter.ScriptingLanguage.PYTHON;
                } else if (sourceFile.toLowerCase().contains(".groovy")) {
                    setLanguage(Scripter.ScriptingLanguage.GROOVY);
                    //language = Scripter.ScriptingLanguage.GROOVY;
                } else if (sourceFile.toLowerCase().contains(".js")) {
                    setLanguage(Scripter.ScriptingLanguage.JAVASCRIPT);
                    //language = Scripter.ScriptingLanguage.JAVASCRIPT;
                }

                this.setTitle("Whitebox Scripter: " + new File(sourceFile).getName());
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

    private void saveAs() {
        sourceFile = null;
        save();
    }

    private void print() {
        try {
            editor.print();
        } catch (Exception e) {
            host.logException("Error in Scripter.", e);
        }
    }

    private void execute() {
        try {
            host.resetRequestForOperationCancel();
            String expression = editor.getText();
            execWithThread(expression);
//            Object result = engine.eval(expression);
        } catch (Exception e) {
            errOut.append(e.getMessage() + "\n");
        }
    }

    private void execWithThread(final String scriptString) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    engine.eval(scriptString);
                } catch (ScriptException e) {
                    errOut.append(e.getMessage() + "\n");
                }
            }
        };
        final Thread t = new Thread(r);
        t.start();
    }

    private void comment() {
        try {
            String selectedText = editor.getSelectedText();
            int start = editor.getSelectionStart();
            int end = editor.getSelectionEnd();

            if (selectedText == null || selectedText.isEmpty()) {
                toggleComment(editor.getCaretLineNumber());
            } else {
                int startLine = editor.getLineOfOffset(start);
                int endLine = editor.getLineOfOffset(end);
                for (int i = startLine; i <= endLine; i++) {
                    toggleComment(i);
                }
            }
        } catch (Exception e) {
            // do nothing
        }
    }

    private void indentSelection() {
        try {
            int start = editor.getSelectionStart();
            int end = editor.getSelectionEnd();

            int startLine = editor.getLineOfOffset(start);
            int endLine = editor.getLineOfOffset(end);
            for (int i = startLine; i <= endLine; i++) {
                int j = editor.getLineStartOffset(i);
                // add a line comment tag.
                editor.insert("\t", j);
            }
        } catch (Exception e) {
            // do nothing
        }
    }

    private void outdentSelection() {
        try {
            int start = editor.getSelectionStart();
            int end = editor.getSelectionEnd();

            int startLine = editor.getLineOfOffset(start);
            int endLine = editor.getLineOfOffset(end);
            for (int i = startLine; i <= endLine; i++) {
                int j = editor.getLineStartOffset(i);
                if (editor.getText(j, "\t".length()).startsWith("\t")) {
                    // remove the indent
                    editor.replaceRange("", j, j + "\t".length());
                } else if (editor.getText(j, "    ".length()).startsWith("    ")) {
                    // remove the indent
                    editor.replaceRange("", j, j + "    ".length());
                }
            }
        } catch (Exception e) {
            // do nothing
        }
    }

    private void toggleComment(int lineNum) {
        try {
            int start = editor.getLineStartOffset(lineNum);
            String openingCharacters = editor.getText(start, language.getCommentMarker().length());
            if (openingCharacters.startsWith(language.getCommentMarker())) {
                // remove the line comment tag.
                editor.replaceRange("", start, start + language.getCommentMarker().length());
            } else {
                // add a line comment tag.
                editor.insert(language.getCommentMarker(), start);
            }
        } catch (Exception e) {
            // do nothing
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        switch (actionCommand.toLowerCase()) {
            case "indent":
                indentSelection();
                break;
            case "outdent":
                outdentSelection();
                break;
            case "close":
                if (editorDirty) {
                    Object[] options = {"Yes", "No", "Cancel"};
                    int n = JOptionPane.showOptionDialog(this,
                            "The source code has changed. Would you like to save it?",
                            "Whitebox GAT Message",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null, //do not use a custom Icon
                            options, //the titles of buttons
                            options[0]); //default button title

                    if (n == JOptionPane.YES_OPTION) {
                        save();
                    } else if (n == JOptionPane.NO_OPTION) {
                        // do nothing
                    } else if (n == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                }
                editor.setText("");
                sourceFile = null;
                break;
            case "exit":
                if (editorDirty) {
                    Object[] options = {"Yes", "No", "Cancel"};
                    int n = JOptionPane.showOptionDialog(this,
                            "The source code has changed. Would you like to save it?",
                            "Whitebox GAT Message",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null, //do not use a custom Icon
                            options, //the titles of buttons
                            options[0]); //default button title

                    if (n == JOptionPane.YES_OPTION) {
                        save();
                    } else if (n == JOptionPane.NO_OPTION) {
                        // do nothing
                    } else if (n == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                }
                this.dispose();
                break;
            case "execute":
                if (sourceFile != null && editorDirty) {
                    save();
                }
                execute();
                break;
            case "open":
                openFile();
                resetAutocompletion();
                scanDoc();
                break;
            case "print":
                print();
                break;
            case "python":
                setLanguage(Scripter.ScriptingLanguage.PYTHON);
                break;
            case "groovy":
                setLanguage(Scripter.ScriptingLanguage.GROOVY);
                break;
            case "javascript":
                setLanguage(Scripter.ScriptingLanguage.JAVASCRIPT);
                break;
            case "comment":
                comment();
                break;
            case "save":
                save();
                break;
            case "saveas":
                saveAs();
                break;
            case "clearconsole":
                textArea.setText("");
                break;
            case "generatedata":
                this.firePropertyChange("generateData", false, true);
                break;
            case "undo":
                editor.undoLastAction();
                break;
            case "redo":
                editor.redoLastAction();
                break;
            case "cut":
                editor.cut();
                break;
            case "copy":
                editor.copy();
                break;
            case "past":
                editor.paste();
                resetAutocompletion();
                scanDoc();
                break;
            case ("findnext"):
            case ("findprev"):
                boolean forward = "findnext".equals(actionCommand.toLowerCase());

                // Create an object defining our search parameters.
                SearchContext context = new SearchContext();
                String text = searchField.getText();
                if (text.length() == 0) {
                    return;
                }
                context.setSearchFor(text);
                context.setMatchCase(matchCaseCB.isSelected());
                context.setRegularExpression(regexCB.isSelected());
                context.setSearchForward(forward);
                context.setWholeWord(wholeWordCB.isSelected());

                boolean found = SearchEngine.find(editor, context);
                if (!found) {
                    JOptionPane.showMessageDialog(this, "Text not found");
                }

                break;

        }
    }

//    @Override
//    public void dispose() {
//        if (editorDirty) {
//            Object[] options = {"Yes", "No", "Cancel"};
//            int n = JOptionPane.showOptionDialog(this,
//                    "The source code has changed. Would you like to save it?",
//                    "Whitebox GAT Message",
//                    JOptionPane.YES_NO_OPTION,
//                    JOptionPane.QUESTION_MESSAGE,
//                    null, //do not use a custom Icon
//                    options, //the titles of buttons
//                    options[0]); //default button title
//
//            if (n == JOptionPane.YES_OPTION) {
//                save();
//            } else if (n == JOptionPane.NO_OPTION) {
//                // do nothing
//            } else if (n == JOptionPane.CANCEL_OPTION) {
//                return;
//            }
//        }
//        super.dispose();
//    }
    public void setLanguage(Scripter.ScriptingLanguage lang) {


        Scripter.ScriptingLanguage oldLang = this.language;
        if (lang == null) {
            return;
        }

        switch (lang) {
            case PYTHON:
                python.setState(true);
                groovy.setState(false);
                javascript.setState(false);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
                break;
            case GROOVY:
                python.setState(false);
                groovy.setState(true);
                javascript.setState(false);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
                break;
            case JAVASCRIPT:
                groovy.setState(false);
                python.setState(false);
                javascript.setState(true);
                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                break;
//            case RUBY:
//                python.setState(false);
//                groovy.setState(false);
//                javascript.setState(false);
//                editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY);
//                break;
        }

        this.language = lang;

//        setupAutoComplete();

//        this.editor.setContentType("text/" + this.language);
        initScriptEngine();

        resetAutocompletion();
        scanDoc();

        this.firePropertyChange(PROP_SCRIPTING_LANGUAGE, oldLang, lang);
    }

    public void showGenerateDataButton(boolean show) {
        generateDataButton.setVisible(show);
    }

    /**
     * Creates a CompiledScript object using the provided text and the currently
     * selected ScriptEngine.
     *
     * @param script
     * @return
     */
    public CompiledScript compileScript() {
        try {

            CompiledScript compiled = ((Compilable) engine).compile(this.editor.getText());

            return compiled;
        } catch (ScriptException e) {
            System.out.println(e);
        }

        return null;
    }

    public Bindings createBindingsObject() {
        return engine.createBindings();
    }

    public void setEditorText(String text) {
        this.editor.setText(text);
    }

    public Scripter.ScriptingLanguage getLanguage() {
        return this.language;
    }

    public static void main(String args[]) throws ScriptException {
        Scripter scripter = new Scripter(null, false);
        scripter.setVisible(true);
    }

    public final class TextAreaWriter extends Writer {

        private final JTextArea textArea;

        public TextAreaWriter(final JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            textArea.append(new String(cbuf, off, len));
        }
    }
//    class JTextAreaInputStream extends InputStream {
//
//        byte[] contents;
//        int pointer = 0;
//
//        public JTextAreaInputStream(final JTextArea text) {
//
//            text.addKeyListener(new KeyAdapter() {
//                @Override
//                public void keyReleased(KeyEvent e) {
//                    if (e.getKeyChar() == '\n') {
//                        contents = text.getText().getBytes();
//                        pointer = 0;
//                        text.setText("");
//                    }
//                    super.keyReleased(e);
//                }
//            });
//        }
//
//        @Override
//        public int read() throws IOException {
//            if (pointer >= contents.length) {
//                return -1;
//            }
//            return this.contents[pointer++];
//        }
//    }
//    final class TextAreaStreamer extends InputStream implements ActionListener {
//
//        private JTextArea textArea;
//        private String str = null;
//        private int pos = 0;
//
//        public TextAreaStreamer(JTextArea jta) {
//            textArea = jta;
//            textArea.addKeyListener(new KeyAdapter() {
//                @Override
//                public void keyReleased(KeyEvent e) {
//                    if (e.getKeyChar() == '\n') {
//                        str = textArea.getText(); //.getBytes();
//                        pos = 0;
//                        read();
//                    }
//                    super.keyReleased(e);
//                }
//            });
//        }
//
//        //gets triggered everytime that "Enter" is pressed on the textfield
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            str = textArea.getText() + "\n";
//            pos = 0;
//            textArea.setText("");
//            synchronized (this) {
//                //maybe this should only notify() as multiple threads may
//                //be waiting for input and they would now race for input
//                this.notify();
//            }
//        }
//
//        @Override
//        public int read() {
//            //test if the available input has reached its end
//            //and the EOS should be returned 
//            if (str != null && pos == str.length()) {
//                str = null;
//                //this is supposed to return -1 on "end of stream"
//                //but I'm having a hard time locating the constant
//                return java.io.StreamTokenizer.TT_EOF;
//            }
//            //no input available, block until more is available because that's
//            //the behavior specified in the Javadocs
//            while (str == null || pos >= str.length()) {
//                try {
//                    //according to the docs read() should block until new input is available
//                    synchronized (this) {
//                        this.wait();
//                    }
//                } catch (InterruptedException ex) {
//                    ex.printStackTrace();
//                }
//            }
//            //read an additional character, return it and increment the index
//            return str.charAt(pos++);
//        }
//    }
}
