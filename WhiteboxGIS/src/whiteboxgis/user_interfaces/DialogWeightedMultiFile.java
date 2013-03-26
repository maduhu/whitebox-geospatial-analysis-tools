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

import javax.swing.border.*;
import whitebox.interfaces.DialogComponent;
import whitebox.interfaces.Communicator;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.Toolkit;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.io.File;
import java.util.ArrayList;
import whitebox.structures.ExtensionFileFilter;

/**
 *
 * @author Dr. John Lindsay <jlindsay@uoguelph.ca>
 */
public class DialogWeightedMultiFile extends JPanel implements ActionListener, DialogComponent {

    public static final String[] columnNames = {
        "Cost?", "File Name", "", "Weight", ""
    };
    protected JTable table;
    protected JScrollPane scroller;
    //protected WeightedMultiFileTableModel tableModel;
    private int numArgs = 4;
    private String name;
    private String description;
    private Communicator hostDialog = null;
    private boolean showCheckbox = true;
    private String pathSep = File.separator;
    private boolean acceptAllFiles = false;
    private ArrayList<ExtensionFileFilter> filters = new ArrayList<ExtensionFileFilter>();
    private String workingDirectory;
    private int fileNameColumnNum = 1;
    
    public DialogWeightedMultiFile(Communicator host) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setMaximumSize(new Dimension(2500, 180));
        this.setPreferredSize(new Dimension(350, 180));
        hostDialog = host;

    }

    private void createUI() {
        try {
            Border border = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            this.setBorder(border);

            MyTableModel model = new MyTableModel();
            table = new JTable(model);
            if (showCheckbox) { table.getColumn("Cost?").setMaxWidth(45); }
            table.getColumn("Weight").setPreferredWidth(45);
            table.getColumn("Weight").setMaxWidth(65);
            table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
            table.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        JTable target = (JTable) e.getSource();
                        int row = target.getSelectedRow();
                        int column = target.getSelectedColumn();
                        if (column == fileNameColumnNum && target.getValueAt(row, column).equals("")) {
                            openFile(target);
                        }
                        // do some action
                    }
                }
            });

            //Create the scroll pane and add the table to it. 
            JScrollPane scrollPane = new JScrollPane(table);

            //Set up real input validation for the integer column.
            setUpEditors(table);

            scroller = new javax.swing.JScrollPane(table);
            table.setPreferredScrollableViewportSize(new java.awt.Dimension(500, 300));

            setLayout(new BorderLayout());
            this.add(scroller, BorderLayout.CENTER);

        } catch (Exception e) {
            System.out.println(e.getCause());
        }
    }

    private void setUpEditors(JTable table) {
        //Set up the editor for the integer cells.
        final FloatNumberField floatField = new FloatNumberField(0, 5);
        floatField.setHorizontalAlignment(FloatNumberField.RIGHT);

        DefaultCellEditor floatEditor =
                new DefaultCellEditor(floatField) {
                    //Override DefaultCellEditor's getCellEditorValue method
                    //to return an Integer, not a String:

                    @Override
                    public Object getCellEditorValue() {
                        return new Float(floatField.getValue());
                    }
                };
        table.setDefaultEditor(Integer.class, floatEditor);
    }

    private void openFile(JTable table) {
        int row = table.getSelectedRow();
        int col = fileNameColumnNum;
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(true);
        MyTableModel model = (MyTableModel) table.getModel();
        fc.setAcceptAllFileFilterUsed(acceptAllFiles);

        for (int i = 0; i < filters.size(); i++) {
            fc.setFileFilter(filters.get(i));
        }

        workingDirectory = hostDialog.getWorkingDirectory();
        fc.setCurrentDirectory(new File(workingDirectory));
        int result = fc.showOpenDialog(this);
        File[] files = null;
        if (result == JFileChooser.APPROVE_OPTION) {
            files = fc.getSelectedFiles();
            String fileDirectory = files[0].getParentFile() + pathSep;
            if (!fileDirectory.equals(workingDirectory)) {
                hostDialog.setWorkingDirectory(fileDirectory);
            }
            //String shortFileName;
            //int j, k;
            for (int a = 0; a < files.length; a++) {
                //j = files[a].toString().lastIndexOf(pathSep);
                //k = files[a].toString().lastIndexOf(".");
                //shortFileName = files[a].toString().substring(j + 1, k);
                //model.add(model.getSize(), shortFileName);
                if (row == model.getRowCount()) {
                    model.addEmptyRow();
                }
                model.setValueAt(files[a].toString(), row, col);
                row++;
            }
        }
    }

    @Override
    public String getValue() {
        String retString = "";
        String str;
        for (int row = 0; row < table.getModel().getRowCount(); row++) {
            str = (String)table.getModel().getValueAt(row, fileNameColumnNum);
            if (!str.trim().equals("")) {
            for (int col = 0; col < table.getModel().getColumnCount(); col++) {
                str = table.getModel().getValueAt(row, col).toString();
                if (row == 0 && col == 0) {
                    if (!str.trim().equals("")) {
                        retString += str;
                    } else {
                        retString += "not specified";
                    }
                } else {
                    if (!str.trim().equals("")) {
                        retString += ";" + str;
                    } else {
                        retString += ";not specified";
                    }
                }
            }
            }
        }
        return retString;
    }

    @Override
    public String getComponentName() {
        return name;
    }

    @Override
    public boolean getOptionalStatus() {
        return false;
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
            showCheckbox = Boolean.parseBoolean(args[2]);
            if (showCheckbox) {
                fileNameColumnNum = 1;
            } else {
                fileNameColumnNum = 0;
            }
            setFilters(args[3]);
            
            createUI();
            String descriptionAndInstruction = description + ". Press the right-arrow "
                    + "key when in the last column to add a new row.";
            this.setToolTipText(descriptionAndInstruction);
            table.setToolTipText(descriptionAndInstruction);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String[] getArgsDescriptors() {
        String[] argsDescriptors = new String[numArgs];
        argsDescriptors[0] = "String name";
        argsDescriptors[1] = "String description";
        argsDescriptors[2] = "Show cost checkbox";
        argsDescriptors[3] = "File extension filters";
        return argsDescriptors;
    }

//    private boolean validateValue(String val) {
//        String ret = "";
//        try {
//            if (!val.equals("")) {
//                Double dbl = Double.parseDouble(val);
//            }
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//
//    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals("open")) {
        }
    }

//    public void highlightLastRow(int row) {
//        int lastrow = table.getModel().getRowCount();
//        if (row == lastrow - 1) {
//            table.setRowSelectionInterval(lastrow - 1, lastrow - 1);
//        } else {
//            table.setRowSelectionInterval(row + 1, row + 1);
//        }
//
//        table.setColumnSelectionInterval(0, 0);
//    }

//    class InteractiveRenderer extends DefaultTableCellRenderer {
//
//        protected int interactiveColumn;
//
//        public InteractiveRenderer(int interactiveColumn) {
//            this.interactiveColumn = interactiveColumn;
//        }
//
//        @Override
//        public Component getTableCellRendererComponent(JTable table,
//                Object value, boolean isSelected, boolean hasFocus, int row,
//                int column) {
//            Component c = super.getTableCellRendererComponent(table, value,
//                    isSelected, hasFocus, row, column);
//            if (column == interactiveColumn && hasFocus) {
//                if ((DialogWeightedMultiFile.this.tableModel.getRowCount() - 1) == row
//                        && !DialogWeightedMultiFile.this.tableModel.hasEmptyRow()) {
//                    DialogWeightedMultiFile.this.tableModel.addEmptyRow();
//                }
//
//                highlightLastRow(row);
//            }
//
//            return c;
//        }
//    }

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

    class MyTableModel extends AbstractTableModel {

        private String[] columnNames = {"Cost?", "File Name", "Weight"};
        private Object[][] data = { {false, "", new Float(0.0)} };
        private boolean[] columnsVisible = new boolean[3];

        
        public MyTableModel() {
            if (showCheckbox) {
                columnsVisible[0] = true;
            } else {
                columnsVisible[0] = false;
            }
            columnsVisible[1] = true;
            columnsVisible[2] = true;
        }

        @Override
        public int getColumnCount() {
            int n = 0;
            for (int i = 0; i < columnsVisible.length; i++) {
                if (columnsVisible[i]) { n++; }
            }
            return n; //columnNames.length;
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[getNumber(col)]; //col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            return data[row][getNumber(col)]; //col];
        }
        
        /** 
         * This functiun converts a column number in the table
         * to the right number of the datas.
         */
        protected int getNumber(int col) {
            int n = col;    // right number to return
            int i = 0;
            do {
                if (!(columnsVisible[i])) {
                    n++;
                }
                i++;
            } while (i < n);
            // If we are on an invisible column, 
            // we have to go one step further
            while (!(columnsVisible[n])) {
                n++;
            }
            return n;
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            data[row][getNumber(col)] = value;

            if (getNumber(col) == 1) {
                if (!hasEmptyRow()) {
                    addEmptyRow();
                }
            }
        }

        public boolean hasEmptyRow() {
            if (data[getRowCount() - 1][1].equals("")) {
                return true;
            } else {
                return false;
            }
        }

        public void addEmptyRow() {
            int nRows = getRowCount();
            Object[][] data2 = new Object[nRows + 1][3];
            for (int i = 0; i < nRows; i++) {
                System.arraycopy(data[i], 0, data2[i], 0, 3);
            }
            data2[nRows][0] = false;
            data2[nRows][1] = "";
            data2[nRows][2] = new Float(0.0);
            data = data2.clone();
            this.fireTableDataChanged();
        }
    }
}

class FloatNumberField extends JTextField {

    private Toolkit toolkit;
    private NumberFormat decimalFormatter;

    public FloatNumberField(int value, int columns) {
        super(columns);
        toolkit = Toolkit.getDefaultToolkit();
        decimalFormatter = NumberFormat.getNumberInstance(Locale.getDefault());
        decimalFormatter.setParseIntegerOnly(false);
        setValue(value);
    }

    public int getValue() {
        int retVal = 0;
        try {
            retVal = decimalFormatter.parse(getText()).intValue();
        } catch (ParseException e) {
            // This should never happen because insertString allows
            // only properly formatted data to get in the field.
        }
        return retVal;
    }

    public void setValue(int value) {
        setText(decimalFormatter.format(value));
    }

    @Override
    protected Document createDefaultModel() {
        return new WholeNumberDocument();
    }

    protected class WholeNumberDocument extends PlainDocument {

        @Override
        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {

            char[] source = str.toCharArray();
            char[] result = new char[source.length];
            int j = 0;

            for (int i = 0; i < result.length; i++) {
                if (Character.isDigit(source[i])) {
                    result[j++] = source[i];
                } else {
                    toolkit.beep();
                }
            }
            super.insertString(offs, new String(result, 0, j), a);
        }
    }
}