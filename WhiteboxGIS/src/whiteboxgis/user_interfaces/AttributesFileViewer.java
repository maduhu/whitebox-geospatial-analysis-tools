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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ResourceBundle;
import java.lang.reflect.InvocationTargetException;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.swing.*;
import static javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import whitebox.geospatialfiles.VectorLayerInfo;
import whitebox.geospatialfiles.ShapeFile;
import whitebox.geospatialfiles.shapefile.ShapeFileRecord;
import whitebox.geospatialfiles.shapefile.ShapeType;
import whitebox.geospatialfiles.shapefile.attributes.DBFField;
import whitebox.geospatialfiles.shapefile.attributes.AttributeTable;
import whitebox.geospatialfiles.shapefile.attributes.DBFField.DBFDataType;
import whitebox.interfaces.WhiteboxPluginHost;
//import whiteboxgis.user_interfaces.Scripter.ScriptingLanguage;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
//import static whiteboxgis.user_interfaces.Scripter.ScriptingLanguage.PYTHON;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class AttributesFileViewer extends JDialog implements ActionListener {

    private String dbfFileName = "";
    private String shapeFileName = "";
    private AttributeTable attributeTable;
    //private JButton edit = new JButton("Edit");
    private JTable dataTable = new JTable();
    private JTable fieldTable = new JTable();
    private JTabbedPane tabs;
    private WhiteboxPluginHost host = null;
    private ShapeFile shapeFile = null;
    private ResourceBundle bundle;
    private ResourceBundle messages;
//    private Scripter scripter = null;
    private int generateDataColumnIndex = -1;
    private VectorLayerInfo vectorLayerInfo = null;
    private RSyntaxTextArea editor;
    private ScriptEngineManager mgr = new ScriptEngineManager();
    private ScriptEngine engine;
    private JComboBox targetFieldCB;
    private JTextArea textArea = new JTextArea();
    private String scriptsDirectory = "";

    public AttributesFileViewer(Frame owner, boolean modal, VectorLayerInfo vli) {
        super(owner, modal);
        if (owner instanceof WhiteboxPluginHost) {
            host = (WhiteboxPluginHost) owner;
            bundle = host.getGuiLabelsBundle();
            messages = host.getMessageBundle();
            scriptsDirectory = host.getResourcesDirectory() + "plugins" + File.separator + "Scripts" + File.separator;
        }
        if (owner != null) {
            Dimension parentSize = owner.getSize();
            Point p = owner.getLocation();
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        this.shapeFileName = vli.getFileName();
        this.vectorLayerInfo = vli;

        if (shapeFileName.toLowerCase().contains(".shp")) {
            dbfFileName = shapeFileName.replace(".shp", ".dbf");
        } else if (shapeFileName.toLowerCase().contains(".dbf")) {
            dbfFileName = shapeFileName;
        }
        try {
            shapeFile = new ShapeFile(shapeFileName);
            attributeTable = shapeFile.getAttributeTable();
            initScriptEngine();
            createGui();
        } catch (IOException e) {
            if (owner instanceof WhiteboxPluginHost) {
                WhiteboxPluginHost wph = (WhiteboxPluginHost) owner;
                wph.showFeedback(messages.getString("NoDBF"));
            } else {
                JLabel warning = new JLabel(messages.getString("NoDBF"));
                this.add(warning);
            }
        }

        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });

//        // Throwing this on the EDT to allow the window to pop up faster
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                scripter = new Scripter((Frame) host, true);
//                // Add listener for clicking the generate data button in the scripter
//                scripter.addPropertyChangeListener(Scripter.PROP_GENERATE_DATA, generateDataListener);
//                scripter.addPropertyChangeListener(Scripter.PROP_SCRIPTING_LANGUAGE, languageChangedListener);
//                setScripterDefaultText(scripter.getLanguage());
//                scripter.showGenerateDataButton(true);
//            }
//        });
    }

    public AttributesFileViewer(Frame owner, boolean modal, String shapeFileName) {
        super(owner, modal);
        if (owner instanceof WhiteboxPluginHost) {
            host = (WhiteboxPluginHost) owner;
            bundle = host.getGuiLabelsBundle();
            messages = host.getMessageBundle();
        }
        if (owner != null) {
            Dimension parentSize = owner.getSize();
            Point p = owner.getLocation();
            setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
        }
        this.shapeFileName = shapeFileName;

        if (shapeFileName.toLowerCase().contains(".shp")) {
            dbfFileName = shapeFileName.replace(".shp", ".dbf");
        } else if (shapeFileName.toLowerCase().contains(".dbf")) {
            dbfFileName = shapeFileName;
        }
        try {
            shapeFile = new ShapeFile(shapeFileName);
            attributeTable = shapeFile.getAttributeTable();
            createGui();
        } catch (IOException e) {
            if (owner instanceof WhiteboxPluginHost) {
                WhiteboxPluginHost wph = (WhiteboxPluginHost) owner;
                wph.showFeedback(messages.getString("NoDBF"));
            } else {
                JLabel warning = new JLabel(messages.getString("NoDBF"));
                this.add(warning);
            }
        }

        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });

//        // Throwing this on the EDT to allow the window to pop up faster
//        SwingUtilities.invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                scripter = new Scripter((Frame) host, true);
//                // Add listener for clicking the generate data button in the scripter
//                scripter.addPropertyChangeListener(Scripter.PROP_GENERATE_DATA, generateDataListener);
//                scripter.addPropertyChangeListener(Scripter.PROP_SCRIPTING_LANGUAGE, languageChangedListener);
//                setScripterDefaultText(scripter.getLanguage());
//                scripter.showGenerateDataButton(true);
//            }
//        });
    }

    private void createGui() {
        try {
            if (System.getProperty("os.name").contains("Mac")) {
                this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
            }

            graphicsDirectory = host.getApplicationDirectory() + File.separator + "resources" + File.separator + "Images" + File.separator;;

            File file = new File(dbfFileName);
            String shortFileName = file.getName();
            shortFileName = shortFileName.replace(".dbf", "");

            setTitle(bundle.getString("LayerAttributeTable") + ": " + shortFileName);

            // okay and close buttons.
            Box box1 = Box.createHorizontalBox();
            box1.add(Box.createHorizontalStrut(10));
            box1.add(Box.createRigidArea(new Dimension(5, 30)));
            box1.add(Box.createRigidArea(new Dimension(5, 30)));

            JButton close = new JButton(bundle.getString("Close"));
            close.setActionCommand("close");
            close.addActionListener(this);
            close.setToolTipText(bundle.getString("ExitWithoutSaving"));
            box1.add(close);

            JButton save = new JButton(bundle.getString("Save"));
            save.setActionCommand("save");
            save.addActionListener(this);
            save.setToolTipText(bundle.getString("SaveChanges"));
            box1.add(save);

            box1.add(Box.createHorizontalStrut(100));
            box1.add(Box.createHorizontalGlue());

            add(box1, BorderLayout.SOUTH);

            Box mainBox = Box.createVerticalBox();

            dataTable = getDataTable();
            dataTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

            JScrollPane scroll = new JScrollPane(dataTable);
            tabs = new JTabbedPane();

            JPanel panel1 = new JPanel();
            panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
            panel1.add(scroll);
            tabs.addTab(bundle.getString("AttributesTable"), panel1);

            // field table
            JPanel panel2 = new JPanel();
            panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));

            fieldTable = getFieldTable();
            fieldTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

            JScrollPane scroll2 = new JScrollPane(fieldTable);
            panel2.add(scroll2);
            tabs.addTab(bundle.getString("FieldSummary"), panel2);

            tabs.addTab(bundle.getString("FieldCalculator"), getFieldCalculator());

            mainBox.add(tabs);
            this.getContentPane().add(mainBox, BorderLayout.CENTER);

            JMenuBar menubar = createMenu();
            this.setJMenuBar(menubar);

            pack();

            // Centre the dialog on the screen.
            // Get the size of the screen
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            int screenHeight = dim.height;
            int screenWidth = dim.width;
            //setSize(screenWidth / 2, screenHeight / 2);
            this.setLocation(screenWidth / 4, screenHeight / 4);

            setScripterDefaultText();

        } catch (Exception e) {
            if (host != null) {
                host.showFeedback(messages.getString("ErrorExecutingScript"));
                host.logException("Error from AttributesFileViewer", e);
            }
        }
    }

    private JPanel getFieldCalculator() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JToolBar toolbar = new JToolBar();

        toolbar.add(new JLabel("Target Field:"));
        targetFieldCB = new JComboBox();
        updateFieldComboBox();
        targetFieldCB.setMaximumSize(new Dimension(20, 80)); //targetFieldCB.getPreferredSize() );
        toolbar.add(targetFieldCB);

        toolbar.addSeparator();

        JButton openBtn = makeToolBarButton("open.png", "open", bundle.getString("OpenFile"), "Open");
        toolbar.add(openBtn);

        JButton closeBtn = makeToolBarButton("close.png", "closeScript", bundle.getString("CloseFile"), "Close");
        toolbar.add(closeBtn);

        JButton saveBtn = makeToolBarButton("SaveMap.png", "savescript", bundle.getString("SaveFile"), "Save");
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

//        generateDataButton = makeToolBarButton("GenerateData.png", "generateData",
//                bundle.getString("GenerateColumnData"), "Generate Data");
//        toolbar.add(generateDataButton);
//
//        showGenerateDataButton(false);
        toolbar.add(Box.createHorizontalGlue());

        toolbar.setMaximumSize(new Dimension(20000, 50));

        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.Y_AXIS));
        toolbarPanel.add(toolbar);
        panel.add(toolbarPanel, BorderLayout.PAGE_START);

        editor = new RSyntaxTextArea(20, 60);
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
        editor.setCodeFoldingEnabled(true);
        editor.setAntiAliasingEnabled(true);
        editor.setCloseCurlyBraces(true);
        //editor.addKeyListener(this);
        editor.setTabSize(4);
        editor.setBracketMatchingEnabled(true);
        editor.setAutoIndentEnabled(true);
        editor.setCloseCurlyBraces(true);
        editor.setMarkOccurrences(true);
        //resetAutocompletion();
//            setupAutocomplete();
        RTextScrollPane scroll = new RTextScrollPane(editor);
        scroll.setFoldIndicatorEnabled(true);

        panel.add(scroll);

        return panel;
    }

    private void updateFieldComboBox() {
        if (targetFieldCB == null) {
            return;
        }
        final AttributeFieldTableModel model = new AttributeFieldTableModel(attributeTable);
        String[] fieldNames = new String[model.getRowCount()];
        for (int i = 0; i < model.getRowCount(); i++) {
            fieldNames[i] = String.valueOf(model.getValueAt(i, 0));
        }
        targetFieldCB.setModel(new DefaultComboBoxModel(fieldNames));
        targetFieldCB.revalidate();
        targetFieldCB.repaint();
    }

    String graphicsDirectory = "";

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

    private JTable getDataTable() {

        final AttributeFileTableModel model = new AttributeFileTableModel(attributeTable);
        final JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int index_row, int index_col) {
                Component comp = super.prepareRenderer(renderer, index_row, index_col);
                //even index, selected or not selected

                if (index_row % 2 == 0) {
                    comp.setBackground(Color.WHITE);
                    comp.setForeground(Color.BLACK);
                } else {
                    comp.setBackground(new Color(225, 245, 255)); //new Color(210, 230, 255));
                    comp.setForeground(Color.BLACK);
                }
                if (isCellSelected(index_row, index_col)) {
                    comp.setForeground(Color.RED);
                }
                return comp;
            }
        };

        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
//        table.setSelectionBackground(Color.CYAN);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        TableColumn column;
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < table.getColumnCount(); i++) {
            column = columnModel.getColumn(i);
            if (i == 0) {
                column.setPreferredWidth(10);
            } else if (i == 1) {
                column.setPreferredWidth(40);
            } else {
                column.setPreferredWidth(70);
            }
        }

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                handleEvent(e);
                selectRecord();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                handleEvent(e);
            }

            private void handleEvent(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
//                if (r >= 0 && r < table.getRowCount()) {
//                    table.setRowSelectionInterval(r, r);
//                } else {
//                    table.clearSelection();
//                }

//                // what is the record index?
//                int recNum = (int)table.getValueAt(r, 1);
//                selectRecord(recNum);
                int rowIndex = table.getSelectedRow();
                if (rowIndex < 0) {
                    return;
                }
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    JPopupMenu popup = createDataRevertPopup(model, rowIndex);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        return table;
    }

    private void toggleComment(int lineNum) {
        try {
            int start = editor.getLineStartOffset(lineNum);
            String openingCharacters = editor.getText(start, 2);
            if (openingCharacters.startsWith("//")) {
                // remove the line comment tag.
                editor.replaceRange("", start, start + 2);
            } else {
                // add a line comment tag.
                editor.insert("//", start);
            }
        } catch (Exception e) {
            host.logException("Error in AttributesFileViewer", e);
        }
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
            host.logException("Error in AttributesFileViewer", e);
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
            host.logException("Error in AttributesFileViewer", e);
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
            host.logException("Error in AttributesFileViewer", e);
        }
    }

    private void initScriptEngine() {
        try {

            engine = mgr.getEngineByName("groovy");
            //PrintWriter out = new PrintWriter(new Scripter.TextAreaWriter(textArea));

//            engine.getContext().setWriter(out);
//            engine.getContext().setErrorWriter(out);
            engine.put("pluginHost", (WhiteboxPluginHost) host);
            engine.put("args", new String[0]);

            // update the statusbar
            ScriptEngineFactory scriptFactory = engine.getFactory();
        } catch (Exception e) {
            host.logException("Error in AttributesFileViewer", e);
        }
    }

    private void selectRecord(int record) {
        if (vectorLayerInfo != null) {
            vectorLayerInfo.selectFeature(record);
            if (host != null) {
                host.refreshMap(false);
            }
        }
    }

    private void selectRecord() {
        if (vectorLayerInfo != null) {
            vectorLayerInfo.clearSelectedFeatures();
            int[] selectedRecords = dataTable.getSelectedRows();
            for (int r = 0; r < selectedRecords.length; r++) {
                int recNum = (int) dataTable.getValueAt(selectedRecords[r], 1) + 1;
                vectorLayerInfo.selectFeature(recNum);
            }
            if (host != null) {
                host.refreshMap(false);
            }
        }
    }

    private JTable getFieldTable() {

        final AttributeFieldTableModel model = new AttributeFieldTableModel(attributeTable);
        final JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int Index_row, int Index_col) {
                Component comp = super.prepareRenderer(renderer, Index_row, Index_col);
                //even index, selected or not selected
                if (Index_row % 2 == 0) {// && !isCellSelected(Index_row, Index_col)) {
                    comp.setBackground(Color.WHITE);
                    comp.setForeground(Color.BLACK);
                } else {
                    comp.setBackground(new Color(225, 245, 255)); //new Color(210, 230, 255));
                    comp.setForeground(Color.BLACK);
                }
                if (isCellSelected(Index_row, Index_col)) {
                    comp.setForeground(Color.RED);
                }
                return comp;
            }
        };

        // Add cell editor for type column
        int typeColIndex = AttributeFieldTableModel.ColumnName.TYPE.ordinal();
        TableColumn typeColumn = table.getColumnModel().getColumn(typeColIndex);
        JComboBox typeComboBox = new JComboBox();
        for (DBFDataType type : DBFDataType.values()) {
            typeComboBox.addItem(type);
        }
        typeColumn.setCellEditor(new DefaultCellEditor(typeComboBox));

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                handleEvent(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                handleEvent(e);
            }

            private void handleEvent(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0 && r < table.getRowCount()) {
                    table.setRowSelectionInterval(r, r);
                } else {
                    table.clearSelection();
                }

                int rowIndex = table.getSelectedRow();
                if (rowIndex < 0) {
                    return;
                }
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    JPopupMenu popup = createFieldRevertPopup(model, rowIndex);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

//        table.addPropertyChangeListener( new PropertyChangeListener() {
//
//            @Override
//            public void propertyChange(PropertyChangeEvent evt) {
//                if (evt.getPropertyName().equals("tableCellEditor")) {
//                    updateFieldComboBox();
//                }
//            }
//        });
        return table;
    }

    /**
     * Displays a popup window for data table at row and will revert all unsaved
     * commands on that row.
     *
     * @param row
     */
    private JPopupMenu createDataRevertPopup(final AttributeFileTableModel model, final int row) {

        JPopupMenu popup = new JPopupMenu();
        JMenuItem revertItem = new JMenuItem(bundle.getString("RevertChanges"));

        if (model.isModified(row)) {
            revertItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    model.revertRow(row);
                }
            });
        } else {
            revertItem.setEnabled(false);
        }

        popup.add(revertItem);

        return popup;
    }

    /**
     * Displays a popup window for field table at row and will revert all
     * unsaved commands on that row.
     *
     * @param row
     */
    private JPopupMenu createFieldRevertPopup(final AttributeFieldTableModel model, final int row) {

        JPopupMenu popup = new JPopupMenu();

        JMenuItem changeFieldTitleItem = new JMenuItem(bundle.getString("ChangeFieldTitle"));
        changeFieldTitleItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String s = (String) JOptionPane.showInputDialog(
                        (JFrame)host,
                        "Please enter a new title.",
                        "New Field Title",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        "");
                if (s.length() > 10) {
                    s = s.substring(0, 9);
                }
                s = s.toUpperCase().replace(" ", "_");
                model.changeFieldTitle(row, s);
                AttributeFileTableModel dataModel = (AttributeFileTableModel) dataTable.getModel();
                dataModel.unhideColumns();
                dataModel.fireTableDataChanged();
                updateFieldComboBox();
            }
        });

        popup.add(changeFieldTitleItem);

        JMenuItem revertItem = new JMenuItem(bundle.getString("RevertChanges"));

        if (model.isModified(row)) {
            revertItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    model.revertRow(row);
                }
            });
        } else {
            revertItem.setEnabled(false);
        }

        popup.add(revertItem);

        JMenuItem deleteItem = new JMenuItem(bundle.getString("DeleteField"));
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.deleteField(row);
            }
        });

        popup.add(deleteItem);

        return popup;
    }

    private JMenuBar createMenu() {
        JMenuBar menubar = new JMenuBar();

        // Add Field menu
        JMenu addFieldMenu = new JMenu(bundle.getString("EditFields"));

        JMenuItem addNewField = new JMenuItem(bundle.getString("AddNewField"));
        addNewField.setActionCommand("addNewField");
        addNewField.addActionListener(this);
        addFieldMenu.add(addNewField);

        JMenuItem deleteField = new JMenuItem(bundle.getString("DeleteField") + "...");
        deleteField.setActionCommand("deleteField");
        deleteField.addActionListener(this);
        addFieldMenu.add(deleteField);

        if (shapeFile.getShapeType().getBaseType() == ShapeType.POLYGON) {
            JMenuItem addAreaField = new JMenuItem(bundle.getString("AddAreaField"));
            addAreaField.setActionCommand("addAreaField");
            addAreaField.addActionListener(this);
            addFieldMenu.add(addAreaField);

            JMenuItem addPerimeterField = new JMenuItem(bundle.getString("AddPerimeterField"));
            addPerimeterField.setActionCommand("addPerimeterField");
            addPerimeterField.addActionListener(this);
            addFieldMenu.add(addPerimeterField);
        }

        menubar.add(addFieldMenu);

//        JMenu generateFieldData = new JMenu(bundle.getString("GenerateData"));
//
//        JMenuItem generateData = new JMenuItem(bundle.getString("GenerateData") + "...");
//        generateData.setActionCommand("generateData");
//        generateData.addActionListener(this);
//        generateFieldData.add(generateData);
//
//        menubar.add(generateFieldData);
        return menubar;
    }

    private void print() {
        try {
            editor.print();
        } catch (Exception e) {
            host.logException("Error in AttributesFileViewer.", e);
        }
    }

    public void openFile(String fileName) {
        sourceFile = fileName;
        if (sourceFile == null || sourceFile.isEmpty()) {
            openFile();
        }

        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
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
            host.logException("Error in AttributesFileViewer", e);
        }

        editor.setEditable(true);
        editor.setCaretPosition(0);

        //this.setTitle("Whitebox Scripter: " + new File(sourceFile).getName());
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

            editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
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
                host.logException("Error in AttributesFileViewer", e);
            }

            editor.setEditable(true);
            editor.setCaretPosition(0);

            //this.setTitle("Whitebox Scripter: " + new File(sourceFile).getName());
        }
    }

    private void save() {
        if (sourceFile == null) {
            String extension = ".groovy";

            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
            fc.setAcceptAllFileFilterUsed(true);
            fc.setFileHidingEnabled(true);

            FileFilter ft = new FileNameExtensionFilter("Groovy " + bundle.getString("Files"), "groovy");
            fc.addChoosableFileFilter(ft);

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

                //this.setTitle("Whitebox Scripter: " + new File(sourceFile).getName());
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
            host.logException("Error in AttributesFileViewer", e);
        } catch (Exception e) { //Catch exception if any
            host.logException("Error in AttributesFileViewer", e);
        } finally {
            if (out != null || bw != null) {
                out.flush();
                out.close();
            }
        }
    }

    private String sourceFile;

    private void saveAs() {
        sourceFile = null;
        save();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand().toLowerCase();
        switch (actionCommand) {
            case "open":
                openFile();
                break;
            case "closescript":
                //if (editorDirty) {
                Object[] options = {"Yes", "No", "Cancel"};
                int n = JOptionPane.showOptionDialog(this,
                        "Would you like to save the code?",
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
                //}
                editor.setText("");
                sourceFile = null;
                break;
            case "close":
                closeWindow();
                break;
            case "save":
                saveChanges();
                break;
            case "addnewfield":
                tabs.setSelectedIndex(1);
                addNewField();
                break;
            case "deletefield":
                deleteField();
                break;
            case "addareafield":
                addAreaField();
                break;
            case "addperimeterfield":
                addPerimeterField();
                break;
//            case "generateData":
//                showScripter();
//                break;
//            
//           case "generatedata":
//                this.firePropertyChange("generateData", false, true);
//                break;
            case "savescript":
                saveAs();
                break;
            case "indent":
                indentSelection();
                break;
            case "outdent":
                outdentSelection();
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
            case "paste":
                editor.paste();
//                resetAutocompletion();
//                scanDoc();
                break;
            case "comment":
                comment();
                break;
            case "execute":
                executeScript();
                break;
            case "print":
                print();
                break;

        }
    }

    private void closeWindow() {
        AttributeFileTableModel dataModel = (AttributeFileTableModel) dataTable.getModel();
        AttributeFieldTableModel fieldModel = (AttributeFieldTableModel) fieldTable.getModel();
        if (!fieldModel.isSaved()) {
            tabs.setSelectedIndex(1);
            int continueChoice = JOptionPane.showOptionDialog(this,
                    messages.getString("NewFieldsCreated"),
                    messages.getString("Continue") + "?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (continueChoice != JOptionPane.OK_OPTION) {
                return;
            }
        }
        if (!dataModel.isSaved()) {
            tabs.setSelectedIndex(0);
            int continueChoice = JOptionPane.showOptionDialog(this,
                    messages.getString("UnsavedChanges"),
                    messages.getString("Continue") + "?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (continueChoice != JOptionPane.OK_OPTION) {
                return;
            }
        }
        this.dispose();
    }

    private void saveChanges() {

        AttributeFileTableModel dataModel = (AttributeFileTableModel) dataTable.getModel();
        AttributeFieldTableModel fieldModel = (AttributeFieldTableModel) fieldTable.getModel();
//        dataTable.getCellEditor().stopCellEditing();
//        fieldTable.getCellEditor().stopCellEditing();

        if (!fieldModel.isSaved()) {
            tabs.setSelectedIndex(1);
            int option = JOptionPane.showOptionDialog(rootPane,
                    messages.getString("SaveChangesQuestion"),
                    messages.getString("SaveChanges"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (option == JOptionPane.OK_OPTION) {

                boolean success = fieldModel.saveChanges();
                if (!success) {
                    JOptionPane.showMessageDialog(this,
                            messages.getString("ErrorSavingChanges"),
                            messages.getString("ErrorSaving"),
                            JOptionPane.ERROR_MESSAGE);
                }

                fieldModel.fireTableDataChanged();
                dataModel.unhideColumns();
                dataModel.fireTableDataChanged();
            }

        }

        if (!dataModel.isSaved()) {
            tabs.setSelectedIndex(0);
            int option = JOptionPane.showOptionDialog(rootPane,
                    messages.getString("SaveChangesQuestion"),
                    messages.getString("SaveChanges") + "?",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (option == JOptionPane.OK_OPTION) {

                boolean success = dataModel.saveChanges();
                if (!success) {
                    JOptionPane.showMessageDialog(this,
                            messages.getString("ErrorSavingChanges"),
                            messages.getString("ErrorSaving"),
                            JOptionPane.ERROR_MESSAGE);
                }
                dataModel.fireTableDataChanged();
            }
        }

        updateFieldComboBox();

    }

    /**
     * Adds a new field to the field table model.
     */
    private void addNewField() {
        AttributeFieldTableModel model = (AttributeFieldTableModel) fieldTable.getModel();

        model.createNewField();

        updateFieldComboBox();
    }

    private class SelectionIdentifier {

        private int index;
        private Object value;

        public SelectionIdentifier(int index, Object value) {
            this.index = index;
            this.value = value;
        }

        public int getIndex() {
            return index;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public String toString() {
            if (value != null) {
                return value.toString();
            }

            return null;
        }
    }

    /**
     * Hides a field from the field table model and marks the field for deletion
     * on the next save.
     */
    private void deleteField() {
        AttributeFieldTableModel fieldModel = (AttributeFieldTableModel) fieldTable.getModel();

        int fieldCount = fieldModel.getRowCount();

        Object[] selectionOptions = new Object[fieldCount];

        for (int i = 0; i < fieldCount; i++) {
            SelectionIdentifier wrapper = new SelectionIdentifier(i, fieldModel.getValueAt(i,
                    fieldModel.findColumn(AttributeFieldTableModel.ColumnName.NAME.toString())));
            selectionOptions[i] = wrapper;

        }

        Object selection = JOptionPane.showInputDialog(this,
                messages.getString("SelectFieldToDelete"),
                bundle.getString("DeleteField"),
                JOptionPane.OK_CANCEL_OPTION, null, selectionOptions, null);

        if (selection != null) {
            int selectionIndex = ((SelectionIdentifier) selection).getIndex();

            fieldModel.deleteField(selectionIndex);

            AttributeFileTableModel dataModel = (AttributeFileTableModel) dataTable.getModel();
            dataModel.hideColumn(selectionIndex);

            //System.out.println("Deleting: " + selection.toString());
        }

    }

    private void addAreaField() {

        try {
            ShapeType inputType = shapeFile.getShapeType();
            if (inputType.getBaseType() != ShapeType.POLYGON) {
                if (host != null) {
                    host.showFeedback(messages.getString("PolygonsOnly"));
                    return;
                }
            }
            double area;
            int recNum;

            DBFField field = new DBFField();
            field.setName(bundle.getString("Area"));
            field.setDataType(DBFField.DBFDataType.NUMERIC);
            field.setFieldLength(10);
            field.setDecimalCount(3);
            this.attributeTable.addField(field);
            for (ShapeFileRecord record : shapeFile.records) {
                if (record.getShapeType() != ShapeType.NULLSHAPE) {
                    if (inputType == ShapeType.POLYGON) {
                        whitebox.geospatialfiles.shapefile.Polygon recPolygon
                                = (whitebox.geospatialfiles.shapefile.Polygon) (record.getGeometry());
                        area = recPolygon.getArea();
                    } else if (inputType == ShapeType.POLYGONZ) {
                        whitebox.geospatialfiles.shapefile.PolygonZ recPolygon
                                = (whitebox.geospatialfiles.shapefile.PolygonZ) (record.getGeometry());
                        area = recPolygon.getArea();
                    } else { // POLYGONM
                        whitebox.geospatialfiles.shapefile.PolygonM recPolygon
                                = (whitebox.geospatialfiles.shapefile.PolygonM) (record.getGeometry());
                        area = recPolygon.getArea();
                    }

                    recNum = record.getRecordNumber() - 1;
                    Object[] recData = this.attributeTable.getRecord(recNum);
                    recData[recData.length - 1] = new Double(area);
                    this.attributeTable.updateRecord(recNum, recData);

                }
            }

            updateFieldComboBox();

            host.showFeedback(messages.getString("CalculationComplete"));

        } catch (Exception e) {
            if (host != null) {
                host.showFeedback("Error in attributes table viewer. Exception being logged");
                host.logException("Error from AttributesFileViewer", e);
            }
        }

    }

    private void addPerimeterField() {

        try {
            ShapeType inputType = shapeFile.getShapeType();
            if (inputType.getBaseType() != ShapeType.POLYGON) {
                if (host != null) {
                    host.showFeedback(messages.getString("PolygonsOnly"));
                    return;
                }
            }
            double perimeter;
            int recNum;

            DBFField field = new DBFField();
            field.setName(bundle.getString("Perimeter"));
            field.setDataType(DBFField.DBFDataType.NUMERIC);
            field.setFieldLength(10);
            field.setDecimalCount(3);
            this.attributeTable.addField(field);
            for (ShapeFileRecord record : shapeFile.records) {
                if (inputType != ShapeType.NULLSHAPE) {
                    if (shapeFile.getShapeType() == ShapeType.POLYGON) {
                        whitebox.geospatialfiles.shapefile.Polygon recPolygon
                                = (whitebox.geospatialfiles.shapefile.Polygon) (record.getGeometry());
                        perimeter = recPolygon.getPerimeter();
                    } else if (inputType == ShapeType.POLYGONZ) {
                        whitebox.geospatialfiles.shapefile.PolygonZ recPolygon
                                = (whitebox.geospatialfiles.shapefile.PolygonZ) (record.getGeometry());
                        perimeter = recPolygon.getPerimeter();
                    } else { // POLYGONM
                        whitebox.geospatialfiles.shapefile.PolygonM recPolygon
                                = (whitebox.geospatialfiles.shapefile.PolygonM) (record.getGeometry());
                        perimeter = recPolygon.getPerimeter();
                    }
                    recNum = record.getRecordNumber() - 1;
                    Object[] recData = this.attributeTable.getRecord(recNum);
                    recData[recData.length - 1] = new Double(perimeter);
                    this.attributeTable.updateRecord(recNum, recData);

                }

            }

            updateFieldComboBox();
            host.showFeedback(messages.getString("CalculationComplete"));

        } catch (Exception e) {
            if (host != null) {
                host.showFeedback("Error in attributes table viewer. Exception being logged");
                host.logException("Error from AttributesFileViewer", e);
            }
        }

    }

//    /**
//     * Prompts the user for which column they want to modify then shows the
//     * Scripter dialog that allows them to generate data for that column. The
//     * field model must be saved before this can be run in so that data can be
//     * generated for a new column.
//     */
//    private void showScripter() {
//
//        AttributeFieldTableModel fieldModel = (AttributeFieldTableModel) fieldTable.getModel();
//
//        if (!fieldModel.isSaved()) {
//            JOptionPane.showMessageDialog(this,
//                    messages.getString("FileMustBeSaved"),
//                    messages.getString("SaveToContinue"),
//                    JOptionPane.INFORMATION_MESSAGE);
//            return;
//        }
//
//        int fieldCount = fieldModel.getRowCount();
//
//        Object[] selectionOptions = new Object[fieldCount];
//
//        for (int i = 0; i < fieldCount; i++) {
//            SelectionIdentifier wrapper = new SelectionIdentifier(i, fieldModel.getValueAt(i,
//                    fieldModel.findColumn(AttributeFieldTableModel.ColumnName.NAME.toString())));
//            selectionOptions[i] = wrapper;
//
//        }
//
//        Object selection = JOptionPane.showInputDialog(this,
//                messages.getString("SelectField"),
//                messages.getString("GenerateColumnData"),
//                JOptionPane.OK_CANCEL_OPTION, null, selectionOptions, null);
//
//        if (selection != null) {
//
//            this.generateDataColumnIndex = ((SelectionIdentifier) selection).getIndex();
//
//            scripter.setVisible(true);
//        }
//    }
//    PropertyChangeListener generateDataListener = new PropertyChangeListener() {
//        @Override
//        public void propertyChange(PropertyChangeEvent evt) {
//            generateData();
//            scripter.setVisible(false);
//        }
//    };
//    PropertyChangeListener languageChangedListener = new PropertyChangeListener() {
//        @Override
//        public void propertyChange(PropertyChangeEvent evt) {
//            Scripter.ScriptingLanguage lang = (Scripter.ScriptingLanguage) evt.getNewValue();
//
//            setScripterDefaultText(lang);
//        }
//    };
    private void setScripterDefaultText() { //ScriptingLanguage lang) {

        String commentMarker = "//";

        String default_text = commentMarker + " "
                + messages.getString("ScriptMessage1") + "\n"
                + commentMarker + " " + messages.getString("ScriptMessage2") + "\n"
                + commentMarker + " " + messages.getString("ScriptMessage3") + "\n"
                + commentMarker + " " + messages.getString("ScriptMessage4") + "\n";
//        String default_text = lang.getCommentMarker() + " "
//                + messages.getString("ScriptMessage1") + "\n"
//                + lang.getCommentMarker() + " " + messages.getString("ScriptMessage2") + "\n"
//                + lang.getCommentMarker() + " " + messages.getString("ScriptMessage3") + "\n";

//        switch (lang) {
//            case PYTHON:
//            case GROOVY:
//            case JAVASCRIPT:
        default_text += "index + 1";
//                break;
//
//        }
//        scripter.setEditorText(default_text);
        editor.setText(default_text);
    }

    private void executeScript() {
        AttributeFieldTableModel fieldModel = (AttributeFieldTableModel) fieldTable.getModel();
        AttributeFileTableModel dataModel = (AttributeFileTableModel) dataTable.getModel();
        int fieldCount = fieldModel.getRowCount();

        try {

            this.generateDataColumnIndex = targetFieldCB.getSelectedIndex();

            CompiledScript generate_data = ((Compilable) engine).compile(this.editor.getText());

            Bindings bindings = engine.createBindings();

            for (int row = 0; row < dataModel.getRowCount(); row++) {
                bindings.put("index", new Integer(row));

                // Bind each of the variables from the row
                for (int i = 0; i < fieldCount; i++) {
                    String fieldName = (String) fieldModel.getValueAt(i,
                            fieldModel.findColumn(AttributeFieldTableModel.ColumnName.NAME.toString()));
                    // Add 2 because we need to skip modified and ID in dataModel
                    bindings.put(fieldName, dataModel.getValueAt(row, i + 2));

                }

                Object data = generate_data.eval(bindings);

                if (data != null) {
                    Class dataClass = data.getClass();
                    DBFField[] fields = attributeTable.getAllFields();
                    Class fieldClass = fields[this.generateDataColumnIndex].getDataType().getEquivalentClass();
                    if (dataClass != fieldClass) {
                        try {
                            data = fieldClass.getConstructor(String.class).newInstance(data.toString());

                        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            //System.out.println(e);
                            if (host != null) {
                                host.showFeedback(messages.getString("UnableToConvertDataType"));
                                host.logException("Error from AttributesFileViewer", e);
                            }
                            return;
                        }
                    }
                } else {
                    if (host != null) {
                        host.showFeedback(messages.getString("ErrorAddingData"));
                    }
                }

                dataModel.setValueAt(data, row, 2 + this.generateDataColumnIndex);

            }
            if (host != null) {
                host.showFeedback(messages.getString("CalcComplete"));
            }
        } catch (ScriptException e) {
            if (host != null) {
                host.showFeedback(messages.getString("ErrorExecutingScript"));
                host.logException("Error from AttributesFileViewer", e);
            }
        }
    }

//    private void generateData() {
//
//        AttributeFieldTableModel fieldModel = (AttributeFieldTableModel) fieldTable.getModel();
//        AttributeFileTableModel dataModel = (AttributeFileTableModel) dataTable.getModel();
//        int fieldCount = fieldModel.getRowCount();
//
//        CompiledScript generate_data = scripter.compileScript();
//
//        try {
//            Bindings bindings = scripter.createBindingsObject();
//
//            for (int row = 0; row < dataModel.getRowCount(); row++) {
//                bindings.put("index", new Integer(row));
//
//                // Bind each of the variables from the row
//                for (int i = 0; i < fieldCount; i++) {
//                    String fieldName = (String) fieldModel.getValueAt(i,
//                            fieldModel.findColumn(AttributeFieldTableModel.ColumnName.NAME.toString()));
//                    // Add 2 because we need to skip modified and ID in dataModel
//                    bindings.put(fieldName, dataModel.getValueAt(row, i + 2));
//
//                }
//
//                Object data = generate_data.eval(bindings);
//
//                if (data != null) {
//                    Class dataClass = data.getClass();
//                    DBFField[] fields = attributeTable.getAllFields();
//                    Class fieldClass = fields[this.generateDataColumnIndex].getDataType().getEquivalentClass();
//                    if (dataClass != fieldClass) {
//                        try {
//                            data = fieldClass.getConstructor(String.class).newInstance(data.toString());
//
//                        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
//                            //System.out.println(e);
//                            if (host != null) {
//                                host.showFeedback(messages.getString("UnableToConvertDataType"));
//                                host.logException("Error from AttributesFileViewer", e);
//                            }
//                            return;
//                        }
//                    }
//                } else {
//                    if (host != null) {
//                        host.showFeedback(messages.getString("ErrorAddingData"));
//                    }
//                }
//
//                dataModel.setValueAt(data, row, 2 + this.generateDataColumnIndex);
//
//            }
//
//        } catch (ScriptException e) {
//            if (host != null) {
//                host.showFeedback(messages.getString("ErrorExecutingScript"));
//                host.logException("Error from AttributesFileViewer", e);
//            }
//        }
//    }
}
