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

//import whitebox.utilities.Console;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class Scripter extends JFrame implements ActionListener {

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
    private PrintWriter errOut = new PrintWriter(new TextAreaWriter(textArea));
    private String language = "python";
    private JPanel status = null;
    private JLabel statusLabel = new JLabel();
    private JCheckBoxMenuItem python = new JCheckBoxMenuItem("Python");
    private JCheckBoxMenuItem groovy = new JCheckBoxMenuItem("Groovy");
    private JCheckBoxMenuItem javascript = new JCheckBoxMenuItem("Javascript");
    
    public Scripter(Frame owner, boolean modal) {
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
            //findResourcesDirectory(new File(applicationDirectory));
            findGraphicsDirectory(new File(applicationDirectory));
            findScriptDirectory(new File(applicationDirectory));
            
            if (owner != null && owner instanceof Communicator) {
                host = (Communicator) owner;
            }
            
            initUI();
        } catch (Exception e) {
            handleError(e.getMessage());
        }
    }
    
//    private void findResourcesDirectory(File dir) {
//        File[] files = dir.listFiles();
//        for (int x = 0; x < files.length; x++) {
//            if (files[x].isDirectory()) {
//                if (files[x].toString().endsWith(pathSep + "resources")) {
//                    resourcesDirectory = files[x].toString() + pathSep;
//                    break;
//                } else {
//                    findResourcesDirectory(files[x]);
//                }
//            }
//        }
//    }

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
            }

            if (System.getProperty("mrj.version") != null) {
                System.setProperty("com.apple.macos.useScreenMenuBar", "true");
                System.setProperty("apple.laf.useScreenMenuBar", "true");
            }

            errOut = new PrintWriter(new TextAreaWriter(textArea));
            
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
        
        JButton openBtn = makeToolBarButton("open.png", "open", "Open file", "Open");
        toolbar.add(openBtn);
        
        JButton saveBtn = makeToolBarButton("SaveMap.png", "save", "Save file", "Save");
        toolbar.add(saveBtn);
        
        JButton printBtn = makeToolBarButton("print.png", "print", "Print", "Print");
        toolbar.add(printBtn);
        
        toolbar.addSeparator();
        
        JButton executeBtn = makeToolBarButton("Execute.png", "execute", "Execute code", "Execute");
        toolbar.add(executeBtn);
        
        JButton toggleComment = makeToolBarButton("Comment.png", "Comment", "Toggle Comments", "Comment");
        toolbar.add(toggleComment);
        
        toolbar.addSeparator();
        
        JButton clearConsole = makeToolBarButton("ClearConsole.png", "clearConsole", "Clear the Console", "Clear Console");
        toolbar.add(clearConsole);
        
        return toolbar;

    }

    private void createMenu() {
        try {
            JMenuBar menubar = new JMenuBar();

            JMenu fileMenu = new JMenu("File");
            JMenuItem open = new JMenuItem("Open File");
            open.setActionCommand("open");
            open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            open.addActionListener(this);
            fileMenu.add(open);
            JMenuItem save = new JMenuItem("Save");
            save.setActionCommand("save");
            save.addActionListener(this);
            save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            fileMenu.add(save);
            JMenuItem saveAs = new JMenuItem("Save As...");
            saveAs.setActionCommand("saveAs");
            saveAs.addActionListener(this);
            fileMenu.add(saveAs);
            JMenuItem print = new JMenuItem("Print");
            print.setActionCommand("print");
            print.addActionListener(this);
            print.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            fileMenu.add(print);
            JMenuItem close = new JMenuItem("Close File");
            close.setActionCommand("close");
            close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            close.addActionListener(this);
            fileMenu.add(close);
            
            fileMenu.addSeparator();
            
            JMenuItem exit = new JMenuItem("Exit");
            exit.setActionCommand("exit");
            exit.addActionListener(this);
            exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            fileMenu.add(exit);
            
            menubar.add(fileMenu);

            JMenu languageMenu = new JMenu("Language");
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

            JMenu sourceMenu = new JMenu("Source");
            JMenuItem execute = new JMenuItem("Execute");
            execute.setActionCommand("execute");
            execute.addActionListener(this);
            execute.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            sourceMenu.add(execute);
            
            sourceMenu.addSeparator();
            JMenuItem toggleComments = new JMenuItem("Toggle Comments");
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

//            for (ScriptEngineFactory factory : factories) {
//                System.out.println("ScriptEngineFactory Info");
//                String engName = factory.getEngineName();
//                String engVersion = factory.getEngineVersion();
//                String langName = factory.getLanguageName();
//                String langVersion = factory.getLanguageVersion();
//                System.out.printf("\tScript Engine: %s (%s)\n",
//                        engName, engVersion);
//                List<String> engNames = factory.getNames();
//                for (String name : engNames) {
//                    System.out.printf("\tEngine Alias: %s\n", name);
//                }
//                System.out.printf("\tLanguage: %s (%s)\n",
//                        langName, langVersion);
//            }

            engine = mgr.getEngineByName(language);
            //StringWriter sw = new StringWriter();
            //PrintWriter pw = new PrintWriter(sw);
            PrintWriter out = new PrintWriter(new TextAreaWriter(textArea));
            //StreamReader in = new StreamReader(new TextAreaReader(textArea));
            engine.getContext().setWriter(out);
            
            //engine.put("WhiteboxHost", host);
            engine.put("PluginHost", (WhiteboxPluginHost) host);
            
            // update the statusbar
            ScriptEngineFactory scriptFactory = engine.getFactory();
            statusLabel.setText("Script Language: " + scriptFactory.getLanguageName());
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

        FileFilter ft = new FileNameExtensionFilter("Javascript Files", "js");
        fc.addChoosableFileFilter(ft);
        ft = new FileNameExtensionFilter("Groovy Files", "groovy");
        fc.addChoosableFileFilter(ft);
        ft = new FileNameExtensionFilter("Python Files", "py");
        fc.addChoosableFileFilter(ft);
        
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            sourceFile = file.toString();
            //String fileDirectory = file.getParentFile() + pathSep;
            
            if (sourceFile.toLowerCase().contains(".py")) {
                language = "python";
            } else if (sourceFile.toLowerCase().contains(".groovy")) {
                language = "groovy";
            } else {
                language = "javascript";
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
            if (language.toLowerCase().equals("python")) {
                extension = ".py";
            } else if (language.toLowerCase().equals("groovy")) {
                extension = ".groovy";
            } else if (language.toLowerCase().equals("javascript")) {
                extension = ".js";
            }
            
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
            fc.setAcceptAllFileFilterUsed(true);
            fc.setFileHidingEnabled(true);

            FileFilter ft = new FileNameExtensionFilter("Javascript Files", "js");
            fc.addChoosableFileFilter(ft);
            ft = new FileNameExtensionFilter("Groovy Files", "groovy");
            fc.addChoosableFileFilter(ft);
            ft = new FileNameExtensionFilter("Python Files", "py");
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
                sourceFile = file.toString();
                if (sourceFile.toLowerCase().contains(".py")) {
                    language = "python";
                } else if (sourceFile.toLowerCase().contains(".groovy")) {
                    language = "groovy";
                } else if (sourceFile.toLowerCase().contains(".js")) {
                    language = "javascript";
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
        String lineCommentStart = null;
        if (language.equals("groovy") || language.equals("javascript")) {
            lineCommentStart = "// ";
        } else {
            lineCommentStart = "# ";
        }
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
            groovy.setState(false);
            javascript.setState(false);
            language = "python";
            editor.setContentType("text/" + language);
            initScriptEngine();
        } else if (actionCommand.equals("groovy")) {
            python.setState(false);
            javascript.setState(false);
            language = "groovy";
            editor.setContentType("text/" + language);
            initScriptEngine();
        } else if (actionCommand.equals("javascript")) {
            python.setState(false);
            groovy.setState(false);
            language = "javascript";
            editor.setContentType("text/" + language);
            initScriptEngine();
        } else if (actionCommand.equals("Comment")) {
            comment();
        } else if (actionCommand.equals("save")) {
            save();
        } else if (actionCommand.equals("saveAs")) {
            saveAs();
        } else if (actionCommand.equals("clearConsole")) {
            textArea.setText("");
        }
    }

    public static void main(String args[]) throws ScriptException {
        Scripter scripter = new Scripter(null, false);
        scripter.setVisible(true);

//        ScriptEngineManager mgr = new ScriptEngineManager();
//        ScriptEngine pyEngine = mgr.getEngineByName("python");
//        try {
//            pyEngine.eval("print \"Python - Hello, world!\"");
//            pyEngine.eval("a = 4.3");
//            pyEngine.eval("b = 6.7");
//            pyEngine.eval("print a + b");
//            
////            pyEngine.eval("from whitebox.geospatialfiles import WhiteboxRaster");
////            pyEngine.eval("wb = WhiteboxRaster(\"/Users/johnlindsay/Documents/Data/tmp1.dep\", \"r\")");
////            pyEngine.eval("print wb.getValue(24,56)");
////            
//            //pyEngine.eval("from rastercalculator import RasterCalculator");
//            pyEngine.eval("from rastercalculator import ProcessExpression");
//            pyEngine.eval("str = '[tmp2]=[tmp1]*10'");
//            pyEngine.eval("print(str)");
//            pyEngine.eval("pe = ProcessExpression(\"/Users/johnlindsay/Documents/Data/\", str)");
//            pyEngine.eval("pe.run()");
//            pyEngine.eval("print(pe.getReturnValue())");
//            
//            //pyEngine.eval("from javax.swing import JFrame");
//            //pyEngine.eval("frame = JFrame('Hello, Jython!', defaultCloseOperation = JFrame.EXIT_ON_CLOSE, size = (300, 300))");
//            //pyEngine.eval("rc = RasterCalculator(frame, True, '')");
//            //pyEngine.eval("rc.visible = True");
//            //pyEngine.eval("frame.visible = True");
//            
//            
//            // create a script engine manager
////            ScriptEngineManager factory = new ScriptEngineManager();
////            // create a JavaScript engine
////            ScriptEngine engine = factory.getEngineByName("JavaScript");
////            // evaluate JavaScript code from String
////            engine.eval("println('Hello, World js')");
//        } catch (Exception e) {
//            System.out.println(e.getStackTrace());
//            //ex.printStackTrace();
//        }       
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

////    public final class TextAreaReader extends Reader {
////
////        private final JTextArea textArea;
////
////        public TextAreaWriter(final JTextArea textArea) {
////            this.textArea = textArea;
////        }
////
////        @Override
////        public void flush() {
////        }
////
////        @Override
////        public void close() {
////        }
////
////        @Override
////        public void write(char[] cbuf, int off, int len) throws IOException {
////            textArea.append(new String(cbuf, off, len));
////        }
////
////        @Override
////        public int read(char[] chars, int i, int i1) throws IOException {
////            throw new UnsupportedOperationException("Not supported yet.");
////        }
////    }
//    class TextAreaStreamer extends InputStream implements ActionListener {
//
//        private JTextArea tf;
//        private String str = null;
//        private int pos = 0;
//
//        public TextAreaStreamer(JTextArea jta) {
//            tf = jta;
//        }
//
//        //gets triggered everytime that "Enter" is pressed on the textfield
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            str = tf.getText() + "\n";
//            pos = 0;
//            tf.setText("");
//            synchronized (this) {
//                //maybe this should only notify() as multiple threads may
//                //be waiting for input and they would now race for input
//                this.notifyAll();
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
//
//    public final class TextAreaReader extends Reader implements KeyListener {
//
//        JTextArea mJArea;
//        TextArea mAWTArea;
//        Object mKeyLock = new Object();
//        Object mLineLock = new Object();
//        String mLastLine;
//        int mLastKeyCode = 1;
//
//        public TextAreaReader(JTextArea area) {
//            super();
//            mJArea = area;
//            mJArea.addKeyListener(this);
//        }
//
//        public TextAreaReader(TextArea area) {
//            super();
//            mAWTArea = area;
//            mAWTArea.addKeyListener(this);
//        }
//
//        @Override
//        public void keyPressed(KeyEvent ke) {
//            mLastKeyCode = ke.getKeyCode();
//            synchronized (mKeyLock) {
//                mKeyLock.notifyAll();
//            }
//            if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
//                if (mJArea != null) {
//                    String txt = mJArea.getText();
//                    int idx = txt.lastIndexOf('\n', mJArea.getCaretPosition() - 1);
//                    mLastLine = txt.substring(idx != -1 ? idx : 0, mJArea.getCaretPosition());//txt.length());
//                    synchronized (mLineLock) {
//                        mLineLock.notifyAll();
//                    }
//                } else {
//                    String txt = mAWTArea.getText();
//                    int idx = txt.lastIndexOf('\n', mAWTArea.getCaretPosition() - 1);
//                    mLastLine = txt.substring(idx != -1 ? idx : 0, mAWTArea.getCaretPosition());//txt.length());
//                    synchronized (mLineLock) {
//                        mLineLock.notifyAll();
//                    }
//                }
//            }
//        }
//
//        @Override
//        public void keyReleased(KeyEvent ke) {
//        }
//
//        @Override
//        public void keyTyped(KeyEvent ke) {
//        }
//
//        @Override
//        public int read(char[] arg0, int arg1, int arg2) throws IOException {
//            throw new IOException("Not supported");
//        }
//
//        public String readLine() {
//            synchronized (mLineLock) {
//                try {
//                    mLineLock.wait();
//                } catch (InterruptedException ex) {
//                }
//            }
//            return mLastLine;
//        }
//
//        @Override
//        public int read() {
//            synchronized (mKeyLock) {
//                try {
//                    mKeyLock.wait();
//                } catch (InterruptedException ex) {
//                }
//            }
//            return mLastKeyCode;
//        }
//
//        @Override
//        public void close() throws IOException {
//            // TODO Auto-generated method stub
//        }
//
////	public static void main(String args[]) {
////		JFrame f = new JFrame("TextAreaInput Test");
////		JTextArea area = new JTextArea();
////		final TextAreaReader tar = new TextAreaReader(area);
////		f.add(area);
////		
////		Runnable r1 = new Runnable() {
////			public void run() {
////				while (true) {
////					int code = tar.read();
////					System.out.println("read: " + code);
////				}
////			}
////		};
////		Runnable r2 = new Runnable() {
////			public void run() {
////				while (true) {
////					String line = tar.readLine();
////					System.out.println("read line: " + line);
////				}
////			}			
////		};
////		Thread t1 = new Thread(r1);
////		Thread t2 = new Thread(r2);
////		t1.start();
////		t2.start();
////		f.setBounds(100, 100, 200, 200);
////		f.setVisible(true);
////	}
//        public InputStream toInputStream() {
//            return new MyInputStream();
//        }
//
//        private class MyInputStream extends InputStream {
//
//            public int read() {
//                return TextAreaReader.this.read();
//            }
//        }
//    }
}
