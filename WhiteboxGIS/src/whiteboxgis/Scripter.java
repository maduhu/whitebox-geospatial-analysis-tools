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
package whiteboxgis;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.actions.ActionUtils;
import whitebox.interfaces.Communicator;
import whitebox.interfaces.WhiteboxPluginHost;
import static whiteboxgis.Scripter.PROP_SCRIPTING_LANGUAGE;
import static whiteboxgis.Scripter.ScriptingLanguage.GROOVY;
import static whiteboxgis.Scripter.ScriptingLanguage.JAVASCRIPT;
import static whiteboxgis.Scripter.ScriptingLanguage.PYTHON;

//import whitebox.utilities.Console;
/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class Scripter extends JDialog implements ActionListener {

    private String pathSep;
    private String graphicsDirectory;
    private String scriptsDirectory;
    private String sourceFile = null;
    private Communicator host = null;
    private JEditorPane editor = new JEditorPane();
    private JScrollPane scroll = new JScrollPane();
    private ScriptEngineManager mgr = new ScriptEngineManager();
    List<ScriptEngineFactory> factories = mgr.getEngineFactories();
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

    public enum ScriptingLanguage {

        PYTHON("Python", "#"), GROOVY("Groovy", "//"), JAVASCRIPT("Javascript", "//");
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
            findGraphicsDirectory(new File(applicationDirectory));
            findScriptDirectory(new File(applicationDirectory));

            if (owner != null && owner instanceof Communicator) {
                host = (Communicator) owner;
                bundle = host.getGuiLabelsBundle();
            }

            initUI();
        } catch (Exception e) {
            handleError(e.getMessage());
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
            this.setPreferredSize(new Dimension(700, 500));

            Container c = this.getContentPane();

            createMenu();
            JToolBar toolbar = createToolbar();
            c.add(toolbar, BorderLayout.PAGE_START);

            scroll = new JScrollPane(editor);

            DefaultSyntaxKit.initKit();
            editor.setContentType("text/" + language);
            editor.setEditable(true);
            editor.setCaretPosition(0);

            JScrollPane scroll2 = new JScrollPane(textArea);

            splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, scroll2);
            splitPane.setDividerLocation(300);

            c.add(splitPane);

            status = new JPanel();
            status.setLayout(new BoxLayout(status, BoxLayout.LINE_AXIS));
            status.setPreferredSize(new Dimension(10, 24));
            status.add(Box.createHorizontalStrut(5));
            status.add(statusLabel);
            status.add(Box.createHorizontalGlue());

            c.add(status, BorderLayout.PAGE_END);

            this.pack();
        } catch (Exception e) {
            handleError(e.getMessage());
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

        JButton executeBtn = makeToolBarButton("Execute.png", "execute",
                bundle.getString("ExecuteCode"), "Execute");
        toolbar.add(executeBtn);

        JButton toggleComment = makeToolBarButton("Comment.png", "Comment",
                bundle.getString("ToggleComments"), "Comment");
        toolbar.add(toggleComment);

        toolbar.addSeparator();

        JButton clearConsole = makeToolBarButton("ClearConsole.png", "clearConsole",
                bundle.getString("ClearConsole"), bundle.getString("ClearConsole"));
        toolbar.add(clearConsole);

        toolbar.addSeparator();

        generateDataButton = makeToolBarButton("GenerateData.png", "generateData",
                bundle.getString("GenerateColumnData"), "Generate Data");
        toolbar.add(generateDataButton);

        showGenerateDataButton(false);

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
            close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            close.addActionListener(this);
            fileMenu.add(close);

            fileMenu.addSeparator();

            JMenuItem exit = new JMenuItem(bundle.getString("Exit"));
            exit.setActionCommand("exit");
            exit.addActionListener(this);
            exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            fileMenu.add(exit);

            menubar.add(fileMenu);

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
            execute.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            sourceMenu.add(execute);

            sourceMenu.addSeparator();
            JMenuItem toggleComments = new JMenuItem(bundle.getString("ToggleComments"));
            toggleComments.setActionCommand("toggleComments");
            toggleComments.addActionListener(this);
            toggleComments.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            sourceMenu.add(toggleComments);

            menubar.add(sourceMenu);


            this.setJMenuBar(menubar);

        } catch (Exception e) {
            handleError(e.getMessage());
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
            handleError(e.getMessage());
        }

        return button;
    }

    private void handleError(String msg) {
        errOut.append(msg + "\n");
    }

    private void initScriptEngine() {
        try {

            engine = mgr.getEngineByName(language.toString().toLowerCase());
            //StringWriter sw = new StringWriter();
            //PrintWriter pw = new PrintWriter(sw);
            PrintWriter out = new PrintWriter(new Scripter.TextAreaWriter(textArea));
            //StreamReader in = new StreamReader(new TextAreaReader(textArea));
            engine.getContext().setWriter(out);

            //engine.put("WhiteboxHost", host);
            engine.put("PluginHost", (WhiteboxPluginHost) host);

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
                language = Scripter.ScriptingLanguage.PYTHON;
            } else if (sourceFile.toLowerCase().contains(".groovy")) {
                language = Scripter.ScriptingLanguage.GROOVY;
            } else {
                language = Scripter.ScriptingLanguage.JAVASCRIPT;
            }
            editor.setContentType("text/" + language);
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
                    language = Scripter.ScriptingLanguage.PYTHON;
                } else if (sourceFile.toLowerCase().contains(".groovy")) {
                    language = Scripter.ScriptingLanguage.GROOVY;
                } else if (sourceFile.toLowerCase().contains(".js")) {
                    language = Scripter.ScriptingLanguage.JAVASCRIPT;
                }

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

    private void saveAs() {
        sourceFile = null;
        save();
    }

    private void print() {
        try {
            editor.print();
        } catch (Exception e) {
            handleError(e.getMessage());
        }
    }

    private void execute() {
        try {
            String expression = editor.getText();
            Object result = engine.eval(expression);
        } catch (Exception e) {
            errOut.append(e.getMessage() + "\n");
        }
    }

    private void comment() {
        String lineCommentStart = language.getCommentMarker();

        Pattern lineCommentPattern = null;
        if (lineCommentPattern == null) {
            lineCommentPattern = Pattern.compile("(^" + lineCommentStart + ")(.*)");
        }
        String[] lines = ActionUtils.getSelectedLines(editor);
        int start = editor.getSelectionStart();
        StringBuilder toggled = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            Matcher m = lineCommentPattern.matcher(lines[i]);
            if (m.find()) {
                toggled.append(m.replaceFirst("$2"));
            } else {
                toggled.append(lineCommentStart);
                toggled.append(lines[i]);
            }
            toggled.append('\n');
        }
        editor.replaceSelection(toggled.toString());
        editor.select(start, start + toggled.length());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("close")) {
            editor.setText("");
            sourceFile = null;
        } else if (actionCommand.equals("exit")) {
            this.dispose();
        } else if (actionCommand.equals("execute")) {
            execute();
        } else if (actionCommand.equals("open")) {
            openFile();
        } else if (actionCommand.equals("print")) {
            print();
        } else if (actionCommand.equals("python")) {
            setLanguage(Scripter.ScriptingLanguage.PYTHON);
        } else if (actionCommand.equals("groovy")) {
            setLanguage(Scripter.ScriptingLanguage.GROOVY);
        } else if (actionCommand.equals("javascript")) {
            setLanguage(Scripter.ScriptingLanguage.JAVASCRIPT);
        } else if (actionCommand.equals("Comment")) {
            comment();
        } else if (actionCommand.equals("save")) {
            save();
        } else if (actionCommand.equals("saveAs")) {
            saveAs();
        } else if (actionCommand.equals("clearConsole")) {
            textArea.setText("");
        } else if (actionCommand.equals("generateData")) {
            this.firePropertyChange("generateData", false, true);
        }
    }

    public void setLanguage(Scripter.ScriptingLanguage lang) {


        Scripter.ScriptingLanguage oldLang = this.language;
        if (lang == null) {
            return;
        }

        switch (lang) {
            case PYTHON:
                groovy.setState(false);
                javascript.setState(false);
                break;
            case GROOVY:
                python.setState(false);
                javascript.setState(false);
                break;
            case JAVASCRIPT:
                groovy.setState(false);
                python.setState(false);
                break;
        }

        language = lang;

        editor.setContentType("text/" + language);
        initScriptEngine();

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
}
